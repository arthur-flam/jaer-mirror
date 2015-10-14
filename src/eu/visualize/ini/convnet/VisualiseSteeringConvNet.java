/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.visualize.ini.convnet;

import java.awt.Color;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.eventprocessing.FilterChain;
import net.sf.jaer.graphics.MultilineAnnotationTextRenderer;

/**
 * Extends DavisDeepLearnCnnProcessor to add annotation graphics to show
 * steering decision.
 *
 * @author Tobi
 */
@Description("Displays Visualise steering ConvNet results; subclass of DavisDeepLearnCnnProcessor")
@DevelopmentStatus(DevelopmentStatus.Status.Experimental)
public class VisualiseSteeringConvNet extends DavisDeepLearnCnnProcessor implements PropertyChangeListener {

    private static final int LEFT = 0, CENTER = 1, RIGHT = 2, INVISIBLE = 3; // define output cell types
    private boolean hideOutput = getBoolean("hideOutput", false);
    private boolean showAnalogDecisionOutput = getBoolean("showAnalogDecisionOutput", false);
    private TargetLabeler targetLabeler = null;
    private int totalDecisions = 0, correct = 0, incorrect = 0;
    private Error error = new Error();

    public VisualiseSteeringConvNet(AEChip chip) {
        super(chip);
        setPropertyTooltip("showAnalogDecisionOutput", "shows output units as analog shading");
        setPropertyTooltip("hideOutput", "hides output units");
        FilterChain chain = new FilterChain(chip);
        targetLabeler = new TargetLabeler(chip); // used to validate whether descisions are correct or not
        chain.add(targetLabeler);
        setEnclosedFilterChain(chain);
        apsNet.getSupport().addPropertyChangeListener(DeepLearnCnnNetwork.EVENT_MADE_DECISION, this);
        dvsNet.getSupport().addPropertyChangeListener(DeepLearnCnnNetwork.EVENT_MADE_DECISION, this);
    }

    @Override
    public synchronized EventPacket<?> filterPacket(EventPacket<?> in) {
        targetLabeler.filterPacket(in);
        EventPacket out = super.filterPacket(in);
        return out;
    }

//    private Boolean correctDescisionFromTargetLabeler(TargetLabeler targetLabeler, DeepLearnCnnNetwork net) {
//        if (targetLabeler.getTargetLocation() == null) {
//            return null; // no location labeled for this time
//        }
//        Point p = targetLabeler.getTargetLocation().location;
//        if (p == null) {
//            if (net.outputLayer.maxActivatedUnit == 3) {
//                return true; // no target seen
//            }
//        } else {
//            int x = p.x;
//            int third = (x * 3) / chip.getSizeX();
//            if (third == net.outputLayer.maxActivatedUnit) {
//                return true;
//            }
//        }
//        return false;
//    }
    @Override
    public void resetFilter() {
        super.resetFilter();
        totalDecisions = 0;
        correct = 0;
        incorrect = 0;
        error.reset();

    }

