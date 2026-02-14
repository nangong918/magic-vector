package com.demo.aarlib;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public final class WifiNativeBridge {
    public static final int WIFI_SIGNAL_UNAVAILABLE = -999;

    private WifiNativeBridge() {
    }

    public static int getWifiSignalStrength(Context context) {
        if (context == null) {
            return WIFI_SIGNAL_UNAVAILABLE;
        }
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            return WIFI_SIGNAL_UNAVAILABLE;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo != null ? wifiInfo.getRssi() : WIFI_SIGNAL_UNAVAILABLE;
    }

    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            return false;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo != null && wifiInfo.getIpAddress() != 0;
    }
}
