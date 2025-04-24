package com.sythinian.app.service;

import com.sythinian.app.exception.StorageException;
import com.sythinian.app.exception.StorageFileNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileStorageService {
    void store(MultipartFile file, String filename) throws StorageException;
    void store(File file, String filename) throws StorageException;
    Resource load(String filename) throws StorageFileNotFoundException;
}