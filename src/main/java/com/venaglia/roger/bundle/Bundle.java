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

package com.venaglia.roger.bundle;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by ed on 10/18/16.
 */
@Singleton
public class Bundle {

    private final ThreadLocal<File> VALIDATION_DIR = new ThreadLocal<>();
    private final Cache<String,Entry> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .initialCapacity(128)
            .build();

    private File bundleFile = null;

    public Optional<InputStream> get(String name) {
        File validationDir = VALIDATION_DIR.get();
        if (validationDir != null) {
            File file = new File(validationDir, name);
            try {
                return file.exists() ? Optional.of(new FileInputStream(file)) : Optional.empty();
            } catch (FileNotFoundException e) {
                throw new BundleInitializationError(e.getMessage(), e);
            }
        }
        if (cache.getIfPresent("@") == null) {
            load();
        }
        Entry entry = cache.getIfPresent(name);
        if (entry == null) {
            entry = new Entry(name, null);
        }
        Optional<InputStream> result = null;
        if (entry.data == null) {
            result = Optional.empty();
        } else {
            result = Optional.of(new ByteArrayInputStream(entry.data));
        }
        return result;
    }

    private synchronized void load() {
        Entry entry = cache.getIfPresent("@");
        if (entry == null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
            byte[] b = new byte[1024];
            cache.put("@", new Entry("@", null));
            File bundleFile = getBundleFile();
            try (FileInputStream in = new FileInputStream(bundleFile)) {
                ZipInputStream zip = new ZipInputStream(in);
                for (ZipEntry ze = zip.getNextEntry(); ze != null; ze = zip.getNextEntry()) {
                    String name = ze.getName();
                    byte[] data = buffer(buffer, b, zip);
                    if (data.length > 0) {
                        Entry e = new Entry(name, data);
                        cache.put(name, e);
                    }
                }
            } catch (IOException e) {
                throw new BundleInitializationError("An error occurred reading " + bundleFile + ": " + e.getMessage(), e);
            }
        }
    }

    private boolean deployBundle(File parentDir, String id, File[] contents) {
        return useBundle(makeBundle(parentDir, id, contents));
    }

    private File makeBundle(File parentDir, String id, File[] contents) {
        assert id != null;
        assert id.matches("[0-9a-f]{6,12}");
        String newFileName = String.format("bundle-%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%2$s", System.currentTimeMillis(), id);
        File bundleDir = new File(System.getProperty("bundle.dir", "."));
        File tmpFile = new File(bundleDir, newFileName + ".tmp");
        byte[] b = new byte[4096];
        try (OutputStream out = new FileOutputStream(tmpFile);
            ZipOutputStream zip = new ZipOutputStream(out)) {
            for (File file : contents) {
                zip.putNextEntry(new ZipEntry(file.getPath()));
                try (InputStream in = new FileInputStream(new File(parentDir, file.getPath()))) {
                    for (int c = in.read(b); c > 0; c = in.read(b)) {
                        zip.write(b, 0, c);
                    }
                }
            }
        } catch (IOException e) {
            if (tmpFile.exists()) {
                tmpFile.deleteOnExit();
            }
            throw new BundleCreateException("Error creating zip bundle: " + e.getMessage(), e);
        }
        File newBundle = new File(bundleDir, newFileName + ".zip");
        if (tmpFile.renameTo(newBundle)) {
            return newBundle;
        }
        throw new BundleCreateException("Failed to rename temp file to final filename: " + tmpFile + " -> " + newBundle);
    }

    private boolean useBundle(File newFile) {
        File currentFile = getBundleFile();
        if (currentFile.getName().equals("bundle-use.zip")) {
            if (!currentFile.delete()) {
                return false;
            }
        }
        Runtime runtime = Runtime.getRuntime();
        String[] args = { "/bin/ln", "-fs", newFile.getPath(), currentFile.getPath() };
        try {
            Process process = runtime.exec(args);
            String err = buffer(new InputStreamReader(process.getErrorStream()));
            if (process.waitFor() == 0) {
                return true;
            }
            System.err.println(err);
            return false;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private File getBundleFile() {
        if (bundleFile == null) {
            File bundleDir = new File(System.getProperty("bundle.dir", "."));
            for (File file : bundleDir.listFiles((dir, name) -> {
                return name.matches("bundle(-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-[0-9a-f]{6,12}|-dev|-use)\\.zip");
            })) {
                if (bundleFile == null || file.getName().compareTo(bundleFile.getName()) > 0) {
                    bundleFile = file;
                }
            }
            if (bundleFile == null) {
                System.err.println("FATAL - Unable to locate a suitable bundle.zip in " + bundleDir);
                throw new BundleInitializationError("Unable to locate a suitable bundle.zip in " + bundleDir);
            }
        }
        return bundleFile;
    }

    private byte[] buffer(ByteArrayOutputStream buffer, byte[] b, InputStream in) throws IOException {
        for (int c = in.read(b); c > 0; c = in.read(b)) {
            buffer.write(b, 0, c);
        }
        try {
            return buffer.toByteArray();
        } finally {
            buffer.reset();
        }
    }

    private String buffer(Reader reader) throws IOException {
        StringBuilder buffer = new StringBuilder(256);
        char[] b = new char[128];
        for (int c = reader.read(b); c > 0; c = reader.read(b)) {
            buffer.append(b, 0, c);
        }
        return buffer.toString();
    }

    private static class Entry {
        private final String name;
        private final byte[] data;

        private Entry(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }
    }
}
