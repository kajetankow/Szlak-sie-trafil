package com.example.projekt;

import java.util.UUID;

public class Trasa {
    private UUID nId;
    private String nazwa;
    private String opis;
    private String region;
    private double dystansKm;
    private int przewyzszenieM;
    private int czasMin;
    private String poziomTrudnosci;
    private String typ;
    private boolean dev;
    private boolean ulubiona;
    private String uzytkownikId;

    public Trasa() {
        nId = UUID.randomUUID();
        nazwa = "";
        opis = "";
        region = "";
        dystansKm = 0.0;
        przewyzszenieM = 0;
        czasMin = 0;
        poziomTrudnosci = "Łatwa";
        typ = "Spacer";
        dev = false;
        ulubiona = false;
        uzytkownikId = "";
    }

    public UUID getnId() { return nId; }
    public void setnId(UUID nId) { this.nId = nId; }
    public String getNazwa() { return nazwa; }
    public void setNazwa(String nazwa) { this.nazwa = nazwa; }
    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public double getDystansKm() { return dystansKm; }
    public void setDystansKm(double dystansKm) { this.dystansKm = dystansKm; }
    public int getPrzewyzszenieM() { return przewyzszenieM; }
    public void setPrzewyzszenieM(int przewyzszenieM) { this.przewyzszenieM = przewyzszenieM; }
    public int getCzasMin() { return czasMin; }
    public void setCzasMin(int czasMin) { this.czasMin = czasMin; }
    public String getPoziomTrudnosci() { return poziomTrudnosci; }
    public void setPoziomTrudnosci(String poziomTrudnosci) { this.poziomTrudnosci = poziomTrudnosci; }
    public String getTyp() { return typ; }
    public void setTyp(String typ) { this.typ = typ; }
    public boolean isDev() { return dev; }
    public void setDev(boolean dev) { this.dev = dev; }
    public boolean isUlubiona() { return ulubiona; }
    public void setUlubiona(boolean ulubiona) { this.ulubiona = ulubiona; }
    public String getUzytkownikId() { return uzytkownikId; }
    public void setUzytkownikId(String uzytkownikId) { this.uzytkownikId = uzytkownikId; }

    public String getCzasTekst() {
        int godziny = czasMin / 60;
        int minuty = czasMin % 60;
        if (godziny == 0) {
            return minuty + " min";
        }
        return godziny + " h " + minuty + " min";
    }
}
