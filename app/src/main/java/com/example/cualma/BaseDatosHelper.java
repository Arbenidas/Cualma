package com.example.cualma;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseDatosHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CualmaDB_V5.db"; // Incrementé versión
    private static final int DATABASE_VERSION = 5;

    // Nombres de tablas y columnas (Igual que antes)
    private static final String TABLA_ALUMNOS = "alumnos";
    private static final String COL_ALUMNO_CODIGO = "codigo_alumno";
    private static final String COL_NOMBRES = "nombres";
    private static final String COL_APELLIDOS = "apellidos";

    private static final String TABLA_MATERIAS = "materias";
    private static final String COL_MATERIA_ID = "id_materia";
    private static final String COL_FK_ALUMNO = "fk_codigo_alumno";
    private static final String COL_MATERIA_CODIGO = "codigo_materia";
    private static final String COL_MATERIA_NOMBRE = "nombre_materia";
    private static final String COL_HORARIO = "horario";
    private static final String COL_AULA = "aula";
    private static final String COL_DOCENTE = "docente";
    private static final String COL_FOTO_URI = "foto_uri";
    private static final String COL_MAPS_UBICACION = "maps_ubicacion";

    public BaseDatosHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String crearTablaAlumnos = "CREATE TABLE " + TABLA_ALUMNOS + " (" +
                COL_ALUMNO_CODIGO + " TEXT PRIMARY KEY, " +
                COL_NOMBRES + " TEXT, " +
                COL_APELLIDOS + " TEXT)";
        db.execSQL(crearTablaAlumnos);

        String crearTablaMaterias = "CREATE TABLE " + TABLA_MATERIAS + " (" +
                COL_MATERIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FK_ALUMNO + " TEXT, " +
                COL_MATERIA_CODIGO + " TEXT, " +
                COL_MATERIA_NOMBRE + " TEXT, " +
                COL_HORARIO + " TEXT, " +
                COL_AULA + " TEXT, " +
                COL_DOCENTE + " TEXT, " +
                COL_FOTO_URI + " TEXT, " +
                COL_MAPS_UBICACION + " TEXT)";
        db.execSQL(crearTablaMaterias);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_MATERIAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLA_ALUMNOS);
        onCreate(db);
    }

    // --- MÉTODOS EXISTENTES ---
    public boolean insertarAlumno(String codigo, String nombres, String apellidos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ALUMNO_CODIGO, codigo);
        cv.put(COL_NOMBRES, nombres);
        cv.put(COL_APELLIDOS, apellidos);
        return db.insert(TABLA_ALUMNOS, null, cv) != -1;
    }

    public boolean insertarMateria(String codMat, String nomMat, String horario, String aula, String mapsUbicacion, String docente) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MATERIA_CODIGO, codMat);
        cv.put(COL_MATERIA_NOMBRE, nomMat);
        cv.put(COL_HORARIO, horario);
        cv.put(COL_AULA, aula);
        cv.put(COL_MAPS_UBICACION, (mapsUbicacion != null && !mapsUbicacion.isEmpty()) ? mapsUbicacion : aula);
        cv.put(COL_DOCENTE, docente);
        return db.insert(TABLA_MATERIAS, null, cv) != -1;
    }

    // --- NUEVO MÉTODO PARA EL SPINNER ---
    public List<String> obtenerListaAlumnosSimple() {
        List<String> alumnos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ALUMNO_CODIGO + ", " + COL_NOMBRES + " FROM " + TABLA_ALUMNOS, null);
        if (cursor.moveToFirst()) {
            do {
                // Formato: "Juan Perez (SM100)"
                String cod = cursor.getString(0);
                String nom = cursor.getString(1);
                alumnos.add(nom + " (" + cod + ")");
            } while (cursor.moveToNext());
        }
        cursor.close();
        return alumnos;
    }

    // --- EVITAR DUPLICADOS AL ASIGNAR ---
    public boolean asignarAlumnoPorCodigoMateria(String codigoAlumno, String codigoMateria) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 1. VERIFICAR SI YA EXISTE LA RELACIÓN
        Cursor check = db.query(TABLA_MATERIAS, null,
                COL_FK_ALUMNO + "=? AND " + COL_MATERIA_CODIGO + "=?",
                new String[]{codigoAlumno, codigoMateria}, null, null, null);

        if (check.getCount() > 0) {
            check.close();
            return false; // YA EXISTE, NO HACEMOS NADA
        }
        check.close();

        // 2. SI NO EXISTE, PROCEDEMOS A CREAR LA INSCRIPCIÓN
        Cursor cursor = db.query(TABLA_MATERIAS, null, COL_MATERIA_CODIGO + "=?", new String[]{codigoMateria}, null, null, null);
        if (cursor.moveToFirst()) {
            ContentValues nuevaInscripcion = new ContentValues();
            nuevaInscripcion.put(COL_FK_ALUMNO, codigoAlumno);
            nuevaInscripcion.put(COL_MATERIA_CODIGO, codigoMateria);
            // Copiamos datos de la materia base
            nuevaInscripcion.put(COL_MATERIA_NOMBRE, cursor.getString(cursor.getColumnIndexOrThrow(COL_MATERIA_NOMBRE)));
            nuevaInscripcion.put(COL_HORARIO, cursor.getString(cursor.getColumnIndexOrThrow(COL_HORARIO)));
            nuevaInscripcion.put(COL_AULA, cursor.getString(cursor.getColumnIndexOrThrow(COL_AULA)));
            nuevaInscripcion.put(COL_DOCENTE, cursor.getString(cursor.getColumnIndexOrThrow(COL_DOCENTE)));
            nuevaInscripcion.put(COL_MAPS_UBICACION, cursor.getString(cursor.getColumnIndexOrThrow(COL_MAPS_UBICACION)));
            nuevaInscripcion.put(COL_FOTO_URI, cursor.getString(cursor.getColumnIndexOrThrow(COL_FOTO_URI)));

            long id = db.insert(TABLA_MATERIAS, null, nuevaInscripcion);
            cursor.close();
            return id != -1;
        }
        cursor.close();
        return false;
    }

    // Agrega este método en BaseDatosHelper.java
    // --- AGREGAR EN BaseDatosHelper.java ---

    // 1. Método para obtener los datos actuales de la materia y llenar el formulario
    public Map<String, String> obtenerDetalleMateria(String codigoMateria) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, String> map = new java.util.HashMap<>();

        // Buscamos solo 1 registro porque todos los alumnos de la misma materia tienen el mismo horario/aula
        Cursor cursor = db.query(TABLA_MATERIAS, null, COL_MATERIA_CODIGO + "=?", new String[]{codigoMateria}, null, null, null, "1");

        if (cursor.moveToFirst()) {
            map.put("aula", cursor.getString(cursor.getColumnIndexOrThrow(COL_AULA)));
            map.put("maps", cursor.getString(cursor.getColumnIndexOrThrow(COL_MAPS_UBICACION)));
            map.put("horario", cursor.getString(cursor.getColumnIndexOrThrow(COL_HORARIO)));
            map.put("docente", cursor.getString(cursor.getColumnIndexOrThrow(COL_DOCENTE)));
        }
        cursor.close();
        return map;
    }

    // 2. Método para actualizar TODOS los campos de la materia (afecta a todos los alumnos inscritos)
    public boolean actualizarMateriaCompleta(String codigoMateria, String aula, String maps, String horario, String docente) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COL_AULA, aula);
        // Si el usuario deja vacío el link de maps, usamos el nombre del aula como ubicación
        cv.put(COL_MAPS_UBICACION, (maps != null && !maps.trim().isEmpty()) ? maps : aula);
        cv.put(COL_HORARIO, horario);
        cv.put(COL_DOCENTE, docente);

        // El WHERE es importante: actualiza por CÓDIGO para que cambie en todos los registros
        int filas = db.update(TABLA_MATERIAS, cv, COL_MATERIA_CODIGO + " = ?", new String[]{codigoMateria});
        return filas > 0;
    }

    // Actualizaciones y Consultas (Igual que antes)
    public void guardarFotoMateria(String codigoMateria, String fotoBase64) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FOTO_URI, fotoBase64);
        db.update(TABLA_MATERIAS, cv, COL_MATERIA_CODIGO + " = ?", new String[]{codigoMateria});
    }

    public boolean actualizarUbicacionMateria(String codigoMateria, String nuevaAula) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_AULA, nuevaAula);
        return db.update(TABLA_MATERIAS, cv, COL_MATERIA_CODIGO + " = ?", new String[]{codigoMateria}) > 0;
    }

    public boolean actualizarLinkMaps(String codigoMateria, String nuevaUbicacionMaps) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MAPS_UBICACION, nuevaUbicacionMaps);
        return db.update(TABLA_MATERIAS, cv, COL_MATERIA_CODIGO + " = ?", new String[]{codigoMateria}) > 0;
    }

    public String obtenerFotoMateria(String codigoMateria) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLA_MATERIAS, new String[]{COL_FOTO_URI}, COL_MATERIA_CODIGO + " = ?", new String[]{codigoMateria}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            String foto = cursor.getString(0);
            cursor.close();
            return foto;
        }
        return null;
    }

    public List<Map<String, String>> obtenerTodasMaterias() {
        List<Map<String, String>> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " +
                "m." + COL_MATERIA_CODIGO + ", " +
                "MAX(m." + COL_MATERIA_NOMBRE + ") as " + COL_MATERIA_NOMBRE + ", " +
                "MAX(m." + COL_HORARIO + ") as " + COL_HORARIO + ", " +
                "MAX(m." + COL_AULA + ") as " + COL_AULA + ", " +
                "MAX(m." + COL_MAPS_UBICACION + ") as " + COL_MAPS_UBICACION + ", " +
                "GROUP_CONCAT(a." + COL_NOMBRES + ", ', ') as lista_alumnos " + // Solo nombres, separado por coma
                "FROM " + TABLA_MATERIAS + " m " +
                "LEFT JOIN " + TABLA_ALUMNOS + " a ON m." + COL_FK_ALUMNO + " = a." + COL_ALUMNO_CODIGO + " " +
                "GROUP BY m." + COL_MATERIA_CODIGO;

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                Map<String, String> map = new HashMap<>();
                String codigo = cursor.getString(cursor.getColumnIndexOrThrow(COL_MATERIA_CODIGO));
                map.put("codigo", codigo);
                map.put("titulo", cursor.getString(cursor.getColumnIndexOrThrow(COL_MATERIA_NOMBRE)));
                map.put("horario", cursor.getString(cursor.getColumnIndexOrThrow(COL_HORARIO)));
                map.put("aula", cursor.getString(cursor.getColumnIndexOrThrow(COL_AULA)));
                map.put("maps_ubicacion", cursor.getString(cursor.getColumnIndexOrThrow(COL_MAPS_UBICACION)));
                String alumnos = cursor.getString(cursor.getColumnIndexOrThrow("lista_alumnos"));
                map.put("alumno", alumnos != null ? alumnos : "");
                lista.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }
}