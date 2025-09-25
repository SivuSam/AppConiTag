package com.example.appconitag;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddAssetActivity extends AppCompatActivity {

    private EditText assetTagEditText, assetNameEditText, roomEditText,
            conditionEditText, locationEditText, notesEditText;
    private Button submitButton, captureImageButton, scanAssetTagButton;

    private static final String POST_ASSET_URL = "https://oracleapex.com/ords/holdingtechsa/admin/Assert/allassert";
    private static final int CAMERA_REQUEST = 1001;
    private static final int CAMERA_PERMISSION_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_asset);
        setTitle("Add Asset");

        // Initialize UI
        assetTagEditText = findViewById(R.id.assetTagEditText);
        assetNameEditText = findViewById(R.id.assetNameEditText);
        roomEditText = findViewById(R.id.roomEditText);
        conditionEditText = findViewById(R.id.conditionEditText);
        locationEditText = findViewById(R.id.locationEditText);
        notesEditText = findViewById(R.id.notesEditText);
        submitButton = findViewById(R.id.submitButton);
        captureImageButton = findViewById(R.id.captureImageButton);
        scanAssetTagButton = findViewById(R.id.scanAssetTagButton);

        // Scan button -> open ZXing scanner
        scanAssetTagButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(AddAssetActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan Asset Tag");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        // Capture image with permission check
        captureImageButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });


        // Submit asset
        submitButton.setOnClickListener(v -> {
            String assetTag = assetTagEditText.getText().toString().trim();
            String assetName = assetNameEditText.getText().toString().trim();
            String room = roomEditText.getText().toString().trim();
            String condition = conditionEditText.getText().toString().trim();
            String location = locationEditText.getText().toString().trim();
            String notes = notesEditText.getText().toString().trim();

            if (assetTag.isEmpty() || assetName.isEmpty() || room.isEmpty()
                    || condition.isEmpty() || location.isEmpty()) {
                Toast.makeText(AddAssetActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            new PostAssetTask().execute(assetTag, assetName, room, condition, location, notes, "N");
        });
    }

    // Open camera safely
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Handle camera + scan result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Camera photo
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            if (bitmap != null) {
                String assetName = assetNameEditText.getText().toString().trim();
                if (assetName.isEmpty()) assetName = "asset";
                saveImage(bitmap, assetName);
            }
        }

        // Barcode scanner result
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                assetTagEditText.setText(result.getContents());
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Save image to internal app storage
    private void saveImage(Bitmap bitmap, String assetName) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = assetName + "_" + timeStamp + ".jpg";
            File dir = new File(getFilesDir(), "assets");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Toast.makeText(this, "Image saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    // Post asset to API
    private class PostAssetTask extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            String assetTag = (String) params[0];
            String assetName = (String) params[1];
            String room = (String) params[2];
            String condition = (String) params[3];
            String location = (String) params[4];
            String notes = (String) params[5];
            String verified = (String) params[6];

            try {
                URL url = new URL(POST_ASSET_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("asset_tag", assetTag);
                jsonObject.put("asset_name", assetName);
                jsonObject.put("room", room);
                jsonObject.put("condition", condition);
                jsonObject.put("location", location);
                jsonObject.put("notes", notes);
                jsonObject.put("verified", verified);
                jsonObject.put("submitted_by", 1); // always 1

                OutputStream os = conn.getOutputStream();
                os.write(jsonObject.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(AddAssetActivity.this, "Asset posted successfully!", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() -> {
                    Intent intent = new Intent(AddAssetActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(AddAssetActivity.this, "Successfully added", Toast.LENGTH_LONG).show();
                    finish();
                }, 3000);
            } else {
                Toast.makeText(AddAssetActivity.this, "Failed to post asset.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

