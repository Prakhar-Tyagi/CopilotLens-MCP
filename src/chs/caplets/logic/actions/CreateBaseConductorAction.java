package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.ISmartPoint;
import chs.utilities.CollectionUtils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class CreateBaseConductorAction extends LogicMultipointCreateAction
{

	//    static private Cursor m_conductorCursor = CAFUtils.getInstance().loadCursor( "images/ConductorCursor", new Point( 0,0));
	protected CreateSchemConductorCmd m_cmd = null;
	protected List<IDynamicGfxMediator> m_connectingObjects;

	protected CreateBaseConductorAction(ICapletController controller, boolean snapToGrid,
			boolean snapToSubgrid)
	{
		super(controller, snapToGrid, snapToSubgrid);
	}

	/**
	 * Conductor creation uses guidelines
	 */
	protected Set<Class<?>> guideLineClasses()
	{
		Set<Class<?>> result = new HashSet<Class<?>>();
		result.add(IConductor.class);
		result.add(IGeneralHighway.class);

		return result;
	}

	@Override
	public boolean onTerminate(boolean successful) {
		boolean b = super.onTerminate(successful);

		if (successful) {
			if (connectingObjects() != null && connectingObjects().size() == 1 && !m_orthoMode) {
				if (m_cmd != null && m_cmd.getPoints() != null && m_cmd.getPoints().size() == 2) {
					ConductorRouteAction.getInstance()
							.addSegmentForRoute((ISegment) connectingObjects().iterator().next());
					ConductorRouteAction.getInstance().processAction();
				}
			}
		}
		m_cmd = null;    // clear until the next onActivate
		return b;
	}

	protected abstract void assignLibraryDetails();

	protected abstract Class<? extends chs.cof.logical.cable.IConductor> getConductorType();

	protected Model getLocalModel()
	{
		return (Model) super.getModel();
	}

	/**
	 * @param point_list Description of the Parameter
	 *
	 * @return Description of the Return Value
	 */
	protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		List<Point> points = new ArrayList<Point>(point_list.size());
		for (ISmartPoint spt : point_list) {
			points.add(spt.getAbsoluteLocation());
		}

		m_cmd.setDesign(getLocalModel().getDesign());
		m_cmd.setDiagram((ISchemDiagram) getModel().getSheet());
		m_cmd.setPoints(points);
		m_cmd.setConductorType(getConductorType());
		m_cmd.setPrune(true);

		m_cmd.execute();
		m_connectingObjects = CollectionUtils.getObjectList(m_cmd.getSegments(), IDynamicGfxMediator.class);
		assignLibraryDetails();
		return m_cmd.getConductor();
	}
}
