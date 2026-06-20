package com.example.projekt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FragmentTrasy extends Fragment {

    private static final String PREFS_NAME = "tourroute_prefs";

    // Klucze intencji używane przy wybieraniu lokalizacji punktu na mapie.
    // UWAGA: aby opcja "Wskaż na mapie" działała, MapaTrasyActivity musi:
    //  - sprawdzić boolean extra EXTRA_TRYB_WYBORU_PUNKTU,
    //  - po tapnięciu na mapie wywołać setResult(RESULT_OK, intent z extrami
    //    EXTRA_WYBRANA_LAT / EXTRA_WYBRANA_LON) i zakończyć działanie (finish()).
    private static final String EXTRA_TRYB_WYBORU_PUNKTU = "tryb_wyboru_punktu";
    private static final String EXTRA_WYBRANA_LAT = "wybrana_lat";
    private static final String EXTRA_WYBRANA_LON = "wybrana_lon";

    private TrasyViewModel trasyViewModel;

    private LinearLayout kontenerBloczkowFiltrow;
    private LinearLayout kontenerPodpowiedziFiltrow;
    private LinearLayout sekcjaMojeTrasy;
    private LinearLayout kontenerMojeTrasy;
    private LinearLayout kontenerPunktow;

    private TextView txtBrakMoichTras;
    private TextView txtBrakProponowanychTras;
    private TextView txtBrakPunktow;

    private TextInputEditText edtDodajFiltr;
    private TextInputEditText edtSzukajPunkt;

    private Button btnDodajFiltr;
    private Button btnWyczyscFiltry;
    private Button btnDodajTrase;
    private Button btnRozwinMojeTrasy;
    private Button btnPokazWszystkieMojeTrasy;
    private Button btnRozwinPunkty;
    private Button btnDodajPunkt;
    private Button btnRozwinProponowaneTrasy;

    private RecyclerView recyclerTrasy;

    private TrasyAdapter adapterProponowaneTrasy;

    private ActivityResultLauncher<String[]> lokalizacjaLauncher;
    private ActivityResultLauncher<Intent> wyborLokalizacjiLauncher;

    private final List<Trasa> wszystkieTrasy = new ArrayList<>();
    private final List<PunktTrasy> wszystkiePunkty = new ArrayList<>();
    private final List<String> filtry = new ArrayList<>();
    private final List<String> podpowiedziFiltrow = new ArrayList<>();
    private final List<Trasa> proponowaneTrasyAktualne = new ArrayList<>();

    private boolean pokazWszystkieMojeTrasy = false;
    private boolean pokazWszystkiePunkty = false;
    private boolean pokazWszystkieProponowaneTrasy = false;
    private boolean dodanoFiltrGps = false;

    private OdbiorcaWybranejLokalizacji odbiorcaWybranejLokalizacji;

    /**
     * Prosty interfejs pozwalający przekazać wynik wybrania punktu na mapie
     * do aktualnie otwartego dialogu dodawania punktu.
     */
    private interface OdbiorcaWybranejLokalizacji {
        void onWybranoLokalizacje(double lat, double lon);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lokalizacjaLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> pobierzObecnaLokalizacje()
        );

        wyborLokalizacjiLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }

                    if (odbiorcaWybranejLokalizacji == null) {
                        return;
                    }

                    Intent dane = result.getData();
                    double lat = dane.getDoubleExtra(EXTRA_WYBRANA_LAT, Double.NaN);
                    double lon = dane.getDoubleExtra(EXTRA_WYBRANA_LON, Double.NaN);

                    if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                        odbiorcaWybranejLokalizacji.onWybranoLokalizacje(lat, lon);
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trasy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        przypiszWidoki(view);
        przygotujRecyclerView();
        ustawListenery();

        trasyViewModel = new ViewModelProvider(requireActivity()).get(TrasyViewModel.class);

        trasyViewModel.getTrasy().observe(getViewLifecycleOwner(), trasy -> {
            wszystkieTrasy.clear();

            if (trasy != null) {
                wszystkieTrasy.addAll(trasy);
            }

            zbudujPodpowiedziFiltrow();
            odswiezWidokTras();
            odswiezPodpowiedziFiltrow();
        });

        trasyViewModel.getPunkty().observe(getViewLifecycleOwner(), punkty -> {
            wszystkiePunkty.clear();

            if (punkty != null) {
                wszystkiePunkty.addAll(punkty);
            }

            odswiezPunkty();
        });

        odswiezWidokZalogowania();
        sprawdzUprawnieniaLokalizacji();

        trasyViewModel.pobierzTrasy();
        trasyViewModel.pobierzPunkty();
    }

    @Override
    public void onResume() {
        super.onResume();

        odswiezWidokZalogowania();

        if (trasyViewModel != null) {
            trasyViewModel.pobierzTrasy();
            trasyViewModel.pobierzPunkty();
        }

        odswiezWidokTras();
    }

    private void przypiszWidoki(View view) {
        kontenerBloczkowFiltrow = view.findViewById(R.id.kontenerBloczkowFiltrow);
        kontenerPodpowiedziFiltrow = view.findViewById(R.id.kontenerPodpowiedziFiltrow);
        sekcjaMojeTrasy = view.findViewById(R.id.sekcjaMojeTrasy);
        kontenerMojeTrasy = view.findViewById(R.id.kontenerMojeTrasy);
        kontenerPunktow = view.findViewById(R.id.kontenerPunktow);

        txtBrakMoichTras = view.findViewById(R.id.txtBrakMoichTras);
        txtBrakProponowanychTras = view.findViewById(R.id.txtBrakProponowanychTras);
        txtBrakPunktow = view.findViewById(R.id.txtBrakPunktow);

        edtDodajFiltr = view.findViewById(R.id.edtDodajFiltr);
        edtSzukajPunkt = view.findViewById(R.id.edtSzukajPunkt);

        btnDodajFiltr = view.findViewById(R.id.btnDodajFiltr);
        btnWyczyscFiltry = view.findViewById(R.id.btnWyczyscFiltry);
        btnDodajTrase = view.findViewById(R.id.btnDodajTrase);
        btnRozwinMojeTrasy = view.findViewById(R.id.btnRozwinMojeTrasy);
        btnPokazWszystkieMojeTrasy = view.findViewById(R.id.btnPokazWszystkieMojeTrasy);
        btnRozwinPunkty = view.findViewById(R.id.btnRozwinPunkty);
        btnDodajPunkt = view.findViewById(R.id.btnDodajPunkt);
        btnRozwinProponowaneTrasy = view.findViewById(R.id.btnRozwinProponowaneTrasy);

        recyclerTrasy = view.findViewById(R.id.recyclerTrasy);
    }

    private void przygotujRecyclerView() {
        adapterProponowaneTrasy = new TrasyAdapter(new ArrayList<>());
        recyclerTrasy.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTrasy.setNestedScrollingEnabled(false);
        recyclerTrasy.setAdapter(adapterProponowaneTrasy);
    }

    private void ustawListenery() {
        btnDodajTrase.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main, new FragmentDodajTrase())
                .addToBackStack(null)
                .commit());

        btnDodajFiltr.setOnClickListener(v -> dodajFiltrZPola());

        btnWyczyscFiltry.setOnClickListener(v -> {
            filtry.clear();
            pokazWszystkieMojeTrasy = false;
            pokazWszystkieProponowaneTrasy = false;
            btnPokazWszystkieMojeTrasy.setText("Pokaż wszystkie");

            odswiezBloczkiFiltrow();
            odswiezWidokTras();
            odswiezPodpowiedziFiltrow();
        });

        btnRozwinMojeTrasy.setVisibility(View.GONE);

        btnRozwinPunkty.setOnClickListener(v -> {
            pokazWszystkiePunkty = !pokazWszystkiePunkty;
            odswiezPunkty();
        });

        btnDodajPunkt.setOnClickListener(v -> pokazDialogDodajPunkt());

        btnRozwinProponowaneTrasy.setOnClickListener(v -> {
            pokazWszystkieProponowaneTrasy = !pokazWszystkieProponowaneTrasy;
            odswiezListeProponowanychTras();
        });

        btnPokazWszystkieMojeTrasy.setOnClickListener(v -> {
            pokazWszystkieMojeTrasy = !pokazWszystkieMojeTrasy;

            if (pokazWszystkieMojeTrasy) {
                btnPokazWszystkieMojeTrasy.setText("Ukryj");
            } else {
                btnPokazWszystkieMojeTrasy.setText("Pokaż wszystkie");
            }

            odswiezMojeTrasy();
        });

        edtDodajFiltr.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                odswiezPodpowiedziFiltrow();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        edtSzukajPunkt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                odswiezPunkty();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        edtDodajFiltr.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_SEND) {
                dodajFiltrZPola();
                return true;
            }

            return false;
        });
    }

    private void odswiezWidokZalogowania() {
        if (czyZalogowany()) {
            sekcjaMojeTrasy.setVisibility(View.VISIBLE);
        } else {
            sekcjaMojeTrasy.setVisibility(View.GONE);
        }
    }

    private boolean czyZalogowany() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("uzytkownik_zalogowany", false);
    }

    private void sprawdzUprawnieniaLokalizacji() {
        boolean fine = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarse = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            pobierzObecnaLokalizacje();
        } else {
            lokalizacjaLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void pobierzObecnaLokalizacje() {
        if (dodanoFiltrGps) {
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            odswiezBloczkiFiltrow();
            odswiezProponowaneTrasy();
            return;
        }

        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            odswiezBloczkiFiltrow();
            odswiezProponowaneTrasy();
            return;
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        location -> ustawFiltrZGps(location.getLatitude(), location.getLongitude()),
                        Looper.getMainLooper()
                );
                return;
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER,
                        location -> ustawFiltrZGps(location.getLatitude(), location.getLongitude()),
                        Looper.getMainLooper()
                );
                return;
            }

            Location ostatnia = pobierzOstatniaZnana(locationManager);

            if (ostatnia != null) {
                ustawFiltrZGps(ostatnia.getLatitude(), ostatnia.getLongitude());
            } else {
                odswiezBloczkiFiltrow();
                odswiezProponowaneTrasy();
            }
        } catch (Exception e) {
            Location ostatnia = pobierzOstatniaZnana(locationManager);

            if (ostatnia != null) {
                ustawFiltrZGps(ostatnia.getLatitude(), ostatnia.getLongitude());
            }
        }
    }

    private Location pobierzOstatniaZnana(LocationManager locationManager) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (gps != null && network != null) {
            return gps.getTime() > network.getTime() ? gps : network;
        }

        if (gps != null) {
            return gps;
        }

        return network;
    }

    private void ustawFiltrZGps(double lat, double lon) {
        if (dodanoFiltrGps) {
            return;
        }

        dodanoFiltrGps = true;

        new Thread(() -> {
            String lokalizacja = "Twoja okolica";

            try {
                if (!isAdded()) {
                    return;
                }

                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> adresy = geocoder.getFromLocation(lat, lon, 1);

                if (adresy != null && !adresy.isEmpty()) {
                    Address adres = adresy.get(0);

                    if (adres.getLocality() != null) {
                        lokalizacja = adres.getLocality();
                    } else if (adres.getSubAdminArea() != null) {
                        lokalizacja = adres.getSubAdminArea();
                    } else if (adres.getAdminArea() != null) {
                        lokalizacja = adres.getAdminArea();
                    }
                }
            } catch (IOException | IllegalArgumentException ignored) {
            }

            String finalLokalizacja = lokalizacja;

            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }

                if (filtry.isEmpty()) {
                    dodajFiltr(finalLokalizacja, false);
                }
            });
        }).start();
    }

    private void dodajFiltrZPola() {
        String tekst = edtDodajFiltr.getText() == null
                ? ""
                : edtDodajFiltr.getText().toString().trim();

        if (tekst.isEmpty()) {
            edtDodajFiltr.setError("Wpisz filtr");
            return;
        }

        dodajFiltr(tekst, true);
    }

    private void dodajFiltr(String filtr, boolean czyscPole) {
        if (filtr == null || filtr.trim().isEmpty()) {
            return;
        }

        String nowy = filtr.trim();

        for (String istnieje : filtry) {
            if (normalizuj(istnieje).equals(normalizuj(nowy))) {
                Toast.makeText(requireContext(), "Ten filtr jest już aktywny", Toast.LENGTH_SHORT).show();

                if (czyscPole) {
                    edtDodajFiltr.setText("");
                }

                return;
            }
        }

        filtry.add(nowy);

        if (czyscPole) {
            edtDodajFiltr.setText("");
        }

        pokazWszystkieMojeTrasy = false;
        pokazWszystkieProponowaneTrasy = false;
        btnPokazWszystkieMojeTrasy.setText("Pokaż wszystkie");

        odswiezBloczkiFiltrow();
        odswiezWidokTras();
        odswiezPodpowiedziFiltrow();
    }

    private void usunFiltr(String filtr) {
        filtry.remove(filtr);

        pokazWszystkieMojeTrasy = false;
        pokazWszystkieProponowaneTrasy = false;
        btnPokazWszystkieMojeTrasy.setText("Pokaż wszystkie");

        odswiezBloczkiFiltrow();
        odswiezWidokTras();
        odswiezPodpowiedziFiltrow();
    }

    private void odswiezBloczkiFiltrow() {
        kontenerBloczkowFiltrow.removeAllViews();

        if (filtry.isEmpty()) {
            return;
        }

        for (String filtr : filtry) {
            TextView bloczek = new TextView(requireContext());
            bloczek.setText(filtr + "  ×");
            bloczek.setTextColor(Color.parseColor("#111827"));
            bloczek.setTextSize(14);
            bloczek.setTypeface(null, Typeface.BOLD);
            bloczek.setPadding(dp(14), dp(8), dp(14), dp(8));
            bloczek.setBackgroundResource(R.drawable.bg_panel_warning);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, dp(10), 0);
            bloczek.setLayoutParams(params);

            bloczek.setOnClickListener(v -> usunFiltr(filtr));

            kontenerBloczkowFiltrow.addView(bloczek);
        }
    }

    private void zbudujPodpowiedziFiltrow() {
        Set<String> zbior = new LinkedHashSet<>();

        for (Trasa trasa : wszystkieTrasy) {
            dodajDoZbioruPodpowiedzi(zbior, trasa.getNazwa());
            dodajDoZbioruPodpowiedzi(zbior, trasa.getRegion());
            dodajDoZbioruPodpowiedzi(zbior, trasa.getTyp());
            dodajDoZbioruPodpowiedzi(zbior, trasa.getPoziomTrudnosci());
        }

        podpowiedziFiltrow.clear();
        podpowiedziFiltrow.addAll(zbior);

        Collator collator = Collator.getInstance(new Locale("pl", "PL"));
        podpowiedziFiltrow.sort(collator);
    }

    private void dodajDoZbioruPodpowiedzi(Set<String> zbior, String tekst) {
        if (tekst == null || tekst.trim().isEmpty()) {
            return;
        }

        String wartosc = tekst.trim();

        for (String istnieje : zbior) {
            if (normalizuj(istnieje).equals(normalizuj(wartosc))) {
                return;
            }
        }

        zbior.add(wartosc);
    }

    private void odswiezPodpowiedziFiltrow() {
        kontenerPodpowiedziFiltrow.removeAllViews();

        String wpisany = edtDodajFiltr.getText() == null
                ? ""
                : edtDodajFiltr.getText().toString().trim();

        if (wpisany.isEmpty()) {
            kontenerPodpowiedziFiltrow.setVisibility(View.GONE);
            return;
        }

        List<String> pasujace = new ArrayList<>();
        String normalnyWpisany = normalizuj(wpisany);

        for (String podpowiedz : podpowiedziFiltrow) {
            if (czyFiltrAktywny(podpowiedz)) {
                continue;
            }

            String normalnaPodpowiedz = normalizuj(podpowiedz);

            if (normalnaPodpowiedz.startsWith(normalnyWpisany)
                    || normalnaPodpowiedz.contains(normalnyWpisany)) {
                pasujace.add(podpowiedz);
            }

            if (pasujace.size() >= 5) {
                break;
            }
        }

        if (pasujace.isEmpty()) {
            kontenerPodpowiedziFiltrow.setVisibility(View.GONE);
            return;
        }

        kontenerPodpowiedziFiltrow.setVisibility(View.VISIBLE);

        for (String podpowiedz : pasujace) {
            TextView textView = new TextView(requireContext());
            textView.setText("Dodaj: " + podpowiedz);
            textView.setTextColor(Color.parseColor("#2563EB"));
            textView.setTextSize(14);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setPadding(dp(12), dp(8), dp(12), dp(8));
            textView.setBackgroundResource(R.drawable.bg_card_route);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dp(6));
            textView.setLayoutParams(params);

            textView.setOnClickListener(v -> dodajFiltr(podpowiedz, true));

            kontenerPodpowiedziFiltrow.addView(textView);
        }
    }

    private boolean czyFiltrAktywny(String tekst) {
        for (String filtr : filtry) {
            if (normalizuj(filtr).equals(normalizuj(tekst))) {
                return true;
            }
        }

        return false;
    }

    private void odswiezWidokTras() {
        odswiezMojeTrasy();
        odswiezProponowaneTrasy();
    }

    private List<Trasa> pobierzWszystkieMojeTrasy() {
        List<Trasa> moje = new ArrayList<>();

        for (Trasa trasa : wszystkieTrasy) {
            if (!trasa.isDev()) {
                moje.add(trasa);
            }
        }

        return moje;
    }

    private void odswiezMojeTrasy() {
        if (!czyZalogowany()) {
            sekcjaMojeTrasy.setVisibility(View.GONE);
            kontenerMojeTrasy.removeAllViews();
            return;
        }

        sekcjaMojeTrasy.setVisibility(View.VISIBLE);
        btnRozwinMojeTrasy.setVisibility(View.GONE);

        List<Trasa> mojeWszystkie = pobierzWszystkieMojeTrasy();

        if (mojeWszystkie.isEmpty()) {
            kontenerMojeTrasy.setVisibility(View.GONE);
            txtBrakMoichTras.setVisibility(View.VISIBLE);
            txtBrakMoichTras.setText("To jest czas, aby zacząć planować swoje drogi po swojemu.");
            kontenerMojeTrasy.removeAllViews();
            return;
        }

        kontenerMojeTrasy.setVisibility(View.VISIBLE);
        txtBrakMoichTras.setVisibility(View.GONE);

        if (pokazWszystkieMojeTrasy) {
            btnPokazWszystkieMojeTrasy.setText("Ukryj");
            wyswietlMojeTrasy(mojeWszystkie);
            return;
        }

        btnPokazWszystkieMojeTrasy.setText("Pokaż wszystkie");

        List<Trasa> mojeFiltrowane = new ArrayList<>();

        for (Trasa trasa : mojeWszystkie) {
            if (filtry.isEmpty() || pasujeDoFiltrow(trasa)) {
                mojeFiltrowane.add(trasa);
            }
        }

        if (mojeFiltrowane.isEmpty()) {
            kontenerMojeTrasy.setVisibility(View.GONE);
            txtBrakMoichTras.setVisibility(View.VISIBLE);
            txtBrakMoichTras.setText("Nie znaleziono Twoich tras pasujących do aktywnych filtrów.");
            kontenerMojeTrasy.removeAllViews();
            return;
        }

        List<Trasa> doPokazania = new ArrayList<>();
        int limit = Math.min(3, mojeFiltrowane.size());

        for (int i = 0; i < limit; i++) {
            doPokazania.add(mojeFiltrowane.get(i));
        }

        wyswietlMojeTrasy(doPokazania);
    }

    private void wyswietlMojeTrasy(List<Trasa> trasy) {
        kontenerMojeTrasy.removeAllViews();

        for (Trasa trasa : trasy) {
            View karta = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_trasa_list, kontenerMojeTrasy, false);

            TextView txtNazwa = karta.findViewById(R.id.txtNazwaTrasyLista);
            TextView txtOpis = karta.findViewById(R.id.txtOpisTrasyLista);
            TextView txtMeta = karta.findViewById(R.id.txtMetaTrasyLista);
            TextView txtTrudnosc = karta.findViewById(R.id.txtTrudnoscTrasyLista);
            TextView txtOpcje = karta.findViewById(R.id.txtOpcjeTrasyLista);
            Button btnMapa = karta.findViewById(R.id.btnMapaTrasyLista);

            txtNazwa.setText(trasa.getNazwa());
            txtOpis.setText(trasa.getOpis());

            String meta = String.format(
                    Locale.ROOT,
                    "%s • %.1f km • +%d m • %s",
                    bezNulla(trasa.getRegion()),
                    trasa.getDystansKm(),
                    trasa.getPrzewyzszenieM(),
                    trasa.getCzasTekst()
            );

            txtMeta.setText(meta);
            txtTrudnosc.setText(trasa.getPoziomTrudnosci() + " • " + trasa.getTyp() + " • moja");

            txtOpcje.setVisibility(View.VISIBLE);
            txtOpcje.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(requireContext(), txtOpcje);
                popupMenu.getMenu().add("Edytuj");
                popupMenu.getMenu().add("Usuń");

                popupMenu.setOnMenuItemClickListener(item -> {
                    String tytul = item.getTitle().toString();

                    if ("Edytuj".equals(tytul)) {
                        otworzEdycjeTrasy(trasa);
                        return true;
                    }

                    if ("Usuń".equals(tytul)) {
                        potwierdzUsuniecieTrasy(trasa);
                        return true;
                    }

                    return false;
                });

                popupMenu.show();
            });

            btnMapa.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MapaTrasyActivity.class);
                intent.putExtra(MapaTrasyActivity.EXTRA_TRASA_ID, trasa.getnId().toString());
                intent.putExtra(MapaTrasyActivity.EXTRA_TRASA_NAZWA, trasa.getNazwa());
                startActivity(intent);
            });

            kontenerMojeTrasy.addView(karta);
        }
    }

    private void otworzEdycjeTrasy(Trasa trasa) {
        if (trasa == null) {
            return;
        }

        FragmentDodajTrase fragment = new FragmentDodajTrase();

        Bundle args = new Bundle();
        args.putString("tryb", "edycja");
        args.putString("trasa_id", trasa.getnId().toString());
        fragment.setArguments(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.main, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void potwierdzUsuniecieTrasy(Trasa trasa) {
        if (trasa == null) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Usuń trasę")
                .setMessage("Czy na pewno chcesz usunąć trasę \"" + trasa.getNazwa() + "\"?")
                .setPositiveButton("Usuń", (dialog, which) -> {
                    trasyViewModel.usunTrase(trasa.getnId());
                    Toast.makeText(requireContext(), "Trasa została usunięta", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void odswiezPunkty() {
        if (kontenerPunktow == null || txtBrakPunktow == null) {
            return;
        }

        kontenerPunktow.removeAllViews();

        String filtr = edtSzukajPunkt == null || edtSzukajPunkt.getText() == null
                ? ""
                : edtSzukajPunkt.getText().toString().trim();

        List<PunktTrasy> punkty = new ArrayList<>();
        String normalnyFiltr = normalizuj(filtr);

        for (PunktTrasy punkt : wszystkiePunkty) {
            if (punkt == null) {
                continue;
            }

            String nazwa = punkt.getNazwa() == null ? "" : punkt.getNazwa();
            if (!normalnyFiltr.isEmpty() && !normalizuj(nazwa).contains(normalnyFiltr)) {
                continue;
            }

            punkty.add(punkt);
        }

        if (punkty.isEmpty()) {
            txtBrakPunktow.setVisibility(View.VISIBLE);

            if (btnRozwinPunkty != null) {
                btnRozwinPunkty.setVisibility(View.GONE);
            }

            return;
        }

        txtBrakPunktow.setVisibility(View.GONE);

        if (btnRozwinPunkty != null) {
            if (punkty.size() > 3) {
                btnRozwinPunkty.setVisibility(View.VISIBLE);
                btnRozwinPunkty.setText(pokazWszystkiePunkty ? "Zwiń" : "Rozwiń");
            } else {
                btnRozwinPunkty.setVisibility(View.GONE);
                pokazWszystkiePunkty = false;
            }
        }

        int limit = pokazWszystkiePunkty ? punkty.size() : Math.min(3, punkty.size());
        for (int i = 0; i < limit; i++) {
            kontenerPunktow.addView(utworzKartePunktu(punkty.get(i)));
        }
    }

    private View utworzKartePunktu(PunktTrasy punkt) {
        LinearLayout karta = new LinearLayout(requireContext());
        karta.setOrientation(LinearLayout.VERTICAL);
        karta.setPadding(dp(14), dp(12), dp(14), dp(12));
        karta.setBackgroundResource(R.drawable.bg_card_route);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        karta.setLayoutParams(params);

        String nazwa = TextUtils.isEmpty(punkt.getNazwa()) ? "Punkt bez nazwy" : punkt.getNazwa();
        TextView txtNazwa = tekstKartyPunktu(nazwa, 16, "#111827", true);
        karta.addView(txtNazwa);

        String meta = bezNulla(punkt.getKategoria()) + " • "
                + String.format(Locale.ROOT, "%.5f, %.5f", punkt.getLatitude(), punkt.getLongitude());
        karta.addView(tekstKartyPunktu(meta, 13, "#4B5563", false));

        LinearLayout przyciski = new LinearLayout(requireContext());
        przyciski.setOrientation(LinearLayout.HORIZONTAL);
        przyciski.setPadding(0, dp(8), 0, 0);

        Button btnPokaz = new Button(requireContext());
        btnPokaz.setText("Pokaż");
        btnPokaz.setOnClickListener(v -> pokazSzczegolyPunktu(punkt));
        przyciski.addView(btnPokaz, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button btnTrasa = new Button(requireContext());
        btnTrasa.setText("Utwórz trasę");
        btnTrasa.setTextColor(Color.WHITE);
        btnTrasa.setBackgroundColor(Color.parseColor("#7C3AED"));
        btnTrasa.setOnClickListener(v -> otworzKreatorZPunktem(punkt));
        LinearLayout.LayoutParams trasaParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        trasaParams.setMargins(dp(8), 0, 0, 0);
        przyciski.addView(btnTrasa, trasaParams);

        karta.addView(przyciski);
        return karta;
    }

    private TextView tekstKartyPunktu(String tekst, int rozmiar, String kolor, boolean pogrubiony) {
        TextView textView = new TextView(requireContext());
        textView.setText(tekst);
        textView.setTextSize(rozmiar);
        textView.setTextColor(Color.parseColor(kolor));

        if (pogrubiony) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        return textView;
    }

    private void pokazSzczegolyPunktu(PunktTrasy punkt) {
        String wiadomosc = "Kategoria: " + bezNulla(punkt.getKategoria())
                + "\nWspółrzędne: "
                + String.format(Locale.ROOT, "%.6f, %.6f", punkt.getLatitude(), punkt.getLongitude())
                + "\n\nTen punkt możesz dodać jako start nowej trasy.";

        new AlertDialog.Builder(requireContext())
                .setTitle(TextUtils.isEmpty(punkt.getNazwa()) ? "Punkt" : punkt.getNazwa())
                .setMessage(wiadomosc)
                .setPositiveButton("Utwórz trasę", (dialog, which) -> otworzKreatorZPunktem(punkt))
                .setNegativeButton("Zamknij", null)
                .show();
    }

    private void otworzKreatorZPunktem(PunktTrasy punkt) {
        FragmentDodajTrase fragment = new FragmentDodajTrase();
        Bundle args = new Bundle();
        args.putString("punkt_start_nazwa", punkt.getNazwa());
        args.putString("punkt_start_kategoria", punkt.getKategoria());
        args.putDouble("punkt_start_lat", punkt.getLatitude());
        args.putDouble("punkt_start_lon", punkt.getLongitude());
        fragment.setArguments(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.main, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Pokazuje dialog dodawania nowego, samodzielnego punktu.
     * Lokalizację można ustalić trzema sposobami: po nazwie/adresie (geokodowanie),
     * przez podanie współrzędnych ręcznie, albo przez wskazanie miejsca na mapie.
     */
    private void pokazDialogDodajPunkt() {
        Context context = requireContext();
        int padding = dp(20);

        LinearLayout kontener = new LinearLayout(context);
        kontener.setOrientation(LinearLayout.VERTICAL);
        kontener.setPadding(padding, dp(16), padding, dp(4));

        EditText edtNazwa = new EditText(context);
        edtNazwa.setHint("Nazwa punktu");
        kontener.addView(edtNazwa);

        EditText edtKategoria = new EditText(context);
        edtKategoria.setHint("Kategoria (np. Widok, Schronisko, Parking)");
        kontener.addView(edtKategoria, marginTopParams(dp(10)));

        TextView txtSposob = new TextView(context);
        txtSposob.setText("Wybierz sposób ustalenia lokalizacji:");
        txtSposob.setTextColor(Color.parseColor("#111827"));
        txtSposob.setTypeface(null, Typeface.BOLD);
        kontener.addView(txtSposob, marginTopParams(dp(18)));

        RadioGroup grupaSposobow = new RadioGroup(context);
        grupaSposobow.setOrientation(LinearLayout.VERTICAL);

        RadioButton rbNazwa = new RadioButton(context);
        rbNazwa.setId(View.generateViewId());
        rbNazwa.setText("Po nazwie / adresie miejsca");
        rbNazwa.setChecked(true);
        grupaSposobow.addView(rbNazwa);

        RadioButton rbWspolrzedne = new RadioButton(context);
        rbWspolrzedne.setId(View.generateViewId());
        rbWspolrzedne.setText("Podaj współrzędne");
        grupaSposobow.addView(rbWspolrzedne);

        RadioButton rbMapa = new RadioButton(context);
        rbMapa.setId(View.generateViewId());
        rbMapa.setText("Wskaż na mapie");
        grupaSposobow.addView(rbMapa);

        kontener.addView(grupaSposobow, marginTopParams(dp(6)));

        // Sekcja: po nazwie / adresie.
        EditText edtAdres = new EditText(context);
        edtAdres.setHint("Wpisz nazwę miejsca lub adres");
        kontener.addView(edtAdres, marginTopParams(dp(10)));

        // Sekcja: współrzędne.
        LinearLayout panelWspolrzednych = new LinearLayout(context);
        panelWspolrzednych.setOrientation(LinearLayout.HORIZONTAL);

        EditText edtLat = new EditText(context);
        edtLat.setHint("Szerokość (lat)");
        edtLat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        panelWspolrzednych.addView(edtLat, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        EditText edtLon = new EditText(context);
        edtLon.setHint("Długość (lon)");
        edtLon.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        LinearLayout.LayoutParams lonParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lonParams.setMargins(dp(10), 0, 0, 0);
        panelWspolrzednych.addView(edtLon, lonParams);

        kontener.addView(panelWspolrzednych, marginTopParams(dp(10)));

        // Sekcja: wskazanie na mapie.
        LinearLayout panelMapa = new LinearLayout(context);
        panelMapa.setOrientation(LinearLayout.VERTICAL);

        Button btnWybierzNaMapie = new Button(context);
        btnWybierzNaMapie.setText("Otwórz mapę i wskaż punkt");
        panelMapa.addView(btnWybierzNaMapie);

        TextView txtWybranaLokalizacja = new TextView(context);
        txtWybranaLokalizacja.setText("Nie wybrano jeszcze lokalizacji na mapie.");
        txtWybranaLokalizacja.setTextColor(Color.parseColor("#4B5563"));
        txtWybranaLokalizacja.setTextSize(13);
        panelMapa.addView(txtWybranaLokalizacja, marginTopParams(dp(6)));

        kontener.addView(panelMapa, marginTopParams(dp(10)));

        // Przechowuje wybraną na mapie lokalizację (effectively-final tablica jako kontener).
        double[] wybranaLokalizacja = new double[]{Double.NaN, Double.NaN};

        Runnable aktualizujWidocznosc = () -> {
            edtAdres.setVisibility(rbNazwa.isChecked() ? View.VISIBLE : View.GONE);
            panelWspolrzednych.setVisibility(rbWspolrzedne.isChecked() ? View.VISIBLE : View.GONE);
            panelMapa.setVisibility(rbMapa.isChecked() ? View.VISIBLE : View.GONE);
        };
        aktualizujWidocznosc.run();

        grupaSposobow.setOnCheckedChangeListener((group, checkedId) -> aktualizujWidocznosc.run());

        btnWybierzNaMapie.setOnClickListener(v -> {
            odbiorcaWybranejLokalizacji = (lat, lon) -> {
                wybranaLokalizacja[0] = lat;
                wybranaLokalizacja[1] = lon;
                txtWybranaLokalizacja.setText(String.format(Locale.ROOT, "Wybrano: %.6f, %.6f", lat, lon));
            };

            Intent intent = new Intent(requireContext(), MapaTrasyActivity.class);
            intent.putExtra(EXTRA_TRYB_WYBORU_PUNKTU, true);
            wyborLokalizacjiLauncher.launch(intent);
        });

        ScrollView scrollWrapper = new ScrollView(context);
        scrollWrapper.addView(kontener);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Nowy punkt")
                .setView(scrollWrapper)
                .setPositiveButton("Dodaj", null)
                .setNegativeButton("Anuluj", null)
                .create();

        dialog.setOnDismissListener(d -> odbiorcaWybranejLokalizacji = null);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nazwa = edtNazwa.getText() == null ? "" : edtNazwa.getText().toString().trim();

            if (nazwa.isEmpty()) {
                edtNazwa.setError("Podaj nazwę punktu");
                return;
            }

            String kategoria = edtKategoria.getText() == null ? "" : edtKategoria.getText().toString().trim();
            if (kategoria.isEmpty()) {
                kategoria = "Inne";
            }
            String finalKategoria = kategoria;

            if (rbNazwa.isChecked()) {
                String adres = edtAdres.getText() == null ? "" : edtAdres.getText().toString().trim();

                if (adres.isEmpty()) {
                    edtAdres.setError("Podaj nazwę miejsca lub adres");
                    return;
                }

                dialog.dismiss();
                geokodujIZapiszPunkt(nazwa, finalKategoria, adres);
                return;
            }

            if (rbWspolrzedne.isChecked()) {
                String latTekst = edtLat.getText() == null ? "" : edtLat.getText().toString().trim().replace(",", ".");
                String lonTekst = edtLon.getText() == null ? "" : edtLon.getText().toString().trim().replace(",", ".");

                try {
                    double lat = Double.parseDouble(latTekst);
                    double lon = Double.parseDouble(lonTekst);

                    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                        Toast.makeText(context, "Współrzędne są poza dopuszczalnym zakresem", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dialog.dismiss();
                    zapiszNowyPunkt(nazwa, finalKategoria, lat, lon);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Podaj poprawne współrzędne (np. 50.06143, 19.93658)", Toast.LENGTH_SHORT).show();
                }

                return;
            }

            // rbMapa.isChecked()
            if (Double.isNaN(wybranaLokalizacja[0]) || Double.isNaN(wybranaLokalizacja[1])) {
                Toast.makeText(context, "Najpierw wskaż punkt na mapie", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            zapiszNowyPunkt(nazwa, finalKategoria, wybranaLokalizacja[0], wybranaLokalizacja[1]);
        });
    }

    private LinearLayout.LayoutParams marginTopParams(int marginTop) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = marginTop;
        return params;
    }

    /**
     * Geokoduje podaną nazwę/adres na współrzędne (w tle) i zapisuje nowy punkt.
     * Wymaga połączenia z internetem (usługa geokodowania systemowa).
     */
    private void geokodujIZapiszPunkt(String nazwa, String kategoria, String adres) {
        Toast.makeText(requireContext(), "Wyszukiwanie lokalizacji…", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Double znalezionaLat = null;
            Double znalezionaLon = null;

            try {
                if (!isAdded()) {
                    return;
                }

                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> adresy = geocoder.getFromLocationName(adres, 1);

                if (adresy != null && !adresy.isEmpty()) {
                    Address znaleziony = adresy.get(0);
                    znalezionaLat = znaleziony.getLatitude();
                    znalezionaLon = znaleziony.getLongitude();
                }
            } catch (IOException | IllegalArgumentException ignored) {
            }

            if (!isAdded()) {
                return;
            }

            Double finalLat = znalezionaLat;
            Double finalLon = znalezionaLon;

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }

                if (finalLat == null || finalLon == null) {
                    Toast.makeText(requireContext(), "Nie znaleziono lokalizacji dla podanej nazwy/adresu", Toast.LENGTH_LONG).show();
                    return;
                }

                zapiszNowyPunkt(nazwa, kategoria, finalLat, finalLon);
            });
        }).start();
    }

    /**
     * Zapisuje nowy, samodzielny punkt (niezwiązany jeszcze z żadną trasą).
     * UWAGA: wymaga metody TrasyViewModel#dodajPunkt(PunktTrasy) — jeśli ViewModel
     * nie posiada jeszcze takiej metody, należy ją dodać (analogicznie do usunTrase).
     */
    private void zapiszNowyPunkt(String nazwa, String kategoria, double lat, double lon) {
        PunktTrasy punkt = new PunktTrasy();
        punkt.setnId(UUID.randomUUID());
        punkt.setTrasaId(null);
        punkt.setNazwa(nazwa);
        punkt.setKategoria(kategoria);
        punkt.setLatitude(lat);
        punkt.setLongitude(lon);
        punkt.setKolejnosc(0);

        trasyViewModel.dodajPunkt(punkt);
        trasyViewModel.pobierzPunkty();

        Toast.makeText(requireContext(), "Punkt został dodany", Toast.LENGTH_SHORT).show();
    }

    private void odswiezProponowaneTrasy() {
        proponowaneTrasyAktualne.clear();

        for (Trasa trasa : wszystkieTrasy) {
            if (!trasa.isDev()) {
                continue;
            }

            if (!filtry.isEmpty() && !pasujeDoFiltrow(trasa)) {
                continue;
            }

            proponowaneTrasyAktualne.add(trasa);
        }

        odswiezListeProponowanychTras();
    }

    private void odswiezListeProponowanychTras() {
        if (proponowaneTrasyAktualne.isEmpty()) {
            adapterProponowaneTrasy.setTrasy(new ArrayList<>());
            recyclerTrasy.setVisibility(View.GONE);
            txtBrakProponowanychTras.setVisibility(View.VISIBLE);
            btnRozwinProponowaneTrasy.setVisibility(View.GONE);
            return;
        }

        recyclerTrasy.setVisibility(View.VISIBLE);
        txtBrakProponowanychTras.setVisibility(View.GONE);

        if (proponowaneTrasyAktualne.size() > 3) {
            btnRozwinProponowaneTrasy.setVisibility(View.VISIBLE);
            btnRozwinProponowaneTrasy.setText(pokazWszystkieProponowaneTrasy ? "Zwiń" : "Rozwiń");
        } else {
            btnRozwinProponowaneTrasy.setVisibility(View.GONE);
            pokazWszystkieProponowaneTrasy = false;
        }

        int limit = pokazWszystkieProponowaneTrasy
                ? proponowaneTrasyAktualne.size()
                : Math.min(3, proponowaneTrasyAktualne.size());

        adapterProponowaneTrasy.setTrasy(new ArrayList<>(proponowaneTrasyAktualne.subList(0, limit)));
    }

    private boolean pasujeDoFiltrow(Trasa trasa) {
        if (filtry.isEmpty()) {
            return true;
        }

        List<String> filtryRegionu = new ArrayList<>();
        List<String> filtryTypu = new ArrayList<>();
        List<String> filtryPoziomu = new ArrayList<>();
        List<String> filtryTekstowe = new ArrayList<>();

        for (String filtr : filtry) {
            if (pasujePole(trasa.getRegion(), filtr)) {
                filtryRegionu.add(filtr);
            } else if (pasujePole(trasa.getTyp(), filtr)) {
                filtryTypu.add(filtr);
            } else if (pasujePole(trasa.getPoziomTrudnosci(), filtr)) {
                filtryPoziomu.add(filtr);
            } else {
                filtryTekstowe.add(filtr);
            }
        }

        if (!filtryRegionu.isEmpty() && !pasujeDoDowolnegoPola(trasa.getRegion(), filtryRegionu)) {
            return false;
        }

        if (!filtryTypu.isEmpty() && !pasujeDoDowolnegoPola(trasa.getTyp(), filtryTypu)) {
            return false;
        }

        if (!filtryPoziomu.isEmpty() && !pasujeDoDowolnegoPola(trasa.getPoziomTrudnosci(), filtryPoziomu)) {
            return false;
        }

        if (!filtryTekstowe.isEmpty()) {
            String tekstTrasy = (
                    bezNulla(trasa.getNazwa()) + " " +
                            bezNulla(trasa.getOpis()) + " " +
                            bezNulla(trasa.getRegion()) + " " +
                            bezNulla(trasa.getTyp()) + " " +
                            bezNulla(trasa.getPoziomTrudnosci())
            );

            String normalnyTekstTrasy = normalizuj(tekstTrasy);

            for (String filtr : filtryTekstowe) {
                if (!normalnyTekstTrasy.contains(normalizuj(filtr))) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean pasujePole(String pole, String filtr) {
        return normalizuj(pole).equals(normalizuj(filtr));
    }

    private boolean pasujeDoDowolnegoPola(String pole, List<String> filtryGrupy) {
        String normalnePole = normalizuj(pole);

        for (String filtr : filtryGrupy) {
            if (normalnePole.equals(normalizuj(filtr))) {
                return true;
            }
        }

        return false;
    }

    private String normalizuj(String tekst) {
        if (tekst == null) {
            return "";
        }

        return tekst
                .toLowerCase(Locale.ROOT)
                .replace("ą", "a")
                .replace("ć", "c")
                .replace("ę", "e")
                .replace("ł", "l")
                .replace("ń", "n")
                .replace("ó", "o")
                .replace("ś", "s")
                .replace("ź", "z")
                .replace("ż", "z")
                .trim();
    }

    private String bezNulla(String tekst) {
        return tekst == null ? "" : tekst;
    }

    private int dp(int wartosc) {
        return (int) (wartosc * getResources().getDisplayMetrics().density);
    }

    private class TrasyHolder extends RecyclerView.ViewHolder {

        private final TextView txtNazwa;
        private final TextView txtOpis;
        private final TextView txtMeta;
        private final TextView txtTrudnosc;
        private final TextView txtOpcje;
        private final Button btnMapa;
        private Trasa trasa;

        public TrasyHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.item_trasa_list, parent, false));

            txtNazwa = itemView.findViewById(R.id.txtNazwaTrasyLista);
            txtOpis = itemView.findViewById(R.id.txtOpisTrasyLista);
            txtMeta = itemView.findViewById(R.id.txtMetaTrasyLista);
            txtTrudnosc = itemView.findViewById(R.id.txtTrudnoscTrasyLista);
            txtOpcje = itemView.findViewById(R.id.txtOpcjeTrasyLista);
            btnMapa = itemView.findViewById(R.id.btnMapaTrasyLista);
        }

        public void bind(Trasa trasa) {
            this.trasa = trasa;

            txtNazwa.setText(trasa.getNazwa());
            txtOpis.setText(trasa.getOpis());

            String meta = String.format(
                    Locale.ROOT,
                    "%s • %.1f km • +%d m • %s",
                    bezNulla(trasa.getRegion()),
                    trasa.getDystansKm(),
                    trasa.getPrzewyzszenieM(),
                    trasa.getCzasTekst()
            );

            txtMeta.setText(meta);

            if (trasa.isDev()) {
                txtTrudnosc.setText(trasa.getPoziomTrudnosci() + " • " + trasa.getTyp() + " • przykładowa");
                txtOpcje.setVisibility(View.GONE);
            } else {
                txtTrudnosc.setText(trasa.getPoziomTrudnosci() + " • " + trasa.getTyp() + " • moja");
                txtOpcje.setVisibility(View.VISIBLE);
            }

            btnMapa.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MapaTrasyActivity.class);
                intent.putExtra(MapaTrasyActivity.EXTRA_TRASA_ID, this.trasa.getnId().toString());
                intent.putExtra(MapaTrasyActivity.EXTRA_TRASA_NAZWA, this.trasa.getNazwa());
                startActivity(intent);
            });

            txtOpcje.setOnClickListener(v -> {
                if (this.trasa == null || this.trasa.isDev()) {
                    return;
                }

                PopupMenu popupMenu = new PopupMenu(requireContext(), txtOpcje);
                popupMenu.getMenu().add("Edytuj");
                popupMenu.getMenu().add("Usuń");

                popupMenu.setOnMenuItemClickListener(item -> {
                    String tytul = item.getTitle().toString();

                    if ("Edytuj".equals(tytul)) {
                        otworzEdycjeTrasy(this.trasa);
                        return true;
                    }

                    if ("Usuń".equals(tytul)) {
                        potwierdzUsuniecieTrasy(this.trasa);
                        return true;
                    }

                    return false;
                });

                popupMenu.show();
            });
        }
    }

    private class TrasyAdapter extends RecyclerView.Adapter<TrasyHolder> {

        private List<Trasa> trasy;

        public TrasyAdapter(List<Trasa> trasy) {
            this.trasy = trasy;
        }

        public void setTrasy(List<Trasa> trasy) {
            this.trasy = trasy;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TrasyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new TrasyHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(@NonNull TrasyHolder holder, int position) {
            holder.bind(trasy.get(position));
        }

        @Override
        public int getItemCount() {
            return trasy == null ? 0 : trasy.size();
        }
    }
}