/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

/**
 * @author chandras on 22-10-2019.
 */
public enum PlacementCommitType
{
	//in manual placement mode the type is manual.
	//for group placement mode the type is group.
	//for auto (abut) placement the batch of objects
	//will be represent by auto..auto..auto_end.
	//auto_end would mark the end of the batch.
	MANUAL, AUTO, AUTO_END, GROUP, NOT_COMMITED
}
