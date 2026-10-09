/*
 * SPDX-FileCopyrightText: 2024 Ren Linux / Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ren.settings.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import org.ren.settings.R;

public class RenWifiSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.ren_settings_wifi, rootKey);
    }
}