    @Override
    public synchronized void setFilterEnabled(boolean yes) {
        super.setFilterEnabled(yes);
        if (yes && !targetLabeler.hasLocations()) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    targetLabeler.loadLastLocations();
                }
            };
            SwingUtilities.invokeLater(r);
        }
    }

    @Override
    public void annotate(GLAutoDrawable drawable) {
        super.annotate(drawable);
        targetLabeler.annotate(drawable);
        if (hideOutput) {
            return;
        }
        GL2 gl = drawable.getGL().getGL2();
        checkBlend(gl);
        int third = chip.getSizeX() / 3;
        int sy = chip.getSizeY();
        if (apsNet != null && apsNet.outputLayer != null && apsNet.outputLayer.activations != null && (isProcessAPSFrames() | isProcessAPSDVSTogetherInAPSNet())) {
            drawDecisionOutput(third, gl, sy, apsNet, Color.RED);
        }
        if (dvsNet != null && dvsNet.outputLayer != null && dvsNet.outputLayer.activations != null && isProcessDVSTimeSlices()) {
            drawDecisionOutput(third, gl, sy, dvsNet, Color.YELLOW);
        }

        MultilineAnnotationTextRenderer.renderMultilineString(error.toString());
//        if (totalDecisions > 0) {
//            float errorRate = (float) incorrect / totalDecisions;
//            String s = String.format("Error rate %.2f%% (total=%d correct=%d incorrect=%d)\n", errorRate * 100, totalDecisions, correct, incorrect);
//            MultilineAnnotationTextRenderer.renderMultilineString(s);
//        }

    }

    private void drawDecisionOutput(int third, GL2 gl, int sy, DeepLearnCnnNetwork net, Color color) {
        // 0=left, 1=center, 2=right, 3=no target
        int decision = net.outputLayer.maxActivatedUnit;
        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f;
        float[] cv = color.getColorComponents(null);
        if (showAnalogDecisionOutput) {
            final float brightness = .3f; // brightness scale
            for (int i = 0; i < 3; i++) {
                int x0 = third * i;
                int x1 = x0 + third;
                float shade = brightness * net.outputLayer.activations[i];
                gl.glColor3f((shade * r), (shade * g), (shade * b));
                gl.glRecti(x0, 0, x1, sy);
                gl.glRecti(x0, 0, x1, sy);
            }
            float shade = brightness * net.outputLayer.activations[3]; // no target
            gl.glColor3f((shade * r), (shade * g), (shade * b));
            gl.glRecti(0, 0, chip.getSizeX(), sy / 8);

        } else if (decision != INVISIBLE) {
            int x0 = third * decision;
            int x1 = x0 + third;
            float shade = .5f;
            gl.glColor3f((shade * r), (shade * g), (shade * b));
            gl.glRecti(x0, 0, x1, sy);
        }
    }

    /**
     * @return the hideOutput
     */
    public boolean isHideOutput() {
        return hideOutput;
    }

    /**
     * @param hideOutput the hideOutput to set
     */
    public void setHideOutput(boolean hideOutput) {
        this.hideOutput = hideOutput;
        putBoolean("hideOutput", hideOutput);
    }

    /**
     * @return the showAnalogDecisionOutput
     */
    public boolean isShowAnalogDecisionOutput() {
        return showAnalogDecisionOutput;
    }

    /**
     * @param showAnalogDecisionOutput the showAnalogDecisionOutput to set
     */
    public void setShowAnalogDecisionOutput(boolean showAnalogDecisionOutput) {
        this.showAnalogDecisionOutput = showAnalogDecisionOutput;
        putBoolean("showAnalogDecisionOutput", showAnalogDecisionOutput);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() != DeepLearnCnnNetwork.EVENT_MADE_DECISION) {
            super.propertyChange(evt);
        } else {
            DeepLearnCnnNetwork net = (DeepLearnCnnNetwork) evt.getNewValue();
            error.addSample(targetLabeler.getTargetLocation(), net.outputLayer.maxActivatedUnit);
//            Boolean correctDecision = correctDescisionFromTargetLabeler(targetLabeler, net);
//            if (correctDecision != null) {
//                totalDecisions++;
//                if (correctDecision) {
//                    correct++;
//                } else {
//                    incorrect++;
//                }
//            }
        }
    }

    private class Error {

        int totalCount, totalCorrect, totalIncorrect;
        int[] correct = new int[4], incorrect = new int[4], count = new int[4];
        int pixelErrorAllowedForSteering = getInt("pixelErrorAllowedForSteering", 10);

        public Error() {
            reset();
        }

        void reset() {
            totalCount = 0;
            totalCorrect = 0;
            totalIncorrect = 0;
            Arrays.fill(correct, 0);
            Arrays.fill(incorrect, 0);
            Arrays.fill(count, 0);
        }

        void addSample(TargetLabeler.TargetLocation gtTargetLocation, int descision) {
            totalCount++;
            if ((gtTargetLocation == null)) {
                return;
            }

            int third = chip.getSizeX() / 3;

            if (gtTargetLocation.location != null) {
                int x = (int) Math.floor(gtTargetLocation.location.x);
                int gtDescision = x / third;
                if (gtDescision < 0 || gtDescision > 3) {
                    return; // bad descision output, should not happen
                }
                count[gtDescision]++;
                if (gtDescision == descision) {
                    correct[gtDescision]++;
                    totalCorrect++;
                } else {
                    incorrect[gtDescision]++;
                    totalIncorrect++;
                }

            } else {
                count[INVISIBLE]++;
                if (descision == INVISIBLE) {
                    correct[INVISIBLE]++;
                    totalCorrect++;
                } else {
                    incorrect[INVISIBLE]++;
                    totalIncorrect++;
                }
            }
        }

        @Override
        public String toString() {
            if (targetLabeler.hasLocations() == false) {
                return "Error: No ground truth target locations loaded";
            }
            if (totalCount == 0) {
                return "Error: no samples yet";
            }
            StringBuilder sb = new StringBuilder("Error rates: ");
            sb.append(String.format(" Total=%.1f%% (%d/%d) (", (100f * totalIncorrect) / totalCount, totalIncorrect, totalCount));
            for (int i = 0; i < 4; i++) {
                if (count[i] == 0) {
                    sb.append(String.format(" 0/0 "));
                } else {
                    sb.append(String.format(" %.1f%% (%d)", (100f * incorrect[i]) / count[i], count[i]));
                }
            }
            sb.append(")");
            return sb.toString();
        }

    }
}
