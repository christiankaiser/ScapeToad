/*

	Copyright 2007-2008 91NORD

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

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;


public class CartogramLayer
{



	/**
	 * Adds a new attribute to an existing layer.
	 * @param lyr the layer to which we should add the new attribute.
	 * @param name the name of the new attribute.
	 * @param type the type of the new attribute.
	 */
	public static void addAttribute (Layer lyr, String name, AttributeType type)
	{
			
		// Get the FeatureSchema.
		FeatureCollectionWrapper fcw = lyr.getFeatureCollectionWrapper();
		FeatureSchema fs = 
			CartogramLayer.copyFeatureSchema(fcw.getFeatureSchema());

		// If there is already an attribute with the given name, don't add it.
		if (fs.hasAttribute(name))
			return;

		fs.addAttribute(name, type);
		FeatureDataset fd = new FeatureDataset(fs);
		
		// After updating the FeatureSchema, we need to create a bigger
		// array for each Feature where the attribute values can be stored.
		int nattrs = fs.getAttributeCount();
		Iterator featiter = fcw.iterator();
		while (featiter.hasNext())
		{
			Feature feat = (Feature)featiter.next();
			feat.setSchema(fs);
			Object[] newAttributes = new Object[nattrs];
			Object[] oldAttributes = feat.getAttributes();
			for (int attrcnt = 0; attrcnt < oldAttributes.length; attrcnt++)
				newAttributes[attrcnt] = oldAttributes[attrcnt];
			feat.setAttributes(newAttributes);
			
			fd.add(feat);
		}
		
		lyr.setFeatureCollection(fd);
	
	}	// CartogramLayer.addAttribute




	/**
	 * Adds a new attribute containing the density value for a given
	 * attribute.
	 * @param populationAttr the name of the (existing) attribute for which
	 *        we shall compute the density.
	 * @param densityAttr the name of the new density attribute.
	 */
	public static void addDensityAttribute 
		(Layer layer, String populationAttr, String densityAttr)
	{
	
		CartogramLayer.addAttribute(layer, densityAttr, AttributeType.DOUBLE);
		
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			Geometry geom = feat.getGeometry();
			double geomArea = geom.getArea();
			double attrValue = 
				CartogramFeature.getAttributeAsDouble(feat, populationAttr);
			
			double density = 0.0;
			if (geomArea > 0 && attrValue > 0)
				density = attrValue / geomArea;

			feat.setAttribute(densityAttr, new Double(density));
			
		}
	
	}	// CartogramLayer.addDensityAttribute
	
	
	
	
	
	/**
	 * Computes the mean value for the given attribute weighted by
	 * the feature area.
	 */
	public static double meanDensityWithAttribute (Layer layer, String attrName)
	{
	
		double totalArea = CartogramLayer.totalArea(layer);
		double meanDensity = 0.0;
	
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			Geometry geom = feat.getGeometry();
			double geomArea = geom.getArea();
			double attrValue = 
				CartogramFeature.getAttributeAsDouble(feat, attrName);
				
			meanDensity += (geomArea / totalArea) * attrValue;
		}
		
		return meanDensity;
		
	}
	
	
	
	/**
	 * Returns the mean value for the given attribute.
	 */
	public static double meanValueForAttribute (Layer layer, String attrName)
	{
	
		int nobj = 0;
		double meanValue = 0.0;
	
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			double val = CartogramFeature.getAttributeAsDouble(feat, attrName);
			meanValue += val;
			nobj++;
		}
		
		meanValue = meanValue / (double)nobj;
		
		return meanValue;
		
	}
	
	
	
	/**
	 * Returns the minimum value for the given attribute.
	 */
	public static double minValueForAttribute (Layer layer, String attrName)
	{
	
		double minValue = 0.0;
	
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		if (featIter.hasNext() == false)
		{
			return 0.0;
		}
		else
		{
			Feature feat = (Feature)featIter.next();
			minValue = CartogramFeature.getAttributeAsDouble(feat, attrName);
		}
		
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			double attrValue = 
				CartogramFeature.getAttributeAsDouble(feat, attrName);
				
			if (attrValue < minValue)
				minValue = attrValue;
		}
		
		return minValue;
		
	}
	
	
	
	
	/**
	 * Returns the maximum value for the given attribute.
	 */
	public static double maxValueForAttribute (Layer layer, String attrName)
	{
	
		double maxValue = 0.0;
	
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		if (featIter.hasNext() == false)
		{
			return 0.0;
		}
		else
		{
			Feature feat = (Feature)featIter.next();
			maxValue = CartogramFeature.getAttributeAsDouble(feat, attrName);
		}
		
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			double attrValue = 
				CartogramFeature.getAttributeAsDouble(feat, attrName);
				
			if (attrValue > maxValue)
				maxValue = attrValue;
		}
		
		return maxValue;
		
	}
	
	
	
	
	
	/**
	 * Computes the sum of the provided attribute.
	 */
	public static double sumForAttribute (Layer layer, String attrName)
	{
		double sum = 0.0;
	
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			sum += CartogramFeature.getAttributeAsDouble(feat, attrName);
		}
		
		return sum;
		
	}
	
	
	
	
	/**
	 * Computes the variance of the provided attribute.
	 */
	public static double varianceForAttribute (Layer layer, String attrName)
	{
		double mean = CartogramLayer.meanValueForAttribute(layer, attrName);
		double diffSum = 0.0;
		double nFeat = 0;
		
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			double val = CartogramFeature.getAttributeAsDouble(feat, attrName);
			diffSum += (val - mean) * (val - mean);
			nFeat += 1.0;
		}
		
		double var = diffSum / nFeat;
		return var;
		
	}	// CartogramLayer.varianceForAttribute
	
	
	
	
	/**
	 * Computes the standard deviation of the provided attribute.
	 */
	public static double standardDeviationForAttribute (
		Layer layer, String attrName)
	{
		double variance = CartogramLayer.varianceForAttribute(layer, attrName);
		double stdDev = Math.sqrt(variance);
		return stdDev;
		
	}	// CartogramLayer.standardDeviationForAttribute
	
	
	
	
	
	/**
	 * Returns the n-th percentile of the provided attribute.
	 * @param n the percentile, must be between 0 and 100.
	 */
	public static double percentileForAttribute (
		Layer layer, String attrName, int n)
	{
		
		if (n < 0) n = 0;
		if (n > 100) n = 100;
		double dblN = (double)n;
		
		// Create a new TreeSet and store the attribute values inside.
		TreeSet set = new TreeSet();
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			double val = CartogramFeature.getAttributeAsDouble(feat, attrName);
			set.add(new Double(val));
		}
		
		// Get the number of features.
		int nfeat = set.size();
		
		// Create a Vector from the TreeSet.
		Vector attrVector = new Vector(set);
		
		// Get the indexes of the bounding features.
		double dblIndex = (double)n / (double)100 * (double)nfeat;
		int lowerIndex = Math.round((float)Math.floor(dblIndex));
		int upperIndex = Math.round((float)Math.ceil(dblIndex));
		
		if (lowerIndex == upperIndex)
		{
			Double pval = (Double)attrVector.get(lowerIndex);
			return pval.doubleValue();
		}
		
		double lowerPctl = (double)lowerIndex / (double)nfeat * (double)100;
		Double lowerValueDbl = (Double)attrVector.get(lowerIndex);
		double lowerValue = lowerValueDbl.doubleValue();
		double upperPctl = (double)upperIndex / (double)nfeat * (double)100;
		Double upperValueDbl = (Double)attrVector.get(upperIndex);
		double upperValue = upperValueDbl.doubleValue();
		
		double scalingFactor = 1.0;
		if ((upperPctl - lowerPctl) > 0)
			scalingFactor = (dblN - lowerPctl) / (upperPctl - lowerPctl);
		
		double percentileValue = 
			(scalingFactor * (upperValue - lowerValue)) + lowerValue;
		
		
		return percentileValue;
		
	}	// CartogramLayer.percentileForAttribute
	
	
	
	
	
	/**
	 * Replaces a double attribute value with another.
	 */
	public static void replaceAttributeValue (Layer layer, String attrName, 
		double oldValue, double newValue)
	{
	
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			double val = CartogramFeature.getAttributeAsDouble(feat, attrName);
			if (val == oldValue)
			{
				CartogramFeature.setDoubleAttributeValue(
					feat, attrName, newValue);
			}
		}
	
	}	// CartogramLayer.replaceAttributeValue
	
	
	
	
	
	
	/**
	 * Computes the total area of all features in this layer.
	 */
	public static double totalArea (Layer layer)
	{
		
		double totalArea = 0.0;
		
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			Geometry geom = feat.getGeometry();
			totalArea += geom.getArea();
		}
		
		return totalArea;
		
	}	// CartogramLayer.totalArea





	/**
	 * Computes the contour of this layer.
	 * @param layer the layer for which we should compute the layer.
	 * @return the contour as a Geometry.
	 */
	public static Geometry contour (Layer layer)
	{
		
		Geometry contour = null;
		Iterator featIter = layer.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			Geometry geom = feat.getGeometry();
			if (contour == null)
				contour = geom;
			else
				contour = contour.union(geom);
		}
		
		return contour;
	
	}	// CartogramLayer.contour





	/**
	 * Regularizes a layer. This means the length of all line
	 * segments does not exceed a given value. In the case of a too long
	 * line segment, the line is repeatedly divided in two until the length
	 * is less than the given value.
	 * @param lyr the layer to regularize.
	 * @param maxlen the maximum length of the line segments.
	 */
	public static void regularizeLayer (Layer lyr, double maxlen)
	{
		
		Iterator featIter = lyr.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			Geometry geom = feat.getGeometry();
			Geometry regGeom = 
				CartogramFeature.regularizeGeometry(geom, maxlen);
				
			feat.setGeometry(regGeom);
		}
	
	}	// CartogramLayer.regularizeLayer





	/**
	 * Snaps all points of the layer to a grid with given cell size.
	 * @param lyr the layer to snap.
	 * @param snappingDistance the size of the grid cells.
	 * @param snapRegion a envelope with the snapping region. This enables
	 * the snapping to the same grid over several different layers.
	 */
	public static void snapLayer (
		Layer lyr, double snappingDistance, Envelope snapRegion)
	{
	
	}	// CartogramLayer.snapLayer





	/**
	 * Projects a layer using a cartogram grid. Returns the projected layer.
	 */
	public static Layer projectLayerWithGrid (Layer lyr, CartogramGrid grid)
	{
	
		// Create a new FeatureDataset for storing our projected features.
		FeatureSchema fs = lyr.getFeatureCollectionWrapper().getFeatureSchema();
		
		// Make a copy of the FeatureSchema.
		FeatureSchema fs2 = (FeatureSchema)fs.clone();
		FeatureDataset fd = new FeatureDataset(fs2);
	
		// Project each Feature one by one.
		Iterator featIter = lyr.getFeatureCollectionWrapper().iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			Feature projFeat = 
				CartogramFeature.projectFeatureWithGrid(feat, grid);
			
			if (projFeat != null)
			{
				projFeat.setSchema(fs2);
				fd.add(projFeat);
			}
		}
		
		// Create a layer with the FeatureDataset.
		Layer projectedLayer = new Layer(lyr.getName(),
			lyr.getBasicStyle().getFillColor(),
			fd, lyr.getLayerManager());
		
		return projectedLayer;
	
	}	// CartogramLayer.projectLayerWithGrid
	
	
	
	
	
	/**
	 * Computes the cartogram size error and stores it in the layer's attribute
	 * with the provided name. The size error is computed as follows:
	 *   err = 100 * ((areaOptimal * Sum(areaReal)) / 
	 *                (areaReal * Sum(areaOptimal)))
	 * where
	 *   err :				the size error
	 *   areaOptimal :		the optimal or theoretical area of a polygon
	 *   areaReal :			the current area of a polygon
	 * @return the mean size error.
	 */
	public static double computeCartogramSizeError (
		Layer cartogramLayer, String cartogramAttribute, 
		Layer originalLayer, String errorAttribute)
	{
		
		double sumOfRealAreas = CartogramLayer.totalArea(cartogramLayer);
		double sumOfOptimalAreas = CartogramLayer.totalArea(originalLayer);
		double sumOfValues = CartogramLayer.sumForAttribute(
			cartogramLayer, cartogramAttribute);
		
		if (sumOfOptimalAreas == 0 || sumOfValues == 0 || sumOfRealAreas == 0)
			return 0.0;
		
		CartogramLayer.addAttribute(
			cartogramLayer, errorAttribute, AttributeType.DOUBLE);
		
		FeatureCollectionWrapper fcw = 
			cartogramLayer.getFeatureCollectionWrapper();
		
		FeatureSchema fs = fcw.getFeatureSchema();
		
		Iterator featIter = fcw.iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			Geometry geom = feat.getGeometry();
			double geomArea = geom.getArea();
			double attrValue = 
				CartogramFeature.getAttributeAsDouble(feat, cartogramAttribute);
			
			// Compute the optimal cartogram area.
			double optimalArea = attrValue / sumOfValues * sumOfOptimalAreas;
			
			
			double featError = 0.0;
			if (geomArea > 0.0)
				featError = 100 * ((optimalArea * sumOfRealAreas) / 
							       (geomArea * sumOfOptimalAreas));

			if (feat.getSchema().hasAttribute(errorAttribute))
				feat.setAttribute(errorAttribute, new Double(featError));
			
		}


		double meanError = CartogramLayer.meanValueForAttribute(
			cartogramLayer, errorAttribute);
			
		return meanError;
		
	}	// CartogramLayer.computeCartogramSizeError





	/**
	 * Checks the attribute values for invalid values and replaces them
	 * with a zero value. This method works only with double value attributes.
	 */
	public static void cleanAttributeValues(Layer layer, String attrName)
	{
		
		FeatureCollectionWrapper fcw = layer.getFeatureCollectionWrapper();
		FeatureSchema fs = fcw.getFeatureSchema();
		if (fs.hasAttribute(attrName) == false) return;
		
		AttributeType attrType = fs.getAttributeType(attrName);
		if (attrType != AttributeType.DOUBLE) return;
		
		Iterator featIter = fcw.iterator();
		while (featIter.hasNext())
		{
			Feature feat = (Feature)featIter.next();
			Double attrValue = (Double)feat.getAttribute(attrName);
			if (attrValue.isNaN() || attrValue == null)
			{
				feat.setAttribute(attrName, new Double(0.0));
			}
		}
		
	}	// CartogramLayer.cleanAttributeValues






	/**
	 * Creates a new FeatureSchema using the provided FeatureSchema.
	 * @return a new FeatureSchema
	 */
	public static FeatureSchema copyFeatureSchema (FeatureSchema fs)
	{
		FeatureSchema copy = new FeatureSchema();
		int nattrs = fs.getAttributeCount();
		
		for (int i=0; i < nattrs; i++)
		{
			String attrName = fs.getAttributeName(i);
			AttributeType attrType = fs.getAttributeType(i);
			copy.addAttribute(attrName, attrType);
		}
		
		return copy;
	
	}	// CartogramLayer.copyFeatureSchema




}	// CartogramLayer






