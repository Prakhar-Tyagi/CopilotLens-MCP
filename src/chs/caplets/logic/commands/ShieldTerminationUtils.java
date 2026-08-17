package chs.caplets.logic.commands;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cofUtils.cmd.CHSCommand;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.cofUtils.cmd.ResourceCommandEvent;
import chs.common.IDesignContainer;
import chs.common.ILocation;
import chs.common.IUID;
import chs.utilities.CommonUtils;
import chs.utility.Placement;
import chs.utility.helpers.ConductorHelper;
import chs.view.route.NoPrototype;
import chs.view.route.blockage.BlockageUtils;
import chs.view.route.blockage.IRouteContext;
import chs.view.utils.DiagramFlowStyle;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 9 Aug, 2012 Time: 12:30:08 PM
 */
public class ShieldTerminationUtils
{

	private ShieldTerminationUtils()
	{
	}

	/**
	 * Get all the indicators of a multicore. For each indicator a.	Get all the directly & indirectly connected shield
	 * terminations. b.	Get all the segments passing through this indicator body c.	Put segment vs. shield connected pins
	 * in a map
	 *
	 * @param multicore - the multicore for which we want to find the schematically connected shield terminations
	 *
	 * @return map of segment vs directly "or" indirectly connected pins
	 */
	public static Map<ISegment, ShieldPinInfo> getShieldConnections(IMulticore multicore)
	{
		Map<ISegment, ShieldPinInfo> map = new HashMap<>();
		ILogicDesign design = multicore.getLogicDesign();
		chs.cof.logical.cable.IShieldBody cableShield = multicore.getShieldBody();
		if (cableShield == null) {
			return map;
		}
		Set<IUID> diagrams = new HashSet<IUID>();
		if (design != null) {
			design.getDesignWideUsageMgr().getMulticoreDiagrams(multicore, diagrams);
		}
		Set<chs.cof.logical.cable.IConductor> conductors = multicore.getAllConductorsInHierarchy();
		for (IUID diagramuid : diagrams) {
			ISchemDiagram diagram = null;
			if (design != null) {
				diagram = design.getDiagram(diagramuid);
			}
			List<IConductor> schemConductors = new ArrayList<IConductor>();
			if (diagram != null) {
				for (chs.cof.logical.cable.IConductor cableConductor : conductors) {
					for (IDiagramObject condRep : diagram.getRepresentations(cableConductor.getUID())) {
						if (condRep instanceof IConductor) {   //there could be highway representations as well
							schemConductors.add(CommonUtils.cast(condRep, IConductor.class));
						}
					}
				}
				for (IDiagramObject obj : diagram.getRepresentations(cableShield.getUID())) {
					IShieldBody sb = (IShieldBody) obj;
					Collection<IPin> connectedPins = sb.getAllConnectedSchemPins();
					for (IConductor schemConductor : schemConductors) {
						List<ISegment> segs = Placement.condIntersectsIndicator(schemConductor, sb, false);
						for (ISegment segment : segs) {
							IMulticore parentMC = segment.getConductor().getConnectivity().getMulticore();
							ShieldPinInfo value = new ShieldPinInfo();
							if (map.containsKey(segment)) {      //a segment may have multiple indicators
								value = map.get(segment);
							}
							if (parentMC == multicore) {
								Collection<IPin> existingPins = value.get(0);
								if (existingPins == null) {
									existingPins = new HashSet<IPin>();
								}
								existingPins.addAll(connectedPins);
								value.put(0, existingPins);
							}
							else {
								int mcPos = getMulticorePositionInHierarchyWRTGivenMC(parentMC, multicore);
								value.put(mcPos, connectedPins);
							}
							map.put(segment, value);
						}
					}
				}
			}
		}
		for (IMulticore childMC : multicore.getMulticoresAsList()) {
			Map<ISegment, ShieldPinInfo> childmap = getShieldConnections(childMC);
			for (Map.Entry<ISegment, ShieldPinInfo> entry : childmap.entrySet()) {
				ISegment segment = entry.getKey();
				if (map.containsKey(segment)) {
					ShieldPinInfo val = childmap.get(segment);
					for (Map.Entry<Integer, Collection<IPin>> mcEntry : val.entrySet()) {
						map.get(segment).put(mcEntry.getKey(), mcEntry.getValue());
					}
				}
				else {
					map.put(segment, childmap.get(segment));
				}
			}
		}

		pruneMapForEmptyValues(map);

		return map;
	}

	private static void pruneMapForEmptyValues(Map<ISegment, ShieldPinInfo> map)
	{
		Set<ISegment> segmentKeysToDelete = new HashSet<ISegment>();
		for (Map.Entry<ISegment, ShieldPinInfo> entry : map.entrySet()) {
			ShieldPinInfo value = entry.getValue();
			boolean prune = true;
			for (Map.Entry<Integer, Collection<IPin>> valueEntry : value.entrySet()) {
				if (!valueEntry.getValue().isEmpty()) {
					prune = false;
				}
			}
			if (prune) {
				segmentKeysToDelete.add(entry.getKey());
			}
		}

		for (ISegment segment : segmentKeysToDelete) {
			map.remove(segment);
		}
	}

