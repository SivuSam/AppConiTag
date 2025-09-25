package com.example.appconitag;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Optional: find views if you want to manipulate them
        // TextView welcomeText = findViewById(R.id.welcomeText);
        // ProgressBar progressBar = findViewById(R.id.progressBar);

        // Delay for 2 seconds then navigate to SignInActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, SignIn.class);
                startActivity(intent);
                finish(); // Close splash so user can't go back
            }
        }, 2000); // 2000ms delay
    }
}
