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
class MeasurmentEvaluator implements ParticleEvaluator<SimpleParticle, Double> {
	double	mu = 0;
	double	sigma	= 20;
	double noise = 0.0;
	int type = 0;
	Random r = new Random();

        public void setMu(double mu) {
            this.mu = mu;
        }

        public double getMu() {
            return mu;
        }

	public Double  evaluate(SimpleParticle p) {
		double x = p.x;
		double error = r.nextDouble()*noise;
		double result = 0;
		switch(type) {
		case 0: result = gaussian(x, mu, sigma); break;
		case 1: result = Math.max(gaussian(x, mu, sigma), gaussian(x, -mu, sigma)); break;
		case 2: result = Math.max(gaussian(x, mu, sigma), 0.9*gaussian(x, -mu, sigma)); break;
		case 3: result = Math.max(((Math.abs(mu-x))<sigma/5)?1:0, (Math.abs(-mu-x)<sigma/5)?0.5:0); break;
		}
		return result + error; 
	}
	
	public static double gaussian(double x, double mu, double sigma) {
		double d2 = (x - mu) * (x - mu);
		return Math.exp(-d2 / sigma);
	}
	
}
