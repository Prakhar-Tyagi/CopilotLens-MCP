/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2024 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.ActionContainer;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.CreateSingleLineAction;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.FactoryMgr;
import chs.utility.PortHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;

/**
 * Action to support add/placing a Single Line
 */
public class AddSingleLineAction extends CreateSingleLineAction
{

	@Nullable protected ISingleLine mSingleLine;

	public AddSingleLineAction(ICapletController controller)
	{
		super(controller);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		mSingleLine = getOperand();
		if (mSingleLine == null) {
			return IActionEnum.eCanceled;
		}
		if (!LogicObjectLockFinder.tryEdit(mSingleLine)) {
			return IActionEnum.eCanceled;
		}
		return super.onActivate(e);
	}

	public boolean onTerminate(boolean successful)
	{
		boolean ok = super.onTerminate(successful);
		mSingleLine = null;
		return ok;
	}

	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		// Set the connectivity to be this port - we know it already exists in this design to exist at all
		getCommand().setCableHighway(mSingleLine);

		ILogicDesign design = Objects.requireNonNull(mSingleLine, "Single Line should exist").getLogicDesign();
		IHighwaySchematic schemHighway = null;
		if (design != null) {
			// port graphics should only be added for multiple representations of a cable
			// count usages *before* this action was performed
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
			int usageCount = dwum.getDesignSharedUsageCount(mSingleLine);

			schemHighway = (IHighwaySchematic) super.constructDisplayObject(point_list);
			if (schemHighway == null) {
				return null;
			}

			boolean home = true; // the first instance added is initially home
			if (usageCount > 0) {
				int gridSpacing = getLogicModel().getDiagram().getGrid().getGridSpacing();
				PortHelper.addPortGraphics(schemHighway, design, usageCount, gridSpacing,
						dwum.getRepresentations(mSingleLine));
				home = false; // all instances after the first are initially non-home
			}
			schemHighway.setHome(home); // similar rules for XRefs as shared
		}

		return schemHighway;
	}

	public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}

		// if we are in a transaction boundary, we MUST wait
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		ISingleLine cableObject = getOperand();
		return cableObject != null;
	}

	protected Model getLogicModel()
	{
		return (Model) getModel();
	}

	/**
	 * @return If only a single connectivity wire is selected, return it otherwise return null
	 */
	@Nullable
	private ISingleLine getOperand()
	{
		ISingleLine singleLine = null;
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		for (SelectedUIDObjectIterator it = selections.getSelectedUIDObjects(); it.hasNext(); ) {
			IUIDObject obj = it.getNext();

			if (obj instanceof ISingleLine) {
				if (singleLine == null) {
					singleLine = (ISingleLine) obj;
				}
				else {
					singleLine = null;
					break;
				}
			}
		}
		return singleLine;
	}

	public String getActionUIClass()
	{
		return AddSingleLineActionUI.class.getName();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
