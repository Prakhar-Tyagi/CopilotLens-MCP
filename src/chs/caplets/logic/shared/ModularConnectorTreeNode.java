/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic.shared;

import chs.caplets.logic.actions.shared.helper.ModularConnectorHandler.IConnectorNode;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Stores the icon corresponding to the connector of the Tree Node.
 */
public class ModularConnectorTreeNode extends DefaultMutableTreeNode
{

	@NotNull private final Icon m_icon;

	public ModularConnectorTreeNode(@NotNull IConnectorNode rNode)
	{
		super(rNode);
		m_icon = IconUtils.getIcon(rNode.getConnector());
	}

	@NotNull Icon getIcon()
	{
		return m_icon;
	}
}
