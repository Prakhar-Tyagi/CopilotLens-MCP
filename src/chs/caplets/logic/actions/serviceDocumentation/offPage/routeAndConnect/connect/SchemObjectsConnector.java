package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.connect;

import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.ISchemObjectsConnector;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.EdgesConnecter;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.SchemConductorEdge;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.SchemConductorEdges;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge.SchemConductorEdgesHelper;
import chs.cof.drawplus.ISegmentCollector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.ILocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SchemObjectsConnector implements ISchemObjectsConnector
{

	private final EdgesConnecter m_edgesConnecter;

	public SchemObjectsConnector(EdgesConnecter edgesConnecter)
	{
		m_edgesConnecter = edgesConnecter;
	}

	public boolean connectSchemConductors(IConductor schem1, IConductor schem2)
	{
		return connectSegmentCollectors(schem1, schem2);
	}

	public boolean connectHighways(IHighwaySchematic schem1, IHighwaySchematic schem2)
	{
		return connectSegmentCollectors(schem1, schem2);
	}

	@Override public boolean connectSchemConductorAndHighway(IConductor conductor, IHighwaySchematic highwaySchematic)
	{
		return connectSegmentCollectors(conductor, highwaySchematic);
	}

	public boolean connectSchemConductorAndPin(IConductor schem1, IAbstractSchemPin pin)
	{
		Set<IConductor> cs = new HashSet<>();
		cs.add(schem1);
		return connect(getSegments(cs), pin);
	}

	public boolean connectHighwayAndStackPin(IHighwaySchematic schem1, ISchemStackPin pin)
	{
		Set<IHighwaySchematic> cs = new HashSet<>();
		cs.add(schem1);
		return connect(getSegments(cs), pin);
	}

	@Override public boolean connectPinAndHighwaySchematic(IPin pin, IHighwaySchematic highwaySchematic)
	{
		Set<IHighwaySchematic> cs = new HashSet<>();
		cs.add(highwaySchematic);
		return connect(getSegments(cs), pin);
	}

	@Override public boolean connectStackPinAndConductor(ISchemStackPin stackPin, IConductor conductor)
	{
		Set<IConductor> cs = new HashSet<>();
		cs.add(conductor);
		return connect(getSegments(cs), stackPin);
	}

	public boolean connectSchemPins(IAbstractSchemPin pin1, @Nullable IAbstractSchemPin pin2)
	{
		return connect(pin1, pin2);
	}

	private boolean connectSegmentCollectors(ISegmentCollector c1, ISegmentCollector c2)
	{
		Set<ILogicSegment> segments1 = getSegments(Collections.singleton(c1));
		Set<ILogicSegment> segments2 = getSegments(Collections.singleton(c2));
		return connect(segments1, segments2);
	}

	private boolean connect(IAbstractSchemPin pin1, @Nullable IAbstractSchemPin pin2)
	{
		ILocation pin1Loc = pin1.getAbsLocation();
		ILocation pin2Loc = pin2 == null ? null : pin2.getAbsLocation();
		return m_edgesConnecter.connectPins(pin1Loc, pin2Loc, pin1, pin2);
	}

	@NotNull private Set<ILogicSegment> getSegments(Collection<? extends ISegmentCollector> schemConductors)
	{
		return schemConductors
				.stream()
				.flatMap(c -> c.getSegments().stream())
				.filter(c -> c instanceof ILogicSegment)
				.map(c -> (ILogicSegment) c)
				.collect(Collectors.toSet());
	}

	private boolean connect(Collection<ILogicSegment> segmentList, IAbstractSchemPin pin)
	{
		SchemConductorEdges schemConductorEdges = new SchemConductorEdgesHelper().getEdgesOfSameConductor(segmentList);
		if (schemConductorEdges == null) {
			return false;
		}
		SchemConductorEdge start = schemConductorEdges.getStart();
		return connect(start, pin);
	}

//	private boolean connect(Collection<ILogicSegment> segmentList, IHighwaySchematic highwaySchematic)
//	{
//		SchemConductorEdges schemConductorEdges = getJoinEdges(segmentList);
//		if (schemConductorEdges == null) {
//			return false;
//		}
//		SchemConductorEdge start = schemConductorEdges.getStart();
//		return connect(start, highwaySchematic);
//	}

	private boolean connect(@Nullable SchemConductorEdge toConnect, IAbstractSchemPin pin)
	{
		if (toConnect == null) {
			return false;
		}
		ILocation joint1 = toConnect.getJoinLocation();
		if (joint1 == null) {
			return false;
		}
		ILocation joint2 = pin.getAbsLocation();
		ILogicSegment segment = toConnect.getSegment();
		return m_edgesConnecter.connectSegmentWithPin(joint1, joint2, segment, pin);
	}

//	private boolean connect(SchemConductorEdge toConnect, IHighwaySchematic highwaySchematic)
//	{
//		IJoint joint1 = toConnect.getJoint();
//		if (joint1 == null) {
//			return false;
//		}
//		ILocation joint2 = pin.getAbsLocation();
//		ILogicSegment segment = toConnect.getSegment();
//		return m_edgesConnecter.connectSegmentWithPin(joint1, joint2, segment, pin);
//	}

	private boolean connect(Collection<ILogicSegment> segmentList1, Collection<ILogicSegment> segmentList2)
	{
		SchemConductorEdges schemConductorEdges1 =
				new SchemConductorEdgesHelper().getEdgesOfSameConductor(segmentList1);
		SchemConductorEdges schemConductorEdges2 =
				new SchemConductorEdgesHelper().getEdgesOfSameConductor(segmentList2);
		if (schemConductorEdges1 == null || schemConductorEdges2 == null) {
			return false;
		}
		SchemConductorEdge start = schemConductorEdges1.getStart() != null ? schemConductorEdges1.getStart() :
				schemConductorEdges1.getEnd();
		SchemConductorEdge end = schemConductorEdges2.getStart() != null ? schemConductorEdges2.getStart() :
				schemConductorEdges2.getEnd();
		if (start == null) {
			return false;
		}
		if (end == null) {
			return false;
		}
		ILocation joint1 = start.getJoinLocation();
		ILocation joint2 = end.getJoinLocation();
		if (joint1 == null || joint2 == null) {
			return false;
		}
		return m_edgesConnecter
				.joinSegments(joint1, joint2, start.getSegment(), end.getSegment());
	}
}

