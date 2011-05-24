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


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.NoninvertibleTransformException;

import java.util.Vector;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.renderer.style.BasicStyle;
import com.vividsolutions.jump.workbench.ui.renderer.style.Style;
import com.vividsolutions.jump.workbench.ui.renderer.style.StyleUtil;





/**
 * The SizeErrorStyle produces the thematic map based on the SizeError
 * attribute.
 */
public class SizeErrorStyle implements Style
{

	boolean _enabled = false;
	
	String _attrName;
	Vector _limits;
	Vector _colors;
	BasicStyle _defaultStyle;
	
	private Stroke _fillStroke = new BasicStroke(1);
	
	
	public SizeErrorStyle ()
	{
		_limits = new Vector();
		_colors = new Vector();
		_defaultStyle = new BasicStyle(Color.ORANGE);
	}



	public Object clone()
	{
		return null;
	}



	public void initialize (Layer layer)
	{
	}



	public boolean isEnabled ()
	{
		return _enabled;
	}



	public void paint (Feature f, Graphics2D g, Viewport viewport)
		throws NoninvertibleTransformException
	{
	
		BasicStyle s = this.getStyleForFeature(f);
	
        StyleUtil.paint(
			f.getGeometry(), 
			g, 
			viewport, 
			s.isRenderingFill(),
            _fillStroke,
            s.getFillColor(),
            s.isRenderingLine(), 
			s.getLineStroke(), 
			s.getLineColor());
			
	}



	public void setEnabled (boolean enabled)
	{
		_enabled = enabled;
	}




	public void setDefaultStyle (BasicStyle defaultStyle)
	{
		_defaultStyle = defaultStyle;
	}
	
	
	
	public int getNumberOfColors ()
	{
		return _colors.size();
	}
	
	
	public BasicStyle getColorAtIndex (int index)
	{
		return (BasicStyle)_colors.get(index);
	}
	
	
	
	public void addColor (BasicStyle color)
	{
		_colors.add(color);
	}
	
	
	public void setColorAtIndex (BasicStyle color, int index)
	{
		_colors.set(index, color);
	}
	
	
	
	public int getNumberOfLimits ()
	{
		return _limits.size();
	}
	
	
	
	public Double getLimitAtIndex (int index)
	{
		return (Double)_limits.get(index);
	}
	
	
	
	public void addLimit (Double limit)
	{
		_limits.add(limit);
	}
	
	
	
	public void setLimitAtIndex (Double limit, int index)
	{
		_limits.set(index, limit);
	}
	
	
	
	public void setAttributeName (String attrName)
	{
		_attrName = attrName;
	}
	
	
	
	
	private BasicStyle getStyleForFeature (Feature f)
	{
		
		// Get the attribute value.
		Double value = (Double)f.getAttribute(_attrName);
		
		boolean valueFound = false;
		int limitIndex = 0;
		BasicStyle s = null;
		while (valueFound == false && limitIndex < _limits.size())
		{
			Double limit = (Double)_limits.get(limitIndex);
			if (value.doubleValue() <= limit.doubleValue())
				valueFound = true;
			
			limitIndex++;
		}
		
		return (BasicStyle)_colors.get(limitIndex);
		
	}



}

