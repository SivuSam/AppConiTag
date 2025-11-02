package com.example.appconitag;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddAssetActivity extends AppCompatActivity {

    private EditText assetTagEditText, assetNameEditText, roomEditText, conditionEditText, locationEditText, notesEditText;
    private Button submitButton, captureImageButton, scanAssetTagButton;

    private static final int CAMERA_PERMISSION_CODE = 200;
    private Bitmap currentBitmap = null;
    private String currentImagePath = "";

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    currentBitmap = (Bitmap) extras.get("data");
                    if (currentBitmap != null) {
                        String assetName = assetNameEditText.getText().toString().trim();
                        String assetTag = assetTagEditText.getText().toString().trim();
                        if (assetName.isEmpty()) assetName = "asset";
                        saveImage(currentBitmap, assetName, assetTag);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_asset);
        setTitle("Add Asset");

        initUI();
        setupListeners();
    }

    private void initUI() {
        assetTagEditText = findViewById(R.id.assetTagEditText);
        assetNameEditText = findViewById(R.id.assetNameEditText);
        roomEditText = findViewById(R.id.roomEditText);
        conditionEditText = findViewById(R.id.conditionEditText);
        locationEditText = findViewById(R.id.locationEditText);
        notesEditText = findViewById(R.id.notesEditText);
        submitButton = findViewById(R.id.submitButton);
        captureImageButton = findViewById(R.id.captureImageButton);
        scanAssetTagButton = findViewById(R.id.scanAssetTagButton);
    }

    private void setupListeners() {
        scanAssetTagButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(AddAssetActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan Asset Tag");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        captureImageButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });

        submitButton.setOnClickListener(v -> submitAsset());

        /*syncButton.setOnClickListener(v -> {
            DatabaseHelper db = new DatabaseHelper(AddAssetActivity.this);
            List<Asset> unsyncedAssets = db.getUnsyncedAssets(); // get all assets with sync status = 0
            db.close();

            if (unsyncedAssets.isEmpty()) {
                Toast.makeText(AddAssetActivity.this, "All assets are already synced!", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Asset asset : unsyncedAssets) {
                syncWithServer(asset); // reuse your existing sync logic
            }

            Toast.makeText(AddAssetActivity.this, "Syncing unsynced assets...", Toast.LENGTH_SHORT).show();
        });*/
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void saveImage(Bitmap bitmap, String assetName, String assetTag) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = assetName + "_" + (assetTag.isEmpty() ? "" : assetTag + "_") + timeStamp + ".jpg";
            File dir = new File(getFilesDir(), "assets");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            currentImagePath = file.getAbsolutePath();
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitAsset() {
        String tag = assetTagEditText.getText().toString().trim();
        String name = assetNameEditText.getText().toString().trim();
        String room = roomEditText.getText().toString().trim();
        String condition = conditionEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();

        if (tag.isEmpty() || name.isEmpty() || room.isEmpty() || condition.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Asset asset = new Asset(tag, name, room, condition, location, notes, "N", currentImagePath, 1);

        DatabaseHelper db = new DatabaseHelper(this);
        long id = db.addAsset(asset);
        db.close();

        if (id != -1) {
            Toast.makeText(this, "Asset saved locally", Toast.LENGTH_SHORT).show();
            syncWithServer(asset);
        } else {
            Toast.makeText(this, "Failed to save asset locally", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncWithServer(Asset asset) {
        apiService api = apiClient.getApiService(); // Get Retrofit service instance

        File imageFile = new File(asset.getImagePath());

        RequestBody assetTag = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getAssetTag());
        RequestBody assetName = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getAssetName());
        RequestBody room = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getRoom());
        RequestBody condition = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getCondition());
        RequestBody location = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getLocation());
        RequestBody notes = RequestBody.create(okhttp3.MediaType.parse("text/plain"), asset.getNotes());

        MultipartBody.Part picturePart = null;
        if (imageFile.exists()) {
            RequestBody imageBody = RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), imageFile);
            picturePart = MultipartBody.Part.createFormData("Picture", imageFile.getName(), imageBody);
        }

        // Call your API
        Call<ResponseBody> call = api.uploadAsset(assetTag, assetName, room, condition, location, notes, picturePart);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    DatabaseHelper db = new DatabaseHelper(AddAssetActivity.this);
                    db.updateSyncStatus(asset.getId(), 1);
                    db.close();
                    Toast.makeText(AddAssetActivity.this, "Asset synced successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    scheduleRetry(asset);
                    Toast.makeText(AddAssetActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                scheduleRetry(asset);
                Toast.makeText(AddAssetActivity.this, "Sync failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    private void scheduleRetry(Asset asset) {
        Data data = new Data.Builder()
                .putLong("assetId", asset.getId())
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(this).enqueue(syncRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            assetTagEditText.setText(result.getContents());
        }
    }
}
