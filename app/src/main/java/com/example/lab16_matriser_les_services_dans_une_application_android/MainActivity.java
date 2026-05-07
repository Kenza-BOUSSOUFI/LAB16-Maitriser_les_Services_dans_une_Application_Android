package com.example.lab16_matriser_les_services_dans_une_application_android;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int NOTIF_PERMISSION_CODE = 101;

    private TextView displayTimeTv;
    private Button startBtn, stopBtn;
    private TimerForegroundService timerService;
    private boolean isServiceConnected = false;

    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to service");
            TimerForegroundService.TimerBinder binder = (TimerForegroundService.TimerBinder) service;
            timerService = binder.getService();
            isServiceConnected = true;

            timerService.setUpdateListener(new TimerForegroundService.TimerUpdateListener() {
                @Override
                public void onTimeUpdated(String formattedTime) {
                    runOnUiThread(() -> displayTimeTv.setText(formattedTime));
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceConnected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTimeTv = findViewById(R.id.display_time_tv);
        startBtn = findViewById(R.id.start_timer_btn);
        stopBtn = findViewById(R.id.stop_timer_btn);

        checkNotificationPermission();

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeStartService();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeStopService();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Tentative de reconnexion automatique si le service tourne déjà
        Intent intent = new Intent(this, TimerForegroundService.class);
        bindService(intent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIF_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission accordée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void executeStartService() {
        Intent intent = new Intent(this, TimerForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    private void executeStopService() {
        Intent intent = new Intent(this, TimerForegroundService.class);
        intent.setAction("STOP_TIMER_ACTION");
        
        if (isServiceConnected) {
            timerService.setUpdateListener(null);
            unbindService(serviceConn);
            isServiceConnected = false;
        }
        
        startService(intent); 
        stopService(intent);
        displayTimeTv.setText("00:00");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceConnected) {
            unbindService(serviceConn);
            isServiceConnected = false;
        }
    }
}
