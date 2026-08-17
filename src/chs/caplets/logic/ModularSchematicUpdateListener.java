package chs.caplets.logic;

import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.cof.logical.ILogicDesign;
import chs.utility.logic.ModularConnectorSchematicGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 10-01-2019.
 */
public class ModularSchematicUpdateListener implements IModelChangeListener
{

	protected ILogicDesign m_logicDesign;

	public ModularSchematicUpdateListener(@NotNull ILogicDesign logicDesign)
	{
		m_logicDesign = logicDesign;
	}

	@Override public void modelPreChanged(@NotNull ModelChangeEvent e)
	{
		ModularConnectorSchematicGenerator.getInstance()
				.updateSchemticConnectors(m_logicDesign, e.getChangedObjectsUIDs());
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{
	}
}
