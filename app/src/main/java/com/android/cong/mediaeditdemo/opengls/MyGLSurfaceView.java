package com.android.cong.mediaeditdemo.opengls;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

/**
 * Created by xiaokecong on 13/06/2017.
 */

public class MyGLSurfaceView extends GLSurfaceView {
    private final MyRenderer mRenderer;
    public MyGLSurfaceView(Context context) {
        super(context);

        // create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyRenderer();

        // set the renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        // render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


    }
}
