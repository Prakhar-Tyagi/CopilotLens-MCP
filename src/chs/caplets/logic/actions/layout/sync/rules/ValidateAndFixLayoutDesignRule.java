/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync.rules;

import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.caplets.logic.actions.layout.sync.CompositeLayoutSyncObjectValidationResult;
import chs.caplets.logic.actions.layout.sync.ICompositeLayoutSyncValidationResult;
import chs.caplets.logic.actions.layout.sync.ILayoutSyncObjectValidator;
import chs.caplets.logic.actions.layout.sync.PipelinedLayoutSyncObjectValidator;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.cable.DesignContent;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IReadOnlyDesignContent;
import chs.common.IUID;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class ValidateAndFixLayoutDesignRule extends AbstractLayoutDesignSyncRule
{

	public ValidateAndFixLayoutDesignRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "ValidateAndFixLayoutDesignRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final IConnectivity connectivity = design.getConnectivity();
		if (connectivity != null) {
			IReadOnlyDesignContent designContent = new DesignContent(connectivity);
			ILayoutSyncObjectValidator validator = new PipelinedLayoutSyncObjectValidator();
			ICompositeLayoutSyncValidationResult result = new CompositeLayoutSyncObjectValidationResult();
			Collection<ILogicObject> logicObjectsToValidate = new ArrayList<>();
			logicObjectsToValidate.addAll(designContent.getPinLists(false, false));
			logicObjectsToValidate.addAll(designContent.getConductors());
			logicObjectsToValidate.addAll(designContent.getMulticores(false));
			logicObjectsToValidate.addAll(designContent.getHighways());
			logicObjectsToValidate.addAll(designContent.getAssemblies());

			final Collection<IUID> objectUIDsToBeDeleted = getSync().getSyncStateManager().getObjectUIDsToBeDeleted();
			for (ILogicObject logicObject : logicObjectsToValidate) {
				if (!objectUIDsToBeDeleted.contains(logicObject.getUID())) {
					result.recordResult(validator.validate(logicObject));
				}
			}
			reportValidationWarnings(reporter, result.getMessages());
			result.fixupValidationError();
		}
		return true;
	}

	private void reportValidationWarnings(@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter,
			@NotNull Collection<String> messages)
	{
		for (String message : messages) {
			reporter.reportWarning("ValidateAndFixLayoutDesignRule.validation.msg", message);
		}
	}
}
