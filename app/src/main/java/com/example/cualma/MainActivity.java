package com.example.cualma;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    BaseDatosHelper miBD;

    // Vistas principales
    RecyclerView recyclerBento;
    TextView tvVacio;
    FrameLayout overlayModal;
    CardView cardSeleccion, cardFormAlumno, cardFormMateria;
    FloatingActionButton fabRegistrar;

    // Campos Formulario Alumno
    EditText edtAluCod, edtAluNom, edtAluApe;

    // Campos Formulario Materia
    EditText edtMatCod, edtMatNom, edtMatHor, edtMatAula, edtMatDoc;

    // Variables temporales
    String materiaCodigoFotoActual = "";
    ActivityResultLauncher<Intent> cameraLauncher;
    MateriaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        miBD = new BaseDatosHelper(this);

        // 1. Inicialización limpia de vistas
        inicializarVistas();

        // 2. Preparar la cámara
        inicializarCameraLauncher();

        // 3. Cargar datos
        cargarGaleriaBento();

        // --- LISTENERS PRINCIPALES ---

        // Botón flotante (+)
        fabRegistrar.setOnClickListener(v -> mostrarModal(cardSeleccion));

        // Selección: NUEVO ALUMNO
        Button btnNuevoAlumno = findViewById(R.id.btnOpcionAlumno);
        // Click normal -> Crear alumno
        btnNuevoAlumno.setOnClickListener(v -> mostrarModal(cardFormAlumno));
        // Click LARGO -> Eliminar alumnos del sistema
        btnNuevoAlumno.setOnLongClickListener(v -> {
            mostrarListaBorrarAlumnosGlobal();
            return true;
        });

        // Selección: NUEVA MATERIA
        findViewById(R.id.btnOpcionMateria).setOnClickListener(v -> mostrarModal(cardFormMateria));

        // Cerrar modales
        findViewById(R.id.btnCerrarModal).setOnClickListener(v -> cerrarTodoModal());
        findViewById(R.id.btnCancelarAlu).setOnClickListener(v -> cerrarTodoModal());
        findViewById(R.id.btnCancelarMat).setOnClickListener(v -> cerrarTodoModal());
