package com.android.cong.mediaeditdemo.opengls;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix2fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import com.android.cong.mediaeditdemo.R;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

/**
 * Created by xiaokecong on 20/06/2017.
 */

public class BitmapTextureRender implements GLSurfaceView.Renderer {
    private final String TAG = BitmapTextureRender.class.getSimpleName();
    private Context context;

    //每行的前两个是矩形的(x, y)坐标，后来两个为纹理(s, t)坐标。
    // 因为屏幕方向和纹理方向是上下颠倒的，所以矩形左下角(-0.5f,-0.8f)取的是纹理左上角(0f,1f)的颜色，矩形右下角取纹理右上角的颜色
    private static final float[] vertexData = {
            -1f, 1f, 0f, 0f,
            -1f, -1f, 0f, 1f,
            1f, 1f, 0f, 0f,
            1f, -1f, 1f, 1f
    };

    private final FloatBuffer vertexBuffer;

    private int textureId;

    private int program;
    private int a_Position;
    private int u_MVPMatrix;
    private int a_TextureCoordinates;
    private int u_TextureUnit;

    private final float[] mProjectionMatrix = new float[16]; // 投影矩阵
    private final float[] mCameraMatrix = new float[16]; // 相机矩阵
    private final float[] mMVPMatrix = new float[16];

    public BitmapTextureRender(Context context) {
        this.context = context;
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        textureId = BitmapTextureHelper.loadTexture(context, R.drawable.lookup_amatorka);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        int vertexShader = BitmapTextureHelper.loadShader(context, GLES20.GL_VERTEX_SHADER);
        int fragmentShader = BitmapTextureHelper.loadShader(context, GLES20.GL_FRAGMENT_SHADER);

        program = GLES20.glCreateProgram();
        BitmapTextureHelper.checkGLError(TAG, "glCreateProgram, program:" + program);

        GLES20.glAttachShader(program, vertexShader);
        BitmapTextureHelper.checkGLError(TAG, "glAttachShader: vertexShader");

        GLES20.glAttachShader(program, fragmentShader);
        BitmapTextureHelper.checkGLError(TAG, "glAttachShader: fragmentShader");

        GLES20.glLinkProgram(program);
        BitmapTextureHelper.checkGLError(TAG, "glLinkProgram");

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Log.i(TAG,"program:"+program);
        glUseProgram(program);
        BitmapTextureHelper.checkGLError(TAG, "glUseProgram");

        a_TextureCoordinates = GLES20.glGetAttribLocation(program, "a_TextureCoordinates");
        BitmapTextureHelper.checkGLError(TAG, "glGetAttribLocation:a_TextureCoordinates");
        if (a_Position == -1) {
            throw new RuntimeException("could not get a_TextureCoordinates");
        }

        a_Position = GLES20.glGetAttribLocation(program, "a_Position");
        BitmapTextureHelper.checkGLError(TAG, "glGetAttribLocation:a_Position");
        if (a_Position == -1) {
            throw new RuntimeException("could not get a_Position");
        }

        u_TextureUnit = GLES20.glGetUniformLocation(program, "u_TextureUnit");
        BitmapTextureHelper.checkGLError(TAG, "glGetUniformLocation:u_TextureUnit");
        if (a_Position == -1) {
            throw new RuntimeException("could not get u_TextureUnit");
        }

        //        u_MVPMatrix = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
        //        BitmapTextureHelper.checkGLError(TAG,"glGetUniformLocation:u_MVPMatrix");
        //        if (a_Position == -1) {
        //            throw new RuntimeException("could not get u_MVPMatrix");
        //        }

        //        float ratio = (float) height / width;
        //        Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -ratio, ratio, 3, 7);
        //        Matrix.setLookAtM(mCameraMatrix, 0, 0, 0, 3, 0, 0, 0, 1, 0, 0);
        //        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mCameraMatrix, 0);

        //激活纹理单元，GL_TEXTURE0代表纹理单元0，GL_TEXTURE1代表纹理单元1，以此类推。OpenGL使用纹理单元来表示被绘制的纹理
        glActiveTexture(GL_TEXTURE0);
        BitmapTextureHelper.checkGLError(TAG, "glActiveTexture");

        //绑定纹理到这个纹理单元
        glBindTexture(GL_TEXTURE_2D, textureId);
        BitmapTextureHelper.checkGLError(TAG, "glBindTexture");

        //把选定的纹理单元传给片段着色器中的u_TextureUnit，
        glUniform1i(u_TextureUnit, 0);
        BitmapTextureHelper.checkGLError(TAG, "glUniform1i");

        //传递矩形顶点坐标
        vertexBuffer.position(0);
        glVertexAttribPointer(a_Position, 2, GL_FLOAT, false, 2 * 4, vertexBuffer);
        BitmapTextureHelper.checkGLError(TAG, "glVertexAttribPointer:a_Position");
        glEnableVertexAttribArray(a_Position);
        BitmapTextureHelper.checkGLError(TAG, "glEnableVertexAttribArray:a_Position");

        //传递纹理坐标
        vertexBuffer.position(2);
        glVertexAttribPointer(a_TextureCoordinates, 2, GL_FLOAT, false, 2 * 4, vertexBuffer);
        BitmapTextureHelper.checkGLError(TAG, "glVertexAttribPointer:a_TextureCoordinates");
        glEnableVertexAttribArray(a_TextureCoordinates);
        BitmapTextureHelper.checkGLError(TAG, "glEnableVertexAttribArray:a_TextureCoordinates");

        //        Matrix.setIdentityM(mMVPMatrix,0);
        //        glUniformMatrix4fv(u_MVPMatrix, 1, false, mMVPMatrix, 0);
        //        BitmapTextureHelper.checkGLError(TAG,"glUniformMatrix4fv:u_MVPMatrix");

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        BitmapTextureHelper.checkGLError(TAG, "glDrawArrays");
        glDisableVertexAttribArray(a_Position);
        BitmapTextureHelper.checkGLError(TAG, "glDisableVertexAttribArray:a_Position");
        glDisableVertexAttribArray(a_TextureCoordinates);
        BitmapTextureHelper.checkGLError(TAG, "glDisableVertexAttribArray:a_Position");

    }

}
