package com.sythinian.app.service;

import com.sythinian.app.exception.StorageException;
import com.sythinian.app.exception.StorageFileNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

// TODO: This service can be made a interface, with 'FileSystemStorageService' implementation
@Service
public class StorageService {

    private final Path rootLocation;

    public StorageService() {
        this.rootLocation = Paths.get("runtime-data/video-files");
    }

    public void store(MultipartFile file, String filename) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Empty file!");
            }

            Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException("No no! Root path invalid.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    Resource load(String filename) {
        try {
            Path file = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            if (!file.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException("Tried to read from invalid location.");
            }

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
}
