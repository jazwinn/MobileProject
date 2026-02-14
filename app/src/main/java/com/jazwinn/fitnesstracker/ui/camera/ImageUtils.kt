package com.jazwinn.fitnesstracker.ui.camera

import android.util.Log

object ImageUtils {
    private const val TAG = "ImageUtils"

    init {
        try {
            System.loadLibrary("image_utils")
            Log.d(TAG, "✅ Native library 'image_utils' loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ FAILED to load native library 'image_utils'", e)
        }
    }

    /**
     * Native method to convert NV21 YUV data to a resized, normalized NCHW float array.
     *
     * @param yuvData Source NV21 byte array
     * @param srcWidth Source image width
     * @param srcHeight Source image height
     * @param dstWidth Target width (e.g. 640)
     * @param dstHeight Target height (e.g. 640)
     * @return Float array (3 * dstWidth * dstHeight) in NCHW format or null on error
     */
    external fun yuvToNchwFloats(
        yuvData: ByteArray,
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int
    ): FloatArray?
}
