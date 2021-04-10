package com.polites.snowbrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SnowLog {
    static boolean debugEnabled = false;
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static final LinkedList<String> entries = new LinkedList<>();
    static int maxEntries = 100;
    static boolean initialized = false;
    public static final void log(Context context, String entry) {

        if(!initialized) {
            SharedPreferences sharedPref = context.getSharedPreferences("snow", Context.MODE_PRIVATE);
            debugEnabled = sharedPref.getBoolean("debug", false);
            initialized = true;
        }

        if(debugEnabled == true) {
            Log.e("Snow", entry);
            entries.addLast(DATE_FORMAT.format(System.currentTimeMillis()) + ": " + entry);
            if(entries.size() > maxEntries) {
                entries.removeFirst();
            }
        }
    }

    public static final List<String> getAllLogs() {
        return Collections.unmodifiableList(entries);
    }

    public static final void clear() {
        entries.clear();
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean debugEnabled) {
        SnowLog.debugEnabled = debugEnabled;
    }
}
