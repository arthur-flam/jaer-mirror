/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.seebetter.ini.chips.davis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeSupport;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import ch.unizh.ini.jaer.chip.retina.DVSTweaks;
import ch.unizh.ini.jaer.config.AbstractConfigValue;
import ch.unizh.ini.jaer.config.spi.SPIConfigBit;
import ch.unizh.ini.jaer.config.spi.SPIConfigInt;
import ch.unizh.ini.jaer.config.spi.SPIConfigValue;
import eu.seebetter.ini.chips.davis.imu.ImuAccelScale;
import eu.seebetter.ini.chips.davis.imu.ImuControl;
import eu.seebetter.ini.chips.davis.imu.ImuControlPanel;
import net.sf.jaer.biasgen.AddressedIPotArray;
import net.sf.jaer.biasgen.Biasgen;
import net.sf.jaer.biasgen.ChipControlPanel;
import net.sf.jaer.biasgen.Pot;
import net.sf.jaer.biasgen.PotTweakerUtilities;
import net.sf.jaer.biasgen.coarsefine.AddressedIPotCF;
import net.sf.jaer.biasgen.coarsefine.ShiftedSourceBiasCF;
import net.sf.jaer.biasgen.coarsefine.ShiftedSourceControlsCF;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.chip.Chip;
import net.sf.jaer.graphics.AEFrameChipRenderer;
import net.sf.jaer.hardwareinterface.HardwareInterfaceException;
import net.sf.jaer.hardwareinterface.usb.cypressfx3libusb.CypressFX3;
import net.sf.jaer.util.HasPropertyTooltips;
import net.sf.jaer.util.ParameterControlPanel;
import net.sf.jaer.util.PropertyTooltipSupport;

/**
 * Base class for configuration of Davis cameras
 *
 * @author tobi
 */
public class DavisConfig extends Biasgen implements DavisDisplayConfigInterface, DavisTweaks, ChipControlPanel {
	// All preferences, excluding biases.
	private final List<AbstractConfigValue> allPreferencesList = new ArrayList<>();

	private final Map<SPIConfigValue, JComponent> configValueMap = new HashMap<>();

	// Preferences by category.
	final List<SPIConfigValue> muxControl = new ArrayList<>();
	final List<SPIConfigValue> dvsControl = new ArrayList<>();
	final List<SPIConfigValue> apsControl = new ArrayList<>();
	final List<SPIConfigValue> imuControl = new ArrayList<>();
	final List<SPIConfigValue> extInControl = new ArrayList<>();
	final List<SPIConfigValue> chipControl = new ArrayList<>();

	// All bias types.
	final AddressedIPotArray ipots = new AddressedIPotArray(this);
	final ShiftedSourceBiasCF[] ssBiases = new ShiftedSourceBiasCF[2];
	final ShiftedSourceBiasCF ssp, ssn;

	// these pots for DVSTweaks
	protected AddressedIPotCF diffOn, diffOff, refr, pr, sf, diff;
	protected SPIConfigBit globalShutter, apsRun, dvsRun;
	protected SPIConfigInt apsExposure, apsFrameDelay;

	// subclasses for controlling aspects of camera
	protected DavisConfig.VideoControl videoControl;
	protected ImuControl imuControlGUI;

