package com.sythinian.app.service;

import com.sythinian.app.config.StorageProperties;
import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.repository.VideoFileRepository;
import org.bytedeco.ffmpeg.global.avformat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {

    @Autowired
    private StorageService storageService;

    @Autowired
    private VideoFileRepository videoFileRepository;

    @Autowired
    private StorageProperties storageProperties;

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
            storageService.store(file, newFilename);
        } catch (Exception e) {
            videoFileRepository.delete(videoFile);
            throw e;
        }

        System.out.println("New video file stored!");
        this.processVideo(videoFile);
    }

    /**
     * Loads an actual video file resource from the filesystem.
     *
     * @param videoId ID of the video entry in database
     */
    public Resource loadVideoFile(long videoId, VideoFileVariant variant) {
        String filename = VideoService.GetFilesystemFilename(videoId, VideoFileVariant.ORIGINAL);
        return this.storageService.load(filename);
    }

    private void processVideo(VideoFileModel video) {
        long start = System.currentTimeMillis();
        long videoId = video.getId();
        System.out.println("Processing video id: " + videoId);

        Resource videoFile = this.loadVideoFile(videoId, VideoFileVariant.ORIGINAL);
        this.reMuxVideo(videoId, videoFile);
        this.transcode480p(videoId, videoFile);

        System.out.println("Video processing id: " + videoId + " done in: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void reMuxVideo(long videoId, Resource sourceFile) {
    }

    private void transcode480p(long videoId, Resource sourceFile) {
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
