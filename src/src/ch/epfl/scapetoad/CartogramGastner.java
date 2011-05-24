/*

	Copyright 2007-2009 361DEGRES

	This program is free software; you can redistribute it and/or
	modify it under the terms of the GNU General Public License as
	published by the Free Software Foundation; either version 2 of the
	License, or (at your option) any later version.

	This program is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
	02110-1301, USA.
	
*/



package ch.epfl.scapetoad;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;

import org.apache.commons.math.special.Erf;
import org.apache.commons.math.MathException;



/**
 * This class implements Gastner's diffusion algorithm.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-30
 */
public class CartogramGastner
{

	/**
	 * The cartogram grid which contains the density and which we 
	 * will deform.
	 */
	CartogramGrid mGrid;
	
	
	/**
	 * The cartogram bounding box. It is slightly larger than the
	 * grid bounding box for computational reasons.
	 */
	Envelope mExtent;
	
	
	/**
	 * Length of map in x direction. This is our grid size (number of cells).
	 * The number should always be a power of 2.
	 */
	private int lx;
	
	/**
	 * Length of map in y direction. This is our grid size (number of cells).
	 * The number should always be a power of 2.
	 */
	private int ly;
	
	
	/**
	 * Array for the density at time t = 0.
	 */
	private double[][] rho_0;

	/**
	 * Array for the density at time t > 0.
	 */
	private double[][] rho;

	
	/**
	 * Array for the velocity field in x direction at position (j,k).
	 */
	private double[][] gridvx;
	
	/**
	 * Array for the velocity field in y direction at position (j,k).
	 */
	private double[][] gridvy;
	
	
	/**
	 * Array for the x position at t > 0. x[j][k] is the x-coordinate for the
	 * element that was at position (j,k) at time t = 0.
	 */
	private double[][] x;
	
	/**
	 * Array for the y position at t > 0. y[j][k] is the y-coordinate for the
	 * element that was at position (j,k) at time t = 0.
	 */
	private double[][] y;
	
	
	// Arrays for the velocity field at position (x[j][k], y[j][k]).
	private double[][] vx;
	private double[][] vy;
	
	
	
	// Definition of some other class wide variables.
	private double minpop = 0.0;
	private int nblurs = 0;
	private double[][] xappr;
	private double[][] yappr;
	
	
	
	/**
	 * Some constants needed for the cartogram computation.
	 */
	
	private double CONVERGENCE = 1e-100;
	private double INFTY = 1e100;
	private double HINITIAL = 1e-4;
	private int IMAX = 50;
	private double MINH = 1e-5;
	private int MAXINTSTEPS = 3000;
	private double SIGMA = 0.1;
	private double SIGMAFAC = 1.2;
	private double TIMELIMIT = 1e8;
	private double TOLF = 1e-3;
	private double TOLINT = 1e-3;
	private double TOLX = 1e-3;

	
	
	
	/**
	 * Attributes for the computation progress bar.
	 */
	public int mProgressStart;
	public int mProgressEnd;
	public String mProgressText;
	public CartogramWizard mCartogramWizard;
	
	
	
	
	
	/**
	 * The constructor takes the cartogram grid which contains the
	 * density values.
	 */
	public CartogramGastner (CartogramGrid grid)
	{
	
		mGrid = grid;
		
	}	// CartogramGastner.<init>
	
	
	
	/**
	 * Starts the cartogram computation using the given grid size.
	 * @param gridSize the size of the grid used for computation. The
	 *        grid size must be a power of 2.
	 */
	public void compute (int gridSize)
		throws InterruptedException
	{
	
		// Store the grid size in the lx and ly attributes.
		lx = gridSize;
		ly = gridSize;
		
		this.initializeArrays();
		this.computeInitialDensity();
		FFT.coscosft(rho_0, 1, 1);
		
		
		boolean hasConverged = false;
		while (hasConverged == false)
		{
			if (Thread.interrupted())
			{
				// Raise an InterruptedException.
				throw new InterruptedException(
					"Computation has been interrupted by the user.");
			}
			
			hasConverged = this.integrateNonlinearVolterraEquation();
			
		}
		
		
		this.projectCartogramGrid();
	
	}	// CartogramGastner.compute




	/**
	 * Initializes the arrays using the grid size.
	 */
	private void initializeArrays ()
	{
	
		rho_0 = new double[lx+1][ly+1];
		rho = new double[lx+1][ly+1];
		gridvx = new double[lx+1][ly+1];
		gridvy = new double[lx+1][ly+1];
		x = new double[lx+1][ly+1];
		y = new double[lx+1][ly+1];
		vx = new double[lx+1][ly+1];
		vy = new double[lx+1][ly+1];
		xappr = new double[lx+1][ly+1];
		yappr = new double[lx+1][ly+1];
	
	}	// CartogramGastner.initializeArrays
	



	/**
	 * Computes the initial density of the Gastner grid using the provided
	 * cartogram grid.
	 */
	private void computeInitialDensity ()
	{
	
		// Compute the cartogram extent.
		mExtent = this.cartogramExtent(mGrid.envelope(), lx, ly);
		
		
		// Compute the cell size in x and y direction.
		double cellSizeX = mExtent.getWidth() / lx;
		double cellSizeY = mExtent.getHeight() / ly;
		
		// Store the extent's minimum and maximum coordinates.
		double extentMinX = mExtent.getMinX() - (cellSizeX / 2);
		double extentMinY = mExtent.getMinY() - (cellSizeY / 2);
		double extentMaxX = mExtent.getMaxX() + (cellSizeX / 2);
		double extentMaxY = mExtent.getMaxY() + (cellSizeY / 2);
		

		// Let the cartogram grid fill in the density.
		mGrid.fillRegularDensityGrid(rho_0, extentMinX, extentMaxX, 
			extentMinY, extentMaxY);
		
		// If there are 0 density values, introduce a small bias.
		double minimumDensity = rho_0[0][0];
		double maximumDensity = rho_0[0][0];
		for (int j = 0; j <= ly; j++)
		{
			for (int i = 0; i <= lx; i++)
			{
				if (rho_0[i][j] < minimumDensity)
					minimumDensity = rho_0[i][j];
				
				if (rho_0[i][j] > maximumDensity)
					maximumDensity = rho_0[i][j];
			}
		}
		
		if ((minimumDensity * 1000) < maximumDensity)
		{
			double bias = (maximumDensity / 1000) - minimumDensity;
			
			for (int j = 0; j <= ly; j++)
				for (int i = 0; i <= lx; i++)
					rho_0[i][j] += bias;
		}
		
		this.correctInitialDensityEdges();
	
	}	// CartogramGastner.computeInitialDensity





