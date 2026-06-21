package com.example.projekt;

import androidx.room.ColumnInfo;

public class PunktTrasyZKategoria {

    @ColumnInfo(name = "PunktUuid")
    public String uuid;

    @ColumnInfo(name = "TrasaUuid")
    public String trasaUuid;

    @ColumnInfo(name = "Nazwa")
    public String nazwa;

    @ColumnInfo(name = "Opis")
    public String opis;

    @ColumnInfo(name = "Kategoria")
    public String kategoria;

    @ColumnInfo(name = "Latitude")
    public double latitude;

    @ColumnInfo(name = "Longitude")
    public double longitude;

    @ColumnInfo(name = "Kolejnosc")
    public int kolejnosc;
}
