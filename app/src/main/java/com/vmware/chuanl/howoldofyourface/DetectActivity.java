package com.vmware.chuanl.howoldofyourface;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;
import com.vmware.chuanl.howoldofyourface.Utils.FaceDetect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class DetectActivity extends ActionBarActivity implements View.OnClickListener {

    private static final int MSG_SUCCESS = 0;
    private static final int MSG_ERROR = 1;
    private ImageView mDetectImage;
    private String mPhotoStr;
    private Bitmap mPhotoImgBitmap;
    private Button mDetectButton;
    private FrameLayout mLoading;
    private Paint mPaint;
    private boolean isUseNativePic = false;
    private int mNativeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);

        if (getIntent() != null) {
            Intent intent = getIntent();
            mPhotoStr = intent.getStringExtra("photo");
            mNativeIndex = intent.getIntExtra("NativePicture", StartActivity.mImgIds.length);
            isUseNativePic = !(mNativeIndex == StartActivity.mImgIds.length);
        }

        mPaint = new Paint();
        mDetectButton = (Button) findViewById(R.id.btn_detect_face);
        mDetectButton.setOnClickListener(this);
        mLoading = (FrameLayout) findViewById(R.id.fragment_loading);
        mDetectImage = (ImageView) findViewById(R.id.img_detect_photo);
        resizePhoto();
        mDetectImage.setImageBitmap(mPhotoImgBitmap);
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        if (isUseNativePic)
            BitmapFactory.decodeResource(getResources(), StartActivity.mImgIds[mNativeIndex], options);
        else
            BitmapFactory.decodeFile(mPhotoStr, options);

        double ratio = Math.max(options.outWidth * 1.0d / 1024, options.outHeight * 1.0d / 1024);
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        if (isUseNativePic)
            mPhotoImgBitmap = BitmapFactory.decodeResource(getResources(), StartActivity.mImgIds[mNativeIndex], options);
        else
            mPhotoImgBitmap = BitmapFactory.decodeFile(mPhotoStr, options);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_detect_face:
                mLoading.setVisibility(View.VISIBLE);
                FaceDetect.decect(mPhotoImgBitmap, new FaceDetect.CallBack() {
                    @Override
                    public void success(JSONObject json) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCCESS;
                        msg.obj = json;
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = exception.toString();
                        mHandler.sendMessage(msg);
                    }
                });
                break;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mLoading.setVisibility(View.GONE);
            switch (msg.what) {
                case MSG_SUCCESS:
                    JSONObject rs = (JSONObject) msg.obj;
                    prepareRsBitmap(rs);
                    mDetectImage.setImageBitmap(mPhotoImgBitmap);
                    break;
                case MSG_ERROR:
                    String errorMsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errorMsg)) {
//                        mTip.setText("Error.");
                    } else {
//                        mTip.setText(errorMsg);
                    }
                    break;
            }
        }
    };

    private void prepareRsBitmap(JSONObject result) {
        Bitmap bitmap = Bitmap.createBitmap(mPhotoImgBitmap.getWidth(), mPhotoImgBitmap.getHeight(), mPhotoImgBitmap.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImgBitmap, 0, 0, null);

        try {
            JSONArray faces = result.getJSONArray("face");
            int faceCount = faces.length();
//            mTip.setText("Find " + faceCount);

            for (int i = 0; i < faceCount; ++i) {
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");

                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");

                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");

                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();

                w = w / 100 * bitmap.getWidth();
                h = h / 100 * bitmap.getHeight();


                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);

                canvas.drawLine(x - w / 2, y - h / 2, x - w / 2, y + h / 2, mPaint);
                canvas.drawLine(x - w / 2, y - h / 2, x + w / 2, y - h / 2, mPaint);
                canvas.drawLine(x + w / 2, y - h / 2, x + w / 2, y + h / 2, mPaint);
                canvas.drawLine(x - w / 2, y + h / 2, x + w / 2, y + h / 2, mPaint);

                //get age and gender
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));

                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();

                Log.d("TAG", bitmap.getWidth() + "");
                Log.d("TAG", mDetectImage.getWidth() + "");
                Log.d("TAG", ageBitmap.getWidth() + "");
                if (bitmap.getWidth() < mDetectImage.getWidth() && bitmap.getHeight() < mDetectImage.getHeight()) {
                    float ratio = Math.max(
                            bitmap.getWidth() * 1.0f / mDetectImage.getWidth(),
                            bitmap.getHeight() * 1.0f / mDetectImage.getHeight()
                    );

                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio), (int) (ageHeight * ratio), false);
                }

                canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2, y - h / 2 - ageBitmap.getHeight(), null);
                mPhotoImgBitmap = bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView) mLoading.findViewById(R.id.tv_age_and_gender);
        tv.setText(age + "");
        if (isMale) {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.icon_male), null, null, null);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.icon_female), null, null, null);
        }

        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();

        return bitmap;
    }
}
