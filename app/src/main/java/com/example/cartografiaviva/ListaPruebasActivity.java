package com.example.cartografiaviva;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListaPruebasActivity extends AppCompatActivity {

    private static final String TAG = "ListaPruebasActivity";
    private final List<ItemHistorial> listaHistorial = new ArrayList<>();
    private HistorialAdapter adapter;
    private List<Etnia> listaEtnias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_pruebas);

        ListView lvPruebas = findViewById(R.id.lv_pruebas);
        adapter = new HistorialAdapter();
        lvPruebas.setAdapter(adapter);

        cargarEtniasDesdeAssets();
        consumirApi();

        lvPruebas.setOnItemClickListener((parent, view, position, id) -> {
            ItemHistorial seleccion = listaHistorial.get(position);
            Intent intent = new Intent();
            intent.putExtra("etnia_seleccionada", seleccion.nombreEtnia);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    private void cargarEtniasDesdeAssets() {
        try (InputStream is = getAssets().open("etnias.json");
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Etnia>>(){}.getType();
            listaEtnias = gson.fromJson(reader, listType);
        } catch (IOException ex) {
            Log.e(TAG, "Error cargando etnias.json", ex);
        }
    }

    private void consumirApi() {
        String url = "https://faceapp-cfc13-default-rtdb.firebaseio.com/.json?t=" + System.currentTimeMillis();
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    listaHistorial.clear();
                    // Escaneamos el JSON buscando registros únicos
                    escanearPorRegistros(response);

                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Se cargaron " + listaHistorial.size() + " registros", Toast.LENGTH_SHORT).show();
                },
                error -> Log.e(TAG, "Error de red")
        );
        request.setShouldCache(false);
        queue.add(request);
    }

    private void escanearPorRegistros(Object nodo) {
        if (nodo == null) return;
        try {
            if (nodo instanceof JSONObject) {
                JSONObject obj = (JSONObject) nodo;

                // Intentamos encontrar una etnia dentro de este objeto (sin entrar a sub-objetos todavía)
                Etnia etniaEncontrada = null;
                String mejorDesc = "";

                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = obj.get(key);

                    if (!(val instanceof JSONObject) && !(val instanceof JSONArray)) {
                        String valStr = String.valueOf(val);

                        // FILTRO: Saltamos enlaces de imágenes y tokens
                        if (valStr.contains("http") || valStr.contains("token=") || valStr.contains(".jpg")) continue;

                        for (Etnia e : listaEtnias) {
                            if (normalizar(valStr).contains(normalizar(e.nacionalidad))) {
                                etniaEncontrada = e;
                                // Si el campo es 'perfil', lo guardamos como descripción principal
                                if (key.equalsIgnoreCase("perfil") || mejorDesc.isEmpty()) {
                                    mejorDesc = valStr;
                                }
                            }
                        }
                    }
                }

                if (etniaEncontrada != null) {
                    // Solo agregamos UNA VEZ por este objeto/registro
                    String lengua = (etniaEncontrada.lengua != null) ? etniaEncontrada.lengua : "N/D";
                    listaHistorial.add(0, new ItemHistorial(etniaEncontrada.nacionalidad, mejorDesc + "\nLengua: " + lengua));
                } else {
                    // Si este objeto no era una prueba, buscamos en sus hijos
                    keys = obj.keys();
                    while (keys.hasNext()) {
                        escanearPorRegistros(obj.get(keys.next()));
                    }
                }
            } else if (nodo instanceof JSONArray) {
                JSONArray array = (JSONArray) nodo;
                for (int i = 0; i < array.length(); i++) {
                    escanearPorRegistros(array.get(i));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    private String normalizar(String t) {
        if (t == null) return "";
        return t.toLowerCase().replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ñ","n").trim();
    }

    private static class ItemHistorial {
        String nombreEtnia;
        String descripcion;
        ItemHistorial(String n, String d) { this.nombreEtnia = n; this.descripcion = d; }
    }

    private class HistorialAdapter extends BaseAdapter {
        @Override
        public int getCount() { return listaHistorial.size(); }
        @Override
        public Object getItem(int position) { return listaHistorial.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ListaPruebasActivity.this)
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            ItemHistorial item = listaHistorial.get(position);
            TextView t1 = convertView.findViewById(android.R.id.text1);
            TextView t2 = convertView.findViewById(android.R.id.text2);

            t1.setText("Etnia: " + item.nombreEtnia);
            t1.setTypeface(null, Typeface.BOLD);
            t1.setTextColor(ContextCompat.getColor(ListaPruebasActivity.this, android.R.color.black));

            t2.setText(item.descripcion);
            t2.setLines(4);
            return convertView;
        }
    }
}