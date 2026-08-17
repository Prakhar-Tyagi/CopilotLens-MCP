/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Instance details table information for function signals and function messages
 */
public class FunctionBaseConductorDetailsTableInfo extends InstanceDetailsTableInfo
{

	private static final String DIAGRAM_COLUMN_NAME = ResourceMgr
			.getString(FunctionBaseConductorDetailsTableInfo.class,
					"FunctionBaseConductorDetailsTableInfo.diagramColumnName.text");
	private static final String PORTS_COLUMN_NAME = ResourceMgr
			.getString(FunctionBaseConductorDetailsTableInfo.class,
					"FunctionBaseConductorDetailsTableInfo.portsColumnName.text");

	public FunctionBaseConductorDetailsTableInfo()
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLUMN_NAME, "");
		coloumValueMap.put(PORTS_COLUMN_NAME, "");
	}

	public FunctionBaseConductorDetailsTableInfo(@NotNull ISchemDiagram diagram, @NotNull IDiagramObject diagramObject)
	{
		instanceDiagram = diagram;
		m_diagramObject = diagramObject;
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLUMN_NAME, diagram.getName());
		coloumValueMap.put(PORTS_COLUMN_NAME, getAllPortNames(diagramObject));
	}

	@NotNull @Override protected DetailsTableInfo createInstanceDetailObject(@NotNull ISchemDiagram diagram,
			@NotNull IDiagramObject diagramObject)
	{
		return new FunctionBaseConductorDetailsTableInfo(diagram, diagramObject);
	}

	@NotNull private String getAllPortNames(@NotNull IDiagramObject diagramObject)
	{
		IConductor conductor = CommonUtils.cast(diagramObject, IConductor.class);
		String portNames =
				conductor != null ? conductor.getPins().stream().map(port -> port.getConnectivity().getName()).sorted()
						.collect(Collectors.joining(", ")) : "";
		return portNames;
	}
}
