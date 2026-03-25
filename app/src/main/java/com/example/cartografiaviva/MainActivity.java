package com.example.cartografiaviva;

import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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
    private TextView tvNationalityName, tvNarrativeDescription, tvLocation, tvPopulation, tvCommunityLabel, tvDistance;
    private List<Etnia> listaEtnias;
    private final List<Marker> marcadoresPOIs = new ArrayList<>();
    private Polyline lineaRuta;
    private final LatLng ubicacionUsuario = new LatLng(-1.0241, -79.4611); // Quevedo
    private final Set<String> etniasMostradas = new HashSet<>();
    
    private MediaPlayer mediaPlayer;
    private ImageButton btnPlayMusic;
    private Etnia etniaSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvNationalityName = findViewById(R.id.tv_nationality_name);
        tvNarrativeDescription = findViewById(R.id.tv_narrative_description);
        tvLocation = findViewById(R.id.tv_location);
        tvPopulation = findViewById(R.id.tv_population);
        tvCommunityLabel = findViewById(R.id.tv_community_label);
        tvDistance = findViewById(R.id.tv_distance);
        btnPlayMusic = findViewById(R.id.btn_play_music);

        if (btnPlayMusic != null) {
            btnPlayMusic.setOnClickListener(v -> toggleMusica());
        }

        View btnDado = findViewById(R.id.fab_random_game);
        if (btnDado != null) {
            btnDado.setOnClickListener(v -> iniciarMiniJuego());
        }

        findViewById(R.id.fab_my_location).setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 12f));
            }
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

    private void iniciarMiniJuego() {
        if (listaEtnias == null || listaEtnias.isEmpty()) return;

        final Random random = new Random();
        final Handler handler = new Handler();
        final int duracionGiro = 1500;
        final int intervalo = 100;
        
        Toast.makeText(this, "🎲 ¡Lanzando el dado! 🎲", Toast.LENGTH_SHORT).show();

        Runnable runnable = new Runnable() {
            int tiempoTranscurrido = 0;
            @Override
            public void run() {
                if (tiempoTranscurrido < duracionGiro) {
                    Etnia randomEtnia = listaEtnias.get(random.nextInt(listaEtnias.size()));
                    tvCommunityLabel.setText("BUSCANDO... " + randomEtnia.nacionalidad.toUpperCase());
                    tiempoTranscurrido += intervalo;
                    handler.postDelayed(this, intervalo);
                } else {
                    Etnia etniaFinal = listaEtnias.get(random.nextInt(listaEtnias.size()));
                    actualizarUIConEtnia(etniaFinal);
                }
            }
        };
        handler.post(runnable);
    }

    private void toggleMusica() {
        if (etniaSeleccionada == null || etniaSeleccionada.musica_recurso == null) {
            Toast.makeText(this, "Selecciona una etnia con el dado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayMusic.setImageResource(android.R.drawable.ic_media_play);
        } else if (mediaPlayer != null) {
            mediaPlayer.start();
            btnPlayMusic.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            reproducirMusica(etniaSeleccionada.musica_recurso);
        }
    }

    private void reproducirMusica(String nombreRecurso) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            int resId = getResources().getIdentifier(nombreRecurso, "raw", getPackageName());

            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId);
                if (mediaPlayer != null) {
                    mediaPlayer.setOnCompletionListener(mp -> btnPlayMusic.setImageResource(android.R.drawable.ic_media_play));
                    mediaPlayer.start();
                    btnPlayMusic.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    Toast.makeText(this, "Error: El archivo '" + nombreRecurso + "' no es un MP3 válido o está dañado.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Audio no encontrado: " + nombreRecurso + ".mp3", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Fallo en MediaPlayer", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // ... Resto de métodos de la API y Mapa ...
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
                .title("Mi ubicación actual")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
    }

    private void recorrerYBuscarEtnias(Object json) {
        try {
            if (json instanceof JSONObject) {
                JSONObject obj = (JSONObject) json;
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    Object val = obj.get(keys.next());
                    if (val instanceof String) verificarSiEsEtnia((String) val);
                    else recorrerYBuscarEtnias(val);
                }
            } else if (json instanceof JSONArray) {
                JSONArray array = (JSONArray) json;
                for (int i = 0; i < array.length(); i++) recorrerYBuscarEtnias(array.get(i));
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
        this.etniaSeleccionada = etnia;

        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception e) {}
            mediaPlayer = null;
            btnPlayMusic.setImageResource(android.R.drawable.ic_media_play);
        }

        mMap.clear();
        marcadoresPOIs.clear();
        marcarUbicacionUsuario();
        LatLng posEtnia = new LatLng(etnia.latitud, etnia.longitud);
        agregarMarcadorMapa(etnia);
        
        if (lineaRuta != null) lineaRuta.remove();
        lineaRuta = mMap.addPolyline(new PolylineOptions()
                .add(ubicacionUsuario, posEtnia)
                .width(8).color(0xFFD4462B).geodesic(true));

        mostrarPuntosDeInteres(etnia);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(ubicacionUsuario);
        builder.include(posEtnia);
        for (Marker poiMarker : marcadoresPOIs) builder.include(poiMarker.getPosition());
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 250));

        float[] results = new float[1];
        Location.distanceBetween(ubicacionUsuario.latitude, ubicacionUsuario.longitude, etnia.latitud, etnia.longitud, results);
        
        tvCommunityLabel.setText("¡DESCUBIERTO!: " + etnia.nacionalidad.toUpperCase());
        tvNationalityName.setText("Nacionalidad: " + etnia.nacionalidad);
        tvLocation.setText("Región: " + etnia.region);
        tvPopulation.setText("Población Est.: " + (etnia.poblacion != null ? etnia.poblacion : "No disponible"));
        tvNarrativeDescription.setText(etnia.descripcion + "\n\nLengua: " + etnia.lengua);
        tvDistance.setText(String.format(Locale.US, "Distancia: %.2f km", results[0] / 1000));
    }

    private void mostrarPuntosDeInteres(Etnia etnia) {
        if (etnia.puntos_interes != null) {
            for (Etnia.PuntoInteres poi : etnia.puntos_interes) {
                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(poi.lat, poi.lng))
                        .title(poi.nombre).snippet(poi.tipo)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                marcadoresPOIs.add(m);
            }
        }
    }
}