// --- BUSCADOR ---
        // Al pulsar el FAB de Lupa
        FloatingActionButton fabBuscar = findViewById(R.id.fabBuscar);
        fabBuscar.setOnClickListener(v -> mostrarDialogoBusqueda());

        // --- ADMIN ---
        // Al pulsar el engranaje de arriba
        findViewById(R.id.btnAdmin).setOnClickListener(v -> mostrarMenuAdmin());
        // --- GUARDAR ALUMNO ---
        findViewById(R.id.btnGuardarAluFinal).setOnClickListener(v -> {
            String cod = edtAluCod.getText().toString().trim();
            String nom = edtAluNom.getText().toString().trim();
            String ape = edtAluApe.getText().toString().trim();

            if (cod.isEmpty() || nom.isEmpty()) {
                Toast.makeText(this, "Código y Nombre son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            if(miBD.insertarAlumno(cod, nom, ape)) {
                Toast.makeText(this, "Alumno Registrado", Toast.LENGTH_SHORT).show();
                cerrarTodoModal();
            } else {
                Toast.makeText(this, "Error: Código de alumno duplicado", Toast.LENGTH_SHORT).show();
            }
        });

        // --- GUARDAR MATERIA ---
        findViewById(R.id.btnGuardarMatFinal).setOnClickListener(v -> {
            String cod = edtMatCod.getText().toString().trim();
            String nom = edtMatNom.getText().toString().trim();

            if (cod.isEmpty() || nom.isEmpty()) {
                Toast.makeText(this, "Código de materia y Asignatura son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            if(miBD.insertarMateria(
                    cod,
                    nom,
                    edtMatHor.getText().toString(),
                    edtMatAula.getText().toString(),
                    null, // Ubicación Maps inicial vacía
                    edtMatDoc.getText().toString())) {

                Toast.makeText(this, "Materia Creada", Toast.LENGTH_SHORT).show();
                cargarGaleriaBento();
                cerrarTodoModal();
            } else {
                Toast.makeText(this, "Error: Ya existe una materia con ese código", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // 1. Menú Principal de Admin
    private void mostrarMenuAdmin() {
        String[] opciones = {"👨‍🎓 Gestionar Alumnos", "📚 Gestionar Materias", "👨‍🏫 Gestionar Profesores"};

        new AlertDialog.Builder(this)
                .setTitle("Panel de Administración")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) adminGestionarAlumnos();
                    if (which == 1) adminGestionarMaterias();
                    if (which == 2) adminGestionarProfesores();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // 2. Gestión de Alumnos (Borrar del sistema)
    private void adminGestionarAlumnos() {
        mostrarListaBorrarAlumnosGlobal(); // Reutilizamos el método que ya creamos antes
    }

    // 3. Gestión de Materias (Borrar materias)
    private void adminGestionarMaterias() {
        List<Map<String, String>> materias = miBD.obtenerTodasMaterias();
        if (materias.isEmpty()) {
            Toast.makeText(this, "No hay materias registradas", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preparamos lista de nombres para mostrar
        String[] nombresMaterias = new String[materias.size()];
        final String[] codigosMaterias = new String[materias.size()];

        for (int i = 0; i < materias.size(); i++) {
            nombresMaterias[i] = materias.get(i).get("titulo") + " (" + materias.get(i).get("codigo") + ")";
            codigosMaterias[i] = materias.get(i).get("codigo");
        }

        new AlertDialog.Builder(this)
                .setTitle("Eliminar Materia")
                .setItems(nombresMaterias, (dialog, which) -> {
                    String codigoABorrar = codigosMaterias[which];
                    new AlertDialog.Builder(this)
                            .setTitle("⚠️ Confirmar")
                            .setMessage("¿Eliminar " + nombresMaterias[which] + "?")
                            .setPositiveButton("Sí, Eliminar", (d, w) -> {
                                if (miBD.eliminarMateria(codigoABorrar)) {
                                    Toast.makeText(this, "Materia eliminada", Toast.LENGTH_SHORT).show();
                                    cargarGaleriaBento();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                })
                .show();
    }

    // 4. Gestión de Profesores (Borrar/Desasignar)
    private void adminGestionarProfesores() {
        List<String> docentes = miBD.obtenerListaDocentesUnicos();
        if (docentes.isEmpty()) {
            Toast.makeText(this, "No hay profesores asignados", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] arrayDocentes = docentes.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Gestionar Profesores")
                .setItems(arrayDocentes, (dialog, which) -> {
                    String nombreProfesor = arrayDocentes[which];
                    new AlertDialog.Builder(this)
                            .setTitle(nombreProfesor)
                            .setMessage("¿Qué deseas hacer con este profesor?")
                            .setPositiveButton("Eliminar de Materias", (d, w) -> {
                                // Esto borra el nombre del profesor de todas las materias, pero no borra las materias
                                if (miBD.eliminarDocenteDeTodasMaterias(nombreProfesor)) {
                                    Toast.makeText(this, "Profesor eliminado de sus asignaciones", Toast.LENGTH_SHORT).show();
                                    cargarGaleriaBento();
                                }
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .show();
    }
    private void mostrarDialogoBusqueda() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔍 Buscar");

        // Creamos un EditText programáticamente para no crear otro XML
        final EditText input = new EditText(this);
        input.setHint("Escribe materia, código o profesor...");
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        // Botón Buscar
        builder.setPositiveButton("Buscar", (dialog, which) -> {
            String query = input.getText().toString();
            List<Map<String, String>> resultados = miBD.buscarMaterias(query);
            adapter.actualizarDatos(resultados);

            if (resultados.isEmpty()) {
                Toast.makeText(this, "No se encontraron resultados", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón "Ver Todo" para restaurar la lista
        builder.setNeutralButton("Ver Todo", (dialog, which) -> {
            cargarGaleriaBento(); // Recarga la lista completa
        });

        builder.show();
    }

    private void inicializarVistas() {
        // Layouts contenedores
        overlayModal = findViewById(R.id.overlayModal);
        cardSeleccion = findViewById(R.id.cardSeleccion);
        cardFormAlumno = findViewById(R.id.cardFormAlumno);
        cardFormMateria = findViewById(R.id.cardFormMateria);
        tvVacio = findViewById(R.id.tvVacio);

        // Listas y Botones
        fabRegistrar = findViewById(R.id.fabRegistrar);
        recyclerBento = findViewById(R.id.listaGaleria);
        recyclerBento.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        // Campos Alumno
        edtAluCod = findViewById(R.id.edtAluCod);
        edtAluNom = findViewById(R.id.edtAluNom);
        edtAluApe = findViewById(R.id.edtAluApe);

        // Campos Materia
        edtMatCod = findViewById(R.id.edtMatCod);
        edtMatNom = findViewById(R.id.edtMatNom);
        edtMatAula = findViewById(R.id.edtMatAula);
        edtMatDoc = findViewById(R.id.edtMatDoc);

        // Campo Horario con Reloj
        edtMatHor = findViewById(R.id.edtMatHor);
        edtMatHor.setFocusable(false);
        edtMatHor.setClickable(true);
        edtMatHor.setOnClickListener(v -> mostrarReloj());
    }

    private void cargarGaleriaBento() {
        List<Map<String, String>> datos = miBD.obtenerTodasMaterias();

        if (datos.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
            recyclerBento.setVisibility(View.GONE);
        } else {
            tvVacio.setVisibility(View.GONE);
            recyclerBento.setVisibility(View.VISIBLE);

            if (adapter != null) {
                adapter.actualizarDatos(datos);
            } else {
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

    // --- GESTIÓN DE ALUMNOS (SPINNER) ---
    private void mostrarDialogoAsignarAlumno(String codigoMateria) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Alumno a la Clase");

        View view = getLayoutInflater().inflate(R.layout.dialog_asignar_alumno, null);
        Spinner spinner = view.findViewById(R.id.spinnerAlumnos);

        List<String> listaAlumnos = miBD.obtenerListaAlumnosSimple();

        if (listaAlumnos.isEmpty()) {
            Toast.makeText(this, "Primero registra alumnos en el botón '+'", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listaAlumnos);
        spinner.setAdapter(adapterSpinner);

        builder.setView(view);
        builder.setPositiveButton("Asignar", (dialog, which) -> {
            if (spinner.getSelectedItem() != null) {
                String seleccion = spinner.getSelectedItem().toString();
                // Extraer el código "Juan (SM100)" -> "SM100"
                try {
                    String codigoAlumno = seleccion.substring(seleccion.lastIndexOf("(") + 1, seleccion.lastIndexOf(")"));
                    boolean exito = miBD.asignarAlumnoPorCodigoMateria(codigoAlumno, codigoMateria);

                    if(exito) {
                        Toast.makeText(this, "Alumno asignado", Toast.LENGTH_SHORT).show();
                        cargarGaleriaBento();
                    } else {
                        Toast.makeText(this, "El alumno ya está inscrito", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error al leer código", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // --- MENÚ DE OPCIONES DE LA TARJETA ---
    private void mostrarOpcionesMateria(Map<String, String> item) {
        String codigo = item.get("codigo");
        String aula = item.get("aula");
        String maps = item.get("maps_ubicacion");

        // Opciones actualizadas
        String[] opciones = {"📍 Ver Mapa", "🖼️ Ver Foto Aula", "✏️ Editar / Borrar Materia", "📸 Tomar Foto Rápida", "👥 Gestionar Alumnos"};

        new AlertDialog.Builder(this)
                .setTitle(item.get("titulo"))
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) abrirGoogleMaps(maps != null && !maps.isEmpty() ? maps : aula);
                    if (which == 1) verFotoGuardada(codigo);
                    if (which == 2) mostrarDialogoEdicion(codigo, aula, maps);
                    if (which == 3) {
                        materiaCodigoFotoActual = codigo;
                        abrirCamara();
                    }
                    if (which == 4) mostrarGestionAlumnosMateria(codigo);
                }).show();
    }

    // --- NUEVO: GESTIONAR ALUMNOS DE UNA CLASE (ELIMINAR) ---
    private void mostrarGestionAlumnosMateria(String codigoMateria) {
        List<String> alumnos = miBD.obtenerAlumnosPorMateria(codigoMateria);

        if (alumnos.isEmpty()) {
            Toast.makeText(this, "No hay alumnos inscritos en esta materia", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] arrayAlumnos = alumnos.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Toca para quitar alumno")
                .setItems(arrayAlumnos, (dialog, which) -> {
                    String seleccionado = arrayAlumnos[which];
                    String codAlumno = seleccionado.substring(seleccionado.lastIndexOf("(") + 1, seleccionado.lastIndexOf(")"));

                    new AlertDialog.Builder(this)
                            .setTitle("¿Quitar de la clase?")
                            .setMessage("Se desinscribirá a " + seleccionado + " de esta materia.")
                            .setPositiveButton("Quitar", (d, w) -> {
                                if (miBD.eliminarAlumnoDeMateria(codAlumno, codigoMateria)) {
                                    Toast.makeText(this, "Alumno eliminado de la materia", Toast.LENGTH_SHORT).show();
                                    cargarGaleriaBento();
                                }
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // --- NUEVO: ELIMINAR ALUMNOS GLOBALMENTE ---
    private void mostrarListaBorrarAlumnosGlobal() {
        List<String> todosLosAlumnos = miBD.obtenerListaAlumnosSimple();

        if (todosLosAlumnos.isEmpty()) {
            Toast.makeText(this, "No hay alumnos registrados en el sistema", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] arrayAlumnos = todosLosAlumnos.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("ELIMINAR ALUMNO (Sistema)")
                .setItems(arrayAlumnos, (dialog, which) -> {
                    String seleccionado = arrayAlumnos[which];
                    String codAlumno = seleccionado.substring(seleccionado.lastIndexOf("(") + 1, seleccionado.lastIndexOf(")"));

                    new AlertDialog.Builder(this)
                            .setTitle("⚠️ ¿Eliminar Definitivamente?")
                            .setMessage("Se borrará a " + seleccionado + " y se le sacará de TODAS las materias.")
                            .setPositiveButton("Borrar", (d, w) -> {
                                if (miBD.eliminarAlumnoGlobal(codAlumno)) {
                                    Toast.makeText(this, "Alumno eliminado del sistema", Toast.LENGTH_SHORT).show();
                                    cargarGaleriaBento();
                                    cerrarTodoModal();
                                }
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // --- SISTEMA DE FOTOS (GUARDAR EN ARCHIVO) ---

    private void inicializarCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        String nombreArchivo = guardarImagenEnDispositivo(imageBitmap, materiaCodigoFotoActual);

                        if (nombreArchivo != null) {
                            miBD.guardarFotoMateria(materiaCodigoFotoActual, nombreArchivo);
                            Toast.makeText(this, "Foto guardada correctamente", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private String guardarImagenEnDispositivo(Bitmap bitmap, String nombreBase) {
        try {
            String nombreArchivo = "IMG_" + nombreBase + "_" + System.currentTimeMillis() + ".jpg";
            FileOutputStream fos = openFileOutput(nombreArchivo, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            fos.close();
            return nombreArchivo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap cargarImagenDelDispositivo(String nombreArchivo) {
        try {
            FileInputStream fis = openFileInput(nombreArchivo);
            return BitmapFactory.decodeStream(fis);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try { cameraLauncher.launch(takePictureIntent); } catch (Exception e) {
            Toast.makeText(this, "No se encontró cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void verFotoGuardada(String codigoMateria) {
        String nombreArchivo = miBD.obtenerFotoMateria(codigoMateria);

        if (nombreArchivo != null && !nombreArchivo.isEmpty()) {
            Bitmap bitmap = cargarImagenDelDispositivo(nombreArchivo);

            if (bitmap != null) {
                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                new AlertDialog.Builder(this)
                        .setTitle("Foto del Aula")
                        .setView(imageView)
                        .setPositiveButton("Cerrar", null)
                        .show();
            } else {
                Toast.makeText(this, "La imagen ya no existe en el dispositivo", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay foto guardada para esta materia", Toast.LENGTH_SHORT).show();
        }
    }

    // --- EDICIÓN AVANZADA (CON BOTÓN ELIMINAR) ---
    private void mostrarDialogoEdicion(String codigoMateria, String aulaActual, String mapsActual) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_editar_bento, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText inputAula = view.findViewById(R.id.edtEditAula);
        EditText inputMaps = view.findViewById(R.id.edtEditMaps);
        EditText inputHorario = view.findViewById(R.id.edtEditHorario);
        EditText inputDocente = view.findViewById(R.id.edtEditDocente);
        Button btnFoto = view.findViewById(R.id.btnEditFoto);
        Button btnGuardar = view.findViewById(R.id.btnEditGuardar);
        Button btnCancelar = view.findViewById(R.id.btnEditCancelar);
        Button btnEliminar = view.findViewById(R.id.btnEliminarMateria);

        // Pre-llenar datos
        Map<String, String> detalles = miBD.obtenerDetalleMateria(codigoMateria);
        if (detalles != null && !detalles.isEmpty()) {
            inputAula.setText(detalles.get("aula"));
            inputMaps.setText(detalles.get("maps"));
            inputHorario.setText(detalles.get("horario"));
            inputDocente.setText(detalles.get("docente"));
        } else {
            inputAula.setText(aulaActual);
            inputMaps.setText(mapsActual);
        }

        // Configurar TimePicker
        inputHorario.setFocusable(false);
        inputHorario.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new android.app.TimePickerDialog(this, (tpView, hourOfDay, minute) -> {
                String amPm = (hourOfDay < 12) ? "AM" : "PM";
                int horaMostrar = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
                if (horaMostrar == 0) horaMostrar = 12;
                inputHorario.setText(String.format("%02d:%02d %s", horaMostrar, minute, amPm));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        btnFoto.setOnClickListener(v -> {
            materiaCodigoFotoActual = codigoMateria;
            abrirCamara();
        });

        // BOTÓN ELIMINAR MATERIA
        btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Eliminar Materia")
                    .setMessage("¿Estás seguro? Se borrará la materia y todas sus inscripciones.")
                    .setPositiveButton("ELIMINAR", (d, w) -> {
                        if(miBD.eliminarMateria(codigoMateria)) {
                            Toast.makeText(this, "Materia eliminada", Toast.LENGTH_SHORT).show();
                            cargarGaleriaBento();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
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
                Toast.makeText(this, "Información Actualizada", Toast.LENGTH_SHORT).show();
                cargarGaleriaBento();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- UTILIDADES (MAPAS, RELOJ, MODALES) ---

    private void abrirGoogleMaps(String ubicacion) {
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            Toast.makeText(this, "Sin ubicación registrada", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri gmmIntentUri;
        if (ubicacion.startsWith("http") || ubicacion.startsWith("www")) {
            gmmIntentUri = Uri.parse(ubicacion);
        } else {
            gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(ubicacion));
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, gmmIntentUri));
            } catch (Exception ex) {
                Toast.makeText(this, "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void mostrarReloj() {
        Calendar c = Calendar.getInstance();
        int hora = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);

        android.app.TimePickerDialog tpd = new android.app.TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String amPm = (hourOfDay < 12) ? "AM" : "PM";
                    int horaMostrar = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
                    if (horaMostrar == 0) horaMostrar = 12;
                    edtMatHor.setText(String.format("%02d:%02d %s", horaMostrar, minute, amPm));
                }, hora, min, false);
        tpd.show();
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
}