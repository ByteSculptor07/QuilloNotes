package com.github.bytesculptor07.quillo;

import android.os.Handler;
import android.util.Log;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.content.ClipData;
import android.view.View.DragShadowBuilder;
import android.view.GestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.DragEvent;
import android.content.Context;
import android.graphics.Color;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.util.List;

public class DragAndDropHandler {
    public static final class DragListener implements View.OnTouchListener {
        private GestureDetector gestureDetector;
        private View view;

        public interface TapListener {
            void onShortClick(View view);
        }

        private TapListener tapListener;

        public DragListener(Context context, TapListener listener) {
            this.tapListener = listener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    tapListener.onShortClick(view);
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    if (view != null) {
                        ClipData data = ClipData.newPlainText("", "");
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                        view.startDragAndDrop(data, shadowBuilder, view, 0); // Use startDragAndDrop for modern APIs
                        view.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }

        @Override
        public boolean onTouch(View v, MotionEvent motionEvent) {
            this.view = v; // Track the current view being touched
            gestureDetector.onTouchEvent(motionEvent);
            return true;
        }
    }
    
    public interface dropCallback {
        void onDrop(View page, View placeholder);
    }

    public static final class DropListener implements View.OnDragListener {
        private final List<View> placeholderViews;
        private final ScrollView scrollView;
        private View highlightedPlaceholder;
        private final Handler handler = new Handler();
        private dropCallback callback;

        private static final int SCROLL_SPEED = 10;
        private static final int SCROLL_DELAY = 1;

        public DropListener(List<View> placeholderViews, ScrollView scrollView) {
            this.placeholderViews = placeholderViews;
            this.scrollView = scrollView;
        }
        
        public void setCallback(dropCallback callback) {
            this.callback = callback;
        }
        
        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    float x = event.getX();
                    float y = event.getY();
                
                    highlightNearestPlaceholder(x - scrollView.getScrollX(), y - scrollView.getScrollY());
                    
                    int[] location = new int[2];
                    highlightedPlaceholder.getLocationOnScreen(location);
                    float pcYPos = location[1] + highlightedPlaceholder.getWidth() / 2.0f;
                    //debugView.setText("y: " + String.valueOf(y) + "           nearest-y: " + String.valueOf(pcYPos));
                
                    autoScrollIfNeeded(y);
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    // Reset visibility of the dragged view
                    View draggedView = (View) event.getLocalState();
                    if (draggedView != null) {
                        draggedView.setVisibility(View.VISIBLE);
                    }
                    
                    if (callback != null) {
                        callback.onDrop(draggedView, highlightedPlaceholder);
                    }
                
                    clearPlaceholderHighlight();
                    
                    return true;

                default:
                    return false;
            }
        }

        private void highlightNearestPlaceholder(float x, float y) {
            if (placeholderViews.isEmpty()) return;

            View nearest = null;
            float minDistance = Float.MAX_VALUE;

            for (View placeholder : placeholderViews) {
                int[] location = new int[2];
                placeholder.getLocationOnScreen(location);
                //float centerX = location[0] + placeholder.getWidth() / 2.0f;
                //float centerY = location[1] + placeholder.getHeight() / 2.0f;

                float distance = (float) Math.sqrt(Math.pow(location[0] - x, 2) + Math.pow(location[1] - y, 2));

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = placeholder;
                }
            }

            if (nearest != null && nearest != highlightedPlaceholder) {
                clearPlaceholderHighlight();
                nearest.setBackgroundColor(Color.parseColor("#FFA500"));
                highlightedPlaceholder = nearest;
            }
        }

        private void clearPlaceholderHighlight() {
            if (highlightedPlaceholder != null) {
                highlightedPlaceholder.setBackgroundColor(Color.TRANSPARENT);
                highlightedPlaceholder = null;
            }
        }
        
        private void autoScrollIfNeeded(float y) {
            float currentY = y - scrollView.getScrollY();
            float threshold = 50;
            if (currentY < threshold) {
                handler.postDelayed(() -> {
                    scrollView.smoothScrollBy(0, -SCROLL_SPEED * (int) (threshold - currentY) / 50);
                }, SCROLL_DELAY);
            } else if (currentY > (scrollView.getHeight() - threshold)) {
                handler.postDelayed(() -> {
                    scrollView.smoothScrollBy(0, SCROLL_SPEED * (int) (currentY - (scrollView.getHeight() - threshold)) / 50);
                }, SCROLL_DELAY);
            }
        }
    }
}