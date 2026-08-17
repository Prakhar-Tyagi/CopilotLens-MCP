/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.caplets.logic.actions.shared.HyperLinkStatusMessage;
import chs.caplets.logic.actions.shared.IHyperLinkStatusMessage;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Status message for testing
 */
public class HarnessPropagateStatusMessage extends HyperLinkStatusMessage implements IHarnessPropagateStatusMessage
{

	@NotNull private String m_objectType;
	@NotNull private String m_previousHarness;
	@NotNull private String m_currentHarness;
	private boolean m_propagate = true;
	@NotNull private IHarnessPropagateStatusMessageGroup m_group;
	@NotNull private IUID m_objectUid;
	@Nullable private IUID m_designUid;
	private HarnessPropagateMessageType m_messageType;
	private boolean m_isEditable;


	public HarnessPropagateStatusMessage(@NotNull HarnessPropagateMessageType messageType, @NotNull ISharedObject object,
			@NotNull String objectType, @NotNull String previousHarness,
			@NotNull String currentHarness, @NotNull String design, @NotNull IHarnessPropagateStatusMessageGroup group)
	{
		m_messageType = messageType;
		m_status = messageType.getStatus();
		m_isEditable = messageType.isEditable();
		m_objectDetailText = object.getName();
		m_objectUid = object.getUID();
		m_objectType = objectType;
		m_message = messageType.getMessage();
		m_previousHarness = previousHarness;
		m_currentHarness = currentHarness;
		m_designName = design;
		m_group = group;
		group.addElement(this);
	}

	public HarnessPropagateStatusMessage(@NotNull HarnessPropagateMessageType messageType, @NotNull ILogicObject logicObject,
			@NotNull String previousHarness, @NotNull String currentHarness,
			@NotNull ILogicDesign design, @NotNull IHarnessPropagateStatusMessageGroup group)
	{
		m_messageType = messageType;
		m_status = messageType.getStatus();
		m_isEditable = messageType.isEditable();
		m_objectDetailText = logicObject.getName();
		m_objectUid = logicObject.getUID();
		m_designUid = design.getUID();
		m_objectType = COFTypeEnum.getDisplayableTypeName(logicObject);
		m_message = messageType.getMessage();
		m_previousHarness = previousHarness;
		m_currentHarness = currentHarness;
		m_designName = design.getFullName();
		m_group = group;
		group.addElement(this);
	}

	@Override public void setupPropagationStatus(boolean propagate)
	{
		m_propagate = propagate;
	}

	@Override public boolean isEditable()
	{
		return m_isEditable;
	}

	@NotNull @Override public HarnessPropagateMessageType getMessageType()
	{
		return m_messageType;
	}

	@NotNull @Override public IHarnessPropagateStatusMessageGroup getGroup()
	{
		return m_group;
	}

	@Override public boolean shouldPropgate()
	{
		return m_propagate;
	}

	@NotNull @Override public IUID getObjectId()
	{
		return m_objectUid;
	}

	@Nullable @Override public IUID getDesignId()
	{
		return m_designUid;
	}

	@Override public boolean isSharedRow()
	{
		return HarnessPropagateMessageType.SHARED_READY_TO_UPDATE.equals(m_messageType);
	}

	@NotNull @Override public String getObjectType()
	{
		return m_objectType;
	}

	@NotNull @Override public String getPreviousHarness()
	{
		return m_previousHarness;
	}

	@NotNull @Override public String getCurrentHarness()
	{
		return m_currentHarness;
	}

	@NotNull @Override public String getObjectDetailLink()
	{
		return m_designUid != null ? IHyperLinkStatusMessage.getHyperlink(m_designUid, m_objectUid) : m_objectUid.getString();
	}

	@Override public boolean isSharedObjectLink()
	{
		return m_designUid == null;
	}

	@Override public void setMessage(String message)
	{
		m_message = message;
	}
}
