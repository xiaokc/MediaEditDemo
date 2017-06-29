package com.android.cong.mediaeditdemo.audiomux;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by xiaokecong on 16/06/2017.
 */

public class MuxHelper {
    public static boolean decodeMusicFile(String musicFileUrl, String decodeFileUrl, int startSecond,
                                    int endSecond/*,
                                    Handler handler,
                                    DecodeOperateInterface decodeOperateInterface*/, int uniChannelNum, int
                                                  uniSampleRate) {
        int sampleRate = 0;
        int channelCount = 0;
        long duration = 0;
        String mime = null;

        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaFormat mediaFormat = null;
        MediaCodec mediaCodec = null;

        try {
            mediaExtractor.setDataSource(musicFileUrl);
        } catch (Exception e) {
            Log.e("===>xkc","设置解码音频文件路径错误");
            return false;
        }

        mediaFormat = mediaExtractor.getTrackFormat(0);
        sampleRate = mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
        channelCount = mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
        duration = mediaFormat.containsKey(MediaFormat.KEY_DURATION) ? mediaFormat.getLong
                (MediaFormat.KEY_DURATION)
                : 0;
        mime = mediaFormat.containsKey(MediaFormat.KEY_MIME) ? mediaFormat.getString(MediaFormat
                .KEY_MIME) : "";

        Log.i("歌曲信息",
                "Track info: mime:" + mime + " 采样率sampleRate:" + sampleRate + " channels:" +
                        channelCount + " duration:" + duration);

        if (TextUtils.isEmpty(mime) || !mime.startsWith("audio/")) {
            Log.e("解码文件不是音频文件", "mime:" + mime);
            return false;
        }

        if (mime.equals("audio/ffmpeg")) {
            mime = "audio/mpeg";
            mediaFormat.setString(MediaFormat.KEY_MIME, mime);
        }

        try {
            mediaCodec = MediaCodec.createDecoderByType(mime);

            mediaCodec.configure(mediaFormat, null, null, 0);
        } catch (Exception e) {
            Log.e("===>xkc","解码器configure出错");
            return false;
        }

        getDecodeData(mediaExtractor, mediaCodec, decodeFileUrl, sampleRate, channelCount,
                startSecond,
                endSecond/*, handler, decodeOperateInterface*/, uniChannelNum, uniSampleRate);
        return true;
    }


    private static void getDecodeData(MediaExtractor mediaExtractor, MediaCodec mediaCodec,
                               String decodeFileUrl, int sampleRate,
                               int channelCount, int startSecond, int endSecond /*,
                               Handler handler,
                               final DecodeOperateInterface decodeOperateInterface*/, int uniChannelNum, int
                                              uniSampleRate) {
        boolean decodeInputEnd = false;
        boolean decodeOutputEnd = false;

        int sampleDataSize;
        int inputBufferIndex;
        int outputBufferIndex;
        int byteNumber;

        long decodeNoticeTime = System.currentTimeMillis();
        long decodeTime;
        long presentationTimeUs = 0;

        final long timeOutUs = 100;
        final long startMicroseconds = startSecond * 1000 * 1000;
        final long endMicroseconds = endSecond;

        ByteBuffer[] inputBuffers;
        ByteBuffer[] outputBuffers;

        ByteBuffer sourceBuffer;
        ByteBuffer targetBuffer;

        MediaFormat outputFormat = mediaCodec.getOutputFormat();

        MediaCodec.BufferInfo bufferInfo;

        byteNumber = (outputFormat.containsKey("bit-width") ? outputFormat.getInteger("bit-width") : 0) / 8;

        mediaCodec.start();

        inputBuffers = mediaCodec.getInputBuffers();
        outputBuffers = mediaCodec.getOutputBuffers();

        mediaExtractor.selectTrack(0);

        bufferInfo = new MediaCodec.BufferInfo();

        BufferedOutputStream bufferedOutputStream = FileFunction.GetBufferedOutputStreamFromFile(decodeFileUrl);

        while (!decodeOutputEnd) {
            if (decodeInputEnd) {
                return;
            }

            decodeTime = System.currentTimeMillis();

//            if (decodeTime - decodeNoticeTime > Constant.OneSecond) {
//                final int decodeProgress =
//                        (int) ((presentationTimeUs - startMicroseconds) * Constant
//                                .NormalMaxProgress /
//                                       endMicroseconds);
//
//                if (decodeProgress > 0) {
//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            decodeOperateInterface.updateDecodeProgress(decodeProgress);
//                        }
//                    });
//                }
//
//                decodeNoticeTime = decodeTime;
//            }

            try {
                inputBufferIndex = mediaCodec.dequeueInputBuffer(timeOutUs);

                if (inputBufferIndex >= 0) {
                    sourceBuffer = inputBuffers[inputBufferIndex];

                    sampleDataSize = mediaExtractor.readSampleData(sourceBuffer, 0);

                    if (sampleDataSize < 0) {
                        decodeInputEnd = true;
                        sampleDataSize = 0;
                    } else {
                        presentationTimeUs = mediaExtractor.getSampleTime();
                    }

                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleDataSize,
                            presentationTimeUs,
                            decodeInputEnd ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!decodeInputEnd) {
                        mediaExtractor.advance();
                    }
                } else {
                    Log.i("===>xkc","inputBufferIndex:"+inputBufferIndex);
                }

                // decode to PCM and push it to the AudioTrack player
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeOutUs);

                if (outputBufferIndex < 0) {
                    switch (outputBufferIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            outputBuffers = mediaCodec.getOutputBuffers();
                            Log.i("===>xkc","MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            outputFormat = mediaCodec.getOutputFormat();

                            sampleRate = outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                                    outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) :
                                    sampleRate;
                            channelCount = outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                                    outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) :
                                    channelCount;
                            byteNumber = (outputFormat.containsKey("bit-width") ? outputFormat
                                    .getInteger("bit-width") : 0) / 8;

                            Log.i("===>xkc","MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.i("===>xkc","output try again later");
                            break;
                        default:
                            Log.i("===>xkc","dequeueOutputBuffer returned:"+outputBufferIndex);
                            break;
                    }
                    continue;
                }

                targetBuffer = outputBuffers[outputBufferIndex];

                byte[] sourceByteArray = new byte[bufferInfo.size];

                targetBuffer.get(sourceByteArray);
                targetBuffer.clear();

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    decodeOutputEnd = true;
                }

