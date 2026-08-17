/*
 * Copyright 2004-2017 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;

import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 22-12-2016.
 */
public class CAFSharedDeviceConnectorCleaner extends SharedDeviceConnectorCleaner
{

	public CAFSharedDeviceConnectorCleaner(@NotNull ILogicModel model)
	{
		super(model.getDesign());
	}
}
