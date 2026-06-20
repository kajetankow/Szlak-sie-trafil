package com.example.projekt;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "PunktyTrasy",
        foreignKeys = @ForeignKey(
                entity = TrasaEntity.class,
                parentColumns = "TrasaUuid",
                childColumns = "TrasaUuid",
                onDelete = ForeignKey.CASCADE
        )
)
public class PunktTrasyEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "PunktUuid")
    public String uuid;

    @ColumnInfo(name = "TrasaUuid", index = true)
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

    public PunktTrasyEntity(@NonNull String uuid, String trasaUuid, String nazwa, String opis,
                            String kategoria, double latitude, double longitude, int kolejnosc) {
        this.uuid = uuid;
        this.trasaUuid = trasaUuid;
        this.nazwa = nazwa;
        this.opis = opis;
        this.kategoria = kategoria;
        this.latitude = latitude;
        this.longitude = longitude;
        this.kolejnosc = kolejnosc;
    }
}