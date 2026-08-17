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

import chs.ctf.ui.utility.statusmessage.HarnessPropagateStatus;
import chs.ctf.ui.utility.statusmessage.IStatus;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * Types of HarnessPropagateMessage
 */
public enum HarnessPropagateMessageType
{
	READY_TO_UPDATE ("ReadyToUpdate"),
	READY_TO_UPDATE_POKE_HOME ("ReadyToUpdatePokeHome") {
		@NotNull @Override public IStatus getStatus()
		{
			return HarnessPropagateStatus.Warning;
		}
	},
	READY_TO_UPDATE_VARIANT_CONN ("ReadyToUpdateVariantConnector") {
		@NotNull @Override public IStatus getStatus()
		{
			return HarnessPropagateStatus.Warning;
		}
	},
	SHARED_READY_TO_UPDATE ("SharedReadyToUpdate"){
		@NotNull @Override public IStatus getStatus()
		{
			return HarnessPropagateStatus.Warning;
		}
	},
	UPDATED ("Updated"){
		@Override public boolean isEditable()
		{
			return false;
		}

		@NotNull @Override public IStatus getStatus()
		{
			return HarnessPropagateStatus.Completed;
		}
	},
	FAILURE ("FailedToUpdate"){
		@NotNull @Override public IStatus getStatus()
		{
			return HarnessPropagateStatus.Error;
		}

		@Override public boolean isEditable()
		{
			return false;
		}
	};

	private String name;

	HarnessPropagateMessageType(String name)
	{
		this.name = name;
	}

	@NotNull public IStatus getStatus()
	{
		return HarnessPropagateStatus.Information;
	}

	public boolean isEditable()
	{
		return true;
	}

	@NotNull public String getMessage()
	{
		return ResourceMgr.getString(HarnessUpdateStatusMessageTableModel.class,
				"HarnessPropagateMessageType." + name + ".message");
	}
}
