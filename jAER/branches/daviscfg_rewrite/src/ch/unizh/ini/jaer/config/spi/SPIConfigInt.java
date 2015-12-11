package ch.unizh.ini.jaer.config.spi;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import ch.unizh.ini.jaer.config.ConfigInt;

public class SPIConfigInt extends SPIConfigValue implements ConfigInt {

	private final int defaultValue;
	private int value;
	private Preferences sprefs;

	public SPIConfigInt(final String configName, final String toolTip, final short moduleAddr, final short paramAddr, final int numBits,
		final int defaultValue, Preferences sprefs) {
		super(configName, toolTip, moduleAddr, paramAddr, numBits);

		this.defaultValue = defaultValue;

		this.sprefs = sprefs;
		loadPreference();
		sprefs.addPreferenceChangeListener(this);
	}

	@Override
	public int get() {
		return value;
	}

	@Override
	public void set(final int value) {
		if ((value < 0) || (value >= (1 << getNumBits()))) {
			throw new IllegalArgumentException("Attempted to store value=" + value
				+ ", which is larger than the maximum permitted value of " + (1 << getNumBits()) + " or negative, in " + this);
		}

		if (this.value != value) {
			setChanged();
		}

		this.value = value;

		notifyObservers();
	}

	@Override
	public String toString() {
		return String.format("SPIConfigInt {configName=%s, prefKey=%s, moduleAddr=%d, paramAddr=%d, numBits=%d, default=%d}", getName(),
			getPreferencesKey(), getModuleAddr(), getParamAddr(), getNumBits(), defaultValue);
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent e) {
		if (e.getKey().equals(getPreferencesKey())) {
			final int newVal = Integer.parseInt(e.getNewValue());
			set(newVal);
		}
	}

	@Override
	public void loadPreference() {
		set(sprefs.getInt(getPreferencesKey(), defaultValue));
	}

	@Override
	public void storePreference() {
		sprefs.putInt(getPreferencesKey(), get());
	}
}
