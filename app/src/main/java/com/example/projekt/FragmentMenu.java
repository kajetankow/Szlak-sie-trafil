package com.example.projekt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentMenu extends Fragment {

    private static final String PREFS_NAME = "tourroute_prefs";
    private static final String PREFS_AKTYWNA_TRASA = "aktywna_trasa_prefs";

    private TextView txtKontoAvatar;
    private TextView txtKontoTytul;
    private TextView txtKontoOpis;
    private LinearLayout panelKontoLogin;
    private EditText edtKontoLogin;
    private EditText edtKontoHaslo;
    private Spinner spinnerKontoKraj;
    private Button btnKontoZaloguj;
    private TextView txtKontoLinkRegister;

    private Button btnEdytujProfil;
    private Button btnHistoriaTras;
    private Button btnAktywnosc;
    private Button btnWyloguj;
    private Button btnUsunKonto;

    private TrasyViewModel trasyViewModel;
    private List<TreningEntity> treningi = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtKontoAvatar = view.findViewById(R.id.txtKontoAvatar);
        txtKontoTytul = view.findViewById(R.id.txtKontoTytul);
        txtKontoOpis = view.findViewById(R.id.txtKontoOpis);
        panelKontoLogin = view.findViewById(R.id.panelKontoLogin);
        edtKontoLogin = view.findViewById(R.id.edtKontoLogin);
        edtKontoHaslo = view.findViewById(R.id.edtKontoHaslo);
        spinnerKontoKraj = view.findViewById(R.id.spinnerKontoKraj);
        btnKontoZaloguj = view.findViewById(R.id.btnKontoZaloguj);
        txtKontoLinkRegister = view.findViewById(R.id.txtKontoLinkRegister);
        btnEdytujProfil = view.findViewById(R.id.btnEdytujProfil);
        btnHistoriaTras = view.findViewById(R.id.btnHistoriaTras);
        btnAktywnosc = view.findViewById(R.id.btnAktywnosc);
        btnWyloguj = view.findViewById(R.id.btnWyloguj);
        btnUsunKonto = view.findViewById(R.id.btnUsunKonto);

        trasyViewModel = new ViewModelProvider(requireActivity()).get(TrasyViewModel.class);
        trasyViewModel.getTreningi().observe(getViewLifecycleOwner(), noweTreningi ->
                treningi = noweTreningi == null ? new ArrayList<>() : noweTreningi
        );

        btnEdytujProfil.setOnClickListener(v -> {
            if (!czyZalogowany()) {
                startActivity(new Intent(requireContext(), RegisterActivity.class));
            } else {
                pokazEdycjeProfilu();
            }
        });

        btnHistoriaTras.setOnClickListener(v -> pokazHistorieTrasPopup());
        btnAktywnosc.setOnClickListener(v -> pokazAktywnoscPopup());
        btnWyloguj.setOnClickListener(v -> wyloguj());
        btnUsunKonto.setOnClickListener(v -> pokazPierwszyKrokUsunieciaKonta());
        btnKontoZaloguj.setOnClickListener(v -> zalogujWKonto());
        przygotujSpinnerKonto();
        ustawLinkRejestracjiKonto();

        trasyViewModel.pobierzTreningi();
        odswiezKonto();
    }

    @Override
    public void onResume() {
        super.onResume();
        odswiezKonto();

        if (trasyViewModel != null) {
            trasyViewModel.pobierzTreningi();
        }
    }

    private void odswiezKonto() {
        SharedPreferences prefs = prefs();

        if (!czyZalogowany()) {
            txtKontoAvatar.setText("?");
            txtKontoTytul.setText("Cześć, gościu");
            txtKontoOpis.setText("Zaloguj się, aby zobaczyć profil, historię tras i statystyki.");
            panelKontoLogin.setVisibility(View.VISIBLE);
            btnEdytujProfil.setVisibility(View.GONE);
            btnHistoriaTras.setVisibility(View.GONE);
            btnAktywnosc.setVisibility(View.GONE);
            btnWyloguj.setVisibility(View.GONE);
            btnUsunKonto.setVisibility(View.GONE);
            btnWyloguj.setEnabled(false);
            btnUsunKonto.setEnabled(false);
            return;
        }

        String imie = prefs.getString("uzytkownik_imie", "");
        String nazwisko = prefs.getString("uzytkownik_nazwisko", "");
        String pseudonim = prefs.getString("uzytkownik_pseudonim", "");
        String waga = prefs.getString("uzytkownik_waga", "");
        String rok = prefs.getString("uzytkownik_rok_urodzenia", "");

        String nazwa = !pusty(pseudonim) ? pseudonim : (imie + " " + nazwisko).trim();
        if (pusty(nazwa)) {
            nazwa = "podróżniku";
        }

        txtKontoTytul.setText("Cześć, " + nazwa);
        txtKontoAvatar.setText(inicjaly(imie, nazwisko, pseudonim));
        txtKontoOpis.setText(
                tekstLubBrak((imie + " " + nazwisko).trim())
                        + " • Wiek: " + tekstLubBrak(obliczWiekTekst(rok))
                        + " • Waga: " + tekstLubBrak(waga) + " kg"
        );
        btnEdytujProfil.setText("Edytuj dane profilu");
        panelKontoLogin.setVisibility(View.GONE);
        btnEdytujProfil.setVisibility(View.VISIBLE);
        btnHistoriaTras.setVisibility(View.VISIBLE);
        btnAktywnosc.setVisibility(View.VISIBLE);
        btnWyloguj.setVisibility(View.VISIBLE);
        btnUsunKonto.setVisibility(View.VISIBLE);
        btnWyloguj.setEnabled(true);
        btnUsunKonto.setEnabled(true);
    }

    private void przygotujSpinnerKonto() {
        List<KrajTelefon> kraje = przygotujKrajeTelefonow();
        ArrayAdapter<KrajTelefon> adapter = new ArrayAdapter<KrajTelefon>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                kraje
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(kraje.get(position).kierunkowy);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKontoKraj.setAdapter(adapter);
    }

    private void ustawLinkRejestracjiKonto() {
        String tekst = "Nie masz jeszcze konta? Zarejestruj się";
        SpannableString spannable = new SpannableString(tekst);
        int start = tekst.indexOf("Zarejestruj");

        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(requireContext(), RegisterActivity.class));
            }
        }, start, tekst.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        txtKontoLinkRegister.setText(spannable);
        txtKontoLinkRegister.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void zalogujWKonto() {
        String login = tekst(edtKontoLogin);
        String haslo = tekst(edtKontoHaslo);

        if (TextUtils.isEmpty(login)) {
            edtKontoLogin.setError("Podaj email albo telefon");
            return;
        }

        if (TextUtils.isEmpty(haslo)) {
            edtKontoHaslo.setError("Podaj hasło");
            return;
        }

        SharedPreferences prefs = prefs();
        String zapisanyEmail = prefs.getString("uzytkownik_email", "");
        String zapisanyTelefon = prefs.getString("uzytkownik_telefon", "");
        String zapisanyKontakt = prefs.getString("uzytkownik_kontakt", "");
        String sol = prefs.getString("uzytkownik_haslo_sol", "");
        String hash = prefs.getString("uzytkownik_haslo_hash", "");

        KrajTelefon kraj = (KrajTelefon) spinnerKontoKraj.getSelectedItem();
        String loginTelefon = kraj == null ? login : kraj.kierunkowy + " " + oczyscTelefon(login);

        boolean loginPasuje = login.equalsIgnoreCase(zapisanyEmail)
                || oczyscTelefon(login).equals(oczyscTelefon(zapisanyTelefon))
                || oczyscTelefon(loginTelefon).equals(oczyscTelefon(zapisanyTelefon))
                || login.equalsIgnoreCase(zapisanyKontakt)
                || oczyscTelefon(login).equals(oczyscTelefon(zapisanyKontakt));

        String hashPodany = PasswordUtils.hashujHaslo(haslo, sol);

        if (!loginPasuje || TextUtils.isEmpty(hash) || !hash.equals(hashPodany)) {
            Toast.makeText(requireContext(), "Niepoprawny login lub hasło", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit().putBoolean("uzytkownik_zalogowany", true);
        if (TextUtils.isEmpty(prefs.getString("uzytkownik_id", ""))) {
            editor.putString("uzytkownik_id", java.util.UUID.randomUUID().toString());
        }
        editor.apply();
        edtKontoHaslo.setText("");
        odswiezKonto();
        Toast.makeText(requireContext(), "Zalogowano", Toast.LENGTH_SHORT).show();
    }

    private void pokazEdycjeProfilu() {
        SharedPreferences prefs = prefs();

        LinearLayout formularz = new LinearLayout(requireContext());
        formularz.setOrientation(LinearLayout.VERTICAL);
        formularz.setPadding(dp(18), dp(6), dp(18), 0);

        String kontakt = prefs.getString("uzytkownik_kontakt", "");
        String email = prefs.getString("uzytkownik_email", "");
        String telefon = prefs.getString("uzytkownik_telefon", "");

        if (pusty(email) && kontakt != null && kontakt.contains("@")) {
            email = kontakt;
        }

        if (pusty(telefon) && !pusty(kontakt) && !kontakt.contains("@")) {
            telefon = kontakt;
        }

        Spinner spinnerKraj = new Spinner(requireContext());
        List<KrajTelefon> kraje = przygotujKrajeTelefonow();
        ArrayAdapter<KrajTelefon> adapterKrajow = new ArrayAdapter<KrajTelefon>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                kraje
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(kraje.get(position).kierunkowy);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                KrajTelefon kraj = kraje.get(position);
                view.setText(kraj.nazwa + " (" + kraj.kierunkowy + ")");
                return view;
            }
        };
        adapterKrajow.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKraj.setAdapter(adapterKrajow);
        ustawKrajDlaTelefonu(spinnerKraj, kraje, telefon);

        EditText edtImie = pole("Imię", prefs.getString("uzytkownik_imie", ""));
        EditText edtNazwisko = pole("Nazwisko", prefs.getString("uzytkownik_nazwisko", ""));
        EditText edtPseudonim = pole("Pseudonim", prefs.getString("uzytkownik_pseudonim", ""));
        EditText edtRok = pole("Rok urodzenia", prefs.getString("uzytkownik_rok_urodzenia", ""));
        EditText edtWzrost = pole("Wzrost", prefs.getString("uzytkownik_wzrost", ""));
        EditText edtWaga = pole("Waga", prefs.getString("uzytkownik_waga", ""));
        EditText edtEmail = pole("Email", email);
        EditText edtTelefon = pole("Telefon", oczyscTelefon(telefon));

        edtRok.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtWzrost.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtWaga.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edtEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edtTelefon.setInputType(InputType.TYPE_CLASS_PHONE);

        formularz.addView(wierszPola("Imię", edtImie));
        formularz.addView(wierszPola("Nazwisko", edtNazwisko));
        formularz.addView(wierszPola("Pseudonim", edtPseudonim));
        formularz.addView(wierszPola("Rok ur.", edtRok));
        formularz.addView(wierszPola("Wzrost", edtWzrost));
        formularz.addView(wierszPola("Waga", edtWaga));
        formularz.addView(wierszPola("Email", edtEmail));
        formularz.addView(wierszWidoku("Kier.", spinnerKraj));
        formularz.addView(wierszPola("Telefon", edtTelefon));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Edytuj profil")
                .setView(formularz)
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Zapisz", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nowyEmail = tekst(edtEmail);
            String nowyTelefon = tekst(edtTelefon);

            if (pusty(nowyEmail) && pusty(nowyTelefon)) {
                Toast.makeText(requireContext(), "Podaj email albo telefon", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pusty(nowyEmail) && !Patterns.EMAIL_ADDRESS.matcher(nowyEmail).matches()) {
                edtEmail.setError("Podaj poprawny email");
                return;
            }

            if (!pusty(nowyTelefon) && oczyscTelefon(nowyTelefon).length() < 6) {
                edtTelefon.setError("Podaj poprawny telefon");
                return;
            }

            KrajTelefon kraj = (KrajTelefon) spinnerKraj.getSelectedItem();
            String telefonZKierunkowym = pusty(nowyTelefon)
                    ? ""
                    : kraj.kierunkowy + " " + oczyscTelefon(nowyTelefon);
            String kontaktTyp = !pusty(nowyEmail) ? "email" : "telefon";
            String nowyKontakt = !pusty(nowyEmail) ? nowyEmail : telefonZKierunkowym;

            prefs.edit()
                    .putString("uzytkownik_imie", tekst(edtImie))
                    .putString("uzytkownik_nazwisko", tekst(edtNazwisko))
                    .putString("uzytkownik_pseudonim", tekst(edtPseudonim))
                    .putString("uzytkownik_rok_urodzenia", tekst(edtRok))
                    .putString("uzytkownik_wzrost", tekst(edtWzrost))
                    .putString("uzytkownik_waga", tekst(edtWaga))
                    .putString("uzytkownik_email", nowyEmail)
                    .putString("uzytkownik_telefon", telefonZKierunkowym)
                    .putString("uzytkownik_kontakt_typ", kontaktTyp)
                    .putString("uzytkownik_kontakt", nowyKontakt)
                    .apply();

            dialog.dismiss();
            odswiezKonto();
            Toast.makeText(requireContext(), "Profil zapisany", Toast.LENGTH_SHORT).show();
        }));

        dialog.show();
        zaokraglijDialog(dialog);
    }

    private void pokazHistorieTrasPopup() {
        LinearLayout kontener = popupKontener();

        if (treningi.isEmpty()) {
            kontener.addView(info("Brak historii tras. Rozpocznij i zakończ trasę, aby pojawiła się tutaj."));
        } else {
            for (TreningEntity trening : treningi) {
                kontener.addView(kartaTreningu(trening));
            }
        }

        pokazPopup("Historia tras", kontener);
    }

    private void pokazAktywnoscPopup() {
        double kilometry = 0.0;
        int czasMin = 0;
        int kalorie = 0;
        int przewyzszenie = 0;

        for (TreningEntity trening : treningi) {
            kilometry += trening.dystansKm;
            czasMin += trening.czasMin;
            kalorie += trening.kalorie;
            przewyzszenie += trening.przewyzszenieM;
        }

        String tempo = kilometry > 0
                ? String.format(Locale.ROOT, "%.1f min/km", czasMin / kilometry)
                : "brak danych";

        LinearLayout kontener = popupKontener();
        kontener.addView(wierszStatystyki("Kilometry", String.format(Locale.ROOT, "%.2f km", kilometry)));
        kontener.addView(wierszStatystyki("Czas", czasMin + " min"));
        kontener.addView(wierszStatystyki("Kalorie", kalorie + " kcal"));
        kontener.addView(wierszStatystyki("Przewyższenie", przewyzszenie + " m"));
        kontener.addView(wierszStatystyki("Tempo", tempo));
        kontener.addView(wierszStatystyki("Liczba aktywności", String.valueOf(treningi.size())));

        pokazPopup("Aktywność", kontener);
    }

    private void pokazPopup(String tytul, LinearLayout zawartosc) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(zawartosc);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(tytul)
                .setView(scrollView)
                .setPositiveButton("Zamknij", null)
                .create();
        dialog.show();
        zaokraglijDialog(dialog);
    }

    private LinearLayout popupKontener() {
        LinearLayout kontener = new LinearLayout(requireContext());
        kontener.setOrientation(LinearLayout.VERTICAL);
        kontener.setPadding(dp(18), dp(8), dp(18), dp(6));
        return kontener;
    }

    private View kartaTreningu(TreningEntity trening) {
        LinearLayout karta = new LinearLayout(requireContext());
        karta.setOrientation(LinearLayout.VERTICAL);
        karta.setPadding(dp(14), dp(12), dp(14), dp(12));
        karta.setBackgroundResource(R.drawable.bg_card_route);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(10), 0, 0);
        karta.setLayoutParams(params);

        karta.addView(tekstKarty(trening.nazwaTrasy == null ? "Trasa" : trening.nazwaTrasy, 16, "#111827", true));
        karta.addView(tekstKarty(formatujDate(trening.dataStartu), 13, "#6B7280", false));
        karta.addView(tekstKarty(
                String.format(Locale.ROOT, "%.2f km • %d min • %d kcal", trening.dystansKm, trening.czasMin, trening.kalorie),
                14,
                "#374151",
                false
        ));
        return karta;
    }

    private TextView wierszStatystyki(String etykieta, String wartosc) {
        TextView textView = tekstKarty(etykieta + ": " + wartosc, 16, "#374151", false);
        textView.setPadding(0, dp(8), 0, dp(8));
        return textView;
    }

    private void wyloguj() {
        prefs().edit().putBoolean("uzytkownik_zalogowany", false).apply();
        odswiezKonto();
        Toast.makeText(requireContext(), "Wylogowano", Toast.LENGTH_SHORT).show();
    }

    private void pokazPierwszyKrokUsunieciaKonta() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Usunąć konto?")
                .setMessage("To usunie lokalne dane profilu i historię treningów z tego urządzenia.")
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Dalej", (d, which) -> pokazDrugiKrokUsunieciaKonta())
                .create();
        dialog.show();
        zaokraglijDialog(dialog);
    }

    private void pokazDrugiKrokUsunieciaKonta() {
        EditText pole = new EditText(requireContext());
        pole.setHint("Wpisz USUN");
        pole.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        pole.setPadding(dp(18), 0, dp(18), 0);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Potwierdzenie")
                .setMessage("Aby usunąć konto, wpisz: USUN")
                .setView(pole)
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Usuń konto", (d, which) -> {
                    if (!"USUN".equals(tekst(pole).toUpperCase(Locale.ROOT))) {
                        Toast.makeText(requireContext(), "Nie usunięto konta. Potwierdzenie było niepoprawne.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    usunKonto();
                })
                .create();
        dialog.show();
        zaokraglijDialog(dialog);
    }

    private void usunKonto() {
        prefs().edit()
                .remove("uzytkownik_zalogowany")
                .remove("uzytkownik_id")
                .remove("uzytkownik_imie")
                .remove("uzytkownik_nazwisko")
                .remove("uzytkownik_pseudonim")
                .remove("uzytkownik_rok_urodzenia")
                .remove("uzytkownik_wzrost")
                .remove("uzytkownik_waga")
                .remove("uzytkownik_email")
                .remove("uzytkownik_telefon")
                .remove("uzytkownik_kontakt_typ")
                .remove("uzytkownik_kontakt")
                .remove("uzytkownik_haslo_sol")
                .remove("uzytkownik_haslo_hash")
                .apply();

        requireContext().getSharedPreferences(PREFS_AKTYWNA_TRASA, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        trasyViewModel.usunWszystkieTreningi();
        odswiezKonto();
        Toast.makeText(requireContext(), "Konto usunięte", Toast.LENGTH_LONG).show();
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private boolean czyZalogowany() {
        return prefs().getBoolean("uzytkownik_zalogowany", false);
    }

    private EditText pole(String hint, String wartosc) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setText(wartosc == null ? "" : wartosc);
        editText.setSingleLine(true);
        return editText;
    }

    private View wierszPola(String etykieta, EditText editText) {
        return wierszWidoku(etykieta, editText);
    }

    private View wierszWidoku(String etykieta, View pole) {
        LinearLayout wiersz = new LinearLayout(requireContext());
        wiersz.setOrientation(LinearLayout.HORIZONTAL);
        wiersz.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, dp(6));
        wiersz.setLayoutParams(params);

        TextView label = tekstKarty(etykieta, 14, "#374151", true);
        label.setWidth(dp(86));

        if (pole instanceof EditText) {
            ((EditText) pole).setHint("");
        }

        pole.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        wiersz.addView(label);
        wiersz.addView(pole);
        return wiersz;
    }

    private void zaokraglijDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }
    }

    private TextView info(String tekst) {
        TextView textView = tekstKarty(tekst, 14, "#92400E", false);
        textView.setPadding(dp(14), dp(14), dp(14), dp(14));
        textView.setBackgroundResource(R.drawable.bg_panel_warning);
        return textView;
    }

    private TextView tekstKarty(String tekst, int rozmiar, String kolor, boolean pogrubiony) {
        TextView textView = new TextView(requireContext());
        textView.setText(tekst);
        textView.setTextSize(rozmiar);
        textView.setTextColor(Color.parseColor(kolor));
        textView.setPadding(0, dp(2), 0, dp(2));

        if (pogrubiony) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        return textView;
    }

    private String inicjaly(String imie, String nazwisko, String pseudonim) {
        String pierwsze = !pusty(imie) ? imie : pseudonim;
        String drugie = !pusty(nazwisko) ? nazwisko : "";
        String wynik = "";

        if (!pusty(pierwsze)) {
            wynik += pierwsze.substring(0, 1);
        }

        if (!pusty(drugie)) {
            wynik += drugie.substring(0, 1);
        }

        if (pusty(wynik)) {
            return "ST";
        }

        return wynik.toUpperCase(Locale.ROOT);
    }

    private String tekst(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String tekstLubBrak(String tekst) {
        return pusty(tekst) ? "brak" : tekst;
    }

    private boolean pusty(String tekst) {
        return tekst == null || tekst.trim().isEmpty();
    }

    private String obliczWiekTekst(String rokTekst) {
        if (pusty(rokTekst) || !rokTekst.matches("\\d{4}")) {
            return "";
        }

        int rok = Integer.parseInt(rokTekst);
        int obecnyRok = Calendar.getInstance().get(Calendar.YEAR);

        if (rok < 1900 || rok > obecnyRok) {
            return "";
        }

        return String.valueOf(obecnyRok - rok);
    }

    private String oczyscTelefon(String telefon) {
        if (telefon == null) {
            return "";
        }

        return telefon
                .replace("+48", "")
                .replace("+49", "")
                .replace("+420", "")
                .replace("+421", "")
                .replace("+380", "")
                .replace("+33", "")
                .replace("+34", "")
                .replace("+39", "")
                .replace("+44", "")
                .replace("+1", "")
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .trim();
    }

    private List<KrajTelefon> przygotujKrajeTelefonow() {
        List<KrajTelefon> kraje = new ArrayList<>();
        kraje.add(new KrajTelefon("Polska", "+48"));
        kraje.add(new KrajTelefon("Niemcy", "+49"));
        kraje.add(new KrajTelefon("Czechy", "+420"));
        kraje.add(new KrajTelefon("Słowacja", "+421"));
        kraje.add(new KrajTelefon("Ukraina", "+380"));
        kraje.add(new KrajTelefon("Francja", "+33"));
        kraje.add(new KrajTelefon("Hiszpania", "+34"));
        kraje.add(new KrajTelefon("Włochy", "+39"));
        kraje.add(new KrajTelefon("Wielka Brytania", "+44"));
        kraje.add(new KrajTelefon("USA / Kanada", "+1"));
        return kraje;
    }

    private void ustawKrajDlaTelefonu(Spinner spinner, List<KrajTelefon> kraje, String telefon) {
        if (telefon == null) {
            return;
        }

        for (int i = 0; i < kraje.size(); i++) {
            if (telefon.trim().startsWith(kraje.get(i).kierunkowy)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String formatujDate(long czasMs) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(czasMs));
    }

    private int dp(int wartosc) {
        return (int) (wartosc * getResources().getDisplayMetrics().density);
    }

    private static class KrajTelefon {
        private final String nazwa;
        private final String kierunkowy;

        private KrajTelefon(String nazwa, String kierunkowy) {
            this.nazwa = nazwa;
            this.kierunkowy = kierunkowy;
        }

        @NonNull
        @Override
        public String toString() {
            return nazwa + " (" + kierunkowy + ")";
        }
    }
}
