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

import java.awt.Color;
import java.awt.Font;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import com.sun.swing.SwingWorker;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;




/**
 * The cartogram class is the main computation class.
 * It is a subclass of the SwingWorker class.
 * It has methods for setting all the parameters and for
 * launching the computation.
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-30
 */
public class Cartogram extends com.sun.swing.SwingWorker
{

	/**
	 * The cartogram wizard. We need the wizard reference for updating
	 * the progress status informations.
	 */
	CartogramWizard mCartogramWizard = null;
	
	/**
	 * The layer manager used for cartogram computation.
	 */
	LayerManager mLayerManager = null;
	
	
	/**
	 * The category name for our cartogram layers.
	 */
	String mCategoryName = null;
	
	
	/**
	 * The name of the master layer.
	 */
	String mMasterLayer = null;

	/**
	 * The name of the master attribute.
	 */
	String mMasterAttribute = null;
	
	/**
	 * Is the master attribute already a density value, or must
	 * the value be weighted by the polygon area (only available
	 * for polygons).
	 */
	boolean mMasterAttributeIsDensityValue = true;
	
	
	String mMissingValue = "";
	
	
	
	/**
	 * The projected master layer. We store this in order to make the
	 * computation report after the projection.
	 */
	Layer mProjectedMasterLayer = null;
	
	
	
	/**
	 * The layers to deform simultaneously.
	 */
	Vector mSlaveLayers = null;
	
	/**
	 * The layers used for the constrained deformation.
	 */
	Vector mConstrainedDeforamtionLayers = null;
	
	/**
	 * The initial envelope for all layers.
	 */
	Envelope mEnvelope = new Envelope(0.0, 1.0, 0.0, 1.0);
	
	/**
	 * The size of the cartogram grid.
	 */
	int mGridSizeX = 1000;
	int mGridSizeY = 1000;
	
	/**
	 * All the deformation is done on this cartogram grid.
	 */
	CartogramGrid mGrid = null;
	
	/**
	 * The amount of deformation is a simple stopping criterion.
	 * It is an integer value between 0 (low deformation, early stopping)
	 * and 100 (high deformation, late stopping).
	 */
	int mAmountOfDeformation = 50;
	
	
	/**
	 * Are the advanced options enabled or should the parameters be estimated
	 * automatically by the program?
	 */
	boolean mAdvancedOptionsEnabled = false;
	
	/**
	 * If the advanced options are enabled, this is the grid size for the
	 * diffusion algorithm.
	 */
	//int mDiffusionGridSize = 128;
	
	/**
	 * If the advanced options are enabled, this is the number of iterations
	 * the diffusion algorithm is run on the cartogram grid.
	 */
	//int mDiffusionIterations = 3;
	
	
	/**
	 * The maximum running time in seconds. After this amount of time,
	 * the cartogram computation is finalized. This is to avoid that the
	 * computation lasts for a very very long time.
	 * The default value is 3 hours.
	 */
	int mMaximumRunningTime = 10800;
	
	/**
	 * The maximum length of one line segment. In the projection process,
	 * a straight line might be deformed to a curve. If a line segment is
	 * too long, it might result in a self intersection, especially for
	 * polygons. This parameter can be controlled manually or estimated
	 * using the maximumSegmentLength heuristic.
	 */
	double mMaximumSegmentLength = 500;
	
	


	/**
	 * Should we create a grid layer ?
	 */
	boolean mCreateGridLayer = true;
	
	/**
	 * The size of the grid which can be added as a deformation grid.
	 */
	int mGridLayerSize = 100;
	
	/**
	 * The layer containing the deformation grid.
	 */
	Layer mDeformationGrid = null;
	

	/**
	 * Should we create a legend layer ?
	 */
	boolean mCreateLegendLayer = true;
	
	/**
	 * An array containing the legend values which should be represented
	 * in the legend layer.
	 */
	double[] mLegendValues = null;
	
	/**
	 * The layer containing the cartogram legend.
	 */
	Layer mLegendLayer = null;
	
	
	
	/**
	 * The computation report.
	 */
	String mComputationReport = "";
	
	
	/**
	 * Used for storing the start time of computation. The computation
	 * duration is computed based on this value which is set before starting
	 * the compuation.
	 */
	long mComputationStartTime = 0;
	
	
	
	/**
	 * The cartogram bias value used in the cartogram grid.
	 */
	public double bias = 0.000001;
	
	
	boolean errorOccured = false;
	
	
	
	/**
	 * The constructor for the cartogram class.
	 */
	Cartogram (CartogramWizard cartogramWizard)
	{
	
		// Storing the cartogram wizard reference.
		mCartogramWizard = cartogramWizard;
		
	}	// Cartogram.<init>
	
	
	
