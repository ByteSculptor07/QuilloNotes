package com.github.bytesculptor07.quillo.gl;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TriangleBuffer {
    private static final int MAX_TRIANGLES = 100_000;
    private static final int FLOATS_PER_VERTEX = 3;
    private static final int FLOATS_PER_TRIANGLE = 3 * FLOATS_PER_VERTEX;
    private static final int BYTES_PER_FLOAT = 4;

    private final FloatBuffer vertexDataBuffer;
    private final int[] vbo = new int[1];
    private int triangleCount = 0;
    private int shaderProgram;

    private static final String vertexShaderCode =
            "uniform float uAspectRatio;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  vec4 pos = vPosition;" +
                    "  pos.x = pos.x / uAspectRatio;" +
                    "  gl_Position = pos;" +
                    "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    public TriangleBuffer() {
        // Allocate buffer once
        ByteBuffer bb = ByteBuffer.allocateDirect(MAX_TRIANGLES * FLOATS_PER_TRIANGLE * BYTES_PER_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        vertexDataBuffer = bb.asFloatBuffer();
    }

    public void init() {
        shaderProgram = createProgram(vertexShaderCode, fragmentShaderCode);

        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glBufferData(
                GLES30.GL_ARRAY_BUFFER,
                MAX_TRIANGLES * FLOATS_PER_TRIANGLE * BYTES_PER_FLOAT,
                null,
                GLES30.GL_DYNAMIC_DRAW
        );
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    public void addTriangle(float[] triangleCoords) {
        if (triangleCount >= MAX_TRIANGLES) return;

        Log.d("Quillo", "number of triangles: " + triangleCount);

        vertexDataBuffer.position(triangleCount * FLOATS_PER_TRIANGLE);
        vertexDataBuffer.put(triangleCoords);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        vertexDataBuffer.position(triangleCount * FLOATS_PER_TRIANGLE);
        GLES30.glBufferSubData(
                GLES30.GL_ARRAY_BUFFER,
                triangleCount * FLOATS_PER_TRIANGLE * BYTES_PER_FLOAT,
                FLOATS_PER_TRIANGLE * BYTES_PER_FLOAT,
                vertexDataBuffer
        );
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        triangleCount++;
    }

    public void draw(int screenWidth, int screenHeight) {
        if (triangleCount == 0) return;

        GLES30.glUseProgram(shaderProgram);

        int positionHandle = GLES30.glGetAttribLocation(shaderProgram, "vPosition");
        int colorHandle = GLES30.glGetUniformLocation(shaderProgram, "vColor");
        int aspectHandle = GLES30.glGetUniformLocation(shaderProgram, "uAspectRatio");

        GLES30.glUniform1f(aspectHandle, (float) screenWidth / screenHeight);
        GLES30.glUniform4f(colorHandle, 0.0f, 0.0f, 0.0f, 1.0f); // black

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, triangleCount * 3);

        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    public void reset() {
        triangleCount = 0;
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String error = GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Shader compile error: " + error);
        }

        return shader;
    }

    private static int createProgram(String vertexCode, String fragmentCode) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        int[] linked = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            String error = GLES30.glGetProgramInfoLog(program);
            GLES30.glDeleteProgram(program);
            throw new RuntimeException("Program link error: " + error);
        }

        return program;
    }
}
