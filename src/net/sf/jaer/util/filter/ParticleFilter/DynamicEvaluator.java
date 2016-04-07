/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.jaer.util.filter.ParticleFilter;

import java.util.Random;

/**
 *
 * @author minliu and hongjie
 */
public class DynamicEvaluator implements ParticleEvaluator<SimpleParticle, double[]> {
    private double noise = 5;
    private Random r = new Random();

    @Override
    public double[] evaluate(SimpleParticle p) {
        double x = p.getX();
        double y = p.getY();
        double[] retVal = new double[2];
        double errorX = (r.nextDouble() * 2 - 1) * noise;
        double errorY = (r.nextDouble() * 2 - 1) * noise;
        retVal[0] = x + 0 + errorX;
        retVal[1] = y + 0 + errorY;
        return retVal;
    }    
}
