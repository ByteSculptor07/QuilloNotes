package com.github.bytesculptor07.quillo.gl;

import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import javax.microedition.khronos.egl.*;

public class MultisampleConfigChooser implements GLSurfaceView.EGLConfigChooser {
    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int[] configSpec = {
            EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_SAMPLE_BUFFERS, 1, // <-- enable multisampling
            EGL10.EGL_SAMPLES, 4,         // <-- request 4x MSAA
            EGL10.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!egl.eglChooseConfig(display, configSpec, configs, 1, numConfigs)) {
            throw new IllegalArgumentException("Failed to choose EGL config");
        }
        return configs[0];
    }
}