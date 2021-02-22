package com.polites.snowbrowser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SwitchBrowserActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        setVisible(false);
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        Uri uri = Uri.parse(url);
        BrowserController.showBottomSheet(this, uri);
    }
}
