package com.example.projekt;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.UUID;

public class TrasyViewModel extends AndroidViewModel {

    private final TrasyRoomRepository trasyRepository;

    private final MutableLiveData<List<Trasa>> trasyLive = new MutableLiveData<>();
    private final MutableLiveData<List<PunktTrasy>> punktyTrasyLive = new MutableLiveData<>();
    private final MutableLiveData<List<PunktTrasy>> punktyLive = new MutableLiveData<>();
    private final MutableLiveData<List<TreningEntity>> treningiLive = new MutableLiveData<>();

    public TrasyViewModel(@NonNull Application application) {
        super(application);
        trasyRepository = new TrasyRoomRepository(application);
        pobierzTrasy();
    }

    public LiveData<List<Trasa>> getTrasy() {
        return trasyLive;
    }

    public LiveData<List<PunktTrasy>> getPunktyTrasy() {
        return punktyTrasyLive;
    }

    public LiveData<List<PunktTrasy>> getPunkty() {
        return punktyLive;
    }

    public LiveData<List<TreningEntity>> getTreningi() {
        return treningiLive;
    }

    public void pobierzTrasy() {
        trasyRepository.getTrasyAsync(trasyLive::setValue);
    }

    public void pobierzTrase(UUID trasaId, Callback<Trasa> callback) {
        trasyRepository.getTrasaAsync(trasaId, callback);
    }

    public void pobierzPunktyTrasy(UUID trasaId) {
        trasyRepository.getPunktyTrasyAsync(trasaId, punktyTrasyLive::setValue);
    }

    public void pobierzPunkty() {
        trasyRepository.getPunktyAsync(punktyLive::setValue);
    }

    public void szukajPunkty(String fraza) {
        trasyRepository.szukajPunktyAsync(fraza, punktyLive::setValue);
    }

    public void dodajTrase(Trasa trasa) {
        trasyRepository.dodajTraseAsync(trasa, this::pobierzTrasy);
    }

    public void zmienTrase(Trasa trasa) {
        trasyRepository.zmienTraseAsync(trasa, this::pobierzTrasy);
    }

    public void usunTrase(UUID id) {
        trasyRepository.usunTraseAsync(id, this::pobierzTrasy);
    }

    public void dodajPunkt(PunktTrasy punkt) {
        trasyRepository.dodajPunktAsync(punkt, () -> {
            pobierzTrasy();

            if (punkt.getTrasaId() != null) {
                pobierzPunktyTrasy(punkt.getTrasaId());
            }

            pobierzPunkty();
        });
    }

    public void usunPunkt(UUID punktId, UUID trasaId) {
        trasyRepository.usunPunktAsync(punktId, () -> {
            pobierzTrasy();

            if (trasaId != null) {
                pobierzPunktyTrasy(trasaId);
            }
        });
    }

    public void zastapPunktyTrasy(UUID trasaId, List<PunktTrasy> punkty, Runnable runnable) {
        trasyRepository.zastapPunktyTrasyAsync(trasaId, punkty, () -> {
            pobierzTrasy();
            pobierzPunktyTrasy(trasaId);

            if (runnable != null) {
                runnable.run();
            }
        });
    }

    public void ustawUlubiona(UUID trasaId, boolean ulubiona) {
        trasyRepository.ustawUlubionaAsync(trasaId, ulubiona, this::pobierzTrasy);
    }

    public void pobierzTreningi() {
        trasyRepository.getTreningiAsync(treningiLive::setValue);
    }

    public void dodajTrening(TreningEntity trening) {
        trasyRepository.dodajTreningAsync(trening, this::pobierzTreningi);
    }

    public void usunWszystkieTreningi() {
        trasyRepository.usunWszystkieTreningiAsync(this::pobierzTreningi);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        trasyRepository.close();
    }
}
