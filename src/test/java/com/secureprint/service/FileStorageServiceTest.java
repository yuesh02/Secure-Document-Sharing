package com.secureprint.service;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {
    @Test void storesOnlyEncryptedBytesAndDeletesSafely() throws Exception {
        Path directory = Files.createTempDirectory("secure-print-storage");
        FileStorageService storage = new FileStorageService(directory.toString());
        byte[] encrypted = new byte[] { 1, 2, 3 };
        String path = storage.store(encrypted);
        assertArrayEquals(encrypted, storage.read(path));
        assertFalse(new String(storage.read(path)).contains("%PDF-"));
        storage.delete(path);
        assertThrows(IllegalStateException.class, () -> storage.read(path));
        assertThrows(IllegalArgumentException.class, () -> storage.read("../secret"));
    }
}
