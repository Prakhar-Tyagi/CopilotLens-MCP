package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge;

import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ILocation;
import chs.common.Location;
import chs.view.route.NoPrototype;
import chs.view.route.blockage.BlockageUtils;
import chs.view.route.blockage.IRouteContext;
import chs.view.utils.DiagramFlowStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class BaseRoutedSegmentEdgesProvider implements IRoutedSegmentEdgesProvider
{

	@NotNull protected final ISchemDiagram m_diagram;

	protected BaseRoutedSegmentEdgesProvider(@NotNull ISchemDiagram diagram)
	{
		m_diagram = diagram;
	}

	@Nullable public SchemConductorEdges getRoutedEdges(ILocation j1, @Nullable ILocation j2)
	{
		return getEdges(j1, j2);
	}

	@Nullable protected SchemConductorEdges getEdges(ILocation j1, @Nullable ILocation j2)
	{
		ILocation firstLocation = j1;
		ILocation secondLocation = j2;
		if (j2 == null) {
			secondLocation = getSecondLocation(j1);
//			secondLocation = j1;
		}
		List<ILocation> iLocations =
				BlockageUtils.routeConductor(m_diagram, firstLocation, secondLocation, true, Collections.emptyList(),
						Collections.emptySet(), IRouteContext.RouteGraphSize.MINIMUM, NoPrototype.NO_PROTOTYPE, 1,
						DiagramFlowStyle.DEFAULT, false, false, null, false);
		List<ILogicSegment> allSegments = new ArrayList<>(constructDisplayObject(iLocations));
		SchemConductorEdgesHelper helper = new SchemConductorEdgesHelper();
		SchemConductorEdges schemConductorEdges =
				helper.getSchemConductorEdges(firstLocation, secondLocation, allSegments);
		return schemConductorEdges;
	}

	@NotNull private ILocation getSecondLocation(ILocation j1)
	{
		ILocation secondLocation = new Location(j1);
		int i = 5;
		int gridSpacing = m_diagram.getGrid().getGridSpacing();
		secondLocation.setX(secondLocation.getX() + (gridSpacing * i));
		secondLocation.setY(secondLocation.getY() + (gridSpacing * i));
		return secondLocation;
	}

	protected abstract List<? extends ILogicSegment> constructDisplayObject(List<ILocation> iLocations);
}
