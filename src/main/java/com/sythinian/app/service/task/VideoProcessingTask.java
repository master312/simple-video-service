package com.sythinian.app.service.task;

import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.service.FileStorageService;
import com.sythinian.app.service.VideoService;

public abstract class VideoProcessingTask {
    protected VideoFileModel video;

    protected VideoProcessingTask(VideoFileModel video) {
        this.video = video;
    }

    public abstract void execute(VideoService videoService, FileStorageService storageService);
}
