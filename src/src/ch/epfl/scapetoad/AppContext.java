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

import com.vividsolutions.jump.workbench.JUMPWorkbench;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.TreeLayerNamePanel;



/**
 * This class contains some application wide attributes.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-28
 */
public class AppContext
{

	/**
	 * Setting the debug flag enables the output of console messages
	 * for debugging purposes.
	 */
	public static boolean DEBUG = false;
	
	
	/**
	 * The short program name.
	 */
	public static String shortProgramName = 
		"ScapeToad";
	
	/**
	 * The program name including the version number.
	 */
	public static String longProgramName = 
		"ScapeToad version 1.0.0";
	
	/**
	 * The copyright notice.
	 */
	public static String copyrightNotice = 
		"Copyright 2008 91NORD. All rights reserved.";
	
	
	
	/**
	 * The application's main window.
	 */
	public static MainWindow mainWindow;


	
	/**
	 * The layer manager from the JUMP project.
	 */
	public static LayerManager layerManager = null;
	
	
	/**
	 * The JUMP workbench.
	 */
	public static JUMPWorkbench workBench = null;



	/**
	 * The cartogram wizard.
	 */
	public static CartogramWizard cartogramWizard = null;

	
	/**
	 * The LayerViewPanel (pane displaying the maps) from the JUMP project.
	 */
	public static LayerViewPanel layerViewPanel = null;
	
	/**
	 * This panel is needed by the LayerViewPanel for displaying the layers
	 * as a tree with the category names.
	 */
	public static TreeLayerNamePanel layerListPanel = null;
	
	/**
	 * The map panels displays the maps.
	 */
	public static MapPanel mapPanel = null;
	
	
	
	public static SizeErrorLegend sizeErrorLegend = new SizeErrorLegend();


}







