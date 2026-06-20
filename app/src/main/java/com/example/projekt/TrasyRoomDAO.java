package com.example.projekt;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TrasyRoomDAO {

    @Query("SELECT * FROM Trasy WHERE Dev = 1 OR UzytkownikId = :uzytkownikId ORDER BY Dev DESC, Nazwa ASC")
    List<TrasaEntity> getTrasy(String uzytkownikId);

    @Query("SELECT * FROM Trasy WHERE Dev = 1 ORDER BY Nazwa ASC")
    List<TrasaEntity> getTrasyDev();

    @Query("SELECT * FROM Trasy WHERE Dev = 0 AND UzytkownikId = :uzytkownikId ORDER BY Nazwa ASC")
    List<TrasaEntity> getTrasyUzytkownika(String uzytkownikId);

    @Query("SELECT * FROM Trasy WHERE TrasaUuid = :id LIMIT 1")
    TrasaEntity getTrasa(String id);

    @Query("SELECT * FROM PunktyTrasy WHERE TrasaUuid = :trasaId ORDER BY Kolejnosc ASC")
    List<PunktTrasyEntity> getPunktyTrasy(String trasaId);

    @Query("SELECT * FROM PunktyTrasy ORDER BY Nazwa COLLATE NOCASE ASC")
    List<PunktTrasyEntity> getPunkty();

    @Query("SELECT * FROM PunktyTrasy WHERE Nazwa LIKE '%' || :fraza || '%' ORDER BY Nazwa COLLATE NOCASE ASC")
    List<PunktTrasyEntity> szukajPunkty(String fraza);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void dodajTrase(TrasaEntity trasa);

    @Update
    void zmienTrase(TrasaEntity trasa);

    @Query("DELETE FROM PunktyTrasy WHERE TrasaUuid = :trasaId")
    void usunPunktyTrasy(String trasaId);

    @Query("DELETE FROM Trasy WHERE TrasaUuid = :trasaId")
    void usunTrasePoId(String trasaId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void dodajPunkt(PunktTrasyEntity punkt);

    @Query("DELETE FROM PunktyTrasy WHERE PunktUuid = :punktId")
    void usunPunkt(String punktId);

    @Query("UPDATE Trasy SET Ulubiona = :ulubiona WHERE TrasaUuid = :trasaId")
    void ustawUlubiona(String trasaId, boolean ulubiona);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void dodajTrening(TreningEntity trening);

    @Query("SELECT * FROM Treningi WHERE UzytkownikId = :uzytkownikId ORDER BY DataStartu DESC")
    List<TreningEntity> getTreningi(String uzytkownikId);

    @Query("SELECT * FROM Treningi WHERE UzytkownikId = :uzytkownikId AND DataStartu >= :odMs ORDER BY DataStartu DESC")
    List<TreningEntity> getTreningiOd(String uzytkownikId, long odMs);

    @Query("DELETE FROM Treningi WHERE UzytkownikId = :uzytkownikId")
    void usunWszystkieTreningi(String uzytkownikId);
}
