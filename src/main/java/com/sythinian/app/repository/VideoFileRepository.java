package com.sythinian.app.repository;

import com.sythinian.app.model.VideoFileModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFileModel, Long> {
    List<VideoFileModel> findAllByStatus(VideoFileModel.Status status);
} 