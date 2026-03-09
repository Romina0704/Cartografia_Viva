package com.example.cartografiaviva;

import android.os.Bundle;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private TextView tvNationalityName, tvNarrativeTitle, tvNarrativeDescription;

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
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        // Estilo del mapa (puedes personalizarlo más)
        LatLng ecuador = new LatLng(-1.8312, -78.1834);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ecuador, 6.5f));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        agregarEtnias();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        tvNationalityName.setText(getString(R.string.ethnicity_prefix, marker.getTitle()));
        tvNarrativeTitle.setText(marker.getTitle());
        tvNarrativeDescription.setText(marker.getSnippet());
        
        // Mover cámara suavemente al marcador
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        
        return false;
    }

    private void agregarEtnias() {
        // Región Costa - Color Azul
        float colorCosta = BitmapDescriptorFactory.HUE_AZURE;
        mMap.addMarker(new MarkerOptions().position(new LatLng(1.2167, -78.5000))
                .title("Awá").snippet("Región: Costa. Lengua: Awapit. Ubicación: Carchi, Esmeraldas e Imbabura.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorCosta)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(0.7333, -78.9333))
                .title("Chachi").snippet("Región: Costa. Lengua: Cha'palaachi. Ubicación: Esmeraldas.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorCosta)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(1.0000, -78.8000))
                .title("Épera").snippet("Región: Costa. Lengua: Sia Pedee. Ubicación: Esmeraldas.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorCosta)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-0.2521, -79.1714))
                .title("Tsáchila").snippet("Región: Costa. Lengua: Tsafiki. Ubicación: Santo Domingo.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorCosta)));

        // Región Sierra - Color Naranja/Amarillo
        float colorSierra = BitmapDescriptorFactory.HUE_ORANGE;
        mMap.addMarker(new MarkerOptions().position(new LatLng(0.2241, -78.2618))
                .title("Otavalo").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(0.0402, -78.1450))
                .title("Kayambi").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-0.1807, -78.4678))
                .title("Kitu Kara").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-0.9314, -78.6178))
                .title("Panzaleo").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-1.2800, -78.6800))
                .title("Chibuleo").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-1.2500, -78.5600))
                .title("Salasaka").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-1.5905, -79.0024))
                .title("Waranka").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-1.6709, -78.6471))
                .title("Puruhá").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-2.7380, -78.8475))
                .title("Kañari").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-3.6206, -79.2382))
                .title("Saraguro").snippet("Región: Sierra. Pueblo Kichwa de la Sierra.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorSierra)));

        // Región Amazónica - Color Verde
        float colorAmazonia = BitmapDescriptorFactory.HUE_GREEN;
        mMap.addMarker(new MarkerOptions().position(new LatLng(-2.0000, -77.0000))
                .title("Achuar").snippet("Región: Amazonía. Lengua: Achuar Chicham. Ubicación: Pastaza y Morona Santiago.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-2.1000, -76.4000))
                .title("Andoa").snippet("Región: Amazonía. Lengua: Andoa. Ubicación: Pastaza.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(0.3500, -77.2500))
                .title("Cofán (A'i)").snippet("Región: Amazonía. Lengua: A'ingae. Ubicación: Sucumbíos.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(0.1500, -76.3000))
                .title("Siona").snippet("Región: Amazonía. Lengua: Baicoca. Ubicación: Sucumbíos.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-0.2000, -76.2000))
                .title("Secoya (Siekopai)").snippet("Región: Amazonía. Lengua: Paicoca. Ubicación: Sucumbíos.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-3.0000, -78.0000))
                .title("Shuar").snippet("Región: Amazonía. Lengua: Shuar Chicham. Ubicación: Morona Santiago, Zamora, Pastaza.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-2.4000, -76.7000))
                .title("Shiwiar").snippet("Región: Amazonía. Lengua: Shiwiar Chicham. Ubicación: Pastaza.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-1.0000, -76.5000))
                .title("Waorani").snippet("Región: Amazonía. Lengua: Wao Terero. Ubicación: Orellana, Pastaza y Napo.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-1.8000, -76.6000))
                .title("Zápara").snippet("Región: Amazonía. Ubicación: Pastaza. Lengua: Patrimonio Cultural de la Humanidad.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
        mMap.addMarker(new MarkerOptions().position(new LatLng(-1.0000, -77.5000))
                .title("Kichwa Amazónico").snippet("Región: Amazonía. Ubicación: Napo y Pastaza.")
                .icon(BitmapDescriptorFactory.defaultMarker(colorAmazonia)));
    }
}
