/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caplets.logic.actions.shared.HyperLinkStatusMessage;
import chs.caplets.logic.actions.shared.IHyperLinkStatusMessage;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ILogicObjectDesignContainer;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.common.DesignUtils;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.ctf.ui.utility.statusmessage.DesignStatus;
import chs.ctf.ui.utility.statusmessage.IStatus;
import chs.system.FactoryMgr;
import chs.utilities.IXMLTags;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.PromptSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents feedback messages reported during batch share
 */
public class BatchShareStatusMessage extends HyperLinkStatusMessage implements IBatchShareStatusMessage
{

	public BatchShareStatusMessage(@NotNull PromptSeverity severity, @NotNull String message, @Nullable Object object)
	{
		m_status = getStatusFromSeverity(severity);
		m_message = message;
		m_objectDetailText = "";
		m_objectDetailLink = "";
		m_designName = "";
		Object involvedObject = object;
		if (involvedObject instanceof IObjectInfo) {
			IObjectInfo objectInfo = (IObjectInfo) involvedObject;
			ILogicDesign design = DesignUtils.getDesign(objectInfo.getDesignUID(), ILogicDesign.class);
			if (design != null) {
				m_designName = design.getFullName();
			}
			m_objectDetailText = StringUtils.emptyIfBlank(objectInfo.getAttributeValue(IXMLTags.NAME));
			IUID designUID = design != null ? design.getUID() : null;
			IUID objectUID = FactoryMgr.getCommonFactory().constructUID(objectInfo.getUID());
			if (designUID != null) {
				m_objectDetailLink = IHyperLinkStatusMessage.getHyperlink(designUID, objectUID);
			}
		}
		if (involvedObject instanceof IConnectivityRef) {
			involvedObject = ((IConnectivityRef) involvedObject).getConnectivity();
		}
		if (involvedObject instanceof IReadOnlyNamedObject) {
			String objectName = ((IReadOnlyNamedObject) involvedObject).getName();
			m_objectDetailText = objectName;
		}
		if (involvedObject instanceof ILogicObject) {
			ILogicObject logicObj = (ILogicObject) involvedObject;
			ILogicObjectDesignContainer design = logicObj.getDesign();
			m_designName = design != null ? design.getFullName() : "";
			IUID designUID = design != null ? design.getUID() : null;
			IUID objectUID = logicObj.getUID();
			if (designUID != null && objectUID != null) {
				m_objectDetailLink = IHyperLinkStatusMessage.getHyperlink(designUID, objectUID);
			}
		}
	}

	@NotNull private IStatus getStatusFromSeverity(PromptSeverity severity)
	{
		if (severity == PromptSeverity.ERROR) {
			return DesignStatus.Error;
		}
		if (severity == PromptSeverity.INFORMATION) {
			return DesignStatus.Information;
		}
		if (severity == PromptSeverity.WARNING) {
			return DesignStatus.Warning;
		}
		return DesignStatus.Information;
	}
}