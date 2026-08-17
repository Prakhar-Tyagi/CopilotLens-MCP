/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.cafmain.actions.CAFCommandListener;
import chs.cof.parts.ILibraryObject;
import chs.common.ICommandEvent;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;

public class SaveAssemblyConnectivityToLibraryCommandListener<CLIENT_TYPE> extends CAFCommandListener<CLIENT_TYPE>
{

	public SaveAssemblyConnectivityToLibraryCommandListener(@NotNull Class<CLIENT_TYPE> aClientClass)
	{
		super(aClientClass, false);
	}

	protected String getCmdDescription()
	{
		return ResourceMgr
				.getString(SaveAssemblyConnectivityToLibraryCommandListener.class,
						"SaveAssemblyConnectivityToLibraryCommandListener.output.desc");
	}

	protected String getCmdFailureDescription()
	{
		return ResourceMgr.getString(SaveAssemblyConnectivityToLibraryCommandListener.class,
				"SaveAssemblyConnectivityToLibraryCommandListener.failure.desc");
	}

	protected String getStartProcessingMsg()
	{
		return ResourceMgr.getString(SaveAssemblyConnectivityToLibraryCommandListener.class,
				"SaveAssemblyConnectivityToLibraryCommandListener.output.start");
	}

	@Override public void handleFailure(@NotNull ICommandEvent event)
	{
		if (event.getFailure() == ICommandEvent.FAILURE.LOCK_LIBRARYPART_FAILED) {
			ResourceBasedMessageContent content = new ResourceBasedMessageContent(this,
					"SaveAssemblyConnectivityToLibraryCommandListener.LockLibraryPartfailed");
			assert event.getObject() != null;
			content.setImplicationsParameters(((ILibraryObject) event.getObject()).getPartNumber());
			Message.show(PromptSeverity.INFORMATION, content);
		}
        else if (event.getFailure() == ICommandEvent.FAILURE.OBJECT_NOT_EDITABLE) {
            ResourceBasedMessageContent content = new ResourceBasedMessageContent(this,
                    "SaveAssemblyConnectivityToLibraryCommandListener.EditLibraryPartfailed");
            assert event.getObject() != null;
            content.setImplicationsParameters(((ILibraryObject) event.getObject()).getPartNumber());
            Message.show(PromptSeverity.INFORMATION, content);
        }
        else if (event.getFailure() == ICommandEvent.FAILURE.OBJECT_DOES_NOT_EXIST) {
            ResourceBasedMessageContent content = new ResourceBasedMessageContent(this,
                    "SaveAssemblyConnectivityToLibraryCommandListener.RefreshLibraryPartfailed");
            assert event.getObject() != null;
            content.setImplicationsParameters(((ILibraryObject) event.getObject()).getPartNumber());
            Message.show(PromptSeverity.INFORMATION, content);
        }
		else {
			ResourceBasedMessageContent content = new ResourceBasedMessageContent(this,
					"SaveAssemblyConnectivityToLibraryCommandListener.NoLibraryPermission");
			Message.show(PromptSeverity.INFORMATION, content);
		}
	}
}