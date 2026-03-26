package com.example.cartografiaviva;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private final Context context;
    private final List<Integer> fotos;
    private final OnPhotoClickListener listener;
    private final ImageView.ScaleType scaleType;

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
    }

    public PhotoAdapter(Context context, List<Integer> fotos, OnPhotoClickListener listener) {
        this(context, fotos, ImageView.ScaleType.CENTER_CROP, listener);
    }

    public PhotoAdapter(Context context, List<Integer> fotos, ImageView.ScaleType scaleType, OnPhotoClickListener listener) {
        this.context = context;
        this.fotos = fotos;
        this.scaleType = scaleType;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(scaleType);
        return new PhotoViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.imageView.setImageResource(fotos.get(position));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(position);
            }
        });
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
