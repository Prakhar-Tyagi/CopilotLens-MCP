/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public interface ILayoutSyncValidationResult
{

	ILayoutSyncValidationResult SUCCESS = new ILayoutSyncValidationResult()
	{
		@NotNull @Override public Collection<String> getMessages()
		{
			return Collections.emptyList();
		}

		@Override public void fixupValidationError()
		{

		}
	};

	@NotNull Collection<String> getMessages();

	void fixupValidationError();
}
