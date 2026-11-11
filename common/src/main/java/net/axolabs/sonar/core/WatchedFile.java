package net.axolabs.sonar.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class WatchedFile {

    private static final long CHECK_EVERY_MS = 1000;

    private final Path path;
    private final String defaultResource;
    private long lastModified = Long.MIN_VALUE;
    private long lastCheck;

    WatchedFile(Path path, String defaultResource) {
        this.path = path;
        this.defaultResource = defaultResource;
    }

    Path path() {
        return path;
    }

    synchronized boolean changed() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < CHECK_EVERY_MS && lastModified != Long.MIN_VALUE) return false;
        lastCheck = now;
        try {
            if (!Files.exists(path)) writeDefault();
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (modified == lastModified) return false;
            lastModified = modified;
            return true;
        } catch (IOException e) {
            Sonar.LOG.accept("Cannot access " + path + ": " + e);
            boolean first = lastModified == Long.MIN_VALUE;
            lastModified = 0;
            return first;
        }
    }

    private void writeDefault() throws IOException {
        Files.createDirectories(path.getParent());
        try (InputStream in = WatchedFile.class.getResourceAsStream(defaultResource)) {
            if (in == null) throw new IOException("missing default " + defaultResource);
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static InputStream openDefault(String resource) {
        return WatchedFile.class.getResourceAsStream(resource);
    }
}
