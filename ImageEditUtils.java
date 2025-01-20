package com.jio.jiotalkie.util;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.jio.jiotalkie.dispatch.R;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

public class ImageEditUtils {

    public static Bundle getImageEditBundle(Activity mActivity, Uri sourceUri, Uri destinationUri) {
        Bundle uCropBundle = new Bundle();

        // Set the source image URI (file path or content URI)
        uCropBundle.putParcelable(UCrop.EXTRA_INPUT_URI, sourceUri);
        // Set the destination image URI (where the cropped image will be saved)
        uCropBundle.putParcelable(UCrop.EXTRA_OUTPUT_URI, destinationUri);
        // Set the UCrop.Options in the Bundle
        uCropBundle.putInt(UCrop.Options.EXTRA_COMPRESSION_QUALITY, 100);
        uCropBundle.putBoolean(UCrop.Options.EXTRA_FREE_STYLE_CROP, true);
        uCropBundle.putBoolean(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false);
        uCropBundle.putFloat(UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER, 1.0001f);
        uCropBundle.putIntArray(UCrop.Options.EXTRA_ALLOWED_GESTURES,new int[]{UCropActivity.ROTATE, UCropActivity.SCALE});
        uCropBundle.putInt(UCrop.Options.EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE, mActivity.getColor(R.color.colorPrimary));

        uCropBundle.putFloat(UCrop.EXTRA_ASPECT_RATIO_X,0);
        uCropBundle.putFloat(UCrop.EXTRA_ASPECT_RATIO_Y,0);

        return uCropBundle;
    }
}
