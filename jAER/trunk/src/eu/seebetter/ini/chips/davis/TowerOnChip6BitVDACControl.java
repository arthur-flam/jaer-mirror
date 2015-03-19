package eu.seebetter.ini.chips.davis;

/*
 * IPotSliderTextControl.java
 *
 * Created on September 21, 2005, 12:23 PM
 */

import net.sf.jaer.biasgen.coarsefine.*;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.StateEdit;
import javax.swing.undo.StateEditable;
import javax.swing.undo.UndoableEditSupport;

import net.sf.jaer.biasgen.BiasgenFrame;
import net.sf.jaer.biasgen.IPotSliderTextControl;
import net.sf.jaer.biasgen.coarsefine.ShiftedSourceBiasCF.OperatingMode;
import net.sf.jaer.biasgen.coarsefine.ShiftedSourceBiasCF.VoltageLevel;
import net.sf.jaer.util.EngineeringFormat;

/**
 * A GUI control component for controlling a Pot.
 * It shows the name of the Pot, its attributes and 
provides fields for direct bit editing of the Pot value. 
Subclasses provide customized control
of voltage or current biases via the sliderAndValuePanel contents.
 * @author  tobi
 */
public class TowerOnChip6BitVDACControl extends javax.swing.JPanel implements Observer, StateEditable {
    // the IPot is the master; it is an Observable that notifies Observers when its value changes.
    // thus if the slider changes the pot value, the pot calls us back here to update the appearance of the slider and of the
    // text field. likewise, if code changes the pot, the appearance here will automagically be updated.

    static Preferences prefs = Preferences.userNodeForPackage(IPotSliderTextControl.class);
    static Logger log = Logger.getLogger("ConfigurableIPotGUIControl");
    static double ln2 = Math.log(2.);
    TowerOnChip6BitVDAC pot;
    StateEdit edit = null;
    UndoableEditSupport editSupport = new UndoableEditSupport();
    BiasgenFrame frame;
    public static boolean sliderEnabled = prefs.getBoolean("ConfigurableIPot.sliderEnabled", true);
    public static boolean valueEnabled = prefs.getBoolean("ConfigurableIPot.valueEnabled", true);
    public static boolean bitValueEnabled = prefs.getBoolean("ConfigurableIPot.bitValueEnabled", false);
    public static boolean bitViewEnabled = prefs.getBoolean("ConfigurableIPot.bitViewEnabled", true);
    public static boolean sexEnabled = prefs.getBoolean("ConfigurableIPot.sexEnabled", true);
    public static boolean typeEnabled = prefs.getBoolean("ConfigurableIPot.typeEnabled", true);
    private boolean addedUndoListener = false;
    private boolean dontProcessRefSlider = false, dontProcessRegBiasSlider = false;

    // see java tuturial http://java.sun.com/docs/books/tutorial/uiswing/components/slider.html
    // and http://java.sun.com/docs/books/tutorial/uiswing/components/formattedtextfield.html
    /**
     * Creates new form IPotSliderTextControl
     */
    public TowerOnChip6BitVDACControl(TowerOnChip6BitVDAC pot) {
        this.pot = pot;
        initComponents(); // this has unfortunate byproduect of resetting pot value to 0... don't know how to prevent stateChanged event
        dontProcessRegBiasSlider=true;
         bufferSllider.setMaximum(pot.maxBufBitValue - 1); // TODO replace with getter,  needed to prevent extraneous callbacks
        dontProcessRefSlider=true;
         voltageSlider.setMaximum(pot.maxVdacBitValue - 1);
        if (pot != null) {
            nameLabel.setText(pot.getName()); // the name of the bias
            nameLabel.setHorizontalAlignment(SwingConstants.TRAILING);
            nameLabel.setBorder(null);
            if (pot.getTooltipString() != null) {
                nameLabel.setToolTipText(pot.getTooltipString());
            }

            bitPatternTextField.setColumns(pot.getNumBits() + 1);

            pot.loadPreferences(); // to get around slider value change
            pot.addObserver(this); // when pot changes, so does this gui control view
        }
        updateAppearance();  // set controls up with values from ipot
        allInstances.add(this);
        setBitViewEnabled(true);
    }

