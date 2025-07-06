package com.github.bytesculptor07.quillo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ScrollView;
import com.otaliastudios.zoom.ZoomLayout;

public class CustomScrollView extends ZoomLayout {

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private OnScrollListener scrollListener;
    private OnZoomListener zoomListener;

    public CustomScrollView(Context context) {
        super(context);
        init(context);
    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float zoom = getRealZoom();
                if (scrollListener != null && zoom > 0.5) {
                    scrollListener.onScroll(distanceX, distanceY);
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            // Disable scrolling and scaling for stylus
            return false;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float zoom = getRealZoom();
            if (zoomListener != null && zoom > 0.5) {
                zoomListener.onZoom(scaleFactor);
            }
            return true;
        }
    }

    // Listener interfaces
    public interface OnScrollListener {
        void onScroll(float distanceX, float distanceY);
    }

    public interface OnZoomListener {
        void onZoom(float scaleFactor);
    }

    // Setters for listeners
    public void setOnScrollListener(OnScrollListener listener) {
        this.scrollListener = listener;
    }

    public void setOnZoomListener(OnZoomListener listener) {
        this.zoomListener = listener;
    }
}