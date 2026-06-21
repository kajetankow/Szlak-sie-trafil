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

    @Query("SELECT p.PunktUuid, tp.TrasaUuid, p.Nazwa, p.Opis, k.Nazwa AS Kategoria, " +
            "p.Latitude, p.Longitude, tp.Kolejnosc " +
            "FROM Trasa_Punkt tp " +
            "INNER JOIN Punkty p ON p.PunktUuid = tp.PunktUuid " +
            "LEFT JOIN KategoriePunktow k ON k.KategoriaUuid = p.KategoriaUuid " +
            "WHERE tp.TrasaUuid = :trasaId " +
            "ORDER BY tp.Kolejnosc ASC")
    List<PunktTrasyZKategoria> getPunktyTrasy(String trasaId);

    @Query("SELECT p.PunktUuid, NULL AS TrasaUuid, p.Nazwa, p.Opis, k.Nazwa AS Kategoria, " +
            "p.Latitude, p.Longitude, 0 AS Kolejnosc " +
            "FROM Punkty p " +
            "LEFT JOIN KategoriePunktow k ON k.KategoriaUuid = p.KategoriaUuid " +
            "ORDER BY p.Nazwa COLLATE NOCASE ASC")
    List<PunktTrasyZKategoria> getPunkty();

    @Query("SELECT p.PunktUuid, NULL AS TrasaUuid, p.Nazwa, p.Opis, k.Nazwa AS Kategoria, " +
            "p.Latitude, p.Longitude, 0 AS Kolejnosc " +
            "FROM Punkty p " +
            "LEFT JOIN KategoriePunktow k ON k.KategoriaUuid = p.KategoriaUuid " +
            "WHERE p.Nazwa LIKE '%' || :fraza || '%' " +
            "ORDER BY p.Nazwa COLLATE NOCASE ASC")
    List<PunktTrasyZKategoria> szukajPunkty(String fraza);

    @Query("SELECT p.PunktUuid, tp.TrasaUuid, p.Nazwa, p.Opis, k.Nazwa AS Kategoria, " +
            "p.Latitude, p.Longitude, MIN(tp.Kolejnosc) AS Kolejnosc " +
            "FROM Treningi t " +
            "INNER JOIN Trasa_Punkt tp ON tp.TrasaUuid = t.TrasaUuid " +
            "INNER JOIN Punkty p ON p.PunktUuid = tp.PunktUuid " +
            "LEFT JOIN KategoriePunktow k ON k.KategoriaUuid = p.KategoriaUuid " +
            "WHERE t.UzytkownikId = :uzytkownikId " +
            "GROUP BY p.PunktUuid " +
            "ORDER BY MAX(t.DataStartu) DESC, MIN(tp.Kolejnosc) ASC " +
            "LIMIT 5")
    List<PunktTrasyZKategoria> getOstatnioOdwiedzonePunkty(String uzytkownikId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void dodajTrase(TrasaEntity trasa);

    @Update
    void zmienTrase(TrasaEntity trasa);

    @Query("DELETE FROM Trasa_Punkt WHERE TrasaUuid = :trasaId")
    void usunPunktyTrasy(String trasaId);

    @Query("DELETE FROM Trasy WHERE TrasaUuid = :trasaId")
    void usunTrasePoId(String trasaId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void dodajPunkt(PunktTrasyEntity punkt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void dodajKategorie(KategoriaPunktuEntity kategoria);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void dodajPunktTrasy(TrasaPunktEntity trasaPunkt);

    @Query("SELECT * FROM Punkty WHERE PunktUuid = :punktId LIMIT 1")
    PunktTrasyEntity getPunktPoId(String punktId);

    @Query("SELECT p.PunktUuid FROM Punkty p " +
            "WHERE LOWER(TRIM(p.Nazwa)) = LOWER(TRIM(:nazwa)) " +
            "AND ROUND(p.Latitude, 5) = ROUND(:latitude, 5) " +
            "AND ROUND(p.Longitude, 5) = ROUND(:longitude, 5) " +
            "LIMIT 1")
    String znajdzPunkt(String nazwa, double latitude, double longitude);

    @Query("SELECT KategoriaUuid FROM KategoriePunktow WHERE LOWER(TRIM(Nazwa)) = LOWER(TRIM(:nazwa)) LIMIT 1")
    String znajdzKategorie(String nazwa);

    @Query("DELETE FROM Punkty WHERE PunktUuid = :punktId")
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
