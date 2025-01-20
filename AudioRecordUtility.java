package com.jio.jiotalkie.media;

import android.content.Context;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.jio.jiotalkie.adapter.provider.MediaDataProvider;

import java.io.File;
import java.io.IOException;

public class AudioRecordUtility {

    private static String TAG = AudioRecordUtility.class.getName();
    private static AudioRecordUtility audioRecordUtility = null;

    private MediaRecorder mediaRecorder;
    private String audioFilePath;

    private static Context mContext;

    private static MediaDataProvider mAudioRecordeProvider;

    private AudioRecordUtility(){

    }

    public static AudioRecordUtility getInstance(Context context, MediaDataProvider audioRecordProvider){
        mAudioRecordeProvider = audioRecordProvider;
        mContext = context;
        if(audioRecordUtility==null){
            audioRecordUtility = new AudioRecordUtility();
        }
        return audioRecordUtility;
    }

    public void startRecording() {
        File audioRecordDirectory = new File("/sdcard/Download");
        if(!audioRecordDirectory.exists()){
            audioRecordDirectory.mkdirs();
        }
        File audioFile = new File(audioRecordDirectory, System.currentTimeMillis()+"recorded_audio.3gp");
        if(!audioFile.exists()){
            try {
                audioFile.createNewFile();
            }catch (Exception e){
                Log.d(TAG,"Start Recording create file exception "+Log.getStackTraceString(e));
            }
        }
        if(audioFile.exists()) {
            audioFilePath = audioFile.getAbsolutePath();
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setMaxDuration(10000);
            mediaRecorder.setOnInfoListener((mr, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    releaseMediaRecorder();
                    //Toast.makeText(mContext,"Recording Completed, File upload start",Toast.LENGTH_LONG).show();
                    Uri audioUri = FileProvider.getUriForFile(mContext, "com.jio.jiotalkie.dispatch.provider", audioFile);
                    mAudioRecordeProvider.getMediaUri(audioUri);
                }
            });
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Toast.makeText(mContext,"Audio recording started internally",Toast.LENGTH_LONG).show();
                Log.d(TAG, "Recording started");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to start recording: " + e.getMessage());
            }
        }
    }

    public void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                Log.d("AudioRecorder", "Recording stopped, file saved: " + audioFilePath);
            }catch (Exception e){
                Log.d(TAG,"stop recording exception "+Log.getStackTraceString(e));
            }
        }
    }

    private void releaseMediaRecorder(){
        if(mediaRecorder!=null){
            mediaRecorder.release();
            mediaRecorder=null;
        }
    }
}
