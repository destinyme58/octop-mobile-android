package com.octop.mobile;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPreferences {
    private static final String FILE_NAME = "octop_mobile";
    private static final String KEY_SERVER_URL = "server_url";

    private final SharedPreferences preferences;

    public AppPreferences(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public String getServerUrl() {
        return preferences.getString(KEY_SERVER_URL, BuildConfig.DEFAULT_SERVER_URL);
    }

    public void setServerUrl(String serverUrl) {
        preferences.edit().putString(KEY_SERVER_URL, serverUrl).apply();
    }
}

