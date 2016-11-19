package com.ratus.trex.nikbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import mehdi.sakout.fancybuttons.FancyButton;
//import yuku.ambilwarna.AmbilWarnaDialog;

public class Options extends Activity {

    public static String json_query;

    AutoCompleteTextView et_tag;
    EditText et_width;
    EditText et_height;
    EditText et_ratio;
    EditText et_sort ;
    EditText et_filter;
    EditText et_date ;
    EditText et_color;
    EditText et_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        et_tag = (AutoCompleteTextView) findViewById(R.id.txt_tags);
        et_width = (EditText) findViewById(R.id.txt_width);
        et_height = (EditText) findViewById(R.id.txt_height);
        et_ratio = (EditText) findViewById(R.id.txt_ratio);
        et_sort = (EditText) findViewById(R.id.txt_sort);
        et_filter = (EditText) findViewById(R.id.txt_filter);
        et_date = (EditText) findViewById(R.id.txt_date);
        et_color = (EditText) findViewById(R.id.txt_color);
        et_name = (EditText) findViewById(R.id.txt_name);

        Button bt_width = (Button) findViewById(R.id.btn_width);
        Button bt_height = (Button) findViewById(R.id.btn_height);
        Button bt_ratio = (Button) findViewById(R.id.btn_ratio);
        Button bt_sort = (Button) findViewById(R.id.btn_sort);
        Button bt_filter = (Button) findViewById(R.id.btn_filter);
        Button bt_color = (Button) findViewById(R.id.btn_color);
        FancyButton fin_opt = (FancyButton) findViewById(R.id.bt_exit_opt);

        AssetManager assetManager = getAssets();
        InputStream input;
        String tagdata = null;

        try {
            input = assetManager.open("taglist.txt");
            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();
            tagdata = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> TAG_LIST = Arrays.asList(tagdata.split(" "));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, TAG_LIST);
        et_tag.setAdapter(adapter);

        bt_width.setOnClickListener(new View.OnClickListener() {
            final CharSequence[] items = {
                    "1280", "1366", "1440", "1600", "1680", "1920", "2560", "3656", "3840", "3996", "4096"
            };
            String title = "Select width:";
            @Override
            public void onClick(View v) {
                buildDialog(items, title, et_width);
            }
        });

        bt_height.setOnClickListener(new View.OnClickListener() {
            final CharSequence[] items = {
                    "720", "768", "960", "1024", "1050", "1080", "1220", "1600", "1714", "2048", "2160", "2664", "3112"
            };
            String title = "Select height";
            @Override
            public void onClick(View v) {
                buildDialog(items, title, et_height);
            }
        });

        bt_ratio.setOnClickListener(new View.OnClickListener() {
            final CharSequence[] items = {
                    "5:4", "4:3", "3:2", "8:5", "5:3", "16:9", "17:9"
            };
            String title = "Select ratio:";
            @Override
            public void onClick(View v) {
                buildDialog(items, title, et_ratio);
            }
        });

        bt_sort.setOnClickListener(new View.OnClickListener() {
            final CharSequence[] items = {
                    "Random", "ID Asc", "Date Desc", "Date Asc", "Hits Desc", "Size Desc", "Size Asc"
            };
            String title = "Select sort type:";
            @Override
            public void onClick(View v) {
                buildDialog(items, title, et_sort);
            }
        });

        bt_filter.setOnClickListener(new View.OnClickListener() {
            final CharSequence[] items = {
                    "Off", "SFW-Sketchy", "Sketchy", "Adult-Sketchy", "Adult"
            };
            String title = "Select filter type:";
            @Override
            public void onClick(View v) {
                buildDialog(items, title, et_filter);
            }
        });

        bt_color.setOnClickListener(new View.OnClickListener() {
            String title = "Select color:";
            @Override
            public void onClick(View v) {
                /*AmbilWarnaDialog dialog = new AmbilWarnaDialog(RightOptions.this, 0xff0000ff, new AmbilWarnaDialog.OnAmbilWarnaListener() {

                    // Executes, when user click Cancel button
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog){
                    }

                    // Executes, when user click OK button
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        String hexColor = String.format("#%06X", (0xFFFFFF & color));
                        hexColor = hexColor.replace("#","");
                        et_color.setText(hexColor);
                    }
                });
                dialog.show();*/
            }
        });

        fin_opt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences getPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String card_count = getPref.getString("card_count", "20");

                json_query = "https://nik.bot.nu/wt.fu?req=mode:json%20count:"+card_count;
                if (isEmpty(et_name)) {json_query += "%20name:" + et_name.getText().toString();}
                if (isEmpty(et_tag)) {json_query += "%20tag:" + et_tag.getText().toString();}
                if (isEmpty(et_width)) {json_query += "%20width:" + et_width.getText().toString();}
                if (isEmpty(et_height)) {json_query += "%20height:" + et_height.getText().toString();}
                if (isEmpty(et_ratio)) {json_query += "%20ratio:" + et_ratio.getText().toString();}
                if (isEmpty(et_sort)) {json_query += "%20sort:" + et_sort.getText().toString();}
                if (isEmpty(et_filter)) {json_query += "%20safe:" + et_filter.getText().toString();}
                if (isEmpty(et_date)) {
                    if (et_date.getText().toString().contains(".")) {
                        json_query += "%20date:" + et_date.getText().toString() + "-" + et_date.getText().toString();
                    } else {
                        json_query += "%20date:" + et_date.getText().toString();
                    }
                }
                if (isEmpty(et_color)) {json_query += "%20color:" + et_color.getText().toString();}

                json_query = json_query.replace(" ","");
                json_query = json_query.toLowerCase();
                Log.d("json", json_query);
                Intent intent = new Intent("fromOptions");
                intent.putExtra("json-url", json_query);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                finish();
            }

            private boolean isEmpty(EditText etText) {
                if (etText.getText().toString().trim().length() > 0) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void buildDialog(final CharSequence[] items, String title, final EditText et_field) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                et_field.setText(items[item]);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