	/**
	 * The construct method is an overriden method from
	 * SwingWorker which does initiate the computation process.
	 */
	public Object construct() {
	
		try {
	
			mComputationStartTime = System.nanoTime();
			
			if (mAdvancedOptionsEnabled == false) {
				// Automatic estimation of the parameters using the amount of deformation slider.
				// The deformation slider modifies only the grid size between 500 (quality 0) and 3000 (quality 100).
				mGridSizeX = (mAmountOfDeformation * 15) + 250;
				mGridSizeY = mGridSizeX;
			}
	
			// User information.
			mCartogramWizard.updateRunningStatus(0, 
				"Preparing the cartogram computation...", 
				"Computing the cartogram bounding box");
			
			// Compute the envelope given the initial layers.
			// The envelope will be somewhat larger than just the layers.
			this.updateEnvelope();
			
			// Adjust the cartogram grid size in order to be proportional
			// to the envelope.
			this.adjustGridSizeToEnvelope();
			if (AppContext.DEBUG) {
				System.out.println("Adjusted grid size: " + mGridSizeX + "x" + 
					mGridSizeY);
			}
			
			mCartogramWizard.updateRunningStatus(20, 
				"Preparing the cartogram computation...", 
				"Creating the cartogram grid");
			
			// Create the cartogram grid.
			mGrid = new CartogramGrid(mGridSizeX, mGridSizeY, mEnvelope);
			
			if (Thread.interrupted())
			{
				// Raise an InterruptedException.
				throw new InterruptedException(
					"Computation has been interrupted by the user.");
			}
				
			
			
			// Check the master attribute for invalid values.
			
			mCartogramWizard.updateRunningStatus(50,
				"Check the cartogram attribute values...",
				"");
			
			Layer masterLayer = AppContext.layerManager.getLayer(mMasterLayer);
			CartogramLayer.cleanAttributeValues(masterLayer, mMasterAttribute);
			
			
			
			// Replace the missing values with the layer mean value.
			if (mMissingValue != "" && mMissingValue != null)
			{
				double mean = CartogramLayer.meanValueForAttribute(
					masterLayer, mMasterAttribute);
				
				Double missVal = new Double(mMissingValue);
				
				CartogramLayer.replaceAttributeValue(masterLayer, 
					mMasterAttribute, missVal.doubleValue(), mean);
			}
			
			
			
			
			// Compute the density values for the cartogram grid using
			// the master layer and the master attribute.
			
			mCartogramWizard.updateRunningStatus(100,
				"Computing the density for the cartogram grid...",
				"");
			
			mGrid.computeOriginalDensityValuesWithLayer
				(masterLayer, 
				 mMasterAttribute, 
				 mMasterAttributeIsDensityValue);
			
			if (Thread.interrupted())
			{
				// Raise an InterruptedException.
				throw new InterruptedException(
					"Computation has been interrupted by the user.");
			}
			
			
			
			
			
			// *** PREPARE THE GRID FOR THE CONSTRAINED DEFORMATION ***
			
			if (mConstrainedDeforamtionLayers != null)
			{
				mCartogramWizard.updateRunningStatus(300,
					"Prepare constrained deformation...",
					"");
				
				mGrid.prepareGridForConstrainedDeformation(
					mConstrainedDeforamtionLayers);
			}
			
			if (Thread.interrupted())
			{
				// Raise an InterruptedException.
				throw new InterruptedException(
					"Computation has been interrupted by the user.");
			}



			// *** COMPUTE THE CARTOGRAM USING THE DIFFUSION ALGORITHM ***
			
			mCartogramWizard.updateRunningStatus(350, "Computing cartogram diffusion...", 
												 "Starting the diffusion process");
			CartogramNewman cnewm = new CartogramNewman(mGrid);
			
			// Enable the CartogramNewman instance to update the running status.
			cnewm.runningStatusWizard = mCartogramWizard;
			cnewm.runningStatusMinimumValue = 350;
			cnewm.runningStatusMaximumValue = 700;
			cnewm.runningStatusMainString = "Computing cartogram diffusion...";
			
			// Let's go!
			cnewm.compute();
			
			if (Thread.interrupted())
			{
				// Raise an InterruptedException.
				throw new InterruptedException("Computation has been interrupted by the user.");
			}
			
			
			
			
			
			// *** CONSTRAINED DEFORMATION ***
			if (mConstrainedDeforamtionLayers != null)
			{
				mCartogramWizard.updateRunningStatus(700, 
					"Applying the constrained deformation layers", 
					"");
			
				mGrid.conformToConstrainedDeformation();
			}
			
			
			if (Thread.interrupted())
			{
				// Raise an InterruptedException.
				throw new InterruptedException(
					"Computation has been interrupted by the user.");
			}
			
			
			
			
			// *** PROJECTION OF ALL LAYERS ***
			
			mCartogramWizard.updateRunningStatus(750,
				"Projecting the layers...",
				"");

			Layer[] projLayers = this.projectLayers();
			
			if (Thread.interrupted()) {
				// Raise an InterruptedException.
				throw new InterruptedException("Computation has been interrupted by the user.");
			}
			
			// *** CREATE THE DEFORMATION GRID LAYER ***
			if (mCreateGridLayer)
				this.createGridLayer();
			
			// *** CREATE THE LEGEND LAYER ***
			if (mCreateLegendLayer) {
				this.createLegendLayer();
			}
			
			mCartogramWizard.updateRunningStatus(950,
				"Producing the comutation report...",
				"");
			
			return projLayers;
		
		} catch (Exception e) {
		
			String exceptionType = e.getClass().getName();
			
			if (exceptionType == "java.lang.InterruptedException") {
				mCartogramWizard.setComputationError(
					"The cartogram computation has been cancelled.",
					"",
					"");
				errorOccured = true;
			} else if (exceptionType == "java.util.zip.DataFormatException") {
				// Retrieve the complete stack trace and display.
				/*StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw, true);
				e.printStackTrace(pw);
				pw.flush();
				sw.flush();*/
				mCartogramWizard.setComputationError(
					"An error occured during cartogram computation!",
					"All attribute values are zero",
					//sw.toString()
					e.getMessage()
				);
				errorOccured = true;
			} /*else {
				// Retrieve the complete stack trace and display.
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw, true);
				e.printStackTrace(pw);
				pw.flush();
				sw.flush();
			
				mCartogramWizard.setComputationError(
					"An error occured during cartogram computation!",
					e.getLocalizedMessage(),
					sw.toString());
			}*/
			
			mCartogramWizard.goToFinishedPanel();
			
			
			/*if (AppContext.DEBUG) {
				System.out.println("Error during cartogram computation!");
				e.printStackTrace();
			}*/
			return null;
		}
		
		
	}	// Cartogram.construct
	
	
	
	
	
	
	/**
	 * This method is called once the construct method has finished.
	 * It terminates the computation, adds all layers and produces the
	 * computation report.
	 */
	public void finished ()
	{
	
		// If there was an error, stop here.
		if (errorOccured) {
			return;
		}
		
		
		// *** GET THE PROJECTED LAYERS ***
		
		Layer[] lyr = (Layer[])this.get();

		if (lyr == null)
		{
			mCartogramWizard.setComputationError("An error occured during cartogram computation!", 
												 "", 
												 "An unknown error has occured.\n\nThere may be unsufficient memory resources available. Try to:\n\n1.\tUse a smaller cartogram grid (through the transformation\n\tquality slider at the wizard step 5, or through the\n\t\"Advanced options...\" button, also at step 5.\n\n2.\tYou also may to want to increase the memory available\n\tto ScapeToad. To do so, you need the cross platform\n\tJAR file and \n\tlaunch ScapeToad from the command\n\tline, using the -Xmx flag of \n\tyour Java Virtual\n\tMachine. By default, ScapeToad has 1024 Mo of memory.\n\tDepending on your system, there may be less available.\n\n3.\tIf you think there is a bug in ScapeToad, you can file\n\ta bug \n\ton Sourceforge \n\t(http://sourceforge.net/projects/scapetoad).\n\tPlease describe in detail your problem and provide all\n\tnecessary \n\tdata for reproducing your error.\n\n");
			mCartogramWizard.goToFinishedPanel();
			return;
		}
		
		
		// *** HIDE ALL LAYERS ALREADY PRESENT ***
		List layerList = mLayerManager.getLayers();
		Iterator layerIter = layerList.iterator();
		while (layerIter.hasNext())
		{
			Layer l = (Layer)layerIter.next();
			l.setVisible(false);
		}
		
		
		
		// *** ADD ALL THE LAYERS ***
		
		String catName = this.getCategoryName();
		
		if (mLayerManager.getCategory(catName) == null)
			mLayerManager.addCategory(catName);
			
			
		int nlyrs = lyr.length;
		for (int lyrcnt = 0; lyrcnt < nlyrs; lyrcnt++)
		{
			mLayerManager.addLayer(catName, lyr[lyrcnt]);
		}
		
		if (mDeformationGrid != null)
			mLayerManager.addLayer(catName, mDeformationGrid);
		
		if (mLegendLayer != null)
			mLayerManager.addLayer(catName, mLegendLayer);
			
			
			
		
		// *** PRODUCE THE COMPUTATION REPORT ***
		this.produceComputationReport(mProjectedMasterLayer);
		
		
		// *** CREATE A THEMATIC MAP USING THE SIZE ERROR ATTRIBUTE ***
		
		// Create a color table for the size error attribute.
		
		BasicStyle bs = 
			(BasicStyle)mProjectedMasterLayer.getStyle(BasicStyle.class);
		bs.setFillColor(Color.WHITE);
		
		SizeErrorStyle errorStyle = new SizeErrorStyle();
		
		errorStyle.setAttributeName("SizeError");
		
		errorStyle.addColor(new BasicStyle(new Color(91, 80, 153)));
		errorStyle.addColor(new BasicStyle(new Color(133, 122, 179)));
		errorStyle.addColor(new BasicStyle(new Color(177, 170, 208)));
		errorStyle.addColor(new BasicStyle(new Color(222, 218, 236)));
		errorStyle.addColor(new BasicStyle(new Color(250, 207, 187)));
		errorStyle.addColor(new BasicStyle(new Color(242, 153, 121)));
		errorStyle.addColor(new BasicStyle(new Color(233, 95, 64)));
		
		errorStyle.addLimit(new Double(70));
		errorStyle.addLimit(new Double(80));
		errorStyle.addLimit(new Double(90));
		errorStyle.addLimit(new Double(100));
		errorStyle.addLimit(new Double(110));
		errorStyle.addLimit(new Double(120));
		


		lyr[0].addStyle(errorStyle);
		errorStyle.setEnabled(true);
		lyr[0].getStyle(BasicStyle.class).setEnabled(false);
		
		
		AppContext.sizeErrorLegend.setVisible(true);
		
		try
		{
			AppContext.layerViewPanel.getViewport().zoomToFullExtent();
		} catch (Exception exc) {}
		
		
		// *** SHOW THE FINISHED PANEL
		
		mCartogramWizard.goToFinishedPanel();
		
		
		
	}
	
	
	
	
	/**
	 * Sets the layer manager.
	 */
	public void setLayerManager (LayerManager lm)
	{
		mLayerManager = lm;
	}
	
	
	
