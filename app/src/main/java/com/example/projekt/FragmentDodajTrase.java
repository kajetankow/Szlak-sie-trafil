package com.example.projekt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentDodajTrase extends Fragment {

    private TrasyViewModel trasyViewModel;

    private TextView txtPowrotDodajTrase;
    private TextView txtKrok1;
    private TextView txtKrok2;
    private TextView txtKrok3;
    private TextView txtInstrukcjaPunktow;
    private TextView txtListaPunktow;
    private TextView txtParametryRobocze;
    private TextView txtPodsumowanieTrasy;

    private LinearLayout panelKrokDane;
    private LinearLayout panelKrokPunkty;
    private LinearLayout panelKrokPodsumowanie;

    private TextInputEditText edtNazwa;
    private TextInputEditText edtOpis;
    private TextInputEditText edtRegion;
    private TextInputEditText edtNazwaPunktu;

    private Spinner spinnerTypTrasy;

    private Button btnDalejDoPunktow;
    private Button btnWrocDoDanych;
    private Button btnDalejDoPodsumowania;
    private Button btnWrocDoPunktow;
    private Button btnSzukajDodajPunkt;
    private Button btnTrybMapa;
    private Button btnBibliotekaPunktow;
    private Button btnOptymalizujPunkty;
    private Button btnUsunOstatniPunkt;
    private Button btnDodajMape;
    private Button btnAnulujKreator;

    private MapView mapaKreator;
    private MapView mapaPodsumowanie;

    private final List<PunktRoboczy> punkty = new ArrayList<>();
    private final List<PunktTrasy> punktyBiblioteki = new ArrayList<>();
    private final List<Marker> markeryKreator = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Polyline liniaKreator;
    private Polyline liniaPodsumowanie;

    private boolean trybKlikaniaMapy = false;
    private boolean dodawaniePunktuBibliotekiZMapy = false;
    private boolean trybEdycji = false;
    private boolean punktyEdycjiWczytane = false;
    private UUID edytowanaTrasaId = null;

    private String nazwaTrasy = "";
    private String opisTrasy = "";
    private String regionTrasy = "";
    private String typTrasy = "Spacer";

    private double dystansKm = 0.0;
    private int czasMin = 0;
    private int przewyzszenieM = 0;
    private String poziomTrudnosci = "Brak danych";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dodaj_trase, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        trasyViewModel = new ViewModelProvider(requireActivity()).get(TrasyViewModel.class);
        trasyViewModel.getPunkty().observe(getViewLifecycleOwner(), punktyZBazy -> {
            punktyBiblioteki.clear();

            if (punktyZBazy != null) {
                punktyBiblioteki.addAll(punktyZBazy);
            }
        });
        trasyViewModel.pobierzPunkty();

        przypiszWidoki(view);
        dostosujEtykietyKrokow(view);
        przygotujSpinnerTypu();
        przygotujMapy();
        ustawListenery();
        sprawdzTrybEdycji();
        sprawdzPunktStartowyZArgumentow();
        pokazKrok(1);
        odswiezStanPunktow();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mapaKreator != null) {
            mapaKreator.onResume();
        }

        if (mapaPodsumowanie != null) {
            mapaPodsumowanie.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mapaKreator != null) {
            mapaKreator.onPause();
        }

        if (mapaPodsumowanie != null) {
            mapaPodsumowanie.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
    }

    private void przypiszWidoki(View view) {
        txtPowrotDodajTrase = view.findViewById(R.id.txtPowrotDodajTrase);
        txtKrok1 = view.findViewById(R.id.txtKrok1);
        txtKrok2 = view.findViewById(R.id.txtKrok2);
        txtKrok3 = view.findViewById(R.id.txtKrok3);
        txtInstrukcjaPunktow = view.findViewById(R.id.txtInstrukcjaPunktow);
        txtListaPunktow = view.findViewById(R.id.txtListaPunktow);
        txtParametryRobocze = view.findViewById(R.id.txtParametryRobocze);
        txtPodsumowanieTrasy = view.findViewById(R.id.txtPodsumowanieTrasy);

        panelKrokDane = view.findViewById(R.id.panelKrokDane);
        panelKrokPunkty = view.findViewById(R.id.panelKrokPunkty);
        panelKrokPodsumowanie = view.findViewById(R.id.panelKrokPodsumowanie);

        edtNazwa = view.findViewById(R.id.edtNazwaNowejTrasy);
        edtOpis = view.findViewById(R.id.edtOpisNowejTrasy);
        edtRegion = view.findViewById(R.id.edtRegionNowejTrasy);
        edtNazwaPunktu = view.findViewById(R.id.edtNazwaPunktu);

        spinnerTypTrasy = view.findViewById(R.id.spinnerTypTrasy);

        btnDalejDoPunktow = view.findViewById(R.id.btnDalejDoPunktow);
        btnWrocDoDanych = view.findViewById(R.id.btnWrocDoDanych);
        btnDalejDoPodsumowania = view.findViewById(R.id.btnDalejDoPodsumowania);
        btnWrocDoPunktow = view.findViewById(R.id.btnWrocDoPunktow);
        btnSzukajDodajPunkt = view.findViewById(R.id.btnSzukajDodajPunkt);
        btnTrybMapa = view.findViewById(R.id.btnTrybMapa);
        btnBibliotekaPunktow = view.findViewById(R.id.btnBibliotekaPunktow);
        btnOptymalizujPunkty = view.findViewById(R.id.btnOptymalizujPunkty);
        btnUsunOstatniPunkt = view.findViewById(R.id.btnUsunOstatniPunkt);
        btnDodajMape = view.findViewById(R.id.btnDodajMape);
        btnAnulujKreator = view.findViewById(R.id.btnAnulujKreator);

        mapaKreator = view.findViewById(R.id.mapaKreatorTrasy);
        mapaPodsumowanie = view.findViewById(R.id.mapaPodsumowanieTrasy);
    }

    private void dostosujEtykietyKrokow(View view) {
        View panelKroki = view.findViewById(R.id.panelKroki);
        if (panelKroki == null) {
            return;
        }

        panelKroki.post(() -> {
            int szerokoscSegmentu = panelKroki.getWidth() / 3;
            boolean wasko = szerokoscSegmentu < dp(116);

            txtKrok1.setText(wasko ? "1.\nDane" : "1. Dane");
            txtKrok2.setText(wasko ? "2.\nPunkty" : "2. Punkty");
            txtKrok3.setText(wasko ? "3.\nPodsumowanie" : "3. Podsumowanie");

            txtKrok1.setSingleLine(!wasko);
            txtKrok2.setSingleLine(!wasko);
            txtKrok3.setSingleLine(!wasko);

            txtKrok1.setMaxLines(wasko ? 2 : 1);
            txtKrok2.setMaxLines(wasko ? 2 : 1);
            txtKrok3.setMaxLines(wasko ? 2 : 1);
        });
    }

    private void przygotujSpinnerTypu() {
        List<String> typy = new ArrayList<>();
        typy.add("Spacer");
        typy.add("Miejska");
        typy.add("Górska");
        typy.add("Przyrodnicza");
        typy.add("Rowerowa");
        typy.add("Krajoznawcza");
        typy.add("Własna");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                typy
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypTrasy.setAdapter(adapter);
    }

    private void przygotujMapy() {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        przygotujMape(mapaKreator);
        przygotujMape(mapaPodsumowanie);

        mapaKreator.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                obsluzKlikniecieMapy(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                obsluzKlikniecieMapy(p);
                return true;
            }
        }));
    }

    private void przygotujMape(MapView mapa) {
        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.getController().setZoom(13.0);
        mapa.getController().setCenter(new GeoPoint(50.061947, 19.936856));
    }

    private void ustawListenery() {
        txtPowrotDodajTrase.setOnClickListener(v -> potwierdzWyjscie());

        btnDalejDoPunktow.setOnClickListener(v -> {
            if (walidujKrokDane()) {
                zapiszDanePodstawowe();
                pokazKrok(2);
                wycentrujMapeNaRegionie();
            }
        });

        btnWrocDoDanych.setOnClickListener(v -> pokazKrok(1));

        btnDalejDoPodsumowania.setOnClickListener(v -> {
            if (walidujKrokPunkty()) {
                zapiszDanePodstawowe();
                przeliczParametry();
                odswiezPodsumowanie();
                pokazKrok(3);
            }
        });

        btnWrocDoPunktow.setOnClickListener(v -> pokazKrok(2));

        btnSzukajDodajPunkt.setOnClickListener(v -> dodajPunktZPola());
        btnBibliotekaPunktow.setOnClickListener(v -> pokazPopupPunktow());

        btnTrybMapa.setOnClickListener(v -> {
            trybKlikaniaMapy = !trybKlikaniaMapy;

            if (trybKlikaniaMapy) {
                btnTrybMapa.setText("Zakończ wybieranie z mapy");
                txtInstrukcjaPunktow.setText("Klikaj na mapie, aby dodawać kolejne punkty trasy.");
                Toast.makeText(requireContext(), "Tryb dodawania z mapy włączony", Toast.LENGTH_SHORT).show();
            } else {
                btnTrybMapa.setText("Wybierz punkty z mapy");
                odswiezStanPunktow();
            }
        });

        btnOptymalizujPunkty.setOnClickListener(v -> {
            if (punkty.size() < 4) {
                Toast.makeText(requireContext(), "Do optymalizacji dodaj przynajmniej 2 punkty pośrednie", Toast.LENGTH_SHORT).show();
                return;
            }

            optymalizujKolejnoscPunktow();
            odswiezStanPunktow();
            Toast.makeText(requireContext(), "Kolejność punktów została zoptymalizowana", Toast.LENGTH_SHORT).show();
        });

        btnUsunOstatniPunkt.setOnClickListener(v -> {
            usunOstatniPunkt();
            odswiezStanPunktow();
        });

        btnDodajMape.setOnClickListener(v -> zapiszTrase());
        btnAnulujKreator.setOnClickListener(v -> potwierdzWyjscie());
    }

    private void sprawdzTrybEdycji() {
        Bundle args = getArguments();

        if (args == null) {
            return;
        }

        String tryb = args.getString("tryb", "");
        String trasaIdTekst = args.getString("trasa_id", "");

        if (!"edycja".equals(tryb) || TextUtils.isEmpty(trasaIdTekst)) {
            return;
        }

        try {
            trybEdycji = true;
            edytowanaTrasaId = UUID.fromString(trasaIdTekst);
        } catch (Exception e) {
            trybEdycji = false;
            edytowanaTrasaId = null;
            return;
        }

        btnDodajMape.setText("Zapisz zmiany");
        txtPowrotDodajTrase.setText("← Wróć do tras");

        trasyViewModel.getPunktyTrasy().observe(getViewLifecycleOwner(), punktyZBazy -> {
            if (!trybEdycji || punktyEdycjiWczytane || punktyZBazy == null) {
                return;
            }

            if (!punktyZBazy.isEmpty()) {
                UUID idPierwszegoPunktu = punktyZBazy.get(0).getTrasaId();

                if (idPierwszegoPunktu != null && edytowanaTrasaId != null
                        && !idPierwszegoPunktu.equals(edytowanaTrasaId)) {
                    return;
                }
            }

            punktyEdycjiWczytane = true;
            punkty.clear();

            for (PunktTrasy punkt : punktyZBazy) {
                punkty.add(new PunktRoboczy(
                        punkt.getNazwa(),
                        punkt.getKategoria(),
                        new GeoPoint(punkt.getLatitude(), punkt.getLongitude())
                ));
            }

            odswiezStanPunktow();
        });

        trasyViewModel.pobierzTrase(edytowanaTrasaId, trasa -> {
            if (trasa == null || !isAdded()) {
                Toast.makeText(requireContext(), "Nie udało się wczytać trasy do edycji", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
                return;
            }

            edtNazwa.setText(trasa.getNazwa());
            edtOpis.setText(trasa.getOpis());
            edtRegion.setText(trasa.getRegion());

            nazwaTrasy = trasa.getNazwa();
            opisTrasy = trasa.getOpis();
            regionTrasy = trasa.getRegion();
            typTrasy = trasa.getTyp();

            ustawTypNaSpinnerze(typTrasy);

            dystansKm = trasa.getDystansKm();
            przewyzszenieM = trasa.getPrzewyzszenieM();
            czasMin = trasa.getCzasMin();
            poziomTrudnosci = trasa.getPoziomTrudnosci();

            trasyViewModel.pobierzPunktyTrasy(edytowanaTrasaId);
        });
    }

    private void sprawdzPunktStartowyZArgumentow() {
        if (trybEdycji) {
            return;
        }

        Bundle args = getArguments();
        if (args == null || !args.containsKey("punkt_start_lat") || !args.containsKey("punkt_start_lon")) {
            return;
        }

        double lat = args.getDouble("punkt_start_lat", 0.0);
        double lon = args.getDouble("punkt_start_lon", 0.0);

        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return;
        }

        String nazwa = args.getString("punkt_start_nazwa", "Punkt startu");
        String kategoria = args.getString("punkt_start_kategoria", "Start");

        punkty.clear();
        punkty.add(new PunktRoboczy(
                TextUtils.isEmpty(nazwa) ? "Punkt startu" : nazwa,
                TextUtils.isEmpty(kategoria) ? "Start" : kategoria,
                new GeoPoint(lat, lon)
        ));
    }

    private void ustawTypNaSpinnerze(String typ) {
        if (TextUtils.isEmpty(typ)) {
            return;
        }

        for (int i = 0; i < spinnerTypTrasy.getCount(); i++) {
            Object item = spinnerTypTrasy.getItemAtPosition(i);

            if (item != null && typ.equals(item.toString())) {
                spinnerTypTrasy.setSelection(i);
                return;
            }
        }
    }

    private void pokazKrok(int krok) {
        panelKrokDane.setVisibility(krok == 1 ? View.VISIBLE : View.GONE);
        panelKrokPunkty.setVisibility(krok == 2 ? View.VISIBLE : View.GONE);
        panelKrokPodsumowanie.setVisibility(krok == 3 ? View.VISIBLE : View.GONE);

        txtKrok1.setTextColor(krok == 1 ? Color.parseColor("#2563EB") : Color.parseColor("#6B7280"));
        txtKrok2.setTextColor(krok == 2 ? Color.parseColor("#2563EB") : Color.parseColor("#6B7280"));
        txtKrok3.setTextColor(krok == 3 ? Color.parseColor("#2563EB") : Color.parseColor("#6B7280"));

        if (krok == 2) {
            mapaKreator.postDelayed(() -> {
                mapaKreator.invalidate();
                odswiezMapeKreatora();
            }, 250);
        }

        if (krok == 3) {
            mapaPodsumowanie.postDelayed(() -> {
                mapaPodsumowanie.invalidate();
                odswiezMapePodsumowania();
            }, 250);
        }
    }

    private boolean walidujKrokDane() {
        String nazwa = tekst(edtNazwa);
        String region = tekst(edtRegion);

        if (TextUtils.isEmpty(nazwa)) {
            edtNazwa.setError("Podaj nazwę trasy");
            edtNazwa.requestFocus();
            return false;
        }

        if (nazwa.length() < 3) {
            edtNazwa.setError("Nazwa trasy jest za krótka");
            edtNazwa.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(region)) {
            edtRegion.setError("Podaj region trasy");
            edtRegion.requestFocus();
            return false;
        }

        return true;
    }

    private void zapiszDanePodstawowe() {
        nazwaTrasy = tekst(edtNazwa);
        opisTrasy = tekst(edtOpis);
        regionTrasy = tekst(edtRegion);

        Object wybranyTyp = spinnerTypTrasy.getSelectedItem();
        typTrasy = wybranyTyp == null ? "Własna" : wybranyTyp.toString();
    }

    private boolean walidujKrokPunkty() {
        if (punkty.size() < 2) {
            Toast.makeText(requireContext(), "Dodaj minimum punkt startu i punkt końcowy", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void wycentrujMapeNaRegionie() {
        if (TextUtils.isEmpty(regionTrasy)) {
            return;
        }

        geokoduj(regionTrasy, new GeoCallback() {
            @Override
            public void onSuccess(GeoPoint point, String displayName) {
                mapaKreator.getController().setZoom(12.0);
                mapaKreator.getController().animateTo(point);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "Nie udało się znaleźć regionu: " + regionTrasy, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dodajPunktZPola() {
        String wpis = tekst(edtNazwaPunktu);

        if (TextUtils.isEmpty(wpis)) {
            edtNazwaPunktu.setError("Podaj nazwę punktu albo współrzędne");
            return;
        }

        GeoPoint point = parsujWspolrzedne(wpis);

        if (point != null) {
            dodajPunktDoTrasy("", point);
            return;
        }

        String zapytanie = wpis;

        if (!TextUtils.isEmpty(regionTrasy)) {
            zapytanie = wpis + ", " + regionTrasy;
        }

        btnSzukajDodajPunkt.setEnabled(false);
        btnSzukajDodajPunkt.setText("Szukam...");

        geokoduj(zapytanie, new GeoCallback() {
            @Override
            public void onSuccess(GeoPoint point, String displayName) {
                btnSzukajDodajPunkt.setEnabled(true);
                btnSzukajDodajPunkt.setText(tekstPrzyciskuDodawaniaPunktu());
                dodajPunktDoTrasy(wpis, point);
            }

            @Override
            public void onError(String message) {
                btnSzukajDodajPunkt.setEnabled(true);
                btnSzukajDodajPunkt.setText(tekstPrzyciskuDodawaniaPunktu());
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void obsluzKlikniecieMapy(GeoPoint punktMapy) {
        if (dodawaniePunktuBibliotekiZMapy) {
            String nazwa = tekst(edtNazwaPunktu);
            if (TextUtils.isEmpty(nazwa)) {
                nazwa = "Punkt z mapy";
            }

            zapiszPunktBiblioteki(nazwa, punktMapy, null);
            dodawaniePunktuBibliotekiZMapy = false;
            trybKlikaniaMapy = false;
            btnTrybMapa.setText("Wybierz punkty z mapy");
            odswiezStanPunktow();
            return;
        }

        if (!trybKlikaniaMapy) {
            Toast.makeText(requireContext(), "Włącz tryb: Wybierz punkty z mapy", Toast.LENGTH_SHORT).show();
            return;
        }

        String nazwa = tekst(edtNazwaPunktu);
        dodajPunktDoTrasy(nazwa, punktMapy);
    }

    private void dodajPunktDoTrasy(String nazwa, GeoPoint geoPoint) {
        String kategoria;

        if (punkty.isEmpty()) {
            kategoria = "Start";
            if (TextUtils.isEmpty(nazwa)) {
                nazwa = "Punkt startu";
            }
        } else if (punkty.size() == 1) {
            kategoria = "Koniec";
            if (TextUtils.isEmpty(nazwa)) {
                nazwa = "Punkt końcowy";
            }
        } else {
            kategoria = "Pośredni";
            if (TextUtils.isEmpty(nazwa)) {
                nazwa = "Punkt " + punkty.size();
            }

            PunktRoboczy koniec = punkty.remove(punkty.size() - 1);
            punkty.add(new PunktRoboczy(nazwa, kategoria, geoPoint));
            punkty.add(koniec);

            wyczyscPolaPunktu();
            odswiezStanPunktow();
            return;
        }

        punkty.add(new PunktRoboczy(nazwa, kategoria, geoPoint));

        wyczyscPolaPunktu();
        odswiezStanPunktow();
    }

    private void pokazPopupPunktow() {
        LinearLayout kontener = new LinearLayout(requireContext());
        kontener.setOrientation(LinearLayout.VERTICAL);
        kontener.setPadding(dp(16), dp(12), dp(16), dp(8));
        kontener.setBackgroundResource(R.drawable.bg_dialog_rounded);

        TextView opis = new TextView(requireContext());
        opis.setText("Wyszukaj punkt po nazwie, dodaj nowy punkt z adresu lub współrzędnych i przypisz go do trasy. Kolejność możesz zmienić przyciskami ↑/↓.");
        opis.setTextColor(Color.parseColor("#4B5563"));
        opis.setTextSize(13);
        kontener.addView(opis);

        EditText wyszukiwarka = new EditText(requireContext());
        wyszukiwarka.setHint("Szukaj punktu po nazwie");
        wyszukiwarka.setSingleLine(true);
        kontener.addView(wyszukiwarka);

        LinearLayout przyciski = new LinearLayout(requireContext());
        przyciski.setOrientation(LinearLayout.VERTICAL);

        Button btnSzukaj = new Button(requireContext());
        btnSzukaj.setText("Szukaj");
        stylizujPrzyciskFioletowy(btnSzukaj);
        przyciski.addView(btnSzukaj, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button btnDodaj = new Button(requireContext());
        btnDodaj.setText("Dodaj z wpisu");
        stylizujPrzyciskFioletowy(btnDodaj);
        LinearLayout.LayoutParams btnDodajParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnDodajParams.setMargins(0, dp(6), 0, 0);
        przyciski.addView(btnDodaj, btnDodajParams);

        Button btnMapa = new Button(requireContext());
        btnMapa.setText("Wybierz z mapy");
        stylizujPrzyciskFioletowy(btnMapa);
        LinearLayout.LayoutParams btnMapaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnMapaParams.setMargins(0, dp(6), 0, 0);
        przyciski.addView(btnMapa, btnMapaParams);

        kontener.addView(przyciski);

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout lista = new LinearLayout(requireContext());
        lista.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(lista);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(360)
        );
        scrollParams.setMargins(0, dp(8), 0, 0);
        kontener.addView(scrollView, scrollParams);

        ScrollView dialogScroll = new ScrollView(requireContext());
        dialogScroll.setFillViewport(false);
        dialogScroll.addView(kontener);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Punkty")
                .setView(dialogScroll)
                .setNegativeButton("Zamknij", null)
                .create();

        btnSzukaj.setOnClickListener(v -> odswiezPopupPunktow(lista, wyszukiwarka.getText().toString()));
        btnDodaj.setOnClickListener(v -> dodajPunktDoBibliotekiZPopupu(wyszukiwarka, lista));
        btnMapa.setOnClickListener(v -> rozpocznijDodawaniePunktuBibliotekiZMapy(dialog));

        dialog.setOnShowListener(d -> odswiezPopupPunktow(lista, ""));
        dialog.show();
        stylizujPrzyciskZamknij(dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void dodajPunktDoBibliotekiZPopupu(EditText wyszukiwarka, LinearLayout lista) {
        String wpis = wyszukiwarka.getText() == null ? "" : wyszukiwarka.getText().toString().trim();

        if (TextUtils.isEmpty(wpis)) {
            wyszukiwarka.setError("Wpisz adres, nazwę albo współrzędne");
            return;
        }

        GeoPoint wspolrzedne = parsujWspolrzedne(wpis);

        if (wspolrzedne != null) {
            zapiszPunktBiblioteki(wpis, wspolrzedne, lista);
            return;
        }

        String zapytanie = TextUtils.isEmpty(regionTrasy) ? wpis : wpis + ", " + regionTrasy;

        geokoduj(zapytanie, new GeoCallback() {
            @Override
            public void onSuccess(GeoPoint point, String displayName) {
                zapiszPunktBiblioteki(wpis, point, lista);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rozpocznijDodawaniePunktuBibliotekiZMapy(AlertDialog dialog) {
        dodawaniePunktuBibliotekiZMapy = true;
        trybKlikaniaMapy = true;
        btnTrybMapa.setText("Kliknij punkt biblioteki na mapie");

        if (dialog != null) {
            dialog.dismiss();
        }

        Toast.makeText(requireContext(), "Kliknij miejsce na mapie, aby zapisać punkt do listy", Toast.LENGTH_LONG).show();
    }

    private void zapiszPunktBiblioteki(String nazwa, GeoPoint geoPoint, LinearLayout lista) {
        PunktTrasy punkt = new PunktTrasy();
        punkt.setTrasaId(null);
        punkt.setNazwa(TextUtils.isEmpty(nazwa) ? "Nowy punkt" : nazwa);
        punkt.setOpis("");
        punkt.setKategoria("Biblioteka");
        punkt.setLatitude(geoPoint.getLatitude());
        punkt.setLongitude(geoPoint.getLongitude());
        punkt.setKolejnosc(0);

        punktyBiblioteki.add(0, punkt);
        trasyViewModel.dodajPunkt(punkt);
        if (lista != null) {
            odswiezPopupPunktow(lista, "");
        }
        Toast.makeText(requireContext(), "Punkt dodany do listy", Toast.LENGTH_SHORT).show();
    }

    private void odswiezPopupPunktow(LinearLayout lista, String filtr) {
        lista.removeAllViews();

        TextView naglowekKolejnosc = tekstPopupu("Kolejność punktów na trasie", 16, "#111827", true);
        lista.addView(naglowekKolejnosc);

        if (punkty.isEmpty()) {
            lista.addView(tekstPopupu("Brak punktów na aktualnej trasie.", 13, "#6B7280", false));
        } else {
            for (int i = 0; i < punkty.size(); i++) {
                lista.addView(wierszKolejnosciPunktu(i, lista, filtr));
            }
        }

        lista.addView(separatorPopupu());
        lista.addView(tekstPopupu("Lista punktów", 16, "#111827", true));

        String szukane = filtr == null ? "" : filtr.trim().toLowerCase(Locale.ROOT);
        int pokazane = 0;

        for (PunktTrasy punkt : punktyBiblioteki) {
            if (punkt == null) {
                continue;
            }

            String nazwa = punkt.getNazwa() == null ? "" : punkt.getNazwa();
            if (!TextUtils.isEmpty(szukane) && !nazwa.toLowerCase(Locale.ROOT).contains(szukane)) {
                continue;
            }

            lista.addView(wierszPunktuBiblioteki(punkt, lista, filtr));
            pokazane++;

            if (pokazane >= 40) {
                lista.addView(tekstPopupu("Pokazano pierwsze 40 wyników. Doprecyzuj wyszukiwanie, jeśli punktu nie ma na liście.", 12, "#6B7280", false));
                break;
            }
        }

        if (pokazane == 0) {
            lista.addView(tekstPopupu("Brak punktów pasujących do wyszukiwania.", 13, "#6B7280", false));
        }
    }

    private View wierszKolejnosciPunktu(int indeks, LinearLayout lista, String filtr) {
        LinearLayout wiersz = new LinearLayout(requireContext());
        wiersz.setOrientation(LinearLayout.HORIZONTAL);
        wiersz.setPadding(0, dp(4), 0, dp(4));

        PunktRoboczy punkt = punkty.get(indeks);
        TextView tekst = tekstPopupu((indeks + 1) + ". " + punkt.nazwa, 13, "#374151", true);
        wiersz.addView(tekst, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button gora = malyPrzycisk("↑");
        gora.setEnabled(indeks > 0);
        gora.setOnClickListener(v -> {
            zamienPunkty(indeks, indeks - 1);
            odswiezStanPunktow();
            odswiezPopupPunktow(lista, filtr);
        });
        wiersz.addView(gora);

        Button dol = malyPrzycisk("↓");
        dol.setEnabled(indeks < punkty.size() - 1);
        dol.setOnClickListener(v -> {
            zamienPunkty(indeks, indeks + 1);
            odswiezStanPunktow();
            odswiezPopupPunktow(lista, filtr);
        });
        wiersz.addView(dol);

        return wiersz;
    }

    private View wierszPunktuBiblioteki(PunktTrasy punkt, LinearLayout lista, String filtr) {
        LinearLayout wiersz = new LinearLayout(requireContext());
        wiersz.setOrientation(LinearLayout.VERTICAL);
        wiersz.setPadding(0, dp(8), 0, dp(8));

        String nazwa = TextUtils.isEmpty(punkt.getNazwa()) ? "Punkt bez nazwy" : punkt.getNazwa();
        wiersz.addView(tekstPopupu(nazwa, 14, "#111827", true));
        wiersz.addView(tekstPopupu(
                punkt.getKategoria() + " • "
                        + String.format(Locale.ROOT, "%.5f, %.5f", punkt.getLatitude(), punkt.getLongitude()),
                12,
                "#6B7280",
                false
        ));

        Button dodaj = new Button(requireContext());
        dodaj.setText("Dodaj do trasy");
        stylizujPrzyciskFioletowy(dodaj);
        dodaj.setOnClickListener(v -> {
            dodajPunktDoTrasy(nazwa, new GeoPoint(punkt.getLatitude(), punkt.getLongitude()));
            odswiezPopupPunktow(lista, filtr);
            Toast.makeText(requireContext(), "Punkt przypisany do trasy", Toast.LENGTH_SHORT).show();
        });
        wiersz.addView(dodaj);

        return wiersz;
    }

    private TextView tekstPopupu(String tekst, int rozmiar, String kolor, boolean pogrubiony) {
        TextView textView = new TextView(requireContext());
        textView.setText(tekst);
        textView.setTextSize(rozmiar);
        textView.setTextColor(Color.parseColor(kolor));
        textView.setPadding(0, dp(3), 0, dp(3));

        if (pogrubiony) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        return textView;
    }

    private View separatorPopupu() {
        View separator = new View(requireContext());
        separator.setBackgroundColor(Color.parseColor("#E5E7EB"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        params.setMargins(0, dp(10), 0, dp(10));
        separator.setLayoutParams(params);

        return separator;
    }

    private Button malyPrzycisk(String tekst) {
        Button button = new Button(requireContext());
        button.setText(tekst);
        button.setTextSize(24);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setPadding(0, 0, 0, 0);
        button.setTextColor(Color.WHITE);
        button.setBackground(utworzTloPionowegoPrzycisku("#7C3AED", "#7C3AED"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(36), dp(52));
        params.setMargins(dp(6), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void stylizujPrzyciskFioletowy(Button button) {
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
    }

    private void stylizujPrzyciskZamknij(AlertDialog dialog) {
        Button zamknij = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (zamknij == null) {
            return;
        }

        zamknij.setTextColor(Color.parseColor("#7C3AED"));
        zamknij.setBackground(utworzTloPionowegoPrzycisku("#FFFFFF", "#D1D5DB"));
    }

    private GradientDrawable utworzTloPionowegoPrzycisku(String kolorTla, String kolorObrysu) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(18));
        drawable.setColor(Color.parseColor(kolorTla));
        drawable.setStroke(dp(1), Color.parseColor(kolorObrysu));
        return drawable;
    }

    private void zamienPunkty(int pierwszy, int drugi) {
        if (pierwszy < 0 || drugi < 0 || pierwszy >= punkty.size() || drugi >= punkty.size()) {
            return;
        }

        PunktRoboczy punkt = punkty.get(pierwszy);
        punkty.set(pierwszy, punkty.get(drugi));
        punkty.set(drugi, punkt);
    }

    private void wyczyscPolaPunktu() {
        edtNazwaPunktu.setText("");
    }

    private GeoPoint parsujWspolrzedne(String tekst) {
        try {
            String przygotowany = tekst
                    .replace(";", ",")
                    .replace(" ", ",");

            String[] czesci = przygotowany.split(",");
            List<String> liczby = new ArrayList<>();

            for (String czesc : czesci) {
                if (!czesc.trim().isEmpty()) {
                    liczby.add(czesc.trim());
                }
            }

            if (liczby.size() < 2) {
                return null;
            }

            double lat = Double.parseDouble(liczby.get(0).replace(",", "."));
            double lon = Double.parseDouble(liczby.get(1).replace(",", "."));

            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return null;
            }

            return new GeoPoint(lat, lon);
        } catch (Exception e) {
            return null;
        }
    }

    private void geokoduj(String zapytanie, GeoCallback callback) {
        Context appContext = requireContext().getApplicationContext();

        executorService.execute(() -> {
            try {
                String encoded = URLEncoder.encode(zapytanie, "UTF-8");
                String adres = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encoded;

                HttpURLConnection connection = (HttpURLConnection) new URL(adres).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("User-Agent", appContext.getPackageName());

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONArray array = new JSONArray(response.toString());

                if (array.length() == 0) {
                    pokazBladGeokodowania(callback, "Nie znaleziono miejsca");
                    return;
                }

                JSONObject object = array.getJSONObject(0);

                double lat = Double.parseDouble(object.getString("lat"));
                double lon = Double.parseDouble(object.getString("lon"));
                String displayName = object.optString("display_name", zapytanie);

                GeoPoint point = new GeoPoint(lat, lon);

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> callback.onSuccess(point, displayName));
            } catch (Exception e) {
                pokazBladGeokodowania(callback, "Błąd wyszukiwania miejsca");
            }
        });
    }

    private void pokazBladGeokodowania(GeoCallback callback, String message) {
        if (!isAdded()) {
            return;
        }

        requireActivity().runOnUiThread(() -> callback.onError(message));
    }

    private void odswiezStanPunktow() {
        przeliczParametry();
        odswiezMapeKreatora();
        odswiezListePunktow();
        odswiezParametryRobocze();

        if (punkty.isEmpty()) {
            txtInstrukcjaPunktow.setText("Dodaj punkt startu przez nazwę, współrzędne albo kliknięcie na mapie.");
        } else if (punkty.size() == 1) {
            txtInstrukcjaPunktow.setText("Punkt startu dodany. Teraz dodaj punkt końcowy.");
        } else {
            txtInstrukcjaPunktow.setText("Trasa ma start i koniec. Możesz dodawać kolejne punkty pośrednie.");
        }

        btnSzukajDodajPunkt.setText(tekstPrzyciskuDodawaniaPunktu());
        btnUsunOstatniPunkt.setVisibility(punkty.isEmpty() ? View.GONE : View.VISIBLE);
        btnOptymalizujPunkty.setVisibility(punkty.size() >= 4 ? View.VISIBLE : View.GONE);
    }

    private String tekstPrzyciskuDodawaniaPunktu() {
        if (punkty.isEmpty()) {
            return "Dodaj punkt startu";
        }

        if (punkty.size() == 1) {
            return "Dodaj punkt końcowy";
        }

        return "Dodaj punkt pośredni";
    }

    private void usunOstatniPunkt() {
        if (punkty.isEmpty()) {
            return;
        }

        if (punkty.size() <= 2) {
            punkty.remove(punkty.size() - 1);
            return;
        }

        punkty.remove(punkty.size() - 2);
    }

    private void optymalizujKolejnoscPunktow() {
        if (punkty.size() < 4) {
            return;
        }

        PunktRoboczy start = punkty.get(0);
        PunktRoboczy koniec = punkty.get(punkty.size() - 1);
        List<PunktRoboczy> posrednie = new ArrayList<>();

        for (int i = 1; i < punkty.size() - 1; i++) {
            posrednie.add(punkty.get(i));
        }

        List<PunktRoboczy> uporzadkowane = nearestNeighbour(start, posrednie);
        poprawTwoOpt(start, uporzadkowane, koniec);

        punkty.clear();
        punkty.add(start);
        punkty.addAll(uporzadkowane);
        punkty.add(koniec);
    }

    private List<PunktRoboczy> nearestNeighbour(PunktRoboczy start, List<PunktRoboczy> wejscie) {
        List<PunktRoboczy> dostepne = new ArrayList<>(wejscie);
        List<PunktRoboczy> wynik = new ArrayList<>();
        PunktRoboczy aktualny = start;

        while (!dostepne.isEmpty()) {
            int najlepszyIndeks = 0;
            double najlepszaOdleglosc = odleglosc(aktualny, dostepne.get(0));

            for (int i = 1; i < dostepne.size(); i++) {
                double obecnaOdleglosc = odleglosc(aktualny, dostepne.get(i));

                if (obecnaOdleglosc < najlepszaOdleglosc) {
                    najlepszaOdleglosc = obecnaOdleglosc;
                    najlepszyIndeks = i;
                }
            }

            aktualny = dostepne.remove(najlepszyIndeks);
            wynik.add(aktualny);
        }

        return wynik;
    }

    private void poprawTwoOpt(PunktRoboczy start, List<PunktRoboczy> posrednie, PunktRoboczy koniec) {
        boolean poprawiono = true;

        while (poprawiono) {
            poprawiono = false;

            for (int i = 0; i < posrednie.size() - 1; i++) {
                for (int k = i + 1; k < posrednie.size(); k++) {
                    double przed = dlugoscPelnejTrasy(start, posrednie, koniec);
                    odwrocFragment(posrednie, i, k);
                    double po = dlugoscPelnejTrasy(start, posrednie, koniec);

                    if (po + 0.001 < przed) {
                        poprawiono = true;
                    } else {
                        odwrocFragment(posrednie, i, k);
                    }
                }
            }
        }
    }

    private void odwrocFragment(List<PunktRoboczy> lista, int start, int koniec) {
        while (start < koniec) {
            PunktRoboczy tmp = lista.get(start);
            lista.set(start, lista.get(koniec));
            lista.set(koniec, tmp);
            start++;
            koniec--;
        }
    }

    private double dlugoscPelnejTrasy(PunktRoboczy start, List<PunktRoboczy> posrednie, PunktRoboczy koniec) {
        double suma = 0.0;
        PunktRoboczy poprzedni = start;

        for (PunktRoboczy punkt : posrednie) {
            suma += odleglosc(poprzedni, punkt);
            poprzedni = punkt;
        }

        suma += odleglosc(poprzedni, koniec);
        return suma;
    }

    private double odleglosc(PunktRoboczy a, PunktRoboczy b) {
        return a.geoPoint.distanceToAsDouble(b.geoPoint);
    }

    private void odswiezMapeKreatora() {
        for (Marker marker : markeryKreator) {
            mapaKreator.getOverlays().remove(marker);
        }

        markeryKreator.clear();

        if (liniaKreator != null) {
            mapaKreator.getOverlays().remove(liniaKreator);
        }

        List<GeoPoint> geoPunkty = new ArrayList<>();

        for (int i = 0; i < punkty.size(); i++) {
            PunktRoboczy punkt = punkty.get(i);

            Marker marker = new Marker(mapaKreator);
            marker.setPosition(punkt.geoPoint);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle((i + 1) + ". " + punkt.nazwa);
            marker.setSnippet(punkt.kategoria);

            mapaKreator.getOverlays().add(marker);
            markeryKreator.add(marker);
            geoPunkty.add(punkt.geoPoint);
        }

        if (!geoPunkty.isEmpty()) {
            liniaKreator = new Polyline();
            liniaKreator.setPoints(geoPunkty);
            liniaKreator.setColor(Color.parseColor("#2563EB"));
            liniaKreator.setWidth(6f);
            mapaKreator.getOverlays().add(liniaKreator);
            mapaKreator.getController().animateTo(geoPunkty.get(geoPunkty.size() - 1));
        }

        mapaKreator.invalidate();
    }

    private void odswiezMapePodsumowania() {
        mapaPodsumowanie.getOverlays().clear();

        List<GeoPoint> geoPunkty = new ArrayList<>();

        for (int i = 0; i < punkty.size(); i++) {
            PunktRoboczy punkt = punkty.get(i);

            Marker marker = new Marker(mapaPodsumowanie);
            marker.setPosition(punkt.geoPoint);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle((i + 1) + ". " + punkt.nazwa);
            marker.setSnippet(punkt.kategoria);

            mapaPodsumowanie.getOverlays().add(marker);
            geoPunkty.add(punkt.geoPoint);
        }

        if (!geoPunkty.isEmpty()) {
            liniaPodsumowanie = new Polyline();
            liniaPodsumowanie.setPoints(geoPunkty);
            liniaPodsumowanie.setColor(Color.parseColor("#2563EB"));
            liniaPodsumowanie.setWidth(6f);
            mapaPodsumowanie.getOverlays().add(liniaPodsumowanie);

            mapaPodsumowanie.getController().setCenter(geoPunkty.get(0));
            mapaPodsumowanie.getController().setZoom(13.0);
        }

        mapaPodsumowanie.invalidate();
    }

    private void odswiezListePunktow() {
        if (punkty.isEmpty()) {
            txtListaPunktow.setText("Nie dodano jeszcze żadnego punktu.");
            return;
        }

        PunktRoboczy ostatni;

        if (punkty.size() <= 2) {
            ostatni = punkty.get(punkty.size() - 1);
        } else {
            ostatni = punkty.get(punkty.size() - 2);
        }

        int numer = punkty.indexOf(ostatni) + 1;

        txtListaPunktow.setText(
                "Ostatnio dodano:\n"
                        + "Punkt " + numer + " — " + ostatni.nazwa
                        + "\n" + ostatni.kategoria
        );
    }

    private void przeliczParametry() {
        dystansKm = obliczDystansKm();
        czasMin = oszacujCzasMin(dystansKm);
        przewyzszenieM = oszacujPrzewyzszenie(dystansKm);
        poziomTrudnosci = oszacujPoziom(dystansKm, przewyzszenieM);
    }

    private double obliczDystansKm() {
        if (punkty.size() < 2) {
            return 0.0;
        }

        double suma = 0.0;

        for (int i = 0; i < punkty.size() - 1; i++) {
            GeoPoint a = punkty.get(i).geoPoint;
            GeoPoint b = punkty.get(i + 1).geoPoint;
            suma += a.distanceToAsDouble(b) / 1000.0;
        }

        return suma;
    }

    private int oszacujCzasMin(double dystans) {
        if (dystans <= 0) {
            return 0;
        }

        double predkoscKmH;

        if ("Rowerowa".equalsIgnoreCase(typTrasy)) {
            predkoscKmH = 14.0;
        } else if ("Górska".equalsIgnoreCase(typTrasy)) {
            predkoscKmH = 3.5;
        } else {
            predkoscKmH = 4.8;
        }

        return Math.max(10, (int) Math.round((dystans / predkoscKmH) * 60.0));
    }

    private int oszacujPrzewyzszenie(double dystans) {
        if ("Górska".equalsIgnoreCase(typTrasy)) {
            return (int) Math.round(dystans * 90);
        }

        if ("Rowerowa".equalsIgnoreCase(typTrasy)) {
            return (int) Math.round(dystans * 25);
        }

        if ("Przyrodnicza".equalsIgnoreCase(typTrasy)) {
            return (int) Math.round(dystans * 35);
        }

        return (int) Math.round(dystans * 10);
    }

    private String oszacujPoziom(double dystans, int przewyzszenie) {
        if (dystans <= 0) {
            return "Brak danych";
        }

        if (dystans < 5 && przewyzszenie < 100) {
            return "Łatwa";
        }

        if (dystans < 12 && przewyzszenie < 400) {
            return "Średnia";
        }

        return "Trudna";
    }

    private void odswiezParametryRobocze() {
        txtParametryRobocze.setText(
                "Punkty: " + punkty.size()
                        + " • Dystans: " + String.format(Locale.ROOT, "%.1f", dystansKm)
                        + " km • Czas: " + czasMin
                        + " min • Poziom: " + poziomTrudnosci
        );
    }

    private void odswiezPodsumowanie() {
        zapiszDanePodstawowe();
        przeliczParametry();
        odswiezMapePodsumowania();

        StringBuilder builder = new StringBuilder();

        builder.append("Nazwa: ").append(nazwaTrasy).append("\n");
        builder.append("Region: ").append(regionTrasy).append("\n");
        builder.append("Typ: ").append(typTrasy).append("\n");
        builder.append("Liczba punktów: ").append(punkty.size()).append("\n");
        builder.append("Dystans: ").append(String.format(Locale.ROOT, "%.1f", dystansKm)).append(" km\n");
        builder.append("Szacowany czas: ").append(czasMin).append(" min\n");
        builder.append("Przewyższenie: ").append(przewyzszenieM).append(" m\n");
        builder.append("Poziom trudności: ").append(poziomTrudnosci).append("\n");

        if (!TextUtils.isEmpty(opisTrasy)) {
            builder.append("\nOpis:\n").append(opisTrasy);
        }

        txtPodsumowanieTrasy.setText(builder.toString());
    }

    private void zapiszTrase() {
        if (!walidujKrokDane()) {
            pokazKrok(1);
            return;
        }

        if (!walidujKrokPunkty()) {
            pokazKrok(2);
            return;
        }

        zapiszDanePodstawowe();
        przeliczParametry();

        Trasa trasa = new Trasa();
        UUID trasaId;

        if (trybEdycji && edytowanaTrasaId != null) {
            trasaId = edytowanaTrasaId;
            trasa.setnId(edytowanaTrasaId);
        } else {
            trasaId = trasa.getnId();
        }

        trasa.setNazwa(nazwaTrasy);
        trasa.setOpis(opisTrasy);
        trasa.setRegion(regionTrasy);
        trasa.setDystansKm(dystansKm);
        trasa.setPrzewyzszenieM(przewyzszenieM);
        trasa.setCzasMin(czasMin);
        trasa.setPoziomTrudnosci(poziomTrudnosci);
        trasa.setTyp(typTrasy);
        trasa.setDev(false);
        trasa.setUlubiona(false);

        List<PunktTrasy> punktyDoZapisu = new ArrayList<>();

        for (int i = 0; i < punkty.size(); i++) {
            PunktRoboczy roboczy = punkty.get(i);

            PunktTrasy punkt = new PunktTrasy();
            punkt.setTrasaId(trasaId);
            punkt.setNazwa(roboczy.nazwa);
            punkt.setOpis("");
            punkt.setKategoria(roboczy.kategoria);
            punkt.setLatitude(roboczy.geoPoint.getLatitude());
            punkt.setLongitude(roboczy.geoPoint.getLongitude());
            punkt.setKolejnosc(i + 1);

            punktyDoZapisu.add(punkt);
        }

        if (trybEdycji && edytowanaTrasaId != null) {
            trasyViewModel.zmienTrase(trasa);
            trasyViewModel.zastapPunktyTrasy(edytowanaTrasaId, punktyDoZapisu, () -> {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), "Trasa została zaktualizowana", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            });

            return;
        }

        trasyViewModel.dodajTrase(trasa);

        for (PunktTrasy punkt : punktyDoZapisu) {
            trasyViewModel.dodajPunkt(punkt);
        }

        Toast.makeText(requireContext(), "Trasa została dodana", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }

    private void potwierdzWyjscie() {
        if (!czySaDaneRobocze()) {
            getParentFragmentManager().popBackStack();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Uwaga")
                .setMessage("Niezapisane dane zostaną utracone. Czy na pewno chcesz wyjść?")
                .setPositiveButton("Wyjdź", (dialog, which) -> getParentFragmentManager().popBackStack())
                .setNegativeButton("Zostań", null)
                .show();
    }

    public boolean czySaNiezapisaneDane() {
        return czySaDaneRobocze();
    }

    public void potwierdzAnulowanieKreatora(Runnable poPotwierdzeniu) {
        if (!czySaDaneRobocze()) {
            if (poPotwierdzeniu != null) {
                poPotwierdzeniu.run();
            }
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Anulować kreator?")
                .setMessage("Przejście do innej zakładki anuluje dodawanie lub edycję trasy i wyzeruje formularz. Czy na pewno chcesz kontynuować?")
                .setPositiveButton("Anuluj kreator", (dialog, which) -> {
                    if (poPotwierdzeniu != null) {
                        poPotwierdzeniu.run();
                    }
                })
                .setNegativeButton("Zostań tutaj", null)
                .show();
    }

    private boolean czySaDaneRobocze() {
        return !TextUtils.isEmpty(tekst(edtNazwa))
                || !TextUtils.isEmpty(tekst(edtOpis))
                || !TextUtils.isEmpty(tekst(edtRegion))
                || !TextUtils.isEmpty(tekst(edtNazwaPunktu))
                || !punkty.isEmpty();
    }

    private String tekst(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int dp(int wartosc) {
        return (int) (wartosc * getResources().getDisplayMetrics().density);
    }

    private interface GeoCallback {
        void onSuccess(GeoPoint point, String displayName);

        void onError(String message);
    }

    private static class PunktRoboczy {

        private final String nazwa;
        private final String kategoria;
        private final GeoPoint geoPoint;

        private PunktRoboczy(String nazwa, String kategoria, GeoPoint geoPoint) {
            this.nazwa = nazwa;
            this.kategoria = kategoria;
            this.geoPoint = geoPoint;
        }
    }
}
