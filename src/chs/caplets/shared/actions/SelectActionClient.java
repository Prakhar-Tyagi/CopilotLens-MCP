/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.actions;

// CAF imports

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.IManipulate;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.MoveManipulator;
import chs.caf.caplet.helpers.SelectClientHelper;
import chs.caf.caplet.helpers.StretchManipulator;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.Selection;
import chs.caplets.logic.LogicMoveManipulator;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.dynamicgfx.ISmartPointIterator;
import chs.utilities.CommonUtils;
import chs.utility.helpers.ConnectorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * Provide an implementation that knows how to return the correct manipulators, and the fact that this is the base
 * action.
 *
 * @author Glenn Reynholds
 */

public class SelectActionClient extends SelectClientHelper
{

	protected static final int STRETCH_ALLOWED = 1;
	protected static final int MOVE_ALLOWED = 2;

	/**
	 * Contains an {@link IGfxModel}.
	 */
	private Reference m_model = null;
	protected StretchManipulator m_stretchManip = null;
	protected MoveManipulator m_moveManip = null;

	public SelectActionClient(IAction action, ICapletController controller)
	{
		super(action);
		IGfxModel model = (IGfxModel) controller.getCapletModel();
		m_model = new WeakReference(model);

		// Create the manipulators
		createManipulators(model);
	}

	protected void createManipulators(IGfxModel model)
	{
		m_stretchManip = new LogicStretchManipulator(model.getDynamicGfxService(), model);
		m_moveManip = new LogicMoveManipulator(model.getDynamicGfxService(), model);
	}

	protected IGfxModel getGfxModel()
	{
		return (IGfxModel) m_model.get();
	}

	/**
	 * This client is capable of returning manipulators
	 */
	public boolean isManipulatorProvider()
	{
		return true;
	}

	/**
	 * Return a mainpulator for the current mouse position if appropriate.
	 */
	@Nullable public IManipulate getManipulator(MouseEvent e)
	{
		int status = previewSetHasManipulableObjects();
		Point startPoint = e.getPoint();
		IDynamicGfxService dgs = getGfxModel().getDynamicGfxService();
		ISmartPointIterator iter = dgs.getPreviewSelectionGripPoints(startPoint,
				m_stretchManip.getGripRadiusCandidates());

		if (iter.hasNext() && checkStatus(status, STRETCH_ALLOWED)) {
			return m_stretchManip;
		}
		if (checkStatus(status, MOVE_ALLOWED)) {
			return getMoveManipulator();
		}
		return null;
	}

	private boolean checkStatus(int status, int check)
	{
		return (status & check) == check;
	}

	public IManipulate getMoveManipulator()
	{
		return m_moveManip;
	}

	/**
	 * This is the base action
	 */
	public boolean isBaseAction()
	{
		return true;
	}

	protected int previewSetHasManipulableObjects()
	{
		// Return true if the selection set contains anything besides [stacked ]pins.
		ISelectMgr activeSelectMgr = CAFUtils.getInstance().getActiveSelectMgr();
		assert activeSelectMgr != null;
		int status = 0;
		for (Selection selection : activeSelectMgr.getPreviewSelections().getSelected()) {
			status |= checkManipulableObject(selection);
		}
		return status;
	}

	protected int checkManipulableObject(@NotNull Selection sel)
	{
		if (IAbstractSchemPin.class.isAssignableFrom(sel.getSelectionClass())) {
			return 0;
		}
		IPinList pinList = CommonUtils.cast(sel.getObject(), IPinList.class);
		if (pinList == null) {
			return (STRETCH_ALLOWED | MOVE_ALLOWED);
		}
		int status = MOVE_ALLOWED;
		//do not allow stretch on child modular schematics.
		if (ConnectorHelper.getParentSchemPinList(pinList) == null) {
			status |= STRETCH_ALLOWED;
		}
		return status;
	}
}