	/**
	 * Sets the name of the cartogram master layer.
	 */
	public void setMasterLayer (String layerName)
	{
		mMasterLayer = layerName;
	}
	
	
	
	/**
	 * Sets the name of the cartogram master attribute.
	 */
	public void setMasterAttribute (String attributeName)
	{
		mMasterAttribute = attributeName;
	}
	
	
	
	
	/**
	 * Lets define us whether the master attribute is a density value
	 * or a population value.
	 */
	public void setMasterAttributeIsDensityValue (boolean isDensityValue)
	{
		mMasterAttributeIsDensityValue = isDensityValue;
	}
	
	
	
	/**
	 * Defines the layers to deform during the
	 * cartogram process.
	 */
	public void setSlaveLayers (Vector slaveLayers)
	{
		mSlaveLayers = slaveLayers;
	}

	
	
	/**
	 * Defines the layers which should not be deformed.
	 */
	public void setConstrainedDeformationLayers (Vector layers)
	{
		mConstrainedDeforamtionLayers = layers;
	}
	
	
	
	/**
	 * Defines the grid size in x and y dimensions.
	 */
	public void setGridSize (int x, int y)
	{
		mGridSizeX = x;
		mGridSizeY = y;
	}
	
	
	/**
	 * Defines the amount of deformation. This is an integer value between
	 * 0 and 100. The default value is 50.
	 */
	public void setAmountOfDeformation (int deformation)
	{
		mAmountOfDeformation = deformation;
	}
	
	
	/**
	 * Defines the maximum running time in seconds. The default value is
	 * 259200 seconds (3 days).
	 */
	public void setMaximumRunningTime (int seconds)
	{
		mMaximumRunningTime = seconds;
	}




