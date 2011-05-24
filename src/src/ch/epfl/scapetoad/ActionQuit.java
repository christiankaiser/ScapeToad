/*

	Copyright 2007-2009 361DEGRES

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
import javax.swing.AbstractAction;




/**
 * This class is an action performed on a quit event.
 * @author christian@swisscarto.ch
 */
public class ActionQuit extends AbstractAction
{
	
	
	/**
	 * Terminates ScapeToad.
	 */
	public void actionPerformed(ActionEvent e)
	{
		
		if (AppContext.cartogramWizard != null)
			AppContext.cartogramWizard.dispose();
		
		AppContext.mainWindow.dispose();
		
		System.exit(0);
		
	}
	

}
