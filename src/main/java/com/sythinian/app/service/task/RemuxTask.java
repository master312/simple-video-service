package com.sythinian.app.service.task;

import com.sythinian.app.model.VideoFileModel;
import com.sythinian.app.service.FileStorageService;
import com.sythinian.app.service.VideoService;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;

import java.io.File;
import java.io.IOException;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class RemuxTask extends VideoProcessingTask {
    private File tempOutputFile;

    private AVFormatContext inputCtx;
    private AVFormatContext outputCtx;

    public RemuxTask(VideoFileModel video) {
        super(video);
    }

    @Override
    public void prepare(VideoService videoService, FileStorageService storageService) {
        this.prepareInputCtx(videoService);
        this.prepareOutputCtx();
        this.validateCodecSupport();
        System.out.println("RemuxTask preparation done for videoID: " + this.video.getId());
    }

    @Override
    public void execute(VideoService videoService, FileStorageService storageService) {
        int streamsCnt = this.inputCtx.nb_streams();
        int[] streamsMapping = new int[streamsCnt];
        int streamMapIndex = 0;

        // (re)Map streams and codecs to output ctx
        for (int i = 0; i < streamsCnt; i++) {
            AVCodecParameters codecPar = this.inputCtx.streams(i).codecpar();
            int codecType = codecPar.codec_type();
            if (codecType != AVMEDIA_TYPE_AUDIO && codecType != AVMEDIA_TYPE_VIDEO && codecType != AVMEDIA_TYPE_SUBTITLE) {
                streamsMapping[i] = -1;
                continue;
            }

            streamsMapping[i] = streamMapIndex++;

            // Create and setup new stream in output
            AVStream outStream = avformat_new_stream(this.outputCtx, null);
            if (outStream == null || outStream.isNull()) {
                throw new RuntimeException("Failed allocating output stream");
            }

            if (avcodec_parameters_copy(outStream.codecpar(), codecPar) < 0) {
                throw new RuntimeException("Failed to copy codec parameters.");
            }

            outStream.codecpar().codec_tag(0); // Tells FFMPEG to use default codec tags?
        }

        if (avformat_write_header(this.outputCtx, (AVDictionary) null) < 0) {
            throw new RuntimeException("Error occurred when writing output header.");
        }

        // An actual packet remux code
        AVPacket packet = av_packet_alloc();
        int result;
        while ((result = av_read_frame(this.inputCtx, packet)) >= 0) {
            int inStreamIndex = packet.stream_index();
            if (inStreamIndex >= streamsMapping.length || streamsMapping[inStreamIndex] < 0) {
                // We do not copy this stream.... Just unref packet
                av_packet_unref(packet);
                continue;
            }

            packet.stream_index(streamsMapping[inStreamIndex]);

            AVStream inStream = this.inputCtx.streams(inStreamIndex);
            AVStream outStream = this.outputCtx.streams(packet.stream_index());

            AVRational inTimeBase = inStream.time_base();
            AVRational outTimeBase = outStream.time_base();
            av_packet_rescale_ts(packet, inTimeBase, outTimeBase);

            // Write actual packet
            packet.pos(-1);
            int writeResult = av_interleaved_write_frame(this.outputCtx, packet);
            if (writeResult < 0) {
                System.err.println("ERROR muxing packet! Could not write frame.");
                result = writeResult;
                break;
            }
        }

        if (result != AVERROR_EOF()) {
            av_packet_free(packet);
            throw new RuntimeException("RemuxTask failed!");
        }

        av_write_trailer(this.outputCtx);
        av_packet_free(packet);

        videoService.saveVideoFile(video.getId(), VideoService.VideoFileVariant.REMUX, tempOutputFile);
        System.out.println("Video ID " + video.getId() + " successfully remuxed to MP4!");
    }

    @Override
    public void cleanup() {
        try {
            if (this.inputCtx != null && !this.inputCtx.isNull()) {
                avformat_close_input(this.inputCtx);
                this.inputCtx = null;
            }

            if (this.outputCtx != null && !this.outputCtx.isNull()) {
                // Close the IO context
                avio_closep(this.outputCtx.pb());
                avformat_free_context(this.outputCtx);
                this.outputCtx = null;
            }
        } catch (Exception e) {
            System.err.println("RemuxTask error on cleanup. ID: " + this.video.getId() + ": " + e.getMessage());
        }

        if (this.tempOutputFile != null) {
            this.tempOutputFile.delete();
            this.tempOutputFile = null;
        }
    }

    private void prepareOutputCtx() {
        try {
            tempOutputFile = File.createTempFile("remux_tmp_" + this.video.getId(), ".mp4");
            tempOutputFile.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file mp4 remuxing", e);
        }

        String outAbsPath = tempOutputFile.getAbsolutePath();
        this.outputCtx = new AVFormatContext(null);
        if (avformat_alloc_output_context2(this.outputCtx, null, null, outAbsPath) < 0) {
            throw new RuntimeException("Could not create output context");
        }

        // Open file for output
        AVIOContext outIoCtx = new AVIOContext(null);
        if (avio_open(outIoCtx, outAbsPath, AVIO_FLAG_WRITE) < 0) {
            throw new RuntimeException("Could not open output file: " + outAbsPath);
        }

        this.outputCtx.pb(outIoCtx); // Assign the IO context to the format context
    }

    private void prepareInputCtx(VideoService videoService) {
        this.inputCtx = new AVFormatContext(null);
        String inputFilename;

        try {
            inputFilename = videoService.loadVideoFile(this.video.getId(), VideoService.VideoFileVariant.ORIGINAL).getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Could not open source file " + e);
        }

        if (avformat_open_input(inputCtx, inputFilename, null, null) < 0) {
            throw new RuntimeException("FFMPEG could not open " + inputFilename);
        }

        if (avformat_find_stream_info(this.inputCtx, (AVDictionary) null) < 0) {
            throw new RuntimeException("Failed to retrieve input stream information");
        }

//        av_dump_format(this.inputCtx, 0, inputFilename, 0);
    }

    /**
     * Validates if codecs in inputCtx(flv) streams are valid to use in outputCtx(mp4).
     */
    private void validateCodecSupport() {
        if (this.inputCtx == null || this.outputCtx == null) {
            throw new RuntimeException("This should never happen 1!");
        }

        AVOutputFormat outFormat = this.outputCtx.oformat();
        if (outFormat == null || outFormat.isNull()) {
            throw new RuntimeException("This should never happen 2!");
        }

        for (int i = 0; i < this.inputCtx.nb_streams(); i++) {
            AVCodecParameters codecPar = this.inputCtx.streams(i).codecpar();
            int codecType = codecPar.codec_type();
            int codecId = codecPar.codec_id();
            if (codecType != AVMEDIA_TYPE_VIDEO && codecType != AVMEDIA_TYPE_AUDIO) {
                continue;
            }

            if (avformat_query_codec(outFormat, codecId, FF_COMPLIANCE_NORMAL) == 1) {
                continue;
            }

            // If this is reached, Codec 's unsupported....
            String codecName = avcodec_get_name(codecId).getString();
            throw new RuntimeException(
                    "Input " + ((codecType == AVMEDIA_TYPE_VIDEO) ? "Video" : "Audio") + " codec '" + codecName + "' "
                            + "in stream " + i + " not supported to remux into MP4 container."
            );
        }
    }
}
