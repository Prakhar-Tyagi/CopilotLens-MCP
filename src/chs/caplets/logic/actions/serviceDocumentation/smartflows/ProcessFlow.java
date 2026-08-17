/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.serviceDocumentation.smartflows;

import chs.cof.logical.cable.NetConductor;
import chs.common.IProperty;
import chs.utility.helpers.PropertyHelper;
import org.jetbrains.annotations.Nullable;

public class ProcessFlow implements IProcessFlow
{

	public static final String DIRECTION = "Direction";
	private NetConductor propertySetter;

	public ProcessFlow(NetConductor net)
	{
		propertySetter = net;
	}

	@Nullable @Override public ProcessFlowDirection getDirection()
	{
		String propName = DIRECTION;
		IProperty propValue = propertySetter.findPropertyByName(propName);
		if (propValue != null) {
			String asString = propValue.getAsString();
			if (asString != null) {
				ProcessFlowDirection direction = ProcessFlowDirection.getDirection(asString);
				return direction;
			}
		}
		return null;
	}

	@Override public void changeDirectionTo(ProcessFlowDirection direction)
	{
		String propName = DIRECTION;
		String dirValue = direction.toString();
		PropertyHelper.setProperty(propertySetter, propName, dirValue, "String");
	}
}
