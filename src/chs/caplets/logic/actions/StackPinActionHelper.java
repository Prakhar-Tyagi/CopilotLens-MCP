/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2011-2024 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caplets.logic.DeleteHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.PinPlacementConstraintsHolder;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.cofUtils.parameterized.PinSideCalculator;
import chs.common.ICommonFactory;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.Side;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.NodeHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.helpers.SegmentHelperInfo;
import chs.utility.helpers.StackedPinHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 1 Mar, 2011 Time: 3:28:03 PM To change this template use File |
 * Settings | File Templates.
 */
public class StackPinActionHelper
{

	public enum MATING_STATE
	{

		MATING,    // If all selected pins are mated
		NON_MATING, // If all selected pins are non-mated
		MIX    // Some selected pins are mated and some are non-mated
	}

	private StackPinActionHelper()
	{
	}

	public static Set<IPinList> removeSchemPins(IPin[] pins)
	{
		Set<IPinList> pinLists = new HashSet<IPinList>();
		for (IPin m_pin : pins) {
			//parent.removeObject(m_pin);
			pinLists.add((IPinList) m_pin.getParent());
			m_pin.delete();
		}

		Set<IPinList> nonDeletedParents = new HashSet<IPinList>();
		for (IPinList pinList : pinLists) {
			if (pinList.getAllPins().isEmpty()) {
				Collection<IDiagramObject> deletables = new HashSet<IDiagramObject>();
				deletables.add(pinList);

				ISchemDiagram diagram = getDiagram(pinList);

				DeleteHelper.getInstance().delete(diagram, deletables, true);
			}
			else {
				nonDeletedParents.add(pinList);
			}
		}
		return nonDeletedParents;
	}

