package com.sythinian.app.controller;

import com.sythinian.app.dto.VideoFileDto;
import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.repository.VideoFileRepository;
import com.sythinian.app.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class RestApiController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoFileRepository videoRepository;

    @GetMapping("/downloadOriginal/{id}")
    public ResponseEntity<Resource> downloadOriginal(@PathVariable("id") long id, HttpServletRequest request) throws IOException {
        return this.download(id, VideoService.VideoFileVariant.REMUX, request);
    }

    @GetMapping("/downloadTranscoded/{id}")
    public ResponseEntity<Resource> downloadTranscoded(@PathVariable("id") long id, HttpServletRequest request) throws IOException {
        return this.download(id, VideoService.VideoFileVariant.TRANSCODE, request);
    }

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

    private ResponseEntity<Resource> download(long id, VideoService.VideoFileVariant variant, HttpServletRequest request) throws IOException {
        Optional<VideoFileModel> byId = videoRepository.findById(id);
        if (byId.isEmpty()) {
            throw new FileNotFoundException();
        }

        Resource resource = this.videoService.loadVideoFile(id, variant);
        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + byId.get().getOriginalFilename() + ".mp4\"")
                .body(resource);
    }
}
