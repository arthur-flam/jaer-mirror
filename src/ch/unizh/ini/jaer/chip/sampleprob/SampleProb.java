package ch.unizh.ini.jaer.chip.sampleprob;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ch.unizh.ini.jaer.chip.cochlea.CochleaAMS1cRollingCochleagramADCDisplayMethod;
import ch.unizh.ini.jaer.chip.cochlea.CochleaAMSEvent;
import ch.unizh.ini.jaer.chip.cochlea.CochleaChip;
import ch.unizh.ini.jaer.chip.cochlea.CochleaLP;
import ch.unizh.ini.jaer.chip.cochlea.CochleaLP.AbstractConfigValue;
import ch.unizh.ini.jaer.chip.cochlea.CochleaLP.SPIConfigBit;
import ch.unizh.ini.jaer.chip.cochlea.CochleaLP.SPIConfigInt;
import ch.unizh.ini.jaer.chip.cochlea.CochleaLP.SPIConfigValue;
import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;
import net.sf.jaer.aemonitor.AEPacketRaw;
import net.sf.jaer.biasgen.BiasgenHardwareInterface;
import net.sf.jaer.biasgen.IPot;
import net.sf.jaer.biasgen.IPotArray;
import net.sf.jaer.biasgen.Pot;
import net.sf.jaer.biasgen.PotArray;
import net.sf.jaer.biasgen.VDAC.DAC;
import net.sf.jaer.biasgen.VDAC.VPot;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.chip.Chip;
import net.sf.jaer.chip.TypedEventExtractor;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.event.OutputEventIterator;
import net.sf.jaer.hardwareinterface.HardwareInterface;
import net.sf.jaer.hardwareinterface.HardwareInterfaceException;
import net.sf.jaer.hardwareinterface.usb.cypressfx3libusb.CypressFX3;

@Description("Probabilistic Sample circuit")
@DevelopmentStatus(DevelopmentStatus.Status.Experimental)
public class SampleProb extends CochleaChip implements Observer {
	/** Creates a new instance of SampleProb */
	public SampleProb() {
		super();
		addObserver(this);

		setName("SampleProb");
		setEventClass(CochleaAMSEvent.class);

		setSizeX(16);
		setSizeY(1);
		setNumCellTypes(1);

		setRenderer(new CochleaLP.Renderer(this));
		setBiasgen(new SampleProb.Biasgen(this));
		setEventExtractor(new SampleProb.Extractor(this));

		getCanvas().setBorderSpacePixels(40);
		getCanvas().addDisplayMethod(new CochleaAMS1cRollingCochleagramADCDisplayMethod(getCanvas()));
	}

	/**
	 * Updates AEViewer specialized menu items according to capabilities of
	 * HardwareInterface.
	 *
	 * @param o
	 *            the observable, i.e. this Chip.
	 * @param arg
	 *            the argument (e.g. the HardwareInterface).
	 */
	@Override
	public void update(final Observable o, final Object arg) {
		// Nothing to do here.
	}

	/**
	 * overrides the Chip setHardware interface to construct a biasgen if one doesn't exist already.
	 * Sets the hardware interface and the bias generators hardware interface
	 *
	 * @param hardwareInterface
	 *            the interface
	 */
	@Override
	public void setHardwareInterface(final HardwareInterface hardwareInterface) {
		this.hardwareInterface = hardwareInterface;
		try {
			if (getBiasgen() == null) {
				setBiasgen(new SampleProb.Biasgen(this));
			}
			else {
				getBiasgen().setHardwareInterface((BiasgenHardwareInterface) hardwareInterface);
			}
		}
		catch (final ClassCastException e) {
			System.err.println(e.getMessage() + ": probably this chip object has a biasgen but the hardware interface doesn't, ignoring");
		}
	}

	public class Biasgen extends net.sf.jaer.biasgen.Biasgen implements net.sf.jaer.biasgen.ChipControlPanel {
		// All preferences, excluding biases.
		private final List<AbstractConfigValue> allPreferencesList = new ArrayList<>();

		// Preferences by category.
		final List<SPIConfigValue> aerControl = new ArrayList<>();
		final List<SPIConfigValue> chipControl = new ArrayList<>();

