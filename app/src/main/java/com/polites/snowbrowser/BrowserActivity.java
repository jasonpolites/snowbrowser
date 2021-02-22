package com.polites.snowbrowser;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry activity which proxies web page requests to the "best" browser for the url.
 * Default behavior is to check for an existing record based on the URL, and if not found open the
 * default browser nominated by the user.
 */
public class BrowserActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.enableDefaults();
        if(getIntent().getData() != null) {
            Intent targetIntent = BrowserController.getBrowserIntent(this, getIntent().getData());
            if(targetIntent != null) {
                targetIntent.setData(getIntent().getData());
                startActivity(targetIntent);
            }
        }
        finish();
    }
}
