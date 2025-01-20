package com.jio.jiotalkie.drawable;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;


import com.jio.jiotalkie.dispatch.R;


public class SOSCustomCircleView extends View {
    private final Handler handler = new Handler(Looper.getMainLooper()); // Handler for managing timed tasks
    int currentIndex = 0;
    String type = "";

    private int counterTime = 10;
    private int circleColor; // Default circle color

    private int circleRadius = 100; // Default circle radius

    private Paint paint;

    private Drawable imageResource; // Drawable image resource to be displayed
    private float sweepAngle = 0f; // Angle for the filling animation
    private Path arcPath; // Path for drawing the circular arc
    // Time in seconds for the timer
    private Paint paint2; // Paint object for drawing the background of the speaking state
    Drawable[] speakerdrawable = {getResources().getDrawable(R.drawable.ic_sos_speaker2), getResources().getDrawable(R.drawable.ic_sos_speaker3), getResources().getDrawable(R.drawable.ic_sos_speaker4)};
    private CountDownTimer countDownTimer;

    public interface CountdownListener {
        void onTimerFinish();
    }

    private CountdownListener countdownListener;

    // Method to set the listener
    public void setCountdownListener(CountdownListener listener) {
        this.countdownListener = listener;
    }

    // Constructors
    public SOSCustomCircleView(Context context) {
        super(context);
        //  init();
    }

    public SOSCustomCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // init();
    }

    public SOSCustomCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // init();
    }

    private void invalidateView() {
        invalidate();
    }

    public void setColorImage() {
        init(); // Re-initialize to update color
        invalidate(); // Invalidate the view to trigger redraw
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG); // Make sure to set anti-alias flag
        paint.setColor(circleColor);
        paint.setStyle(Paint.Style.FILL);
        paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint2.setColor(getResources().getColor(R.color.sos_progress_bar_color));
        paint2.setStrokeWidth(getResources().getInteger(R.integer.stroke_width));
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeCap(Paint.Cap.ROUND);
        paint2.setAntiAlias(true);
    }
//    mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        mArcPaint.setColor(Color.WHITE);
//        mArcPaint.setStyle(Paint.Style.STROKE);
//        mArcPaint.setStrokeWidth(5);
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int screenWidth = getWidth();
        int screenHeight = getHeight();
        int cx = screenWidth / 2;
        int cy = (screenHeight / 2) - 90;
        int radius = Math.min(screenWidth, screenHeight) / 3;
        int colorHex = Color.parseColor("#D9008D");
        Paint colorPaint = new Paint();
        colorPaint.setColor(colorHex);
        canvas.drawCircle(cx, cy, radius, colorPaint);
        int imageWidth = 80;
        int imageHeight = 80;
        int left = cx - (imageWidth / 2);
        int top = cy - (imageHeight / 2);
        int right = cx + (imageWidth / 2);
        int bottom = cy + (imageHeight / 2);
        arcPath = new Path();
        arcPath.addArc(cx - radius, cy - radius, cx + radius, cy + radius, 0, 360);
        canvas.drawPath(arcPath, paint);

        canvas.drawArc(cx - radius+9, cy - radius+9, cx + radius-9, cy + radius-9, 270, -sweepAngle, false, paint2);

        Drawable timerImage = getResources().getDrawable(R.drawable.ic_timer);
        timerImage.setBounds(left, top - imageHeight, right, bottom - imageHeight);
        timerImage.draw(canvas);
        String text = String.valueOf(counterTime);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);
        if (Typeface.DEFAULT != null) {
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
        float textX = left + (right - left) / 2;
        float textY = top - imageHeight + (bottom - top) / 2 + textPaint.getTextSize() / 2;
        canvas.drawText(text, textX, textY, textPaint);
        int marginTop = dpToPx(30); // Convert 20dp to pixels
        int newTop = bottom - imageHeight + marginTop; // Position 20dp below the additional image

        // Draw the new image below the additional image
        int speakerWaveImageWidth = 200; // Adjust width as needed
        int speakerWaveImageHeight = 150; // Adjust height as needed
        int speakerWaveImageLeft = cx - (speakerWaveImageWidth / 2);
        int speakerWaveImageRight = cx + (speakerWaveImageWidth / 2);
        int speakerWaveImageBottom = newTop + speakerWaveImageHeight;
        Drawable speakerWaveImage = imageResource;
        speakerWaveImage.setBounds(speakerWaveImageLeft, newTop, speakerWaveImageRight, speakerWaveImageBottom);
        speakerWaveImage.draw(canvas);
    }
    public void startCountdownTimer() {
        countDownTimer = new CountDownTimer(counterTime * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the counterTime variable with the remaining time
                counterTime = (int) (millisUntilFinished / 1000);
                invalidate();
                handler.postDelayed(loadImageRunnable, 50);
            }

            @Override
            public void onFinish() {
                counterTime = 0;
                invalidateView();
                handler.removeCallbacks(loadImageRunnable);
                if (countdownListener != null) {
                    countdownListener.onTimerFinish();
                }
            }
        };
        countDownTimer.start();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }




    public void startFillingAnimation() {
        this.imageResource = getResources().getDrawable(R.drawable.ic_sos_speaker);
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);     ///if (userState.equals(PTTChannelFragment.USER_STATE_SPEAKER)) {
        animator.setDuration(9000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sweepAngle = (float) animation.getAnimatedValue();
                invalidate(); // Invalidate the view to trigger redraw
            }
        });
        animator.start(); // Start the animation
    }

    Runnable loadImageRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentIndex < speakerdrawable.length) {
                imageResource = speakerdrawable[currentIndex];
                currentIndex++;
                handler.postDelayed(this, 400);
            } else {
                currentIndex = 0;
                handler.postDelayed(this, 400);

            }
        }

    };

    public void forceStopAnimation() {
        counterTime = 0;
        invalidateView();
        handler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (countdownListener != null) {
            countdownListener.onTimerFinish();
        }
    }
}