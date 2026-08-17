/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2025 Siemens
 */
package chs.caplets.shared.actions;

import chs.caf.AppAction;
import chs.caf.IFIB;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.helpers.snapping.ISnapDrawingGridModel;
import chs.caf.caplet.helpers.snapping.SnapHelper;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import com.mentor.capital.ui.IToggleAction;

import java.awt.event.ActionEvent;

/**
 * @author Matt Boyd
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
				Application.CapitalSymbolDesigner, Application.CapitalEssentialsSymbolDesigner, Application.CapitalSymbolForCapture,
				Application.CapitalArchitect, Application.SvcDoc, Application.ArtisanFunction, Application.XSCSymbol,
				Application.SEElectricalDesign, Application.SEElectricalSymbol}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId="CAPITAL_SNAP_TO_DRAWING_GRID_ACTION",
		label="Snap to Drawing Grid",
		tooltip="Snap to Drawing Grid",
		icon="snap_to_grid",
		buttonStyle="MEDIUM_IMAGE_AND_TEXT")
public class ToggleSubGridAction extends AppAction implements IToggleAction
{

	public ToggleSubGridAction(IFIB fib)
	{
		super(fib);

		putValue(NAME, ResourceMgr.getString(ToggleSubGridAction.class, "ToggleSubGridAction.putValue.action.text"));
		putValue(SHORT_DESCRIPTION, getValue(NAME));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(ToggleSubGridAction.class, "ToggleSubGridAction.putValue.action.text_1"));
		putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_S));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent_thin.gif"));
	}

	public void updateUI()
	{
	}

	public void actionPerformed(ActionEvent e)
	{
		ISnapDrawingGridModel model = SnapHelper.getSnapModel();
		if (model != null) {
			model.setDrawingGridSnap(!isSelected());
		}
	}

	public boolean isSelected()
	{
		ISnapDrawingGridModel model = SnapHelper.getSnapModel();
		if (model != null) {
			return model.isDrawingGridSnap();
		}
		return true;
	}

	@Override public boolean isOn()
	{
		return isSelected();
	}
}
