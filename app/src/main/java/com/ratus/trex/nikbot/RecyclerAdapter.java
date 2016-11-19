package com.ratus.trex.nikbot;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Objects;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private ArrayList<String> imageUrl;
    private Context mContext;

    public RecyclerAdapter(ArrayList<String> dataArgs) {imageUrl = dataArgs;}

    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        mContext = viewGroup.getContext();
        final View view = LayoutInflater.from(mContext)
                .inflate(R.layout.default_card, viewGroup, false);

        final ViewHolder holder = new ViewHolder(view);
        mContext = viewGroup.getContext();
        holder.imgFav.setVisibility(View.GONE);

        holder.cardImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = holder.cardImage.getContext();
                Intent intent = new Intent(context, ImagePreview.class);
                intent.putExtra("image.preview", Integer.toString(holder.getAdapterPosition()));
                context.startActivity(intent);
            }
        });

        holder.cardImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String img_id = MainActivity.jsonImgData.get(holder.getAdapterPosition()).get(MainActivity.TAG_ID);

                if (holder.imgSel.getVisibility() != View.VISIBLE) { holder.imgSel.setVisibility(View.VISIBLE); }
                else { holder.imgSel.setVisibility(View.GONE); }

                Context context = holder.cardImage.getContext();
                Intent intent = new Intent("onImgLongClick");
                intent.putExtra("message", img_id);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                return true;
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerAdapter.ViewHolder holder, int i) {
        //TODO: Shared preferences compat view for cards
        String url = imageUrl.get(i);
        if (!url.contains("nik.bot")) {url = "https://nik.bot.nu/t" + url;}
        String id = url.substring(url.indexOf(".nu/t") + 5, url.length() - 4);
        ImageLoader.getInstance().displayImage(url, holder.cardImage);

        if (MainActivity.favList.contains(url)) {holder.imgFav.setVisibility(View.VISIBLE);}
        else {holder.imgFav.setVisibility(View.GONE);}

        if (MainActivity.dloadList.contains(id)) {holder.imgSel.setVisibility(View.VISIBLE);}
        else {holder.imgSel.setVisibility(View.GONE);}
    }

    @Override
    public int getItemCount() {
        return imageUrl.size();
    }

    public Object getItem(int position) {
        return super.getItemId(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView cardImage;
        ImageView imgSel;
        ImageView imgFav;

        public ViewHolder(View itemView) {
            super(itemView);

            cardImage = (ImageView) itemView.findViewById(R.id.cardImgView);
            cardView = (CardView) itemView.findViewById(R.id.cv);
            imgSel = (ImageView) itemView.findViewById(R.id.imgSel);
            imgFav = (ImageView) itemView.findViewById(R.id.imgFav);
        }
    }
}
