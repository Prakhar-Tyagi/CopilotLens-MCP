/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caf.caplet.helpers.snapping;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IDesignContainer;
import chs.system.FactoryMgr;
import chs.utility.logic.DesignHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * holds cable connector reference for schem connector placeholder
 */
public class ConnectorPlaceholder
{

	@NotNull private ISchemDiagram m_diagram;
	@Nullable private IHarnessPlugConnector m_connector;
	@NotNull private String m_name;
	private boolean m_isMated;
	private boolean m_isNew;

	ConnectorPlaceholder(@NotNull ISchemDiagram diagram, @NotNull IHarnessPlugConnector refConnector,
			boolean isMated)
	{
		m_diagram = diagram;
		m_connector = refConnector;
		m_name = refConnector.getName();
		m_isMated = isMated;
		m_isNew = false;
	}

	ConnectorPlaceholder(@NotNull ISchemDiagram diagram, @NotNull String name)
	{
		m_diagram = diagram;
		m_connector = null;
		m_name = name;
		m_isMated = false;
		m_isNew = true;
	}

	@Nullable IHarnessPlugConnector getConnector()
	{
		return m_connector;
	}

	@NotNull public ISchemDiagram getDiagram()
	{
		return m_diagram;
	}

	@NotNull public ILogicDesign getDesign()
	{
		IDesignContainer designContainer = getDiagram().getDesignContainer();
		if (designContainer == null) {
			throw new IllegalStateException();
		}
		return DesignHelper.getDesignNotNull(designContainer, ILogicDesign.class);
	}

	@NotNull public IHarnessPlugConnector getTranformedConnector()
	{
		if (m_connector == null) {
			m_connector = createCableConnector();
		}
		return m_connector;
	}

	public boolean isMated()
	{
		return m_isMated;
	}

	@NotNull public String getName()
	{
		return m_name;
	}

	@NotNull private IHarnessPlugConnector createCableConnector()
	{
		IHarnessPlugConnector plugConnector =
				FactoryMgr.getCableFactory().createPlugConnector(FactoryMgr.createUID());
		ILogicDesign logicDesign = getDesign();
		IConnectivity connectivity = logicDesign.getConnectivity();
		if (connectivity != null) {
			connectivity.addConnector(plugConnector);
		}
		return plugConnector;
	}

	@Nullable public IHarnessPlugConnector getExistingConnector()
	{
		return m_isNew ? null : m_connector;
	}
}