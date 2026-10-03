package com.example.escapevault.call_setup_pack;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Handles 360-degree free dragging for Accept and Decline buttons
 * while using the Floating Bob (Y-translation) idle animation from IncomingFakeCallActivity.
 */
public class CircularCallSwipeView extends View {

    public interface OnSwipeListener {
        void onAccept();
        void onDecline();
    }

    private static final float TRIGGER_THRESHOLD_DP = 70f;
    private float triggerThresholdPx;

    private OnSwipeListener listener;
    private View btnDecline, btnAccept;

    private View activeView = null;
    private float initialTouchX, initialTouchY;
    private float initialTranslationX, initialTranslationY;
    private boolean isActionTriggered = false;

    // তোর অ্যাক্টিভিটির মতো ObjectAnimator ফ্লোটিং অ্যানিমেশন
    private ObjectAnimator acceptFloatAnim;
    private ObjectAnimator declineFloatAnim;

    public CircularCallSwipeView(Context context) {
        super(context);
        init(context);
    }

    public CircularCallSwipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        triggerThresholdPx = TRIGGER_THRESHOLD_DP * density;
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.listener = listener;
    }

    public void bindButtons(View decline, View accept) {
        this.btnDecline = decline;
        this.btnAccept = accept;

        setupTouchListener();
        post(this::startIdleAnimation);
    }

    /**
     * তোর অ্যাক্টিভিটিতে ঠিক যেভাবে setupCallButtonAnimation-এ ObjectAnimator ব্যবহার করেছিস,
     * সেই একই Floating Bob (0f -> -14f -> 0f) অ্যানিমেশন।
     */
    public void startIdleAnimation() {
        if (isActionTriggered) return;

        stopIdleAnimation();

        if (btnDecline != null) {
            declineFloatAnim = ObjectAnimator.ofFloat(btnDecline, "translationY", 0f, -14f, 0f);
            declineFloatAnim.setDuration(2200);
            declineFloatAnim.setRepeatCount(ValueAnimator.INFINITE);
            declineFloatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            declineFloatAnim.setStartDelay(0);
            declineFloatAnim.start();
        }

        if (btnAccept != null) {
            acceptFloatAnim = ObjectAnimator.ofFloat(btnAccept, "translationY", 0f, -14f, 0f);
            acceptFloatAnim.setDuration(2200);
            acceptFloatAnim.setRepeatCount(ValueAnimator.INFINITE);
            acceptFloatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            acceptFloatAnim.setStartDelay(600); // হালকা টাইম অফসেট
            acceptFloatAnim.start();
        }
    }

    public void stopIdleAnimation() {
        if (acceptFloatAnim != null) {
            acceptFloatAnim.cancel();
            acceptFloatAnim = null;
        }
        if (declineFloatAnim != null) {
            declineFloatAnim.cancel();
            declineFloatAnim = null;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        OnTouchListener touchListener = (v, event) -> {
            if (isActionTriggered) return true;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (activeView != null) return true;
                    activeView = v;

                    // টাচ করলেই তোর অরিজিনাল পজ সিলেক্টেড অ্যানিমেশন পজ করা
                    if (v == btnAccept && acceptFloatAnim != null) acceptFloatAnim.pause();
                    if (v == btnDecline && declineFloatAnim != null) declineFloatAnim.pause();

                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    initialTranslationX = v.getTranslationX();
                    initialTranslationY = v.getTranslationY();

                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    if (activeView != v) return true;

                    // ৩৬০ ডিগ্রি যেকোনো দিকে ড্র্যাগ লজিক
                    float deltaX = event.getRawX() - initialTouchX;
                    float deltaY = event.getRawY() - initialTouchY;

                    float newTransX = initialTranslationX + deltaX;
                    float newTransY = initialTranslationY + deltaY;

                    v.setTranslationX(newTransX);
                    v.setTranslationY(newTransY);

                    // কতখানি ডিস্টেন্স ড্র্যাগ হলো তার হিসাব
                    double draggedDistance = Math.hypot(newTransX, newTransY);

                    if (draggedDistance >= triggerThresholdPx) {
                        isActionTriggered = true;
                        stopIdleAnimation();

                        if (listener != null) {
                            if (v == btnAccept) {
                                listener.onAccept();
                            } else if (v == btnDecline) {
                                listener.onDecline();
                            }
                        }
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (activeView == v) {
                        if (!isActionTriggered) {
                            bounceBack(v);
                        }
                        activeView = null;
                    }
                    return true;
            }
            return false;
        };

        if (btnAccept != null) btnAccept.setOnTouchListener(touchListener);
        if (btnDecline != null) btnDecline.setOnTouchListener(touchListener);
    }

    /**
     * বাটন ছেড়ে দিলে স্প্রিং বাউন্স ব্যাক করে জায়গায় আসবে এবং অবজেক্ট এনিমেশন রিজুম হবে।
     */
    private void bounceBack(View v) {
        v.animate()
                .translationX(0f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .withEndAction(() -> {
                    if (!isActionTriggered) {
                        if (v == btnAccept && acceptFloatAnim != null) acceptFloatAnim.resume();
                        if (v == btnDecline && declineFloatAnim != null) declineFloatAnim.resume();
                    }
                })
                .start();
    }
}