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

import java.awt.Color;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.Ostermiller.util.Browser;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollectionWrapper;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerManager;





/**
 * The cartogram wizard guiding the user through the process
 * of cartogram creation.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-11-30
 */
public class CartogramWizard extends JFrame
{


	int mCurrentStep = -1;
	CartogramWizardPanelZero mPanelZero = null;
	CartogramWizardPanelOne mPanelOne = null;
	CartogramWizardPanelTwo mPanelTwo = null;
	CartogramWizardPanelThree mPanelThree = null;
	CartogramWizardPanelFour mPanelFour = null;
	
	
	Cartogram mCartogram = null;
	
	
	
	/**
	 * The panel shown during cartogram computation.
	 */
	final CartogramWizardRunningPanel mRunningPanel = 
		new CartogramWizardRunningPanel(this);
	
	
	/**
	 * The panel shown after cartogram computation.
	 */
	final CartogramWizardFinishedPanel mFinishedPanel =
		new CartogramWizardFinishedPanel(this);
	
	
	/**
	 * The name of the selected cartogram layer (the master layer).
	 */
	String mCartogramLayerName = null;
	
	/**
	 * The name of the selected cartogram attribute.
	 */
	String mCartogramAttributeName = null;
	
	/**
	 * Some parameters for the cartogram computation.
	 */
	Vector mSimultaneousLayers = null;
	Vector mConstrainedDeformationLayers = null;
	int mAmountOfDeformation = 50;
	int mCartogramGridSizeX = 200;
	int mCartogramGridSizeY = 200;
	
	
	boolean mAdvancedOptionsEnabled = false;
	
	int mDiffusionGridSize = 128;
	int mDiffusionIterations = 3;
	
	
	/**
	 * Defines whether we should create a layer with the deformation grid.
	 */
	boolean mCreateGridLayer = true;
	
	
	/**
	 * Defines the size of the deformation grid which can be created as
	 * additional layer.
	 */
	int mDeformationGridSize = 100;
	
	
	
	/**
	 * The icon panel at the left side of each wizard window.
	 */
	ScapeToadIconPanel mScapeToadIconPanel = null;


	/**
	 * The step icon panel at the upper right side of the wizard.
	 */
	WizardStepIconPanel mWizardStepIconPanel = null;
	
	
	
	JButton mCancelButton = null;


	String mMissingValue = "";



	/**
	 * The default constructor for the wizard.
	 */
	public CartogramWizard ()
	{
	
		// Set the window parameters.
		this.setTitle(AppContext.shortProgramName + " _ Cartogram Wizard");
		this.setSize(640, 480);
		this.setLocation(30, 40);
		this.setResizable(false);
		this.setLayout(null);
		//this.setModal(true);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new CartogramWizardWindowListener());
		
		
		// Adding the cartogram wizard to the app context.
		AppContext.cartogramWizard = this;
		
		
		// Add icon panel at the left of the wizard window.
		// This panel contains the ScapeToad icon.
		if (mScapeToadIconPanel == null)
		{
			mScapeToadIconPanel = new ScapeToadIconPanel(this);
		}
		
		mScapeToadIconPanel.setLocation(30, 90);
		this.add(mScapeToadIconPanel);
		
		
		
		// Add title panel.
		CartogramWizardTitlePanel titlePanel =
			new CartogramWizardTitlePanel(this);
		
		titlePanel.setLocation(30, 20);
		this.add(titlePanel);
		
		
		
		
		// Add icon panel at the left of the wizard window.
		// This panel contains the ScapeToad icon.
		mWizardStepIconPanel = new WizardStepIconPanel(this);
		mWizardStepIconPanel.setLocation(380, 20);
		this.add(mWizardStepIconPanel);
		
		
		
		
		// Ajouter l'introduction au wizard.
		// Explication des étapes à suivre :
		//   1. Sélectionner la couche des polygones (master layer).
		//   2. Sélectionner l'information statistique.
		//   3. Sélection des couches à transformer simultanément.
		
		mPanelZero = new CartogramWizardPanelZero(this);
		this.getContentPane().add(mPanelZero);
		
		mCurrentStep = 0;
		
		
		
		// Add the running panel which is already created.
		mRunningPanel.setVisible(false);
		this.add(mRunningPanel);
		
		
		// Add the finished panel which is already created.
		mFinishedPanel.setVisible(false);
		this.add(mFinishedPanel);
		
				
		// Add the Cancel button.
		mCancelButton = new JButton("Cancel");
		mCancelButton.setLocation(30, 404);
		mCancelButton.setSize(100, 26);
		mCancelButton.addActionListener(new CartogramWizardCloseAction());
		this.getContentPane().add(mCancelButton);
		
		
	}	// CartogramWizard.<init>
	



	/**
	 * Returns the wizard panel 0.
	 */
	CartogramWizardPanelZero getPanelZero ()
	{
		return mPanelZero;
	}
	
	
	/**
	 * Returns the wizard panel 1.
	 */
	CartogramWizardPanelOne getPanelOne ()
	{
		return mPanelOne;
	}
	
	
	/**
	 * Returns the wizard panel 2.
	 */
	CartogramWizardPanelTwo getPanelTwo ()
	{
		return mPanelTwo;
	}


	/**
	 * Returns the wizard panel 3.
	 */
	CartogramWizardPanelThree getPanelThree ()
	{
		return mPanelThree;
	}



	/**
	 * Returns the wizard panel 4.
	 */
	CartogramWizardPanelFour getPanelFour ()
	{
		return mPanelFour;
	}
	


	/**
	 * Returns the wizard's running panel.
	 */
	CartogramWizardRunningPanel getRunningPanel ()
	{	
		return mRunningPanel;
	}




	/**
	 * Returns the wizard step icon panel.
	 */
	public WizardStepIconPanel getWizardStepIconPanel ()
	{
		return mWizardStepIconPanel;
	}
	
	
	

	/**
	 * Switches the wizard to the given step.
	 * The step number must be between 0 (introduction) and 3.
	 */
	public void goToStep (int step)
	{
		if (step < 0 || step > 4)
			return;
			
		if (mCurrentStep == step)
			return;
		
		
		// Hide the current step.
		switch (mCurrentStep)
		{
			case 0:
				mPanelZero.setVisible(false);
				break;
			
			case 1:
				mPanelOne.setVisible(false);
				break;
			
			case 2:
				mPanelTwo.setVisible(false);
				break;
			
			case 3:
				mPanelThree.setVisible(false);
				break;
			
			case 4:
				mPanelFour.setVisible(false);
				
		}
		
		
		
		// Show the new step.
		switch (step)
		{
			case 0:
				if (mPanelZero == null)
				{
					mPanelZero = new CartogramWizardPanelZero(this);
					this.getContentPane().add(mPanelZero);
				}
				mPanelZero.setVisible(true);
				mCurrentStep = 0;
				mWizardStepIconPanel.setStepIcon(1);
				break;
			
			case 1:
				if (mPanelOne == null)
				{
					mPanelOne = new CartogramWizardPanelOne(this);
					this.getContentPane().add(mPanelOne);
				}
				mPanelOne.setVisible(true);
				mCurrentStep = 1;
				mWizardStepIconPanel.setStepIcon(2);
				break;
			
			case 2:
				if (mPanelTwo == null)
				{
					mPanelTwo = new CartogramWizardPanelTwo(this);
					this.getContentPane().add(mPanelTwo);
				}
				mPanelTwo.setVisible(true);
				mCurrentStep = 2;
				mWizardStepIconPanel.setStepIcon(3);
				break;

			case 3:
				if (mPanelThree == null)
				{
					mPanelThree = new CartogramWizardPanelThree(this);
					this.getContentPane().add(mPanelThree);
				}
				mPanelThree.setVisible(true);
				mCurrentStep = 3;
				mWizardStepIconPanel.setStepIcon(4);
				break;
			
			case 4:
				if (mPanelFour == null)
				{
					mPanelFour = new CartogramWizardPanelFour(this);
					this.getContentPane().add(mPanelFour);
				}
				mPanelFour.setVisible(true);
				mCurrentStep = 4;
				mWizardStepIconPanel.setStepIcon(5);
			
			
		}
		
		
	}
	
	
	
	/**
	 * Shows the finished panel.
	 */
	public void goToFinishedPanel ()
	{
		mRunningPanel.setVisible(false);
		mFinishedPanel.setVisible(true);
		mWizardStepIconPanel.setStepIcon(7);
	}
	
	
	
	
	/**
	 * Returns the cartogram computation process.
	 */
	public Cartogram getCartogram ()
	{
		return mCartogram;
	
	}	// CartogramWizard.getCartogram
	
	
	
	/**
	 * Sets the cartogram computation process.
	 */
	public void setCartogram (Cartogram cg)
	{
		mCartogram = cg;
	
	}	// CartogramWizard.setCartogram
	
	
	
	/**
	 * Returns the name of the selected cartogram layer.
	 * This is the master layer for the cartogram transformation.
	 */
	public String getCartogramLayerName ()
	{
		return mCartogramLayerName;
	}
	
	
	/**
	 * Sets the cartogram layer name.
	 */
	public void setCartogramLayerName (String layerName)
	{
		mCartogramLayerName = layerName;
	}
	
	
	
	/**
	 * Returns the cartogram attribute name.
	 */
	public String getCartogramAttributeName ()
	{
		return mCartogramAttributeName;
	}
	
	
	
	/**
	 * Sets the cartogram attribute name.
	 */
	public void setCartogramAttributeName (String attrName)
	{
		mCartogramAttributeName = attrName;
	}
	
	
	
	/**
	 * Returns the parameter for the creation of a deformation grid layer.
	 * @return whether we should create or not a deformation grid layer.
	 */
	public boolean getCreateGridLayer ()
	{
		return mCreateGridLayer;
		
	}	// CartogramWizard.getCreateGridLayer
	
	
	
	
	/**
	 * Sets the parameter for the creation of a deformation grid layer.
	 * @param createGridLayer true if we should create a deformation grid
	 *        layer, false otherwise.
	 */
	public void setCreateGridLayer (boolean createGridLayer)
	{
		mCreateGridLayer = createGridLayer;
	
	}	// CartogramWizard.setCreateGridLayer
	
	
	
	
	/**
	 * Returns the size of the deformation grid which can be created as
	 * an additional layer.
	 * @return the size of the deformation grid.
	 */
	public int getDeformationGridSize ()
	{
		return mDeformationGridSize;
		
	}	// CartogramWizard.getDeformationGridSize
	
	
	
	/**
	 * Sets the size of the deformation grid which can be created as
	 * an additional layer. The effective grid size is adapted to the
	 * layer extent; this parameter sets the larger side of the layer
	 * extent rectangle.
	 * @param gridSize the size of the deformation grid.
	 */
	public void setDeformationGridSize (int gridSize)
	{
		mDeformationGridSize = gridSize;
		
	}	// CartogramWizard.setDeformationGridSize
	
	
	
	
	
