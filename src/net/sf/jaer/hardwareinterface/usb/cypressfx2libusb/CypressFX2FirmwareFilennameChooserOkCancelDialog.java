/*
 * CypressFX2FirmwareFilennameChooserOkCancelDialog.java
 *
 * Created on October 20, 2008, 4:03 PM
 */
package net.sf.jaer.hardwareinterface.usb.cypressfx2libusb;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;

import net.sf.jaer.chip.Chip;

/**
 * A dialog for choosing a firmware file.
 *
 * @author tobi
 */
public class CypressFX2FirmwareFilennameChooserOkCancelDialog extends javax.swing.JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 3094975364945203710L;
	/**
	 * The path relative to starting folder of CypressFX2 firmware files
	 */
	public static final String DEFAULT_RELATIVE_FIRMWARE_PATH = "../../devices/firmware/CypressFX2/";
	static Logger log = Logger.getLogger("CypressFX2FirmwareFilennameChooserOkCancelDialog");
	static Preferences prefs = Preferences.userNodeForPackage(CypressFX2FirmwareFilennameChooserOkCancelDialog.class);
	/**
	 * A return status code - returned if Cancel button has been pressed
	 */
	public static final int RET_CANCEL = 0;
	/**
	 * A return status code - returned if OK button has been pressed
	 */
	public static final int RET_OK = 1;

	/**
	 * Creates new form CypressFX2FirmwareFilennameChooserOkCancelDialog.
	 *
	 * @param parent
	 *            enclosing Frame.
	 * @param modal
	 *            true to block other GUI input while open.
	 * @param chip
	 *            from where we get the default firmware, or null if you want
	 *            to use a default location
	 */
	public CypressFX2FirmwareFilennameChooserOkCancelDialog(final java.awt.Frame parent, final boolean modal,
		final Chip chip) {
		super(parent, modal);
		initComponents();
		filenameTextField.setText(chip.getDefaultFirmwareBixFileForBlankDevice());
		final File f = new File(filenameTextField.getText());
		if (!filenameTextField.getText().isEmpty() && (f != null) && !f.exists()) {
			filenameTextField.setForeground(Color.RED);
			filenameTextField.setToolTipText("File or path does not exist or is not readable");
		}
		else {
			filenameTextField.setToolTipText(filenameTextField.getText());
			filenameTextField.setForeground(Color.BLACK);
		}
	}

	/**
	 * @return the return status of this dialog - one of RET_OK or RET_CANCEL
	 */
	public int getReturnStatus() {
		return returnStatus;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		okButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();
		infoLabel = new javax.swing.JLabel();
		filenameTextField = new javax.swing.JTextField();
		chooseButton = new javax.swing.JButton();

		setTitle("CypressFX2 firmware file chooser");
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(final java.awt.event.WindowEvent evt) {
				closeDialog(evt);
			}
		});

		okButton.setText("OK");
		okButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				okButtonActionPerformed(evt);
			}
		});

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		infoLabel
			.setText("<html>Choose the .bix firmware file you wish to download to this device.<p>These files are usually located in either the folder <em>deviceFirmwarePCBLayout</em> or the package <em>net.sf.jaer.hardwareinterface.usb</em><html>");

		filenameTextField.setText("The chosen file");
		filenameTextField.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				filenameTextFieldActionPerformed(evt);
			}
		});

		chooseButton.setText("Choose...");
		chooseButton.setToolTipText("Browses for a firmware file");
		chooseButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				chooseButtonActionPerformed(evt);
			}
		});

		final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
			layout
				.createSequentialGroup()
				.addContainerGap()
				.addGroup(
					layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(infoLabel, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addGroup(
							layout
								.createSequentialGroup()
								.addGroup(
									layout
										.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
										.addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 67,
											javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(filenameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 807,
											Short.MAX_VALUE))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addGroup(
									layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
										.addComponent(chooseButton).addComponent(cancelButton)))).addContainerGap()));

		layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { cancelButton, okButton });

		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
			layout
				.createSequentialGroup()
				.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(infoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
					javax.swing.GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(
					layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(filenameTextField, javax.swing.GroupLayout.PREFERRED_SIZE,
							javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(chooseButton))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(
					layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(cancelButton)
						.addComponent(okButton))));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void okButtonActionPerformed(final java.awt.event.ActionEvent evt) {// GEN-FIRST:event_okButtonActionPerformed
		doClose(CypressFX2FirmwareFilennameChooserOkCancelDialog.RET_OK);
	}// GEN-LAST:event_okButtonActionPerformed

	private void cancelButtonActionPerformed(final java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cancelButtonActionPerformed
		doClose(CypressFX2FirmwareFilennameChooserOkCancelDialog.RET_CANCEL);
	}// GEN-LAST:event_cancelButtonActionPerformed

	/**
	 * Closes the dialog
	 */
	private void closeDialog(final java.awt.event.WindowEvent evt) {// GEN-FIRST:event_closeDialog
		doClose(CypressFX2FirmwareFilennameChooserOkCancelDialog.RET_CANCEL);
	}// GEN-LAST:event_closeDialog

	private void filenameTextFieldActionPerformed(final java.awt.event.ActionEvent evt) {// GEN-FIRST:event_filenameTextFieldActionPerformed
		final File f = new File(filenameTextField.getText());
		if (!f.exists()) {
			filenameTextField.selectAll();
			filenameTextField.setBackground(Color.RED);
		}
		else {
			filenameTextField.setForeground(Color.BLACK);
		}
	}// GEN-LAST:event_filenameTextFieldActionPerformed

	private void chooseButtonActionPerformed(final java.awt.event.ActionEvent evt) {// GEN-FIRST:event_chooseButtonActionPerformed

		String startFolder = filenameTextField.getText();
		if ((startFolder == null) || !(new File(startFolder).exists())) {
			startFolder = System.getProperty("user.dir") + File.separator
				+ CypressFX2FirmwareFilennameChooserOkCancelDialog.DEFAULT_RELATIVE_FIRMWARE_PATH;
		}
		final JFileChooser chooser = new JFileChooser(startFolder);
		chooser.setApproveButtonText("Choose");
		chooser.setFileFilter(new FirmwareFileFilter());
		chooser.setMultiSelectionEnabled(false);
		final int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String relative = chooser.getSelectedFile().getPath();
			try {
				relative = CypressFX2FirmwareFilennameChooserOkCancelDialog.getPathRelativeToStartupFolder(chooser
					.getSelectedFile());
			}
			catch (final IOException ex) {
				Logger.getLogger(CypressFX2FirmwareFilennameChooserOkCancelDialog.class.getName()).log(Level.SEVERE,
					null, ex);
			}
			filenameTextField.setText(relative);
			filenameTextField.setToolTipText(chooser.getSelectedFile().getPath());
		}
	}// GEN-LAST:event_chooseButtonActionPerformed

	private void doClose(final int retStatus) {
		returnStatus = retStatus;
		setVisible(false);
		dispose();
	}

	/**
	 * Computes the path for a file relative to a given base, or fails if the
	 * only shared directory is the root and the absolute form is better.
	 *
	 * @param base
	 *            File that is the base for the result
	 * @param name
	 *            File to be "relativized"
	 * @return the relative name
	 * @throws IOException
	 *             if files have no common sub-directories, i.e. at best
	 *             share the root prefix "/" or "C:\"
	 */
	public static String getRelativePath(final File base, final File name) throws IOException {
		final File parent = base.getParentFile();

		if (parent == null) {
			throw new IOException(base + " does not have parent folder, cannot find relative path to " + name);
		}

		final String bpath = base.getCanonicalPath();
		final String fpath = name.getCanonicalPath();

		if (fpath.startsWith(bpath)) {
			return fpath.substring(bpath.length() + 1);
		}
		else {
			return (".." + File.separator + CypressFX2FirmwareFilennameChooserOkCancelDialog.getRelativePath(parent,
				name));
		}
	}

	public static String getPathRelativeToStartupFolder(final File name) throws IOException {
		final File base = new File(System.getProperty("user.dir"));
		return CypressFX2FirmwareFilennameChooserOkCancelDialog.getRelativePath(base, name);
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(final String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				final CypressFX2FirmwareFilennameChooserOkCancelDialog dialog = new CypressFX2FirmwareFilennameChooserOkCancelDialog(
					new javax.swing.JFrame(), true, new Chip());
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(final java.awt.event.WindowEvent e) {
						System.exit(0);
					}
				});
				dialog.setVisible(true);
				dialog.getReturnStatus();
				System.exit(0);
			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton cancelButton;
	private javax.swing.JButton chooseButton;
	private javax.swing.JTextField filenameTextField;
	private javax.swing.JLabel infoLabel;
	private javax.swing.JButton okButton;
	// End of variables declaration//GEN-END:variables
	private int returnStatus = CypressFX2FirmwareFilennameChooserOkCancelDialog.RET_CANCEL;

	/**
	 * Returns the chosen file path
	 *
	 * @return the full path to the file
	 */
	public String getChosenFile() {
		return filenameTextField.getText();
	}

	private class FirmwareFileFilter extends javax.swing.filechooser.FileFilter {

		@Override
		public boolean accept(final File f) {
			if (f.isDirectory()) {
				return true;
			}
			final String s = f.getName().toLowerCase();
			// if(s.endsWith(".hex")) return true; // we only download binaries uniformly in CypressFX2, not hex files
			// which are handled separately. TODO fix this handling
			// if(s.endsWith(".iic")) return true;
			if (s.endsWith(".bix")) {
				return true;
			}
			return false;
		}

		@Override
		public String getDescription() {
			return "Cypress firmware file (hex, iic or bix)";
		}
	}
}
