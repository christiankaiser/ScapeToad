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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.NoninvertibleTransformException;

import java.net.URL;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;


import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerTreeModel;

import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import com.vividsolutions.jump.workbench.ui.TreeLayerNamePanel;



import com.vividsolutions.jump.workbench.ui.renderer.RenderingManager;



/**
 * The main window of the ScapeToad application.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-28
 */
public class MainWindow extends JFrame 
	implements LayerViewPanelContext, ErrorHandler
{
	
	
	MainPanel mMainPanel = null;
	MainMenu mMainMenu = null;
	
	
	/**
	 * The default constructor for the main window.
	 */
	public MainWindow ()
	{
		
		// Set the window parameters.
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle(AppContext.shortProgramName);
		this.setSize(640, 480);
		this.setLocation(20, 30);
		
		// Add the window content.
		mMainPanel = new MainPanel(this);
		this.getContentPane().add(mMainPanel);
		
		
		// Create the menu bar.
		mMainMenu = new MainMenu();
		this.setJMenuBar(mMainMenu);
		
		
		this.update();
		
	}

	
	
		/**
	 * Displays a message indicating the status of current operations
	 * in the status bar.
	 * @param message the message to display in the status bar.
	 */
	public void setStatusMessage (String message)
	{
	}
	
	
	
	
	/**
	 * Notifies the user in an alert box about a minor issue.
	 * @param warning the warning message to display.
	 */
	public void warnUser (String warning)
	{
	}
	
	
	
	
	/**
	 * Handles an exception.
	 */
	public void handleThrowable (Throwable t)
	{
		if (AppContext.DEBUG) t.printStackTrace();
	}
	



	/**
	 * Updates the window content.
	 */
	public void update ()
	{
		try
		{
			AppContext.mapPanel.update();
			AppContext.layerViewPanel.getViewport().update();
			AppContext.layerViewPanel.repaint();
			mMainMenu.enableMenus();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}




	/**
	 * Displays a dialog for exporting a Shape file.
	 */
	public void exportShapeFile ()
	{
		ExportShapeFileDialog dialog = new ExportShapeFileDialog();
		dialog.setVisible(true);
	
	}	// MainWindow.exportShapeFile
	
	
	
	/**
	 * Displays a dialog for export the layers as a SVG file.
	 */
	public void exportSvgFile ()
	{
		ExportSvgFileDialog dialog = new ExportSvgFileDialog();
		dialog.setVisible(true);
		
	}	// MainWindow.exportSvgFile

	
}	// MainWindow






class MainPanel extends JPanel
{

	MainPanel (JFrame contentFrame)
	{
	
		// Set the layout parameters.
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setBorder(BorderFactory.createEmptyBorder(5,20,20,20));
	
	
		// Create the toolbar.
		MainToolbar toolbar = new MainToolbar(contentFrame);
		toolbar.setAlignmentX(LEFT_ALIGNMENT);
		this.add(toolbar);
		
		
		// Create the two scroll views with the content pane.
		
		AppContext.mapPanel = new MapPanel(contentFrame);
		
		JScrollPane rightScrollPane = 
			new JScrollPane(AppContext.mapPanel);
		
		JScrollPane leftScrollPane = 
			new JScrollPane(new LayerListPanel(contentFrame));

		
		
		// Set the minimum sizes for the scroll panes.
		Dimension minimumSize = new Dimension(150, 200);
		leftScrollPane.setMinimumSize(minimumSize);
		rightScrollPane.setMinimumSize(minimumSize);
		
		// Create a new split pane and add the two scroll panes.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
											leftScrollPane, rightScrollPane);
		
		
		// Set the divider location for our split pane.
		splitPane.setDividerLocation(150);
		
		splitPane.setAlignmentX(LEFT_ALIGNMENT);
		
		this.add(splitPane);
		
	}	// MainPanel.<init>




	
	

}	// MainPanel





/**
 * This class represents the layer list panel which is located inside
 * the main window (table of contents for the layers).
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-28
 */
class LayerListPanel extends JPanel
{



