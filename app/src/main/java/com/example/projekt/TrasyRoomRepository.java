package com.example.projekt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrasyRoomRepository {

    private static final String PREFS_NAME = "tourroute_prefs";

    private final Context appContext;
    private final TrasyRoomDAO trasyDAO;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public TrasyRoomRepository(Context context) {
        appContext = context.getApplicationContext();
        AppBazaDanych bazaDanych = AppBazaDanych.getInstance(appContext);
        trasyDAO = bazaDanych.trasyRoomDAO();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getTrasyAsync(Callback<List<Trasa>> callback) {
        executorService.execute(() -> {
            List<TrasaEntity> encje = trasyDAO.getTrasy(aktualnyUzytkownikId());
            List<Trasa> trasy = TrasaMapper.toModelList(encje);

            mainHandler.post(() -> callback.onResult(trasy));
        });
    }

    public void getTrasaAsync(UUID trasaId, Callback<Trasa> callback) {
        executorService.execute(() -> {
            TrasaEntity encja = trasyDAO.getTrasa(trasaId.toString());
            Trasa trasa = encja == null ? null : TrasaMapper.toModel(encja);

            mainHandler.post(() -> callback.onResult(trasa));
        });
    }

    public void getPunktyTrasyAsync(UUID trasaId, Callback<List<PunktTrasy>> callback) {
        executorService.execute(() -> {
            List<PunktTrasyZKategoria> encje = trasyDAO.getPunktyTrasy(trasaId.toString());
            List<PunktTrasy> punkty = PunktTrasyMapper.toModelList(encje);

            mainHandler.post(() -> callback.onResult(punkty));
        });
    }

    public void getPunktyAsync(Callback<List<PunktTrasy>> callback) {
        executorService.execute(() -> {
            List<PunktTrasyZKategoria> encje = trasyDAO.getPunkty();
            List<PunktTrasy> punkty = PunktTrasyMapper.toModelList(encje);

            mainHandler.post(() -> callback.onResult(punkty));
        });
    }

    public void szukajPunktyAsync(String fraza, Callback<List<PunktTrasy>> callback) {
        executorService.execute(() -> {
            List<PunktTrasyZKategoria> encje = trasyDAO.szukajPunkty(fraza == null ? "" : fraza);
            List<PunktTrasy> punkty = PunktTrasyMapper.toModelList(encje);

            mainHandler.post(() -> callback.onResult(punkty));
        });
    }

    public void getOstatnioOdwiedzonePunktyAsync(Callback<List<PunktTrasy>> callback) {
        executorService.execute(() -> {
            List<PunktTrasyZKategoria> encje = trasyDAO.getOstatnioOdwiedzonePunkty(aktualnyUzytkownikId());
            List<PunktTrasy> punkty = PunktTrasyMapper.toModelList(encje);

            mainHandler.post(() -> callback.onResult(punkty));
        });
    }

    public void dodajTraseAsync(Trasa trasa, Runnable runnable) {
        executorService.execute(() -> {
            trasa.setUzytkownikId(aktualnyUzytkownikId());
            trasyDAO.dodajTrase(TrasaMapper.toEntity(trasa));
            mainHandler.post(runnable);
        });
    }

    public void zmienTraseAsync(Trasa trasa, Runnable runnable) {
        executorService.execute(() -> {
            if (!trasa.isDev()) {
                trasa.setUzytkownikId(aktualnyUzytkownikId());
            }
            trasyDAO.zmienTrase(TrasaMapper.toEntity(trasa));
            mainHandler.post(runnable);
        });
    }

    public void usunTraseAsync(UUID id, Runnable runnable) {
        executorService.execute(() -> {
            trasyDAO.usunPunktyTrasy(id.toString());
            trasyDAO.usunTrasePoId(id.toString());

            mainHandler.post(runnable);
        });
    }

    public void dodajPunktAsync(PunktTrasy punkt, Runnable runnable) {
        executorService.execute(() -> {
            zapiszPunktIRelacje(punkt);
            mainHandler.post(runnable);
        });
    }

    public void usunPunktAsync(UUID punktId, Runnable runnable) {
        executorService.execute(() -> {
            trasyDAO.usunPunkt(punktId.toString());
            mainHandler.post(runnable);
        });
    }

    public void zastapPunktyTrasyAsync(UUID trasaId, List<PunktTrasy> punkty, Runnable runnable) {
        executorService.execute(() -> {
            trasyDAO.usunPunktyTrasy(trasaId.toString());

            for (PunktTrasy punkt : punkty) {
                punkt.setTrasaId(trasaId);
                zapiszPunktIRelacje(punkt);
            }

            mainHandler.post(runnable);
        });
    }

    public void ustawUlubionaAsync(UUID trasaId, boolean ulubiona, Runnable runnable) {
        executorService.execute(() -> {
            trasyDAO.ustawUlubiona(trasaId.toString(), ulubiona);
            mainHandler.post(runnable);
        });
    }

    public void dodajTreningAsync(TreningEntity trening, Runnable runnable) {
        executorService.execute(() -> {
            trening.uzytkownikId = aktualnyUzytkownikId();
            trasyDAO.dodajTrening(trening);

            if (runnable != null) {
                mainHandler.post(runnable);
            }
        });
    }

    public void getTreningiAsync(Callback<List<TreningEntity>> callback) {
        executorService.execute(() -> {
            List<TreningEntity> treningi = trasyDAO.getTreningi(aktualnyUzytkownikId());
            mainHandler.post(() -> callback.onResult(treningi));
        });
    }

    public void getTreningiOdAsync(long odMs, Callback<List<TreningEntity>> callback) {
        executorService.execute(() -> {
            List<TreningEntity> treningi = trasyDAO.getTreningiOd(aktualnyUzytkownikId(), odMs);
            mainHandler.post(() -> callback.onResult(treningi));
        });
    }

    public void usunWszystkieTreningiAsync(Runnable runnable) {
        executorService.execute(() -> {
            trasyDAO.usunWszystkieTreningi(aktualnyUzytkownikId());

            if (runnable != null) {
                mainHandler.post(runnable);
            }
        });
    }

    public void close() {
        executorService.shutdown();
    }

    private String aktualnyUzytkownikId() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("uzytkownik_zalogowany", false)
                ? prefs.getString("uzytkownik_id", "")
                : "";
    }

    private void zapiszPunktIRelacje(PunktTrasy punkt) {
        if (punkt == null) {
            return;
        }

        String kategoria = tekstLubDomyslny(punkt.getKategoria(), "Inne");
        String kategoriaUuid = trasyDAO.znajdzKategorie(kategoria);
        if (kategoriaUuid == null || kategoriaUuid.trim().isEmpty()) {
            kategoriaUuid = uuidKategorii(kategoria);
            trasyDAO.dodajKategorie(new KategoriaPunktuEntity(kategoriaUuid, kategoria));
        }

        String punktUuid = trasyDAO.znajdzPunkt(
                tekstLubDomyslny(punkt.getNazwa(), "Punkt"),
                punkt.getLatitude(),
                punkt.getLongitude()
        );

        if (punktUuid == null || punktUuid.trim().isEmpty()) {
            punktUuid = punkt.getnId().toString();
            punkt.setnId(UUID.fromString(punktUuid));
            trasyDAO.dodajPunkt(PunktTrasyMapper.toEntity(punkt, kategoriaUuid));
        } else {
            punkt.setnId(UUID.fromString(punktUuid));
        }

        if (punkt.getTrasaId() != null) {
            trasyDAO.dodajPunktTrasy(new TrasaPunktEntity(
                    punkt.getTrasaId().toString(),
                    punktUuid,
                    punkt.getKolejnosc()
            ));
        }
    }

    private String uuidKategorii(String nazwa) {
        String klucz = "kategoria-" + tekstLubDomyslny(nazwa, "Inne").toLowerCase(Locale.ROOT).trim();
        return UUID.nameUUIDFromBytes(klucz.getBytes()).toString();
    }

    private String tekstLubDomyslny(String tekst, String domyslny) {
        return tekst == null || tekst.trim().isEmpty() ? domyslny : tekst.trim();
    }
}
