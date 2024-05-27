package com.example.physics_weseek;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;


public class MainActivity extends AppCompatActivity {

    ProgressBar moistProgress;
    ProgressBar waterProgress;
    Button btnWater;
    Dialog dialog;

    TextView sched;
    TextView txtWaterLevel;
    TextView txtMoistureLevel;
    String response;
    private boolean isCommandScheduled = false;
    private int soilMoisture = 1010;
    private int previousSoilMoisture = soilMoisture;
    private int waterLevel = 200;
    private int previousWaterLevel = waterLevel;

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // Setup Network Connection
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        NetworkRequest request = builder.build();
        connManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                connManager.bindProcessToNetwork(network);
            }
        });
        sched = findViewById(R.id.tvSchedule);
        txtWaterLevel = findViewById(R.id.tvStatusWaterLevel);
        txtMoistureLevel = findViewById(R.id.tvStatuesMoistLevel);
        init();
        setupDialog();
        setupButtonClick();

        receiveDataFromESP8266();
    }

    private void init() {
        moistProgress = findViewById(R.id.progBarMoistLevel);
        moistProgress.setProgress(0);

        waterProgress = findViewById(R.id.progBarWaterLevel);
        waterProgress.setProgress(0);
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
            if (isCommandScheduled) {
                return;
            }

            // Set the flag to indicate that the command is scheduled
            isCommandScheduled = true;

            sendCommand("ewater");
            Toast.makeText(MainActivity.this, "OKAY", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendCommand("istap");
                    isCommandScheduled = false;
                }
            }, 25000);
            isCommandScheduled = true;
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

    public void sendCommand(String cmd) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String command = "http://192.168.4.1/" + cmd;
                Log.d("Command------------------------------------------", command);
                Request request = new Request.Builder().url(command).build();
                try {
                    Response response = client.newCall(request).execute();
                    String myResponse = response.body().string();
                    final String cleanResponse = myResponse.replaceAll("\\<.*?\\>", ""); // remove HTML tags
                    cleanResponse.replace("\n", ""); // remove all new line characters
                    cleanResponse.replace("\r", ""); // remove all carriage characters
                    cleanResponse.replace(" ", ""); // removes all space characters
                    cleanResponse.replace("\t", ""); // removes all tab characters
                    cleanResponse.trim();
                    Log.d("Response  = ", cleanResponse);
                    // Update UI with response
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            //txtRES.setText(cleanResponse);
//                        }
//                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void receiveDataFromESP8266() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // Setup URL for receiving data from ESP8266
                    String dataUrl = "http://192.168.4.1/"; // Adjust the URL as needed

                    // Build the request
                    Request request = new Request.Builder().url(dataUrl).build();

                    try {
                        // Execute the request
                        Response response = client.newCall(request).execute();

                        // Process the response
                        String responseData = response.body().string();
                        //String myResponse = response.body().string();
                        final String cleanResponse = responseData.replaceAll("\\<.*?\\>", ""); // remove HTML tags
                        cleanResponse.replace("\n", ""); // remove all new line characters
                        cleanResponse.replace("\r", ""); // remove all carriage characters
                        cleanResponse.replace(" ", ""); // removes all space characters
                        cleanResponse.replace("\t", ""); // removes all tab characters
                        cleanResponse.trim();
                        Log.d("Response  = ", cleanResponse);
                        String[] parts = cleanResponse.split(" ");
                        String moistureValue = parts[0];
                        String waterLevelValue = parts[1];
                        soilMoisture = cleanMoistureValue(moistureValue);
                        waterLevel = cleanWaterLevelValue(waterLevelValue);

                        // Update UI with received data
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Display received data in TextView
                                moistProgress.setProgress(determineMoistureLevel(soilMoisture));
                                waterProgress.setProgress(determineWaterLevel(waterLevel));

                                if (determineWaterLevel(waterLevel)>40){
                                    txtWaterLevel.setText("There is enough water");
                                }else{
                                    txtWaterLevel.setText("Needs more water");
                                }
                                if (determineMoistureLevel(soilMoisture)>40){
                                    txtMoistureLevel.setText("Has Enough Water");
                                }else{
                                    txtMoistureLevel.setText("Does not have enough water");
                                }
                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Sleep for a while before making the next request
                    try {
                        Thread.sleep(2000); // Adjust the interval as needed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public int cleanMoistureValue(String sensorValueString) {
        // Convert the string to an integer
        int sensorValue = 0;

        // Check if all characters are integers
        if (!sensorValueString.matches("\\d+")) {
            return previousSoilMoisture;
        }

        // Convert the string to an integer
        sensorValue = Integer.parseInt(sensorValueString);

        // Check if the sensor value exceeds 1020
        if (sensorValue > 1020) {
            return previousSoilMoisture;
        }

        // Update previousSoilMoisture only when a valid sensor value is obtained
        previousSoilMoisture = sensorValue;

        return sensorValue;
    }
    public int cleanWaterLevelValue(String sensorValueString) {
        // Convert the string to an integer
        int sensorValue = 0;

        // Check if all characters are integers
        if (!sensorValueString.matches("\\d+")) {
            return previousWaterLevel;
        }

        // Take the appropriate number of characters based on the string length
        if (sensorValueString.length() >= 4) {
            sensorValueString = sensorValueString.substring(0, 3); // Take the first 3 characters
        }

        // Convert the string to an integer
        sensorValue = Integer.parseInt(sensorValueString);

        // Update previousWaterLevel only when a valid sensor value is obtained
        previousWaterLevel = sensorValue;

        return sensorValue;
    }


    public int determineWaterLevel(int waterLevelVal){
        int total = 0;

        if(waterLevelVal < 390) {
            total = 0; // Ensuring the total is not negative and is a whole number
        } else if (waterLevelVal >= 390 && waterLevelVal < 400) {
            total = 10;
        } else if (waterLevelVal >= 400 && waterLevelVal < 410) {
            total = 20;
        } else if (waterLevelVal >= 410 && waterLevelVal < 420) {
            total = 30;
        } else if (waterLevelVal >= 420 && waterLevelVal < 430) {
            total = 40;
        } else if (waterLevelVal >= 430 && waterLevelVal < 440) {
            total = 50;
        } else if (waterLevelVal >= 440 && waterLevelVal < 450) {
            total = 60;
        } else if (waterLevelVal >= 450 && waterLevelVal < 460) {
            total = 70;
        } else if (waterLevelVal >= 460 && waterLevelVal < 470) {
            total = 80;
        } else if (waterLevelVal >= 470 && waterLevelVal < 475) {
            total = 90;
        } else if (waterLevelVal >= 475) {
            total = 100; // Ensuring the highest total is 100
        }

        return total;
    }


    public int determineMoistureLevel(int moistureLevelVal){

        int total = 0;

        if(moistureLevelVal == 1020) total += 0;
        if(moistureLevelVal <= 958) total += 10;
        if(moistureLevelVal <= 896) total += 10;
        if(moistureLevelVal <= 834) total += 10;
        if(moistureLevelVal <= 772) total += 10;
        if(moistureLevelVal <= 710) total += 10;
        if(moistureLevelVal <= 648) total += 10;
        if(moistureLevelVal <= 586) total += 10;
        if(moistureLevelVal <= 524) total += 10;
        if(moistureLevelVal <= 462) total += 10;
        if(moistureLevelVal <= 400) total += 10;

        return total;
    }

}
