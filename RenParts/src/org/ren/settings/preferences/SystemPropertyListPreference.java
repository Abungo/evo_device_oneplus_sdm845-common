/*
 * SPDX-FileCopyrightText: 2024 Ren Linux / Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ren.settings.preferences;

import android.content.Context;
import android.os.SystemProperties;
import android.util.AttributeSet;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

public class SystemPropertyListPreference extends ListPreference implements Preference.OnPreferenceChangeListener {

    public SystemPropertyListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SystemPropertyListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SystemPropertyListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SystemPropertyListPreference(Context context) {
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
            String defaultStr = defaultValue != null ? defaultValue.toString() : "";
            String currentVal = SystemProperties.get(key, defaultStr);
            setValue(currentVal);
            setSummary(getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = getKey();
        if (key != null && newValue != null) {
            SystemProperties.set(key, newValue.toString());
            setValue(newValue.toString());
            setSummary(getEntry());
            return true;
        }
        return false;
    }
}