	/**
	 * Fills the edges of the initial density grid correctly.
	 */
	private void correctInitialDensityEdges ()
	{
	
		int i, j;
		rho_0[0][0] += rho_0[0][ly] + rho_0[lx][0] + rho_0[lx][ly];
		for (i = 1; i < lx; i++) rho_0[i][0] += rho_0[i][ly];
		for (j = 1; j < ly; j++) rho_0[0][j] += rho_0[lx][j];
		for (i = 0; i < lx; i++) rho_0[i][ly] = rho_0[i][0];
		for (j = 0; j <= ly; j++) rho_0[lx][j] = rho_0[0][j];
	
	}	// CartogramGastner.correctInitialDensityEdges
	





	/**
	 * Computes the cartogram extent bounding box using the layer extent
	 * and the grid size.
	 * @param env the bounding box of the layers (or the cartogram grid).
	 * @param gridX the grid size in x
	 * @param gridY the grid size in y
	 * @return the cartogram extent as Envelope
	 */
	private Envelope cartogramExtent(Envelope env, int gridX, int gridY)
	{

		double margin = 1.5;
		double minx, maxx, miny, maxy;
		if ( (env.getWidth() / gridX) > (env.getHeight() / gridY))
		{
			maxx = 0.5 * 
				((1 + margin) * env.getMaxX() + (1 - margin) * env.getMinX());
			minx = 0.5 * 
				((1 - margin) * env.getMaxX() + (1 + margin) * env.getMinX());
			maxy = 0.5 * 
				(env.getMaxY() + env.getMinY() + (maxx - minx) * gridY / gridX);
			miny = 0.5 * 
				(env.getMaxY() + env.getMinY() - (maxx - minx) * gridY / gridX);
		}
		else
		{
			maxy = 0.5 * 
				((1 + margin) * env.getMaxY() + (1 - margin) * env.getMinY());
			miny = 0.5 * 
				((1 - margin) * env.getMaxY() + (1 + margin) * env.getMinY());
			maxx = 0.5 * 
				(env.getMaxX() + env.getMinX() + (maxy - miny) * gridX / gridY);
			minx = 0.5 * 
				(env.getMaxX() + env.getMinX() - (maxy - miny) * gridX / gridY);
		}
	
		// Creating the Envelope
		Envelope bbox = new Envelope(minx, maxx, miny, maxy);
		return bbox;
		
	}	// CartogramGastner.cartogramExtent






	/**
	 * Integrates the non-linear Volterra equation.
	 * @return true if the displacement field has converged, false otherwise.
	 */
	private boolean integrateNonlinearVolterraEquation ()
	throws InterruptedException
	{
		boolean stepsize_ok;
		double h, maxchange = this.INFTY, t, vxplus, vyplus, xguess, yguess;
		int i, j, k;
		
		do
		{
			this.initcond();
			this.nblurs++;
			//if (this.minpop < 0.0)
			//	double sigmaVal = SIGMA * Math.pow(this.SIGMAFAC, this.nblurs);
			
		} while (this.minpop < 0.0);
		
		h = HINITIAL;
		t = 0;


		for (j = 0; j <= this.lx; j++)
		{
			for (k = 0; k <= this.ly; k++)
			{
				this.x[j][k] = j;
				this.y[j][k] = k;
			}
		}
		
		this.calculateVelocityField(0.0);

		for (j = 0; j <= this.lx; j++)
		{
			for (k = 0; k <= this.ly; k++)
			{
				vx[j][k] = gridvx[j][k];
				vy[j][k] = gridvy[j][k];
			}
		}

		i = 1;
		
		do
		{
			// Stop if the user has interrupted the process.
			if (Thread.interrupted())
			{
				// Raise an InterruptedException.
				throw new InterruptedException(
					"Computation has been interrupted by the user.");
			}

			stepsize_ok = true;
			this.calculateVelocityField(t+h);
			
			for (j = 0; j <= this.lx; j++)
			{
				for (k = 0; k <= this.ly; k++)
				{
					
					double xinterpol = this.x[j][k] + (h * this.vx[j][k]);
					double yinterpol = this.y[j][k] + (h * this.vy[j][k]);
					if (xinterpol < 0.0 || yinterpol < 0.0)
					{
						if (AppContext.DEBUG) System.out.println(
								"[ERROR] Cartogram out of bounds !");
					}
					
					vxplus = this.interpolateBilinear(
						this.gridvx, xinterpol, yinterpol);
						
					vyplus = this.interpolateBilinear(
						this.gridvy, xinterpol, yinterpol);
	  
					xguess = this.x[j][k] + 
						(0.5 * h * (this.vx[j][k] + vxplus));
						
					yguess = this.y[j][k] + 
						(0.5 * h * (this.vy[j][k] + vyplus));

					double[] ptappr = new double[2];
					ptappr[0] = this.xappr[j][k];
					ptappr[1] = this.yappr[j][k];
					boolean solving_ok = 
						this.newt2(h, ptappr, xguess, yguess, j, k);
					this.xappr[j][k] = ptappr[0];
					this.yappr[j][k] = ptappr[1];
					if (solving_ok == false)
						return false;
	      
					if ( ((xguess - this.xappr[j][k]) * 
						  (xguess - this.xappr[j][k])) + 
						 ((yguess - this.yappr[j][k]) * 
						  (yguess - this.yappr[j][k])) > this.TOLINT)
					{
						if (h < this.MINH)
						{
							//double sigmaVal = this.SIGMA * Math.pow(
							//	this.SIGMAFAC, this.nblurs);
							this.nblurs++;
							return false;
						}
						h = h / 10;
						stepsize_ok = false;
						break;
					}
			
				}	// for (k = 0; k <= this.ly; k++)
				
			}	// for (j = 0; j <= this.lx; j++)
		
	
			if (!stepsize_ok)
			{
				continue;
			}
			else
			{
				t += h;
				maxchange = 0.0;

				for (j = 0; j <= this.lx; j++)
				{
					for (k = 0; k <= this.ly; k++)
					{
						if ( ((this.x[j][k] - this.xappr[j][k]) * 
							  (this.x[j][k] - this.xappr[j][k])) +
							 ((this.y[j][k] - this.yappr[j][k]) * 
							  (this.y[j][k] - this.yappr[j][k])) > maxchange)
						{
							maxchange = ((this.x[j][k] - this.xappr[j][k]) * 
										 (this.x[j][k] - this.xappr[j][k])) +
										((this.y[j][k] - this.yappr[j][k]) * 
										 (this.y[j][k] - this.yappr[j][k]));
						}
						
						this.x[j][k] = this.xappr[j][k];
						this.y[j][k] = this.yappr[j][k];
						this.vx[j][k] = this.interpolateBilinear(
							this.gridvx, this.xappr[j][k], this.yappr[j][k]);
						this.vy[j][k] = this.interpolateBilinear(
							this.gridvy, this.xappr[j][k], this.yappr[j][k]);
		  
					}	// for (k=0; k<=ly; k++)
		
				}	// for (j = 0; j <= this.lx; j++)
		
			}

			h = 1.2 * h;
			

			int progress = mProgressEnd;
			if (i < 200)
				progress = mProgressStart + 
					(i * ((mProgressEnd - mProgressStart) / 200));
			mCartogramWizard.updateRunningStatus(progress, mProgressText, 
				"Doing time step " + i);


			i++;
			
		} while (i < this.MAXINTSTEPS && 
				 t < this.TIMELIMIT && 
				 maxchange > this.CONVERGENCE);

		return true;
	
	}	// CartogramGastner.integrateNonlinearVolterraEquation






