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


import java.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;




/**
 * Represents a basic feature from the Jump package, but with a couple
 * of additional methods useful for the cartogram project.
 * @author Christian Kaiser <Christian.Kaiser@91nord.com>
 * @version v1.0.0, 2007-11-30
 */
public class CartogramFeature
{
	
	public static double getAttributeAsDouble (Feature feat, String attrName)
	{
		AttributeType attrType = feat.getSchema().getAttributeType(attrName);
		
		if (attrType == AttributeType.DOUBLE)
		{
			Double dblValue = (Double)feat.getAttribute(attrName);
			return dblValue.doubleValue();
		}
		else if (attrType == AttributeType.INTEGER)
		{
			Integer intValue = (Integer)feat.getAttribute(attrName);
			return intValue.doubleValue();
		}
		
		return 0.0;
		
	}	// CartogramFeature.getAttributeAsDouble
	
	
	
	
	public static void setDoubleAttributeValue (Feature feat, 
		String attrName, double value)
	{
		AttributeType attrType = feat.getSchema().getAttributeType(attrName);
		
		if (attrType == AttributeType.DOUBLE)
		{
			feat.setAttribute(attrName, new Double(value));
		}
		else if (attrType == AttributeType.INTEGER)
		{
			int intValue = (int)Math.round(value);
			feat.setAttribute(attrName, new Integer(intValue));
		}
		
	}	// CartogramFeature.setDoubleAttributeValue
	
	
	
	
	
	/**
	 * Projects the provided Feature using the provided cartogram grid.
	 */
	public static Feature projectFeatureWithGrid 
		(Feature feat, CartogramGrid grid)
	{
	
		Geometry geom = feat.getGeometry();
		GeometryFactory gf = geom.getFactory();
		String geomType = geom.getGeometryType();
		
		// Create a copy of the Feature, but without the geometry.
		Feature projFeat = feat.clone(true);
		
		
		if (geomType == "Point")
		{
			Point pt = (Point)geom;
			double[] c = grid.projectPoint(pt.getX(), pt.getY());
			Point pt2 = gf.createPoint(new Coordinate(c[0], c[1]));
			projFeat.setGeometry(pt2);
			
		}
		else if (geomType == "LineString")
		{
			LineString l1 = (LineString)geom;
			Coordinate[] cs = grid.projectCoordinates(l1.getCoordinates());
			LineString l2 = gf.createLineString(cs);
			projFeat.setGeometry(l2);
			
		}
		else if (geomType == "LinearRing")
		{
			LinearRing l1 = (LinearRing)geom;
			Coordinate[] cs = grid.projectCoordinates(l1.getCoordinates());
			LinearRing l2 = gf.createLinearRing(cs);
			projFeat.setGeometry(l2);
			
		}
		else if (geomType == "MultiLineString")
		{
			MultiLineString mls1 = (MultiLineString)geom;
			int ngeoms = mls1.getNumGeometries();
			LineString[] lineStrings = new LineString[ngeoms];
			for (int geomcnt = 0; geomcnt < ngeoms; geomcnt++)
			{
				LineString l1 = (LineString)mls1.getGeometryN(geomcnt);
				Coordinate[] cs = grid.projectCoordinates(l1.getCoordinates());
				lineStrings[geomcnt] = gf.createLineString(cs);
			}
			MultiLineString mls2 = gf.createMultiLineString(lineStrings);
			projFeat.setGeometry(mls2);
			
		}
		else if (geomType == "MultiPoint")
		{
			MultiPoint mp1 = (MultiPoint)geom;
			int npts = mp1.getNumPoints();
			Point[] points = new Point[npts];
			for (int ptcnt = 0; ptcnt < npts; ptcnt++)
			{
				Point pt = (Point)mp1.getGeometryN(ptcnt);
				Coordinate c = 
					grid.projectPointAsCoordinate(pt.getX(), pt.getY());
				points[ptcnt] = gf.createPoint(c);
			}
			MultiPoint mp2 = gf.createMultiPoint(points);
			projFeat.setGeometry(mp2);
			
		}
		else if (geomType == "Polygon")
		{
			Polygon p1 = (Polygon)geom;
			Coordinate[] exteriorRingCoords = 
				grid.projectCoordinates(p1.getExteriorRing().getCoordinates());
			LinearRing exteriorRing = gf.createLinearRing(exteriorRingCoords);
			LinearRing[] interiorRings = null;
			int nrings = p1.getNumInteriorRing();
			if (nrings > 0)
			{
				interiorRings = new LinearRing[nrings];
				for (int ringcnt = 0; ringcnt < nrings; ringcnt++)
				{
					Coordinate[] interiorRingCoords = grid.projectCoordinates(
						p1.getInteriorRingN(ringcnt).getCoordinates());
					interiorRings[ringcnt] = 
						gf.createLinearRing(interiorRingCoords);
				}
			}
			Polygon p2 = gf.createPolygon(exteriorRing, interiorRings);
			if (p2 == null)
				System.out.println("Polygon creation failed.");
			projFeat.setGeometry(p2);
			
		}
		else if (geomType == "MultiPolygon")
		{
			MultiPolygon mp1 = (MultiPolygon)geom;
			int npolys = mp1.getNumGeometries();
			Polygon[] polys = new Polygon[npolys];
			for (int polycnt = 0; polycnt < npolys; polycnt++)
			{
				Polygon p1 = (Polygon)mp1.getGeometryN(polycnt);
				Coordinate[] exteriorRingCoords = grid.projectCoordinates(
					p1.getExteriorRing().getCoordinates());
				LinearRing exteriorRing = 
					gf.createLinearRing(exteriorRingCoords);
				LinearRing[] interiorRings = null;
				int nrings = p1.getNumInteriorRing();
				if (nrings > 0)
				{
					interiorRings = new LinearRing[nrings];
					for (int ringcnt = 0; ringcnt < nrings; ringcnt++)
					{
						Coordinate[] interiorRingCoords = 
							grid.projectCoordinates(
								p1.getInteriorRingN(ringcnt).getCoordinates());
								
						interiorRings[ringcnt] = 
							gf.createLinearRing(interiorRingCoords);
					}
				}
				polys[polycnt] = gf.createPolygon(exteriorRing, interiorRings);
			}
			
			MultiPolygon mp2 = gf.createMultiPolygon(polys);
			if (mp2 == null)
				System.out.println("Multi-polygon creation failed.");
			
			projFeat.setGeometry(mp2);
			
		}
		else
		{
			System.out.println("Unknown feature type");
		}
		
		return projFeat;
	
	}	// CartogramFeature.projectFeatureWithGrid
	
	
	
	
	
