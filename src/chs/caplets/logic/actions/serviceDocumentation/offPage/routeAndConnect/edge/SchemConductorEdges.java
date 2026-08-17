package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SchemConductorEdges
{

	@Nullable private SchemConductorEdge m_start;
	@Nullable private SchemConductorEdge m_end;

	public SchemConductorEdges(@NotNull SchemConductorEdge start, @Nullable SchemConductorEdge end)
	{
		m_start = start;
		m_end = end;
	}

	@Nullable public SchemConductorEdge getStart()
	{
		return m_start;
	}

	@Nullable public SchemConductorEdge getEnd()
	{
		return m_end;
	}
}
