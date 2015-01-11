/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.visualize.ini.convnet;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.sun.glass.ui.CommonDialogs;
import eu.seebetter.ini.chips.ApsDvsChip;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.BasicEvent;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.eventio.AEInputStream;
import net.sf.jaer.eventprocessing.EventFilter2DMouseAdaptor;
import net.sf.jaer.graphics.MultilineAnnotationTextRenderer;

/**
 * Labels location of target using mouse GUI in recorded data for later
 * supervised learning.
 *
 * @author tobi
 */
@DevelopmentStatus(DevelopmentStatus.Status.Experimental)
@Description("Labels location of target using mouse GUI in recorded data for later supervised learning.")
public class TargetLabeler extends EventFilter2DMouseAdaptor implements PropertyChangeListener, KeyListener {

    private boolean mousePressed = false;
    private boolean shiftPressed = false;
    private boolean altPressed = false;
    private Point mousePoint = null;
    final float labelRadius = 5f;
    private GLUquadric mouseQuad = null;
    private TreeMap<Integer, TargetLocation> targetLocations = new TreeMap();
    private TargetLocation targetLocation = null;
    private ApsDvsChip apsDvsChip = null;
    private int lastFrameNumber = -1;
    private int lastTimestamp = Integer.MIN_VALUE;
    private int currentFrameNumber = -1;
    private final String LAST_FOLDER_KEY = "lastFolder";
    TextRenderer textRenderer = null;
    private int minTargetPointIntervalUs = getInt("minTargetPointIntervalUs", 10000);
    private int targetRadius = getInt("targetRadius", 10);
    private int maxTimeLastTargetLocationValidUs = getInt("maxTimeLastTargetLocationValidUs", 100000);
    private int minSampleTimestamp = Integer.MAX_VALUE, maxSampleTimestamp = Integer.MIN_VALUE;

    private boolean propertyChangeListenerAdded = false;
    private String DEFAULT_FILENAME = "locations.txt";
    private String lastFileName = getString("lastFileName", DEFAULT_FILENAME);

    public TargetLabeler(AEChip chip) {
        super(chip);
        if (chip instanceof ApsDvsChip) {
            apsDvsChip = ((ApsDvsChip) chip);
        }
        setPropertyTooltip("minTargetPointIntervalUs", "minimum interval between target positions in the database in us");
        setPropertyTooltip("targetRadius", "drawn radius of target in pixels");
        setPropertyTooltip("maxTimeLastTargetLocationValidUs", "this time after last sample, the data is shown as not yet been labeled");
        setPropertyTooltip("saveLocations", "saves target locations");
        setPropertyTooltip("saveLocationsAs", "show file dialog to save target locations to a new file");
        setPropertyTooltip("loadLocations", "loads locations from a file");

    }

    @Override
    public void mouseDragged(MouseEvent e) {

        Point p = (getMousePixel(e));
        if (p != null) {
            if (mousePoint != null) {
                mousePoint.setLocation(p);
            } else {
                mousePoint = new Point(p);
            }
        } else {
            mousePoint = null;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mousePressed = false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {

        Point p = (getMousePixel(e));
        if (p != null) {
            if (mousePoint != null) {
                mousePoint.setLocation(p);
            } else {
                mousePoint = new Point(p);
            }
        } else {
            mousePoint = null;
        }
    }

    @Override
    synchronized public void annotate(GLAutoDrawable drawable) {
        super.annotate(drawable);
        if (textRenderer == null) {
            textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 36));
            textRenderer.setColor(1, 1, 1, 1);
        }
        GL2 gl = drawable.getGL().getGL2();
        MultilineAnnotationTextRenderer.setColor(Color.BLUE);
        MultilineAnnotationTextRenderer.resetToYPositionPixels(chip.getSizeY() * .9f);
        MultilineAnnotationTextRenderer.setScale(.3f);
        StringBuilder sb = new StringBuilder("Shift + Alt + mouse position: specify target location\nShift: no target seen\n");
        MultilineAnnotationTextRenderer.renderMultilineString(sb.toString());

        MultilineAnnotationTextRenderer.renderMultilineString(String.format("%d TargetLocation samples specified\nFirst sample time: %.1fs, Last sample time: %.1fs", targetLocations.size(), minSampleTimestamp * 1e-6f, maxSampleTimestamp * 1e-6f));

        if (shiftPressed && !altPressed) {
            MultilineAnnotationTextRenderer.renderMultilineString("Specifying no target");
        } else if (shiftPressed && altPressed) {
            MultilineAnnotationTextRenderer.renderMultilineString("Specifying target location");
        } else {
            MultilineAnnotationTextRenderer.renderMultilineString("Playing recorded target locations");
        }
        if (targetLocation != null) {
            targetLocation.draw(drawable, gl);
        }

    }

