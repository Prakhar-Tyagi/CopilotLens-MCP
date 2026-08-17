package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.ISupplementaryObject;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.common.ILocation;
import chs.system.FactoryMgr;
import chs.utility.helpers.SegmentHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.Collections;
import java.util.Set;

public class EdgesConnecter
{

	private final IRoutedSegmentEdgesProvider m_routedSegmentEdgesProvider;

	public EdgesConnecter(IRoutedSegmentEdgesProvider routedSegmentEdgesProvider)
	{
		m_routedSegmentEdgesProvider = routedSegmentEdgesProvider;
	}

	public boolean connectPins(ILocation j1, @Nullable ILocation j2, IAbstractSchemPin pin1,
			@Nullable IAbstractSchemPin pin2)
	{
		SchemConductorEdges routedEdges = m_routedSegmentEdgesProvider.getRoutedEdges(j1, j2);
		if (routedEdges == null) {
			return false;
		}
		SchemConductorEdge start = routedEdges.getStart();
		SchemConductorEdge end = routedEdges.getEnd();
		ILogicSegment newStartSeg = null;
		if (start != null) {
			newStartSeg = start.getSegment();
			newStartSeg.connectPin(pin1);
		}
		ILogicSegment newEndSeg = end == null ? null : end.getSegment();
		if (newEndSeg != null && pin2 != null) {
			newEndSeg.connectPin(pin2);
		}
		IDiagramObject parent = getParentDiagramObject(newStartSeg, newEndSeg);
		postProcessSegmentContainer(parent, true, null, null);
		return true;
	}

	public boolean connectSegmentWithPin(ILocation j1, ILocation j2, ILogicSegment oldStartSeg, IAbstractSchemPin pin)
	{
		SchemConductorEdges routedEdges = m_routedSegmentEdgesProvider.getRoutedEdges(j1, j2);
		if (routedEdges == null) {
			return false;
		}
		SchemConductorEdge start = routedEdges.getStart();
		SchemConductorEdge end = routedEdges.getEnd();
		ILogicSegment newStartSeg = null;
		//if a wire is connecting highway and a pin, then it is a new schematic
		//if a highway is connecting conductor and a stackpin, then it is a new schematic
		boolean newSchematic = isNewSchematic(oldStartSeg)
				|| ((oldStartSeg instanceof IHighwaySegment) && (pin instanceof IPin))
				|| ((oldStartSeg instanceof ISegment) && (pin instanceof ISchemStackPin));
		if (start != null) {
			newStartSeg = start.getSegment();
			connectSegments(oldStartSeg, newStartSeg, new Point(j1.getX(), j1.getY()));
		}
		ILogicSegment newEndSeg = end == null ? null : end.getSegment();
		if (newEndSeg != null) {
			newEndSeg.connectPin(pin);
		}
		IDiagramObject parent = getParentDiagramObject(newStartSeg, newEndSeg);
		postProcessSegmentContainer(parent, newSchematic, oldStartSeg.getParent(),
				null);
		return true;
	}

	@Nullable private IDiagramObject getParentDiagramObject(@Nullable ILogicSegment newStartSeg,
			@Nullable ILogicSegment newEndSeg)
	{
		return newStartSeg != null ? newStartSeg.getParent() : (newEndSeg != null ? newEndSeg.getParent() : null);
	}

	public boolean joinSegments(ILocation j1, ILocation j2, ILogicSegment oldStartSeg, ILogicSegment oldEndSeg)
	{
		SchemConductorEdges routedEdges = m_routedSegmentEdgesProvider.getRoutedEdges(j1, j2);
		if (routedEdges == null) {
			return false;
		}
		SchemConductorEdge start = routedEdges.getStart();
		SchemConductorEdge end = routedEdges.getEnd();
		ILogicSegment newStartSeg = null;
		boolean oldStartIsNew = isNewSchematic(oldStartSeg);
		boolean oldEndIsNew = isNewSchematic(oldEndSeg);
		if (start != null) {
			newStartSeg = start.getSegment();
			oldStartIsNew = oldStartIsNew || areDifferentKindOfSegments(oldStartSeg, newStartSeg);
			connectSegments(oldStartSeg, newStartSeg, new Point(j1.getX(), j1.getY()));
		}
		ILogicSegment newEndSeg = end == null ? null : end.getSegment();
		if (newEndSeg != null) {
			oldEndIsNew = oldEndIsNew || areDifferentKindOfSegments(oldEndSeg,newEndSeg);
			connectSegments(oldEndSeg, newEndSeg, new Point(j2.getX(), j2.getY()));
		}
		boolean newSchematic = oldStartIsNew && oldEndIsNew;
		IDiagramObject parent = getParentDiagramObject(newStartSeg, newEndSeg);
		postProcessSegmentContainer(parent, newSchematic, oldStartSeg.getParent(),
				oldEndSeg.getParent());
		return true;
	}

	private boolean areDifferentKindOfSegments(ILogicSegment seg1, ILogicSegment seg2)
	{
		return (seg1 instanceof ISegment && seg2 instanceof IHighwaySegment)
				||
				(seg2 instanceof ISegment && seg1 instanceof IHighwaySegment);
	}

	private boolean isNewSchematic(ILogicSegment segment)
	{
		return segment.isSupplementary();
	}

	private void connectSegments(ILogicSegment sourceCondSegment, ILogicSegment destCondSegment, Point p)
	{
		ILogicSegment dest = destCondSegment;
		ILogicSegment source = sourceCondSegment;
		IDiagramObject parent = source.getParent();
		if ((parent instanceof IConductor && ((IConductor) parent).getHookup() != null)
				|| parent instanceof IHighwaySchematic) {
			ILogicSegment temp = source;
			source = dest;
			dest = temp;
		}
		SegmentHelper
				.connectSegments(FactoryMgr.getCommonFactory(), FactoryMgr.getSchemFactory(), source,
						dest, p);
	}

	private void postProcessSegmentContainer(@Nullable IDiagramObject segmentContainer, boolean newSchem,
			@Nullable IDiagramObject otherSC1, @Nullable IDiagramObject otherSC2)
	{
		if (ILogicSegmentContainer.class.isInstance(segmentContainer)) {
			if (ISupplementaryObject.class.isInstance(segmentContainer) && newSchem) {
				ISupplementaryObject fetchedObject = ISupplementaryObject.class.cast(segmentContainer);
				fetchedObject.markAsSupplementary();
			}
		}
		addToAutoRoute(segmentContainer);
		addOtherSCToAutoRouteIfItIsAHighway(otherSC1);
		addOtherSCToAutoRouteIfItIsAHighway(otherSC2);
	}

	private void addOtherSCToAutoRouteIfItIsAHighway(@Nullable IDiagramObject segmentContainer)
	{
		if (IHighwaySchematic.class.isInstance(segmentContainer)) {
			addToAutoRoute(segmentContainer);
		}
	}

	private void addToAutoRoute(@Nullable IDiagramObject segmentContainer)
	{
		if (ILogicSegmentContainer.class.isInstance(segmentContainer)) {
			Set<ILogicSegmentContainer> conductors =
					Collections.singleton(ILogicSegmentContainer.class.cast(segmentContainer));
			ConductorRouteAction.getInstance().addConductorsForRoute(
					conductors, true);
		}
	}
}

