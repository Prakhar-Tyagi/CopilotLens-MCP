/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IFIB;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectPropagator;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.logical.shared.ISharedInternalPosition;
import chs.common.IUIDObject;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import chs.utility.logic.ILogicModel;

import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.ActionEvent;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_EXPAND_SELECTION_ACTION",
		label = "Expand Selection",
		tooltip = "Expand the selection to associated objects(Ctrl+E)",
		icon = "ico_expand_selection",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class ExpandSelectionAction extends AppAction implements ICtxMenuProvider
{

	SelectPropagator m_propagator;

	public ExpandSelectionAction(IFIB fib)
	{
		super(fib);
		m_propagator = new SelectPropagator();
		putValue(NAME,
				ResourceMgr.getString(ExpandSelectionAction.class, "ExpandSelectionAction.putValue.action.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(ExpandSelectionAction.class, "ExpandSelectionAction.putValue.action.text_1"));
		putValue(LONG_DESCRIPTION, getValue(SHORT_DESCRIPTION));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, Event.CTRL_MASK));
		putValue(MNEMONIC_KEY, new Integer(java.awt.event.KeyEvent.VK_X));
	}

	// AppAction implementation
	public void updateUI()
	{
	}

	public void setEnabled(boolean newValue)
	{
	}

	public boolean isEnabled()
	{
		if (!isLogic()) {
			return false;
		}
		SelectSet selections = CAFUtils.getInstance().getActiveSelectMgr().getPreSelections();
		return (CAFUtils.getInstance().getActiveSelectMgr() != null &&
				selections.getSelectCount() > 0 && !containsOnlyInvalidObjects(selections));
	}

	private boolean containsOnlyInvalidObjects(SelectSet selections)
	{
		for (IUIDObject operand : selections.getSelectedObjects(IUIDObject.class)) {
			if (!(operand instanceof ISharedInternalPosition)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isLogic()
	{
		ICapletController capletController = CAFUtils.getInstance().getActiveCapletController();
		if (capletController != null) {
			return CAFUtils.getInstance().getActiveCapletController().getCapletModel() instanceof ILogicModel;
		}
		return false;
	}

	// ActionListener  implementation
	public void actionPerformed(ActionEvent e)
	{
		m_propagator.propagate(CAFUtils.getInstance().getActiveSelectMgr().getPreSelections(),
				((Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel()).getDiagram());
	}

	// ICtxMenuProvider implementation

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// If there is something selected we can delete it (make sure we're actually in Logic !!!)
		if (CAFUtils.getInstance().getActiveCapletController().getCapletModel() instanceof Model &&
				CAFUtils.getInstance().getActiveSelectMgr().getPreSelections().getSelectCount() > 0) {
			container.add(new ActionEntry(this));
		}
	}
}
