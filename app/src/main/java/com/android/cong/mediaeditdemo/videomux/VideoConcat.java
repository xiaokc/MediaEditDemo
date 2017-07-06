package com.android.cong.mediaeditdemo.videomux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.Surface;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class VideoConcat {

    private static final String TAG = "===>xkc";
    private static final boolean VERBOSE = true;

    private String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private int OUTPUT_VIDEO_BIT_RATE = 2000000;         // 2Mbps
    private int OUTPUT_VIDEO_FRAME_RATE = 15;            // 15fps
    private int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;       // 10 seconds between I-frames
    private int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    private String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm";  // Advanced Audio Coding
    private int OUTPUT_AUDIO_CHANNEL_COUNT = 2;                 // Must match the input stream.
    private int OUTPUT_AUDIO_BIT_RATE = 128 * 1024;
    private int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;
    private int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 44100;            // Must match the input stream.

    private boolean mCopyVideo = true;
    private boolean mCopyAudio = false;

    private int mWidth = -1;
    private int mHeight = -1;

    private String mOutputFile;
    private List<String> srcVideoFileList;

    private MediaExtractor mVideoExtractor = null;
    private MediaExtractor mAudioExtractor = null;
    private InputSurface mInputSurface = null;
    private OutputSurface mOutputSurface = new OutputSurface();
    private MediaCodec mVideoDecoder = null;
    private MediaCodec mAudioDecoder = null;
    private MediaCodec mVideoEncoder = null;
    private MediaCodec mAudioEncoder = null;
    private MediaMuxer mMuxer = null;

    private HandlerThread mVideoDecoderHandlerThread;
    private CallbackHandler mVideoDecoderHandler;

    private ProgressListener progressListener;
    private MuxerController muxerController;

    private int fileIndex = 0;
    private int fileSize;
    private boolean videoEncoderRuning = false;
    private long lastVideoSamplePresentationTime = 0;
    private MediaFormat inputFormat;

    public static interface MuxerController {
        boolean isDropFrame(long presentationTime);
    }

    interface ProgressListener {

        void onStart();

        void onProgress(int progress);

        void onCompleted();

        void onError();
    }

    interface DecoderCallback {
        void onFinish();
    }

    public VideoConcat(List<String> srcVideoList, String outFile) {
        if (null == srcVideoList || srcVideoList.size() <= 0) {
            return;
        }
        this.srcVideoFileList = srcVideoList;
        this.mOutputFile = outFile;
        fileSize = srcVideoList.size();
    }

    public void setProgressListener(ProgressListener listener) {
        progressListener = listener;
    }

    public void setMuxerController(MuxerController controller) {
        muxerController = controller;
    }

    public void start(boolean sync) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    extractDecodeEditEncodeMux();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
        if (sync) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setCopyVideo(boolean val) {
        mCopyVideo = val;
    }

    public void setCopyAudio(boolean val) {
        mCopyAudio = val;
    }

    public void setSize(int width, int height) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        mWidth = width;
        mHeight = height;
    }

    private void extractDecodeEditEncodeMux() throws Exception {

        Exception exception = null;

        mDecoderOutputVideoFormat = null;
        mDecoderOutputAudioFormat = null;
        mEncoderOutputVideoFormat = null;
        mEncoderOutputAudioFormat = null;

        mOutputVideoTrack = -1;
        mOutputAudioTrack = -1;
        mVideoExtractorDone = false;
        mVideoDecoderDone = false;
        mVideoEncoderDone = false;
        mAudioExtractorDone = false;
        mAudioDecoderDone = false;
        mAudioEncoderDone = false;
        mPendingAudioDecoderOutputBufferIndices = new LinkedList<>();
        mPendingAudioDecoderOutputBufferInfos = new LinkedList<>();
        mPendingAudioEncoderInputBufferIndices = new LinkedList<>();
        mPendingVideoEncoderOutputBufferIndices = new LinkedList<>();
        mPendingVideoEncoderOutputBufferInfos = new LinkedList<>();
        mPendingAudioEncoderOutputBufferIndices = new LinkedList<>();
        mPendingAudioEncoderOutputBufferInfos = new LinkedList<>();
        mMuxing = false;
        mVideoExtractedFrameCount = 0;
        mVideoDecodedFrameCount = 0;
        mVideoEncodedFrameCount = 0;
        mAudioExtractedFrameCount = 0;
        mAudioDecodedFrameCount = 0;
        mAudioEncodedFrameCount = 0;

        if (progressListener != null) {
            progressListener.onStart();
        }

        MediaCodecInfo videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE);
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + OUTPUT_VIDEO_MIME_TYPE);
            return;
        }

        if (VERBOSE) {
            Log.d(TAG, "video found codec: " + videoCodecInfo.getName());
        }

        MediaCodecInfo audioCodecInfo = selectCodec(OUTPUT_AUDIO_MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + OUTPUT_AUDIO_MIME_TYPE);
            return;
        }

        if (VERBOSE) {
            Log.d(TAG, "audio found codec: " + audioCodecInfo.getName());
        }

        try {
            mMuxer = createMuxer();

            while (fileIndex < fileSize) {
                if (fileIndex == 0) {
                    if (mCopyVideo) {

                        mVideoExtractor = createExtractor(srcVideoFileList.get(fileIndex));
                        int videoInputTrack = getAndSelectVideoTrackIndex(mVideoExtractor);
                        inputFormat = mVideoExtractor.getTrackFormat(videoInputTrack);

                        if (mWidth == -1 || mHeight == -1) {
                            mWidth = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
                            mHeight = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        }

                        int rotation = 0;
                        if (inputFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                            rotation = inputFormat.getInteger(MediaFormat.KEY_ROTATION);
                        }

                        MediaFormat outputVideoFormat;

                        if (rotation == 0) {
                            outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, mWidth, mHeight);
                        } else {
                            outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, mHeight, mWidth);
                        }

                        outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
                        outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
                        outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
                        outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);

                        if (rotation != 0) {
                            outputVideoFormat.setInteger(MediaFormat.KEY_ROTATION, rotation);
                        }

                        if (VERBOSE) {
                            Log.d(TAG, "video format: " + outputVideoFormat);
                        }

                        AtomicReference<Surface> inputSurfaceReference = new AtomicReference<Surface>();
                        mVideoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
                        mInputSurface = new InputSurface(inputSurfaceReference.get());
                        mInputSurface.makeCurrent();

                        mOutputSurface.setup(mWidth, mHeight, rotation);
                        mVideoDecoder = createVideoDecoder(inputFormat, mOutputSurface.getSurface());
                        mInputSurface.releaseEGLContext();

                    }

                    if (mCopyAudio) {
                        mAudioExtractor = createExtractor(srcVideoFileList.get(fileIndex));
                        int audioInputTrack = getAndSelectAudioTrackIndex(mAudioExtractor);
                        MediaFormat inputFormat = mAudioExtractor.getTrackFormat(audioInputTrack);

                        String mimeType = "audio/mp4a-latm";
                        int sampleRateHz = OUTPUT_AUDIO_SAMPLE_RATE_HZ;
                        int channelCount = OUTPUT_AUDIO_CHANNEL_COUNT;
                        int bitRate = OUTPUT_AUDIO_BIT_RATE;
                        int accProfile = OUTPUT_AUDIO_AAC_PROFILE;

                        if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRateHz = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        }

                        if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        }

                        if (inputFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            bitRate = inputFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                        }

                        if (inputFormat.containsKey(MediaFormat.KEY_AAC_PROFILE)) {
                            accProfile = inputFormat.getInteger(MediaFormat.KEY_AAC_PROFILE);
                        }

                        MediaFormat outputAudioFormat =
                                MediaFormat.createAudioFormat(mimeType, sampleRateHz, channelCount);

                        outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                        outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, accProfile);

                        mAudioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat);
                        mAudioDecoder = createAudioDecoder(inputFormat);
                    }

                }
            }
            awaitEncode();

        } finally {

            if (VERBOSE) {
                Log.d(TAG, "releasing extractor, decoder, encoder, and muxer");
            }

            try {
                if (mVideoExtractor != null) {
                    mVideoExtractor.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoExtractor", e);
                if (exception == null) {
                    exception = e;
                }
            }

            try {
                if (mAudioExtractor != null) {
                    mAudioExtractor.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing audioExtractor", e);
                if (exception == null) {
                    exception = e;
                }
            }

            try {
                if (mVideoDecoder != null) {
                    mVideoDecoder.stop();
                    mVideoDecoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoDecoder", e);
                if (exception == null) {
                    exception = e;
                }
            }

            try {
                if (mOutputSurface != null) {
//                    mOutputSurface.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing outputSurface", e);
                if (exception == null) {
                    exception = e;
                }
            }

            try {
                if (mVideoEncoder != null) {
                    mVideoEncoder.stop();
                    mVideoEncoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing videoEncoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (mAudioDecoder != null) {
                    mAudioDecoder.stop();
                    mAudioDecoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing audioDecoder", e);
                if (exception == null) {
                    exception = e;
                }
            }

            try {
                if (mAudioEncoder != null) {
                    mAudioEncoder.stop();
                    mAudioEncoder.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing audioEncoder", e);
                if (exception == null) {
                    exception = e;
                }
            }

            try {
                if (mMuxer != null) {
                    //                    mMuxer.stop();
                    //                    mMuxer.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing muxer", e);
                if (exception == null) {
                    exception = e;
                }
            }

            try {
                if (mInputSurface != null) {
                    mInputSurface.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "error while releasing inputSurface", e);
                if (exception == null) {
                    exception = e;
                }
            }

            if (mVideoDecoderHandlerThread != null) {
                mVideoDecoderHandlerThread.quitSafely();
            }

            mVideoExtractor = null;
            mAudioExtractor = null;
            mOutputSurface = null;
            mInputSurface = null;
            mVideoDecoder = null;
            mAudioDecoder = null;
            mVideoEncoder = null;
            mAudioEncoder = null;
            mMuxer = null;
            mVideoDecoderHandlerThread = null;
        }

        if (exception != null) {
            throw exception;
        } else {
            if (progressListener != null) {
                progressListener.onCompleted();
            }
        }
    }

    private MediaExtractor createExtractor(String srcVideoFile) throws IOException {
        MediaExtractor extractor;
        extractor = new MediaExtractor();
        extractor.setDataSource(srcVideoFile);
        return extractor;
    }

    private static class CallbackHandler extends Handler {

        private MediaCodec mCodec;
        private boolean mEncoder;
        private MediaCodec.Callback mCallback;
        private String mMime;
        private boolean mSetDone;

        CallbackHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                mCodec = mEncoder ? MediaCodec.createEncoderByType(mMime) : MediaCodec.createDecoderByType(mMime);
            } catch (IOException ioe) {
                Log.e(TAG, "CallbackHandler handleMessage IOException:" + ioe.getMessage());
            }
            mCodec.setCallback(mCallback);
            synchronized (this) {
                mSetDone = true;
                notifyAll();
            }
        }

        void create(boolean encoder, String mime, MediaCodec.Callback callback) {
            mEncoder = encoder;
            mMime = mime;
            mCallback = callback;
            mSetDone = false;
            sendEmptyMessage(0);
            synchronized (this) {
                while (!mSetDone) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "CallbackHandler create InterruptedException:" + ie.getMessage());
                    }
                }
            }
        }

        MediaCodec getCodec() {
            return mCodec;
        }
    }

    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws IOException {

        mVideoDecoderHandlerThread = new HandlerThread("DecoderThread");
        mVideoDecoderHandlerThread.start();
        mVideoDecoderHandler = new CallbackHandler(mVideoDecoderHandlerThread.getLooper());
        final MediaCodec.Callback callback = new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                mDecoderOutputVideoFormat = codec.getOutputFormat();
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: output format changed: "
                            + mDecoderOutputVideoFormat);
                }
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {

                ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);
                while (!mVideoExtractorDone) {

                    int size = mVideoExtractor.readSampleData(decoderInputBuffer, 0);
                    long presentationTime = mVideoExtractor.getSampleTime();

                    if (VERBOSE) {
                        Log.d(TAG, "video extractor: returned buffer of size " + size);
                        Log.d(TAG, "video extractor: returned buffer for time " + presentationTime);
                    }

                    if (size >= 0) {
                        codec.queueInputBuffer(
                                index,
                                0,
                                size,
                                presentationTime,
                                mVideoExtractor.getSampleFlags());
                    }

                    mVideoExtractorDone = !mVideoExtractor.advance();
                    if (mVideoExtractorDone) {
                        if (VERBOSE) {
                            Log.d(TAG, "video extractor: EOS, fileIndex=" + fileIndex);
                        }
                        Log.i(TAG, "video decoder inputbuffer, fileIndex=" + fileIndex);
                    }

                    mVideoExtractedFrameCount++;
                    logState();
                    if (size >= 0) {
                        break;
                    }
                }
            }

            long dropedVideoSampleTime = 0;
            long presentationTime;

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned output buffer: " + index);
                    Log.d(TAG, "video decoder: returned buffer of size " + info.size);
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "video decoder: codec config buffer");
                    }
                    codec.releaseOutputBuffer(index, false);
                    return;
                }

                presentationTime = info.presentationTimeUs;
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned buffer for time " + presentationTime);
                }

                if (lastVideoSamplePresentationTime == 0) {
                    lastVideoSamplePresentationTime = presentationTime;
                }

                boolean render = info.size != 0;

                if (render && muxerController != null) {
                    if (muxerController.isDropFrame(info.presentationTimeUs)) {
                        dropedVideoSampleTime += info.presentationTimeUs - lastVideoSamplePresentationTime;
                        render = false;
                    }
                }

                if (presentationTime < lastVideoSamplePresentationTime) {
                    presentationTime += lastVideoSamplePresentationTime;
                }

                lastVideoSamplePresentationTime = presentationTime;

                codec.releaseOutputBuffer(index, render);
                if (render) {
                    mInputSurface.makeCurrent();

                    if (VERBOSE) {
                        Log.d(TAG, "output surface: await new image");
                    }

                    mOutputSurface.awaitNewImage();

                    if (VERBOSE) {
                        Log.d(TAG, "output surface: draw image");
                    }
                    mOutputSurface.drawImage(info.presentationTimeUs);

                    mInputSurface.setPresentationTime(presentationTime * 1000);

                    if (VERBOSE) {
                        Log.d(TAG, "input surface: swap buffers");
                    }
                    mInputSurface.swapBuffers();

                    if (VERBOSE) {
                        Log.d(TAG, "video encoder: notified of new frame");
                    }
                    mInputSurface.releaseEGLContext();
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "video decoder: EOS");
                    }
                    mVideoDecoderDone = true;
                    mVideoEncoder.signalEndOfInputStream();
                    if (fileIndex != fileSize - 1) {
                        fileIndex ++;
                        createNextDecoder();
                    }

                }

                mVideoDecodedFrameCount++;
                logState();
            }
        };

        mVideoDecoderHandler.create(false, getMimeTypeFor(inputFormat), callback);
        MediaCodec decoder = mVideoDecoderHandler.getCodec();
        decoder.setCallback(callback);
        decoder.configure(inputFormat, surface, null, 0);
        decoder.start();
        return decoder;
    }

    private void createNextDecoder() {
        try {
            mVideoExtractor = createExtractor(srcVideoFileList.get(fileIndex));
            int videoInputTrack = getAndSelectVideoTrackIndex(mVideoExtractor);
            inputFormat = mVideoExtractor.getTrackFormat(videoInputTrack);

            if (mWidth == -1 || mHeight == -1) {
                mWidth = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
                mHeight = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
            }

            int rotation = 0;
            if (inputFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                rotation = inputFormat.getInteger(MediaFormat.KEY_ROTATION);
            }

            mVideoDecoder = createVideoDecoder(inputFormat, mOutputSurface.getSurface());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private MediaCodec createVideoEncoder(
            MediaCodecInfo codecInfo,
            MediaFormat format,
            AtomicReference<Surface> surfaceReference) throws IOException {

        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.setCallback(new MediaCodec.Callback() {

            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                if (VERBOSE) {
                    Log.d(TAG, "video encoder: output format changed");
                }
                if (mOutputVideoTrack >= 0) {
                    Log.e(TAG, "video encoder changed its output format again?");
                }
                mEncoderOutputVideoFormat = codec.getOutputFormat();
                setupMuxer();
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE) {
                    Log.d(TAG, "video encoder: returned output buffer: " + index);
                    Log.d(TAG, "video encoder: returned buffer of size " + info.size);
                }
                muxVideo(index, info);
            }
        });

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surfaceReference.set(encoder.createInputSurface());
        encoder.start();
        videoEncoderRuning = true;
        return encoder;
    }

    private MediaCodec createAudioDecoder(MediaFormat inputFormat) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.setCallback(new MediaCodec.Callback() {

            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                mDecoderOutputAudioFormat = codec.getOutputFormat();
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: output format changed: " + mDecoderOutputAudioFormat);
                }
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
                ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);
                while (!mAudioExtractorDone) {

                    int size = mAudioExtractor.readSampleData(decoderInputBuffer, 0);
                    long presentationTime = mAudioExtractor.getSampleTime();
                    if (VERBOSE) {
                        Log.d(TAG, "audio extractor: returned buffer of size " + size);
                        Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);
                    }

                    if (size >= 0) {
                        codec.queueInputBuffer(
                                index,
                                0,
                                size,
                                presentationTime,
                                mAudioExtractor.getSampleFlags());
                    }

                    mAudioExtractorDone = !mAudioExtractor.advance();

                    if (mAudioExtractorDone) {
                        if (VERBOSE) {
                            Log.e(TAG, "audio extractor: EOS");
                        }
                        codec.queueInputBuffer(
                                index,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                    mAudioExtractedFrameCount++;
                    logState();
                    if (size >= 0) {
                        break;
                    }
                }
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned output buffer: " + index);
                }

                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned buffer of size " + info.size);
                }

                ByteBuffer decoderOutputBuffer = codec.getOutputBuffer(index);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (VERBOSE) {
                        Log.d(TAG, "audio decoder: codec config buffer");
                    }
                    codec.releaseOutputBuffer(index, false);
                    return;
                }

                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned buffer for time " + info.presentationTimeUs);
                }

                mAudioDecodedFrameCount++;

                mPendingAudioDecoderOutputBufferIndices.add(index);
                mPendingAudioDecoderOutputBufferInfos.add(info);

                logState();
                tryEncodeAudio();
            }
        });

        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        return decoder;
    }

    private MediaCodec createAudioEncoder(MediaCodecInfo codecInfo, MediaFormat format) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.setCallback(new MediaCodec.Callback() {

            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: output format changed");
                }
                if (mOutputAudioTrack >= 0) {
                    Log.e(TAG, "audio encoder changed its output format again?");
                }

                mEncoderOutputAudioFormat = codec.getOutputFormat();
                setupMuxer();
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned input buffer: " + index);
                }
                mPendingAudioEncoderInputBufferIndices.add(index);
                tryEncodeAudio();
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned output buffer: " + index);
                    Log.d(TAG, "audio encoder: returned buffer of size " + info.size);
                }
                muxAudio(index, info);
            }
        });

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        return encoder;
    }

    private long lastAudioSamplePresentationTime = 0;
    private long dropedAudioPresentationTime = 0;

    private void tryEncodeAudio() {

        if (mPendingAudioEncoderInputBufferIndices.size() == 0 ||
                mPendingAudioDecoderOutputBufferIndices.size() == 0) {
            return;
        }

        int decoderIndex = mPendingAudioDecoderOutputBufferIndices.poll();
        MediaCodec.BufferInfo info = mPendingAudioDecoderOutputBufferInfos.poll();
        int size = info.size;
        long presentationTime = info.presentationTimeUs;

        if (lastAudioSamplePresentationTime == 0) {
            lastAudioSamplePresentationTime = info.presentationTimeUs;
        }

        if (size >= 0) {

            boolean drop;
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                drop = false;
            } else {
                drop = muxerController != null && muxerController.isDropFrame(presentationTime);
            }

            if (drop) {
                dropedAudioPresentationTime += presentationTime - lastAudioSamplePresentationTime;
            }

            if (!drop) {

                if (dropedAudioPresentationTime > 0) {
                    presentationTime -= dropedAudioPresentationTime;
                }

                int encoderIndex = mPendingAudioEncoderInputBufferIndices.poll();

                ByteBuffer encoderInputBuffer = mAudioEncoder.getInputBuffer(encoderIndex);

                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: processing pending buffer: " + decoderIndex);
                }

                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: pending buffer of size " + size);
                    Log.d(TAG, "audio decoder: pending buffer for time " + presentationTime);
                }

                ByteBuffer decoderOutputBuffer = mAudioDecoder.getOutputBuffer(decoderIndex).duplicate();
                decoderOutputBuffer.position(info.offset);
                decoderOutputBuffer.limit(info.offset + size);
                encoderInputBuffer.position(0);
                encoderInputBuffer.put(decoderOutputBuffer);

                mAudioEncoder.queueInputBuffer(
                        encoderIndex,
                        0,
                        size,
                        presentationTime,
                        info.flags);
            }

        }

        lastAudioSamplePresentationTime = info.presentationTimeUs;
        mAudioDecoder.releaseOutputBuffer(decoderIndex, false);

        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {

            if (VERBOSE) {
                Log.e(TAG, "audio decoder: EOS");
            }

            mAudioDecoderDone = true;
        }

        logState();
    }

    private void setupMuxer() {
        if (!mMuxing && (!mCopyAudio || mEncoderOutputAudioFormat != null) && (!mCopyVideo || mEncoderOutputVideoFormat
                != null)) {
            if (mCopyVideo) {
                Log.d(TAG, "muxer: adding video track.");
                mOutputVideoTrack = mMuxer.addTrack(mEncoderOutputVideoFormat);
            }

            if (mCopyAudio) {
                Log.d(TAG, "muxer: adding audio track.");
                mOutputAudioTrack = mMuxer.addTrack(mEncoderOutputAudioFormat);
            }

            Log.d(TAG, "muxer: starting");
            mMuxer.start();
            mMuxing = true;

            MediaCodec.BufferInfo info;
            while ((info = mPendingVideoEncoderOutputBufferInfos.poll()) != null) {
                int index = mPendingVideoEncoderOutputBufferIndices.poll().intValue();
                muxVideo(index, info);
            }

            while ((info = mPendingAudioEncoderOutputBufferInfos.poll()) != null) {
                int index = mPendingAudioEncoderOutputBufferIndices.poll().intValue();
                muxAudio(index, info);
            }
        }
    }

    private void muxVideo(int index, MediaCodec.BufferInfo info) {
        if (!mMuxing) {
            mPendingVideoEncoderOutputBufferIndices.add(new Integer(index));
            mPendingVideoEncoderOutputBufferInfos.add(info);
            return;
        }

        ByteBuffer encoderOutputBuffer = mVideoEncoder.getOutputBuffer(index);
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {

            if (VERBOSE) {
                Log.d(TAG, "video encoder: codec config buffer");
            }

            mVideoEncoder.releaseOutputBuffer(index, false);
            return;
        }

        if (VERBOSE) {
            Log.d(TAG, "video encoder: returned buffer for time " + info.presentationTimeUs);
        }

        if (info.size != 0) {
            mMuxer.writeSampleData(mOutputVideoTrack, encoderOutputBuffer, info);
        }

        mVideoEncoder.releaseOutputBuffer(index, false);
        mVideoEncodedFrameCount++;
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 && fileIndex == fileSize - 1) {
            if (VERBOSE) {
                Log.d(TAG, "video encoder: EOS");
            }

            synchronized (this) {
                mVideoEncoderDone = true;
                notifyAll();
            }

        }

        logState();
    }

    private void muxAudio(int index, MediaCodec.BufferInfo info) {
        if (!mMuxing) {
            mPendingAudioEncoderOutputBufferIndices.add(new Integer(index));
            mPendingAudioEncoderOutputBufferInfos.add(info);
            return;
        }

        ByteBuffer encoderOutputBuffer = mAudioEncoder.getOutputBuffer(index);
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            if (VERBOSE) {
                Log.d(TAG, "audio encoder: codec config buffer");
            }

            mAudioEncoder.releaseOutputBuffer(index, false);
            return;
        }

        if (VERBOSE) {
            Log.d(TAG, "audio encoder: returned buffer for time " + info.presentationTimeUs);
        }

        if (info.size != 0) {
            mMuxer.writeSampleData(mOutputAudioTrack, encoderOutputBuffer, info);
        }

        mAudioEncoder.releaseOutputBuffer(index, false);
        mAudioEncodedFrameCount++;
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE) {
                Log.e(TAG, "audio encoder: EOS");
            }
            synchronized (this) {
                mAudioEncoderDone = true;
                notifyAll();
            }
        }
        logState();
    }

    private MediaMuxer createMuxer() throws IOException {
        return new MediaMuxer(mOutputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    private int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is "
                        + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is "
                        + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private MediaFormat mDecoderOutputVideoFormat = null;
    private MediaFormat mDecoderOutputAudioFormat = null;
    private MediaFormat mEncoderOutputVideoFormat = null;
    private MediaFormat mEncoderOutputAudioFormat = null;

    private int mOutputVideoTrack = -1;
    private int mOutputAudioTrack = -1;
    private boolean mVideoExtractorDone = false;
    private boolean mVideoDecoderDone = false;
    private boolean mVideoEncoderDone = false;
    private boolean mAudioExtractorDone = false;
    private boolean mAudioDecoderDone = false;
    private boolean mAudioEncoderDone = false;
    private LinkedList<Integer> mPendingAudioDecoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> mPendingAudioDecoderOutputBufferInfos;
    private LinkedList<Integer> mPendingAudioEncoderInputBufferIndices;

    private LinkedList<Integer> mPendingVideoEncoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> mPendingVideoEncoderOutputBufferInfos;
    private LinkedList<Integer> mPendingAudioEncoderOutputBufferIndices;
    private LinkedList<MediaCodec.BufferInfo> mPendingAudioEncoderOutputBufferInfos;

    private boolean mMuxing = false;

    private int mVideoExtractedFrameCount = 0;
    private int mVideoDecodedFrameCount = 0;
    private int mVideoEncodedFrameCount = 0;

    private int mAudioExtractedFrameCount = 0;
    private int mAudioDecodedFrameCount = 0;
    private int mAudioEncodedFrameCount = 0;

    private void logState() {
        if (VERBOSE) {
            Log.d(TAG, String.format(
                    "loop: "
                            + "V(%b){"
                            + "extracted:%d(done:%b) "
                            + "decoded:%d(done:%b) "
                            + "encoded:%d(done:%b)} "

                            + "A(%b){"
                            + "extracted:%d(done:%b) "
                            + "decoded:%d(done:%b) "
                            + "encoded:%d(done:%b) "

                            + "muxing:%b(V:%d,A:%d)",

                    mCopyVideo,
                    mVideoExtractedFrameCount, mVideoExtractorDone,
                    mVideoDecodedFrameCount, mVideoDecoderDone,
                    mVideoEncodedFrameCount, mVideoEncoderDone,

                    mCopyAudio,
                    mAudioExtractedFrameCount, mAudioExtractorDone,
                    mAudioDecodedFrameCount, mAudioDecoderDone,
                    mAudioEncodedFrameCount, mAudioEncoderDone,

                    mMuxing, mOutputVideoTrack, mOutputAudioTrack));
        }
    }

    private void awaitEncode() {
        synchronized (this) {
            while ((mCopyVideo && !mVideoEncoderDone) || (mCopyAudio && !mAudioEncoderDone)) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                    Log.e(TAG, "awaitEncode InterruptedException:" + ie.getMessage());
                }
            }
        }
    }

    private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }

    private static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }

    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

}
