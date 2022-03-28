package com.clickfreebackup.clickfree;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.clickfreebackup.clickfree.model.UrlBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FacebookGalleryAdapter extends RecyclerView.Adapter<FacebookGalleryAdapter.ViewHolder> {
    private Context context;
    private List<UrlBody> urlList;
    private HashMap<String, String> instagramUrlMap;
    private HashMap<String, String> newInstagramUrlMap;
    private int windowWidth;

    FacebookGalleryAdapter(Context context, HashMap<String, String> mediaUrlMap, int windowWidth) {
        this.context = context;
        this.instagramUrlMap = mediaUrlMap;
        this.windowWidth = windowWidth;
        setUrlList(mediaUrlMap);
        newInstagramUrlMap = new HashMap<>();
    }

    HashMap<String, String> getNewInstagramUrlMap() {
        return newInstagramUrlMap;
    }

    private void setUrlList(HashMap<String, String> mediaUrlMap) {
        urlList = new ArrayList<>();
        for (String key : mediaUrlMap.keySet()) {
            urlList.add(new UrlBody(mediaUrlMap.get(key), false));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater
                .from(viewGroup.getContext()).inflate(R.layout.gallery_item_layout, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        final UrlBody urlBody = urlList.get(position);
        setImageUrl(viewHolder.image, urlBody.getUrl());
        if (urlBody.isSelected()) {
            viewHolder.imageMask.setVisibility(View.VISIBLE);
        } else {
            viewHolder.imageMask.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return urlList.size();
    }

    private void setImageUrl(final ImageView imageView, String url) {
        Glide.with(context)
                .load(Uri.parse(url))
                .into(imageView);
    }

    private void fillNewInstagramMap(String url) {
        for (String key : instagramUrlMap.keySet()) {
            final String urlByKey = instagramUrlMap.get(key);
            if (urlByKey != null && urlByKey.equals(url)) {
                newInstagramUrlMap.put(key, url);
            }
        }
    }

    private void removeInstagramItem(String url) {
        for (String key : instagramUrlMap.keySet()) {
            final String urlByKey = instagramUrlMap.get(key);
            if (urlByKey != null && urlByKey.equals(url)) {
                newInstagramUrlMap.remove(key);
            }
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image, imageMask;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img);
            imageMask = itemView.findViewById(R.id.mask);

            image.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    image.getViewTreeObserver().removeOnPreDrawListener(this);
                    image.setLayoutParams(new ConstraintLayout.LayoutParams(windowWidth / 3, windowWidth / 3));
                    return true;
                }
            });

            imageMask.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    imageMask.getViewTreeObserver().removeOnPreDrawListener(this);
                    imageMask.setLayoutParams(new ConstraintLayout.LayoutParams(windowWidth / 3, windowWidth / 3));
                    return true;
                }
            });

            image.setOnClickListener(view -> {
                final UrlBody urlBody = urlList.get(getAdapterPosition());
                if (imageMask.getVisibility() == View.GONE) {
                    imageMask.setVisibility(View.VISIBLE);
                    fillNewInstagramMap(urlBody.getUrl());
                    urlBody.setSelected(true);
                } else {
                    imageMask.setVisibility(View.GONE);
                    removeInstagramItem(urlBody.getUrl());
                    urlBody.setSelected(false);
                }
            });
        }
    }
}
