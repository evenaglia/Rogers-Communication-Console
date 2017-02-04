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

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.venaglia.roger.console.server.impl.ButtonDownSupplier;
import com.venaglia.roger.console.server.impl.DelegatedCommand;
import com.venaglia.roger.console.server.impl.TestImage;
import com.venaglia.roger.console.server.pi.ConPi;
import com.venaglia.roger.console.server.sim.ConSim;
import com.venaglia.roger.console.server.sim.SimulatedButtons;
import com.venaglia.roger.ui.Command;
import com.venaglia.roger.ui.ImageSerializer;
import com.venaglia.roger.ui.impl.ConClient;
import com.venaglia.roger.ui.impl.ImageSerializer444;
import com.venaglia.roger.ui.impl.ImageSerializer565;
import com.venaglia.roger.ui.impl.ImageSerializer888;
import com.venaglia.roger.ui.impl.Sha256;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 1/5/17.
 */
public abstract class ConServer implements Runnable {

    private static final Pattern[] MATCH_LCD_SELECTOR = { Pattern.compile("^(0x[0-9a-f][0-9a-f])$") };
    private static final Pattern[] MATCH_LCD_RESET_SELECTOR = { Pattern.compile("^(0x[0-9a-f][0-9a-f]|hard)$") };
    private static final Pattern[] MATCH_LCD_BRIGHTNESS = { Pattern.compile("^(0|[1-9][0-9]{0,2}|1000)$") };
    private static final Pattern MATCH_LCD_COMMAND_DATA = Pattern.compile("^(0x[0-9a-f][0-9a-f]|0|[1-9][0-9]?|2[0-4][0-9]|25[0-5])$");
    private static final Pattern MATCH_IMAGE_NAME = Pattern.compile("^(\\w+)$");
    private static final Pattern MATCH_IMAGE_NAME_OR_BUILT_IN = Pattern.compile("^(@black|@white|@color-bars|@checkerboard|@aspect-ratio-grid|@skin-tones|@number-[0-7]|\\w+)$");
    private static final Pattern MATCH_IMAGE_NAME_OR_ALL = Pattern.compile("^(\\*|\\w+)$");
    private static final Pattern[] MATCH_IMAGE_STORE_ARGS = {
            MATCH_IMAGE_NAME,
            Pattern.compile("^([0-9ZA-Za-z/+]+)$")
    };
    private static final Pattern[] MATCH_IMAGE_SHOW_ARGS = {
            MATCH_IMAGE_NAME_OR_BUILT_IN,
            MATCH_LCD_SELECTOR[0]
    };
    private static final Pattern[] MATCH_IMAGE_CLEAR_ARGS = {
            MATCH_IMAGE_NAME_OR_ALL
    };
    private static final Pattern MATCH_OK = Pattern.compile("^ok.*$");
    private static final Pattern MATCH_DOWN = Pattern.compile("^down [x-]*$");
    private static final String[] LCD_SELECTOR_ARG_NAMES = { "selector" };
    private static final String[] IMAGE_STORE_ARG_NAMES = { "image name", "image data" };
    private static final String[] IMAGE_SHOW_ARG_NAMES = { "image name", "selector" };
    private static final String[] IMAGE_CLEAR_ARG_NAMES = { "image name" };

    protected static final int PWM_RANGE = 1000;
//    protected static final ImageSerializer IMAGE_SERIALIZER = new ImageSerializer888();
//    protected static final ImageSerializer IMAGE_SERIALIZER = new ImageSerializer565();
    protected static final ImageSerializer IMAGE_SERIALIZER = new ImageSerializer444();

