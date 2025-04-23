package com.sythinian.app.repository;

import com.sythinian.app.model.VideoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, Long> {
    List<VideoFile> findAllByStatus(VideoFile.Status status);
} 