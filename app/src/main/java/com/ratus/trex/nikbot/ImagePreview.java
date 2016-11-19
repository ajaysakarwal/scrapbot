package com.ratus.trex.nikbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImagePreview extends Activity {

    String imgID, imgName, imgDate, imgBoard, imgHits, imgColors;
    String imgSize, imgWidth, imgHeight, fwdURL, index;
    List<String> imgTags;

    TextView tvBoard, tvDate, tvTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        Intent intent = getIntent();
        index = intent.getStringExtra("image.preview");

        imgID = MainActivity.jsonImgData.get(Integer.parseInt(index)).get(MainActivity.TAG_ID);
        imgSize = MainActivity.jsonImgData.get(Integer.parseInt(index)).get(MainActivity.TAG_SIZE);
        imgWidth = MainActivity.jsonImgData.get(Integer.parseInt(index)).get(MainActivity.TAG_WIDTH);
        imgHeight = MainActivity.jsonImgData.get(Integer.parseInt(index)).get(MainActivity.TAG_HEIGHT);
        fwdURL = "https://nik.bot.nu/st.fu?req=mode:json%20action:preview%20id:" + imgID;
        setTitle(imgID);

        TextView imgDim = (TextView) findViewById(R.id.img_dim);
        imgDim.setText("Dim:" + imgWidth + "x" + imgHeight + "  Size:" + imgSize + "KB");

        //TODO: Original or thumbnail as preview
        ImageView imgPreview = (ImageView) findViewById(R.id.img_prv);

        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);
        Integer realWidth = size.x;
        Integer realHeight = size.y;

        Integer minDim = Math.min(realHeight, realWidth);
        imgPreview.getLayoutParams().height = minDim - 30;
        imgPreview.getLayoutParams().width = minDim - 30;

        imgPreview.setOnTouchListener(new OnSwipeTouchListener(ImagePreview.this) {
            @Override
            public void onSwipeRight() {
                if (!index.equals("0")) {
                    Intent intent = new Intent(ImagePreview.this, ImagePreview.class);
                    intent.putExtra("image.preview",Integer.toString(Integer.parseInt(index) - 1));
                    ImagePreview.this.startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onSwipeLeft() {
                //TODO: Change 20 to settings value
                if (!index.equals("19")) {
                    Intent intent = new Intent(ImagePreview.this, ImagePreview.class);
                    intent.putExtra("image.preview",Integer.toString(Integer.parseInt(index) + 1));
                    ImagePreview.this.startActivity(intent);
                    finish();
                }
            }
        });

        String imgURL = MainActivity.jsonImgData.get(Integer.parseInt(index)).get(MainActivity.TAG_URL);
        Log.d("URL", imgURL);
        ImageLoader.getInstance().displayImage(imgURL, imgPreview);
        new GetImageProperties().execute();
    }

    public void addList(View view) {
        Intent intent = new Intent("onImgLongClick");
        intent.putExtra("message", imgID);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        finish();
    }

    public void dloadImage(View view) {
        String imgURL = MainActivity.jsonImgData.get(Integer.parseInt(index)).get(MainActivity.TAG_URL);
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imgURL));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        //TODO: Shared preference: wifi, private
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, imgName + imgURL.substring(imgURL.length() - 4));
        downloadManager.enqueue(request);
    }

    public void addToFav(View view) {
        String imgURL = MainActivity.jsonImgData.get(Integer.parseInt(index)).get(MainActivity.TAG_URL);
        if (MainActivity.favList.contains(imgURL)) {
            MainActivity.favList = MainActivity.favList.replace(imgURL + ",", "");
            //TODO: Icon change for fav button
        } else {
            MainActivity.favList += imgURL + ",";
        }
        finish();
    }

    public void search_this(View view) {
        AlertDialog dialog;
        final CharSequence[] items = {"Width", "Height", "Date", "Color", "Tags"};
        final ArrayList selItem = new ArrayList();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search with...");
        builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {selItem.add(items[which]);} else if (selItem.contains(items[which]))
                {selItem.remove(Integer.valueOf(which));}
            }
        })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String opInt = "";
                        for (int i = 0; i < selItem.size(); i++) {
                            opInt += queryBuilder(selItem.get(i).toString());
                        }

                        //TODO: Card count option.
                        opInt = "https://nik.bot.nu/wt.fu?req=mode:json%20count:20%20" + opInt;
                        Intent intent = new Intent("fromOptions");
                        intent.putExtra("json-url", opInt);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        finish();
                    }

                    public String queryBuilder(String chk) {
                        String output = "";
                        String tagComb = "";
                        List<String> dateSp = Arrays.asList(imgDate.substring(0, imgDate.indexOf(" ")).split("-"));
                        String newImgDate = dateSp.get(1) + "." + dateSp.get(2) + "." + dateSp.get(0);

                        for (int i = 0; i < imgTags.size(); i++) {
                            tagComb += (i == 0) ? imgTags.get(i) : "," + imgTags.get(i);
                        }

                        switch (chk) {
                            case "Width": output += "%20w:" + imgWidth; break;
                            case "Height": output += "%20h:" + imgHeight; break;
                            case "Date": output += "%20date:" + newImgDate + "-" + newImgDate; break;
                            case "Color": output += "%20color:" + imgColors.substring(0, imgColors.indexOf(",")); break;
                            case "Tags": output += "%20tag:" + tagComb; break;
                        }
                        return output;
                    }
                })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do Nothing
                    }
                });
        dialog = builder.create();
        dialog.show();
    }

    private boolean isNetAvailable() {
        try {
            ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            return conMan.getActiveNetworkInfo().isConnectedOrConnecting();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    private class GetImageProperties extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (isNetAvailable()) {
                ServiceHandler sh = new ServiceHandler();
                imgName = sh.makeServiceCall(fwdURL, ServiceHandler.GET);
                try {
                    JSONObject jsonObj = new JSONObject(imgName);
                    imgName = jsonObj.getString("name");
                    imgDate = jsonObj.getString("date");
                    imgBoard = jsonObj.getString("board");
                    imgColors = jsonObj.getString("colors");
                    imgTags = new ArrayList<String>();
                    JSONArray jsonTag = jsonObj.getJSONArray("tags");
                    for (int i = 0; i < jsonTag.length(); i++) {
                        JSONObject c = jsonTag.getJSONObject(i);
                        imgTags.add(c.getString("name"));
                    }
                } catch (JSONException e) {e.printStackTrace();}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (isNetAvailable()) {
                setTitle(imgName);
                String tagComb = "";
                imgHits = MainActivity.jsonImgData.get(Integer.parseInt(index)).get(MainActivity.TAG_HITS);
                try {for (int i = 0; i < imgTags.size(); i++) {tagComb += (i == 0) ? imgTags.get(i) : "," + imgTags.get(i);}}
                catch (Exception e) {e.printStackTrace();}

                tvBoard = (TextView) findViewById(R.id.img_board);
                tvDate = (TextView) findViewById(R.id.img_date);
                tvTag = (TextView) findViewById(R.id.img_tags);

                tvBoard.setText("ID:" + imgID + "  Board:" + imgBoard.replace(" ", ""));
                tvDate.setText("Date:" + imgDate);
                tvTag.setText((!tagComb.equals("")) ? "Hits:" + imgHits + "  Tags:" + tagComb : "Hits:" + imgHits);
            }
        }
    }
}