	private void initcond ()
	{
		double maxpop;
		int i,j;

		FFT.coscosft(rho_0, -1, -1);
		for (i = 0; i < this.lx; i++)
		{
			for (j = 0; j < this.ly; j++)
			{
				if (this.rho_0[i][j] < -1e10)
				{
					this.rho_0[i][j] = 0.0;
				}
			}
		}
	
		this.gaussianBlur();

		minpop = this.rho_0[0][0];
		maxpop = this.rho_0[0][0];
		for (i = 0; i < this.lx; i++)
		{
			for (j = 0; j < this.ly; j++)
			{
				if (this.rho_0[i][j] < minpop)
				{
					minpop = rho_0[i][j];
				}
			}
		}
		for (i = 0; i < this.lx; i++)
		{
			for (j = 0; j < this.ly; j++)
			{
				if (this.rho_0[i][j] > maxpop) 
				{
					maxpop = this.rho_0[i][j];
				}
			}
		}

		FFT.coscosft(this.rho_0, 1, 1);

	}	// CartogramGastner.initcond






	/**
	 * Performs au Gaussian blur on the density grid.
	 */
	public void gaussianBlur ()
	{
	
		double[][][] blur = new double[1][this.lx][this.ly];
		double[][][] conv = new double[1][this.lx][this.ly];
		double[][][] pop = new double[1][this.lx][this.ly];
		double[][] speqblur = new double[1][2*this.lx];
		double[][] speqconv = new double[1][2*this.lx];
		double[][] speqpop = new double[1][2*this.lx];

		int i, j, p, q;
		for (i=1; i <= this.lx; i++)
		{
			for (j = 1; j <= this.ly; j++)
			{
				if (i > (this.lx / 2))
					p = i - 1 - this.lx;
				else
					p = i - 1;
				
				if (j > (this.ly / 2))
					q = j - 1 - this.ly;
				else
					q = j-1;
				
				pop[0][i-1][j-1] = this.rho_0[i-1][j-1];
				
				double erfDenominator = Math.sqrt(2.0) * 
					(this.SIGMA * Math.pow(this.SIGMAFAC, this.nblurs));
					
				double erfParam1 = this.erf((p + 0.5) / erfDenominator);
				double erfParam2 = this.erf((p - 0.5) / erfDenominator);
				double erfParam3 = this.erf((q + 0.5) / erfDenominator);
				double erfParam4 = this.erf((q - 0.5) / erfDenominator);
				
				conv[0][i-1][j-1] = 0.5 * (erfParam1 - erfParam2) * 
					(erfParam3 - erfParam4) / (this.lx * this.ly);

			}
		}
  
 
		FFT.rlft3(pop, speqpop, 1, this.lx, this.ly, 1);
		FFT.rlft3(conv, speqconv, 1, this.lx, this.ly, 1);
		for (i = 1; i <= this.lx; i++)
		{
			for (j = 1; j<= (this.ly / 2); j++)
			{
				blur[0][i-1][(2*j)-2] = 
					pop[0][i-1][(2*j)-2] * conv[0][i-1][(2*j)-2] -
					pop[0][i-1][(2*j)-1] * conv[0][i-1][(2*j)-1];
									  
				blur[0][i-1][(2*j)-1] = 
					pop[0][i-1][(2*j)-1] * conv[0][i-1][(2*j)-2] +
					pop[0][i-1][(2*j)-2] * conv[0][i-1][(2*j)-1];
			}
		}
	  
		for (i = 1; i <= this.lx; i++)
		{
			speqblur[0][(2*i)-2] = 
				speqpop[0][(2*i)-2] * speqconv[0][(2*i)-2] -
				speqpop[0][(2*i)-1] * speqconv[0][(2*i)-1];

			speqblur[0][(2*i)-1] = 
				speqpop[0][(2*i)-1] * speqconv[0][(2*i)-2] +
				speqpop[0][(2*i)-2] * speqconv[0][(2*i)-1];
		}

		FFT.rlft3(blur, speqblur, 1, this.lx, this.ly, -1);

		for (i = 1; i <= this.lx; i++)
		{
			for (j = 1; j <= this.ly; j++)
			{
				rho_0[i-1][j-1] = blur[0][i-1][j-1];
			}
		}

	}	// CartogramGastner.gaussianBlur





