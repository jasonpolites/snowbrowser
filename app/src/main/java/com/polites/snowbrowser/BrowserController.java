package com.polites.snowbrowser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import com.google.common.net.InternetDomainName;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class BrowserController {

    static Map<String, Intent> intents = new HashMap<>();

    public static String getHostFromUri(Uri uri) {
        return InternetDomainName.from(uri.getHost()).topDomainUnderRegistrySuffix().toString();
    }

    public static final Intent getBrowserIntent(Context context, Uri uri) {

        SharedPreferences sharedPref = context.getSharedPreferences("snow", Context.MODE_PRIVATE);
        boolean reset = sharedPref.getBoolean("reset", false);

        final String host = getHostFromUri(uri);

        Intent targetIntent = intents.get(host);

        if(targetIntent == null || reset) {

            Log.e("Snow", "No target set for " + host);

            String targetBrowserName = sharedPref.getString(host, "Brave"); // TODO: Should be app default not hard coded

            Log.e("Snow", "Got target browser: " + targetBrowserName);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> allBrowsers = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
            ResolveInfo target = null;
            for (ResolveInfo b : allBrowsers) {
                String appName = b.loadLabel(pm).toString();

                Log.e("snow", appName);

                if(appName.equalsIgnoreCase(targetBrowserName)) {
                    target = b;
                    break;
                }
            }

            if(target != null) {
                Log.e("Snow", "Launching Browser..." + targetBrowserName);
                ActivityInfo activity = target.activityInfo;
                ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);
                targetIntent = new Intent(Intent.ACTION_MAIN);

                targetIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                targetIntent.setComponent(name);

                intents.put(host, targetIntent);

            } else {
                Log.e("Snow", "No Target!");
            }
        } else {
            Log.e("Snow", "Already have intent :)");
        }

        return targetIntent;
    }

    public static final void showBottomSheet(AppCompatActivity parent, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.google.com")); // Just any url to get browsers
        PackageManager pm = parent.getApplicationContext().getPackageManager();
        List<ResolveInfo> allBrowsers = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        List<ResolveInfo> defaultBrowser = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        List<BrowserItem> browsers = new LinkedList<>();

        String me = parent.getString(R.string.browser_name);
        for (ResolveInfo b : allBrowsers) {
            String name = b.loadLabel(pm).toString();
            if(!name.equals(me)) {
                BrowserItem bItem = new BrowserItem();
                bItem.setName(b.loadLabel(pm).toString());
                bItem.setActivityInfo(b.activityInfo);
                browsers.add(bItem);
            }
        }

        final BottomSheetDialog bottomSheet = new BottomSheetDialog();
        bottomSheet.setBrowsers(browsers);
        bottomSheet.setUri(uri);
        bottomSheet.show(parent.getSupportFragmentManager(), "BrowserChooser");
    }

}
