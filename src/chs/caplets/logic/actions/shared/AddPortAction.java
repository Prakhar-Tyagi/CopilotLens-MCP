/*
 * Copyright 2007-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.IUpdateableAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateConductorAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IConductor;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utility.PortHelper;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

// TODO jacobt FEAT13040 : delete this class
public class AddPortAction extends CreateConductorAction
{

	private ISpecialSelectMgr specialSelectMgr;
	private IConductor port;

	public AddPortAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller);

		specialSelectMgr = libSelectMgr;
		if (getActionUI() != null) {
			specialSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							return getOperand() != null;
						}

						public String getName()
						{
							return (String) getAction().getValue(Action.NAME);
						}
					}
			);
		}
	}

	protected Model getLogicModel()
	{
		return (Model) getModel();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		port = getOperand();
		if (port == null) {
			return IActionEnum.eCanceled;
		}
		return super.onActivate(e);
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = super.onTerminate(successful);
		port = null;
		return ok;
	}

	@Override protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		// Set the connectivity to be this port - we know it already exists in this design to exist at all
		getCommand().setCableConductor(port);

		chs.cof.logical.schem.IConductor schemCond =
				(chs.cof.logical.schem.IConductor) super.constructDisplayObject(point_list);

		PortHelper.assignPortedConductor(schemCond, port);
		PortHelper.addPortGfx(schemCond, getLogicModel().getDiagram().getGrid().getGridSpacing());

		return schemCond;
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		IConductor conductor = getOperand();
		((IUpdateableAction) getActionUI()).updateUI();

		return super.isEnabled() && conductor != null;
	}

	public String getActionUIClass()
	{
		return AddPortActionUI.class.getName();
	}

	@Nullable protected IConductor getSelectedConductorObject()
	{
		if (specialSelectMgr != null && specialSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = specialSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof IConductor) {
				IConductor netCond = (IConductor) uidObj;
				if (netCond.getSharedConductor() == null) {
					return netCond;
				}
			}
		}
		return null;
	}

	@Nullable public IConductor getOperand()
	{
		return getSelectedConductorObject();
	}
}