	/**
	 * Computes the cartogram envelope using the provided layers.
	 * The envelope will be larger than the layers in order to allow
	 * the cartogram deformation inside this envelope.
	 */
	private void updateEnvelope ()
	{
		
		// Setting the initial envelope using the master layer.
		Layer lyr = mLayerManager.getLayer(mMasterLayer);
		Envelope masterEnvelope = 
			lyr.getFeatureCollectionWrapper().getEnvelope();
			
		mEnvelope = new Envelope(
			masterEnvelope.getMinX(),
			masterEnvelope.getMaxX(),
			masterEnvelope.getMinY(),
			masterEnvelope.getMaxY());
		
		
		// Expanding the initial envelope using the slave and
		// constrained deformation layers.
		if (mSlaveLayers != null)
		{
			Iterator lyrIterator = mSlaveLayers.iterator();
			while (lyrIterator.hasNext())
			{
				lyr = (Layer)lyrIterator.next();
				mEnvelope.expandToInclude(
					lyr.getFeatureCollectionWrapper().getEnvelope());
			}
		}
		
		if (mConstrainedDeforamtionLayers != null)
		{
			Iterator lyrIterator = mConstrainedDeforamtionLayers.iterator();
			while (lyrIterator.hasNext())
			{
				lyr = (Layer)lyrIterator.next();
				mEnvelope.expandToInclude(
					lyr.getFeatureCollectionWrapper().getEnvelope());
			}
		}
		
		
		// Enlarge the envelope by 50%.
		mEnvelope.expandBy(mEnvelope.getWidth() * 0.2, 
			mEnvelope.getHeight() * 0.2);
		
		
	}	// Cartogram.updateEnvelope
	
	
	
	
	/**
	 * Adjusts the grid size in order to be proportional to the
	 * envelope. It will not increase the grid size, but it will
	 * decrease the grid size on the shorter side.
	 */
	private void adjustGridSizeToEnvelope ()
	{
	
		if (mEnvelope == null)
			return;
			
			
		double width = mEnvelope.getWidth();
		double height = mEnvelope.getHeight();
				
		
		if (width < height)
		{
			// Adjust the x grid size.
			mGridSizeX = (int)Math.round(mGridSizeY * (width / height));
		}
		else if (width > height)
		{
			// Adjust the y grid size.
			mGridSizeY = (int)Math.round(mGridSizeX * (height / width));
		}
		
		

		
		
	}	// Cartogram.adjustGridSizeToEnvelope





