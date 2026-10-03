package com.example.escapevault;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {

    // Threshold 2.7f dilam jate halka naracharay na hoy
    private float SHAKE_THRESHOLD_GRAVITY = 2.7f;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private static final int SHAKE_COOLDOWN_MS = 500;
    private static final int MIN_SHAKE_COUNT = 3;

    private OnShakeListener mListener;
    private long mShakeTimestamp;
    private long mLastTriggerTime;
    private int mShakeCount = 0;

    public interface OnShakeListener {
        void onShake();
    }


    // থ্রেশহোল্ড সেট করার জন্য নতুন মেথড
    public void setSensitivityThreshold(float threshold) {
        this.SHAKE_THRESHOLD_GRAVITY = threshold;
    }


    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mListener == null) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            final long now = System.currentTimeMillis();

            // Action trigger hooyar por 3 second cooldown
            if (now - mLastTriggerTime < SHAKE_COOLDOWN_MS) {
                return;
            }

            // Jodi onnodike jhakuni dite 1.5 second er besi deri hoye jay, counter reset hobe
            if (mShakeTimestamp + 1500 < now) {
                mShakeCount = 0;
            }

            // Slop time check (por por jhakunir majhe minimum gap)
            if (mShakeTimestamp + SHAKE_SLOP_TIME_MS < now) {
                mShakeTimestamp = now;
                mShakeCount++;

                // Minimum 2 bar jhakuni dilei shudhu trigger hobe
                if (mShakeCount >= MIN_SHAKE_COUNT) {
                    mShakeCount = 0;
                    mLastTriggerTime = now;
                    mListener.onShake();
                }
            }
        }
    }
}