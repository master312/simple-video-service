package com.sythinian.app.service.task;

import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.service.FileStorageService;
import com.sythinian.app.service.VideoService;

public abstract class VideoProcessingTask {
    protected VideoFileModel video;

    protected VideoProcessingTask(VideoFileModel video) {
        this.video = video;
    }

    public abstract void prepare(VideoService videoService, FileStorageService storageService);

    public abstract void execute(VideoService videoService, FileStorageService storageService);

    /**
     * Invoked when service is done with this task.
     * Guaranteed to be invoked in any case (exception, success, etc...)
     */
    public abstract void cleanup();
}
