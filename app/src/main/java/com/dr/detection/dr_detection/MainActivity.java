package com.dr.detection.dr_detection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.dr.detection.network.ApiInterface;
import com.dr.detection.network.RetrofitClient;
import com.dr.detection.utils.Utils;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.internal.Util;
import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText ipEditText;
    private TextView chooseImage;
    private final int GALLERY_REQUEST_CODE = 1, REQUEST_ID_MULTIPLE_PERMISSIONS = 101;
    private final String TAG = MainActivity.class.getSimpleName();
    private View loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loader = findViewById(R.id.loader);
        chooseImage = findViewById(R.id.chooseImageTextView);
        ipEditText = findViewById(R.id.ipEditText);
        chooseImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view == chooseImage){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkForPermissions()) {
                    showPictureDialog();
                }
            } else {
                showPictureDialog();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkForPermissions() {
        int permissionCamera = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int writePermission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermissions = 0;
        readPermissions = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (writePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (readPermissions != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);

            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                            perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                        showPictureDialog();

                    } else {
                        showPermissionDeniedDialog();
                    }
                }
                break;
            }
        }
    }

    public void showPictureDialog() {
        choosePhotoFromGallery();
    }

    public void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY_REQUEST_CODE) {
            if (data != null) {
                showLoaderScreen();
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                Bitmap capturedBitmap = BitmapFactory.decodeFile(picturePath);
                if (capturedBitmap != null) {
                    covertBitmapIntoBase64(capturedBitmap);
                }
            }
        }
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setCancelable(false);
        builder.setMessage(R.string.app_not_work)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
    }

    private void covertBitmapIntoBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP);

        if (base64String != null && !base64String.isEmpty()) {
            sendVerificationRequest(base64String);
        }
    }

    private void sendVerificationRequest(String base64String) {
        String ip = ipEditText.getText().toString();
        if (ip.isEmpty()){
            Utils.displayMessage(this,"Please provide IP address, for example (10.1.1.112)");
            return;
        }

        if (base64String != null && !base64String.isEmpty()) {
            sendRequestToServer(ip, base64String);
        }
    }

    private void sendRequestToServer(String ip,String base64String) {
        RetrofitClient client = new RetrofitClient(ip);
        final ApiInterface service = client.getInstance().create(ApiInterface.class);
        Call<JsonObject> call = service.sendClassificationRequest(base64String);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull retrofit2.Response<JsonObject> response) {
                try {
                    if (response.isSuccessful()) {
                        JsonObject responseObject = response.body();
                        if (responseObject != null) {
                            hideLoaderScreen();
                            try {
                                String message = responseObject.get("response").getAsString();
                                Utils.displayMessage(MainActivity.this, message);
                            }catch (Exception e){
                                Log.e(TAG,e.getLocalizedMessage());
                            }
                        }
                    }else {
                        hideLoaderScreen();
                        Utils.displayMessage(MainActivity.this,"Server is not responding. Please try again.");
                    }
                } catch (Exception ex) {
                    hideLoaderScreen();
                    Utils.displayMessage(MainActivity.this,ex.getLocalizedMessage());
                    ex.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                hideLoaderScreen();
                Utils.displayMessage(MainActivity.this,t.getLocalizedMessage());
                Log.e(TAG, t.getMessage());
            }
        });
    }

    private void showLoaderScreen() {
        loader.setVisibility(View.VISIBLE);
    }

    private void hideLoaderScreen() {
        loader.setVisibility(View.GONE);
    }
}
