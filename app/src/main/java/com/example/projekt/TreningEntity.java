package com.example.projekt;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Treningi")
public class TreningEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "TreningUuid")
    public String uuid;

    @ColumnInfo(name = "TrasaUuid")
    public String trasaUuid;

    @ColumnInfo(name = "NazwaTrasy")
    public String nazwaTrasy;

    @ColumnInfo(name = "DataStartu")
    public long dataStartu;

    @ColumnInfo(name = "DataKonca")
    public long dataKonca;

    @ColumnInfo(name = "CzasMin")
    public int czasMin;

    @ColumnInfo(name = "DystansKm")
    public double dystansKm;

    @ColumnInfo(name = "PrzewyzszenieM")
    public int przewyzszenieM;

    @ColumnInfo(name = "Kalorie")
    public int kalorie;

    @ColumnInfo(name = "UzytkownikId")
    public String uzytkownikId;

    public TreningEntity(@NonNull String uuid, String trasaUuid, String nazwaTrasy,
                         long dataStartu, long dataKonca, int czasMin,
                         double dystansKm, int przewyzszenieM, int kalorie,
                         String uzytkownikId) {
        this.uuid = uuid;
        this.trasaUuid = trasaUuid;
        this.nazwaTrasy = nazwaTrasy;
        this.dataStartu = dataStartu;
        this.dataKonca = dataKonca;
        this.czasMin = czasMin;
        this.dystansKm = dystansKm;
        this.przewyzszenieM = przewyzszenieM;
        this.kalorie = kalorie;
        this.uzytkownikId = uzytkownikId;
    }
}
