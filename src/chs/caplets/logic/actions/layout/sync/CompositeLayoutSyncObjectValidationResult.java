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

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class CompositeLayoutSyncObjectValidationResult implements ICompositeLayoutSyncValidationResult
{

	@NotNull private Collection<ILayoutSyncValidationResult> mValidationResults;

	public CompositeLayoutSyncObjectValidationResult()
	{
		mValidationResults = new ArrayList<>();
	}

	@NotNull @Override public Collection<String> getMessages()
	{
		return mValidationResults.stream()
				.flatMap(result -> result.getMessages().stream())
				.collect(Collectors.toList());
	}

	@Override public void fixupValidationError()
	{
		for (ILayoutSyncValidationResult validationResult : mValidationResults) {
			validationResult.fixupValidationError();
		}
	}

	@Override public void recordResult(@NotNull ILayoutSyncValidationResult result)
	{
		mValidationResults.add(result);
	}
}
