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
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rmahato
 */
public class HighwayDetailsTableInfo extends InstanceDetailsTableInfo
{

	private static final String DIAGRAM_COLOUM_NAME =
			ResourceMgr.getString(HighwayDetailsTableInfo.class, "HighwayDetailsTableInfo.diagramColoumnName.text");
	private static final String PINLISTS_COLOUM_NAME = ResourceMgr
			.getString(HighwayDetailsTableInfo.class, "HighwayDetailsTableInfo.pinlistsColoumnName.text");

	public HighwayDetailsTableInfo()
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, "");
		coloumValueMap.put(PINLISTS_COLOUM_NAME, "");
	}

	public HighwayDetailsTableInfo(@NotNull ISchemDiagram diagram, @NotNull IDiagramObject highway)
	{
		instanceDiagram = diagram;
		m_diagramObject = highway;
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, diagram.getName());
		coloumValueMap.put(PINLISTS_COLOUM_NAME, getAllTerminatingPinlistNames(highway));
	}

	@NotNull @Override
	protected DetailsTableInfo createInstanceDetailObject(@NotNull ISchemDiagram diagram,
			@NotNull IDiagramObject diagramObject)
	{
		return new HighwayDetailsTableInfo(diagram, diagramObject);
	}

	@NotNull private String getAllTerminatingPinlistNames(@NotNull IDiagramObject highway)
	{
		IHighwaySchematic highwaySchematic = (IHighwaySchematic) highway;
		Set<IPinList> pinOwners = new HashSet<>();
		for (IUID stackPinUID : highwaySchematic.getConnectedStackPins()) {
			ISchemStackPin stackPin = (ISchemStackPin) stackPinUID.getObject();
			if (stackPin != null) {
				stackPin.getConnectivityUIDs().stream().forEach(uid ->
				{
					IUIDObject object = uid.getObject();
					if (object != null) {
						pinOwners.add(((IGenericPin) object).getOwner());
					}
				});
			}
		}

		return pinOwners.stream().map(pinlist -> pinlist.getName()).collect(Collectors.joining(", "));
	}
}
