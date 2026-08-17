/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2019-2024 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.cmd.CreateSchemGeneralHighwayCmd;
import chs.common.ILocation;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoutedHighwaySegmentEdges extends BaseRoutedSegmentEdgesProvider
{

	private final IGeneralHighway m_conductor;

	public RoutedHighwaySegmentEdges(@NotNull ISchemDiagram diagram, IGeneralHighway conductor)
	{
		super(diagram);
		m_conductor = conductor;
	}

	protected List<IHighwaySegment> constructDisplayObject(List<ILocation> iLocations)
	{
		CreateSchemGeneralHighwayCmd cmd =
				new CreateSchemGeneralHighwayCmd(ConductorRouteAction.getInstance());
		ILogicDesign design = m_diagram.getDesign();
		if (design == null) {
			return Collections.emptyList();
		}
		List<Point> points = new ArrayList<Point>(iLocations.size());
		for (ILocation spt : iLocations) {
			points.add(new Point(spt.getX(), spt.getY()));
		}
		cmd.setCableHighway(m_conductor);
		cmd.setDesign(design);
		cmd.setDiagram(m_diagram);
//		cmd.setOrthoMode(true);
		cmd.setPoints(points);
//		cmd.setPrune(true);
		cmd.execute();
		return cmd.getSegments();
	}
}
