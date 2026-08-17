/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.IBrowserClient;
import chs.cof.logical.cable.IInterconnectToDoItem;
import chs.cof.parts.ILibraryInnerCore;
import chs.common.IObjectFilter;

public class InterconnectSourceBrowserTree extends GenericBrowserTree
{

	private static final IObjectFilter LIBRARY_FILTER = new IObjectFilter()
	{
		public boolean accept(Object obj)
		{
			return obj instanceof IInterconnectToDoItem
					|| obj instanceof ILibraryInnerCore;
		}
	};

	public InterconnectSourceBrowserTree(IBrowserClient client, String name)
	{
		super(client, name);
		setRootVisible(false);
	}

	protected IObjectFilter getSelectionFilter()
	{
		return LIBRARY_FILTER;
	}

	protected void registerListeners()
	{
		// add this as a model change listener
		m_client.getController().getCapletModel().addModelChangeListener(this);

		// add this as a model activation listener
		m_client.getController().getCapletModel().addModelActivationListener(this);
	}

	protected void unregisterListeners()
	{
		// add this as a model change listener
		m_client.getController().getCapletModel().removeModelChangeListener(this);

		// add this as a model activation listener
		m_client.getController().getCapletModel().removeModelActivationListener(this);
	}
}
