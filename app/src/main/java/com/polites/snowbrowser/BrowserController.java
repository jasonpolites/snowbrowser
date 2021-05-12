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
import android.util.Patterns;

import com.google.common.net.InternetDomainName;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class BrowserController {

    static Map<String, Intent> intents = new HashMap<>();

    public static String getHostFromUri(Uri uri) {
        String host = uri.getHost();
        if(host != null) {
            return InternetDomainName.from(host).topDomainUnderRegistrySuffix().toString();
        }
        return null;
    }

    public static Intent getBrowserIntent(Context context, Uri uri) {

        SharedPreferences sharedPref = context.getSharedPreferences("snow", Context.MODE_PRIVATE);
        String redirectBrowser =  sharedPref.getString("redirect", "Chrome");
        boolean unamp = sharedPref.getBoolean("unamp", false);
        if(unamp) {
            if(uri.toString().contains("amp")) {
                SnowLog.log(context, "Got amp url: " + uri);
                // Attempt to read the LINK element from the given URL to get the non-AMP target
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
                    conn.setInstanceFollowRedirects(true);
                    InputStream in = conn.getInputStream();
                    String linkValue = TagParser.getCanonicalLink(in);
                    if(linkValue != null &&  Patterns.WEB_URL.matcher(linkValue).matches()) {
                        SnowLog.log(context, "Replacing AMP url with canonical link value: " + linkValue);
                        uri = Uri.parse(linkValue);
                    } else {
                        SnowLog.log(context, "No canonical url found or is not a valid url: " + linkValue);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }

        SnowLog.log(context, "Redirect browser is " + redirectBrowser);

        final String host = getHostFromUri(uri);

        Intent targetIntent = intents.get(host);

        SnowLog.log(context, "No target set for " + host);

        String targetBrowserName = sharedPref.getString(host, redirectBrowser);

        SnowLog.log(context,"Got target browser: " + targetBrowserName);

        SnowLog.log(context,"Opening link: " + uri.toString());

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
            SnowLog.log(context,"Launching Browser..." + targetBrowserName);
            ActivityInfo activity = target.activityInfo;

            targetIntent = intents.get(activity.applicationInfo.packageName);

            if(targetIntent == null) {
                ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);
                targetIntent = new Intent(Intent.ACTION_MAIN);
                targetIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                targetIntent.setComponent(name);
            }

            targetIntent.setData(uri);

            intents.put(activity.applicationInfo.packageName, targetIntent);
        } else {
            SnowLog.log(context,"No Target!");
        }

        return targetIntent;
    }

    public static ResolveInfo getDefaultBrowser(AppCompatActivity parent) {
        PackageManager pm = parent.getApplicationContext().getPackageManager();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.google.com")); // Just any url to get browsers
        List<ResolveInfo> defaultBrowser = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if(defaultBrowser != null && defaultBrowser.size() > 0) {
            return defaultBrowser.get(0);
        }
        return null;
    }

    public static List<ResolveInfo> getAllBrowsers(AppCompatActivity parent) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.google.com")); // Just any url to get browsers
        PackageManager pm = parent.getApplicationContext().getPackageManager();
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
    }

    public static void showBottomSheet(AppCompatActivity parent, Uri uri) {
        PackageManager pm = parent.getApplicationContext().getPackageManager();
        List<ResolveInfo> allBrowsers = getAllBrowsers(parent);
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
