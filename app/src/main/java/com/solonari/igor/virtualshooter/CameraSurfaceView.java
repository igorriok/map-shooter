package com.solonari.igor.virtualshooter;


import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;



public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    @SuppressWarnings("unused")
    private static final String TAG = "CameraSurfaceView";
    private SurfaceHolder holder;
    private Camera camera;
    public float verticalFOV;
    public float horizontalFOV;

    // private DrawSurfaceView draw;


    @SuppressWarnings("deprecation")
    public CameraSurfaceView(Context context, AttributeSet set) {
        super(context, set);

        // Initiate the Surface Holder properly
        this.holder = this.getHolder();
        this.holder.addCallback(this);
        this.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // Open the Camera in preview mode
            this.camera = Camera.open();
            Camera.Parameters params = camera.getParameters();
            verticalFOV = params.getVerticalViewAngle();
            horizontalFOV = params.getHorizontalViewAngle();

            this.camera.setDisplayOrientation(0);

            this.camera.setPreviewDisplay(this.holder);
            // this.camera.setPreviewCallback(mPreviewCallback);

        } catch (IOException ioe) {
            ioe.printStackTrace(System.out);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.d("Test", "SurfaceChanged");

        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> csc = parameters.getSupportedPreviewSizes();
        Camera.Size cs = null;

        for (Camera.Size s : csc) {
            if (s.width <= width && s.height <= height) {
                parameters.setPreviewSize(s.width, s.height);
                break;
            }
        }

        camera.setParameters(parameters);
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when replaced with a new screen
        // Always make sure to release the Camera instance
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

}
