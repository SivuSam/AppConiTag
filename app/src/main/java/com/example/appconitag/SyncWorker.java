package com.example.appconitag;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String assetTag = getInputData().getString("AssetTag");
            String assetName = getInputData().getString("AssetName");
            String room = getInputData().getString("Room");
            String condition = getInputData().getString("Condition");
            String location = getInputData().getString("Location");
            String notes = getInputData().getString("Notes");
            String imagePath = getInputData().getString("Picture");

            apiService service = apiClient.getApiService();

            RequestBody assetTagBody = RequestBody.create(MediaType.parse("text/plain"), assetTag);
            RequestBody assetNameBody = RequestBody.create(MediaType.parse("text/plain"), assetName);
            RequestBody roomBody = RequestBody.create(MediaType.parse("text/plain"), room);
            RequestBody conditionBody = RequestBody.create(MediaType.parse("text/plain"), condition);
            RequestBody locationBody = RequestBody.create(MediaType.parse("text/plain"), location);
            RequestBody notesBody = RequestBody.create(MediaType.parse("text/plain"), notes);

            MultipartBody.Part picturePart = null;
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    RequestBody imageBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
                    picturePart = MultipartBody.Part.createFormData("Picture", imageFile.getName(), imageBody);
                }
            }

            Call<ResponseBody> call = service.uploadAsset(assetTagBody, assetNameBody, roomBody, conditionBody, locationBody, notesBody, picturePart);
            Response<ResponseBody> response = call.execute();

            if (response.isSuccessful()) {
                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
