package com.dr.detection.network;

import android.support.annotation.NonNull;
import android.util.Log;

import com.dr.detection.DrDetectionApplication;
import com.dr.detection.utils.Utils;
import java.io.IOException;
import java.net.SocketTimeoutException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LoggingInterceptor implements Interceptor {
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {

        if (!Utils.isOnline(DrDetectionApplication.getInstance().getApplicationContext())) {
            throw new NoConnectivityException(DrDetectionApplication.getContext());

        } else {
            try {
                Request request = chain.request();
                Response response = chain.proceed(request);
                String rawJson = response.body().string();
                Log.d("Logging Screen", String.format("Response: %s", rawJson));
                return response.newBuilder()
                        .body(ResponseBody.create(response.body().contentType(), rawJson))
                        .build();
            } catch (SocketTimeoutException ex) {
                ex.printStackTrace();
                throw new NoConnectivityException(DrDetectionApplication.getContext());
            }
        }

    }
}