	/**
	 * Regularizes a geometry.
	 */
	public static Geometry regularizeGeometry (Geometry geom, double maxlen)
	{
		
		GeometryFactory gf = geom.getFactory();
		String geomType = geom.getGeometryType();
		
		if (geomType == "Point" || geomType == "MultiPoint")
		{
			return geom;
		}
		
		if (geomType == "MultiLineString")
		{
			MultiLineString mls = (MultiLineString)geom;
			int ngeoms = mls.getNumGeometries();
			LineString[] lss = new LineString[ngeoms];
			for (int lscnt = 0; lscnt < ngeoms; lscnt++)
			{
				LineString ls = (LineString)mls.getGeometryN(lscnt);
				lss[lscnt] = (LineString)
					CartogramFeature.regularizeGeometry(ls, maxlen);
			}
			mls = gf.createMultiLineString(lss);
			return mls;
		}
		
		if (geomType == "MultiPolygon")
		{
			MultiPolygon mpoly = (MultiPolygon)geom;
			int ngeoms = mpoly.getNumGeometries();
			Polygon[] polys = new Polygon[ngeoms];
			for (int polycnt = 0; polycnt < ngeoms; polycnt++)
			{
				Polygon poly = (Polygon)mpoly.getGeometryN(polycnt);
				polys[polycnt] = (Polygon)
					CartogramFeature.regularizeGeometry(poly, maxlen);
			}
			mpoly = gf.createMultiPolygon(polys);
			return mpoly;
		}


		
		if (geomType == "LineString")
		{
			Coordinate[] cs1 = geom.getCoordinates();
			Coordinate[] cs2 = 
				CartogramFeature.regularizeCoordinates(cs1, maxlen);
				
			LineString ls = gf.createLineString(cs2);
			return ls;
		}
		
		
		if (geomType == "LinearRing")
		{
			Coordinate[] cs1 = geom.getCoordinates();
			Coordinate[] cs2 =
				CartogramFeature.regularizeCoordinates(cs1, maxlen);
			
			LinearRing lr = gf.createLinearRing(cs2);
			return lr;
		}
		
		
		if (geomType == "Polygon")
		{
			Polygon p = (Polygon)geom;
			LineString shell = p.getExteriorRing();
			Coordinate[] shellCoords = CartogramFeature.regularizeCoordinates(
				shell.getCoordinates(), maxlen);
			LinearRing regShell = gf.createLinearRing(shellCoords);
			
			int nholes = p.getNumInteriorRing();
			LinearRing[] holes = null;
			if (nholes > 0)
				holes = new LinearRing[nholes];
				
			for (int holecnt = 0; holecnt < nholes; holecnt++)
			{
				LineString hole = p.getInteriorRingN(holecnt);
				Coordinate[] holeCoords = 
					CartogramFeature.regularizeCoordinates(
					hole.getCoordinates(), maxlen);
					
				holes[holecnt] = gf.createLinearRing(holeCoords);
			}
			
			Polygon p2 = gf.createPolygon(regShell, holes);
			
			return p2;
		}
		
		return null;
	
	}	// CartogramFeature.regularizeGeometry