	/**
	 * Updates the progress bar and the progress labels during
	 * cartogram computation.
	 * @param progress the progress status (integer 0-1000).
	 * @param label1 the progress main message.
	 * @param label2 the progress secondary message.
	 */
	public void updateRunningStatus 
		(final int progress, final String label1, final String label2)
	{
		
		Runnable doSetRunningStatus = new Runnable()
		{
			public void run()
			{
				mRunningPanel.updateProgressBar(progress);
				mRunningPanel.updateProgressLabel1(label1);
				mRunningPanel.updateProgressLabel2(label2);
			}
		};
		
		SwingUtilities.invokeLater(doSetRunningStatus);
		
	} // CartogramWizard.updateRunningStatus
	
	

	
	/**
	 * Returns the list of simultaneous layers.
	 */
	public Vector getSimultaneousLayers ()
	{
		return mSimultaneousLayers;
	}
	
	
	/**
	 * Sets the list of simultaneous layers.
	 */
	public void setSimultaneousLayers (Vector layers)
	{
		mSimultaneousLayers = layers;
	}
	
	
	/**
	 * Returns the simultaneous layer at a given index.
	 */
	public Layer getSimultaneousLayerAtIndex (int index)
	{
		return (Layer)mSimultaneousLayers.get(index);
	}
	
	
	/**
	 * Sets the simultaneous layer at a given index.
	 */
	public void setSimultaneousLayerAtIndex (Layer layer, int index)
	{
		if (mSimultaneousLayers == null)
			mSimultaneousLayers = new Vector();
		
		mSimultaneousLayers.set(index, layer);
	}
	
	
	/**
	 * Returns the list of constrained deformation layers.
	 */
	public Vector getConstrainedDeformationLayers ()
	{
		return mConstrainedDeformationLayers;
	}
	
	
	/**
	 * Sets the list of constrained deformation layers.
	 */
	public void setConstrainedDeformationLayers (Vector layers)
	{
		mConstrainedDeformationLayers = layers;
	}
	
	
	/**
	 * Returns the constrained deformation layer at the given index.
	 */
	public Layer getConstrainedDeformationLayerAtIndex (int index)
	{
		return (Layer)mConstrainedDeformationLayers.get(index);
	}
	
	
	/**
	 * Sets the constrained deformation layer at the given index.
	 */
	public void setConstrainedDeformationLayerAtIndex (Layer layer, int index)
	{
		if (mConstrainedDeformationLayers == null)
			mConstrainedDeformationLayers = new Vector();
		
		mConstrainedDeformationLayers.set(index, layer);
	}
	
	
	/**
	 * Returns the amount of deformation, an integer value between 0
	 * (low deformation) and 100 (high deformation).
	 */
	public int getAmountOfDeformation ()
	{
		return mAmountOfDeformation;
	}
	
	
	/**
	 * Changes the amount of deformation. This must be an integer value
	 * between 0 and 100.
	 */
	public void setAmountOfDeformation (int deformation)
	{
		mAmountOfDeformation = deformation;
	}
	
	
	/**
	 * Returns the cartogram grid size in x direction.
	 * The cartogram grid is the grid which is deformed by the cartogram
	 * computing process. It is not the same grid as the one used by
	 * Gastner's algorithm. The cartogram grid can have an arbitrary size;
	 * it is only limited by the available amount of memory and disk space.
	 */
	public int getCartogramGridSizeInX ()
	{
		return mCartogramGridSizeX;
	}
	
	
	/**
	 * Changes the cartogram grid size in x direction.
	 */
	public void setCartogramGridSizeInX (int gridSizeX)
	{
		mCartogramGridSizeX = gridSizeX;
	}
	
	
	/**
	 * Returns the cartogram grid size in y direction.
	 */
	public int getCartogramGridSizeInY ()
	{
		return mCartogramGridSizeY;
	}
	
	
	/**
	 * Changes the cartogram grid size in y direction.
	 */
	public void setCartogramGridSizeInY (int gridSizeY)
	{
		mCartogramGridSizeY = gridSizeY;
	}




	/**
	 * Returns true if the advanced options for the cartogram computation
	 * are enabled.
	 * @return true if the advanced parameters should be taken in account,
	 *         and false otherwise.
	 */
	public boolean getAdvancedOptionsEnabled ()
	{
		return mAdvancedOptionsEnabled;
		
	}	// CartogramWizard.getAdvancedOptionsEnabled
	
	
	
	
	
	public int getDiffusionGridSize ()
	{
		return mDiffusionGridSize;
	}
	
	
	public void setDiffusionGridSize (int diffusionGridSize)
	{
		mDiffusionGridSize = diffusionGridSize;
	}
	
	
	
	public int getDiffusionIterations ()
	{
		return mDiffusionIterations;
	}
	
	
	public void setDiffusionIteratations (int iterations)
	{
		mDiffusionIterations = iterations;
	}
	
	
	
	/**
	 * Defines whether the advances options should be taken into account.
	 * @param enabled true if the advanced options should be taken into 
	 *        account, and false if the advanced options should be ignored.
	 */
	public void setAdvancedOptionsEnabled (boolean enabled)
	{
		mAdvancedOptionsEnabled = enabled;
		
		if (enabled)
		{
			mPanelFour.enableAmountOfDeformationSlider(false);
		}
		else
		{
			mPanelFour.enableAmountOfDeformationSlider(true);
		}
		
	}	// CartogramWizard.setAdvancedOptionsEnabled





	/**
	 * Sets a cartogram computation error message for the user.
	 */
	public void setComputationError (
		String title, String message, String stackTrace)
	{
	
		mFinishedPanel.setErrorOccured(true);
		mFinishedPanel.setErrorMessage(title, message, stackTrace);
	
	}	// CartogramWizard.setComputationError
	


	
	
	/**
	 * Returns the cancel button of the cartogram wizard.
	 */
	public JButton getCancelButton ()
	{
		return mCancelButton;
	}




	public String getMissingValue ()
	{
		return mMissingValue;
	}
	
	
	public void setMissingValue (String value)
	{
		mMissingValue = value;
	}



}	// CartogramWizard








/**
 * This class represents a panel containing the ScapeToad icon with
 * a size of 97 x 152 pixels. It is used to be displayed in the
 * different wizard steps. The panel is always located at the left
 * side of the wizard window.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-12-01
 */
class ScapeToadIconPanel extends JPanel
{

	
	/**
	 * The default constructor for the ScapeToad icon panel.
	 */
	ScapeToadIconPanel (JFrame contentFrame)
	{
		
		// Setting some panel parameters.
		this.setSize(100, 155);
		this.setLayout(null);
		
		
		// Loading the ScapeToad icon from the resources.
		ClassLoader cldr = this.getClass().getClassLoader();
		URL iconURL = cldr.getResource("ScapeToad-25p.png");
		ImageIcon scapeToadIcon = new ImageIcon(iconURL);


		// Create a new label containing the icon.
		JLabel iconLabel = new JLabel(scapeToadIcon);
		
		// Setting the label parameters.
		iconLabel.setLayout(null);
		iconLabel.setSize(97, 152);
		iconLabel.setLocation(1, 1);
		
		// Add the icon label to this panel.
		this.add(iconLabel);
		
	}


}





/**
 * This class represents the overall cartogram wizard title for
 * all wizard steps.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardTitlePanel extends JPanel
{


	/**
	 * The default constructor for the panel.
	 */
	CartogramWizardTitlePanel (JFrame contentFrame)
	{
		
		// Setting panel parameters.
		this.setSize(350, 45);
		this.setLayout(null);
		
		
		// Create the title text.
		JLabel title = new JLabel("Cartogram creation wizard");
		title.setFont(new Font(null, Font.BOLD, 13));
		title.setLocation(0, 0);
		title.setSize(400, 20);
		this.add(title);
		
		
		// Create the sub-title text.
		/*JLabel subtitle = new JLabel(
			"A leap into a different space."
			);
		
		subtitle.setFont(new Font(null, Font.PLAIN, 11));
		subtitle.setLocation(0, 22);
		subtitle.setSize(400, 20);
		this.add(subtitle);*/
		
	}
	
	
	
}






class WizardStepIconPanel extends JPanel
{

	
	JLabel mIconLabel;
	
	ImageIcon mIcon1;
	ImageIcon mIcon2;
	ImageIcon mIcon3;
	ImageIcon mIcon4;
	ImageIcon mIcon5;
	ImageIcon mIcon6;
	ImageIcon mIcon7;
	
	/**
	 * The default constructor for the ScapeToad icon panel.
	 */
	WizardStepIconPanel (JFrame contentFrame)
	{
		
		// Setting some panel parameters.
		this.setSize(220, 30);
		this.setLayout(null);
		
		
		// Loading the step icons from the resources.
		ClassLoader cldr = this.getClass().getClassLoader();
		mIcon1 = new ImageIcon(cldr.getResource("WizardStep1.png"));
		mIcon2 = new ImageIcon(cldr.getResource("WizardStep2.png"));
		mIcon3 = new ImageIcon(cldr.getResource("WizardStep3.png"));
		mIcon4 = new ImageIcon(cldr.getResource("WizardStep4.png"));
		mIcon5 = new ImageIcon(cldr.getResource("WizardStep5.png"));
		mIcon6 = new ImageIcon(cldr.getResource("WizardStep6.png"));
		mIcon7 = new ImageIcon(cldr.getResource("WizardStep7.png"));
		

		// Create a new label containing the icon.
		mIconLabel = new JLabel(mIcon1);
		
		// Setting the label parameters.
		mIconLabel.setLayout(null);
		mIconLabel.setSize(206, 27);
		mIconLabel.setLocation(1, 1);
		
		// Add the icon label to this panel.
		this.add(mIconLabel);
		
	}
	
	
	
	
	public void setStepIcon (int step)
	{
		switch (step)
		{
			case 1:
				mIconLabel.setIcon(mIcon1);
				break;
			case 2:
				mIconLabel.setIcon(mIcon2);
				break;
			case 3:
				mIconLabel.setIcon(mIcon3);
				break;
			case 4:
				mIconLabel.setIcon(mIcon4);
				break;
			case 5:
				mIconLabel.setIcon(mIcon5);
				break;
			case 6:
				mIconLabel.setIcon(mIcon6);
				break;
			case 7:
				mIconLabel.setIcon(mIcon7);
				break;
		}
		
	}	// WizardStepIcon.setStepIcon


}









/**
 * This class represents the first screen in the cartogram wizard.
 * It contains general information on cartograms and on the steps
 * needed in cartogram creation.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardPanelZero extends JPanel implements HyperlinkListener
{



	/**
	 * The default constructor for the panel.
	 */
	CartogramWizardPanelZero (JFrame contentFrame)
	{
	
		this.setLocation(160, 90);
		this.setSize(440, 340);
		this.setLayout(null);
				
		// Add the Next button
		JButton nextButton = new JButton("Next >");
		nextButton.setLocation(340, 314);
		nextButton.setSize(100, 26);
		nextButton.setMnemonic(KeyEvent.VK_ENTER);
		
		nextButton.addActionListener(new 
			CartogramWizardGoToStepAction((CartogramWizard)contentFrame, 1));
			
		this.add(nextButton);
		
		
		
		// Create the text pane which displays the message.
		// The message itself is read from a RTF file.
		
		JTextPane text = new JTextPane();
		
		// Get the wizard content from a text file.
		ClassLoader cldr = this.getClass().getClassLoader();
		URL wizardStepZeroURL = cldr.getResource("WizardIntroduction.html");
	
		// Get the content from the text file.
		String wizardStepZeroContent = null;
		try
		{
			InputStream inStream = wizardStepZeroURL.openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			
			inStream.close();
			
			wizardStepZeroContent = inBuffer.toString();
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		
		
		text.setContentType("text/html");
		text.setText(wizardStepZeroContent);
		text.setEditable(false);
		text.addHyperlinkListener(this);
		text.setBackground(null);
		text.setLocation(0, 0);
		text.setSize(440, 300);
		this.add(text);
		
		
		
		

		// Add the help button
		
		//ClassLoader cldr = this.getClass().getClassLoader();
		
		java.net.URL imageURL = cldr.getResource("help-22.png");
		ImageIcon helpIcon = new ImageIcon(imageURL);

		JButton helpButton = 
			new JButton(helpIcon);
		
		helpButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		helpButton.setHorizontalTextPosition(AbstractButton.CENTER);
		helpButton.setSize(30, 30);
		helpButton.setLocation(0, 312);
		helpButton.setFocusable(false);
		helpButton.setContentAreaFilled(false);
		helpButton.setBorderPainted(false);
		
		helpButton.addActionListener(new CartogramWizardShowURL(
			"http://chorogram.choros.ch/scapetoad/help/a-cartogram-creation.php"));
		
		this.add(helpButton);
		
		
	}





	public void hyperlinkUpdate (HyperlinkEvent e)
	{
		try
		{
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
                Browser.init();
				Browser.displayURL(e.getURL().toString());
             }
            
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
	}





}








