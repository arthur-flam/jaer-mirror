/**
 * CochleaLPChannelConfig.java
 *
 * @author Ilya Kiselev, ilya.fpga@gmail.com
 * 
 * Created on August 7, 2016, 22:23
 */
package ch.unizh.ini.jaer.chip.cochlea;

import ch.unizh.ini.jaer.config.MultiBitValue;
import net.sf.jaer.chip.AEChip;

/** 
 * Represents a configuration register for the CochleaTow4 channel. Actual bit fields are defined in MultiBitValue[] parameters
 */
public class CochleaTow4EarChannelConfig extends CochleaChannelConfig {
	private static final MultiBitValue[] CHANNEL_FIELDS;

	// Create the structure of the cochlea channel
	static {
		CHANNEL_FIELDS = new MultiBitValue[12];
		// Leak current for second two neurons (L/R)
		CHANNEL_FIELDS[0] = new MultiBitValue(1, 19, 
			"Clear = Double leak current for second two neurons (B/T)");
		// Current Gain for first two neurons (L/R)
		CHANNEL_FIELDS[1] = new MultiBitValue(3, 16, 
			"PGA gain control for second two neurons (B/T), 3 bits, effectively 4 levels, uses only values 0,1,3,7. 0 is highest gain, 7 is lowest");
		// Leak current for first two neurons (B/T)
		CHANNEL_FIELDS[2] = new MultiBitValue(1, 15, 
			"Clear = Double leak current for first two neurons (B/T)");
		// Current Gain for first two neurons (B/T)
		CHANNEL_FIELDS[3] = new MultiBitValue(3, 12, 
			"PGA gain control for first two neurons (B/T), 3 bits, effectively 4 levels, uses only values 0,1,3,7. 0 is highest gain, 7 is lowest");
		// Double leak current
		CHANNEL_FIELDS[4] = new MultiBitValue(1, 11, 
			"Double leak current for all 4 neurons. nQ3=0 adds one more NeuronVleak branch to neuron");
		// Diff1
		CHANNEL_FIELDS[5] = new MultiBitValue(3, 8, 
			"diff1. nQ0=0 adds 25fF to feedback branch, nQ1=0 adds 33.3fF to FB branch, nQ2=0 adds 100fF to FB branch");
		// qTuning
		CHANNEL_FIELDS[6] = new MultiBitValue(3, 5, 
			"Gain for SOS. Q tuning for SO. Q5, Q6, Q7 adds one xtoron the other leg of diffpair.");
		CHANNEL_FIELDS[7] = new MultiBitValue(1, 4, 
			"Gain for SOS. Q tuning for SO. nQ4 picks chain with 1 xtor in series");
		CHANNEL_FIELDS[8] = new MultiBitValue(1, 3, 
			"Gain for SOS. Q tuning for SO. nQ3 picks chain with 2 xtor in series");
		CHANNEL_FIELDS[9] = new MultiBitValue(1, 2, 
			"Gain for SOS. Q tuning for SO. nQ2 picks chain with 3 xtor in series");
		CHANNEL_FIELDS[10] = new MultiBitValue(1, 1, 
			"Gain for SOS. Q tuning for SO. nQ1 picks chain with 4 xtor in series");
		CHANNEL_FIELDS[11] = new MultiBitValue(1, 0, 
			"Gain for SOS. Q tuning for SO. nQ0 picks chain with 5 xtor in series");
	}
	public CochleaTow4EarChannelConfig(final String configName, final String toolTip, final int channelAddress, final AEChip chip) {
		super(CHANNEL_FIELDS, configName, toolTip, channelAddress, chip);
	}

	@Override
	public String toString() {
		return String.format("CochleaTow4EarChannel {configName=%s, prefKey=%s, channelAddress=%d}", getName(), getPreferencesKey(), channelAddress);
	}

	// Sort of a static iterface which have to be implemented in every ChannelConfig class
	public static int getFieldsNumber() {
		return CHANNEL_FIELDS.length;
	}

	public static int[] getFieldsLengths() {
		final int[] fieldsLengths = new int[CHANNEL_FIELDS.length];
		for (int i = 0; i < CHANNEL_FIELDS.length; ++i) {
			fieldsLengths[i] = CHANNEL_FIELDS[i].length;
		}
		return fieldsLengths;
	}

	public static MultiBitValue getFieldConfig(int i) {
		return CHANNEL_FIELDS[i];
	}
}
