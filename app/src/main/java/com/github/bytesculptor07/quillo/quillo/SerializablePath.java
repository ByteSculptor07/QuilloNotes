package com.github.bytesculptor07.quillo;

import android.content.Context;
import android.graphics.Path;
import java.util.ArrayList;
import java.io.Serializable;
import android.graphics.Color;

public class SerializablePath extends Path implements Serializable {
    public static final int STATE_UNDEFINED = 0;
    public static final int STATE_PX = 1;
    public static final int STATE_DP = 2;

    private ArrayList<float[]> pathPoints;
    private int color; // Declare a field for the color
    private int tool;
    private int width;
    private String shape;
    private float offsetX;
    private float offsetY;
    private int conversionState = STATE_UNDEFINED;

    // Default constructor
    public SerializablePath() {
        super();
        pathPoints = new ArrayList<float[]>();
        color = Color.BLACK; // Default color (black)
        shape = null;
        offsetX = 0;
        offsetY = 0;
    }

    // Copy constructor
    public SerializablePath(SerializablePath p) {
        super(p);
        pathPoints = new ArrayList<>();
        for (float[] points : p.pathPoints) {
            pathPoints.add(points.clone());
        }
        color = p.color;
        shape = p.shape;
        offsetX = p.offsetX;
        offsetY = p.offsetY;
        tool = p.tool;
        width = p.width;
        conversionState = p.conversionState;
    }


    public void setOffset(float x, float y) {
        offsetX = x;
        offsetY = y;
        this.offset(x, y);
        for (float[] pointSet : pathPoints) {
            /*
            if (pointSet.length == 2) {
                pointSet[0] += offsetX;
                pointSet[1] += offsetY;
            } else if (pointSet.length == 4) {
                pointSet[0] += offsetX;
                pointSet[1] += offsetY;
                pointSet[2] += offsetX;
                pointSet[3] += offsetY;
            }
            */
            for (int i = 0; i < pointSet.length-1; i += 2) {
                pointSet[i] += offsetX;
                pointSet[i + 1] += offsetY;
            }
        }
    }
    
    public float[] getOffset() {
        float result[] = new float[2];
        result[0] = offsetX;
        result[1] = offsetY;
        return result;
    }
    
    public ArrayList<float[]> getPathPoints() {
        return pathPoints;
    }
    
    public void setPathPoints(ArrayList<float[]> pathPoints) {
        this.pathPoints = pathPoints;
    }

    // Getter for the color
    public int getColor() {
        return color;
    }

    // Setter for the color
    public void setColor(int color) {
        this.color = color;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public String getShape() {
        return this.shape;
    }
    
    public void setShape(String shape) {
        this.shape = shape;
    }
    
    public int getTool() {
        return tool;
    }
    
    public void setTool(int tool) {
        this.tool = tool;
    }

    // Add path points
    public void addPathPoints(float[] points) {
        this.pathPoints.add(points);
    }
    
    public void removeLastPoint() {
        pathPoints.remove(pathPoints.size() - 1);
    }
    
    public void loadPoints(Context context) {
        if (shape == null) {
            loadPathPointsAsQuadTo();
        } else if (shape.equalsIgnoreCase("arrow")) {
            loadPathPointsAsLineTo();
        }
    }

    // Load path points as QuadTo
    public void loadPathPointsAsQuadTo() {
        if (!pathPoints.isEmpty()) {
            float[] initPoints = pathPoints.get(0);
            this.moveTo(initPoints[0], initPoints[1]);

            ArrayList<float[]> copyOfPathPoints = new ArrayList<>(pathPoints);
            copyOfPathPoints.remove(0);

            for (float[] pointSet : copyOfPathPoints) {
                this.quadTo(pointSet[0], pointSet[1], pointSet[2], pointSet[3]);
            }
            //this.offset(offsetX, offsetY);
        }
    }
    
    public void loadPathPointsAsLineTo() {
        if (!pathPoints.isEmpty()) {
            float[] initPoints = pathPoints.get(0);
            float[] endPoints = pathPoints.get(pathPoints.size()-1);
            this.moveTo(initPoints[0], initPoints[1]);
            this.lineTo(endPoints[0], endPoints[1]);
        }
    }

    public void convertToDp(Context context) {
        if (conversionState == STATE_DP) return; // Already in dp
        conversionState = STATE_DP;
        float density = context.getResources().getDisplayMetrics().density;
        for (float[] points : pathPoints) {
            for (int i = 0; i < points.length; i++) {
                points[i] = points[i] / density;
            }
        }
        width = (int) (width / density);
    }


    public void convertToPx(Context context) {
        if (conversionState == STATE_PX) return; // Already in px
        conversionState = STATE_PX;
        float density = context.getResources().getDisplayMetrics().density;
        for (float[] points : pathPoints) {
            for (int i = 0; i < points.length; i++) {
                points[i] = points[i] * density;
            }
        }
        width = (int) (width * density);
    }
}