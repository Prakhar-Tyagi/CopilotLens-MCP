package chs.caplets.logic.shared;

import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.shared.ISharedHighwayConnectionMgr;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IDeletedObject;
import chs.system.UIDMgr;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: Sep 1, 2009 Time: 10:47:59 PM
 */
public class SharedHighwayConnectionsListener implements IModelChangeListener
{

	protected ILogicDesign m_logicDesign = null;
	protected IProject m_projet = null;
	protected ISharedHighwayConnectionMgr m_highwayconnections = null;

	public SharedHighwayConnectionsListener(@NotNull ILogicDesign logicDesign)
	{
		m_logicDesign = logicDesign;
		m_projet = logicDesign.getProject();
		assert m_projet != null;

		m_highwayconnections = m_projet.getSharedHighwayConnectionMgr();
	}

	public void modelPreChanged(ModelChangeEvent e)
	{
	}

	public void modelChanged(ModelChangeEvent e)
	{
		if (hasHighway(e.getChangedObjectsUIDs())) {
			m_highwayconnections.update(m_logicDesign);
			m_projet.getSharedConductorMgr().fireChangeEvent();
		}
	}

	private boolean hasHighway(Collection<IUID> changedObjectsUIDs)
	{
		for (IUID uid : changedObjectsUIDs) {
			IUIDObject obj = UIDMgr.getObject(uid);
			if (obj instanceof IDeletedObject) {
				obj = ((IDeletedObject) obj).getOriginalObject();
			}
			else if (obj instanceof IHighwayConductor) {
				if (!((IHighwayConductor) obj).getHighways().isEmpty()) {
					return true;
				}
			}

			if (obj instanceof IHighwaySchematic || obj instanceof IHighwaySegment || obj instanceof IHighway) {
				return true;
			}
		}

		return false;
	}
}
