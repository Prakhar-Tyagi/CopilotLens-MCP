/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border.actions;

import chs.caf.caplet.ICapletController;
import chs.caplets.symbol.actions.AbstractCreateRectangularDatumAction;
import chs.cof.symbol.IStamp;
import chs.common.IBaseDatum;
import chs.common.IFormboardRegionDatum;
import chs.common.IFormboardRegionDatumHolder;
import chs.system.FactoryMgr;
import org.jetbrains.annotations.NotNull;

/**
 * Action to create a rectangular formboard region datum
 */
public class CreateFormboardRegionDatumAction extends AbstractCreateRectangularDatumAction
{

	public CreateFormboardRegionDatumAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return CreateFormboardRegionDatumActionUI.class.getName();
	}

	@Override @NotNull protected IBaseDatum newDatum()
	{
		return FactoryMgr.getCommonFactory().createFormboardRegionDatum(FactoryMgr.createUID());
	}

	@Override protected void addDatumToStamp(@NotNull IBaseDatum datum)
	{
		IStamp stamp = getStamp();
		if (stamp instanceof IFormboardRegionDatumHolder) {
			((IFormboardRegionDatumHolder) stamp).addDatum((IFormboardRegionDatum) datum);
		}
	}

	public boolean isEnabled()
	{
		if (super.isEnabled()) {
			return getStamp() instanceof IFormboardRegionDatumHolder;
		}
		return false;
	}
}


