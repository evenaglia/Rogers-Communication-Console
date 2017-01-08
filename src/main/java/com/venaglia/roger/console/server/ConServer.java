/*
 * Copyright 2016 - 2017 Ed Venaglia
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.venaglia.roger.console.server;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.System.currentTimeMillis;

import com.venaglia.roger.console.server.pi.ConPi;
import com.venaglia.roger.console.server.sim.ConSim;
import com.venaglia.roger.console.server.sim.SimulatedButtons;
import com.venaglia.roger.ui.impl.Sha256;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 1/5/17.
 */
public abstract class ConServer implements Runnable {

    private static final Pattern MATCH_LCD_SELECTOR = Pattern.compile("^0x([0-9a-f][0-9a-f])$");
    private static final Pattern MATCH_LCD_RESET_SELECTOR = Pattern.compile("^(?:0x([0-9a-f][0-9a-f])|hard)$");
    private static final Pattern MATCH_LCD_BRIGHTNESS = Pattern.compile("^(0|[1-9][0-9]{0,2}|1000)$");
    private static final Pattern MATCH_IMAGE_NAME = Pattern.compile("^(\\w+)$");
    private static final Pattern MATCH_IMAGE_NAME_OR_BUILT_IN = Pattern.compile("^(@color-bars|@checkerboard|@aspect-ratio-grid|\\w+)$");
    private static final Pattern MATCH_IMAGE_NAME_OR_ALL = Pattern.compile("^(\\*|\\w+)$");
    private static final Pattern[] MATCH_IMAGE_STORE_ARGS = {
            MATCH_IMAGE_NAME,
            Pattern.compile("^([0-9ZA-Za-z/+]+)$")
    };
    private static final Pattern[] MATCH_IMAGE_SHOW_ARGS = {
            MATCH_IMAGE_NAME_OR_BUILT_IN,
            MATCH_LCD_SELECTOR
    };
    private static final Pattern[] MATCH_IMAGE_CLEAR_ARGS = {
            MATCH_IMAGE_NAME_OR_ALL
    };
    private static final String[] IMAGE_STORE_ARG_NAMES = { "image name", "image data" };
    private static final String[] IMAGE_SHOW_ARG_NAMES = { "image name", "selector" };
    private static final String[] IMAGE_CLEAR_ARG_NAMES = { "image name" };

    protected static final int PWM_RANGE = 1000;

    private final byte[] secret;
    private final Cache<String,byte[]> imageDataCache;

    private Con con;
    private ServerSocket serverSocket;
    private Supplier<Socket> socketSupplier;
    private volatile boolean running = false;
    private PrintWriter out;

