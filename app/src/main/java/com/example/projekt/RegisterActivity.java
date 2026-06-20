package com.example.projekt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "tourroute_prefs";

    private TextView txtPowrotRegister;
    private TextView txtPokazRegulaminRegister;
    private TextView txtRegulaminRegister;

    private TextInputEditText edtImie;
    private TextInputEditText edtNazwisko;
    private TextInputEditText edtPseudonim;
    private TextInputEditText edtRokUrodzenia;
    private TextInputEditText edtWzrost;
    private TextInputEditText edtWaga;
    private TextInputEditText edtEmailRegister;
    private TextInputEditText edtTelefonRegister;
    private TextInputEditText edtHasloRegister;
    private TextInputEditText edtPowtorzHasloRegister;

    private RadioGroup radioKontakt;
    private RadioButton radioEmail;
    private RadioButton radioTelefon;

    private LinearLayout panelEmail;
    private LinearLayout panelTelefon;

    private Spinner spinnerKraj;
    private CheckBox checkRegulaminRegister;
    private Button btnRegisterSubmit;

    private final List<KrajTelefon> kraje = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        przypiszWidoki();
        przygotujKraje();
        przygotujSpinner();
        ustawListenery();
    }

    private void przypiszWidoki() {
        txtPowrotRegister = findViewById(R.id.txtPowrotRegister);
        txtPokazRegulaminRegister = findViewById(R.id.txtPokazRegulaminRegister);
        txtRegulaminRegister = findViewById(R.id.txtRegulaminRegister);

        edtImie = findViewById(R.id.edtImie);
        edtNazwisko = findViewById(R.id.edtNazwisko);
        edtPseudonim = findViewById(R.id.edtPseudonim);
        edtRokUrodzenia = findViewById(R.id.edtRokUrodzenia);
        edtWzrost = findViewById(R.id.edtWzrost);
        edtWaga = findViewById(R.id.edtWaga);
        edtEmailRegister = findViewById(R.id.edtEmailRegister);
        edtTelefonRegister = findViewById(R.id.edtTelefonRegister);
        edtHasloRegister = findViewById(R.id.edtHasloRegister);
        edtPowtorzHasloRegister = findViewById(R.id.edtPowtorzHasloRegister);

        radioKontakt = findViewById(R.id.radioKontakt);
        radioEmail = findViewById(R.id.radioEmail);
        radioTelefon = findViewById(R.id.radioTelefon);

        panelEmail = findViewById(R.id.panelEmail);
        panelTelefon = findViewById(R.id.panelTelefon);

        spinnerKraj = findViewById(R.id.spinnerKraj);
        checkRegulaminRegister = findViewById(R.id.checkRegulaminRegister);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
    }

    private void przygotujKraje() {
        kraje.clear();

        kraje.add(new KrajTelefon("Polska", "+48", 9, 9));
        kraje.add(new KrajTelefon("Niemcy", "+49", 10, 11));
        kraje.add(new KrajTelefon("Czechy", "+420", 9, 9));
        kraje.add(new KrajTelefon("Słowacja", "+421", 9, 9));
        kraje.add(new KrajTelefon("Ukraina", "+380", 9, 9));
        kraje.add(new KrajTelefon("Francja", "+33", 9, 9));
        kraje.add(new KrajTelefon("Hiszpania", "+34", 9, 9));
        kraje.add(new KrajTelefon("Włochy", "+39", 9, 10));
        kraje.add(new KrajTelefon("Wielka Brytania", "+44", 10, 10));
        kraje.add(new KrajTelefon("USA / Kanada", "+1", 10, 10));
    }

    private void przygotujSpinner() {
        ArrayAdapter<KrajTelefon> adapter = new ArrayAdapter<KrajTelefon>(
                this,
                android.R.layout.simple_spinner_item,
                kraje
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(kraje.get(position).kierunkowy);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                KrajTelefon kraj = kraje.get(position);
                view.setText(kraj.nazwa + " (" + kraj.kierunkowy + ")");
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKraj.setAdapter(adapter);
    }

    private void ustawListenery() {
        txtPowrotRegister.setOnClickListener(v -> finish());

        txtPokazRegulaminRegister.setOnClickListener(v -> {
            if (txtRegulaminRegister.getVisibility() == View.VISIBLE) {
                txtRegulaminRegister.setVisibility(View.GONE);
                txtPokazRegulaminRegister.setText("Pokaż regulamin");
            } else {
                txtRegulaminRegister.setVisibility(View.VISIBLE);
                txtPokazRegulaminRegister.setText("Ukryj regulamin");
            }
        });

        radioKontakt.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioEmail) {
                panelEmail.setVisibility(View.VISIBLE);
                panelTelefon.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioTelefon) {
                panelEmail.setVisibility(View.GONE);
                panelTelefon.setVisibility(View.VISIBLE);
            }
        });

        btnRegisterSubmit.setOnClickListener(v -> zarejestruj());
    }

    private void zarejestruj() {
        String imie = tekst(edtImie);
        String nazwisko = tekst(edtNazwisko);
        String pseudonim = tekst(edtPseudonim);
        String rokUrodzeniaTekst = tekst(edtRokUrodzenia);
        String wzrostTekst = tekst(edtWzrost);
        String wagaTekst = tekst(edtWaga);
        String email = tekst(edtEmailRegister);
        String telefon = tekst(edtTelefonRegister);
        String haslo = tekst(edtHasloRegister);
        String powtorzHaslo = tekst(edtPowtorzHasloRegister);

        if (TextUtils.isEmpty(imie)) {
            edtImie.setError("Podaj imię");
            edtImie.requestFocus();
            return;
        }

        if (imie.length() < 2) {
            edtImie.setError("Imię jest za krótkie");
            edtImie.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(nazwisko)) {
            edtNazwisko.setError("Podaj nazwisko");
            edtNazwisko.requestFocus();
            return;
        }

        if (nazwisko.length() < 2) {
            edtNazwisko.setError("Nazwisko jest za krótkie");
            edtNazwisko.requestFocus();
            return;
        }

        if (!walidujRokUrodzenia(rokUrodzeniaTekst)) {
            edtRokUrodzenia.setError("Podaj poprawny rok urodzenia");
            edtRokUrodzenia.requestFocus();
            return;
        }

        if (!walidujWzrost(wzrostTekst)) {
            edtWzrost.setError("Podaj poprawny wzrost");
            edtWzrost.requestFocus();
            return;
        }

        if (!walidujWage(wagaTekst)) {
            edtWaga.setError("Podaj poprawną wagę");
            edtWaga.requestFocus();
            return;
        }

        boolean wybranoEmail = radioEmail.isChecked();

        String kontaktTyp;
        String kontaktWartosc;

        if (wybranoEmail) {
            if (!walidujEmail(email)) {
                edtEmailRegister.setError("Podaj poprawny adres email");
                edtEmailRegister.requestFocus();
                return;
            }

            kontaktTyp = "email";
            kontaktWartosc = email;
        } else {
            KrajTelefon kraj = (KrajTelefon) spinnerKraj.getSelectedItem();

            if (!walidujTelefon(telefon, kraj)) {
                edtTelefonRegister.setError("Podaj poprawny numer telefonu");
                edtTelefonRegister.requestFocus();
                return;
            }

            kontaktTyp = "telefon";
            kontaktWartosc = kraj.kierunkowy + " " + oczyscTelefon(telefon);
        }

        if (!walidujHaslo(haslo)) {
            edtHasloRegister.setError("Min. 8 znaków, cyfra i znak specjalny");
            edtHasloRegister.requestFocus();
            return;
        }

        if (!haslo.equals(powtorzHaslo)) {
            edtPowtorzHasloRegister.setError("Hasła muszą być takie same");
            edtPowtorzHasloRegister.requestFocus();
            return;
        }

        if (!checkRegulaminRegister.isChecked()) {
            Toast.makeText(this, "Musisz zaakceptować regulamin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(pseudonim)) {
            pseudonim = imie + " " + nazwisko;
        }

        zapiszUzytkownika(
                imie,
                nazwisko,
                pseudonim,
                rokUrodzeniaTekst,
                wzrostTekst,
                wagaTekst,
                kontaktTyp,
                kontaktWartosc,
                email,
                wybranoEmail ? "" : kontaktWartosc,
                haslo
        );

        Toast.makeText(this, "Konto utworzone. Zalogowano jako: " + pseudonim, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void zapiszUzytkownika(String imie, String nazwisko, String pseudonim,
                                   String rokUrodzenia, String wzrost, String waga,
                                   String kontaktTyp, String kontaktWartosc,
                                   String email, String telefon, String haslo) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String sol = PasswordUtils.utworzSol();
        String hash = PasswordUtils.hashujHaslo(haslo, sol);

        prefs.edit()
                .putBoolean("uzytkownik_zalogowany", true)
                .putString("uzytkownik_id", UUID.randomUUID().toString())
                .putString("uzytkownik_imie", imie)
                .putString("uzytkownik_nazwisko", nazwisko)
                .putString("uzytkownik_pseudonim", pseudonim)
                .putString("uzytkownik_rok_urodzenia", rokUrodzenia)
                .putString("uzytkownik_wzrost", wzrost)
                .putString("uzytkownik_waga", waga)
                .putString("uzytkownik_email", email)
                .putString("uzytkownik_telefon", telefon)
                .putString("uzytkownik_kontakt_typ", kontaktTyp)
                .putString("uzytkownik_kontakt", kontaktWartosc)
                .putString("uzytkownik_haslo_sol", sol)
                .putString("uzytkownik_haslo_hash", hash)
                .apply();
    }

    private String tekst(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private boolean walidujEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean walidujTelefon(String telefon, KrajTelefon kraj) {
        if (kraj == null) {
            return false;
        }

        String oczyszczony = oczyscTelefon(telefon);

        if (!oczyszczony.matches("\\d+")) {
            return false;
        }

        return oczyszczony.length() >= kraj.minCyfr && oczyszczony.length() <= kraj.maxCyfr;
    }

    private String oczyscTelefon(String telefon) {
        return telefon
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace(".", "")
                .trim();
    }

    private boolean walidujRokUrodzenia(String rokTekst) {
        if (TextUtils.isEmpty(rokTekst) || !rokTekst.matches("\\d{4}")) {
            return false;
        }

        int rok = Integer.parseInt(rokTekst);
        return rok >= 1900 && rok <= 2020;
    }

    private boolean walidujWzrost(String wzrostTekst) {
        if (TextUtils.isEmpty(wzrostTekst) || !wzrostTekst.matches("\\d+")) {
            return false;
        }

        int wzrost = Integer.parseInt(wzrostTekst);
        return wzrost >= 80 && wzrost <= 250;
    }

    private boolean walidujWage(String wagaTekst) {
        if (TextUtils.isEmpty(wagaTekst)) {
            return false;
        }

        try {
            double waga = Double.parseDouble(wagaTekst.replace(",", "."));
            return waga >= 20 && waga <= 300;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean walidujHaslo(String haslo) {
        if (TextUtils.isEmpty(haslo) || haslo.length() < 8) {
            return false;
        }

        boolean maCyfre = haslo.matches(".*\\d.*");
        boolean maZnakSpecjalny = haslo.matches(".*[^A-Za-z0-9].*");

        return maCyfre && maZnakSpecjalny;
    }

    private static class KrajTelefon {

        private final String nazwa;
        private final String kierunkowy;
        private final int minCyfr;
        private final int maxCyfr;

        private KrajTelefon(String nazwa, String kierunkowy, int minCyfr, int maxCyfr) {
            this.nazwa = nazwa;
            this.kierunkowy = kierunkowy;
            this.minCyfr = minCyfr;
            this.maxCyfr = maxCyfr;
        }

        @NonNull
        @Override
        public String toString() {
            return nazwa + " (" + kierunkowy + ")";
        }
    }
}