    private final byte[] secret;
    private final Cache<String,byte[]> imageDataCache;
    private final ConClient delegate;
    private final ButtonDownSupplier buttonDownSupplier;

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
        } else if (secret.length < 4) {
            System.err.println("The provided secret is too short. It will not work!");
        }
        SocketAddress delegateAddress = getDelegate();
        if (delegateAddress != null) {
            BlockingQueue<Command> delegateQueue = new ArrayBlockingQueue<>(4, false);
            delegate = new ConClient(delegateAddress, secret, delegateQueue, delegateQueue::offer);
        } else {
            delegate = null;
        }
        buttonDownSupplier = new ButtonDownSupplier(12);
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
                                    response = "err must send \"hello\" command immediately before sending \"auth\" command";
                                } else if (secret == null) {
                                    response = "auth-success";
                                } else {
                                    auth = expectAuth.equals(rawCommand.substring(4).trim());
                                    response = auth ? "auth-success" : "err auth challenge response did not match";
                                }
                                break;
                            case "help":
                                if (secret == null) {
                                    socket.setSoTimeout(300000);
                                }
                                response = help(args(rawCommand.substring(4)), auth);
                                break;
                            case "ping":
                                if (delegate != null) {
                                    response = delegate.sendCommand(new DelegatedCommand(command[0], args(rawCommand.substring(4)), Pattern.compile("^pong.*$"))).get();
                                } else {
                                    response = "pong" + rawCommand.substring(4);
                                }
                                break;
                            case "image":
                                checkAuth("image", auth);
                                if (delegate != null) {
                                    List<String> argList = args(rawCommand.substring(5));
                                    response = delegate.sendCommand(new DelegatedCommand(command[0], argList, MATCH_OK)).get();
                                    image(argList);
                                } else {
                                    response = image(args(rawCommand.substring(5)));
                                }
                                break;
                            case "lcd":
                                checkAuth("lcd", auth);
                                if (delegate != null) {
                                    List<String> argList = args(rawCommand.substring(3));
                                    response = delegate.sendCommand(new DelegatedCommand(command[0], argList, MATCH_OK)).get();
                                    lcd(argList);
                                } else {
                                    response = lcd(args(rawCommand.substring(3)));
                                }
                                break;
                            case "scan":
                                checkAuth("scan", auth);
                                if (delegate != null) {
                                    response = scan(delegate.sendCommand(new DelegatedCommand(command[0], Collections.emptyList(), MATCH_DOWN)));
                                } else {
                                    response = scan(null);
                                }
                                break;
                            case "test":
                                if (secret == null) {
                                    socket.setSoTimeout(300000);
                                }
                                checkAuth("test", auth);
                                Future<String> future = null;
                                if (delegate != null) {
                                    future = delegate.sendCommand(new DelegatedCommand(command[0], Collections.emptyList(), MATCH_OK));
                                }
                                lcd(Arrays.asList("reset", "hard"));
                                lcd(Arrays.asList("wake", "0xff"));
                                lcd(Arrays.asList("brightness", "750"));
                                image(Arrays.asList("show", "@white", "0xff"));
                                Thread.sleep(250);
                                image(Arrays.asList("show", "@color-bars", "0xff"));
                                Thread.sleep(2500);
                                image(Arrays.asList("show", "@checkerboard", "0xff"));
                                Thread.sleep(2500);
                                image(Arrays.asList("show", "@aspect-ratio-grid", "0xff"));
                                Thread.sleep(2500);
                                lcd(Arrays.asList("brightness", "0"));
                                image(Arrays.asList("show", "@skin-tones", "0xff"));
                                for (int i = 0; i <= 1000; i += 25) {
                                    lcd(Arrays.asList("brightness", String.valueOf(i)));
                                    Thread.sleep(100);
                                }
                                for (int i = 1000; i >= 0; i -= 25) {
                                    lcd(Arrays.asList("brightness", String.valueOf(i)));
                                    Thread.sleep(100);
                                }
                                Thread.sleep(500);
                                image(Arrays.asList("show", "@black", "0xff"));
                                lcd(Arrays.asList("brightness", "750"));
                                for (int i = 0; i < 8; i++) {
                                    image(Arrays.asList("show", "@number-" + i, String.format("0x%02x", 1 << i)));
                                }
                                if (future != null) {
                                    response = future.get();
                                }
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
                        if (e instanceof ExecutionException && e.getCause() instanceof Exception) {
                            e = (Exception)e.getCause();
                        }
                        String message = e.getMessage();
                        response = message == null ? "err" : "err " + message;
                        expectAuth = null;
                        e.printStackTrace();
                    } finally {
                        if (!"hello".equals(command[0])) {
                            expectAuth = null;
                        }
                    }
                    out.println(response);
                    out.flush();
                }
            } catch (SocketTimeoutException e) {
                // don't care
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                running = false;
            }
        }
    }

    private Iterable<Boolean> parseButtons(String down) {
        for (int i = 0, j = 5, l = Math.min(down.length() - 5, buttonDownSupplier.size()); i < l; i++, j++) {
            buttonDownSupplier.set(i, Character.isLetterOrDigit(down.charAt(j)));
        }
        return buttonDownSupplier;
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

    protected String help(List<String> args, boolean auth) {
        StringBuilder buffer = new StringBuilder();
        if (args.size() == 0) {
            buffer.append("\tAvailable commands:\n");
            buffer.append("\t\n");
            if (auth) {
                buffer.append("\tauth, exit, hello, help, image, lcd, ping, quit, scan, test\n");
            } else {
                buffer.append("\tauth, exit, hello, help, ping, quit\n");
            }
            buffer.append("\tEnter 'help [command]' for help on a specific command\n");
            buffer.append("ok");
        } else switch (args.get(0)) {
            case "auth":
                buffer.append("\tUsage: auth [token]\n");
                buffer.append("\t\n");
                buffer.append("\tAuthenticate to this system using the provided token. The token is a \n");
                buffer.append("\thex encoded HMAC-SHA-256 of the challenge token returned by 'hello'\n");
                buffer.append("\tusing the shared secret key. This command must be issued immediately\n");
                buffer.append("\tfollowing the 'hello' command.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tauth-success\n");
                buffer.append("\terr [message]\n");
                buffer.append("ok");
                break;
            case "exit":
                buffer.append("\tUsage exit\n");
                buffer.append("\t\n");
                buffer.append("\tTerminate the connection to this system, the socket will be closed.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tgoodbye\n");
                buffer.append("ok");
                break;
            case "hello":
                buffer.append("\tUsage: hello\n");
                buffer.append("\t\n");
                buffer.append("\tIssues a request to authenticate to this system.\n");
                buffer.append("\tThis command must be followed immediately with the 'auth' command.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tauth-challenge [challenge-token]\n");
                buffer.append("ok");
                break;
            case "help":
                buffer.append("\tUsage: help [command]\n");
                buffer.append("\t\n");
                buffer.append("\tDisplays help on commands.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tok -- followed by multi-line plain text documentation.\n");
                buffer.append("\terr [message]\n");
                buffer.append("ok");
                break;
            case "image":
                buffer.append("\tUsage: image store [name] [base64-data]\n");
                buffer.append("\t       image show [name] [selector]\n");
                buffer.append("\t       image clear [name]\n");
                buffer.append("\t\n");
                buffer.append("\tManipulates images that can be displayed. The [name] can only\n");
                buffer.append("\tcontain letters or numbers. The base-64 data must contain a\n");
                buffer.append("\trenderable uncompressed image in 12-bit, 16-bit or 24-bit packed\n");
                buffer.append("\tform. The [selector] is an 8-bit hexadecimal selector indicating which\n");
                buffer.append("\tdisplay or displays the image will be sent. The [name] may also be\n");
                buffer.append("\tone of the built in test images:\n");
                buffer.append("\t    @aspect-ratio-grid, @black, @checkerboard, @color-bars\n");
                buffer.append("\t    @number-[0-7], @skin-tones or @white\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tok\n");
                buffer.append("\terr [message]\n");
                buffer.append("ok");
                break;
            case "lcd":
                buffer.append("\tUsage: lcd reset [selector]\n");
                buffer.append("\t       lcd reset hard\n");
                buffer.append("\t       lcd brightness [value]\n");
                buffer.append("\t       lcd sleep [selector]\n");
                buffer.append("\t       lcd wake [selector]\n");
                buffer.append("\t       lcd cmd [selector] [command] [data...]\n");
                buffer.append("\t\n");
                buffer.append("\tManipulates the LCD displays. 'reset 0x##' will issue a soft reset\n");
                buffer.append("\tcommand to the selected displays, while 'reset hard' will issue a\n");
                buffer.append("\thard reset to all connected displays. [value] is a decimal value\n");
                buffer.append("\tfrom 0 to 1000 and allows fine adjustment of the brightness of the\n");
                buffer.append("\tLCD backlights. 'lcd sleep' will disable the selected displays and\n");
                buffer.append("\tput them in a low-power state while 'lcd wake' will re-enable them.\n");
                buffer.append("\t'lcd cmd' allows arbitrary commands to be issued directly to the\n");
                buffer.append("\tdisplays. The [command] and [data] arguments are hexadecimal bytes.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tok\n");
                buffer.append("\terr [message]\n");
                buffer.append("ok");
                break;
            case "ping":
                buffer.append("\tUsage: ping [text-to-echo]\n");
                buffer.append("\t\n");
                buffer.append("\tVerifies connectivity.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tpong [text-to-echo]\n");
                buffer.append("ok");
                break;
            case "quit":
                buffer.append("\tUsage quit\n");
                buffer.append("\t\n");
                buffer.append("\tTerminate the connection to this system, the socket will be closed.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tgoodbye\n");
                buffer.append("ok");
                break;
            case "scan":
                buffer.append("\tUsage: scan\n");
                buffer.append("\t\n");
                buffer.append("\tScans the input buttons and returns an array of flags, '-' or 'x',\n");
                buffer.append("\twhere 'x' indicates that a particular button is down.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tdown [flags]\n");
                buffer.append("ok");
                break;
            case "test":
                buffer.append("\tUsage: test\n");
                buffer.append("\t\n");
                buffer.append("\tPerforms a simple test of the console. This series starts with a\n");
                buffer.append("\thard reset then generates a series of diagnostic images and\n");
                buffer.append("\tbrightness levels. These tests take approximately 25 seconds to\n");
                buffer.append("\tcomplete.\n");
                buffer.append("\t\n");
                buffer.append("\tResponses:\n");
                buffer.append("\t\n");
                buffer.append("\tok\n");
                buffer.append("ok");
                break;
            default:
                buffer.append("err Unrecognized command '").append(args.get(0)).append("'");
                break;
        }
        return buffer.toString();
    }

    protected String lcd(List<String> args) throws IOException {
        if (args.size() < 1) {
            throw new IllegalArgumentException("missing sub-command for \"lcd\"");
        }
        Pattern[] patterns;
        String[] argNames;
        boolean parseSelector = true;
        switch (args.get(0)) {
            case "reset":
                patterns = MATCH_LCD_RESET_SELECTOR;
                argNames = LCD_SELECTOR_ARG_NAMES;
                break;
            case "brightness":
                patterns = MATCH_LCD_BRIGHTNESS;
                argNames = new String[]{ "brightness" };
                parseSelector = false;
                break;
            case "sleep":
                patterns = MATCH_LCD_SELECTOR;
                argNames = LCD_SELECTOR_ARG_NAMES;
                break;
            case "wake":
                patterns = MATCH_LCD_SELECTOR;
                argNames = LCD_SELECTOR_ARG_NAMES;
                break;
            case "cmd":
                patterns = new Pattern[Math.max(args.size() - 1, 2)];
                Arrays.fill(patterns, MATCH_LCD_COMMAND_DATA);
                patterns[0] = MATCH_LCD_SELECTOR[0];
                argNames = new String[patterns.length];
                argNames[0] = LCD_SELECTOR_ARG_NAMES[0];
                argNames[1] = "command";
                for (int i = 2; i < argNames.length; i++) {
                    argNames[i] = "data[" + (i-2) + "]";
                }
                break;
            default:
                throw new IllegalArgumentException("unrecognized sub-command \"" + args.get(0) + "\"");
        }
        if (args.size() != patterns.length + 1) {
            throw new IllegalArgumentException("bad args for \"image " + args.get(0) + "\"");
        }
        Matcher[] matchers = new Matcher[patterns.length];
        for (int i = 0, l = patterns.length; i < l; i++) {
            Matcher matcher = patterns[i].matcher(args.get(i + 1));
            if (!matcher.find()) {
                throw new IllegalArgumentException("bad " + argNames[i] + " for \"image " + args.get(0) + "\": " + args.get(i + 1));
            }
            matchers[i] = matcher;
        }
        String selector = null;
        byte selectorByte = 0;
        if (parseSelector) {
            selector = matchers[0].group(1);
            if ("hard".equals(selector)) {
                selectorByte = (byte)0xFF;
            } else if ("0x00".equals(selector)) {
                return "ok";
            } else if (selector == null) {
                throw new IllegalArgumentException("missing selector");
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
                con.brightness(Integer.parseInt(matchers[0].group(1)));
                break;
            case "cmd":
                byte command = 0x00;
                byte[] data = new byte[matchers.length - 2];
                for (int i = 1; i < matchers.length; i++) {
                    String part = matchers[i].group();
                    byte b = part.length() > 2 && part.charAt(1) == 'x'
                        ? (byte)Integer.parseInt(part.substring(2), 16)
                        : (byte)Integer.parseInt(part, 10);
                    if (i == 1) {
                        command = b;
                    } else {
                        data[i - 2] = b;
                    }
                }
                con.sendRaw(selectorByte, command, data);
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
                    case "@aspect-ratio-grid":
                        data = IMAGE_SERIALIZER.serialize(TestImage.getAspectRatioGrid().getImage());
                        break;
                    case "@black":
                        data = IMAGE_SERIALIZER.serialize(TestImage.getSolid(Color.BLACK).getImage());
                        break;
                    case "@checkerboard":
                        data = IMAGE_SERIALIZER.serialize(TestImage.getCheckerboard().getImage());
                        break;
                    case "@color-bars":
                        data = IMAGE_SERIALIZER.serialize(TestImage.getColorBars().getImage());
                        break;
                    case "@skin-tones":
                        data = IMAGE_SERIALIZER.serialize(TestImage.getSkinTones().getImage());
                        break;
                    case "@white":
                        data = IMAGE_SERIALIZER.serialize(TestImage.getSolid(Color.WHITE).getImage());
                        break;
                    default:
                        if (imageName.matches("@number-[0-7]")) {
                            data = IMAGE_SERIALIZER.serialize(TestImage.getNumber(imageName.charAt(8) - '0').getImage());
                        } else {
                            data = imageDataCache.get(imageName);
                        }
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

    public String scan(Future<String> mergeResult) throws ExecutionException, InterruptedException {
        StringBuilder builder = new StringBuilder(18);
        builder.append("down ");
        con.readButtons((b) -> builder.append(b ? 'x' : '-'));
        if (mergeResult != null) {
            String remoteResult = mergeResult.get();
            if (remoteResult.startsWith("down") && remoteResult.length() == builder.length()) {
                for (int i = 0, l = builder.length(); i < l; i++) {
                    if (builder.charAt(i) == '-') {
                        builder.setCharAt(i, remoteResult.charAt(i));
                    }
                }
            }
        }
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

    public int getIdleTimeout() {
        try {
            return Integer.parseInt(System.getProperty("con.network.idle.timeout", "2500"));
        } catch (NumberFormatException e) {
            return 2500;
        }
    }

    public SocketAddress getDelegate() {
        try {
            String host = System.getProperty("con.network.delegate");
            if (host != null) {
                int port = 65432;
                int colon;
                if ((colon = host.indexOf(":")) < 0) {
                    port = Integer.parseInt(host.substring(colon));
                }
                return new InetSocketAddress(InetAddress.getByName(host), port);
            } else {
                return null;
            }
        } catch (NumberFormatException | UnknownHostException e) {
            e.printStackTrace();
            return null;
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
