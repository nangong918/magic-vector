package com.demo.aarlib;

import android.content.Context;
import android.os.BatteryManager;

public final class BatteryNativeBridge {
    private BatteryNativeBridge() {
    }

    public static int getBatteryLevel(Context context) {
        if (context == null) {
            return -1;
        }
        BatteryManager batteryManager = (BatteryManager) context.getApplicationContext()
                .getSystemService(Context.BATTERY_SERVICE);
        return batteryManager != null ? batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
    }
}