	/**
	 * Computes the velocity field at time t.
	 * @param t the time.
	 */
	private void calculateVelocityField (double t)
	{
		int j, k;
		for (j = 0; j <= this.lx; j++)
		{
			for (k = 0; k <= this.ly; k++)
			{
				this.rho[j][k] = Math.exp(-1 * ((Math.PI * j / this.lx) * 
					(Math.PI * j / this.lx) + (Math.PI * k / this.ly) * 
					(Math.PI * k / this.ly)) * t) * this.rho_0[j][k];
			}
		}

		for (j = 0; j <= this.lx; j++)
		{
			for (k = 0; k <= this.ly; k++)
			{
				this.gridvx[j][k] = 
					-1 * (Math.PI * j / this.lx) * this.rho[j][k];
					
				this.gridvy[j][k] = 
					-1 * (Math.PI * k / this.ly) * this.rho[j][k];
			}
		}
  
		FFT.coscosft(this.rho, -1, -1);
		FFT.sincosft(this.gridvx, -1, -1);
		FFT.cossinft(this.gridvy, -1, -1);

		for (j = 0; j <= this.lx; j++)
		{
			for (k = 0; k <= this.ly; k++)
			{
				this.gridvx[j][k] = (-1 * this.gridvx[j][k]) / this.rho[j][k];
				this.gridvy[j][k] = (-1 * this.gridvy[j][k]) / this.rho[j][k];
			}
		}
		
	}	// CartogramGastner.calculateVelocityField





	/**
	 * Bilinear interpolation in 2D.
	 */
	private double interpolateBilinear(double[][] arr, double x, double y)
	{
	
		int gaussx,gaussy;
		double deltax,deltay;

		if (x < 0.0 || y < 0.0)
		{
			return 0.0;
		}
		
		int xlen = arr.length;
		int ylen = arr[0].length;
		if (x >= xlen || y >= ylen)
		{
			return 0.0;
		}

		Double xobj = new Double(x);
		gaussx = xobj.intValue();
		Double yobj = new Double(y);
		gaussy = yobj.intValue();
		deltax = x - gaussx;
		deltay = y - gaussy;

		if (gaussx == this.lx && gaussy == this.ly)
			return arr[gaussx][gaussy];
		
		if (gaussx == this.lx)
			return ((1 - deltay) * arr[gaussx][gaussy]) + 
					(deltay * arr[gaussx][gaussy+1]);
		
		if (gaussy == this.ly)
			return ((1 - deltax) * arr[gaussx][gaussy]) + 
					(deltax * arr[gaussx+1][gaussy]);
		
		return  ((1 - deltax) * (1 - deltay) * arr[gaussx][gaussy]) +
				((1 - deltax) * deltay * arr[gaussx][gaussy+1]) +
				(deltax * (1 - deltay) * arr[gaussx+1][gaussy]) +
				(deltax * deltay * arr[gaussx+1][gaussy+1]);

	}	// CartogramGastner.interpolateBilinear




	public boolean newt2 (double h, double[] ptappr, 
		double xguess, double yguess, int j, int k)
	{
		
		double deltax, deltay, dfxdx, dfxdy, dfydx, dfydy, fx, fy;
		int gaussx, gaussxplus, gaussy, gaussyplus, i;
		double temp;
		Double tempobj = null;
  
		ptappr[0] = xguess;
		ptappr[1] = yguess;
  
		for (i = 1; i <= IMAX; i++)
		{
			temp = this.interpolateBilinear(this.gridvx, ptappr[0], ptappr[1]);
			
			fx = ptappr[0] - (0.5 * h * temp) - this.x[j][k] - 
				(0.5 * h * this.vx[j][k]);
				
			temp = this.interpolateBilinear(this.gridvy, ptappr[0], ptappr[1]);
			
			fy = ptappr[1] - (0.5 * h * temp) - this.y[j][k] - 
				(0.5 * h * this.vy[j][k]);
			
			tempobj = new Double(ptappr[0]);
			gaussx = tempobj.intValue();
			tempobj = new Double(ptappr[1]);
			gaussy = tempobj.intValue();
			
			if (gaussx == this.lx)
				gaussxplus = 0;
			else
				gaussxplus = gaussx + 1;
				
			if (gaussy == this.ly)
				gaussyplus = 0;
			else
				gaussyplus = gaussy + 1;

			deltax = this.x[j][k] - gaussx;
			deltay = this.y[j][k] - gaussy;
			
			dfxdx = 1 - 0.5 * h * 
					((1 - deltay) * (this.gridvx[gaussxplus][gaussy] - 
									 this.gridvx[gaussx][gaussy]) +
					deltay * (this.gridvx[gaussxplus][gaussyplus] - 
							 this.gridvx[gaussx][gaussyplus]));
	 
			dfxdy = -0.5 * h *
					((1 - deltax) * (this.gridvx[gaussx][gaussyplus] - 
									 this.gridvx[gaussx][gaussy]) +
					deltax * (this.gridvx[gaussxplus][gaussyplus] - 
							  this.gridvx[gaussxplus][gaussy]));

			dfydx = -0.5 * h *
					((1 - deltay) * (this.gridvy[gaussxplus][gaussy] - 
									 this.gridvy[gaussx][gaussy]) +
					deltay * (this.gridvy[gaussxplus][gaussyplus] - 
							  this.gridvy[gaussx][gaussyplus]));

			dfydy = 1 - 0.5 * h *
					((1 - deltax) * (this.gridvy[gaussx][gaussyplus] - 
									 this.gridvy[gaussx][gaussy]) +
					deltax * (this.gridvy[gaussxplus][gaussyplus] - 
							  this.gridvy[gaussxplus][gaussy]));
      
	  
			if ((fx*fx + fy*fy) < this.TOLF)
				return true;
			
			deltax = (fy*dfxdy - fx*dfydy) / (dfxdx*dfydy - dfxdy*dfydx);
			deltay = (fx*dfydx - fy*dfxdx) / (dfxdx*dfydy - dfxdy*dfydx);
      
			if ((deltax*deltax + deltay*deltay) < this.TOLX)
				return true;
			
			ptappr[0] += deltax;
			ptappr[1] += deltay;

		}

		return false;

	}	// CartogramGastner.newt2




