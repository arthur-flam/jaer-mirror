package ch.unizh.ini.jaer.projects.rbodo.opticalflow;

import eu.seebetter.ini.chips.davis.imu.IMUSample;
import java.util.Iterator;
import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.ApsDvsEvent;
import net.sf.jaer.event.ApsDvsEventPacket;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.event.PolarityEvent;

/**
 * Draws individual optical flow vectors and computes global motion, 
 * rotation and expansion, based upon motion estimate from IMU gyro sensors.
 * This assumes that the objects seen by the camera are stationary and the only motion is the motion field caused by
 * pure camera rotation.
 * @author rbodo
 */

@Description("Class for amplitude and orientation of local motion optical flow using IMU gyro sensors.")
@DevelopmentStatus(DevelopmentStatus.Status.Experimental)
public class ImuFlow extends AbstractMotionFlowIMU {   
    
    public ImuFlow(AEChip chip) {
        super(chip);
        numInputTypes = 2;
        resetFilter();
    }
    
    @Override synchronized public EventPacket filterPacket(EventPacket in) {
        setupFilter(in);
        
        Iterator i=null;
        if(in instanceof ApsDvsEventPacket){
            i=((ApsDvsEventPacket)in).fullIterator();
        }else{
            i=((ApsDvsEventPacket)in).inputIterator();
        }
        while(i.hasNext()){
            Object ein=i.next();
            ApsDvsEvent apsDvsEvent=(ApsDvsEvent)ein;
            if(apsDvsEvent.isApsData()) continue;
            extractEventInfo(ein);
            imuFlowEstimator.calculateImuFlow((ApsDvsEvent) inItr.next());
            if(apsDvsEvent.isImuSample()) continue;
            if (isInvalidAddress(0)) continue;
            if (isInvalidTimestamp()) continue;
            if (xyFilter()) continue;
            countIn++;
            vx = imuFlowEstimator.getVx();
            vy = imuFlowEstimator.getVy();
            v = imuFlowEstimator.getV();
            if (measureAccuracy || discardOutliersForStatisticalMeasurementEnabled) setGroundTruth();
            if (accuracyTests()) continue;
            //exportFlowToMatlab(2500000,2600000); // for IMU_APS_translSin
            //exportFlowToMatlab(1360000,1430000); // for IMU_APS_rotDisk
            //exportFlowToMatlab(295500000,296500000); // for IMU_APS_translBoxes
            writeOutputEvent();
            if (measureAccuracy) motionFlowStatistics.update(vx,vy,v,vxGT,vyGT,vGT);
        }
        motionFlowStatistics.updatePacket(countIn, countOut);
        return isShowRawInputEnabled() ? in : dirPacket;
    }
}