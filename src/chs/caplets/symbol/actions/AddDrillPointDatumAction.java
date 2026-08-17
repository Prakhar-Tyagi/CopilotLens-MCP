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
import chs.common.IDrillPointDatum;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;


public class AddDrillPointDatumAction extends AbstractAddDatumAction
{


	public AddDrillPointDatumAction(ICapletController controller)
	{
		super(controller, null);
	}

	@NotNull @Override protected IBaseDatum newDatum()
	{
		return FactoryMgr.getCommonFactory().createDrillPointDatum(FactoryMgr.createUID());
	}

	@Override protected void addDatumToStamp(@NotNull IStamp stamp, @NotNull IBaseDatum datum)
	{
		IDrillPointDatum dpDatum = (IDrillPointDatum) datum;
		stamp.addDatum(dpDatum);
	}

	protected boolean onTerminate(boolean successful)
	{
		cleanUpTransientGraphics();

		if (successful && m_currPoint != null) {
			createPositionnedDatum(m_currPoint, null);
		}

		refreshUIOnTerminate();

		return true;
	}

	public String getActionUIClass()
	{
		return AddDrillPointDatumActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "AddDrillPointDatumAction.statusbar.text");
	}

	@Override protected boolean checkSymbol()
	{
		return true;
	}

	protected boolean isAvailableInBorder()
	{
		return true;
	}

}
