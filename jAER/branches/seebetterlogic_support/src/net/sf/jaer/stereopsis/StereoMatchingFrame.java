/*
 * StereoMatchingViewer.java
 *
 * Created on 13. Juni 2006, 09:05
 */

package net.sf.jaer.stereopsis;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import net.sf.jaer.chip.AEChip;

/**
 * Frame for visualizing matching matrix and disparites.
 *
 * @author  Peter Hess
 */
public class StereoMatchingFrame extends javax.swing.JFrame {
    static Preferences prefs = Preferences.userNodeForPackage(StereoMatchingFrame.class);
    static Logger log=Logger.getLogger("filter");
    AEChip chip;
    int maxDisp;
    
    private Label labelDisp, labelSmc, labelScd;
    private StereoMatchingCanvas smc;
    private StereoDisparitiesCanvas sdc;
    
    /** Creates new form StereoMatchingViewer */
    public StereoMatchingFrame(AEChip chip, int maxDisp) {
        this.chip = chip;
        this.maxDisp = maxDisp;
        initComponents();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints layoutConstraints = new GridBagConstraints();
        layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
        layoutConstraints.gridwidth = GridBagConstraints.REMAINDER;

        setLayout(layout);
        labelDisp = new Label("Disparity: " + 0);
        layout.setConstraints(labelDisp, layoutConstraints);
        add(labelDisp);
        labelSmc = new Label("Matching matrix");
        layout.setConstraints(labelSmc, layoutConstraints);
        add(labelSmc);
        smc = new StereoMatchingCanvas(chip);
        layout.setConstraints(smc, layoutConstraints);
        add(smc);
        labelScd = new Label("Accumulated disparity values");
        layout.setConstraints(labelScd, layoutConstraints);
        add(labelScd);
        sdc = new StereoDisparitiesCanvas(maxDisp);
        layout.setConstraints(sdc, layoutConstraints);
        add(sdc);
        
        setPreferredSize(null);
        pack();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        getContentPane().setLayout(null);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Disparity Matching Viewer");
        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    public void visualize(int bestDisp, float[][] matchings, float[] disparities) {
        smc.setBestDisp(bestDisp);
        smc.setMatchings(matchings);
        sdc.setBestDisp(bestDisp);
        sdc.setDisparities(disparities);
        
        labelDisp.setText("Disparity: " + bestDisp);
        smc.repaint();
        sdc.repaint();
    }
}
