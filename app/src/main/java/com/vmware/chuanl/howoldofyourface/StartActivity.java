package com.vmware.chuanl.howoldofyourface;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class StartActivity extends ActionBarActivity implements View.OnClickListener, ViewSwitcher.ViewFactory, View.OnTouchListener {

    public static final int[] mImgIds = new int[]{R.mipmap.ab0, R.mipmap.ab1, R.mipmap.ab2, R.mipmap.t4};
    private static final int PICK_CODE = 0;
    private static final int CAMERA_CODE = 1;
    private Button mCamera;
    private Button mGallary;
    private Button mUseDemo;

    private String mPhotoPath;

    private ImageSwitcher mImageSwither;
//    private int[] mImgIds;
    private ImageView[] mIndicatorViews;
    private int mCurrentPosition = 0;
    private float mDownX;
    private LinearLayout mIndicatorContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        init();
    }

    private void init() {
        mCamera = (Button) findViewById(R.id.btn_from_camera);
        mGallary = (Button) findViewById(R.id.btn_from_gallary);
        mUseDemo = (Button)findViewById(R.id.btn_use_demo);
        mImageSwither = (ImageSwitcher) findViewById(R.id.image_switcher_demo_pic);
        mIndicatorContainer = (LinearLayout) findViewById(R.id.linearLayout_indicator);

        mCamera.setOnClickListener(this);
        mGallary.setOnClickListener(this);
        mUseDemo.setOnClickListener(this);
        mImageSwither.setFactory(this);
        mImageSwither.setOnTouchListener(this);

        initIndicator();
        initImageSwitcher();
    }

    private void initIndicator() {

        mIndicatorViews = new ImageView[mImgIds.length];
        for (int i = 0; i < mIndicatorViews.length; ++i) {
            ImageView imageView = new ImageView(this);
            mIndicatorViews[i] = imageView;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            params.rightMargin = 3;
            params.leftMargin = 3;

            imageView.setBackgroundResource(R.mipmap.page_indicator_unfocused);
            mIndicatorContainer.addView(imageView, params);
        }
    }


    private void initImageSwitcher() {
        mImageSwither.setImageResource(mImgIds[mCurrentPosition]);
        setIndicatorBackground(mCurrentPosition);
    }

    private void setIndicatorBackground(int selectItem) {
        for(int i = 0; i < mIndicatorViews.length; ++i){
            if(i == selectItem)
                mIndicatorViews[i].setBackgroundResource(R.mipmap.page_indicator_focused);
            else
                mIndicatorViews[i].setBackgroundResource(R.mipmap.page_indicator_unfocused);
        }
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_from_camera:
                startTakePhoto();
                break;
            case R.id.btn_from_gallary:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_CODE);
                break;
            case R.id.btn_use_demo:
                Intent i = new Intent(StartActivity.this, DetectActivity.class);
                i.putExtra("NativePicture", mCurrentPosition);
                startActivity(i);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == PICK_CODE) {
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

        if (requestCode == CAMERA_CODE) {
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
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HowOld/Camera/";
            File saveDir = new File(savePath);
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }
        }

        if (TextUtils.isEmpty(savePath)) {
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

    @Override
    public View makeView() {
        final ImageView imageView = new ImageView(this);
        imageView.setBackgroundColor(0xFF000000);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return imageView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                float lastX = event.getX();
                if(lastX > mDownX){
                    if(mCurrentPosition > 0){
                        mImageSwither.setInAnimation(AnimationUtils.loadAnimation(getApplication(), R.anim.left_in));
                        mImageSwither.setOutAnimation(AnimationUtils.loadAnimation(getApplication(), R.anim.right_out));
                        mCurrentPosition--;
                        mImageSwither.setImageResource(mImgIds[mCurrentPosition]);
                        setIndicatorBackground(mCurrentPosition);
                    }
                }

                if(lastX < mDownX){
                    if(mCurrentPosition < mImgIds.length - 1){
                        mImageSwither.setInAnimation(AnimationUtils.loadAnimation(getApplication(), R.anim.right_in));
                        mImageSwither.setOutAnimation(AnimationUtils.loadAnimation(getApplication(), R.anim.left_out));
                        mCurrentPosition++;
                        mImageSwither.setImageResource(mImgIds[mCurrentPosition]);
                        setIndicatorBackground(mCurrentPosition);
                    }
                }
                break;
        }
        return true;
    }
}
