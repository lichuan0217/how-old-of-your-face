package com.vmware.chuanl.howoldofyourface;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class StartActivity extends ActionBarActivity implements View.OnClickListener {

    private static final int PICK_CODE = 0;
    private static final int CAMERA_CODE = 1;
    private Button mCamera;
    private Button mGallary;

    private String mPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        init();
    }

    private void init() {
        mCamera = (Button)findViewById(R.id.btn_from_camera);
        mGallary = (Button)findViewById(R.id.btn_from_gallary);

        mCamera.setOnClickListener(this);
        mGallary.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_from_camera:
                startTakePhoto();
                break;
            case R.id.btn_from_gallary:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(resultCode != RESULT_OK){
            return;
        }

        if(requestCode == PICK_CODE){
            if (intent != null) {
                Uri uri = intent.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();

                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                String mPhotoStr = cursor.getString(index);
                cursor.close();
                Intent i = new Intent(StartActivity.this, DetectActivity.class);
                i.putExtra("photo", mPhotoStr);
                startActivity(i);
            }
        }

        if(requestCode == CAMERA_CODE){
            Intent i = new Intent(StartActivity.this, DetectActivity.class);
            i.putExtra("photo", mPhotoPath);
            startActivity(i);
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void startTakePhoto() {
        Intent intent;

        String savePath = "";
        String storageState = Environment.getExternalStorageState();
        if(storageState.equals(Environment.MEDIA_MOUNTED)){
            savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HowOld/Camera/";
            File saveDir = new File(savePath);
            if(!saveDir.exists()){
                saveDir.mkdirs();
            }
        }

        if(TextUtils.isEmpty(savePath)){
            Toast.makeText(StartActivity.this, "Can not", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = "howOld_" + timeStamp + ".jpg";
        File out = new File(savePath, fileName);
        Uri uri = Uri.fromFile(out);
        mPhotoPath = savePath + fileName;
        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, CAMERA_CODE);
    }
}