    public String toString() {
        return "ShiftedSourceControls " + pot.getName();
    }

    void rr() {
        revalidate();
        repaint();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nameLabel = new javax.swing.JLabel();
        voltageSlider = new javax.swing.JSlider();
        voltageTF = new javax.swing.JTextField();
        bufferBiasPanel = new javax.swing.JPanel();
        bufferSllider = new javax.swing.JSlider();
        bufferCurrentTF = new javax.swing.JTextField();
        bitPatternTextField = new javax.swing.JTextField();

        setMaximumSize(new java.awt.Dimension(131243, 25));
        setPreferredSize(new java.awt.Dimension(809, 20));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                formMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                formMouseExited(evt);
            }
        });
        addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                formAncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));

        nameLabel.setFont(new java.awt.Font("Microsoft Sans Serif", 1, 12)); // NOI18N
        nameLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        nameLabel.setText("name");
        nameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        nameLabel.setMaximumSize(new java.awt.Dimension(75, 15));
        nameLabel.setMinimumSize(new java.awt.Dimension(50, 15));
        nameLabel.setPreferredSize(new java.awt.Dimension(70, 15));
        add(nameLabel);

        voltageSlider.setMaximum(63);
        voltageSlider.setToolTipText("Slide to adjust shifted source voltage");
        voltageSlider.setValue(0);
        voltageSlider.setAlignmentX(0.0F);
        voltageSlider.setMaximumSize(new java.awt.Dimension(32767, 16));
        voltageSlider.setMinimumSize(new java.awt.Dimension(36, 10));
        voltageSlider.setPreferredSize(new java.awt.Dimension(200, 25));
        voltageSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                voltageSliderMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                voltageSliderMouseReleased(evt);
            }
        });
        voltageSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                voltageSliderStateChanged(evt);
            }
        });
        add(voltageSlider);

        voltageTF.setColumns(6);
        voltageTF.setFont(new java.awt.Font("Courier New", 0, 11)); // NOI18N
        voltageTF.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        voltageTF.setText("value");
        voltageTF.setToolTipText("Enter bias current here. Up and Down arrows change values. Shift to increment/decrement bit value.");
        voltageTF.setMaximumSize(new java.awt.Dimension(100, 16));
        voltageTF.setMinimumSize(new java.awt.Dimension(11, 15));
        voltageTF.setPreferredSize(new java.awt.Dimension(53, 15));
        voltageTF.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                voltageTFMouseWheelMoved(evt);
            }
        });
        voltageTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                voltageTFActionPerformed(evt);
            }
        });
        voltageTF.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                voltageTFFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                voltageTFFocusLost(evt);
            }
        });
        voltageTF.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                voltageTFKeyPressed(evt);
                valueTextFieldKeyPressed(evt);
            }
        });
        add(voltageTF);

        bufferBiasPanel.setMaximumSize(new java.awt.Dimension(32767, 16));
        bufferBiasPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                bufferBiasPanelformMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                bufferBiasPanelformMouseExited(evt);
            }
        });
        bufferBiasPanel.setLayout(new javax.swing.BoxLayout(bufferBiasPanel, javax.swing.BoxLayout.X_AXIS));

        bufferSllider.setMaximum(63);
        bufferSllider.setToolTipText("Slide to adjust internal buffer bias for shifted source");
        bufferSllider.setValue(0);
        bufferSllider.setAlignmentX(0.0F);
        bufferSllider.setMaximumSize(new java.awt.Dimension(32767, 50));
        bufferSllider.setMinimumSize(new java.awt.Dimension(36, 10));
        bufferSllider.setPreferredSize(new java.awt.Dimension(100, 10));
        bufferSllider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                bufferSlliderMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                bufferSlliderMouseReleased(evt);
            }
        });
        bufferSllider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                bufferSlliderStateChanged(evt);
            }
        });
        bufferBiasPanel.add(bufferSllider);

        bufferCurrentTF.setColumns(6);
        bufferCurrentTF.setFont(new java.awt.Font("Courier New", 0, 11)); // NOI18N
        bufferCurrentTF.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        bufferCurrentTF.setText("value");
        bufferCurrentTF.setToolTipText("Enter buffer bias current here. Up and Down arrows change values. Shift to increment/decrement bit value.");
        bufferCurrentTF.setMaximumSize(new java.awt.Dimension(100, 2147483647));
        bufferCurrentTF.setMinimumSize(new java.awt.Dimension(11, 15));
        bufferCurrentTF.setPreferredSize(new java.awt.Dimension(53, 15));
        bufferCurrentTF.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                bufferCurrentTFMouseWheelMoved(evt);
            }
        });
        bufferCurrentTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bufferCurrentTFActionPerformed(evt);
            }
        });
        bufferCurrentTF.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                bufferCurrentTFFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                bufferCurrentTFFocusLost(evt);
            }
        });
        bufferCurrentTF.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                bufferCurrentTFKeyPressed(evt);
            }
        });
        bufferBiasPanel.add(bufferCurrentTF);

        add(bufferBiasPanel);

        bitPatternTextField.setEditable(false);
        bitPatternTextField.setText("bitPatternTextField");
        bitPatternTextField.setToolTipText("Bit pattern sent to bias gen");
        add(bitPatternTextField);
    }// </editor-fold>//GEN-END:initComponents