	public static IMulticore getMulticoreInHierarchyAtGivenPosWRTGivenMulticore(int mcPos, IMulticore mc)
	{
		int i = 0;
		IMulticore parentMC = mc.getParent();
		IMulticore reqMC = mc;
		while (i < mcPos && parentMC != null) {
			i++;
			reqMC = parentMC;
			parentMC = parentMC.getParent();
		}
		return reqMC;
	}

	private static int getMulticorePositionInHierarchyWRTGivenMC(IMulticore indexMC, IMulticore multicore)
	{
		int mcPos = 0;
		IMulticore parent = indexMC.getParent();
		while (parent != null) {
			mcPos++;
			if (parent == multicore) {
				break;
			}
			parent = parent.getParent();
		}
		return mcPos;
	}

	/**
	 * Gets the nearest hookup from a schem pin in the given collection of hookups
	 *
	 * @param hookups nearest hookup from this collection required
	 * @param schemPin the pin for which the nearest hookup has to be found
	 *
	 * @return hookup nearest to the specified point.
	 */
	@Nullable public static IShieldBodyHookup findNearestHookup(Collection<IShieldBodyHookup> hookups,
			IPin schemPin)
	{
		double minimumDistance = Double.MAX_VALUE;
		IShieldBodyHookup result = null;

		ILocation absLocation = schemPin.getAbsLocation();
		Point pt = new Point(absLocation.getX(), absLocation.getY());

		for (IShieldBodyHookup hookup : hookups) {
			ILocation location = hookup.getAbsLocation();
			double distance = pt.distance((double) location.getX(), (double) location.getY());
			if (distance < minimumDistance) {
				minimumDistance = distance;
				result = hookup;
			}
		}
		return result;
	}

	/**
	 * This function draws the schematic shield conductors based on the cable shield information & map of cable shield vs
	 * schematic pins
	 *
	 * @param multicore - the multicore for which shields have to be drawn
	 * @param diagram - the diagram in which shields have to be drawn
	 * @param map - map containing cable shield and the schematic pins to which this should connect to.
	 * @param existingShields - list of existing cable shields. the segments of these should be ignored while auto-routing
	 * as they would any way be deleted. If these are not ignored (as obstaclesToIgnore), unnecessarily we see angular
	 * schem shields for new MCs
	 */
	public static void drawSchemShields(IMulticore multicore, ISchemDiagram diagram,
			Map<IShieldConductor, Collection<IPin>> map, List<IShieldConductor> existingShields)
	{
		boolean createSchemShield = true;

		chs.cof.logical.cable.IShieldBody cableShield = multicore.getShieldBody();
		IShieldConductor shieldConductor = multicore.getShield();
		if (cableShield == null || shieldConductor == null) {
			createSchemShield = false;
		}
		Collection<IPin> schemPins = map.get(shieldConductor);
		if (schemPins == null || schemPins.isEmpty()) {
			createSchemShield = false;
		}

		if (createSchemShield) {
			Collection<IShieldBodyHookup> hookups = new ArrayList<IShieldBodyHookup>();
			for (IDiagramObject obj : diagram.getRepresentations(cableShield.getUID())) {
				IShieldBody sb = (IShieldBody) obj;
				hookups.addAll(sb.getObjects(IShieldBodyHookup.class));
			}
			for (IPin schemPin : schemPins) {
				IShieldBodyHookup nearestHookup = findNearestHookup(hookups, schemPin);
				if (nearestHookup != null) {
					List<ISegment> obstaclesToIgnore = new ArrayList<ISegment>();
					for (Object obj : schemPin.getSegments()) {
						if (obj instanceof ISegment) {
							ISegment seg = (ISegment) obj;
							IConductor cond = seg.getConductor();
							chs.cof.logical.cable.IConductor cable_cond = cond.getConnectivity();
							if (existingShields != null && existingShields.contains(cable_cond)) {
								obstaclesToIgnore.add(seg);
							}
						}
					}
					List<ILocation> conductorSegmentPoints =
							BlockageUtils.routeConductor(diagram, schemPin.getAbsLocation(),
									nearestHookup.getAbsLocation(), true, obstaclesToIgnore,
									Collections.emptySet(), IRouteContext.RouteGraphSize.MINIMUM,
									NoPrototype.NO_PROTOTYPE, 1,
									DiagramFlowStyle.DEFAULT, false, false, null, false);

					IConductor schemConductor =
							CreateSchemConductorCmd
									.createConductor(diagram, conductorSegmentPoints, shieldConductor,
											true);

					for (ISegment segment : schemConductor.getSegmentsOfType(ISegment.class)) {
						segment.connectPin(schemPin);
						ConductorHelper.connectShieldBodyHookup(segment, nearestHookup,
								new Point(nearestHookup.getAbsLocation().getX(),
										nearestHookup.getAbsLocation().getY()));
					}
				}
			}
		}
		for (IMulticore childMC : multicore.getMulticoresAsList()) {
			drawSchemShields(childMC, diagram, map, existingShields);
		}
	}

