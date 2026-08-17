/*
 * Copyright 2003-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol;

import chs.caf.caplet.ICapletController;
import chs.common.IUID;

import java.util.List;

public class BrowserClient extends BaseBrowserClient
{

	public BrowserClient(ICapletController cont)
	{
		super(cont);
	}

	@Override protected void createFormboardRegionFolder()
	{
		// no formboard region in symbol caplet
	}

	protected void addBorderFolders(List<IUID> children)
	{
		// no border folders in symbol caplet
	}

	protected void createUserDefinedZoneFolder()
	{
		// no user defined zones folders in symbol caplet
	}
}

