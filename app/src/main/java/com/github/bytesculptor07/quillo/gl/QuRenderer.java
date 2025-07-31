package com.github.bytesculptor07.quillo.gl;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.util.List;
import java.util.ArrayList;


public class QuRenderer implements GLSurfaceView.Renderer {
    private TriangleBuffer triangleBuffer;

    private int screenWidth;
    private int screenHeight;


    float[] p1 = null;
    float[] p2 = null;

    private List<float[]> points = new ArrayList<>();
    private float lastPressure = 1.0f;


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(1f, 1f, 1f, 1f);

        // Turn on smoothing
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        triangleBuffer = new TriangleBuffer();
        triangleBuffer.init();

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        triangleBuffer.draw(screenWidth, screenHeight);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        screenWidth = width;
        screenHeight = height;
    }

    private void addStroke(float[] startPoint, float[] endPoint, float pressure, GLSurfaceView view) {
        view.queueEvent(() -> {
            calcTriangles(startPoint, endPoint, pressure);
        });
    }

    private void calcTriangles(float[] startPoint, float[] endPoint, float pressure) {
        float strokeWidth = 0.01f * pressure;

        List<float[]> points = pointMath.calcPoints(startPoint, endPoint, strokeWidth);

        if (p1 == null && p2 == null) {
            p1 = points.get(0);
            p2 = points.get(1);
        }

        float[] p3 = points.get(2);
        float[] p4 = points.get(3);


        float[] newCoords = {p1[0], p1[1], 0f, p2[0], p2[1], 0f, p3[0], p3[1], 0f};
        float[] newCoords2 = {p3[0], p3[1], 0f, p4[0], p4[1], 0f, p1[0], p1[1], 0f};

        triangleBuffer.addTriangle(newCoords);
        triangleBuffer.addTriangle(newCoords2);

        p1 = p4;
        p2 = p3;
    }


    public void handleTouch(MotionEvent event, GLSurfaceView view) {
        if (screenWidth == 0 || screenHeight == 0) return;

        final float x = (((event.getX() / screenWidth) * 2 - 1) * (float) screenWidth / screenHeight);
        final float y = -((event.getY() / screenHeight) * 2 - 1);

        float[] point = new float[]{x, y};
        float pressure = event.getPressure();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                points.clear();
                points.add(point);
                lastPressure = pressure;
                break;

            case MotionEvent.ACTION_MOVE:
                points.add(point);
                if (points.size() >= 4) {
                    int last = points.size() - 1;

                    double distance = Math.sqrt(
                            Math.pow(points.get(last - 1)[0] - points.get(last)[0], 2) +
                                    Math.pow(points.get(last - 1)[1] - points.get(last)[1], 2)
                    );

                    //Log.d("Quillo", "distance:" + (int) (distance * 100));

                    //int steps = Math.max((int) (distance * 100), 1); // adjust the number here
                    int steps = 10;

                    drawSmoothSegment(
                            points.get(last - 3),
                            points.get(last - 2),
                            points.get(last - 1),
                            points.get(last),
                            pressure,
                            steps,
                            view
                    );
                }

                lastPressure = pressure;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                points.clear();
                p1 = null;
                p2 = null;
                break;
        }
    }

    private void drawSmoothSegment(float[] p0, float[] p1, float[] p2, float[] p3, float pressure, int steps, GLSurfaceView view) {
        // Convert Catmull-Rom to Bezier
        float[] c1 = new float[]{
                p1[0] + (p2[0] - p0[0]) / 6f,
                p1[1] + (p2[1] - p0[1]) / 6f
        };

        float[] c2 = new float[]{
                p2[0] - (p3[0] - p1[0]) / 6f,
                p2[1] - (p3[1] - p1[1]) / 6f
        };

        int stepCount = steps;

        // Draw Bezier curve from p1 to p2
        float pressureStep = (pressure - lastPressure) / stepCount;
        float[] lastPoint = p1;
        for (int i = 1; i <= stepCount; i++) {
            float t = i / (float) stepCount;
            float x = bezier(t, p1[0], c1[0], c2[0], p2[0]);
            float y = bezier(t, p1[1], c1[1], c2[1], p2[1]);
            float[] newPoint = new float[]{x, y};
            addStroke(lastPoint, newPoint, lastPressure + pressureStep * i, view);
            lastPoint = newPoint;
        }
    }


    private float bezier(float t, float p0, float c1, float c2, float p1) {
        return (float) (Math.pow(1 - t, 3) * p0 +
                3 * Math.pow(1 - t, 2) * t * c1 +
                3 * (1 - t) * Math.pow(t, 2) * c2 +
                Math.pow(t, 3) * p1);
    }

}