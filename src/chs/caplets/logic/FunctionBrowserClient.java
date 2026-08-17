/*
 * Copyright 2002-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IUID;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionBrowserClient extends BrowserClient
{

	public FunctionBrowserClient(ICapletController controller)
	{
		super(controller);
	}

	protected void buildChildrenFolders()
	{
		doCreateObjectFolder(LogicFolder.FUNCTION_COMPONENT);
		doCreateObjectFolder(LogicFolder.LOGIC_BLOCKS);
		doCreateObjectFolder(LogicFolder.FUNCTION_CONDUCTOR);
		doCreateObjectFolder(LogicFolder.FUNCTION_MESSAGE);
		doCreateObjectFolder(LogicFolder.HIGHWAYS);
	}

	protected List<IUID> getUnusedFunctionConductors()
	{
		IConnectivity conn = getDesignConnectivity();
		List<IUID> unused = new ArrayList<IUID>();
		addUnusedObjects(unused,
				conn.getFunctionConductors().stream().filter(cond -> !cond.isAssociatedMessageSignal()).collect(
						Collectors.toSet()).iterator());
		return unused;
	}

	protected boolean cancHeckDWUSage(ILogicObject object)
	{
		if (object instanceof IFunctionConductor && ((IFunctionConductor) object).isAssociatedMessageSignal()) {
			return false;
		}
		return true;
	}
}
