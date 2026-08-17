/*
 * Copyright 2002-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.helpers.ui.std.UIManager;
import chs.caplets.logic.actions.shared.SharedObjectAvailabilityReporter;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.utilities.CommonUtils;
import chs.utility.DiagramHelper;
import chs.utility.ICDUtils;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.PinListHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.Set;

/**
 * Add Pin Action.
 * <p>
 * Adds one or more pins to a single, selected schematic pinlist instance.  This may be multiple schem instances of
 * existing connectivity pins, or a single new connectivity+schematic pin.
 */
public class AddPinAction extends AbstractAddPinAction implements IICDProviderAction
{
	private boolean withConductor;

	public AddPinAction(ICapletController controller)
	{
		super(controller);
		setupActionHelper();
	}

	protected void setupActionHelper()
	{
		m_addPinActionPresenter = new AddPinActionHelper(this, false, true);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (!initAddPinModel()) {
			return IActionEnum.eCanceled;
		}
		boolean altPress = (e.getSource() instanceof UIManager) && (e.getModifiers() & InputEvent.ALT_MASK) != 0;
		boolean shiftNotPressed =
				(!(e.getSource() instanceof UIManager) && !((e.getModifiers() & InputEvent.SHIFT_MASK) != 0))
						|| isBlockDeviceSelected();
		boolean retState = initializePresenter(altPress, shiftNotPressed);

		return retState ? IActionEnum.eActivated : IActionEnum.eCanceled;
	}

	protected boolean initializePresenter(boolean altPress, boolean shiftNotPressed)
	{
		return m_addPinActionPresenter.initialize(m_addPinActionModel, altPress, shiftNotPressed);
	}

	protected boolean initAddPinModel()
	{
		// show a dialog to select the pins
		IPinList pinList = getOperand(getController().getSelectMgr().getPreSelections());
		if (pinList != null) {
			chs.cof.logical.cable.IPinList connectivity = pinList.getConnectivity();
			if (!LogicObjectLockFinder.tryEdit(connectivity)) {
				return false;
			}
			ISharedObject sharedObject = connectivity.getSharedObject();
			if(sharedObject != null) {
				final IDesign logicDesign = connectivity.getLogicDesign();
				// Cancel 'Add Pin' action on a shared object (all types) instance which is restricted to current user
				final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
				if (!new SharedObjectAvailabilityChecker().check(sharedObject, logicDesign, reporter, false)) {
					return false;
				}
			}

			m_addPinActionModel = setupPinActionModel(pinList);
			return true;
		}
		return false;
	}

	@NotNull protected AddPinActionModel setupPinActionModel(@NotNull IPinList pinList)
	{
		if (pinList.getConnectivity() instanceof IBlockDevice) {
			 return new AddBlockPinActionModel(pinList);
		}
		return new AddPinActionModel(pinList);
	}

	public boolean onTerminate(boolean successful)
	{
		boolean itWorked = false;
		if (m_addPinActionModel != null) {
			withConductor = m_addPinActionPresenter.isWithConductor();
			itWorked = m_addPinActionPresenter.execute(successful);
			doOnTerminate(successful, itWorked);
		}

		cleanUp();

		return itWorked;
	}

	protected void doOnTerminate(boolean successful, boolean itWorked)
	{
		if (successful) {
			for (IPinList pinList : m_addPinActionModel.getPinLists()) {
				Set<IAbstractSchemPin> pins = m_addPinActionModel.getPins(pinList);
				updateICDRouting(pinList, pins::contains);
			}
		}
	}

	private void updateICDRouting(IPinList pinList, @NotNull IObjectFilter<IPin> pinFilter)
	{
		if (pinList != null) {
			IDeviceICD icd = getICD(pinList);
			m_addPinActionPresenter.updateICDRouting(pinList, icd, pinFilter, withConductor);
		}
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return AddPinActionUI.class.getName();
	}

	@Nullable
	protected IPinList getOperand(SelectSet selections)
	{
		IPinList pinList1 = null;
		IPinList pinList2 = null;

		int plCount = 0;
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();

			if (uidObj instanceof IPinList) {
				IPinList pl = (IPinList) uidObj;
//				if (pl.getConnectivity() instanceof IBlockDevice && !Environment.isDesignHierarchyEnabled()) {
//					//these lines should be removed when environment variable is disposed.
//					return null;
//				}
				if (pl.getParameterized() != null
						&& !(pl.getConnectivity() instanceof ISplice)
						&& !(pl.getConnectivity() instanceof IDeviceConnector)) {

					//Pins can not be added to the Ring Terminal type connectors
					if (IConnector.Statics.isRingTerminalTypeConnector(pl) && !pl.getPins().isEmpty()) {
						continue;
					}

					plCount++;
					if (plCount == 1) {
						pinList1 = (IPinList) uidObj;
					}
					else if (plCount == 2) {
						pinList2 = (IPinList) uidObj;
					}
					else {
						break;
					}
				}
			}
		}

		IPinList operand;
		if (plCount == 1) {
			operand = pinList1;
		}
		else if (plCount == 2
				&& pinList1 != null && pinList1.getConnectivity() instanceof IConnector
				&& pinList2.getConnectivity() instanceof IConnector
				&& ((IConnector) pinList1.getConnectivity()).getMates().contains(pinList2.getConnectivity())) {
			if (pinList1.getConnectivity() instanceof IInlinePlugConnector) {
				operand = pinList1;
			}
			else {
				operand = pinList2;
			}
		}
		else {
			return null;
		}

		//melmorsy - FEAT12331
		//If this is an automatically generated harness connector, then it doesn't support pin modification
		if (!PinListHelper.isEditableHarnessConnector(operand)) {
			return null;
		}

		// Add Pin currently expects the pinlist to be on the active diagram
		// This may change if we do the "Enhanced Pin Placement" part of FEAT13040 but for now we just disable the action
		if (DiagramHelper.getDiagram(operand) != CAFUtils.getInstance().getActiveDiagram()) {
			return null;
		}

		chs.cof.logical.cable.IPinList cpl = operand.getConnectivity();
		final ISharedPinList spl = cpl.getSharedPinList();
		if (spl != null && spl.getPins().getSize() == 0 || cpl instanceof IInterconnectObject
				&& (cpl instanceof IConnector
				|| (cpl instanceof IDevice && cpl.getSharedPinList() == null))) {
			return null;
		}
		return operand;
	}

	private boolean isBlockDeviceSelected()
	{
		IPinList pinList = getOperand(getController().getSelectMgr().getPreSelections());
		chs.cof.logical.cable.IPinList connectivity = (pinList != null) ? pinList.getConnectivity() : null;
		return connectivity instanceof IBlockDevice;
	}

	@Nullable @Override public IDeviceICD getICD()
	{
		return getICD(null);
	}

	@Nullable public IDeviceICD getICD(@Nullable IPinList pl)
	{
		IPinList pinList = pl;
		if (pinList == null) {
			pinList = getOperand(getController().getSelectMgr().getPreSelections());
		}
		IDevice device =
				CommonUtils.cast((pinList != null) ? pinList.getConnectivity() : null, IDevice.class);
		if (device != null) {
			ILogicDesign logicDesign = device.getLogicDesign();
			assert logicDesign != null;
			IProject project = logicDesign.getProject();
			assert project != null;
			IBuildList activeBuildList = project.getBuildListMgr().getActiveBuildList();
			Set<IDeviceICD> matchingICDs = ICDUtils.getMatchingICDs(device, logicDesign, activeBuildList, true);
			if (!matchingICDs.isEmpty()) {
				return matchingICDs.iterator().next();
			}
		}
		return null;
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}
}




