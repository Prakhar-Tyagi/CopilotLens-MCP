/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.topology.inlineassist.IInlineAssistConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISystemLogicDiagram;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class holds the data required to create inline connectors on the diagram.
 */
public class DiagramConnectorData implements IDiagramConnectorData
{

	private final ISystemLogicDiagram diagram;
	private final List<IConductor> conductors;
	private final Collection<IInlineAssistConductor> logicalConductors;
	private final Set<IgnoredConductorInformation> ignoredConductors;
	private final Collection<NewConnectorData> connectorData;

	public DiagramConnectorData(@NotNull ISystemLogicDiagram diagram, @NotNull List<IConductor> conductors,
			@NotNull Collection<IInlineAssistConductor> logicalConductors,
			@NotNull Set<IgnoredConductorInformation> ignoredConductors,
			@NotNull Collection<NewConnectorData> connectorData)
	{
		this.diagram = diagram;
		this.conductors = conductors;
		this.logicalConductors = logicalConductors;
		this.ignoredConductors = ignoredConductors;
		this.connectorData = connectorData;
	}

	@Override @NotNull
	public List<IConductor> getConductors()
	{
		return conductors;
	}

	@Override @NotNull
	public Set<IgnoredConductorInformation> getIgnoredConductors()
	{
		return ignoredConductors;
	}

	@Override @NotNull
	public Collection<IInlineAssistConductor> getLogicalConductors()
	{
		return logicalConductors;
	}

	@Override @NotNull
	public Collection<INewConnectorData> getConnectorData()
	{
		return connectorData.stream()
				.map(INewConnectorData.class::cast)
				.collect(Collectors.toList());
	}

	@Override @NotNull
	public ISystemLogicDiagram getDiagram()
	{
		return diagram;
	}
}
