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


import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.vividsolutions.jump.workbench.model.Layer;




/**
 * The main menu of the ScapeToad application.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-28
 */
public class MainMenu extends JMenuBar
{


	JMenuItem mMenuFile_RemoveLayer;
	JMenuItem mMenuFile_SaveLayer;
	JMenuItem mMenuFile_ExportAsSvg;


	/**
	 * The default constructor for the main menu.
	 * Creates the main menu.
	 */
	public MainMenu ()
	{
	
		// Create the FILE menu.
		JMenu fileMenu = new JMenu("File");
		
		// File > Add layer...
		JMenuItem menuFile_AddLayer = new JMenuItem("Add layer...");
		menuFile_AddLayer.addActionListener(new ActionLayerAdd());
		menuFile_AddLayer.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_L, 
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.add(menuFile_AddLayer);
		
		
		// File > Remove layer
		mMenuFile_RemoveLayer = new JMenuItem("Remove layer");
		mMenuFile_RemoveLayer.addActionListener(new ActionLayerRemove());
		mMenuFile_RemoveLayer.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_BACK_SPACE,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.add(mMenuFile_RemoveLayer);

		
		
		// Seperator
		JMenuItem sepMenu = new JMenuItem("-");
		fileMenu.add(sepMenu);
		
		
		// File > Save layer...
		mMenuFile_SaveLayer = new JMenuItem("Export layer as Shape file...");
		mMenuFile_SaveLayer.addActionListener(new ActionLayerSave());
		mMenuFile_SaveLayer.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_S,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.add(mMenuFile_SaveLayer);
		
		
		// File > Export as SVG...
		mMenuFile_ExportAsSvg = new JMenuItem("Export to SVG...");
		mMenuFile_ExportAsSvg.addActionListener(new ActionExportAsSvg());
		mMenuFile_ExportAsSvg.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_E,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		fileMenu.add(mMenuFile_ExportAsSvg);
		
		
		// Add a quit menu if we are not on a Mac.
		// (on a Mac, there is a default quit menu under the program name's
		// menu).
		if (System.getProperty("os.name").indexOf("Mac OS") == -1 ||
			AppContext.DEBUG)
		{
			JMenuItem sepMenu2 = new JMenuItem("-");
			fileMenu.add(sepMenu2);
			
			// File > Quit
			JMenuItem menuFile_Quit = new JMenuItem("Quit ScapeToad");
			menuFile_Quit.addActionListener(new ActionQuit());
			menuFile_Quit.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Q,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.add(menuFile_Quit);
		}
		
		
		this.add(fileMenu);
	
	
	
	
		// Add the help menu
		
		// Create the HELP menu.
		JMenu helpMenu = new JMenu("Help");
		
		JMenuItem menuHelp_Help = new JMenuItem("ScapeToad Help");
		menuHelp_Help.addActionListener(new ActionShowHelp());
		menuHelp_Help.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_HELP,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		helpMenu.add(menuHelp_Help);
		
		this.add(helpMenu);
	
	}	// MainMenu.<init>




	/**
	 * Enables/disables the menu items.
	 */
	public void enableMenus ()
	{

		if (AppContext.layerManager.getLayers().size() > 0)
		{
			mMenuFile_RemoveLayer.setEnabled(true);
			mMenuFile_SaveLayer.setEnabled(true);
			mMenuFile_ExportAsSvg.setEnabled(true);
		}
		else
		{
			mMenuFile_RemoveLayer.setEnabled(false);
			mMenuFile_SaveLayer.setEnabled(false);
			mMenuFile_ExportAsSvg.setEnabled(false);
		}

	}	// MainMenu.enableMenus
	
	


}	// MainMenu












