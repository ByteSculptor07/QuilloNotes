package com.github.bytesculptor07.quillo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxSessionStore;

public class NoteSessionStore implements DbxSessionStore {
    private SharedPreferences prefs;
    private static final String STATE_KEY = "oauth_state";

    public NoteSessionStore(Context context) {
        prefs = context.getSharedPreferences("dropbox_prefs", Context.MODE_PRIVATE);
    }

    @Override
    public String get() {
        return prefs.getString(STATE_KEY, null);
    }

    @Override
    public void set(String state) {
        prefs.edit().putString(STATE_KEY, state).apply();
    }
    
    @Override
    public void clear() {
        prefs.edit().remove(STATE_KEY).apply();
    }
}