	/**
	 * The default constructor for the layer list panel.
	*/
	LayerListPanel (JFrame contentFrame)
	{
		
		LayerTreeModel layerTreeModel = 
			new LayerTreeModel(AppContext.layerViewPanel);
		
		RenderingManager renderingManager = 
			new RenderingManager(AppContext.layerViewPanel);
			
		AppContext.layerListPanel = 
			new TreeLayerNamePanel (AppContext.layerViewPanel, 
					layerTreeModel, renderingManager, new TreeMap());
		
		//layerList.setBounds(20, 20, 160, 450);
		//layerList.setLayout(new BoxLayer(contentFrame, BoxLayout.Y_AXIS));
		
		//CGLayerListListener layerListListener = new CGLayerListListener(this);
		//mLayerList.addListener(layerListListener);

		this.add(AppContext.layerListPanel);
		
	}
	

}








/**
 * This class represents the map panel which is located inside
 * the main window.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-28
 */
class MapPanel extends JPanel
{



	/**
	 * The default constructor for the map panel.
	 */
	MapPanel (JFrame contentFrame)
	{
		this.setSize(contentFrame.getSize());
		this.setLayout(null);
		this.setLocation(0,0);
		this.setBackground(Color.WHITE);
	
	
		// Create the new layer view panel taken from the JUMP project.
		AppContext.layerViewPanel = 
			new LayerViewPanel(AppContext.layerManager, AppContext.mainWindow);
		
		
		// If this flag is false, the viewport will be zoomed to the
		// extent of the layer when a new layer is added.
		AppContext.layerViewPanel.setViewportInitialized(false);
		AppContext.layerViewPanel.setLocation(0, 0);
		AppContext.layerViewPanel.setSize(this.getSize());
		
		// Add the layer view panel to the panel.
		this.add(AppContext.layerViewPanel);
		
		
		
		// Zoom to full extent.
		// A NoninvertibleTransformException might be thrown.
		try
		{
			AppContext.layerViewPanel.getViewport().zoomToFullExtent();
			AppContext.layerViewPanel.getViewport().update();
		}
		catch (NoninvertibleTransformException exception)
		{
			AppContext.mainWindow.warnUser(
				"Error while zooming to full extent.");
				
		}
		
		
	}	// MapPanel.<init>



	void update ()
	{
		AppContext.layerViewPanel.setSize(this.getSize());
	}



}	// MapPanel






/**
 * This class represents the main toolbar at the top of the main window.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-28
 */
class MainToolbar extends JPanel
{


	/**
	 * The default constructor for the map panel.
	 */
	MainToolbar (JFrame contentFrame)
	{
		
		ClassLoader cldr = this.getClass().getClassLoader();
		
		
		// Full extent button
		
		java.net.URL imageURL = cldr.getResource("full-extent-32.gif");
		ImageIcon fullExtentIcon = new ImageIcon(imageURL);

		JButton fullExtentButton = 
			new JButton("Full extent", fullExtentIcon);
		
		fullExtentButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		fullExtentButton.setHorizontalTextPosition(AbstractButton.CENTER);
		fullExtentButton.setSize(53, 53);
		fullExtentButton.setFocusable(false);
		fullExtentButton.setContentAreaFilled(false);
		fullExtentButton.setBorderPainted(false);
		
		fullExtentButton.addActionListener(new ActionZoomToFullExtent());
		this.add(fullExtentButton);
		
		
		// Add layer button
		
		java.net.URL addLayerURL = cldr.getResource("addLayer-32.png");
		ImageIcon addLayerIcon = new ImageIcon(addLayerURL);

		JButton addLayerButton = 
			new JButton("Add layer", addLayerIcon);
		
		addLayerButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		addLayerButton.setHorizontalTextPosition(AbstractButton.CENTER);
		addLayerButton.setSize(53, 53);
		addLayerButton.setFocusable(false);
		addLayerButton.setContentAreaFilled(false);
		addLayerButton.setBorderPainted(false);
		
		addLayerButton.addActionListener(new ActionLayerAdd());
		this.add(addLayerButton);

		
		
		// Create cartogram button
		
		java.net.URL createCartogramURL = cldr.getResource("buildAndGo-32.png");
		ImageIcon createCartogramIcon = new ImageIcon(createCartogramURL);

		JButton createCartogramButton = 
			new JButton("Create cartogram", createCartogramIcon);
		
		createCartogramButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		createCartogramButton.setHorizontalTextPosition(AbstractButton.CENTER);
		createCartogramButton.setSize(53, 53);
		createCartogramButton.setFocusable(false);
		createCartogramButton.setContentAreaFilled(false);
		createCartogramButton.setBorderPainted(false);
		
		createCartogramButton.addActionListener(new ActionCreateCartogram());
		this.add(createCartogramButton);



		// Export to SVG button
		
		java.net.URL svgURL = cldr.getResource("export-to-svg-32.png");
		ImageIcon svgIcon = new ImageIcon(svgURL);

		JButton svgButton = 
			new JButton("Export to SVG", svgIcon);
		
		svgButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		svgButton.setHorizontalTextPosition(AbstractButton.CENTER);
		svgButton.setSize(53, 53);
		svgButton.setFocusable(false);
		svgButton.setContentAreaFilled(false);
		svgButton.setBorderPainted(false);
		
		svgButton.addActionListener(new ActionExportAsSvg());
		this.add(svgButton);

		
		
		// Export to SHP button
		
		java.net.URL shpURL = cldr.getResource("export-to-shp-32.png");
		ImageIcon shpIcon = new ImageIcon(shpURL);

		JButton shpButton = 
			new JButton("Export to Shape", shpIcon);
		
		shpButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		shpButton.setHorizontalTextPosition(AbstractButton.CENTER);
		shpButton.setSize(53, 53);
		shpButton.setFocusable(false);
		shpButton.setContentAreaFilled(false);
		shpButton.setBorderPainted(false);
		
		shpButton.addActionListener(new ActionLayerSave());
		this.add(shpButton);
		
		
		
		
	}	// Toolbar.<init>



}	// MainToolbar







