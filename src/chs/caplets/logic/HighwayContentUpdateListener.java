package chs.caplets.logic;

import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.cof.logical.ILogicDesign;
import chs.common.IUID;
import chs.utility.helpers.HighwayHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 9 Mar, 2011 Time: 1:36:04 PM To change this template use File |
 * Settings | File Templates.
 */
public class HighwayContentUpdateListener implements IModelChangeListener
{

	protected ILogicDesign m_logicDesign;

	public HighwayContentUpdateListener(@NotNull ILogicDesign logicDesign)
	{
		m_logicDesign = logicDesign;
	}

	@Override public void modelPreChanged(ModelChangeEvent e)
	{
		Collection<IUID> changedObjectsUIDs = e.getChangedObjectsUIDs();
		HighwayHelper.updateHighways(changedObjectsUIDs, m_logicDesign);
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{
	}
}
