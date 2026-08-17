/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2009-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.ctf.ui.action.IRibbonRelatedActionMapper;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * FEAT00013725 - Automated handling of shared object revisions
 * <p>
 * UI class for Enhanced Swap Out from CLogic
 * <p>
 *
 * @author ntewari
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalCapture, Application.CapitalArchitect, Application.CapitalLogicDesigner,
				Application.ArtisanFunction}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class EnhancedSwapOutSharedObjectRevisionActionUI extends ActionUI implements IRibbonRelatedActionMapper
{

	/**
	 * Constructor for the EnhancedSwapOutSharedObjectRevisionActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public EnhancedSwapOutSharedObjectRevisionActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");

		Integer iMnemonic =
				(int) ResourceMgr
						.getMnemonic(EnhancedSwapOutSharedObjectRevisionActionUI.class,
								"EnhancedSwapOutSharedObjectRevisionActionUI.mnemonic");
		putValue(MNEMONIC_KEY, iMnemonic);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getStringForMenu(EnhancedSwapOutSharedObjectRevisionActionUI.class,
				"EnhancedSwapOutSharedObjectRevisionActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getStringForMenu(EnhancedSwapOutSharedObjectRevisionActionUI.class,
				"EnhancedSwapOutSharedObjectRevisionActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(EnhancedSwapOutSharedObjectRevisionActionUI.class,
				"EnhancedSwapOutSharedObjectRevisionActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			IAction action = getAction();
			if (action != null) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			}
			return false;
		}
		return super.isEnabled();
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return EnhancedSwapOutSharedObjectRevisionAction.class.getName();
	}


	@NotNull @Override public String getRibbonRelatedAction()
	{
		return SwapOutSharedObjectRevisionAction.class.getName();
	}
}
