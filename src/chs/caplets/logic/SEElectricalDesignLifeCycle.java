package chs.caplets.logic;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.LifecycleTypeIterator;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.ResourceMgr;

import java.util.Collections;


public class SEElectricalDesignLifeCycle extends LogicLifecycle
{

	public SEElectricalDesignLifeCycle(ICaplet caplet)
	{
		super(caplet);
	}

	protected ICapletController createController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		return new SEElectricalDesignController(caplet, design, diagram);
	}

	public LifecycleTypeIterator getTypesForFilter()
	{
		return new LifecycleTypeIterator(Collections.EMPTY_LIST.iterator());
	}

	@Override protected void doForceClosePrompt(String headMessage, ActionType actionType)
	{
		getStatusReporter().showInformationMessage(getParentFrame(), headMessage,
				ResourceMgr.getString(getResourceClass(), "BaseLifecycle.msg.noRevision.forceClose"));
	}
}
