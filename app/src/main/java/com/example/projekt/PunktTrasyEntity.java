package com.example.projekt;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "Punkty",
        foreignKeys = @ForeignKey(
                entity = KategoriaPunktuEntity.class,
                parentColumns = "KategoriaUuid",
                childColumns = "KategoriaUuid",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {
                @Index(value = "KategoriaUuid"),
                @Index(value = {"Nazwa", "Latitude", "Longitude"}, unique = true)
        }
)
public class PunktTrasyEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "PunktUuid")
    public String uuid;

    @ColumnInfo(name = "KategoriaUuid")
    public String kategoriaUuid;

    @ColumnInfo(name = "Nazwa")
    public String nazwa;

    @ColumnInfo(name = "Opis")
    public String opis;

    @ColumnInfo(name = "Latitude")
    public double latitude;

    @ColumnInfo(name = "Longitude")
    public double longitude;

    public PunktTrasyEntity(@NonNull String uuid, String kategoriaUuid, String nazwa, String opis,
                            double latitude, double longitude) {
        this.uuid = uuid;
        this.kategoriaUuid = kategoriaUuid;
        this.nazwa = nazwa;
        this.opis = opis;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