	/**
	 * Projects all layers. Creates a new layer for each projected layer.
	 */
	private Layer[] projectLayers () 
	throws Exception
	{
		
		// Get the number of layers to project
		// (one master layer and all slave layers).
		int nlyrs = 1;
		if (mSlaveLayers != null)
			nlyrs += mSlaveLayers.size();
		
		
		// We store the projected layers in an array.
		Layer[] layers = new Layer[nlyrs];
		
		
		// Compute the maximum segment length for the layers.
		mMaximumSegmentLength = this.estimateMaximumSegmentLength();
		
		
		// Project the master layer.
		
		mCartogramWizard.updateRunningStatus(750,
			"Projecting the layers...",
			"Layer 1 of "+ nlyrs);
				
		
		Layer masterLayer = mLayerManager.getLayer(mMasterLayer);
		CartogramLayer.regularizeLayer(masterLayer, mMaximumSegmentLength);
		mProjectedMasterLayer = 
			CartogramLayer.projectLayerWithGrid(masterLayer, mGrid);
		
		layers[0] = mProjectedMasterLayer;
		
		
		
		if (Thread.interrupted())
		{
			// Raise an InterruptedException.
			throw new InterruptedException(
				"Computation has been interrupted by the user.");
		}
			
			
		// Project the slave layers.
		for (int lyrcnt = 0; lyrcnt < (nlyrs - 1); lyrcnt++)
		{
			mCartogramWizard.updateRunningStatus(800 + ((lyrcnt+1)/(nlyrs-1)*150),
			"Projecting the layers...",
			"Layer "+ (lyrcnt+2) +" of "+ nlyrs);
		
			Layer slaveLayer = (Layer)mSlaveLayers.get(lyrcnt);
			CartogramLayer.regularizeLayer(slaveLayer, mMaximumSegmentLength);
			layers[lyrcnt+1] =
				CartogramLayer.projectLayerWithGrid(slaveLayer, mGrid);
		}
		
		
		return layers;
	
	}	// Cartogram.projectLayers
	
	
	
	
	
	
	/**
	 * Says whether we should create a grid layer or not.
	 */
	public boolean getCreateGridLayer ()
	{
		return mCreateGridLayer;
	}
	
	
	
	/**
	 * Sets the flag for creating or not a grid layer.
	 */
	public void setCreateGridLayer (boolean createGridLayer)
	{
		mCreateGridLayer = createGridLayer;
	}
	
	
	
	/**
	 * Returns the grid layer size. This is the grid which is produced
	 * for visual effect only.
	 */
	public int getGridLayerSize ()
	{
		return mGridLayerSize;
	}
	
	
	
	/**
	 * Changes the size of the grid layer to produce.
	 */
	public void setGridLayerSize (int gridLayerSize)
	{
		mGridLayerSize = gridLayerSize;
	}
	
	
	
	/**
	 * Says whether we should create a legend layer or not.
	 */
	public boolean getCreateLegendLayer ()
	{
		return mCreateLegendLayer;
	}
	
	
	/**
	 * Sets the flag which says whether to create a legend layer or not.
	 */
	public void setCreateLegendLayer (boolean createLegendLayer)
	{
		mCreateLegendLayer = createLegendLayer;
	}
	
	
	
	public double[] getLegendValues ()
	{
		return mLegendValues;
	}
	
	
	
	public void setLegendValues (double[] legendValues)
	{
		mLegendValues = legendValues;
	}



	public boolean getAdvancedOptionsEnabled ()
	{
		return mAdvancedOptionsEnabled;
	}
	
	
	public void setAdvancedOptionsEnabled (boolean enabled)
	{
		mAdvancedOptionsEnabled = enabled;
	}
	
	
	
	/*public int getDiffusionGridSize ()
	{
		return mDiffusionGridSize;
	}
	
	
	public void setDiffusionGridSize (int size)
	{
		mDiffusionGridSize = size;
	}*/
	
	
	/*public int getDiffusionIterations ()
	{
		return mDiffusionIterations;
	}
	
	
	public void setDiffusionIterations (int iterations)
	{
		mDiffusionIterations = iterations;
	}*/
	
	

	/**
	 * Returns the category name for our cartogram layers.
	 */
	public String getCategoryName ()
	{
	
		if (mCategoryName == null)
		{
	
			// Create a new category in the layer manager in order to 
			// properly separate the cartogram layers. We call the new category
			// «Cartogram x», where x is a serial number.
		
			int catNumber = 1;
			String categoryName = "Cartogram " + catNumber;
			while (mLayerManager.getCategory(categoryName) != null)
			{
				catNumber++;
				categoryName = "Cartogram " + catNumber;
			}
		
			mCategoryName = categoryName;
			
		}
		
		return mCategoryName;

	}
	


