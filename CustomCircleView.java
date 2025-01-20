// This class represents a custom View component that displays a circular shape with various states,
// such as speaking, receiving, or default. It includes functionalities for updating the color/image
// based on the current state, drawing the circular shape, animating the filling of the circle, and
// starting a timer.

package com.jio.jiotalkie.drawable;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;


import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.util.EnumConstant;

public class CustomCircleView extends View {
    private final static String TAG = CustomCircleView.class.getName();
    private final Handler handler = new Handler(Looper.getMainLooper()); // Handler for managing timed tasks
    private final Handler handler2 = new Handler(Looper.getMainLooper()); // Handler for managing timed tasks
    int currentIndex = 0;
    String type = "" ;
    private String userState = ""; // Current state of the custom view
    private Paint paint; // Paint object for drawing the circular shape
    private Drawable imageResource; // Drawable image resource to be displayed
    private float sweepAngle = 0f; // Angle for the filling animation
    private Path arcPath; // Path for drawing the circular arc

    private Path arcPath1;
    private int timeInSeconds; // Time in seconds for the timer
    private Paint paint2; // Paint object for drawing the background of the speaking state

    private Paint paint1;
    Drawable[] speakerdrawable = {getResources().getDrawable(R.drawable.ic_speaker_2), getResources().getDrawable(R.drawable.ic_speaker_3), getResources().getDrawable(R.drawable.ic_speaker_4)};
    Drawable[] receievdrawable = {getResources().getDrawable(R.drawable.ic_receiver_1), getResources().getDrawable(R.drawable.ic_receiver_2), getResources().getDrawable(R.drawable.ic_receiver_3)};

    Context mContext;
    private CustomViewInterface customViewInterface; // Interface for communicating with the parent
    private Runnable timerRunnable;

    private int mDensityMetric;

    private int strokeWidth;

    // Interface definition for callbacks to the parent
    public interface CustomViewInterface {
        void onProgressTimerUpdate(String timeInStringFormat);
    }

    // Setter method for setting the callback interface
    public void setCustomViewInterface(CustomViewInterface customViewInterface) {
        this.customViewInterface = customViewInterface;
    }

    // Constructors
    public CustomCircleView(Context context) {
        super(context);
        mContext=context;
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        mDensityMetric = metrics.densityDpi;
        //init();
    }

