/*
 * Copyright 2017 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.serviceDocumentation.smartflows;

import org.jetbrains.annotations.Nullable;

public enum ProcessFlowDirection
{
	Left, Right;

	ProcessFlowDirection()
	{
	}

	@Nullable public static ProcessFlowDirection getDirection(String value)
	{
		if (value == null || "right".equalsIgnoreCase(value)) {
			return Right;
		}
		if ("left".equalsIgnoreCase(value)) {
			return Left;
		}
		return null;
	}
}