	public DavisConfig(final Chip chip) {
		super(chip);
		setName("DavisConfig");

		// masterbias
		getMasterbias().setKPrimeNFet(55e-3f); // estimated from tox=42A, mu_n=670 cm^2/Vs
		getMasterbias().setMultiplier(4); // =45 correct for dvs320
		getMasterbias().setWOverL(4.8f / 2.4f); // masterbias has nfet with w/l=2 at output
		getMasterbias().addObserver(this); // changes to masterbias come back to update() here

		diff = addAIPot(ipots, this, "DiffBn,0,n,normal,differencing amp");
		diffOn = addAIPot(ipots, this, "OnBn,1,n,normal,DVS brighter threshold");
		diffOff = addAIPot(ipots, this, "OffBn,2,n,normal,DVS darker threshold");
		addAIPot(ipots, this, "ApsCasEpc,3,p,cascode,cascode between APS und DVS");
		addAIPot(ipots, this, "DiffCasBnc,4,n,cascode,differentiator cascode bias");
		addAIPot(ipots, this, "ApsROSFBn,5,n,normal,APS readout source follower bias");
		addAIPot(ipots, this, "LocalBufBn,6,n,normal,Local buffer bias");
		addAIPot(ipots, this, "PixInvBn,7,n,normal,Pixel request inversion static inverter bias");
		pr = addAIPot(ipots, this, "PrBp,8,p,normal,Photoreceptor bias current");
		sf = addAIPot(ipots, this, "PrSFBp,9,p,normal,Photoreceptor follower bias current (when used in pixel type)");
		refr = addAIPot(ipots, this, "RefrBp,10,p,normal,DVS refractory period current");
		addAIPot(ipots, this, "AEPdBn,11,n,normal,Request encoder pulldown static current");
		addAIPot(ipots, this, "LcolTimeoutBn,12,n,normal,No column request timeout");
		addAIPot(ipots, this, "AEPuXBp,13,p,normal,AER column pullup");
		addAIPot(ipots, this, "AEPuYBp,14,p,normal,AER row pullup");
		addAIPot(ipots, this, "IFThrBn,15,n,normal,Integrate and fire intensity neuron threshold");
		addAIPot(ipots, this, "IFRefrBn,16,n,normal,Integrate and fire intensity neuron refractory period bias current");
		addAIPot(ipots, this, "PadFollBn,17,n,normal,Follower-pad buffer bias current");
		addAIPot(ipots, this, "apsOverflowLevel,18,n,normal,special overflow level bias ");
		addAIPot(ipots, this, "biasBuffer,19,n,normal,special buffer bias ");

		setPotArray(ipots);

		// shifted sources
		ssp = new ShiftedSourceBiasCF(this);
		ssp.setSex(Pot.Sex.P);
		ssp.setName("SSP");
		ssp.setTooltipString("p-type shifted source that generates a regulated voltage near Vdd");
		ssp.setAddress(20);
		ssp.addObserver(this);

		ssn = new ShiftedSourceBiasCF(this);
		ssn.setSex(Pot.Sex.N);
		ssn.setName("SSN");
		ssn.setTooltipString("n-type shifted source that generates a regulated voltage near ground");
		ssn.setAddress(21);
		ssn.addObserver(this);

		ssBiases[0] = ssp;
		ssBiases[1] = ssn;

		// Multiplexer module
		muxControl.add(new SPIConfigBit("ForceChipBiasEnable", "Force the chip's bias generator to be always ON.", CypressFX3.FPGA_MUX,
			(short) 3, false, chip.getPrefs()));
		muxControl.add(new SPIConfigBit("DropDVSOnTransferStall", "Drop DVS events when USB FIFO is full.", CypressFX3.FPGA_MUX, (short) 4,
			true, chip.getPrefs()));
		muxControl.add(new SPIConfigBit("DropAPSOnTransferStall", "Drop APS events when USB FIFO is full.", CypressFX3.FPGA_MUX, (short) 5,
			false, chip.getPrefs()));
		muxControl.add(new SPIConfigBit("DropIMUOnTransferStall", "Drop IMU events when USB FIFO is full.", CypressFX3.FPGA_MUX, (short) 6,
			false, chip.getPrefs()));
		muxControl.add(new SPIConfigBit("DropExtInputOnTransferStall", "Drop External Input events when USB FIFO is full.",
			CypressFX3.FPGA_MUX, (short) 7, true, chip.getPrefs()));
		muxControl.add(new SPIConfigInt("EarlyPacketDelay", "Ensure a USB packet is committed at least every N x 125µs timesteps.",
			CypressFX3.FPGA_USB, (short) 1, 13, 8, chip.getPrefs()));

		for (final SPIConfigValue cfgVal : muxControl) {
			cfgVal.addObserver(this);
			allPreferencesList.add(cfgVal);
		}

		// DVS module
		dvsRun = new SPIConfigBit("Run", "Enable DVS.", CypressFX3.FPGA_DVS, (short) 3, false, chip.getPrefs());
		dvsControl.add(dvsRun);
		dvsControl.add(new SPIConfigInt("AckDelayRow", "Delay Row AER ACK by this many cycles.", CypressFX3.FPGA_DVS, (short) 4, 5, 4,
			chip.getPrefs()));
		dvsControl.add(new SPIConfigInt("AckDelayColumn", "Delay Column AER ACK by this many cycles.", CypressFX3.FPGA_DVS, (short) 5, 5, 0,
			chip.getPrefs()));
		dvsControl.add(new SPIConfigInt("AckExtensionRow", "Extend Row AER ACK by this many cycles.", CypressFX3.FPGA_DVS, (short) 6, 5, 1,
			chip.getPrefs()));
		dvsControl.add(new SPIConfigInt("AckExtensionColumn", "Extend Column AER ACK by this many cycles.", CypressFX3.FPGA_DVS, (short) 7,
			5, 0, chip.getPrefs()));
		dvsControl.add(new SPIConfigBit("WaitOnTransferStall",
			"On event FIFO full, wait to ACK until again empty if true, or just continue ACKing if false.", CypressFX3.FPGA_DVS, (short) 8,
			false, chip.getPrefs()));
		dvsControl.add(new SPIConfigBit("FilterRowOnlyEvents", "Filter out row-only events (y,y,y,...).", CypressFX3.FPGA_DVS, (short) 9,
			true, chip.getPrefs()));
		dvsControl.add(new SPIConfigBit("ExternalAERControl", "Don't drive AER ACK pin from FPGA (also must disable Event Capture).",
			CypressFX3.FPGA_DVS, (short) 10, false, chip.getPrefs()));
		dvsControl.add(new SPIConfigBit("FilterBackgroundActivity", "Filter background events using hardware filter.", CypressFX3.FPGA_DVS,
			(short) 29, false, chip.getPrefs()));
		dvsControl.add(new SPIConfigInt("FilterBackgroundActivityDeltaTime", "Hardware background events filter delta time (in µs).",
			CypressFX3.FPGA_DVS, (short) 30, 16, 20000, chip.getPrefs()));

		for (final SPIConfigValue cfgVal : dvsControl) {
			cfgVal.addObserver(this);
			allPreferencesList.add(cfgVal);
		}

		// APS module
		apsRun = new SPIConfigBit("Run", "Enable APS.", CypressFX3.FPGA_APS, (short) 4, false, chip.getPrefs());
		apsControl.add(apsRun);
		apsControl.add(new SPIConfigBit("ResetRead", "Do the reset read in addition to the signal read.", CypressFX3.FPGA_APS, (short) 5,
			true, chip.getPrefs()));
		apsControl.add(new SPIConfigBit("WaitOnTransferStall",
			"On event FIFO full, pause and wait for free space. This ensures no APS pixels are dropped.", CypressFX3.FPGA_APS, (short) 6,
			true, chip.getPrefs()));
		globalShutter = new SPIConfigBit("GlobalShutter", "Enable global shutter versus rolling shutter.", CypressFX3.FPGA_APS, (short) 8,
			true, chip.getPrefs());
		apsControl.add(globalShutter);
		apsExposure = new SPIConfigInt("Exposure", "Set exposure time (in µs).", CypressFX3.FPGA_APS, (short) 13, 20, 4000,
			chip.getPrefs());
		apsControl.add(apsExposure);
		apsFrameDelay = new SPIConfigInt("FrameDelay", "Set delay time between frames (in µs).", CypressFX3.FPGA_APS, (short) 14, 20, 1000,
			chip.getPrefs());
		apsControl.add(apsFrameDelay);
		apsControl.add(
			new SPIConfigInt("ResetSettle", "Set reset settle time (in cycles).", CypressFX3.FPGA_APS, (short) 15, 7, 10, chip.getPrefs()));
		apsControl.add(new SPIConfigInt("ColumnSettle", "Set column settle time (in cycles).", CypressFX3.FPGA_APS, (short) 16, 7, 30,
			chip.getPrefs()));
		apsControl.add(
			new SPIConfigInt("RowSettle", "Set row settle time (in cycles).", CypressFX3.FPGA_APS, (short) 17, 6, 10, chip.getPrefs()));
		apsControl.add(
			new SPIConfigInt("NullSettle", "Set null settle time (in cycles).", CypressFX3.FPGA_APS, (short) 18, 5, 3, chip.getPrefs()));

		// TODO: new chips only.
		apsControl.add(new SPIConfigBit("UseInternalADC", "Use the on-chip ADC instead of the external TI ADC.", CypressFX3.FPGA_APS,
			(short) 34, true, chip.getPrefs()));
		apsControl
			.add(new SPIConfigBit("SampleEnable", "Enable Sample&Hold circuitry.", CypressFX3.FPGA_APS, (short) 35, true, chip.getPrefs()));
		apsControl.add(
			new SPIConfigInt("SampleSettle", "Sample hold time (in cycles).", CypressFX3.FPGA_APS, (short) 36, 8, 30, chip.getPrefs()));
		apsControl
			.add(new SPIConfigInt("RampReset", "Ramp reset time (in cycles).", CypressFX3.FPGA_APS, (short) 37, 8, 10, chip.getPrefs()));
		apsControl.add(new SPIConfigBit("RampShortReset", "Only go through half the ramp for reset read.", CypressFX3.FPGA_APS, (short) 38,
			false, chip.getPrefs()));

		for (final SPIConfigValue cfgVal : apsControl) {
			cfgVal.addObserver(this);
			allPreferencesList.add(cfgVal);
		}

		// IMU module
		imuControl.add(new SPIConfigBit("Run", "Enable IMU.", CypressFX3.FPGA_IMU, (short) 0, false, chip.getPrefs()));
		imuControl.add(
			new SPIConfigBit("TempStandby", "Disable temperature measurement.", CypressFX3.FPGA_IMU, (short) 1, false, chip.getPrefs()));
		// imuControl.add(new SPIConfigInt("AccelStandby", ".", CypressFX3.FPGA_IMU, (short) 2, 3, 0, chip.getPrefs()));
		// imuControl.add(new SPIConfigInt("GyroStandby", ".", CypressFX3.FPGA_IMU, (short) 3, 3, 0, chip.getPrefs()));
		imuControl.add(new SPIConfigBit("LPCycle", "Low-power cycle.", CypressFX3.FPGA_IMU, (short) 4, false, chip.getPrefs()));
		imuControl.add(new SPIConfigInt("LPWakeup", "Low-power wakeup mode.", CypressFX3.FPGA_IMU, (short) 5, 2, 1, chip.getPrefs()));
		imuControl.add(
			new SPIConfigInt("SampleRateDivider", "Sample-rate divider value.", CypressFX3.FPGA_IMU, (short) 6, 8, 0, chip.getPrefs()));
		imuControl.add(new SPIConfigInt("DigitalLowPassFilter", "Digital low-pass filter configuration.", CypressFX3.FPGA_IMU, (short) 7, 3,
			1, chip.getPrefs()));
		imuControl.add(new SPIConfigInt("AccelFullScale", "Accellerometer scale configuration.", CypressFX3.FPGA_IMU, (short) 8, 2, 1,
			chip.getPrefs()));
		imuControl.add(
			new SPIConfigInt("GyroFullScale", "Gyroscope scale configuration.", CypressFX3.FPGA_IMU, (short) 9, 2, 1, chip.getPrefs()));

		for (final SPIConfigValue cfgVal : imuControl) {
			cfgVal.addObserver(this);
			allPreferencesList.add(cfgVal);
		}

		// External Input module
		extInControl
			.add(new SPIConfigBit("RunDetector", "Enable signal detector.", CypressFX3.FPGA_EXTINPUT, (short) 0, false, chip.getPrefs()));
		extInControl.add(new SPIConfigBit("DetectRisingEdges", "Emit special event if a rising edge is detected.", CypressFX3.FPGA_EXTINPUT,
			(short) 1, false, chip.getPrefs()));
		extInControl.add(new SPIConfigBit("DetectFallingEdges", "Emit special event if a falling edge is detected.",
			CypressFX3.FPGA_EXTINPUT, (short) 2, false, chip.getPrefs()));
		extInControl.add(new SPIConfigBit("DetectPulses", "Emit special event if a pulse is detected.", CypressFX3.FPGA_EXTINPUT, (short) 3,
			true, chip.getPrefs()));
		extInControl.add(new SPIConfigBit("DetectPulsePolarity", "Polarity of the pulse to be detected.", CypressFX3.FPGA_EXTINPUT,
			(short) 4, true, chip.getPrefs()));
		extInControl.add(new SPIConfigInt("DetectPulseLength", "Minimal length of the pulse to be detected.", CypressFX3.FPGA_EXTINPUT,
			(short) 5, 27, 120, chip.getPrefs()));

		// TODO: new boards only.
		extInControl.add(new SPIConfigBit("RunGenerator", "Enable signal generator (PWM-like).", CypressFX3.FPGA_EXTINPUT, (short) 7, false,
			chip.getPrefs()));
		extInControl
			.add(new SPIConfigBit("GenerateUseCustomSignal", "Use custom FPGA-internal signal, instead of PWM-like generator output.",
				CypressFX3.FPGA_EXTINPUT, (short) 8, false, chip.getPrefs()));
		extInControl.add(new SPIConfigBit("GeneratePulsePolarity", "Polarity of the generated pulse.", CypressFX3.FPGA_EXTINPUT, (short) 9,
			false, chip.getPrefs()));
		extInControl.add(new SPIConfigInt("GeneratePulseInterval", "Time interval between consecutive pulses.", CypressFX3.FPGA_EXTINPUT,
			(short) 10, 27, 120, chip.getPrefs()));
		extInControl.add(new SPIConfigInt("GeneratePulseLength", "Time length of a pulse.", CypressFX3.FPGA_EXTINPUT, (short) 11, 27, 60,
			chip.getPrefs()));

		for (final SPIConfigValue cfgVal : extInControl) {
			cfgVal.addObserver(this);
			allPreferencesList.add(cfgVal);
		}

		// Chip diagnostic chain
		chipControl.add(new SPIConfigBit("DigitalMux0", "Digital multiplexer 0 (debug).", CypressFX3.FPGA_CHIPBIAS, (short) 128, true,
			chip.getPrefs()));
		chipControl.add(new SPIConfigBit("DigitalMux1", "Digital multiplexer 1 (debug).", CypressFX3.FPGA_CHIPBIAS, (short) 129, true,
			chip.getPrefs()));
		chipControl.add(new SPIConfigBit("DigitalMux2", "Digital multiplexer 2 (debug).", CypressFX3.FPGA_CHIPBIAS, (short) 130, true,
			chip.getPrefs()));
		chipControl.add(new SPIConfigBit("DigitalMux3", "Digital multiplexer 3 (debug).", CypressFX3.FPGA_CHIPBIAS, (short) 131, true,
			chip.getPrefs()));
		chipControl.add(
			new SPIConfigBit("AnalogMux0", "Analog multiplexer 0 (debug).", CypressFX3.FPGA_CHIPBIAS, (short) 132, true, chip.getPrefs()));
		chipControl.add(
			new SPIConfigBit("AnalogMux1", "Analog multiplexer 1 (debug).", CypressFX3.FPGA_CHIPBIAS, (short) 133, true, chip.getPrefs()));
		chipControl.add(
			new SPIConfigBit("AnalogMux2", "Analog multiplexer 2 (debug).", CypressFX3.FPGA_CHIPBIAS, (short) 134, true, chip.getPrefs()));
		chipControl
			.add(new SPIConfigBit("BiasMux0", "Bias multiplexer 0 (debug).", CypressFX3.FPGA_CHIPBIAS, (short) 135, true, chip.getPrefs()));

		chipControl.add(new SPIConfigBit("ResetCalibNeuron", "Turn off the integrate and fire calibration neuron (bias generator).",
			CypressFX3.FPGA_CHIPBIAS, (short) 136, true, chip.getPrefs()));
		chipControl.add(new SPIConfigBit("TypeNCalibNeuron",
			"Make the integrate and fire calibration neuron configured to measure N type biases; otherwise measures P-type currents.",
			CypressFX3.FPGA_CHIPBIAS, (short) 137, true, chip.getPrefs()));
		chipControl.add(new SPIConfigBit("ResetTestPixel", "Keep the text pixel in reset.", CypressFX3.FPGA_CHIPBIAS, (short) 138, true,
			chip.getPrefs()));
		chipControl.add(new SPIConfigBit("AERnArow", "Use nArow in the AER state machine.", CypressFX3.FPGA_CHIPBIAS, (short) 140, true,
			chip.getPrefs()));
		chipControl.add(new SPIConfigBit("UseAOut", "Turn the pads for the analog MUX outputs on.", CypressFX3.FPGA_CHIPBIAS, (short) 141,
			true, chip.getPrefs()));

		for (final SPIConfigValue cfgVal : chipControl) {
			cfgVal.addObserver(this);
			allPreferencesList.add(cfgVal);
		}

		// graphicOptions
		videoControl = new VideoControl();
		videoControl.addObserver(this);

		// imuControl
		imuControlGUI = new ImuControl(this);

		setBatchEditOccurring(true);
		loadPreferences();
		setBatchEditOccurring(false);

		try {
			sendConfiguration(this);
		}
		catch (final HardwareInterfaceException ex) {
			log.log(Level.SEVERE, null, ex);
		}
	}

