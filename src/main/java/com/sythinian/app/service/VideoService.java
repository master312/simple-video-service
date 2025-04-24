package com.sythinian.app.service;

import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.repository.VideoFileRepository;
import com.sythinian.app.service.task.RemuxTask;
import com.sythinian.app.service.task.TranscodeTask;
import com.sythinian.app.service.task.VideoProcessingTask;
import org.bytedeco.ffmpeg.global.avformat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private VideoFileRepository videoFileRepository;

    public enum VideoFileVariant {
        // Loads original flv
        ORIGINAL,
        // Loads orignal in mp4 container
        REMUX,
        // Loads transcoded mp4 in 480p
        TRANSCODE,
    }

    public VideoService() {
        System.out.println("$$$$$ libAvFormat version: " + avformat.avformat_version());
    }

    /**
     * Called to handle addition of new video.
     * Stores actual file in storage, store metadata in DB, and queue for processing
     */
    public void handleNewVideo(MultipartFile file) {
        VideoFileModel videoFile = new VideoFileModel(file.getOriginalFilename());
        VideoFileModel savedFile = videoFileRepository.save(videoFile);
        String newFilename = VideoService.GetFilesystemFilename(savedFile.getId(), VideoFileVariant.ORIGINAL);
        try {
            fileStorageService.store(file, newFilename);
        } catch (Exception e) {
            videoFileRepository.delete(videoFile);
            throw e;
        }

        System.out.println("New video file stored!");

        ///////// For simplicity, we just process tasks immediately /////////
        // TODO: Maybe implement proper task queuing and processing on thread.
        long start = System.currentTimeMillis();
        TranscodeTask transcodeTask = new TranscodeTask(savedFile);
        RemuxTask remuxTask = new RemuxTask(savedFile);

        this.processTaskNow(transcodeTask);
        this.processTaskNow(remuxTask);

        savedFile.setStatus(VideoFileModel.Status.AVAILABLE);
        videoFileRepository.save(savedFile);

        System.out.println("Video ID: " + savedFile.getId() + " processed in " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Loads an actual video file resource from the filesystem.
     *
     * @param videoId ID of the video entry in database
     */
    public Resource loadVideoFile(long videoId, VideoFileVariant variant) {
        String filename = VideoService.GetFilesystemFilename(videoId, VideoFileVariant.ORIGINAL);
        return this.fileStorageService.load(filename);
    }

    private void processTaskNow(VideoProcessingTask task) {
        task.execute(this, this.fileStorageService);
    }

    /**
     * TODO: Maybe move to utils class?
     *
     * @param videoId ID of video entry in the database
     */
    private static String GetFilesystemFilename(long videoId, VideoFileVariant variant) {
        String strId = Long.toString(videoId);
        switch (variant) {
            case ORIGINAL:
                return strId + ".flv";
            case REMUX:
                return strId + ".mp4";
            case TRANSCODE:
                return strId + " - 480p.mp4";
        }

        throw new RuntimeException("Should never be reached!");
    }
}
