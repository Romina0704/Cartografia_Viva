package com.example.cartografiaviva;

import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, TextToSpeech.OnInitListener {

    private static final String TAG = "MainActivity";
    private GoogleMap mMap;
    private TextView tvNationalityName, tvNarrativeDescription, tvLocation, tvPopulation, tvCommunityLabel, tvDistance, tvDuration;
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

    // Voz - Text To Speech
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar TTS
        tts = new TextToSpeech(this, this);

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
        tvDuration = findViewById(R.id.tv_duration);
        btnPlayMusic = findViewById(R.id.btn_play_music);

        if (btnPlayMusic != null) {
            btnPlayMusic.setOnClickListener(v -> toggleMusica());
        }

        // Botón Narración del Shamán
        View btnNarration = findViewById(R.id.btn_narration);
        if (btnNarration != null) {
            btnNarration.setOnClickListener(v -> narrarEtnia());
        }

        // Botón Tab Nacionalidades
        View tabNacionalidades = findViewById(R.id.tab_nacionalidades);
        if (tabNacionalidades != null) {
            tabNacionalidades.setOnClickListener(v -> mostrarListaNacionalidades());
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

        findViewById(R.id.fab_map_type).setOnClickListener(v -> cambiarTipoMapa());
        cargarDatosDesdeJSON();
    }

    private void mostrarListaNacionalidades() {
        if (listaEtnias == null || listaEtnias.isEmpty()) {
            Toast.makeText(this, "Cargando datos...", Toast.LENGTH_SHORT).show();
            return;
        }

        final Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_nacionalidades);

        RecyclerView rvNacionalidades = dialog.findViewById(R.id.rv_nacionalidades);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_list);

        if (rvNacionalidades != null) {
            rvNacionalidades.setLayoutManager(new LinearLayoutManager(this));
            NacionalidadAdapter adapter = new NacionalidadAdapter(listaEtnias, etnia -> {
                actualizarUIConEtnia(etnia);
                dialog.dismiss();
            });
            rvNacionalidades.setAdapter(adapter);
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("es", "ES"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Lenguaje no soportado para TTS");
            }
        } else {
            Log.e(TAG, "Fallo al inicializar TTS");
        }
    }

    private void narrarEtnia() {
        if (etniaSeleccionada == null) {
            Toast.makeText(this, "Primero selecciona una nacionalidad en el mapa o usa el dado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayMusic.setImageResource(android.R.drawable.ic_media_play);
        }

        float[] results = new float[1];
        Location.distanceBetween(ubicacionUsuario.latitude, ubicacionUsuario.longitude, etniaSeleccionada.latitud, etniaSeleccionada.longitud, results);
        int distanciaKm = (int) (results[0] / 1000);

        String guion = "Saludos viajero. Te presento a la nacionalidad " + etniaSeleccionada.nacionalidad + ". " +
                "Ellos habitan en la región " + etniaSeleccionada.region + ". " +
                "Actualmente te encuentras a " + distanciaKm + " kilómetros de su territorio. " +
                etniaSeleccionada.descripcion + ". " +
                "Su lengua ancestral es el " + etniaSeleccionada.lengua + ". " +
                "¡Disfruta aprendiendo de nuestra Cartografía Viva!";

        tts.speak(guion, TextToSpeech.QUEUE_FLUSH, null, "ID_NARRACION");
        Toast.makeText(this, "Escuchando narración del Shamán...", Toast.LENGTH_SHORT).show();
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

        final MediaPlayer mpDado = MediaPlayer.create(this, R.raw.efecto_dado);
        if (mpDado != null) {
            mpDado.setLooping(true);
            mpDado.start();
        }

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
                    if (mpDado != null) {
                        try {
                            mpDado.stop();
                            mpDado.release();
                        } catch (Exception e) {
                            Log.e(TAG, "Error liberando MediaPlayer dado", e);
                        }
                    }

                    Etnia etniaFinal = listaEtnias.get(random.nextInt(listaEtnias.size()));
                    actualizarUIConEtnia(etniaFinal);
                }
            }
        };
        handler.post(runnable);
    }

    private void toggleMusica() {
        if (etniaSeleccionada == null || etniaSeleccionada.musica_recurso == null) {
            Toast.makeText(this, "Selecciona una etnia primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tts != null && tts.isSpeaking()) {
            tts.stop();
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

            String nombreLimpio = nombreRecurso.replace(".mp3", "").trim().toLowerCase();
            int resId = getResources().getIdentifier(nombreLimpio, "raw", getPackageName());

            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId);
                if (mediaPlayer != null) {
                    mediaPlayer.setOnCompletionListener(mp -> btnPlayMusic.setImageResource(android.R.drawable.ic_media_play));
                    mediaPlayer.start();
                    btnPlayMusic.setImageResource(android.R.drawable.ic_media_pause);
                    Toast.makeText(this, "Reproduciendo música: " + etniaSeleccionada.nacionalidad, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error de formato en archivo: " + nombreLimpio, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Audio '" + nombreLimpio + "' no encontrado en res/raw", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "No se encontró el recurso: " + nombreLimpio);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fallo crítico en MediaPlayer", e);
            Toast.makeText(this, "Error al cargar el audio", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (tts != null) {
            tts.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
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
        
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
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
        float distanciaKm = results[0] / 1000;
        
        tvCommunityLabel.setText("¡DESCUBIERTO!: " + etnia.nacionalidad.toUpperCase());
        tvNationalityName.setText("Nacionalidad: " + etnia.nacionalidad);
        tvLocation.setText("Región: " + etnia.region);
        tvPopulation.setText("Población Est.: " + (etnia.poblacion != null ? etnia.poblacion : "No disponible"));
        tvNarrativeDescription.setText(etnia.descripcion + "\n\nLengua: " + etnia.lengua);
        tvDistance.setText(String.format(Locale.US, "Distancia: %.2f km", distanciaKm));
        
        int horas = (int) (distanciaKm / 80);
        if (tvDuration != null) {
            tvDuration.setText("Aprox. " + (horas > 0 ? horas + "h " : "") + "por tierra");
        }

        cargarFotosEtnia(etnia.nacionalidad);
    }

    private void mostrarFotoGrande(int position, List<Integer> fotos) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_photo_viewer);
        
        ViewPager2 vpFull = dialog.findViewById(R.id.vp_full_photo);
        TextView tvFullCounter = dialog.findViewById(R.id.tv_full_photo_counter);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_viewer);
        
        if (vpFull != null) {
            PhotoAdapter fullAdapter = new PhotoAdapter(this, fotos, ImageView.ScaleType.FIT_CENTER, null);
            vpFull.setAdapter(fullAdapter);
            vpFull.setCurrentItem(position, false);
            
            int total = fotos.size();
            tvFullCounter.setText((position + 1) + "/" + total);
            
            vpFull.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int newPos) {
                    tvFullCounter.setText((newPos + 1) + "/" + total);
                    vpCommunityPhotos.setCurrentItem(newPos, true);
                }
            });
        }
        
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }

    private void cargarFotosEtnia(String nacionalidad) {
        Map<String, List<Integer>> fotosMap = new HashMap<>();
        fotosMap.put("Awá", Arrays.asList(R.drawable.awa_01, R.drawable.awa_02, R.drawable.awa_03, R.drawable.awa_04, R.drawable.awa_05));
        fotosMap.put("Chachi", Arrays.asList(R.drawable.chachi_01, R.drawable.chachi_02, R.drawable.chachi_03, R.drawable.chachi_04, R.drawable.chachi_05));
        fotosMap.put("Épera", Arrays.asList(R.drawable.epera_01, R.drawable.epera_02, R.drawable.epera_03, R.drawable.epera_04, R.drawable.epera_05));
        fotosMap.put("Tsáchila", Arrays.asList(R.drawable.tsachila_01, R.drawable.tsachila_02, R.drawable.tsachila_03, R.drawable.tsachila_04, R.drawable.tsachila_05));
        fotosMap.put("Otavalo", Arrays.asList(R.drawable.otavalo_01, R.drawable.otavalo_02, R.drawable.otavalo_03, R.drawable.otavalo_04, R.drawable.otavalo_05));
        fotosMap.put("Kayambi", Arrays.asList(R.drawable.kayambi_01, R.drawable.kayambi_02, R.drawable.kayambi_03, R.drawable.kayambi_04, R.drawable.kayambi_05));
        fotosMap.put("Kitu Kara", Arrays.asList(R.drawable.kitu_kara_01, R.drawable.kitu_kara_02, R.drawable.kitu_kara_03, R.drawable.kitu_kara_04, R.drawable.kitu_kara_05));
        fotosMap.put("Panzaleo", Arrays.asList(R.drawable.panzaleo_01, R.drawable.panzaleo_02, R.drawable.panzaleo_03, R.drawable.panzaleo_04, R.drawable.panzaleo_05));
        fotosMap.put("Chibuleo", Arrays.asList(R.drawable.chibuleo_01, R.drawable.chibuleo_02, R.drawable.chibuleo_03, R.drawable.chibuleo_04, R.drawable.chibuleo_05));
        fotosMap.put("Salasaka", Arrays.asList(R.drawable.salaka_01, R.drawable.salaka_02, R.drawable.salaka_03, R.drawable.salaka_04, R.drawable.salaka_05));
        fotosMap.put("Waranka", Arrays.asList(R.drawable.waranka_01, R.drawable.waranka_02, R.drawable.waranka_03, R.drawable.waranka_04));
        fotosMap.put("Puruhá", Arrays.asList(R.drawable.puruha_01, R.drawable.puruha_02, R.drawable.puruha_03, R.drawable.puruha_04, R.drawable.puruha_05));
        fotosMap.put("Kañari", Arrays.asList(R.drawable.kanari_01, R.drawable.kanari_02, R.drawable.kanari_03, R.drawable.kanari_04, R.drawable.kanari_05));
        fotosMap.put("Saraguro", Arrays.asList(R.drawable.saraguro_01, R.drawable.saraguro_02, R.drawable.saraguro_03, R.drawable.saraguro_04, R.drawable.saraguro_05));
        fotosMap.put("Achuar", Arrays.asList(R.drawable.achuar_01, R.drawable.achuar_02, R.drawable.achuar_03, R.drawable.achuar_04, R.drawable.achuar_05));
        fotosMap.put("Andoa", Arrays.asList(R.drawable.andoa_01, R.drawable.andoa_02, R.drawable.andoa_03, R.drawable.andoa_04, R.drawable.andoa_05));
        fotosMap.put("Cofán", Arrays.asList(R.drawable.cofan_01, R.drawable.cofan_02, R.drawable.cofan_03, R.drawable.cofan_04, R.drawable.cofan_05));
        fotosMap.put("Siona", Arrays.asList(R.drawable.siona_01, R.drawable.siona_02, R.drawable.siona_03, R.drawable.siona_04, R.drawable.siona_05));
        fotosMap.put("Secoya", Arrays.asList(R.drawable.secoya_01, R.drawable.secoya_02, R.drawable.secoya_03, R.drawable.secoya_04, R.drawable.secoya_05));
        fotosMap.put("Shuar", Arrays.asList(R.drawable.shuar_01, R.drawable.shuar_02, R.drawable.shuar_03, R.drawable.shuar_04, R.drawable.shuar_05));
        fotosMap.put("Shiwiar", Arrays.asList(R.drawable.shiwiar_01, R.drawable.shiwiar_02, R.drawable.shiwiar_03, R.drawable.shiwiar_04, R.drawable.shiwiar_05));
        fotosMap.put("Waorani", Arrays.asList(R.drawable.waorani_01, R.drawable.waorani_02, R.drawable.waorani_03, R.drawable.waorani_04, R.drawable.waorani_05));
        fotosMap.put("Zápara", Arrays.asList(R.drawable.zapara_01, R.drawable.zapara_02, R.drawable.zapara_03, R.drawable.zapara_04, R.drawable.zapara_05));
        fotosMap.put("Kichwa de la Amazonia", Arrays.asList(R.drawable.kichwa_01, R.drawable.kichwa_02, R.drawable.kichwa_03, R.drawable.kichwa_04, R.drawable.kichwa_05));

        List<Integer> fotos = fotosMap.get(nacionalidad);
        if (fotos == null) {
            for (Map.Entry<String, List<Integer>> entry : fotosMap.entrySet()) {
                if (nacionalidad.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    fotos = entry.getValue();
                    break;
                }
            }
        }

        if (fotos == null) {
            fotos = Arrays.asList(R.drawable.placeholder_community);
        }

        final List<Integer> fotosFinal = fotos;
        final int total = fotosFinal.size();

        PhotoAdapter adapter = new PhotoAdapter(this, fotosFinal, position -> mostrarFotoGrande(position, fotosFinal));
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
