package com.go.cqh.moneygo.fragments;


import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.go.cqh.moneygo.R;

import java.util.List;

/**
 * Created by caoqianghui on 2016/12/1.
 */
public class SettingFragment extends PreferenceFragment implements AccessibilityManager.AccessibilityStateChangeListener {


    private Preference watch_auto_btn;
    private AccessibilityManager accessibilityManager;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting);
        initPreferencesClick();
        //监听AccessibilityService 变化
        accessibilityManager = (AccessibilityManager) getActivity().getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    /**
     * 更新开关按扭
     */
    private void updateState() {
        if (isServiceEnabled()) {
            ((SwitchPreference) watch_auto_btn).setChecked(true);
        } else {
            ((SwitchPreference) watch_auto_btn).setChecked(false);
        }
    }

    /**
     * preferences开关点击事件
     */
    private void initPreferencesClick() {
        watch_auto_btn = findPreference("watch_auto_btn");
        watch_auto_btn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isSelect = newValue.toString().equals("true");
                if (isSelect) {
                    Toast.makeText(getActivity(), "点击「无障碍」开启「MoneyGo服务」", Toast.LENGTH_LONG).show();
                    /*启动监控服务*/
                    Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(accessibleIntent);
                }
                return true;
            }
        });
    }
    /**
     * 获取 HongbaoService 是否启用状态
     *
     * @return
     */
    private boolean isServiceEnabled() {
        List<AccessibilityServiceInfo> accessibilityServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getActivity().getPackageName() + "/.services.MoneyGoService")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        updateState();
    }
}
