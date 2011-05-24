//
//  CartogramNewman.java
//  ScapeToad
//
//  Created by Christian on 29.10.08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//



package ch.epfl.scapetoad;

import java.io.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;



/**
 * Implementation of the diffusion algorithm of Gastner and Newman.
 * This version is a Java adaptation of Mark Newman's C code.
 */
public class CartogramNewman
{
	
	/**
	 * The cartogram grid on which we will apply the cartogram transformation.
	 */
	private CartogramGrid cartogramGrid;
	 
	/**
	 * The size of the diffusion grid.
	 */
	public Size gridSize = new Size(512, 512);
	
	/**
	 * The cartogram bounding box. It is slightly larger than the
	 * grid bounding box for computational reasons.
	 */
	private Envelope extent;
	
	
	/**
	 * Pop density at time t (five snaps needed)
	 */
	private double[][][] rhot;
	
	/**
	 * FT of initial density
	 */
	private double[][] fftrho;
	//private ComplexArray fftrho;
	
	/**
	 * FT of density at time t
	 */
	//private ComplexArray fftexpt;
	private double[][] fftexpt;
	
	/**
	 * Array needed for the Gaussian convolution
	 */
	private double[] expky;
	
	
	/**
	 * Array for storing the grid points
	 */
	private double[] gridPointsX;
	private double[] gridPointsY;
	
	
	/** 
	 * Initial size of a time step
	 */
	public static double INITH = 0.001;
	
	/**
	 * Desired accuracy per step in pixels
	 */
	public static double TARGETERROR = 0.01;
	
	/**
	 * Max ratio to increase step size by
	 */
	public static double MAXRATIO = 4.0;
	
	
	/**
	 * Guess of the time it will take. Only used for the completion estimation.
	 */
	private static double EXPECTEDTIME = 100000000.0f;
	
	/**
	 *  The maximum integration error found for any polygon 
	 *	vertex for the complete two-step integration process.
	 */
	private double errorp;
	
	/**
	 * Maximum distance moved by any point during an integration step.
	 */
	private double drp;

	
	
	
	
	/**
	 * The Cartogram Wizard is used to update the running status.
	 * If NULL, no running status update is done.
	 */
	public CartogramWizard runningStatusWizard;
	
	/**
	 * The value of the running status at the begin of the cartogram computation.
	 */
	public int runningStatusMinimumValue;
	
	/**
	 * The value of the running statut at the end of the cartogram computation.
	 */
	public int runningStatusMaximumValue;
	
	/**
	 * The main string of the running status which is set by the caller.
	 * This string will be displayed in the first line of the running status wizard.
	 * The second line is freely used by this class.
	 */
	public String runningStatusMainString;
	
	
	
	
	
	
	
	/**
	 * Constructor for the CartogramNewman class.
	 */
	public CartogramNewman (CartogramGrid g)
	{
		this.cartogramGrid = g;
		this.gridSize = g.getGridSize();
		this.gridSize.x--;
		this.gridSize.y--;
	}
	
	
	
	/**
	 * Set the diffusion grid size.
	 */
	/*public void setGridSize (int x, int y)
	{
		this.gridSize = new Size(x,y);
	}*/
	
	
	
	
	