	/**
	 * This function takes the segment vs connected pins (directly "or" indirectly) and filters those pins which can
	 * possibly go to differnet Multicore. Then, assigns the other pins to cable shield of the MC to which the segment's
	 * wire's MC belong sto
	 *
	 * @param segVsshieldConns - map of segment vs pins directly "or" indirectly connected to it
	 * @param design - current design
	 * @param cmd - command whose log has to be dumped
	 * @param m_multicoreConversionInfo multicore coversion information
	 *
	 * @return map of cable shield & schematic pins to which a schem shield has to be drawn
	 */
	public static Map<IShieldConductor, Collection<IPin>> addShieldConnections(
			Map<ISegment, ShieldPinInfo> segVsshieldConns, IDesignContainer design, CHSCommand cmd,
			@Nullable ConvertNetsToWiresCmd.INetsToWiresResultCollector multicoreConversionInfo)
	{
		Map<IShieldConductor, Collection<IPin>> map = new HashMap<IShieldConductor, Collection<IPin>>();
		Map<IAbstractPin, Set<IMulticore>> processedPins = new HashMap<IAbstractPin, Set<IMulticore>>();
		for (Map.Entry<ISegment, ShieldPinInfo> entry : segVsshieldConns.entrySet()) {
			ISegment segment = entry.getKey();
			Map<Integer, Collection<IPin>> pinsColl = entry.getValue();
			chs.cof.logical.cable.IConductor cond = segment.getConductor().getConnectivity();
			IMulticore mc = cond.getRootMulticore();
			if (mc != null) {
				for (Collection<IPin> pins : pinsColl.values()) {
					for (IPin pin : pins) {
						IAbstractPin cablePin = pin.getConnectivity();
						Set<IMulticore> connMC = processedPins.get(cablePin);
						if (connMC == null) {
							connMC = new HashSet<IMulticore>();
							processedPins.put(cablePin, connMC);
						}
						connMC.add(mc);
					}
				}
			}
		}
		Set<IPin> pinsWithMultipleMCs = new HashSet<IPin>();
		for (Map.Entry<ISegment, ShieldPinInfo> entry : segVsshieldConns.entrySet()) {
			ISegment segment = entry.getKey();
			ShieldPinInfo pinsColl = entry.getValue();
			chs.cof.logical.cable.IConductor cond = segment.getConductor().getConnectivity();
			IMulticore mc = cond.getMulticore();
			if (mc != null) {
				for (Map.Entry<Integer, Collection<IPin>> pinEntry : pinsColl.entrySet()) {
					int mcPos = pinEntry.getKey();
					Collection<IPin> pins = pinEntry.getValue();
					if (pins.isEmpty()) {
						continue;
					}
					IMulticore targetMC = mc;
					if (mcPos != 0) {
						targetMC = getMulticoreInHierarchyAtGivenPosWRTGivenMulticore(mcPos, mc);
					}
					IShieldConductor shield = targetMC.getShield();
					if (shield != null) {
						for (IPin pin : pins) {
							IAbstractPin cablePin = pin.getConnectivity();
							Set<IMulticore> connMCs = processedPins.get(cablePin);
							if (connMCs.size() == 1) {
								shield.addPin(cablePin);
							}
							else {
								pinsWithMultipleMCs.add(pin);
								if (multicoreConversionInfo != null) {
									IShieldConductor sourceShield = multicoreConversionInfo.getSourceShield(shield);
									if (sourceShield != null) {
										multicoreConversionInfo.addIgnoredShield(cablePin, sourceShield);
									}
								}
							}
						}
						pins.removeAll(pinsWithMultipleMCs);
						Collection<IPin> shieldPins = map.get(shield);
						if (shieldPins != null) {
							shieldPins.addAll(pins);
						}
						else {
							map.put(shield, pins);
						}
					}
				}
			}
		}
		for (IPin pin : pinsWithMultipleMCs) {
			IAbstractPin abstractPin = pin.getConnectivity();
			IPinList pinOwner = abstractPin.getOwner();
			String name;
			if (pinOwner == null) {
				name = abstractPin.getName();
			}
			else {
				name = new StringBuilder().append(pinOwner.getName()).append(":").append(abstractPin.getName())
						.toString();
			}
			abstractPin.getName();
			cmd.getCommandListener().handleEvent(
					ResourceCommandEvent.create("Message.ShieldConnectionSpanningMultipleMulticores",
							CHSCommand.link(design, pin, name)));
		}

		return map;
	}

	@SuppressWarnings({"EmptyClass"}) public static class ShieldPinInfo extends HashMap<Integer, Collection<IPin>>
	{

	}
}
