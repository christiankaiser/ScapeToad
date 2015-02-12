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

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;

import com.vividsolutions.jump.workbench.model.Category;
import com.vividsolutions.jump.workbench.model.Layer;




/**
 * This class is an action performed on a remove layer event.
 * @author Christian.Kaiser@91nord.com
 */
public class ActionLayerRemove extends AbstractAction
{
	
	/**
	 * Removes the selected layer from the layer manager.
	 */
	public void actionPerformed (ActionEvent e)
	{
		// Get the selected layers.
		Layer[] lyrs = AppContext.layerListPanel.getSelectedLayers();
		
		if (lyrs.length > 0)
		{
			for (int i = 0; i < lyrs.length; i++)
			{
				AppContext.layerManager.remove(lyrs[i]);
			}
		}
		
		
		// Remove all empty categories (but we keep at least one).
		List cats = AppContext.layerManager.getCategories();
		if (cats.size() > 1)
		{
			Iterator catIter = cats.iterator();
			while (catIter.hasNext())
			{
				Category cat = (Category)catIter.next();
				AppContext.layerManager.removeIfEmpty(cat);
			}
		}
		
		// If there is only one empty category left, rename it to
		// "Original layers".
		if (cats.size() == 1)
		{
			Category cat = (Category)cats.get(0);
			if (cat.isEmpty())
			{
				cat.setName("Original layers");
			}
		}
		
		AppContext.mainWindow.update();
		
	}	// ActionLayerRemove.actionPerformed
	

}
