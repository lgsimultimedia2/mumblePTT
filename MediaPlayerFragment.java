package com.jio.jiotalkie.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.jio.jiotalkie.dataclass.UserTalkState;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.Objects;

public class MediaPlayerFragment extends Fragment{
    private PlayerView playerViewFullScreen;
    private String mediaPath;
    private boolean isVideo;
    ImageView imageView;
    ExoPlayer player;
    private DashboardViewModel mViewModel;
    private LiveData<UserTalkState> mUserTalkStateObserver;


    public MediaPlayerFragment() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mediaPath = getArguments().getString(EnumConstant.MEDIA_PATH);
            isVideo = getArguments().getBoolean(EnumConstant.IS_VIDEO);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.video_player_fragment, container, false);
        mViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(DashboardViewModel.class);
        playerViewFullScreen = view.findViewById(R.id.player_view_full);
        imageView = view.findViewById(R.id.galleryImageView);
        if (isVideo) {
            playVideo();
        } else {
            showImage();
        }
        registerViewModeObserver();
        return view;
    }

    public void playVideo() {
        playerViewFullScreen.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        player = new ExoPlayer.Builder(getActivity().getApplicationContext()).build();
        playerViewFullScreen.setPlayer(player);
        MediaItem firstItem = MediaItem.fromUri(mediaPath);
        player.addMediaItem(firstItem);
        player.prepare();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if (playbackState == Player.STATE_READY) player.play();
            }
        });
    }

    private void showImage() {
        playerViewFullScreen.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        Glide.with(this).load(mediaPath).into(imageView);
    }

    private void registerViewModeObserver() {
        mUserTalkStateObserver = mViewModel.observeSelfUserTalkState();
        mUserTalkStateObserver.observe(this,observerUserTalkState);
    }

    private final Observer<UserTalkState> observerUserTalkState = new Observer<UserTalkState>() {
        @Override
        public void onChanged(UserTalkState userTalkState) {
            if (userTalkState.getUserTalkState() == EnumConstant.userTalkState.TALKING){
                if (player !=null && player.isPlaying())player.pause();
            }
            else if (userTalkState.getUserTalkState() == EnumConstant.userTalkState.PASSIVE){
                Log.v("Video",userTalkState.getUserTalkState().toString());
                if (player !=null && !player.getPlayWhenReady() && player.getPlaybackState() != Player.STATE_READY){
                    player.play();
                }
            }
        }
    };

    public boolean isMediaPlayerPlaying(){
        if(player!=null && player.isPlaying())
            return true;

        return false;
    }

    public void pauseVideoPlay(){
        if(player!=null && player.isPlaying()){
            player.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player!=null)player.release();
    }
}