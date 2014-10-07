/*
 * ZipZapControlGUI.java
 *
 * Created on April 20, 2008, 3:55 PM
 */
package ch.unizh.ini.jaer.projects.tobi.zipzaps;

import java.awt.event.KeyEvent;

import net.sf.jaer.hardwareinterface.HardwareInterfaceException;
import net.sf.jaer.util.ExceptionListener;

/**
 *  GUI to control ZipZap car.
 * @author  tobi
 */
public class ZipZapControlGUI extends javax.swing.JFrame implements ExceptionListener {

    ZipZapControl control;

    /** Creates new form ZipZapControlGUI */
    public ZipZapControlGUI() {
        initComponents();
        control = new ZipZapControl();
        panel1.requestFocusInWindow();
        HardwareInterfaceException.addExceptionListener(this);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        statusTextField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ZipZap");

        panel1.setToolTipText("Use arrow keys to drive the ZipZap");
        panel1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                panel1KeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                panel1KeyReleased(evt);
            }
        });

        jLabel1.setText("Use arrow keys to drive");

        javax.swing.GroupLayout panel1Layout = new javax.swing.GroupLayout(panel1);
        panel1.setLayout(panel1Layout);
        panel1Layout.setHorizontalGroup(
            panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel1Layout.createSequentialGroup()
                .addContainerGap(80, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(384, 384, 384))
        );
        panel1Layout.setVerticalGroup(
            panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel1Layout.createSequentialGroup()
                .addGap(52, 52, 52)
                .addComponent(jLabel1)
                .addContainerGap(56, Short.MAX_VALUE))
        );

        statusTextField.setEditable(false);
        statusTextField.setText("status");
        statusTextField.setToolTipText("shows last HardwarInterfaceException");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(statusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 578, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(statusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    int lastKeyCode=0;
    void keyPressed(KeyEvent evt) {
        if(evt.getKeyCode()==lastKeyCode) return;
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_X:
                System.exit(0);
                break;
            case KeyEvent.VK_UP:
                control.fwd();
                break;
            case KeyEvent.VK_DOWN:
                control.back();
                break;
            case KeyEvent.VK_LEFT:
                control.left();
                break;
            case KeyEvent.VK_RIGHT:
                control.right();
                break;

        }
        lastKeyCode=evt.getKeyCode();
//        System.out.println("got key code " + evt.getKeyCode());
    }

    void keyReleased(KeyEvent evt) {
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
                control.coast();
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                control.straight();
                break;
        }
        lastKeyCode=0;
    }

    private void panel1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_panel1KeyReleased
        keyReleased(evt);
    }//GEN-LAST:event_panel1KeyReleased

    private void panel1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_panel1KeyPressed
        keyPressed(evt);        // TODO add your handling code here:
    }//GEN-LAST:event_panel1KeyPressed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new ZipZapControlGUI().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel panel1;
    private javax.swing.JTextField statusTextField;
    // End of variables declaration//GEN-END:variables

    public void exceptionOccurred(Exception x, Object source) {
        statusTextField.setText(x.getMessage());
    }
}
