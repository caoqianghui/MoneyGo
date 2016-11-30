package com.go.cqh.moneygo.fragments;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.widget.Toast;

import com.go.cqh.moneygo.R;

public class SettingFragment extends PreferenceFragment {


    private boolean selectable;
    private Preference watch_auto_btn;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.firstsetting);
        initPreferencesClick();
    }

    private void initPreferencesClick() {
        watch_auto_btn = findPreference("watch_auto_btn");
        //watch_auto_btn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        //    @Override
        //    public boolean onPreferenceClick(Preference preference) {
        //        return false;
        //    }
        //});
        watch_auto_btn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isSelect = newValue.toString().equals("true");
                if (isSelect) {
                    Toast.makeText(getActivity(), "点击「无障碍」开启MoneyGo", Toast.LENGTH_SHORT).show();
                    Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(accessibleIntent);
                }
                return true;
            }
        });
    }

}
