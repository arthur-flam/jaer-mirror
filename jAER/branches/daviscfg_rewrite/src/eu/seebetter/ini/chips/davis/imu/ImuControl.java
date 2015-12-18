/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.seebetter.ini.chips.davis.imu;

import java.util.List;
import java.util.Observable;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import ch.unizh.ini.jaer.config.spi.SPIConfigBit;
import ch.unizh.ini.jaer.config.spi.SPIConfigInt;
import ch.unizh.ini.jaer.config.spi.SPIConfigValue;
import eu.seebetter.ini.chips.davis.DavisBaseCamera;
import eu.seebetter.ini.chips.davis.DavisConfig;
import eu.seebetter.ini.chips.davis.DavisDisplayConfigInterface;
import net.sf.jaer.biasgen.Biasgen;
import net.sf.jaer.util.HasPropertyTooltips;
import net.sf.jaer.util.PropertyTooltipSupport;

/**
 * IMU control of Invensense IMU-6100A, encapsulated here to control via DavisConfig object that contains the low level
 * control registers.
 */
public class ImuControl extends Observable implements HasPropertyTooltips, Biasgen.HasPreference, PreferenceChangeListener {
	PropertyTooltipSupport tooltipSupport = new PropertyTooltipSupport();
	private ImuGyroScale imuGyroScale;
	private ImuAccelScale imuAccelScale;
	private boolean displayImuEnabled;
	private final DavisConfig davisConfig;
	private final DavisBaseCamera davisChip;
	private final List<SPIConfigValue> imuControl;

	public ImuControl(final DavisConfig davisConfig, final List<SPIConfigValue> imuControl) {
		super();
		this.davisConfig = davisConfig;
		davisChip = (DavisBaseCamera) davisConfig.getChip();

		this.imuControl = imuControl;

		loadPreference();

		IMUSample.setFullScaleGyroDegPerSec(imuGyroScale.fullScaleDegPerSec);
		IMUSample.setGyroSensitivityScaleFactorDegPerSecPerLsb(1 / imuGyroScale.scaleFactorLsbPerDegPerSec);
		IMUSample.setFullScaleAccelG(imuAccelScale.fullScaleG);
		IMUSample.setAccelSensitivityScaleFactorGPerLsb(1 / imuAccelScale.scaleFactorLsbPerG);
		davisConfig.getChip().getPrefs().addPreferenceChangeListener(this);
	}

	public boolean isImuEnabled() {
		return ((SPIConfigBit) DavisConfig.getConfigValueByName(imuControl, "Run")).isSet();
	}

	public void setImuEnabled(final boolean yes) {
		((SPIConfigBit) DavisConfig.getConfigValueByName(imuControl, "Run")).set(yes);

		davisConfig.getSupport().firePropertyChange(DavisDisplayConfigInterface.PROPERTY_IMU_ENABLED, null, yes);
	}

	/**
	 * Register 26: CONFIG, digital low pass filter setting DLPF_CFG
	 */
	public void setDLPF(final int dlpf) {
		if ((dlpf < 0) || (dlpf > 6)) {
			throw new IllegalArgumentException("dlpf=" + dlpf + " is outside allowed range 0-6");
		}

		((SPIConfigInt) DavisConfig.getConfigValueByName(imuControl, "DigitalLowPassFilter")).set(dlpf);

		davisConfig.getSupport().firePropertyChange(DavisDisplayConfigInterface.PROPERTY_IMU_DLPF_CHANGED, null, dlpf);
	}

	public int getDLPF() {
		return ((SPIConfigInt) DavisConfig.getConfigValueByName(imuControl, "DigitalLowPassFilter")).get();
	}

	/**
	 * Register 27: Sample rate divider
	 */
	public void setSampleRateDivider(final int srd) {
		if ((srd < 0) || (srd > 255)) {
			throw new IllegalArgumentException("sampleRateDivider=" + srd + " is outside allowed range 0-255");
		}

		((SPIConfigInt) DavisConfig.getConfigValueByName(imuControl, "SampleRateDivider")).set(srd);

		davisConfig.getSupport().firePropertyChange(DavisDisplayConfigInterface.PROPERTY_IMU_SAMPLE_RATE_CHANGED, null, srd);
	}

	public int getSampleRateDivider() {
		return ((SPIConfigInt) DavisConfig.getConfigValueByName(imuControl, "SampleRateDivider")).get();
	}

	public ImuGyroScale getGyroScale() {
		return imuGyroScale;
	}

