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

import chs.cof.logical.cable.ILogicObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PipelinedLayoutSyncObjectValidator implements ILayoutSyncObjectValidator
{

	@NotNull private final List<ILayoutSyncObjectValidator> mValidators;

	public PipelinedLayoutSyncObjectValidator()
	{
		mValidators = new ArrayList<>();
		initValidators(mValidators);
	}

	private static void initValidators(@NotNull List<ILayoutSyncObjectValidator> validatorCollection)
	{
		validatorCollection.add(new ConnectorDeviceMatingValidator());
	}

	@NotNull @Override
	public ILayoutSyncValidationResult validate(@NotNull ILogicObject logicObject)
	{
		if (!accepts(logicObject)) {
			return ILayoutSyncValidationResult.SUCCESS;
		}
		final CompositeLayoutSyncObjectValidationResult compositeResult =
				new CompositeLayoutSyncObjectValidationResult();

		for (ILayoutSyncObjectValidator validator : mValidators) {
			if (validator.accepts(logicObject)) {
				final ILayoutSyncValidationResult result = validator.validate(logicObject);
				compositeResult.recordResult(result);
			}
		}

		return compositeResult;
	}

	@Override public boolean accepts(@NotNull ILogicObject logicObject)
	{
		return true;
	}
}
