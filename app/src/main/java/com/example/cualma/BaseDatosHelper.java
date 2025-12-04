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

    private static final String DATABASE_NAME = "CualmaDB_V6.db"; // Subimos versión
    private static final int DATABASE_VERSION = 6;

    // Nombres de tablas y columnas
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

    // --- MÉTODOS DE INSERCIÓN ---
    public boolean insertarAlumno(String codigo, String nombres, String apellidos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ALUMNO_CODIGO, codigo);
        cv.put(COL_NOMBRES, nombres);
        cv.put(COL_APELLIDOS, apellidos);
        long result = db.insert(TABLA_ALUMNOS, null, cv);
        return result != -1;
    }

    public boolean insertarMateria(String codMat, String nomMat, String horario, String aula, String mapsUbicacion, String docente) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Verificar si ya existe
        Cursor cursor = db.query(TABLA_MATERIAS, null, COL_MATERIA_CODIGO + "=?", new String[]{codMat}, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.close();
            return false; // Ya existe
        }
        cursor.close();

        ContentValues cv = new ContentValues();
        cv.put(COL_MATERIA_CODIGO, codMat);
        cv.put(COL_MATERIA_NOMBRE, nomMat);
        cv.put(COL_HORARIO, horario);
        cv.put(COL_AULA, aula);
        cv.put(COL_MAPS_UBICACION, (mapsUbicacion != null) ? mapsUbicacion : "");
        cv.put(COL_DOCENTE, docente);
        return db.insert(TABLA_MATERIAS, null, cv) != -1;
    }

    // --- MÉTODOS DE CONSULTA Y LISTAS ---
    public List<String> obtenerListaAlumnosSimple() {
        List<String> alumnos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ALUMNO_CODIGO + ", " + COL_NOMBRES + " FROM " + TABLA_ALUMNOS, null);
        if (cursor.moveToFirst()) {
            do {
                String cod = cursor.getString(0);
                String nom = cursor.getString(1);
                alumnos.add(nom + " (" + cod + ")");
            } while (cursor.moveToNext());
        }
        cursor.close();
        return alumnos;
    }

    public List<Map<String, String>> obtenerTodasMaterias() {
        List<Map<String, String>> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query optimizada para agrupar alumnos en una sola fila
        String query = "SELECT " +
                "m." + COL_MATERIA_CODIGO + ", " +
                "MAX(m." + COL_MATERIA_NOMBRE + ") as " + COL_MATERIA_NOMBRE + ", " +
                "MAX(m." + COL_HORARIO + ") as " + COL_HORARIO + ", " +
                "MAX(m." + COL_AULA + ") as " + COL_AULA + ", " +
                "MAX(m." + COL_MAPS_UBICACION + ") as " + COL_MAPS_UBICACION + ", " +
                "GROUP_CONCAT(a." + COL_NOMBRES + ", ', ') as lista_alumnos " +
                "FROM " + TABLA_MATERIAS + " m " +
                "LEFT JOIN " + TABLA_ALUMNOS + " a ON m." + COL_FK_ALUMNO + " = a." + COL_ALUMNO_CODIGO + " " +
                "GROUP BY m." + COL_MATERIA_CODIGO;

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                Map<String, String> map = new HashMap<>();
                map.put("codigo", cursor.getString(cursor.getColumnIndexOrThrow(COL_MATERIA_CODIGO)));
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

    // --- MÉTODOS DE DETALLE Y ACTUALIZACIÓN ---
    public Map<String, String> obtenerDetalleMateria(String codigoMateria) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, String> map = new HashMap<>();
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
    // --- BÚSQUEDA ---
    public List<Map<String, String>> buscarMaterias(String texto) {
        List<Map<String, String>> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Buscamos coincidencias en Nombre, Código o Docente
        String query = "SELECT " +
                "m." + COL_MATERIA_CODIGO + ", " +
                "MAX(m." + COL_MATERIA_NOMBRE + ") as " + COL_MATERIA_NOMBRE + ", " +
                "MAX(m." + COL_HORARIO + ") as " + COL_HORARIO + ", " +
                "MAX(m." + COL_AULA + ") as " + COL_AULA + ", " +
                "MAX(m." + COL_MAPS_UBICACION + ") as " + COL_MAPS_UBICACION + ", " +
                "GROUP_CONCAT(a." + COL_NOMBRES + ", ', ') as lista_alumnos " +
                "FROM " + TABLA_MATERIAS + " m " +
                "LEFT JOIN " + TABLA_ALUMNOS + " a ON m." + COL_FK_ALUMNO + " = a." + COL_ALUMNO_CODIGO + " " +
                "WHERE m." + COL_MATERIA_NOMBRE + " LIKE ? OR m." + COL_MATERIA_CODIGO + " LIKE ? OR m." + COL_DOCENTE + " LIKE ? " +
                "GROUP BY m." + COL_MATERIA_CODIGO;

        String pattern = "%" + texto + "%";
        Cursor cursor = db.rawQuery(query, new String[]{pattern, pattern, pattern});

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> map = new HashMap<>();
                map.put("codigo", cursor.getString(cursor.getColumnIndexOrThrow(COL_MATERIA_CODIGO)));
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

    // --- GESTIÓN DE DOCENTES (ADMIN) ---
    public List<String> obtenerListaDocentesUnicos() {
        List<String> docentes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Obtenemos los nombres distintos que no estén vacíos
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COL_DOCENTE + " FROM " + TABLA_MATERIAS + " WHERE " + COL_DOCENTE + " IS NOT NULL AND " + COL_DOCENTE + " != ''", null);
        if (cursor.moveToFirst()) {
            do {
                docentes.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return docentes;
    }

    public boolean eliminarDocenteDeTodasMaterias(String nombreDocente) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DOCENTE, ""); // Lo dejamos vacío, no borramos la materia
        return db.update(TABLA_MATERIAS, cv, COL_DOCENTE + "=?", new String[]{nombreDocente}) > 0;
    }

    public boolean actualizarMateriaCompleta(String codigoMateria, String aula, String maps, String horario, String docente) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_AULA, aula);
        cv.put(COL_MAPS_UBICACION, maps);
        cv.put(COL_HORARIO, horario);
        cv.put(COL_DOCENTE, docente);

        // Actualiza TODAS las filas que tengan ese código de materia
        return db.update(TABLA_MATERIAS, cv, COL_MATERIA_CODIGO + " = ?", new String[]{codigoMateria}) > 0;
    }

    public void guardarFotoMateria(String codigoMateria, String nombreArchivo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FOTO_URI, nombreArchivo);
        db.update(TABLA_MATERIAS, cv, COL_MATERIA_CODIGO + " = ?", new String[]{codigoMateria});
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

    // --- MÉTODOS DE ASIGNACIÓN Y ELIMINACIÓN ---
    public boolean asignarAlumnoPorCodigoMateria(String codigoAlumno, String codigoMateria) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 1. Evitar duplicados
        Cursor check = db.query(TABLA_MATERIAS, null, COL_FK_ALUMNO + "=? AND " + COL_MATERIA_CODIGO + "=?",
                new String[]{codigoAlumno, codigoMateria}, null, null, null);
        if (check.getCount() > 0) {
            check.close();
            return false;
        }
        check.close();

        // 2. Copiar datos de la materia base
        Cursor cursor = db.query(TABLA_MATERIAS, null, COL_MATERIA_CODIGO + "=?", new String[]{codigoMateria}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            ContentValues nuevaInscripcion = new ContentValues();
            nuevaInscripcion.put(COL_FK_ALUMNO, codigoAlumno);
            nuevaInscripcion.put(COL_MATERIA_CODIGO, codigoMateria);
            nuevaInscripcion.put(COL_MATERIA_NOMBRE, cursor.getString(cursor.getColumnIndexOrThrow(COL_MATERIA_NOMBRE)));
            nuevaInscripcion.put(COL_HORARIO, cursor.getString(cursor.getColumnIndexOrThrow(COL_HORARIO)));
            nuevaInscripcion.put(COL_AULA, cursor.getString(cursor.getColumnIndexOrThrow(COL_AULA)));
            nuevaInscripcion.put(COL_DOCENTE, cursor.getString(cursor.getColumnIndexOrThrow(COL_DOCENTE)));
            nuevaInscripcion.put(COL_MAPS_UBICACION, cursor.getString(cursor.getColumnIndexOrThrow(COL_MAPS_UBICACION)));
            nuevaInscripcion.put(COL_FOTO_URI, cursor.getString(cursor.getColumnIndexOrThrow(COL_FOTO_URI)));

            db.insert(TABLA_MATERIAS, null, nuevaInscripcion);
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }

    // --- NUEVOS MÉTODOS DE ELIMINACIÓN ---

    // 1. Obtener lista de alumnos de una materia específica (para saber a quién borrar de la clase)
    public List<String> obtenerAlumnosPorMateria(String codMateria) {
        List<String> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT a." + COL_NOMBRES + ", a." + COL_ALUMNO_CODIGO + " " +
                "FROM " + TABLA_MATERIAS + " m " +
                "JOIN " + TABLA_ALUMNOS + " a ON m." + COL_FK_ALUMNO + " = a." + COL_ALUMNO_CODIGO + " " +
                "WHERE m." + COL_MATERIA_CODIGO + " = ?";
        Cursor c = db.rawQuery(query, new String[]{codMateria});
        if (c.moveToFirst()) {
            do {
                // Formato: "Juan (COD123)"
                lista.add(c.getString(0) + " (" + c.getString(1) + ")");
            } while (c.moveToNext());
        }
        c.close();
        return lista;
    }

    // 2. Eliminar (desinscribir) a un alumno de una materia
    public boolean eliminarAlumnoDeMateria(String codigoAlumno, String codigoMateria) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Borramos solo la fila que relaciona al alumno con esa materia
        return db.delete(TABLA_MATERIAS,
                COL_FK_ALUMNO + "=? AND " + COL_MATERIA_CODIGO + "=?",
                new String[]{codigoAlumno, codigoMateria}) > 0;
    }

    // 3. Eliminar un alumno COMPLETAMENTE (de la lista global y de todas sus clases)
    public boolean eliminarAlumnoGlobal(String codigoAlumno) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Primero lo sacamos de todas las materias
        db.delete(TABLA_MATERIAS, COL_FK_ALUMNO + "=?", new String[]{codigoAlumno});
        // Luego lo borramos del registro de alumnos
        return db.delete(TABLA_ALUMNOS, COL_ALUMNO_CODIGO + "=?", new String[]{codigoAlumno}) > 0;
    }
    // Nuevo método para eliminar
    public boolean eliminarMateria(String codigoMateria) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLA_MATERIAS, COL_MATERIA_CODIGO + "=?", new String[]{codigoMateria}) > 0;
    }
}