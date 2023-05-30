package com.example.energychaser;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class single_player_activity extends AppCompatActivity
{
    SeekBar seekBarsingle;
    TextView textViewsingle;
    private Integer countsec = 20;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_player);
        seekBarsingle = (SeekBar)findViewById(R.id.seekBarSingle);
        textViewsingle = (TextView)findViewById(R.id.textViewSingle);

        seekBarsingle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int sec, boolean b)
            {
                // Όσο τραβάμε την seekbar αυξάνουμε ή μειώνουμε τον χρόνο παιχνιδιού ανα 5 δευτερόλεπτα
                sec = sec / 5;
                sec = sec * 5;
                // Εδω εμφανίζονται τα δευτερόλεπτα στην textViewSingle
                textViewsingle.setText("Δευτερόλεπτα :" + sec);
                // Περνάμε τα δευτερόλεπτα σε μια μεταβλητή.
                countsec = sec;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        Button camerascan = (Button) findViewById(R.id.camerascan);//single mode
        camerascan.setOnClickListener(new View.OnClickListener() {
            // Με το πάτημα του κουμπιού μετατρέπουμε τα δευτερόλεπτα σε χιλιοστά του δευτερολέπτου και στέλνουμε την
            // τιμή με intent.PutExtra στην επόμενη κλάση, για να αρχίσει η αντιστροφη μέτρηση, παράλληλα
            //προωθείτε και όλη η δραστηριότητα στην activity_scan_list, όπου αρχίζει και το ψάξιμο.
            @Override
            public void onClick(View v) {
                openSingleActivity(countsec);
            }
        });
    }
    public void openSingleActivity(int value) {
        Integer countime = value;
        //Method To Pass The Seconds and redirect
        if(countime != null || countime != 0 ){countime = value*1000;}
        else{countime=20000;}
        Intent intent = new Intent(single_player_activity.this, activity_scan_list.class);
        intent.putExtra("MY_INTEGER", countime);
        startActivity(intent);
    }
}