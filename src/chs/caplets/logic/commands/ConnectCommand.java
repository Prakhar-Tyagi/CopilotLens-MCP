/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.commands;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.common.ILocation;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.logic.PinUtils;
import chs.view.utils.DiagramGenerationUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TODO: jacobt FEAT2081 : OK so this isn't a command - it's just a useful place to put some static utils
 */
public class ConnectCommand
{

	private ConnectCommand()
	{
	}

	/**
	 * Create and connect a conductor between each distinct pair of pins in the set.
	 *
	 * @param pins The set of pins
	 */
	public static void connectPins(Set<IPin> pins, ISchemDiagram diagram, @Nullable Class<? extends IConductor> condCls)
	{
		// usually it's just the 2 pins but...
		assert pins.size() > 1;
		Object[] pinarr = pins.toArray();
		if (condCls != null) {
			for (int i = 0; i < pinarr.length - 1; ++i) {
				for (int j = i + 1; j < pinarr.length; ++j) {
					connectPins((IPin) (pinarr[i]), (IPin) pinarr[j], diagram, condCls);
				}
			}
		}
		else {
			for (int i = 0; i < pinarr.length - 1; ++i) {
				for (int j = i + 1; j < pinarr.length; ++j) {
					final IPin pin0 = (IPin) pinarr[i];
					final IPin pin1 = (IPin) pinarr[j];
					connectDevicePins(pin0, pin1);
				}
			}
		}
	}

	private static void connectDevicePins(@NotNull IPin pin0, @NotNull IPin pin1)
	{
		if (!arePinsOfSameCablePinlist(pin0, pin1)) {
			final boolean connectByPinResult = ConnectionHelper.connectDevicePins(pin0, pin1);
			if (connectByPinResult) {
				logConnectedByPinMsg(pin0, pin1);
			}
		}
	}

	private static void logConnectedByPinMsg(@NotNull IPin pin0, @NotNull IPin pin1)
	{
		if (pin0.isDevicePin() && pin1.isDevicePin()) {
			final IDevicePin dPin0 = (IDevicePin) pin0.getConnectivity();
			final IDevicePin dPin1 = (IDevicePin) pin1.getConnectivity();
			final IPinList pin0Owner = dPin0.getOwner();
			final IPinList pin1Owner = dPin1.getOwner();

			if (pin0Owner != null && pin1Owner != null) {
				final String msg = ResourceMgr
						.getString(ConnectCommand.class, "ConnectCommand.ConnectByPin.connected.info",
								pin0Owner.getName(), dPin0.getName(), pin1Owner.getName(), dPin1.getName());
				LogHelper.appMsgSafe(msg);
			}
		}
	}

	private static boolean arePinsOfSameCablePinlist(@NotNull IPin pin0, @NotNull IPin pin1)
	{
		// check local connectivity
		final IAbstractPin aPin0 = pin0.getConnectivity();
		final IAbstractPin aPin1 = pin1.getConnectivity();

		assert aPin0 != null;
		assert aPin1 != null;

		return aPin0.getOwner() == aPin1.getOwner();
	}

	private static boolean arePinsOfSameSchemPinlist(@NotNull IPin pin0, @NotNull IPin pin1)
	{
		return pin0.getParent() == pin1.getParent();
	}

	/**
	 * Create and connect a conductor between 2 pins.
	 *
	 * @param pin0 Start pin
	 * @param pin1 End pin
	 */
	public static void connectPins(IPin pin0, IPin pin1, ISchemDiagram diagram, Class<? extends IConductor> condCls)
	{
		if (arePinsOfSameSchemPinlist(pin0, pin1)) {
			// It could be the case of symbol device with co-located pins e.g. terminal device
			return;
		}

		// we checked the attached conductors on all pins earlier
		assert connectionAllowed(pin0, condCls) : "Unexpected conductors attached to pin : " + pin0;
		assert connectionAllowed(pin1, condCls) : "Unexpected conductors attached to pin : " + pin1;

		// create the diagram + connectivity conductor
		List<ILocation> points = new ArrayList<ILocation>(2);
		points.add(DiagramGenerationUtilities.getNode(pin0));
		points.add(DiagramGenerationUtilities.getNode(pin1));
		chs.cof.logical.schem.IConductor cond = CreateSchemConductorCmd.createConductor(diagram, points, condCls);

		// connect the conductor endpoints to the pins
		ConnectionHelper.connect(pin0.getConnectivity(), cond.getConnectivity());
		ConnectionHelper.connect(pin1.getConnectivity(), cond.getConnectivity());

		assert cond.getSegments().size() == 1;
		// TODO jacobt FEAT2081 : why do these assertions fail?
//		assert(cond.isConnectedTo(pin0));
//		assert(cond.isConnectedTo(pin1));
	}

	/**
	 * Are we allowed to connect a conductor of this type to a pin?
	 *
	 * @param pin The pin
	 * @param condCls The class of Conductor that is allowed to be connected to this pin
	 *
	 * @return true iff no such conductors are attached.
	 */
	public static boolean connectionAllowed(IPin pin, @Nullable Class<? extends IConductor> condCls)
	{
		//Todo Moattia: if no condCls is null then pin should be connected by pin
		// a check should be made on connected connectors
		if (condCls == null) {
			if ((pin.getConnectivity() instanceof IDevicePin)) {
				// can connect by pin
				return !pin.isReference() && ((IDevice) pin.getConnectivity().getOwner()).acceptsDeviceMating();
			}
			else {
				return false;
			}
		}
		// check general rules for this type of conductor/pin
		if (!ConnectionHelper.isInterconnectionAllowed(condCls, pin)) {
			return false;
		}

		// check for existing connection of the "wrong" type of conductor
		for (Object obj : pin.getConductors()) {
			chs.cof.logical.schem.IConductor cond = (chs.cof.logical.schem.IConductor) obj;
			if (!condCls.isInstance(cond.getConnectivity())) {
				return false;
			}
		}

		// check for reference pin
		ISchemDiagram diag = DiagramHelper.getDiagram(pin);
		if (diag != null) {
			return PinUtils.isValidPinForConductorConnection(pin);
		}

		return true;
	}
}
