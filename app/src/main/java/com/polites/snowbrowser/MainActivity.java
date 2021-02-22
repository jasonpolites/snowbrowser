package com.polites.snowbrowser;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


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

        final RadioGroup radioButtonGroup = findViewById(R.id.chooseDefaultBrowserGroup);
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ResolveInfo> allBrowsers = BrowserController.getAllBrowsers(this);

        ResolveInfo defaultBrowser = BrowserController.getDefaultBrowser(this);
        final SharedPreferences sharedPref = getSharedPreferences("snow", Context.MODE_PRIVATE);
        String redirectBrowser =  sharedPref.getString("redirect", null);

        if(redirectBrowser == null) {
            redirectBrowser = defaultBrowser.loadLabel(pm).toString();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("redirect", redirectBrowser);
            editor.commit();
        }

        Button resetButton = findViewById(R.id.resetAll);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                RadioButton rb = (RadioButton) radioButtonGroup.getChildAt(0);
                editor.putString("redirect", rb.getText().toString());
                rb.setChecked(true);
                editor.commit();
                Toast.makeText(v.getContext(), "Preferences cleared", Toast.LENGTH_SHORT).show();
             }
        });

        String me = getString(R.string.browser_name);

        final float scale = getResources().getDisplayMetrics().density;
        int imgSize =  (int) (32 * scale + 0.5f);
        int padding =  (int) (8 * scale + 0.5f);

        int id = 0;
        for (ResolveInfo browser : allBrowsers) {
            final String name = browser.loadLabel(pm).toString();
            if(!name.equals(me)) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setText(name);
                radioButton.setPadding(padding, padding, padding, padding);
                radioButton.setId(id++);
                Drawable res = null;
                try {
                    res = pm.getApplicationIcon(browser.activityInfo.packageName);
                    res.setBounds(0, 0, imgSize, imgSize);
                    radioButton.setCompoundDrawables(res, null, null, null);
                    radioButton.setCompoundDrawablePadding(padding);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if(redirectBrowser.equals(name)) {
                    radioButton.setChecked(true);
                }

                radioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("redirect", name);
                        editor.commit();
                    }
                });

                radioButtonGroup.addView(radioButton);
            }
        }
    }

    private void setBrowserDefaultState() {
        ResolveInfo defaultBrowser = BrowserController.getDefaultBrowser(this);
        Button setDefault = findViewById(R.id.setDefaultBrowserButton);
        TextView textView = findViewById(R.id.browserDefaultText);
        boolean isDefault = false;
        if(defaultBrowser != null) {
            if(defaultBrowser.activityInfo.name.equals(BrowserActivity.class.getName())) {
                setDefault.setVisibility(View.INVISIBLE);
                setDefault.setEnabled(false);
                textView.setText("Snow Browser is currently active!");
                isDefault = true;
            }
        }

        if(!isDefault) {
            setDefault.setVisibility(View.VISIBLE);
            setDefault.setEnabled(true);
            textView.setText("Snow Browser not is currently active. Set it to the system default browser to activate");
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