	public void setGyroScale(final ImuGyroScale scale) {
		final ImuGyroScale old = imuGyroScale;
		imuGyroScale = scale;
		setFS_SEL(imuGyroScale.fs_sel);
		IMUSample.setFullScaleGyroDegPerSec(imuGyroScale.fullScaleDegPerSec);
		IMUSample.setGyroSensitivityScaleFactorDegPerSecPerLsb(1 / imuGyroScale.scaleFactorLsbPerDegPerSec);
		davisConfig.getSupport().firePropertyChange(DavisDisplayConfigInterface.PROPERTY_IMU_GYRO_SCALE_CHANGED, old, imuGyroScale);
	}

	/**
	 * @return the imuAccelScale
	 */
	public ImuAccelScale getAccelScale() {
		return imuAccelScale;
	}

	/**
	 * @param imuAccelScale
	 *            the imuAccelScale to set
	 */
	public void setAccelScale(final ImuAccelScale scale) {
		final ImuAccelScale old = imuAccelScale;
		imuAccelScale = scale;
		setAFS_SEL(imuAccelScale.afs_sel);
		IMUSample.setFullScaleAccelG(imuAccelScale.fullScaleG);
		IMUSample.setAccelSensitivityScaleFactorGPerLsb(1 / imuAccelScale.scaleFactorLsbPerG);
		davisConfig.getSupport().firePropertyChange(DavisDisplayConfigInterface.PROPERTY_IMU_ACCEL_SCALE_CHANGED, old, imuAccelScale);
	}

	// accel scale bits
	private void setAFS_SEL(final int val) {
		// AFS_SEL bits are bits 4:3 in accel register
		if ((val < 0) || (val > 3)) {
			throw new IllegalArgumentException("value " + val + " is outside range 0-3");
		}

		((SPIConfigInt) DavisConfig.getConfigValueByName(imuControl, "AccelFullScale")).set(val);
	}

	// gyro scale bits
	private void setFS_SEL(final int val) {
		// AFS_SEL bits are bits 4:3 in accel register
		if ((val < 0) || (val > 3)) {
			throw new IllegalArgumentException("value " + val + " is outside range 0-3");
		}

		((SPIConfigInt) DavisConfig.getConfigValueByName(imuControl, "GyroFullScale")).set(val);
	}

	public boolean isDisplayImu() {
		return displayImuEnabled;
	}

	public void setDisplayImu(final boolean yes) {
		final boolean old = displayImuEnabled;
		displayImuEnabled = yes;
		davisConfig.getSupport().firePropertyChange(DavisDisplayConfigInterface.PROPERTY_IMU_DISPLAY_ENABLED, old, displayImuEnabled);
	}

	@Override
	public String getPropertyTooltip(final String propertyName) {
		return tooltipSupport.getPropertyTooltip(propertyName);
	}

	@Override
	public final void loadPreference() {
		try {
			setGyroScale(
				ImuGyroScale.valueOf(davisChip.getPrefs().get("ImuGyroScale", ImuGyroScale.GyroFullScaleDegPerSec1000.toString())));
			setAccelScale(ImuAccelScale.valueOf(davisChip.getPrefs().get("ImuAccelScale", ImuAccelScale.ImuAccelScaleG8.toString())));
			setDisplayImu(davisConfig.getChip().getPrefs().getBoolean("IMU.displayEnabled", true));
		}
		catch (final Exception e) {
			davisConfig.getChip().getLog().warning(e.toString());
		}
	}

	@Override
	public void storePreference() {
		davisChip.getPrefs().put("ImuGyroScale", imuGyroScale.toString());
		davisChip.getPrefs().put("ImuAccelScale", imuAccelScale.toString());
		davisChip.getPrefs().putBoolean("IMU.displayEnabled", displayImuEnabled);
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent e) {
		if (e.getKey().toLowerCase().contains("imu")) {
			davisChip.getLog().info(this + " preferenceChange(): event=" + e + " key=" + e.getKey() + " newValue=" + e.getNewValue());
		}
		if (e.getNewValue() == null) {
			return;
		}
		try {
			// log.info(e.toString());
			switch (e.getKey()) {
				case "ImuAccelScale":
					setAccelScale(ImuAccelScale.valueOf(e.getNewValue()));
					break;
				case "ImuGyroScale":
					setGyroScale(ImuGyroScale.valueOf(e.getNewValue()));
					break;
				case "IMU.displayEnabled":
					setDisplayImu(Boolean.valueOf(e.getNewValue()));
			}
		}
		catch (final IllegalArgumentException iae) {
			davisChip.getLog().warning(iae.toString() + ": Preference value=" + e.getNewValue() + " for the preferenc with key="
				+ e.getKey() + " is not a proper enum for an IMU setting");
		}
	}

}
