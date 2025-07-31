package com.github.bytesculptor07.quillo.gl;

import android.opengl.GLSurfaceView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class QuDrawingView extends GLSurfaceView {
    QuRenderer mRenderer;

    public QuDrawingView(Context context) {
        super(context);
        init(context);
    }
    
    public QuDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        setEGLContextClientVersion(3);
        mRenderer = new QuRenderer();
        setEGLConfigChooser(new MultisampleConfigChooser());
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mRenderer.handleTouch(e, this);
        
        // redraw Canvas
        //requestRender();
        return true;
    }
}