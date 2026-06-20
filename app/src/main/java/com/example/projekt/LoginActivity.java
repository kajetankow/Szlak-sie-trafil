package com.example.projekt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "tourroute_prefs";

    private TextInputEditText edtEmail;
    private TextInputEditText edtTelefon;
    private TextInputEditText edtHaslo;
    private TextInputLayout inputEmail;
    private LinearLayout panelTelefon;
    private RadioGroup radioLoginTyp;
    private RadioButton radioEmail;
    private RadioButton radioTelefon;
    private Spinner spinnerKraj;
    private Button btnZaloguj;
    private Button btnGosc;
    private TextView txtPowrotLogin;
    private TextView txtLinkRegister;

    private final List<KrajTelefon> kraje = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtLoginEmail);
        edtTelefon = findViewById(R.id.edtLoginTelefon);
        edtHaslo = findViewById(R.id.edtLoginHaslo);
        inputEmail = findViewById(R.id.inputLoginEmail);
        panelTelefon = findViewById(R.id.panelLoginTelefon);
        radioLoginTyp = findViewById(R.id.radioLoginTyp);
        radioEmail = findViewById(R.id.radioLoginEmail);
        radioTelefon = findViewById(R.id.radioLoginTelefon);
        spinnerKraj = findViewById(R.id.spinnerLoginKraj);
        btnZaloguj = findViewById(R.id.btnZaloguj);
        btnGosc = findViewById(R.id.btnKontynuujJakoGosc);
        txtPowrotLogin = findViewById(R.id.txtPowrotLogin);
        txtLinkRegister = findViewById(R.id.txtLinkRegister);

        przygotujKraje();
        przygotujSpinner();
        ustawTrybLogowania(true);

        txtPowrotLogin.setOnClickListener(v -> finish());
        btnGosc.setOnClickListener(v -> finish());
        btnZaloguj.setOnClickListener(v -> zaloguj());
        radioLoginTyp.setOnCheckedChangeListener((group, checkedId) ->
                ustawTrybLogowania(checkedId == R.id.radioLoginEmail)
        );
        ustawLinkRejestracji();
    }

    private void zaloguj() {
        boolean logowanieEmail = radioEmail.isChecked();
        String login = logowanieEmail ? tekst(edtEmail) : tekst(edtTelefon);
        String haslo = tekst(edtHaslo);

        if (TextUtils.isEmpty(login)) {
            if (logowanieEmail) {
                edtEmail.setError("Podaj email");
            } else {
                edtTelefon.setError("Podaj telefon");
            }
            return;
        }

        if (TextUtils.isEmpty(haslo)) {
            edtHaslo.setError("Podaj hasło");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String zapisanyEmail = prefs.getString("uzytkownik_email", "");
        String zapisanyTelefon = prefs.getString("uzytkownik_telefon", "");
        String zapisanyKontakt = prefs.getString("uzytkownik_kontakt", "");
        String sol = prefs.getString("uzytkownik_haslo_sol", "");
        String hash = prefs.getString("uzytkownik_haslo_hash", "");

        boolean loginPasuje;
        if (logowanieEmail) {
            loginPasuje = login.equalsIgnoreCase(zapisanyEmail)
                    || login.equalsIgnoreCase(zapisanyKontakt);
        } else {
            KrajTelefon kraj = (KrajTelefon) spinnerKraj.getSelectedItem();
            String loginTelefon = kraj == null ? login : kraj.kierunkowy + " " + oczyscTelefon(login);
            loginPasuje = oczyscTelefon(loginTelefon).equals(oczyscTelefon(zapisanyTelefon))
                    || oczyscTelefon(loginTelefon).equals(oczyscTelefon(zapisanyKontakt));
        }

        String hashPodany = PasswordUtils.hashujHaslo(haslo, sol);

        if (!loginPasuje || TextUtils.isEmpty(hash) || !hash.equals(hashPodany)) {
            Toast.makeText(this, "Niepoprawny login lub hasło", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit().putBoolean("uzytkownik_zalogowany", true);
        if (TextUtils.isEmpty(prefs.getString("uzytkownik_id", ""))) {
            editor.putString("uzytkownik_id", java.util.UUID.randomUUID().toString());
        }
        editor.apply();
        Toast.makeText(this, "Zalogowano", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void ustawLinkRejestracji() {
        String tekst = "Nie masz jeszcze konta? Zarejestruj się";
        SpannableString spannable = new SpannableString(tekst);
        int start = tekst.indexOf("Zarejestruj");

        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        }, start, tekst.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        txtLinkRegister.setText(spannable);
        txtLinkRegister.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void ustawTrybLogowania(boolean email) {
        inputEmail.setVisibility(email ? View.VISIBLE : View.GONE);
        panelTelefon.setVisibility(email ? View.GONE : View.VISIBLE);
        radioEmail.setChecked(email);
        radioTelefon.setChecked(!email);
    }

    private void przygotujKraje() {
        kraje.clear();
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

    private String tekst(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
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