/**
 * This class represents the first screen in the cartogram wizard.
 * It contains a pop-up menu for selecting the master layer.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardPanelOne extends JPanel
{


	CartogramWizard mCartogramWizard = null;
	JComboBox mLayerMenu = null;
	
	
	/**
	 * The "Next" button.
	 */
	JButton mNextButton = null;
	


	/**
	 * The default constructor for the panel.
	 */
	CartogramWizardPanelOne (JFrame contentFrame)
	{
		
		mCartogramWizard = (CartogramWizard)contentFrame;
	
		int width = 440;
		int height = 340;
	
		this.setLocation(160, 90);
		this.setSize(width, height);
		this.setLayout(null);
				
		
		// Add the Next button
		mNextButton = new JButton("Next >");
		mNextButton.setLocation(340, 314);
		mNextButton.setSize(100, 26);
		mNextButton.setMnemonic(KeyEvent.VK_ACCEPT);
		
		mNextButton.addActionListener(new
			CartogramWizardGoToStepAction(mCartogramWizard, 2));
		
		this.add(mNextButton);
		
		
		// Add the Back button
		JButton backButton = new JButton("< Back");
		backButton.setLocation(235, 314);
		backButton.setSize(100, 26);
		
		backButton.addActionListener(new
			CartogramWizardGoToStepAction(mCartogramWizard, 0));
			
		this.add(backButton);
		
		
		// Add a pop-up menu with the list of available layers.
		
		JLabel layerMenuLabel = new JLabel("Spatial coverage:");
		layerMenuLabel.setFont(new Font(null, Font.PLAIN, 11));
		layerMenuLabel.setBounds(0, 0, 190, 14);
		this.add(layerMenuLabel);
		
		mLayerMenu = new JComboBox();
		mLayerMenu.setBounds(0, 20, 190, 26);
		mLayerMenu.setFont(new Font(null, Font.PLAIN, 11));
		mLayerMenu.setMaximumRowCount(20);
		
		// Add all polygon layers to the list.
		int nlayers = AppContext.layerManager.size();
		
		// Check for each layer whether it is a polygon layer or not.
		for (int lyrcnt = 0; lyrcnt < nlayers; lyrcnt++)
		{
			Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
			FeatureCollectionWrapper fcw = lyr.getFeatureCollectionWrapper();
			int nfeat = fcw.size();
			if (nfeat > 0)
			{
				Feature feat = (Feature)fcw.getFeatures().get(0);
				Geometry geom = feat.getGeometry();
				if (geom.getArea() != 0.0)
				{
					mLayerMenu.addItem(lyr.getName());
				}
			}
		
			
		}
		
		
		// If there is no layer for the cartogram deformation,
		// add a menu item "<none>" and disable the "Next" button.
		if (mLayerMenu.getItemCount() == 0)
		{
			mLayerMenu.addItem("<none>");
			mNextButton.setEnabled(false);
		}
		else
		{
			mNextButton.setEnabled(true);
		}
		
			
		this.add(mLayerMenu);
		
		
		
		// Adding the polygon image
		ClassLoader cldr = this.getClass().getClassLoader();
		URL iconURL = cldr.getResource("Topology.png");
		ImageIcon topologyImage = new ImageIcon(iconURL);

		// Create a new label containing the image.
		JLabel iconLabel = new JLabel(topologyImage);
		
		// Setting the label parameters.
		iconLabel.setLayout(null);
		iconLabel.setSize(192, 239);
		iconLabel.setLocation(240, 30);
		
		// Add the icon label to this panel.
		this.add(iconLabel);
		
		
		
		// Adding the explanatory text.
		// The message itself is read from a RTF file.
		JTextPane layerMenuTextPane = new JTextPane();
	
		// Get the content from the text file.
		String layerMenuText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("LayerMenuText.rtf").openStream();
				
			StringBuffer inBuffer = new StringBuffer();
			int c;
			
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			
			inStream.close();
			layerMenuText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		
		layerMenuTextPane.setContentType("text/rtf");
		layerMenuTextPane.setText(layerMenuText);
		layerMenuTextPane.setEditable(false);
		layerMenuTextPane.setFont(new Font(null, Font.PLAIN, 11));
		layerMenuTextPane.setBackground(null);
		layerMenuTextPane.setLocation(0, 60);
		layerMenuTextPane.setSize(220, 240);
		this.add(layerMenuTextPane);
		
		
		
		// ADD THE HELP BUTTON
		
		//ClassLoader cldr = this.getClass().getClassLoader();
		
		java.net.URL imageURL = cldr.getResource("help-22.png");
		ImageIcon helpIcon = new ImageIcon(imageURL);

		JButton helpButton = 
			new JButton(helpIcon);
		
		helpButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		helpButton.setHorizontalTextPosition(AbstractButton.CENTER);
		helpButton.setSize(30, 30);
		helpButton.setLocation(0, 312);
		helpButton.setFocusable(false);
		helpButton.setContentAreaFilled(false);
		helpButton.setBorderPainted(false);
		
		helpButton.addActionListener(new CartogramWizardShowURL(
			"http://chorogram.choros.ch/scapetoad/help/a-cartogram-creation.php#cartogram-layer"));
		
		this.add(helpButton);
		
		
		
		
	}	// CartogramWizardPanelOne.<init>




	/**
	 * If this panel is hidden, set the selected layer name
	 * in the cartogram wizard.
	 * This is an overriden method from its super class. Once the
	 * job done, we call the super class' setVisible method.
	 */
	public void setVisible (boolean visible)
	{
	
		if (visible)
		{
			this.updateLayerList();
			
			// If there is no layer for the cartogram deformation,
			// add a menu item "<none>" and disable the "Next" button.
			if (mLayerMenu.getItemCount() == 0)
			{
				mLayerMenu.addItem("<none>");
				mNextButton.setEnabled(false);
			}
			else
			{
				mNextButton.setEnabled(true);
			}
		}
		
	
		if (!visible)
			mCartogramWizard.setCartogramLayerName(
				(String)mLayerMenu.getSelectedItem());
		
		super.setVisible(visible);
		
	}	// CartogramWizardPanelOne.setVisible



	/**
	 * Updates the pop-up menu with the cartogram layers.
	 */
	public void updateLayerList ()
	{
	
		String selectedLayer = null;
		if (mLayerMenu != null)
		{
			selectedLayer = (String)mLayerMenu.getSelectedItem();
		}
	
		mLayerMenu.removeAllItems();
		
		// Add all polygon layers to the list.
		int nlayers = AppContext.layerManager.size();
		
		// Check for each layer whether it is a polygon layer or not.
		for (int lyrcnt = 0; lyrcnt < nlayers; lyrcnt++)
		{
			Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
			FeatureCollectionWrapper fcw = lyr.getFeatureCollectionWrapper();
			int nfeat = fcw.size();
			if (nfeat > 0)
			{
				Feature feat = (Feature)fcw.getFeatures().get(0);
				Geometry geom = feat.getGeometry();
				if (geom.getArea() != 0.0)
				{
					String layerName = lyr.getName();
					mLayerMenu.addItem(layerName);
					if (layerName == selectedLayer)
						mLayerMenu.setSelectedItem(layerName);
				}
			}
		
		}
		
	}	// CartogramWizardPanelOne.updateLayerList



}	// CartogramWizardPanelOne





