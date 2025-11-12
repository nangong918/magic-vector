package com.core.baseutil.log;


import androidx.multidex.BuildConfig;

import com.core.baseutil.debug.DebugEnvironment;


public class Log {


    private static final boolean isForceOutPut =
            // 测试环境
            DebugEnvironment.INSTANCE.getProjectEnvironment() != DebugEnvironment.Environment.PRODUCTION;

    public static void d(String TAG, String msg){
        if (BuildConfig.DEBUG || isForceOutPut){
            android.util.Log.d(TAG, msg);
        }
    }

    public static void e(String TAG, String msg){
        if (BuildConfig.DEBUG || isForceOutPut){
            android.util.Log.e(TAG, msg);
        }
    }

    public static void e(String TAG, String msg, Throwable tr){
        if (BuildConfig.DEBUG || isForceOutPut){
            android.util.Log.e(TAG, msg, tr);
        }
    }

    public static void i(String TAG, String msg){
        if (BuildConfig.DEBUG || isForceOutPut){
            android.util.Log.i(TAG, msg);
        }
    }

    public static void i(String TAG, String msg, Throwable tr){
        if (BuildConfig.DEBUG || isForceOutPut){
            android.util.Log.i(TAG, msg, tr);
        }
    }

    public static void v(String TAG, String msg){
        if (BuildConfig.DEBUG || isForceOutPut){
            android.util.Log.v(TAG, msg);
        }
    }

    public static void w(String TAG, String msg){
        if (BuildConfig.DEBUG || isForceOutPut){
            android.util.Log.w(TAG, msg);
        }
    }

    public static void wtf(String TAG, String msg){
        if (BuildConfig.DEBUG || isForceOutPut){
            android.util.Log.wtf(TAG, msg);
        }
    }


}
