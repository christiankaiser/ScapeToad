/*

	Copyright 2007 91NORD

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
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.ShapefileReader;
import com.vividsolutions.jump.io.ShapefileWriter;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.renderer.style.LabelStyle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;



/**
 * The input/output manager reads and writes all the files
 * for our application.
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2007-11-28
 */
public class IOManager
{


	/**
	 * Displays an open dialog and reads the shape file in.
	 */
	public static Layer openShapefile ()
	{
		try
		{
		
			// Open dialog.
			FileDialog fd = new FileDialog(
				(Frame)AppContext.mainWindow, 
				"Add Layer...", 
				FileDialog.LOAD);
				
			fd.setFilenameFilter(new ShapeFilenameFilter());
			fd.setModal(true);
			fd.setBounds(20, 30, 150, 200);
			fd.setVisible(true);
		
			// Get the selected File name.
			if (fd.getFile() == null)
			{
				// User has cancelled.
				return null;
			}
			String shpName = fd.getFile();
			if (shpName.endsWith(".shp") == false &&
				shpName.endsWith(".SHP") == false)
			{
				OpenLayerErrorDialog errDial = new OpenLayerErrorDialog();
				errDial.setModal(true);
				errDial.setVisible(true);
			}
			String shpDirectory = fd.getDirectory();
			String shpPath = fd.getDirectory() + fd.getFile();

			Layer lyr = IOManager.readShapefile(shpPath);
			return lyr;
				
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	
	}	// IOManager.openShapefile
	
	
	

	/**
	 * Reads the provided shape file and returns a Layer.
	 */
	public static Layer readShapefile (String shapefile)
	{
		try
		{
		
			// Compute the layer name.
			String ln = IOManager.fileNameFromPath(shapefile);
		
			// Define a layer fill color.
			Color lc = Color.GREEN;
		
			// Read the Shape file.
			DriverProperties dp = new DriverProperties(shapefile);
			ShapefileReader shpReader = new ShapefileReader();
			FeatureCollection fc = shpReader.read(dp);
		
			// If there is no category "Original layers", we add one.
			if (AppContext.layerManager.getCategory("Original layers") == null)
				AppContext.layerManager.addCategory("Original layers");
			
			// Add the layer to the "Original layers" category.
			Layer lyr = new Layer(ln, lc, fc, AppContext.layerManager);
			lyr = AppContext.layerManager.addLayer("Original layers", ln, fc);
			
			// If the number of layers is 1, zoom to full extent in the 
			// layer view panel.
			if (AppContext.layerManager.getLayers().size() == 1)
				AppContext.layerViewPanel.getViewport().zoomToFullExtent();
			
			AppContext.layerViewPanel.getViewport().update();
			
			return lyr;
		
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	
		return null;
		
	}	// IOManager.readShapefile
	
	
	
	
	
	
	/**
	 * Returns the file name from a given complete file path.
	 */
	private static String fileNameFromPath (String path)
	{
		
		// Find last / or \ and eliminate text before.
		int lastSlash = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
		String fileName = path.substring(lastSlash+1);
		
		// Find last . and eliminate text after.
		int lastPoint = fileName.lastIndexOf(".");
		fileName = fileName.substring(0, lastPoint);
		
		return fileName;
	
	}




	/**
	 * Shows a save dialog and writes the Shapefile out.
	 */
	public static void saveShapefile (FeatureCollection fc)
	{
		// Create the File Save dialog.
		FileDialog fd = new FileDialog(
			(Frame)AppContext.mainWindow, 
			"Save Layer As...", 
			FileDialog.SAVE);

		fd.setFilenameFilter(new ShapeFilenameFilter());
		fd.setModal(true);
		fd.setBounds(20, 30, 150, 200);
		fd.setVisible(true);
		
		// Get the selected File name.
		if (fd.getFile() == null)
		{
			AppContext.mainWindow.setStatusMessage(
				"[Save layer...] User has cancelled the action.");
				
			return;
		}
		
		String shpPath = fd.getDirectory() + fd.getFile();
		if (shpPath.endsWith(".shp") == false)
			shpPath = shpPath + ".shp";
		
		IOManager.writeShapefile(fc, shpPath);
	
	}






	/**
	 * Writes the provided Feature Collection into a Shape file.
	 * @param fc the FeatureCollection to write out.
	 * @param path the path of the .shp file.
	 */
	public static void writeShapefile (FeatureCollection fc, String path)
	{
	
		try
		{
			ShapefileWriter shpWriter = new ShapefileWriter();
			DriverProperties dp = new DriverProperties();
			dp.set("DefaultValue", path);
			dp.set("ShapeType", "xy"); 
			shpWriter.write(fc, dp);
		
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}	// IOManager.writeShapefile




	/**
	 * Shows a save file dialog for exporting the layers into a SVG file.
	 * @param lyrs an array with the layers to include in the SVG file.
	 */
	public static void saveSvg (Layer[] lyrs)
	{
	
		// Create the File Save dialog.
		FileDialog fd = new FileDialog(
			(Frame)AppContext.mainWindow, 
			"Save Layer As...", 
			FileDialog.SAVE);

		fd.setFilenameFilter(new SVGFilenameFilter());
		fd.setModal(true);
		fd.setBounds(20, 30, 150, 200);
		fd.setVisible(true);
		
		// Get the selected File name.
		if (fd.getFile() == null)
		{
			AppContext.mainWindow.setStatusMessage(
				"[Export as SVG...] User has cancelled the action.");
				
			return;
		}
		
		String svgPath = fd.getDirectory() + fd.getFile();
		if (svgPath.endsWith(".svg") == false)
			svgPath = svgPath + ".svg";
		
		IOManager.writeSvg(lyrs, svgPath);
		
	}	// IOManager.saveSvg






	/**
	 * Writes the provided layers into a SVG file.
	 * @param lyrs an array with the layers to include in the SVG file.
	 * @param path the location of the SVG file to create.
	 */
	public static void writeSvg (Layer[] lyrs, String path)
	{
		
		int lyrcnt = 0;
		
		int nlyrs = lyrs.length;
		if (nlyrs < 1)
		{
			System.out.println("No layer available");
			return;
		}
		
		// Find the extent of all layers.
		Layer lyr = lyrs[0];
		FeatureCollectionWrapper fcw = lyr.getFeatureCollectionWrapper();
		Envelope extent = new Envelope(fcw.getEnvelope());
		
		for (lyrcnt = 1; lyrcnt < nlyrs; lyrcnt++)
		{
			lyr = lyrs[lyrcnt];
			Envelope lyrEnv = lyr.getFeatureCollectionWrapper().getEnvelope();
			extent.expandToInclude(lyrEnv);
		}
	
	
		// Find the dimensions of the output SVG file. We suppose a A4 document
		// with 595 x 842 pixels. The orientation depends on the extent.
		int svgWidth = 595;
		int svgHeight = 842;
		if (extent.getWidth() > extent.getHeight())
		{
			svgWidth = 842;
			svgHeight = 595;
		}
		
		// Define the margins.
		int svgMarginLeft = 30;
		int svgMarginRight = 30;
		int svgMarginTop = 30;
		int svgMarginBottom = 30;
		
		

		// Compute the scaling factor for the coordinate conversion.

		double scaleFactorX = (svgWidth - svgMarginLeft - svgMarginRight) / extent.getWidth();
		double scaleFactorY = (svgHeight - svgMarginTop - svgMarginBottom) / extent.getHeight();
		double sclfact = Math.min(scaleFactorX, scaleFactorY);



	
		try
		{
			// Open the file to write out.
			PrintWriter out = new PrintWriter(new FileWriter(path));
		
			// Write the XML header
			out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			out.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"");
			out.println(" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
			
			// Write the SVG header
			out.println("<svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\""); 
			out.println("     x=\"0px\" y=\"0px\" width=\"" + svgWidth + 
				"px\" height=\"" + svgHeight + "px\" viewBox=\"0 0 "+ svgWidth +
				" "+ svgHeight +"\">");
			out.println("");	
			
			
			// Write layer by layer.
			for (lyrcnt = (nlyrs - 1); lyrcnt >= 0; lyrcnt--)
			{
				Layer layer = lyrs[lyrcnt];
				
				// Create a group for every layer.
				out.println("	<g id=\"" + layer.getName() + "\">");
				
				
				// Get the colors and transparency for this layer.
				Color fillColor = layer.getBasicStyle().getFillColor();
				Color strokeColor = layer.getBasicStyle().getLineColor();
				int alpha = layer.getBasicStyle().getAlpha();
				double opacity = (double)alpha / 255.0;
				
			
				// Output every Feature.
				FeatureCollection fc = layer.getFeatureCollectionWrapper();
				Iterator featIter = fc.iterator();
				while (featIter.hasNext())
				{
					Feature feat = (Feature)featIter.next();
					
					// If it is a point, we output a small rectangle.
					// Otherwise we output a path.
					Geometry geom = feat.getGeometry();
					String geomType = geom.getGeometryType();
					
					if (geomType == "Point" || geomType == "MultiPoint")
					{
						Coordinate[] coords = geom.getCoordinates();
						
						String fillColorString = "rgb("+ fillColor.getRed() +
							","+ fillColor.getGreen() +","+ fillColor.getBlue() +
							")";
							
						for (int i = 0; i < coords.length; i++)
						{
							double x = 
								(coords[i].x - extent.getMinX()) * sclfact + 
								svgMarginLeft;
				
							double y = 
								((coords[i].y - extent.getMinY()) / 
								extent.getHeight());
								y = 1.0 - y;
								y = (y * extent.getHeight() * sclfact) + 
									svgMarginTop;
						
							out.println("		<rect fill=\""+ 
								fillColorString +"\" x=\""+ 
								x +"\" y=\""+ y + 
								"\" width=\"2\" height=\"2\" />");
						}
						
					}
					else
					{
						// Fill only a polygon.
						String fillColorString = "none";
						if (geomType == "Polygon" || geomType == "MultiPolygon")
						{
							fillColorString = "rgb("+ fillColor.getRed() +
							","+ fillColor.getGreen() +","+ fillColor.getBlue() +
							")";
						}
							
						String geomPath = IOManager.geometryToSvgPath(
							geom, extent, sclfact,
							svgMarginLeft, 
							(svgWidth - svgMarginRight),
							svgMarginTop,
							(svgHeight - svgMarginBottom));
							
						if (geomPath != "")
						{
							out.print("		<path fill=\""+ fillColorString +
								"\" stroke=\"rgb("+ strokeColor.getRed() +","+
								strokeColor.getGreen() +","+ strokeColor.getBlue() +
								")\" stroke-width=\"0.5pt\" d=\"");
							
							out.print(geomPath);
						
							// Close the path if it is a polygon.
							if (geomType == "Polygon" || geomType == "MultiPolygon")
								out.print("z");
						
							out.println("\" />");
						}
					
					}
					
				}
				
				
				// Close the layer group.
				out.println("	</g>");
				out.println("");
			}
			
			
			
			
			// Write the labels if there are any.
			// (There are typically for the legend layer.)
			for (lyrcnt = (nlyrs - 1); lyrcnt >= 0; lyrcnt--)
			{
				Layer layer = lyrs[lyrcnt];
				
				LabelStyle style = layer.getLabelStyle();
				if (style.isEnabled())
				{
					
					out.println("<g>");
					
					String attrName = style.getAttribute();
				
					// Output every Feature label.
					FeatureCollection fc = layer.getFeatureCollectionWrapper();
					Iterator featIter = fc.iterator();
					while (featIter.hasNext())
					{
						Feature feat = (Feature)featIter.next();
						Geometry geom = feat.getGeometry();
						Point center = geom.getCentroid();
					
						double x = 
							(center.getX() - extent.getMinX()) * sclfact + 
							svgMarginLeft;
				
						double y = 
							((center.getY() - extent.getMinY()) / 
							extent.getHeight());
							y = 1.0 - y;
							y = (y * extent.getHeight() * sclfact) + 
								svgMarginTop;
						
						
						out.print("<text x=\""+ x +"\" y=\""+ y +
							"\" text-anchor=\"middle\">");
						out.print(feat.getAttribute(attrName));
						out.print("</text>");
					}
					
					out.println("</g>");
					
				}
				
			}
			
			
			
			// Write the SVG footer
			out.print("</svg>");
			
			// Close the output stream.
			out.close();
		
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	
	}	// IOManager.writeSvg
	



	/**
	 * Converts a Geometry into a SVG path sequence.
	 * @param geom the Geometry to convert.
	 * @param env the Envelope we use for the coordinate conversion.
	 * @param minX, maxX, minY, maxY the maximum coordinates for the
	 *        SVG coordinates and corresponding to the Envelope env.
	 * @return a string for use in a SVG path element.
	 */
	public static String geometryToSvgPath (Geometry geom, Envelope env,
		double scaleFactor, double minX, double maxX, double minY, double maxY)
	{
	
		String path = "";
		int ngeoms = geom.getNumGeometries();
		int geomcnt = 0;
		
		if (ngeoms > 1)
		{
			for (geomcnt = 0; geomcnt < ngeoms; geomcnt++)
			{
				Geometry g = geom.getGeometryN(geomcnt);
				String pathPart = IOManager.geometryToSvgPath (g, env, scaleFactor, minX, maxX, minY, maxY);
				path = path + pathPart;
			}
		}
		else
		{
			
			// Get the type of the Geometry. If it is a Polygon, we should handle the exterior and interior rings
			// in an appropriate way.
			String geomType = geom.getGeometryType();
			
			if (geomType == "Polygon") {
				// Exterior ring first.
				Polygon p = (Polygon)geom;
				Coordinate[] coords = p.getExteriorRing().getCoordinates();
				String subPath = IOManager.coordinatesToSvgPath(coords, env, scaleFactor, minX, maxX, minY, maxY);
				path = path + subPath;
				
				// Interior rings.
				int nrings = p.getNumInteriorRing();
				for (int i=0; i < nrings; i++) {
					coords = p.getInteriorRingN(i).getCoordinates();
					subPath = IOManager.coordinatesToSvgPath(coords, env, scaleFactor, minX, maxX, minY, maxX);
					path = path + subPath;
				}
			} else {
				Coordinate[] coords = geom.getCoordinates();
				String subPath = IOManager.coordinatesToSvgPath(coords, env, scaleFactor, minX, maxX, minY, maxY);
				path = path + subPath;
			}
			
		}
		
		return path;
	
	}	// IOManager.geometryToSvgPath


	

	/**
	 * Converts a Coordinate sequence into a SVG path sequence.
	 * @param coords the coordinates to convert (a Coordinate[]).
	 * @param env the Envelope we use for the coordinate conversion.
	 * @param minX, maxX, minY, maxY the maximum coordinates for the
	 *        SVG coordinates and corresponding to the Envelope env.
	 * @return a string for use in a SVG path element.
	 */
	public static String coordinatesToSvgPath (Coordinate[] coords, Envelope env,
											double scaleFactor, double minX, double maxX, double minY, double maxY)
	{
		String path = "M ";
		int ncoords = coords.length;
		if (ncoords == 0) return "";
		
		int coordcnt = 0;
		for (coordcnt = 0; coordcnt < ncoords; coordcnt++)
		{
			double x = (coords[coordcnt].x - env.getMinX()) * scaleFactor + minX;
			double y = ((coords[coordcnt].y - env.getMinY()) / env.getHeight());
			y = 1.0 - y;
			y = (y * env.getHeight() * scaleFactor) + minY;
			path = path + x +" "+ y +" ";
			
			if (coordcnt < (ncoords - 1)) {
				path = path + "L ";
			}
		}
		
		return path;
	}
	
	
	
	



}	// IOManager






/**
 * This class allows the filtering of Shapefiles in the Java file dialog.
 */
class ShapeFilenameFilter implements FilenameFilter
{
	
	/**
	 * This is the method used for filtering.
	 */
	public boolean accept (File dir, String name)
	{
	
		if (name.endsWith(".shp") || name.endsWith(".SHP"))
			return true;
		else
			return false;
		
	}
	
	
}	// ShapeFilenameFilter






/**
 * This class allows the filtering of SVG files in the Java file dialog.
 */
class SVGFilenameFilter implements FilenameFilter
{
	
	/**
	 * This is the method used for filtering.
	 */
	public boolean accept (File dir, String name)
	{
	
		if (name.endsWith(".svg") || name.endsWith(".SVG"))
			return true;
		else
			return false;
		
	}
	
	
}	// SVGFilenameFilter










/**
 * Dialog window for Shape file error. Normally, in the open layer dialog,
 * there is a filter for Shape files. But M$ does not think it useful...
 * @author christian@swisscarto.ch
 * @version v1.0.0, 2008-05-20
 */
class OpenLayerErrorDialog extends JDialog
{


	JButton mOkButton;
	JLabel mNoShapeFileLabel;
	JLabel mSelectShapeFileLabel;
	


	/**
	 * Constructor for the export SVG file dialog.
	 */
	OpenLayerErrorDialog ()
	{
		
		// Set the window parameters.
		
		this.setTitle("Open layer error");
		this.setSize(300, 130);
		this.setLocation(40, 50);
		this.setResizable(false);
		this.setLayout(null);
		this.setModal(true);
		
		
		JLabel mNoShapeFileLabel = 
			new JLabel("Not a Shape file.");
		mNoShapeFileLabel.setSize(260, 14);
		mNoShapeFileLabel.setFont(new Font(null, Font.PLAIN, 11));
		mNoShapeFileLabel.setLocation(20, 20);
		this.add(mNoShapeFileLabel);
		
		
		JLabel mSelectShapeFileLabel = 
			new JLabel("Please select a file with the extension .shp.");
		mSelectShapeFileLabel.setSize(260, 14);
		mSelectShapeFileLabel.setFont(new Font(null, Font.PLAIN, 11));
		mSelectShapeFileLabel.setLocation(20, 40);
		this.add(mSelectShapeFileLabel);
		
		
		// Ok button
		
		mOkButton = new JButton("OK");
		mOkButton.setLocation(180, 70);
		mOkButton.setSize(100, 26);
		mOkButton.addActionListener(new OpenLayerErrorDialogAction(this));
		mOkButton.setMnemonic(KeyEvent.VK_ENTER);
		this.add(mOkButton);
	
	}	// OpenLayerErrorDialog.<init>


}	// OpenLayerErrorDialog







/**
 * The actions for the open layer error dialog.
 */
class OpenLayerErrorDialogAction extends AbstractAction
{

	OpenLayerErrorDialog mDialog;


	OpenLayerErrorDialogAction (OpenLayerErrorDialog dialog)
	{
		mDialog = dialog;
		
	}	// OpenLayerErrorDialogAction.<init>
	
	
	
	/**
	 * Method which performs the action.
	 */
	public void actionPerformed (ActionEvent e)
	{
		mDialog.setVisible(false);
		mDialog.dispose();
	
	}	// OpenLayerErrorDialogAction.actionPerformed
	


}	// OpenLayerErrorDialogAction