/**
 * This class represents the third screen in the cartogram wizard.
 * It is used for the selection of cartogram attribute.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardPanelTwo extends JPanel implements HyperlinkListener
{


	CartogramWizard mCartogramWizard = null;
	JComboBox mAttributeMenu = null;
	String mCurrentCartogramLayer = null;
	
	/**
	 * The "Next" button.
	 */
	JButton mNextButton = null;
	
	/**
	 * The attribute type radio button.
	 */
	ButtonGroup mAttributeTypeButtonGroup = null;
	JRadioButton mAttributeTypeDensityButton = null;
	JRadioButton mAttributeTypePopulationButton = null;
	
	JTextField mMissingValueTextField = null;
	


	/**
	 * The default constructor for the panel.
	 */
	CartogramWizardPanelTwo (JFrame contentFrame)
	{
	
		mCartogramWizard = (CartogramWizard)contentFrame;
	
		this.setLocation(160, 90);
		this.setSize(440, 340);
		this.setLayout(null);
				
		// Add the Next button
		mNextButton = new JButton("Next >");
		mNextButton.setLocation(340, 314);
		mNextButton.setSize(100, 26);
		mNextButton.setMnemonic(KeyEvent.VK_ACCEPT);
		
		mNextButton.addActionListener(new
			CartogramWizardGoToStepAction((CartogramWizard)contentFrame, 3));
		
		this.add(mNextButton);
		
		// Add the Back button
		JButton backButton = new JButton("< Back");
		backButton.setLocation(235, 314);
		backButton.setSize(100, 26);
		
		backButton.addActionListener(new
			CartogramWizardGoToStepAction((CartogramWizard)contentFrame, 1));
			
		this.add(backButton);
		
		
		
		// Create the attribute label
		JLabel attributeLabel = new JLabel("Cartogram attribute:");
		attributeLabel.setFont(new Font(null, Font.PLAIN, 11));
		attributeLabel.setBounds(0, 0, 190, 14);
		this.add(attributeLabel);
		
		
		
		// Create the attribute pop-up menu
		mAttributeMenu = new JComboBox();
		mAttributeMenu.setBounds(0, 20, 190, 26);
		mAttributeMenu.setFont(new Font(null, Font.PLAIN, 11));
		mAttributeMenu.setMaximumRowCount(20);
		
		
		// Find out the current cartogram layer name.
		mCurrentCartogramLayer = mCartogramWizard.getCartogramLayerName();


		// Get the numerical attributes of the current cartogram layer.
		if (mCurrentCartogramLayer != null &&
			mCurrentCartogramLayer != "" &&
			mCurrentCartogramLayer != "<none>")
		{
		
			Layer lyr = AppContext.layerManager.getLayer(
							mCurrentCartogramLayer);
			
			FeatureSchema fs = 
				lyr.getFeatureCollectionWrapper().getFeatureSchema();
			
			int nattrs = fs.getAttributeCount();
			
			for (int attrcnt = 0; attrcnt < nattrs; attrcnt++)
			{
				
				AttributeType attrtype = fs.getAttributeType(attrcnt);
				if (attrtype == AttributeType.DOUBLE ||
					attrtype == AttributeType.INTEGER)
				{
					mAttributeMenu.addItem(fs.getAttributeName(attrcnt));
				}
				
			}
			
		}
		
		if (mAttributeMenu.getItemCount() == 0)
		{
			mAttributeMenu.addItem("<none>");
			mNextButton.setEnabled(false);
		}
		else
		{
			mNextButton.setEnabled(true);
		}
		
		
		
		// Create the attribute type label
		JLabel attributeTypeLabel = new JLabel("Attribute type:");
		attributeTypeLabel.setFont(new Font(null, Font.PLAIN, 11));
		attributeTypeLabel.setBounds(220, 0, 190, 14);
		this.add(attributeTypeLabel);
		
		
		
		// Create the attribute type radio buttons.
		mAttributeTypePopulationButton = new JRadioButton("Mass");
		mAttributeTypePopulationButton.setSelected(true);
		mAttributeTypePopulationButton.setFont(new Font(null, Font.PLAIN, 11));
		mAttributeTypePopulationButton.setBounds(220, 20, 190, 20);
		
		mAttributeTypeDensityButton = new JRadioButton("Density");
		mAttributeTypeDensityButton.setSelected(false);
		mAttributeTypeDensityButton.setFont(new Font(null, Font.PLAIN, 11));
		mAttributeTypeDensityButton.setBounds(220, 45, 190, 20);
		
		mAttributeTypeButtonGroup = new ButtonGroup();
		mAttributeTypeButtonGroup.add(mAttributeTypePopulationButton);
		mAttributeTypeButtonGroup.add(mAttributeTypeDensityButton);
		
		this.add(mAttributeTypePopulationButton);
		this.add(mAttributeTypeDensityButton);
		
		
		this.add(mAttributeMenu);
		
		
		
		
		
		// Create the text pane which displays the attribute message.
		// The message itself is read from a RTF file.
		
		JTextPane attributeMenuTextPane = new JTextPane();
		
		// Get the wizard content from a text file.
		ClassLoader cldr = this.getClass().getClassLoader();
	
		// Get the content from the text file.
		String attributeMenuText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("AttributeMenuText.rtf").openStream();
				
			StringBuffer inBuffer = new StringBuffer();
			int c;
			
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			
			inStream.close();
			attributeMenuText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		
		attributeMenuTextPane.setContentType("text/rtf");
		attributeMenuTextPane.setText(attributeMenuText);
		attributeMenuTextPane.setEditable(false);
		attributeMenuTextPane.setFont(new Font(null, Font.PLAIN, 11));
		attributeMenuTextPane.setBackground(null);
		attributeMenuTextPane.setLocation(0, 80);
		attributeMenuTextPane.setSize(190, 100);
		this.add(attributeMenuTextPane);



		// Create the text pane which displays the attribute type message.
		// The message itself is read from a RTF file.
		
		JTextPane attributeTypeTextPane = new JTextPane();
	
		// Get the content from the text file.
		String attributeTypeText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("AttributeTypeText.html").openStream();
				
			StringBuffer inBuffer = new StringBuffer();
			int c;
			
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			
			inStream.close();
			attributeTypeText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		
		attributeTypeTextPane.setContentType("text/html");
		attributeTypeTextPane.setText(attributeTypeText);
		attributeTypeTextPane.setEditable(false);
		attributeTypeTextPane.addHyperlinkListener(this);
		attributeTypeTextPane.setBackground(null);
		attributeTypeTextPane.setLocation(220, 80);
		attributeTypeTextPane.setSize(190, 200);
		this.add(attributeTypeTextPane);




		// MISSING VALUE TEXT FIELD
		
		/*JLabel missingValueLabel = 
			new JLabel("Define a missing value:");
		missingValueLabel.setBounds(0, 200, 190, 16);
		missingValueLabel.setFont(new Font(null, Font.BOLD, 11));
		this.add(missingValueLabel);
		
		JLabel missingValueLabel2 = 
			new JLabel("(leave empty if you don't have any)");
		missingValueLabel2.setBounds(0, 216, 190, 16);
		missingValueLabel2.setFont(new Font(null, Font.PLAIN, 11));
		this.add(missingValueLabel2);

		String missingValue = mCartogramWizard.getMissingValue();
		mMissingValueTextField = new JTextField(missingValue);
		mMissingValueTextField.setBounds(0, 232, 100, 26);
		mMissingValueTextField.setFont(new Font(null, Font.PLAIN, 11));
		mMissingValueTextField.setHorizontalAlignment(JTextField.RIGHT);
		this.add(mMissingValueTextField);*/




		// ADD THE HELP BUTTON
		
		//ClassLoader cldr = this.getClass().getClassLoader();
		
		java.net.URL imageURL = cldr.getResource("help-22.png");
		ImageIcon helpIcon = new ImageIcon(imageURL);

		JButton helpButton = 
			new JButton(helpIcon);
		
		helpButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		helpButton.setHorizontalTextPosition(AbstractButton.CENTER);
		helpButton.setSize(30, 30);
		helpButton.setLocation(0, 312);
		helpButton.setFocusable(false);
		helpButton.setContentAreaFilled(false);
		helpButton.setBorderPainted(false);
		
		helpButton.addActionListener(new CartogramWizardShowURL(
			"http://chorogram.choros.ch/scapetoad/help/a-cartogram-creation.php#cartogram-attribute"));
		
		this.add(helpButton);



	}	// CartogramWizardPanelTwo.<init>




	
	public void setVisible(boolean visible)
	{
		if (visible)
			this.updateAttributeName();
		else
			mCartogramWizard.setCartogramAttributeName(
				(String)mAttributeMenu.getSelectedItem());
	
		super.setVisible(visible);
	}




	public void updateAttributeName()
	{
	
		// Find out the current layer name.
		String layerName = mCartogramWizard.getCartogramLayerName();
		
		if (mCurrentCartogramLayer != layerName)
		{
			
			// Change the layer name attribute.
			mCurrentCartogramLayer = layerName;


			// Remove all existing items.
			mAttributeMenu.removeAllItems();
			

			// Get the numerical attributes of the current cartogram layer.
			if (mCurrentCartogramLayer != null &&
				mCurrentCartogramLayer != "" &&
				mCurrentCartogramLayer != "<none>")
			{
		
				Layer lyr = AppContext.layerManager.getLayer(
								mCurrentCartogramLayer);
			
				FeatureSchema fs = 
					lyr.getFeatureCollectionWrapper().getFeatureSchema();
			
				int nattrs = fs.getAttributeCount();
			
				for (int attrcnt = 0; attrcnt < nattrs; attrcnt++)
				{
				
					AttributeType attrtype = fs.getAttributeType(attrcnt);
					if (attrtype == AttributeType.DOUBLE ||
						attrtype == AttributeType.INTEGER)
					{
						mAttributeMenu.addItem(fs.getAttributeName(attrcnt));
					}
				
				}
			
			}
		
		
			// If there is no attribute we can select,
			// add an item "<none>" and disable the "Next" button.
			if (mAttributeMenu.getItemCount() == 0)
			{
				mAttributeMenu.addItem("<none>");
				mNextButton.setEnabled(false);
			}
			else
			{
				mNextButton.setEnabled(true);
			}
			
				
		}
	
	}	// CartogramWizardPanelTwo.updateAttributeName




	/**
	 * Tells us whether the attribute type is a density or population value.
	 */
	public boolean attributeIsDensityValue ()
	{
	
		return mAttributeTypeDensityButton.isSelected();
	
	}	// CartogramWizardPanelTwo.attributeIsDensityValue





	public void hyperlinkUpdate (HyperlinkEvent e)
	{
		try
		{
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
                Browser.init();
				Browser.displayURL(e.getURL().toString());
             }
            
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
	}




	public String getMissingValue ()
	{
		//return mMissingValueTextField.getText();
		return "";
	}
	


}	// CartogramWizardPanelTwo









/**
 * This class represents the panel for the selection of the
 * layers for simultaneous and constrained transformation.
 * There is also a slider for the amount of deformation and
 * the size for the grid to overlay.
 */
class CartogramWizardPanelThree extends JPanel
{

	
	CartogramWizard mCartogramWizard = null;
	
	
	/**
	 * Slider for the amount of deformation (high area error and low
	 * shape error or low area error and high shape error).
	 */
	JSlider mDeformationSlider = null;

	
	
	/**
	 * The default constructor for the panel.
	 */
	CartogramWizardPanelThree (JFrame contentFrame)
	{
	
		mCartogramWizard = (CartogramWizard)contentFrame;
		
		this.setLocation(160, 90);
		this.setSize(440, 340);
		this.setLayout(null);
	
		
		// Button for simultanous layers.
		JButton simLayerButton = new JButton("Layers to transform...");
		simLayerButton.setLocation(0, 0);
		simLayerButton.setSize(240, 26);
		simLayerButton.addActionListener(
			new CartogramWizardSimulaneousLayerAction("showDialog", null));
		this.add(simLayerButton);

		// Create the text pane which displays the help text for the
		// simultaneous layers.
		JTextPane simLayerTextPane = new JTextPane();
		ClassLoader cldr = this.getClass().getClassLoader();
		String simLayerText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("SimLayersText.rtf").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			simLayerText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		simLayerTextPane.setContentType("text/rtf");
		simLayerTextPane.setText(simLayerText);
		simLayerTextPane.setEditable(false);
		simLayerTextPane.setFont(new Font(null, Font.PLAIN, 11));
		simLayerTextPane.setBackground(null);
		simLayerTextPane.setLocation(40, 35);
		simLayerTextPane.setSize(400, 80);
		this.add(simLayerTextPane);
		
		
		
		// Button for constrained layers.
		JButton constLayersButton = 
			new JButton("Constrained transformation...");
		constLayersButton.setLocation(0, 140);
		constLayersButton.setSize(240, 26);
		constLayersButton.addActionListener(
			new CartogramWizardConstrainedLayerAction("showDialog", null));
		this.add(constLayersButton);
		
		// Create the text pane which displays the help text for the
		// simultaneous layers.
		JTextPane constLayerTextPane = new JTextPane();
		String constLayerText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("ConstLayersText.rtf").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			constLayerText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		constLayerTextPane.setContentType("text/rtf");
		constLayerTextPane.setText(constLayerText);
		constLayerTextPane.setEditable(false);
		constLayerTextPane.setFont(new Font(null, Font.PLAIN, 11));
		constLayerTextPane.setBackground(null);
		constLayerTextPane.setLocation(40, 175);
		constLayerTextPane.setSize(400, 60);
		this.add(constLayerTextPane);
		
		
		
		
		// Add the Next button
		JButton computeButton = new JButton("Next >");
		computeButton.setLocation(340, 314);
		computeButton.setSize(100, 26);
		computeButton.setMnemonic(KeyEvent.VK_ENTER);
		computeButton.addActionListener(new 
			CartogramWizardGoToStepAction((CartogramWizard)contentFrame, 4));
		this.add(computeButton);
		
		
		
		// Add the Back button
		JButton backButton = new JButton("< Back");
		backButton.setLocation(235, 314);
		backButton.setSize(100, 26);
		backButton.addActionListener(new
			CartogramWizardGoToStepAction((CartogramWizard)contentFrame, 2));
		this.add(backButton);

		
		
		
		// Create the slave layer list.
		// By default, we add all layers but the master layer.
		/*int nlayers = AppContext.layerManager.size();
		int lyrcnt = 0;
		Vector layerList = new Vector();
		String masterLayerName = mCartogramWizard.getCartogramLayerName();
		for (lyrcnt = 0; lyrcnt < nlayers; lyrcnt++)
		{
			Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
			if (lyr.getName() != masterLayerName)
			{
				layerList.add(lyr);
			}
		}
		mCartogramWizard.setSimultaneousLayers(layerList);*/
		
		
		
		// ADD THE HELP BUTTON
		
		//ClassLoader cldr = this.getClass().getClassLoader();
		
		java.net.URL imageURL = cldr.getResource("help-22.png");
		ImageIcon helpIcon = new ImageIcon(imageURL);

		JButton helpButton = 
			new JButton(helpIcon);
		
		helpButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		helpButton.setHorizontalTextPosition(AbstractButton.CENTER);
		helpButton.setSize(30, 30);
		helpButton.setLocation(0, 312);
		helpButton.setFocusable(false);
		helpButton.setContentAreaFilled(false);
		helpButton.setBorderPainted(false);
		
		helpButton.addActionListener(new CartogramWizardShowURL(
			"http://chorogram.choros.ch/scapetoad/help/b-other-layers.php"));
		
		this.add(helpButton);

		
	
	}	// CartogramWizardPanelThree.<init>


}	// CartogramWizardPanelThree











/**
 * This class represents the panel for the slider for the amount 
 * of deformation the size for the grid to overlay.
 */
class CartogramWizardPanelFour extends JPanel
{

	
	CartogramWizard mCartogramWizard = null;
	
	
	/**
	 * Slider for the amount of deformation (high area error and low
	 * shape error or low area error and high shape error).
	 */
	JSlider mDeformationSlider = null;

	
	
