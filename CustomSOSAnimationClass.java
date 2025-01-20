package com.jio.jiotalkie.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.jio.jiotalkie.dispatch.R;

import java.util.ArrayList;
import java.util.List;

public class CustomSOSAnimationClass {

    boolean isPaused;
    boolean justResumed;
    boolean isStopped;
    ImageView mImageView;
    Context context;
    List<Drawable> animList = new ArrayList<>();
    int mDuration;
    int current = 0;
    ImageView zerothView;
    int timer = 10;
    TextView mTimerTextView;

    public CustomSOSAnimationClass(Context context, ImageView imageView, int duration, TextView sosTimerText) {
        this.context = context;
        this.mImageView = imageView;
        this.mDuration = duration;
        this.mTimerTextView = sosTimerText;
        zerothView = imageView;
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve0));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve1));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve2));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve3));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve4));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve5));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve6));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_receive7));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve8));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve9));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve10));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_receive11));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve12));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_receive13));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve14));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve15));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve16));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve17));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve18));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve19));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve20));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve21));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve22));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve23));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve24));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve25));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve26));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve27));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve28));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve29));
        animList.add(AppCompatResources.getDrawable(context, R.drawable.ic_sos_wave_recieve30));

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
                mImageView.setImageDrawable(animList.get(current));

                current = current + 1;
                if (current % 3 == 0) {
                    mTimerTextView.setText(context.getString(R.string.sos_play_timer, timer));
                    timer = timer - 1;
                }
            }
            handler.postDelayed(this, mDuration / animList.size());
        }
    };


    public void start() {
        current = 0;
        timer = 10;
        isStopped = false;
        handler.postDelayed(runnable, mDuration / animList.size());
    }

    public void pause() {
        if (current <= animList.size())
            mImageView.setImageDrawable(animList.get(current));
        isPaused = true;
    }

    public void resume() {
        justResumed = true;
        isPaused = false;
    }

    public void stop() {
        isStopped = true;
        current = 0;
        timer = 10;
        mImageView.setImageDrawable(animList.get(current));
        mTimerTextView.setText(context.getString(R.string.sos_play_timer, timer));
        handler.removeCallbacks(runnable);
    }

}
