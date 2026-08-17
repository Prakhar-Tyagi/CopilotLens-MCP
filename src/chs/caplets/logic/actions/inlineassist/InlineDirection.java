/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.inlineassist;

/**
 * This Enum represents possible directions of the inline.
 */
public enum InlineDirection
{
	LEFTRIGHT,
	RIGHTLEFT,
	TOPDOWN,
	DOWNTOP;

	/**
	 * Determines "reversed" state of the inline. The normal state is Jack at the left and Plug at the right;
	 * Plug at the top and Jack atht he bottom.
	 *
	 * @return True if direction is considered reversed.
	 */
	public boolean isReversedPinSide()
	{
		switch (this) {
			case LEFTRIGHT:
				return false;
			case RIGHTLEFT:
				return true;
			case TOPDOWN:
				return true;
			case DOWNTOP:
				return false;
			default:
				return false;
		}
	}

	/**
	 * @return True if this direction is vertical.
	 */
	public boolean isVertical()
	{
		return equals(DOWNTOP) || equals(TOPDOWN);
	}
}
