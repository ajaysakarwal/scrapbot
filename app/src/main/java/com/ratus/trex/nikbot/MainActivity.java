package com.ratus.trex.nikbot;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;

import mehdi.sakout.fancybuttons.FancyButton;

public class MainActivity extends ActionBarActivity implements NavDrawerFrag.OnFragmentInteractionListener {

    private ProgressDialog pDialog;
    public static String url = "";
    public static String dloadList = "";
    public static String favList = "";
    public static Integer pageNo = 0;
    public static ArrayList<HashMap<String, String>> jsonImgData;

    public static final String TAG_DATA = "data";
    public static final String TAG_URL = "path";
    public static final String TAG_ID = "id";
    public static final String TAG_SIZE = "size";
    public static final String TAG_WIDTH = "w";
    public static final String TAG_HEIGHT = "h";
    public static final String TAG_IS_NSFW = "nsfw";
    public static final String TAG_DEL = "del";
    public static final String TAG_OPT = "opt";
    public static final String TAG_HITS = "hits";

    ArrayList<String> imageURL;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_closed) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                syncState();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                syncState();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                //if (slideOffset < 0.6) {toolbar.setAlpha(1 - slideOffset);}
            }
        };

        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        actionBarDrawerToggle.syncState();
        url = "https://nik.bot.nu/wt.fu?req=mode:json%20count:20%20tag:cat";

        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .diskCacheExtraOptions(480, 320, null)
                .diskCacheSize(50 * 1024 * 1024)
                .defaultDisplayImageOptions(defaultOptions)
                .build();
        ImageLoader.getInstance().init(config);

        //TODO: Shared preferences
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        favList = sharedPref.getString("flist", "");

        LocalBroadcastManager.getInstance(this).registerReceiver(mMsgRcv, new IntentFilter("onImgLongClick"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMsgRcv2, new IntentFilter("fromOptions"));

        SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new GetImages().execute();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.rec_view);
        recyclerView.setHasFixedSize(true);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutManager = new GridLayoutManager(MainActivity.this, 3);
        } else {
            layoutManager = new GridLayoutManager(MainActivity.this, 2);
        }
        recyclerView.setLayoutManager(layoutManager);
        new GetImages().execute();
    }

    private BroadcastReceiver mMsgRcv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Button btLoad = (Button) findViewById(R.id.btn_dwld);

            if (dloadList.contains(message)) {
                dloadList = dloadList.replace(message + ",", "");
                if (dloadList.trim().length() == 0) {
                    btLoad.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Everything removed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                dloadList += message + ",";
                btLoad.setVisibility(View.VISIBLE);
            }

            if (dloadList.trim().length() != 0) {
                int counter = dloadList.split(",").length;
                btLoad.setText(Integer.toString(counter));
            }
        }
    };

    private BroadcastReceiver mMsgRcv2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            url = intent.getStringExtra("json-url");
            new GetImages().execute();
        }
    };

    public void gotoNextPage(View view) {
        pageNo++;
        url += (url.contains("page:")) ? url.substring(0, url.indexOf("page:")) + "page:" +
                Integer.toString(pageNo) : "%20page:1";
        new GetImages().execute();
    }

    public void gotoPrevPage(View view) {
        if (pageNo != 0) {
            pageNo--;
            url = (url.contains("page:")) ? url.substring(0, url.indexOf("page:")) + "page:" + pageNo.toString() : url;
        } else {
            Toast.makeText(MainActivity.this, "No more images to show.", Toast.LENGTH_SHORT).show();
        }
    }

    public void refreshNet(View view) {
        new GetImages().execute();
    }

    public void downList(View view) {
        downAction();
    }

    public void favSet(View view) {
        final CharSequence[] items = {"Clear all", "Add all to downloads"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Options");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    favList = "";
                    new GetImages().execute();
                } else {
                    String tmpID = favList.replace("https://nik.bot.nu/t", "");
                    tmpID = tmpID.replace(".jpg", "");
                    tmpID = tmpID.replace(".png", "");
                    tmpID = tmpID.substring(0, tmpID.length() - 1);

                    List<String> tmpIDList = Arrays.asList(tmpID.split(","));
                    for (int i = 0; i < tmpIDList.size(); i++) {
                        if (!dloadList.contains(tmpIDList.get(i))) {
                            dloadList += tmpIDList.get(i) + ",";
                        }
                    }

                    Button btLoad = (Button) findViewById(R.id.btn_dwld);
                    btLoad.setVisibility(View.VISIBLE);
                    String count = Integer.toString(dloadList.trim().split(",").length);
                    btLoad.setText(count);
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

    private class GetImages extends AsyncTask<Void, Void, Void> {
        String jsonStr;
        TextView txtNoNet = (TextView) findViewById(R.id.txtNet);
        FancyButton btFav = (FancyButton) findViewById(R.id.btn_fav);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            txtNoNet.setVisibility(View.GONE);
            btFav.setVisibility(View.GONE);
            if (isNetAvailable()) {
                if (!(MainActivity.this).isFinishing()) {
                    pDialog = new ProgressDialog(MainActivity.this);
                    pDialog.setMessage("Loading, please wait...");
                    pDialog.setCancelable(true);
                    pDialog.show();
                }
            } else {
                txtNoNet.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (isNetAvailable()) {
                ServiceHandler sh = new ServiceHandler();
                jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);
                if (jsonStr.contains("page")) {
                    try {
                        pageNo = Integer.parseInt(jsonStr.substring(8, jsonStr.indexOf(",\"data")));
                        JSONObject jsonObject = new JSONObject(jsonStr);
                        JSONArray jsonArray = jsonObject.getJSONArray(TAG_DATA);
                        imageURL = new ArrayList<String>();
                        jsonImgData = new ArrayList<HashMap<String, String>>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject c = jsonArray.getJSONObject(i);

                            String img_url = "https://nik.bot.nu/t" + c.getString(TAG_URL);
                            String img_id = c.getString(TAG_ID);
                            String img_size = c.getString(TAG_SIZE);
                            String img_width = c.getString(TAG_WIDTH);
                            String img_height = c.getString(TAG_HEIGHT);
                            String img_isNSFW = c.getString(TAG_IS_NSFW);
                            String img_del = c.getString(TAG_DEL);
                            String img_opt = c.getString(TAG_OPT);
                            String img_hits = c.getString(TAG_HITS);

                            HashMap<String, String> imgData = new HashMap<String, String>();

                            imgData.put(TAG_ID, img_id);
                            imgData.put(TAG_URL, img_url);
                            imgData.put(TAG_SIZE, img_size);
                            imgData.put(TAG_WIDTH, img_width);
                            imgData.put(TAG_HEIGHT, img_height);
                            imgData.put(TAG_IS_NSFW, img_isNSFW);
                            imgData.put(TAG_DEL, img_del);
                            imgData.put(TAG_OPT, img_opt);
                            imgData.put(TAG_HITS, img_hits);

                            jsonImgData.add(imgData);
                            imageURL.add(img_url);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("ServiceHandler", "Couldn't get any data from the url.");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            FancyButton btNext = (FancyButton) findViewById(R.id.bt_nxt_pg);
            FancyButton btBack = (FancyButton) findViewById(R.id.bt_prv_pg);

            if (isNetAvailable()) {
                if (pDialog.isShowing()) {
                    pDialog.dismiss();
                }
                if (jsonStr.contains("page")) {
                    adapter = new RecyclerAdapter(imageURL);
                    recyclerView.setAdapter(adapter);

                    btBack.setVisibility(View.VISIBLE);
                    btNext.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(MainActivity.this, "No more images to show.", Toast.LENGTH_SHORT).show();
                }
            } else {
                txtNoNet.setVisibility(View.VISIBLE);
                btBack.setVisibility(View.GONE);
                btNext.setVisibility(View.GONE);
            }
            SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("dl-list", dloadList);
        savedInstanceState.putString("fav-list", favList);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        dloadList = (String) savedInstanceState.get("dl-list");
        favList = (String) savedInstanceState.get("fav-list");
    }

    @Override
    protected void onDestroy() {
        Context context = getApplicationContext();
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("flist", favList);
        editor.apply();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            new GetImages().execute();
        }
        if (id == R.id.action_favorite) {
            showFav();
            return true;
        }
        if (id == R.id.action_options) {
            Intent intent = new Intent(getApplicationContext(), Options.class);
            startActivity(intent);
        }
        if (id == R.id.action_download) {
            new AlertDialog.Builder(this)
                    .setTitle("Downloads")
                    .setMessage(dloadList)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Do Nothing
                        }
                    }).setIcon(R.drawable.ic_action_download)
                    .show();
        }

        //TODO: All menu item click handling

        return super.onOptionsItemSelected(item);
    }

    public void showFav() {
        Log.d("Fav", favList);
        FancyButton btNext = (FancyButton) findViewById(R.id.bt_nxt_pg);
        FancyButton btBack = (FancyButton) findViewById(R.id.bt_prv_pg);
        FancyButton btFav = (FancyButton) findViewById(R.id.btn_fav);
        TextView noNet = (TextView) findViewById(R.id.txtNet);

        if (isNetAvailable()) {
            String tempFav = favList.substring(0, favList.length() - 1);
            imageURL = new ArrayList<String>(Arrays.asList(tempFav.split(",")));
            adapter = new RecyclerAdapter(imageURL);
            recyclerView.setAdapter(adapter);

            btFav.setVisibility(View.VISIBLE);
            btNext.setVisibility(View.VISIBLE);
            btBack.setVisibility(View.VISIBLE);
            noNet.setVisibility(View.GONE);
        } else {
            btNext.setVisibility(View.GONE);
            btBack.setVisibility(View.GONE);
            noNet.setVisibility(View.VISIBLE);
        }
    }

    //TODO: Show board list

    private void downAction() {
        Integer counter = dloadList.split(",").length;
        final Button btLoad = (Button) findViewById(R.id.btn_dwld);
        new AlertDialog.Builder(this)
                .setTitle("Download Images(" + counter.toString() + ")")
                .setMessage(dloadList)
                .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!dloadList.isEmpty()) {
                            String dlURL = "https://nik.bot.nu/zip.fu?id=" + dloadList;
                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

                            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(dlURL));
                            request.setMimeType("application/zip");
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, timeStamp + ".zip");
                            downloadManager.enqueue(request);

                            dloadList = "";
                            btLoad.setText("");
                            btLoad.setVisibility(View.GONE);
                            new GetImages().execute();
                        }
                    }
                })
                .setNegativeButton("Clear List", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dloadList = "";
                        btLoad.setText("");
                        btLoad.setVisibility(View.GONE);
                        new GetImages().execute();
                    }
                })
                .setIcon(android.R.drawable.stat_sys_download)
                .show();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //Do Nothing
    }
}
