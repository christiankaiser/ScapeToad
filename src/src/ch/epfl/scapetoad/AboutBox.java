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



import java.io.InputStream;

import javax.swing.JDialog;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.Ostermiller.util.Browser;



/**
 * The about box for ScapeToad
 * @author Christian Kaiser <christian@361degres.ch>
 * @version v1.0.0, 2009-05-21
 */
public class AboutBox extends JDialog
	implements HyperlinkListener
{
	/**
	 * The constructor for the about box window.
	 */
	AboutBox ()
	{
		
		// Set the window parameters.
		this.setTitle("About ScapeToad");
		
		this.setSize(500, 400);
		this.setLocation(40, 50);
		this.setResizable(false);
		this.setLayout(null);
		this.setModal(true);
		
		
				
		// About box content
		ClassLoader cldr = this.getClass().getClassLoader();
		JTextPane aboutPane = new JTextPane();
		String aboutText = null;
		try
		{
			InputStream inStream = cldr.getResource("AboutText.html").openStream();
			StringBuffer inBuffer = new StringBuffer();
			int c;
			while ((c = inStream.read()) != -1)
			{
				inBuffer.append((char)c);
			}
			inStream.close();
			aboutText = inBuffer.toString();
		} 
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		aboutPane.setContentType("text/html");
		aboutPane.setText(aboutText);
		aboutPane.setEditable(false);
		aboutPane.addHyperlinkListener(this);
		aboutPane.setBackground(null);
		aboutPane.setLocation(50, 50);
		aboutPane.setSize(400, 300);
		this.add(aboutPane);
		
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




