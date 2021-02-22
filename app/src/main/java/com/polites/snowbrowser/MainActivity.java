package com.polites.snowbrowser;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;



public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setBrowserDefaultState();

        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setBrowserDefaultState();
                pullToRefresh.setRefreshing(false);
            }
        });
    }

    private void setBrowserDefaultState() {
        ResolveInfo defaultBrowser = BrowserController.getDefaultBrowser(this);
        Button setDefault = findViewById(R.id.setDefaultBrowserButton);
        TextView textView = findViewById(R.id.browserDefaultText);
        boolean isDefault = false;
        if(defaultBrowser != null) {
            if(defaultBrowser.activityInfo.name.equals(BrowserActivity.class.getName())) {
                setDefault.setEnabled(false);
                textView.setText("Snow Browser is currently default");
                isDefault = true;
            }
        }

        if(!isDefault) {
            setDefault.setEnabled(true);
            textView.setText("Snow Browser not is currently default");
            setDefault.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                    startActivityForResult(intent, 0);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setBrowserDefaultState();
    }
}