//    Border selectedBorder=new EtchedBorder(), unselectedBorder=new EmptyBorder(1,1,1,1);

    private void formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseExited
//        setBorder(unselectedBorder); // TODO add your handling code here:
    }//GEN-LAST:event_formMouseExited

    private void formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseEntered
//        setBorder(selectedBorder);
    }//GEN-LAST:event_formMouseEntered

    private void voltageSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_voltageSliderStateChanged
        // 1. changing slider, e.g. max value, will generate change events here.
        //
        // 2. we can get a double send here if user presses uparrow key,
        // resulting in new pot value,
        // which updates the slider position, which ends up with
        // a different bitvalue that makes a new
        // pot value.
        //See http://java.sun.com/docs/books/tutorial/uiswing/components/slider.html
        //        System.out.println("slider state changed");

        // 3. slider is only source of ChangeEvents, but we can create these event by changing the slider in code.

        // 4. to avoid this, we use dontProcessSlider flag to not update slider from change event handler.

        if (dontProcessRefSlider) {
            dontProcessRefSlider = false;
            return;
        }
        int bv = refBitValueFromSliderValue();
        pot.setRefBitValue(bv);
}//GEN-LAST:event_voltageSliderStateChanged

    private void voltageSliderMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_voltageSliderMousePressed
        startEdit(); // start slider edit when mouse is clicked in it! not when dragging it
}//GEN-LAST:event_voltageSliderMousePressed

    private void voltageSliderMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_voltageSliderMouseReleased
        endEdit();
}//GEN-LAST:event_voltageSliderMouseReleased

    private void voltageTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_voltageTFActionPerformed
        // new pots current value entered
        //        System.out.println("value field action performed");
        try {
            //            float v=Float.parseFloat(valueTextField.getText());
            float v = engFormat.parseFloat(voltageTF.getText());
            //            System.out.println("parsed "+valueTextField.getText()+" as "+v);
            startEdit();
            pot.setRefCurrent(v);
            endEdit();
        } catch (NumberFormatException e) {
            Toolkit.getDefaultToolkit().beep();
            voltageTF.selectAll();
        }
}//GEN-LAST:event_voltageTFActionPerformed

    private void voltageTFFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_voltageTFFocusGained
        voltageTF.setFont(new java.awt.Font("Courier New", 1, 11));  // bold
}//GEN-LAST:event_voltageTFFocusGained

    private void voltageTFFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_voltageTFFocusLost
        voltageTF.setFont(new java.awt.Font("Courier New", 0, 11));
}//GEN-LAST:event_voltageTFFocusLost

    private void voltageTFKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_voltageTFKeyPressed
        int code = evt.getKeyCode();
        if (code == KeyEvent.VK_UP) {
            pot.setRefBitValue(pot.getVdacBitValue() + 1);
            endEdit();
        } else if (code == KeyEvent.VK_DOWN) {
            startEdit();
            pot.setRefBitValue(pot.getVdacBitValue() - 1);
            endEdit();
        }
        pot.updateBitValue();
}//GEN-LAST:event_voltageTFKeyPressed

    private void voltageTFMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_voltageTFMouseWheelMoved
        int clicks = evt.getWheelRotation();
        pot.setRefBitValue(pot.getVdacBitValue() - clicks);
}//GEN-LAST:event_voltageTFMouseWheelMoved

    private void bufferSlliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bufferSlliderStateChanged
        // we can get a double send here if user presses uparrow key,
        // resulting in new pot value,
        // which updates the slider position, which ends up with
        // a different bitvalue that makes a new
        // pot value.
        //See http://java.sun.com/docs/books/tutorial/uiswing/components/slider.html
        //        System.out.println("slider state changed");
        // slider is only source of ChangeEvents
        //        System.out.println("slider state changed for "+pot);

        //        if(!s.getValueIsAdjusting()){
        //            startEdit();
        //        }

        if (dontProcessRegBiasSlider) {
            dontProcessRegBiasSlider = false;
            return;
        }
        int bbv = regBitValueFromSliderValue();
        pot.setBufferBitValue(bbv);
