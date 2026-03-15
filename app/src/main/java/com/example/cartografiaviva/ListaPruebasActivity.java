package com.example.cartografiaviva;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ListaPruebasActivity extends AppCompatActivity {

    private static final String TAG = "ListaPruebasActivity";
    private ListView lvPruebas;
    private final ArrayList<String> infoMostrar = new ArrayList<>();
    private final ArrayList<String> nacionalidadesParaMapa = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_pruebas);

        lvPruebas = findViewById(R.id.lv_pruebas);

        cargarTodasLasPruebas();

        lvPruebas.setOnItemClickListener((parent, view, position, id) -> {
            if (position < nacionalidadesParaMapa.size()) {
                String seleccion = nacionalidadesParaMapa.get(position);
                Intent intent = new Intent();
                intent.putExtra("etnia_seleccionada", seleccion);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void cargarTodasLasPruebas() {
        new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL("https://faceapp-cfc13-default-rtdb.firebaseio.com/.json");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String json;
                try (Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A")) {
                    json = s.hasNext() ? s.next() : "";
                }

                if (json.isEmpty() || json.equals("null")) {
                    runOnUiThread(() -> Toast.makeText(this, "No hay datos disponibles", Toast.LENGTH_SHORT).show());
                    return;
                }

                Gson gson = new Gson();
                Map<String, PruebaAPI> todasLasPruebas;
                try {
                    Type type = new TypeToken<Map<String, PruebaAPI>>() {}.getType();
                    todasLasPruebas = gson.fromJson(json, type);
                } catch (Exception e) {
                    Type listType = new TypeToken<ArrayList<PruebaAPI>>() {}.getType();
                    ArrayList<PruebaAPI> lista = gson.fromJson(json, listType);
                    todasLasPruebas = new HashMap<>();
                    if (lista != null) {
                        for (int i = 0; i < lista.size(); i++) {
                            if (lista.get(i) != null) todasLasPruebas.put(String.valueOf(i), lista.get(i));
                        }
                    }
                }

                if (todasLasPruebas != null) {
                    for (PruebaAPI prueba : todasLasPruebas.values()) {
                        String perfil = prueba.getPerfilLimpio();
                        String clasificacion = prueba.getClasificacionExperimental();
                        infoMostrar.add("PERFIL: " + perfil + "\n" + clasificacion);
                        nacionalidadesParaMapa.add(extraerNombreEtnia(clasificacion, perfil));
                    }

                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, infoMostrar);
                        lvPruebas.setAdapter(adapter);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error de conexión", e);
                runOnUiThread(() -> Toast.makeText(this, "Error de conexión con la base de datos", Toast.LENGTH_LONG).show());
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
        }).start();
    }

    private String extraerNombreEtnia(String clasificacion, String perfil) {
        if (clasificacion.contains(":")) return clasificacion.split(":")[1].trim();
        if (perfil.contains(":")) {
            String[] partes = perfil.split(":");
            return partes[partes.length - 1].trim();
        }
        return "Desconocido";
    }
}
