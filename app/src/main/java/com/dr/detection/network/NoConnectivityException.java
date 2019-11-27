package com.dr.detection.network;

import android.content.Context;
import android.widget.Toast;
import com.dr.detection.dr_detection.R;

import java.io.IOException;

public class NoConnectivityException extends IOException {
    private Context context;

    public NoConnectivityException(Context context) {
        this.context = context;
    }

    @Override
    public String getMessage() {
        Toast.makeText(context, R.string.connection_error, Toast.LENGTH_LONG).show();
        return context.getString(R.string.connection_error);
    }
}
