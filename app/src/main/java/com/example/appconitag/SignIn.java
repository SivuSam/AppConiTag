package com.example.appconitag;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignIn extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        setTitle("signIn");

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        // Handle login button click
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String email = emailEditText.getText().toString().trim();
                final String password = passwordEditText.getText().toString().trim();

                if (!email.isEmpty() && !password.isEmpty()) {
                    // Call AsyncTask to perform login via API
                    new LoginTask().execute(email, password);
                } else {
                    Toast.makeText(SignIn.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Handle register button click
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SignIn.this, "Register clicked", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SignIn.this, SignUp.class);
                startActivity(intent);
            }
        });
    }

    // AsyncTask to perform network login
    private class LoginTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String email = params[0];
            String password = params[1];
            AuthService authService = new AuthService();
            return authService.authenticate(email, password);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {

                Toast.makeText(SignIn.this, "Login Successful", Toast.LENGTH_SHORT).show();
                // Navigate to MainActivity
                Intent intent = new Intent(SignIn.this, MainActivity.class);
                startActivity(intent);
                finish();
                // startActivity(intent);
            } else {
                Toast.makeText(SignIn.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
