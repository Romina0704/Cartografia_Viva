package com.example.cartografiaviva;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;

public class PruebaAPI {
    public JsonElement perfil; 
    public JsonElement imagen; 
    public List<Rasgo> rasgos;
    public JsonElement confianza; // Usamos JsonElement para evitar errores si es String o Number

    public static class Rasgo {
        public String nombre;
    }

    public double getConfianzaDouble() {
        if (confianza != null && confianza.isJsonPrimitive()) {
            try {
                return confianza.getAsDouble();
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    public String getPerfilLimpio() {
        if (perfil != null && perfil.isJsonPrimitive()) {
            return perfil.getAsString().replace("\n", " ").trim();
        }
        if (imagen != null && imagen.isJsonObject()) {
            JsonObject imgObj = imagen.getAsJsonObject();
            if (imgObj.has("perfil") && imgObj.get("perfil").isJsonPrimitive()) {
                return imgObj.get("perfil").getAsString().replace("\n", " ").trim();
            }
        }
        return "Perfil no disponible";
    }

    public String getClasificacionExperimental() {
        if (rasgos != null) {
            for (Rasgo r : rasgos) {
                if (r.nombre != null && r.nombre.toLowerCase().contains("clasificación experimental")) {
                    return r.nombre;
                }
            }
        }
        return "Clasificación: No detectada";
    }
}
