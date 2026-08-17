/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IPinList;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.Nullable;

public class BackshellAddTerminationHelper extends PinListAddPinHelper
{

	public BackshellAddTerminationHelper(IPinList pinList, boolean isReference)
	{
		super(pinList, isReference);
	}

	@Override protected void createPinName(IAbstractPin newpin, @Nullable String pinName)
	{
		setNameIfDifferent(newpin, pinName);
	}

	private void setNameIfDifferent(IAbstractPin pin, @Nullable String pinName)
	{
		if (!StringUtils.isEmpty(pinName)) {
			if (pin.getName().compareToIgnoreCase(pinName) != 0) {
				pin.setName(pinName);
			}
		}
	}
}
