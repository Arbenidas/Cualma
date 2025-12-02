package com.example.cualma;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MateriaAdapter extends RecyclerView.Adapter<MateriaAdapter.ViewHolder> {

    // Inicializamos la lista para evitar NullPointerException si llega nula
    private List<Map<String, String>> datos = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAddAlumnoClick(String codigoMateria);
        void onOptionsClick(Map<String, String> item);
    }

    public MateriaAdapter(List<Map<String, String>> datos, OnItemClickListener listener) {
        if (datos != null) {
            this.datos = datos;
        }
        this.listener = listener;
    }

    // --- NUEVO MÉTODO IMPORTANTE ---
    // Esto permite refrescar la cuadrícula sin recrear el adaptador (mantiene la posición del scroll)
    @SuppressLint("NotifyDataSetChanged")
    public void actualizarDatos(List<Map<String, String>> nuevosDatos) {
        this.datos = (nuevosDatos != null) ? nuevosDatos : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_materia_bento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = datos.get(position);

        // Usamos una función auxiliar para evitar poner "null" en pantalla
        holder.tvTitulo.setText(obtenerTexto(item, "titulo"));
        holder.tvCodigo.setText(obtenerTexto(item, "codigo"));
        holder.tvHorario.setText(obtenerTexto(item, "horario"));
        holder.tvAula.setText(obtenerTexto(item, "aula"));

        String alumnos = item.get("alumno");
        if(alumnos == null || alumnos.trim().isEmpty()){
            holder.tvAlumnos.setText("Sin alumnos inscritos");
            holder.tvAlumnos.setAlpha(0.5f); // Hacemos el texto un poco más gris si está vacío
        } else {
            holder.tvAlumnos.setText(alumnos);
            holder.tvAlumnos.setAlpha(1.0f);
        }

        // Click en "Agregar Alumno" (Icono + Texto pequeño)
        holder.btnBentoAddAlumno.setOnClickListener(v -> {
            if (listener != null) listener.onAddAlumnoClick(item.get("codigo"));
        });

        // Click en "Opciones" (Botón de 3 puntos o toda la tarjeta)
        holder.btnBentoOptions.setOnClickListener(v -> {
            if (listener != null) listener.onOptionsClick(item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOptionsClick(item);
        });
    }

    // Método auxiliar seguro
    private String obtenerTexto(Map<String, String> item, String key) {
        String val = item.get(key);
        return (val != null && !val.equals("null")) ? val : "---";
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvCodigo, tvHorario, tvAula, tvAlumnos;
        LinearLayout btnBentoAddAlumno, btnBentoOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // --- CORRECCIÓN: USAR LOS IDs DE "item_materia_bento.xml" ---

            // Títulos y Textos
            tvTitulo = itemView.findViewById(R.id.tvBentoTitulo);
            tvCodigo = itemView.findViewById(R.id.tvBentoCodigo);
            tvHorario = itemView.findViewById(R.id.tvBentoHorario);
            tvAula = itemView.findViewById(R.id.tvBentoAula);
            tvAlumnos = itemView.findViewById(R.id.tvBentoAlumnos);

            // Botones (que en realidad son LinearLayouts en el XML)
            btnBentoAddAlumno = itemView.findViewById(R.id.btnBentoAddAlumno);
            btnBentoOptions = itemView.findViewById(R.id.btnBentoOptions);
        }
    }
}