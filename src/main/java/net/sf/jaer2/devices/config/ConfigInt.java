package net.sf.jaer2.devices.config;

import java.util.EnumSet;

import javafx.beans.property.IntegerProperty;
import net.sf.jaer.jaerfx2.BoundedProperties;
import net.sf.jaer.jaerfx2.GUISupport;
import net.sf.jaer.jaerfx2.Numbers.NumberFormat;
import net.sf.jaer.jaerfx2.Numbers.NumberOptions;
import net.sf.jaer.jaerfx2.SSHS.SSHSType;
import net.sf.jaer.jaerfx2.SSHSNode;

public final class ConfigInt extends ConfigBase {
	private final int address;

	public ConfigInt(final String name, final String description, final SSHSNode configNode, final int defaultValue) {
		this(name, description, configNode, null, defaultValue);
	}

	public ConfigInt(final String name, final String description, final SSHSNode configNode, final Address address,
		final int defaultValue) {
		this(name, description, configNode, address, defaultValue, Integer.SIZE);
	}

	public ConfigInt(final String name, final String description, final SSHSNode configNode, final int defaultValue, final int numBits) {
		this(name, description, configNode, null, defaultValue, numBits);
	}

	public ConfigInt(final String name, final String description, final SSHSNode configNode, final Address address, final int defaultValue,
		final int numBits) {
		super(name, description, configNode, numBits);

		if (numBits < 2) {
			throw new IllegalArgumentException("Invalid numBits value, must be at least 2. Use ConfigBit for 1 bit quantities.");
		}

		if (numBits > 32) {
			throw new IllegalArgumentException("Invalid numBits value, must be at most 32. Use ConfigLong for larger quantities.");
		}

		if (address != null) {
			if (address.address() < 0) {
				throw new IllegalArgumentException("Negative addresses are not allowed!");
			}

			this.address = address.address();
		}
		else {
			this.address = -1;
		}

		setValue(defaultValue);
	}

	public int getValue() {
		return getConfigNode().getInt(getName());
	}

	public void setValue(final int val) {
		getConfigNode().putInt(getName(), val);
	}

	@Override
	public int getAddress() {
		if (address == -1) {
			throw new UnsupportedOperationException("Addressed mode not supported.");
		}

		return address;
	}

	@Override
	protected long computeBinaryRepresentation() {
		return getValue();
	}

	@Override
	protected void buildConfigGUI() {
		super.buildConfigGUI();

		final IntegerProperty intProp = new BoundedProperties.BoundedIntegerProperty(getValue(), (int) getMinBitValue(),
			(int) getMaxBitValue());

		GUISupport.addTextNumberField(rootConfigLayout, intProp, 10, (int) getMinBitValue(), (int) getMaxBitValue(), NumberFormat.DECIMAL,
			EnumSet.of(NumberOptions.UNSIGNED), null);

		GUISupport.addTextNumberField(rootConfigLayout, intProp, getNumBits(), (int) getMinBitValue(), (int) getMaxBitValue(),
			NumberFormat.BINARY, EnumSet.of(NumberOptions.UNSIGNED, NumberOptions.LEFT_PADDING, NumberOptions.ZERO_PADDING), null);

		intProp.addListener((valueRef, oldValue, newValue) -> setValue(newValue.intValue()));

		getConfigNode().addAttributeListener(null, (node, userData, event, changeKey, changeType, changeValue) -> {
			if ((changeType == SSHSType.INT) && changeKey.equals(getName())) {
				intProp.set(changeValue.getInt());
			}
		});
	}

	@Override
	public String toString() {
		return String.format("%s, value=%d", super.toString(), getValue());
	}
}