		/**
		 * Three DACs, 16 channels. Internal 2.5V reference is used, so VOUT in range 0-5.0V. VDD is 3.3V.
		 */
		private final DAC dac1 = new DAC(16, 14, 0, 5.0f, 3.3f);
		private final DAC dac2 = new DAC(16, 14, 0, 5.0f, 3.3f);
		private final DAC dac3 = new DAC(16, 14, 0, 5.0f, 3.3f);

		final SPIConfigBit dacRun;

		// All bias types.
		final SPIConfigBit biasForceEnable;
		final IPotArray ipots = new IPotArray(this);
		final PotArray vpots = new PotArray(this);

		public Biasgen(final Chip chip) {
			super(chip);
			setName("SampleProb.Biasgen");

			// Use shift-register number as address.
			ipots.addPot(new IPot(this, "Bias0", 5, IPot.Type.NORMAL, IPot.Sex.N, 0, 0, "Bias0"));
			ipots.addPot(new IPot(this, "Bias1", 6, IPot.Type.NORMAL, IPot.Sex.N, 0, 1, "Bias1"));
			ipots.addPot(new IPot(this, "Bias2", 7, IPot.Type.NORMAL, IPot.Sex.N, 0, 2, "Bias2"));
			ipots.addPot(new IPot(this, "Bias3", 8, IPot.Type.NORMAL, IPot.Sex.N, 0, 3, "Bias3"));
			ipots.addPot(new IPot(this, "Bias4", 9, IPot.Type.NORMAL, IPot.Sex.N, 0, 4, "Bias4"));
			ipots.addPot(new IPot(this, "Bias5", 10, IPot.Type.NORMAL, IPot.Sex.N, 0, 5, "Bias5"));
			ipots.addPot(new IPot(this, "Bias6", 11, IPot.Type.NORMAL, IPot.Sex.N, 0, 6, "Bias6"));
			ipots.addPot(new IPot(this, "Bias7", 12, IPot.Type.NORMAL, IPot.Sex.N, 0, 7, "Bias7"));
			ipots.addPot(new IPot(this, "Bias8", 13, IPot.Type.NORMAL, IPot.Sex.N, 0, 8, "Bias8"));
			ipots.addPot(new IPot(this, "Bias9", 14, IPot.Type.NORMAL, IPot.Sex.N, 0, 9, "Bias9"));
			ipots.addPot(new IPot(this, "Bias10", 15, IPot.Type.NORMAL, IPot.Sex.N, 0, 10, "Bias10"));
			ipots.addPot(new IPot(this, "Bias11", 16, IPot.Type.NORMAL, IPot.Sex.N, 0, 11, "Bias11"));
			ipots.addPot(new IPot(this, "Bias12", 17, IPot.Type.NORMAL, IPot.Sex.N, 0, 12, "Bias12"));
			ipots.addPot(new IPot(this, "Bias13", 18, IPot.Type.NORMAL, IPot.Sex.N, 0, 13, "Bias13"));
			ipots.addPot(new IPot(this, "Bias14", 19, IPot.Type.NORMAL, IPot.Sex.N, 0, 14, "Bias14"));
			ipots.addPot(new IPot(this, "Bias15", 20, IPot.Type.NORMAL, IPot.Sex.N, 0, 15, "Bias15"));
			ipots.addPot(new IPot(this, "Bias16", 21, IPot.Type.NORMAL, IPot.Sex.N, 0, 16, "Bias16"));
			ipots.addPot(new IPot(this, "Bias17", 22, IPot.Type.NORMAL, IPot.Sex.N, 0, 17, "Bias17"));
			ipots.addPot(new IPot(this, "Bias18", 23, IPot.Type.NORMAL, IPot.Sex.N, 0, 18, "Bias18"));
			ipots.addPot(new IPot(this, "Bias19", 24, IPot.Type.NORMAL, IPot.Sex.N, 0, 19, "Bias19"));
			ipots.addPot(new IPot(this, "Bias20", 25, IPot.Type.NORMAL, IPot.Sex.N, 0, 20, "Bias20"));
			ipots.addPot(new IPot(this, "Bias21", 26, IPot.Type.NORMAL, IPot.Sex.N, 0, 21, "Bias21"));
			ipots.addPot(new IPot(this, "Bias22", 27, IPot.Type.NORMAL, IPot.Sex.N, 0, 22, "Bias22"));

			setPotArray(ipots);

			// DAC1 channels (16)
			vpots.addPot(new VPot(getChip(), "VHazardrefD00", dac1, 0, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD01", dac1, 1, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD02", dac1, 2, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD03", dac1, 3, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD04", dac1, 4, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD05", dac1, 5, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD06", dac1, 6, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD07", dac1, 7, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD08", dac1, 8, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD09", dac1, 9, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD10", dac1, 10, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD11", dac1, 11, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD12", dac1, 12, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD13", dac1, 13, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VHazardrefD14", dac1, 14, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "Vsrc1Bns", dac1, 15, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));

			// DAC2 channels (16)
			vpots.addPot(new VPot(getChip(), "VnoiseExt04", dac2, 0, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt00", dac2, 1, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt01", dac2, 2, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt02", dac2, 3, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt03", dac2, 4, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt05", dac2, 5, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt06", dac2, 6, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt08", dac2, 7, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt12", dac2, 8, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt07", dac2, 9, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt10", dac2, 10, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt11", dac2, 11, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt13", dac2, 12, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt14", dac2, 13, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt15", dac2, 14, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VnoiseExt09", dac2, 15, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));

			// DAC3 channels (16)
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 0, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 1, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 2, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 3, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 4, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VRVGrefh", dac3, 5, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VRVGrefl", dac3, 6, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			vpots.addPot(new VPot(getChip(), "VRVGrefm", dac3, 7, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 8, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 9, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 10, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 11, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 12, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 13, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 14, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));
			// vpots.addPot(new VPot(getChip(), "NC", dac3, 15, Pot.Type.NORMAL, Pot.Sex.N, 0, 0, ""));

			// New logic SPI configuration values.
			// DAC control
			dacRun = new SPIConfigBit("DACRun", "Enable external DAC.", CypressFX3.FPGA_DAC, (short) 0, false, getPrefs());
			dacRun.addObserver(this);
			allPreferencesList.add(dacRun);

			// Multiplexer module
			biasForceEnable = new SPIConfigBit("ForceBiasEnable", "Force the biases to be always ON.", CypressFX3.FPGA_MUX, (short) 3,
				false, getPrefs());
			biasForceEnable.addObserver(this);
			allPreferencesList.add(biasForceEnable);

			// Generic AER from chip
			aerControl
				.add(new SPIConfigBit("AERRun", "Run the main AER state machine.", CypressFX3.FPGA_DVS, (short) 3, false, getPrefs()));
			aerControl.add(
				new SPIConfigInt("AERAckDelay", "Delay AER ACK by this many cycles.", CypressFX3.FPGA_DVS, (short) 4, 6, 0, getPrefs()));
			aerControl.add(new SPIConfigInt("AERAckExtension", "Extend AER ACK by this many cycles.", CypressFX3.FPGA_DVS, (short) 6, 6, 0,
				getPrefs()));
			aerControl.add(new SPIConfigBit("AERWaitOnTransferStall",
				"Whether the AER state machine should wait,<br> or continue servicing the AER bus when the FIFOs are full.",
				CypressFX3.FPGA_DVS, (short) 8, false, getPrefs()));
			aerControl.add(new SPIConfigBit("AERExternalAERControl",
				"Do not control/ACK the AER bus anymore, <br>but let it be done by an external device.", CypressFX3.FPGA_DVS, (short) 10,
				false, getPrefs()));

			for (final SPIConfigValue cfgVal : aerControl) {
				cfgVal.addObserver(this);
				allPreferencesList.add(cfgVal);
			}

			// Additional chip configuration
			chipControl.add(new SPIConfigInt("MasterBias", "", CypressFX3.FPGA_CHIPBIAS, (short) 0, 8, 0, getPrefs()));
			chipControl.add(new SPIConfigInt("SelSpikeExtend", "", CypressFX3.FPGA_CHIPBIAS, (short) 1, 3, 0, getPrefs()));
			chipControl.add(new SPIConfigInt("SelHazardIV", "", CypressFX3.FPGA_CHIPBIAS, (short) 2, 8, 0, getPrefs()));
			chipControl.add(new SPIConfigBit("SelCH", "", CypressFX3.FPGA_CHIPBIAS, (short) 3, false, getPrefs()));
			chipControl.add(new SPIConfigBit("SelNS", "", CypressFX3.FPGA_CHIPBIAS, (short) 4, false, getPrefs()));
			chipControl.add(new SPIConfigBit("ClockEnable", "Enable clock generation for RNG.", CypressFX3.FPGA_CHIPBIAS, (short) 40, false,
				getPrefs()));
			chipControl.add(new SPIConfigInt("ClockPeriod", "Period of RNG clock in cycles at 120MHz.", CypressFX3.FPGA_CHIPBIAS,
				(short) 41, 20, 0, getPrefs()));

			for (final SPIConfigValue cfgVal : chipControl) {
				cfgVal.addObserver(this);
				allPreferencesList.add(cfgVal);
			}

			setBatchEditOccurring(true);
			loadPreferences();
			setBatchEditOccurring(false);
		}

		@Override
		final public void loadPreferences() {
			super.loadPreferences();

			if (allPreferencesList != null) {
				for (final HasPreference hp : allPreferencesList) {
					hp.loadPreference();
				}
			}

			if (ipots != null) {
				ipots.loadPreferences();
			}

			if (vpots != null) {
				vpots.loadPreferences();
			}
		}

		@Override
		public void storePreferences() {
			for (final HasPreference hp : allPreferencesList) {
				hp.storePreference();
			}

			ipots.storePreferences();

			vpots.storePreferences();

			super.storePreferences();
		}

		@Override
		public JPanel buildControlPanel() {
			final JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			final JComponent c = new SampleProbControlPanel(SampleProb.this);
			c.setPreferredSize(new Dimension(1000, 800));
			panel.add(new JScrollPane(c), BorderLayout.CENTER);
			return panel;
		}

		@Override
		public void setHardwareInterface(final BiasgenHardwareInterface hw) {
			if (hw == null) {
				hardwareInterface = null;
				return;
			}

			hardwareInterface = hw;

			try {
				sendConfiguration();
			}
			catch (final HardwareInterfaceException ex) {
				net.sf.jaer.biasgen.Biasgen.log.warning(ex.toString());
			}
		}

		/**
		 * The central point for communication with HW from biasgen. All objects in Biasgen are Observables
		 * and add Biasgen.this as Observer. They then call notifyObservers when their state changes.
		 *
		 * @param observable
		 *            IPot, DAC, etc
		 * @param object
		 *            notifyChange used at present
		 */
		@Override
		public void update(final Observable observable, final Object object) {
			// while it is sending something
			if (isBatchEditOccurring()) {
				return;
			}

			if (getHardwareInterface() != null) {
				final CypressFX3 fx3HwIntf = (CypressFX3) getHardwareInterface();

				try {
					if (observable instanceof IPot) {
						final IPot iPot = (IPot) observable;

						fx3HwIntf.spiConfigSend(CypressFX3.FPGA_CHIPBIAS, (short) iPot.getShiftRegisterNumber(), iPot.getBitValue());
					}
					else if (observable instanceof VPot) {
						final VPot vPot = (VPot) observable;

						if (vPot.getDac() == dac1) {
							fx3HwIntf.spiConfigSend(CypressFX3.FPGA_DAC, (short) 1, 0); // Select DAC1.
						}
						else if (vPot.getDac() == dac2) {
							fx3HwIntf.spiConfigSend(CypressFX3.FPGA_DAC, (short) 1, 1); // Select DAC2.
						}
						else {
							fx3HwIntf.spiConfigSend(CypressFX3.FPGA_DAC, (short) 1, 2); // Select DAC3.
						}

						fx3HwIntf.spiConfigSend(CypressFX3.FPGA_DAC, (short) 2, 0x03); // Select input data register.
						fx3HwIntf.spiConfigSend(CypressFX3.FPGA_DAC, (short) 3, vPot.getChannel());
						fx3HwIntf.spiConfigSend(CypressFX3.FPGA_DAC, (short) 5, vPot.getBitValue());

						// Toggle SET flag.
						fx3HwIntf.spiConfigSend(CypressFX3.FPGA_DAC, (short) 6, 1);
						fx3HwIntf.spiConfigSend(CypressFX3.FPGA_DAC, (short) 6, 0);

						// Wait 1ms to ensure operation is completed.
						try {
							Thread.sleep(1);
						}
						catch (final InterruptedException e) {
							// Nothing to do here.
						}
					}
					else if (observable instanceof SPIConfigBit) {
						final SPIConfigBit cfgBit = (SPIConfigBit) observable;

						fx3HwIntf.spiConfigSend(cfgBit.getModuleAddr(), cfgBit.getParamAddr(), (cfgBit.isSet()) ? (1) : (0));
					}
					else if (observable instanceof SPIConfigInt) {
						final SPIConfigInt cfgInt = (SPIConfigInt) observable;

						fx3HwIntf.spiConfigSend(cfgInt.getModuleAddr(), cfgInt.getParamAddr(), cfgInt.get());
					}
					else {
						super.update(observable, object); // super (Biasgen) handles others, e.g. masterbias
					}
				}
				catch (final HardwareInterfaceException e) {
					net.sf.jaer.biasgen.Biasgen.log.warning("On update() caught " + e.toString());
				}
			}
		}

		// sends complete configuration information to multiple shift registers and off chip DACs
		public void sendConfiguration() throws HardwareInterfaceException {
			if (!isOpen()) {
				open();
			}

			for (final Pot iPot : ipots.getPots()) {
				update(iPot, null);
			}

			for (final Pot vPot : vpots.getPots()) {
				update(vPot, null);
			}

			for (final AbstractConfigValue spiCfg : allPreferencesList) {
				update(spiCfg, null);
			}
		}
	}

	/**
	 * Extract cochlea events from CochleaAMS1c including the ADC samples that are intermixed with cochlea AER data.
	 * <p>
	 * The event class returned by the extractor is CochleaAMSEvent.
	 */
	public class Extractor extends TypedEventExtractor<CochleaAMSEvent> {

		private static final long serialVersionUID = -3469492271382423090L;

		public Extractor(final AEChip chip) {
			super(chip);
		}

		/**
		 * Extracts the meaning of the raw events. This form is used to supply an output packet. This method is used for
		 * real time
		 * event filtering using a buffer of output events local to data acquisition. An AEPacketRaw may contain
		 * multiple events,
		 * not all of them have to sent out as EventPackets. An AEPacketRaw is a set(!) of addresses and corresponding
		 * timing moments.
		 *
		 * A first filter (independent from the other ones) is implemented by subSamplingEnabled and
		 * getSubsampleThresholdEventCount.
		 * The latter may limit the amount of samples in one package to say 50,000. If there are 160,000 events and
		 * there is a sub samples
		 * threshold of 50,000, a "skip parameter" set to 3. Every so now and then the routine skips with 4, so we end
		 * up with 50,000.
		 * It's an approximation, the amount of events may be less than 50,000. The events are extracted uniform from
		 * the input.
		 *
		 * @param in
		 *            the raw events, can be null
		 * @param out
		 *            the processed events. these are partially processed in-place. empty packet is returned if null is
		 *            supplied as input.
		 */
		@Override
		synchronized public void extractPacket(final AEPacketRaw in, final EventPacket<CochleaAMSEvent> out) {
			out.clear();

			if (in == null) {
				return;
			}

			final int n = in.getNumEvents();

			final int[] addresses = in.getAddresses();
			final int[] timestamps = in.getTimestamps();

			final OutputEventIterator<CochleaAMSEvent> outItr = out.outputIterator();

			for (int i = 0; i < n; i++) {
				final int addr = addresses[i];
				final int ts = timestamps[i];

				final CochleaAMSEvent e = outItr.nextOutput();

				e.address = addr;
				e.timestamp = ts;
				e.x = (short) (addr & 0x0F);
				e.y = 1;
				e.type = 1;
			}
		}
	}
}
