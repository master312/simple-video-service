package com.sythinian.app.service;

import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.repository.VideoFileRepository;
import com.sythinian.app.service.task.RemuxTask;
import com.sythinian.app.service.task.TranscodeTask;
import com.sythinian.app.service.task.VideoProcessingTask;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_INFO;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

@Service
public class VideoService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private VideoFileRepository videoFileRepository;

    public enum VideoFileVariant {
        // original flv
        ORIGINAL,
        // orignal, but in mp4 container
        REMUX,
        // transcoded, mp4 @ 480p
        TRANSCODE,
    }

    public VideoService() {
        FFmpegLogCallback.set();
        av_log_set_level(AV_LOG_INFO);
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

        System.out.println("New video file stored! ID: " + savedFile.getId());

        long start = System.currentTimeMillis();
        TranscodeTask transcodeTask = new TranscodeTask(savedFile);
        RemuxTask remuxTask = new RemuxTask(savedFile);
        try {
            ///////// For simplicity, we just process tasks immediately /////////
            /////////  In real project, we would have some kind of smart task queueing and scheduling /////////
            // TODO: Maybe implement proper task queuing and processing on thread?
            this.processTaskNow(transcodeTask);
            this.processTaskNow(remuxTask);

            savedFile.setStatus(VideoFileModel.Status.AVAILABLE);
            videoFileRepository.save(savedFile);
        } catch (Exception e) {
            System.err.println("Video ID: " + savedFile.getId() + " failed to process!");
            // TODO: also delete all other files that might be created by tasks
            fileStorageService.delete(newFilename);
            videoFileRepository.delete(videoFile);
            throw e;
        } finally {
            transcodeTask.cleanup();
            remuxTask.cleanup();
        }

        System.out.println("Video ID: " + savedFile.getId() + " processed in " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Loads an actual video file resource from the filesystem.
     *
     * @param videoId ID of the video entry in database
     */
    public Resource loadVideoFile(long videoId, VideoFileVariant variant) {
        String filename = VideoService.GetFilesystemFilename(videoId, variant);
        return this.fileStorageService.load(filename);
    }

    /**
     * Saves a processed video file to storage
     *
     * @param videoId ID of the video entry in database
     */
    public void saveVideoFile(long videoId, VideoFileVariant variant, File file) {
        String filename = VideoService.GetFilesystemFilename(videoId, variant);
        try {
            this.fileStorageService.store(file, filename);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save video file.", e);
        }
    }

    private void processTaskNow(VideoProcessingTask task) {
        // Just execute everything now, since we don't have any scheduling mechanism
        task.prepare(this, this.fileStorageService);
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
