package com.secureprint.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {
    private final Path baseDirectory;
    public FileStorageService(@Value("${app.storage-directory}") String directory) {
        this.baseDirectory = Paths.get(directory).toAbsolutePath().normalize();
        try { Files.createDirectories(baseDirectory); }
        catch (IOException ex) { throw new IllegalStateException("Unable to initialize storage directory", ex); }
    }
    public String store(byte[] encryptedBlob) {
        String filename = UUID.randomUUID().toString() + ".bin";
        Path target = resolve(filename);
        Path temp;
        try {
            temp = Files.createTempFile(baseDirectory, ".upload-", ".tmp");
            Files.write(temp, encryptedBlob, StandardOpenOption.TRUNCATE_EXISTING);
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target); }
            return filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store encrypted document", ex);
        }
    }
    public byte[] read(String relativePath) {
        try { return Files.readAllBytes(resolve(relativePath)); }
        catch (IOException ex) { throw new IllegalStateException("Unable to read stored document", ex); }
    }
    public void delete(String relativePath) {
        try { Files.deleteIfExists(resolve(relativePath)); }
        catch (IOException ex) { throw new IllegalStateException("Unable to delete stored document", ex); }
    }
    private Path resolve(String relativePath) {
        Path resolved = baseDirectory.resolve(relativePath).normalize();
        if (!resolved.startsWith(baseDirectory) || relativePath.contains("\\") || relativePath.contains("/")) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        return resolved;
    }
}
