/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.symbol.IStamp;
import chs.common.IBaseDatum;
import chs.common.IGenericDatum;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

public class AddGenericDatumAction extends AbstractAddDatumAction
{


	public AddGenericDatumAction(ICapletController controller)
	{
		super(controller, null);
	}

	@NotNull @Override protected IBaseDatum newDatum()
	{
		return FactoryMgr.getCommonFactory().createGenericDatum(FactoryMgr.createUID());
	}

	@Override protected void addDatumToStamp(@NotNull IStamp stamp, @NotNull IBaseDatum datum)
	{
		IGenericDatum genDatum = (IGenericDatum) datum;
		stamp.addDatum(genDatum);
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		cleanUpTransientGraphics();

		if (successful && m_currPoint != null) {
			createPositionnedDatum(m_currPoint, null);
		}

		refreshUIOnTerminate();

		return true;
	}

	@Override public String getActionUIClass()
	{
		return AddGenericDatumActionUI.class.getName();
	}

	@Override public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "AddGenericDatumAction.statusbar.text");
	}

	@Override protected boolean isAvailableInBorder()
	{
		return true;
	}

	@Override protected boolean checkSymbol()
	{
		return true;
	}
}
