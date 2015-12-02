package net.sf.jaer2.devices.config.pots;

import java.util.EnumSet;

import javafx.beans.property.IntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import net.sf.jaer.jaerfx2.GUISupport;
import net.sf.jaer.jaerfx2.SSHS.SSHSType;
import net.sf.jaer.jaerfx2.SSHSNode;
import net.sf.jaer.jaerfx2.SSHSNode.SSHSAttrListener.AttributeEvents;

public class ShiftedSourceBiasCoarseFine extends AddressedIPot {
	public static enum OperatingMode {
		ShiftedSource(0, "ShiftedSource"),
		HiZ(1, "HiZ"),
		TiedToRail(2, "TiedToRail");

		public static final int mask = 0x0003;
		private final int bits;
		private final String str;

		OperatingMode(final int b, final String s) {
			bits = b;
			str = s;
		}

		public final int bits() {
			return bits << Integer.numberOfTrailingZeros(OperatingMode.mask);
		}

		@Override
		public final String toString() {
			return str;
		}
	}

	public static enum VoltageLevel {
		SplitGate(0, "SplitGate"),
		SingleDiode(1, "SingleDiode"),
		DoubleDiode(2, "DoubleDiode");

		public static final int mask = 0x000C;
		private final int bits;
		private final String str;

		VoltageLevel(final int b, final String s) {
			bits = b;
			str = s;
		}

		public final int bits() {
			return bits << Integer.numberOfTrailingZeros(VoltageLevel.mask);
		}

		@Override
		public final String toString() {
			return str;
		}
	}

	// 6 bits for level of shifted source
	/** Bit mask for bias bits */
	private static final int refBiasMask = 0x03F0;

	// 6 bits for bias current for shifted source buffer amplifier
	/** Bit mask for buffer bias bits */
	private static final int regBiasMask = 0xFC00;

	/** Number of bits used for bias value */
	private static final int numRefBiasBits = Integer.bitCount(ShiftedSourceBiasCoarseFine.refBiasMask);

	/**
	 * The number of bits specifying buffer bias current as fraction of master
	 * bias current
	 */
	private static final int numRegBiasBits = Integer.bitCount(ShiftedSourceBiasCoarseFine.regBiasMask);

	/** Max bias bit value */
	private static final int maxRefBitValue = (1 << ShiftedSourceBiasCoarseFine.numRefBiasBits) - 1;

	/** Maximum buffer bias value (all bits on) */
	private static final int maxRegBitValue = (1 << ShiftedSourceBiasCoarseFine.numRegBiasBits) - 1;

	public ShiftedSourceBiasCoarseFine(final String name, final String description, final SSHSNode configNode, final int address,
		final Masterbias masterbias, final Type type, final Sex sex) {
		this(name, description, configNode, address, masterbias, type, sex, ShiftedSourceBiasCoarseFine.maxRefBitValue,
			ShiftedSourceBiasCoarseFine.maxRegBitValue, OperatingMode.ShiftedSource, VoltageLevel.SplitGate);
	}

	public ShiftedSourceBiasCoarseFine(final String name, final String description, final SSHSNode configNode, final int address,
		final Masterbias masterbias, final Type type, final Sex sex, final int defaultRefBitValue, final int defaultRegBitValue,
		final OperatingMode opMode, final VoltageLevel vLevel) {
		super(name, description, configNode, address, masterbias, type, sex, 0,
			ShiftedSourceBiasCoarseFine.numRefBiasBits + ShiftedSourceBiasCoarseFine.numRegBiasBits + 4);
		// Add four bits for: operatingMode (2) and voltageLevel (2).
		setRefBitValue(defaultRefBitValue);

		setRegBitValue(defaultRegBitValue);

		setOperatingMode(opMode);

		setVoltageLevel(vLevel);
	}

	public int getRefBitValue() {
		return getConfigNode().getByte("ref");
	}

	public void setRefBitValue(final int ref) {
		getConfigNode().putByte("ref", (byte) ShiftedSourceBiasCoarseFine.clipRef(ref));
	}

