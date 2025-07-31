package com.github.bytesculptor07.quillo.gl;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class pointMath {
    public static float[] setDistance(float x0, float y0, float x2, float y2, float distance) {
        float dx = x2 - x0;
        float dy = y2 - y0;
        float currentDistance = (float) Math.sqrt(dx * dx + dy * dy);
        if (currentDistance == 0) {
            return new float[] {x0, y0};
        }
        
        float ux = dx / currentDistance;
        float uy = dy / currentDistance;
        float newX = x0 + distance * ux;
        float newY = y0 + distance * uy;
        return new float[] {newX, newY};
    }
    
    public static float[] rotatePoint(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
    
        float rotatedX = -dy;
        float rotatedY = dx;
    
        float x2 = rotatedX + x0;
        float y2 = rotatedY + y0;
    
        return new float[]{x2, y2};
    }
    
    public static float[] rotatePointClockwise(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
    
        float rotatedX = dy;
        float rotatedY = -dx;
    
        float x2 = rotatedX + x0;
        float y2 = rotatedY + y0;
    
        return new float[]{x2, y2};
    }
    
    public static List<float[]> calcPoints(float[] startPoint, float[] endPoint, float strokeWidth) {
        float[] rotatedPoint1 = pointMath.rotatePoint(startPoint[0], startPoint[1], endPoint[0], endPoint[1]);
        float[] p1 = pointMath.setDistance(startPoint[0], startPoint[1], rotatedPoint1[0], rotatedPoint1[1], strokeWidth);
        
        float[] rotatedPoint2 = pointMath.rotatePointClockwise(startPoint[0], startPoint[1], endPoint[0], endPoint[1]);
        float[] p2 = pointMath.setDistance(startPoint[0], startPoint[1], rotatedPoint2[0], rotatedPoint2[1], strokeWidth);
        
        float[] rotatedPoint3 = pointMath.rotatePoint(endPoint[0], endPoint[1], startPoint[0], startPoint[1]);
        float[] p3 = pointMath.setDistance(endPoint[0], endPoint[1], rotatedPoint3[0], rotatedPoint3[1], strokeWidth);
        
        float[] rotatedPoint4 = pointMath.rotatePointClockwise(endPoint[0], endPoint[1], startPoint[0], startPoint[1]);
        float[] p4 = pointMath.setDistance(endPoint[0], endPoint[1], rotatedPoint4[0], rotatedPoint4[1], strokeWidth);
        
        return new ArrayList<>(Arrays.asList(p1, p2, p3, p4));
    }
    
    public static float calculateDistance(float[] startPoint, float[] endPoint) {
        if (startPoint.length != endPoint.length) {
            throw new IllegalArgumentException("Points must have the same dimensions.");
        }
        
        float sum = 0;
        for (int i = 0; i < startPoint.length; i++) {
            sum += Math.pow(endPoint[i] - startPoint[i], 2);
        }
        return (float) Math.sqrt(sum);
    }
    
    public static float calculateBezierPoint(float t, float p0, float p1, float p2) {
        // Quadratic Bezier formula: B(t) = (1 - t)^2 * P0 + 2 * (1 - t) * t * P1 + t^2 * P2
        float u = 1 - t;
        return u * u * p0 + 2 * u * t * p1 + t * t * p2;
    }
}
