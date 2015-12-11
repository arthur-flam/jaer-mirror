package ch.unizh.ini.jaer.projects.rbodo.opticalflow;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import static net.sf.jaer.eventprocessing.EventFilter.log;

/**
 * This class computes and prints several objects of interest when evaluating
 * optical flow performance: The average angular error, (relative) average end-
 * point error, processing time, and event density (fraction of events that
 * successfully passed the filter). It also computes global motion averages 
 * (translation, rotation and expansion).
 * For global translation, rotation and expansion we average over all the 
 * individual translation (expansion, rotation) values of one packet. 
 * We compute the Standard Deviation (not Standard Error) because we are 
 * interested in how much the individual translation values deviate from 
 * the mean to be able to insure that our averaging was justified.
 * However, an even better indicator would be the robust measure MAD
 * (Median Absolute Deviation), because the Standard Deviation is heavily
 * influenced by outliers.
 * The Standard Error (Standard Deviation devided by Sqrt(N)) indicates 
 * how far the sample estimate of the mean is away from the true population 
 * mean; it will converge to zero for large sample sizes. 
 * The SD is a measure of how much we can expect the sample individuals 
 * to differ from the sample mean; it will converge towards the population
 * standard deviation for larger sample sizes.
 * @author rbodo
 */
public class MotionFlowStatistics {
    GlobalMotion globalMotion;
    AngularError angularError;
    EndpointErrorAbs endpointErrorAbs;
    EndpointErrorRel endpointErrorRel;
    ProcessingTime processingTime;
    EventDensity eventDensity;
    
    // For logging.
    private static String filename;
    private final DateFormat DATE_FORMAT;
    PrintStream logStream;

    private float timeslice;

    // Number of packets to skip before logging.
    private int warmupCounter;
    
    private final String filterClassName;

    protected MotionFlowStatistics(String filterClassName, int sX, int sY) {
        DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        reset(sX,sY);
        this.filterClassName = filterClassName;
    }

    protected final void reset(int sX, int sY) {
        globalMotion = new GlobalMotion(sX,sY);
        angularError = new AngularError();
        endpointErrorAbs = new EndpointErrorAbs();
        endpointErrorRel = new EndpointErrorRel();
        processingTime = new ProcessingTime();
        eventDensity = new EventDensity();
        warmupCounter = 5;
    }

    protected void update(float vx, float vy, float v, float vxGT, float vyGT, float vGT) {
        if (warmupCounter > 0) {
            warmupCounter--;
            return;
        }
        angularError.update(vx,vy,v,vxGT,vyGT,vGT);
        endpointErrorAbs.update(vx,vy,v,vxGT,vyGT,vGT);
        endpointErrorRel.update(vx,vy,v,vxGT,vyGT,vGT);
    }
    
    protected void updatePacket(boolean measureProcessingTime, boolean showGlobalEnabled,
                                int countIn, int countOut) {
        eventDensity.update(countIn,countOut);
        if (measureProcessingTime) processingTime.update();
        if (showGlobalEnabled) globalMotion.bufferMean();
    }
    
    @Override public String toString() {
        return String.format("Motion Flow Statistics Summary: %n") + 
               eventDensity.toString() + globalMotion.toString() +
               processingTime.toString() + angularError.toString() + 
               endpointErrorAbs.toString() + endpointErrorRel.toString();
    }
    
    public class EventDensity {
        // Number of events in packet before and after filtering.
        private int packetIn, packetOut, totalIn, totalOut, nPackets;
    
        public float getPacketDensity() {return packetIn == 0 ? 0 : (float) 100*packetOut/packetIn;}
        
        public float getTotalDensity() {return totalIn == 0 ? 0 : (float) 100*totalOut/totalIn;}
        
        public void update(int pIn, int pOut) {
            packetIn = pIn;
            packetOut = pOut;
            totalIn += pIn;
            totalOut += pOut;
            nPackets++;
        }
        
        public void reset() {
            packetIn = 0;
            packetOut = 0;
            totalIn = 0;
            totalOut = 0;
            nPackets = 0;
        }
        
        @Override public String toString() {
            return String.format(Locale.ENGLISH,"%1$s: %2$4.2f%% %n", 
                getClass().getSimpleName(), getTotalDensity());
        }
    }
    
