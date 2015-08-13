/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.seebetter.ini.chips.davis;

import java.awt.Point;

import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;

/**
 * Davis346cBSI camera
 *
 * @author tobi
 */
@Description("DAVIS346 346x260 pixel APS-DVS DAVIS sensor (BSI)")
@DevelopmentStatus(DevelopmentStatus.Status.Experimental)
public class Davis346cBSI extends Davis346BaseCamera {
	public Davis346cBSI() {
		setName("Davis346cBSI");
		setDefaultPreferencesFile("biasgenSettings/Davis346cBSI/DAVIS346cBSI_Test.xml");

		apsFirstPixelReadOut = new Point(getSizeX() - 1, 0);
		apsLastPixelReadOut = new Point(0, getSizeY() - 1);
	}
}