	/**
	 * Our wrapper function for the Jakarta Commons Math Erf.erf function.
	 * For values <= -4 or >= 4, we return -1 or 1 directly, without
	 * computation. Erf.erf raises too often an exception for failing
	 * convergence.
	 */
	public static double erf (double value)
	{
		if (value <= -4.0)
			return -1.0;
		
		if (value >= 4.0)
			return 1.0;
		
		double erf = 0.0;
		try
		{
			erf = Erf.erf(value);
		}
		catch (MathException mexc)
		{
			if (value < 0)
				return -1.0;
			else
				return 1.0;
		}
		
		return erf;
	
	}	// CartogramGastner.erf





	/**
	 * Applies the cartogram deformation to the cartogram grid.
	 */
	private void projectCartogramGrid ()
	{
		
		// Project each point in the cartogram grid.
		double[][] x = mGrid.getXCoordinates();
		double[][] y = mGrid.getYCoordinates();
		
		int gridSizeX = x.length;
		int gridSizeY = x[0].length;
		
		int i, j;
		for (i = 0; i < gridSizeX; i++)
		{
			for (j = 0; j < gridSizeY; j++)
			{
				double[] projectedPoint = this.projectPoint(x[i][j], y[i][j]);
				x[i][j] = projectedPoint[0];
				y[i][j] = projectedPoint[1];
			}
		}
	
	}	// CartogramGastner.projectCartogramGrid







	/**
	 * Projects one point using the deformed grid.
	 * @param x the x coordinate of the point to project.
	 * @param y the y coordinate of the point to project.
	 * @return a double array with the coordinates of the projected point.
	 */
	private double[] projectPoint (double x, double y)
	{
	
		double deltax, deltay, den, t, u, temp;
		long gaussx, gaussy;

		// Get the grid size and the cellsize.
		double cellSizeX = mExtent.getWidth() / lx;
		double cellSizeY = mExtent.getHeight() / ly;

		// Make a copy of the point coordinate.
		double px = x;
		double py = y;

		px = (px - mExtent.getMinX()) * lx / mExtent.getWidth();
		py = (py - mExtent.getMinY()) * ly / mExtent.getHeight();
		temp = Math.floor(px);
		gaussx = Math.round(temp);
		temp = Math.floor(py);
		gaussy = Math.round(temp);
		if (gaussx < 0 || gaussx > lx || gaussy < 0 || gaussy > ly)
		{
			System.out.println("[ERROR] Coordinate limits exceeded.");
			return null;
		}
		deltax = px - gaussx;
		deltay = py - gaussy;


		double ax = (1 - deltax) * this.x[(int)gaussx][(int)gaussy] + 
			deltax * this.x[(int)(gaussx+1)][(int)gaussy];
		double ay = (1 - deltax) * this.y[(int)gaussx][(int)gaussy] + 
			deltax * this.y[(int)(gaussx+1)][(int)gaussy];
		double bx = (1 - deltax) * this.x[(int)gaussx][(int)(gaussy+1)] + 
			deltax * this.x[(int)(gaussx+1)][(int)(gaussy+1)];
		double by = (1 - deltax) * this.y[(int)gaussx][(int)(gaussy+1)] + 
			deltax * this.y[(int)(gaussx+1)][(int)(gaussy+1)];
		double cx = (1 - deltay) * this.x[(int)gaussx][(int)gaussy] + 
			deltay * this.x[(int)gaussx][(int)(gaussy+1)];
		double cy = (1 - deltay) * this.y[(int)gaussx][(int)gaussy] + 
			deltay * this.y[(int)gaussx][(int)(gaussy+1)];
		double dx = (1 - deltay) * this.x[(int)(gaussx+1)][(int)gaussy] + 
			deltay * this.x[(int)(gaussx+1)][(int)(gaussy+1)];
		double dy = (1 - deltay) * this.y[(int)(gaussx+1)][(int)gaussy] + 
			deltay * this.y[(int)(gaussx+1)][(int)(gaussy+1)];

		den = (bx - ax)*(cy - dy) + (ay - by)*(cx - dx);
		if (Math.abs(den) < 1e-12)
		{
			double ix = (ax + bx + cx + dx) / 4;
			double iy = (ay + by + cy + dy) / 4;
			double meanpoint[] = new double[2];
			meanpoint[0] = (ix * (lx / mExtent.getWidth())) + mExtent.getMinX();
			meanpoint[1] = (iy * (ly / mExtent.getHeight())) + mExtent.getMinY();
			return meanpoint;
			
		}
		t = ((cx - ax)*(cy - dy) + (ay - cy)*(cx - dx)) / den;
		u = ((bx - ax)*(cy - ay) + (ay - by)*(cx - ax)) / den;
		
		px = (1 - (ax + t*(bx-ax)) / lx) * mExtent.getMinX() + 
				((ax + t*(bx - ax)) / lx) * mExtent.getMaxX();
		py = (1 - (ay + t*(by-ay)) / ly) * mExtent.getMinY() + 
				((ay + t*(by - ay)) / ly) * mExtent.getMaxY();
				
		double point[] = new double[2];
		point[0] = px;
		point[1] = py;
		
		return point;

	}	// CartogramGastner.projectPointWithGrid






