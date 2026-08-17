/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2008-2026 Siemens
 */
package chs.caplets.logic.actions.partbrowser;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caplets.logic.actions.AddDeviceFromLibraryPartAction;
import chs.cof.parts.partselector.ILibraryPartSelection;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.CapitalCapture,
				Application.CapitalArchitect, Application.SEElectricalDesign}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class CreateDeviceFromPartBrowserAction extends AbstractCreateDeviceFromPartBrowserAction
{

	public CreateDeviceFromPartBrowserAction()
	{
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController().getAction(AddDeviceFromLibraryPartAction.class);
	}

	public boolean isEnabled(){
		ILibraryPartSelection part = PartBrowserActionHelper.getCurrentSelectedBrowserPart();
		return super.isEnabled() && part!=null && part.getSelectedSymbol()!=null;
	}
}
