package com.example.projekt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PunktTrasyMapper {

    public static PunktTrasy toModel(PunktTrasyEntity entity) {
        if (entity == null) {
            return null;
        }

        PunktTrasy punkt = new PunktTrasy();

        punkt.setnId(UUID.fromString(entity.uuid));
        if (entity.trasaUuid != null && !entity.trasaUuid.trim().isEmpty()) {
            punkt.setTrasaId(UUID.fromString(entity.trasaUuid));
        } else {
            punkt.setTrasaId(null);
        }
        punkt.setNazwa(entity.nazwa);
        punkt.setOpis(entity.opis);
        punkt.setKategoria(entity.kategoria);
        punkt.setLatitude(entity.latitude);
        punkt.setLongitude(entity.longitude);
        punkt.setKolejnosc(entity.kolejnosc);

        return punkt;
    }

    public static List<PunktTrasy> toModelList(List<PunktTrasyEntity> entities) {
        List<PunktTrasy> punkty = new ArrayList<>();

        if (entities == null) {
            return punkty;
        }

        for (PunktTrasyEntity entity : entities) {
            punkty.add(toModel(entity));
        }

        return punkty;
    }

    public static PunktTrasyEntity toEntity(PunktTrasy punkt) {
        if (punkt == null) {
            return null;
        }

        return new PunktTrasyEntity(
                punkt.getnId().toString(),
                punkt.getTrasaId() == null ? null : punkt.getTrasaId().toString(),
                punkt.getNazwa(),
                punkt.getOpis(),
                punkt.getKategoria(),
                punkt.getLatitude(),
                punkt.getLongitude(),
                punkt.getKolejnosc()
        );
    }
}
