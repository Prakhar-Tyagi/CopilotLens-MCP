/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2019-2026 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.cof.logical.IDesign;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IReleaseLevel;
import chs.common.IRevisionedObject;
import chs.task.ReleaseLevelResultHtmlDisplayer;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author chandras on 27-08-2019.
 */
public class SharedObjectAvailabilityReporter implements ISharedObjectAvailabilityReporter
{

	@Override
	public void report(@NotNull FailureReason reason, @NotNull ISharedObject sharedObject, @Nullable IDesign design)
	{
		String messageRootKey = "";
		String errorImplication = "";
		String errorGuidance = "";
		String messageText = "";
		final String sharedObjectReportableName = sharedObject instanceof IRevisionedObject ?
				((IRevisionedObject) sharedObject).getFullName() : sharedObject.getReportableName();
		final String designFullName = design != null ? design.getFullName() : "";
		final String sharedObjType =
				StringUtils.toLowerCase(ReleaseLevelResultHtmlDisplayer.typeString(sharedObject));
		final IReleaseLevel releaseLevel = design != null ? design.getReleaseLevel() : null;
		final String releaseLevelName = releaseLevel != null ? releaseLevel.getName() : "";
		switch (reason) {
			case DOMAIN_ON_SHARED_OBJECT:
				messageRootKey = "SharedObjectAvailability.domainAccess";
				messageText = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.message.domainOnThisObject");
				errorImplication = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.thisRevisionIsRestricted.domainOnThisObject", sharedObjType,
						sharedObjectReportableName);
				errorGuidance = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.domainAccess.guidance.text");
				break;
			case DOMAIN_ON_ASSOCIATED_PART:
				messageRootKey = "SharedObjectAvailability.domainAccess";
				messageText = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.message.domainOnLibPart");
				errorImplication = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.thisRevisionIsRestricted.domainOnLibPart", sharedObjType,
						sharedObjectReportableName);
				errorGuidance = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.domainAccess.guidance.text");
				break;
			case DOMAIN_ON_ASSOCIATED_ICD:
				messageRootKey = "SharedObjectAvailability.domainAccess";
				messageText = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.message.domainOnICD");
				errorImplication = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.thisRevisionIsRestricted.domainOnICD", sharedObjType,
						sharedObjectReportableName);
				errorGuidance = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.domainAccess.guidance.text");
				break;
			case FROZEN_STATUS_SHARED_OBJECT:
				messageRootKey = "SharedObjectAvailability.frozenStatus";
				messageText = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.frozenStatus.message.text");
				errorGuidance = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.frozenStatus.guidance.text");
				errorImplication = ResourceMgr.getString(SharedObjectAvailabilityReporter.class,
						"SharedObjectAvailability.frozenStatus.implications.text", releaseLevelName,
						designFullName, sharedObjType, sharedObjectReportableName);
				break;
		}
		if (!StringUtils.isBlank(messageRootKey)) {
			ResourceBasedMessageContent content = new ResourceBasedMessageContent(
					SharedObjectAvailabilityReporter.class, messageRootKey);
			content.setMessageParameters(messageText);
			content.setImplicationsParameters(errorImplication);
			content.setGuidanceParameters(errorGuidance);
			Message.show(PromptSeverity.ERROR, content);
		}
	}
}