	/**
	 * Regularizes a coordinate sequence.
	 */
	public static Coordinate[] regularizeCoordinates 
		(Coordinate[] coords, double maxlen)
	{
	
		int ncoords = coords.length;
		if (ncoords < 1)
			return coords;
		
		// The vector where we will temporarily store the regularized
		// coordinates.
		Vector newCoords = new Vector();
		newCoords.add(coords[0]);
		
		
		// Compute for each line segment the length. If the length is 
		// more than maxlen, we divide it in 2 until all the line segments
		// are shorter than maxlen.
		
		double sqMaxLen = maxlen * maxlen;
		
		for (int i = 0; i < (ncoords-1); i++)
		{
			
			double sqSegLen = 
				(coords[i].x - coords[i+1].x) * (coords[i].x - coords[i+1].x) +
				(coords[i].y - coords[i+1].y) * (coords[i].y - coords[i+1].y);
			
			if (sqSegLen > sqMaxLen)
			{
				double seglen = Math.sqrt(sqSegLen);
				
				// How much times we have to divide the line segment into 2?
				double dblndiv = Math.log(seglen/maxlen) / Math.log(2);
				dblndiv = Math.ceil(dblndiv);
				int ndiv = (int)Math.round(dblndiv);
				int nseg = (int)Math.round(Math.pow(2.0, dblndiv));
				
				// Compute the vector AB (from coord i to coord i+1).
				double abx = coords[i+1].x - coords[i].x;
				double aby = coords[i+1].y - coords[i].y;
				
				// Compute the new coordinates.
				for (int j = 1; j < nseg; j++)
				{
					double t = (double)j / (double)nseg;
					
					// Now we can compute the coordinate for the new point.
					double cx = coords[i].x + t * abx;
					double cy = coords[i].y + t * aby;
					Coordinate c = new Coordinate(cx, cy);
					newCoords.add(c);
				}
				
			}
			
			newCoords.add(coords[i+1]);
			
		}
	
	
		// Convert the vector holding all coordinates into an array.
		ncoords = newCoords.size();
		Coordinate[] newCoordsArray = new Coordinate[ncoords];
		for (int coordcnt = 0; coordcnt < ncoords; coordcnt++)
		{
			newCoordsArray[coordcnt] = (Coordinate)newCoords.get(coordcnt);
		}
	
		return newCoordsArray;
		
	}	// CartogramFeature.regularizeCoordinates

	
	
	
	
	
}	// CartogramFeature



