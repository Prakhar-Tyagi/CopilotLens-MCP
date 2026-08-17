/*
 * Copyright 2004-2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.ISheet;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.parameterized.BSPinPlacementConstraints;
import chs.cofUtils.parameterized.DSCHelper;
import chs.cofUtils.parameterized.PinPlacementConstraints;
import chs.common.IUIDObject;
import chs.services.gfx.GfxView;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ReferenceHelper;
import chs.view.assist.BackshellPinInfo;
import chs.view.assist.IPinInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class AddBSTerminationActionHelper extends AddPinActionHelper
{

	private boolean hasTempPlaceHolders;

	public AddBSTerminationActionHelper(ControllerActionRT actionRT)
	{
		super(actionRT, true, true);
	}

	@Override @Nullable
	protected chs.cof.logical.cable.IPinList getConnectivityPinOwner(chs.cof.logical.cable.IPinList cablePL)
	{
		return ((IConnector) cablePL).getBackshell();
	}

	protected boolean isPlacingBackshellTerminations()
	{
		return true;
	}

	@Override public void createConnectionSchematics(@NotNull IPinList pinList)
	{
		List<AddPinArgs> pinsToAdd = getPinsCommitedToPlace(pinList);
		if (pinsToAdd.isEmpty()) {
			return;
		}

		ISchemDiagram diagram = DiagramHelper.getDiagram(pinList);
		if (diagram == null) {
			return;
		}

		List<String> pinNames = new ArrayList<>();
		for (AddPinArgs addPinArg : pinsToAdd) {
			@Nullable IAbstractPin pin = addPinArg.getPin();
			String pinName = pin != null ? pin.getName() : addPinArg.getName();
			pinNames.add(pinName);
		}
		//dts0101168650 - Do backshell termination connection only
		IPinInfo pinInfo = new BackshellPinInfo(pinList);
		ObjectConnectionsGetter.createBackshellConnectionSchematics(pinNames, diagram, pinInfo);
	}

	@Override protected boolean addingBackshell()
	{
		return true;
	}

	public String getStatusbarText()
	{
		return ResourceMgr
				.getString(AddBackshellTerminationActionUI.class, "AddBackshellTerminationActionUI.placement.guidance");
	}

	protected void updateToolTipText(GfxView gview, Point worldPoint)
	{
	}

	@Nullable public IPinList getPinListThatAllowsBackshellAddition(@NotNull SelectSet selections)
	{
		IPinList pinList = null;
		int plCount = 0;
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof IPinList pl) {
				chs.cof.logical.cable.IPinList plc = pl.getConnectivity();
				if (!isBackshellAdditionSupported(plc)) {
					continue;
				}

				plCount++;
				if (plCount == 1) {
					pinList = pl;
				}
				else {
					break;
				}
			}
		}

		if (plCount != 1) {
			return null;
		}
		ISchemDiagram diagram = pinList.getDiagram();
		if (diagram != CAFUtils.getInstance().getActiveDiagram()) {
			return null;
		}
		ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(pinList);
		if (logicObject == null || LogicObjectLockFinder.isLogicObjectLockedInOtherSession(logicObject)) {
			return null;
		}
		return pinList;
	}

	/**
	 * Determines whether a backshell termination can be added to the given pinList's.
	 * <p>
	 *
	 * Backshell addition is supported only when all of the following conditions are met:
	 * <ul>
	 *   <li>The pinList is an {@link IConnector} (not just any pin list)</li>
	 *   <li>The connector is <strong>not</strong> an {@link IInterconnectObject}</li>
	 *   <li>The connector is <strong>not</strong> a Ring Terminal type connector</li>
	 *   <li>The connector is <strong>not</strong> a mated Device-Side Connector (DSC)</li>
	 * </ul>
	 *
	 * @param cablePinList the connectivity pinList to evaluate;
	 *                     may be {@code null}, in which case {@code false} is returned
	 * @return {@code true} if a backshell termination can be added; {@code false} otherwise
	 */
	private boolean isBackshellAdditionSupported(@Nullable chs.cof.logical.cable.IPinList cablePinList)
	{
		//Backshell is not allowed on Ring Terminal type Connectors
		return cablePinList instanceof IConnector connector &&
				!(cablePinList instanceof IInterconnectObject) &&
				!IConnector.Statics.isRingTerminalTypeConnector(cablePinList) &&
				!isDSCMated(connector);
	}

	/**
	 * Determines whether a Device-Side Connector (DSC) is mated — i.e.,
	 * has at least one device pin (associated to its child DSC pins) that is already connected to another pin.
	 * <p>
	 *
	 * A connector is considered "mated" if it is an {@link IDeviceConnector} and any of its {@link IDeviceConnPin} pins
	 * has a corresponding device pin with one or more connected pins.
	 * <p>
	 *
	 * Mated DSCs do not support backshell addition.
	 * <p>
	 *
	 * {@code filter(Objects::nonNull} is needed to - filter DSC pins not mated to device pins
	 * (eg. DSC generated from library footprint might contains additional DSC pins/ cavities,
	 * which haven't been mapped to device pins)
	 *
	 * @param connector the connector to evaluate;
	 * @return {@code true} if the connector is an {@link IDeviceConnector} with at least one mated device pin;
	 * {@code false} otherwise
	 */
	private boolean isDSCMated(@NotNull IConnector connector)
	{
		DSCHelper dscHelper = new DSCHelper();
		return connector instanceof IDeviceConnector dsc &&
				dscHelper.streamAssociatedDevicePins(dsc)
						.anyMatch(devicePin -> !devicePin.getConnectedPins().isEmpty());
	}

	@NotNull @Override protected AbstractPinActionHelper.PinPlacementController createPinPlacementController()
	{
		return new BSPinPlacementController();
	}

	@NotNull @Override
	protected PinPlacementConstraints getPinPlacementConstraints(@NotNull IPinList candidate,
			boolean includeBoundaryExtensions, ISheet sheet, IGfxContext context)
	{
		BSPinPlacementConstraints pinPlacementConstraints =
				new BSPinPlacementConstraintsBuilder(candidate, sheet)
						.addTempPlaceHolderForDevicesWithSymbols()
						.build(m_currentPin, includeBoundaryExtensions, context);
		// Once placeholders are created they are tracked so AbstractBackshellAction can remove them before commit.
		// todo - See if need to reset this flag after removal
		hasTempPlaceHolders = true;
		return pinPlacementConstraints;
	}

	@Override public boolean hasTempPlaceHolderForDevicesWithSymbols()
	{
		return hasTempPlaceHolders;
	}

	private static class BSPinPlacementController extends PinPlacementController
	{

		private BSPinPlacementController()
		{
		}

		@NotNull @Override protected PinListAddPinHelper getPinListAddPinHelper(IPinList pinlist, boolean isReference)
		{
			return new BackshellAddTerminationHelper(pinlist, isReference);
		}
	}
}
