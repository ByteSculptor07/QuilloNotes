package com.github.bytesculptor07.quillo;

import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;

public class customDrawingEngineTestActivity extends Activity {
    //private MyGLSurfaceView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //glView = new MyGLSurfaceView(this);
        //glView = findViewById(R.id.drawingView);
        setContentView(R.layout.activity_customdrawingenginetest);
    }
}
