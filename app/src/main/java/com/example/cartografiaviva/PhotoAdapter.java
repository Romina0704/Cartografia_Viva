package com.example.cartografiaviva;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private final Context context;
    private final List<Integer> fotos; // Lista de R.drawable.xxx

    public PhotoAdapter(Context context, List<Integer> fotos) {
        this.context = context;
        this.fotos = fotos;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Creamos un ImageView dinámicamente (sin layout XML extra)
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new PhotoViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.imageView.setImageResource(fotos.get(position));
    }

    @Override
    public int getItemCount() {
        return fotos != null ? fotos.size() : 0;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}