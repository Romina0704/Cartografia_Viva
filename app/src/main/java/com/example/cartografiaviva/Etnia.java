package com.example.cartografiaviva;

import java.util.List;

public class Etnia {
    public String nacionalidad;
    public String region;
    public String lengua;
    public String poblacion;
    public double latitud;
    public double longitud;
    public String descripcion;
    public String musica_recurso; // Nombre del archivo en res/raw (ej: "musica_awa")
    public List<PuntoInteres> puntos_interes;

    public static class PuntoInteres {
        public String nombre;
        public String tipo;
        public double lat;
        public double lng;
    }
}