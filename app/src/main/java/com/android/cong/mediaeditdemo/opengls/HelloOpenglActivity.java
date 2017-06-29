package com.android.cong.mediaeditdemo.opengls;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.android.cong.mediaeditdemo.R;

import android.app.Activity;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;

/**
 * Created by xiaokecong on 13/06/2017.
 */

public class HelloOpenglActivity extends Activity {
    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_opengl);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surfaceview);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new MyRenderer());
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private static class MyRenderer implements GLSurfaceView.Renderer {
        private static final String VERTEX_SHADER = "attribute vec4 vPosition; "
                + "uniform mat4 uMVPMatrix;"
                + "void main(){gl_Position=uMVPMatrix * vPosition;}";
        private static final String FRAGMENT_SHADER = "precision mediump float; "
                + "void main(){gl_FragColor=vec4(0.5,0,0,1);}";
        private static final float[] VERTEX = { // 逆时针方向
                0, 1, 0, // 顶部的点
                -0.5f, -1, 0, // 左下角的点
                1, -1, 0, // 右下角的点
        };

        private final FloatBuffer mVertexBuffer;
        private final float[] mProjectionMatrix = new float[16]; // 投影矩阵
        private final float[] mCameraMatrix = new float[16]; // 相机矩阵
        private final float[] mMVPMatrix = new float[16];
        private float[] mRotationMatrix = new float[16]; // 旋转矩阵

        private int mProgram;
        private int mPositionHandler;
        private int mMatrixHandler;

        MyRenderer() {
            mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(VERTEX);
            mVertexBuffer.position(0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mProgram = GLES20.glCreateProgram();
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            mPositionHandler = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

            float ratio = (float) height / width;
            Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -ratio, ratio, 3, 7);
            Matrix.setLookAtM(mCameraMatrix, 0, 0, 0, 3, 0, 0, 0, 1, 0, 0);

            long time = SystemClock.uptimeMillis() % 4000L;
            float angle = 0.090f * ((int) time);
            Matrix.setRotateM(mRotationMatrix, 0, angle, 0, 0, -1.0f);

            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mRotationMatrix, 0);

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);
            GLES20.glEnableVertexAttribArray(mPositionHandler);
            GLES20.glVertexAttribPointer(mPositionHandler, 3, GLES20.GL_FLOAT, false, 12, mVertexBuffer);

            GLES20.glUniformMatrix4fv(mMatrixHandler, 1, false, mMVPMatrix, 0);

            gl.glRotatef(0f, 0f,1f,0f);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
            GLES20.glDisableVertexAttribArray(mPositionHandler);
        }

        static int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }
}