    public class GlobalMotion {
        float meanGlobalVx, sdGlobalVx, meanGlobalVy, sdGlobalVy, meanGlobalRotation, 
              sdGlobalRotation, meanGlobalExpansion, sdGlobalExpansion;
        private final Measurand globalVx, globalVy, globalRotation, globalExpansion;
        private int rx, ry;
        private int subSizeX, subSizeY;
    
        GlobalMotion(int sX, int sY) {
            subSizeX = sX;
            subSizeY = sY;
            globalVx = new Measurand();
            globalVy = new Measurand();
            globalRotation = new Measurand();
            globalExpansion = new Measurand();
        }
        
        void reset(int sX, int sY) {
            subSizeX = sX;
            subSizeY = sY;
            globalVy.reset();
            globalVy.reset(); 
            globalRotation.reset();
            globalExpansion.reset();
        }
        
        void update(float vx, float vy, float v, int x, int y) {
            // Translation
            if (v == 0) return;
            globalVx.update(vx);
            globalVy.update(vy);

            // Rotation
            // <editor-fold defaultstate="collapsed" desc="Comment">
            /**
             * Each event implies a certain rotational motion. The larger the
             * radius, the smaller the effect of a given local motion vector on
             * rotation. The contribution to rotational motion is computed by
             * dot product between tangential vector (which is closely related
             * to radial vector) and local motion vector.
             * If (vx,vy) is the local motion vector, (rx,ry) the radial vector
             * (from center of rotation), and (tx,ty) the tangential *unit*
             * vector, then the tangential velocity is computed as v.t=rx*tx+ry*ty.
             * The tangential vector is given by dual of radial vector:
             * tx=-ry/r, ty=rx/r, where r is length of radial vector.
             * Thus tangential comtribution is given by v.t/r=(-vx*ry+vy*rx)/r^2.
             * (Tobi)
             */ 
            // </editor-fold>
            rx = x - subSizeX/2; 
            ry = y - subSizeY/2;
            if (rx == 0 && ry == 0) return; // Don't add singular event at origin.
            globalRotation.update((float) (180/Math.PI)*(vy*rx - vx*ry)/(rx*rx + ry*ry));

            // Expansion
            // <editor-fold defaultstate="collapsed" desc="Comment">
            /*
             * Each event implies a certain expansion contribution.
             * Velocity components in the radial direction are weighted by radius;
             * events that are close to the origin contribute more to expansion
             * metric than events that are near periphery. The contribution to
             * expansion is computed by dot product between radial vector
             * and local motion vector.
             * If vx,vy is the local motion vector and rx,ry the radial vector
             * (from center of rotation) then the radial velocity is computed
             * as v.r/r.r=(vx*rx+vy*ry)/(rx*rx+ry*ry), where r is radial vector.
             * Thus in scalar units, each motion event contributes v/r to the metric.
             * This metric is exactly 1/Tcoll with Tcoll=time to collision.
             * (Tobi)
             */
            // </editor-fold>
            if (rx > -2 && rx < 2 && ry > -2 && ry < 2) return; // Don't add singular event at origin.
            globalExpansion.update((vx*rx + vy*ry)/(rx*rx + ry*ry));
        }

        void bufferMean() {
            meanGlobalVx = globalVx.getMean();
            sdGlobalVx = globalVx.getStdErr();
            meanGlobalVy = globalVy.getMean();
            sdGlobalVy = globalVy.getStdErr();
            meanGlobalRotation = globalRotation.getMean();
            sdGlobalRotation = globalRotation.getStdErr();
            meanGlobalExpansion = globalExpansion.getMean();
            sdGlobalExpansion = globalExpansion.getStdErr();

            // Call resets here because global motion should in general not
            // be averaged over more than one packet.
            globalVx.reset();
            globalVy.reset();
            globalRotation.reset();
            globalExpansion.reset();
        }
        
        @Override public String toString() {
            return String.format(Locale.ENGLISH,"Global velocity: " +
                "[%1$4.2f, %2$4.2f] +/- [%3$4.2f, %4$4.2f] pixel/s, global rotation: " +
                "%5$4.2f +/- %6$2.2f °/s %n", meanGlobalVx, meanGlobalVy, sdGlobalVx,
                sdGlobalVy, meanGlobalRotation, sdGlobalRotation);
        }
    }
    
    public class AngularError extends Measurand {
        private final Histogram histogram;
        private final int NUM_BINS = 20;
        private final int SIZE_BINS = 10;
        private final int START = 3;
        private final float X1 = 3;
        private final float X2 = 10;
        private final float X3 = 30;
        private float tmp;
        
