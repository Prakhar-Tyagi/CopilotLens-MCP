/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.layout;

import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;

public class ParameterizedPhysicalDimensionUpdateListener implements IModelChangeListener
{

	protected ILogicDesign m_logicDesign;

	public ParameterizedPhysicalDimensionUpdateListener(@NotNull ILogicDesign logicDesign)
	{
		m_logicDesign = logicDesign;
	}

	@Override public void modelPreChanged(@NotNull ModelChangeEvent e)
	{
		ParameterizedPhysicalDimensionUpdater.getInstance()
				.updatePhysicalDimensions(m_logicDesign, e.getChangedObjectsUIDs());
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{
	}
}
