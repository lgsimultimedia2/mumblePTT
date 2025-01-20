package com.jio.jiotalkie.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import com.jio.jiotalkie.dispatch.R;

import java.util.ArrayList;
import java.util.List;


public class CustomAnimationClass {

    boolean isPaused;
    boolean justResumed;
    boolean isStopped;
    ImageView mImageView;
    Context mContext;
    List<Drawable> animList = new ArrayList<>();
    int mDuration;
    int current = 0;
    public CustomAnimationClass(Context context, ImageView imageView, int duration) {
        this.mContext = context;
        this.mImageView = imageView;
        this.mDuration = duration;
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_1));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_2));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_3));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_4));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_5));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_6));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_7));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_8));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_9));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_10));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_11));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_12));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_13));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_14));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_15));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_16));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_17));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_18));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_19));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_20));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_21));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.waveform_22));
    }

    Handler handler = new Handler();

    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!isPaused) {
                if (justResumed) {
                    justResumed = false;

                }
                if (current >= animList.size()) {
                    current = 0;
                }
                mImageView.setBackground(animList.get(current));
                current = current + 1;
            }
            handler.postDelayed(this, mDuration/ animList.size());
        }
    };

    public void start() {
        current = 0;
        isStopped = false;
        handler.postDelayed(runnable, mDuration/animList.size());
    }

    public void pause() {
        if(current < animList.size())
            mImageView.setBackground(animList.get(current));
        isPaused = true;
    }

    public void resume() {
        justResumed = true;
        isPaused = false;
    }

    public void stop() {
        isStopped = true;
        current = 0;
        mImageView.setBackground(animList.get(current));
        handler.removeCallbacks(runnable);
    }
}