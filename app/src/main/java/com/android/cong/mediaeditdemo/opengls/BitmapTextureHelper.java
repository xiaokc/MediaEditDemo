package com.android.cong.mediaeditdemo.opengls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.android.cong.mediaeditdemo.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Created by xiaokecong on 23/06/2017.
 */

public class BitmapTextureHelper {
    private static final String TAG = BitmapTextureHelper.class.getSimpleName();

    public static int loadTexture(Context context, int resId) {
        int[] textureObjectIds = new int[1];

        // 生成纹理
        GLES20.glGenTextures(1, textureObjectIds, 0);
        checkGLError(TAG, "glGenTextures");

        if (textureObjectIds[0] == 0) {
            Log.e("===>xkc", "创建纹理对象失败");
            return 0;
        }

        // 加载纹理资源，解码成bitmap形式
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);

        if (null == bitmap) {
            Log.e("===>xkc", "bitmap decode failed");
            GLES20.glDeleteTextures(1, textureObjectIds, 0);
            return 0;
        }

        // 两个参数说明：2D 纹理，OpenGL 要绑定的纹理对象ID，后面的纹理调用使用此纹理对象
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0]);
        checkGLError(TAG, "glBindTexture textureid:" + textureObjectIds[0]);

        // 设置纹理过滤参数
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGLError(TAG,"glTexParameter");

        // 加载实际纹理图像数据到OpenGL ES 的纹理对象
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        checkGLError(TAG, "texImage2D");

        bitmap.recycle();
        // 为纹理生成MIP贴图，提高渲染性能，但会占用较多内存
        //        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        //        checkGLError(TAG,"glGenerateMipmap");

        // 纹理加载完，不需要再绑定此纹理了
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        checkGLError(TAG, "glBindTexture 0");

        Log.i(TAG, "texture id is:" + textureObjectIds[0]);
        return textureObjectIds[0];

    }

    public static int loadShader(Context context, int type) {
        int shader = GLES20.glCreateShader(type);
        checkGLError(TAG, "glCreateShader,type:" + type);
        String shaderStr = "";
        if (type == GLES20.GL_VERTEX_SHADER) {
            shaderStr = readFromResource(context, R.raw.bitmap_texture_vertex);
        } else if (type == GLES20.GL_FRAGMENT_SHADER) {
            shaderStr = readFromResource(context, R.raw.bitmap_texture_fragment);
        }

        Log.i(TAG, "shaderStr:" + shaderStr);
        GLES20.glShaderSource(shader, shaderStr);
        checkGLError(TAG, "glShaderSource:" + shaderStr);

        GLES20.glCompileShader(shader);
        checkGLError(TAG, "glCompileShader");
        return shader;
    }

    private static String readFromResource(Context context, int resId) {
        StringBuilder body = new StringBuilder();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resId)));

        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                body.append(line);
                body.append("\n");

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return body.toString();

    }

    public static void checkGLError(String TAG, String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
}
