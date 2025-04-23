package com.sythinian.app.service;

import com.sythinian.app.model.VideoFile;
import com.sythinian.app.repository.VideoFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {

    @Autowired
    private StorageService storageService;

    @Autowired
    private VideoFileRepository videoFileRepository;

    public VideoService() {
    }

    /**
     * Called to handle addition of new video.
     * Stores actual file in storage, store metadata in DB, and queue for processing
     */
    public void handleNewVideo(MultipartFile file) {
        VideoFile videoFile = new VideoFile(file.getOriginalFilename());
        VideoFile savedFile = videoFileRepository.save(videoFile);
        String newFilename = savedFile.getId().toString() + ".flv";
        try {
            storageService.store(file, newFilename);
        } catch (Exception e) {
            videoFileRepository.delete(videoFile);
            throw e;
        }

        System.out.println("New video file stored!");
    }
}