        public AngularError() {histogram = new Histogram(START, NUM_BINS, SIZE_BINS);}
        
        @Override public void reset() {
            super.reset();
            histogram.reset();
        }
     
        // <editor-fold defaultstate="collapsed" desc="Comment">
        /** 
         * Returns the angle between the observed optical flow and ground truth.
         * The case that either one or both v and vGT are zero is unnatural 
         * because in principle every event should be the result of motion (in this context).
         * So we skip it by returning 181 (which is large so that the 
         * event is still filtered out when calculateAngularError() is called in 
         * the context of discarding outliers during filterPacket). When updating 
         * PacketStatistics, we detect 181 and skip updating the statistics.
         */
        // </editor-fold> 
        float calculateError(float vx, float vy, float v, float vxGT, float vyGT, float vGT) {
            if (v == 0 || vGT == 0) return 181;
            tmp = (vx*vxGT + vy*vyGT)/(v*vGT);
            if (tmp >  1) return 0; // Can happen due to roundoff error.
            if (tmp < -1) return 180;
            return (float) (Math.acos(tmp)*180/Math.PI);
        }

        // Calculates the angular error of a single event and adds it to the sample.
        void update(float vx, float vy, float v, float vxGT, float vyGT, float vGT) {
            tmp = calculateError(vx,vy,v,vxGT,vyGT,vGT);
            if (tmp == 181) return;
            update(tmp);
            histogram.update(tmp);
        }
        
        @Override public String toString() {
            return String.format(Locale.ENGLISH,"%1$s: %2$4.2f +/- %3$5.2f °, %4$s, " +
                "percentage above %5$2f °: %6$4.2f%%, above %7$2f °: %8$4.2f%%, above %9$2f °: %10$4.2f%% %n", 
                getClass().getSimpleName(), getMean(), getStdDev(), histogram.toString(), 
                X1, histogram.getPercentageAboveX(X1),
                X2, histogram.getPercentageAboveX(X2),
                X3, histogram.getPercentageAboveX(X3));
        }
    }
            
    public class EndpointErrorAbs extends Measurand {
        private final Histogram histogram;
        private final int NUM_BINS = 20;
        private final int SIZE_BINS = 10;
        private final int START = 1;
        private final float X1 = 1;
        private final float X2 = 10;
        private final float X3 = 20;
        private float tmp;
        
        EndpointErrorAbs() {histogram = new Histogram(START, NUM_BINS, SIZE_BINS);}
        
        @Override public void reset() {histogram.reset();}
        
        void update(float vx, float vy, float v, float vxGT, float vyGT, float vGT) {
            if (v == 0 || vGT == 0) return;
            tmp = (float) Math.sqrt((vx-vxGT)*(vx-vxGT) + (vy-vyGT)*(vy-vyGT));
            update(tmp);
            histogram.update(tmp);
        }
        
        @Override public String toString() {
            return String.format(Locale.ENGLISH,"%1$s: %2$4.2f +/- %3$5.2f pixels/s, " +
                "%4$s, percentage above %5$4.2f pixels/s: %6$4.2f%%, above %7$4.2f " +
                "pixels/s: %8$4.2f%%, above %9$4.2f pixels/s: %10$4.2f%% %n",
                getClass().getSimpleName(), 
                getMean(), getStdDev(), histogram.toString(), 
                X1, histogram.getPercentageAboveX(X1),
                X2, histogram.getPercentageAboveX(X2),
                X3, histogram.getPercentageAboveX(X3));
        }
    }
    
    public class EndpointErrorRel extends Measurand {
        private final Histogram histogram;
        private final int NUM_BINS = 20;
        private final int SIZE_BINS = 100;
        private final int START = 2;
        private float tmp;
        
        EndpointErrorRel() {histogram = new Histogram(START, NUM_BINS, SIZE_BINS);}
        
        @Override public void reset() {histogram.reset();}
        
        void update(float vx, float vy, float v, float vxGT, float vyGT, float vGT) {
            if (v == 0 || vGT == 0) return;
            tmp = (float) Math.sqrt((vx-vxGT)*(vx-vxGT) + (vy-vyGT)*(vy-vyGT))*100/vGT;
            update(tmp);
            histogram.update(tmp);
        }
        
