package com.sythinian.app.controller;

import com.sythinian.app.dto.VideoFileDto;
import com.sythinian.app.repository.VideoFileRepository;
import com.sythinian.app.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class RestApiController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoFileRepository videoRepository;

    @GetMapping("/uploads")
    public List<VideoFileDto> getAllVideos() {
        return videoRepository.findAll().stream().map(VideoFileDto::fromEntity).collect(Collectors.toList());
    }

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".flv")) {
            return ResponseEntity.badRequest().body("Only FLV files are allowed");
        }

        try {
            videoService.handleNewVideo(file);
            return ResponseEntity.ok("File uploaded successfully: " + file.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + e.getMessage());
        }
    }
}
