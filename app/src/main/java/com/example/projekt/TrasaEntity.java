package com.example.projekt;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Trasy")
public class TrasaEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "TrasaUuid")
    public String uuid;

    @ColumnInfo(name = "Nazwa")
    public String nazwa;

    @ColumnInfo(name = "Opis")
    public String opis;

    @ColumnInfo(name = "Region")
    public String region;

    @ColumnInfo(name = "DystansKm")
    public double dystansKm;

    @ColumnInfo(name = "PrzewyzszenieM")
    public int przewyzszenieM;

    @ColumnInfo(name = "CzasMin")
    public int czasMin;

    @ColumnInfo(name = "Poziom")
    public String poziom;

    @ColumnInfo(name = "Typ")
    public String typ;

    @ColumnInfo(name = "Dev")
    public boolean dev;

    @ColumnInfo(name = "Ulubiona")
    public boolean ulubiona;

    @ColumnInfo(name = "UzytkownikId")
    public String uzytkownikId;

    public TrasaEntity(@NonNull String uuid, String nazwa, String opis, String region,
                       double dystansKm, int przewyzszenieM, int czasMin,
                       String poziom, String typ, boolean dev, boolean ulubiona,
                       String uzytkownikId) {
        this.uuid = uuid;
        this.nazwa = nazwa;
        this.opis = opis;
        this.region = region;
        this.dystansKm = dystansKm;
        this.przewyzszenieM = przewyzszenieM;
        this.czasMin = czasMin;
        this.poziom = poziom;
        this.typ = typ;
        this.dev = dev;
        this.ulubiona = ulubiona;
        this.uzytkownikId = uzytkownikId;
    }
}
