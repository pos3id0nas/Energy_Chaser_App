package com.example.energychaser;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // VIDEO AUTOPLAY AT LOADING
        VideoView videoView = findViewById(R.id.video_view);
        videoView.setVideoPath("android.resource://"+getPackageName()+"/"+R.raw.entrance);

        videoView.setMediaController(new MediaController((Context) this));
        videoView.start();

        // REDIRECT TO THE NEXT PAGE AFTER 5SEC
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent((Context) MainActivity.this, ButtonsActivity.class);
                startActivity(intent);
                finish();
            }
        },14000);
    }
}
