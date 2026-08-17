/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

/**
 *  state of shareable group
 */
public enum ShareabilityStatus
{
	VALID,
	MULTIPLE_TARGET_SHARED_OBJECTS,
	MULTIPLE_TARGET_SHARED_OBJECT_REVISIONS,
	FROZEN_TARGET_SHARED_OBJECT
}
