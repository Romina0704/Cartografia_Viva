package com.example.cartografiaviva;

import java.util.List;

public class Etnia {
    public String nacionalidad;
    public String region;
    public String lengua;
    public double latitud;
    public double longitud;
    public String descripcion;
    public List<PuntoInteres> puntos_interes;

    public static class PuntoInteres {
        public String nombre;
        public String tipo;
        public double lat;
        public double lng;
    }
}
