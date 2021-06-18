package com.polites.snowbrowser;

import android.content.pm.ActivityInfo;

public class BrowserItem {

    private String name;

    public ActivityInfo getActivityInfo() {
        return activityInfo;
    }

    public void setActivityInfo(ActivityInfo activityInfo) {
        this.activityInfo = activityInfo;
    }

    private ActivityInfo activityInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