	private static ISchemDiagram getDiagram(IDiagramObject diagramObject)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(diagramObject);
		assert diagram != null;
		return diagram;
	}

	public static void addPinsToStackPin(IPinList destPinList, ISchemStackPin stackpin, IPin[] pins)
	{
		IJoint stackPinNode = stackpin.getJoint();
		//1. Add the selected pins to the StackPin
		for (IPin m_pin : pins) {
			stackpin.addPinToStack(m_pin.getConnectivity());
		}
		if (!arePinsGraphicallyConnectedToWires(pins)) {
			return;
		}
		//2. Atleast one of the selected pin(s) is connected to a conductor
		for (IPin pin : pins) {
			Set<ISegment> segs = CollectionUtils.getObjects(pin.getSegments(), ISegment.class);
			if (!segs.isEmpty()) {
				Map<ISegment, Boolean> segmentToNodeMap = new HashMap<ISegment, Boolean>();
				for (ISegment seg : segs) {
					if (seg.isStartNode(pin.getJoint())) {
						segmentToNodeMap.put(seg, true);
					}
					else if (seg.isEndNode(pin.getJoint())) {
						segmentToNodeMap.put(seg, false);
					}
				}
				//There exists some conductors that are graphically connected to selected pin(s). Disconnect them
				pin.disconnectGraphically();

				//Create a highway, if one doesn't exist already
				boolean bHighwayAlreadyCreated =
						stackPinNode != null && !stackPinNode.getAssociations(IHighwaySegment.class).isEmpty();
				if (!bHighwayAlreadyCreated) {
					IHighwaySchematic schemHighway = HighwayHelper.createHighway(destPinList, stackpin);
					schemHighway.addSchemStackPin(stackpin.getUID());
				}
				stackPinNode = stackpin.getJoint();
				//Get the highway segment that is connected to stackPin (If there are many, get the first one)
				Collection<IHighwaySegment> highwaySegs = stackPinNode.getAssociations(IHighwaySegment.class);
				IHighwaySegment highwaySeg = highwaySegs.iterator().next();

				//This highway segment can be either a dangling segment 'or' not
				IJoint highwaynodeToConnSeg = HighwayHelper.getOtherNodeOfHighwaySegment(highwaySeg, stackpin);
				if (!highwaynodeToConnSeg.getAssociations(ISchemStackPin.class).isEmpty()) {
					ICommonFactory commFact = FactoryMgr.getCommonFactory();
					ISchemFactory schemFact = FactoryMgr.getSchemFactory();
					//ILogicSegment newSegment = LogicConnectionUtils.insertGripPoint((ILogicSegment) highwaySeg, location);
					ILocation midPoint = SegmentHelper.getMidPointSnappedToGrid(highwaySeg);
					Point breakPoint = new Point(midPoint.getX(), midPoint.getY());
					SegmentHelperInfo shInfo = SegmentHelper.splitSegment(commFact, schemFact, highwaySeg, breakPoint);
					NodeHelper
							.getMergedNode(commFact, schemFact, shInfo.getNode1(), shInfo.getNode2(), breakPoint.x,
									breakPoint.y);
					highwaynodeToConnSeg =
							shInfo.getNode1().getAssociations(ISchemStackPin.class).isEmpty() ? shInfo.getNode1() :
									shInfo.getNode2();
				}
				if (highwaynodeToConnSeg != null) {
					//If this is a dangling highway, interface the conductors to the dangling end
					adjustConnectedWires(segmentToNodeMap, highwaySeg, highwaynodeToConnSeg);
				}
			}
		}
	}

	private static void adjustConnectedWires(Map<ISegment, Boolean> segs, IHighwaySegment highwaySeg,
			IJoint highwaySegOtherNode)
	{
		IGeneralHighway cableHighway = HighwayHelper.toGeneralHighway(highwaySeg.getHighway());
		for (Map.Entry<ISegment, Boolean> entry : segs.entrySet()) {
			ISegment seg = entry.getKey();
			IConductor schemCond = seg.getConductor();
			chs.cof.logical.cable.IConductor cablecond = schemCond.getConnectivity();
			if (cablecond instanceof IShieldConductor) {
				// We have added pin connected to shield to stacked pin, delete diagram object of it				
				Collection<IDiagramObject> deletables = new HashSet<IDiagramObject>();
				for (IDiagramObject diagramObj : schemCond.getObjects(IDiagramObject.class)) {
					deletables.add(diagramObj);
				}
				deletables.add(schemCond);
				DeleteHelper.getInstance().delete(getDiagram(schemCond), deletables, false);
			}
			else {
				SegmentHelper.interfaceToHighway(seg, entry.getValue(), highwaySeg, highwaySegOtherNode);
			}

			if (cableHighway != null) {
				cableHighway.addStackPinConductor(((IHighwayConductor) seg.getConductor().getConnectivity()));
			}
		}
		if (!segs.isEmpty()) {
			Set<IHighwaySchematic> highwaySchems = new HashSet<IHighwaySchematic>();
			highwaySchems.add(highwaySeg.getHighway());
			Set<IHighwaySchematic> deletedHWSchems = new HashSet<IHighwaySchematic>();
			HighwayHelper.mergeHighwaySchem(highwaySchems, deletedHWSchems);
		}
		RegenerateGraphicsAction.getInstance().addObjectForRegenrate(highwaySeg.getHighway());
	}

	private static boolean arePinsGraphicallyConnectedToWires(IPin[] pins)
	{
		for (IPin m_pin : pins) {
			if (!CollectionUtils.getObjects(m_pin.getSegments(), ISegment.class).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public static IGfxObject getMatchingTransientObject(List<PinPlacementConstraintsHolder> constraintsHolders,
			Point point)
	{
		for (PinPlacementConstraintsHolder holder : constraintsHolders) {
			IGfxObject matchObj = holder.getMatchingLocDynamics(point);
			if (matchObj != null) {
				return matchObj;
			}
		}
		return null;
	}

	/**
	 * Checks if pin is valid to be added to a stack.
	 * <p>
	 * Here some basic checks are performed
	 *
	 * @param pinList Pinlist on which pins exists
	 * @param pin Pin to be verified for adding to stack
	 *
	 * @return true if pin is valid to add to stack
	 */
	public static boolean isPinValidToAddToStack(IPinList pinList, IPin pin)
	{
		if (pin == null || pinList == null) {
			return false;
		}

		if (pinList != pin.getParent()) {
			//only valid for pins belonging to same schem pinlist
			return false;
		}
		if (pin.isReference()) {
			return false;
		}

		IAbstractPin cablePin = pin.getConnectivity();
		return isValidToCreateStackOnPinList(pinList, cablePin);
	}

	public static boolean isValidToCreateStackOnPinList(IPinList pinList, @Nullable IAbstractPin cablePin)
	{
		if (cablePin != null) {
			// Reference pin and Backshell termination should not be added to stack pin
			if (cablePin instanceof IBackshellTermination) {
				return false;
			}

			if (cablePin instanceof IDevicePin) {
				if (((IDevicePin) cablePin).getConnectedDevicePin() != null) {
					return false;
				}
			}

			for (Object object : cablePin.getConductors()) {
				if (object instanceof IShieldConductor &&
						((chs.cof.logical.cable.IConductor) object).getMulticore() == null) {
					return false;
				}
			}
		}

		if (pinList.getParameterized() == null) {
			//Applicable only for parameterized pinlists
			return false;
		}

		chs.cof.logical.cable.IPinList connectivityPinList = pinList.getConnectivity();

		if (connectivityPinList instanceof IInterconnectObject) {
			return false;
		}

		if (connectivityPinList instanceof IConnector && ((IConnector) connectivityPinList).isRingTerminal()) {
			return false;
		}

		return !(connectivityPinList instanceof ISplice);
	}

	public static boolean isValidToAddToStackedPin(@NotNull IPin[] pins, IGfxObject stackorPlaceHodler)
	{
		if (pins.length < 1) {
			return true;
		}
		MATING_STATE matchingState = MATING_STATE.MIX;

		for (IPin pin : pins) {
			IPinList pinList = (IPinList) pin.getParent();
			assert pinList != null;
			if (!isPinValidToAddToStack(pinList, pin)) {
				return false;
			}
			Collection<IPin> matepins = PinPlacementHelper.getConnectedSchemPins(pin);
			if (matepins.size() > 1) {
				return false;
			}
			IPin matepin = PinPlacementHelper.getSingleConnectedSchemPin(pin);

			MATING_STATE currentaState = matepin != null ? MATING_STATE.MATING : MATING_STATE.NON_MATING;
			if (matchingState == MATING_STATE.MIX) {
				matchingState = currentaState;
			}
			else if (currentaState != matchingState) {
				return false;
			}
		}

		if (stackorPlaceHodler instanceof ISchemStackPin stackPin) {
			if (StackedPinHelper.isConnectedToSingleLine(stackPin)) {
				return false;
			}
			// Addition of mated pin is not allowed to add to non-mated stacked pin and wise versa
			if ((matchingState == MATING_STATE.MATING && ConnectionHelper.hasMatedStackPin(stackPin))) {
				ISchemStackPin matedStackedPin = ConnectionHelper.getConnectedStackedPin(stackPin);
				assert matedStackedPin != null : "Mated stacked pin can not be null here";
				IPinList matedPinList = (IPinList) matedStackedPin.getParent();
				assert matedPinList != null : "Mated pin list can not be null here";
				IAbstractPin cablePin = pins[0].getConnectivity();
				return cablePin.getConnectedPin(matedPinList.getConnectivity()) != null;
			}
			return (matchingState == MATING_STATE.NON_MATING && !ConnectionHelper.hasMatedStackPin(stackPin));
		}
		if (stackorPlaceHodler instanceof IPinPlaceholder) {
			IPinList pinList = (IPinList) ((IPinPlaceholder) stackorPlaceHodler).getOwner();
			assert pinList != null;
			IGfxObject match =
					ConnectionHelper.getMatchingPinOrPlaceholderForPlaceHolder((IPinPlaceholder) stackorPlaceHodler,
							pinList, chs.cof.logical.cable.IPinList.class);
			// If selected pins are all mated, addition to any place holder is allowed or else do not allow addition
			return matchingState == MATING_STATE.MATING || (match == null || match instanceof IPinPlaceholder);
		}
		return false;
	}

	/**
	 * Creates mated pin stack if already created pin stack
	 *
	 * @param pinsOfStack pins in the given stack
	 * @param stack pin stack for which mated pin stack to be created
	 */
	@Nullable static ISchemStackPin createMatedStackedPin(IPin[] pinsOfStack, ISchemStackPin stack, Generator generator,
			GeneratorParameters gp)
	{
		ISchemStackPin matedStackedPin = null;
		Map<IPin, IPin> matedPinMap = getMatedPins(pinsOfStack);
		if (matedPinMap != null && !matedPinMap.isEmpty()) {
			IPin[] matedPins = new IPin[matedPinMap.size()];
			matedPinMap.values().toArray(matedPins);
			if (matedPinMap.size() != pinsOfStack.length) {
				throw new IllegalArgumentException("All pins of a stack must be mated");
			}
			IPinList pinStackParent = (IPinList) stack.getParent();
			if (pinStackParent == null) {
				return null;
			}

			IPinList matedPinParent = (IPinList) matedPinMap.entrySet().iterator().next().getValue().getParent();
			if (matedPinParent == null) {
				return null;
			}

			Set<IPinList> candidateAttachedPLs = new HashSet<IPinList>();
			getCandidateMatePLs(pinStackParent, matedPinParent, candidateAttachedPLs);

			IGfxObject match =
					ConnectionHelper.getFirstMatchingPinOrPlaceholderForStackPin(stack, pinStackParent,
							candidateAttachedPLs);
			chs.cof.logical.cable.IPinList matedCablePinList = matedPinParent.getConnectivity();
			IPinList matedStackParent = null;
			ILocation relativeLocationOfMatedStack = null;

			if (match == null &&
					((matedCablePinList instanceof IConnector) || (matedCablePinList instanceof IDevice))) {
				IPin pinOfStackClosestToStack = getNearestPin(pinStackParent, pinsOfStack, stack);
				assert pinOfStackClosestToStack != null;
				IPin matedPin = matedPinMap.get(pinOfStackClosestToStack);
				assert matedPin != null;
				matedStackParent = (IPinList) matedPin.getParent();
				assert matedStackParent != null;

				relativeLocationOfMatedStack = createLocationForMatedStackOnOpenSpace(stack, matedPin);
			}
			else if (match instanceof IPinPlaceholder) {
				matedStackParent = (IPinList) ((IPinPlaceholder) match).getOwner();
				relativeLocationOfMatedStack = match.getLocation();
			}
			else if (match instanceof IPin) {
				IPin matchPin = (IPin) match;
				assert containsInArray(matedPins, matchPin);
				matedStackParent = (IPinList) matchPin.getParent();
				relativeLocationOfMatedStack = match.getLocation();
			}
			else if (match instanceof ISchemStackPin) {
				assert true : "New pin stack cannot be created on existing pin stack";
				matedStackParent = (IPinList) ((IDiagramObject) match).getParent();
				relativeLocationOfMatedStack = match.getLocation();
				matedStackedPin = (ISchemStackPin) match;
			}

			if (matedStackParent == null || relativeLocationOfMatedStack == null) {
				throw new IllegalArgumentException("Stack containing mated pins must have mated stack");
			}

			ILocation mateStackPinLoc = FactoryMgr.getCommonFactory()
					.constructLocation(relativeLocationOfMatedStack.getX(), relativeLocationOfMatedStack.getY());

			//create a stack on the mated pinlist
			if (matedStackedPin == null) {
				matedStackedPin = StackedPinHelper.createAndAddStackPin(matedStackParent, mateStackPinLoc);
				RegenerateGraphicsAction.getInstance().addObjectForRegenrate(matedStackedPin);
			}
			//add the mated schem pinsOfStack to mated schem stack pin
			addPinsToStackPin(matedStackParent, matedStackedPin, matedPins);

			//remove the mated schem pinsOfStack from the mated pinlist
			Set<IPinList> parents = removeSchemPins(matedPins);

			generator.generate(matedStackParent, gp, Generator.NOREGENERATE_PROPERTIES, false);

			//regenerate the mated pinlist
			for (IPinList parent : parents) {
				parent.regenerateDiagramObject();
			}
		}
		return matedStackedPin;
	}

	@NotNull private static ILocation createLocationForMatedStackOnOpenSpace(ISchemStackPin stack, IPin matedPin)
	{
		ILocation pinAbsLoc = stack.getAbsLocation();
		ILocation pinLocRelativeToMate = CoordinateHelper
				.getRelLocation(matedPin, pinAbsLoc.getX(), pinAbsLoc.getY());
		ILocation relativeLocationOfMatedStack = FactoryMgr.getCommonFactory().createLocation();

		relativeLocationOfMatedStack.setX(matedPin.getLocation().getX());
		relativeLocationOfMatedStack.setY(pinLocRelativeToMate.getY());
		return relativeLocationOfMatedStack;
	}

	private static void getCandidateMatePLs(IPinList pinStackParent, @NotNull IPinList matedPinParent,
			Set<IPinList> candidateAttachedPLs)
	{
		for (IPinList pl : pinStackParent.getAttachedPinListObjects()) {
			IUID uid = pl.getConnectivityUID();
			IUID connectivityUID = matedPinParent.getConnectivityUID();
			if (uid != null && connectivityUID != null && uid.isEquiv(connectivityUID)) {
				candidateAttachedPLs.add(pl);
			}
		}
	}

	@Nullable public static Map<IPin, IPin> getMatedPins(IPin[] pins)
	{
		Map<IPin, IPin> matedPinMap = new HashMap<IPin, IPin>();
		for (IPin pin : pins) {
			IPin matePin = PinPlacementHelper.getSingleConnectedSchemPin(pin);
			if (matePin != null) {
				matedPinMap.put(pin, matePin);
			}
		}
		if (!matedPinMap.isEmpty() && matedPinMap.size() == pins.length) {
			return matedPinMap;
		}
		return null;
	}

	/**
	 * Adds mated pins for given pins if exists to the mated stack of the given stack
	 *
	 * @param pinsOfStack Pins whose mated pins to be added to mated stack
	 * @param stackpin Stack pin whose mated stack
	 * @param generator Generator
	 * @param genParams GeneratorParameters
	 *
	 * @return true if mated pins are added to mated stack
	 */
	@Nullable static ISchemStackPin addConnectedPinsToMatedStack(IPin[] pinsOfStack, ISchemStackPin stackpin,
			Generator generator,
			GeneratorParameters genParams)
	{
		Map<IPin, IPin> matedPinMap = getMatedPins(pinsOfStack);

		ISchemStackPin matestackpin = null;
		if (matedPinMap != null && !matedPinMap.isEmpty()) {
			IPin[] matedPins = new IPin[matedPinMap.size()];
			matedPinMap.values().toArray(matedPins);
			if (matedPinMap.size() != pinsOfStack.length) {
				throw new IllegalArgumentException("All pins of a stack must be mated");
			}

			ConnectionHelper sourceHelper = new ConnectionHelper();
			ISchemDiagram diagram = DiagramHelper.getDiagram(stackpin);
			assert diagram != null;
			sourceHelper.examine(stackpin, diagram);
			IPinList pinlist = (IPinList) stackpin.getParent();
			assert pinlist != null;
			IGfxObject match = sourceHelper.getMatchingPinPosition(stackpin, pinlist);

			if (match instanceof ISchemStackPin) {
				IPinList mate = (IPinList) ((IDiagramObject) match).getParent();
				if (mate != null) {
					matestackpin = (ISchemStackPin) match;
					//add the mated schem pins to mated schem stack pin
					addPinsToStackPin(mate, matestackpin, matedPins);

					//remove the mated schem pins from the mated pinlist
					removeSchemPins(matedPins);

					//regenerate the mated pinlist
					generator.generate(mate, genParams, Generator.NOREGENERATE_PROPERTIES, false);
					mate.regenerateDiagramObject();
				}
			}
			else {
				throw new IllegalArgumentException("Pin stack in argument must have mated stack");
			}
		}
		return matestackpin;
	}

	private static boolean containsInArray(IPin[] matedPins, IPin pin)
	{
		for (IPin matedPin : matedPins) {
			if (matedPin == pin) {
				return true;
			}
		}
		return false;
	}

	@Nullable static IPin getNearestPin(IPinList pinList, IPin[] pins, IAbstractSchemPin schemPin)
	{
		//trying for absolute co-ordinate to support modular connector schematics
		Map<IPinList, PinSideCalculator> sideCalculators = new HashMap<>();
		PinSideCalculator sideCal = PinSideCalculator.createAbsolute(pinList);
		Side newPinSide = sideCal.getSide(schemPin);
		double minDistance = 0;
		IPin closestPin = null;
		ILocation schemPinAbsLocation = schemPin.getAbsLocation();
		for (IPin pin : pins) {
			IPinList owner = (IPinList) pin.getParent();
			assert owner != null;
			PinSideCalculator pinSideCalculator =
					sideCalculators.computeIfAbsent(owner, (p) -> PinSideCalculator.createAbsolute(owner));
			Side pinSide = pinSideCalculator.getSide(pin);
			if (pinSide == newPinSide) {
				double distance = schemPinAbsLocation.distance(pin.getAbsLocation());
				if (closestPin == null) {
					closestPin = pin;
					minDistance = distance;
				}
				else if (distance < minDistance) {
					minDistance = distance;
					closestPin = pin;
				}
			}
		}
		return closestPin;
	}
}
