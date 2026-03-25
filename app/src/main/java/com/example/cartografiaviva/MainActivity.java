package com.example.cartografiaviva;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import java.util.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MainActivity";
    private GoogleMap mMap;
    private TextView tvNationalityName, tvNarrativeTitle, tvNarrativeDescription;
    private List<Etnia> listaEtnias;
    private final List<Marker> marcadoresPOIs = new ArrayList<>();
    private final LatLng ubicacionUsuario = new LatLng(-1.0241, -79.4611);
    private final Set<String> etniasMostradas = new HashSet<>();

    private final ActivityResultLauncher<Intent> listaActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String etniaSeleccionada = result.getData().getStringExtra("etnia_seleccionada");
                    if (etniaSeleccionada != null && listaEtnias != null) {
                        for (Etnia e : listaEtnias) {
                            if (e.nacionalidad.equalsIgnoreCase(etniaSeleccionada)) {
                                actualizarUIConEtnia(e);
                                break;
                            }
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      //  Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        tvNationalityName = findViewById(R.id.tv_nationality_name);
       // tvNarrativeTitle = findViewById(R.id.tv_narrative_title);
        tvNarrativeDescription = findViewById(R.id.tv_narrative_description);

       // Button btnVerPruebas = findViewById(R.id.btn_ver_pruebas);
       // if (btnVerPruebas != null) {
         //   btnVerPruebas.setOnClickListener(v -> {
             //   Intent intent = new Intent(MainActivity.this, ListaPruebasActivity.class);
            //    listaActivityResultLauncher.launch(intent);
          //  });
       // }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        cargarDatosDesdeJSON();
    }

    private void cargarDatosDesdeJSON() {
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
        String url = "https://faceapp-cfc13-default-rtdb.firebaseio.com/.json";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> procesarJsonDinamico(response),
                error -> Log.e(TAG, "Error de red: " + error.getMessage())
        );
        queue.add(request);
    }

    private void procesarJsonDinamico(JSONObject response) {
        if (mMap == null || listaEtnias == null) return;
        mMap.clear();
        etniasMostradas.clear();
        marcarUbicacionUsuario();
        recorrerYBuscarEtnias(response);
    }

    private void marcarUbicacionUsuario() {
        mMap.addMarker(new MarkerOptions()
                .position(ubicacionUsuario)
                .title("Hito Histórico - Mi ubicación actual")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
    }

    private void recorrerYBuscarEtnias(Object json) {
        try {
            if (json instanceof JSONObject) {
                JSONObject obj = (JSONObject) json;
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    Object val = obj.get(keys.next());
                    if (val instanceof String) {
                        verificarSiEsEtnia((String) val);
                    } else {
                        recorrerYBuscarEtnias(val);
                    }
                }
            } else if (json instanceof JSONArray) {
                JSONArray array = (JSONArray) json;
                for (int i = 0; i < array.length(); i++) {
                    recorrerYBuscarEtnias(array.get(i));
                }
            }
        } catch (Exception e) { }
    }

    private void verificarSiEsEtnia(String texto) {
        String textoLimpio = normalizar(texto);
        for (Etnia etnia : listaEtnias) {
            if (textoLimpio.contains(normalizar(etnia.nacionalidad))) {
                if (!etniasMostradas.contains(etnia.nacionalidad)) {
                    etniasMostradas.add(etnia.nacionalidad);
                    agregarMarcadorMapa(etnia);
                }
            }
        }
    }

    private Marker agregarMarcadorMapa(Etnia etnia) {
        float color = BitmapDescriptorFactory.HUE_GREEN;
        if ("Costa".equalsIgnoreCase(etnia.region)) color = BitmapDescriptorFactory.HUE_AZURE;
        else if ("Sierra".equalsIgnoreCase(etnia.region)) color = BitmapDescriptorFactory.HUE_ORANGE;

        Marker m = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(etnia.latitud, etnia.longitud))
                .title(etnia.nacionalidad)
                .icon(BitmapDescriptorFactory.defaultMarker(color)));
        if (m != null) m.setTag(etnia);
        return m;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return texto.toLowerCase().replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ñ","n").trim();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);

        LatLng ecuador = new LatLng(-1.83, -78.18);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ecuador, 6.5f));
        consumirApi();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof Etnia) {
            actualizarUIConEtnia((Etnia) tag);
            return true;
        }
        return false;
    }

    private void actualizarUIConEtnia(Etnia etnia) {
        if (mMap == null) return;

        mMap.clear();
        marcadoresPOIs.clear();

        // 1. Marcador Mi Ubicación
        marcarUbicacionUsuario();

        // 2. Marcador Etnia Principal
        Marker mEtnia = agregarMarcadorMapa(etnia);
        if (mEtnia != null) mEtnia.showInfoWindow();

        // 3. Marcadores de Sitios Representativos (POIs)
        mostrarPuntosDeInteres(etnia);

        // --- Lógica de Encuadre de Cámara (Para ver los 3 tipos de marcadores) ---
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(ubicacionUsuario); // Incluir usuario
        builder.include(new LatLng(etnia.latitud, etnia.longitud)); // Incluir etnia

        // Incluir todos los puntos representativos encontrados
        for (Marker poiMarker : marcadoresPOIs) {
            builder.include(poiMarker.getPosition());
        }

        // Animamos la cámara para que todo sea visible con un margen (padding)
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));

        // Actualizar Textos
        float[] results = new float[1];
        Location.distanceBetween(ubicacionUsuario.latitude, ubicacionUsuario.longitude,
                etnia.latitud, etnia.longitud, results);
        float distanciaKm = results[0] / 1000;

        tvNationalityName.setText("Etnia/Pueblo: " + etnia.nacionalidad);
        tvNarrativeTitle.setText(String.format(Locale.US, "Distancia: %.2f km", distanciaKm));
        tvNarrativeDescription.setText(etnia.descripcion + "\n\nLengua: " + etnia.lengua);
    }

    private void mostrarPuntosDeInteres(Etnia etnia) {
        if (etnia.puntos_interes != null) {
            for (Etnia.PuntoInteres poi : etnia.puntos_interes) {
                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(poi.lat, poi.lng))
                        .title("Sitio Representativo: " + poi.nombre)
                        .snippet(poi.tipo)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                marcadoresPOIs.add(m);
            }
        }
    }
}