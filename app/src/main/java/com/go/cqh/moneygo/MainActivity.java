package com.go.cqh.moneygo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.go.cqh.moneygo.fragments.SettingFragment;
/**
 * Created by caoqianghui on 2016/12/1.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getFragmentManager().beginTransaction().replace(R.id.contentFrameLayout, new SettingFragment()).commit();
    }
}
