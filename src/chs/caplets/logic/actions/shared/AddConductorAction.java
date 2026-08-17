/*
 * Copyright 2006-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.snapping.SnapThroughConnectorHelper;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateConductorAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.services.dynamicgfx.IDynamicSnap;
import chs.services.dynamicgfx.ISmartPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.List;

public class AddConductorAction extends CreateConductorAction implements ICreateConductorInstanceAction
{

	private static Cursor m_cantAddWireCursor = null;

	public AddConductorAction(ICapletController controller)
	{
		super(controller);
		m_cantAddWireCursor = CAFUtils.getInstance()
				.loadCursor(controller.getCaplet(), "chs/images/app/cur_cantaddwire.gif", new Point(7, 7));
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (!mCreateCondInstanceHelper.onActivate(this::refresh, getLogicModel().getDesign(), IConductor.class)) {
			return IActionEnum.eCanceled;
		}
		return super.onActivate(e);
	}

	@Override public boolean isReadyForActivation()
	{
		return isEnabled();
	}

	@Nullable protected IConductor getSelectedConductorObject()
	{
		return mCreateCondInstanceHelper.getSelectedConductorObject();
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = super.onTerminate(successful);
		mCreateCondInstanceHelper.onTerminate();
		return ok;
	}

	// TODO jacobt FEAT13040 : generify CreateConductorAction.constructDisplayObject

	@Override protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		IConductor conductor = mCreateCondInstanceHelper.getConductor();
		// Set the connectivity to be this port - we know it already exists in this design to exist at all
		getCommand().setCableConductor(conductor);

		return mCreateCondInstanceHelper.processInstanceConductorCreation(conductor, getLogicModel().getDiagram(),
				() -> (chs.cof.logical.schem.IConductor) super.constructDisplayObject(point_list));
	}

	public boolean isEnabled()
	{
		return super.isEnabled() && mCreateCondInstanceHelper.isReadyForActivation(IConductor.class);
	}

	protected Model getLogicModel()
	{
		return (Model) getModel();
	}

	public String getActionUIClass()
	{
		return AddConductorActionUI.class.getName();
	}

	// TODO jacobt FEAT13040 : we want this shown only in the design browser tree context menu - special select mgr?
//	public void populateCtxMenu(ActionContainer container, SelectSet selections)
//	{
//		if (isEnabled()) {
//			container.add(new ActionEntry(getActionUI()));
//		}
//	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	@Override
	public boolean checkWireCanBeSnapped(@NotNull IDynamicSnap dynamicSnap, @Nullable ILogicObject logicObject) {

		return SnapThroughConnectorHelper.checkWireCanBeSnapped(dynamicSnap, logicObject);
	}

	public void updateCursor(boolean valid)
	{
		if(valid){
			setCursor(getCursor());
		}
		else {
			setCursor(m_cantAddWireCursor);
		}
	}
}