//        log.info("from slider state change got new buffer bit value = " + pot.getBufferBitValue() + " from slider value =" + s.getValue());
}//GEN-LAST:event_bufferSlliderStateChanged

    private void bufferSlliderMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bufferSlliderMousePressed
        startEdit(); // start slider edit when mouse is clicked in it! not when dragging it
}//GEN-LAST:event_bufferSlliderMousePressed

    private void bufferSlliderMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bufferSlliderMouseReleased
        endEdit();
}//GEN-LAST:event_bufferSlliderMouseReleased

    private void bufferCurrentTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bufferCurrentTFActionPerformed
        try {
            float v = engFormat.parseFloat(bufferCurrentTF.getText());
            startEdit();
            bufferCurrentTF.setText(engFormat.format(pot.setRegCurrent(v)));
            endEdit();
        } catch (NumberFormatException e) {
            Toolkit.getDefaultToolkit().beep();
            bufferCurrentTF.selectAll();
        }
}//GEN-LAST:event_bufferCurrentTFActionPerformed

    private void bufferCurrentTFFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bufferCurrentTFFocusGained
        bufferCurrentTF.setFont(new java.awt.Font("Courier New", 1, 11));
}//GEN-LAST:event_bufferCurrentTFFocusGained

    private void bufferCurrentTFFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bufferCurrentTFFocusLost
        bufferCurrentTF.setFont(new java.awt.Font("Courier New", 0, 11));
}//GEN-LAST:event_bufferCurrentTFFocusLost

    private void bufferCurrentTFKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_bufferCurrentTFKeyPressed
        // key pressed in text field
        //        System.out.println("keyPressed evt "+evt);
        //        System.out.println("value field key pressed");
        int code = evt.getKeyCode();
        if (code == KeyEvent.VK_UP) {
            pot.setBufferBitValue(pot.getBufferBitValue() + 1);
            endEdit();
        } else if (code == KeyEvent.VK_DOWN) {
            startEdit();
            pot.setBufferBitValue(pot.getBufferBitValue() - 1);
            endEdit();
        }
        pot.updateBitValue();
}//GEN-LAST:event_bufferCurrentTFKeyPressed

    private void bufferCurrentTFMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_bufferCurrentTFMouseWheelMoved
        int clicks = evt.getWheelRotation();
        pot.setBufferBitValue(pot.getBufferBitValue() - clicks); // rotating wheel away gives negative clicks (scrolling up) but should increase current
}//GEN-LAST:event_bufferCurrentTFMouseWheelMoved

    private void bufferBiasPanelformMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bufferBiasPanelformMouseEntered
        //        setBorder(selectedBorder);
}//GEN-LAST:event_bufferBiasPanelformMouseEntered

    private void bufferBiasPanelformMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bufferBiasPanelformMouseExited
        //        setBorder(unselectedBorder); // TODO add your handling code here:
}//GEN-LAST:event_bufferBiasPanelformMouseExited

    private void valueTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_valueTextFieldKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_valueTextFieldKeyPressed