	/**
	 * The default constructor for the panel.
	 */
	CartogramWizardPanelFour (JFrame contentFrame)
	{
	
		mCartogramWizard = (CartogramWizard)contentFrame;
		
		this.setLocation(160, 90);
		this.setSize(440, 340);
		this.setLayout(null);
	
		ClassLoader cldr = this.getClass().getClassLoader();

		// Add the slider for the amount of deformation.
		Font smallFont = new Font(null, Font.PLAIN, 11);
		mDeformationSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
		mDeformationSlider.setMajorTickSpacing(25);
		mDeformationSlider.setMinorTickSpacing(5);
		mDeformationSlider.setPaintTicks(true);
		mDeformationSlider.setFont(smallFont);
		mDeformationSlider.setSize(440, 40);
		mDeformationSlider.setLocation(0, 20);
		
		Hashtable labelTable = new Hashtable();
		JLabel sliderLabel = new JLabel("Low");
		sliderLabel.setFont(smallFont);
		labelTable.put(new Integer(0), sliderLabel);
		sliderLabel = new JLabel("Medium");
		sliderLabel.setFont(smallFont);
		labelTable.put(new Integer(50), sliderLabel);
		sliderLabel = new JLabel("High");
		sliderLabel.setFont(smallFont);
		labelTable.put(new Integer(100), sliderLabel);
		
		mDeformationSlider.setLabelTable(labelTable);
		mDeformationSlider.setPaintLabels(true);
		this.add(mDeformationSlider);
		
		
		// Add the label for the amount of deformation.
		JLabel deformationLabel = new JLabel("Transformation quality:");
		deformationLabel.setSize(440, 14);
		deformationLabel.setFont(new Font(null, Font.BOLD, 11));
		deformationLabel.setLocation(0, 0);
		this.add(deformationLabel);




		// Create the text pane which displays the help text for the
		// amount of deformation.
		JTextPane deformationTextPane = new JTextPane();
		String deformationText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("AmountOfDeformationText.rtf").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			deformationText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		deformationTextPane.setContentType("text/rtf");
		deformationTextPane.setText(deformationText);
		deformationTextPane.setEditable(false);
		deformationTextPane.setFont(new Font(null, Font.PLAIN, 11));
		deformationTextPane.setBackground(null);
		deformationTextPane.setLocation(40, 70);
		deformationTextPane.setSize(400, 70);
		this.add(deformationTextPane);
		
		
		
		// ADVANCED OPTIONS
		
		// A button and an explanatory text for the advanced options.
		JButton advancedButton = 
			new JButton("Advanced options...");
		advancedButton.setLocation(0, 170);
		advancedButton.setSize(240, 26);
		advancedButton.addActionListener(
			new CartogramWizardAdvancedOptionsAction("showDialog", null));
		this.add(advancedButton);
		
		// Create the text pane which displays the help text for the
		// simultaneous layers.
		JTextPane advancedTextPane = new JTextPane();
		String advancedText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("AdvancedOptionsText.rtf").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			advancedText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		advancedTextPane.setContentType("text/rtf");
		advancedTextPane.setText(advancedText);
		advancedTextPane.setEditable(false);
		advancedTextPane.setFont(new Font(null, Font.PLAIN, 11));
		advancedTextPane.setBackground(null);
		advancedTextPane.setLocation(40, 205);
		advancedTextPane.setSize(400, 60);
		this.add(advancedTextPane);
		
		
		
		
		
		
		// Add the Compute button
		JButton computeButton = new JButton("Compute");
		computeButton.setLocation(340, 314);
		computeButton.setSize(100, 26);
		computeButton.setMnemonic(KeyEvent.VK_ENTER);
		computeButton.addActionListener(new 
			CartogramWizardComputeAction((CartogramWizard)contentFrame));
		this.add(computeButton);
		
		
		
		// Add the Back button
		JButton backButton = new JButton("< Back");
		backButton.setLocation(235, 314);
		backButton.setSize(100, 26);
		backButton.addActionListener(new
			CartogramWizardGoToStepAction((CartogramWizard)contentFrame, 3));
		this.add(backButton);





		// ADD THE HELP BUTTON
		
		//ClassLoader cldr = this.getClass().getClassLoader();
		
		java.net.URL imageURL = cldr.getResource("help-22.png");
		ImageIcon helpIcon = new ImageIcon(imageURL);

		JButton helpButton = 
			new JButton(helpIcon);
		
		helpButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		helpButton.setHorizontalTextPosition(AbstractButton.CENTER);
		helpButton.setSize(30, 30);
		helpButton.setLocation(0, 312);
		helpButton.setFocusable(false);
		helpButton.setContentAreaFilled(false);
		helpButton.setBorderPainted(false);
		
		helpButton.addActionListener(new CartogramWizardShowURL(
			"http://chorogram.choros.ch/scapetoad/help/c-transformation-parameters.php"));
		
		this.add(helpButton);


		
	
	}	// CartogramWizardPanelFour.<init>
	
	
	
	
	
	/**
	 * If the panel is shown, update the layer list before displaying
	 * the panel.
	 */
	public void setVisible (boolean visible)
	{
		if (visible)
		{
			//this.updateLayerList();
			//this.updateConstrainedLayerList();
		}
		else
		{
			// Update the amount of deformation.
			mCartogramWizard.setAmountOfDeformation(
				(int)mDeformationSlider.getValue());
				
		}
		
		super.setVisible(visible);
	}





	public void enableAmountOfDeformationSlider (boolean enable)
	{
		mDeformationSlider.setEnabled(enable);
	}
	


}	// CartogramWizardPanelFour










/**
 * This class represents the panel shown during the cartogram computation.
 * It shows a progress bar and a label explaining the advances in the
 * computation.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardRunningPanel extends JPanel
{

	/**
	 * The cartogram wizard.
	 */
	CartogramWizard mCartogramWizard = null;


	/**
	 * The progress bar.
	 */
	JProgressBar mProgressBar = null;



	/**
	 * The progress labels. The label 1 is for the current task name.
	 * In a lengthy task, the label 2 may be used for more detailed
	 * user information.
	 */
	JLabel mProgressLabel1 = null;
	JLabel mProgressLabel2 = null;
	


	/**
	 * The default constructor.
	 */
	CartogramWizardRunningPanel (JFrame contentFrame)
	{
	
		mCartogramWizard = (CartogramWizard)contentFrame;
	
	
		this.setLocation(160, 90);
		this.setSize(440, 340);
		this.setLayout(null);
		
		
	
		// Creating the progress bar.
		mProgressBar = new JProgressBar();
		mProgressBar.setMaximum(1000);
		mProgressBar.setValue(0);
		mProgressBar.setStringPainted(false);
		mProgressBar.setSize(300, 26);
		mProgressBar.setLocation(0, 0);
		this.add(mProgressBar);
		
		
		// Creating the progess label 1.
		mProgressLabel1 = new JLabel("Starting cartogram computation...");
		mProgressLabel1.setFont(new Font(null, Font.BOLD, 11));
		mProgressLabel1.setSize(400, 14);
		mProgressLabel1.setLocation(0, 30);
		this.add(mProgressLabel1);
		
		
		// Creating the progress label 2.
		mProgressLabel2 = new JLabel("");
		mProgressLabel2.setFont(new Font(null, Font.PLAIN, 11));
		mProgressLabel2.setSize(400, 14);
		mProgressLabel2.setLocation(0, 50);
		this.add(mProgressLabel2);
	
	
	}
	
	
	
	
	/**
	 * Updates the progress bar using the nloops parameter.
	 * The parameter must be between 0 and 1000.
	 * @param nloops the progress status (0-1000).
	 */
	public void updateProgressBar (int nloops)
	{
		
		if (nloops < 0) nloops = 0;
		if (nloops > 1000) nloops = 1000;
		mProgressBar.setValue(nloops);
	
	}	// CartogramWizardRunningPanel.updateProgressBar
	
	


	/**
	 * Updates the progress label 1.
	 */
	public void updateProgressLabel1 (String label1)
	{
		
		mProgressLabel1.setText(label1);
		mProgressLabel1.repaint();
	
	}	// CartogramWizardRunningPanel.updateProgressLabel1
	
	


	/**
	 * Updates the progress label 2.
	 */
	public void updateProgressLabel2 (String label2)
	{
		
		mProgressLabel2.setText(label2);
	
	}	// CartogramWizardRunningPanel.updateProgressLabel2






}	// CartogramWizardRunningPanel






/**
 * This class shows the finished panel in the cartogram wizard.
 */
class CartogramWizardFinishedPanel extends JPanel
{

	/**
	 * Attributes for the text to display and for the report.
	 */
	String mShortMessage = null;


	/**
	 * Says whether an error has occured during the
	 * cartogram computation process.
	 */
	boolean mErrorOccured = false;
	
	
	/**
	 * The title of the error message, if there is one.
	 */
	String mErrorTitle = null;
	
	/**
	 * The error message itself, if there is one.
	 */
	String mErrorMessage = null;
	
	/**
	 * The technical details of the error, if there is one.
	 */
	String mStackTrace = null;
	
	
	/**
	 * The help button.
	 */
	JButton mHelpButton = null;
	
	
	/**
	 * The save report button.
	 */
	JButton mSaveReportButton = null;
	
	
	
	
	

	/**
	 * Initializes the new panel.
	 */
	CartogramWizardFinishedPanel (JFrame contentFrame)
	{
		this.setLocation(160, 90);
		this.setSize(440, 340);
		this.setLayout(null);
		
		
		
		// ADD THE HELP BUTTON
		
		ClassLoader cldr = this.getClass().getClassLoader();
		
		java.net.URL imageURL = cldr.getResource("help-22.png");
		ImageIcon helpIcon = new ImageIcon(imageURL);

		mHelpButton = new JButton(helpIcon);
		mHelpButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		mHelpButton.setHorizontalTextPosition(AbstractButton.CENTER);
		mHelpButton.setSize(30, 30);
		mHelpButton.setLocation(0, 312);
		mHelpButton.setFocusable(false);
		mHelpButton.setContentAreaFilled(false);
		mHelpButton.setBorderPainted(false);
		
		mHelpButton.addActionListener(new CartogramWizardShowURL(
			"http://chorogram.choros.ch/scapetoad/help/d-computation-report.php"));




		mSaveReportButton = new JButton("Save report...");
		mSaveReportButton.setBounds(300, 312, 130, 26);
		mSaveReportButton.setVisible(false);
		mSaveReportButton.addActionListener(
			new CartogramWizardSaveReportAction());
		
		
				
	}	// CartogramWizardFinishedPanel.<init>
	
	
	
	
	/**
	 * Adapts the finished panels according to the current
	 * cartogram wizard settings (parameters and error message).
	 */
	public void setVisible (boolean visible)
	{
		
		if (visible)
		{
		
			JButton cancelButton = AppContext.cartogramWizard.getCancelButton();
			cancelButton.setText("End");
			
		
			// Remove all elements in this pane.
			this.removeAll();
		
			if (mErrorOccured)
			{
				JLabel errorTitle = new JLabel(mErrorTitle);
				errorTitle.setFont(new Font(null, Font.BOLD, 11));
				errorTitle.setBounds(0, 0, 400, 14);
				this.add(errorTitle);
				
				JLabel finishedMessage = new JLabel(mErrorMessage);
				finishedMessage.setFont(new Font(null, Font.PLAIN, 11));
				finishedMessage.setBounds(0, 22, 400, 14);
				this.add(finishedMessage);
				
				JTextArea finishedReport = new JTextArea(mStackTrace);
				finishedReport.setFont(new Font(null, Font.PLAIN, 11));
				finishedReport.setEditable(false);
				
				JScrollPane scrollPane = new JScrollPane(finishedReport);
				scrollPane.setBounds(0, 45, 430, 250);
				this.add(scrollPane);
		
			}
			else
			{
				JLabel finishedTitle = new JLabel(
					"Cartogram computation successfully terminated");
				finishedTitle.setFont(new Font(null, Font.BOLD, 11));
				finishedTitle.setBounds(0, 0, 400, 14);
				this.add(finishedTitle);
				
				JLabel finishedMessage = new JLabel(mShortMessage);
				finishedMessage.setFont(new Font(null, Font.PLAIN, 11));
				finishedMessage.setBounds(0, 22, 400, 14);
				this.add(finishedMessage);
				
				JTextArea finishedReport = new JTextArea(
					AppContext.cartogramWizard.getCartogram().getComputationReport());
					
				finishedReport.setFont(new Font(null, Font.PLAIN, 11));
				finishedReport.setEditable(false);
				
				JScrollPane scrollPane = new JScrollPane(finishedReport);
				scrollPane.setBounds(0, 45, 430, 250);
				this.add(scrollPane);
				
				
				this.add(mSaveReportButton);
				mSaveReportButton.setVisible(true);
				
				
			}
			
			this.add(mHelpButton);
			
		}
		
		super.setVisible(visible);
		
	}	// CartogramWizardFinishedPanel.setVisible
	
	
	
	
	/**
	 * Defines whether an error has occured or not. This parameter will
	 * define whether the error message will be displayed or a report
	 * generated.
	 * @param errorOccured true if an error has occured.
	 */
	public void setErrorOccured (boolean errorOccured)
	{
		mErrorOccured = errorOccured;
		
	}	// CartogramWizardFinishedPanel.setErrorOccured
	
	
	
	
	/**
	 * Defines the error message.
	 * @param title the title of the error message.
	 * @param message a short description of the message.
	 * @param stackTrace the complete stack trace in the case of an exception.
	 */
	public void setErrorMessage (
		String title, String message, String stackTrace)
	{
		mErrorTitle = title;
		mErrorMessage = message;
		mStackTrace = stackTrace;
		
	}	// CartogramWizardFinishedPanel.setErrorMessage
	
	
	
	
	/**
	 * Defines the short message.
	 */
	public void setShortMessage (String message)
	{
		mShortMessage = message;
		
	}	// CartogramWizardFinishedPanel.setShortMessage
	
	
	
	
	 

}	// CartogramWizardFinishedPanel







