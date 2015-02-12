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




import javax.swing.ImageIcon;
import javax.swing.JWindow;

import ch.epfl.scapetoad.AppContext;
import ch.epfl.scapetoad.MainWindow;

import com.vividsolutions.jump.task.DummyTaskMonitor;
import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.LayerManager;


/**
 * This class contains the main method of the ScapeToad application.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-28
 */
public class ScapeToad
{


	/**
	 * The main method for the ScapeToad application.
	 */
    public static void main (String args[])
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
		try
		{
			AppContext.workBench = new JUMPWorkbench(
				AppContext.shortProgramName, 
				args, icon, window, tm);
		}
		catch (Exception e)
		{
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





