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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

@SuppressWarnings("ALL")
public class BrowserController {

    static Map<String, Intent> intents = new HashMap<>();
    static Uri sentinalUrl = Uri.parse("https://www.google.com"); // Just any url to get browsers
    public static String getHostFromUri(Uri uri) {
        String host = uri.getHost();
        if(host != null) {
            return InternetDomainName.from(host).topDomainUnderRegistrySuffix().toString();
        }
        return null;
    }

    public static Intent getBrowserIntent(Context context, Uri uri) {
        // Loading shared prefs does a FS STAT operation, which causes a StrictMode violation
        // OK sure, but come on.. I'm not going to do a whole lot of async work just to avoid a 10ms STAT
        SharedPreferences sharedPref = context.getSharedPreferences("snow", Context.MODE_PRIVATE);
        String redirectBrowser =  sharedPref.getString("redirect", "Chrome");
        boolean unamp = sharedPref.getBoolean("unamp", false);
        if(unamp) {
            if(uri.toString().toLowerCase().contains("amp")) { // iffy condition, but works for 99% of cases

                SnowLog.log(context, "Got AMP url: " + uri);

                // Attempt to read the LINK element from the given URL to get the non-AMP target
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
                    conn.setInstanceFollowRedirects(true);

                    // We don't ever want to exceed 1 second
                    // Technically this allows for 2 seconds, but the API doesn't really cater
                    // for a single time period to be shared across connect and read.
                    conn.setConnectTimeout(1000);
                    conn.setReadTimeout(1000);

                    // This is opening a socket on the main UI thread, which is generally "bad"
                    // But is might also be bad (and a PITA to implement) an interstitial loader, or
                    // to just have nothing happen.
                    InputStream in = conn.getInputStream();
                    String linkValue = TagParser.getCanonicalLink(in);
                    if(linkValue != null && Patterns.WEB_URL.matcher(linkValue).matches()) {
                        SnowLog.log(context, "Replacing AMP url with canonical link value: " + linkValue);
                        uri = Uri.parse(linkValue);
                    } else {
                        SnowLog.log(context, "No canonical url found or is not a valid url: " + linkValue);
                    }
                } catch (IOException e) {
                    // Don't need this exception.. probably don't even need the trace TBH
                    e.printStackTrace();
                } finally {
                    if(conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }

        final String host = getHostFromUri(uri);
        Intent targetIntent = intents.get(host);
        String targetBrowserName = sharedPref.getString(host, redirectBrowser);

        SnowLog.log(context, String.format("Default browser: %s", redirectBrowser));
        SnowLog.log(context, String.format("Chosen browser: %s", targetBrowserName));
        SnowLog.log(context, String.format("Opening link: %s", uri.toString()));

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> allBrowsers = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        ResolveInfo target = null;
        for (ResolveInfo b : allBrowsers) {
            String appName = b.loadLabel(pm).toString();
            if(appName.equalsIgnoreCase(targetBrowserName)) {
                target = b;
                break;
            }
        }

        if(target != null) {
            SnowLog.log(context,"Launching Browser: " + targetBrowserName);
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
        intent.setData(sentinalUrl);
        List<ResolveInfo> defaultBrowser = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if(defaultBrowser != null && defaultBrowser.size() > 0) {
            return defaultBrowser.get(0);
        }
        return null;
    }

    public static List<ResolveInfo> getAllBrowsers(AppCompatActivity parent) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(sentinalUrl);
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
