package com.example.cartografiaviva;

import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private TextView tvNationalityName, tvNarrativeTitle, tvNarrativeDescription;
    private List<Etnia> listaEtnias;
    private List<Marker> marcadoresPOIs = new ArrayList<>();

    // Ubicación simulada del usuario (Quevedo, Ecuador)
    private final LatLng ubicacionUsuario = new LatLng(-1.0241, -79.4611);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvNationalityName = findViewById(R.id.tv_nationality_name);
        tvNarrativeTitle = findViewById(R.id.tv_narrative_title);
        tvNarrativeDescription = findViewById(R.id.tv_narrative_description);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 1. Cargar datos del archivo JSON en Assets
        cargarDatosDesdeJSON();
    }

    private void cargarDatosDesdeJSON() {
        try {
            InputStream is = getAssets().open("etnias.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Etnia>>(){}.getType();
            listaEtnias = gson.fromJson(json, listType);
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Error cargando etnias.json", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        // Centrar mapa en Ecuador
        LatLng ecuador = new LatLng(-1.8312, -78.1834);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ecuador, 6.5f));

        // Marcador de ubicación del usuario
        mMap.addMarker(new MarkerOptions()
                .position(ubicacionUsuario)
                .title("Tu ubicación actual")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        // 2. Dibujar las nacionalidades del JSON
        dibujarEtnias();
    }

    private void dibujarEtnias() {
        if (listaEtnias == null) return;

        for (Etnia etnia : listaEtnias) {
            float color;
            switch (etnia.region) {
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
            Etnia etnia = (Etnia) tag;

            // 3. Cálculo de distancia
            float[] results = new float[1];
            Location.distanceBetween(ubicacionUsuario.latitude, ubicacionUsuario.longitude,
                    etnia.latitud, etnia.longitud, results);
            float distanciaKm = results[0] / 1000;

            // 4. Actualizar interfaz con prefijo del strings.xml
            tvNationalityName.setText(getString(R.string.ethnicity_prefix, etnia.nacionalidad));
            tvNarrativeTitle.setText(getString(R.string.distance_label, distanciaKm));
            tvNarrativeDescription.setText(etnia.descripcion + "\n\nLengua: " + etnia.lengua);

            // 5. Mostrar Puntos de Interés Dinámicos
            limpiarPOIsAnteriores();
            mostrarPuntosDeInteres(etnia);
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        return false;
    }

    private void mostrarPuntosDeInteres(Etnia etnia) {
        if (etnia.puntos_interes != null) {
            for (Etnia.PuntoInteres poi : etnia.puntos_interes) {
                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(poi.lat, poi.lng))
                        .title(poi.nombre)
                        .snippet("Tipo: " + poi.tipo)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                marcadoresPOIs.add(m);
            }
        }
    }

    private void limpiarPOIsAnteriores() {
        for (Marker m : marcadoresPOIs) {
            m.remove();
        }
        marcadoresPOIs.clear();
    }
}