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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.DataFormatException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;



/**
 * The cartogram grid class represents the grid which is overlaid on
 * all the layers and which is used for the deformation computation.
 * The grid has nodes and cells. Each node has x/y-coordinates, and each
 * cell has a density value.
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-30
 */
public class CartogramGrid
{

	/**
	 * The grid size in x and y direction.
	 */
	int mGridSizeX = 256;
	int mGridSizeY = 256;
	
	/**
	 * The real world initial envelope for the grid.
	 * The grid is constructed upon this region.
	 */
	Envelope mEnvelope = null;
	
	
	
	/**
	 * The arrays for storing the nodes and the cells.
	 */
	double[][] mNodeX;
	double[][] mNodeY;
	double[][] mCellOriginalDensity;
	double[][] mCellCurrentDensity;
	short[][] mCellConstrainedDeformation;
	
	/**
	 * The mean density is the optimal density for a cell.
	 */
	double mMeanDensity = -1.0;
	
	
	/**
	 * The size of one cell in x and y direction. This is used for
	 * internal purpose only. Do not modify these values directly.
	 */
	private double mCellSizeX;
	private double mCellSizeY;
	
	
	/**
	 * The bias value is a small value bigger than 0 which is added to
	 * every grid cell in order to avoid computation problems with 0 values.
	 * Additionally, we will rescale all the values in order to have a minimum
	 * value of 10 at least.
	 */
	public double bias = 0.00001;
	public double minValue = 10;
	
	
	
	/**
	 * The constructor for the cartogram grid.
	 */
	CartogramGrid (int gridSizeX, int gridSizeY, Envelope env)
	{
		// Store the attributes.
		this.mGridSizeX = gridSizeX;
		this.mGridSizeY = gridSizeY;
		mEnvelope = env;
		
		// Allocate memory for the grid arrays.
		mNodeX = new double[gridSizeX][gridSizeY];
		mNodeY = new double[gridSizeX][gridSizeY];
		mCellOriginalDensity = new double[gridSizeX-1][gridSizeY-1];
		mCellCurrentDensity = new double[gridSizeX-1][gridSizeY-1];
		mCellConstrainedDeformation = new short[gridSizeX-1][gridSizeY-1];
		
		// Compute the node coordinates.
		this.computeNodeCoordinates();
		
	}	// CartogramGrid.<init>

	
	
	
	/**
	 * Returns the grid's bounding box.
	 * @return an Envelope representing the bounding box.
	 */
	public Envelope envelope ()
	{
		return mEnvelope;
		
	}	// CartogramGrid.envelope
	
	
	
	
	
	/**
	 * Returns the x coordinates array.
	 */
	public double[][] getXCoordinates ()
	{
		return mNodeX;
	}
	
	
	
	/**
	 * Returns the y coordinates array.
	 */
	public double[][] getYCoordinates ()
	{
		return mNodeY;
	}
	
	
	
	/**
	 * Returns the array containing the current densities for the grid.
	 */
	public double[][] getCurrentDensityArray ()
	{
		return mCellCurrentDensity;
	}
	
	
	/**
	 * Returns the cartogram grid size.
	 */
	public Size getGridSize ()
	{
		return new Size(mGridSizeX, mGridSizeY);
	}
	
	
	
	
	/**
	 * Computes the node coordinates and fills them into the
	 * nodeX and nodeY arrays.
	 */
	private void computeNodeCoordinates ()
	{
				
		// Verify the grid size.
		if (mGridSizeX <= 0 || mGridSizeY <= 0)
			return;

		// Compute the size of a cell in x and y.
		mCellSizeX = mEnvelope.getWidth() / (mGridSizeX - 1);
		mCellSizeY = mEnvelope.getHeight() / (mGridSizeY - 1);
		
		double x = mEnvelope.getMinX();
		double y = mEnvelope.getMinY();
		

		// Create all nodes.
		int i, j;
		for (j = 0; j < mGridSizeY; j++)
		{
			for (i = 0; i < mGridSizeX; i++)
			{
				mNodeX[i][j] = x;
				mNodeY[i][j] = y;
				x += mCellSizeX;
			}
				
			x = mEnvelope.getMinX();
			y += mCellSizeY;
		
		}
		
	}	// CartogramGrid.computeNodeCoordinates





