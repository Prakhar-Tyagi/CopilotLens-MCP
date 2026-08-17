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
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * @author rmahato
 */
public class SpliceDetailsTableInfo extends InstanceDetailsTableInfo
{

	private static final String DIAGRAM_COLOUM_NAME =
			ResourceMgr.getString(SpliceDetailsTableInfo.class, "SpliceDetailsTableInfo.diagramColoumnName.text");
	private static final String CONDUCTORS_COLOUM_NAME = ResourceMgr
			.getString(SpliceDetailsTableInfo.class, "SpliceDetailsTableInfo.conductorsColoumnName.text");

	public SpliceDetailsTableInfo()
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, "");
		coloumValueMap.put(CONDUCTORS_COLOUM_NAME, "");
	}

	@NotNull @Override
	protected DetailsTableInfo createInstanceDetailObject(@NotNull ISchemDiagram diagram,
			@NotNull IDiagramObject diagramObject)
	{
		return new SpliceDetailsTableInfo(diagram, diagramObject);
	}

	public SpliceDetailsTableInfo(@NotNull ISchemDiagram diagram, @NotNull IDiagramObject diagramObject)
	{
		instanceDiagram = diagram;
		m_diagramObject = diagramObject;
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, diagram.getName());
		coloumValueMap.put(CONDUCTORS_COLOUM_NAME, getAllTerminatingConductorNames(diagramObject));
	}

	@NotNull private String getAllTerminatingConductorNames(@NotNull IDiagramObject diagramObject)
	{
		IPinList splice = (IPinList) diagramObject;
		IPin pin = splice.getPins().iterator().next();
		return pin.getConductors().stream().map(conductor -> conductor.getConnectivity().getName()).distinct()
				.collect(Collectors.joining(", "));
	}
}
