package chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.edge;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.common.ILocation;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class RoutedConductorSegmentEdges extends BaseRoutedSegmentEdgesProvider
{

	private final IConductor m_conductor;

	public RoutedConductorSegmentEdges(@NotNull ISchemDiagram diagram, IConductor conductor)
	{
		super(diagram);
		m_conductor = conductor;
	}

	protected List<ISegment> constructDisplayObject(List<ILocation> iLocations)
	{
		CreateSchemConductorCmd cmd =
				new CreateSchemConductorCmd(ConductorRouteAction.getInstance());
		ILogicDesign design = m_diagram.getDesign();
		assert design != null;
		List<Point> points = new ArrayList<Point>(iLocations.size());
		for (ILocation spt : iLocations) {
			points.add(new Point(spt.getX(), spt.getY()));
		}
		cmd.setCableConductor(m_conductor);
		cmd.setDesign(design);
		cmd.setDiagram(m_diagram);
		cmd.setOrthoMode(true);
		cmd.setPoints(points);
		setConductorType(cmd);
		cmd.execute();
		List<ISegment> segments = cmd.getSegments();
		return segments;
	}

	private void setConductorType(CreateSchemConductorCmd cmd)
	{
		cmd.setConductorType(IWireConductor.class);
		if (IShieldConductor.class.isInstance(m_conductor)) {
			cmd.setConductorType(IShieldConductor.class);
		}
		if (INetConductor.class.isInstance(m_conductor)) {
			cmd.setConductorType(INetConductor.class);
		}
	}
}
