/*
 * Copyright 2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.partbrowser;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.partbrowser.PartActionHandlerBase;
import chs.caf.cafmain.actions.partbrowser.PartBrowserAction;
import chs.caf.caplet.ICapletModel;
import chs.cof.logical.schem.ILayoutLogicDiagram;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.utilities.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PartActionHandler extends PartActionHandlerBase
{

	public PartActionHandler(@NotNull ICapletModel model)
	{
		super(model);
	}

	@NotNull protected List<PartBrowserAction> getPartBrowserActions()
	{
		List<PartBrowserAction> partBrowserActions = new ArrayList<PartBrowserAction>();
		if (CAFUtils.getInstance().getActiveDiagram() instanceof ILayoutLogicDiagram) {
			partBrowserActions.add(new CreateOtherComponentWithPartAndSymbolFromPartBrowserAction());
			partBrowserActions.add(new CreateOtherComponentOnlyWithPartFromPartBrowserAction());
			partBrowserActions.add(new CreateMountWithPartAndSymbolFromPartBrowserAction());
			partBrowserActions.add(new CreateMountOnlyWithPartFromPartBrowserAction());
			partBrowserActions.add(new CreateDuctWithPartAndSymbolFromPartBrowserAction());
			partBrowserActions.add(new CreateDuctOnlyWithPartFromPartBrowserAction());
		}
		else {
			boolean interconnectFlowEnabled = Environment.isInterconnectFlowAllowed();
			partBrowserActions.add(new CreatePlugFromPartBrowserAction());
			partBrowserActions.add(new CreateReceptacleFromPartBrowserAction());
			if (interconnectFlowEnabled) {
				partBrowserActions.add(new CreateInterconnectConnectorFromPartBrowserAction());
			}
			partBrowserActions.add(new CreateDeviceFromPartBrowserAction());
			partBrowserActions.add(new CreateDeviceWithPinsFromPartBrowserAction());
			if (interconnectFlowEnabled) {
				partBrowserActions.add(new CreateInterconnectDeviceFromPartBrowserAction());
			}
			partBrowserActions.add(new CreateWireFromPartBrowserAction());
			partBrowserActions.add(new CreateNetFromPartBrowserAction());
			partBrowserActions.add(new CreateShieldFromPartBrowserAction());
			partBrowserActions.add(new CreateOverbraidFromPartBrowserAction());
			partBrowserActions.add(new CreateMulticoreFromPartBrowserAction());
			partBrowserActions.add(new CreateSingleLineFromPartBrowserAction());
			partBrowserActions.add(new CreateSpliceFromPartBrowserAction());
			partBrowserActions.add(new CreateAssemblyFromPartBrowserAction());
			partBrowserActions.add(new CreateRingTerminalFromPartBrowserAction());
		}
		return partBrowserActions;
	}

	protected void addActionItems(ActionContainer actiontMenu, ILibraryPartSelection partSel)
	{
		if (partSel != null) {
			List<PartBrowserAction> partBrowserActions = getPartBrowserActions();
			for (PartBrowserAction act : partBrowserActions) {
				final ILibraryObject selectedObject = partSel.getSelectedObject();
				if (selectedObject != null && act.isApplicable(selectedObject)) {
					actiontMenu.add(new ActionEntry(act));
				}
			}
		}
	}
}