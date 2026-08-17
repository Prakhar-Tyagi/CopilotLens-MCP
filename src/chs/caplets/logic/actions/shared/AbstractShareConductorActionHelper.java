package chs.caplets.logic.actions.shared;

import chs.cof.draw.IGrid;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.common.IUID;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utility.DiagramHelper;
import chs.utility.PortHelper;
import chs.utility.helpers.SharedConductorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractShareConductorActionHelper
		extends AbstractBaseShareConductorActionHelper<ISharedConductor>
{

	protected AbstractShareConductorActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram)
	{
		super(design, diagram, SharedConductorHelper.getShareHelper());
	}

	@Override protected Iterator<ISharedConductor> getShareObjectsUsedOnDesign()
	{
		return m_design.getSharedConductors();
	}

	@NotNull @Override protected Set<ISharedConductor> getOrderedSharedConductors(@NotNull ISharedConductorMgr condMgr)
	{
		//dts0100874550 this set must be ordered because we want design used shared cond to come first.
		Set<ISharedConductor> shCondOrderedSet = new LinkedHashSet<>();
		shCondOrderedSet.addAll(CollectionUtils.createList(getShareObjectsUsedOnDesign()));
		shCondOrderedSet.addAll(CollectionUtils.createList(condMgr.getSharedConductors()));
		shCondOrderedSet.addAll(CollectionUtils.createList(condMgr.getFunctionalSharedMessages()));
		return shCondOrderedSet;
	}

	@Override protected boolean hasDuplicateName(@NotNull ISharedConductor sharedObject)
	{
		chs.cof.logical.cable.IConductor conductor = (chs.cof.logical.cable.IConductor) m_logicObject;
		return sharedObject.getName().equalsIgnoreCase(conductor.getName()) &&
				sharedObject.getType().equals(conductor.getType());
	}

	protected void transferConnectivity(ILogicObject logicObject)
	{
		if (m_logicObject instanceof chs.cof.logical.cable.IConductor &&
				logicObject instanceof chs.cof.logical.cable.IConductor) {
			transferConnectivityForConductor((chs.cof.logical.cable.IConductor) m_logicObject,
					(chs.cof.logical.cable.IConductor) logicObject);
		}
	}

	public static void transferConnectivityForConductor(@NotNull chs.cof.logical.cable.IConductor srcConductor,
			@NotNull chs.cof.logical.cable.IConductor targetConductor)
	{
		//pin connections
		final Set<? extends chs.cof.logical.cable.IPinList> centerStripSplices =
				getCenterStrippedSplices(targetConductor);
		srcConductor.getPinSet().stream()
				.filter(pin -> !centerStripSplices.contains(pin.getOwner()))
				.forEach(targetConductor::addPin);
		srcConductor.removeAllPins();
		//highways
		if (srcConductor instanceof IHighwayConductor && targetConductor instanceof IHighwayConductor) {
			IHighwayConductor highwayConductor = (IHighwayConductor) srcConductor;
			Set<IGeneralHighway> highways = highwayConductor.getHighways();
			for (IGeneralHighway highway : highways) {
				highway.addConductor((IHighwayConductor) targetConductor);
			}
		}
		//splices
		if (srcConductor instanceof IWireConductor && targetConductor instanceof IWireConductor) {
			Set<ISplice> splices = getCenterStrippedSplices(srcConductor);
			for (ISplice splice : splices) {
				((IWireConductor) targetConductor).addCenterStripSplice(splice);
			}
		}
	}

	@NotNull
	private static Set<ISplice> getCenterStrippedSplices(@NotNull chs.cof.logical.cable.IConductor conductor)
	{
		Set<ISplice> centerStripSplices = Collections.emptySet();
		if (conductor instanceof IWireConductor) {
			IWireConductor wc = (IWireConductor) conductor;
			centerStripSplices = CollectionUtils.createSet(wc.getCenterStripSplices());
		}
		return centerStripSplices;
	}

	protected void reassignConnectivityForSchematic(IDiagramObject diagObj, ILogicObject logicObject)
	{
		if (diagObj instanceof IConductor && logicObject instanceof chs.cof.logical.cable.IConductor) {
			reassignConnectivityForSchematicConductor((IConductor) diagObj,
					(chs.cof.logical.cable.IConductor) logicObject);
		}
		else if (diagObj instanceof IHighwaySchematic && logicObject instanceof chs.cof.logical.cable.IConductor) {
			reassignConnectivityForSchematicHighway((IHighwaySchematic) diagObj,
					(chs.cof.logical.cable.IConductor) m_logicObject,
					(chs.cof.logical.cable.IConductor) logicObject);
		}
	}

	private void reassignConnectivityForSchematicHighway(@NotNull IHighwaySchematic schemHighway,
			@NotNull chs.cof.logical.cable.IConductor srcCableConductor,
			@NotNull chs.cof.logical.cable.IConductor targetCableConductor)
	{
		// Diagram representation of conductor is highway, so conductor must be added to some pins in the stacked pin
		// these pins will be connected to new conductor now
		IHighwaySchematic highway = schemHighway;
		Set<IUID> stackedPins = highway.getConnectedStackPins();
		for (IUID stackedPinUID : stackedPins) {
			ISchemStackPin stackedPin = UIDMgr.getObjectOfType(stackedPinUID, ISchemStackPin.class);
			assert stackedPin != null;
			Collection<IAbstractPin> pinsConnectedToCond = stackedPin.getConnectedPins(srcCableConductor);
			for (IAbstractPin cablePin : pinsConnectedToCond) {
				cablePin.removeConductor(srcCableConductor);
				srcCableConductor.removePin(cablePin);
				cablePin.addConductor(targetCableConductor);
				targetCableConductor.addPin(cablePin);
			}
		}
	}

	public static void reassignConnectivityForSchematicConductor(@NotNull IConductor schemConductor,
			@NotNull chs.cof.logical.cable.IConductor targetCableConductor)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(schemConductor);
		if (diagram == null) {
			return;
		}
		IGrid grid = diagram.getGrid();
		schemConductor.setConnectivity(targetCableConductor);
		// dts0100603717 - For logic, we now stop a wire with centre strip splices (CCS) from terminating
		// at its own CCS.
		Set<? extends chs.cof.logical.cable.IPinList> m_centerStripSplices =
				getCenterStrippedSplices(targetCableConductor);

		// Add the port graphics
		PortHelper.addPortGfx(schemConductor, grid.getGridSpacing());

		for (Object o : schemConductor.getPins()) {
			IPin pin = (IPin) o;
			IDiagramObject parentPinlist = pin.getParent();
			// m_centerStripSplices can be null if existingSharedCond an instanceof INetConductor
			//so null check introducd as per dts0100627708.
			if (parentPinlist instanceof IPinList &&
					m_centerStripSplices.contains(((IConnectivityRef) parentPinlist).getConnectivity())) {
				IJoint joint = pin.getJoint();
				if (joint != null) {
					for (ISegment seg : schemConductor.getObjects(ISegment.class)) {
						if (seg.getStartNode() == joint) {
							seg.setStartNode(null);
						}
						else if (seg.getEndNode() == joint) {
							seg.setEndNode(null);
						}
					}
				}
			}
		}
	}
}