	/**
	 * Starts the cartogram computation.
	 */
	public void compute () throws Exception {
		
		// Allocate space for the cartogram code to use
		this.initializeArrays();
		
		// Read in the population data, and store it in fftrho.
		this.computeInitialDensity();
		
		// Create the grid of points.
		this.createGridOfPoints();
		
		// Compute the cartogram.
		this.makeCartogram(0.0);
		
		// Project the cartogram grid.
		this.projectCartogramGrid();
		
		//this.writeGridOfPointsToFile("/Temp/gridOfPoints.txt");
	}
	
	
	
	
	
	
	
	
	/**
	 * Private method for initializing the class arrays.
	 */
	private void initializeArrays ()
		throws InterruptedException
	{
		try
		{
			this.rhot = new double[5][this.gridSize.x][this.gridSize.y];
			this.fftrho = new double[this.gridSize.x][this.gridSize.y];
			this.fftexpt = new double[this.gridSize.x][this.gridSize.y];
			this.expky = new double[this.gridSize.y];
		}
		catch (Exception e)
		{
			System.out.println("Out of memory error.");
			throw new InterruptedException("Out of memory. Use a smaller cartogram grid or increase memory.");
		}
	}
	
	
	
	
	/**
	 * Method to read population data,
	 * transform it, and store it in fftrho.
	 */
	private void computeInitialDensity ()
	{
		// Read population density into fftrho.
		this.readPopulationDensity();
		
		// Transform fftrho.
		DoubleDCT_2D transform = new DoubleDCT_2D(this.gridSize.x, this.gridSize.y);
		transform.forward(fftrho, false);
	}
	
	
	
	
	/**
	 * Reads the population density into fftrho.
	 */
	private void readPopulationDensity () {
		// Copy the cartogram bounding box.
		this.extent = this.cartogramGrid.envelope();
		
		// Fill the diffusion grid using the cartogram grid values.
		double[][] cgridx = this.cartogramGrid.getXCoordinates();
		double[][] cgridy = this.cartogramGrid.getYCoordinates();
		double[][] cgridv = this.cartogramGrid.getCurrentDensityArray();
		this.fillDiffusionGrid(cgridx, cgridy, cgridv);
	}
	
	
	

	
	
	
	// Fills fftrho using the provided grid values.
	private void fillDiffusionGrid (double[][] x, double[][] y, double[][] v) {
		for (int i = 0; i < this.gridSize.x; i++) {
			for (int j = 0; j < this.gridSize.y; j++) {
				this.fftrho[i][j] = v[i][j];				
			}
		}
	}
	
	
	

	
	
	
	/**
	 * Create the grid of points (store the coordinates of the diffusion
	 * grid points).
	 */
	private void createGridOfPoints ()
	{
		this.gridPointsX = new double[(this.gridSize.x+1) * (this.gridSize.y+1)];
		this.gridPointsY = new double[(this.gridSize.x+1) * (this.gridSize.y+1)];
		
		int i = 0;
		double ix, iy;
		
		for (iy = 0.0f; iy <= this.gridSize.y; iy++)
		{
			for (ix = 0.0f; ix <= this.gridSize.x; ix++)
			{
				this.gridPointsX[i] = ix;
				this.gridPointsY[i] = iy;
				i++;
			}
		}
		
	}
	
	
	
	
	