/**
 * This class moves the wizard from the given step.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardGoToStepAction extends AbstractAction
{

	CartogramWizard mWizard = null;
	int mStep = -1;
	


	/**
	 * The constructor needs a reference to the CartogramWizard
	 * object.
	 */
	CartogramWizardGoToStepAction (CartogramWizard wizard, int step)
	{
		mWizard = wizard;
		mStep = step;
	}
	
	
	
	
	public void actionPerformed(ActionEvent e)
	{
		mWizard.goToStep(mStep);
	}
	



}




/**
 * This class closes the cartogram wizard.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2008-02-12
 */
class CartogramWizardCloseAction extends AbstractAction
{

	public void actionPerformed(ActionEvent e)
	{
		
		Cartogram cg = AppContext.cartogramWizard.getCartogram();
		if (cg != null)
		{
			boolean cgRunning = cg.isRunning();
			if (cgRunning)
				AppContext.cartogramWizard.getCartogram().interrupt();
		}
		
		AppContext.cartogramWizard.setVisible(false);
		AppContext.cartogramWizard.dispose();
		AppContext.cartogramWizard = null;
		
	}


}	// CartogramWizardCloseAction







/**
 * This class launches the cartogram computation process.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-12-01
 */
class CartogramWizardComputeAction extends AbstractAction
{

	/**
	 * A reference to the cartogram wizard. This is needed in order
	 * to extract the selected options.
	 */
	CartogramWizard mCartogramWizard = null;


	
	/**
	 * The default constructor.
	 */
	CartogramWizardComputeAction (CartogramWizard cartogramWizard)
	{
	
		mCartogramWizard = cartogramWizard;
	
	}	// CartogramWizardComputeAction.<init>



	
	/**
	 * This method launches the cartogram computation process.
	 */
	public void actionPerformed(ActionEvent e)
	{
		
		// Hide the 3rd wizard panel.
		mCartogramWizard.getPanelFour().setVisible(false);
		
		// Show the running panel.
		mCartogramWizard.getRunningPanel().setVisible(true);
		
		mCartogramWizard.getWizardStepIconPanel().setStepIcon(6);
		
		
		// Get the name of the selected layer.
		String selectedLayer = mCartogramWizard.getCartogramLayerName();
		
		// Get the name of the selected attribute.
		String selectedAttribute = mCartogramWizard.getCartogramAttributeName();
		
		// Get the attribute type (population or density value).
		boolean isDensityValue = 
			mCartogramWizard.getPanelTwo().attributeIsDensityValue();
		
		
		mCartogramWizard.setMissingValue(
			mCartogramWizard.getPanelTwo().getMissingValue());
		
		
		// Create a new cartogram instance and set the parameters.
		Cartogram cg = new Cartogram(mCartogramWizard);
		cg.setLayerManager(AppContext.layerManager);
		cg.setMasterLayer(selectedLayer);
		cg.setMasterAttribute(selectedAttribute);
		cg.setMasterAttributeIsDensityValue(isDensityValue);
		cg.setMissingValue(mCartogramWizard.getMissingValue());
		cg.setSlaveLayers(mCartogramWizard.getSimultaneousLayers());
		cg.setConstrainedDeformationLayers(
			mCartogramWizard.getConstrainedDeformationLayers());
			
		
		cg.setAmountOfDeformation(mCartogramWizard.getAmountOfDeformation());
		
		cg.setAdvancedOptionsEnabled(
			mCartogramWizard.getAdvancedOptionsEnabled());
			
		cg.setGridSize(mCartogramWizard.getCartogramGridSizeInX(),
			mCartogramWizard.getCartogramGridSizeInY());
			
		cg.setDiffusionGridSize(mCartogramWizard.getDiffusionGridSize());
		cg.setDiffusionIterations(mCartogramWizard.getDiffusionIterations());
		
		
		
		// Set the parameters for the deformation grid layer.
		cg.setCreateGridLayer(mCartogramWizard.getCreateGridLayer());
		cg.setGridLayerSize(mCartogramWizard.getDeformationGridSize());
		
		
		
		// Set the parameters for the legend layer.
		// We have to estimate the legend values.
		if (isDensityValue)
			cg.setCreateLegendLayer(false);
		else
		{
			cg.setCreateLegendLayer(true);
		}
		
		mCartogramWizard.setCartogram(cg);
		
		// Start the cartogram computation.
		cg.start();
		
		
	}	// CartogramWizardComputeAction.actionPerformed




}	// CartogramWizardComputeAction







/**
 * Dialog window for specifying some advanced parameters for the
 * cartogram computation process.
 * The following parameters can be modified:
 *  -- the creation of a deformation grid layer and its size
 *  -- the cartogram grid size
 *  -- the number of iterations for the Gastner algorithm
 *  -- the size of the Gastner grid size
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-02-01
 */
