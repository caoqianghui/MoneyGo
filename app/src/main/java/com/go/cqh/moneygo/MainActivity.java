package com.go.cqh.moneygo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.go.cqh.moneygo.fragments.SettingFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getFragmentManager().beginTransaction().replace(R.id.contentFrameLayout, new SettingFragment()).commit();
    }
}
