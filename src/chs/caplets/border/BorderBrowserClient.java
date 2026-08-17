/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border;

import chs.caf.caplet.ICapletController;
import chs.caplets.symbol.BaseBrowserClient;
import chs.cof.symbol.IStamp;
import chs.common.IUID;
import chs.utilities.ResourceMgr;

import java.util.List;

public class BorderBrowserClient extends BaseBrowserClient
{

	public BorderBrowserClient(ICapletController cont)
	{
		super(cont);
	}

	@Override protected void createSymbolOnlyFolders()
	{
		if (isCapitalSuite()) {
			m_drillPointDatumFolder = createFolder(
					ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.DrillPointDatum.Title"));
		}
	}

	@Override protected void addSymbolFolders(IStamp root, List<IUID> children)
	{
		if (m_drillPointDatumFolder != null) {
			children.add(m_drillPointDatumFolder.getUID());
		}
	}
}

