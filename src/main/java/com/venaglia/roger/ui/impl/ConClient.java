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

package com.venaglia.roger.ui.impl;

import static com.google.common.base.Charsets.UTF_8;

import com.venaglia.roger.ui.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ed on 1/3/17.
 */
public class ConClient implements Runnable {

    private static Pattern MATCH_AUTH_CHALLENGE = Pattern.compile("auth-challenge ([0-9a-f]+)");
    private static Pattern MATCH_AUTH_RESPONSE = Pattern.compile("auth-success");

    private final SocketAddress addr;
    private final byte[] secret;
    private final BlockingQueue<Command> queue;
    private final Consumer<Command> queueIn;

    private BufferedReader in;
    private Writer out;

    public ConClient(SocketAddress addr,
                     byte[] secret,
                     BlockingQueue<Command> queue,
                     Consumer<Command> queueIn) {
        assert addr != null;
        assert secret != null && secret.length > 4;
        assert queue != null;
        assert queueIn != null;
        this.addr = addr;
        this.secret = secret;
        this.queue = queue;
        this.queueIn = queueIn;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        while (true) {
            try (Socket socket = connect()) {
                socket.setTcpNoDelay(true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                out = new OutputStreamWriter(socket.getOutputStream(), UTF_8);
                auth();
                while (true) {
                    Command command = null;
                    try {
                        command = queue.take();
                    } catch (InterruptedException e) {
                        // don't care
                    }
                    if (command != null) {
                        try {
                            command.handleResponse(deliver(command.expectedResponsePattern(),
                                                           command.getCommand(),
                                                           command.getArgs()),
                                                   queueIn);
                        } catch (IOException e) {
                            command.handleError(e, queueIn);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void auth() throws IOException {
        Matcher challenge = deliver(MATCH_AUTH_CHALLENGE, "hello");
        deliver(MATCH_AUTH_RESPONSE, "auth", Sha256.hmac(secret, challenge.group(1)));
    }

    private Matcher deliver(Pattern responsePattern, String command, String... args) throws IOException {
        String response = deliver(command, args);
        if (response.startsWith("err ")) {
            throw new IOException(response.substring(4));
        }
        Matcher matcher = responsePattern.matcher(response);
        if (matcher.find() && matcher.start(0) == 0 && matcher.end(0) == response.length()) {
            return matcher;
        }
        throw new IOException("Unmatched response: " + response);
    }

    private String deliver(String command, String... args) throws IOException {
        out.write(command);
        for (String arg : args) {
            out.write(' ');
            out.write(arg);
        }
        out.write('\n');
        out.flush();
        return in.readLine();
    }

    private Socket connect() {
        Socket socket = new Socket();
        while (true) {
            try {
                socket.connect(addr, 500);
                return socket;
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e1) {
                    // don't care
                }
            }
        }
    }

    // public API

    public void sendCommand(Command command) {
        if (!queue.offer(command)) {
            throw new RuntimeException("Queue is full, unable to update the display");
        }
    }
}
