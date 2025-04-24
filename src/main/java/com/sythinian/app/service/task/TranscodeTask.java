package com.sythinian.app.service.task;

import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.service.FileStorageService;
import com.sythinian.app.service.VideoService;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

public class TranscodeTask extends VideoProcessingTask {
    private static final int TARGET_HEIGHT = 480;

    private File tempOutputFile;
    private FFmpegFrameGrabber grabber;
    private FFmpegFrameRecorder recorder;

    public TranscodeTask(VideoFileModel video) {
        super(video);
    }

    @Override
    public void prepare(VideoService videoService, FileStorageService storageService) {
        Resource inputFileResource = videoService.loadVideoFile(this.video.getId(), VideoService.VideoFileVariant.ORIGINAL);
        try {
            this.grabber = new FFmpegFrameGrabber(inputFileResource.getFile());
            this.grabber.start();

            int inputWidth = this.grabber.getImageWidth();
            int inputHeight = this.grabber.getImageHeight();
            int outputWidth = (int) Math.round(((double) inputWidth / inputHeight) * TARGET_HEIGHT);
            if (outputWidth % 2 != 0) {
                // codec might break with uneven numbers...
                outputWidth++;
            }

            this.tempOutputFile = File.createTempFile("transcode_tmp_" + this.video.getId(), ".mp4");
            this.tempOutputFile.deleteOnExit();

            this.recorder = new FFmpegFrameRecorder(this.tempOutputFile, outputWidth, TARGET_HEIGHT, this.grabber.getAudioChannels());
            this.recorder.setFormat("mp4");
            this.recorder.setFrameRate(this.grabber.getFrameRate());
            this.recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            this.recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            // recorder.setVideoQuality(20);
            // recorder.setVideoBitrate(1000000);
            this.recorder.setSampleRate(this.grabber.getSampleRate());
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare TranscodeTask for video ID: " + this.video.getId(), e);
        }

        System.out.println("TranscodeTask preparation done for videoID: " + this.video.getId());
    }

    @Override
    public void execute(VideoService videoService, FileStorageService storageService) {
        try {
            this.recorder.start();

            Frame frame;
            while ((frame = this.grabber.grab()) != null) {
                this.recorder.record(frame);
            }

            this.recorder.stop();
            videoService.saveVideoFile(video.getId(), VideoService.VideoFileVariant.TRANSCODE, tempOutputFile);
            System.out.println("Video ID " + video.getId() + " transcoded to 480p MP4!");
        } catch (Exception e) {
            throw new RuntimeException("Failed to transcode videoID: " + this.video.getId(), e);
        }
    }

    @Override
    public void cleanup() {
        try {
            if (this.recorder != null) {
                this.recorder.stop();
                this.recorder.release();
                this.recorder = null;
            }

            if (this.grabber != null) {
                this.grabber.stop();
                this.grabber.release();
                this.grabber = null;
            }
        } catch (Exception e) {
            System.err.println("TranscodeTask error on cleanup. ID: " + this.video.getId() + ": " + e.getMessage());
        }

        if (this.tempOutputFile != null) {
            this.tempOutputFile.delete();
            this.tempOutputFile = null;
        }
    }
}
