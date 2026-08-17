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
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * @author rmahato
 */
public class InlineDetailsTableInfo extends PinlistDetailsTableInfo
{

	private static final String DIAGRAM_COLOUM_NAME =
			ResourceMgr.getString(InlineDetailsTableInfo.class, "InlineDetailsTableInfo.diagramColoumnName.text");
	private static final String JACK_PINS_COLOUM_NAME =
			ResourceMgr.getString(InlineDetailsTableInfo.class, "InlineDetailsTableInfo.jackPinColoumnName.text");
	private static final String PLUG_PINS_COLOUM_NAME =
			ResourceMgr.getString(InlineDetailsTableInfo.class, "InlineDetailsTableInfo.plugPinColoumnName.text");

	public InlineDetailsTableInfo()
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, "");
		coloumValueMap.put(JACK_PINS_COLOUM_NAME, "");
		coloumValueMap.put(PLUG_PINS_COLOUM_NAME, "");
	}

	public InlineDetailsTableInfo(@NotNull ISchemDiagram diagram, @NotNull IDiagramObject diagramObject)
	{
		IPinList jackSchem = (IPinList) diagramObject;
		Collection<IPinList> attachedPinlists = jackSchem.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR);
		assert attachedPinlists.iterator().hasNext();
		IPinList plugSchem = attachedPinlists.iterator().next();

		instanceDiagram = diagram;
		m_diagramObject = diagramObject;
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, diagram.getName());
		coloumValueMap.put(JACK_PINS_COLOUM_NAME, getAllPinNames(jackSchem));
		coloumValueMap.put(PLUG_PINS_COLOUM_NAME, getAllPinNames(plugSchem));
	}

	@NotNull @Override
	protected DetailsTableInfo createInstanceDetailObject(@NotNull ISchemDiagram diagram,
			@NotNull IDiagramObject diagramObject)
	{
		return new InlineDetailsTableInfo(diagram, diagramObject);
	}
}
