package com.vmware.chuanl.howoldofyourface;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;
import com.vmware.chuanl.howoldofyourface.Utils.FaceDetect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends ActionBarActivity implements View.OnClickListener{

    private static final int PICK_CODE = 0X11;
    private static final int CAMERA_CODE = 0X10;
    private ImageView mPhoto;
    private Button mGetImage;
    private Button mCamera;
    private Button mDetect;
    private TextView mTip;
    private View mWaiting;

    private Uri mPhotoUrl;
    private String mPhotoPath;

    private Bitmap mphotoImg;
    private String mPhotoStr;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initEvents();
    }

    private void initEvents() {
        mGetImage.setOnClickListener(this);
        mCamera.setOnClickListener(this);
        mDetect.setOnClickListener(this);
    }

    private void initViews() {
        mPaint = new Paint();
        mPhoto = (ImageView) findViewById(R.id.img_photo);
        mGetImage = (Button) findViewById(R.id.btn_pick);
        mCamera = (Button) findViewById(R.id.btn_camera);
        mDetect = (Button) findViewById(R.id.btn_how_old);
        mTip = (TextView) findViewById(R.id.tv_tip);
        mWaiting = findViewById(R.id.frameLayout_waiting);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(resultCode != RESULT_OK)
            return;
        if (requestCode == PICK_CODE) {
            if (intent != null) {
                Uri uri = intent.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();

                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mPhotoStr = cursor.getString(index);
                cursor.close();

                // compress
                resizePhoto();
                mPhoto.setImageBitmap(mphotoImg);
                mTip.setText("Click Detect ==>");
            }
        }
        if(requestCode == CAMERA_CODE){
            mPhotoStr = mPhotoPath;

            // compress
            resizePhoto();
            mPhoto.setImageBitmap(mphotoImg);
            mTip.setText("Click Detect ==>");
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mPhotoStr, options);

        double ratio = Math.max(options.outWidth * 1.0d / 1024, options.outHeight * 1.0d / 1024);
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        mphotoImg = BitmapFactory.decodeFile(mPhotoStr, options);
    }


    private static final int MSG_SUCCESS = 0X111;
    private static final int MSG_ERROR = 0X112;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mWaiting.setVisibility(View.GONE);
            switch (msg.what) {
                case MSG_SUCCESS:
                    JSONObject rs = (JSONObject) msg.obj;
                    prepareRsBitmap(rs);
                    mPhoto.setImageBitmap(mphotoImg);
                    break;
                case MSG_ERROR:
                    String errorMsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errorMsg)) {
                        mTip.setText("Error.");
                    } else {
                        mTip.setText(errorMsg);
                    }
                    break;
            }
        }
    };

    private void prepareRsBitmap(JSONObject result) {
        Bitmap bitmap = Bitmap.createBitmap(mphotoImg.getWidth(), mphotoImg.getHeight(), mphotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mphotoImg, 0, 0, null);

        try {
            JSONArray faces = result.getJSONArray("face");
            int faceCount = faces.length();
            mTip.setText("Find " + faceCount);

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
                Log.d("TAG", mPhoto.getWidth() + "");
                Log.d("TAG", ageBitmap.getWidth() + "");
                if(bitmap.getWidth() < mPhoto.getWidth() && bitmap.getHeight() < mPhoto.getHeight()){
                    float ratio = Math.max(
                            bitmap.getWidth() * 1.0f / mPhoto.getWidth(),
                            bitmap.getHeight() * 1.0f / mPhoto.getHeight()
                    );

                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int)(ageWidth * ratio), (int)(ageHeight * ratio), false);
                }

                canvas.drawBitmap(ageBitmap, x - ageBitmap.getWidth() / 2, y - h / 2 - ageBitmap.getHeight(), null);
                mphotoImg = bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap buildAgeBitmap(int age, boolean isMale){
        TextView tv = (TextView)mWaiting.findViewById(R.id.tv_age_and_gender);
        tv.setText(age + "");
        if(isMale){
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.icon_male), null, null, null);
        }else {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.icon_female), null, null, null);
        }

        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();

        return bitmap;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_how_old:
                mWaiting.setVisibility(View.VISIBLE);

                if(mPhotoStr == null || mPhotoStr.trim().equals("")){
                    mphotoImg = BitmapFactory.decodeResource(getResources(), R.mipmap.t4);
                }
                FaceDetect.decect(mphotoImg, new FaceDetect.CallBack() {
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
            case R.id.btn_pick:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_CODE);
                break;
            case R.id.btn_camera:
                startTakePhoto();
                break;
        }
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
            Toast.makeText(MainActivity.this, "Can not", Toast.LENGTH_SHORT).show();
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
