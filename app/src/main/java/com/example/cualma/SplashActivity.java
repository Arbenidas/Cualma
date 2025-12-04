package com.example.cualma;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Configuración de tiempo: 3000 milisegundos = 3 segundos
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Iniciar la actividad principal
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // Cerrar el Splash para que el usuario no pueda volver atrás con el botón 'Atrás'
            finish();
        }, 3000);
    }
}