	/**
	 * returns clipped value of potential new value for buffer bit value,
	 * constrained by limits of hardware.
	 *
	 * @param in
	 *            candidate new value.
	 * @return allowed value.
	 */
	private static int clipRef(final int in) {
		int out = in;

		if (in < 0) {
			out = 0;
		}
		if (in > ShiftedSourceBiasCoarseFine.maxRefBitValue) {
			out = ShiftedSourceBiasCoarseFine.maxRefBitValue;
		}

		return out;
	}

	public int getRegBitValue() {
		return getConfigNode().getByte("reg");
	}

	public void setRegBitValue(final int reg) {
		getConfigNode().putByte("reg", (byte) ShiftedSourceBiasCoarseFine.clipReg(reg));
	}

	/**
	 * returns clipped value of potential new value for buffer bit value,
	 * constrained by limits of hardware.
	 *
	 * @param in
	 *            candidate new value.
	 * @return allowed value.
	 */
	private static int clipReg(final int in) {
		int out = in;

		if (in < 0) {
			out = 0;
		}
		if (in > ShiftedSourceBiasCoarseFine.maxRegBitValue) {
			out = ShiftedSourceBiasCoarseFine.maxRegBitValue;
		}

		return out;
	}

	public static int getMaxRefBitValue() {
		return ShiftedSourceBiasCoarseFine.maxRefBitValue;
	}

	public static int getMinRefBitValue() {
		return 0;
	}

	public static int getMaxRegBitValue() {
		return ShiftedSourceBiasCoarseFine.maxRegBitValue;
	}

	public static int getMinRegBitValue() {
		return 0;
	}

	@Override
	public int getBitValueBits() {
		return ShiftedSourceBiasCoarseFine.numRefBiasBits + ShiftedSourceBiasCoarseFine.numRegBiasBits;
	}

	public OperatingMode getOperatingMode() {
		switch (getConfigNode().getString("operatingMode")) {
			case "ShiftedSource":
				return OperatingMode.ShiftedSource;

			case "HiZ":
				return OperatingMode.HiZ;

			case "TiedToRail":
				return OperatingMode.TiedToRail;

			default:
				return null;
		}
	}

	public void setOperatingMode(final OperatingMode opMode) {
		getConfigNode().putString("operatingMode", opMode.toString());
	}

	public VoltageLevel getVoltageLevel() {
		switch (getConfigNode().getString("voltageLevel")) {
			case "SplitGate":
				return VoltageLevel.SplitGate;

			case "SingleDiode":
				return VoltageLevel.SingleDiode;

			case "DoubleDiode":
				return VoltageLevel.DoubleDiode;

			default:
				return null;
		}
	}

	public void setVoltageLevel(final VoltageLevel vLevel) {
		getConfigNode().putString("voltageLevel", vLevel.toString());
	}

	/**
	 * sets the bit value based on desired current and {@link #masterbias}
	 * current.
	 * Observers are notified if value changes.
	 *
	 * @param current
	 *            in amps
	 * @return actual float value of current after resolution clipping.
	 */
	public float setRegCurrent(final float current) {
		final float im = getMasterbias().getCurrent();
		final float r = current / im;
		setRegBitValue(Math.round(r * ShiftedSourceBiasCoarseFine.getMaxRegBitValue()));
		return getRegCurrent();
	}

	/**
	 * Computes the estimated current based on the bit value for the current
	 * splitter and the {@link #masterbias}
	 *
	 * @return current in amps
	 */
	public float getRegCurrent() {
		final float im = getMasterbias().getCurrent();
		final float i = (im * getRegBitValue()) / ShiftedSourceBiasCoarseFine.getMaxRegBitValue();
		return i;
	}

	/**
	 * sets the bit value based on desired current and {@link #masterbias}
	 * current.
	 * Observers are notified if value changes.
	 *
	 * @param current
	 *            in amps
	 * @return actual float value of current after resolution clipping.
	 */
	public float setRefCurrent(final float current) {
		final float im = getMasterbias().getCurrent();
		final float r = current / im;
		setRefBitValue(Math.round(r * ShiftedSourceBiasCoarseFine.getMaxRefBitValue()));
		return getRefCurrent();
	}

