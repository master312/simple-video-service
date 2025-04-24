package com.sythinian.app.dto;

import com.sythinian.app.model.VideoFileModel;

import java.time.LocalDateTime;

public class VideoFileDto {
    private Long id;
    private String originalFilename;
    private VideoFileModel.Status status;
    private LocalDateTime createdAt;

    private VideoFileDto() {
    }

    public static VideoFileDto fromEntity(VideoFileModel videoFile) {
        VideoFileDto dto = new VideoFileDto();
        dto.id = videoFile.getId();
        dto.originalFilename = videoFile.getOriginalFilename();
        dto.status = videoFile.getStatus();
        dto.createdAt = videoFile.getCreatedAt();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public VideoFileModel.Status getStatus() {
        return status;
    }
} 