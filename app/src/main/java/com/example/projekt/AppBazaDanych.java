package com.example.projekt;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

@Database(
        entities = {
                TrasaEntity.class,
                PunktTrasyEntity.class,
                KategoriaPunktuEntity.class,
                TrasaPunktEntity.class,
                TreningEntity.class
        },
        version = 7,
        exportSchema = false
)
public abstract class AppBazaDanych extends RoomDatabase {

    private static AppBazaDanych instance;

    public abstract TrasyRoomDAO trasyRoomDAO();

    public static synchronized AppBazaDanych getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppBazaDanych.class,
                            "BazaTrasRoom.db"
                    )
                    .fallbackToDestructiveMigration(false)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(callback)
                    .build();
        }

        return instance;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `Treningi` (`TreningUuid` TEXT NOT NULL, `TrasaUuid` TEXT, `NazwaTrasy` TEXT, `DataStartu` INTEGER NOT NULL, `DataKonca` INTEGER NOT NULL, `CzasMin` INTEGER NOT NULL, `DystansKm` REAL NOT NULL, `PrzewyzszenieM` INTEGER NOT NULL, `Kalorie` INTEGER NOT NULL, PRIMARY KEY(`TreningUuid`))");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `Trasy` ADD COLUMN `UzytkownikId` TEXT");
            db.execSQL("ALTER TABLE `Treningi` ADD COLUMN `UzytkownikId` TEXT");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            dodajKrakowskieTrasyDev(db);
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            dodajPunktyDevKrakowStare(db);
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            usunDuplikatyPunktowBiblioteki(db);
            dodajPunktyDevKrakowStare(db);
            usunDuplikatyPunktowBiblioteki(db);
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `KategoriePunktow` (`KategoriaUuid` TEXT NOT NULL, `Nazwa` TEXT, PRIMARY KEY(`KategoriaUuid`))");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_KategoriePunktow_Nazwa` ON `KategoriePunktow` (`Nazwa`)");
            db.execSQL("INSERT OR IGNORE INTO `KategoriePunktow` (`KategoriaUuid`, `Nazwa`) VALUES ('inne', 'Inne')");
            db.execSQL("INSERT OR IGNORE INTO `KategoriePunktow` (`KategoriaUuid`, `Nazwa`) " +
                    "SELECT LOWER(TRIM(COALESCE(NULLIF(`Kategoria`, ''), 'Inne'))), COALESCE(NULLIF(TRIM(`Kategoria`), ''), 'Inne') " +
                    "FROM `PunktyTrasy`");

            db.execSQL("CREATE TABLE IF NOT EXISTS `Punkty` (`PunktUuid` TEXT NOT NULL, `KategoriaUuid` TEXT, `Nazwa` TEXT, `Opis` TEXT, `Latitude` REAL NOT NULL, `Longitude` REAL NOT NULL, PRIMARY KEY(`PunktUuid`), FOREIGN KEY(`KategoriaUuid`) REFERENCES `KategoriePunktow`(`KategoriaUuid`) ON UPDATE NO ACTION ON DELETE SET NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Punkty_KategoriaUuid` ON `Punkty` (`KategoriaUuid`)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Punkty_Nazwa_Latitude_Longitude` ON `Punkty` (`Nazwa`, `Latitude`, `Longitude`)");
            db.execSQL("INSERT OR IGNORE INTO `Punkty` (`PunktUuid`, `KategoriaUuid`, `Nazwa`, `Opis`, `Latitude`, `Longitude`) " +
                    "SELECT MIN(`PunktUuid`), LOWER(TRIM(COALESCE(NULLIF(`Kategoria`, ''), 'Inne'))), `Nazwa`, `Opis`, `Latitude`, `Longitude` " +
                    "FROM `PunktyTrasy` " +
                    "GROUP BY LOWER(TRIM(`Nazwa`)), ROUND(`Latitude`, 5), ROUND(`Longitude`, 5)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `Trasa_Punkt` (`TrasaUuid` TEXT NOT NULL, `PunktUuid` TEXT NOT NULL, `Kolejnosc` INTEGER NOT NULL, PRIMARY KEY(`TrasaUuid`, `PunktUuid`), FOREIGN KEY(`TrasaUuid`) REFERENCES `Trasy`(`TrasaUuid`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`PunktUuid`) REFERENCES `Punkty`(`PunktUuid`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Trasa_Punkt_TrasaUuid` ON `Trasa_Punkt` (`TrasaUuid`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Trasa_Punkt_PunktUuid` ON `Trasa_Punkt` (`PunktUuid`)");
            db.execSQL("INSERT OR REPLACE INTO `Trasa_Punkt` (`TrasaUuid`, `PunktUuid`, `Kolejnosc`) " +
                    "SELECT pt.`TrasaUuid`, " +
                    "(SELECT MIN(p2.`PunktUuid`) FROM `PunktyTrasy` p2 WHERE LOWER(TRIM(p2.`Nazwa`)) = LOWER(TRIM(pt.`Nazwa`)) AND ROUND(p2.`Latitude`, 5) = ROUND(pt.`Latitude`, 5) AND ROUND(p2.`Longitude`, 5) = ROUND(pt.`Longitude`, 5)), " +
                    "pt.`Kolejnosc` FROM `PunktyTrasy` pt WHERE pt.`TrasaUuid` IS NOT NULL");
            db.execSQL("DROP TABLE IF EXISTS `PunktyTrasy`");

            dodajPunktyDevKrakow(db);
        }
    };

    private static final RoomDatabase.Callback callback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            Executors.newSingleThreadExecutor().execute(() -> {
                AppBazaDanych baza = instance;
                if (baza != null) {
                    dodajTrasyStartowe(baza.trasyRoomDAO());
                }
            });
        }
    };

    private static void dodajTrasyStartowe(TrasyRoomDAO dao) {
        String krakow = "11111111-1111-1111-1111-111111111111";
        String ojcow = "22222222-2222-2222-2222-222222222222";
        String tatry = "33333333-3333-3333-3333-333333333333";
        String muzea = "44444444-4444-4444-4444-444444444444";
        String zabytki = "55555555-5555-5555-5555-555555555555";
        String uczelnie = "66666666-6666-6666-6666-666666666666";
        String galerie = "77777777-7777-7777-7777-777777777777";
        String bulwary = "88888888-8888-8888-8888-888888888888";

        dodajTraseDev(dao, krakow, "Kraków: Stare Miasto",
                "Krótka miejska trasa przez najważniejsze punkty historycznego centrum.",
                "Kraków", 4.8, 35, 85, "Łatwa", "Miejska");
        dodajTraseDev(dao, ojcow, "Ojców: Dolina Prądnika",
                "Spacer krajobrazowy z punktami widokowymi i zamkiem w Ojcowie.",
                "Ojców", 7.2, 180, 150, "Średnia", "Przyrodnicza");
        dodajTraseDev(dao, tatry, "Tatry: Rusinowa Polana",
                "Widokowa trasa górska dla osób chcących zacząć przygodę z Tatrami.",
                "Tatry", 9.5, 390, 210, "Średnia", "Górska");

        dodajKrakowskieTrasyDev(dao, muzea, zabytki, uczelnie, galerie, bulwary);
        dodajPunktyDevKrakow(dao);

        dodajPunkt(dao, krakow, "Rynek Główny", "Start trasy", "Zabytek", 50.061947, 19.936856, 1);
        dodajPunkt(dao, krakow, "Sukiennice", "Punkt centralny rynku", "Zabytek", 50.061694, 19.937342, 2);
        dodajPunkt(dao, krakow, "Wawel", "Zamek Królewski", "Zabytek", 50.054018, 19.935203, 3);

        dodajPunkt(dao, ojcow, "Brama Krakowska", "Charakterystyczna formacja skalna", "Przyroda", 50.203114, 19.830462, 1);
        dodajPunkt(dao, ojcow, "Zamek w Ojcowie", "Ruiny zamku", "Zabytek", 50.210591, 19.829394, 2);
        dodajPunkt(dao, ojcow, "Źródełko Miłości", "Punkt odpoczynku", "Przyroda", 50.205631, 19.828931, 3);

        dodajPunkt(dao, tatry, "Wierch Poroniec", "Początek trasy", "Start", 49.269548, 20.092957, 1);
        dodajPunkt(dao, tatry, "Rusinowa Polana", "Widok na Tatry Wysokie", "Punkt widokowy", 49.255963, 20.093705, 2);
        dodajPunkt(dao, tatry, "Gęsia Szyja", "Opcjonalne podejście", "Szczyt", 49.250731, 20.088168, 3);
    }

    private static void dodajKrakowskieTrasyDev(TrasyRoomDAO dao, String muzea, String zabytki,
                                                String uczelnie, String galerie, String bulwary) {
        dodajTraseDev(dao, muzea, "Kraków: Muzea krakowskie",
                "Trasa przez najciekawsze muzea w centrum Krakowa.",
                "Kraków", 5.6, 40, 115, "Łatwa", "Kulturalna");
        dodajTraseDev(dao, zabytki, "Kraków: Zabytki Krakowa",
                "Klasyczny spacer przez Wawel, Rynek, kościoły i historyczne ulice.",
                "Kraków", 6.1, 55, 130, "Łatwa", "Historyczna");
        dodajTraseDev(dao, uczelnie, "Kraków: Uczelnie",
                "Miejska trasa łącząca kampusy AGH, UJ i Politechniki Krakowskiej.",
                "Kraków", 7.4, 65, 145, "Łatwa", "Miejska");
        dodajTraseDev(dao, galerie, "Kraków: Galerie handlowe",
                "Dłuższa trasa miejska po znanych galeriach i centrach handlowych Krakowa.",
                "Kraków", 22.0, 120, 300, "Średnia", "Miejska");
        dodajTraseDev(dao, bulwary, "Kraków: Bulwary Wiślane",
                "Spokojna trasa spacerowa wzdłuż Wisły z widokami na Wawel i Kazimierz.",
                "Kraków", 8.2, 30, 150, "Łatwa", "Spacerowa");

        dodajPunktyKrakowskichTras(dao, muzea, zabytki, uczelnie, galerie, bulwary);
    }

    private static void dodajPunktyKrakowskichTras(TrasyRoomDAO dao, String muzea, String zabytki,
                                                   String uczelnie, String galerie, String bulwary) {
        dodajPunkt(dao, muzea, "Muzeum Narodowe", "Gmach Główny", "Muzeum", 50.060611, 19.923833, 1);
        dodajPunkt(dao, muzea, "Muzeum Czartoryskich", "Sztuka i historia", "Muzeum", 50.064890, 19.939450, 2);
        dodajPunkt(dao, muzea, "Podziemia Rynku", "Muzeum pod Rynkiem Głównym", "Muzeum", 50.061730, 19.937250, 3);
        dodajPunkt(dao, muzea, "MOCAK", "Muzeum sztuki współczesnej", "Muzeum", 50.047793, 19.961037, 4);

        dodajPunkt(dao, zabytki, "Wawel", "Zamek Królewski i katedra", "Zabytek", 50.054018, 19.935203, 1);
        dodajPunkt(dao, zabytki, "Kościół Mariacki", "Jeden z symboli Krakowa", "Zabytek", 50.061675, 19.939375, 2);
        dodajPunkt(dao, zabytki, "Sukiennice", "Historyczna hala targowa", "Zabytek", 50.061694, 19.937342, 3);
        dodajPunkt(dao, zabytki, "Barbakan", "Element dawnych fortyfikacji", "Zabytek", 50.065627, 19.941657, 4);
        dodajPunkt(dao, zabytki, "Brama Floriańska", "Wejście na Drogę Królewską", "Zabytek", 50.064950, 19.941333, 5);

        dodajPunkt(dao, uczelnie, "AGH", "Akademia Górniczo-Hutnicza", "Uczelnia", 50.064707, 19.923515, 1);
        dodajPunkt(dao, uczelnie, "Uniwersytet Jagielloński", "Collegium Novum", "Uczelnia", 50.060878, 19.932362, 2);
        dodajPunkt(dao, uczelnie, "Politechnika Krakowska", "Kampus przy Warszawskiej", "Uczelnia", 50.070150, 19.944470, 3);
        dodajPunkt(dao, uczelnie, "Uniwersytet Ekonomiczny", "Kampus UEK", "Uczelnia", 50.068920, 19.955370, 4);

        dodajPunkt(dao, galerie, "Galeria Krakowska", "Centrum przy dworcu", "Galeria", 50.067971, 19.945038, 1);
        dodajPunkt(dao, galerie, "Galeria Kazimierz", "Centrum na Kazimierzu", "Galeria", 50.051728, 19.955057, 2);
        dodajPunkt(dao, galerie, "Bonarka", "Bonarka City Center", "Galeria", 50.029420, 19.949290, 3);
        dodajPunkt(dao, galerie, "Zakopianka", "Centrum przy Zakopiańskiej", "Galeria", 50.009930, 19.928900, 4);
        dodajPunkt(dao, galerie, "M1", "Centrum przy al. Pokoju", "Galeria", 50.065490, 20.004980, 5);
        dodajPunkt(dao, galerie, "Serenada", "Centrum w północnej części miasta", "Galeria", 50.088620, 19.983580, 6);
        dodajPunkt(dao, galerie, "Galeria Bronowice", "Centrum w Bronowicach", "Galeria", 50.089160, 19.897490, 7);

        dodajPunkt(dao, bulwary, "Bulwar Czerwieński", "Widok na Wawel", "Bulwar", 50.052690, 19.934360, 1);
        dodajPunkt(dao, bulwary, "Most Dębnicki", "Przeprawa z widokiem na Wisłę", "Most", 50.055339, 19.927870, 2);
        dodajPunkt(dao, bulwary, "Kładka Bernatka", "Połączenie Kazimierza i Podgórza", "Most", 50.046248, 19.948868, 3);
        dodajPunkt(dao, bulwary, "Bulwar Kurlandzki", "Spacer wzdłuż Wisły", "Bulwar", 50.048160, 19.951930, 4);
        dodajPunkt(dao, bulwary, "Stopień Dąbie", "Spokojny odcinek nad rzeką", "Bulwar", 50.054760, 19.982820, 5);
    }

    private static void dodajKrakowskieTrasyDev(SupportSQLiteDatabase db) {
        String muzea = "44444444-4444-4444-4444-444444444444";
        String zabytki = "55555555-5555-5555-5555-555555555555";
        String uczelnie = "66666666-6666-6666-6666-666666666666";
        String galerie = "77777777-7777-7777-7777-777777777777";
        String bulwary = "88888888-8888-8888-8888-888888888888";

        dodajTraseDev(db, muzea, "Kraków: Muzea krakowskie", "Trasa przez najciekawsze muzea w centrum Krakowa.", "Kraków", 5.6, 40, 115, "Łatwa", "Kulturalna");
        dodajTraseDev(db, zabytki, "Kraków: Zabytki Krakowa", "Klasyczny spacer przez Wawel, Rynek, kościoły i historyczne ulice.", "Kraków", 6.1, 55, 130, "Łatwa", "Historyczna");
        dodajTraseDev(db, uczelnie, "Kraków: Uczelnie", "Miejska trasa łącząca kampusy AGH, UJ i Politechniki Krakowskiej.", "Kraków", 7.4, 65, 145, "Łatwa", "Miejska");
        dodajTraseDev(db, galerie, "Kraków: Galerie handlowe", "Dłuższa trasa miejska po znanych galeriach i centrach handlowych Krakowa.", "Kraków", 22.0, 120, 300, "Średnia", "Miejska");
        dodajTraseDev(db, bulwary, "Kraków: Bulwary Wiślane", "Spokojna trasa spacerowa wzdłuż Wisły z widokami na Wawel i Kazimierz.", "Kraków", 8.2, 30, 150, "Łatwa", "Spacerowa");

        dodajPunktyKrakowskichTras(db, muzea, zabytki, uczelnie, galerie, bulwary);
    }

    private static void dodajPunktyKrakowskichTras(SupportSQLiteDatabase db, String muzea, String zabytki,
                                                   String uczelnie, String galerie, String bulwary) {
        dodajPunkt(db, muzea, "Muzeum Narodowe", "Gmach Główny", "Muzeum", 50.060611, 19.923833, 1);
        dodajPunkt(db, muzea, "Muzeum Czartoryskich", "Sztuka i historia", "Muzeum", 50.064890, 19.939450, 2);
        dodajPunkt(db, muzea, "Podziemia Rynku", "Muzeum pod Rynkiem Głównym", "Muzeum", 50.061730, 19.937250, 3);
        dodajPunkt(db, muzea, "MOCAK", "Muzeum sztuki współczesnej", "Muzeum", 50.047793, 19.961037, 4);

        dodajPunkt(db, zabytki, "Wawel", "Zamek Królewski i katedra", "Zabytek", 50.054018, 19.935203, 1);
        dodajPunkt(db, zabytki, "Kościół Mariacki", "Jeden z symboli Krakowa", "Zabytek", 50.061675, 19.939375, 2);
        dodajPunkt(db, zabytki, "Sukiennice", "Historyczna hala targowa", "Zabytek", 50.061694, 19.937342, 3);
        dodajPunkt(db, zabytki, "Barbakan", "Element dawnych fortyfikacji", "Zabytek", 50.065627, 19.941657, 4);
        dodajPunkt(db, zabytki, "Brama Floriańska", "Wejście na Drogę Królewską", "Zabytek", 50.064950, 19.941333, 5);

        dodajPunkt(db, uczelnie, "AGH", "Akademia Górniczo-Hutnicza", "Uczelnia", 50.064707, 19.923515, 1);
        dodajPunkt(db, uczelnie, "Uniwersytet Jagielloński", "Collegium Novum", "Uczelnia", 50.060878, 19.932362, 2);
        dodajPunkt(db, uczelnie, "Politechnika Krakowska", "Kampus przy Warszawskiej", "Uczelnia", 50.070150, 19.944470, 3);
        dodajPunkt(db, uczelnie, "Uniwersytet Ekonomiczny", "Kampus UEK", "Uczelnia", 50.068920, 19.955370, 4);

        dodajPunkt(db, galerie, "Galeria Krakowska", "Centrum przy dworcu", "Galeria", 50.067971, 19.945038, 1);
        dodajPunkt(db, galerie, "Galeria Kazimierz", "Centrum na Kazimierzu", "Galeria", 50.051728, 19.955057, 2);
        dodajPunkt(db, galerie, "Bonarka", "Bonarka City Center", "Galeria", 50.029420, 19.949290, 3);
        dodajPunkt(db, galerie, "Zakopianka", "Centrum przy Zakopiańskiej", "Galeria", 50.009930, 19.928900, 4);
        dodajPunkt(db, galerie, "M1", "Centrum przy al. Pokoju", "Galeria", 50.065490, 20.004980, 5);
        dodajPunkt(db, galerie, "Serenada", "Centrum w północnej części miasta", "Galeria", 50.088620, 19.983580, 6);
        dodajPunkt(db, galerie, "Galeria Bronowice", "Centrum w Bronowicach", "Galeria", 50.089160, 19.897490, 7);

        dodajPunkt(db, bulwary, "Bulwar Czerwieński", "Widok na Wawel", "Bulwar", 50.052690, 19.934360, 1);
        dodajPunkt(db, bulwary, "Most Dębnicki", "Przeprawa z widokiem na Wisłę", "Most", 50.055339, 19.927870, 2);
        dodajPunkt(db, bulwary, "Kładka Bernatka", "Połączenie Kazimierza i Podgórza", "Most", 50.046248, 19.948868, 3);
        dodajPunkt(db, bulwary, "Bulwar Kurlandzki", "Spacer wzdłuż Wisły", "Bulwar", 50.048160, 19.951930, 4);
        dodajPunkt(db, bulwary, "Stopień Dąbie", "Spokojny odcinek nad rzeką", "Bulwar", 50.054760, 19.982820, 5);
    }

    private static void dodajTraseDev(TrasyRoomDAO dao, String id, String nazwa, String opis,
                                      String region, double dystansKm, int przewyzszenieM,
                                      int czasMin, String poziom, String typ) {
        dao.dodajTrase(new TrasaEntity(
                id,
                nazwa,
                opis,
                region,
                dystansKm,
                przewyzszenieM,
                czasMin,
                poziom,
                typ,
                true,
                false,
                null
        ));
    }

    private static void dodajTraseDev(SupportSQLiteDatabase db, String id, String nazwa, String opis,
                                      String region, double dystansKm, int przewyzszenieM,
                                      int czasMin, String poziom, String typ) {
        db.execSQL(
                "INSERT OR IGNORE INTO `Trasy` (`TrasaUuid`, `Nazwa`, `Opis`, `Region`, `DystansKm`, `PrzewyzszenieM`, `CzasMin`, `Poziom`, `Typ`, `Dev`, `Ulubiona`, `UzytkownikId`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, NULL)",
                new Object[]{id, nazwa, opis, region, dystansKm, przewyzszenieM, czasMin, poziom, typ}
        );
    }

    private static void dodajPunktyDevKrakow(TrasyRoomDAO dao) {
        dodajPunktDev(dao, "Wawel", "Zamek Królewski i katedra", "Zabytek", 50.054018, 19.935203);
        dodajPunktDev(dao, "AGH A0", "Gmach główny Akademii Górniczo-Hutniczej", "Użytkowe", 50.064707, 19.923515);
        dodajPunktDev(dao, "Sukiennice", "Historyczna hala targowa na Rynku Głównym", "Zabytek", 50.061694, 19.937342);
        dodajPunktDev(dao, "Kościół Mariacki", "Bazylika Mariacka przy Rynku Głównym", "Sakralne", 50.061675, 19.939375);
        dodajPunktDev(dao, "Dworzec Główny Zachód", "Zachodnia część dworca kolejowego", "Użytkowe", 50.067460, 19.944360);
        dodajPunktDev(dao, "Bonarka", "Bonarka City Center", "Miejsce handlowe", 50.029420, 19.949290);
        dodajPunktDev(dao, "Galeria Krakowska", "Centrum handlowe przy dworcu", "Miejsce handlowe", 50.067971, 19.945038);
        dodajPunktDev(dao, "Most Grunwaldzki", "Most nad Wisłą przy centrum Krakowa", "Użytkowe", 50.049450, 19.934660);
        dodajPunktDev(dao, "Rondo Mogilskie", "Duży węzeł komunikacyjny", "Użytkowe", 50.065300, 19.958200);
        dodajPunktDev(dao, "Rynek Główny", "Centralny plac Starego Miasta", "Zabytek", 50.061947, 19.936856);
        dodajPunktDev(dao, "Barbakan", "Element dawnych fortyfikacji Krakowa", "Zabytek", 50.065627, 19.941657);
        dodajPunktDev(dao, "Kładka Bernatka", "Kładka pieszo-rowerowa między Kazimierzem i Podgórzem", "Użytkowe", 50.046248, 19.948868);
    }

    private static void dodajPunktyDevKrakow(SupportSQLiteDatabase db) {
        dodajPunktDev(db, "Wawel", "Zamek Królewski i katedra", "Zabytek", 50.054018, 19.935203);
        dodajPunktDev(db, "AGH A0", "Gmach główny Akademii Górniczo-Hutniczej", "Użytkowe", 50.064707, 19.923515);
        dodajPunktDev(db, "Sukiennice", "Historyczna hala targowa na Rynku Głównym", "Zabytek", 50.061694, 19.937342);
        dodajPunktDev(db, "Kościół Mariacki", "Bazylika Mariacka przy Rynku Głównym", "Sakralne", 50.061675, 19.939375);
        dodajPunktDev(db, "Dworzec Główny Zachód", "Zachodnia część dworca kolejowego", "Użytkowe", 50.067460, 19.944360);
        dodajPunktDev(db, "Bonarka", "Bonarka City Center", "Miejsce handlowe", 50.029420, 19.949290);
        dodajPunktDev(db, "Galeria Krakowska", "Centrum handlowe przy dworcu", "Miejsce handlowe", 50.067971, 19.945038);
        dodajPunktDev(db, "Most Grunwaldzki", "Most nad Wisłą przy centrum Krakowa", "Użytkowe", 50.049450, 19.934660);
        dodajPunktDev(db, "Rondo Mogilskie", "Duży węzeł komunikacyjny", "Użytkowe", 50.065300, 19.958200);
        dodajPunktDev(db, "Rynek Główny", "Centralny plac Starego Miasta", "Zabytek", 50.061947, 19.936856);
        dodajPunktDev(db, "Barbakan", "Element dawnych fortyfikacji Krakowa", "Zabytek", 50.065627, 19.941657);
        dodajPunktDev(db, "Kładka Bernatka", "Kładka pieszo-rowerowa między Kazimierzem i Podgórzem", "Użytkowe", 50.046248, 19.948868);
    }

    private static void dodajPunktyDevKrakowStare(SupportSQLiteDatabase db) {
        dodajPunktDevStary(db, "Wawel", "Zamek Królewski i katedra", "Zabytek", 50.054018, 19.935203);
        dodajPunktDevStary(db, "AGH A0", "Gmach główny Akademii Górniczo-Hutniczej", "Użytkowe", 50.064707, 19.923515);
        dodajPunktDevStary(db, "Sukiennice", "Historyczna hala targowa na Rynku Głównym", "Zabytek", 50.061694, 19.937342);
        dodajPunktDevStary(db, "Kościół Mariacki", "Bazylika Mariacka przy Rynku Głównym", "Sakralne", 50.061675, 19.939375);
        dodajPunktDevStary(db, "Dworzec Główny Zachód", "Zachodnia część dworca kolejowego", "Użytkowe", 50.067460, 19.944360);
        dodajPunktDevStary(db, "Bonarka", "Bonarka City Center", "Miejsce handlowe", 50.029420, 19.949290);
        dodajPunktDevStary(db, "Galeria Krakowska", "Centrum handlowe przy dworcu", "Miejsce handlowe", 50.067971, 19.945038);
        dodajPunktDevStary(db, "Most Grunwaldzki", "Most nad Wisłą przy centrum Krakowa", "Użytkowe", 50.049450, 19.934660);
        dodajPunktDevStary(db, "Rondo Mogilskie", "Duży węzeł komunikacyjny", "Użytkowe", 50.065300, 19.958200);
        dodajPunktDevStary(db, "Rynek Główny", "Centralny plac Starego Miasta", "Zabytek", 50.061947, 19.936856);
        dodajPunktDevStary(db, "Barbakan", "Element dawnych fortyfikacji Krakowa", "Zabytek", 50.065627, 19.941657);
        dodajPunktDevStary(db, "Kładka Bernatka", "Kładka pieszo-rowerowa między Kazimierzem i Podgórzem", "Użytkowe", 50.046248, 19.948868);
    }

    private static void dodajPunktDev(TrasyRoomDAO dao, String nazwa, String opis,
                                      String kategoria, double lat, double lon) {
        String punktId = UUID.nameUUIDFromBytes(("dev-punkt-krakow-" + nazwa).getBytes()).toString();
        String kategoriaId = uuidKategorii(kategoria);
        dao.dodajKategorie(new KategoriaPunktuEntity(kategoriaId, kategoria));
        dao.dodajPunkt(new PunktTrasyEntity(
                punktId,
                kategoriaId,
                nazwa,
                opis,
                lat,
                lon
        ));
    }

    private static void dodajPunktDev(SupportSQLiteDatabase db, String nazwa, String opis,
                                      String kategoria, double lat, double lon) {
        String punktId = UUID.nameUUIDFromBytes(("dev-punkt-krakow-" + nazwa).getBytes()).toString();
        String kategoriaId = uuidKategorii(kategoria);
        dodajKategorie(db, kategoriaId, kategoria);
        db.execSQL(
                "INSERT OR IGNORE INTO `Punkty` (`PunktUuid`, `KategoriaUuid`, `Nazwa`, `Opis`, `Latitude`, `Longitude`) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{punktId, kategoriaId, nazwa, opis, lat, lon}
        );
    }

    private static void dodajPunktDevStary(SupportSQLiteDatabase db, String nazwa, String opis,
                                           String kategoria, double lat, double lon) {
        db.execSQL(
                "INSERT OR IGNORE INTO `PunktyTrasy` (`PunktUuid`, `TrasaUuid`, `Nazwa`, `Opis`, `Kategoria`, `Latitude`, `Longitude`, `Kolejnosc`) VALUES (?, NULL, ?, ?, ?, ?, ?, 0)",
                new Object[]{UUID.nameUUIDFromBytes(("dev-punkt-krakow-" + nazwa).getBytes()).toString(), nazwa, opis, kategoria, lat, lon}
        );
    }

    private static void usunDuplikatyPunktowBiblioteki(SupportSQLiteDatabase db) {
        db.execSQL(
                "DELETE FROM `PunktyTrasy` " +
                        "WHERE `TrasaUuid` IS NULL " +
                        "AND `PunktUuid` NOT IN (" +
                        "SELECT MIN(`PunktUuid`) FROM `PunktyTrasy` " +
                        "WHERE `TrasaUuid` IS NULL " +
                        "GROUP BY LOWER(TRIM(`Nazwa`)), ROUND(`Latitude`, 5), ROUND(`Longitude`, 5)" +
                        ")"
        );
    }

    private static void dodajPunkt(TrasyRoomDAO dao, String trasaId, String nazwa,
                                   String opis, String kategoria,
                                   double lat, double lon, int kolejnosc) {
        String punktId = UUID.nameUUIDFromBytes(("punkt-" + nazwa + "-" + lat + "-" + lon).getBytes()).toString();
        String kategoriaId = uuidKategorii(kategoria);
        dao.dodajKategorie(new KategoriaPunktuEntity(kategoriaId, kategoria));
        dao.dodajPunkt(new PunktTrasyEntity(
                punktId,
                kategoriaId,
                nazwa,
                opis,
                lat,
                lon
        ));
        dao.dodajPunktTrasy(new TrasaPunktEntity(trasaId, punktId, kolejnosc));
    }

    private static void dodajPunkt(SupportSQLiteDatabase db, String trasaId, String nazwa,
                                   String opis, String kategoria,
                                   double lat, double lon, int kolejnosc) {
        String punktId = UUID.nameUUIDFromBytes(("punkt-" + nazwa + "-" + lat + "-" + lon).getBytes()).toString();
        String kategoriaId = uuidKategorii(kategoria);
        dodajKategorie(db, kategoriaId, kategoria);
        db.execSQL(
                "INSERT OR IGNORE INTO `Punkty` (`PunktUuid`, `KategoriaUuid`, `Nazwa`, `Opis`, `Latitude`, `Longitude`) VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{punktId, kategoriaId, nazwa, opis, lat, lon}
        );
        db.execSQL(
                "INSERT OR REPLACE INTO `Trasa_Punkt` (`TrasaUuid`, `PunktUuid`, `Kolejnosc`) VALUES (?, ?, ?)",
                new Object[]{trasaId, punktId, kolejnosc}
        );
    }

    private static void dodajKategorie(SupportSQLiteDatabase db, String id, String nazwa) {
        db.execSQL(
                "INSERT OR IGNORE INTO `KategoriePunktow` (`KategoriaUuid`, `Nazwa`) VALUES (?, ?)",
                new Object[]{id, nazwa}
        );
    }

    private static String uuidKategorii(String nazwa) {
        String wartosc = nazwa == null || nazwa.trim().isEmpty() ? "Inne" : nazwa.trim();
        return UUID.nameUUIDFromBytes(("kategoria-" + wartosc.toLowerCase(Locale.ROOT)).getBytes()).toString();
    }
}
