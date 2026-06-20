package com.example.projekt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrasaMapper {

    public static Trasa toModel(TrasaEntity entity) {
        if (entity == null) {
            return null;
        }

        Trasa trasa = new Trasa();

        trasa.setnId(UUID.fromString(entity.uuid));
        trasa.setNazwa(entity.nazwa);
        trasa.setOpis(entity.opis);
        trasa.setRegion(entity.region);
        trasa.setDystansKm(entity.dystansKm);
        trasa.setPrzewyzszenieM(entity.przewyzszenieM);
        trasa.setCzasMin(entity.czasMin);
        trasa.setPoziomTrudnosci(entity.poziom);
        trasa.setTyp(entity.typ);
        trasa.setDev(entity.dev);
        trasa.setUlubiona(entity.ulubiona);
        trasa.setUzytkownikId(entity.uzytkownikId);

        return trasa;
    }

    public static List<Trasa> toModelList(List<TrasaEntity> entities) {
        List<Trasa> trasy = new ArrayList<>();

        if (entities == null) {
            return trasy;
        }

        for (TrasaEntity entity : entities) {
            try {
                Trasa model = toModel(entity);
                if (model != null) {
                    trasy.add(model);
                }
            } catch (IllegalArgumentException e) {
                android.util.Log.e("TrasaMapper", "Pomijam uszkodzony wiersz: " + entity.uuid, e);
            }
        }

        return trasy;
    }

    public static TrasaEntity toEntity(Trasa trasa) {
        if (trasa == null) {
            return null;
        }

        return new TrasaEntity(
                trasa.getnId().toString(),
                trasa.getNazwa(),
                trasa.getOpis(),
                trasa.getRegion(),
                trasa.getDystansKm(),
                trasa.getPrzewyzszenieM(),
                trasa.getCzasMin(),
                trasa.getPoziomTrudnosci(),
                trasa.getTyp(),
                trasa.isDev(),
                trasa.isUlubiona(),
                trasa.getUzytkownikId()
        );
    }
}
