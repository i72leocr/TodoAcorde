package com.tuguitar.todoacorde;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;

public class AppMonitorService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showExitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle("Confirmación");
        builder.setMessage("¿Deseas parar la práctica?");
        builder.setPositiveButton("Sí", (dialog, which) -> {
            stopSelf();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            Intent intent = new Intent(getApplicationContext(), MainContainerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }
}