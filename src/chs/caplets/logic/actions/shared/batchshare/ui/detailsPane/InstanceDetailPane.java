/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.ISplice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class InstanceDetailPane extends AbstractInstanceDetailPane
{

	public InstanceDetailPane(@Nullable ILogicObject selectedObject)
	{
		super(selectedObject);
	}

	@NotNull @Override protected DetailsTableInfo createTableInfo(@Nullable ILogicObject selectedObject)
	{
		if (selectedObject instanceof IInlineJackConnector) {
			return new InlineDetailsTableInfo();
		}
		else if (selectedObject instanceof ISplice) {
			return new SpliceDetailsTableInfo();
		}
		else if (selectedObject instanceof IConductor) {
			return new ConductorDetailsTableInfo();
		}
		else if (selectedObject instanceof IMulticore) {
			return new MulticoreDetailsTableInfo();
		}
		else if (selectedObject instanceof IHighway) {
			return new HighwayDetailsTableInfo();
		}
		else {
			return new PinlistDetailsTableInfo();
		}
	}
}
