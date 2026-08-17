/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.AbstractShareRow;
import chs.caplets.logic.actions.shared.batchshare.IConnectivityInfo;
import chs.caplets.logic.actions.shared.batchshare.IInlineConnectorInfo;
import chs.caplets.logic.actions.shared.batchshare.IObjectInfo;
import chs.caplets.logic.actions.shared.batchshare.ShareableEntityTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.common.DesignUtils;
import chs.common.IUID;
import chs.common.attr.IAttributeType;
import chs.system.FactoryMgr;
import chs.utilities.IXMLTags;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class BatchShareRow extends AbstractShareRow implements IBatchShareRow
{
	@NotNull private String m_name;
	private Action action;
	@NotNull private IBatchShareGroup group;
	private ShareableEntityTypeEnum m_objectType;
	private IObjectInfo underlyingObjectInfo;

	public BatchShareRow(@NotNull IObjectInfo objectInfo, @NotNull IBatchShareGroup group)
	{
		super(DesignUtils.getDesign(objectInfo.getDesignUID(), ILogicDesign.class));
		underlyingObjectInfo = objectInfo;
		m_name = getObjectDisplayName(objectInfo);
		m_objectType = underlyingObjectInfo.getType();
		action = Action.SHARE_INTO;
		this.group = group;
	}

	/**
	 * Generates display name for an object, handling inline connectors specially
	 * by concatenating mated connector names with a colon separator.
	 */
	@NotNull private String getObjectDisplayName(@NotNull IObjectInfo objectInfo)
	{
		String name = StringUtils.nonNull(objectInfo.getAttributeValue(IXMLTags.NAME));
		String mateName = StringUtils.EMPTY_STRING;
		if (objectInfo instanceof IInlineConnectorInfo inline) {
			IInlineConnectorInfo matedConnector = inline.getMatedConnector();
			mateName = matedConnector.getAttributeValue(IXMLTags.NAME);
		}
		return StringUtils.isBlank(mateName) ? name : StringUtils.concatenate(name, StringUtils.COLON, mateName);
	}

	@NotNull @Override public String getName()
	{
		return m_name;
	}

	@NotNull @Override public String getMatchBy()
	{
		return getGroup().getMatchCriteriaValues();
	}

	@NotNull @Override public Action getAction()
	{
		return action;
	}

	@Override public void setAction(Action action)
	{
		this.action = action;
	}

	@Nullable @Override public String getAttributeValue(@NotNull IAttributeType attribute)
	{
		return underlyingObjectInfo.getAttributeValue(attribute.getXMLName());
	}

	@Nullable @Override public String getPropertyValue(@NotNull String propertyName)
	{
		return underlyingObjectInfo.getPropertyValue(propertyName);
	}

	@Nullable @Override public IConnectivityInfo getConnectivityInfo()
	{
		return underlyingObjectInfo.getConnectivityInfo();
	}

	@NotNull @Override public IBatchShareGroup getGroup()
	{
		return group;
	}

	@Override @NotNull public ShareableEntityTypeEnum getObjectType()
	{
		return m_objectType;
	}

	@Override @NotNull public IUID getObjectUID()
	{
		return FactoryMgr.getCommonFactory().constructUID(underlyingObjectInfo.getUID());
	}

	@Override @NotNull public IUID getDesignUID()
	{
		return FactoryMgr.getCommonFactory().constructUID(underlyingObjectInfo.getDesignUID());
	}

	@Override public boolean isValid()
	{
		return group.isValid();
	}

}
