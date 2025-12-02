package com.example.cualma;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    BaseDatosHelper miBD;

    // Vistas nuevas
    RecyclerView recyclerBento;
    TextView tvVacio;
    FrameLayout overlayModal;
    CardView cardSeleccion, cardFormAlumno, cardFormMateria;
    FloatingActionButton fabRegistrar;

    // Campos Alumno
    EditText edtAluCod, edtAluNom, edtAluApe;

    // Campos Materia
    EditText edtMatCod, edtMatNom, edtMatHor, edtMatAula, edtMatDoc;

    // Variables temporales
    String materiaCodigoFotoActual = "";
    ActivityResultLauncher<Intent> cameraLauncher;
    MateriaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Asegúrate que el XML se llame así

        miBD = new BaseDatosHelper(this);
        inicializarVistas();
        inicializarCameraLauncher();
        cargarGaleriaBento();

        // Listeners
        fabRegistrar.setOnClickListener(v -> mostrarModal(cardSeleccion));
        findViewById(R.id.btnOpcionAlumno).setOnClickListener(v -> mostrarModal(cardFormAlumno));
        findViewById(R.id.btnOpcionMateria).setOnClickListener(v -> mostrarModal(cardFormMateria));
        findViewById(R.id.btnCerrarModal).setOnClickListener(v -> cerrarTodoModal());

        // Guardar Alumno Simple
        findViewById(R.id.btnGuardarAluFinal).setOnClickListener(v -> {
            if(miBD.insertarAlumno(edtAluCod.getText().toString(), edtAluNom.getText().toString(), edtAluApe.getText().toString())) {
                Toast.makeText(this, "Alumno Registrado", Toast.LENGTH_SHORT).show();
                cerrarTodoModal();
            } else {
                Toast.makeText(this, "Error: Código duplicado", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnCancelarAlu).setOnClickListener(v -> cerrarTodoModal());

        // Guardar Materia
        findViewById(R.id.btnGuardarMatFinal).setOnClickListener(v -> {
            if(miBD.insertarMateria(
                    edtMatCod.getText().toString(),
                    edtMatNom.getText().toString(),
                    edtMatHor.getText().toString(),
                    edtMatAula.getText().toString(),
                    null,
                    edtMatDoc.getText().toString())) {
                Toast.makeText(this, "Materia Creada", Toast.LENGTH_SHORT).show();
                cargarGaleriaBento();
                cerrarTodoModal();
            } else {
                Toast.makeText(this, "Error al crear materia", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btnCancelarMat).setOnClickListener(v -> cerrarTodoModal());
    }


    private void inicializarVistas() {
        edtMatCod = findViewById(R.id.edtMatCod);
        edtMatNom = findViewById(R.id.edtMatNom);

        // --- AQUÍ PEGAS ESTA PARTE ---
        edtMatHor = findViewById(R.id.edtMatHor);
        edtMatHor.setFocusable(false); // Evita que salga el teclado al tocarlo
        edtMatHor.setClickable(true);  // Habilita el clic
        edtMatHor.setOnClickListener(v -> mostrarReloj()); // Llama al método del reloj
        // -----------------------------

        edtMatAula = findViewById(R.id.edtMatAula);
        edtMatDoc = findViewById(R.id.edtMatDoc);
        // En el XML activity_main, cambia el ListView por RecyclerView con id: recyclerBento
        recyclerBento = findViewById(R.id.listaGaleria); // Asumo que cambiaste el tipo en XML
        tvVacio = findViewById(R.id.tvVacio);
        overlayModal = findViewById(R.id.overlayModal);
        cardSeleccion = findViewById(R.id.cardSeleccion);
        cardFormAlumno = findViewById(R.id.cardFormAlumno);
        cardFormMateria = findViewById(R.id.cardFormMateria);
        fabRegistrar = findViewById(R.id.fabRegistrar);

        edtAluCod = findViewById(R.id.edtAluCod);
        edtAluNom = findViewById(R.id.edtAluNom);
        edtAluApe = findViewById(R.id.edtAluApe);

        edtMatCod = findViewById(R.id.edtMatCod);
        edtMatNom = findViewById(R.id.edtMatNom);
        edtMatHor = findViewById(R.id.edtMatHor);
        edtMatAula = findViewById(R.id.edtMatAula);
        edtMatDoc = findViewById(R.id.edtMatDoc);

        // Configurar RecyclerView tipo Bento (2 columnas)
        recyclerBento.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

    }

    // Variable global en MainActivity (arriba, junto a los EditText)
    // ...

    private void cargarGaleriaBento() {
        List<Map<String, String>> datos = miBD.obtenerTodasMaterias();

        if (datos.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
            recyclerBento.setVisibility(View.GONE);
        } else {
            tvVacio.setVisibility(View.GONE);
            recyclerBento.setVisibility(View.VISIBLE);

            // Si el adaptador ya existe, solo actualizamos los datos
            if (adapter != null) {
                adapter.actualizarDatos(datos);
            } else {
                // Si es la primera vez, lo creamos
                adapter = new MateriaAdapter(datos, new MateriaAdapter.OnItemClickListener() {
                    @Override
                    public void onAddAlumnoClick(String codigoMateria) {
                        mostrarDialogoAsignarAlumno(codigoMateria);
                    }

                    @Override
                    public void onOptionsClick(Map<String, String> item) {
                        mostrarOpcionesMateria(item);
                    }
                });
                recyclerBento.setAdapter(adapter);
            }
        }
    }

    // --- NUEVO DIALOGO CON SPINNER (SELECT) ---
    private void mostrarDialogoAsignarAlumno(String codigoMateria) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Alumno a la Clase");

        View view = getLayoutInflater().inflate(R.layout.dialog_asignar_alumno, null);
        Spinner spinner = view.findViewById(R.id.spinnerAlumnos);

        List<String> listaAlumnos = miBD.obtenerListaAlumnosSimple();
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listaAlumnos);
        spinner.setAdapter(adapterSpinner);

        builder.setView(view);
        builder.setPositiveButton("Asignar", (dialog, which) -> {
            if (spinner.getSelectedItem() != null) {
                String seleccion = spinner.getSelectedItem().toString();
                // Extraer el código que está entre parentesis: "Juan (SM100)" -> "SM100"
                String codigoAlumno = seleccion.substring(seleccion.lastIndexOf("(") + 1, seleccion.lastIndexOf(")"));

                // ... dentro del setPositiveButton ...
                boolean exito = miBD.asignarAlumnoPorCodigoMateria(codigoAlumno, codigoMateria);
                if(exito) {
                    Toast.makeText(this, "Alumno asignado correctamente", Toast.LENGTH_SHORT).show();
                    cargarGaleriaBento();
                } else {
                    Toast.makeText(this, "El alumno ya está en esta clase o la clase no existe", Toast.LENGTH_LONG).show();
                }
// ...
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // --- LÓGICA DE FOTOS Y MAPAS (REUTILIZADA) ---
    private void mostrarOpcionesMateria(Map<String, String> item) {
        String codigo = item.get("codigo");
        String aula = item.get("aula");
        String maps = item.get("maps_ubicacion");

        String[] opciones = {"📍 Ver Mapa", "🖼️ Ver Foto Aula", "✏️ Editar Info", "📸 Cambiar Foto"};
        new AlertDialog.Builder(this)
                .setTitle(item.get("titulo"))
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) abrirGoogleMaps(maps != null ? maps : aula);
                    if (which == 1) verFotoGuardada(codigo);
                    if (which == 2) mostrarDialogoEdicion(codigo, aula, maps);
                    if (which == 3) {
                        materiaCodigoFotoActual = codigo;
                        abrirCamara();
                    }
                }).show();
    }

    private void mostrarModal(CardView cardAMostrar) {
        overlayModal.setVisibility(View.VISIBLE);
        cardSeleccion.setVisibility(View.GONE);
        cardFormAlumno.setVisibility(View.GONE);
        cardFormMateria.setVisibility(View.GONE);
        cardAMostrar.setVisibility(View.VISIBLE);
    }

    private void cerrarTodoModal() {
        overlayModal.setVisibility(View.GONE);
        edtAluCod.setText(""); edtAluNom.setText(""); edtAluApe.setText("");
        edtMatCod.setText(""); edtMatNom.setText(""); edtMatHor.setText(""); edtMatAula.setText(""); edtMatDoc.setText("");
    }

    // ... (Métodos de Cámara, Bitmap y Maps se mantienen igual que tu código original) ...

    private void inicializarCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        String base64Image = bitmapToBase64(imageBitmap);
                        miBD.guardarFotoMateria(materiaCodigoFotoActual, base64Image);
                        Toast.makeText(this, "Foto guardada", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try { cameraLauncher.launch(takePictureIntent); } catch (Exception e) {}
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String b64) {
        byte[] imageBytes = Base64.decode(b64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private void verFotoGuardada(String codigoMateria) {
        String base64 = miBD.obtenerFotoMateria(codigoMateria);
        if (base64 != null && !base64.isEmpty()) {
            Bitmap bitmap = base64ToBitmap(base64);
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            new AlertDialog.Builder(this).setView(imageView).show();
        } else {
            Toast.makeText(this, "No hay foto", Toast.LENGTH_SHORT).show();
        }
    }

    // Reemplaza el método mostrarDialogoEdicion por este:
    // --- EN MainActivity.java ---

    private void mostrarDialogoEdicion(String codigoMateria, String aulaActual, String mapsActual) {
        // 1. Crear el Builder y la View
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        // IMPORTANTE: Aquí definimos 'view' usando el layout correcto
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_editar_bento, null);

        builder.setView(view);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 2. Referencias (Ahora 'view' ya existe y no dará error)
        android.widget.EditText inputAula = view.findViewById(R.id.edtEditAula);
        android.widget.EditText inputMaps = view.findViewById(R.id.edtEditMaps);
        android.widget.EditText inputHorario = view.findViewById(R.id.edtEditHorario);
        android.widget.EditText inputDocente = view.findViewById(R.id.edtEditDocente);
        android.widget.Button btnFoto = view.findViewById(R.id.btnEditFoto);
        android.widget.Button btnGuardar = view.findViewById(R.id.btnEditGuardar);
        android.widget.Button btnCancelar = view.findViewById(R.id.btnEditCancelar);

        // 3. Pre-llenar datos
        Map<String, String> detalles = miBD.obtenerDetalleMateria(codigoMateria);
        if (!detalles.isEmpty()) {
            inputAula.setText(detalles.get("aula"));
            inputMaps.setText(detalles.get("maps"));
            inputHorario.setText(detalles.get("horario"));
            inputDocente.setText(detalles.get("docente"));
        } else {
            inputAula.setText(aulaActual);
            inputMaps.setText(mapsActual);
        }

        // 4. Lógica TimePicker
        inputHorario.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            new android.app.TimePickerDialog(this, (tpView, hourOfDay, minute) -> {
                String amPm = (hourOfDay < 12) ? "AM" : "PM";
                int horaMostrar = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
                if (horaMostrar == 0) horaMostrar = 12;
                inputHorario.setText(String.format("%02d:%02d %s", horaMostrar, minute, amPm));
            }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), false).show();
        });

        // 5. Botones
        btnFoto.setOnClickListener(v -> {
            materiaCodigoFotoActual = codigoMateria;
            abrirCamara();
        });

        btnGuardar.setOnClickListener(v -> {
            boolean exito = miBD.actualizarMateriaCompleta(
                    codigoMateria,
                    inputAula.getText().toString(),
                    inputMaps.getText().toString(),
                    inputHorario.getText().toString(),
                    inputDocente.getText().toString()
            );

            if (exito) {
                android.widget.Toast.makeText(this, "Actualizado", android.widget.Toast.LENGTH_SHORT).show();
                cargarGaleriaBento();
                dialog.dismiss();
            }
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void abrirGoogleMaps(String ubicacion) {
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            Toast.makeText(this, "Sin ubicación registrada", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri gmmIntentUri;
        // Si parece un link web
        if (ubicacion.startsWith("http") || ubicacion.startsWith("www")) {
            gmmIntentUri = Uri.parse(ubicacion);
        } else {
            // Si es texto (ej: "Aula 22"), le decimos a maps que lo busque
            gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(ubicacion));
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps"); // Intentar abrir app de Maps

        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            // Si no tiene app de maps, abrir en navegador
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, gmmIntentUri));
            } catch (Exception ex) {
                Toast.makeText(this, "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // ... otros métodos de tu clase ...

    // --- PEGA ESTO AL FINAL DE LA CLASE ---
    private void mostrarReloj() {
        // Hora actual por defecto para que el reloj empiece ahí
        java.util.Calendar c = java.util.Calendar.getInstance();
        int hora = c.get(java.util.Calendar.HOUR_OF_DAY);
        int min = c.get(java.util.Calendar.MINUTE);

        android.app.TimePickerDialog tpd = new android.app.TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    // Lógica para determinar AM o PM
                    String amPm = (hourOfDay < 12) ? "AM" : "PM";
                    int horaMostrar = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
                    if (horaMostrar == 0) horaMostrar = 12; // Para que las 00:00 sean las 12 AM

                    // Formateamos para que los minutos siempre tengan dos dígitos (ej: 05 en vez de 5)
                    String time = String.format("%02d:%02d %s", horaMostrar, minute, amPm);

                    // Ponemos el texto en el campo
                    edtMatHor.setText(time);
                }, hora, min, false); // 'false' es para que no sea formato 24h forzado
        tpd.show();
    }

} // <--- Esta es la llave final de MainActivity