                if (sourceByteArray.length > 0 && bufferedOutputStream != null) {
                    if (presentationTimeUs < startMicroseconds) {
                        continue;
                    }

                    byte[] convertByteNumberByteArray = ConvertByteNumber(byteNumber, Constant
                                    .RecordByteNumber,
                            sourceByteArray);

                    byte[] resultByteArray =
                            ConvertChannelNumber(channelCount, uniChannelNum,
                                    Constant.RecordByteNumber,
                                    convertByteNumberByteArray);


                    try {
                        bufferedOutputStream.write(resultByteArray);
                    } catch (Exception e) {
                        Log.e("===>xkc","输出解压音频数据异常");
                    }
                }

                if (presentationTimeUs > endMicroseconds) {
                    break;
                }
            } catch (Exception e) {
                Log.e("===>xkc","getDecodeData异常");
            }
        }

        if (bufferedOutputStream != null) {
            try {
                bufferedOutputStream.close();
            } catch (IOException e) {
                Log.e("===>xkc","关闭bufferedOutputStream异常");
            }
        }

        if (sampleRate != uniSampleRate) {
            Resample(sampleRate, decodeFileUrl, uniSampleRate);
        }

        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }

        if (mediaExtractor != null) {
            mediaExtractor.release();
        }
    }

    private static byte[] ConvertByteNumber(int sourceByteNumber, int outputByteNumber, byte[]
            sourceByteArray) {
        if (sourceByteNumber == outputByteNumber) {
            return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceByteNumber) {
            case 1:
                switch (outputByteNumber) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte resultByte[];

                        for (int index = 0; index < sourceByteArrayLength; index += 1) {
                            resultByte = CommonFunction.GetBytes((short) (sourceByteArray[index]
                                                                                  * 256), Variable
                                    .isBigEnding);

                            byteArray[2 * index] = resultByte[0];
                            byteArray[2 * index + 1] = resultByte[1];
                        }

                        return byteArray;
                }
                break;
            case 2:
                switch (outputByteNumber) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        for (int index = 0; index < outputByteArrayLength; index += 1) {
                            byteArray[index] = (byte) (CommonFunction.GetShort(sourceByteArray[2
                                            * index],
                                    sourceByteArray[2 * index + 1], Variable.isBigEnding) / 256);
                        }

                        return byteArray;
                }
                break;
        }

        return sourceByteArray;
    }

    private static byte[] ConvertChannelNumber(int sourceChannelCount, int outputChannelCount,
                                               int byteNumber,
                                               byte[] sourceByteArray) {
        if (sourceChannelCount == outputChannelCount) {
            return sourceByteArray;
        }

        switch (byteNumber) {
            case 1:
            case 2:
                break;
            default:
                return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceChannelCount) {
            case 1:
                switch (outputChannelCount) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte firstByte;
                        byte secondByte;

                        switch (byteNumber) {
                            case 1:
                                for (int index = 0; index < sourceByteArrayLength; index += 1) {
                                    firstByte = sourceByteArray[index];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = firstByte;
                                }
                                break;
                            case 2:
                                for (int index = 0; index < sourceByteArrayLength; index += 2) {
                                    firstByte = sourceByteArray[index];
                                    secondByte = sourceByteArray[index + 1];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = secondByte;
                                    byteArray[2 * index + 2] = firstByte;
                                    byteArray[2 * index + 3] = secondByte;
                                }
                                break;
                        }

                        return byteArray;
                }
                break;
            case 2:
                switch (outputChannelCount) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        switch (byteNumber) {
                            case 1:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    short averageNumber =
                                            (short) ((short) sourceByteArray[2 * index] + (short)
                                                    sourceByteArray[2 *
                                                            index + 1]);
                                    byteArray[index] = (byte) (averageNumber >> 1);
                                }
                                break;
                            case 2:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    byte resultByte[] = CommonFunction.AverageShortByteArray
                                            (sourceByteArray[2 * index],
                                                    sourceByteArray[2 * index + 1],
                                                    sourceByteArray[2 *
                                                            index + 2],
                                                    sourceByteArray[2 * index + 3], Variable
                                                            .isBigEnding);

                                    byteArray[index] = resultByte[0];
                                    byteArray[index + 1] = resultByte[1];
                                }
                                break;
                        }

                        return byteArray;
                }
                break;
        }

        return sourceByteArray;
    }

    private static void Resample(int sampleRate, String decodeFileUrl, int uniSampleRate) {
        String newDecodeFileUrl = decodeFileUrl + "new";

        try {
            FileInputStream fileInputStream =
                    new FileInputStream(new File(decodeFileUrl));
            FileOutputStream fileOutputStream =
                    new FileOutputStream(new File(newDecodeFileUrl));

            // 使用开源工程SSRC 进行重采样
            new SSRC(fileInputStream, fileOutputStream, sampleRate, uniSampleRate,
                    Constant.RecordByteNumber, Constant.RecordByteNumber, 1, Integer.MAX_VALUE,
                    0, 0, true);

            fileInputStream.close();
            fileOutputStream.close();

            FileFunction.RenameFile(newDecodeFileUrl, decodeFileUrl);
        } catch (IOException e) {
            Log.e("===>xkc","关闭bufferedOutputStream异常");
        }
    }


    public static void ComposeAudio(String firstAudioFilePath, String secondAudioFilePath,
                                    String composeAudioFilePath, boolean deleteSource,
                                    float firstAudioWeight, float secondAudioWeight,
                                    int audioOffset,
                                    final ComposeAudioInterface composeAudioInterface, int uniSampleRate) {
        boolean firstAudioFinish = false;
        boolean secondAudioFinish = false;

        byte[] firstAudioByteBuffer;
        byte[] secondAudioByteBuffer;
        byte[] mp3Buffer;

        short resultShort;
        short[] outputShortArray;

        int index;
        int firstAudioReadNumber;
        int secondAudioReadNumber;
        int outputShortArrayLength;
        final int byteBufferSize = 1024;

        firstAudioByteBuffer = new byte[byteBufferSize];
        secondAudioByteBuffer = new byte[byteBufferSize];
        mp3Buffer = new byte[(int) (7200 + (byteBufferSize * 1.25))];

        outputShortArray = new short[byteBufferSize / 2];

        Handler handler = new Handler(Looper.getMainLooper());

        FileInputStream firstAudioInputStream = FileFunction.GetFileInputStreamFromFile
                (firstAudioFilePath);
        FileInputStream secondAudioInputStream = FileFunction.GetFileInputStreamFromFile
                (secondAudioFilePath);
        FileOutputStream composeAudioOutputStream = FileFunction.GetFileOutputStreamFromFile
                (composeAudioFilePath);

        // 使用开源工程Lame进行mp3合成
        LameUtil.init(uniSampleRate, Constant.LameBehaviorChannelNumber,
                Constant.BehaviorSampleRate, Constant.LameBehaviorBitRate, Constant.LameMp3Quality);
        Log.i("===>xkc","Lame init");

        try {
            while (!firstAudioFinish && !secondAudioFinish) {
                index = 0;

                if (audioOffset < 0) {
                    secondAudioReadNumber = secondAudioInputStream.read(secondAudioByteBuffer);

                    outputShortArrayLength = secondAudioReadNumber / 2;

                    for (; index < outputShortArrayLength; index++) {
                        resultShort = CommonFunction.GetShort(secondAudioByteBuffer[index * 2],
                                secondAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                        outputShortArray[index] = (short) (resultShort * secondAudioWeight);
                    }

                    audioOffset += secondAudioReadNumber;

                    if (secondAudioReadNumber < 0) {
                        secondAudioFinish = true;
                        break;
                    }

                    if (audioOffset >= 0) {
                        break;
                    }
                } else {
                    firstAudioReadNumber = firstAudioInputStream.read(firstAudioByteBuffer);

                    outputShortArrayLength = firstAudioReadNumber / 2;

                    for (; index < outputShortArrayLength; index++) {
                        resultShort = CommonFunction.GetShort(firstAudioByteBuffer[index * 2],
                                firstAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                        outputShortArray[index] = (short) (resultShort * firstAudioWeight);
                    }

                    audioOffset -= firstAudioReadNumber;

                    if (firstAudioReadNumber < 0) {
                        firstAudioFinish = true;
                        break;
                    }

                    if (audioOffset <= 0) {
                        break;
                    }
                }

                if (outputShortArrayLength > 0) {
                    int encodedSize = LameUtil.encode(outputShortArray, outputShortArray,
                            outputShortArrayLength, mp3Buffer);

                    if (encodedSize > 0) {
                        composeAudioOutputStream.write(mp3Buffer, 0, encodedSize);
                    }
                }
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (composeAudioInterface != null) {
                        composeAudioInterface.updateComposeProgress(20);
                    }
                }
            });

            while (!firstAudioFinish || !secondAudioFinish) {
                index = 0;

                firstAudioReadNumber = firstAudioInputStream.read(firstAudioByteBuffer);
                secondAudioReadNumber = secondAudioInputStream.read(secondAudioByteBuffer);

                int minAudioReadNumber = Math.min(firstAudioReadNumber, secondAudioReadNumber);
                int maxAudioReadNumber = Math.max(firstAudioReadNumber, secondAudioReadNumber);

                if (firstAudioReadNumber < 0) {
                    firstAudioFinish = true;
                }

                if (secondAudioReadNumber < 0) {
                    secondAudioFinish = true;
                }

                int halfMinAudioReadNumber = minAudioReadNumber / 2;

                outputShortArrayLength = maxAudioReadNumber / 2;

                for (; index < halfMinAudioReadNumber; index++) {
                    resultShort = CommonFunction.WeightShort(firstAudioByteBuffer[index * 2],
                            firstAudioByteBuffer[index * 2 + 1], secondAudioByteBuffer[index * 2],
                            secondAudioByteBuffer[index * 2 + 1], firstAudioWeight,
                            secondAudioWeight, Variable.isBigEnding);

                    outputShortArray[index] = resultShort;
                }

                if (firstAudioReadNumber != secondAudioReadNumber) {
                    if (firstAudioReadNumber > secondAudioReadNumber) {
                        for (; index < outputShortArrayLength; index++) {
                            resultShort = CommonFunction.GetShort(firstAudioByteBuffer[index * 2],
                                    firstAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                            outputShortArray[index] = (short) (resultShort * firstAudioWeight);
                        }
                    } else {
                        for (; index < outputShortArrayLength; index++) {
                            resultShort = CommonFunction.GetShort(secondAudioByteBuffer[index * 2],
                                    secondAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                            outputShortArray[index] = (short) (resultShort * secondAudioWeight);
                        }
                    }
                }

                if (outputShortArrayLength > 0) {
                    int encodedSize = LameUtil.encode(outputShortArray, outputShortArray,
                            outputShortArrayLength, mp3Buffer);

                    if (encodedSize > 0) {
                        composeAudioOutputStream.write(mp3Buffer, 0, encodedSize);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("===>xkc","ComposeAudio异常");

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (composeAudioInterface != null) {
                        composeAudioInterface.composeFail();
                    }
                }
            });

            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (composeAudioInterface != null) {
                    composeAudioInterface.updateComposeProgress(50);
                }
            }
        });

        Log.i("===>xkc","Lame encode");

        try {
            final int flushResult = LameUtil.flush(mp3Buffer);
            Log.i("===>xkc","Lame fluse");

            if (flushResult > 0) {
                composeAudioOutputStream.write(mp3Buffer, 0, flushResult);
            }
        } catch (Exception e) {
            Log.e("===>xkc","释放ComposeAudio LameUtil异常");
        } finally {
            try {
                composeAudioOutputStream.close();
            } catch (Exception e) {
                Log.e("===>xkc","关闭合成输出音频流异常");
            }

            LameUtil.close();
            Log.i("===>xkc","Lame close");
        }

        if (deleteSource) {
            FileFunction.DeleteFile(firstAudioFilePath);
            FileFunction.DeleteFile(secondAudioFilePath);
        }

        try {
            firstAudioInputStream.close();
            secondAudioInputStream.close();
        } catch (IOException e) {
            Log.e("===>xkc","关闭合成输入音频流异常");
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (composeAudioInterface != null) {
                    composeAudioInterface.composeSuccess();
                }
            }
        });
    }

}
