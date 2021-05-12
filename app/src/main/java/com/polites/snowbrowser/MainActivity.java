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
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateState();
        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        final SharedPreferences sharedPref = getSharedPreferences("snow", Context.MODE_PRIVATE);
        final RadioGroup radioButtonGroup = findViewById(R.id.chooseDefaultBrowserGroup);

        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
            updateState();
            pullToRefresh.setRefreshing(false);
            }
        });

        PackageManager pm = getApplicationContext().getPackageManager();
        List<ResolveInfo> allBrowsers = BrowserController.getAllBrowsers(this);
        ResolveInfo defaultBrowser = BrowserController.getDefaultBrowser(this);

        String redirectBrowser =  sharedPref.getString("redirect", null);

        if(redirectBrowser == null) {
            redirectBrowser = defaultBrowser.loadLabel(pm).toString();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("redirect", redirectBrowser);
            editor.apply();
        }

        Button resetButton = findViewById(R.id.resetAll);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                RadioButton rb = (RadioButton) radioButtonGroup.getChildAt(0);
                editor.putString("redirect", rb.getText().toString());
                rb.setChecked(true);
                editor.apply();
                Toast.makeText(v.getContext(), "Preferences cleared", Toast.LENGTH_SHORT).show();
            }
        });

        final CheckBox logToggle = findViewById(R.id.logToggle);
        logToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView logView = findViewById(R.id.logView);
                Button btnPrintPrefs = findViewById(R.id.printPrefs);
                if(logToggle.isChecked()) {
                    logView.setVisibility(View.VISIBLE);
                    btnPrintPrefs.setVisibility(View.VISIBLE);
                } else {
                    logView.setVisibility(View.INVISIBLE);
                    btnPrintPrefs.setVisibility(View.INVISIBLE);
                }

                sharedPref.edit().putBoolean("debug", logToggle.isChecked()).apply();
                SnowLog.setDebugEnabled(logToggle.isChecked());
            }
        });

        final CheckBox unampToggle = findViewById(R.id.unampToggle);
        unampToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPref.edit().putBoolean("unamp", unampToggle.isChecked()).apply();
            }
        });

        final Button btnPrintPrefs = findViewById(R.id.printPrefs);

        btnPrintPrefs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SnowLog.log(MainActivity.this, "############### SAVED SETTINGS ##############");
                Map<String, ?> allPrefs = sharedPref.getAll();
                for(String key: allPrefs.keySet()) {
                    SnowLog.log(MainActivity.this, key + ":" + allPrefs.get(key).toString());
                }
                SnowLog.log(MainActivity.this, "#############################################");
                updateState();
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
                Drawable res;
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
                        editor.apply();
                    }
                });

                radioButtonGroup.addView(radioButton);
            }
        }
    }

    private void updateState() {
        final SharedPreferences sharedPref = getSharedPreferences("snow", Context.MODE_PRIVATE);
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

        boolean debug = sharedPref.getBoolean("debug", false);
        boolean unamp = sharedPref.getBoolean("unamp", false);

        TextView logView = findViewById(R.id.logView);
        CheckBox logToggle = findViewById(R.id.logToggle);
        CheckBox unampToggle = findViewById(R.id.unampToggle);
        Button btnPrintPrefs = findViewById(R.id.printPrefs);

        logToggle.setChecked(debug);
        unampToggle.setChecked(unamp);

        if(debug) {
            logView.setVisibility(View.VISIBLE);
            btnPrintPrefs.setVisibility(View.VISIBLE);
        } else {
            logView.setVisibility(View.INVISIBLE);
            btnPrintPrefs.setVisibility(View.INVISIBLE);
        }

        List<String> logs = SnowLog.getAllLogs();
        logView.setText("");
        if(logs.size() > 0) {
            StringBuilder buffer = new StringBuilder(logs.size());
            for (String log: logs) {
                buffer.append(log);
                buffer.append(System.getProperty("line.separator"));
            }
            logView.setText(buffer.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateState();
    }
}