    synchronized public void doClearLocations() {
        targetLocations.clear();
        minSampleTimestamp = Integer.MAX_VALUE;
        maxSampleTimestamp = Integer.MIN_VALUE;
    }

    synchronized public void doSaveLocationsAs() {
        JFileChooser c = new JFileChooser(lastFileName);
        c.setSelectedFile(new File(lastFileName));
        int ret = c.showSaveDialog(glCanvas);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }
        lastFileName = c.getSelectedFile().toString();
        if (c.getSelectedFile().exists()) {
            int r = JOptionPane.showConfirmDialog(glCanvas, "File " + c.getSelectedFile().toString() + " already exists, overwrite it?");
            if (r != JOptionPane.OK_OPTION) {
                return;
            }
        }
        saveLocations(c.getSelectedFile());
    }

    synchronized public void doSaveLocations() {
        File f = new File(lastFileName);
        saveLocations(new File(lastFileName));
    }

    synchronized public void doLoadLocations() {
        JFileChooser c = new JFileChooser(lastFileName);
        c.setSelectedFile(new File(lastFileName));
        int ret = c.showOpenDialog(glCanvas);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }
        lastFileName = c.getSelectedFile().toString();
        putString("lastFileName", lastFileName);
        loadLocations(new File(lastFileName));
    }

    private TargetLocation lastNewTargetLocation = null;

    @Override
    synchronized public EventPacket<?> filterPacket(EventPacket<?> in) {
        if (!propertyChangeListenerAdded) {
            if (chip.getAeViewer() != null) {
                chip.getAeViewer().addPropertyChangeListener(this);
                propertyChangeListenerAdded = true;
            }
        }

        for (BasicEvent e : in) {
            if (e.isSpecial()) {
                continue;
            }
            if (apsDvsChip != null) {

                // update actual frame number, starting from 0 at start of recording (for playback or after rewind)
                // this can be messed up by jumping in the file using slider
                int newFrameNumber = apsDvsChip.getFrameCount();
                if (newFrameNumber != lastFrameNumber) {
                    if (newFrameNumber > lastFrameNumber) {
                        currentFrameNumber++;
                    } else if (newFrameNumber < lastFrameNumber) {
                        currentFrameNumber--;
                    }
                    lastFrameNumber = newFrameNumber;
                }

                // show the nearest TargetLocation if at least minTargetPointIntervalUs has passed by,
                // or "No target" if the location was previously 
                if ((long) e.timestamp - (long) lastTimestamp >= minTargetPointIntervalUs) {
                    lastTimestamp = e.timestamp;
                    // find next saved target location that is just before this time (lowerEntry)
                    Map.Entry<Integer, TargetLocation> mostRecentLocationBeforeThisEvent = targetLocations.lowerEntry(e.timestamp);
                    if (mostRecentLocationBeforeThisEvent == null || (mostRecentLocationBeforeThisEvent != null && (mostRecentLocationBeforeThisEvent.getValue() != null && (e.timestamp - mostRecentLocationBeforeThisEvent.getValue().timestamp) > maxTimeLastTargetLocationValidUs))) {
                        targetLocation = null;
                    } else {
                        targetLocation = mostRecentLocationBeforeThisEvent.getValue();
                    }
                    TargetLocation newTargetLocation = null;
                    if (shiftPressed && altPressed && mousePoint != null) {
                        // add a labeled location sample
                        maybeRemovePreviouslyRecordedSample(mostRecentLocationBeforeThisEvent, e, lastNewTargetLocation);
                        newTargetLocation = new TargetLocation(currentFrameNumber, e.timestamp, mousePoint);
                        targetLocations.put(e.timestamp, newTargetLocation);

                    } else if (shiftPressed && !altPressed) {
                        maybeRemovePreviouslyRecordedSample(mostRecentLocationBeforeThisEvent, e, lastNewTargetLocation);
                        newTargetLocation = new TargetLocation(currentFrameNumber, e.timestamp, null);
                        targetLocations.put(e.timestamp, newTargetLocation);
                    }
                    if (newTargetLocation != null) {
                        if (newTargetLocation.timestamp > maxSampleTimestamp) {
                            maxSampleTimestamp = newTargetLocation.timestamp;
                        }
                        if (newTargetLocation.timestamp < minSampleTimestamp) {
                            minSampleTimestamp = newTargetLocation.timestamp;
                        }
                    }
                    lastNewTargetLocation = newTargetLocation;
                }
                if (e.timestamp < lastTimestamp) {
                    lastTimestamp = e.timestamp;
                }

            }
        }
        return in;
    }

    private void maybeRemovePreviouslyRecordedSample(Map.Entry<Integer, TargetLocation> entry, BasicEvent e, TargetLocation lastSampleAdded) {
        if (entry != null && entry.getValue() != lastSampleAdded && e.timestamp - entry.getKey() < minTargetPointIntervalUs) {
            log.info("removing previous " + entry.getValue() + " because entry.getValue()!=lastSampleAdded=" + (entry.getValue() != lastSampleAdded) + " && timestamp difference " + (e.timestamp - entry.getKey()) + " is < " + minTargetPointIntervalUs);

            targetLocations.remove(entry.getKey());
        }
    }

    @Override
    public void setSelected(boolean yes) {
        super.setSelected(yes); // register/deregister mouse listeners
        if (yes) {
            glCanvas.addKeyListener(this);
        } else {
            glCanvas.removeKeyListener(this);
        }
    }

    @Override
    public void resetFilter() {
    }

    @Override
    public void initFilter() {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt
    ) {
        switch (evt.getPropertyName()) {
            case AEInputStream.EVENT_REWIND:
                log.info("frameNumber reset to -1");
                lastFrameNumber = -1;
                currentFrameNumber = 0;
                lastTimestamp = Integer.MIN_VALUE;
                break;
            case AEInputStream.EVENT_POSITION:
                break;
            case AEInputStream.EVENT_EOF:

        }
    }

    /**
     * @return the minTargetPointIntervalUs
     */
    public int getMinTargetPointIntervalUs() {
        return minTargetPointIntervalUs;
    }

    /**
     * @param minTargetPointIntervalUs the minTargetPointIntervalUs to set
     */
    public void setMinTargetPointIntervalUs(int minTargetPointIntervalUs) {
        this.minTargetPointIntervalUs = minTargetPointIntervalUs;
        putInt("minTargetPointIntervalUs", minTargetPointIntervalUs);
    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        int k = ke.getKeyCode();
        if (k == KeyEvent.VK_SHIFT) {
            shiftPressed = true;
        } else if (k == KeyEvent.VK_ALT) {
            altPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
        int k = ke.getKeyCode();
        if (k == KeyEvent.VK_SHIFT) {
            shiftPressed = false;
        } else if (k == KeyEvent.VK_ALT) {
            altPressed = false;
        }
    }

    /**
     * @return the targetRadius
     */
    public int getTargetRadius() {
        return targetRadius;
    }

    /**
     * @param targetRadius the targetRadius to set
     */
    public void setTargetRadius(int targetRadius) {
        this.targetRadius = targetRadius;
        putInt("targetRadius", targetRadius);
    }

    /**
     * @return the maxTimeLastTargetLocationValidUs
     */
    public int getMaxTimeLastTargetLocationValidUs() {
        return maxTimeLastTargetLocationValidUs;
    }

    /**
     * @param maxTimeLastTargetLocationValidUs the
     * maxTimeLastTargetLocationValidUs to set
     */
    public void setMaxTimeLastTargetLocationValidUs(int maxTimeLastTargetLocationValidUs) {
        if (maxTimeLastTargetLocationValidUs < minTargetPointIntervalUs) {
            maxTimeLastTargetLocationValidUs = minTargetPointIntervalUs;
        }
        this.maxTimeLastTargetLocationValidUs = maxTimeLastTargetLocationValidUs;
        putInt("maxTimeLastTargetLocationValidUs", maxTimeLastTargetLocationValidUs);
    }

    private class TargetLocationComparator implements Comparator<TargetLocation> {

        @Override
        public int compare(TargetLocation o1, TargetLocation o2) {
            return Integer.valueOf(o1.frameNumber).compareTo(Integer.valueOf(o2.frameNumber));
        }

    }

    private class TargetLocation {

        int timestamp;
        int frameNumber;
        Point location;

        public TargetLocation(int frameNumber, int timestamp, Point location) {
            this.frameNumber = frameNumber;
            this.timestamp = timestamp;
            this.location = location != null ? new Point(location) : null;
        }

        private void draw(GLAutoDrawable drawable, GL2 gl) {

            if (targetLocation.location == null) {
                textRenderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
                textRenderer.draw("Target not visible", chip.getSizeX() / 2, chip.getSizeY() / 2);
                textRenderer.endRendering();
                return;
            }
            gl.glPushMatrix();
            gl.glTranslatef(targetLocation.location.x, targetLocation.location.y, 0f);
            gl.glColor4f(0, 1, 0, .5f);
            if (mouseQuad == null) {
                mouseQuad = glu.gluNewQuadric();
            }
            glu.gluQuadricDrawStyle(mouseQuad, GLU.GLU_LINE);
            glu.gluDisk(mouseQuad, getTargetRadius(), getTargetRadius() + 1, 32, 1);
            gl.glPopMatrix();
        }

        public String toString() {
            return String.format("TargetLocation frameNumber=%d timestamp=%d location=%s", frameNumber, timestamp, location == null ? "null" : location.toString());
        }

    }

    private void saveLocations(File f) {
        try {
            FileWriter writer = new FileWriter(f);
            writer.write(String.format("# target locations\n"));
            writer.write(String.format("# written %s\n", new Date().toString()));
            writer.write(String.format("# frameNumber timestamp x y\n"));
            for (Map.Entry<Integer, TargetLocation> entry : targetLocations.entrySet()) {
                TargetLocation l = entry.getValue();
                if (l.location != null) {
                    writer.write(String.format("%d %d %d %d\n", l.frameNumber, l.timestamp, l.location.x, l.location.y));
                } else {
                    writer.write(String.format("%d %d null\n", l.frameNumber, l.timestamp));
                }
            }
            writer.close();
            log.info("wrote locations to file " + f.getAbsolutePath());
            lastFileName = f.toString();
            putString("lastFileName", lastFileName);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(glCanvas, ex.toString(), "Couldn't save locations", JOptionPane.WARNING_MESSAGE, null);
            return;
        }
    }

    private void loadLocations(File f) {
        targetLocations.clear();
        minSampleTimestamp = Integer.MAX_VALUE;
        maxSampleTimestamp = Integer.MIN_VALUE;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String s = reader.readLine();
            StringBuilder sb = new StringBuilder();
            while (s != null && s.startsWith("#")) {
                sb.append(s + "\n");
                s = reader.readLine();
            }
            log.info("header lines on " + f.getAbsolutePath() + " are\n" + sb.toString());
            while (s != null) {
                Scanner scanner = new Scanner(s);
                try {
                    TargetLocation targetLocation = new TargetLocation(scanner.nextInt(), scanner.nextInt(), new Point(scanner.nextInt(), scanner.nextInt())); // read target location
                    targetLocations.put(targetLocation.timestamp, targetLocation);
                    if (targetLocation != null) {
                        if (targetLocation.timestamp > maxSampleTimestamp) {
                            maxSampleTimestamp = targetLocation.timestamp;
                        }
                        if (targetLocation.timestamp < minSampleTimestamp) {
                            minSampleTimestamp = targetLocation.timestamp;
                        }
                    }
                } catch (InputMismatchException ex) {
                    // infer this line is null target sample
                    Scanner scanner2 = new Scanner(s);
                    try {
                        TargetLocation targetLocation = new TargetLocation(scanner2.nextInt(), scanner2.nextInt(), null);
                        targetLocations.put(targetLocation.timestamp, targetLocation);
                    } catch (InputMismatchException ex2) {
                        throw new IOException("couldn't parse file, got InputMismatchException on line: " + s);
                    }
                }
                s = reader.readLine();
            }
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(glCanvas, ex.toString(), "Couldn't load locations", JOptionPane.WARNING_MESSAGE, null);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(glCanvas, ex.toString(), "Couldn't load locations", JOptionPane.WARNING_MESSAGE, null);
        }
    }

}
