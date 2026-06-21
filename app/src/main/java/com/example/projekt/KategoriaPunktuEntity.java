package com.example.projekt;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "KategoriePunktow",
        indices = @Index(value = "Nazwa", unique = true)
)
public class KategoriaPunktuEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "KategoriaUuid")
    public String uuid;

    @ColumnInfo(name = "Nazwa")
    public String nazwa;

    public KategoriaPunktuEntity(@NonNull String uuid, String nazwa) {
        this.uuid = uuid;
        this.nazwa = nazwa;
    }
}