	/**
	 * Writes the grid into the specified shape file.
	 * @param shapefile the path to the shape file.
	 */
	public void writeToShapefile (String shapefile)
	{
	
		// Create a new Feature Schema for our shape file.
		FeatureSchema fs = new FeatureSchema();
		
		// We add the following attributes to the Feature Schema:
		// cellId : a serial number starting at 1
		// geom : the geometry (polygon)
		// i : the index of the cell in x direction
		// j : the index of the cell in y direction
		fs.addAttribute("cellId", AttributeType.INTEGER);
		fs.addAttribute("geom", AttributeType.GEOMETRY);
		fs.addAttribute("i", AttributeType.INTEGER);
		fs.addAttribute("j", AttributeType.INTEGER);
		fs.addAttribute("rho_0", AttributeType.DOUBLE);
		fs.addAttribute("rho", AttributeType.DOUBLE);
		
		
		// Create a new Geometry Factory for creating our geometries.
		GeometryFactory gf = new GeometryFactory();
		
		// Create a new Feature Dataset in order to store our new Features.
		FeatureDataset fd = new FeatureDataset(fs);
		
		
		// Create one Feature for each cell.
		int i, j;
		int cellId = 0;
		for (j = 0; j < ly; j++)
		{
			for (i = 0; i < lx; i++)
			{
				cellId++;
				
				// Extract the coordinates for the cell polygon.
				Coordinate[] coords = new Coordinate[5];
				coords[0] = new Coordinate(x[i][j], y[i][j]);
				coords[1] = new Coordinate(x[i][j+1], y[i][j+1]);
				coords[2] = new Coordinate(x[i+1][j+1], y[i+1][j+1]);
				coords[3] = new Coordinate(x[i+1][j], y[i+1][j]);
				coords[4] = coords[0];
				
				// Create the polygon.
				LinearRing ring = gf.createLinearRing(coords);
				Polygon poly = gf.createPolygon(ring, null);
				
				// Create a new Feature.
				BasicFeature feat = new BasicFeature(fs);
				
				// Setting the Feature's attributes.
				feat.setAttribute("cellId", new Integer(cellId));
				feat.setAttribute("geom", poly);
				feat.setAttribute("i", new Integer(i));
				feat.setAttribute("j", new Integer(j));
				feat.setAttribute("rho_0", new Double(rho_0[i][j]));
				feat.setAttribute("rho", new Double(rho[i][j]));
				
				// Add the Feature to the Feature Dataset.
				fd.add(feat);
				
			}
		}
		
		
		// Write the Feature Dataset to the Shape file.
		IOManager.writeShapefile(fd, shapefile);
	
			
	}	// CartogramGrid.writeToShapefile





}	// CartogramGastner











class FFT
{


	public static void coscosft (double[][] y, int isign1, int isign2)
	{
		int lx = y.length - 1;
		int ly = y[0].length - 1;
		double temp[] = new double[lx+1];
		int i, j;
		for (i = 0; i <= lx; i++)
		{
			FFT.cosft(y[i], ly, isign2);
		}
		for (j = 0; j <= ly; j++)
		{
			for (i = 0; i <= lx; i++)
			{
				temp[i] = y[i][j];
			}
			FFT.cosft(temp, lx, isign1);
			for (i = 0; i <= lx; i++)
			{
				y[i][j] = temp[i];
			}
		}

	}
	
	

	public static void cosft (double z[], int n, int isign)
	{
		double theta, wi=0.0, wpi, wpr, wr=1.0, wtemp;
		double[] a;
		double sum, y1, y2;
		int j, n2;
		
		a = new double[n+2];
		for (j = 1; j <= (n+1); j++)
		{
			a[j] = z[j-1];
		}

		theta = Math.PI / n;
		wtemp = Math.sin(0.5 * theta);
		wpr = -2.0 * wtemp * wtemp;
		wpi = Math.sin(theta);
		sum = 0.5 * (a[1] - a[n+1]);
		a[1] = 0.5 * (a[1] + a[n+1]);
		n2 = n + 2;

		for (j = 2; j <= (n/2); j++)
		{
			wtemp = wr;
			wr = wr*wpr - wi*wpi + wr;
			wi = wi*wpr + wtemp*wpi + wi;
			y1 = 0.5 * (a[j] + a[n2-j]);
			y2 = a[j] - a[n2-j];
			a[j] = y1 - wi*y2;
			a[n2-j] = y1 + wi*y2;
			sum += wr * y2;
		}
		FFT.realft(a, n, 1);
		a[n+1] = a[2];
		a[2] = sum;
		for (j = 4; j <= n; j += 2)
		{
			sum += a[j];
			a[j] = sum;
		}

  
		if (isign == 1)
		{
			for (j = 1; j <= (n+1); j++)
				z[j-1] = a[j];
		}
		else if (isign == -1)
		{
			for (j = 1; j <= (n+1); j++)
				z[j-1] = 2.0 * a[j] / n;
		}

	}	// FFT.cosft




	public static void cossinft (double[][] y, int isign1, int isign2)
	{
		int lx = y.length - 1;
		int ly = y[0].length - 1;
		double[] temp = new double[lx+1];
		int i, j;
		
		for (i = 0; i <= lx; i++)
			FFT.sinft(y[i], ly, isign2);
		
		for (j = 0; j <= ly; j++)
		{
			for (i = 0; i <= lx; i++)
				temp[i] = y[i][j];
			
			FFT.cosft(temp, lx, isign1);
			for (i = 0; i <= lx; i++)
				y[i][j] = temp[i];
		}
			
	}



	public static void four1 (double[] data, int nn, int isign)
	{
		double theta, wi, wpi, wpr, wr, wtemp;
		double tempi, tempr;
		int i, istep, j, m, mmax, n;
		n = nn * 2;
		j = 1;
		for (i = 1; i < n; i += 2)
		{
			if (j > i)
			{
				tempr = data[j];
				data[j] = data[i];
				data[i] = tempr;
				tempr = data[j+1];
				data[j+1] = data[i+1];
				data[i+1] = tempr;
			}
      
			m = n / 2;
			while (m >= 2 && j > m)
			{
				j -= m;
				m = m / 2;
			}
			j += m;
		}
		
		mmax = 2;
		while (n > mmax)
		{
			istep = mmax * 2;
			theta = isign * (2 * Math.PI / mmax);
			wtemp = Math.sin(0.5 * theta);
			wpr = -2.0 * wtemp * wtemp;
			wpi = Math.sin(theta);
			wr = 1.0;
			wi = 0.0;
			
			for (m = 1; m < mmax; m += 2)
			{
				for (i = m; i <= n; i += istep)
				{
					j = i + mmax;
					tempr = wr * data[j] - wi * data[j+1];
					tempi = wr * data[j+1] + wi * data[j];
					data[j] = data[i] - tempr;
					data[j+1] = data[i+1] - tempi;
					data[i] += tempr;
					data[i+1] += tempi;
				}
				
				wtemp = wr;
				wr = wtemp*wpr - wi*wpi + wr;
				wi = wi*wpr + wtemp*wpi + wi;
			}
			
			mmax = istep;
		} 

	}
	



