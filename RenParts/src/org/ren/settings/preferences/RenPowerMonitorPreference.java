/*
 * SPDX-FileCopyrightText: 2024 Ren Linux / Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ren.settings.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.ren.settings.R;
import org.ren.settings.views.RenRealtimeGraphView;

public class RenPowerMonitorPreference extends Preference {

    private RenRealtimeGraphView mGraphView;
    private int mGraphMode = RenRealtimeGraphView.MODE_POWER;

    public RenPowerMonitorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.ren_power_monitor_preference);
    }

    public RenPowerMonitorPreference(Context context) {
        this(context, null);
    }

    public void setGraphMode(int mode) {
        mGraphMode = mode;
        if (mGraphView != null) {
            mGraphView.setGraphMode(mode);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mGraphView = (RenRealtimeGraphView) holder.findViewById(R.id.power_realtime_graph);
        if (mGraphView != null) {
            mGraphView.setGraphMode(mGraphMode);
        }
    }

    public void addSample(float primary, float secondary) {
        if (mGraphView != null) {
            mGraphView.addSample(primary, secondary);
        }
    }
}