    public CustomCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //init();
    }

    public CustomCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //init();
    }

    // Method to set the color/image based on the user state
    public void setColorImage(String userState) {
        this.userState = userState;

        // Determine the image resource based on the user state
        switch (userState) {
            case EnumConstant.USER_STATE_SPEAKER:
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(loadImageRunnable,100);
                break;
            case EnumConstant.USER_STATE_RECEIVER:
                handler2.removeCallbacksAndMessages(null);
                //this.imageResource = getResources().getDrawable(R.drawable.ic_wave_receive);
                handler2.postDelayed(loadrecievRunnable, 100);
                break;
            case EnumConstant.USER_STATE_DEFAULT:
                handler.removeCallbacksAndMessages(null);
                handler2.removeCallbacksAndMessages(null);
                this.imageResource = getResources().getDrawable(R.drawable.ic_wave_normal);
                break;
            default:
                break;
        }

        init(); // Re-initialize to update color
        invalidate(); // Invalidate the view to trigger redraw

    }

    private void invalidateView() {
        invalidate();
    }

    // Initialize paint objects
    private void init() {
        if(mDensityMetric >= DisplayMetrics.DENSITY_450) {
            strokeWidth=getResources().getInteger(R.integer.stroke_width);
        }else if(mDensityMetric >= DisplayMetrics.DENSITY_420){
            strokeWidth=getResources().getInteger(R.integer.stroke_width_medium);
        }else if (mDensityMetric >= DisplayMetrics.DENSITY_XHIGH) {
            strokeWidth=getResources().getInteger(R.integer.stroke_width_low);
        }else if (mDensityMetric >= DisplayMetrics.DENSITY_280){
            strokeWidth=getResources().getInteger(R.integer.stroke_width_very_low);
        }
        paint = new Paint();
        paint2 = new Paint();
        paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Set paint color based on the user state
        switch (userState) {
            case EnumConstant.USER_STATE_SPEAKER:
                paint.setColor(getResources().getColor(R.color.color_speaker));
                break;
            case EnumConstant.USER_STATE_RECEIVER:
                paint.setColor(getResources().getColor(R.color.message_receiver_color));
                break;
            case EnumConstant.USER_STATE_DEFAULT:
                paint.setColor(getResources().getColor(R.color.default_state_color));
                break;
            default:
                break;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);

        paint2.setColor(getResources().getColor(R.color.while_talking_background));
        paint2.setStrokeWidth(strokeWidth);
        paint2.setStyle(Paint.Style.STROKE);

        // Make sure to set anti-alias flag
        paint1.setColor(Color.WHITE);
        paint1.setStyle(Paint.Style.FILL);
    }

    // Draw method for drawing the custom view
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int screenWidth = getWidth();
        int screenHeight = getHeight();
        int shiftHorizontal;
        if(mDensityMetric >= DisplayMetrics.DENSITY_450){
            shiftHorizontal = 150;
        }else if(mDensityMetric >= DisplayMetrics.DENSITY_420){
            shiftHorizontal =130;
        }else{
            shiftHorizontal =100;
        }
        int cx = (int)(screenWidth / 1.5) - shiftHorizontal;
        int cy = (int)(screenHeight / 1.5) - 45;
        int radius;
        if(mDensityMetric >= DisplayMetrics.DENSITY_450){
            radius = (int)(Math.min(screenWidth, screenHeight) / 3.0);
        }else{
            radius = (int)(Math.min(screenWidth, screenHeight) / 2.5);
        }
        int imageWidth;
        int imageHeight;
        int radius1 = radius;
        if(mDensityMetric >= DisplayMetrics.DENSITY_450) {
            radius1 = radius + 5;
        }else if(mDensityMetric >= DisplayMetrics.DENSITY_420){
            radius1= radius-20;
            radius=radius-22;
            cx=cx-10;
        }
        arcPath1 = new Path();
        arcPath1.addArc(cx - radius1-strokeWidth, cy - radius1-strokeWidth, cx + radius1+strokeWidth, cy + radius1+strokeWidth, 0, 360);
        canvas.drawPath(arcPath1, paint1);

        arcPath = new Path();
        arcPath.addArc(cx - radius, cy - radius, cx + radius, cy + radius, 0, 360);
        canvas.drawPath(arcPath, paint);

        // Draw the background for the speaking state
        if (userState.equals(EnumConstant.USER_STATE_SPEAKER)) {
            canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, 270, -sweepAngle, false, paint2);
        }

        if (mDensityMetric <= DisplayMetrics.DENSITY_XHIGH) {
            imageWidth = getResources().getInteger(R.integer.inner_wave_width_low);
            imageHeight = getResources().getInteger(R.integer.inner_wave_height_low);
        }else{
            imageWidth = getResources().getInteger(R.integer.inner_wave_width);
            imageHeight = getResources().getInteger(R.integer.inner_wave_height);
        }
        // Calculate image bounds and draw the image resource
        int left = cx - (imageWidth / 2);
        int top = cy - (imageHeight / 2);
        int right = cx + (imageWidth / 2);
        int bottom = cy + (imageHeight / 2);
        if(imageResource!=null)
        {
            Drawable image = imageResource;
            image.setBounds(left, top, right, bottom);
            image.draw(canvas);
        }
    }

    // Method to draw shadow borders around the circular shape
    private void drawBorderShadow(int cx, int cy, Canvas canvas, int radius) {
        Paint shadowPaint1 = new Paint();
        shadowPaint1.setColor(Color.argb(30, 200, 200, 200));
        shadowPaint1.setStyle(Paint.Style.STROKE);
        shadowPaint1.setStrokeWidth(getResources().getInteger(R.integer.shadow_border_thickness));
        canvas.drawCircle(cx, cy, radius + 20/*+ 70*/, shadowPaint1);

        Paint shadowPaint2 = new Paint();
        shadowPaint2.setColor(Color.argb(25, 200, 200, 200));
        shadowPaint2.setStyle(Paint.Style.STROKE);
        shadowPaint2.setStrokeWidth(10);
        canvas.drawCircle(cx, cy, radius +30/*+ 84*/, shadowPaint2);
    }




    // Method to start the filling animation of the circular shape
    public void startFillingAnimation() {
        if (!userState.equals(EnumConstant.USER_STATE_SPEAKER))
            return;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);
        if (userState.equals(EnumConstant.USER_STATE_SPEAKER)) {
            animator.setDuration(60000); // Set the duration of the animation to 60 seconds
//            startTimer(); // Start the timer
            customViewInterface.onProgressTimerUpdate("00:60 sec"); // Update the timer text

        }

        // Update the sweep angle during the animation
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sweepAngle = (float) animation.getAnimatedValue();
                invalidate(); // Invalidate the view to trigger redraw
            }
        });
        animator.start(); // Start the animation
    }

    Runnable loadrecievRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentIndex < receievdrawable.length) {
                    imageResource = receievdrawable[currentIndex];
                    currentIndex++;
                    invalidateView();
                    handler2.postDelayed(loadrecievRunnable, 300);
                } else {
                    currentIndex = 0;
                    handler2.postDelayed(loadrecievRunnable, 300);
                }
            }
    };


    Runnable loadImageRunnable = new Runnable() {
        @Override
        public void run() {
                if (currentIndex < speakerdrawable.length) {
                    imageResource = speakerdrawable[currentIndex];
                    currentIndex++;
                    handler.postDelayed(this, 300);
                } else {
                    currentIndex = 0;
                    handler.postDelayed(this, 300);

            }
        }

    };
}
