//Class Name: PuzzleView.java
//Purpose: Main class for puzzle
//Created by Josh on 2012-09-04
package com.joshl.drop7;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PuzzleView extends SurfaceView implements Runnable, AnimationListener
{
	private Game game;
	private int gameDifficulty;
	private Tile tiles[][];
	private Tile topTile;
	private Bitmap tileImages[];
	private Paint backgroundPaint = new Paint();
	private Paint foregroundPaint = new Paint();
	private Paint puzzleLinePaint = new Paint();
	private Paint circleDarkPaint = new Paint();
	private Paint circleLitePaint = new Paint();
	private int currentLevel;
	private int numbersToPlace;
	private int score;
	private Paint scorePaint = new Paint();
	private Paint levelPaint = new Paint();
	private AnimationManager animationManager = null;
	private boolean sizeHasChanged = false;
	
	// constants for the animation
	private static final float	FRAME_RATE	= 1.0f / 60.0f; // NTSC 60 FPS
	private Thread	thread = null;	// thread that will run the animation
	private volatile boolean running; // = true if animation is running
	private SurfaceHolder holder;
	private boolean isDropping = false;

	public static final int X_AXIS_TILES = 7;
	public static final int Y_AXIS_TILES = 7;
	public static final int MAX_TILE_VALUE = 7;
	
	public static final float TOP_OFFSET = 0.25f;
	public static final float LEFT_OFFSET = 0.06f;
	public static final float RIGHT_OFFSET = 0.05f;
	public static final float CIRCLE_BOTTOM_OFFSET = 0.02f;
	public static final float CIRCLE_PADDING = 0.008f;
	public static final float SCORE_FONT_SIZE = 0.1f;
	public static final float LEVEL_FONT_SIZE = 0.04f;
	public static final float SCORE_OFFSET_X = 0.05f;
	public static final float LEVEL_OFFSET_Y = 0.04f;
	public static final float DROP_RATE = 10.0f;
	
	//animations enum
	public static final int ANIM_DROP_TILE = 0;
	
	public static final int MAX_LEVELS = 30;
	
	public PuzzleView(Context context) 
	{
		super(context);
		game = (Game)context;
		// in order to render to a SurfaceView from a different thread than the UI thread,
		// we need to acquire an instance of the SurfaceHolder class.
		holder = getHolder ();
		setFocusable(true);
		setFocusableInTouchMode(true);
		gameDifficulty = game.getIntent().getIntExtra("game_difficulty", 0);
		backgroundPaint.setColor(getResources().getColor(R.color.main_game_background));
		foregroundPaint.setColor(getResources().getColor(R.color.main_game_foreground));
		puzzleLinePaint.setColor(getResources().getColor(R.color.puzzle_line));
		circleLitePaint.setColor(getResources().getColor(R.color.circle_lite));
		circleDarkPaint.setColor(getResources().getColor(R.color.circle_dark));
		scorePaint.setColor(getResources().getColor(R.color.score_color));
		levelPaint.setColor(getResources().getColor(R.color.level_color));
		running = false;
		animationManager = new AnimationManager();
		animationManager.addAnimationListener(this);
	}
	
	public void addAnimationEvent(AnimationEvent animation)
	{
		animationManager.add(animation);
	}
	
	public float getTileDimension()
	{
		return getContentWidth() / X_AXIS_TILES;
	}
	
	private void initializeImages()
	{
		Resources res = game.getResources();
		tileImages = new Bitmap[10];
		tileImages[1] = BitmapFactory.decodeResource(res, R.drawable.number1);
		tileImages[2] = BitmapFactory.decodeResource(res, R.drawable.number2);
		tileImages[3] = BitmapFactory.decodeResource(res, R.drawable.number3);
		tileImages[4] = BitmapFactory.decodeResource(res, R.drawable.number4);
		tileImages[5] = BitmapFactory.decodeResource(res, R.drawable.number5);
		tileImages[6] = BitmapFactory.decodeResource(res, R.drawable.number6);
		tileImages[7] = BitmapFactory.decodeResource(res, R.drawable.number7);
		tileImages[8] = BitmapFactory.decodeResource(res, R.drawable.gray);
		//tileImages[9] = BitmapFactory.decodeResource(res, R.drawable.grinded);
	}
	
	private Tile createTile(int state, int value, RectF rect)
	{
		Tile tile = new Tile(value,state,rect,null);
	
		tile.setImage(tileImageFromValue(value, state));
		
		return tile;
	}
	
	private RectF createTileRect(int x, int y)
	{
		float tileDimension = getTileDimension();
		float l = x * tileDimension;
		float t = y * tileDimension;
		return new RectF(l,t,l + tileDimension,t + tileDimension);
	}
	
	private void collapseTiles()
	{
		//swap current tile with tile below until every (bottom -1)nth tile is contiguous.
		//Non-empty tiles must be contiguous (must have a tile beneath them).
		boolean swapped = false;
		do 
		{
			swapped = false;
			for(int i = 0; i < tiles.length; ++i)
			{
				//avoid checking lowest tiles
				for(int j = tiles[i].length - 2; j >= 0; j--)
				{
					if(!tiles[i][j].isEmpty() && tiles[i][j + 1].isEmpty())
					{
						//swap pointers and location in array
						Tile t = tiles[i][j];
						tiles[i][j] = tiles[i][j + 1];
						tiles[i][j + 1] = t;
						
						//swap positions
						RectF r = tiles[i][j].getRect();
						tiles[i][j].setRect(tiles[i][j + 1].getRect());
						tiles[i][j + 1].setRect(r);
						swapped = true;
						
						//TODO: Adapt for animation such that each row falls one after the other.
						break;
					}
				}
			}
		} while(swapped);
	}
	
	private void generateRandomTile(Tile t)
	{
		generateRandomTile(t,true);
	}
	
	private void generateRandomTile(Tile t, boolean canBeEmpty)
	{
		int state = Tile.STATE_NUMBER;
		int num = 0;
		//25% chance of anything
		boolean isTile =  JMath.randomRange(0, 3) == 0;
		if(isTile || !canBeEmpty) 
		{
			//2 chances of gray, 1 chance of number
			boolean isGray = JMath.randomRange(0, 2) > 0;
			
			if(isGray)
			{
				state = Tile.STATE_GRAY;
			}
			
			num = JMath.randomRange(1, MAX_TILE_VALUE);
		}
		else // it is empty
		{
			state = Tile.STATE_NUMBER;
			num = 0;
		}
		
		t.setImage(tileImageFromValue(num, state));
		t.setState(state);
		t.setValue(num);
	}
	
	private Bitmap tileImageFromValue(int val, int state)
	{
		if(state == Tile.STATE_GRAY)
		{
			return tileImages[8];
		}
		else if(state == Tile.STATE_GRINDED)
		{
			return tileImages[9];
		}
		else if(JMath.isInRange(val, 0, MAX_TILE_VALUE))
		{
			return tileImages[val];
		}
		
		return null;
	}
	
	private void initializeTiles()
	{
		//init tiles to nothing for now
		tiles = new Tile[X_AXIS_TILES][Y_AXIS_TILES];
		for(int i = 0; i < tiles.length; i++)
		{
			for(int j = 0; j < tiles[i].length; j++)
			{
				tiles[i][j] = createTile(Tile.STATE_NUMBER, 1, createTileRect(i, j));
			}
		}
		
		topTile = createTile(Tile.STATE_NUMBER, 1, createTileRect(X_AXIS_TILES / 2, -1));
	}
	
	private void generatePuzzle()
	{
		for(int i = 0; i < tiles.length; i++)
		{
			for(int j = 0; j < tiles[i].length; j++)
			{
				generateRandomTile(tiles[i][j]);
			}
		}
		
		//make a non-empty tile
		generateRandomTile(topTile,false);
		collapseTiles();
	}
	
	@Override
	protected void onDraw(Canvas canvas)
	{		
		//background
		canvas.drawRect(0,0,getWidth(),getHeight(), backgroundPaint);
	
		drawScore(canvas);
		
		//start drawing at top left of board
		canvas.translate(getLeftOffset(), getTopOffset());
		canvas.drawRect(0,0,getContentWidth(),getContentHeight(), foregroundPaint);
		
		drawLines(canvas);
		drawTiles(canvas);
		drawLevelCircles(canvas);
		drawLevel(canvas);
	}
	
	private void drawScore(Canvas canvas)
	{
		String score = String.valueOf(getScore());
		float xOffset = getWidth() - (getWidth() * SCORE_OFFSET_X);
		Paint.FontMetrics metrics = scorePaint.getFontMetrics();
		float totalHeight = metrics.bottom - metrics.top;
		
		canvas.drawText(score,xOffset,totalHeight,scorePaint);
	}
	
	private void drawLevel(Canvas canvas)
	{
		String level = getResources().getString(R.string.level_label) + " " +
				String.valueOf(getCurrentLevel());
		float yOffset = (getWidth() * LEVEL_OFFSET_Y) + getContentHeight();
		Paint.FontMetrics metrics = levelPaint.getFontMetrics();
		float totalHeight = metrics.bottom - metrics.top;
		
		canvas.drawText(level,0,totalHeight + yOffset,levelPaint);
	}
	
	private void drawTiles(Canvas canvas)
	{
		for(int i = 0; i < tiles.length; i++)
		{
			for(int j = 0; j < tiles[i].length; j++)
			{
				tiles[i][j].draw(canvas);
			}
		}
		
		topTile.draw(canvas);
	}
	
	private void drawLines(Canvas canvas)
	{
		float tileDimension = getTileDimension();
		for(int i = 0; i < X_AXIS_TILES; i++)
		{
			canvas.drawLine(tileDimension * i, 0, 
					tileDimension * i, getContentHeight(),puzzleLinePaint);
		}
		
		for(int i = 0; i < Y_AXIS_TILES; i++)
		{
			canvas.drawLine(0, tileDimension * i, 
					getContentWidth(), tileDimension * i,puzzleLinePaint);
		}
	}
	
	private void drawLevelCircles(Canvas canvas)
	{
		float maxWidth = getContentWidth() + (getRightOffset() / 2);
		float offsetY = getContentHeight() + ( CIRCLE_BOTTOM_OFFSET * getHeight() );
		float individualPadding = CIRCLE_PADDING * getWidth();
		float padding =  individualPadding * (MAX_LEVELS - 1);
		float radius = ((maxWidth - padding) / MAX_LEVELS) / 2.0f;
		float diameter = radius * 2;
		float curX = 0.0f;
		
		for(int i = 0; i < MAX_LEVELS; ++i)
		{
			//draw darker circles for numbers remaining
			Paint curPaint = i < getNumbersToPlace() ? circleLitePaint : circleDarkPaint;
			canvas.drawCircle(curX + (radius), offsetY + radius, radius, curPaint);
			curX += diameter + individualPadding;
		}
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
	   
		//tried on actual android device and onSizeChanged was called more than once
	   if(!sizeHasChanged)
	   {
		   sizeHasChanged = true;   
		   initializeImages();
		   initializeTiles();
		   generatePuzzle();
		   setCurrentLevel(1);
		   setScore(0);
		   
		   scorePaint.setTextSize(getWidth() * SCORE_FONT_SIZE);
		   scorePaint.setTextAlign(Align.RIGHT);
		   scorePaint.setAntiAlias(true);
		  
		   levelPaint.setTextSize(getWidth() * LEVEL_FONT_SIZE);
		   levelPaint.setAntiAlias(true);
	   }
	
	}
	
	public float getTopOffset()
	{
		return getHeight() * TOP_OFFSET;
	}
	
	public float getLeftOffset()
	{
		return getWidth() * LEFT_OFFSET;
	}
	
	public float getBottomOffset()
	{
		return getHeight() - getContentHeight() - getTopOffset();
	}
	
	public float getRightOffset()
	{
		return getWidth() * RIGHT_OFFSET;
	}
	
	public float getContentWidth()
	{
		return getWidth() - getLeftOffset() - getRightOffset();
	}
	
	public float getContentHeight()
	{
		return getTileDimension() * Y_AXIS_TILES;
	}

	public int getCurrentLevel()
	{
		return currentLevel;
	}

	public void setCurrentLevel(int currentLevel)
	{
		this.currentLevel = currentLevel;
		setNumbersToPlace(MAX_LEVELS - (currentLevel - 1));
	}

	public int getNumbersToPlace() 
	{
		return numbersToPlace;
	}

	public void setNumbersToPlace(int numbersToPlace)
	{
		if(numbersToPlace < 0)
		{
			numbersToPlace = 0;
		}
		
		this.numbersToPlace = numbersToPlace;
	}

	public int getScore() 
	{
		return score;
	}

	public void setScore(int score) 
	{
		this.score = score;
	}
	
	public void increaseScore(int score)
	{
		setScore(getScore() + score);
	}
	
	private void dropTileTo(Tile t)
	{
		t.setValue(topTile.getValue());
		t.setImage(topTile.getImage());
		t.setState(topTile.getState());
		t.setRect(new RectF(topTile.getRect()));
	}
	
	
	private void clearTopTile()
	{
		topTile.setValue(0);
		topTile.setImage(null);
		topTile.setState(Tile.STATE_NUMBER);
	}
	
	private void onDropEvent(int col)
	{
		if(isDropping)
		{
			return;
		}
		
		//prevent any new drops while dropping
		isDropping = true;
		
		float dropRate = getTileDimension() * DROP_RATE;
		//where to place the tile
		int lastEmptyRow = findLastEmptyRow(col);
		
		//turn the target tile into the top tile
		Tile targetTile = tiles[col][lastEmptyRow];
		dropTileTo(targetTile);
		
		//generate an animation event
		AnimationEvent event = new AnimationEvent(ANIM_DROP_TILE);
		//animate moving to column
		Animation moveToCol = animatePositionToTile(targetTile, col, -1, dropRate);
		event.add(moveToCol);
		//animate moving to bottom
		Animation moveToBottom = animatePositionToTile(targetTile, col, lastEmptyRow ,dropRate);
		event.add(moveToBottom);
		addAnimationEvent(event);
	}
	
	private void onDropComplete()
	{
		generateRandomTile(topTile, false);
		setNumbersToPlace(getNumbersToPlace() - 1);
		if(getNumbersToPlace() == 0 && getCurrentLevel() < MAX_LEVELS)
		{
			generatePuzzle();
			setCurrentLevel(getCurrentLevel() + 1);
		}
		
		//touch events cause drops again
		isDropping = false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) 
	{
		if(event.getAction() == MotionEvent.ACTION_DOWN)
		{
			int mouseCol = JMath.colFromPos(getLeftOffset(), getTileDimension(), event.getX());
			//check if everything is in bounds and empty destination, and tiles to place
			if(mouseCol >= 0 && mouseCol < X_AXIS_TILES &&
					event.getY() > getTopOffset() &&
					event.getY() < getHeight() - getBottomOffset() &&
					tiles[mouseCol][0].isEmpty() &&
					getNumbersToPlace() > 0)
			{
				onDropEvent(mouseCol);
				return true;
			}
			
			return false;
		}
		else
		{
			return super.onTouchEvent(event);
		}
	}
	
	public void resume()
	{
		// start the animation thread
		thread = new Thread (this);
		thread.start ();
		running = true;
	}

	public void pause()
	{
		// set the running flag to false. this will stop the animation loop
		// running was defined as volatile since it is used by two different
		// threads
		running = false;

		// we wait for the thread to die
		while (true)
		{
			try
			{
				thread.join ();
				break;
			}
			catch (InterruptedException e)
			{
			}
		}
	}
	
	public void run()
	{
		// this is the method that gets called when the thread is started.

		// first we get the current time before the loop starts
		long startTime = System.currentTimeMillis();

		// start the animation loop
		while (running)
		{
			// we have to make sure that the surface has been created
			// if not we wait until it gets created
			if (!holder.getSurface ().isValid())
				continue;

			// get the time elapsed since the loop was started
			// this is important to achieve frame rate-independent movement,
			// otherwise on faster processors the animation will go too fast
			float timeElapsed = (System.currentTimeMillis () - startTime);

			// is it time to display the next frame?
			if (timeElapsed > FRAME_RATE)
			{
				// compute the next step in the animation
				update();

				// display the new frame
				display();

				// reset the start time
				startTime = System.currentTimeMillis();
			}
		}

		// run is over: thread dies
	}
	

	private void update()
	{
		//update the animations (if any)
		animationManager.update();
	}

	private void display()
	{
		// we lock the surface for rendering and get a Canvas instance we can use
		Canvas canvas = holder.lockCanvas();
		onDraw(canvas);
		// we unlock the surface and make sure that what we've drawn via the Canvas gets
		// displayed on the screen
		holder.unlockCanvasAndPost(canvas);
	}
	
	public void onAnimationStarted(AnimationEvent animation)
	{
		/*
		 * This is called here because this is always called from the game thread
		 * thus it happens in the update() and ensures this logic is not performed on the UI thread
		 * otherwise strange flickering may occur due to concurrency issues
		 */
		if(animation.getId() == ANIM_DROP_TILE)
		{
			clearTopTile();
		}
	}
	
	public void onAnimationFinished(AnimationEvent animation)
	{
		if(animation.getId() == ANIM_DROP_TILE)
		{
			onDropComplete();
			
			//increase the score for the sake of making it more engaging
			increaseScore(JMath.randomRange(50, 300));
		}
	}
	
	public void onAnimationCanceled(AnimationEvent animation)
	{
		
	}
	
	private Animation animatePositionToTile(PositionComponent t, int x, int y, float pixelsPerSec)
	{
		RectF newPos = createTileRect(x, y);
		//calculate time from speed and distance
		float time = JMath.calcMovementTime(t.getRect(), newPos,pixelsPerSec);
		return new Animation(t, new PositionComponent(newPos), time);
	}
	
	int findLastEmptyRow(int column)
	{
		//out of bounds
		if(column < 0 || column >= X_AXIS_TILES)
		{
			return -1;
		}
		
		//start at the top and keep going down until something is there
		for(int i = 0; i < tiles[column].length; ++i)
		{
		  if(!tiles[column][i].isEmpty())
		  {
			  return i - 1;
		  }
		}
		
		//the column was empty
		return tiles[column].length - 1;
	}
}
