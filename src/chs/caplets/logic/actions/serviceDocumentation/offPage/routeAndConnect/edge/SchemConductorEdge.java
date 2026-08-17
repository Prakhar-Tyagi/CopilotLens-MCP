package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge;

import chs.cof.logical.schem.ILogicSegment;
import chs.common.ILocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemConductorEdge
{

	@Nullable private ILocation joint;
	@NotNull private ILogicSegment segment;

	public SchemConductorEdge(@Nullable ILocation j, @NotNull ILogicSegment seg)
	{
		joint = j;
		segment = seg;
	}

	@Nullable public ILocation getJoinLocation()
	{
		return joint;
	}

	@NotNull public ILogicSegment getSegment()
	{
		return segment;
	}
}
