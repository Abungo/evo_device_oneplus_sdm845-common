/*
 * SPDX-FileCopyrightText: 2024 Ren Linux / Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ren.settings.preferences;

import android.content.Context;
import android.os.SystemProperties;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

public class SystemPropertySwitchPreference extends SwitchPreferenceCompat implements Preference.OnPreferenceChangeListener {

    public SystemPropertySwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SystemPropertySwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SystemPropertySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SystemPropertySwitchPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        String key = getKey();
        if (key != null) {
            boolean defaultBool = defaultValue instanceof Boolean ? (Boolean) defaultValue : false;
            setChecked(SystemProperties.getBoolean(key, defaultBool));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = getKey();
        if (key != null && newValue instanceof Boolean) {
            boolean val = (Boolean) newValue;
            SystemProperties.set(key, Boolean.toString(val));
            setChecked(val);
            return true;
        }
        return false;
    }
}
