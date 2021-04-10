package com.polites.snowbrowser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class BottomSheetDialog extends BottomSheetDialogFragment {

    public void setBrowsers(List<BrowserItem> browsers) {
        this.browsers = browsers;
    }

    private List<BrowserItem> browsers;
    private Uri uri;

    public BottomSheetDialog() {}

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable
            ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.bottom_sheet, container, false);

        if(this.browsers != null && this.browsers.size() > 0) {

            final String host = BrowserController.getHostFromUri(uri);

            for (final BrowserItem browser: browsers) {
                final View itemView = inflater.inflate(R.layout.bottom_sheet_item, container, false);
                TextView textView = itemView.findViewById(R.id.browser_item_text);
                textView.setText(browser.getName());
                ImageView imageView = itemView.findViewById(R.id.browser_item_icon);

                Drawable res = null;
                try {
                    res = getContext().getPackageManager().getApplicationIcon(browser.getActivityInfo().packageName);
                    imageView.setImageDrawable(res);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                v.addView(itemView);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    AsyncUtils.doAsync(new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences sharedPref = getContext().getSharedPreferences("snow", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(host, browser.getName());
                            editor.commit();
                        }
                    });
                    Intent intent = new Intent(getContext(), BrowserActivity.class);
                    intent.setData(uri);
                    startActivity(intent);
                    dismiss();
                    }
                });
            }
        }

        return v;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}