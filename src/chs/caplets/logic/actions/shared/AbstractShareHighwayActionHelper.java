package chs.caplets.logic.actions.shared;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedGeneralHighway;
import chs.utilities.CollectionUtils;
import chs.utility.DiagramHelper;
import chs.utility.PortHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.HighwayShareHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * share/share-into implementation specific to highway
 */
public abstract class AbstractShareHighwayActionHelper extends AbstractBaseShareConductorActionHelper<ISharedGeneralHighway>
{

	protected AbstractShareHighwayActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram)
	{
		super(design, diagram, HighwayShareHelper.getInstance());
	}

	@Override protected Iterator<ISharedGeneralHighway> getShareObjectsUsedOnDesign()
	{
		return m_design.getSharedHighways();
	}

	@NotNull @Override protected Set<ISharedGeneralHighway> getOrderedSharedConductors(@NotNull ISharedConductorMgr condMgr)
	{
		//dts0100874550 this set must be ordered because we want design used shared highways to come first.
		Set<ISharedGeneralHighway> shHighwayOrderedSet = new LinkedHashSet<ISharedGeneralHighway>();
		shHighwayOrderedSet.addAll(CollectionUtils.createList(getShareObjectsUsedOnDesign()));
		shHighwayOrderedSet.addAll(CollectionUtils.createList(condMgr.getSharedGeneralHighways()));
		return shHighwayOrderedSet;
	}

	@Override protected boolean hasDuplicateName(@NotNull ISharedGeneralHighway sharedObject)
	{
		return sharedObject.getName().equalsIgnoreCase(m_logicObject.getName());
	}

	protected void transferConnectivity(ILogicObject logicObject)
	{
		if (m_logicObject instanceof IGeneralHighway && logicObject instanceof IGeneralHighway) {
			HighwayHelper.transferConnectivity((IGeneralHighway) m_logicObject, (IGeneralHighway) logicObject);
		}
	}

	protected void reassignConnectivityForSchematic(IDiagramObject diagObj, ILogicObject logicObject)
	{
		IHighwaySchematic schemHighway = (IHighwaySchematic) diagObj;
		schemHighway.setConnectivity((IHighway) logicObject);
		ISchemDiagram diagram = DiagramHelper.getDiagram(diagObj);
		if (diagram == null) {
			return;
		}
		PortHelper.updatePortGfx(schemHighway, diagram.getGrid().getGridSpacing());
	}
}
