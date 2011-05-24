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



import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JWindow;

import ch.epfl.scapetoad.AppContext;
import ch.epfl.scapetoad.MainWindow;

import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.LayerManager;





/**
 * This class contains the main method of the ScapeToad application.
 * 
 * For launching ScapeToad with the GUI, just type the following command:
 * java -Xmx1024M -jar ScapeToad.jar
 *
 * The command line version can be launched by adding some more arguments
 * after ScapeToad.jar
 * Syntax:
 *	ScapeToad.jar polygrid layer=/path/to/layer.shp attribute=shapeAttribute densityGrid=/path/to/grass/grid.asc
 *	ScapeToad.jar diffusion densityGrid=/path/to/grass/grid.asc deformationGrid=/path/to/output/grid.asc bias=0.5
 *	ScapeToad.jar project deformationGrid=/path/to/grid.asc layers='layer1_original','layer1_deformed','layer2_original','layer2_deformed'
 *
 * @author christian@361degres.ch
 * @version v1.2.0, 2010-03-03
 */
public class ScapeToad
{


	/**
	 * The main method for the ScapeToad application.
	 */
    public static void main (String args[])
	{
	
		// If we have no command line arguments, launch the GUI.
		if (args.length == 0)
		{
			ScapeToad.launchGUI(args);
			return;
		}
		
		
		
		/*
		// Process the command line arguments.
		
		// Get the first command line argument. This will be the name of the tool to launch.
		String tool = args[0];

		
		// Get all the other command line arguments and store them in a hash table.
		Hashtable argsDic = new Hashtable();
		for (int i = 1; i < args.length; i++)
		{
			String[] kv = args[i].split("=", 2);
			argsDic.put(kv[0], kv[1]);
		}
		
		
		
		
		if (tool.equals("polygrid"))
		{
			System.out.println("Creating grid from polygon layer");
			String layer = (String)argsDic.get("layer");
			String attr = (String)argsDic.get("attribute");
			String densityGrid = (String)argsDic.get("densityGrid");
			
			if (densityGrid == null || layer == null || attr == null) {
				System.out.println("ERROR. Polygrid algorithm needs layer, attribute and densityGrid arguments. Aborting...");
				return;
			}
			
			ScapeToad.polygrid(layer, attribute, densityGrid);
		}
		else if (tool.equals("diffusion"))
		{
			System.out.println("Running diffusion algorithm");
			String densityGrid = (String)argsDic.get("densityGrid");
			String outGrid = (String)argsDic.get("deformationGrid");
			
			if (densityGrid == null || outGrid == null) {
				System.out.println("ERROR. Diffusion algorithm needs densityGrid and deformationGrid arguments. Aborting...");
				return;
			}
			
			String strBias = (String)argsDic.get("bias");
			double bias = 0.0;
			if (strBias != null)
			{
				Double dblBias = new Double(strBias);
				bias = dblBias.doubleValue();
			}
			
			ScapeToad.diffusion(densityGrid, outGrid, bias);
		}
		else if (tool.equals("project"))
		{
			System.out.println("Projecting layers");
			String deformationGrid = (String)argsDic.get("deformationGrid");
			String layers = (String)argsDic.get("layers");
			String[] layerList = layers.split("','");
			
			ScapeToad.project(deformationGrid, layerList);
		}
		*/
		
		
    }
	
	
	
	
	
	
	
	public static void launchGUI (String[] args)
	{
		
		// Set the Look & Feel properties.
		// This is specific for MacOS X environments.
		// On other environments, this property does not have any effect.
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		
		// Create a new JUMP workbench.
		ImageIcon icon = new ImageIcon("resources/scapetoad-icon-small.gif");
		JWindow window = new JWindow();
		DummyTaskMonitor tm = new DummyTaskMonitor();
		
		// An exception might be thrown when creating a new workbench.
		try {
			AppContext.workBench = new JUMPWorkbench(AppContext.shortProgramName, args, icon, window, tm);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			return;
		}
		
		// Create a new layer manager.
		AppContext.layerManager = new LayerManager();
		AppContext.layerManager.addCategory("Original layers");
		
		
		// Create the main window and display it.
		AppContext.mainWindow = new MainWindow();
		AppContext.mainWindow.setVisible(true);
		
	}
	
	
	
	
	
	
	
}





