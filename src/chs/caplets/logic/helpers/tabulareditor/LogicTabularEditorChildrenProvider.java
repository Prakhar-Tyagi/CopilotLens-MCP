/*
* Copyright 2017 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic.helpers.tabulareditor;

import chs.caplets.logic.helpers.ILogicObjectChildrenProvider;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author pbhawsar on 19-05-2017
 */
public class LogicTabularEditorChildrenProvider implements ILogicObjectChildrenProvider
{

	@NotNull public List<IUID> getChildren(@NotNull IPinList pinList)
	{
		IDeviceConnector deviceConnector = CommonUtils.cast(pinList, IDeviceConnector.class);
		if (deviceConnector != null) {
			IBackshell backshell = deviceConnector.getBackshell();
			if (backshell != null) {
				return Collections.singletonList(backshell.getUID());
			}
			return Collections.emptyList();
		}

		Stream<? extends IUIDObject> childrenStream = getPins(pinList);

		IDevice device = CommonUtils.cast(pinList, IDevice.class);
		if (device != null) {
			childrenStream = Stream.concat(childrenStream, getDeviceConnectors(device));
			return toUIDList(childrenStream);
		}

		IConnector connector = CommonUtils.cast(pinList, IConnector.class);
		if (connector != null) {
			childrenStream = Stream.concat(childrenStream, getConnectorChildren(connector));
		}

		return toUIDList(childrenStream);
	}

	private Stream<? extends IUIDObject> getConnectorChildren(@NotNull IConnector connector)
	{
		Stream<? extends IUIDObject> connectorChildren = Stream.empty();
		if (connector.isModularParent()) {
			connectorChildren = connector
					.getChildConnectors()
					.stream();
		}
		final IBackshell backshell = connector.getBackshell();
		if (backshell != null) {
			connectorChildren = Stream.concat(connectorChildren, Stream.of(backshell));
		}

		return connectorChildren;
	}

	private Stream<? extends IUIDObject> getDeviceConnectors(@NotNull IDevice device)
	{
		if (device.getNumDeviceConnectors() > 0) {
			return device.getDeviceConnectors().stream();
		}
		return Stream.empty();
	}

	@NotNull private Stream<? extends IUIDObject> getPins(@NotNull IPinList pinList)
	{
		return pinList.getPinCollection().stream();
	}

	@NotNull public List<IUID> getChildren(@NotNull IGeneralHighway highway)
	{
		return toUIDList(highway.getAllConductors().stream());
	}

	@NotNull public List<IUID> getChildren(@NotNull IAssembly assembly)
	{
		return toUIDList(assembly.getElements().stream());
	}

	@NotNull public List<IUID> getChildren(@NotNull IMulticore multicore)
	{
		final Stream<? extends IUIDObject> childMulticoreStream = multicore.getMulticores().stream();
		final Stream<? extends IUIDObject> conductorStream = multicore.getConductorsIncludingShields().stream();

		Stream<? extends IUIDObject> childStream = Stream.concat(childMulticoreStream, conductorStream);

		return toUIDList(childStream);
	}

	@NotNull public List<IUID> getChildren(@NotNull ISchemStackPin schemStackPin)
	{
		// get the pins for this representation
		return toUIDList(schemStackPin.getAllConnectivity().stream());
	}

	@NotNull private IUID getUID(@NotNull IUIDObject uidObject)
	{
		return uidObject.getUID();
	}

	@NotNull List<IUID> toUIDList(@NotNull Stream<? extends IUIDObject> stream)
	{
		return stream
				.map(this::getUID)
				.collect(Collectors.toList());
	}
}
