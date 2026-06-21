package com.example.projekt;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "Trasa_Punkt",
        primaryKeys = {"TrasaUuid", "PunktUuid"},
        foreignKeys = {
                @ForeignKey(
                        entity = TrasaEntity.class,
                        parentColumns = "TrasaUuid",
                        childColumns = "TrasaUuid",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = PunktTrasyEntity.class,
                        parentColumns = "PunktUuid",
                        childColumns = "PunktUuid",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "TrasaUuid"),
                @Index(value = "PunktUuid")
        }
)
public class TrasaPunktEntity {

    @NonNull
    @ColumnInfo(name = "TrasaUuid")
    public String trasaUuid;

    @NonNull
    @ColumnInfo(name = "PunktUuid")
    public String punktUuid;

    @ColumnInfo(name = "Kolejnosc")
    public int kolejnosc;

    public TrasaPunktEntity(@NonNull String trasaUuid, @NonNull String punktUuid, int kolejnosc) {
        this.trasaUuid = trasaUuid;
        this.punktUuid = punktUuid;
        this.kolejnosc = kolejnosc;
    }
}
