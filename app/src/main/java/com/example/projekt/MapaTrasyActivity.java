package com.example.projekt;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MapaTrasyActivity extends AppCompatActivity {

    public static final String EXTRA_TRASA_ID = "com.example.pierwzsy_program.TRASA_ID";
    public static final String EXTRA_TRASA_NAZWA = "com.example.pierwzsy_program.TRASA_NAZWA";

    private static final int REQUEST_LOCATION = 5001;
    private static final String PREFS_AKTYWNA_TRASA = "aktywna_trasa_prefs";
    private static final String KEY_AKTYWNA = "aktywna";
    private static final String KEY_TRASA_ID = "trasa_id";
    private static final String KEY_TRASA_NAZWA = "trasa_nazwa";
    private static final String KEY_START_MS = "start_ms";
    private static final String KEY_DYSTANS_GPS_KM = "dystans_gps_km";
    private static final String PREFS_NAME = "tourroute_prefs";

    private MapView mapa;
    private TextView btnPowrot;
    private TextView txtTytulMapy;
    private TextView txtStatusMapy;
    private TextView btnRozpocznijTrase;
    private TextView btnZoomPlus;
    private TextView btnZoomMinus;

    private TrasyViewModel trasyViewModel;
    private LocationManager locationManager;

    private Marker markerLokalizacji;
    private Polyline liniaTrasy;

    private UUID trasaId;
    private String nazwaTrasy;

    private final List<PunktTrasy> punktyTrasy = new ArrayList<>();

    private boolean jestTrasa = false;
    private boolean trasaAktywna = false;
    private long czasStartuMs = 0L;

    private double dystansTrasyKm = 0.0;
    private double dystansGpsKm = 0.0;
    private int szacowanyCzasMin = 0;
    private Location ostatniaLokalizacjaUzytkownika;
    private Location ostatniaLokalizacjaTreningu;

    private final Handler handlerCzasu = new Handler(Looper.getMainLooper());
    private final Runnable odswiezanieCzasu = new Runnable() {
        @Override
        public void run() {
            if (!trasaAktywna) {
                return;
            }

            pokazStatusAktywnejTrasy();
            handlerCzasu.postDelayed(this, 1000);
        }
    };

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            aktualizujDystansGps(location);
            pokazLokalizacjeUzytkownika(location, false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_mapa_trasy);

        przypiszWidoki();
        przygotujMape();
        ustawListenery();

        trasyViewModel = new ViewModelProvider(this).get(TrasyViewModel.class);

        odczytajDaneZIntentu();
        przygotujTrybEkranu();
        uruchomLokalizacje();

        if (jestTrasa) {
            trasyViewModel.getPunktyTrasy().observe(this, this::pokazPunktyNaMapie);
            trasyViewModel.pobierzPunktyTrasy(trasaId);
        }
    }

    private void przypiszWidoki() {
        mapa = findViewById(R.id.mapViewTrasy);
        btnPowrot = findViewById(R.id.btnPowrotMapaTrasy);
        txtTytulMapy = findViewById(R.id.txtTytulMapyTrasy);
        txtStatusMapy = findViewById(R.id.txtStatusMapyTrasy);
        btnRozpocznijTrase = findViewById(R.id.btnRozpocznijTrase);
        btnZoomPlus = findViewById(R.id.btnZoomPlus);
        btnZoomMinus = findViewById(R.id.btnZoomMinus);
    }

    private void przygotujMape() {
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.setBuiltInZoomControls(false);
        mapa.getController().setZoom(15.0);
        mapa.getController().setCenter(new GeoPoint(50.0647, 19.9450));
    }

    private void ustawListenery() {
        btnPowrot.setOnClickListener(v -> finish());

        btnZoomPlus.setOnClickListener(v -> mapa.getController().zoomIn());
        btnZoomMinus.setOnClickListener(v -> mapa.getController().zoomOut());

        btnRozpocznijTrase.setOnClickListener(v -> {
            if (!jestTrasa) {
                Toast.makeText(this, "Brak wybranej trasy", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!trasaAktywna) {
                rozpocznijTrase();
            } else {
                zakonczTrase();
            }
        });
    }

    private void odczytajDaneZIntentu() {
        String idTekst = getIntent().getStringExtra(EXTRA_TRASA_ID);
        nazwaTrasy = getIntent().getStringExtra(EXTRA_TRASA_NAZWA);

        if (idTekst == null || idTekst.trim().isEmpty()) {
            odczytajAktywnaTrase();
            return;
        }

        try {
            trasaId = UUID.fromString(idTekst);
            jestTrasa = true;
            odczytajStanAktywnejTrasyDlaWybranejTrasy(idTekst);
        } catch (Exception e) {
            jestTrasa = false;
            trasaId = null;
        }
    }

    private void odczytajAktywnaTrase() {
        SharedPreferences prefs = getSharedPreferences(PREFS_AKTYWNA_TRASA, MODE_PRIVATE);

        if (!prefs.getBoolean(KEY_AKTYWNA, false)) {
            jestTrasa = false;
            trasaAktywna = false;
            return;
        }

        String idTekst = prefs.getString(KEY_TRASA_ID, null);

        if (idTekst == null || idTekst.trim().isEmpty()) {
            wyczyscAktywnaTrase();
            jestTrasa = false;
            trasaAktywna = false;
            return;
        }

        try {
            trasaId = UUID.fromString(idTekst);
            nazwaTrasy = prefs.getString(KEY_TRASA_NAZWA, null);
            czasStartuMs = prefs.getLong(KEY_START_MS, 0L);
            dystansGpsKm = prefs.getFloat(KEY_DYSTANS_GPS_KM, 0f);
            jestTrasa = true;
            trasaAktywna = czasStartuMs > 0L;
        } catch (Exception e) {
            wyczyscAktywnaTrase();
            jestTrasa = false;
            trasaAktywna = false;
            trasaId = null;
        }
    }

    private void odczytajStanAktywnejTrasyDlaWybranejTrasy(String idTekst) {
        SharedPreferences prefs = getSharedPreferences(PREFS_AKTYWNA_TRASA, MODE_PRIVATE);

        if (!prefs.getBoolean(KEY_AKTYWNA, false)) {
            return;
        }

        String aktywnaTrasaId = prefs.getString(KEY_TRASA_ID, null);

        if (idTekst.equals(aktywnaTrasaId)) {
            czasStartuMs = prefs.getLong(KEY_START_MS, 0L);
            dystansGpsKm = prefs.getFloat(KEY_DYSTANS_GPS_KM, 0f);
            trasaAktywna = czasStartuMs > 0L;
        }
    }

    private void przygotujTrybEkranu() {
        if (jestTrasa) {
            txtTytulMapy.setText(nazwaTrasy == null ? "Podgląd trasy" : nazwaTrasy);
            txtStatusMapy.setText("Wczytywanie trasy...");
            btnRozpocznijTrase.setText(trasaAktywna ? "Zakończ" : "Rozpocznij trasę");
            btnRozpocznijTrase.setVisibility(TextView.VISIBLE);
        } else {
            txtTytulMapy.setText("Mapa");
            txtStatusMapy.setText("Pokazujemy Twoją aktualną lokalizację.");
            btnRozpocznijTrase.setVisibility(TextView.GONE);
        }
    }

    private void uruchomLokalizacje() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION
            );
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager == null) {
            Toast.makeText(this, "Nie udało się uruchomić lokalizacji", Toast.LENGTH_SHORT).show();
            return;
        }

        Location ostatnia = pobierzOstatniaZnanaLokalizacje();

        if (ostatnia != null) {
            pokazLokalizacjeUzytkownika(ostatnia, true);
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        3000,
                        5,
                        locationListener
                );
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        10,
                        locationListener
                );
            }
        } catch (SecurityException ignored) {
        }
    }

    private Location pobierzOstatniaZnanaLokalizacje() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Location gps = null;
        Location network = null;

        try {
            gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        if (gps != null && network != null) {
            return gps.getTime() > network.getTime() ? gps : network;
        }

        if (gps != null) {
            return gps;
        }

        return network;
    }

    private void pokazLokalizacjeUzytkownika(Location location, boolean centruj) {
        if (location == null) {
            return;
        }

        ostatniaLokalizacjaUzytkownika = new Location(location);

        GeoPoint punkt = new GeoPoint(location.getLatitude(), location.getLongitude());

        if (markerLokalizacji == null) {
            markerLokalizacji = new Marker(mapa);
            markerLokalizacji.setTitle("Twoja lokalizacja");
            markerLokalizacji.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            markerLokalizacji.setIcon(utworzCzerwonaKropke());
            mapa.getOverlays().add(markerLokalizacji);
        }

        markerLokalizacji.setPosition(punkt);

        if (centruj || !jestTrasa) {
            mapa.getController().setZoom(16.0);
            mapa.getController().animateTo(punkt);
        }

        mapa.invalidate();
    }

    private void aktualizujDystansGps(Location location) {
        if (!trasaAktywna || location == null) {
            return;
        }

        if (ostatniaLokalizacjaTreningu == null) {
            ostatniaLokalizacjaTreningu = new Location(location);
            return;
        }

        float dystansM = ostatniaLokalizacjaTreningu.distanceTo(location);

        if (dystansM < 5f) {
            ostatniaLokalizacjaTreningu = new Location(location);
            return;
        }

        dystansGpsKm += dystansM / 1000.0;
        ostatniaLokalizacjaTreningu = new Location(location);
        zapiszDystansGpsAktywnejTrasy();
        pokazStatusAktywnejTrasy();
    }

    private BitmapDrawable utworzCzerwonaKropke() {
        int rozmiar = 42;

        Bitmap bitmap = Bitmap.createBitmap(rozmiar, rozmiar, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint obrys = new Paint(Paint.ANTI_ALIAS_FLAG);
        obrys.setColor(Color.WHITE);
        obrys.setStyle(Paint.Style.FILL);

        Paint wypelnienie = new Paint(Paint.ANTI_ALIAS_FLAG);
        wypelnienie.setColor(Color.parseColor("#EF4444"));
        wypelnienie.setStyle(Paint.Style.FILL);

        canvas.drawCircle(rozmiar / 2f, rozmiar / 2f, 18f, obrys);
        canvas.drawCircle(rozmiar / 2f, rozmiar / 2f, 13f, wypelnienie);

        return new BitmapDrawable(getResources(), bitmap);
    }

    private void pokazPunktyNaMapie(List<PunktTrasy> punkty) {
        usunStaraTraseZMapy();

        punktyTrasy.clear();

        if (punkty == null || punkty.isEmpty()) {
            txtStatusMapy.setText("Ta trasa nie ma jeszcze punktów.");
            Toast.makeText(this, "Ta trasa nie ma jeszcze punktów", Toast.LENGTH_SHORT).show();
            mapa.invalidate();
            return;
        }

        punktyTrasy.addAll(punkty);

        List<GeoPoint> geoPoints = new ArrayList<>();

        for (PunktTrasy punkt : punktyTrasy) {
            GeoPoint geoPoint = new GeoPoint(punkt.getLatitude(), punkt.getLongitude());
            geoPoints.add(geoPoint);

            Marker marker = new Marker(mapa);
            marker.setPosition(geoPoint);
            marker.setTitle(punkt.getKolejnosc() + ". " + punkt.getNazwa());
            marker.setSnippet(punkt.getKategoria() + " — " + punkt.getOpis());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapa.getOverlays().add(marker);
        }

        if (geoPoints.size() >= 2) {
            liniaTrasy = new Polyline();
            liniaTrasy.setPoints(geoPoints);
            liniaTrasy.setWidth(8f);
            liniaTrasy.setColor(Color.parseColor("#7C3AED"));
            mapa.getOverlays().add(liniaTrasy);
        }

        dystansTrasyKm = obliczDystansTrasyKm(geoPoints);
        szacowanyCzasMin = oszacujCzasMin(dystansTrasyKm);

        if (trasaAktywna) {
            btnRozpocznijTrase.setText("Zakończ");
            pokazStatusAktywnejTrasy();
            startLicznikaCzasu();
        } else {
            txtStatusMapy.setText(
                    "Dystans: " + String.format(Locale.ROOT, "%.1f", dystansTrasyKm)
                            + " km • Szacowany czas: " + szacowanyCzasMin + " min"
            );
        }

        pokazCalaTraseNaMapie(geoPoints);
        mapa.invalidate();
    }

    private void usunStaraTraseZMapy() {
        mapa.getOverlays().clear();

        markerLokalizacji = null;
        liniaTrasy = null;

        Location ostatnia = null;

        if (locationManager != null) {
            ostatnia = pobierzOstatniaZnanaLokalizacje();
        }

        if (ostatnia != null) {
            pokazLokalizacjeUzytkownika(ostatnia, false);
        }
    }

    private double obliczDystansTrasyKm(List<GeoPoint> geoPoints) {
        if (geoPoints.size() < 2) {
            return 0.0;
        }

        double suma = 0.0;

        for (int i = 0; i < geoPoints.size() - 1; i++) {
            suma += geoPoints.get(i).distanceToAsDouble(geoPoints.get(i + 1)) / 1000.0;
        }

        return suma;
    }

    private int oszacujCzasMin(double dystansKm) {
        if (dystansKm <= 0) {
            return 0;
        }

        return Math.max(10, (int) Math.round((dystansKm / 4.8) * 60.0));
    }

    private void pokazCalaTraseNaMapie(List<GeoPoint> geoPoints) {
        if (geoPoints == null || geoPoints.isEmpty()) {
            return;
        }

        if (geoPoints.size() == 1) {
            mapa.getController().setZoom(16.0);
            mapa.getController().animateTo(geoPoints.get(0));
            return;
        }

        double minLat = geoPoints.get(0).getLatitude();
        double maxLat = geoPoints.get(0).getLatitude();
        double minLon = geoPoints.get(0).getLongitude();
        double maxLon = geoPoints.get(0).getLongitude();

        for (GeoPoint punkt : geoPoints) {
            minLat = Math.min(minLat, punkt.getLatitude());
            maxLat = Math.max(maxLat, punkt.getLatitude());
            minLon = Math.min(minLon, punkt.getLongitude());
            maxLon = Math.max(maxLon, punkt.getLongitude());
        }

        BoundingBox obszarTrasy = new BoundingBox(maxLat, maxLon, minLat, minLon);
        mapa.post(() -> mapa.zoomToBoundingBox(obszarTrasy, true, dp(80)));
    }

    private List<GeoPoint> pobierzGeoPunktyTrasy() {
        List<GeoPoint> geoPoints = new ArrayList<>();

        for (PunktTrasy punkt : punktyTrasy) {
            geoPoints.add(new GeoPoint(punkt.getLatitude(), punkt.getLongitude()));
        }

        return geoPoints;
    }

    private void rozpocznijTrase() {
        if (punktyTrasy.isEmpty()) {
            Toast.makeText(this, "Trasa nie ma jeszcze punktów na mapie", Toast.LENGTH_SHORT).show();
            return;
        }

        trasaAktywna = true;
        czasStartuMs = System.currentTimeMillis();
        dystansGpsKm = 0.0;
        ostatniaLokalizacjaTreningu = ostatniaLokalizacjaUzytkownika == null
                ? null
                : new Location(ostatniaLokalizacjaUzytkownika);
        zapiszAktywnaTrase();

        btnRozpocznijTrase.setText("Zakończ");
        pokazStatusAktywnejTrasy();
        startLicznikaCzasu();

        pokazCalaTraseNaMapie(pobierzGeoPunktyTrasy());
        Toast.makeText(this, "Trasa rozpoczęta", Toast.LENGTH_SHORT).show();
    }

    private void zapiszAktywnaTrase() {
        if (trasaId == null) {
            return;
        }

        getSharedPreferences(PREFS_AKTYWNA_TRASA, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AKTYWNA, true)
                .putString(KEY_TRASA_ID, trasaId.toString())
                .putString(KEY_TRASA_NAZWA, nazwaTrasy)
                .putLong(KEY_START_MS, czasStartuMs)
                .putFloat(KEY_DYSTANS_GPS_KM, (float) dystansGpsKm)
                .apply();
    }

    private void zapiszDystansGpsAktywnejTrasy() {
        getSharedPreferences(PREFS_AKTYWNA_TRASA, MODE_PRIVATE)
                .edit()
                .putFloat(KEY_DYSTANS_GPS_KM, (float) dystansGpsKm)
                .apply();
    }

    private void wyczyscAktywnaTrase() {
        getSharedPreferences(PREFS_AKTYWNA_TRASA, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void startLicznikaCzasu() {
        handlerCzasu.removeCallbacks(odswiezanieCzasu);
        handlerCzasu.post(odswiezanieCzasu);
    }

    private void stopLicznikaCzasu() {
        handlerCzasu.removeCallbacks(odswiezanieCzasu);
    }

    private void pokazStatusAktywnejTrasy() {
        if (czasStartuMs <= 0L) {
            return;
        }

        long terazMs = System.currentTimeMillis();
        long mineloSekund = Math.max(0L, (terazMs - czasStartuMs) / 1000L);

        txtStatusMapy.setText(
                "Trasa aktywna • Czas: " + formatujCzasTrwania(mineloSekund)
                        + " • Przebyto: " + String.format(Locale.ROOT, "%.2f", dystansGpsKm) + " km"
                        + " • Kalorie: " + obliczKalorie(dystansGpsKm)
                        + " • Start: " + formatujCzas(czasStartuMs)
                        + " • Szacowany koniec: " + formatujCzas(czasStartuMs + szacowanyCzasMin * 60_000L)
        );
    }

    private String formatujCzasTrwania(long sekundy) {
        long godziny = sekundy / 3600L;
        long minuty = (sekundy % 3600L) / 60L;
        long pozostaleSekundy = sekundy % 60L;

        return String.format(Locale.ROOT, "%02d:%02d:%02d", godziny, minuty, pozostaleSekundy);
    }

    private int obliczKalorie(double kilometry) {
        double waga = 75.0;

        try {
            String wagaTekst = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString("uzytkownik_waga", "75");
            waga = Double.parseDouble(wagaTekst.replace(",", "."));
        } catch (Exception ignored) {
        }

        return (int) Math.round(kilometry * waga * 0.65);
    }

    private int dp(int wartosc) {
        return (int) (wartosc * getResources().getDisplayMetrics().density);
    }

    private void zakonczTrase() {
        trasaAktywna = false;
        stopLicznikaCzasu();
        wyczyscAktywnaTrase();

        long czasKoncaMs = System.currentTimeMillis();
        int rzeczywistyCzasMin = Math.max(1, (int) Math.round((czasKoncaMs - czasStartuMs) / 60000.0));
        int kalorie = obliczKalorie(dystansGpsKm);

        trasyViewModel.dodajTrening(new TreningEntity(
                UUID.randomUUID().toString(),
                trasaId == null ? null : trasaId.toString(),
                nazwaTrasy == null ? "Trasa" : nazwaTrasy,
                czasStartuMs,
                czasKoncaMs,
                rzeczywistyCzasMin,
                dystansGpsKm,
                0,
                kalorie,
                ""
        ));

        btnRozpocznijTrase.setText("Rozpocznij trasę");
        txtStatusMapy.setText(
                "Trasa zakończona • Czas: " + rzeczywistyCzasMin
                        + " min • Przebyto: " + String.format(Locale.ROOT, "%.2f", dystansGpsKm)
                        + " km • Kalorie: " + kalorie
        );

        Toast.makeText(this, "Trasa zakończona. Później zapiszemy ją do treningów.", Toast.LENGTH_LONG).show();
    }

    private String formatujCzas(long czasMs) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(czasMs));
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION) {
            uruchomLokalizacje();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mapa != null) {
            mapa.onResume();
        }

        if (trasaAktywna) {
            startLicznikaCzasu();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mapa != null) {
            mapa.onPause();
        }

        stopLicznikaCzasu();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

        stopLicznikaCzasu();
    }
}