	public static void fourn(double[] data, int[] nn, int ndim, int isign)
	{
		int idim;
		int i1, i2, i3, i2rev, i3rev, ip1, ip2, ip3, ifp1, ifp2;
		int ibit, k1, k2, n, nprev, nrem, ntot;
		double tempi, tempr;
		double theta, wi, wpi, wpr, wr, wtemp;
		double swaptemp;

		ntot = 1;
		for (idim = 1; idim <= ndim; idim++)
		{
			ntot *= nn[idim];
		}
 
		nprev = 1;
		for (idim = ndim; idim >= 1; idim--)
		{
			n = nn[idim];
			nrem = ntot / (n*nprev);
			ip1 = nprev * 2;
			ip2 = ip1 * n;
			ip3 = ip2 * nrem;
			i2rev = 1;
			
			for (i2 = 1; i2 <= ip2; i2 += ip1)
			{
				if (i2 < i2rev)
				{
					for (i1 = i2; i1 <= (i2 + ip1 - 2); i1 += 2)
					{
						for (i3 = i1; i3 <= ip3; i3 += ip2)
						{
							i3rev = i2rev + i3 - i2;
							
							swaptemp = data[i3-1];
							data[i3-1] = data[i3rev-1];
							data[i3rev-1] = swaptemp;
							
							swaptemp = data[i3];
							data[i3] = data[i3rev];
							data[i3rev] = swaptemp;
						}
					}
				}
				
				ibit = ip2 / 2;
				while (ibit >= ip1 && i2rev > ibit)
				{
					i2rev -= ibit;
					ibit = ibit / 2;
				}
				i2rev += ibit;
			}
			
			ifp1 = ip1;

			while (ifp1 < ip2)
			{
				ifp2 = ifp1 * 2;
				theta = 2 * isign * Math.PI / (ifp2 / ip1);
				wtemp = Math.sin(0.5*theta);
				wpr = -2.0 * wtemp * wtemp;
				wpi = Math.sin(theta);
				wr = 1.0;
				wi = 0.0;
				
				for (i3 = 1; i3 <= ifp1; i3 += ip1)
				{
					for (i1 = i3; i1 <= (i3 + ip1 - 2); i1 += 2)
					{
						for (i2 = i1; i2 <= ip3; i2 += ifp2)
						{
							k1 = i2;
							k2 = k1 + ifp1;
							tempr = (wr * data[k2-1]) - (wi * data[k2]);
							tempi = (wr * data[k2]) + (wi * data[k2-1]);
							data[k2-1] = data[k1-1] - tempr;
							data[k2] = data[k1] - tempi;
							data[k1-1] += tempr;
							data[k1] += tempi;
						}
					}

					wtemp = wr;
					wr = wtemp*wpr - wi*wpi + wr;
					wi = wi*wpr + wtemp*wpi + wi;
				}
				
				ifp1 = ifp2;
			}
			nprev *= n;
		
		}

	}




	public static void realft (double[] data, int n, int isign)
	{
		double theta, wi, wpi, wpr, wr, wtemp;
		double c1=0.5, c2, h1i, h1r, h2i, h2r;
		int i, i1, i2, i3, i4, np3;
		
		theta = Math.PI / (n/2);
		if (isign == 1)
		{
			c2 = -0.5;
			FFT.four1(data, (n/2), 1);
		}
		else
		{
			c2 = 0.5;
			theta = -1.0 * theta;
		}
		wtemp = Math.sin(0.5 * theta);
		wpr = -2.0 * wtemp * wtemp;
		wpi = Math.sin(theta);
		wr = 1.0 + wpr;
		wi = wpi;
		np3 = n + 3;
		for (i = 2; i <= (n/4); i++)
		{
			i1 = i + i - 1;
			i2 = 1 + i + i - 1;
			i3 = np3 - i2;
			i4 = 1 + i3;
			
			h1r = c1 * (data[i1] + data[i3]);
			h1i = c1 * (data[i2] - data[i4]);
			h2r = (-1.0 * c2) * (data[i2] + data[i4]);
			h2i = c2 * (data[i1] - data[i3]);

			data[i1] = h1r + wr*h2r - wi*h2i;
			data[i2] = h1i + wr*h2i + wi*h2r;
			data[i3] = h1r - wr*h2r + wi*h2i;
			data[i4] = (-1.0 * h1i) + wr*h2i + wi*h2r;
			wtemp = wr;
			wr = wr*wpr - wi*wpi + wr;
			wi = wi*wpr + wtemp*wpi + wi;
		}
		if (isign == 1)
		{
			h1r = data[1];
			data[1] = h1r + data[2];
			data[2] = h1r - data[2];
		}
		else
		{
			h1r = data[1];
			data[1] = c1 * (h1r + data[2]);
			data[2] = c1 * (h1r - data[2]);
			FFT.four1(data, (n/2), -1);
		}

	}



