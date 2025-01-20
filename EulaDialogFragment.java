package com.jio.jiotalkie.fragment;


import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

import com.jio.jiotalkie.dispatch.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class EulaDialogFragment extends DialogFragment {
    private static final String TAG = EulaDialogFragment.class.getName();
    private AssetManager mAssetManager;
    public interface EulaAcceptCallBack {
        void onAccepted();
    }
    private EulaAcceptCallBack mEulaAcceptCallBack;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAssetManager = getActivity().getAssets();
    }

    public void setEulaAcceptCallBack(EulaAcceptCallBack eulaAcceptCallBack) {
        mEulaAcceptCallBack = eulaAcceptCallBack;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.eula_dialog_layout, container, false);
        getDialog().setCancelable(false);
        setCancelable(false);
        TextView eulaTv = view.findViewById(R.id.eula_info);
        eulaTv.setText(Html.fromHtml(getHtmlFile("eula.html"), HtmlCompat.FROM_HTML_MODE_LEGACY));
        view.findViewById(R.id.accept).setOnClickListener(view1 -> {
            dismiss();
        });
        return view;
    }
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
    public String getHtmlFile(String fileName) {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = mAssetManager.open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception occur while load file from asset folder : "+e.getMessage());
        }
        return builder.toString();
    }
}
