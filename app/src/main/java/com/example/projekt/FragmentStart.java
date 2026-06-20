package com.example.projekt;

import android.Manifest;
import android.app.AlertDialog;
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
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentStart extends Fragment {

    private static final String PREFS_NAME = "tourroute_prefs";
    private static final String PREF_REGULAMIN = "regulamin_zaakceptowany";

    private TextView txtPolecaneNaglowek;
    private TextView txtGpsInfo;
    private TextView txtBrakTras;
    private TextView txtPowitanie;
    private TextView txtStatusLogowania;
    private TextView txtOpisLogowania;

    private LinearLayout panelNiezalogowany;
    private LinearLayout panelStatystyki;
    private LinearLayout sekcjaMojeTrasy;
    private LinearLayout sekcjaOstatnieTrasy;
    private LinearLayout sekcjaOstatniePunkty;
    private LinearLayout kontenerOstatnichPunktow;

    private Button btnZaloguj;
    private Button btnZarejestruj;

    private RecyclerView recyclerPolecaneTrasy;
    private RecyclerView recyclerMojeTrasy;
    private RecyclerView recyclerOstatnieTrasy;

    private TextView txtBrakMoichTras;
    private TextView txtBrakOstatnichTras;
    private TextView txtBrakOstatnichPunktow;

    private TrasyViewModel trasyViewModel;

    private TrasyAdapter adapterPolecane;
    private TrasyAdapter adapterMoje;
    private OstatnieTreningiAdapter adapterOstatnie;

    private ActivityResultLauncher<String[]> lokalizacjaLauncher;

    private String aktualnaLokacja = "";
    private List<Trasa> wszystkieTrasy = new ArrayList<>();
    private List<TreningEntity> wszystkieTreningi = new ArrayList<>();
    private List<PunktTrasy> wszystkiePunkty = new ArrayList<>();

    private final Handler lokalizacjaHandler = new Handler(Looper.getMainLooper());

    private Double ostatniaSzerokosc = null;
    private Double ostatniaDlugosc = null;

    private boolean automatyczneOdswiezanieLokacji = false;

    private final Runnable lokalizacjaRunnable = new Runnable() {
        @Override
        public void run() {
            if (!automatyczneOdswiezanieLokacji) {
                return;
            }

            pobierzLokacje();
            lokalizacjaHandler.postDelayed(this, 5000);
        }
    };

    public FragmentStart() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lokalizacjaLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> pobierzLokacje()
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View widok, Bundle savedInstanceState) {
        super.onViewCreated(widok, savedInstanceState);

        txtPolecaneNaglowek = widok.findViewById(R.id.txtPolecaneNaglowek);
        txtGpsInfo = widok.findViewById(R.id.txtGpsInfo);
        txtBrakTras = widok.findViewById(R.id.txtBrakTras);
        txtPowitanie = widok.findViewById(R.id.txtPowitanie);
        txtStatusLogowania = widok.findViewById(R.id.txtStatusLogowania);
        txtOpisLogowania = widok.findViewById(R.id.txtOpisLogowania);

        panelNiezalogowany = widok.findViewById(R.id.panelNiezalogowany);
        panelStatystyki = widok.findViewById(R.id.panelStatystyki);

        btnZaloguj = widok.findViewById(R.id.btnZaloguj);
        btnZarejestruj = widok.findViewById(R.id.btnZarejestruj);

        recyclerPolecaneTrasy = widok.findViewById(R.id.recyclerPolecaneTrasy);

        trasyViewModel = new ViewModelProvider(requireActivity()).get(TrasyViewModel.class);

        adapterPolecane = new TrasyAdapter(new ArrayList<>());
        recyclerPolecaneTrasy.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        recyclerPolecaneTrasy.setAdapter(adapterPolecane);

        przygotujSekcjeMojeIOstatnieTrasy();

        btnZaloguj.setOnClickListener(v -> otworzLogowanie());
        btnZarejestruj.setOnClickListener(v -> otworzRejestracje());

        trasyViewModel.getTrasy().observe(getViewLifecycleOwner(), trasy -> {
            if (trasy == null) {
                wszystkieTrasy = new ArrayList<>();
            } else {
                wszystkieTrasy = trasy;
            }

            odswiezWszystkieSekcjeTras();
            odswiezStatystyki();
        });

        trasyViewModel.getTreningi().observe(getViewLifecycleOwner(), treningi -> {
            if (treningi == null) {
                wszystkieTreningi = new ArrayList<>();
            } else {
                wszystkieTreningi = treningi;
            }

            odswiezOstatnieTrasy();
            odswiezStatystyki();
        });

        trasyViewModel.getPunkty().observe(getViewLifecycleOwner(), punkty -> {
            if (punkty == null) {
                wszystkiePunkty = new ArrayList<>();
            } else {
                wszystkiePunkty = punkty;
            }

            odswiezOstatnioOdwiedzonePunkty();
        });

        trasyViewModel.pobierzTrasy();
        trasyViewModel.pobierzTreningi();
        trasyViewModel.pobierzPunkty();

        ustawPowitanieGlowne();
        odswiezStanLogowania();
        odswiezStatystyki();

        sprawdzRegulamin();
        sprawdzUprawnieniaLokalizacji();
    }

    @Override
    public void onResume() {
        super.onResume();

        ustawPowitanieGlowne();
        odswiezStanLogowania();
        odswiezWszystkieSekcjeTras();
        odswiezStatystyki();

        if (trasyViewModel != null) {
            trasyViewModel.pobierzTreningi();
            trasyViewModel.pobierzPunkty();
        }

        automatyczneOdswiezanieLokacji = true;
        lokalizacjaHandler.removeCallbacks(lokalizacjaRunnable);
        lokalizacjaHandler.post(lokalizacjaRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();

        automatyczneOdswiezanieLokacji = false;
        lokalizacjaHandler.removeCallbacks(lokalizacjaRunnable);
    }

    private void ustawPowitanieGlowne() {
        if (txtPowitanie != null) {
            txtPowitanie.setText("Dokąd dzisiaj idziemy?");
        }
    }

    private void otworzLogowanie() {
        try {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Ekran logowania nie jest jeszcze skonfigurowany", Toast.LENGTH_SHORT).show();
        }
    }

    private void otworzRejestracje() {
        try {
            Intent intent = new Intent(requireContext(), RegisterActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Ekran rejestracji nie jest jeszcze skonfigurowany", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean czyZalogowany() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("uzytkownik_zalogowany", false);
    }

    private void odswiezStanLogowania() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean zalogowany = prefs.getBoolean("uzytkownik_zalogowany", false);

        if (!zalogowany) {
            panelNiezalogowany.setVisibility(View.VISIBLE);
            panelNiezalogowany.setBackgroundResource(R.drawable.bg_panel_warning);

            txtStatusLogowania.setText("Hej, jesteś niezalogowany");
            txtOpisLogowania.setVisibility(View.VISIBLE);
            txtOpisLogowania.setText("Masz dostęp do ograniczonych funkcjonalności. Zaloguj się, aby zapisywać własne trasy, statystyki, ulubione miejsca i znajomych.");

            btnZaloguj.setVisibility(View.VISIBLE);
            btnZarejestruj.setVisibility(View.VISIBLE);
            return;
        }

        String pseudonim = prefs.getString("uzytkownik_pseudonim", "");
        String imie = prefs.getString("uzytkownik_imie", "");

        String nazwa = pseudonim != null && !pseudonim.trim().isEmpty()
                ? pseudonim.trim()
                : imie;

        if (nazwa == null || nazwa.trim().isEmpty()) {
            nazwa = "podróżniku";
        }

        panelNiezalogowany.setVisibility(View.VISIBLE);
        panelNiezalogowany.setBackgroundResource(R.drawable.bg_card_route);

        txtStatusLogowania.setText("Hej " + nazwa + "! Świetny dzień na zaplanowanie niezapomnianej podróży.");
        txtOpisLogowania.setVisibility(View.GONE);

        btnZaloguj.setVisibility(View.GONE);
        btnZarejestruj.setVisibility(View.GONE);
    }

    private void przygotujSekcjeMojeIOstatnieTrasy() {
        if (panelStatystyki == null) {
            return;
        }

        ViewGroup parent = (ViewGroup) panelStatystyki.getParent();
        if (!(parent instanceof LinearLayout)) {
            return;
        }

        LinearLayout kontener = (LinearLayout) parent;
        int indeksStatystyk = kontener.indexOfChild(panelStatystyki);

        sekcjaMojeTrasy = new LinearLayout(requireContext());
        sekcjaMojeTrasy.setOrientation(LinearLayout.VERTICAL);
        sekcjaMojeTrasy.setVisibility(View.GONE);
        sekcjaMojeTrasy.setBackgroundResource(R.drawable.bg_section_panel);
        sekcjaMojeTrasy.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams sekcjaMojeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sekcjaMojeParams.setMargins(0, dp(18), 0, 0);
        sekcjaMojeTrasy.setLayoutParams(sekcjaMojeParams);

        TextView txtMojeNaglowek = utworzNaglowekSekcji("Moje trasy");
        sekcjaMojeTrasy.addView(txtMojeNaglowek);
        sekcjaMojeTrasy.addView(utworzSeparatorSekcji());

        recyclerMojeTrasy = new RecyclerView(requireContext());
        recyclerMojeTrasy.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        adapterMoje = new TrasyAdapter(new ArrayList<>());
        recyclerMojeTrasy.setAdapter(adapterMoje);
        recyclerMojeTrasy.setNestedScrollingEnabled(false);
        recyclerMojeTrasy.setLayoutParams(utworzRecyclerParams());
        sekcjaMojeTrasy.addView(recyclerMojeTrasy);

        txtBrakMoichTras = utworzInfoBox("Nie masz jeszcze własnych tras. Utwórz pierwszą trasę i zacznij budować swoją historię podróży.");
        sekcjaMojeTrasy.addView(txtBrakMoichTras);

        kontener.addView(sekcjaMojeTrasy, indeksStatystyk++);

        sekcjaOstatnieTrasy = new LinearLayout(requireContext());
        sekcjaOstatnieTrasy.setOrientation(LinearLayout.VERTICAL);
        sekcjaOstatnieTrasy.setVisibility(View.GONE);
        sekcjaOstatnieTrasy.setBackgroundResource(R.drawable.bg_section_panel);
        sekcjaOstatnieTrasy.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams sekcjaOstatnieParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sekcjaOstatnieParams.setMargins(0, dp(18), 0, 0);
        sekcjaOstatnieTrasy.setLayoutParams(sekcjaOstatnieParams);

        TextView txtOstatnieNaglowek = utworzNaglowekSekcji("Ostatnie trasy");
        sekcjaOstatnieTrasy.addView(txtOstatnieNaglowek);
        sekcjaOstatnieTrasy.addView(utworzSeparatorSekcji());

        recyclerOstatnieTrasy = new RecyclerView(requireContext());
        recyclerOstatnieTrasy.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        adapterOstatnie = new OstatnieTreningiAdapter(new ArrayList<>());
        recyclerOstatnieTrasy.setAdapter(adapterOstatnie);
        recyclerOstatnieTrasy.setNestedScrollingEnabled(false);
        recyclerOstatnieTrasy.setLayoutParams(utworzRecyclerParams());
        sekcjaOstatnieTrasy.addView(recyclerOstatnieTrasy);

        txtBrakOstatnichTras = utworzInfoBox("Brak ostatnich tras do wyświetlenia.");
        sekcjaOstatnieTrasy.addView(txtBrakOstatnichTras);

        kontener.addView(sekcjaOstatnieTrasy, indeksStatystyk++);

        sekcjaOstatniePunkty = new LinearLayout(requireContext());
        sekcjaOstatniePunkty.setOrientation(LinearLayout.VERTICAL);
        sekcjaOstatniePunkty.setVisibility(View.GONE);
        sekcjaOstatniePunkty.setBackgroundResource(R.drawable.bg_section_panel);
        sekcjaOstatniePunkty.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams sekcjaPunktyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sekcjaPunktyParams.setMargins(0, dp(18), 0, 0);
        sekcjaOstatniePunkty.setLayoutParams(sekcjaPunktyParams);

        TextView txtPunktyNaglowek = utworzNaglowekSekcji("Ostatnio odwiedzone punkty");
        sekcjaOstatniePunkty.addView(txtPunktyNaglowek);
        sekcjaOstatniePunkty.addView(utworzSeparatorSekcji());

        kontenerOstatnichPunktow = new LinearLayout(requireContext());
        kontenerOstatnichPunktow.setOrientation(LinearLayout.VERTICAL);
        sekcjaOstatniePunkty.addView(kontenerOstatnichPunktow);

        txtBrakOstatnichPunktow = utworzInfoBox("Brak ostatnio odwiedzonych punktów.");
        sekcjaOstatniePunkty.addView(txtBrakOstatnichPunktow);

        kontener.addView(sekcjaOstatniePunkty, indeksStatystyk);
    }

    private TextView utworzNaglowekSekcji(String tekst) {
        TextView textView = new TextView(requireContext());
        textView.setText(tekst);
        textView.setTextColor(Color.parseColor("#111827"));
        textView.setTextSize(19);
        textView.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(2));
        textView.setLayoutParams(params);

        return textView;
    }

    private View utworzSeparatorSekcji() {
        View separator = new View(requireContext());
        separator.setBackgroundResource(R.drawable.bg_section_divider);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        params.setMargins(0, dp(10), 0, dp(10));
        separator.setLayoutParams(params);

        return separator;
    }

    private TextView utworzInfoBox(String tekst) {
        TextView textView = new TextView(requireContext());
        textView.setText(tekst);
        textView.setTextColor(Color.parseColor("#92400E"));
        textView.setTextSize(15);
        textView.setPadding(dp(16), dp(16), dp(16), dp(16));
        textView.setBackgroundResource(R.drawable.bg_panel_warning);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(12), 0, 0);
        textView.setLayoutParams(params);

        return textView;
    }

    private LinearLayout.LayoutParams utworzRecyclerParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(12), 0, 0);
        return params;
    }

    private int dp(int wartosc) {
        return (int) (wartosc * getResources().getDisplayMetrics().density);
    }

    private void odswiezWszystkieSekcjeTras() {
        odswiezPolecaneTrasy();
        odswiezMojeTrasy();
        odswiezOstatnieTrasy();
        odswiezOstatnioOdwiedzonePunkty();
    }

    private void odswiezPolecaneTrasy() {
        List<Trasa> polecane = new ArrayList<>();

        for (Trasa trasa : wszystkieTrasy) {
            if (!trasa.isDev()) {
                continue;
            }

            if (aktualnaLokacja == null || aktualnaLokacja.trim().isEmpty()) {
                continue;
            }

            String lokacja = aktualnaLokacja.toLowerCase(Locale.ROOT);
            String region = trasa.getRegion() == null ? "" : trasa.getRegion().toLowerCase(Locale.ROOT);

            if (region.contains(lokacja) || lokacja.contains(region)) {
                polecane.add(trasa);
            }
        }

        adapterPolecane.setTrasy(polecane);

        if (polecane.isEmpty()) {
            txtBrakTras.setVisibility(View.VISIBLE);
        } else {
            txtBrakTras.setVisibility(View.GONE);
        }
    }

    private void odswiezMojeTrasy() {
        if (adapterMoje == null || sekcjaMojeTrasy == null) {
            return;
        }

        if (!czyZalogowany()) {
            sekcjaMojeTrasy.setVisibility(View.GONE);
            adapterMoje.setTrasy(new ArrayList<>());
            return;
        }

        sekcjaMojeTrasy.setVisibility(View.VISIBLE);

        List<Trasa> moje = new ArrayList<>();

        for (Trasa trasa : wszystkieTrasy) {
            if (!trasa.isDev()) {
                moje.add(trasa);
            }
        }

        adapterMoje.setTrasy(moje);

        if (txtBrakMoichTras != null) {
            txtBrakMoichTras.setVisibility(moje.isEmpty() ? View.VISIBLE : View.GONE);
        }

        if (recyclerMojeTrasy != null) {
            recyclerMojeTrasy.setVisibility(moje.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void odswiezOstatnieTrasy() {
        if (adapterOstatnie == null || sekcjaOstatnieTrasy == null) {
            return;
        }

        if (!czyZalogowany()) {
            sekcjaOstatnieTrasy.setVisibility(View.GONE);
            adapterOstatnie.setTreningi(new ArrayList<>());
            return;
        }

        sekcjaOstatnieTrasy.setVisibility(View.VISIBLE);

        List<TreningEntity> ostatnie = new ArrayList<>();

        for (TreningEntity trening : wszystkieTreningi) {
            ostatnie.add(trening);

            if (ostatnie.size() >= 5) {
                break;
            }
        }

        adapterOstatnie.setTreningi(ostatnie);

        if (txtBrakOstatnichTras != null) {
            txtBrakOstatnichTras.setVisibility(ostatnie.isEmpty() ? View.VISIBLE : View.GONE);
        }

        if (recyclerOstatnieTrasy != null) {
            recyclerOstatnieTrasy.setVisibility(ostatnie.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void odswiezOstatnioOdwiedzonePunkty() {
        if (sekcjaOstatniePunkty == null || kontenerOstatnichPunktow == null) {
            return;
        }

        if (!czyZalogowany()) {
            sekcjaOstatniePunkty.setVisibility(View.GONE);
            kontenerOstatnichPunktow.removeAllViews();
            return;
        }

        List<PunktTrasy> punkty = new ArrayList<>();
        List<String> dodane = new ArrayList<>();

        for (TreningEntity trening : wszystkieTreningi) {
            if (trening == null || trening.trasaUuid == null) {
                continue;
            }

            for (PunktTrasy punkt : wszystkiePunkty) {
                if (punkt == null || punkt.getTrasaId() == null) {
                    continue;
                }

                if (!trening.trasaUuid.equals(punkt.getTrasaId().toString())) {
                    continue;
                }

                String klucz = punkt.getnId().toString();
                if (dodane.contains(klucz)) {
                    continue;
                }

                dodane.add(klucz);
                punkty.add(punkt);

                if (punkty.size() >= 5) {
                    break;
                }
            }

            if (punkty.size() >= 5) {
                break;
            }
        }

        sekcjaOstatniePunkty.setVisibility(View.VISIBLE);
        kontenerOstatnichPunktow.removeAllViews();

        if (punkty.isEmpty()) {
            txtBrakOstatnichPunktow.setVisibility(View.VISIBLE);
            return;
        }

        txtBrakOstatnichPunktow.setVisibility(View.GONE);

        for (PunktTrasy punkt : punkty) {
            kontenerOstatnichPunktow.addView(utworzKarteOdwiedzonegoPunktu(punkt));
        }
    }

    private View utworzKarteOdwiedzonegoPunktu(PunktTrasy punkt) {
        LinearLayout karta = new LinearLayout(requireContext());
        karta.setOrientation(LinearLayout.VERTICAL);
        karta.setPadding(dp(12), dp(10), dp(12), dp(10));
        karta.setBackgroundResource(R.drawable.bg_card_route);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        karta.setLayoutParams(params);

        String nazwa = punkt.getNazwa() == null || punkt.getNazwa().trim().isEmpty()
                ? "Punkt"
                : punkt.getNazwa();
        karta.addView(utworzTextPanelu(nazwa, 15, "#111827", true));

        String meta = (punkt.getKategoria() == null ? "Punkt" : punkt.getKategoria())
                + " • " + String.format(Locale.ROOT, "%.5f, %.5f", punkt.getLatitude(), punkt.getLongitude());
        karta.addView(utworzTextPanelu(meta, 13, "#4B5563", false));

        return karta;
    }

    private void odswiezStatystyki() {
        if (panelStatystyki == null) {
            return;
        }

        panelStatystyki.removeAllViews();

        if (!czyZalogowany()) {
            pokazStatystykiNiezalogowany();
        } else {
            pokazStatystykiZalogowany();
        }
    }

    private void pokazStatystykiNiezalogowany() {
        TextView naglowek = utworzTextPanelu("Statystyki z ostatnich 7 dni", 19, "#111827", true);
        TextView opis = utworzTextPanelu("Aby zbierać i analizować dane o swoich trasach, zaloguj się.", 14, "#4B5563", false);

        Button btn = new Button(requireContext());
        btn.setText("Zaloguj się");
        btn.setOnClickListener(v -> otworzLogowanie());

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, dp(12), 0, 0);
        btn.setLayoutParams(btnParams);

        panelStatystyki.addView(naglowek);
        panelStatystyki.addView(utworzSeparatorSekcji());
        panelStatystyki.addView(opis);
        panelStatystyki.addView(btn);
    }

    private void pokazStatystykiZalogowany() {
        double kilometry = 0;
        int czasMin = 0;
        int kalorie = 0;
        long tydzienTemu = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L;

        for (TreningEntity trening : wszystkieTreningi) {
            if (trening.dataStartu < tydzienTemu) {
                continue;
            }

            kilometry += trening.dystansKm;
            czasMin += trening.czasMin;
            kalorie += trening.kalorie;
        }
        String poziom = okreslPoziomWysilku(kilometry, czasMin);
        String kolorPoziomu = dobierzKolorPoziomuWysilku(poziom);
        String wykres = utworzWykresTekstowy(poziom);

        panelStatystyki.addView(utworzTextPanelu("Statystyki z ostatnich 7 dni", 19, "#111827", true));
        panelStatystyki.addView(utworzSeparatorSekcji());
        panelStatystyki.addView(utworzTextPanelu("Przebyte kilometry: " + String.format(Locale.ROOT, "%.1f", kilometry) + " km", 15, "#374151", false));
        panelStatystyki.addView(utworzTextPanelu("Czas aktywnie: " + czasMin + " min", 15, "#374151", false));
        panelStatystyki.addView(utworzTextPanelu("Spalone kalorie: " + kalorie + " kcal", 15, "#374151", false));
        panelStatystyki.addView(utworzTextPanelu("Poziom wysiłku: " + poziom, 15, kolorPoziomu, true));
        panelStatystyki.addView(utworzTextPanelu(wykres, 24, kolorPoziomu, true));
    }

    private TextView utworzTextPanelu(String tekst, int rozmiar, String kolor, boolean pogrubiony) {
        TextView textView = new TextView(requireContext());
        textView.setText(tekst);
        textView.setTextSize(rozmiar);
        textView.setTextColor(Color.parseColor(kolor));
        textView.setPadding(0, dp(4), 0, dp(4));

        if (pogrubiony) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        return textView;
    }

    private String okreslPoziomWysilku(double km, int czasMin) {
        if (km <= 0 && czasMin <= 0) {
            return "brak danych";
        }

        if (km < 5 || czasMin < 45) {
            return "niski";
        }

        if (km < 15 || czasMin < 150) {
            return "średni";
        }

        return "wysoki";
    }

    private String dobierzKolorPoziomuWysilku(String poziom) {
        String wartosc = poziom == null ? "" : poziom.toLowerCase(Locale.ROOT);

        if (wartosc.contains("niski")) {
            return "#059669";
        }

        if (wartosc.contains("średni") || wartosc.contains("redni")) {
            return "#F97316";
        }

        if (wartosc.contains("wysoki")) {
            return "#DC2626";
        }

        return "#6B7280";
    }

    private String utworzWykresTekstowy(String poziom) {
        String wartosc = poziom == null ? "" : poziom.toLowerCase(Locale.ROOT);

        if (wartosc.contains("brak")) {
            return "▱▱▱▱▱ brak aktywności";
        }

        if (wartosc.contains("niski")) {
            return "▰▱▱▱▱ niski wysiłek";
        }

        if (wartosc.contains("średni") || wartosc.contains("redni")) {
            return "▰▰▰▱▱ średni wysiłek";
        }

        return "▰▰▰▰▰ wysoki wysiłek";
    }

    private int dobierzGrafikeTrasy(Trasa trasa) {
        String region = trasa.getRegion() == null ? "" : trasa.getRegion().toLowerCase(Locale.ROOT);
        String typ = trasa.getTyp() == null ? "" : trasa.getTyp().toLowerCase(Locale.ROOT);

        if (region.contains("kraków") || typ.contains("miejska")) {
            return R.drawable.ic_route_city;
        }

        if (region.contains("tatry") || typ.contains("górska")) {
            return R.drawable.ic_route_mountain;
        }

        if (region.contains("ojców") || typ.contains("przyrodnicza")) {
            return R.drawable.ic_route_nature;
        }

        return R.drawable.ic_route_placeholder;
    }

    private void sprawdzRegulamin() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean zaakceptowany = prefs.getBoolean(PREF_REGULAMIN, false);

        if (!zaakceptowany) {
            pokazRegulamin();
        }
    }

    private void pokazRegulamin() {
        View widok = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_regulamin, null, false);

        TextView txtOpisRegulaminu = widok.findViewById(R.id.txtOpisRegulaminu);
        TextView txtRegulaminDlugi = widok.findViewById(R.id.txtRegulaminDlugi);
        Button btnAkceptuj = widok.findViewById(R.id.btnAkceptuj);
        Button btnOdrzuc = widok.findViewById(R.id.btnOdrzuc);

        ustawKlikalnyRegulamin(txtOpisRegulaminu, txtRegulaminDlugi);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(widok)
                .setCancelable(false)
                .create();

        dialog.setCanceledOnTouchOutside(false);

        btnAkceptuj.setOnClickListener(v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(PREF_REGULAMIN, true).apply();
            dialog.dismiss();
        });

        btnOdrzuc.setOnClickListener(v -> {
            btnAkceptuj.setEnabled(false);
            btnOdrzuc.setEnabled(false);
            txtOpisRegulaminu.setEnabled(false);
            txtOpisRegulaminu.setClickable(false);
            txtOpisRegulaminu.setMovementMethod(null);

            Toast.makeText(
                    requireContext(),
                    "Jest nam przykro, że nie zgadzasz się na nasze warunki usług. Kontakt: pomoc@szlaksietrafil.example.com",
                    Toast.LENGTH_LONG
            ).show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (getActivity() != null) {
                    getActivity().finishAffinity();
                }
            }, 5000);
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        });

        dialog.show();
    }

    private void ustawKlikalnyRegulamin(TextView txtOpisRegulaminu, TextView txtRegulaminDlugi) {
        String tekst = "Aplikacja Szlak się Trafił © jest narzędziem wspierającym podróżujących — "
                + "umożliwia planowanie i śledzenie swoich tras. Aplikacja wykorzystuje dane "
                + "lokalizacyjne wyłącznie do dopasowania rekomendacji i wspierania procesu "
                + "nawigacji. Aby kontynuować, zaakceptuj Regulamin.";

        SpannableString spannable = new SpannableString(tekst);

        int start = tekst.indexOf("Regulamin");
        int end = start + "Regulamin".length();

        ClickableSpan regulaminSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if (txtRegulaminDlugi.getVisibility() == View.VISIBLE) {
                    txtRegulaminDlugi.setVisibility(View.GONE);
                } else {
                    txtRegulaminDlugi.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#2563EB"));
                ds.setUnderlineText(true);
                ds.setFakeBoldText(true);
            }
        };

        spannable.setSpan(regulaminSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        txtOpisRegulaminu.setText(spannable);
        txtOpisRegulaminu.setMovementMethod(LinkMovementMethod.getInstance());
        txtOpisRegulaminu.setHighlightColor(Color.TRANSPARENT);
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
            pobierzLokacje();
        } else {
            lokalizacjaLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void pobierzLokacje() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            aktualnaLokacja = "";

            txtPolecaneNaglowek.setText("Polecane przykładowe trasy dla lokalizacji:");
            txtGpsInfo.setText("Brak dostępu do GPS. Nie znaleziono tras w Twojej lokalizacji.");

            odswiezPolecaneTrasy();
            return;
        }

        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            aktualnaLokacja = "";

            txtPolecaneNaglowek.setText("Polecane przykładowe trasy dla lokalizacji:");
            txtGpsInfo.setText("Nie udało się uruchomić usługi lokalizacji.");

            odswiezPolecaneTrasy();
            return;
        }

        if (aktualnaLokacja == null || aktualnaLokacja.trim().isEmpty()) {
            txtGpsInfo.setText("Pobieramy aktualną lokalizację telefonu...");
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        location -> obsluzNowaLokalizacje(location.getLatitude(), location.getLongitude()),
                        Looper.getMainLooper()
                );
                return;
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER,
                        location -> obsluzNowaLokalizacje(location.getLatitude(), location.getLongitude()),
                        Looper.getMainLooper()
                );
                return;
            }

            pobierzOstatniaZnanaLokacje(locationManager);

        } catch (Exception e) {
            pobierzOstatniaZnanaLokacje(locationManager);
        }
    }

    private void pobierzOstatniaZnanaLokacje(LocationManager locationManager) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = null;

        Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (gps != null && network != null) {
            location = gps.getTime() > network.getTime() ? gps : network;
        } else if (gps != null) {
            location = gps;
        } else if (network != null) {
            location = network;
        }

        if (location == null) {
            aktualnaLokacja = "";

            txtPolecaneNaglowek.setText("Polecane przykładowe trasy dla lokalizacji:");
            txtGpsInfo.setText("Nie udało się pobrać lokalizacji telefonu.");

            odswiezPolecaneTrasy();
            return;
        }

        obsluzNowaLokalizacje(location.getLatitude(), location.getLongitude());
    }

    private void obsluzNowaLokalizacje(double lat, double lon) {
        if (ostatniaSzerokosc != null && ostatniaDlugosc != null) {
            float[] wynik = new float[1];

            Location.distanceBetween(
                    ostatniaSzerokosc,
                    ostatniaDlugosc,
                    lat,
                    lon,
                    wynik
            );

            if (wynik[0] < 50) {
                return;
            }
        }

        ostatniaSzerokosc = lat;
        ostatniaDlugosc = lon;

        ustawNazweLokacji(lat, lon);
    }

    private void ustawNazweLokacji(double lat, double lon) {
        if (!isAdded()) {
            return;
        }

        Context appContext = requireContext().getApplicationContext();

        new Thread(() -> {
            String lokacja = "Twoja okolica";

            try {
                Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                List<Address> adresy = geocoder.getFromLocation(lat, lon, 1);

                if (adresy != null && !adresy.isEmpty()) {
                    Address adres = adresy.get(0);

                    if (adres.getLocality() != null) {
                        lokacja = adres.getLocality();
                    } else if (adres.getSubAdminArea() != null) {
                        lokacja = adres.getSubAdminArea();
                    } else if (adres.getAdminArea() != null) {
                        lokacja = adres.getAdminArea();
                    }
                }
            } catch (IOException | IllegalArgumentException ignored) {
            }

            String finalLokacja = lokacja;

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded() || getView() == null) {
                    return;
                }

                aktualnaLokacja = finalLokacja;

                txtPolecaneNaglowek.setText("Polecane przykładowe trasy dla lokalizacji: " + finalLokacja);
                txtGpsInfo.setText("Lokalizacja została odczytana z GPS telefonu. Pokazujemy trasy pasujące do Twojej okolicy.");

                odswiezPolecaneTrasy();
            });
        }).start();
    }

    private class TrasyHolder extends RecyclerView.ViewHolder {

        private final ImageView imgTrasa;
        private final TextView txtNazwa;
        private final TextView txtOpis;
        private final TextView txtParametry;
        private final TextView txtTyp;
        private Trasa trasa;

        public TrasyHolder(@NonNull View itemView) {
            super(itemView);

            imgTrasa = itemView.findViewById(R.id.imgTrasa);
            txtNazwa = itemView.findViewById(R.id.txtNazwaTrasy);
            txtOpis = itemView.findViewById(R.id.txtOpisTrasy);
            txtParametry = itemView.findViewById(R.id.txtParametryTrasy);
            txtTyp = itemView.findViewById(R.id.txtTypTrasy);

            itemView.setOnClickListener(v -> {
                if (trasa == null) {
                    return;
                }

                Intent intent = new Intent(requireContext(), MapaTrasyActivity.class);
                intent.putExtra(MapaTrasyActivity.EXTRA_TRASA_ID, trasa.getnId().toString());
                startActivity(intent);
            });
        }

        public void bind(Trasa trasa) {
            this.trasa = trasa;

            imgTrasa.setImageResource(dobierzGrafikeTrasy(trasa));
            txtNazwa.setText(trasa.getNazwa());
            txtOpis.setText(trasa.getOpis());

            String parametry = String.format(
                    Locale.ROOT,
                    "%.1f km • %d m przew. • %d min • %s",
                    trasa.getDystansKm(),
                    trasa.getPrzewyzszenieM(),
                    trasa.getCzasMin(),
                    trasa.getPoziomTrudnosci()
            );

            txtParametry.setText(parametry);
            txtTyp.setText(trasa.getRegion() + " / " + trasa.getTyp());
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
            View widok = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_polecana_trasa, parent, false);
            return new TrasyHolder(widok);
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

    private class OstatnieTreningiHolder extends RecyclerView.ViewHolder {

        private final ImageView imgTrasa;
        private final TextView txtNazwa;
        private final TextView txtOpis;
        private final TextView txtParametry;
        private final TextView txtTyp;
        private TreningEntity trening;

        public OstatnieTreningiHolder(@NonNull View itemView) {
            super(itemView);

            imgTrasa = itemView.findViewById(R.id.imgTrasa);
            txtNazwa = itemView.findViewById(R.id.txtNazwaTrasy);
            txtOpis = itemView.findViewById(R.id.txtOpisTrasy);
            txtParametry = itemView.findViewById(R.id.txtParametryTrasy);
            txtTyp = itemView.findViewById(R.id.txtTypTrasy);

            itemView.setOnClickListener(v -> {
                if (trening == null || trening.trasaUuid == null) {
                    return;
                }

                Intent intent = new Intent(requireContext(), MapaTrasyActivity.class);
                intent.putExtra(MapaTrasyActivity.EXTRA_TRASA_ID, trening.trasaUuid);
                intent.putExtra(MapaTrasyActivity.EXTRA_TRASA_NAZWA, trening.nazwaTrasy);
                startActivity(intent);
            });
        }

        public void bind(TreningEntity trening) {
            this.trening = trening;

            imgTrasa.setImageResource(R.drawable.ic_route_placeholder);
            txtNazwa.setText(trening.nazwaTrasy == null ? "Trasa" : trening.nazwaTrasy);
            txtOpis.setText("Ostatnio: " + formatujDateTreningu(trening.dataStartu));

            String parametry = String.format(
                    Locale.ROOT,
                    "%.2f km • %d min • %d kcal",
                    trening.dystansKm,
                    trening.czasMin,
                    trening.kalorie
            );

            txtParametry.setText(parametry);
            txtTyp.setText("Zapisany trening");
        }
    }

    private class OstatnieTreningiAdapter extends RecyclerView.Adapter<OstatnieTreningiHolder> {

        private List<TreningEntity> treningi;

        public OstatnieTreningiAdapter(List<TreningEntity> treningi) {
            this.treningi = treningi;
        }

        public void setTreningi(List<TreningEntity> treningi) {
            this.treningi = treningi;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public OstatnieTreningiHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View widok = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_polecana_trasa, parent, false);
            return new OstatnieTreningiHolder(widok);
        }

        @Override
        public void onBindViewHolder(@NonNull OstatnieTreningiHolder holder, int position) {
            holder.bind(treningi.get(position));
        }

        @Override
        public int getItemCount() {
            return treningi == null ? 0 : treningi.size();
        }
    }

    private String formatujDateTreningu(long czasMs) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(czasMs));
    }
}
