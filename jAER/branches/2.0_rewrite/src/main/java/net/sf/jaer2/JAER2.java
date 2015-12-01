package net.sf.jaer2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

import javax.management.modelmbean.XMLParseException;

import org.xml.sax.SAXException;

import ch.unizh.ini.devices.ApsDvs10FX3;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.sf.jaer.jaerfx2.Files;
import net.sf.jaer.jaerfx2.GUISupport;
import net.sf.jaer.jaerfx2.SSHS;

public final class JAER2 extends Application {
	public static final String homeDirectory = System.getProperty("user.home") + File.separator + "jAER2";

	public static void main(final String[] args) {
		// Launch the JavaFX application: do initialization and call start()
		// when ready.
		Application.launch(args);
	}

	@Override
	public void start(final Stage primaryStage) {
		if (!GUISupport.checkJavaVersion(primaryStage)) {
			return;
		}

		final String lastSessionDirectory = JAER2.homeDirectory + File.separator + "lastSession";
		final File savedSession = new File(lastSessionDirectory + File.separator + "net-last.xml");

		if (Files.checkReadPermissions(savedSession)) {
			// Restore configuration from saved file.
			try (FileInputStream fin = new FileInputStream(savedSession)) {
				SSHS.GLOBAL.getNode("/").importSubTreeFromXML(fin, true);
			}
			catch (SAXException | IOException | XMLParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		final ApsDvs10FX3 dvs = new ApsDvs10FX3(null);

		final Scene rootScene = GUISupport.startGUI(primaryStage, dvs.getConfigGUI(), "jAER2 Device Configuration", (event) -> {
			// Try to save the current configuration to file.
			if (Files.checkWritePermissions(savedSession)) {
				try (FileOutputStream fout = new FileOutputStream(savedSession)) {
					savedSession.getParentFile().mkdirs();
					SSHS.GLOBAL.getNode("/").exportSubTreeToXML(fout, Collections.emptyList());
				}
				catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		// Add default CSS style-sheet.
		rootScene.getStylesheets().add("/styles/root.css");
	}
}