	/**
	 * Computes the density value given a layer and an attribute name.
	 * @param layer the master layer
	 * @param attrName the name of the master attribute
	 * @param attrIsDensityValue is true if the master attribute is a density
	 *		  value, and false if it is a population value.
	 */
	public void computeOriginalDensityValuesWithLayer (Layer layer, String attrName, boolean attrIsDensityValue)
	throws InterruptedException, DataFormatException
	{
		// If the attribute is not a density value, we create a new
		// attribute for the computed density value.
		String densityAttrName = attrName;
		if (!attrIsDensityValue) {
			densityAttrName = attrName + "Density";
			CartogramLayer.addDensityAttribute(layer, attrName, densityAttrName);
		}
		
		// Compute the mean density.
		mMeanDensity = CartogramLayer.meanDensityWithAttribute(layer, densityAttrName);
		
		
		// For each Feature in the layer, we find all grid cells which
		// are at least in part inside the Feature. We add the density
		// weighted by the Feature's proportion of coverage of the cell.
		// For this, we set to 0 all optimal density values. At the same time
		// we set the current density value to the mean density value and 
		// the value for constrained deformation to 0.
		
		int i, j;
		for (j = 0; j < (mGridSizeY-1); j++)
		{
			for (i = 0; i < (mGridSizeX-1); i++)
			{
				mCellCurrentDensity[i][j] = mMeanDensity;
				mCellOriginalDensity[i][j] = mMeanDensity;
				mCellConstrainedDeformation[i][j] = -1;
			}
		}
		
		int nFeat = layer.getFeatureCollectionWrapper().size();
		int featCnt = 0;
		
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			
			int progress = 100 + (featCnt * 100 / nFeat);
			
			// Interrupt the process ?
			if (Thread.interrupted())
			{
				// Raise an InterruptedException.
				throw new InterruptedException(
					"Computation has been interrupted by the user.");
			}

			
			AppContext.cartogramWizard.updateRunningStatus(progress,
				"Computing the density for the cartogram grid...",
				"Treating feature " + (featCnt+1) + " of " + nFeat);
				
			Feature feat = (Feature)featIter.next();
			
			this.fillDensityValueWithFeature (feat, densityAttrName);
			
			featCnt++;
		}
		
		
		// Rescale and the bias value to every cell.
		this.rescaleValues();
		this.addBias();
		
	
	}	// CartogramGrid.computeDensityValueWithLayer


	
	
	/**
	 * Rescales the density value to have a value of at least this.minValue
	 * for all non-zero cells.
	 */
	public void rescaleValues () {
		
		// Find out the smallest non-zero value in the grid.
		double minval = 0;
		for (int j = 0; j < (mGridSizeY-1); j++) {
			for (int i = 0; i < (mGridSizeX-1); i++) {
				if (mCellCurrentDensity[i][j] > 0.0 && (minval == 0 || minval > mCellCurrentDensity[i][j])) {
					minval = mCellCurrentDensity[i][j];
				}
			}
		}
		
		// Compute the scaling factor
		if (minval > 0) {
			double factor = this.minValue / minval;
			if (factor > 1) {
				for (int j = 0; j < (mGridSizeY-1); j++) {
					for (int i = 0; i < (mGridSizeX-1); i++) {
						mCellCurrentDensity[i][j] *= factor;
					}
				}
			}
		}
		
		
		
	}
	
	
	
	/**
	 * Adds the bias value to every grid cell.
	 */
	public void addBias () {
		for (int j = 0; j < (mGridSizeY-1); j++) {
			for (int i = 0; i < (mGridSizeX-1); i++) {
				mCellCurrentDensity[i][j] += this.bias;
			}
		}
	}
	
	


	/**
	 * Prepares the original grid for a constrained deformation process.
	 * After this preparation, the grid can be deformed using a cartogram
	 * algorithm. After this deformation, the grid can be corrected using
	 * the constrained deformation information by a call to the CartogramGrid
	 * method conformToConstrainedDeformation.
	 * @param layers a Vector containing the constrained layer names.
	 */
	public void prepareGridForConstrainedDeformation (Vector layers)
	{
	
		if (layers == null)
			return;
	
		// For all cells containing a constrained feature and no deformation
		// feature, we set the constrained cell value to 1.
		// The cell values of 0 are for deformation cells, and -1 for
		// empty cells.
	
		Iterator layerIterator = layers.iterator();
		while (layerIterator.hasNext())
		{
			Layer lyr = (Layer)layerIterator.next();
			
			Iterator featIter = lyr.getFeatureCollectionWrapper().iterator();
			while (featIter.hasNext())
			{
				Feature feat = (Feature)featIter.next();
				this.prepareGridForConstrainedDeformationWithFeature(feat);
			}
			
		}
		
	
	}	// CartogramGrid.prepareGridForConstrainedDeformation
	




	/**
	 * Prepares the grid for constrained deformation using the provided
	 * feature.
	 */
	private void prepareGridForConstrainedDeformationWithFeature (Feature feat)
	{
	
		// Extract the minimum and maximum coordinates from the Feature.
		Geometry geom = feat.getGeometry();
		Envelope featEnv = geom.getEnvelopeInternal();
		
		// Find the minimum and maximum cell indexes for this Feature.
		int minI = this.originalCellIndexForCoordinateX(featEnv.getMinX());
		int minJ = this.originalCellIndexForCoordinateY(featEnv.getMinY());
		int maxI = this.originalCellIndexForCoordinateX(featEnv.getMaxX());
		int maxJ = this.originalCellIndexForCoordinateY(featEnv.getMaxY());
		
		
		// Create a new Geometry Factory.
		// We need to create a new geometry with the cell in order to know
		// whether the cell intersects with the feature.
		GeometryFactory gf = new GeometryFactory();
		
		
		int i, j;
		for (j = minJ; j <= maxJ; j++)
		{
			for (i = minI; i <= maxI; i++)
			{
			
				// We treat this cell only if it does not intersect with
				// a deformation feature or if it is already a constrained
				// deformation cell.
				if (mCellConstrainedDeformation[i][j] == -1)
				{
					double minX = this.coordinateXForOriginalCellIndex(i);
					double maxX = minX + mCellSizeX;
					double minY = this.coordinateYForOriginalCellIndex(j);
					double maxY = minY + mCellSizeY;
				
					Envelope cellEnv = new Envelope(minX, maxX, minY, maxY);
					Geometry cellEnvGeom = gf.toGeometry(cellEnv);
					if (geom.contains(cellEnvGeom) || 
						geom.intersects(cellEnvGeom))
					{
						mCellConstrainedDeformation[i][j] = 1;
					}
				}
				
			}
		}
	
	
	}	// CartogramGrid.prepareGridForConstrainedDeformationWithFeature





	/**
	 * Updates the optimal density value for the grid cells inside
	 * the provided Feature.
	 * @param feat the CartoramFeature which serves as update source.
	 * @param densityAttribute the name of the attribute containing the
	 *        density value for the Feature.
	 */
	private void fillDensityValueWithFeature 
		(Feature feat, String densityAttribute)
	{
		
		// Extract the minimum and maximum coordinates from the Feature.
		Geometry geom = feat.getGeometry();
		Envelope featEnv = feat.getGeometry().getEnvelopeInternal();
		
		// Get the density attribute value.
		double densityValue = 
			CartogramFeature.getAttributeAsDouble(feat, densityAttribute);
		
		
		// Find the minimum and maximum cell indexes for this Feature.
		int minI = this.originalCellIndexForCoordinateX(featEnv.getMinX());
		int minJ = this.originalCellIndexForCoordinateY(featEnv.getMinY());
		int maxI = this.originalCellIndexForCoordinateX(featEnv.getMaxX());
		int maxJ = this.originalCellIndexForCoordinateY(featEnv.getMaxY());
		
		
		// Create a new Geometry Factory.
		GeometryFactory gf = new GeometryFactory();
		
		int i, j;
		for (j = minJ; j <= maxJ; j++)
		{
			for (i = minI; i <= maxI; i++)
			{
				double minX = this.coordinateXForOriginalCellIndex(i);
				//double maxX = minX + mCellSizeX;
				double minY = this.coordinateYForOriginalCellIndex(j);
				//double maxY = minY + mCellSizeY;
				
				double midX = minX + (mCellSizeX / 2);
				double midY = minY + (mCellSizeY / 2);
				
				Point cellPt = gf.createPoint(new Coordinate(midX, midY));
				
				//Envelope cellEnv = new Envelope(minX, maxX, minY, maxY);
				//Geometry cellEnvGeom = gf.toGeometry(cellEnv);
				//if (geom.contains(cellEnvGeom))
				if (geom.contains(cellPt))
				{
					mCellOriginalDensity[i][j] = densityValue;
					mCellCurrentDensity[i][j] = densityValue;
					mCellConstrainedDeformation[i][j] = 0;
				}
				/*else if (geom.intersects(cellEnvGeom))
				{
					// The cell is not completely inside the geometry.
					Geometry intersection = geom.intersection(cellEnvGeom);
					double densityProportion = 
						intersection.getArea() / cellEnvGeom.getArea();
					
					// Add the weighted density value for this feature.
					mCellOriginalDensity[i][j] += 
						densityProportion * densityValue;
					
					// Substract the weighted mean density value which
					// was already in the cell.
					mCellOriginalDensity[i][j] -= 
						densityProportion * mMeanDensity;
					
					// Copy the value to the current density array.
					mCellCurrentDensity[i][j] = mCellOriginalDensity[i][j];
					
					// Before the density computation, the value for
					// the constrained deformation is -1. If this cell
					// is concerned by one of the features, its value
					// becomes 0.
					mCellConstrainedDeformation[i][j] = 0;
						
				}*/
				
			}
		}
	
	}	// CartogramGrid.fillDensityValueWithFeature
	
	
	
	
	
	/**
	 * Corrects the grid for corresponding the constrained deformation
	 * information computed by the prepareGridForConstrainedDeformation 
	 * method.
	 */
	public void conformToConstrainedDeformation ()
	{
	
	
		// Algorithm outline:
		// 1. Identify constrained cells.
		// 2. Is there a node which can move?
		// 3. If yes, where should this node go?
		// 4. Is this movement partially or completely feasible?
		//    (no topologic problem)
		// 5. If yes, move point.
		
	
		int i, j;
		for (j = 0; j < (mGridSizeY-1); j++)
		{
			for (i = 0; i < (mGridSizeX-1); i++)
			{
				
				if (mCellConstrainedDeformation[i][j] == 1)
				{
					
					// Can we move a node ?
					boolean canMove = false;
					
					// If there is a corner, we can move.
					if ((i == 0 && j == 0) || 
						(i == 0 && j == (mGridSizeY-2)) ||
						(i == (mGridSizeX-2) && j == 0) ||
						(i == (mGridSizeX-2) && j == (mGridSizeY-1)))
					{
						canMove = true;
					}
					
					
					// If the cell is on the border but not a corner,
					// we can move depending on the neighbours.
					
					else if (i == 0 || i == (mGridSizeX-2))
					{
						// Left or right border
						if (mCellConstrainedDeformation[i][j+1] != 0 ||
							mCellConstrainedDeformation[i][j-1] != 0)
						{
							canMove = true;
						}
					}
					
					else if (j == 0 || j == (mGridSizeY-2))
					{
						// Lower or upper border
						if (mCellConstrainedDeformation[i-1][j] != 0 ||
							mCellConstrainedDeformation[i+1][j] != 0)
						{
							canMove = true;
						}
					}
					
					
					// If there is an empty cell or a constrained cell
					// in the neighbourhood, we can propably move (it
					// depends on the exact configuration). We have to test
					// for each node of the cell whether it can move or not.
					
					if (i > 0 && j > 0 && 
						i < (mGridSizeX-2) && j < (mGridSizeY-2))
					{

						// Test upper left node.
						if (mCellConstrainedDeformation[i-1][j] != 0 &&
							mCellConstrainedDeformation[i-1][j+1] != 0 &&
							mCellConstrainedDeformation[i][j+1] != 0)
						{
							canMove = true;
						}
					
						// Test upper right node.
						if (mCellConstrainedDeformation[i][j+1] != 0 &&
							mCellConstrainedDeformation[i+1][j+1] != 0 &&
							mCellConstrainedDeformation[i+1][j] != 0)
						{
							canMove = true;
						}
					
						// Test lower left node.
						if (mCellConstrainedDeformation[i-1][j] != 0 &&
							mCellConstrainedDeformation[i-1][j-1] != 0 &&
							mCellConstrainedDeformation[i][j-1] != 0)
						{
							canMove = true;
						}
					
						// Test lower right node.
						if (mCellConstrainedDeformation[i][j-1] != 0 &&
							mCellConstrainedDeformation[i+1][j-1] != 0 &&
							mCellConstrainedDeformation[i+1][j] != 0)
						{
							canMove = true;
						}
					}
					
					
					// Try to apply the constrained deformation to the node.
					if (canMove)
						this.applyConstrainedDeformationToCell(i, j);
					
					
				}
				
			}
		}
	
	}	// CartogramGrid.conformToConstrainedDeformation
	
	
	
	
	
	
	/**
	 * Tries to give the original form to the provided cell.
	 */
	private void applyConstrainedDeformationToCell (int i, int j)
	{
		
		// Compute the location where each of the 4 nodes should go.
		
		// Get the position of each of the 4 nodes.
		double ulx = mNodeX[i][j+1];
		double uly = mNodeY[i][j+1];
		double urx = mNodeX[i+1][j+1];
		double ury = mNodeX[i+1][j+1];
		double lrx = mNodeX[i+1][j];
		double lry = mNodeY[i+1][j];
		double llx = mNodeX[i][j];
		double lly = mNodeY[i][j];
		
		
		// Compute the ideal x/y values for the cell.
		
		double minX = (ulx + llx) / 2;
		double maxX = (urx + lrx) / 2;
		double minY = (lly + lry) / 2;
		double maxY = (uly + ury) / 2;
		
		double edgeLength = Math.sqrt((maxX - minX) * (maxY - minY));
		
		double diffX = edgeLength - (maxX - minX);
		double diffY = edgeLength - (maxY - minY);
		
		minX -= (diffX / 2);
		maxX += (diffX / 2);
		minY -= (diffY / 2);
		maxY += (diffY / 2);
		
		
		
		
		// Try to move each of the 4 nodes to the new position.
		
		// Upper left node
		if ((i == 0 && j == (mGridSizeY-2)) ||
			(i == 0 && mCellConstrainedDeformation[i][j+1] != 0) ||
			(j == (mGridSizeY-2) && mCellConstrainedDeformation[i-1][j] != 0) ||
			(mCellConstrainedDeformation[i-1][j] != 0 &&
			 mCellConstrainedDeformation[i-1][j+1] != 0 &&
			 mCellConstrainedDeformation[i][j+1] != 0))
		{
			this.tryToMoveNode(i, (j+1), minX, maxY);
		}
		
		// Upper right node
		if ((i == (mGridSizeX-2) && j == (mGridSizeY-2)) ||
			(i == (mGridSizeX-2) && mCellConstrainedDeformation[i][j+1] != 0) ||
			(j == (mGridSizeY-2) && mCellConstrainedDeformation[i+1][j] != 0) ||
			(mCellConstrainedDeformation[i+1][j] != 0 &&
			 mCellConstrainedDeformation[i+1][j+1] != 0 &&
			 mCellConstrainedDeformation[i][j+1] != 0))
		{
			this.tryToMoveNode((i+1), (j+1), maxX, maxY);
		}
		
		// Lower right node
		if ((i == (mGridSizeX-2) && j == 0) ||
			(i == (mGridSizeX-2) && mCellConstrainedDeformation[i][j-1] != 0) ||
			(j == 0 && mCellConstrainedDeformation[i+1][j] != 0) ||
			(mCellConstrainedDeformation[i+1][j] != 0 &&
			 mCellConstrainedDeformation[i+1][j-1] != 0 &&
			 mCellConstrainedDeformation[i][j-1] != 0))
		{
			this.tryToMoveNode((i+1), j, maxX, minY);
		}
		
		// Lower left node
		if ((i == 0 && j == 0) ||
			(i == 0 && mCellConstrainedDeformation[i][j-1] != 0) ||
			(j == 0 && mCellConstrainedDeformation[i-1][j] != 0) ||
			(mCellConstrainedDeformation[i][j-1] != 0 &&
			 mCellConstrainedDeformation[i-1][j-1] != 0 &&
			 mCellConstrainedDeformation[i-1][j] != 0))
		{
			this.tryToMoveNode(i, j, minX, minY);
		}

	
	}	// CartogramGrid.applyConstrainedDeformationToNode
	
	
	
	
	
	
	/**
	 * Tries to move the provided node to the provided location.
	 * The decision to move or not depends on the neighbourhood structure.
	 * The topology must be respected in all cases.
	 */
	private void tryToMoveNode (int i, int j, double x, double y)
	{
		
		// Create a polygon with the neighboring nodes.
		// If the new location is inside this polygon, we can potentially
		// move the node. However, we will insure that the point does not
		// move too far. There is a maximum distance which is 1/10 of the
		// original cell size.
		
		double moveDistance = 
			Math.sqrt(((mNodeX[i][j] - x) * (mNodeX[i][j] - x)) +
					  ((mNodeY[i][j] - y) * (mNodeY[i][j] - y)));
		
		
		// If the distance to move is too big, we compute a new, closer
		// location.
		if (moveDistance > (mCellSizeX / (double)10))
		{
			double newMoveDistance = mCellSizeX / (double)10;
			
			double moveVectorX = x - mNodeX[i][j];
			double moveVectorY = y - mNodeY[i][j];
			
			double correctionFactor = newMoveDistance / moveDistance;
			
			x = mNodeX[i][j] + (correctionFactor * moveVectorX);
			y = mNodeY[i][j] + (correctionFactor * moveVectorY);
			moveDistance = newMoveDistance;
		}
		
		
		boolean canMove = true;
		
		if (i > 0)
		{
			if (j < (mGridSizeY-2) && mNodeX[i-1][j+1] >= x)
				canMove = false;
			
			if (mNodeX[i-1][j] >= x) canMove = false;
			
			if (j > 0 && mNodeX[i-1][j-1] >= x)
				canMove = false;
		}
		
		if (i < (mGridSizeX-2))
		{
			if (j < (mGridSizeY-2) && mNodeX[i+1][j+1] <= x)
				canMove = false;
			
			if (mNodeX[i+1][j] <= x) canMove = false;
			
			if (j > 0 && mNodeX[i+1][j-1] <= x)
				canMove = false;
		}
		
		if (j > 0)
		{
			if (i > 0 && mNodeY[i-1][j-1] >= y)
				canMove = false;
			
			if (mNodeY[i][j-1] >= y) canMove = false;
			
			if (i < (mGridSizeX-2) && mNodeY[i+1][j-1] >= y)
				canMove = false;
		}
		
		if (j < (mGridSizeY-2))
		{
			if (i > 0 && mNodeY[i-1][j+1] <= y)
				canMove = false;
			
			if (mNodeY[i][j+1] <= y) canMove = false;
			
			if (i < (mGridSizeX-2) && mNodeY[i+1][j+1] <= y)
				canMove = false;
		}
		
		
		
		if (canMove)
		{
			mNodeX[i][j] = x;
			mNodeY[i][j] = y;
		}
		
	
	}	// CartogramGrid.tryToMoveNode
	
	
	
	
	
	
	/**
	 * Scales the density values given the minimum and maximum value.
	 * @param minValue the new minimum value for the densities.
	 * @param maxValue the new maximum value for the densities.
	 */
	public void scaleDensityValues (double minValue, double maxValue)
	{
	
		// We need to find the minimum and maximum density value in order
		// to find the scaling parameters.
		double minDensity = mCellCurrentDensity[0][0];
		double maxDensity = mCellCurrentDensity[0][0];
		
		int i, j;
		for (j = 0; j < (mGridSizeY-1); j++)
		{
			for (i = 0; i < (mGridSizeX-1); i++)
			{
				if (mCellCurrentDensity[i][j] < minDensity)
					minDensity = mCellCurrentDensity[i][j];
				
				if (mCellCurrentDensity[i][j] > maxDensity)
					maxDensity = mCellCurrentDensity[i][j];
				
				if (mCellOriginalDensity[i][j] < minDensity)
					minDensity = mCellOriginalDensity[i][j];
					
				if (mCellOriginalDensity[i][j] > maxDensity)
					maxDensity = mCellOriginalDensity[i][j];
			}
		}
		
		
		double deltaOldDensity = maxDensity - minDensity;
		double deltaNewDensity = maxValue - minValue;
		double conversionFactor = deltaNewDensity / deltaOldDensity;
		
		for (j = 0; j < (mGridSizeY-1); j++)
		{
			for (i = 0; i < (mGridSizeX-1); i++)
			{
				mCellCurrentDensity[i][j] = 
					((mCellCurrentDensity[i][j] - minDensity) * 
					conversionFactor) + minValue;
				
				mCellOriginalDensity[i][j] = 
					((mCellOriginalDensity[i][j] - minDensity) *
					conversionFactor) + minValue;
			}
		}
		
	
	}	// CartogramGrid.scaleDensityValues
	
	
	
	
	
	
	
	
	/**
	 * Converts the provided x coordinate into the grid's cell index.
	 * @param x the real world x coordinate.
	 * @return the cell index in x direction.
	 */
	public int originalCellIndexForCoordinateX (double x)
	{
		
		if (mEnvelope == null)
			return -1;
		
		if (x == mEnvelope.getMinX())
			return 0;

		double dblCellX = (x - mEnvelope.getMinX()) / mCellSizeX;
		long cellX = Math.round(Math.ceil(dblCellX) - 1);
		int intCellX = (int)cellX;
		return intCellX;

	}	// CartogramGrid.cellIndexForCoordinateX
	
	
	
	
	/**
	 * Converts the provided y coordinate into the grid's cell index.
	 * @param y the real world y coordinate.
	 * @return the cell index in y direction.
	 */
	public int originalCellIndexForCoordinateY (double y)
	{
	
		if (mEnvelope == null)
			return -1;
		
		if (y == mEnvelope.getMinY())
			return 0;

		double dblCellY = (y - mEnvelope.getMinY()) / mCellSizeY;
		long cellY = Math.round(Math.ceil(dblCellY) - 1);
		int intCellY = (int)cellY;
		return intCellY;
	
	}	// CartogramGrid.cellIndexForCoordinateY





	/**
	 * Converts a grid cell index in x direction into real world
	 * x coordinate. The coordinate of the cell's lower left corner
	 * is returned.
	 * @param i the cell index in x direction.
	 * @return the x coordinate of the cell's lower left corner.
	 */
	public double coordinateXForOriginalCellIndex (int i)
	{
	
		if (mEnvelope == null)
			return 0.0;
		
		double x = mEnvelope.getMinX() + (i * mCellSizeX);
		return x;
	
	}	// CartogramGrid.coordinateXForOriginalCellIndex
	
	
	
	
	
	/**
	 * Converts a grid cell index in y direction into real world
	 * y coordinate. The coordinate of the cell's lower left corner
	 * is returned.
	 * @param i the cell index in y direction.
	 * @return the y coordinate of the cell's lower left corner.
	 */
	public double coordinateYForOriginalCellIndex (int j)
	{
		if (mEnvelope == null)
			return 0.0;
		
		double y = mEnvelope.getMinY() + (j * mCellSizeY);
		return y;
		
	}	// CartogramGrid.coordinateYForOriginalCellIndex




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
		// origDens : the orignal density of the cell
		// currDens : the current density of the cell
		// constr : the constrained deformation value of the cell
		fs.addAttribute("cellId", AttributeType.INTEGER);
		fs.addAttribute("geom", AttributeType.GEOMETRY);
		fs.addAttribute("i", AttributeType.INTEGER);
		fs.addAttribute("j", AttributeType.INTEGER);
		fs.addAttribute("origDens", AttributeType.DOUBLE);
		fs.addAttribute("currDens", AttributeType.DOUBLE);
		fs.addAttribute("constr", AttributeType.INTEGER);
		
		// Create a new Geometry Factory for creating our geometries.
		GeometryFactory gf = new GeometryFactory();
		
		// Create a new Feature Dataset in order to store our new Features.
		FeatureDataset fd = new FeatureDataset(fs);
		
		
		// Create one Feature for each cell.
		int i, j;
		int cellId = 0;
		for (j = 0; j < (mGridSizeY - 1); j++)
		{
			for (i = 0; i < (mGridSizeX - 1); i++)
			{
				cellId++;
				
				// Extract the coordinates for the cell polygon.
				Coordinate[] coords = new Coordinate[5];
				coords[0] = new Coordinate(mNodeX[i][j], mNodeY[i][j]);
				coords[1] = new Coordinate(mNodeX[i][j+1], mNodeY[i][j+1]);
				coords[2] = new Coordinate(mNodeX[i+1][j+1], mNodeY[i+1][j+1]);
				coords[3] = new Coordinate(mNodeX[i+1][j], mNodeY[i+1][j]);
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
				feat.setAttribute("origDens", 
					new Double(mCellOriginalDensity[i][j]));
				feat.setAttribute("currDens",
					new Double(mCellCurrentDensity[i][j]));
				feat.setAttribute("constr", 
					new Integer(mCellConstrainedDeformation[i][j]));
				
				// Add the Feature to the Feature Dataset.
				fd.add(feat);
				
			}
		}
		
		
		// Write the Feature Dataset to the Shape file.
		IOManager.writeShapefile(fd, shapefile);
	
			
	}	// CartogramGrid.writeToShapefile
	
	
	
	
	
	/**
	 * Returns the mean density error. The density error is the squared
	 * difference between the current and the desired (optimal) density.
	 * @return the mean density error
	 */
	public double meanDensityError ()
	{
	
		double error = 0.0;
		
		int i, j;
		for (j = 0; j < (mGridSizeY - 1); j++)
		{
			for (i = 0; i < (mGridSizeX - 1); i++)
			{
				double densityDifference = 
					mCellCurrentDensity[i][j] - mCellOriginalDensity[i][j];
				
				error += densityDifference * densityDifference;
			}
		}
		
		error = error / ((mGridSizeX - 1) * (mGridSizeY - 1));
	
		return error;
		
	}
	
	
	
	/**
	 * Updates the current density values.
	 */
	public void updateDensityValues ()
	{
	
		// The original cell area is computed using the cell size.
		double originalCellArea = mCellSizeX * mCellSizeY;
		
		int i, j;
		for (j = 0; j < (mGridSizeY - 1); j++)
		{
			for (i = 0; i < (mGridSizeX - 1); i++)
			{
			
				// Compute the current area of the cell.
				double currentArea = 
					ch.epfl.scapetoad.Geometry.areaOfQuadrangle(
						mNodeX[i][j], mNodeY[i][j],
						mNodeX[i+1][j], mNodeY[i+1][j],
						mNodeX[i+1][j+1], mNodeY[i+1][j+1],
						mNodeX[i][j+1], mNodeY[i][j+1]);
				
				mCellCurrentDensity[i][j] = 
					mCellOriginalDensity[i][j] * originalCellArea / currentArea;
				
			}
		}
	
	}	// CartogramGrid.updateDensityValues
	




	/**
	 * Fills a regular grid with the mean density.
	 * If there is no information, the mean density for the whole grid
	 * is assumed to be the desired value.
	 */
	public void fillRegularDensityGrid (double[][] densityGrid, 
		double minX, double maxX, double minY, double maxY)
	{
	
		int i,j;
		
		// Compute the grid size.
		int gridSizeX = densityGrid.length;
		int gridSizeY = densityGrid[0].length;		
		
		
		// Compute the width, height and cell size of the density grid.
		double gridWidth = maxX - minX;
		double gridHeight = maxY - minY;
		double cellSizeX = gridWidth / gridSizeX;
		double cellSizeY = gridHeight / gridSizeY;
		
	
		// For each node at the lower left corner of a cell, 
		// we compute the regular grid cells concerned by the 
		// cartogram grid cell.
		
		
		// Initialize the counting grid and the density grid.
		short[][] cntgrid = new short[gridSizeX][gridSizeY];
		for (i = 0; i < gridSizeX; i++)
		{
			for (j = 0; j < gridSizeY; j++)
			{
				densityGrid[i][j] = 0;
				cntgrid[i][j] = 0;
			}
		}
		
		
		for (i = 0; i < (mGridSizeX - 1); i++)
		{
			for (j = 0; j < (mGridSizeY - 1); j++)
			{
				
				// Compute the cell index in which the node is located.
				
				int llx = (int)Math.round(Math.floor(
					((mNodeX[i][j] - minX) / cellSizeX)));
				int lly = (int)Math.round(Math.floor(
					((mNodeY[i][j] - minY) / cellSizeY)));
				
				int lrx = (int)Math.round(Math.floor(
					((mNodeX[i+1][j] - minX) / cellSizeX)));
				int lry = (int)Math.round(Math.floor(
					((mNodeY[i+1][j] - minY) / cellSizeY)));
				
				int urx = (int)Math.round(Math.floor(
					((mNodeX[i+1][j+1] - minX) / cellSizeX)));
				int ury = (int)Math.round(Math.floor(
					((mNodeY[i+1][j+1] - minY) / cellSizeY)));
				
				int ulx = (int)Math.round(Math.floor(
					((mNodeX[i][j+1] - minX) / cellSizeX)));
				int uly = (int)Math.round(Math.floor(
					((mNodeY[i][j+1] - minY) / cellSizeY)));
				
				
				int x, y;
				int minx = Math.max(Math.min(llx, ulx), 0);
				int maxx = Math.min(Math.max(lrx, urx), (gridSizeX - 1));
				int miny = Math.max(Math.min(lly, lry), 0);
				int maxy = Math.min(Math.max(uly, ury), (gridSizeY - 1));
				for (x = minx; x <= maxx; x++)
				{
					for (y = miny; y <= maxy; y++)
					{
						densityGrid[x][y] += mCellCurrentDensity[i][j];
						cntgrid[x][y]++;
					}
				}
				
				
			}
		}
		
		
		for (i = 0; i < gridSizeX; i++)
		{
			for (j = 0; j < gridSizeY; j++)
			{
			
				if (cntgrid[i][j] == 0)
					densityGrid[i][j] = mMeanDensity;
				else
					densityGrid[i][j] /= cntgrid[i][j];
					
			}
		}
		
	
	}	// CartogramGrid.fillRegularDensityGrid
	
	
	
	
	
	/**
	 * Projects one point using this grid.
	 * @param x the x coordinate of the point to project.
	 * @param y the y coordinate of the point to project.
	 * @return a double array with the coordinates of the projected point.
	 */
	public double[] projectPoint (double x, double y)
	{
		double p1x = 
			(x - mEnvelope.getMinX()) * mGridSizeX / mEnvelope.getWidth();
			
		double p1y = 
			(y - mEnvelope.getMinY()) * mGridSizeY / mEnvelope.getHeight();
			
		int i = (int)Math.round(Math.floor(p1x));
		int j = (int)Math.round(Math.floor(p1y));

		
		if (i < 0) i = 0;
		if (i >= mGridSizeX-1) i = mGridSizeX - 2;
		if (j < 0) j = 0;
		if (j >= mGridSizeY-1) j = mGridSizeY - 2;
		
		/*if (i < 0 || i >= (mGridSizeX-1) || j < 0 || j >= (mGridSizeY-1))
		{
			System.out.println(
				"[CartogramGrid projectPoint] Coordinate outside bounds.");
			return null;
		}*/
		
		double ti = p1x - i;
		double tj = p1y - j;


		double ax = mNodeX[i][j];
		double ay = mNodeY[i][j];
		double bx = mNodeX[i+1][j];
		double by = mNodeY[i+1][j];
		double cx = mNodeX[i+1][j+1];
		double cy = mNodeY[i+1][j+1];
		double dx = mNodeX[i][j+1];
		double dy = mNodeY[i][j+1];
		
		double ex = ax + ti * (bx - ax);
		double ey = ay + ti * (by - ay);
		double fx = bx + tj * (cx - bx);
		double fy = by + tj * (cy - by);
		double gx = dx + ti * (cx - dx);
		double gy = dy + ti * (cy - dy);
		double hx = ax + tj * (dx - ax);
		double hy = ay + tj * (dy - ay);
		
		double[] s = ch.epfl.scapetoad.Geometry.intersectionOfSegments(
			ex, ey, gx, gy, fx, fy, hx, hy);
		
		return s;

	}	// CartogramGrid.projectPoint






	/**
	 * Projects one point using this grid.
	 * @param x the x coordinate of the point to project.
	 * @param y the y coordinate of the point to project.
	 * @return a Coordinate with the projected point.
	 */
	public Coordinate projectPointAsCoordinate (double x, double y)
	{
	
		double[] coord = this.projectPoint(x, y);
		Coordinate c = new Coordinate(coord[0], coord[1]);
		return c;

	}	// CartogramGrid.projectPoint
	
	
	


	/**
	 * Projects a line segment. Returns two or more coordinates.
	 */
	public Coordinate[] projectLineSegment (Coordinate c1, Coordinate c2)
	{
		
		// Compute the index of the grid cells for each coordinate.
		double d1x = (c1.x - mEnvelope.getMinX()) / mCellSizeX;
		double d1y = (c1.y - mEnvelope.getMinY()) / mCellSizeY;
		double d2x = (c2.x - mEnvelope.getMinX()) / mCellSizeX;
		double d2y = (c2.y - mEnvelope.getMinY()) / mCellSizeY;
		
		int i1x = (int)Math.round(Math.floor(d1x));
		int i1y = (int)Math.round(Math.floor(d1y));
		int i2x = (int)Math.round(Math.floor(d2x));
		int i2y = (int)Math.round(Math.floor(d2y));
		if ((d1x - i1x) > 0.99) i1x++;
		if ((d1y - i1y) > 0.99) i2y++;
		if ((d2x - i2x) > 0.99) i2x++;
		if ((d2y - i2y) > 0.99) i2y++;
		
		// Get the minimum and maximum index for x and y.
		int iminx = Math.min(i1x, i2x);
		int imaxx = Math.max(i1x, i2x);
		int iminy = Math.min(i1y, i2y);
		int imaxy = Math.max(i1y, i2y);
		
		
		// Compute the parameters a and b of the equation :
		//  y = a*x + b
		double d = d2x - d1x;
		double a = 0, b = 0;
		boolean aIsInfinite = false;
		if (d > 0.0001 || d < -0.0001)
		{
			a = (d2y - d1y) / (d2x - d1x);
			b = d2y - ( d2x * (d2y - d1y) / (d2x - d1x) );
		}
		else
			aIsInfinite = true;
		
		
		// Compute the number of intersections and allocate the t value array.
		int nIntersections = (imaxx - iminx) + (imaxy - iminy);
		double[] tValues = new double[nIntersections];
		
		// For each intersection, compute the t value (between 0 and 1).
		int tcnt = 0;
		int i;
		for (i = (iminx+1); i <= imaxx; i++)
		{
			if (!aIsInfinite)
			{
				// Compute the y coordinate for each intersection with 
				// a vertical grid line.
				double sy = a*i + b;
			
				// Compute the t value for the intersection point S(i,sy).
				tValues[tcnt] = 
					Math.sqrt( (i-d1x)*(i-d1x) + (sy-d1y)*(sy-d1y) ) /
					Math.sqrt( (d2x-d1x)*(d2x-d1x) + (d2y-d1y)*(d2y-d1y) );
			
				tcnt++;
			}
			else
			{
				System.out.println("a is infinite");
			}
			
		}
		
		for (i = (iminy+1); i <= imaxy; i++)
		{
			// Compute the x coordinate for each intersection with 
			// a horizontal grid line.
			double sx;
			if (!aIsInfinite)
				sx = (i - b) / a;
			else
				sx = (d1x + d2x) / 2;
			
			// Compute the t value for the intersection point S(i,sy).
			tValues[tcnt] = 
				Math.sqrt( (sx-d1x)*(sx-d1x) + (i-d1y)*(i-d1y) ) / 
				Math.sqrt( (d2x-d1x)*(d2x-d1x) + (d2y-d1y)*(d2y-d1y) );
			
			tcnt++;
		}
		
		
		// Sort the array of t values.
		Arrays.sort(tValues);
		
		
		
		// Project all coordinate points.
		
		Coordinate[] coords = new Coordinate[(2 + nIntersections)];
		coords[0] = this.projectPointAsCoordinate(c1.x, c1.y);
		
		tcnt = 1;
		for (i = 0; i < nIntersections; i++)
		{
			// Compute the coordinates of the given intersection using
			// the associated t value.
			// Compute only if the t value is between 0 and 1.
			if (tValues[i] > 0 && tValues[i] < 1)
			{
				double sx = c1.x + tValues[i]*(c2.x - c1.x);
				double sy = c1.y + tValues[i]*(c2.y - c1.y);
				coords[tcnt] = this.projectPointAsCoordinate(sx, sy);
				tcnt++;
			}
		}
		
		coords[tcnt] = this.projectPointAsCoordinate(c2.x, c2.y);
		
		return coords;
		
	}	// CartogramGrid.projectLineSegment
	
	
	

	
	
	/**
	 * Projects a coordinate sequence using this grid.
	 */
	public Coordinate[] projectCoordinates (Coordinate[] coords)
	{
		int ncoords = coords.length;
		Vector projCoords = new Vector();
		
		// Project each line segment in the coordinate sequence.
		int i, j, nProjCoords = 0;
		Coordinate[] cs = null;
		for (i = 0; i < (ncoords-1); i++)
		{
			cs = this.projectLineSegment(coords[i], coords[i+1]);
			
			// Copy the coordinates into a Vector.
			// Don't copy the last coordinate, otherwise it will be twice
			// in the vector. Instead, we add the last coordinate at the end
			// of the process.
			nProjCoords = cs.length;
			for (j = 0; j < nProjCoords; j++)
			{
				if (cs[j] != null)
					projCoords.add(cs[j]);
			}
			if (i < (ncoords-2))
				projCoords.removeElementAt(projCoords.size()-1);
			
		}
		
		// Add the last coordinate.
		//projCoords.add(cs[(nProjCoords - 1)]);
		
		// Transform the Vector into an array.
		nProjCoords = projCoords.size();
		cs = new Coordinate[nProjCoords];
		for (i = 0; i < nProjCoords; i++)
		{
			cs[i] = (Coordinate)projCoords.get(i);
		}
		
		
		return cs;
		
	}	// CartogramGrid.projectCoordinates
	
	
	
	
	
	/**
	 * Returns the current minimum density value of the grid.
	 * @return the minimum density value.
	 */
	public double getMinimumDensity ()
	{
		
		double minDensity = mCellCurrentDensity[0][0];
	
		for (int j = 0; j < (mGridSizeY-1); j++)
		{
			for (int i = 0; i < (mGridSizeX-1); i++)
			{
				if (minDensity > mCellCurrentDensity[i][j])
					minDensity = mCellCurrentDensity[i][j];
			}
		}
		
		return minDensity;
		
	}	// CartogramGrid.getMinimumDensity
	
	
	
	/**
	 * Returns the current maximum density value of the grid.
	 * @return the maximum density value.
	 */
	public double getMaximumDensity ()
	{
		double maxDensity = mCellCurrentDensity[0][0];
	
		for (int j = 0; j < (mGridSizeY-1); j++)
		{
			for (int i = 0; i < (mGridSizeX-1); i++)
			{
				if (maxDensity < mCellCurrentDensity[i][j])
					maxDensity = mCellCurrentDensity[i][j];
			}
		}
		
		return maxDensity;
		
	}	// CartogramGrid.getMaximumDensity
	
	
	
	
	

}	// CartogramGrid






