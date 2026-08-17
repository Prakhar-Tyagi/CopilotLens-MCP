/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2008-2025 Siemens
 */
package chs.caplets.logic.actions.icdbrowser;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionNodeIterator;
import chs.caf.ActionSeparator;
import chs.caf.action.utility.ActionUtilities;
import chs.caf.cafmain.actions.ActionUtils;
import chs.caf.cafmain.actions.icd.ICDLogicDesignValidatorUtil;
import chs.caf.cafmain.actions.partbrowser.PartActionHandlerBase;
import chs.caf.cafmain.actions.partbrowser.PartBrowserAction;
import chs.caf.cafmain.actions.partbrowser.PartBrowserHelpAction;
import chs.caf.cafmain.actions.partbrowser.PartSelectDetailsAction;
import chs.caf.cafmain.actions.partbrowser.PartViewAction;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IAction;
import chs.caplets.logic.icd.ICDBrowserTree;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.GeneralReportValidationHandler;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.concurrency.ILogicConcurrencyController;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.common.IProjectPreferenceMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.ICDUtils;
import chs.utility.icd.placement.ICDPlacementServiceLocator;
import chs.utility.icd.placement.IICDPlacementPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class ICDObjectActionHandler extends PartActionHandlerBase
{

	@NotNull private ICDBrowserTree m_ICDTree;

	public ICDObjectActionHandler(@NotNull ICapletModel model, @NotNull ICDBrowserTree icdTree)
	{
		super(model);
		m_ICDTree = icdTree;
	}

	protected ActionContainer getActionContainer(ILibraryPartSelection partSel)
	{
		try {
			GeneralReportValidationHandler.beginScope();
			ActionContainer actionContainer = doGetActionContainer(partSel);
			ActionUtils.filterActionContainer(actionContainer);
			return actionContainer;
		}
		finally {
			GeneralReportValidationHandler.clearScope();
		}
	}

	private ActionContainer doGetActionContainer(ILibraryPartSelection partSel)
	{
		ActionContainer actiontCont = new ActionContainer("Part Browser Actions");
		IICDSelection icdSelection = CommonUtils.cast(partSel, IICDSelection.class);
		if (icdSelection != null) {
			Collection<ILibraryObject> libraryObjects = icdSelection.getSelectedObjects();
			IDeviceICD icd = icdSelection.getICD();
			IDesign design = icdSelection.getDesign();
			final boolean multipleICDsSelected = m_ICDTree.areMultipleICDsSelected();
			if (icd != null && design != null && !StringUtils.isBlank(icd.getRole()) && libraryObjects.size() == 1) {
				IProject project = design.getProject();
				IICDPlacementPreferences preferences =
						ICDPlacementServiceLocator.getInstace().locateService(IICDPlacementPreferences.class);
				if (project != null && preferences.getOptionExpressionSourceForDevice(project) ==
						IICDPlacementPreferences.OptionExpressionSource.ICD && !ICDUtils.isVariantOpexValid(icd)) {
					new ICDLogicDesignValidatorUtil().report(ResourceMgr.getString(ICDObjectActionHandler.class,
							"ICDObjectActionHandler.invalidVariantOpexErrorMsg", icd.getRole()), false);
					return actiontCont;
				}

				String role = icd.getRole();
				addSpecificActions(actiontCont, icdSelection, icd, libraryObjects.iterator().next(), role, design,
						multipleICDsSelected);
				addCommonActions(actiontCont, icd, role, design.getConnectivity(), multipleICDsSelected);
				addPartBrowserHelpAction(actiontCont);
			}
			else {
				ActionUtilities.addTreeActions(actiontCont, m_ICDTree);
				addPartBrowserHelpAction(actiontCont);
			}
		}
		return actiontCont;
	}

	private void addSpecificActions(@NotNull ActionContainer actiontMenu, @NotNull IICDSelection icdSelection,
			@NotNull IDeviceICD icd, @NotNull ILibraryObject libObj, @NotNull String role, @NotNull IDesign design,
			boolean multipleICDsSelected)
	{
		if (multipleICDsSelected) {
			return;
		}
		if (ILogicConcurrencyController.isUnderLogicConcurrencyLimitation(design)) {
			return;
		}
		IConnectivity connectivity = design.getConnectivity();
		if (connectivity != null) {
			IPinList placedDevice = ICDUtils.getMatchingDevice(design, role);
			if (placedDevice != null) {
				if (placedDevice.isShared()) {
					addActionForSharedICD(actiontMenu);
				}
				else {
					addActionForPlacedUnsharedICD(actiontMenu);
				}
			}
			else {
				IProject project = design.getProject();
				IProjectPreferenceMgr preferences = project != null ? project.getPreferences() : null;
				boolean doNotAutoShareICD = preferences != null && preferences.getDoNotAutoShareICD();
				if (doNotAutoShareICD) {
					addActionsForUnplacedUnsharedICD(actiontMenu, libObj, icdSelection);
					addActionForAutoShareICD(actiontMenu, icd);
				}
				else {
					addActionForAutoShareICD(actiontMenu, icd);
					addActionsForUnplacedUnsharedICD(actiontMenu, libObj, icdSelection);
				}
			}
		}
	}

	private void addActionForAutoShareICD(@NotNull ActionContainer actiontMenu, @NotNull IDeviceICD icd)
	{
		if (!ICDUtils.isICDApplicableForJustOneDesign(icd)) {
			// do not add the action that creates a new shared device, if icd is applicable to just one design
			addActionForSharedICD(actiontMenu);
		}
	}

	private void addActionForSharedICD(@NotNull ActionContainer actiontMenu)
	{
		actiontMenu.add(new ActionEntry(new CreateICDFromSharedICDBrowserAction()));
	}

	@Nullable protected Action getDefaultActionToPerform(@NotNull ActionNodeIterator iter)
	{
		ActionEntry actionEntry = CommonUtils.cast(iter.hasNext() ? iter.getNext() : null, ActionEntry.class);
		return actionEntry != null ? actionEntry.getAction() : null;
	}

	private void addActionsForUnplacedUnsharedICD(@NotNull ActionContainer actiontMenu, @NotNull ILibraryObject libObj,
			@NotNull IICDSelection icdSelection)
	{
		PartBrowserAction symbolICDBrowserAction = new CreateDeviceFromICDBrowserAction();
		IAction action = symbolICDBrowserAction.getActionToPerform();
		AddDeviceFromICDAction symbolICDAct = CommonUtils.cast(action, AddDeviceFromICDAction.class);
		if (symbolICDAct != null) {
			if (symbolICDAct.isSelectionAssociatedWithSymbol(icdSelection)) {
				if (symbolICDBrowserAction.isApplicable(libObj)) {
					actiontMenu.add(new ActionEntry(symbolICDBrowserAction));
				}
			}
			PartBrowserAction parametrizedDeviceFromICDBrowserAction =
					new CreateParametrizedDeviceFromICDBrowserAction();
			if (parametrizedDeviceFromICDBrowserAction.isApplicable(libObj)) {
				actiontMenu.add(new ActionEntry(parametrizedDeviceFromICDBrowserAction));
			}
			actiontMenu.add(new ActionEntry(new PartSelectDetailsAction(true)));
		}
	}

	private void addActionForPlacedUnsharedICD(@NotNull ActionContainer actiontMenu)
	{
		CreatePlacedICDFromICDBrowserAction placeICDAction = new CreatePlacedICDFromICDBrowserAction();
		actiontMenu.add(new ActionEntry(placeICDAction));
	}

	private boolean isDeviceAlreadyPlaced(@NotNull IConnectivity connectivity, @NotNull String deviceName)
	{
		Set<String> placedDevices = getDeviceNamesInLowerCase(connectivity);
		return placedDevices.contains(deviceName.toLowerCase());
	}

	private void addCommonActions(ActionContainer actiontCont, @NotNull IDeviceICD icd, @NotNull String role,
			@Nullable IConnectivity connectivity, boolean multipleICDsSelected)
	{
		if (connectivity != null && isDeviceAlreadyPlaced(connectivity, role)) {
			UpdateICDFromICDBrowserAction action = new UpdateICDFromICDBrowserAction();
			actiontCont.add(new ActionEntry(action));
		}
		if (!multipleICDsSelected) {
			ViewICDDetailsAction action = new ViewICDDetailsAction(icd);
			actiontCont.add(new ActionEntry(action));
			if (icd.getLibraryDevice() != null) {
				PartViewAction partViewAct = new PartViewAction(true);
				actiontCont.add(new ActionEntry(partViewAct));
			}
		}
	}

	private void addPartBrowserHelpAction(ActionContainer actiontCont)
	{
		actiontCont.add(new ActionSeparator());
		PartBrowserHelpAction partBrowserHelpAct = new PartBrowserHelpAction(true);
		actiontCont.add(new ActionEntry(partBrowserHelpAct));
	}

	@Override protected void addActionItems(ActionContainer actiontMenu, ILibraryPartSelection partSel)
	{

	}

	public static Set<String> getDeviceNamesInLowerCase(@NotNull IConnectivity connectivity)
	{
		Set<String> placedDevices = new HashSet<String>();

		for (IDevice device : connectivity.getAllDevices()) {
			placedDevices.add(ICDUtils.getICDMatchName(device).toLowerCase());
		}
		return placedDevices;
	}
}
