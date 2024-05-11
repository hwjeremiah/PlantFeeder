package com.example.physics_weseek;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    ProgressBar moistProgress;
    ProgressBar waterProgress;
    Button btnWater;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        init();
        setupDialog();
        setupButtonClick();
    }

    private void init() {
        moistProgress = findViewById(R.id.progBarMoistLevel);
        moistProgress.setProgress(60);

        waterProgress = findViewById(R.id.progBarWaterLevel);
        waterProgress.setProgress(20);
    }

    private void setupDialog() {
        dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.custom_dialog);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.container_rounded_corner));
        }
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);

        TextView yes = dialog.findViewById(R.id.tvYes);
        TextView no = dialog.findViewById(R.id.tvNo);

        yes.setOnClickListener(view -> {
            waterPlant();
            Toast.makeText(MainActivity.this, "OKAY", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        no.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "CANCEL", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void setupButtonClick() {
        btnWater = findViewById(R.id.btnWaterPlant);
        btnWater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });
    }
    private void waterPlant() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://ESP_IP_ADDRESS/water_plant");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.connect();

                    // InputStream in = conn.getInputStream(); -> read response if needed


                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
