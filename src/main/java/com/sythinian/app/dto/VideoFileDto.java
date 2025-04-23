package com.sythinian.app.dto;

import com.sythinian.app.model.VideoFile;

public class VideoFileDto {
    private Long id;
    private String originalFilename;
    private VideoFile.Status status;

    private VideoFileDto() {
    }

    public static VideoFileDto fromEntity(VideoFile videoFile) {
        VideoFileDto dto = new VideoFileDto();
        dto.id = videoFile.getId();
        dto.originalFilename = videoFile.getOriginalFilename();
        dto.status = videoFile.getStatus();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public VideoFile.Status getStatus() {
        return status;
    }
} 