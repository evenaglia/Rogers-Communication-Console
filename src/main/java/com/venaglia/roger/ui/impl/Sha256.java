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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by ed on 1/4/17.
 */
public class Sha256 {

    private static ThreadLocal<MessageDigest> SHA256 = new ThreadLocal<MessageDigest>() {
        @Override
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    };

    public static String digest(byte[] data) {
        MessageDigest digest = SHA256.get();
        digest.reset();
        byte[] rawResult = digest.digest(data);
        char[] result = new char[rawResult.length * 2];
        for (int i = 0, j = 0, l = rawResult.length; i < l; i++) {
            byte b = rawResult[i];
            result[j++] = "0123456789abcdef".charAt((b >> 4) & 0xF);
            result[j++] = "0123456789abcdef".charAt(b & 0xF);
        }
        return new String(result);
    };

    public static String hmac(byte[] secret, String source) {
        // use the existing sha256 thread local and avoid instantiating a secret key spec
        MessageDigest digest = SHA256.get();
        digest.reset();
        int blockSize = 64; // 512 bits for sha-256
        if (secret.length < blockSize) {
            byte[] buffer = new byte[blockSize];
            System.arraycopy(secret, 0, buffer, 0, secret.length);
            secret = buffer;
        } else if (secret.length > blockSize) {
            secret = digest.digest(secret);
        } else {
            secret = secret.clone(); // because we modify the contents
        }

        for (int i = 0; i < blockSize; i++) {
            secret[i] ^= 0x36; // apply i-pad
        }
        digest.update(secret);
        digest.update(source.getBytes(UTF_8));
        byte[] inner = digest.digest();
        for (int i = 0; i < blockSize; i++) {
            secret[i] ^= 0x36 ^ 0x5c; // revert i-pad, apply o-pad
        }
        digest.update(secret);
        digest.update(inner);
        byte[] rawResult = digest.digest();
        char[] result = new char[rawResult.length * 2];
        for (int i = 0, j = 0, l = rawResult.length; i < l; i++) {
            byte b = rawResult[i];
            result[j++] = "0123456789abcdef".charAt((b >> 4) & 0xF);
            result[j++] = "0123456789abcdef".charAt(b & 0xF);
        }
        return new String(result);
    }
}
