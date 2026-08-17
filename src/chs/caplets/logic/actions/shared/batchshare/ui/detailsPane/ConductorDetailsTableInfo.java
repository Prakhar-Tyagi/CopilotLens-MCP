/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * @author rmahato
 */
public class ConductorDetailsTableInfo extends InstanceDetailsTableInfo
{

	private static final String DIAGRAM_COLOUM_NAME = ResourceMgr
			.getString(ConductorDetailsTableInfo.class, "ConductorDetailsTableInfo.diagramColoumnName.text");
	private static final String PINS_COLOUM_NAME = ResourceMgr
			.getString(ConductorDetailsTableInfo.class, "ConductorDetailsTableInfo.pinsColoumnName.text");

	public ConductorDetailsTableInfo()
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, "");
		coloumValueMap.put(PINS_COLOUM_NAME, "");
	}

	public ConductorDetailsTableInfo(@NotNull ISchemDiagram diagram, @NotNull IDiagramObject diagramObject)
	{
		instanceDiagram = diagram;
		m_diagramObject = diagramObject;
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, diagram.getName());
		coloumValueMap.put(PINS_COLOUM_NAME, getAllPinNames(diagramObject));
	}

	@NotNull @Override
	protected DetailsTableInfo createInstanceDetailObject(@NotNull ISchemDiagram diagram,
			@NotNull IDiagramObject diagramObject)
	{
		return new ConductorDetailsTableInfo(diagram, diagramObject);
	}

	@NotNull private String getAllPinNames(@NotNull IDiagramObject diagramObject)
	{
		IConductor conductor = (IConductor) diagramObject;
		return conductor.getPins().stream().map(pin -> pin.getConnectivity().getName()).distinct()
				.collect(Collectors.joining(", "));
	}
}
