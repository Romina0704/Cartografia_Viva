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
    private ImageView ivCommunityPhoto;
    private List<Etnia> listaEtnias;
    private final List<Marker> marcadoresPOIs = new ArrayList<>();
    private Polyline lineaRuta;
    private final LatLng ubicacionUsuario = new LatLng(-1.0241, -79.4611); // Quevedo
    private final Set<String> etniasMostradas = new HashSet<>();
    
    private MediaPlayer mediaPlayer;
    private ImageButton btnPlayMusic;
    private Etnia etniaSeleccionada;
    private androidx.viewpager2.widget.ViewPager2 vpCommunityPhotos;
    private TextView tvPhotoCounter;
    private ImageButton btnPhotoPrev, btnPhotoNext;
    private int mapTypeIndex = 0;
    private final int[] mapTypes = {
            GoogleMap.MAP_TYPE_NORMAL,
            GoogleMap.MAP_TYPE_SATELLITE,
            GoogleMap.MAP_TYPE_HYBRID
    };
    private final String[] mapTypeNames = {"Normal", "Satélite", "Híbrido"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vpCommunityPhotos = findViewById(R.id.vp_community_photos);
        tvPhotoCounter    = findViewById(R.id.tv_photo_counter);
        btnPhotoPrev      = findViewById(R.id.btn_photo_prev);
        btnPhotoNext      = findViewById(R.id.btn_photo_next);
        tvNationalityName = findViewById(R.id.tv_nationality_name);
        tvNarrativeDescription = findViewById(R.id.tv_narrative_description);
        tvLocation = findViewById(R.id.tv_location);
        tvPopulation = findViewById(R.id.tv_population);
        tvCommunityLabel = findViewById(R.id.tv_community_label);
        tvDistance = findViewById(R.id.tv_distance);
       //ivCommunityPhoto = findViewById(R.id.iv_community_photo);
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
// En onCreate
        findViewById(R.id.fab_map_type).setOnClickListener(v -> cambiarTipoMapa());        cargarDatosDesdeJSON();
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
    private void cambiarTipoMapa() {
        if (mMap == null) return;
        mapTypeIndex = (mapTypeIndex + 1) % mapTypes.length;
        mMap.setMapType(mapTypes[mapTypeIndex]);
        Toast.makeText(this, "Mapa: " + mapTypeNames[mapTypeIndex], Toast.LENGTH_SHORT).show();
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
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mapTypeIndex = 1;
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
        cargarFotosEtnia(etnia.nacionalidad);
    }
    // ══════════════════════════════════════════════════════
// REEMPLAZA tu método cargarFotosEtnia en MainActivity
// ══════════════════════════════════════════════════════

    private void cargarFotosEtnia(String nacionalidad) {

        Map<String, List<Integer>> fotosMap = new HashMap<>();

        // ── COSTA ──
        fotosMap.put("Awá", Arrays.asList(
                R.drawable.awa_01, R.drawable.awa_02, R.drawable.awa_03,
                R.drawable.awa_04, R.drawable.awa_05));

        fotosMap.put("Chachi", Arrays.asList(
                R.drawable.chachi_01, R.drawable.chachi_02, R.drawable.chachi_03,
                R.drawable.chachi_04, R.drawable.chachi_05));

        fotosMap.put("Épera", Arrays.asList(
                R.drawable.epera_01, R.drawable.epera_02, R.drawable.epera_03,
                R.drawable.epera_04, R.drawable.epera_05));

        fotosMap.put("Tsáchila", Arrays.asList(
                R.drawable.tsachila_01, R.drawable.tsachila_02, R.drawable.tsachila_03,
                R.drawable.tsachila_04, R.drawable.tsachila_05));

        // ── SIERRA ──

        fotosMap.put("Otavalo", Arrays.asList(
                R.drawable.otavalo_01, R.drawable.otavalo_02, R.drawable.otavalo_03,
                R.drawable.otavalo_04, R.drawable.otavalo_05));

        fotosMap.put("Kayambi", Arrays.asList(
                R.drawable.kayambi_01, R.drawable.kayambi_02, R.drawable.kayambi_03,
                R.drawable.kayambi_04, R.drawable.kayambi_05));

        fotosMap.put("Kitu Kara", Arrays.asList(
                R.drawable.kitu_kara_01, R.drawable.kitu_kara_02, R.drawable.kitu_kara_03,
                R.drawable.kitu_kara_04, R.drawable.kitu_kara_05));

        fotosMap.put("Panzaleo", Arrays.asList(
                R.drawable.panzaleo_01, R.drawable.panzaleo_02, R.drawable.panzaleo_03,
                R.drawable.panzaleo_04, R.drawable.panzaleo_05));

        fotosMap.put("Chibuleo", Arrays.asList(
                R.drawable.chibuleo_01, R.drawable.chibuleo_02, R.drawable.chibuleo_03,
                R.drawable.chibuleo_04, R.drawable.chibuleo_05));

        fotosMap.put("Salasaka", Arrays.asList(
                R.drawable.salaka_01, R.drawable.salaka_02, R.drawable.salaka_03,
                R.drawable.salaka_04, R.drawable.salaka_05));

        fotosMap.put("Waranka", Arrays.asList(
                R.drawable.waranka_01, R.drawable.waranka_02, R.drawable.waranka_03,
                R.drawable.waranka_04));

        fotosMap.put("Puruhá", Arrays.asList(
                R.drawable.puruha_01, R.drawable.puruha_02, R.drawable.puruha_03,
                R.drawable.puruha_04, R.drawable.puruha_05));

        fotosMap.put("Kañari", Arrays.asList(
                R.drawable.kanari_01, R.drawable.kanari_02, R.drawable.kanari_03,
                R.drawable.kanari_04, R.drawable.kanari_05));

        fotosMap.put("Saraguro", Arrays.asList(
                R.drawable.saraguro_01, R.drawable.saraguro_02, R.drawable.saraguro_03,
                R.drawable.saraguro_04, R.drawable.saraguro_05));

        // ── AMAZONÍA ──
        fotosMap.put("Achuar", Arrays.asList(
                R.drawable.achuar_01, R.drawable.achuar_02, R.drawable.achuar_03,
                R.drawable.achuar_04, R.drawable.achuar_05));

        fotosMap.put("Andoa", Arrays.asList(
                R.drawable.andoa_01, R.drawable.andoa_02, R.drawable.andoa_03,
                R.drawable.andoa_04, R.drawable.andoa_05));

        fotosMap.put("Cofán", Arrays.asList(
                R.drawable.cofan_01, R.drawable.cofan_02, R.drawable.cofan_03,
                R.drawable.cofan_04, R.drawable.cofan_05));

        fotosMap.put("Siona", Arrays.asList(
                R.drawable.siona_01, R.drawable.siona_02, R.drawable.siona_03,
                R.drawable.siona_04, R.drawable.siona_05));

        fotosMap.put("Secoya", Arrays.asList(
                R.drawable.secoya_01, R.drawable.secoya_02, R.drawable.secoya_03,
                R.drawable.secoya_04, R.drawable.secoya_05));

        fotosMap.put("Shuar", Arrays.asList(
                R.drawable.shuar_01, R.drawable.shuar_02, R.drawable.shuar_03,
                R.drawable.shuar_04, R.drawable.shuar_05));

        fotosMap.put("Shiwiar", Arrays.asList(
                R.drawable.shiwiar_01, R.drawable.shiwiar_02, R.drawable.shiwiar_03,
                R.drawable.shiwiar_04, R.drawable.shiwiar_05));

        fotosMap.put("Waorani", Arrays.asList(
                R.drawable.waorani_01, R.drawable.waorani_02, R.drawable.waorani_03,
                R.drawable.waorani_04, R.drawable.waorani_05));

        fotosMap.put("Zápara", Arrays.asList(
                R.drawable.zapara_01, R.drawable.zapara_02, R.drawable.zapara_03,
                R.drawable.zapara_04, R.drawable.zapara_05));

        fotosMap.put("Kichwa de la Amazonia", Arrays.asList(
                R.drawable.kichwa_01, R.drawable.kichwa_02, R.drawable.kichwa_03,
                R.drawable.kichwa_04, R.drawable.kichwa_05));

        // ── Buscar coincidencia exacta primero, luego parcial ──
        List<Integer> fotos = fotosMap.get(nacionalidad);

        if (fotos == null) {
            // Búsqueda parcial por si acaso
            for (Map.Entry<String, List<Integer>> entry : fotosMap.entrySet()) {
                if (nacionalidad.toLowerCase().contains(entry.getKey().toLowerCase()) ||
                        entry.getKey().toLowerCase().contains(nacionalidad.toLowerCase())) {
                    fotos = entry.getValue();
                    break;
                }
            }
        }

        // Si no encuentra nada, mostrar placeholder
        if (fotos == null) {
            fotos = Arrays.asList(R.drawable.placeholder_community);
        }

        final List<Integer> fotosFinal = fotos;
        final int total = fotosFinal.size();

        PhotoAdapter adapter = new PhotoAdapter(this, fotosFinal);
        vpCommunityPhotos.setAdapter(adapter);
        tvPhotoCounter.setText("1/" + total);

        vpCommunityPhotos.registerOnPageChangeCallback(
                new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        tvPhotoCounter.setText((position + 1) + "/" + total);
                    }
                });

        btnPhotoNext.setOnClickListener(v -> {
            int next = vpCommunityPhotos.getCurrentItem() + 1;
            if (next < total) vpCommunityPhotos.setCurrentItem(next);
        });

        btnPhotoPrev.setOnClickListener(v -> {
            int prev = vpCommunityPhotos.getCurrentItem() - 1;
            if (prev >= 0) vpCommunityPhotos.setCurrentItem(prev);
        });
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