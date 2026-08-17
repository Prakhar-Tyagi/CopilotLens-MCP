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

public interface IProcessFlow
{

	ProcessFlowDirection getDirection();

	default boolean isDirectionRight()
	{
		ProcessFlowDirection flowDirection = getDirection();
		if (flowDirection == null) {
			return true;
		}
		return flowDirection == ProcessFlowDirection.Right;
	}

	default void toggleDirection()
	{
		Boolean directionRight = isDirectionRight();
		if (directionRight) {
			changeDirectionTo(ProcessFlowDirection.Left);
		}
		else {
			changeDirectionTo(ProcessFlowDirection.Right);
		}
	}

	void changeDirectionTo(ProcessFlowDirection direction);
}
