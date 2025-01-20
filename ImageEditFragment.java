package com.jio.jiotalkie.fragment;

import static android.view.View.GONE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.dispatch.R;
import com.yalantis.ucrop.UCropFragment;

public class ImageEditFragment extends UCropFragment {

    ViewGroup viewGroup = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        DashboardActivity mActivity = ((DashboardActivity)getActivity());
        mActivity.showToolWithBack(getActivity().getString(R.string.image_edit));
        mActivity.needBottomNavigation(false);

        this.viewGroup = view.findViewById(R.id.ucrop_photobox);
        Toolbar mToolbar = mActivity.findViewById(R.id.toolbar_main);
        mToolbar.findViewById(R.id.actionCalender).setVisibility(GONE);
        mToolbar.findViewById(R.id.actionCropImage).setVisibility(View.VISIBLE);

        mToolbar.findViewById(R.id.actionCropImage).setOnClickListener(v -> {
            ImageEditFragment.this.cropAndSaveImage();
        });

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStop() {
        // Remove all child views from the parent view
        if (viewGroup != null) {
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                viewGroup.removeViewAt(0); // Remove the first child (index 0)
            }
        }
        super.onStop();
    }

}

