package net.sf.jaer2.devices.config.pots;

import java.util.EnumSet;

import javafx.beans.property.IntegerProperty;
import javafx.scene.control.Label;
import net.sf.jaer.jaerfx2.GUISupport;
import net.sf.jaer.jaerfx2.Numbers;
import net.sf.jaer.jaerfx2.Numbers.NumberFormat;
import net.sf.jaer.jaerfx2.Numbers.NumberOptions;
import net.sf.jaer.jaerfx2.SSHS;
import net.sf.jaer.jaerfx2.SSHSNode;
import net.sf.jaer.jaerfx2.SSHSNode.SSHSAttrListener.AttributeEvents;
import net.sf.jaer2.devices.config.ConfigBase;

public abstract class Pot extends ConfigBase {
	/** Type of bias, NORMAL or CASCODE. */
	public static enum Type {
		NORMAL("Normal"),
		CASCODE("Cascode");

		private final String str;

		private Type(final String s) {
			str = s;
		}

		@Override
		public final String toString() {
			return str;
		}
	}

	/** Transistor type for bias, N or P. */
	public static enum Sex {
		N("N"),
		P("P");

		private final String str;

		private Sex(final String s) {
			str = s;
		}

		@Override
		public final String toString() {
			return str;
		}
	}

	public Pot(final String name, final String description, final SSHSNode configNode, final Type type, final Sex sex) {
		this(name, description, configNode, type, sex, 0, 24);
	}

	public Pot(final String name, final String description, final SSHSNode configNode, final Type type, final Sex sex,
		final int defaultValue, final int numBits) {
		super(name, description, configNode, numBits);

		// Reset config node to be one level deeper that what is passed in, so
		// that each bias appears isolated inside their own node.
		this.configNode = SSHS.getRelativeNode(this.configNode, name + "/");

		setType(type);

		setSex(sex);

		setBitValue(defaultValue);
	}

	public Type getType() {
		return (getConfigNode().getString("type").equals(Type.NORMAL.toString())) ? (Type.NORMAL) : (Type.CASCODE);
	}

	public void setType(final Type t) {
		getConfigNode().putString("type", t.toString());
	}

	public Sex getSex() {
		return (getConfigNode().getString("sex").equals(Sex.N.toString())) ? (Sex.N) : (Sex.P);
	}

	public void setSex(final Sex s) {
		getConfigNode().putString("sex", s.toString());
	}

	public int getBitValue() {
		return getConfigNode().getInt("value");
	}

	public void setBitValue(final int bitVal) {
		getConfigNode().putInt("value", clip(bitVal));
	}

	private int clip(final int in) {
		int out = in;

		if (in < getMinBitValue()) {
			out = (int) getMinBitValue();
		}
		if (in > getMaxBitValue()) {
			out = (int) getMaxBitValue();
		}

		return out;
	}

	public int getBitValueBits() {
		return getNumBits();
	}

	@Override
	public long getMaxBitValue() {
		return ((1L << getBitValueBits()) - 1);
	}

	/** Increment bias value by one count. */
	public boolean incrementBitValue() {
		if (getBitValue() == getMaxBitValue()) {
			return false;
		}

		setBitValue(getBitValue() + 1);
		return true;
	}

	/** Decrement bias value by one count. */
	public boolean decrementBitValue() {
		if (getBitValue() == getMinBitValue()) {
			return false;
		}

		setBitValue(getBitValue() - 1);
		return true;
	}

	public String getBitValueAsString() {
		return Numbers
			.integerToString(getBitValue(), NumberFormat.BINARY,
				EnumSet.of(NumberOptions.UNSIGNED, NumberOptions.ZERO_PADDING, NumberOptions.LEFT_PADDING))
			.substring(Integer.SIZE - getBitValueBits(), Integer.SIZE);
	}

	/**
	 * Returns the physical value of the bias, e.g. for current Amps or for
	 * voltage Volts.
	 *
	 * @return physical value.
	 */
	abstract public float getPhysicalValue();

	/**
	 * Sets the physical value of the bias.
	 *
	 * @param value
	 *            the physical value, e.g. in Amps or Volts.
	 */
	abstract public void setPhysicalValue(float value);

	/** Return the unit (e.g. A, mV) of the physical value for this bias. */
	abstract public String getPhysicalValueUnits();

	@Override
	protected long computeBinaryRepresentation() {
		return getBitValue();
	}

	@Override
	protected void buildConfigGUI() {
		super.buildConfigGUI();

		GUISupport.addLabel(rootConfigLayout, getType().toString(), null);

		GUISupport.addLabel(rootConfigLayout, getSex().toString(), null);

		final IntegerProperty intProp = GUISupport.addTextNumberFieldWithSlider(rootConfigLayout, getBitValue(), (int) getMinBitValue(),
			(int) getMaxBitValue());

		GUISupport.addTextNumberField(rootConfigLayout, intProp, getBitValueBits(), (int) getMinBitValue(), (int) getMaxBitValue(),
			NumberFormat.BINARY, EnumSet.of(NumberOptions.UNSIGNED, NumberOptions.LEFT_PADDING, NumberOptions.ZERO_PADDING), null);

		final Label binaryRep = GUISupport.addLabel(rootConfigLayout, getBinaryRepresentationAsString(),
			"Binary data to be sent to the device.");

		intProp.addListener((valueRef, oldValue, newValue) -> setBitValue(newValue.intValue()));

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
		return String.format("%s, Type=%s, Sex=%s, bitValue=%d", super.toString(), getType().toString(), getSex().toString(),
			getBitValue());
	}
}
