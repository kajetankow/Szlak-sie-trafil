package com.example.projekt;

import java.util.UUID;

public class PunktTrasy {
    private UUID nId;
    private UUID trasaId;
    private String nazwa;
    private String opis;
    private String kategoria;
    private double latitude;
    private double longitude;
    private int kolejnosc;

    public PunktTrasy() {
        nId = UUID.randomUUID();
        trasaId = null;
        nazwa = "";
        opis = "";
        kategoria = "Inne";
        latitude = 0.0;
        longitude = 0.0;
        kolejnosc = 0;
    }

    public UUID getnId() { return nId; }
    public void setnId(UUID nId) { this.nId = nId; }
    public UUID getTrasaId() { return trasaId; }
    public void setTrasaId(UUID trasaId) { this.trasaId = trasaId; }
    public String getNazwa() { return nazwa; }
    public void setNazwa(String nazwa) { this.nazwa = nazwa; }
    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }
    public String getKategoria() { return kategoria; }
    public void setKategoria(String kategoria) { this.kategoria = kategoria; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public int getKolejnosc() { return kolejnosc; }
    public void setKolejnosc(int kolejnosc) { this.kolejnosc = kolejnosc; }
}
