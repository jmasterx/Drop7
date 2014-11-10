//Class Name: Tile.java
//Purpose: Represents a Tile
//Created by Josh on 2012-09-04
package com.joshl.drop7;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView;

public class Tile extends PositionComponent
{
	private int value;
	private Bitmap image;
	private int state;
	
	public static final int STATE_GRAY = 0;
	public static final int STATE_GRINDED = 0;
	public static final int STATE_NUMBER = 2;

	public Tile(int value, int state, RectF rect, Bitmap image) 
	{
		setValue(value);
		setState(state);
		setRect(rect);
		setImage(image);
	}
	
	public Tile()
	{
		this(0,STATE_NUMBER,new RectF(0,0,0,0),null);
	}
	
	public void setValue(int val)
	{
		//clamp 0 to 7
		if(val < 0)
		{
			val = 0;
		}
		
		if(val > 7)
		{
			val = 7;
		}
		
		value = val;
	}
	
	public int getValue()
	{
		return value;
	}
	
	public void setState(int state)
	{
		this.state = state;
	}
	
	public int getState()
	{
		return state;
	}
	
	public Bitmap getImage() 
	{
		return image;
	}
	
	public void setImage(Bitmap image) 
	{
		this.image = image;
	}
	
	
	public void draw(Canvas canvas)
	{
		if(image != null)
		canvas.drawBitmap (image, null, getRect(), null);
	}
	
	public boolean isEmpty()
	{
		return getValue() == 0;
	}
}