        @Override public String toString() {
            return String.format(Locale.ENGLISH,"%1$s: %2$4.2f +/- %3$5.2f%%, %4$s %n",
                getClass().getSimpleName(), getMean(), getStdDev(), histogram.toString());
        }
    }
    
    public class ProcessingTime extends Measurand {
        // Processing time in microseconds averaged over packet.
        private float meanProcessingTimePacket;
        
        // Start time of packet filtering.
        long startTime; 
        
        // Number of bins to cluster packets according to input density.
        // keps = 1000 events per second, epp = events per packet.
        private final int kepsBins = 10;
        private final int kepsIncr = 150;
        private final int kepsInit = 50;
        private final Measurand[] processingTimeEPS;

        private int i;
        
        protected ProcessingTime() {
            processingTimeEPS = new Measurand[kepsBins];
            for (i = 0; i < kepsBins; i++) processingTimeEPS[i] = new Measurand();
        }

        @Override public void reset() {
            super.reset();
            for (i = 0; i < kepsBins; i++) processingTimeEPS[i].reset();
            meanProcessingTimePacket = 0;
        }
        
        void update() {
            if (warmupCounter > 0) {
                warmupCounter--;
                return;
            }
            meanProcessingTimePacket = (System.nanoTime()-startTime)*1e-3f/eventDensity.packetIn;
            update(meanProcessingTimePacket);
            for (i = 0; i < kepsBins; i++)
                if (eventDensity.packetIn < (kepsIncr*i+kepsInit)*timeslice) {
                    processingTimeEPS[i].update(meanProcessingTimePacket);
                    break;
                }
        }

        // Opens the file and prints the header to it.
        void openLog(final String loggingFolder) {
            try {
                filename = loggingFolder + "/ProcessingTime_" + filterClassName + 
                           DATE_FORMAT.format(new Date()) + ".txt";
                logStream = new PrintStream(new BufferedOutputStream(
                            new FileOutputStream(new File(filename))));
                log.log(Level.INFO,"Created motion flow logging file with " +
                                   "processing time statistics at {0}",filename);
                logStream.println("Processing time statistics of motion flow " +
                                  "calculation, averaged over event packets.");
                logStream.println("Date: " + new Date());
                logStream.println("Filter used: " + filterClassName);
                logStream.println();
                logStream.println("timestamp [us] | processing time [us]");
            } catch (FileNotFoundException ex) {log.log(Level.SEVERE, null, ex);}
        }

        // Closes the file and nulls the stream.
        void closeLog(final String loggingFolder, int searchDistance) {
            if (logStream != null) {
                logStream.flush();
                logStream.close();
                logStream = null;
                log.log(Level.INFO, "Closed log data file {0}", filename);
            }
            try {
                filename = loggingFolder + "/SummaryProcessingTime_" + 
                           filterClassName + DATE_FORMAT.format(new Date()) + ".txt";
                logStream = new PrintStream(new BufferedOutputStream(
                            new FileOutputStream(new File(filename))));
                logStream.println("Summary of processing time statistics.");
                logStream.println("Date: " + new Date());
                logStream.println("Filter used: " + filterClassName);
                logStream.println("Search distance: " + searchDistance);
                logStream.println("Time slice [ms]: " + timeslice);
                logStream.println();
                for (i = 0; i < kepsBins; i++) 
                    if (processingTimeEPS[i].n > 0) 
                        logStream.println(processingTimeEPS[i] + " @ " + 
                            (int) ((kepsIncr*i+kepsInit)*timeslice) + " events/packet");
                } catch (FileNotFoundException ex) {log.log(Level.SEVERE, null, ex);}
            if (logStream != null) {
                logStream.flush();
                logStream.close();
                logStream = null;
                log.log(Level.INFO, "Closed log data file {0}", filename);
            }
        }

        void log(int ts, int tsFirst, int tsLast, int countIn, int countOut) {
            timeslice = (tsLast-tsFirst)*1e-3f;
            if (timeslice <= 0) return;
            update();
            if (logStream != null) logStream.printf(
                Locale.ENGLISH,"%1$12d %2$11.2f %n",ts,meanProcessingTimePacket);
        }
        
        @Override public String toString() {
            return String.format(Locale.ENGLISH,"%1$s: %2$4.2f +/- %3$5.2f us/event %n",
                getClass().getSimpleName(), getMean(), getStdDev());
        }
    }
}
