/*
 * Copyright 2006-2013 Mentor Graphics Corporation
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
import chs.common.IEngineeringDatum;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;


public class AddEngineeringDatumAction extends AbstractAddDatumAction
{

	private String m_type;

	public AddEngineeringDatumAction(ICapletController controller, String type)
	{
		super(controller, type);
		m_type = type;
	}

	@NotNull protected IBaseDatum newDatum()
	{
		return FactoryMgr.getCommonFactory().createEngineeringDatum(FactoryMgr.createUID(), m_type);
	}

	protected void addDatumToStamp(@NotNull IStamp stamp, @NotNull IBaseDatum datum)
	{
		stamp.addDatum((IEngineeringDatum) datum);
	}

	public boolean onTerminate(boolean successful)
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
		return AddEngineeringDatumActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		if (super.isEnabled()) {
			for (IEngineeringDatum datum : m_model.getSymbolDef().getAllEngineeringDatums()) {
				if (datum.getName() != null && m_type.equalsIgnoreCase(datum.getName())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "AddDatumAction.statusbar.text");
	}

	@Override public boolean isActionRepeatable()
	{
		return false;
	}
}