	public static void rlft3 (double[][][] data, double[][] speq, int nn1, 
		int nn2, int nn3, int isign)
	{
		double theta, wi, wpi, wpr, wr, wtemp;
		double c1, c2, h1r, h1i, h2r, h2i;
		int i1, i2, i3, j1, j2, j3, ii3;
		int[] nn = new int[4];
		
		c1 = 0.5;
		c2 = -0.5 * isign;
		theta = 2 * isign * (Math.PI / nn3);
		wtemp = Math.sin(0.5 * theta);
		wpr = -2.0 * wtemp * wtemp;
		wpi = Math.sin(theta);
		nn[1] = nn1;
		nn[2] = nn2;
		nn[3] = nn3 / 2;
  
		double[] datatemp = new double[(nn1*nn2*nn3)];
		if (isign == 1)
		{
			j1 = 0;
			for (i1 = 0; i1 < nn1; i1++)
			{
				for (i2 = 0; i2 < nn2; i2++)
				{
					for (i3 = 0; i3 < nn3; i3++)
					{
						datatemp[j1] = data[i1][i2][i3];
						j1++;
					}
				}
			}
		
			FFT.fourn(datatemp, nn, 3, isign);
			j1 = 0;
			for (i1 = 0; i1 < nn1; i1++)
			{
				for (i2 = 0; i2 < nn2; i2++)
				{
					for (i3 = 0; i3 < nn3; i3++)
					{
						data[i1][i2][i3] = datatemp[j1];
						j1++;
					}
				}
			}
			
			
			for (i1 = 1; i1 <= nn1; i1++)
			{
				for (i2 = 1, j2 = 0; i2 <= nn2; i2++)
				{
					speq[i1-1][j2] = data[i1-1][i2-1][0];
					j2++;
					speq[i1-1][j2] = data[i1-1][i2-1][1];
					j2++;
				}
			}
			
		}
		
		
		for (i1 = 1; i1 <= nn1; i1++)
		{
			if (i1 != 1)
				j1 = nn1 - i1 + 2;
			else
				j1 = 1;

			wr = 1.0;
			wi = 0.0;
			for (ii3 = 1, i3 = 1; i3 <= ((nn3/4) + 1); i3++, ii3 += 2)
			{
				for (i2 = 1; i2 <= nn2; i2++)
				{
					if (i3 == 1)
					{
						if (i2 != 1)
							j2 = ((nn2 - i2) * 2) + 3;
						else
							j2 = 1;
						
						h1r = c1 * (data[i1-1][i2-1][0] + speq[j1-1][j2-1]);
						h1i = c1 * (data[i1-1][i2-1][1] - speq[j1-1][j2]);
						h2i = c2 * (data[i1-1][i2-1][0] - speq[j1-1][j2-1]);
						h2r = -1 * c2 * (data[i1-1][i2-1][1] + speq[j1-1][j2]);
						data[i1-1][i2-1][0] = h1r + h2r;
						data[i1-1][i2-1][1] = h1i + h2i;
						speq[j1-1][j2-1] = h1r-h2r;
						speq[j1-1][j2] = h2i-h1i;
					}
					else
					{
						if (i2 != 1)
							j2 = nn2 - i2 + 2;
						else
							j2 = 1;
						
						j3 = nn3 + 3 - (i3 * 2);
						h1r = c1 * (data[i1-1][i2-1][ii3-1] + data[j1-1][j2-1][j3-1]);
						h1i = c1 * (data[i1-1][i2-1][ii3] - data[j1-1][j2-1][j3]);
						h2i = c2 * (data[i1-1][i2-1][ii3-1] - data[j1-1][j2-1][j3-1]);
						h2r = -1 * c2 * (data[i1-1][i2-1][ii3] + data[j1-1][j2-1][j3]);
						data[i1-1][i2-1][ii3-1] = h1r + wr*h2r - wi*h2i;
						data[i1-1][i2-1][ii3] = h1i + wr*h2i + wi*h2r;
						data[j1-1][j2-1][j3-1] = h1r - wr*h2r + wi*h2i;
						data[j1-1][j2-1][j3] = -1*h1i + wr*h2i + wi*h2r;
					}
				}
				
				wtemp = wr;
				wr = wtemp*wpr - wi*wpi + wr;
				wi = wi*wpr + wtemp*wpi + wi;
			}
		
		}
		
		if (isign == -1)
		{
			j1 = 0;
			for (i1 = 0; i1 < nn1; i1++)
			{
				for (i2 = 0; i2 < nn2; i2++)
				{
					for (i3 = 0; i3 < nn3; i3++)
					{
						datatemp[j1] = data[i1][i2][i3];
						j1++;
					}
				}
			}
			
			FFT.fourn(datatemp, nn, 3, isign);
			
			j1 = 0;
			for (i1 = 0; i1 < nn1; i1++)
			{
				for (i2 = 0; i2 < nn2; i2++)
				{
					for (i3 = 0; i3 < nn3; i3++)
					{
						data[i1][i2][i3] = datatemp[j1];
						j1++;
					}
				}
			}
			
		}
	
	}



	public static void sincosft (double[][] y, int isign1, int isign2)
	{
		int lx = y.length - 1;
		int ly = y[0].length - 1;
		double[] temp = new double[lx+1];
		int i, j;

		for (i = 0; i <= lx; i++)
		{
			FFT.cosft(y[i], ly, isign2);
		}
		
		for (j = 0; j <= ly; j++)
		{
			for (i = 0; i <= lx; i++)
			{
				temp[i] = y[i][j];
			}
			FFT.sinft(temp, lx, isign1);
			for (i = 0; i <= lx; i++)
			{
				y[i][j] = temp[i];
			}
		}
	
	}
	
	
	
	public static void sinft (double[] z, int n, int isign)
	{
		double theta, wi = 0.0, wpi, wpr, wr = 1.0, wtemp;
		double[] a;
		double sum, y1, y2;
		int j;
		int n2 = n + 2;
		a = new double[n+1];
		for (j = 1; j <= n; j++)
		{
			a[j] = z[j-1];
		}

		theta = Math.PI / n;
		wtemp = Math.sin(0.5 * theta);
		wpr = -2.0 * wtemp * wtemp;
		wpi = Math.sin(theta);
		a[1] = 0.0;
		for (j = 2; j <= ((n/2) + 1); j++)
		{
			wtemp = wr;
			wr = wtemp*wpr - wi*wpi + wr;
			wi = wi*wpr + wtemp*wpi + wi;
			y1 = wi * (a[j] + a[n2-j]);
			y2 = 0.5 * (a[j] - a[n2-j]);
			a[j] = y1 + y2;
			a[n2-j] = y1 - y2;
		}

		FFT.realft(a, n, 1);
		a[1] *= 0.5;
		sum = a[2] = 0.0;
		for (j = 1; j <= (n-1); j += 2)
		{
			sum += a[j];
			a[j] = a[j+1];
			a[j+1] = sum;
		}

		if (isign == 1)
		{
			for (j = 1; j <= n; j++)
			{
				z[j-1] = a[j];
			}
		}
		else if (isign == -1)
		{
			for (j=1; j<=n; j++)
			{
				z[j-1] = 2.0 * a[j] / n;
			}
		}
		
		z[n] = 0.0;
	
	}



}










