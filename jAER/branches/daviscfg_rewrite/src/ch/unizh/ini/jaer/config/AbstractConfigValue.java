/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.unizh.ini.jaer.config;

import java.util.Observable;
import java.util.prefs.PreferenceChangeListener;

import net.sf.jaer.biasgen.Biasgen.HasPreference;

public abstract class AbstractConfigValue extends Observable implements PreferenceChangeListener, HasPreference {

	private final String configName, toolTip, prefKey;

	public AbstractConfigValue(final String configName, final String toolTip) {
		this.configName = configName;
		this.toolTip = toolTip;
		prefKey = getClass().getSimpleName() + "." + configName;
	}

	public String getName() {
		return configName;
	}

	public String getDescription() {
		return toolTip;
	}

	public String getPreferencesKey() {
		return prefKey;
	}

	@Override
	public String toString() {
		return String.format("AbstractConfigValue {configName=%s, prefKey=%s}", getName(), getPreferencesKey());
	}

	@Override
	public synchronized void setChanged() {
		super.setChanged();
	}
}
