package fr.pchab.androidrtc;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import models.Expert;

public class MainActivity extends AppCompatActivity {

    Button btnReadyToReceive, call;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnReadyToReceive = (Button) findViewById(R.id.ready_to_receive);
        call = (Button) findViewById(R.id.call);

        btnReadyToReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RtcActivity.class);
                startActivity(intent);
            }
        });

        final ProgressDialog progressDialog = new ProgressDialog(this);
        final String web_base = "http://" + getResources().getString(R.string.host) +
                ":" + getResources().getString(R.string.port);
        final RequestQueue queue = Volley.newRequestQueue(this);

        progressDialog.setTitle("Loading technicians...");
        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                StringRequest request = new StringRequest(web_base + "/streams.json", new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        progressDialog.hide();

                        try {
                            JSONArray array = new JSONArray(s);
                            if(array.length() ==0){
                                Toast.makeText(MainActivity.this,"Sorry, no experts are active now.", Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }

//                            else if(array.length() > 1){
//
//
//                                Toast.makeText(MainActivity.this,"Multiple technicians found. Choosing the first one.", Toast.LENGTH_SHORT)
//                                        .show();
//                            }
                            final ArrayList<Expert> experts = new ArrayList<>();

                            for (int i=0; i<array.length(); ++i){
                                JSONObject jsonObject = array.getJSONObject(i);
                                experts.add(new Expert(jsonObject.getString("id"), jsonObject.getString("name")));
                            }

                            new MaterialDialog.Builder(MainActivity.this)
                                    .title("Choose the export you want to connect to.")
                                    .items(experts)
                                    .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                                        @Override
                                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                            /**
                                             * If you use alwaysCallSingleChoiceCallback(), which is discussed below,
                                             * returning false here won't allow the newly selected radio button to actually be selected.
                                             **/

                                            if (which == -1){
                                                Toast.makeText(MainActivity.this,
                                                        "You didn't select any expert. ", Toast.LENGTH_SHORT).show();
                                                return false;
                                            }

                                            Expert expert = experts.get(which);
                                            Intent intent = new Intent(MainActivity.this, RtcActivity.class);

                                            Toast.makeText(MainActivity.this,
                                                    "Connecting to "+expert.name+" with id "+expert.id, Toast.LENGTH_SHORT).show();

                                            intent.putExtra("receiver", expert.id);
                                            startActivity(intent);


                                            Toast.makeText(MainActivity.this, ""+ which, Toast.LENGTH_SHORT).show();
                                            return true;
                                        }
                                    })
                                    .positiveText("Call")
                                    .show();

//                            JSONObject technician = array.getJSONObject(0);
//                            Toast.makeText(MainActivity.this,
//                                    "Connecting to "+technician.getString("name")+" at "+technician.getString("id"),Toast.LENGTH_SHORT).show();
//
//
//                            Intent intent = new Intent(MainActivity.this, RtcActivity.class);
//                            intent.putExtra("receiver", technician.getString("id"));
//                            startActivity(intent);


                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,"Could not parse streams.json", Toast.LENGTH_SHORT).show();

                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        progressDialog.hide();

                        Toast.makeText(MainActivity.this,
                                "Could not connect to server. Please check connection.",Toast.LENGTH_LONG).show();
                    }
                });

                queue.add(request);


            }
        });

    }
}
