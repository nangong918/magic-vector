package com.vectordemo.live.camera;

import android.util.Size;

/**
 * Camera2 采集回调接口：
 * 把相机生命周期和 YUV 预览帧回调给上层页面。
 */
public interface Camera2Listener {
    /**
     * 相机打开回调。
     *
     * @param previewSize        相机输出尺寸
     * @param displayOrientation 预览方向
     */
    void onCameraOpened(Size previewSize, int displayOrientation);

    /**
     * 预览帧回调，输出 I420 数据。
     *
     * @param yuvData I420 帧数据
     */
    void onPreviewFrame(byte[] yuvData);

    /**
     * 相机关闭回调。
     */
    void onCameraClosed();

    /**
     * 相机异常回调。
     *
     * @param e 异常对象
     */
    void onCameraError(Exception e);
}