private void formAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_formAncestorAdded
    if (addedUndoListener) {
        return;
    }
    addedUndoListener = true;
    if (evt.getComponent() instanceof Container) {
        Container anc = (Container) evt.getComponent();
        while (anc != null && anc instanceof Container) {
            if (anc instanceof UndoableEditListener) {
                editSupport.addUndoableEditListener((UndoableEditListener) anc);
                break;
            }
            anc = anc.getParent();
        }
    }
}//GEN-LAST:event_formAncestorAdded

//     private int oldPotValue=0;
    /** when slider is moved, event is sent here. The slider is the 'master' of the value in the text field.
     * Slider is log scale, from pot min to pot max with caveat that zero position is zero current (no current splitter
     * outputs switched on) and rest of values are log scale from pot.getCurrentResolution to pot.getMaxCurrent
     * @param e the ChangeEvent
     */
    void startEdit() {
//        System.out.println("ipot start edit "+pot);
        edit = new MyStateEdit(this, "ShiftedSourceControlsEdit");
//         oldPotValue=pot.getVdacBitValue();
    }

    void endEdit() {
//         if(oldPotValue==pot.getVdacBitValue()){
////            System.out.println("no edit, because no change in "+pot);
//             return;
//         }
//        System.out.println("ipot endEdit "+pot);
        if (edit != null) {
            edit.end();
        }
//        System.out.println("ipot "+pot+" postEdit");
        editSupport.postEdit(edit);
    }
    final String KEY_REFBITVALUE = "refBitValue";
    final String KEY_REGBITVALUE = "regBitValue";
    final String KEY_OPERATINGMODE = "operatingMode";
    final String KEY_VOLTAGELEVEL = "voltageLevel";
    final String KEY_ENABLED = "enabled";

    public void restoreState(Hashtable<?, ?> hashtable) {
//        System.out.println("restore state");
        if (hashtable == null) {
            throw new RuntimeException("null hashtable");
        }
        if (hashtable.get(KEY_REFBITVALUE) == null) {
            log.warning("pot " + pot + " not in hashtable " + hashtable + " with size=" + hashtable.size());

            return;
        }
        if (hashtable.get(KEY_REGBITVALUE) == null) {
            log.warning("pot " + pot + " not in hashtable " + hashtable + " with size=" + hashtable.size());
            return;
        }
        pot.setRefBitValue((Integer) hashtable.get(KEY_REFBITVALUE));
        pot.setBufferBitValue((Integer) hashtable.get(KEY_REGBITVALUE));
    }

    public void storeState(Hashtable<Object, Object> hashtable) {
//        System.out.println(" storeState "+pot);
        hashtable.put(KEY_REFBITVALUE, new Integer(pot.getVdacBitValue()));
        hashtable.put(KEY_REGBITVALUE, new Integer(pot.getBufferBitValue()));

    }

    class MyStateEdit extends StateEdit {

        public MyStateEdit(StateEditable o, String s) {
            super(o, s);
        }

        protected void removeRedundantState() {
        }// override this to actually get a state stored!!
    }
    private static EngineeringFormat engFormat = new EngineeringFormat();

    /** updates the GUI slider and text fields to match actual pot values.
     * These updates should not trigger events that cause edits to be stored.
     */
    protected final void updateAppearance() {
        if (pot == null) {
            return;
        }
        if (voltageSlider.isVisible() != sliderEnabled) {
            voltageSlider.setVisible(sliderEnabled);
            rr();
        }
        if (voltageTF.isVisible() != valueEnabled) {
            voltageTF.setVisible(valueEnabled);
            rr();
        }
        
        if (bufferSllider.isVisible() != sliderEnabled) {
            bufferSllider.setVisible(sliderEnabled);
            rr();
        }
        if (bufferSllider.isVisible() != valueEnabled) {
            bufferSllider.setVisible(valueEnabled);
            rr();
        }

        voltageSlider.setValue(refSliderValueFromBitValue());
        voltageTF.setText(engFormat.format(pot.getRefCurrent()));

        if (bitPatternTextField.isVisible() != bitViewEnabled) {
            bitPatternTextField.setVisible(bitViewEnabled);
            rr();
        }
        bitPatternTextField.setText(String.format("%16s", Integer.toBinaryString(pot.computeBinaryRepresentation())).replace(' ', '0'));

        bufferSllider.setValue(regSliderValueFromBitValue());
        bufferCurrentTF.setText(engFormat.format(pot.getRegCurrent()));


        //log.info("update appearance "+pot.getName());

    }
    // following two methods compute slider/bit value inverses
    private final int knee = 8;  // at this value, mapping goes from linear to log
    // following assumes slider max value is same as max bit value

    private double log2(double x) {
        return Math.log(x) / ln2;
    }

    /** Maps from bit value to linear/log slider value.
     * 
     * @param v bit value.
     * @param vmax max bit value.
     * @param slider the slider for the value.
     * @return the correct slider value.
     */
    private int bitVal2SliderVal(int v, int vmax, JSlider slider) {
        int s = 0;
        double sm = slider.getMaximum();
        double vm = vmax;
        s = (int) Math.round(sm*v/vm);;
//        log.info("bitValue=" + v + " -> sliderValue=" + s);
        return s;
    }

    /** Maps from linear slider to linear/exponential bit value.
     *
     * @param vmax max bit value.
     * @param slider the slider.
     * @return the bit value.
     */
    private int sliderVal2BitVal(int vmax, JSlider slider) {
        int v = 0;
        int s = slider.getValue();
        double sm = slider.getMaximum();
        double vm = vmax;
        v = (int) Math.round(vm*s/sm);
//        log.info("sliderValue=" + s + " -> bitValue=" + v);
        return v;
    }

    private int refSliderValueFromBitValue() {
        int s = bitVal2SliderVal(pot.getVdacBitValue(), pot.maxVdacBitValue, voltageSlider);
        return s;
    }

    private int refBitValueFromSliderValue() {
        int v = sliderVal2BitVal(pot.maxVdacBitValue, voltageSlider);
        return v;
    }

    /** Returns slider value for this pots buffer bit value. */
    private int regSliderValueFromBitValue() {
        int v = bitVal2SliderVal(pot.getBufferBitValue(), pot.maxBufBitValue, bufferSllider);
        return v;
    }

    /** Returns buffer bit value from the slider value. */
    private int regBitValueFromSliderValue() {
        int v = sliderVal2BitVal(pot.maxBufBitValue, bufferSllider);
        return v;
    }

    /** called when Observable changes (pot changes) */
    public void update(Observable observable, Object obj) {
        if (observable instanceof ShiftedSourceBiasCF) {
//            log.info("observable="+observable);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    // don't do the following - it sometimes prevents display updates or results in double updates
//                        slider.setValueIsAdjusting(true); // try to prevent a new event from the slider
                    updateAppearance();
                }
            });
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField bitPatternTextField;
    private javax.swing.JPanel bufferBiasPanel;
    private javax.swing.JTextField bufferCurrentTF;
    private javax.swing.JSlider bufferSllider;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JSlider voltageSlider;
    private javax.swing.JTextField voltageTF;
    // End of variables declaration//GEN-END:variables

    public JTextField getBitPatternTextField() {
        return this.bitPatternTextField;
    }

    public static boolean isBitValueEnabled() {
        return TowerOnChip6BitVDACControl.bitValueEnabled;
    }

    public static void setBitValueEnabled(final boolean bitValueEnabled) {
        TowerOnChip6BitVDACControl.bitValueEnabled = bitValueEnabled;
        prefs.putBoolean("ConfigurableIPot.bitValueEnabled", bitValueEnabled);
    }

    public static boolean isBitViewEnabled() {
        return TowerOnChip6BitVDACControl.bitViewEnabled;
    }

    public static void setBitViewEnabled(final boolean bitViewEnabled) {
        TowerOnChip6BitVDACControl.bitViewEnabled = bitViewEnabled;
        prefs.putBoolean("ConfigurableIPot.bitViewEnabled", bitViewEnabled);
    }

    public static boolean isValueEnabled() {
        return TowerOnChip6BitVDACControl.valueEnabled;
    }

    public static void setValueEnabled(final boolean valueEnabled) {
        TowerOnChip6BitVDACControl.valueEnabled = valueEnabled;
        prefs.putBoolean("ConfigurableIPot.valueEnabled", valueEnabled);
    }

    public static boolean isSexEnabled() {
        return TowerOnChip6BitVDACControl.sexEnabled;
    }

    public static void setSexEnabled(final boolean sexEnabled) {
        TowerOnChip6BitVDACControl.sexEnabled = sexEnabled;
        prefs.putBoolean("ConfigurableIPot.sliderEnabled", sliderEnabled);
    }

    public static boolean isSliderEnabled() {
        return IPotSliderTextControl.sliderEnabled;
    }

    public static void setSliderEnabled(final boolean sliderEnabled) {
        TowerOnChip6BitVDACControl.sliderEnabled = sliderEnabled;
        prefs.putBoolean("ConfigurableIPot.sliderEnabled", sliderEnabled);
    }

    public static boolean isTypeEnabled() {
        return TowerOnChip6BitVDACControl.typeEnabled;
    }

    public static void setTypeEnabled(final boolean typeEnabled) {
        TowerOnChip6BitVDACControl.typeEnabled = typeEnabled;
        prefs.putBoolean("ConfigurableIPot.typeEnabled", typeEnabled);
    }
    static ArrayList<TowerOnChip6BitVDACControl> allInstances = new ArrayList<TowerOnChip6BitVDACControl>();

    public static void revalidateAllInstances() {
        for (TowerOnChip6BitVDACControl c : allInstances) {
            c.updateAppearance();
            c.revalidate();
        }
    }
    static String[] controlNames = {"Type", "Sex", "Slider"}; // TODO ,"BitValue","BitView"
    public static JMenu viewMenu;

    static {
        viewMenu = new JMenu("View options");
        viewMenu.setMnemonic('V');
        for (int i = 0; i < controlNames.length; i++) {
            viewMenu.add(new VisibleSetter(controlNames[i])); // add a menu item to enable view of this class of information
        }
    }

    /** this inner static class updates the appearance of all instances of the control 
     */
    static class VisibleSetter extends JCheckBoxMenuItem {

        public String myName;
        Method setMethod, isSetMethod;

        public VisibleSetter(String myName) {
            super(myName);
            this.myName = myName;
            try {
                setMethod = TowerOnChip6BitVDACControl.class.getMethod("set" + myName + "Enabled", Boolean.TYPE);
                isSetMethod = TowerOnChip6BitVDACControl.class.getMethod("is" + myName + "Enabled");
                boolean isSel = (Boolean) isSetMethod.invoke(TowerOnChip6BitVDACControl.class);
                setSelected(isSel);
            } catch (Exception e) {
                e.printStackTrace();
            }
            addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        setMethod.invoke(TowerOnChip6BitVDACControl.class, new Boolean(isSelected()));
                        setSelected(isSelected());
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    TowerOnChip6BitVDACControl.revalidateAllInstances();
                }
            });
        }
    }
}