	/**
	 * Creates a layer with the deformation grid.
	 */
	private void createGridLayer ()
	{
		
		Envelope env = mEnvelope;
		
		// Compute the deformation grid size in x and y direction.
		
		double resolution = 
			Math.max((env.getWidth() / (double)(mGridLayerSize + 1)), 
					 (env.getHeight() / (double)(mGridLayerSize + 1)));
					 
		int sizeX = 
			(int)Math.round(Math.floor(env.getWidth() / resolution)) - 1;
			
		int sizeY = 
			(int)Math.round(Math.floor(env.getHeight() / resolution)) - 1;
		
		
		
		// CREATE THE NEW LAYER
		
		// Create a new Feature Schema for the new layer.
		FeatureSchema fs = new FeatureSchema();
		fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
		fs.addAttribute("ID", AttributeType.INTEGER);
		
		// Create a new empty Feature Dataset.
		FeatureDataset fd = new FeatureDataset(fs);
		
		// Create a Geometry Factory for creating the points.
		GeometryFactory gf = new GeometryFactory();
		
		
		
		// CREATE ALL FEATURES AND LINES
		int j, k;
		int i = 0;
		
		// Horizontal lines
		for (k = 0; k < sizeY; k++)
		{
			// Create an empty Feature.
			BasicFeature feat = new BasicFeature(fs);
			
			// Create the line string and add it to the Feature.
			Coordinate[] coords = new Coordinate[sizeX];
			for (j = 0; j < sizeX; j++)
			{
				double x = env.getMinX() + (j * resolution);
				double y = env.getMinY() + (k * resolution);
				coords[j] = mGrid.projectPointAsCoordinate(x, y);
			}
			
			LineString ls = null;
			if (coords != null)
				ls = gf.createLineString(coords);
				
			if (ls != null)
			{
				feat.setGeometry(ls);
			
				// Add the other attributes.
				Integer idobj = new Integer((int)i);
				feat.setAttribute("ID", idobj);
				i++;
			
				// Add Feature to the Feature Dataset.
				fd.add(feat);
			}
			
		}


		// Vertical lines
		for (j = 0; j < sizeX; j++)
		{
			// Create an empty Feature.
			BasicFeature feat = new BasicFeature(fs);
			
			// Create the line string and add it to the Feature.
			Coordinate[] coords = new Coordinate[sizeY];
			for (k = 0; k < sizeY; k++)
			{
				double x = env.getMinX() + (j * resolution);
				double y = env.getMinY() + (k * resolution);
				coords[k] = mGrid.projectPointAsCoordinate(x, y);
			}
			
			LineString ls = null;
			if (coords != null)
				ls = gf.createLineString(coords);
				
			if (ls != null)
			{
				feat.setGeometry(ls);
			
				// Add the other attributes.
				Integer idobj = new Integer((int)i);
				feat.setAttribute("ID", idobj);
				i++;
			
				// Add Feature to the Feature Dataset.
				fd.add(feat);
			}
			
		}

		
		
		
		
		// Create the layer.
		mDeformationGrid = 
			new Layer("Deformation grid", Color.GRAY, fd, mLayerManager);

		
	}	// Cartogram.createGridLayer





