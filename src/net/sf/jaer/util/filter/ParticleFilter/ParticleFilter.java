/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.jaer.util.filter.ParticleFilter;

/**
 *
 * @author minliu and hongjie
 */

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class ParticleFilter<T extends Particle> {
	private ParticleEvaluator<T, Double> estimateEvaluator; 
        private ParticleEvaluator<T, T> dynamicEvaluator;
        private ParticleEvaluator<T, Point2D.Double> averageEvaluator;
        
	private ArrayList<ParticleWeight<T> > particles = new ArrayList<ParticleWeight<T> >();


	private double[] selectionSum = new double[0];
	private int nextParticleCount=0;
	private boolean useWeightRatio = false;
	private boolean recalculateWeightAfterDrift = false;
	private Comparator<ParticleWeight<T> > strengthComparator = new Comparator<ParticleWeight<T> >() {
		public int compare(ParticleWeight<T> arg0, ParticleWeight<T> arg1) {
			double s0 = getSelectionWeight(arg0);
			double s1 = getSelectionWeight(arg1);
			if( s0 < s1) return -1;
			else if( s0 > s1) return +1;
			else return 0;
		}
	};
        
        public ArrayList<ParticleWeight<T>> getParticles() {
            return particles;
        }
    
	public ParticleFilter(ParticleEvaluator<T, T> dynamic, ParticleEvaluator<T, Double> measurement, ParticleEvaluator<T, Point2D.Double> average) {
		this.estimateEvaluator = measurement;
                this.dynamicEvaluator = dynamic;
                this.averageEvaluator = average;
	}
        
	public void addParticle(T p) {
		this.particles.add(new ParticleWeight<T>(p));
		nextParticleCount++;
	}

	public int getParticleCount() {
		return particles.size();
	}

	public T get(int i) {
		return particles.get(i).data;
	}

	public void evaluateStrength() {
		for(ParticleWeight<T> p : this.particles) {
                        p.data = dynamicEvaluator.evaluate(p.data);           // Generate the proposal distribution by the motion model.
			double weight = estimateEvaluator.evaluate(p.data);   // Evaluate it with the measurement value.
			if( p.lastWeight == 0 ) {
				p.weightRatio = weight;
			} else {
				p.weightRatio = weight / p.lastWeight;
			}
			p.weight = weight;
		}
	}
        
        // If we don't need resample, then we should update the weight.
        public void updateWeight() {
            for(int i = 0; i < particles.size(); i++) {
                ParticleWeight<T> p = particles.get(i);
                p.weight = p.weight * p.lastWeight;
                p.lastWeight = p.weight;
            }
        }

	@SuppressWarnings("unchecked")
	public void resample(Random r) {
                this.prepareResampling();
 
                int[] selectionDistribution = new int[this.particles.size()];
                ArrayList<ParticleWeight<T> > nextDistribution = new ArrayList<ParticleWeight<T> >();
                Charset charset = Charset.forName("US-ASCII");
                String s = "Hello!";
                Path file = Paths.get("E:/DVS/databases/PF Tracking/dataset/test");
                for(int i = 0; i < nextParticleCount; i++) {
                        double sel = r.nextDouble();
                        int index = Arrays.binarySearch(this.selectionSum, sel);
                        if( index < 0 ) {
                                index = -(index+1);
                        }

                        ParticleWeight<T> p = particles.get(index);
                        ParticleWeight<T> particleWeight = new ParticleWeight<T>((T)p.data.clone(), p.weight, selectionDistribution[index]);
                        if(selectionDistribution[index] >= 7) {
                            if(p.weight > 0.5) {
                                System.out.println("Weight is:");
                                System.out.println(p.weight);
                                System.out.println("Index is:");
                                System.out.println(selectionDistribution[index]);                                
                            }
                        }
 
                        try (BufferedWriter writer = Files.newBufferedWriter(file, charset)) {
                            writer.write(s);
                            writer.close();
                        } catch (IOException x) {
                            System.err.format("IOException: %s%n", x);
                        }
                        
                        nextDistribution.add(particleWeight);
                        selectionDistribution[index]++;
                }
//		System.out.println();
                this.particles = nextDistribution;     
                // disperseDistribution(r, 1);
                              
	}

	private double prepareResampling() {
		Collections.sort(this.particles, strengthComparator);
		this.selectionSum = new double[getParticleCount()];
		double sum = 0;
		for(int i = 0; i < particles.size(); i++) {
			ParticleWeight<T> p = particles.get(i);
			sum += getSelectionWeight(p);
			this.selectionSum[i] = sum;
		}
		return sum;
	}

        public double calculateNeff() {
            double retVal = 0;
            for(int i = 0; i < particles.size(); i++) {
                ParticleWeight<T> p = particles.get(i);
                double weight = p.weight;
                retVal += weight * weight;
            }
            return 1/retVal;
        }
        
	public void disperseDistribution(Random r, double spread) {
		for(ParticleWeight<T> p : this.particles) {
			// do not add error to one copy of the particle
			if( p.copyCount > 0 ) {
				p.data.addNoise(r, spread);
				if( recalculateWeightAfterDrift ) {
					// The weight ratio depends on small changes in strength after noise is added.
					// The filter can be made more accurate by finding the exact strength of the new particle for the previous timestep.
					p.lastWeight = this.estimateEvaluator.evaluate(p.data);
				}
			}
		}
	}
	
	public void setParticleCount(int value) {
		this.nextParticleCount = value;
	}

        public double normalize() {
            double originSum = 0;

            for(int i = 0; i < nextParticleCount; i++) {
                ParticleWeight<T> p = particles.get(i);
                originSum += getSelectionWeight(p);
            }

            for(int i = 0; i < nextParticleCount; i++) {
                ParticleWeight<T> p = particles.get(i);
                p.weight = p.weight/originSum;
            }
            return originSum;
        }
        
	public boolean isUsingWeightRatio() { return useWeightRatio; }
	public void useWeightRatio(boolean b) { useWeightRatio = b; }
	
	private double getSelectionWeight(ParticleWeight<T> p) {
		if( useWeightRatio )	return p.weightRatio;
		else return p.weight;
	}
	
	private static class ParticleWeight<T extends Particle> implements Cloneable {
		public ParticleWeight(T p) {
			this(p, 1.0, 0);
		}
		public ParticleWeight(T p, double lastWeight, int copyCount) {
			this.data = p;
			this.lastWeight = lastWeight;
			this.weight = 1.0;
			this.weightRatio = 1.0;
			this.copyCount = copyCount;
		}
		T data;
		double lastWeight;
		double weight;
		double weightRatio;
		int copyCount;
	}

        public double getAverageX() {
            double sumX = 0;
            for(ParticleWeight<T> p : this.particles) {
                    sumX += averageEvaluator.evaluate(p.data).x;
            }
            return sumX/particles.size();
        }
        
        public double getAverageY() {
            double sumY = 0;
            for(ParticleWeight<T> p : this.particles) {
                    sumY += averageEvaluator.evaluate(p.data).y;
            }
            return sumY/particles.size();            
        }
        
	public void setEvaluator(ParticleEvaluator<T, T>dynamic, ParticleEvaluator<T, Double> measurement) {
		this.estimateEvaluator = measurement;
                this.dynamicEvaluator = dynamic;
        }

	public void setReevaluateAfterNoise(boolean b) {
		this.recalculateWeightAfterDrift = b;
	}
	
}

