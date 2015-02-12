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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JFrame;

import com.Ostermiller.util.Browser;




/**
 * The size error legend window.
 * @author Christian.Kaiser@91nord.com
 * @version v1.0.0, 2008-04-30
 */
public class SizeErrorLegend extends JFrame
{
	
	
	public SizeErrorLegend ()
	{
	
		this.setTitle("Size Error");
		this.setBounds(10, 30, 120, 220);
		this.setVisible(false);
		
		
		// Loading the size error legend image from the resources.
		ClassLoader cldr = this.getClass().getClassLoader();
		URL iconURL = cldr.getResource("SizeErrorLegend.png");
		ImageIcon sizeErrorImage = new ImageIcon(iconURL);


		// Create a new label containing the icon.
		JLabel iconLabel = new JLabel(sizeErrorImage);
		
		// Setting the label parameters.
		iconLabel.setLayout(null);
		iconLabel.setSize(98, 198);
		iconLabel.setLocation(1, 1);
		iconLabel.addMouseListener(new IconMouseListener());
		
		// Add the icon label to this panel.
		this.add(iconLabel);
	}
	
}



class IconMouseListener implements MouseListener
{

	public void mouseClicked (MouseEvent e)
	{
		try
		{
			Browser.init();
			Browser.displayURL("http://chorogram.choros.ch/scapetoad/help/d-computation-report.php#cartogram-error");
		}
		catch (Exception exc)
		{
		}
	}
	
	
	public void mouseEntered (MouseEvent e)
	{
	}
	
	
	public void mouseExited (MouseEvent e)
	{
	}
	
	
	public void mousePressed (MouseEvent e)
	{
	}
	
	
	public void mouseReleased (MouseEvent e)
	{
	}
	
}





