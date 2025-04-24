package com.sythinian.app.service;

import com.sythinian.app.config.StorageProperties;
import com.sythinian.app.exception.StorageException;
import com.sythinian.app.exception.StorageFileNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileSystemFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public FileSystemFileStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Override
    public void store(MultipartFile file, String filename) {
        if (file.isEmpty()) {
            throw new StorageException("Empty file!");
        }

        Path destinationFile = validateAndResolveDestinationPath(filename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public void store(File file, String filename) {
        if (!file.exists()) {
            throw new StorageException("File does not exist!");
        }

        Path destinationFile = validateAndResolveDestinationPath(filename);
        try {
            Files.copy(file.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = validateAndResolveDestinationPath(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    /**
     * Validates and resolves the destination path for a file
     *
     * @param filename The target filename
     */
    private Path validateAndResolveDestinationPath(String filename) {
        Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new StorageException("Invalid path: attempted to access outside of storage root.");
        }
        return destinationFile;
    }
} 