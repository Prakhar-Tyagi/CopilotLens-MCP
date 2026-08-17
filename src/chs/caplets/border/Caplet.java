/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border;

import chs.caf.IResource;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletLifecycle;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;

// caf imports

@ApplicationSpecification(
		includeIn = {Application.CapitalEssentialsSymbolDesigner, Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture,
						Application.XSCSymbol, Application.SEElectricalSymbol})
public class Caplet extends chs.caplets.symbol.Caplet
{

	public Caplet()
	{
	}

	// The name of the caplet
	public String getName()
	{
		return AppInfo.getFullApplicationName("Border");
	}

	/**
	 * @return name of the designs created by this caplet
	 */
	public String getDesignType()
	{
		return ResourceMgr.getString(this, "Caplet.Design.Type");
	}

	@Override public String getUnlocalizedDesignType(){
		return "Border";
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new Lifecycle(this);
	}

	protected IResource createResource()
	{
		return new Resource(this);
	}

	@Override public Type getType()
	{
		return Type.BORDER_SYMBOL;
	}
}