	/**
	 * Computes the estimated current based on the bit value for the current
	 * splitter and the {@link #masterbias}
	 *
	 * @return current in amps
	 */
	public float getRefCurrent() {
		final float im = getMasterbias().getCurrent();
		final float i = (im * getRefBitValue()) / ShiftedSourceBiasCoarseFine.getMaxRefBitValue();
		return i;
	}

	/**
	 * Computes the actual bit pattern to be sent to chip based on configuration
	 * values.
	 * The order of the bits from the input end of the shift register is
	 * operating mode config bits, buffer bias current code bits, voltage level
	 * config bits, voltage level code bits.
	 */
	@Override
	protected long computeBinaryRepresentation() {
		int ret = 0;

		ret |= getOperatingMode().bits();

		ret |= getVoltageLevel().bits();

		ret |= getRefBitValue() << Integer.numberOfTrailingZeros(ShiftedSourceBiasCoarseFine.refBiasMask);

		ret |= getRegBitValue() << Integer.numberOfTrailingZeros(ShiftedSourceBiasCoarseFine.regBiasMask);

		return ret;
	}

	@Override
	protected void buildConfigGUI() {
		// Add name label, with description as tool-tip.
		final Label l = GUISupport.addLabel(rootConfigLayout, getName(), getDescription(), null, null);

		l.setPrefWidth(80);
		l.setAlignment(Pos.CENTER_RIGHT);

		final ComboBox<OperatingMode> opModeBox = GUISupport.addComboBox(rootConfigLayout, EnumSet.allOf(OperatingMode.class),
			getOperatingMode().ordinal());

		opModeBox.valueProperty().addListener((valueRef, oldValue, newValue) -> setOperatingMode(newValue));

		getConfigNode().addAttributeListener(null, (node, userData, event, changeKey, changeType, changeValue) -> {
			if ((changeType == SSHSType.STRING) && changeKey.equals("operatingMode")) {
				opModeBox.valueProperty().setValue(OperatingMode.ShiftedSource); // TODO: add others.
			}
		});

		final ComboBox<VoltageLevel> vLevelBox = GUISupport.addComboBox(rootConfigLayout, EnumSet.allOf(VoltageLevel.class),
			getVoltageLevel().ordinal());

		vLevelBox.valueProperty().addListener((valueRef, oldValue, newValue) -> setVoltageLevel(newValue));

		getConfigNode().addAttributeListener(null, (node, userData, event, changeKey, changeType, changeValue) -> {
			if ((changeType == SSHSType.STRING) && changeKey.equals("voltageLevel")) {
				vLevelBox.valueProperty().setValue(VoltageLevel.SplitGate); // TODO: add others.
			}
		});

		final IntegerProperty refProp = GUISupport.addTextNumberFieldWithSlider(rootConfigLayout, getRefBitValue(),
			ShiftedSourceBiasCoarseFine.getMinRefBitValue(), ShiftedSourceBiasCoarseFine.getMaxRefBitValue());
		refProp.addListener((valueRef, oldValue, newValue) -> setRefBitValue(newValue.intValue()));

		final IntegerProperty regProp = GUISupport.addTextNumberFieldWithSlider(rootConfigLayout, getRegBitValue(),
			ShiftedSourceBiasCoarseFine.getMinRegBitValue(), ShiftedSourceBiasCoarseFine.getMaxRegBitValue());
		regProp.addListener((valueRef, oldValue, newValue) -> setRegBitValue(newValue.intValue()));

		final Label binaryRep = GUISupport.addLabel(rootConfigLayout, getBinaryRepresentationAsString(),
			"Binary data to be sent to the device.", null, null);

		// Add listener directly to the node, so that any change to a
		// subordinate setting results in the update of the shift register
		// display value.
		getConfigNode().addAttributeListener(null, (node, userData, event, changeKey, changeType, changeValue) -> {
			if (event == AttributeEvents.ATTRIBUTE_MODIFIED) {
				// On any subordinate attribute update, refresh the
				// displayed value.
				binaryRep.setText(getBinaryRepresentationAsString());
			}
		});
	}

	@Override
	public String toString() {
		return String.format("%s, OperatingMode=%s, VoltageLevel=%s, refBitValue=%d, regBitValue=%d", super.toString(),
			getOperatingMode().toString(), getVoltageLevel().toString(), getRefBitValue(), getRegBitValue());
	}
}
