package com.jio.jiotalkie.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.jio.jiotalkie.dispatch.R;

import java.util.concurrent.ThreadLocalRandom;

public class BitmapUtils {
    public static Bitmap resizeKeepingAspect(Bitmap image, int maxWidth, int maxHeight){
        int width = image.getWidth();
        int height = image.getHeight();

        if (width < maxWidth && height < maxHeight) {
            return image;
        }

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
    }

    public static void setRandomBgColor(Context context, TextView userIconBg, boolean isOnline) {
        int[] colors = {R.color.user_icon_bg1, R.color.user_icon_bg2, R.color.user_icon_bg3,
                R.color.user_icon_bg4, R.color.user_icon_bg5, R.color.user_icon_bg6,
                R.color.user_icon_bg7};
        int randomNum = ThreadLocalRandom.current().nextInt(0, colors.length);
        Drawable background = userIconBg.getBackground();
        if (background instanceof GradientDrawable) {
            // cast to 'GradientDrawable'
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            if (isOnline) {
                gradientDrawable.setColor(ContextCompat.getColor(context, colors[randomNum]));
            } else {
                gradientDrawable.setColor(ContextCompat.getColor(context, R.color.user_icon_bg_offline));
            }

        }
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;

        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap squaredBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(squaredBitmap);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        paint.setAntiAlias(true);

        canvas.drawCircle(size / 2, size / 2, size / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);

        return squaredBitmap;
    }
}
