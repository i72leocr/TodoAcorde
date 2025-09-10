package com.todoacorde.todoacorde;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.WindowManager;

/**
 * Servicio en segundo plano que puede mantenerse activo (START_STICKY)
 * y mostrar un diálogo de confirmación para salir de la práctica.
 *
 * Consideraciones técnicas:
 * - {@link #onStartCommand(Intent, int, int)} devuelve START_STICKY para que el sistema
 *   intente recrear el servicio si es finalizado por falta de recursos.
 * - {@link #showExitConfirmationDialog()} crea un {@link AlertDialog} desde contexto
 *   de aplicación y lo presenta como superposición (overlay). Para que esto funcione:
 *     * Es necesario el permiso SYSTEM_ALERT_WINDOW (pantalla superpuesta).
 *     * En Android O+ se usa TYPE_APPLICATION_OVERLAY (ya aplicado).
 *     * Usar contexto de aplicación con AlertDialog puede requerir un tema adecuado
 *       (posibles problemas de estilo/tema si no está configurado).
 */
public class AppMonitorService extends Service {

    /**
     * Arranque del servicio. No realiza trabajo adicional aquí; simplemente
     * indica al sistema que el servicio debe permanecer activo (sticky).
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Este servicio no ofrece interfaz de enlace (bound service), por lo que
     * retorna null para indicar que no acepta conexiones de clientes.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Muestra un diálogo de confirmación para detener la práctica.
     *
     * Flujo:
     * - Botón “Sí”: se invoca {@link #stopSelf()} para finalizar el servicio.
     * - Botón “No”: se relanza {@link MainContainerActivity} en una nueva tarea.
     *
     * Detalles del diálogo:
     * - Se crea con el contexto de aplicación y se fuerza como overlay usando
     *   {@link WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY}.
     * - Requiere que la app tenga permiso de superposición y que el usuario lo haya concedido.
     */
    private void showExitConfirmationDialog() {
        // Construcción del diálogo de confirmación usando el contexto de aplicación.
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle("Confirmación");
        builder.setMessage("¿Deseas parar la práctica?");

        // Opción afirmativa: detiene el propio servicio.
        builder.setPositiveButton("Sí", (dialog, which) -> {
            stopSelf();
        });

        // Opción negativa: vuelve a la actividad principal de la app.
        builder.setNegativeButton("No", (dialog, which) -> {
            Intent intent = new Intent(getApplicationContext(), MainContainerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        // Creación y configuración para mostrar como overlay (superposición).
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }
}
