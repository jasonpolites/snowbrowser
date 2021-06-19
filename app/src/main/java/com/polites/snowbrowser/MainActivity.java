package com.polites.snowbrowser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SS");
    // Request code for creating a PDF document.
    private static final int CREATE_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateState();

        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        final SharedPreferences sharedPref = getSharedPreferences("snow", Context.MODE_PRIVATE);
        final RadioGroup radioButtonGroup = findViewById(R.id.chooseDefaultBrowserGroup);

        pullToRefresh.setOnRefreshListener(() -> {
            updateState();
            pullToRefresh.setRefreshing(false);
        });

        PackageManager pm = getApplicationContext().getPackageManager();
        List<ResolveInfo> allBrowsers = BrowserController.getAllBrowsers(this);
        ResolveInfo defaultBrowser = BrowserController.getDefaultBrowser(this);

        String redirectBrowser =  sharedPref.getString("redirect", null);

        if(redirectBrowser == null) {
            assert defaultBrowser != null;
            redirectBrowser = defaultBrowser.loadLabel(pm).toString();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("redirect", redirectBrowser);
            editor.apply();
        }

        Button resetButton = findViewById(R.id.resetAll);
        resetButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to clear your prefs?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    printPrefs();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.clear();
                    RadioButton rb = (RadioButton) radioButtonGroup.getChildAt(0);
                    editor.putString("redirect", rb.getText().toString());
                    rb.setChecked(true);
                    editor.apply();
                    Toast.makeText(v.getContext(), "Preferences cleared", Toast.LENGTH_SHORT).show();
                    updateState();
                })
                .setNegativeButton(android.R.string.no, null).show();
         });

        final CheckBox logToggle = findViewById(R.id.logToggle);
        logToggle.setOnClickListener(v -> {
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
        });

        final CheckBox unampToggle = findViewById(R.id.unampToggle);
        unampToggle.setOnClickListener(v -> sharedPref.edit().putBoolean("unamp", unampToggle.isChecked()).apply());

        final CheckBox redirectToggle = findViewById(R.id.redirectToggle);
        redirectToggle.setOnClickListener(v -> sharedPref.edit().putBoolean("followRedirects", redirectToggle.isChecked()).apply());

        final Button btnPrintPrefs = findViewById(R.id.printPrefs);
        final Button btnSaveLog = findViewById(R.id.saveLogs);

        btnPrintPrefs.setOnClickListener(v -> {
            printPrefs();
        });

        btnSaveLog.setOnClickListener(v -> {
            List<String> allLogs = SnowLog.getAllLogs();
            if(allLogs != null && allLogs.size() > 0) {
                btnSaveLog.setEnabled(false);
                String fileName = getString(R.string.browser_name) + "-DebugLog-" + DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".log";
                File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File path = new File(externalStoragePublicDirectory, fileName);
                createFile(Uri.fromFile(externalStoragePublicDirectory), path);
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

                radioButton.setOnClickListener(v -> {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("redirect", name);
                    editor.apply();
                });

                radioButtonGroup.addView(radioButton);
            }
        }
    }

    private void printPrefs() {
        final SharedPreferences sharedPref = getSharedPreferences("snow", Context.MODE_PRIVATE);
        SnowLog.log(MainActivity.this, "############### SAVED SETTINGS ##############");
        Map<String, ?> allPrefs = sharedPref.getAll();
        for(String key: allPrefs.keySet()) {
            SnowLog.log(MainActivity.this, key + ":" + Objects.requireNonNull(allPrefs.get(key)).toString());
        }
        SnowLog.log(MainActivity.this, "#############################################");
        updateState();
    }

    private void createFile(Uri pickerInitialUri, File file) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, file.getName());

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_FILE);
    }


    private void writeToFileAsync(final Uri file, final String data, Consumer<? super Boolean> callback) {
        CompletableFuture.supplyAsync(() -> {
            FileWriter writer = null;
            ParcelFileDescriptor pfd = null;
            try {
                pfd = getContentResolver().openFileDescriptor(file, "w");
                writer = new FileWriter(pfd.getFileDescriptor());
                writer.write(data);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if(writer != null) {
                        writer.close();
                    }
                    if(pfd != null) {
                        pfd.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).thenAccept(callback);
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
                textView.setText(R.string.browser_active);
                isDefault = true;
            }
        }

        if(!isDefault) {
            setDefault.setVisibility(View.VISIBLE);
            setDefault.setEnabled(true);
            textView.setText(R.string.browser_inactive);
            setDefault.setOnClickListener(v -> {
                final Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                startActivityForResult(intent, 0);
            });
        }

        boolean debug = sharedPref.getBoolean("debug", false);
        boolean unamp = sharedPref.getBoolean("unamp", false);
        boolean followRedirects = sharedPref.getBoolean("followRedirects", false);

        TextView logView = findViewById(R.id.logView);
        CheckBox logToggle = findViewById(R.id.logToggle);
        CheckBox unampToggle = findViewById(R.id.unampToggle);
        CheckBox redirectToggle = findViewById(R.id.redirectToggle);
        Button btnPrintPrefs = findViewById(R.id.printPrefs);

        logToggle.setChecked(debug);
        unampToggle.setChecked(unamp);
        redirectToggle.setChecked(followRedirects);

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

        if (resultCode == RESULT_OK && requestCode == CREATE_FILE) {
            Uri filePath = data.getData();

            List<String> allLogs = SnowLog.getAllLogs();
            StringBuilder b = new StringBuilder();
            for(String line: allLogs) {
                b.append(line);
                b.append(System.getProperty("line.separator"));
            }

            String fileData = b.toString();

            writeToFileAsync(filePath, fileData, written -> {
                runOnUiThread(() -> {
                    final Button btnSaveLog = findViewById(R.id.saveLogs);
                    if(written == true) {
                        Toast.makeText(MainActivity.this, "Debug Log Saved", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error writing file", Toast.LENGTH_SHORT).show();
                    }
                    btnSaveLog.setEnabled(true);
                });
            });
        }

        updateState();
    }
}
