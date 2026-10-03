package com.example.escapevault.admob_pack;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdManager {

    private static InterstitialAd mInterstitialAd;
    private static boolean isLoading = false;

    public static void loadInterstitialAd(Context context) {
        if (mInterstitialAd != null || isLoading) return;

        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(
                context.getApplicationContext(),
                "ca-app-pub-3940256099942544/1033173712",
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        isLoading = false;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                        isLoading = false;
                    }
                }
        );
    }


    public static void showInterstitialAd(Context context, Runnable onAdDismissed) {
        if (mInterstitialAd != null && context instanceof Activity) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    loadInterstitialAd(context);
                    onAdDismissed.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    mInterstitialAd = null;
                    loadInterstitialAd(context);
                    onAdDismissed.run();
                }
            });

            mInterstitialAd.show((Activity) context);
        } else {
            loadInterstitialAd(context);
            onAdDismissed.run();
        }
    }
}