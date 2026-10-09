/*
 * SPDX-FileCopyrightText: 2024 Ren Linux / Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ren.settings.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.ren.settings.R;
import org.ren.settings.preferences.RenPowerMonitorPreference;
import org.ren.settings.views.RenRealtimeGraphView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

public class RenDashboardSettingsFragment extends PreferenceFragmentCompat {

    private static final int ROLLING_WINDOW_SIZE = 20;

    private RenPowerMonitorPreference mPowerPref;
    private RenPowerMonitorPreference mCpuPref;
    private RenPowerMonitorPreference mThermalPref;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsListening = false;

    /* O(1) Circular Buffers */
    private final double[] mPowerBuffer = new double[ROLLING_WINDOW_SIZE];
    private final double[] mCurrentBuffer = new double[ROLLING_WINDOW_SIZE];
    private double mPowerSum = 0.0;
    private double mCurrentSum = 0.0;
    private int mBufferIndex = 0;
    private int mSampleCount = 0;

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateMetrics();
            if (mIsListening) {
                long interval = SystemProperties.getLong("persist.sys.power_monitor_interval", 1000);
                if (interval < 200) interval = 200;
                mHandler.postDelayed(this, interval);
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.ren_settings_dashboard, rootKey);

        mPowerPref = findPreference("ren_power_graph_card");
        mCpuPref = findPreference("ren_cpu_graph_card");
        mThermalPref = findPreference("ren_thermal_graph_card");

        if (mPowerPref != null) {
            mPowerPref.setGraphMode(RenRealtimeGraphView.MODE_POWER);
        }
        if (mCpuPref != null) {
            mCpuPref.setGraphMode(RenRealtimeGraphView.MODE_CPU);
        }
        if (mThermalPref != null) {
            mThermalPref.setGraphMode(RenRealtimeGraphView.MODE_THERMAL_GPU);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mIsListening) {
            mIsListening = true;
            mHandler.post(mUpdateRunnable);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mIsListening = false;
        mHandler.removeCallbacks(mUpdateRunnable);
    }

    private void updateMetrics() {
        Context context = getContext();
        if (context == null) return;

        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        /* Instantaneous Current (uA -> mA) */
        long currentNowUs = 0;
        if (bm != null) {
            currentNowUs = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        }
        if (currentNowUs == 0) {
            currentNowUs = readSysfsLong("/sys/class/power_supply/battery/current_now");
        }
        if (currentNowUs == 0) {
            currentNowUs = readSysfsLong("/sys/class/power_supply/bms/current_now");
        }
        if (currentNowUs == 0) {
            currentNowUs = readSysfsLong("/sys/class/power_supply/battery/batt_current");
        }

        double currentMa = currentNowUs / 1000.0;
        if (Math.abs(currentMa) > 10000) {
            currentMa = currentMa / 1000.0;
        }

        /* Voltage (mV) */
        long voltageMv = readSysfsLong("/sys/class/power_supply/battery/voltage_now");
        if (voltageMv == 0) {
            voltageMv = readSysfsLong("/sys/class/power_supply/bms/voltage_now");
        }
        if (voltageMv == 0) {
            voltageMv = readSysfsLong("/sys/class/power_supply/battery/batt_vol");
        }
        if (voltageMv == 0) {
            voltageMv = readSysfsLong("/sys/class/power_supply/battery/battery_voltage");
        }
        if (voltageMv == 0) {
            voltageMv = readSysfsLong("/sys/class/power_supply/bms/battery_voltage");
        }
        if (voltageMv == 0) {
            voltageMv = readSysfsLong("/sys/class/power_supply/battery/voltage_ocv");
        }
        if (voltageMv > 1000000) {
            voltageMv = voltageMv / 1000000;
        } else if (voltageMv > 10000) {
            voltageMv = voltageMv / 1000;
        }

        /* Real-Time Power (Watts) */
        double voltageVolts = voltageMv / 1000.0;
        double currentAmps = Math.abs(currentMa) / 1000.0;
        double powerWatts = voltageVolts * currentAmps;

        /* O(1) Sliding Window */
        if (mSampleCount >= ROLLING_WINDOW_SIZE) {
            mPowerSum -= mPowerBuffer[mBufferIndex];
            mCurrentSum -= mCurrentBuffer[mBufferIndex];
        } else {
            mSampleCount++;
        }
        mPowerBuffer[mBufferIndex] = powerWatts;
        mCurrentBuffer[mBufferIndex] = currentMa;
        mPowerSum += powerWatts;
        mCurrentSum += currentMa;
        mBufferIndex = (mBufferIndex + 1) % ROLLING_WINDOW_SIZE;

        double avgPowerWatts = mPowerSum / mSampleCount;
        double avgCurrentMa = mCurrentSum / mSampleCount;

        /* Battery Status, Temp, Level */
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int tempTenths = 0;
        int status = BatteryManager.BATTERY_STATUS_UNKNOWN;
        int level = 0;
        if (batteryStatus != null) {
            tempTenths = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        }
        double tempC = tempTenths / 10.0;

        String statusStr;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusStr = "Charging (+)";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusStr = "Discharging (-)";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusStr = "Full (100%)";
                break;
            default:
                statusStr = "Not Charging";
                break;
        }

        /* CPU Clocks */
        long littleCpuKhz = readSysfsLong("/sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq");
        if (littleCpuKhz == 0) {
            littleCpuKhz = readSysfsLong("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
        }
        long bigCpuKhz = readSysfsLong("/sys/devices/system/cpu/cpufreq/policy4/scaling_cur_freq");
        if (bigCpuKhz == 0) {
            bigCpuKhz = readSysfsLong("/sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq");
        }

        /* Thermal Sensors (SoC Junction) */
        long socTempRaw = readSysfsLong("/sys/class/thermal/thermal_zone0/temp");
        if (socTempRaw == 0) {
            socTempRaw = readSysfsLong("/sys/class/thermal/thermal_zone1/temp");
        }
        double socTempC = socTempRaw > 1000 ? socTempRaw / 1000.0 : (socTempRaw > 100 ? socTempRaw / 10.0 : socTempRaw);

        /* Adreno GPU Clock (Hz / MHz) */
        long gpuFreqHz = readSysfsLong("/sys/class/kgsl/kgsl-3d0/gpuclk");
        if (gpuFreqHz == 0) {
            gpuFreqHz = readSysfsLong("/sys/class/kgsl/kgsl-3d0/clock_mhz");
        }
        if (gpuFreqHz == 0) {
            gpuFreqHz = readSysfsLong("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq");
        }
        if (gpuFreqHz == 0) {
            gpuFreqHz = readSysfsLong("/sys/class/devfreq/5000000.qcom,kgsl-3d0/cur_freq");
        }
        long gpuMhz = gpuFreqHz > 1000000 ? gpuFreqHz / 1000000 : gpuFreqHz;

        /* 1. Update Power Card */
        if (mPowerPref != null) {
            String powerSummary = String.format(Locale.US,
                    "Live Power: %.2f W  •  20-Avg: %.2f W\n" +
                    "Current: %.0f mA (%s)  •  Voltage: %d mV  •  Batt: %.1f °C (%d%%)",
                    powerWatts, avgPowerWatts, currentMa, statusStr, voltageMv, tempC, level);
            mPowerPref.setSummary(powerSummary);
            mPowerPref.addSample((float) powerWatts, (float) Math.abs(currentMa));
        }

        /* 2. Update CPU Card */
        if (mCpuPref != null) {
            String cpuSummary = String.format(Locale.US,
                    "Big Kryo Cores (4-7): %d MHz\nLITTLE Cores (0-3): %d MHz",
                    bigCpuKhz / 1000, littleCpuKhz / 1000);
            mCpuPref.setSummary(cpuSummary);
            mCpuPref.addSample((float) (bigCpuKhz / 1000), (float) (littleCpuKhz / 1000));
        }

        /* 3. Update Thermal & GPU Card */
        if (mThermalPref != null) {
            String thermalSummary = String.format(Locale.US,
                    "SoC Junction Temp: %.1f °C\nAdreno 630 GPU Clock: %d MHz",
                    socTempC, gpuMhz);
            mThermalPref.setSummary(thermalSummary);
            mThermalPref.addSample((float) socTempC, (float) gpuMhz);
        }
    }

    private long readSysfsLong(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            if (line != null) {
                return Long.parseLong(line.trim());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