    public ConServer() {
        this.secret = loadSecret();
        this.imageDataCache = new Cache<>(256);
        if (secret == null) {
            System.err.println("Console service is insecure! No secret has been set to protect it from unauthorized access.");
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        this.con = getCon();
        while (true) {
            try (Socket socket = getSocketSupplier().get()) {
                running = true;
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(getIdleTimeout());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
                boolean auth = secret == null;
                String expectAuth = null;
                boolean done = false;
                while (!done) {
                    String rawCommand = in.readLine();
                    if (rawCommand == null) {
                        done = true;
                        continue; // disconnected
                    }
                    String[] command = rawCommand.split(" ", 2);
                    if (command.length == 0) continue;
                    String response = "ok";
                    try {
                        switch (command[0]) {
                            case "":
                                response = "";
                                break;
                            case "hello":
                                String challenge = Sha256.digest(new Date().toString().getBytes(UTF_8));
                                response = "auth-challenge " + challenge;
                                expectAuth = Sha256.hmac(secret, challenge);
                                break;
                            case "auth":
                                if (expectAuth == null) {
                                    response =
                                            "err must send \"hello\" command immediately before sending \"auth\" command";
                                } else if (secret == null) {
                                    response = "auth-success";
                                } else {
                                    auth = expectAuth.equals(rawCommand.substring(4).trim());
                                    response = auth ? "auth-success" : "err auth challenge response did not match";
                                }
                                break;
                            case "ping":
                                response = "pong" + rawCommand.substring(4);
                                break;
                            case "image":
                                checkAuth("image", auth);
                                response = image(args(rawCommand.substring(5)));
                                break;
                            case "lcd":
                                checkAuth("lcd", auth);
                                response = lcd(args(rawCommand.substring(3)));
                                break;
                            case "scan":
                                checkAuth("scan", auth);
                                response = scan();
                                break;
                            case "test":
                                checkAuth("test", auth);
                                lcd(Arrays.asList("reset", "hard"));
                                lcd(Arrays.asList("wake", "0xff"));
                                lcd(Arrays.asList("brightness", "1000"));
                                image(Arrays.asList("show", "@color-bars", "0xff"));
                                break;
                            case "exit":
                            case "quit":
                                done = true;
                                response = "goodbye";
                                break;
                            default:
                                response = String.format("err unknown command %s", command[0]);
                                break;
                        }
                    } catch (Exception e) {
                        response = "err " + e.getMessage();
                        expectAuth = null;
                    }
                    out.println(response);
                    out.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                running = false;
            }
        }
    }

    protected synchronized Supplier<Socket> getSocketSupplier() throws IOException {
        if (serverSocket == null) {
            serverSocket = new ServerSocket(65432, 1);
            BlockingQueue<Socket> q = new ArrayBlockingQueue<>(1);
            Thread t = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket socket = serverSocket.accept();
                        if (running) {
                            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
                            out.println("console busy, goodbye");
                            out.flush();
                            socket.close();
                        } else {
                            q.offer(socket);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                serverSocket = null;
            }, "Socket Listener");
            t.setDaemon(true);
            t.start();
            socketSupplier = () -> {
                while (true) {
                    try {
                        return q.take();
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
            };
        }
        return socketSupplier;
    }

    protected abstract Con getCon();

    private void checkAuth(String command, boolean auth) {
        if (!auth) {
            throw new IllegalStateException("command \"" + command + "\" requires authentication");
        }
    }

    private List<String> args(String s) {
        s = s.trim();
        return s.length() > 0 ? Arrays.asList(s.split(" ")) : Collections.emptyList();
    }

    protected void sleepUntil(long until) {
        for (long now = currentTimeMillis(); now < until; now = currentTimeMillis()) {
            try {
                Thread.sleep(until - now);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    protected String lcd(List<String> args) throws IOException {
        if (args.size() < 1) {
            throw new IllegalArgumentException("missing sub-command for \"lcd\"");
        }
        Pattern pattern;
        String argName;
        boolean parseSelector = true;
        switch (args.get(0)) {
            case "reset":
                pattern = MATCH_LCD_RESET_SELECTOR;
                argName = "selector";
                break;
            case "brightness":
                pattern = MATCH_LCD_BRIGHTNESS;
                argName = "brightness";
                parseSelector = false;
                break;
            case "sleep":
                pattern = MATCH_LCD_SELECTOR;
                argName = "selector";
                break;
            case "wake":
                pattern = MATCH_LCD_SELECTOR;
                argName = "selector";
                break;
            default:
                throw new IllegalArgumentException("unrecognized sub-command \"" + args.get(0) + "\"");
        }
        if (args.size() != 2) {
            throw new IllegalArgumentException("bad args for \"lcd " + args.get(0) + "\"");
        }
        Matcher matcher = pattern.matcher(args.get(1));
        if (!matcher.find()) {
            throw new IllegalArgumentException("bad " + argName + " for \"lcd " + args.get(0) + "\": " + args.get(1));
        }
        String selector = null;
        byte selectorByte = 0;
        if (parseSelector) {
            selector = args.get(1);
            if ("hard".equals(selector)) {
                selectorByte = (byte)0xFF;
            } else if ("0x00".equals(selector)) {
                return "ok";
            } else {
                selectorByte = (byte)Integer.parseInt(selector.substring(2), 16);
            }
        }
        switch (args.get(0)) {
            case "reset":
                if ("hard".equals(selector)) {
                    con.hardReset();
                } else {
                    con.softReset(selectorByte);
                }
                break;
            case "sleep":
                con.sleep(selectorByte);
                break;
            case "wake":
                con.wake(selectorByte);
                break;
            case "brightness":
                con.brightness(Integer.parseInt(matcher.group(1)));
                break;
        }
        return "ok";
    }

    protected String image(List<String> args) throws IOException {
        if (args.size() < 1) {
            throw new IllegalArgumentException("missing sub-command for \"image\"");
        }
        Pattern[] patterns;
        String[] argNames;
        switch (args.get(0)) {
            case "store":
                patterns = MATCH_IMAGE_STORE_ARGS;
                argNames = IMAGE_STORE_ARG_NAMES;
                break;
            case "show":
                patterns = MATCH_IMAGE_SHOW_ARGS;
                argNames = IMAGE_SHOW_ARG_NAMES;
                break;
            case "clear":
                patterns = MATCH_IMAGE_CLEAR_ARGS;
                argNames = IMAGE_CLEAR_ARG_NAMES;
                break;
            default:
                throw new IllegalArgumentException("unrecognized sub-command \"" + args.get(0) + "\"");
        }
        if (args.size() != patterns.length + 1) {
            throw new IllegalArgumentException("bad args for \"image " + args.get(0) + "\"");
        }
        for (int i = 0, l = patterns.length; i < l; i++) {
            Matcher matcher = patterns[i].matcher(args.get(i + 1));
            if (!matcher.find()) {
                throw new IllegalArgumentException("bad " + argNames[i] + " for \"image " + args.get(0) + "\": " + args.get(i + 1));
            }
        }
        String imageName = args.get(1);
        byte[] data;
        switch (args.get(0)) {
            case "store":
                data = Base64.getDecoder().decode(args.get(2));
                imageDataCache.put(imageName, data);
                break;
            case "show":
                switch (imageName) {
                    case "@color-bars":
                        data = getColorBars();
                        break;
                    case "@checkerboard":
                        data = getCheckerboard();
                        break;
                    case "@aspect-ratio-grid":
                        data = getAspectRatioGrid();
                        break;
                    default:
                        data = imageDataCache.get(imageName);
                        break;
                }
                if (data == null) {
                    throw new IllegalStateException("no stored image named \"" + imageName + "\" was found");
                }
                String selector = args.get(2);
                if ("0x00".equals(selector)) {
                    return "ok";
                }
                byte selectorByte = (byte)Integer.parseInt(selector.substring(2), 16);
                con.updateImage(selectorByte, data);
                break;
            case "clear":
                if ("*".equals(imageName)) {
                    imageDataCache.invalidateAll();
                } else {
                    imageDataCache.invalidate(imageName);
                }
                break;
        }
        return "ok";
    }

    public String scan() {
        StringBuilder builder = new StringBuilder(18);
        builder.append("down ");
        con.readButtons((b) -> builder.append(b ? 'x' : '-'));
        return builder.toString();
    }

    private static byte[] loadSecret() {
        File file = new File(System.getenv("HOME"), ".consec");
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            return in.readLine().getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected static byte[] getColorBars() throws IOException {
        @SuppressWarnings("SpellCheckingInspection")
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAKAAAACACAMAAAC7vZIpAAABO1BMVEUCAgIKAAIE" +
                                                "BAQFBQUGBgYRCB0PChAQDAsPCxkPCxwRDBAKDwkKDhcPDgkLDhUPDgoODg4LDw4R" +
                                                "DgcODhAKDxINDw4NEAcKEBAPDw8IEgoMEQ1JAQIfDhRPAgoSExcMFhUUFBQTFRQM" +
                                                "GAwhEwADA/8DBP4WFhYBBf4AHyUbGwBoAR8AIx5GBX0fIAAAJy0WG6pfAORgAOdl" +
                                                "AOFhAedkAeVnAPlQE4oAOgALKaEKK5ILLpYOLpH/AQnxBgL9AwQuQkv4Af//AP//" +
                                                "Af/8A/35BP/6BP32BvvzB//2B/jkFNzaGNnfFuB3SLyAQ9+AROJdo/hdpflTxUut" +
                                                "qvsD/AAB+vwF+v0C+/8C/PoC/PsA/fsA//QQ/9ns6u///A39/gP9/wT/6+r/6+3/" +
                                                "8Pz49/X89vj/9vb/+fn7+v/7/fph/+z1AAAAAWJLR0QAiAUdSAAAAAlwSFlzAAAL" +
                                                "EwAACxMBAJqcGAAAAAd0SU1FB+AMHQgbAW0xedUAAAAmaVRYdENvbW1lbnQAAAAA" +
                                                "AENyZWF0ZWQgd2l0aCBHSU1QIG9uIGEgTWFjleRfWwAAAUZJREFUeNrt0tVOA0EA" +
                                                "heHBHZbF3aW4w+LuDsVbHPr+T0CyW7INnAmEpFz9//XJzJfJmEdd7F4Vvz5X3Z7J" +
                                                "TtZUGwuLqvkunQEIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQI" +
                                                "ECBAgAABAgQIECBAgAABAgQIECDA/wQO6wYjqo6DQ9X+tGzpWHXU16RqONWZHl1l" +
                                                "taplRT7Kbo2sN6q6Ka9TFd7pjOVlSx1V86oE7six0y//w2W+HFdZ/hpAgAABphvY" +
                                                "qStxXbfi2ynty+t+W5vbqe0Vh5WF66EL1VWOBj7ozJxuxvO855evPb0m/AbGJlKb" +
                                                "HAkrCO9szFPlZmQHJVe1rX5t9Tozay/xZmtqdNxWkfNTmck+gd1BljVAgAABAgQI" +
                                                "ECBAgAABAgQIECBAgH8CvqcHaAJf1u+AH2W5R6gIl+R/AAAAAElFTkSuQmCC");
        return pngImageToRgbBytes(png);
    }

    protected static byte[] getCheckerboard() throws IOException {
        @SuppressWarnings("SpellCheckingInspection")
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAKAAAACACAQAAAAmaqqQAAAAAmJLR0QA/4ePzL8A" +
                                                "AAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQfgDB4FLyceezNiAAAAJmlUWHRD" +
                                                "b21tZW50AAAAAABDcmVhdGVkIHdpdGggR0lNUCBvbiBhIE1hY5XkX1sAAALeSURB" +
                                                "VHja7Z2xbtRAFEXvjCeJEBLdgvgfaEIBEjV/RwUFVajo+Q4qlvwASmR7qBCd75U8" +
                                                "WmXDuW2ejsdnnRnpjTUu6rIJSnTUS40hlaDmWjeDSHtHVEV2BYEIRCACEUgQiEAE" +
                                                "IpAgEIEIRCBBIAIR+P+kfDIt2aKuzwHoqa5tzZU+mopZT/TO/uZFP/Td3Zh+64Op" +
                                                "6Vr0xdZMer91ndX2tLuafM0L/TQ1i6RLy5FWU3Ovpq96a0lFi63purA/xLo5oub3" +
                                                "DYrdN5i0qNodiEk92IHwnCt1TUN2V+aAc6m7TU4bMQ8sSjZnSrTJsw4Q8+9p3s5F" +
                                                "QLljEWEVRiACCQIRiEAEEgQiEIEIJAhEIAIfT9pkClZNtrNbVHSUJynoEU+WU1T1" +
                                                "ypKkakld3XJWzZuc5huYS/iYelKxpKSlX7WoR2NypCm6t8vtlv4pH/eka51pGTPz" +
                                                "LEFN3fl3gkAEIhCBBIEIRCACCQIRiEAEEgQi8AHFNlRLYLlLei5PmgNSiRqzNwFp" +
                                                "CkbUd7d426izVZJmaPJmc4+aqiUgrRrx/nfTfJpzY+oQxSPTh1Bm5kAWEQQikCAQ" +
                                                "gQhEIEEgAhGIQIJABCLw8WRYQ/V20LHXvqFa1PU6IC2DWrzbfUwrsKtGF/H93x4I" +
                                                "7MGpHVJVs6TkHJGuJRjR9k/axrTG0+e0BH/PnlJHaicaEXMgiwgCEYhAgkAEIhCB" +
                                                "BIEIRCACCQIReEZpR1vS7Ucfi1YddLRVJSDNlrOq6VtAutcxEJB80HKL0w52IF23" +
                                                "geQqT1JEOtiKSc8saZUCUg1GtG5ymv8fLtHZzhpyEEnKqcNIew9rGTYH9pPOPP3B" +
                                                "XItFhFUYgQhEIEEgAhGIQIJABCIQgQSBCDyjDPmyoST9GvKSeQ8/3/cmICl62/oE" +
                                                "L5n7DzoWe5m/t5SQMs4Y0rqb00a8iK3olnKSTkjay2EOZBFBIAIRSBCIQAQikCAQ" +
                                                "gQhEIEEgAs8ofwCxCbD+Nzru2QAAAABJRU5ErkJggg==");
        return pngImageToRgbBytes(png);
    }

    protected static byte[] getAspectRatioGrid() throws IOException {
        @SuppressWarnings("SpellCheckingInspection")
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAKAAAACABAMAAAB+TX8oAAAAMFBMVEUAAAAXFxcl" +
                                                "JSUzMzNCQkJUVFRfX19xcXF/f3+MjIydnZ2srKy9vb3Ly8vb29vn5+frgVTdAAAA" +
                                                "AWJLR0QAiAUdSAAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB+EBCAQEDm3F" +
                                                "IwcAAAAmaVRYdENvbW1lbnQAAAAAAENyZWF0ZWQgd2l0aCBHSU1QIG9uIGEgTWFj" +
                                                "leRfWwAAB4hJREFUaN7tmltvE0cUx2d9tynSLpfY5qGyDSROnxwcJQ68OI4aSHjo" +
                                                "BhAUepHdQMKlqHagOKVK5VApdVRQzaUEhyJAlJKgqq1UiRapRUgVLfBUVWrV9Au0" +
                                                "fIM+dmZ29jLj9e6Om1ZC7Ug58Vl7fvbOnJn97zkL6vUri3XYniBzD5krd2n/I93/" +
                                                "DPuPkF3C/izy8cvfiA8A8IrQgCIyHch4Ypofxn4K2bzmu7OMLyM7gowfGeDFgAoy" +
                                                "vdhPacAkBmY1P4J9mfExu6D5Tw2w+DeBMgUU2mcfzsRbBwobq49munSgMNclAilz" +
                                                "tmVgNQf7d9xQgfFD+D+Iyi0Ch/CgAf+0AvT0iQoQRHrM4tBtF4d7Y6R/eCs0Y2Mv" +
                                                "1YDavtk9Nja2bwaasQfInMH+RfTye2SmsP8JNAd0f+99rT/0gSQlNR8E4pIktfVD" +
                                                "s2oaGqkPmbUjyJZ0v4DshOZH9f6+EUmNQNKK5mO4KrH+RiIhmY9hwdC/hKii4UCH" +
                                                "CdB3u348vSmRTh9cvGwC9MqG/hG1k9r8Igt0bVvMam/3LuQagEFjfx/8bM14QMgz" +
                                                "wMDNOPX+hrrIAPuN78PxE4rMAQrYeR4wTah303FJ9+8HrhR1YIICvt4FGprwXI0C" +
                                                "Vqh3N9BzgmdFBw7LwKxtHjEAPfRnIiBAA0MG4FAKmLekrAP99IeC9CRBoCB5u1C4" +
                                                "VsRwETRrL8ZEHOidghQUGeAUeUWOr6xWT39dhe2PqemmPCC8MfkEfejqZPUDuv8K" +
                                                "EADsKSubQ34nsGhCXiYXJT/7CwPsATKGt0QrIGj7goxhIMb099IHegnQXQPW7TgB" +
                                                "uvPMLDNxWCLA/TY84Co2iUPzleLN2gFBu+lKgSF6lhpsWQFO2PLwL0HAHHW0xuwW" +
                                                "AWW38cXsgWj/jyhxYdhtUmqUkPa8sh+OOuChnxhRB9PwJdSoCsqO7ck6AcJRjGgy" +
                                                "SZ/TdHq77q8YSKfTmdEDjnjAfWw7/Hh6ix6xoZ1pUC4f0aZF+HS8DP25a86AoHoG" +
                                                "ff7QT9qBd8bL6BST6hnuwRd6T6dDHgjiU3aPqbHdqUqRSWVeknlFOZxzCgSXiHJQ" +
                                                "Fke4ommbfRcAWDPfT7RN3jFwmGibvpsicJ8sGORcuL50USTqK+iYp+wsSM75Zh/P" +
                                                "xZoJzpJzIO5lp2CFPAcw5wAYFDmAIQfAQQ4e3g0bgB76PqXCA0TdmPsUqA9PIKn3" +
                                                "MzKnod6TuYBboT68g/TiV0QvQn3Y1qXpwW1Q7YlcwJAkrSlperFTQvrQR41hlIuH" +
                                                "5JbNpJT4gLCfDbDICSzYAIUsJ3CTDdDR1cTYojbAECcPdrAGJnmBHrkBSIXNKC8Q" +
                                                "VBigJHnjWA8ikxFq3MBpD7oREl9D/SOCBKrVMwtQ6c3+ifTevdlr3MC33v8VdV0i" +
                                                "elHbHPDvbgMyNzDrkrUb8MYkBncYwgu75Sy7UtzAdksgd1zDyLYEBkVuYMgSyL1Q" +
                                                "nlKgqAE7WgAGVtNAqAd3QpHX/SWSeoe38AP94+dR1wvIQL2I9OF7UOSVf0HmyttN" +
                                                "+zWdfv/8D7DniYeo/5SiDw3X5VZOeb38f9j8o0tv2TcH93JvXy1ssN3W+pD/EtBv" +
                                                "rQ/5r3pla33IDzxlrQ8nuIEVaynSyy1F8sssloKiNTDAu1RCyy040/+2JOa87zEV" +
                                                "7fRtRQev3gTW+lAKcs6JIFnqQ/iCbzVvrlat9WELN4+W+hD+7eLhocyR3f1yiC+s" +
                                                "HaQIeAZxwAGQaxCbJjE8B38fBfxpFn+MAF3jS8eNwMytTa7ehZQC9Dk/580kEdSx" +
                                                "MCD01Ps14DBO+QtDKSWz5PxehaSq1uELitCXJ8B2NVPzZgdfMs2fwZmRXeqo7yH6" +
                                                "8LqW+b2LRF5mv9MdZzfOH/Z8p0XlZawPj+p76rNlnD887zCq53D+8Ki+ha4sl+lc" +
                                                "F05TeWIOBUlYmYQcvRIBpT5GlaRuyeGyQ0DBGLdr2bALKmlnv5MrS9Ak7YwqA9SV" +
                                                "2JVVEuMlZ/sCAnYziXGhwq4lBHQwihFRARaYS4wrawa0VxCCeXFhPVv+GCRAr936" +
                                                "6yGbgavIzDxzZY+qBZoH1jzvjwTYUORqrJopxYGC9bxMy8rvUTYcI/Bl+k7pGa2+" +
                                                "/GrBgveCVl9+he4fYtN7Ib2+3NlclXR2afXldaLtKWuFwvFm4d1WaVooDLGLImkA" +
                                                "CifNozF6CuhAJhyigLmRGKWKrXUzYvg6AE2LrWvZPHiFLgcPN26NRw7Q9eUimym3" +
                                                "LlgPv0sPyaqTMlOwHqHDCdCqGq0buqS++vBNg9aaPyayJfUIrbiZ4mYUNBb9g4/n" +
                                                "com4lEgM1r8lCs4I9KXoLY2uFFZMgJ6UkEgP3hlIx0XTxxKM/XdAfSgakv8+JBVJ" +
                                                "fRmZpACNBwU6yAtKPRn66EEKrb4MRaHxjLE+1L/iKhJ5pL6MzOesP4n8h+ilWl+G" +
                                                "+vC+viIVfbhHHcW23fhrYppetHj4RNeDrkm1v3dc+SfNkBOexhd67sdjCgfJCZ8L" +
                                                "kFDxXojDydn4casP8Lg/RP3XzOliyTU0v3gp1/ojRq5M/falHSKlD2uaPmztIahi" +
                                                "c8H5X3iQbHkfxlvuxwX/AnYpmtz4OdX5AAAAAElFTkSuQmCC");
        return pngImageToRgbBytes(png);
    }

    private static byte[] pngImageToRgbBytes(byte[] png) throws IOException {
        BufferedImage bufferedImage;
        bufferedImage = ImageIO.read(new ByteArrayInputStream(png));

        int i = 0;
        byte[] buf = new byte[61440];
        for (int argb : bufferedImage.getRGB(0, 0, 160, 128, null, 0, 160)) {
            buf[i++] = (byte)((argb >> 16) & 0xFF);
            buf[i++] = (byte)((argb >> 8) & 0xFF);
            buf[i++] = (byte)(argb & 0xFF);
        }
        return buf;
    }

    public int getIdleTimeout() {
        try {
            return Integer.parseInt(System.getProperty("con.network.idle.timeout", "2500"));
        } catch (NumberFormatException e) {
            return 2500;
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            Class.forName("com.pi4j.io.gpio.GpioController");
            new ConPi().run();
        } catch (ClassNotFoundException e) {
            new ConSim(new SimulatedButtons(2.0f)).run();
        }
    }
}