	/**
	 * Writes the grid of points into a text file.
	 */
	private void writeGridOfPointsToFile (String fileName) {
		
		FileOutputStream out;
		PrintStream p;
		
		try
		{
			out = new FileOutputStream(fileName);
			p = new PrintStream(out);
			
			int i = 0;
			for (int iy = 0; iy <= this.gridSize.y; iy++)
			{
				for (int ix = 0; ix <= this.gridSize.x; ix++)
				{
					p.printf("%f %f\n", this.gridPointsX[i], this.gridPointsY[i]);
					i++;
				}
			}
			
			p.close();
		}
		catch (Exception e)
		{
			System.err.println ("[CartogramNewman.writeGridOfPointsToFile] Error writing to file.");
		}

	}
	
	
	
	
		
	
	/**
	 * Do the transformation of the given set of points to the cartogram
	 */
	private void makeCartogram (double blur)
	{
		
		// Calculate the initial density for snapshot zero */
		int s = 0;
		this.densitySnapshot(0.0, s);

		// Integrate the points.
		int step = 0;
		double t = 0.5 * blur * blur;
		double h = this.INITH;
		
		int sp;
		this.drp = 1.0f;
		do
		{
			
			// Do a combined (triple) integration step
			sp = this.integrateTwoSteps(t, h, s);
			
			// Increase the time by 2h and rotate snapshots
			t += 2.0 * h;
			step += 2;
			s = sp;
			
			// Adjust the time-step.
			// Factor of 2 arises because the target for the two-step process is 
			// twice the target for an individual step
			double desiredratio = Math.pow((2 * this.TARGETERROR / this.errorp), 0.2);
			
			if (desiredratio > this.MAXRATIO)
			{
				h *= this.MAXRATIO;
			}
			else
			{
				h *= desiredratio;
			}
			
			
			this.updateRunningStatus(t);
			
			
		} while (this.drp > 0.0f);	// If no point moved then we are finished
			
		
	}
		
	
	
	
	
	
	/** 
	 * Function to calculate the population density at arbitrary time by back-
	 * transforming and put the result in a particular rhot[] snapshot array.
	 */
	private void densitySnapshot (double t, int s)
	{
		int ix, iy;
		double kx, ky;
		double expkx;
		
		// Calculate the expky array, to save time in the next part
		for (iy = 0; iy < this.gridSize.y; iy++)
		{
			ky = Math.PI * iy / this.gridSize.y;
			this.expky[iy] = Math.exp(-ky * ky * t);
		}
		
		// Multiply the FT of the density by the appropriate factors
		for (ix = 0; ix < this.gridSize.x; ix++)
		{
			kx = Math.PI * ix / this.gridSize.x;
			expkx = Math.exp(-kx * kx * t);
			for (iy = 0; iy < this.gridSize.y; iy++)
			{
				this.fftexpt[ix][iy] = expkx * this.expky[iy] * this.fftrho[ix][iy];
				
				// Save a copy to the rhot[s] array on which we will perform the
				// DCT back-transform.
				this.rhot[s][ix][iy] = this.fftexpt[ix][iy];
			}
		}
		
		// Perform the back-transform
		DoubleDCT_2D dct = new DoubleDCT_2D(this.gridSize.x, this.gridSize.y);
		dct.inverse(this.rhot[s], false);
		
	}
	
	
	
	
	/* Integrates 2h time into the future two different ways using
	 * fourth-order Runge-Kutta and compare the differences for the purposes of
	 * the adaptive step size.
	 * @param t the current time, i.e., start time of these two steps
	 * @param h delta t
	 * @param s snapshot index of the initial time
	 * @return the snapshot index for the final function evaluation
	 */
	private int integrateTwoSteps (double t, double h, int s)
	{
				
		int s0 = s;
		int s1 = (s + 1) % 5;
		int s2 = (s + 2) % 5;
		int s3 = (s + 3) % 5;
		int s4 = (s + 4) % 5;
		
		
		// Compute the density field for the four new time slices.
		this.densitySnapshot((t + 0.5 * h), s1);
		this.densitySnapshot((t + 1.0 * h), s2);
		this.densitySnapshot((t + 1.5 * h), s3);
		this.densitySnapshot((t + 2.0 * h), s4);
		
		
		// Do all three Runga-Kutta steps for each point in turn.
		double esqmax = 0.0;
		double drsqmax = 0.0;
		int npoints = (this.gridSize.x + 1) * (this.gridSize.y + 1);
		for (int p = 0; p < npoints; p++)
		{
			double rx1 = this.gridPointsX[p];
			double ry1 = this.gridPointsY[p];
			
			
			// Do the big combined (2h) Runga-Kutta step.
			
			Coordinate v1 = this.velocity(rx1, ry1, s0);
			double k1x = 2 * h * v1.x;
			double k1y = 2 * h * v1.y;
			Coordinate v2 = this.velocity((rx1 + 0.5*k1x), (ry1 + 0.5*k1y), s2);
			double k2x = 2 * h * v2.x;
			double k2y = 2 * h * v2.y;
			Coordinate v3 = this.velocity((rx1 + 0.5*k2x), (ry1 + 0.5*k2y), s2);
			double k3x = 2 * h * v3.x;
			double k3y = 2 * h * v3.y;
			Coordinate v4 = this.velocity((rx1 + k3x), (ry1 + k3y), s4);
			double k4x = 2 * h * v4.x;
			double k4y = 2 * h * v4.y;
			
			double dx12 = (k1x + k4x + 2.0*(k2x + k3x)) / 6.0;
			double dy12 = (k1y + k4y + 2.0*(k2y + k3y)) / 6.0;
			
			
			// Do the first small Runge-Kutta step.
			// No initial call to the velocity method is done
			// because it would be the same as the one above, so there's no need
			// to do it again
			
			k1x = h * v1.x;
			k1y = h * v1.y;
			v2 = this.velocity((rx1 + 0.5*k1x), (ry1 + 0.5*k1y), s1);
			k2x = h * v2.x;
			k2y = h * v2.y;
			v3 = this.velocity((rx1 + 0.5*k2x), (ry1 + 0.5*k2y), s1);
			k3x = h * v3.x;
			k3y = h * v3.y;
			v4 = this.velocity((rx1 + k3x), (ry1 + k3y), s2);
			k4x = h * v4.x;
			k4y = h * v4.y;
			
			double dx1 = (k1x + k4x + 2.0*(k2x + k3x)) / 6.0;
			double dy1 = (k1y + k4y + 2.0*(k2y + k3y)) / 6.0;
			
			
			// Do the second small RK step
			
			double rx2 = rx1 + dx1;
			double ry2 = ry1 + dy1;
			
			v1 = this.velocity(rx2, ry2, s2);
			k1x = h * v1.x;
			k1y = h * v1.y;
			v2 = this.velocity((rx2 + 0.5*k1x), (ry2 + 0.5*k1y), s3);
			k2x = h * v2.x;
			k2y = h * v2.y;
			v3 = this.velocity((rx2 + 0.5*k2x), (ry2 + 0.5*k2y), s3);
			k3x = h * v3.x;
			k3y = h * v3.y;
			v4 = this.velocity((rx2 + k3x), (ry2 + k3y), s4);
			k4x = h * v4.x;
			k4y = h * v4.y;
			
			double dx2 = (k1x + k4x + 2.0*(k2x + k3x)) / 6.0;
			double dy2 = (k1y + k4y + 2.0*(k2y + k3y)) / 6.0;
			
			
			// Calculate the (squared) error.
			
			double ex = (dx1 + dx2 - dx12) / 15;
			double ey = (dy1 + dy2 - dy12) / 15;
			double esq = ex*ex + ey*ey;
			if (esq > esqmax)
			{
				esqmax = esq;
			}
			
			
			// Update the position of the vertex using the more accurate (two small steps) result, 
			// and deal with the boundary conditions.
			// This code does 5th-order "local extrapolation" (which just means taking the estimate of 
			// the 5th-order term above and adding it to our 4th-order result get a result accurate to 
			// the next highest order).
			
			double dxtotal = dx1 + dx2 + ex;   // Last term is local extrapolation
			double dytotal = dy1 + dy2 + ey;   // Last term is local extrapolation
			double drsq = dxtotal*dxtotal + dytotal*dytotal;
			if (drsq > drsqmax)
			{
				drsqmax = drsq;
			}
			
			
			double rx3 = rx1 + dxtotal;
			double ry3 = ry1 + dytotal;
			
			if (rx3 < 0)
			{
				rx3 = 0;
			}
			else if (rx3 > this.gridSize.x)
			{
				rx3 = this.gridSize.x;
			}
			
			if (ry3 < 0)
			{
				ry3 = 0;
			}
			else if (ry3 > this.gridSize.y)
			{
				ry3 = this.gridSize.y;
			}
			
			this.gridPointsX[p] = rx3;
			this.gridPointsY[p] = ry3;
			
		}
		
		this.errorp = Math.sqrt(esqmax);
		this.drp = Math.sqrt(drsqmax);
		
		return s4;
	}
		
		
	
	
	
	
	/* Computes the velocity at an arbitrary point from the grid velocities for a specified 
	 * snapshot by interpolating between grid points.
	 * If the requested point is outside the boundaries, we extrapolate (ensures smooth flow 
	 * back in if we get outside by mistake, although we should never actually do this because 
	 * the calling method integrateTwoSteps() contains code to prevent it).
	 * @param rx the x coordinate of the point for which we compute the velocity.
	 * @param ry the y coordinate of the point for which we compute the velocity.
	 * @param s the snapshot
	 * @return the velocity in x and y as a coordinate.
	 */
	private Coordinate velocity (double rx, double ry, int s)
	{
		
		// Deal with the boundary conditions.
		
		int ix = (int)rx;
		if (ix < 0)
		{
			ix = 0;
		}
		else if (ix >= this.gridSize.x)
		{
			ix = this.gridSize.x - 1;
		}
		
		int ixm1 = ix - 1;
		if (ixm1 < 0)
		{
			ixm1 = 0;
		}
		int ixp1 = ix + 1;
		if (ixp1 >= this.gridSize.x)
		{
			ixp1 = this.gridSize.x - 1;
		}
		
		int iy = (int)ry;
		if (iy < 0)
		{
			iy = 0;
		}
		else if (iy >= this.gridSize.y) 
		{
			iy = this.gridSize.y - 1;
		}
		
		int iym1 = iy - 1;
		if (iym1 < 0)
		{
			iym1 = 0;
		}
		int iyp1 = iy + 1;
		if (iyp1 >= this.gridSize.y) 
		{
			iyp1 = this.gridSize.y - 1;
		}
		
		
		// Calculate the densities at the nine surrounding grid points
		double rho00 = this.rhot[s][ixm1][iym1];
		double rho10 = this.rhot[s][ix  ][iym1];
		double rho20 = this.rhot[s][ixp1][iym1];
		double rho01 = this.rhot[s][ixm1][iy  ];
		double rho11 = this.rhot[s][ix  ][iy  ];
		double rho21 = this.rhot[s][ixp1][iy  ];
		double rho02 = this.rhot[s][ixm1][iyp1];
		double rho12 = this.rhot[s][ix  ][iyp1];
		double rho22 = this.rhot[s][ixp1][iyp1];

		
		// Calculate velocities at the four surrounding grid points
		
		double mid11 = rho00 + rho10 + rho01 + rho11;
		double vx11 = -2.0 * (rho10 - rho00 + rho11 - rho01) / mid11;
		double vy11 = -2.0 * (rho01 - rho00 + rho11 - rho10) / mid11;
		
		double mid21 = rho10 + rho20 + rho11 + rho21;
		double vx21 = -2.0 * (rho20 - rho10 + rho21 - rho11) / mid21;
		double vy21 = -2.0 * (rho11 - rho10 + rho21 - rho20) / mid21;
		
		double mid12 = rho01 + rho11 + rho02 + rho12;
		double vx12 = -2.0 * (rho11 - rho01 + rho12 - rho02) / mid12;
		double vy12 = -2.0 * (rho02 - rho01 + rho12 - rho11) / mid12;
		
		double mid22 = rho11 + rho21 + rho12 + rho22;
		double vx22 = -2.0 * (rho21 - rho11 + rho22 - rho12) / mid22;
		double vy22 = -2.0 * (rho12 - rho11 + rho22 - rho21) / mid22;
		
		
		// Calculate the weights for the bilinear interpolation
		
		double dx = rx - ix;
		double dy = ry - iy;
		
		double dx1m = 1.0 - dx;
		double dy1m = 1.0 - dy;
		
		double w11 = dx1m * dy1m;
		double w21 = dx * dy1m;
		double w12 = dx1m * dy;
		double w22 = dx * dy;
		
		
		// Perform the interpolation for x and y components of velocity
		
		double vxp = w11*vx11 + w21*vx21 + w12*vx12 + w22*vx22;
		double vyp = w11*vy11 + w21*vy21 + w12*vy12 + w22*vy22;
		
		Coordinate vp = new Coordinate(vxp, vyp);
		return vp;
		
	}
	
	
	
	
	
	
	
