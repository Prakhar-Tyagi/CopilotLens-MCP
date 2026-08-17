/*
 * Copyright 2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.IMultipleConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.common.ICommonFactory;
import chs.common.ILocation;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.FactoryMgr;
import chs.utilities.Pair;
import chs.utilities.ReverseMap;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.SegmentHelper;
import chs.view.assist.AbstractConnectionCreator;
import chs.view.assist.AssistDiagramGenerator;
import chs.view.assist.IConnectionVisitor;
import chs.view.assist.IPinInfo;
import chs.view.assist.PinInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ObjectConnectionsGetter
{

	private static boolean m_isAssistedFlowEnabled = true;

	private ObjectConnectionsGetter()
	{
	}

	public static void createTransientGraphics(@NotNull Collection<String> pinNames, @NotNull ISchemDiagram diagram,
			IPinInfo pinInfo, @NotNull IDynamicGfxService dynamicGfxService,
			@NotNull List<IDynamicGfx> transientGfxs)
	{
		if (isIsAssistedFlowEnabled()) {
			AssistDiagramGenerator getter = new AssistDiagramGenerator();
			IConnectionVisitor transientGfxCreator =
					new TransientGraphicsCreator(transientGfxs, dynamicGfxService, diagram);
			getter.visitConnections(pinNames, pinInfo, diagram, transientGfxCreator);
		}
	}

	public static void createConnectionSchematics(@NotNull IPinList currentSchematic, @NotNull ISchemDiagram diagram)
	{

		if (isIsAssistedFlowEnabled()) {
			createConnectionSchematics(getPinNames(currentSchematic), currentSchematic, diagram);
		}
	}

	@NotNull private static List<String> getPinNames(@NotNull IPinList currentSchematic)
	{
		List<String> pinNames = new ArrayList<>();
		for (IAbstractSchemPin schemPin : currentSchematic.getAllPins()) {
			if (schemPin instanceof ISchemStackPin) {
				for (ILogicObject cablePin : ((IMultipleConnectivityRef) schemPin).getAllConnectivity()) {
					pinNames.add(cablePin.getName());
				}
			}
			else if (schemPin instanceof IPin) {
				pinNames.add(((IConnectivityRef) schemPin).getConnectivity().getName());
			}
		}
		return pinNames;
	}

	public static void createConnectionSchematics(@NotNull List<String> pinNames, @NotNull IPinList schematicPinList,
			@NotNull ISchemDiagram diagram)
	{
		if (isIsAssistedFlowEnabled()) {
			AssistDiagramGenerator connectionsGetter = new AssistDiagramGenerator();
			PinInfo pinInfo = new PinInfo(schematicPinList);
			connectionsGetter
					.createConnectionSchematics(pinNames, pinInfo, diagram, ConductorRouteAction.getInstance());
		}
	}

	public static void createBackshellConnectionSchematics(@NotNull List<String> pinNames,
			@NotNull ISchemDiagram diagram, @NotNull IPinInfo backshellPinInfo)
	{
		if (isIsAssistedFlowEnabled()) {
			AssistDiagramGenerator connectionsGetter = new AssistDiagramGenerator();
			connectionsGetter
					.createConnectionSchematics(pinNames, backshellPinInfo, diagram,
							ConductorRouteAction.getInstance());
		}
	}

	public static void createConnectionSchematics(@NotNull AddPinActionModel pinActionModel,
			@NotNull ISchemDiagram diagram)
	{
		if (isIsAssistedFlowEnabled()) {
			List<String> pinNames = new ArrayList<>();
			for (IPinList pinList : pinActionModel.getPinLists()) {
				Set<IAbstractSchemPin> pins = pinActionModel.getPins(pinList);
				for (IAbstractSchemPin schemPin : pins) {
					addCablePinName(schemPin, pinNames);
				}
				createConnectionSchematics(pinNames, pinList, diagram);

				//Get Connector pins mapped to device pins.
				Collection<IPinList> attachedPinListObjects =
						pinList.getAttachedPinListObjects();
				for (IPinList attachedPinList : attachedPinListObjects) {
					List<String> connPinNames = new ArrayList<>();
					addConnectorPinNames(pinActionModel, pinList, attachedPinList, connPinNames);
					createConnectionSchematics(connPinNames, attachedPinList, diagram);
				}

				for (IPinList matedSchemPinList : pinActionModel.getMatePinLists(pinList)) {
					List<String> matePinNames = new ArrayList<>();
					for (IAbstractSchemPin schemPin : pins) {
						IAbstractSchemPin matedSchemPin = pinActionModel.getMatedPin(schemPin);
						addCablePinName(matedSchemPin, matePinNames);
					}
					createConnectionSchematics(matePinNames, matedSchemPinList, diagram);
				}
			}
		}
	}

	private static void addConnectorPinNames(AddPinActionModel pinActionModel, IPinList pinList,
			IPinList attachedPinList, List<String> connPinNames)
	{
		chs.cof.logical.cable.IPinList connectivity = attachedPinList.getConnectivity();
		if (connectivity instanceof IHarnessPlugConnector) {
			ConnectionHelper chelper = ConnectionHelper.getConnectionHelper(pinList, attachedPinList);
			if (chelper == null) {
				return;
			}
			Set<IAbstractSchemPin> pins = pinActionModel.getPins(pinList);
			for (IPin connSchemPin : attachedPinList.getPins()) {
				IPin deviceSchemPin = chelper.getConnectedPin(connSchemPin);
				if (deviceSchemPin != null && pins.contains(deviceSchemPin)) {
					addCablePinName(connSchemPin, connPinNames);
				}
			}
		}
	}

	private static void addCablePinName(@Nullable IAbstractSchemPin schemPin, @NotNull List<String> pinNames)
	{
		if (schemPin instanceof ISchemStackPin) {
			for (ILogicObject cablePin : ((IMultipleConnectivityRef) schemPin).getAllConnectivity()) {
				pinNames.add(cablePin.getName());
			}
		}
		else if (schemPin instanceof IPin) {
			pinNames.add(((IConnectivityRef) schemPin).getConnectivity().getName());
		}
	}

	public static boolean isIsAssistedFlowEnabled()
	{
		return m_isAssistedFlowEnabled;
	}

	public static void setIsAssistedFlowEnabled(boolean isAssistedFlowEnabled)
	{
		m_isAssistedFlowEnabled = isAssistedFlowEnabled;
	}

	private static class TransientGraphicsCreator extends AbstractConnectionCreator
	{

		@NotNull private List<IDynamicGfx> m_transientGfxs;
		@NotNull private IDynamicGfxService m_dynamics;
		@NotNull private ReverseMap<ILocation, ILocation> m_dynamicLinesCreated = new ReverseMap<>();

		private TransientGraphicsCreator(@NotNull List<IDynamicGfx> transientGfxs,
				@NotNull IDynamicGfxService dynamicGfxService, @NotNull ISchemDiagram diagram)
		{
			super(diagram);
			m_transientGfxs = transientGfxs;
			m_dynamics = dynamicGfxService;
		}

		@Override
		public void conductorConnectionWithNoRep(@NotNull IConductor conductor,
				@NotNull List<IAbstractSchemPin> otherSchemPins,
				@NotNull ILocation originatingSchemPinLoc, IAbstractSchemPin originatingSchemPin)
		{
			ILocation currentDevPinAbsLocation = originatingSchemPinLoc;

			for (IAbstractSchemPin otherSchemPin : otherSchemPins) {
				ILocation placedDevPinAbsLocation = otherSchemPin.getAbsLocation();
				IGrid grid = m_diagram.getGrid();
				ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
				ILocation snappedFrom = commonFactory.constructLocation(grid.snap(currentDevPinAbsLocation.getX()),
						grid.snap(currentDevPinAbsLocation.getY()));
				ILocation snappedTo = commonFactory.constructLocation(grid.snap(placedDevPinAbsLocation.getX()),
						grid.snap(placedDevPinAbsLocation.getY()));
				addToDynGraphics(snappedFrom, snappedTo);
			}
		}

		private void addToDynGraphics(@NotNull ILocation from, @NotNull ILocation to)
		{
			ILocation otherLocation = m_dynamicLinesCreated.get(from);
			if (otherLocation == null) {
				otherLocation = m_dynamicLinesCreated.getKey(from);
			}
			if (otherLocation != null && otherLocation.equals(to)) {
				return;
			}

			IDynamicGfx iDynamicGfx = m_dynamics.getFactory().constructLine(
					new Point(from.getX(), from.getY()), new Point(to.getX(), to.getY()), new Point(0, 0), true);
			m_transientGfxs.add(iDynamicGfx);
			m_dynamics.addTransientGfx(iDynamicGfx);
			m_dynamicLinesCreated.put(from, to);
		}

		@Override public void conductorConnectionWithRep(@NotNull IConductor cableConductor,
				@NotNull List<? extends ILogicSegmentContainer> schematicConds,
				@NotNull ILocation originatingSchemPinLoc, @Nullable IAbstractSchemPin originatingSchemPin,
				boolean hasUnconnectedPins, List<IAbstractSchemPin> schemPinsToConnect)
		{
			//Not to show transient graphics for already created schematics.-dts0101150214
			if (schemPinsToConnect.isEmpty() && !hasUnconnectedPins) {
				return;
			}

			connectToExistingSegment(schematicConds, originatingSchemPinLoc, false);
		}

		private boolean connectToExistingSegment(List<? extends ILogicSegmentContainer> schematicConds,
				ILocation originatingSchemPinLoc, boolean connectOnlyToOpenSegment)
		{
			if (!schematicConds.isEmpty()) {
				Pair<IBaseSegment, ? extends ILogicSegmentContainer> baseSegmentPair =
						getAppropriateSegmentToConnect(schematicConds, originatingSchemPinLoc,
								connectOnlyToOpenSegment);

				IBaseSegment candidateSegment = baseSegmentPair.getFirst();
				if (candidateSegment != null) {
					IGrid grid = m_diagram.getGrid();
					ILocation snappedOriginatingPinLoc = FactoryMgr.getCommonFactory()
							.constructLocation(grid.snap(originatingSchemPinLoc.getX()),
									grid.snap(originatingSchemPinLoc.getY()));
					addToDynGraphics(SegmentHelper.getAppropriatePointSnappedToGrid(candidateSegment),
							snappedOriginatingPinLoc);
					return true;
				}
			}
			return false;
		}

		@Override
		public void shieldConnection(@NotNull IShieldConductor shield,
				@NotNull ILocation originatingSchemPinLoc, IAbstractSchemPin originatingSchemPin)
		{
			IShieldBodyHookup shieldHookup = getNearestShieldHookup(shield, originatingSchemPinLoc, m_diagram);
			if (shieldHookup == null) {
				return;
			}

			List<chs.cof.logical.schem.IConductor> conductorsList = new ArrayList<>(shieldHookup.getShieldConductors());
			if (!connectToExistingSegment(conductorsList, originatingSchemPinLoc, true)) {
				addToDynGraphics(originatingSchemPinLoc, shieldHookup.getAbsLocation());
			}
		}

		@Override public void ensureMulticores()
		{

		}

		@Override public void ensureCenterStrippedSplices()
		{

		}

		@NotNull @Override public Set<IMulticore> getGeneratedMulticores()
		{
			return Collections.emptySet();
		}

		@NotNull @Override public Set<IPinList> getGeneratedSplices()
		{
			return Collections.emptySet();
		}
	}
}
