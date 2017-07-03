package com.android.cong.mediaeditdemo.videomux;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

public class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "OutputSurface";
    private static final boolean VERBOSE = false;

    private static final int EGL_OPENGL_ES2_BIT = 4;

    private EGL10 mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private Object mFrameSyncObject = new Object();
    private boolean mFrameAvailable;

    private TextureRender mTextureRender = new TextureRender();

    public OutputSurface() {
    }

    public void setup(int width, int height, int roatation) {

        mTextureRender.surfaceCreated(width, height, roatation);

        if (VERBOSE) {
            Log.d(TAG, "textureID=" + mTextureRender.getTextureId());
        }

        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mSurface = new Surface(mSurfaceTexture);
    }

    public void release() {
        if (mEGL != null) {
            if (mEGL.eglGetCurrentContext().equals(mEGLContext)) {
                mEGL.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            }

            mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
            mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
        }

        mSurface.release();

        mEGLDisplay = null;
        mEGLContext = null;
        mEGLSurface = null;
        mEGL = null;

        mTextureRender = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void awaitNewImage() {
        final int TIMEOUT_MS = 500;

        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }

        mSurfaceTexture.updateTexImage();
    }

    public void drawImage(long pts) {
        mTextureRender.drawFrame(mSurfaceTexture, pts);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        if (VERBOSE) {
            Log.d(TAG, "new frame available");
        }

        synchronized (mFrameSyncObject) {

            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }

            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }

}
