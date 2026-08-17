package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge;

import chs.common.ILocation;
import org.jetbrains.annotations.Nullable;

public interface IRoutedSegmentEdgesProvider
{

	@Nullable SchemConductorEdges getRoutedEdges(ILocation j1, @Nullable ILocation j2);
}
