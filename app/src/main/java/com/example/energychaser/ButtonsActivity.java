package com.example.energychaser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class ButtonsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buttons);
        //Credits
        TextView clicabletext = (TextView) this.findViewById(R.id.clicabletext);
        clicabletext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageView cancel;
                //will create a view of our custom dialog layout
                View alertCustomdialog = LayoutInflater.from((Context) ButtonsActivity.this).inflate(R.layout.credits_popup, null);
                //initialize alert builder.
                AlertDialog.Builder alert = new AlertDialog.Builder((Context) ButtonsActivity.this);

                //set our custom alert dialog to tha alertdialog builder
                alert.setView(alertCustomdialog);
                cancel = (ImageView) alertCustomdialog.findViewById(R.id.cancel_button);
               // cancel = (ImageView) alertCustomdialog.findViewById(R.id.cancel_button1);
                final AlertDialog dialog = alert.create();
                //this line removed app bar from dialog and make it transperent and you see the image is like floating outside dialog box.
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                //finally show the dialog box in android all
                dialog.show();
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        });
        //On click Listener για MODES
        Button button01 = (Button) findViewById(R.id.button1);//single mode
        button01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSingleActivity();
            }
        });
    }
    public void openSingleActivity() {
        Intent intent = new Intent((Context) this, single_player_activity.class);
        startActivity(intent);
    }
}