/**
 * This class is an action zooming to the full layer extent.
 * @author Christian.Kaiser@91nord.com
 */
class ActionZoomToFullExtent extends AbstractAction
{
	
	
	public void actionPerformed(ActionEvent e)
	{
		
		AppContext.mainWindow.update();
		
		try
		{
			AppContext.layerViewPanel.getViewport().zoomToFullExtent();
		}
		catch (NoninvertibleTransformException exc)
		{
			exc.printStackTrace();
		}
	
	}	// ActionLayerAdd.actionPerformed
	
	
	
}





class ActionCreateCartogram extends AbstractAction
{


	public void actionPerformed(ActionEvent e)
	{
		
		if (AppContext.cartogramWizard == null ||
			AppContext.cartogramWizard.isVisible() == false)
		{
			AppContext.cartogramWizard = null;
			AppContext.cartogramWizard = new CartogramWizard();
		}
	
		AppContext.cartogramWizard.setVisible(true);
		
	}


}









/**
 * Dialog window for specifying the layer to export into a Shape file.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2008-05-20
 */
class ExportShapeFileDialog extends JDialog
{


	JButton mOkButton;
	JButton mCancelButton;
	JLabel mLayerMenuLabel;
	JComboBox mLayerMenu;
	


	/**
	 * Constructor for the export Shape file dialog.
	 */
	ExportShapeFileDialog ()
	{
		
		// Set the window parameters.
		
		this.setTitle("Export layer as Shape file");
		this.setSize(300, 140);
		this.setLocation(40, 50);
		this.setResizable(false);
		this.setLayout(null);
		this.setModal(true);
		
		
		// Cancel button
		
		mCancelButton = new JButton("Cancel");
		mCancelButton.setLocation(70, 80);
		mCancelButton.setSize(100, 26);
		
		mCancelButton.addActionListener(new 
			ExportShapeFileDialogAction(
			"closeDialogWithoutSaving", this));
		
		mCancelButton.setMnemonic(KeyEvent.VK_ESCAPE);
		this.add(mCancelButton);
		
		
		// Ok button
		
		mOkButton = new JButton("OK");
		mOkButton.setLocation(180, 80);
		mOkButton.setSize(100, 26);
		
		mOkButton.addActionListener(new 
			ExportShapeFileDialogAction(
			"closeDialogWithSaving", this));
		
		mOkButton.setMnemonic(KeyEvent.VK_ENTER);
		this.add(mOkButton);
				
		
		// Add a popup menu with the list of available layers.
		
		mLayerMenuLabel = new JLabel("Select the layer to export:");
		mLayerMenuLabel.setFont(new Font(null, Font.PLAIN, 11));
		mLayerMenuLabel.setBounds(20, 20, 210, 14);
		this.add(mLayerMenuLabel);
		
		mLayerMenu = new JComboBox();
		mLayerMenu.setBounds(20, 40, 210, 26);
		mLayerMenu.setFont(new Font(null, Font.PLAIN, 11));
		mLayerMenu.setMaximumRowCount(20);
		
		int nlayers = AppContext.layerManager.size();
		for (int lyrcnt = 0; lyrcnt < nlayers; lyrcnt++)
		{
			Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
			mLayerMenu.addItem(lyr.getName());
		}

		// If there is no layer for the cartogram deformation,
		// add a menu item "<none>" and disable the "Next" button.
		if (mLayerMenu.getItemCount() == 0)
		{
			mLayerMenu.addItem("<none>");
			mOkButton.setEnabled(false);
		}
		else
		{
			mOkButton.setEnabled(true);
		}
		
		
		this.add(mLayerMenu);
	
	}	// ExportShapeFileDialog.<init>



