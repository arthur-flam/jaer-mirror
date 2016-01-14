package ch.unizh.ini.jaer.config.spi;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import ch.unizh.ini.jaer.config.ConfigBit;
import net.sf.jaer.biasgen.Biasgen;

public class SPIConfigBit extends SPIConfigValue implements ConfigBit {

	private final boolean defaultValue;
	private boolean value;

	private final Biasgen biasgen;
	private final Preferences sprefs;

	public SPIConfigBit(final String configName, final String toolTip, final short moduleAddr, final short paramAddr,
		final boolean defaultValue, final Biasgen biasgen) {
		super(configName, toolTip, moduleAddr, paramAddr, 1);

		this.defaultValue = defaultValue;

		this.biasgen = biasgen;
		this.sprefs = biasgen.getChip().getPrefs();

		loadPreference();
		sprefs.addPreferenceChangeListener(this);
	}

	@Override
	public boolean isSet() {
		return value;
	}

	@Override
	public void set(final boolean value) {
		if (this.value != value) {
			setChanged();
		}

		this.value = value;

		notifyObservers();
	}

	@Override
	public String toString() {
		return String.format("SPIConfigBit {configName=%s, prefKey=%s, moduleAddr=%d, paramAddr=%d, numBits=%d, default=%b}", getName(),
			getPreferencesKey(), getModuleAddr(), getParamAddr(), getNumBits(), defaultValue);
	}

	@Override
	public void preferenceChange(final PreferenceChangeEvent e) {
		if (e.getKey().equals(getPreferencesKey())) {
			final boolean newVal = Boolean.parseBoolean(e.getNewValue());
			set(newVal);
		}
	}

	@Override
	public String getPreferencesKey() {
		return biasgen.getChip().getClass().getSimpleName() + "." + getName();
	}

	@Override
	public void loadPreference() {
		set(sprefs.getBoolean(getPreferencesKey(), defaultValue));
	}

	@Override
	public void storePreference() {
		sprefs.putBoolean(getPreferencesKey(), isSet());
	}
}
