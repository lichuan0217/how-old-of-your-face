package com.vmware.chuanl.howoldofyourface.Utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by chuanl on 5/22/15.
 */
public class FaceDetect {

    public interface CallBack {
        void success(JSONObject json);

        void error(FaceppParseException exception);
    }

    public static void decect(final Bitmap bm, final CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    //request
                    HttpRequests requests = new HttpRequests(Constant.KEY, Constant.SECRET, true, true);

                    Bitmap bmSmall = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    byte[] array = stream.toByteArray();

                    PostParameters params = new PostParameters();
                    params.setImg(array);
                    JSONObject jsonObject = requests.detectionDetect(params);

                    Log.d("TAG", jsonObject.toString());

                    if (callBack != null) {
                        callBack.success(jsonObject);
                    }
                } catch (FaceppParseException e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.error(e);
                    }
                }
            }
        }).start();
    }
}
