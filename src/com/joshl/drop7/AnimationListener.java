//Class Name: AnimationListener.java
//Purpose: Represents an interface for listening to Animation Events.
//Created by Josh on 2012-09-21
package com.joshl.drop7;

public interface AnimationListener 
{
	public void onAnimationStarted(AnimationEvent animation);
	public void onAnimationFinished(AnimationEvent animation);
	public void onAnimationCanceled(AnimationEvent animation);
}
