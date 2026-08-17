/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

/**
 * * Used to harvest results of the compare.
 */
public interface IComparerFeedback
{

	public void onlyInFirst(Object o);

	public void onlyInSecond(Object o);

	public void inBoth(Object o1, Object o2);
}
