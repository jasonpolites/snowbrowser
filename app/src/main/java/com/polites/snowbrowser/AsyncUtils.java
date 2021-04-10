package com.polites.snowbrowser;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CompletableFuture;

import androidx.core.os.HandlerCompat;

public class AsyncUtils {
//    static  final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    public static final void doAsync(Runnable r) {
        CompletableFuture.runAsync(r);
//        CompletableFuture.runAsync(r).thenRun(new Runnable() {
//            @Override
//            public void run() {
//
//                mainThreadHandler.
//            }
//        });
    }
}
