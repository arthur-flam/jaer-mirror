package net.sf.jaer2.devices.components.controllers.logic;

import net.sf.jaer.jaerfx2.SSHSNode;


public class LatticeECP3 extends Logic {
	public LatticeECP3(final SSHSNode componentConfigNode) {
		this("LatticeECP3", componentConfigNode);
	}

	public LatticeECP3(final String componentName, final SSHSNode componentConfigNode) {
		super(componentName, componentConfigNode);
	}
}