	private void projectCartogramGrid ()
	{
		
		// Project each point in the cartogram grid.
		double[][] x = this.cartogramGrid.getXCoordinates();
		double[][] y = this.cartogramGrid.getYCoordinates();
		
		int gridSizeX = x.length;
		int gridSizeY = x[0].length;
		
		double cellSizeX = this.extent.getWidth() / this.gridSize.x;
		double cellSizeY = this.extent.getHeight() / this.gridSize.y;
		
		double minX = this.extent.getMinX();
		double minY = this.extent.getMinY();
		
		int i, j;
		int index = 0;
		for (j = 0; j < gridSizeY; j++)
		{
			for (i = 0; i < gridSizeX; i++)
			{
				x[i][j] = this.gridPointsX[index] * cellSizeX + minX;
				y[i][j] = this.gridPointsY[index] * cellSizeY + minY;
				index++;
			}
		}

	}
	
	
	
	
	/**
	 * Estimates the computation progress in percentages and displays
	 * the value in the wizard. Works only if a wizard is defined,
	 * does nothing otherwise.
	 * @param t current time from the makeCartogram method.
	 */
	private void updateRunningStatus (double t)
	{
		if (runningStatusWizard != null)
		{
			int perc = (int)Math.round(100.0 * Math.log(t / INITH) / Math.log(EXPECTEDTIME / INITH));
			if (perc > 100) perc = 100;
			
			int res = (runningStatusMaximumValue - runningStatusMinimumValue) * perc / 100;
			res += runningStatusMinimumValue;
			runningStatusWizard.updateRunningStatus(res, runningStatusMainString, "Diffusion process: " + perc + "% done");
		}
	}
	
	
	
}