	/**
	 * Creates an optional legend layer.
	 */
	private void createLegendLayer() {
		
		// The master layer.
		Layer masterLayer = mLayerManager.getLayer(mMasterLayer);
		
		double distanceBetweenSymbols = 
			(masterLayer.getFeatureCollectionWrapper().getEnvelope().
				getWidth() / 10);
		
		
		// Estimate legend values if there are none.

		double attrMax = CartogramLayer.maxValueForAttribute(
				masterLayer, mMasterAttribute);

		if (mLegendValues == null) {
			
			double attrMin = CartogramLayer.minValueForAttribute(masterLayer, mMasterAttribute);
			double attrMean = CartogramLayer.meanValueForAttribute(masterLayer, mMasterAttribute);
			int nvalues = 3;
			
			double maxLog = Math.floor(Math.log10(attrMax));
			double maxValue = Math.pow(10, maxLog);
			double secondValue = Math.pow(10, (maxLog-1));
						
			mLegendValues = new double[nvalues];
			mLegendValues[0] = secondValue;
			mLegendValues[1] = maxValue;
			mLegendValues[2] = attrMax;
		}
		
		
		// CREATE THE NEW LAYER
		
		// Create a new Feature Schema for the new layer.
		FeatureSchema fs = new FeatureSchema();
		fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
		fs.addAttribute("ID", AttributeType.INTEGER);
		fs.addAttribute("VALUE", AttributeType.DOUBLE);
		fs.addAttribute("AREA", AttributeType.DOUBLE);
		fs.addAttribute("COMMENT", AttributeType.STRING);
		
		// Create a new empty Feature Dataset.
		FeatureDataset fd = new FeatureDataset(fs);
		
		// Create a Geometry Factory for creating the points.
		GeometryFactory gf = new GeometryFactory();

		
		// CREATE THE FEATURES FOR THE LEGEND LAYER.
		int nvals = mLegendValues.length;
		double totalArea = CartogramLayer.totalArea(masterLayer);
		double valuesSum = CartogramLayer.sumForAttribute(masterLayer, mMasterAttribute);
		double x = mEnvelope.getMinX();
		double y = mEnvelope.getMinY();
		int id = 1;
		int valcnt;
		for (valcnt = 0; valcnt < nvals; valcnt++) {
			double valsize = totalArea / valuesSum * mLegendValues[valcnt];
			double rectsize = Math.sqrt(valsize);
			
			// Create the coordinate points.
			Coordinate[] coords = new Coordinate[5];
			coords[0] = new Coordinate(x, y);
			coords[1] = new Coordinate((x+rectsize), y);
			coords[2] = new Coordinate((x+rectsize), (y-rectsize));
			coords[3] = new Coordinate(x, (y-rectsize));
			coords[4] = new Coordinate(x, y);
			
			// Create geometry.
			LinearRing lr = gf.createLinearRing(coords);
			Polygon poly = gf.createPolygon(lr, null);
			
			// Create the Feature.
			BasicFeature feat = new BasicFeature(fs);
			feat.setAttribute("GEOMETRY", poly);
			feat.setAttribute("ID", id);
			feat.setAttribute("VALUE", mLegendValues[valcnt]);
			feat.setAttribute("AREA", valsize);
			
			if (valcnt == 0)
				feat.setAttribute("COMMENT", "Mean value");
			else if (valcnt == 1)
				feat.setAttribute("COMMENT", 
					"Rounded value of maximum (" + attrMax + ")");
					
			// Add the Feature to the Dataset.
			fd.add(feat);
			
			
			// Change the coordinates.
			x += rectsize + distanceBetweenSymbols;
			
			id++;
			
		}
		
		
		// Create the layer.
		mLayerManager.setFiringEvents(false);
		mLegendLayer = 
			new Layer("Legend", Color.GREEN, fd, mLayerManager);
		LabelStyle legendLabels = mLegendLayer.getLabelStyle();
		legendLabels.setAttribute("VALUE");
		legendLabels.setEnabled(true);
		legendLabels.setFont(new Font(null, Font.PLAIN, 10));
		mLayerManager.setFiringEvents(true);
			
	}	// Cartogram.createLegendLayer
	
	
	
	
	
