#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "ImageUtilsNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Native function to convert an NV21 image buffer to a resized, normalized NCHW float array.
 *
 * Input: NV21 byte array (YUV420sp)
 * Output: Float array (3 * 640 * 640) in NCHW format, normalized to [0, 1]
 */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_jazwinn_fitnesstracker_ui_camera_ImageUtils_yuvToNchwFloats(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray yuvData,
        jint srcWidth,
        jint srcHeight,
        jint dstWidth,
        jint dstHeight) {

    jbyte* yuv = env->GetByteArrayElements(yuvData, nullptr);
    if (yuv == nullptr) {
        LOGE("Could not get YUV byte array elements");
        return nullptr;
    }

    // Allocate buffer for NCHW floats (3 channels * width * height)
    int channelSize = dstWidth * dstHeight;
    int totalSize = channelSize * 3;
    std::vector<float> nchw(totalSize);

    // Simple nearest-neighbor resizing with YUV conversion
    for (int y = 0; y < dstHeight; ++y) {
        for (int x = 0; x < dstWidth; ++x) {
            int sx = x * srcWidth / dstWidth;
            int sy = y * srcHeight / dstHeight;

            int yIndex = sy * srcWidth + sx;
            int uvIndex = (srcWidth * srcHeight) + ((sy / 2) * srcWidth) + ((sx / 2) * 2);

            int Y = (unsigned char)yuv[yIndex];
            int V = (unsigned char)yuv[uvIndex];
            int U = (unsigned char)yuv[uvIndex + 1];

            int C = Y - 16;
            int D = U - 128;
            int E = V - 128;

            int R = (298 * C + 409 * E + 128) >> 8;
            int G = (298 * C - 100 * D - 208 * E + 128) >> 8;
            int B = (298 * C + 516 * D + 128) >> 8;

            // Clamp and Normalize to [0, 1]
            float rNorm = ((R < 0) ? 0 : ((R > 255) ? 255 : R)) / 255.0f;
            float gNorm = ((G < 0) ? 0 : ((G > 255) ? 255 : G)) / 255.0f;
            float bNorm = ((B < 0) ? 0 : ((B > 255) ? 255 : B)) / 255.0f;

            // Write to planar buffers: RRR... GGG... BBB...
            int pixelIndex = y * dstWidth + x;
            nchw[pixelIndex] = rNorm;                 // R plane
            nchw[pixelIndex + channelSize] = gNorm;     // G plane
            nchw[pixelIndex + 2 * channelSize] = bNorm; // B plane
        }
    }

    env->ReleaseByteArrayElements(yuvData, yuv, JNI_ABORT);

    jfloatArray result = env->NewFloatArray(totalSize);
    env->SetFloatArrayRegion(result, 0, totalSize, nchw.data());

    return result;
}
