/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2024 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;

/**
 * Created by IntelliJ IDEA. User: zali Date: May 25, 2009 Time: 4:39:07 PM To change this template use File | Settings
 * | File Templates.
 */
public abstract class CreateHighwayActionUI extends ActionUI
{

	protected CreateHighwayActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	protected CreateHighwayActionUI(ICaplet caplet, boolean addActionUI)
	{
		super(caplet, addActionUI);
	}

	protected CreateHighwayActionUI(ICaplet caplet, String instanceName)
	{
		super(caplet, instanceName);
	}

	protected CreateHighwayActionUI(ICaplet caplet, boolean addActionUI, String instanceName)
	{
		super(caplet, addActionUI, instanceName);
	}
}