	/**
	 * Creates the computation report and stores it in the object attribute.
	 */
	public void produceComputationReport (Layer projectedMasterLayer)
	{
		
		StringBuffer rep = new StringBuffer();
		
		rep.append("CARTOGRAM COMPUTATION REPORT\n\n");
		
		rep.append("CARTOGRAM PARAMETERS:\n");
		rep.append("Cartogram layer: " + mMasterLayer + "\n");
		rep.append("Cartogram attribute: " + mMasterAttribute + "\n");
		
		String attrType = "Population value";
		if (mMasterAttributeIsDensityValue) attrType = "Density value";
		rep.append("Attribute type: " + attrType + "\n");
		
		String transformationQuality = "";
		if (mAdvancedOptionsEnabled)
			transformationQuality = "disabled";
		else
			transformationQuality = "" + mAmountOfDeformation + " of 100";
		rep.append("Transformation quality: " + transformationQuality + "\n");
		
		rep.append("Cartogram grid size: "+ mGridSizeX +" x "+ mGridSizeY +"\n");
		rep.append("Bias value: " + this.bias + "\n");
		rep.append("\n");
		//rep.append("Diffusion grid size: "+ mDiffusionGridSize +"\n");
		//rep.append("Diffusion iterations: "+ mDiffusionIterations +"\n\n");
		
		
		
		rep.append("CARTOGRAM LAYER & ATTRIBUTE STATISTICS:\n");
		Layer masterLayer = mLayerManager.getLayer(mMasterLayer);
		int nfeat = masterLayer.getFeatureCollectionWrapper().getFeatures().size();
		rep.append("Number of features: "+ nfeat +"\n");
		
		double mean = CartogramLayer.meanValueForAttribute(
			masterLayer, mMasterAttribute);
		rep.append("Attribute mean value: " + mean + "\n");
		
		double min = CartogramLayer.minValueForAttribute(
			masterLayer, mMasterAttribute);
		rep.append("Attribute minimum value: " + min + "\n");
		
		double max = CartogramLayer.maxValueForAttribute(
			masterLayer, mMasterAttribute);
		rep.append("Attribute maximum value: " + max + "\n\n");
	
	
	
		rep.append("SIMULTANEOUSLY TRANSFORMED LAYERS:\n");
		Vector simLayers = mCartogramWizard.getSimultaneousLayers();
		if (simLayers == null || simLayers.size() == 0)
		{
			rep.append("None\n\n");
		}
		else
		{
			Iterator simLayerIter = simLayers.iterator();
			while (simLayerIter.hasNext())
			{
				Layer lyr = (Layer)simLayerIter.next();
				rep.append(lyr.getName() +"\n");
			}
			rep.append("\n");
		}
		
	
		rep.append("CONSTRAINED DEFORMATION LAYERS:\n");
		Vector constLayers = mCartogramWizard.getConstrainedDeformationLayers();
		if (constLayers == null || constLayers.size() == 0)
		{
			rep.append("None\n\n");
		}
		else
		{
			Iterator constLayerIter = constLayers.iterator();
			while (constLayerIter.hasNext())
			{
				Layer lyr = (Layer)constLayerIter.next();
				rep.append(lyr.getName() +"\n");
			}
			rep.append("\n");
		}
		


		// Compute the cartogram error.
		double meanError = CartogramLayer.computeCartogramSizeError(
			projectedMasterLayer, mMasterAttribute, masterLayer, "SizeError");
		
		rep.append("CARTOGRAM ERROR\n");
		rep.append("The cartogram error is a measure for the quality of the result.\n");
		rep.append("Mean cartogram error: "+ meanError +"\n");
		
		double stdDev = CartogramLayer.standardDeviationForAttribute(
			projectedMasterLayer, "SizeError");
		rep.append("Standard deviation: "+ stdDev +"\n");
		
		double pctl25 = CartogramLayer.percentileForAttribute(
			projectedMasterLayer, "SizeError", 25);
		rep.append("25th percentile: "+ pctl25 +"\n");
		
		double pctl50 = CartogramLayer.percentileForAttribute(
			projectedMasterLayer, "SizeError", 50);
		rep.append("50th percentile: "+ pctl50 +"\n");
		
		double pctl75 = CartogramLayer.percentileForAttribute(
			projectedMasterLayer, "SizeError", 75);
		rep.append("75th percentile: "+ pctl75 +"\n");
	
	
	
		
		// Compute the number of features between the 25th and 75th
		// percentile and the percentage.
		
		FeatureCollectionWrapper fcw = 
			projectedMasterLayer.getFeatureCollectionWrapper();
		
		Iterator featIter = fcw.iterator();
		int nFeaturesInStdDev = 0;
		int nFeatures = fcw.size();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			
			double value = 
				CartogramFeature.getAttributeAsDouble(feat, "SizeError");
				
			if (value >= (meanError - stdDev) && value <= (meanError + stdDev))
				nFeaturesInStdDev++;
		}
	
		
		double percFeaturesInStdDev = 
			(double)nFeaturesInStdDev / (double)nFeatures * (double)100;
		
		int pfint = (int)Math.round(percFeaturesInStdDev);
			
		rep.append("Features with mean error +/- 1 standard deviation: "+ 
			nFeaturesInStdDev +" of "+ nFeatures +" ("+ 
			pfint +"%)\n\n");
	
	
	
		
		long estimatedTime = System.nanoTime() - mComputationStartTime;
		estimatedTime /= 1000000000;
		rep.append("Computation time: "+ estimatedTime +" seconds\n");

		
		mComputationReport = rep.toString();
		
	}
	
	
	
	
	public String getComputationReport ()
	{
		return mComputationReport;
	}
	
	
	
	
	
	
	/**
	 * Tries to estimate the maximum segment length allowed for a
	 * geometry. The length is estimated using the envelope of the
	 * master layer and the number of features present in the master layer.
	 * The area of the envelope is considered as a square. The length of
	 * the square's edge is divided by the square root of the number of
	 * features. This gives us an estimate of the number of features along
	 * the square's edge. It is further considered that there should be
	 * about 10 vertices for one feature along the square's edge.
	 */
	public double estimateMaximumSegmentLength ()
	{
		
		// Check the input variables. Otherwise, return a default value.
		double defaultValue = 500.0;
		if (mEnvelope == null) return defaultValue;
		if (mMasterLayer == null) return defaultValue;
		
		
		
		// Compute the edge length of the square having the same area as
		// the cartogram envelope.
		
		double envArea = mEnvelope.getWidth() * mEnvelope.getHeight();
		if (envArea <= 0.0) return defaultValue;
		
		double edgeLength = Math.sqrt(envArea);
		
		
		
		// Get the number of features and the features per edge.
		
		Layer layer = mLayerManager.getLayer(mMasterLayer);
		if (layer == null) return defaultValue;
		
		int nfeat = layer.getFeatureCollectionWrapper().getFeatures().size();
		double featuresPerEdge = Math.sqrt((double)nfeat);
		
		
		
		// Compute the length per feature.
		// 1/10 of the length per feature is our estimate for the
		// maximum segment length.
		
		double lengthPerFeature = edgeLength / featuresPerEdge;
		
		return (lengthPerFeature / 10);
		
	}	// estimateMaximumSegmentLength
	
	
	
	
	
	public void setMissingValue (String value)
	{
		mMissingValue = value;
	}
	
	

}	// Cartogram










