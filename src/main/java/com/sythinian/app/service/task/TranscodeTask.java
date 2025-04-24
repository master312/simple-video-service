package com.sythinian.app.service.task;

import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.service.FileStorageService;
import com.sythinian.app.service.VideoService;

public class TranscodeTask extends VideoProcessingTask {
    public TranscodeTask(VideoFileModel video) {
        super(video);
    }

    @Override
    public void execute(VideoService videoService, FileStorageService storageService) {
        // TODO:////
    }
}
