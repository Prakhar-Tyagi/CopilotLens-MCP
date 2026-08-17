/*
 * Copyright 2008-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.LifecycleTypeIterator;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * FEAT13132 - VeSys Packaging.
 * <p/>
 * This is the caplet lifecycle used for VeSys Design.
 * <p/>
 *
 * @author rjoseph
 */

public class VeSysDesignLifecycle extends LogicLifecycle
{

	public VeSysDesignLifecycle(Caplet caplet)
	{
		super(caplet);
	}

	protected ICapletController createController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		return new VeSysDesignController(caplet, design, diagram);
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

	@Override protected void addElaActions(@NotNull ICaplet caplet)
	{
		// ELA not supported in VeSys - for some reason the ApplicationSpecification annotation doesn't seem to work for
		// lifecycle actions.
	}
}
