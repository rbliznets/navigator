package ru.glorient.granitbk_n.camera

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException


class FrontCamera(
    context: Context?,
    private val mCamera: Camera?
) :
    SurfaceView(context), SurfaceHolder.Callback {
    private val mHolder: SurfaceHolder = holder

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated() ") //Get the brush and set the properties
        try {
            mCamera!!.setPreviewDisplay(mHolder)
            mCamera.startPreview()
        } catch (e: IOException) {
            Log.d(
                TAG,
                "Error setting camera preview: " + e.message
            )
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed() ") //Get the brush and set the properties
        mCamera?.release()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        w: Int,
        h: Int
    ) {
        Log.d(TAG, "surfaceChanged() ") //Get the brush and set the properties
        if (mHolder.surface == null) {
            return
        }

        try {
            mCamera!!.stopPreview()
        } catch (e: java.lang.Exception) {
        }

        try {
//            mCamera!!.setPreviewDisplay(CameraFragmentTest.mFrontCamPreviewCanvas?.holder)
            mCamera!!.setPreviewDisplay(mHolder)
            mCamera.startPreview()
        } catch (e: java.lang.Exception) {
            Log.d(TAG, "Error starting camera : " + e.message)
        }
    }

    companion object {
        var TAG = "FrontCamera"
    }

    init {
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
//        mHolder.setFormat(PixelFormat.TRANSLUCENT or android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    }
}

