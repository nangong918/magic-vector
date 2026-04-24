package com.demo.aarlib.live;

/**
 * 实时推流事件回调接口。
 */
public interface LivePushListener {
    /**
     * 推流错误回调。
     *
     * @param errorCode SDK 错误码
     * @param message   错误描述
     */
    void onError(int errorCode, String message);
}