	public void saveLayer ()
	{
		String layerName = (String)mLayerMenu.getSelectedItem();
		if (layerName == "<none>") return;
		Layer lyr = AppContext.layerManager.getLayer(layerName);
		IOManager.saveShapefile(lyr.getFeatureCollectionWrapper());
	}
	
	
	


}	// ExportShapeFileDialog







/**
 * The actions for the export shape file dialog.
 */
class ExportShapeFileDialogAction extends AbstractAction
{


	String mActionToPerform = "closeDialogWithoutSaving";
	ExportShapeFileDialog mDialog = null;
	
	/**
	 * The default creator for the action.
	 * @param actionToPerform defines the action to perform. Can be 
	 *        "showDialog", if we should create a new dialog and display it.
	 *        Is "closeDialogWithSaving" if we should close the dialog and
	 *        save the changes. is "closeDialogWithoutSaving" if we should
	 *        discard the changes and close the dialog.
	 * @param dialog a reference to the dialog or null if it does not yet exist
	 *        (for the showDialog action).
	 */
	ExportShapeFileDialogAction (
		String actionToPerform, ExportShapeFileDialog dialog)
	{
		mActionToPerform = actionToPerform;
		mDialog = dialog;
	
	}	// ExportShapeFileDialogAction.<init>
	
	
	
