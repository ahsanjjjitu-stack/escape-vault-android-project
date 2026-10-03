package com.example.escapevault;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class Splash_Activity extends AppCompatActivity {


    private static final long SPLASH_HOLD_MS = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);


        ImageView icon = findViewById(R.id.splashIcon);
        TextView name = findViewById(R.id.splashName);
        TextView tag = findViewById(R.id.splashTag);

        // Views start invisible; the animation XML fades/slides them in (fillAfter="true"),
        // this just avoids a one-frame flash of the final state before the animation starts.
        icon.setVisibility(View.INVISIBLE);
        name.setVisibility(View.INVISIBLE);
        tag.setVisibility(View.INVISIBLE);

        icon.post(() -> {
            playStagger(icon, R.anim.slide_up_icon);
            playStagger(name, R.anim.slide_up_name);
            playStagger(tag, R.anim.slide_up_tag);
        });

        // Move on to the main screen after the intro finishes.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(Splash_Activity.this, MainActivity.class));
            finish();
            overridePendingTransition(0, 0);
        }, SPLASH_HOLD_MS);




    }


    private void playStagger(View view, int animRes) {
        view.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(this, animRes);
        view.startAnimation(anim);
    }


}