class CartogramWizardOptionsWindow extends JDialog 
	implements HyperlinkListener, ChangeListener
{


	JCheckBox mAdvancedOptionsCheckBox = null;
	
	/**
	 * The check box for creating a deformation grid layer.
	 */
	JCheckBox mGridLayerCheckBox = null;



	/**
	 * The text field for the deformation grid size.
	 */
	JTextField mGridSizeTextField = null;


	JTextField mCartogramGridSizeTextField = null;


	JComboBox mDiffusionGridMenu = null;
	JTextField mDiffusionIterationsTextField = null;
	
	JTextPane mManualParametersPane = null;
	JTextPane mGrid1Pane = null;
	JLabel mCartogramGridSizeLabel = null;
	JTextPane mGrid2Pane = null;
	JLabel mDiffusionGridSizeLabel = null;
	JTextPane mIterPane = null;
	JLabel mIterationsLabel = null;
	
	


	/**
	 * Constructor for the options window.
	 */
	CartogramWizardOptionsWindow ()
	{
		
		// Set the window parameters.
		this.setTitle("Advanced options");
			
		this.setSize(500, 580);
		this.setLocation(40, 50);
		this.setResizable(false);
		this.setLayout(null);
		this.setModal(true);
		
		
				
		// GRID LAYER CHECK BOX
		mGridLayerCheckBox = 
			new JCheckBox("Create a transformation grid layer");
		
		mGridLayerCheckBox.setSelected(
			AppContext.cartogramWizard.getCreateGridLayer());
			
		mGridLayerCheckBox.setFont(new Font(null, Font.BOLD, 11));
		mGridLayerCheckBox.setLocation(20, 20);
		mGridLayerCheckBox.setSize(300, 26);
		this.add(mGridLayerCheckBox);
		
		
		
		
		// DEFORMATION GRID LAYER HELP TEXT
		
		ClassLoader cldr = this.getClass().getClassLoader();
		
		JTextPane deformationGridPane = new JTextPane();
		String deformationText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("DeformationGridLayerText.html").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			deformationText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		deformationGridPane.setContentType("text/html");
		deformationGridPane.setText(deformationText);
		deformationGridPane.setEditable(false);
		deformationGridPane.addHyperlinkListener(this);
		deformationGridPane.setBackground(null);
		deformationGridPane.setLocation(45, 45);
		deformationGridPane.setSize(400, 30);
		this.add(deformationGridPane);
		
		
		
		// GRID SIZE TEXT FIELD
		JLabel gridSizeLabel = new JLabel("Enter the number of rows:");
		gridSizeLabel.setLocation(45, 85);
		gridSizeLabel.setSize(140, 26);
		gridSizeLabel.setFont(new Font(null, Font.PLAIN, 11));
		this.add(gridSizeLabel);
		
		int gridSize = AppContext.cartogramWizard.getDeformationGridSize();
		String gridSizeString = "" + gridSize;
		mGridSizeTextField = new JTextField(gridSizeString);
		mGridSizeTextField.setLocation(240, 85);
		mGridSizeTextField.setSize(50, 26);
		mGridSizeTextField.setFont(new Font(null, Font.PLAIN, 11));
		mGridSizeTextField.setHorizontalAlignment(JTextField.RIGHT);
		this.add(mGridSizeTextField);
		
		
		
		
		
		
		// Separator
		JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
		separator.setLocation(20, 120);
		separator.setSize(460, 10);
		this.add(separator);
		
		
		
		// ADVANCED OPTIONS CHECK BOX
		mAdvancedOptionsCheckBox = 
			new JCheckBox("Define cartogram parameters manually");
		
		mAdvancedOptionsCheckBox.setSelected(
			AppContext.cartogramWizard.getAdvancedOptionsEnabled());
			
		mAdvancedOptionsCheckBox.setFont(new Font(null, Font.BOLD, 11));
		mAdvancedOptionsCheckBox.setLocation(20, 140);
		mAdvancedOptionsCheckBox.setSize(360, 26);
		mAdvancedOptionsCheckBox.addChangeListener(this);
		this.add(mAdvancedOptionsCheckBox);
		
		
		
		// Manual parameters text
		mManualParametersPane = new JTextPane();
		String manualParametersText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("ManualParametersText.html").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			manualParametersText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		mManualParametersPane.setContentType("text/html");
		mManualParametersPane.setText(manualParametersText);
		mManualParametersPane.setEditable(false);
		mManualParametersPane.addHyperlinkListener(this);
		mManualParametersPane.setBackground(null);
		mManualParametersPane.setLocation(45, 170);
		mManualParametersPane.setSize(400, 30);
		mManualParametersPane.setEnabled(mAdvancedOptionsCheckBox.isSelected());
		this.add(mManualParametersPane);
		
		
		
		
		// Grid 1 text
		mGrid1Pane = new JTextPane();
		String grid1Text = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("Grid1Text.html").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			grid1Text = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		mGrid1Pane.setContentType("text/html");
		mGrid1Pane.setText(grid1Text);
		mGrid1Pane.setEditable(false);
		mGrid1Pane.addHyperlinkListener(this);
		mGrid1Pane.setBackground(null);
		mGrid1Pane.setLocation(45, 210);
		mGrid1Pane.setSize(400, 60);
		mGrid1Pane.setEnabled(mAdvancedOptionsCheckBox.isSelected());
		this.add(mGrid1Pane);
		
		
		
		
		
		
		
		// Cartogram grid size
		mCartogramGridSizeLabel = 
			new JLabel("Enter the number of grid rows:");
			
		mCartogramGridSizeLabel.setLocation(45, 270);
		mCartogramGridSizeLabel.setSize(170, 26);
		mCartogramGridSizeLabel.setFont(new Font(null, Font.PLAIN, 11));
		mCartogramGridSizeLabel.setEnabled(
			mAdvancedOptionsCheckBox.isSelected());
		this.add(mCartogramGridSizeLabel);
		
		int cgGridSizeX = AppContext.cartogramWizard.getCartogramGridSizeInX();
		int cgGridSizeY = AppContext.cartogramWizard.getCartogramGridSizeInY();
		int cgGridSize = Math.max(cgGridSizeX, cgGridSizeY);
		String cgGridSizeString = "" + cgGridSize;
		mCartogramGridSizeTextField = new JTextField(cgGridSizeString);
		mCartogramGridSizeTextField.setLocation(240, 270);
		mCartogramGridSizeTextField.setSize(50, 26);
		mCartogramGridSizeTextField.setFont(new Font(null, Font.PLAIN, 11));
		mCartogramGridSizeTextField.setHorizontalAlignment(JTextField.RIGHT);
		mCartogramGridSizeTextField.setEnabled(
			mAdvancedOptionsCheckBox.isSelected());
		this.add(mCartogramGridSizeTextField);
		
		
		
		
		// Grid 2 text
		mGrid2Pane = new JTextPane();
		String grid2Text = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("Grid2Text.html").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			grid2Text = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		mGrid2Pane.setContentType("text/html");
		mGrid2Pane.setText(grid2Text);
		mGrid2Pane.setEditable(false);
		mGrid2Pane.addHyperlinkListener(this);
		mGrid2Pane.setBackground(null);
		mGrid2Pane.setLocation(45, 315);
		mGrid2Pane.setSize(400, 50);
		mGrid2Pane.setEnabled(mAdvancedOptionsCheckBox.isSelected());
		this.add(mGrid2Pane);
		
		
		
		
		// Diffusion grid size
		mDiffusionGridSizeLabel = new JLabel("Diffusion grid size:");
		mDiffusionGridSizeLabel.setLocation(45, 365);
		mDiffusionGridSizeLabel.setSize(170, 26);
		mDiffusionGridSizeLabel.setFont(new Font(null, Font.PLAIN, 11));
		mDiffusionGridSizeLabel.setEnabled(
			mAdvancedOptionsCheckBox.isSelected());
		this.add(mDiffusionGridSizeLabel);
		
		mDiffusionGridMenu = new JComboBox();
		mDiffusionGridMenu.setBounds(240, 365, 100, 26);
		mDiffusionGridMenu.setFont(new Font(null, Font.PLAIN, 11));
		mDiffusionGridMenu.setEnabled(mAdvancedOptionsCheckBox.isSelected());
		mDiffusionGridMenu.addItem("64");
		mDiffusionGridMenu.addItem("128");
		mDiffusionGridMenu.addItem("256");
		mDiffusionGridMenu.addItem("512");
		mDiffusionGridMenu.addItem("1024");
		
		String strGridSize = 
			"" + AppContext.cartogramWizard.getDiffusionGridSize();
			
		mDiffusionGridMenu.setSelectedItem(strGridSize);
		
		this.add(mDiffusionGridMenu);
		
		
		
		
		// Iterations text
		mIterPane = new JTextPane();
		String iterText = null;
		try
		{
			InputStream inStream = 
				cldr.getResource("GastnerIterationsText.html").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			iterText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		mIterPane.setContentType("text/html");
		mIterPane.setText(iterText);
		mIterPane.setEditable(false);
		mIterPane.addHyperlinkListener(this);
		mIterPane.setBackground(null);
		mIterPane.setLocation(45, 405);
		mIterPane.setSize(400, 45);
		mIterPane.setEnabled(mAdvancedOptionsCheckBox.isSelected());
		this.add(mIterPane);
		
		
		
		
		
		
		// Iterations of diffusion algorithm
		mIterationsLabel = 
			new JLabel("Enter the number of iterations:");
			
		mIterationsLabel.setLocation(45, 450);
		mIterationsLabel.setSize(190, 26);
		mIterationsLabel.setFont(new Font(null, Font.PLAIN, 11));
		mIterationsLabel.setEnabled(mAdvancedOptionsCheckBox.isSelected());
		this.add(mIterationsLabel);

		mDiffusionIterationsTextField = new JTextField(
			"" + AppContext.cartogramWizard.getDiffusionIterations());
		mDiffusionIterationsTextField.setLocation(240, 450);
		mDiffusionIterationsTextField.setSize(50, 26);
		mDiffusionIterationsTextField.setFont(new Font(null, Font.PLAIN, 11));
		mDiffusionIterationsTextField.setHorizontalAlignment(JTextField.RIGHT);
		mDiffusionIterationsTextField.setEnabled(
			mAdvancedOptionsCheckBox.isSelected());
		this.add(mDiffusionIterationsTextField);
		
		
		
		
		
		
		// Cancel button
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setLocation(270, 510);
		cancelButton.setSize(100, 26);
		
		cancelButton.addActionListener(new 
			CartogramWizardAdvancedOptionsAction(
			"closeDialogWithoutSaving", this));
			
		this.add(cancelButton);
		
		
		// Ok button
		JButton okButton = new JButton("OK");
		okButton.setLocation(380, 510);
		okButton.setSize(100, 26);
		
		okButton.addActionListener(new 
			CartogramWizardAdvancedOptionsAction(
			"closeDialogWithSaving", this));
			
		this.add(okButton);
		



		// ADD THE HELP BUTTON
				
		java.net.URL imageURL = cldr.getResource("help-22.png");
		ImageIcon helpIcon = new ImageIcon(imageURL);

		JButton helpButton = 
			new JButton(helpIcon);
		
		helpButton.setVerticalTextPosition(AbstractButton.BOTTOM);
		helpButton.setHorizontalTextPosition(AbstractButton.CENTER);
		helpButton.setSize(30, 30);
		helpButton.setLocation(20, 510);
		helpButton.setFocusable(false);
		helpButton.setContentAreaFilled(false);
		helpButton.setBorderPainted(false);
		
		helpButton.addActionListener(new CartogramWizardShowURL(
			"http://chorogram.choros.ch/scapetoad/help/c-transformation-parameters.php#advanced-options"));
		
		this.add(helpButton);



	
	}	// CartogramWizardOptionsWindow.<init>





	/**
	 * Saves the changes done by the user.
	 */
	public void saveChanges ()
	{
	
		AppContext.cartogramWizard.setAdvancedOptionsEnabled(
			mAdvancedOptionsCheckBox.isSelected());
	
		AppContext.cartogramWizard.setCreateGridLayer(
			mGridLayerCheckBox.isSelected());
		
		try
		{
			String gridSizeString = mGridSizeTextField.getText();
			Integer gridSizeInt = new Integer(gridSizeString);
			AppContext.cartogramWizard.setDeformationGridSize(
				gridSizeInt.intValue());
		}
		catch (NumberFormatException e1)
		{
		}
		
		
		
		try
		{
			String gridSizeString = mCartogramGridSizeTextField.getText();
			Integer gridSizeInt = new Integer(gridSizeString);
			AppContext.cartogramWizard.setCartogramGridSizeInX(
				gridSizeInt.intValue());
			AppContext.cartogramWizard.setCartogramGridSizeInY(
				gridSizeInt.intValue());
		}
		catch (NumberFormatException e2)
		{
		}
		
		
		
		
		try
		{
			String diffusionGridSizeString = 
				(String)mDiffusionGridMenu.getSelectedItem();
			
			Integer diffusionGridSizeInt = new Integer(diffusionGridSizeString);
			
			AppContext.cartogramWizard.setDiffusionGridSize(
				diffusionGridSizeInt);
		}
		catch (NumberFormatException e3)
		{
		}
		
		
		
		
		try
		{
			String iterationsString = mDiffusionIterationsTextField.getText();
			Integer iterationsInt = new Integer(iterationsString);
			AppContext.cartogramWizard.setDiffusionIteratations(
				iterationsInt.intValue());
		}
		catch (NumberFormatException e4)
		{
		}
		
		
		
	
	}	// CartogramWizardOptionsWindow.saveChanges





	public void hyperlinkUpdate (HyperlinkEvent e)
	{
		try
		{
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
                Browser.init();
				Browser.displayURL(e.getURL().toString());
             }
            
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
	}	// CartogramWizardOptionsWindow.hyperlinkUpdate
	 
	
	
	
	/**
	 * This method gets called on a state change of the advanced options
	 * check box. It enables or disables the advanced options.
	 */
	public void stateChanged (ChangeEvent e)
	{
		boolean enabled = mAdvancedOptionsCheckBox.isSelected();
		mManualParametersPane.setEnabled(enabled);
		mGrid1Pane.setEnabled(enabled);
		mCartogramGridSizeLabel.setEnabled(enabled);
		mCartogramGridSizeTextField.setEnabled(enabled);
		mGrid2Pane.setEnabled(enabled);
		mDiffusionGridSizeLabel.setEnabled(enabled);
		mDiffusionGridMenu.setEnabled(enabled);
		mIterPane.setEnabled(enabled);
		mIterationsLabel.setEnabled(enabled);
		mDiffusionIterationsTextField.setEnabled(enabled);
	
	}	// CartogramWizardOptionsWindow


}	// CartogramWizardOptionsWindow





/**
 * Creates the dialog for the advanced options.
 */
class CartogramWizardAdvancedOptionsAction extends AbstractAction
{


	String mActionToPerform = "showDialog";
	CartogramWizardOptionsWindow mDialog = null;
	
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
	CartogramWizardAdvancedOptionsAction (
		String actionToPerform, CartogramWizardOptionsWindow dialog)
	{
		mActionToPerform = actionToPerform;
		mDialog = dialog;
	
	}	// CartogramWizardShowAdvancedOptionsAction.<init>
	
	
	
	/**
	 * Method which performs this action; it creates and opens
	 * the Advanced Options dialog.
	 */
	public void actionPerformed (ActionEvent e)
	{
	
		if (mActionToPerform == "showDialog")
		{
			mDialog = new CartogramWizardOptionsWindow();
			mDialog.setVisible(true);
		}
		
		else if (mActionToPerform == "closeDialogWithoutSaving")
		{
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
		else if (mActionToPerform == "closeDialogWithSaving")
		{
			mDialog.saveChanges();
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
	
	}	// CartogramWizardShowAdvancedOptionsAction.actionPerformed
	


}	// CartogramWizardShowAdvancedOptionsAction







/**
 * Opens the provided URL.
 */
class CartogramWizardShowURL extends AbstractAction
{


	String mUrl = null;
	
	
	/**
	 * The default creator for the action.
	 * @param url the URL to show.
	 */
	CartogramWizardShowURL (String url)
	{
		mUrl = url;
	
	}	// CartogramWizardShowURL.<init>
	
	
	
	/**
	 * Method which performs this action.
	 */
	public void actionPerformed (ActionEvent e)
	{
	
		try
		{
			Browser.init();
			Browser.displayURL(mUrl);
		}
		catch (IOException exc)
		{
			exc.printStackTrace();
		}
	
	
	}	// CartogramWizardShowURL.actionPerformed
	


}	// CartogramWizardShowURL






/**
 * Handles the window events of the wizard window.
 * It handles the windowActivated event and the windowClosed event.
 */
class CartogramWizardWindowListener implements WindowListener
{


	public void windowActivated (WindowEvent e)
	{
	}
	
	
	
	/**
	 * Method invoked in response to a window close event.
	 */
	public void windowClosed (WindowEvent e)
	{
	}	// CartogramWizardWindowListener.windowClosed
	
	
	
	/**
	 * Method invoked in response to a window closing event.
	 * It creates a CartogramWizardCloseAction which is automatically
	 * performed.
	 */
	public void windowClosing (WindowEvent e)
	{
		
		ActionEvent closeEvent = 
			new ActionEvent(e.getSource(), e.getID(), "windowClosed");
		
		CartogramWizardCloseAction closeAction =
			new CartogramWizardCloseAction();
		
		closeAction.actionPerformed(closeEvent);
		
	}	// CartogramWizardWindowListener.windowClosed
	
	
	
	
	
	public void windowDeactivated (WindowEvent e)
	{
	}
	
	
	
	
	public void windowDeiconified (WindowEvent e)
	{
	}
	
	
	
	
	public void windowIconified (WindowEvent e)
	{
	}
	
	
	
	
	public void windowOpened (WindowEvent e)
	{
	}



}	// CartogramWizardWindowListener











/**
 * Dialog window for specifying the simultaneous deformation layers.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-02-01
 */
class CartogramWizardSimulaneousLayerWindow extends JDialog
{


	/**
	 * An inline panel contained in the scroll view, containing the 
	 * layer check boxes.
	 */
	JPanel mLayerListPanel = null;
	
	/**
	 * The scroll pane containing the layer list panel.
	 */
	JScrollPane mLayerListScrollPane = null;

	/**
	 * The list with all the check boxes for the simultaneous layers.
	 */
	Vector mCheckBoxList = null;
	
	/**
	 * The currently selected cartogram layer.
	 */
	String mCurrentCartogramLayer = null;
	
	/**
	 * Label displayed if no layer is present to be selected.
	 */
	JLabel mNoLayerLabel = null;




	/**
	 * Constructor for the simultaneous layer window.
	 */
	CartogramWizardSimulaneousLayerWindow ()
	{
		
		// Set the window parameters.
		this.setTitle("Simultaneous transformation layers");
			
		this.setSize(300, 400);
		this.setLocation(40, 50);
		this.setResizable(false);
		this.setLayout(null);
		this.setModal(true);
		
		

		// LIST WITH SIMULTANEOUS LAYERS
		
		// Create a new pane containing the check boxes with
		// the layers.
		mLayerListPanel = new JPanel(new GridLayout(0,1));
		
		// Create the check boxes for all layers except the selected
		// cartogram layer.
		mCurrentCartogramLayer = 
			AppContext.cartogramWizard.getCartogramLayerName();
		
		// Create the checkbox array.
		mCheckBoxList = new Vector();
		
		Font smallFont = new Font(null, Font.PLAIN, 11);
		
		// Create the check boxes.
		Vector simLayers = AppContext.cartogramWizard.getSimultaneousLayers();
		int nlayers = AppContext.layerManager.size();
		if (nlayers > 1)
		{
			int layersInList = 0;
			int lyrcnt = 0;
			for (lyrcnt = 0; lyrcnt < nlayers; lyrcnt++)
			{
				Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
				if (lyr.getName() != mCurrentCartogramLayer)
				{
					JCheckBox checkbox = new JCheckBox(lyr.getName());
					checkbox.setFont(smallFont);
					
					// Find if this layer is already selected as a 
					// simultaneous layer.
					if (simLayers != null && simLayers.contains(lyr))
						checkbox.setSelected(true);
					else
						checkbox.setSelected(false);
					
					mCheckBoxList.add(checkbox);
					mLayerListPanel.add(checkbox);
					layersInList++;
				}
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
			
		}
		
		
		// Label for the layers to deform.
		JLabel layerListLabel = 
			new JLabel("Select layers to deform simultaneously:");
		layerListLabel.setSize(260, 14);
		layerListLabel.setFont(smallFont);
		layerListLabel.setLocation(20, 20);
		this.add(layerListLabel);
		
		
		// Label for no present layers.
		if (nlayers <= 1)
		{
			mNoLayerLabel = new JLabel("No other layer to be deformed.");
			mNoLayerLabel.setSize(260, 14);
			mNoLayerLabel.setFont(smallFont);
			mNoLayerLabel.setLocation(20, 50);
			this.add(mNoLayerLabel);
		}
		
		
		
		
		// Cancel button
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setLocation(70, 330);
		cancelButton.setSize(100, 26);
		
		cancelButton.addActionListener(new 
			CartogramWizardSimulaneousLayerAction(
			"closeDialogWithoutSaving", this));
			
		this.add(cancelButton);
		
		
		// Ok button
		JButton okButton = new JButton("OK");
		okButton.setLocation(180, 330);
		okButton.setSize(100, 26);
		
		okButton.addActionListener(new 
			CartogramWizardSimulaneousLayerAction(
			"closeDialogWithSaving", this));
			
		this.add(okButton);

	
	}	// CartogramWizardSimulaneousLayerWindow.<init>





	/**
	 * Saves the changes done by the user.
	 */
	public void saveChanges ()
	{
	
		int nlayers = mCheckBoxList.size();
		Vector layers = new Vector();
		
		for (int i = 0; i < nlayers; i++)
		{
			JCheckBox checkBox = (JCheckBox)mCheckBoxList.get(i);
			if (checkBox.isSelected())
			{
				String layerName = checkBox.getText();
				Layer lyr = AppContext.layerManager.getLayer(layerName);
				layers.add(lyr);
			}
		}
	
		AppContext.cartogramWizard.setSimultaneousLayers(layers);
	
	}	// CartogramWizardSimulaneousLayerWindow.saveChanges



}	// CartogramWizardSimulaneousLayerWindow







/**
 * The actions for the simultaneous layer dialog.
 */
class CartogramWizardSimulaneousLayerAction extends AbstractAction
{


	String mActionToPerform = "showDialog";
	CartogramWizardSimulaneousLayerWindow mDialog = null;
	
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
	CartogramWizardSimulaneousLayerAction (
		String actionToPerform, CartogramWizardSimulaneousLayerWindow dialog)
	{
		mActionToPerform = actionToPerform;
		mDialog = dialog;
	
	}	// CartogramWizardSimulaneousLayerAction.<init>
	
	
	
	/**
	 * Method which performs the previously specified action.
	 */
	public void actionPerformed (ActionEvent e)
	{
	
		if (mActionToPerform == "showDialog")
		{
			mDialog = new CartogramWizardSimulaneousLayerWindow();
			mDialog.setVisible(true);
		}
		
		else if (mActionToPerform == "closeDialogWithoutSaving")
		{
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
		else if (mActionToPerform == "closeDialogWithSaving")
		{
			mDialog.saveChanges();
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
	
	}	// CartogramWizardSimulaneousLayerAction.actionPerformed
	


}	// CartogramWizardSimulaneousLayerAction














/**
 * Dialog window for specifying the constrained transformation layers.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2007-02-01
 */
class CartogramWizardConstrainedLayerWindow extends JDialog
{


	/**
	 * An inline panel contained in the scroll view, containing the 
	 * layer check boxes.
	 */
	JPanel mLayerListPanel = null;
	
	/**
	 * The scroll pane containing the layer list panel.
	 */
	JScrollPane mLayerListScrollPane = null;

	/**
	 * The list with all the check boxes for the constrained layers.
	 */
	Vector mCheckBoxList = null;
	
	/**
	 * The currently selected cartogram layer.
	 */
	String mCurrentCartogramLayer = null;
	
	/**
	 * Label displayed if no layer is present to be selected.
	 */
	JLabel mNoLayerLabel = null;




	/**
	 * Constructor for the constrained layer window.
	 */
	CartogramWizardConstrainedLayerWindow ()
	{
		
		// Set the window parameters.
		this.setTitle(AppContext.shortProgramName + 
			" _ Cartogram Wizard _ Constrained transformation layers");
			
		this.setSize(300, 400);
		this.setLocation(40, 50);
		this.setResizable(false);
		this.setLayout(null);
		this.setModal(true);
		
		

		// LIST WITH CONSTRAINED LAYERS
		
		// Create a new pane containing the check boxes with
		// the layers.
		mLayerListPanel = new JPanel(new GridLayout(0,1));
		
		// Create the check boxes for all layers except the selected
		// cartogram layer.
		mCurrentCartogramLayer = 
			AppContext.cartogramWizard.getCartogramLayerName();
		
		// Create the checkbox array.
		mCheckBoxList = new Vector();
		
		Font smallFont = new Font(null, Font.PLAIN, 11);
		
		// Create the check boxes.
		Vector constrLayers = 
			AppContext.cartogramWizard.getConstrainedDeformationLayers();
		int nlayers = AppContext.layerManager.size();
		if (nlayers > 1)
		{
			int layersInList = 0;
			int lyrcnt = 0;
			for (lyrcnt = 0; lyrcnt < nlayers; lyrcnt++)
			{
				Layer lyr = AppContext.layerManager.getLayer(lyrcnt);
				if (lyr.getName() != mCurrentCartogramLayer)
				{
					JCheckBox checkbox = new JCheckBox(lyr.getName());
					checkbox.setFont(smallFont);
					
					// Find if this layer is already selected as a 
					// constrained layer.
					if (constrLayers != null && constrLayers.contains(lyr))
						checkbox.setSelected(true);
					else
						checkbox.setSelected(false);

					mCheckBoxList.add(checkbox);
					mLayerListPanel.add(checkbox);
					layersInList++;
				}
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
			
		}
		
		
		// Label for the layers to deform.
		JLabel layerListLabel = 
			new JLabel("Select layers with limited deformation:");
		layerListLabel.setSize(260, 14);
		layerListLabel.setFont(smallFont);
		layerListLabel.setLocation(20, 20);
		this.add(layerListLabel);
		
		
		// Label for no present layers.
		if (nlayers <= 1)
		{
			mNoLayerLabel = 
				new JLabel("No layer available for limited deformation.");
			mNoLayerLabel.setSize(260, 14);
			mNoLayerLabel.setFont(smallFont);
			mNoLayerLabel.setLocation(20, 50);
			this.add(mNoLayerLabel);
		}
		
		
		
		
		// Cancel button
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setLocation(70, 330);
		cancelButton.setSize(100, 26);
		
		cancelButton.addActionListener(new 
			CartogramWizardConstrainedLayerAction(
			"closeDialogWithoutSaving", this));
			
		this.add(cancelButton);
		
		
		// Ok button
		JButton okButton = new JButton("OK");
		okButton.setLocation(180, 330);
		okButton.setSize(100, 26);
		
		okButton.addActionListener(new 
			CartogramWizardConstrainedLayerAction(
			"closeDialogWithSaving", this));
			
		this.add(okButton);

	
	}	// CartogramWizardConstrainedLayerWindow.<init>





	/**
	 * Saves the changes done by the user.
	 */
	public void saveChanges ()
	{
	
		int nlayers = mCheckBoxList.size();
		Vector layers = new Vector();
		
		for (int i = 0; i < nlayers; i++)
		{
			JCheckBox checkBox = (JCheckBox)mCheckBoxList.get(i);
			if (checkBox.isSelected())
			{
				String layerName = checkBox.getText();
				Layer lyr = AppContext.layerManager.getLayer(layerName);
				layers.add(lyr);
			}
		}
	
		AppContext.cartogramWizard.setConstrainedDeformationLayers(layers);
	
	
	}	// CartogramWizardConstrainedLayerWindow.saveChanges



}	// CartogramWizardConstrainedLayerWindow







/**
 * The actions for the constrained layer dialog.
 */
class CartogramWizardConstrainedLayerAction extends AbstractAction
{


	String mActionToPerform = "showDialog";
	CartogramWizardConstrainedLayerWindow mDialog = null;
	
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
	CartogramWizardConstrainedLayerAction (
		String actionToPerform, CartogramWizardConstrainedLayerWindow dialog)
	{
		mActionToPerform = actionToPerform;
		mDialog = dialog;
	
	}	// CartogramWizardConstrainedLayerAction.<init>
	
	
	
	/**
	 * Method which performs the previously specified action.
	 */
	public void actionPerformed (ActionEvent e)
	{
	
		if (mActionToPerform == "showDialog")
		{
			mDialog = new CartogramWizardConstrainedLayerWindow();
			mDialog.setVisible(true);
		}
		
		else if (mActionToPerform == "closeDialogWithoutSaving")
		{
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
		else if (mActionToPerform == "closeDialogWithSaving")
		{
			mDialog.saveChanges();
			mDialog.setVisible(false);
			mDialog.dispose();
		}
		
	
	}	// CartogramWizardConstrainedLayerAction.actionPerformed
	


}	// CartogramWizardConstrainedLayerAction









/**
 * This actions saves the computation report.
 */
class CartogramWizardSaveReportAction extends AbstractAction
{

	
	/**
	 * Shows a save dialog and writes the computation report to the
	 * specified file.
	 */
	public void actionPerformed (ActionEvent e)
	{
	
		// Create the File Save dialog.
		FileDialog fd = new FileDialog(
			(Frame)AppContext.cartogramWizard, 
			"Save Computation Report As...", 
			FileDialog.SAVE);

		fd.setModal(true);
		fd.setBounds(20, 30, 150, 200);
		fd.setVisible(true);
		
		// Get the selected File name.
		if (fd.getFile() == null)
			return;
		
		String path = fd.getDirectory() + fd.getFile();
		if (path.endsWith(".txt") == false)
			path = path + ".txt";
		
		
		// Write the report to the file.
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			out.write(AppContext.cartogramWizard.getCartogram().getComputationReport());
			out.close();
		} 
		catch (IOException exc)
		{
		}

		
	
	}	// CartogramWizardSaveReportAction.actionPerformed
	



}	// CartogramWizardSaveReportAction
