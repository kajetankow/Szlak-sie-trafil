package com.example.projekt;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tour);

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            ustawFragment(new FragmentStart());
        }

        bottomNavigationView.setOnItemSelectedListener(this::obsluzMenuDolne);
    }

    private boolean obsluzMenuDolne(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment obecnyFragment = getSupportFragmentManager().findFragmentById(R.id.main);

        if (obecnyFragment instanceof FragmentDodajTrase) {
            FragmentDodajTrase fragmentDodajTrase = (FragmentDodajTrase) obecnyFragment;

            if (fragmentDodajTrase.czySaNiezapisaneDane()) {
                fragmentDodajTrase.potwierdzAnulowanieKreatora(() -> przejdzDoMenu(id));
                return false;
            }
        }

        przejdzDoMenu(id);
        return id == R.id.menu_mapa ? false : true;
    }

    private void przejdzDoMenu(int id) {
        if (id == R.id.menu_start) {
            ustawFragment(new FragmentStart());
            bottomNavigationView.getMenu().findItem(R.id.menu_start).setChecked(true);
            return;
        }

        if (id == R.id.menu_trasy) {
            ustawFragment(new FragmentTrasy());
            bottomNavigationView.getMenu().findItem(R.id.menu_trasy).setChecked(true);
            return;
        }

        if (id == R.id.menu_mapa) {
            startActivity(new Intent(this, MapaTrasyActivity.class));
            return;
        }

        if (id == R.id.menu_wiecej) {
            ustawFragment(new FragmentMenu());
            bottomNavigationView.getMenu().findItem(R.id.menu_wiecej).setChecked(true);
        }
    }

    private void ustawFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, fragment)
                .commit();
    }
}
