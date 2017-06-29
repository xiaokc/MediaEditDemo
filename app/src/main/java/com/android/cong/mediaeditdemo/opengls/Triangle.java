package com.android.cong.mediaeditdemo.opengls;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;

/**
 * Created by xiaokecong on 13/06/2017.
 */

public class Triangle {
    private FloatBuffer vertexBuffer;

    // number of coordinates per vertex in this way
    static final int COORDS_PER_VERTEX = 3;
    static float traingleCoords[] = { // in counter-clockwise order
            0.0f, 0.622008459f, 0.0f, // top
            -0.5f, -0.311004243f, 0.0f, // bottom left
            0.5f, -0.311004243f, 0.0f  // bottom right
    };

    // set color with red, green, blue and alpha values
    float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    private final String vertexShader = "attribute vec4 vPosition; "
            + "void main(){"
            + "gl_Position=vPosition;"
            + "}";

    private final String fragmentShader = "precision mediump float; "
            + "uniform vec4 vColor; "
            + "void main(){gl_FragColor=vColor;}";

    public Triangle() {
        // 初始化顶点的byte buffer for shape coordinates

        // number of coordinate values * 4 bytes per float
        ByteBuffer bb = ByteBuffer.allocateDirect(traingleCoords.length * 4);

        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from vertexBuffer
        vertexBuffer = bb.asFloatBuffer();

        // add the coordinates to the FloatBuffer
        vertexBuffer.put(traingleCoords);

        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

//        int vertexShader = MyRenderer.loadShader(GLES20.GL_VERTEX_SHADER)
    }
}
