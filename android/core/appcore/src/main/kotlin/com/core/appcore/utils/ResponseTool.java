package com.core.appcore.utils;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;


import com.core.appcore.api.handler.AsyncRequestCallback;
import com.core.appcore.api.handler.FourConsumer;
import com.core.appcore.api.handler.SyncRequestCallback;
import com.core.appcore.api.handler.TriConsumer;
import com.core.baseutil.network.BaseResponse;
import com.core.baseutil.network.ResponseUtil;
import com.core.baseutil.ui.ToastUtils;
import com.data.domain.constant.BaseConstant;

import java.util.function.BiConsumer;

public abstract class ResponseTool extends ResponseUtil {

    private static final String TAG = ResponseTool.class.getName();

    /**
     * 基础响应处理
     * @param response   响应
     * @param context   上下文
     * @return          处理结果
     * @param <T>       响应数据类型
     */
    public static <T> boolean handleResponse(BaseResponse<T> response, Context context){
        if(response != null && response.getCode() != null){

            if (response.getCode().equals(BaseConstant.NetworkCode.SUCCESS_CODE)){
                return true;
            }
            else {
                if(context instanceof Activity){
                    ((Activity) context).runOnUiThread(() ->
                            ToastUtils.INSTANCE.showToastActivity(context, response.getMessage())
                    );
                }
                return false;
            }
        }
        else {
            if(context instanceof Activity){
                ((Activity) context).runOnUiThread(() -> Toast.makeText(context, context.getString(com.core.appcore.R.string.please_check_your_network), Toast.LENGTH_LONG).show());
            }
            return false;
        }
    }

    /**
     * 基础链式同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param callback      回调
     * @return              处理结果
     * @param <T>           响应数据类型
     */
    public static <T> boolean handleSyncResponse(BaseResponse<T> response, Context context, AsyncRequestCallback callback){
        boolean result = handleResponse(response, context);
        if (result){
            callback.onSingleRequestSuccess();
        }
        else {
            callback.onThrowable(new Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR));
        }
        return result;
    }

    /**
     * 基础并发同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param callback      并发回调
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    public static <T> void handleAsyncResponseEx(
            BaseResponse<T> response,
            Context context,
            AsyncRequestCallback callback,
            BiConsumer<BaseResponse<T>, Context> handler) {

        boolean result = handleResponse(response, context);
        if (result) {
            handler.accept(response, context);
            callback.onSingleRequestSuccess();
        } else {
            callback.onThrowable(new Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR));
        }
    }

    /**
     * 带参数的并发同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param param         参数
     * @param callback      并发回调
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    public static <T> void handleAsyncResponseEx(
            BaseResponse<T> response,
            Context context,
            Object param,
            AsyncRequestCallback callback,
            TriConsumer<BaseResponse<T>, Context, Object> handler) {

        boolean result = handleResponse(response, context);
        if (result) {
            handler.accept(response, context, param);
            callback.onSingleRequestSuccess();
        } else {
            callback.onThrowable(new Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR));
        }
    }

    /**
     * 手动控制响应结果的带参数的并发同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param param         参数
     * @param callback      并发回调
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    public static <T> void handleAsyncResponseEx(
            BaseResponse<T> response,
            Context context,
            Object param,
            AsyncRequestCallback callback,
            FourConsumer<BaseResponse<T>, Context, AsyncRequestCallback, Object> handler) {

        boolean result = handleResponse(response, context);
        if (result) {
            handler.accept(response, context, callback, param);
            // 此处改为手动回调
//            callback.onSingleRequestSuccess();
        } else {
            callback.onThrowable(new Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR));
        }
    }

    public static <T> void handleSyncResponseEx(
            BaseResponse<T> response,
            Context context,
            SyncRequestCallback callback,
            TriConsumer<BaseResponse<T>, Context, SyncRequestCallback> handler) {

        boolean result = handleResponse(response, context);
        if (result) {
            handler.accept(response, context, callback); // 传递三个参数
            // 同步调用需要在最后一个请求成功后手动调用
//        callback.onAllRequestSuccess();
        } else {
            callback.onThrowable(new Throwable(SyncRequestCallback.RESPONSE_BASE_ERROR));
        }
    }

    public static <T> void handleSyncResponseEx(
            BaseResponse<T> response,
            Context context,
            SyncRequestCallback callback,
            Object param,
            FourConsumer<BaseResponse<T>, Context, SyncRequestCallback, Object> handler) {

        boolean result = handleResponse(response, context);
        if (result) {
            handler.accept(response, context, callback, param); // 传递4个参数
            // 同步调用需要在最后一个请求成功后手动调用
//        callback.onAllRequestSuccess();
        } else {
            callback.onThrowable(new Throwable(SyncRequestCallback.RESPONSE_BASE_ERROR));
        }
    }

}
