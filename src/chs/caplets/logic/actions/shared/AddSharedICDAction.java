/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.browser.ICDBrowserActionHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.capitalmanager.appserver.LockException;
import chs.caplets.logic.actions.IICDProviderAction;
import chs.caplets.logic.icd.ICDPlacementHelper;
import chs.cof.browser.IBasePartsBrowser;
import chs.cof.draw.IGfxObject;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.icd.IICDDeviceFootprint;
import chs.cof.logical.FootprintUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.TransientLibraryDevice;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.cofUtils.parameterized.Generator;
import chs.common.ILocation;
import chs.common.IReleaseLevel;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.permission.PermissionHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.ICDSharedObjectUtils;
import chs.utility.ICDUtils;
import chs.utility.SharedObjectWithState;
import chs.utility.helpers.PropertyCopier;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AddSharedICDAction extends AddSharedDeviceAction implements IICDProviderAction
{

	private IDeviceICD m_icd;
	protected chs.cof.logical.schem.IPinList m_schemDevice;
	private List<IDynamicGfx> m_dynamicGfxs = new ArrayList<>();
	@Nullable private ISharedPinList sharedPinListToUse;
	@Nullable private ISharedPinList newlyCreatedSharedPinlist;
	@Nullable protected ILibraryPartSelection partSelection;

	public AddSharedICDAction(ICapletController controller)
	{
		super(controller, null);
	}

	@Override public String getActionUIClass()
	{
		return AddSharedICDActionUI.class.getName();
	}

	@Nullable
	protected ISharedPinList getOperand()
	{
		return sharedPinListToUse;
	}

	protected void initialiseSelectedICD()
	{
		ILogicModel logicModel = CommonUtils.cast(getController().getCapletModel(), ILogicModel.class);
		if (logicModel != null) {
			ILogicDesign design = CommonUtils.cast(logicModel.getDesign(), ILogicDesign.class);
			if (design != null) {
				SelectSet selections = getSelections();
				List<IICD> selectedObjects = selections.getSelectedObjects(IICD.class);
				if (selectedObjects.size() == 1) {
					IICD iicd = selectedObjects.iterator().next();
					m_icd = design.getDesignICDContainer().constructDeviceICD(iicd);
				}
			}
		}
	}

	protected SelectSet getSelections()
	{
		return getController().getSelectMgr().getPreSelections();
	}

	@Override protected chs.cof.logical.schem.IPinList createSchemFromSymbol(ISymbolDef associatedSymbol, int instNum,
			@Nullable IBlock block,
			@Nullable ISymbolDef blockSource)
	{
		m_schemDevice = super.createSchemFromSymbol(associatedSymbol, instNum, block, blockSource);
		return m_schemDevice;
	}

	@Override protected IGfxObject createParamObject(Point p1, Point p2)
	{
		IGfxObject object = super.createParamObject(p1, p2);
		chs.cof.logical.schem.IPinList paramObj = CommonUtils.cast(object, chs.cof.logical.schem.IPinList.class);
		if (paramObj != null) {
			m_schemDevice = paramObj;
		}
		return object;
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		super.mouseMoved(e);
		if (getState() == STATE_SYMBOL) {
			updateICDTransGfx(m_symbolDyn.getLocation());
		}
	}

	protected void updateICDTransGfx(ILocation loc)
	{
		if (m_schemDevice == null) {
			return;
		}
		m_schemDevice.setLocation(loc);
		clearDynamicGfx();
		m_dynamicGfxs =
				ICDPlacementHelper.updateNetTraces(m_schemDevice, m_icd, getDiagram(), null);
		for (IDynamicGfx gfxObj : m_dynamicGfxs) {
			m_dynamics.addTransientGfx(gfxObj);
		}
	}

	private void clearDynamicGfx()
	{
		for (IDynamicGfx dynamicGfx : m_dynamicGfxs) {
			m_dynamics.removeTransientGfx(dynamicGfx);
		}
		m_dynamicGfxs.clear();
	}

	@Override protected boolean isContextButton(Object src)
	{
		return true;
	}

	@NotNull @Override
	protected IPlacementOptionParams createPlacementOptionParams(@NotNull ISharedPinList sharedPinList)
	{
		IPlacementOptionParams params = super.createPlacementOptionParams(sharedPinList);
		params.enableWithConductorOption(true, sharedPinList.getProject());
		return params;
	}

	@NotNull @Override
	protected IPlacementOptionParams createPlacementOptionParams(@NotNull PinListTypeEnum pinListType, boolean isShared)
	{
		IPlacementOptionParams params = super.createPlacementOptionParams(pinListType, isShared);
		params.enableWithConductorOption(true, getCurrentProject());
		return params;
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		initialiseSelectedICD();
		if (m_icd == null) {
			return IActionEnum.eCanceled;
		}

		setPartSelection();

		sharedPinListToUse = getExistingSharedObjectOrCreateOneIfRequired();
		if (sharedPinListToUse == null) {
			return IActionEnum.eCanceled;
		}

		if (!canProceedWithAction(sharedPinListToUse)) {
			return IActionEnum.eCanceled;
		}

		return super.onActivate(e);
	}

	protected void setPartSelection()
	{
		IBasePartsBrowser icdBrowser = ICDBrowserActionHelper.getICDBrowser();
		if (icdBrowser != null) {
			partSelection = icdBrowser.getPartSelection();
		}
	}

	protected boolean getWithConductor()
	{
		return m_addPinListDialog.getWithConductor();
	}

	@Override public boolean onTerminate(boolean successful)
	{
		boolean success;
		try {
			success = super.onTerminate(successful);
			saveorRevertSharedPinlistMgr(success);
			if (success) {
				ISchemDiagram diagram = getDiagram();
				if (m_schemDevice != null && diagram != null && m_icd != null) {
					IPinList connectivity = m_schemDevice.getConnectivity();
					if (connectivity.isShared()) {
						if (newlyCreatedSharedPinlist != null) {
							ICDPlacementHelper.updateICDNameAndRouting(m_schemDevice, partSelection, diagram, getWithConductor());
							newlyCreatedSharedPinlist.save();
						}
						else {
							ICDPlacementHelper.updateICDRouting(m_schemDevice, m_icd, diagram, getWithConductor());
						}
					}
				}
			}
		}
		finally {
			clean();
		}
		return success;
	}

	private void assignLibraryAndSymbolDetailsToSPL(@NotNull ISharedDevice sharedDevice, @NotNull IDeviceICD icd)
	{
		if (partSelection == null) {
			return;
		}
		ILibraryDeviceFootprint footprint = partSelection.getSelectedFootprint();
		if (footprint != null) {
			sharedDevice.setFootprintDescription(footprint.getFootprintName());
			sharedDevice.setFootprintId(footprint.getUID());
			Generator.syncSharedDeviceConnectors(sharedDevice, FactoryMgr.getCommonFactory(),
					FootprintUtils.getLibraryDeviceFootprintContext(sharedDevice, sharedDevice.getProject()),
					(dc, dcName) -> {});
		}
		else {
			IICDDeviceFootprint deviceFootprint = icd.getICDUsageDefinition().getDeviceFootprint();
			if (deviceFootprint != null) {
				Generator.syncSharedDeviceConnectors(sharedDevice, FactoryMgr.getCommonFactory(),
						FootprintUtils.getICDDeviceFootprintContext(sharedDevice, icd), (dc, dcName) -> {});
			}
		}

		ILibraryObject libraryObject = partSelection.getSelectedObject();
		if (libraryObject == null || libraryObject instanceof TransientLibraryDevice) {
			return;
		}
		PropertyCopier.copyCavityAttributes(sharedDevice, libraryObject);
		sharedDevice.assignLibraryDetails(partSelection);
		SharedPinListHelper.addLibraryPartAssociatedSymbolToSPL(libraryObject, sharedDevice);
	}

	private void clean()
	{
		resetMemberData();
		m_icd = null;
		m_schemDevice = null;
		sharedPinListToUse = null;
		newlyCreatedSharedPinlist = null;
		partSelection = null;
	}

	@Nullable public IDeviceICD getICD()
	{
		return m_icd;
	}

	@Override protected boolean allowPinListSelection()
	{
		//Only place the selected icd
		return false;
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}

	@Nullable private ISharedPinList getExistingSharedObjectOrCreateOneIfRequired()
	{
		ILogicDesign design = getLocalModel().getDesign();
		SharedObjectWithState sharedObjectWithState = ICDSharedObjectUtils.getSharedPinList(m_icd, design);
		if (isSharedObjectPresent(sharedObjectWithState)) {
			return sharedObjectWithState.getSharedPinList();
		}

		IProject currentProject = getCurrentProject();
		if (currentProject == null || m_icd == null) {
			return null;
		}

		// the project does not have a shared pinlist corresponding to the selected ICD... so create one
		ISharedPinListMgr sharedPinListMgr = currentProject.getSharedPinListMgr();
		if (sharedPinListMgr.lock()) {
			// lock must have refreshed... check if a shared pinlist with this name has been created
			sharedObjectWithState = ICDSharedObjectUtils.getSharedPinList(m_icd, design);
			if (isSharedObjectPresent(sharedObjectWithState)) {
				sharedPinListMgr.unlock();
				return sharedObjectWithState.getSharedPinList();
			}
			// nothing has been created... so create now
			newlyCreatedSharedPinlist = createSharedPinListFromICD(design, sharedPinListMgr);
			return newlyCreatedSharedPinlist;
		}
		else {
			String resourceKeyRoot = "AddSharedICDAction.pinlistMgrLocked";
			LockException exception = sharedPinListMgr.getLockException();
			if (exception != null && PermissionHelper.isPermissionIssueInExceptionMessage(exception.aError)) {
				resourceKeyRoot = "AddSharedICDAction.noPermission";
			}

			ResourceBasedMessageContent content = new ResourceBasedMessageContent(AddSharedICDAction.class,
					resourceKeyRoot);
			Message.show(PromptSeverity.ERROR, content);
			return null;
		}
	}

	private boolean isSharedObjectPresent(SharedObjectWithState sharedObjectWithState)
	{
		SharedObjectWithState.SharedState sharedPinlistState = sharedObjectWithState.getSharedPinlistState();
		if (sharedPinlistState == SharedObjectWithState.SharedState.PRESENT) {
			return true;
		}

		if (sharedPinlistState == SharedObjectWithState.SharedState.NOT_PRESENT) {
			return false;
		}

		String resourceKeyRoot = "AddSharedICDAction.multipleMatches";
		if (sharedPinlistState == SharedObjectWithState.SharedState.MULTIPLE_REVISIONS_PRESENT_IN_ACTIVEBL) {
			// log multiple revisions in active BL error
			resourceKeyRoot = "AddSharedICDAction.multipleMatchesPresentInActiveBL";
		}
		else if (sharedPinlistState == SharedObjectWithState.SharedState.MULTIPLE_REVISIONS_PRESENT_OUTSIDE_ACTIVEBL) {
			// log no revision in active BL error
			resourceKeyRoot = "AddSharedICDAction.multipleMatchesPresentOutsideActiveBL";
		}

		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(AddSharedICDAction.class, resourceKeyRoot);
		Message.show(PromptSeverity.ERROR, content);
		return true;
	}

	@NotNull
	private ISharedPinList createSharedPinListFromICD(@NotNull ILogicDesign design, ISharedPinListMgr sharedPinListMgr)
	{
		ISharedDevice sharedPinList = FactoryMgr.getSharedFactory().createSharedDevice(FactoryMgr.createUID());
		sharedPinList.setName(m_icd.getRole());
		sharedPinList.setDesignAbstraction(design.getDesignAbstraction());
		for (String pinName : ICDUtils.getICDPins(m_icd)) {
			ISharedPin sharedPin = FactoryMgr.getSharedFactory()
					.createSharedPinForOwner(FactoryMgr.getCommonFactory().createUID(), sharedPinList);
			sharedPin.setName(pinName);
			sharedPin.setReservationType(ISharedPin.ReservationType.AUTOMATIC);
			sharedPinList.addPin(sharedPin);
		}
		assignLibraryAndSymbolDetailsToSPL(sharedPinList, m_icd);
		freezeIfRequired(sharedPinList, design);
		sharedPinListMgr.addSharedPinList(sharedPinList);
		return sharedPinList;
	}

	private void freezeIfRequired(@NotNull ISharedPinList sharedPinList, @NotNull ILogicDesign design)
	{
		IReleaseLevel releaseLevel = design.getReleaseLevel();
		if (releaseLevel != null) {
			sharedPinList.setFrozen(releaseLevel.isFrozenSharedObjectsRequired());
		}
	}

	private void saveorRevertSharedPinlistMgr(boolean success)
	{
		IProject currentProject = getCurrentProject();
		if (currentProject != null) {
			ISharedPinListMgr sharedPinListMgr = currentProject.getSharedPinListMgr();
			if (newlyCreatedSharedPinlist != null) {
				if (!success) {
					sharedPinListMgr.removeSharedPinList(newlyCreatedSharedPinlist);
					newlyCreatedSharedPinlist.delete();
				}
				sharedPinListMgr.saveAndUnlock();
				sharedPinListMgr.fireChangeEvent();
			}
		}
	}

	private boolean canProceedWithAction(@NotNull ISharedPinList sharedPinList)
	{
		if (!ICDUtils.arePinNamesMatching(sharedPinList, m_icd)) {
			// fail if pin names don't match
			String resourceKeyRoot = "AddSharedICDAction.pinsMismatch";
			ResourceBasedMessageContent content = new ResourceBasedMessageContent(AddSharedICDAction.class,
					resourceKeyRoot);
			Message.show(PromptSeverity.ERROR, content);
			return false;
		}
		return true;
	}
}
