package com.android.cong.mediaeditdemo.videomux;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.android.cong.mediaeditdemo.R;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

public class TextureRender {
    private static final String TAG = "TextureRender";

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mTriangleVerticesData = {
            -1f, -1f, 0f, 0f, 0f,
            1f, -1f, 0f, 1f, 0f,
            -1f, 1f, 0f, 0f, 1f,
            1f, 1f, 0f, 1f, 1f};

    private FloatBuffer mTriangleVertices;
    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];

    private int mTextureID = -12345;
    private int mOverlayTextureId;

    private int mProgram;

    private int glsluMVPMatrix;
    private int glsluSTMatrix;
    private int glslAPosition;
    private int glslATexture;
    private int glslOverlayCoord;

    private int glslOverFlag;
    private int glslOverlayTexture;



    public TextureRender() {
        mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);
    }

    public int getTextureId() {
        return mTextureID;
    }



    public void surfaceCreated(int width, int height, int rotation) {

        String VERTEX_SHADER = GLHelper.getVectexShaderString(R.raw.videoeditor_verctex_shader);
        String FRAGMENT_SHADER = GLHelper.getFragmentShaderString(0);

        mProgram = GLHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program");
        }

        glslAPosition = GLES20.glGetAttribLocation(mProgram, "aPosition");
        GLHelper.checkGlError(TAG, "glGetAttribLocation aPosition");
        if (glslAPosition == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }

        glslATexture = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        GLHelper.checkGlError(TAG, "glGetAttribLocation aTextureCoord");
        if (glslATexture == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        glsluMVPMatrix = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLHelper.checkGlError(TAG, "glGetUniformLocation uMVPMatrix");
        if (glsluMVPMatrix == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        glsluSTMatrix = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        GLHelper.checkGlError(TAG, "glGetUniformLocation uSTMatrix");
        if (glsluSTMatrix == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        glslOverlayTexture = GLES20.glGetUniformLocation(mProgram, "oTexture");
        GLHelper.checkGlError(TAG, "glGetUniformLocation oTexture");
        if (glslOverlayTexture == -1) {
            throw new RuntimeException("Could not get attrib location for overlayFlag");
        }

        glslOverFlag = GLES20.glGetUniformLocation(mProgram, "overlayFlag");
        GLHelper.checkGlError(TAG, "glGetUniformLocation overlayFlag");
        if (glslOverFlag == -1) {
            throw new RuntimeException("Could not get attrib location for overlayFlag");
        }

        glslOverlayCoord = GLES20.glGetAttribLocation(mProgram, "aOverlayCoord");
        GLHelper.checkGlError(TAG, "glGetAttribLocation aOverlayCoord");
        if (glslOverlayCoord == -1) {
            throw new RuntimeException("Could not get attrib location for aOverlayCoord");
        }

        int[] textures = new int[2];
        GLES20.glGenTextures(2, textures, 0);

        mTextureID = textures[0];
        mOverlayTextureId = textures[1];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLHelper.checkGlError(TAG, "glBindTexture mTextureID");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLHelper.checkGlError(TAG, "glTexParameter");


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOverlayTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

    }

    public void drawFrame(SurfaceTexture st, long pts) {

        GLHelper.checkGlError(TAG, "onDrawFrame start");
        st.getTransformMatrix(mSTMatrix);

        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);



        int program = mProgram;

        GLES20.glUseProgram(program);
        GLHelper.checkGlError(TAG, "glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(glslAPosition,
                                     3,
                                     GLES20.GL_FLOAT,
                                     false,
                                     TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                                     mTriangleVertices);
        GLHelper.checkGlError(TAG, "glVertexAttribPointer maPosition");

        GLES20.glEnableVertexAttribArray(glslAPosition);
        GLHelper.checkGlError(TAG, "glEnableVertexAttribArray glslAPosition");

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(glslATexture,
                                     2,
                                     GLES20.GL_FLOAT,
                                     false,
                                     TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                                     mTriangleVertices);
        GLHelper.checkGlError(TAG, "glVertexAttribPointer glslATexture");
        GLES20.glEnableVertexAttribArray(glslATexture);
        GLHelper.checkGlError(TAG, "glEnableVertexAttribArray glslATexture");

        Matrix.setIdentityM(mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(glsluMVPMatrix, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(glsluSTMatrix, 1, false, mSTMatrix, 0);



        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLHelper.checkGlError(TAG, "glDrawArrays");
        GLES20.glFinish();
    }

}
