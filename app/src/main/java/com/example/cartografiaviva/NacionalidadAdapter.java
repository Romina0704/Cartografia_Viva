package com.example.cartografiaviva;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NacionalidadAdapter extends RecyclerView.Adapter<NacionalidadAdapter.ViewHolder> {

    private final List<Etnia> etnias;
    private final OnEtniaClickListener listener;

    public interface OnEtniaClickListener {
        void onEtniaClick(Etnia etnia);
    }

    public NacionalidadAdapter(List<Etnia> etnias, OnEtniaClickListener listener) {
        this.etnias = etnias;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nacionalidad, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Etnia etnia = etnias.get(position);
        holder.tvNacionalidad.setText(etnia.nacionalidad);
        holder.tvRegion.setText("Región: " + etnia.region);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEtniaClick(etnia);
        });
    }

    @Override
    public int getItemCount() {
        return etnias != null ? etnias.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNacionalidad, tvRegion;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNacionalidad = itemView.findViewById(R.id.tv_item_nacionalidad);
            tvRegion = itemView.findViewById(R.id.tv_item_region);
        }
    }
}