	// New SeeBetterLogic.
	public static final byte VR_CHIP_BIAS = (byte) 0xC0;
	public static final byte VR_CHIP_DIAG = (byte) 0xC1;

	public static final String PROPERTY_EXPOSURE_DELAY_US = "PROPERTY_EXPOSURE_DELAY_US";
	public static final String PROPERTY_FRAME_DELAY_US = "PROPERTY_FRAME_DELAY_US";

	private ParameterControlPanel videoParameterControlPanel;
	private JPanel userFriendlyControls;
	private JPanel configPanel;
	private JTabbedPane configTabbedPane;

	// threshold for triggering a new frame snapshot automatically
	private int autoShotThreshold;

	// DVSTweasks from DVS128
	private float bandwidth = 1;
	private float maxFiringRate = 1;
	private float onOffBalance = 1;
	private float threshold = 1;

	/**
	 *
	 * Overrides the default to built the custom control panel for configuring
	 * the output multiplexers and many other chip, board and display controls.
	 *
	 * @return a new panel for controlling this chip and board configuration
	 */
	@Override
	public JPanel buildControlPanel() {
		setBatchEditOccurring(true); // stop updates on building panel

		configPanel = new JPanel();
		configPanel.setLayout(new BorderLayout());

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.add(configPanel);

		configTabbedPane = new JTabbedPane();
		userFriendlyControls = new DavisUserControlPanel(getChip());
		configTabbedPane.addTab("<html><strong><font color=\"red\">User-Friendly Controls", userFriendlyControls);

		// biasgen
		final JPanel combinedBiasShiftedSourcePanel = new JPanel();
		combinedBiasShiftedSourcePanel
			.add(new JLabel("<html>Low-level control of on-chip bias currents and voltages. <p>These are only for experts!"));
		combinedBiasShiftedSourcePanel.setLayout(new BoxLayout(combinedBiasShiftedSourcePanel, BoxLayout.Y_AXIS));
		combinedBiasShiftedSourcePanel.add(super.buildControlPanel());
		combinedBiasShiftedSourcePanel.add(new ShiftedSourceControlsCF(ssn));
		combinedBiasShiftedSourcePanel.add(new ShiftedSourceControlsCF(ssp));
		configTabbedPane.addTab("Bias Current Config", combinedBiasShiftedSourcePanel);

		// Multiplexer
		final JPanel muxPanel = new JPanel();
		muxPanel.setLayout(new BoxLayout(muxPanel, BoxLayout.Y_AXIS));
		configTabbedPane.addTab("Multiplexer Config", muxPanel);

		for (final SPIConfigValue cfgVal : muxControl) {
			if (cfgVal instanceof SPIConfigBit) {
				makeSPIBitConfig((SPIConfigBit) cfgVal, muxPanel);
			}
			else if (cfgVal instanceof SPIConfigInt) {
				makeSPIIntConfig((SPIConfigInt) cfgVal, muxPanel);
			}
		}

		// DVS
		final JPanel dvsPanel = new JPanel();
		dvsPanel.setLayout(new BoxLayout(dvsPanel, BoxLayout.Y_AXIS));
		configTabbedPane.addTab("DVS Config", dvsPanel);

		for (final SPIConfigValue cfgVal : dvsControl) {
			if (cfgVal instanceof SPIConfigBit) {
				makeSPIBitConfig((SPIConfigBit) cfgVal, dvsPanel);
			}
			else if (cfgVal instanceof SPIConfigInt) {
				makeSPIIntConfig((SPIConfigInt) cfgVal, dvsPanel);
			}
		}

		// APS
		final JPanel apsPanel = new JPanel();
		apsPanel.setLayout(new BoxLayout(apsPanel, BoxLayout.Y_AXIS));
		configTabbedPane.addTab("APS Config", apsPanel);

		for (final SPIConfigValue cfgVal : apsControl) {
			if (cfgVal instanceof SPIConfigBit) {
				makeSPIBitConfig((SPIConfigBit) cfgVal, apsPanel);
			}
			else if (cfgVal instanceof SPIConfigInt) {
				makeSPIIntConfig((SPIConfigInt) cfgVal, apsPanel);
			}
		}

		// IMU
		final JPanel imuControlPanel = new JPanel();
		imuControlPanel.add(new JLabel("<html>Low-level control of integrated inertial measurement unit."));
		imuControlPanel.setLayout(new BoxLayout(imuControlPanel, BoxLayout.Y_AXIS));
		imuControlPanel.add(new ImuControlPanel(this));
		configTabbedPane.addTab("IMU Config", imuControlPanel);

		// External Input
		final JPanel extPanel = new JPanel();
		extPanel.setLayout(new BoxLayout(extPanel, BoxLayout.Y_AXIS));
		configTabbedPane.addTab("External Input Config", extPanel);

		for (final SPIConfigValue cfgVal : extInControl) {
			if (cfgVal instanceof SPIConfigBit) {
				makeSPIBitConfig((SPIConfigBit) cfgVal, extPanel);
			}
			else if (cfgVal instanceof SPIConfigInt) {
				makeSPIIntConfig((SPIConfigInt) cfgVal, extPanel);
			}
		}

		// Chip config
		final JPanel chipPanel = new JPanel();
		chipPanel.setLayout(new BoxLayout(chipPanel, BoxLayout.Y_AXIS));
		configTabbedPane.addTab("Chip Config", chipPanel);

		for (final SPIConfigValue cfgVal : chipControl) {
			if (cfgVal instanceof SPIConfigBit) {
				makeSPIBitConfig((SPIConfigBit) cfgVal, chipPanel);
			}
			else if (cfgVal instanceof SPIConfigInt) {
				makeSPIIntConfig((SPIConfigInt) cfgVal, chipPanel);
			}
		}

		// Autoexposure
		if (getChip() instanceof DavisBaseCamera) {
			final JPanel autoExposurePanel = new JPanel();
			autoExposurePanel.add(new JLabel(
				"<html>Automatic exposure control.<p>The settings here determine when and by how much the exposure value should be changed. <p> The strategy followed attempts to avoid a sitation <b> where too many pixels are under- or over-exposed. Hover over entry fields to see explanations."));
			autoExposurePanel.setLayout(new BoxLayout(autoExposurePanel, BoxLayout.Y_AXIS));
			autoExposurePanel.add(new ParameterControlPanel(((DavisBaseCamera) getChip()).getAutoExposureController()));
			configTabbedPane.addTab("APS Autoexposure Control", autoExposurePanel);
		}

		// Video Control
		final JPanel videoControlPanel = new JPanel();
		videoControlPanel.add(new JLabel("<html>Controls display of APS video frame data"));
		videoControlPanel.setLayout(new BoxLayout(videoControlPanel, BoxLayout.Y_AXIS));
		videoParameterControlPanel = new ParameterControlPanel(getVideoControl());
		videoControlPanel.add(videoParameterControlPanel);
		configTabbedPane.addTab("Video Control", videoControlPanel);

		getVideoControl().addObserver(videoParameterControlPanel);
		getVideoControl().getContrastContoller().addObserver(videoParameterControlPanel);

		// only select panel after all added
		try {
			configTabbedPane.setSelectedIndex(getChip().getPrefs().getInt("DavisBaseCamera.bgTabbedPaneSelectedIndex", 0));
		}
		catch (final IndexOutOfBoundsException e) {
			configTabbedPane.setSelectedIndex(0);
		}

		// add listener to store last selected tab
		configTabbedPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent evt) {
				tabbedPaneMouseClicked(evt);
			}
		});

		setBatchEditOccurring(false);
		return configPanel;
	}

	@Override
	public int getAutoShotEventThreshold() {
		return autoShotThreshold;
	}

	@Override
	public float getBandwidthTweak() {
		return bandwidth;
	}

	@Override
	public float getMaxFiringRateTweak() {
		return maxFiringRate;
	}

	@Override
	public float getOnOffBalanceTweak() {
		return onOffBalance;
	}

	@Override
	public float getThresholdTweak() {
		return threshold;
	}

	@Override
	public float getBrightness() {
		return getVideoControl().getBrightness();
	}

	@Override
	public float getContrast() {
		return getVideoControl().getContrast();
	}

	@Override
	public float getGamma() {
		return getVideoControl().getGamma();
	}

	@Override
	public boolean isCaptureEventsEnabled() {
		return dvsRun.isSet();
	}

	@Override
	public boolean isCaptureFramesEnabled() {
		return apsRun.isSet();
	}

	@Override
	public boolean isDisplayEvents() {
		return getVideoControl().isDisplayEvents();
	}

	@Override
	public boolean isDisplayFrames() {
		return getVideoControl().isDisplayFrames();
	}

	@Override
	public boolean isDisplayImu() {
		return getImuControl().isDisplayImu();
	}

	@Override
	public boolean isImuEnabled() {
		return getImuControl().isImuEnabled();
	}

	@Override
	public boolean isUseAutoContrast() {
		return getVideoControl().isUseAutoContrast();
	}

	@Override
	public void loadPreferences() {
		super.loadPreferences();

		if (allPreferencesList != null) {
			for (final HasPreference hp : allPreferencesList) {
				hp.loadPreference();
			}
		}

		if (ssBiases != null) {
			for (final ShiftedSourceBiasCF sSrc : ssBiases) {
				sSrc.loadPreferences();
			}
		}

		if (ipots != null) {
			ipots.loadPreferences();
		}
	}

	@Override
	public void setAutoShotEventThreshold(final int threshold) {
		autoShotThreshold = threshold;
	}

	/**
	 * Tweaks bandwidth around nominal value.
	 *
	 * @param val
	 *            -1 to 1 range
	 */
	@Override
	public void setBandwidthTweak(float val) {
		if (val > 1) {
			val = 1;
		}
		else if (val < -1) {
			val = -1;
		}
		final float old = bandwidth;
		if (old == val) {
			return;
		}
		// log.info("tweak bandwidth by " + val);
		bandwidth = val;
		final float MAX = 30;
		pr.changeByRatioFromPreferred(PotTweakerUtilities.getRatioTweak(val, MAX));
		sf.changeByRatioFromPreferred(PotTweakerUtilities.getRatioTweak(val, MAX));
		getChip().getSupport().firePropertyChange(DVSTweaks.BANDWIDTH, old, val);
	}

	@Override
	public void setBrightness(final float brightness) {
		getVideoControl().setBrightness(brightness);
	}

	@Override
	public void setCaptureEvents(final boolean selected) {
		dvsRun.set(selected);
	}

	@Override
	public void setCaptureFramesEnabled(final boolean yes) {
		apsRun.set(yes);
	}

	@Override
	public void setContrast(final float contrast) {
		getVideoControl().setContrast(contrast);
	}

	@Override
	public void setDisplayEvents(final boolean displayEvents) {
		getVideoControl().setDisplayEvents(displayEvents);
	}

	@Override
	public void setDisplayFrames(final boolean displayFrames) {
		getVideoControl().setDisplayFrames(displayFrames);
	}

	@Override
	public void setDisplayImu(final boolean yes) {
		getImuControl().setDisplayImu(yes);
	}

	public void setExposureDelayMs(final float ms) {
		final int expUs = (int) (ms * 1000);
		apsExposure.set(expUs);
	}

	public float getExposureDelayMs() {
		return apsExposure.get() * .001f;
	}

	public void setFrameDelayMs(final float ms) {
		final int fdUs = (int) (ms * 1000);
		apsFrameDelay.set(fdUs);
	}

	public float getFrameDelayMs() {
		return apsFrameDelay.get() * .001f;

	}

	@Override
	public void setGamma(final float gamma) {
		getVideoControl().setGamma(gamma);
	}

	@Override
	public void setImuEnabled(final boolean yes) {
		getImuControl().setImuEnabled(yes);
	}

	/**
	 * Tweaks max firing rate (refractory period), larger is shorter refractory
	 * period.
	 *
	 * @param val
	 *            -1 to 1 range
	 */
	@Override
	public void setMaxFiringRateTweak(float val) {
		if (val > 1) {
			val = 1;
		}
		else if (val < -1) {
			val = -1;
		}
		final float old = maxFiringRate;
		if (old == val) {
			return;
		}
		maxFiringRate = val;
		final float MAX = 100;
		refr.changeByRatioFromPreferred(PotTweakerUtilities.getRatioTweak(val, MAX));
		getChip().getSupport().firePropertyChange(DVSTweaks.MAX_FIRING_RATE, old, val);
	}

	/**
	 * Tweaks balance of on/off events. Increase for more ON events.
	 *
	 * @param val
	 *            -1 to 1 range.
	 */
	@Override
	public void setOnOffBalanceTweak(float val) {
		if (val > 1) {
			val = 1;
		}
		else if (val < -1) {
			val = -1;
		}
		final float old = onOffBalance;
		if (old == val) {
			return;
		}
		onOffBalance = val;
		final float MAX = 10;
		diff.changeByRatioFromPreferred(PotTweakerUtilities.getRatioTweak(val, MAX));
		getChip().getSupport().firePropertyChange(DVSTweaks.ON_OFF_BALANCE, old, val);
	}

	/**
	 * Tweaks threshold, larger is higher threshold.
	 *
	 * @param val
	 *            -1 to 1 range
	 */
	@Override
	public void setThresholdTweak(float val) {
		if (val > 1) {
			val = 1;
		}
		else if (val < -1) {
			val = -1;
		}
		final float old = threshold;
		if (old == val) {
			return;
		}
		final float MAX = 10;
		threshold = val;
		diffOn.changeByRatioFromPreferred(PotTweakerUtilities.getRatioTweak(val, MAX));
		diffOff.changeByRatioFromPreferred(1 / PotTweakerUtilities.getRatioTweak(val, MAX));
		getChip().getSupport().firePropertyChange(DVSTweaks.THRESHOLD, old, val);
	}

	@Override
	public void setUseAutoContrast(final boolean useAutoContrast) {
		getVideoControl().setUseAutoContrast(useAutoContrast);
	}

	@Override
	public void storePreferences() {
		for (final HasPreference hp : allPreferencesList) {
			hp.storePreference();
		}

		for (final ShiftedSourceBiasCF sSrc : ssBiases) {
			sSrc.storePreferences();
		}

		ipots.storePreferences();

		super.storePreferences();
	}

	protected void tabbedPaneMouseClicked(final MouseEvent evt) {
		getChip().getPrefs().putInt("DavisBaseCamera.bgTabbedPaneSelectedIndex", configTabbedPane.getSelectedIndex());
	}

	/**
	 * The central point for communication with HW from biasgen. All objects in
	 * SeeBetterConfig are Observables and addConfigValue SeeBetterConfig.this
	 * as Observer. They then call notifyObservers when their state changes.
	 * Objects such as ADC store preferences for ADC, and update should update
	 * the hardware registers accordingly.
	 *
	 * @param observable
	 *            IPot, Scanner, etc
	 * @param object
	 *            notifyChange - not used at present
	 */
	@Override
	public synchronized void update(final Observable observable, final Object object) {
		// while it is sending something
		if (isBatchEditOccurring()) {
			return;
		}

		if (getHardwareInterface() != null) {
			final CypressFX3 fx3HwIntf = (CypressFX3) getHardwareInterface();

			try {
				if (observable instanceof AddressedIPotCF) {
					final AddressedIPotCF iPot = (AddressedIPotCF) observable;

					fx3HwIntf.spiConfigSend(CypressFX3.FPGA_CHIPBIAS, (short) iPot.getAddress(), iPot.computeCleanBinaryRepresentation());
				}
				else if (observable instanceof ShiftedSourceBiasCF) {
					final ShiftedSourceBiasCF iPot = (ShiftedSourceBiasCF) observable;

					fx3HwIntf.spiConfigSend(CypressFX3.FPGA_CHIPBIAS, (short) iPot.getAddress(), iPot.computeBinaryRepresentation());
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

		for (final ShiftedSourceBiasCF sSrc : ssBiases) {
			update(sSrc, null);
		}

		for (final Pot iPot : ipots.getPots()) {
			update(iPot, null);
		}

		for (final AbstractConfigValue spiCfg : allPreferencesList) {
			update(spiCfg, null);
		}
	}

	public class VideoControl extends Observable implements Observer, HasPreference, HasPropertyTooltips {

		public boolean displayEvents = getChip().getPrefs().getBoolean("VideoControl.displayEvents", true);
		public boolean displayFrames = getChip().getPrefs().getBoolean("VideoControl.displayFrames", true);
		public boolean separateAPSByColor = getChip().getPrefs().getBoolean("VideoControl.separateAPSByColor", false);
		public boolean autoWhiteBalance = getChip().getPrefs().getBoolean("VideoControl.autoWhiteBalance", true);
		public boolean colorCorrection = getChip().getPrefs().getBoolean("VideoControl.colorCorrection", true);
		// on crappy beamer output
		private final PropertyTooltipSupport tooltipSupport = new PropertyTooltipSupport();

		public VideoControl() {
			super();
			tooltipSupport.setPropertyTooltip("displayEvents", "display DVS events");
			tooltipSupport.setPropertyTooltip("displayFrames", "display APS frames");
			tooltipSupport.setPropertyTooltip("useAutoContrast", "automatically set the display contrast for APS frames");
			tooltipSupport.setPropertyTooltip("brightness",
				"sets the brightness for APS frames, which is the lowest level of display intensity. Default is 0.");
			tooltipSupport.setPropertyTooltip("contrast",
				"sets the contrast for APS frames, which multiplies sample values by this quantity. Default is 1.");
			tooltipSupport.setPropertyTooltip("gamma",
				"sets the display gamma for APS frames, which applies a power law to optimize display for e.g. monitors. Default is 1.");
			tooltipSupport.setPropertyTooltip("autoContrastControlTimeConstantMs",
				"Time constant in ms for autocontrast control. This is the lowpasss filter time constant for min and max image values to automatically scale image to 0-1 range.");
		}

		/**
		 * @return the displayFrames
		 */
		public boolean isDisplayFrames() {
			return displayFrames;
		}

		/**
		 * @param displayFrames
		 *            the displayFrames to set
		 */
		public void setDisplayFrames(final boolean displayFrames) {
			final boolean old = this.displayFrames;
			this.displayFrames = displayFrames;
			getChip().getPrefs().putBoolean("VideoControl.displayFrames", displayFrames);
			if (((AEChip) getChip()).getAeViewer() != null) {
				((AEChip) getChip()).getAeViewer().interruptViewloop();
			}
			getSupport().firePropertyChange(DavisDisplayConfigInterface.PROPERTY_DISPLAY_FRAMES_ENABLED, old, displayFrames);
			if (old != displayFrames) {
				setChanged();
				notifyObservers(); // inform ParameterControlPanel
			}
		}

		public boolean isSeparateAPSByColor() {
			return separateAPSByColor;
		}

		public boolean isAutoWhiteBalance() {
			return autoWhiteBalance;
		}

		public boolean isColorCorrection() {
			return colorCorrection;
		}

		/**
		 * @param displayFrames
		 *            the displayFrames to set
		 */
		public void setSeparateAPSByColor(final boolean separateAPSByColor) {
			this.separateAPSByColor = separateAPSByColor;
			getChip().getPrefs().putBoolean("VideoControl.separateAPSByColor", separateAPSByColor);
		}

		public void setAutoWhiteBalance(final boolean autoWhiteBalance) {
			this.autoWhiteBalance = autoWhiteBalance;
			getChip().getPrefs().putBoolean("VideoControl.autoWhiteBalance", autoWhiteBalance);
		}

		public void setColorCorrection(final boolean colorCorrection) {
			this.colorCorrection = colorCorrection;
			getChip().getPrefs().putBoolean("VideoControl.colorCorrection", colorCorrection);
		}

		/**
		 * @return the displayEvents
		 */
		public boolean isDisplayEvents() {
			return displayEvents;
		}

		/**
		 * @param displayEvents
		 *            the displayEvents to set
		 */
		public void setDisplayEvents(final boolean displayEvents) {
			final boolean old = this.displayEvents;
			this.displayEvents = displayEvents;
			getChip().getPrefs().putBoolean("VideoControl.displayEvents", displayEvents);
			if (((AEChip) getChip()).getAeViewer() != null) {
				((AEChip) getChip()).getAeViewer().interruptViewloop();
			}
			getSupport().firePropertyChange(DavisDisplayConfigInterface.PROPERTY_DISPLAY_EVENTS_ENABLED, old, displayEvents);
			if (old != displayEvents) {
				setChanged();
				notifyObservers(); // inform ParameterControlPanel
			}
		}

		public boolean isUseAutoContrast() {
			return getContrastContoller().isUseAutoContrast();
		}

		public void setUseAutoContrast(final boolean useAutoContrast) {
			getContrastContoller().setUseAutoContrast(useAutoContrast);
		}

		public float getContrast() {
			return getContrastContoller().getContrast();
		}

		public void setContrast(final float contrast) {
			getContrastContoller().setContrast(contrast);
		}

		public float getBrightness() {
			return getContrastContoller().getBrightness();
		}

		public void setBrightness(final float brightness) {
			getContrastContoller().setBrightness(brightness);
		}

		public float getGamma() {
			return getContrastContoller().getGamma();
		}

		public void setGamma(final float gamma) {
			getContrastContoller().setGamma(gamma);
		}

		public float getAutoContrastTimeconstantMs() {
			return getContrastContoller().getAutoContrastTimeconstantMs();
		}

		public void setAutoContrastTimeconstantMs(final float tauMs) {
			getContrastContoller().setAutoContrastTimeconstantMs(tauMs);
		}

		@Override
		public void update(final Observable o, final Object arg) {
			setChanged();
			notifyObservers(arg);
			// if (o == ) {
			// propertyChangeSupport.firePropertyChange(EVENT_GRAPHICS_DISPLAY_INTENSITY, null, runAdc.isSet());
			// } // TODO
		}

		/**
		 * @return the propertyChangeSupport
		 */
		public PropertyChangeSupport getPropertyChangeSupport() {
			return getSupport();
		}

		@Override
		public void loadPreference() {
			setDisplayFrames(getChip().getPrefs().getBoolean("VideoControl.displayFrames", true));
			setDisplayEvents(getChip().getPrefs().getBoolean("VideoControl.displayEvents", true));
		}

		@Override
		public void storePreference() {
			getChip().getPrefs().putBoolean("VideoControl.displayEvents", displayEvents);
			getChip().getPrefs().putBoolean("VideoControl.displayFrames", displayFrames);
		}

		@Override
		public String getPropertyTooltip(final String propertyName) {
			return tooltipSupport.getPropertyTooltip(propertyName);
		}

		/**
		 * @return the contrastContoller
		 */
		public DavisVideoContrastController getContrastContoller() {
			if (((AEChip) getChip()).getRenderer() instanceof AEFrameChipRenderer) {
				return ((AEFrameChipRenderer) (((AEChip) getChip()).getRenderer())).getContrastController();
			}

			throw new RuntimeException("Cannot return a video contrast controller for the image output for the current renderer, which is "
				+ ((AEChip) getChip()).getRenderer());
		}
	}

	public static String[] choices() {
		final String[] s = new String[ImuAccelScale.values().length];
		for (int i = 0; i < ImuAccelScale.values().length; i++) {
			s[i] = ImuAccelScale.values()[i].fullScaleString;
		}
		return s;
	}

	/**
	 * @return the videoControl
	 */
	public DavisConfig.VideoControl getVideoControl() {
		return videoControl;
	}

	/**
	 * @return the imuControl
	 */
	public ImuControl getImuControl() {
		return imuControlGUI;
	}

	/**
	 * @return the exposureControlRegister
	 */
	public SPIConfigInt getExposureControlRegister() {
		return apsExposure;
	}

	/**
	 * @return the frameDelayControlRegister
	 */
	public SPIConfigInt getFrameDelayControlRegister() {
		return apsFrameDelay;
	}

	@Override
	public boolean isSeparateAPSByColor() {
		return getVideoControl().isSeparateAPSByColor();
	}

	@Override
	public boolean isAutoWhiteBalance() {
		return getVideoControl().isAutoWhiteBalance();
	}

	@Override
	public boolean isColorCorrection() {
		return getVideoControl().isColorCorrection();
	}

	@Override
	public boolean isGlobalShutter() {
		return globalShutter.isSet();
	}

	@Override
	public void setSeparateAPSByColor(final boolean yes) {
		getVideoControl().setSeparateAPSByColor(yes);
	}

	public void setAutoWhiteBalance(final boolean yes) {
		getVideoControl().setAutoWhiteBalance(yes);
	}

	static final protected AddressedIPotCF addAIPot(final AddressedIPotArray potArray, final Biasgen biasgen, final String s) {
		AddressedIPotCF ret = null;

		try {
			String delim = ",";
			StringTokenizer t = new StringTokenizer(s, delim);

			if (t.countTokens() != 5) {
				throw new Error("only " + t.countTokens() + " tokens in pot " + s
					+ "; use , to separate tokens for name,address,sex,type,tooltip\nsex=n|p, type=normal|cascode");
			}

			String name = t.nextToken();

			String addressT = t.nextToken();
			int address = Integer.parseInt(addressT);

			String sexT = t.nextToken();
			Pot.Sex sex = null;
			if (sexT.equalsIgnoreCase("n")) {
				sex = Pot.Sex.N;
			}
			else if (sexT.equalsIgnoreCase("p")) {
				sex = Pot.Sex.P;
			}
			else {
				throw new ParseException(s, s.lastIndexOf(sexT));
			}

			String typeT = t.nextToken();
			Pot.Type type = null;
			if (typeT.equalsIgnoreCase("normal")) {
				type = Pot.Type.NORMAL;
			}
			else if (typeT.equalsIgnoreCase("cascode")) {
				type = Pot.Type.CASCODE;
			}
			else {
				throw new ParseException(s, s.lastIndexOf(typeT));
			}

			String tooltip = t.nextToken();

			potArray.addPot(new AddressedIPotCF(biasgen, name, address, type, sex, false, true, AddressedIPotCF.maxCoarseBitValue / 2,
				AddressedIPotCF.maxFineBitValue, (address + 1), tooltip));
		}
		catch (Exception e) {
			throw new Error(e.toString());
		}

		return (ret);
	}

	private static final int TF_MAX_HEIGHT = 15;
	private static final int TF_HEIGHT = 6;
	private static final int TF_MIN_W = 15, TF_PREF_W = 20, TF_MAX_W = 40;

	private void makeSPIBitConfig(final SPIConfigBit bitVal, final JPanel panel) {
		final JRadioButton but = new JRadioButton("<html>" + bitVal.getName() + ": " + bitVal.getDescription());
		but.setToolTipText("<html>" + bitVal.toString() + "<br>Select to set bit, clear to clear bit.");
		but.setSelected(bitVal.isSet());
		but.setAlignmentX(Component.LEFT_ALIGNMENT);
		but.addActionListener(new SPIConfigBitAction(bitVal));

		panel.add(but);
		configValueMap.put(bitVal, but);
		bitVal.addObserver(this);
	}

	private void makeSPIIntConfig(final SPIConfigInt intVal, final JPanel panel) {
		final JPanel pan = new JPanel();
		pan.setAlignmentX(Component.LEFT_ALIGNMENT);
		pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));

		final JLabel label = new JLabel(intVal.getName());
		label.setToolTipText("<html>" + intVal.toString() + "<br>" + intVal.getDescription()
			+ "<br>Enter value or use mouse wheel or arrow keys to change value.");
		pan.add(label);

		final JTextField tf = new JTextField();
		tf.setText(Integer.toString(intVal.get()));
		tf.setPreferredSize(new Dimension(TF_PREF_W, TF_HEIGHT));
		tf.setMaximumSize(new Dimension(TF_MAX_W, TF_MAX_HEIGHT));
		tf.addActionListener(new SPIConfigIntAction(intVal));
		pan.add(tf);

		panel.add(pan);
		configValueMap.put(intVal, tf);
		intVal.addObserver(this);
	}

	private class SPIConfigBitAction implements ActionListener {

		private final SPIConfigBit bitConfig;

		SPIConfigBitAction(final SPIConfigBit bitCfg) {
			bitConfig = bitCfg;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final JRadioButton button = (JRadioButton) e.getSource();
			bitConfig.set(button.isSelected());
			setFileModified();
		}
	}

	private class SPIConfigIntAction implements ActionListener {

		private final SPIConfigInt intConfig;

		SPIConfigIntAction(final SPIConfigInt intCfg) {
			intConfig = intCfg;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final JTextField tf = (JTextField) e.getSource();

			try {
				intConfig.set(Integer.parseInt(tf.getText()));
				setFileModified();

				tf.setBackground(Color.white);
			}
			catch (final Exception ex) {
				tf.selectAll();
				tf.setBackground(Color.red);

				log.warning(ex.toString());
			}
		}
	}

	private void setFileModified() {
		if ((((AEChip) getChip()) != null) && (((AEChip) getChip()).getAeViewer() != null)
			&& (((AEChip) getChip()).getAeViewer().getBiasgenFrame() != null)) {
			((AEChip) getChip()).getAeViewer().getBiasgenFrame().setFileModified(true);
		}
	}
}