	/**
	 * Method which performs the previously specified action.
	 */
	public void actionPerformed (ActionEvent e)
	{
		
		if (mActionToPerform == "closeDialogWithoutSaving")
		{
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
		else if (mActionToPerform == "closeDialogWithSaving")
		{
			mDialog.saveLayer();
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
	
	}	// ExportShapeFileDialogAction.actionPerformed
	


}	// ExportShapeFileDialogAction








/**
 * Dialog window for specifying the layers to export into a SVG file.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2008-05-20
 */
class ExportSvgFileDialog extends JDialog
{


	JButton mOkButton;
	JButton mCancelButton;
	JPanel mLayerListPanel;
	JScrollPane mLayerListScrollPane;
	Vector mCheckBoxList;
	JLabel mNoLayerLabel;
	


	/**
	 * Constructor for the export SVG file dialog.
	 */
	ExportSvgFileDialog ()
	{
		
		// Set the window parameters.
		
		this.setTitle("Export layers to SVG file");
		this.setSize(300, 400);
		this.setLocation(40, 50);
		this.setResizable(false);
		this.setLayout(null);
		this.setModal(true);
		
		
		
		// LIST WITH SIMULTANEOUS LAYERS
		
		// Create a new pane containing the check boxes with
		// the layers.
		mLayerListPanel = new JPanel(new GridLayout(0,1));
		
		// Create the checkbox array.
		mCheckBoxList = new Vector();

		Font smallFont = new Font(null, Font.PLAIN, 11);
		
		
		int nlayers = AppContext.layerManager.size();
		int layersInList = 0;
		int lyrcnt = 0;
		for (lyrcnt = 0; lyrcnt < nlayers; lyrcnt++)
		{
			Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
			JCheckBox checkbox = new JCheckBox(lyr.getName());
			checkbox.setFont(smallFont);
					
			if (lyr.isVisible())
				checkbox.setSelected(true);
			else
				checkbox.setSelected(false);
					
			mCheckBoxList.add(checkbox);
			mLayerListPanel.add(checkbox);
			layersInList++;
		}
		
		// Compute the height of the new scroll pane.
		int scrollPaneHeight = layersInList * 26;
		if (layersInList == 0)
			scrollPaneHeight = 260;
		
		if (scrollPaneHeight > 260)
			scrollPaneHeight = 260;
			
		
		// Create a new scroll pane where we will display the
		// list of layers.
		mLayerListScrollPane = new JScrollPane(mLayerListPanel);
		mLayerListScrollPane.setSize(260, scrollPaneHeight);
		mLayerListScrollPane.setLocation(20, 50);
		
		mLayerListScrollPane.setBorder(
			BorderFactory.createEmptyBorder(0, 0, 0, 0));
			
		this.add(mLayerListScrollPane);
			

		
		
		// Label for the layers to deform.
		JLabel layerListLabel = 
		new JLabel("Select layers to export into a SVG file:");
		layerListLabel.setSize(260, 14);
		layerListLabel.setFont(smallFont);
		layerListLabel.setLocation(20, 20);
		this.add(layerListLabel);
		
		
		
		// Cancel button
		
		mCancelButton = new JButton("Cancel");
		mCancelButton.setLocation(70, 330);
		mCancelButton.setSize(100, 26);
		
		mCancelButton.addActionListener(new 
			ExportSvgFileDialogAction(
			"closeDialogWithoutSaving", this));
		
		mCancelButton.setMnemonic(KeyEvent.VK_ESCAPE);
		this.add(mCancelButton);
		
		
		// Ok button
		
		mOkButton = new JButton("OK");
		mOkButton.setLocation(180, 330);
		mOkButton.setSize(100, 26);
		
		mOkButton.addActionListener(new 
			ExportSvgFileDialogAction(
			"closeDialogWithSaving", this));
		
		mOkButton.setMnemonic(KeyEvent.VK_ENTER);
		this.add(mOkButton);



		// Label for no present layers.
		if (nlayers == 0)
		{
			mNoLayerLabel = new JLabel("No layers to be exported.");
			mNoLayerLabel.setSize(260, 14);
			mNoLayerLabel.setFont(smallFont);
			mNoLayerLabel.setLocation(20, 50);
			mOkButton.setEnabled(false);
			this.add(mNoLayerLabel);
		}
		
		

	
	}	// ExportSvgFileDialog.<init>



	public void exportLayers ()
	{
	
		mOkButton.setEnabled(false);
		mCancelButton.setEnabled(false);
	
		if (mCheckBoxList.size() > 0)
		{
			Vector layers = new Vector();
			Iterator iter = mCheckBoxList.iterator();
			while (iter.hasNext())
			{
				JCheckBox checkBox = (JCheckBox)iter.next();
				if (checkBox.isSelected())
				{
					String layerName = checkBox.getText();
					Layer lyr = AppContext.layerManager.getLayer(layerName);
					if (lyr == null)
						System.out.println("Layer "+ layerName +" not found.");
					else
						layers.add(lyr);
				}
			}
			
			int nlyrs = layers.size();
			Layer[] lyrs = new Layer[nlyrs];
			for (int lyrcnt = 0; lyrcnt < nlyrs; lyrcnt++)
			{
				lyrs[lyrcnt] = (Layer)layers.get(lyrcnt);
			}
			IOManager.saveSvg(lyrs);
		}
	}
	
	
	


}	// ExportSvgFileDialog







/**
 * The actions for the export SVG file dialog.
 */
class ExportSvgFileDialogAction extends AbstractAction
{


	String mActionToPerform = "closeDialogWithoutSaving";
	ExportSvgFileDialog mDialog = null;
	


	ExportSvgFileDialogAction (
		String actionToPerform, ExportSvgFileDialog dialog)
	{
		mActionToPerform = actionToPerform;
		mDialog = dialog;
	
	}	// ExportSvgFileDialogAction.<init>
	
	
	
	/**
	 * Method which performs the previously specified action.
	 */
	public void actionPerformed (ActionEvent e)
	{
		
		if (mActionToPerform == "closeDialogWithoutSaving")
		{
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
		else if (mActionToPerform == "closeDialogWithSaving")
		{
			mDialog.exportLayers();
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
	
	}	// ExportSvgFileDialogAction.actionPerformed
	


}	// ExportSvgFileDialogAction





