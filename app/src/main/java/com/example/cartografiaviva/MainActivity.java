package com.example.cartografiaviva;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MainActivity";
    private GoogleMap mMap;
    private TextView tvNationalityName, tvNarrativeTitle, tvNarrativeDescription;
    private List<Etnia> listaEtnias;
    private final List<Marker> marcadoresPOIs = new ArrayList<>();
    private final LatLng ubicacionUsuario = new LatLng(-1.0241, -79.4611);

    private final ActivityResultLauncher<Intent> listaActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String etniaSeleccionada = result.getData().getStringExtra("etnia_seleccionada");
                    if (etniaSeleccionada != null && listaEtnias != null) {
                        for (Etnia e : listaEtnias) {
                            if (e.nacionalidad.equalsIgnoreCase(etniaSeleccionada)) {
                                if (mMap != null) {
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(e.latitud, e.longitud), 12f));
                                }
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvNationalityName = findViewById(R.id.tv_nationality_name);
        tvNarrativeTitle = findViewById(R.id.tv_narrative_title);
        tvNarrativeDescription = findViewById(R.id.tv_narrative_description);

        Button btnVerPruebas = findViewById(R.id.btn_ver_pruebas);
        btnVerPruebas.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ListaPruebasActivity.class);
            listaActivityResultLauncher.launch(intent);
        });

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

    private void obtenerDatosDeAPI() {
        new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL("https://faceapp-cfc13-default-rtdb.firebaseio.com/.json");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String result;
                try (Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A")) {
                    result = s.hasNext() ? s.next() : "";
                }

                if (result.isEmpty() || result.equals("null")) return;

                Gson gson = new Gson();
                Map<String, PruebaAPI> pruebasMap;
                try {
                    Type type = new TypeToken<Map<String, PruebaAPI>>(){}.getType();
                    pruebasMap = gson.fromJson(result, type);
                } catch (Exception e) {
                    Type listType = new TypeToken<ArrayList<PruebaAPI>>(){}.getType();
                    ArrayList<PruebaAPI> lista = gson.fromJson(result, listType);
                    pruebasMap = new HashMap<>();
                    if (lista != null) {
                        for (int i = 0; i < lista.size(); i++) {
                            if (lista.get(i) != null) pruebasMap.put(String.valueOf(i), lista.get(i));
                        }
                    }
                }
                
                final Map<String, PruebaAPI> finalPruebas = pruebasMap;
                runOnUiThread(() -> procesarPruebasDeAPI(finalPruebas));

            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo datos de API", e);
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
        }).start();
    }

    private void procesarPruebasDeAPI(Map<String, PruebaAPI> pruebas) {
        if (pruebas == null || listaEtnias == null || mMap == null) return;
        for (PruebaAPI prueba : pruebas.values()) {
            if (prueba == null) continue;
            String perfilCompleto = prueba.getPerfilLimpio();
            for (Etnia etnia : listaEtnias) {
                if (perfilCompleto.toLowerCase().contains(etnia.nacionalidad.toLowerCase())) {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(etnia.latitud + 0.005, etnia.longitud + 0.005))
                            .title("Prueba: " + etnia.nacionalidad)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                    if (m != null) m.setTag(prueba);
                }
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        LatLng ecuador = new LatLng(-1.8312, -78.1834);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ecuador, 6.5f));

        mMap.addMarker(new MarkerOptions()
                .position(ubicacionUsuario)
                .title("Tu ubicación actual")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        dibujarEtnias();
        obtenerDatosDeAPI();
    }

    private void dibujarEtnias() {
        if (listaEtnias == null) return;
        for (Etnia etnia : listaEtnias) {
            float color;
            switch (etnia.region != null ? etnia.region : "") {
                case "Costa": color = BitmapDescriptorFactory.HUE_AZURE; break;
                case "Sierra": color = BitmapDescriptorFactory.HUE_ORANGE; break;
                case "Amazonía": color = BitmapDescriptorFactory.HUE_GREEN; break;
                default: color = BitmapDescriptorFactory.HUE_RED;
            }
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(etnia.latitud, etnia.longitud))
                    .title(etnia.nacionalidad)
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
            if (m != null) m.setTag(etnia);
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof Etnia) {
            actualizarUIConEtnia((Etnia) tag);
        } else if (tag instanceof PruebaAPI) {
            actualizarUIConPrueba((PruebaAPI) tag);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        return false;
    }

    private void actualizarUIConEtnia(Etnia etnia) {
        float[] results = new float[1];
        Location.distanceBetween(ubicacionUsuario.latitude, ubicacionUsuario.longitude,
                etnia.latitud, etnia.longitud, results);
        float distanciaKm = results[0] / 1000;

        tvNationalityName.setText(getString(R.string.ethnicity_prefix, etnia.nacionalidad));
        tvNarrativeTitle.setText(getString(R.string.distance_label, distanciaKm));
        tvNarrativeDescription.setText(etnia.descripcion + "\n\nLengua: " + etnia.lengua);

        limpiarPOIsAnteriores();
        mostrarPuntosDeInteres(etnia);
    }

    private void actualizarUIConPrueba(PruebaAPI prueba) {
        tvNationalityName.setText("Resultado Experimental");
        tvNarrativeTitle.setText(String.format("Confianza: %.2f%%", prueba.getConfianzaDouble() * 100));

        StringBuilder sb = new StringBuilder();
        sb.append("Perfil: ").append(prueba.getPerfilLimpio()).append("\n\n");
        sb.append(prueba.getClasificacionExperimental()).append("\n\nRasgos:\n");
        if (prueba.rasgos != null) {
            for (PruebaAPI.Rasgo r : prueba.rasgos) {
                if (r != null && r.nombre != null) sb.append("- ").append(r.nombre).append("\n");
            }
        }
        tvNarrativeDescription.setText(sb.toString());
    }

    private void mostrarPuntosDeInteres(Etnia etnia) {
        if (etnia.puntos_interes != null) {
            for (Etnia.PuntoInteres poi : etnia.puntos_interes) {
                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(poi.lat, poi.lng))
                        .title(poi.nombre)
                        .snippet("Tipo: " + poi.tipo)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                marcadoresPOIs.add(m);
            }
        }
    }

    private void limpiarPOIsAnteriores() {
        for (Marker m : marcadoresPOIs) m.remove();
        marcadoresPOIs.clear();
    }
}
