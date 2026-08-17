/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.commands;

import chs.caplets.logic.actions.shared.autoshare.AutoShareParams;
import chs.caplets.logic.actions.shared.autoshare.DeltaShareIntoExecutor;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUIDObject;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class DeltaBulkAutoShareIntoCmd extends BulkAutoShareIntoCmd
{
	public DeltaBulkAutoShareIntoCmd(@NotNull Map<IUIDObject, ISharedObject> objectsToBeSharedInto,
			@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext messageReporter,
			boolean continueToShareAfterFailure, boolean saveDesignChanges, @NotNull
	AutoShareParams params, Set<ISharedObject> newlyCreatedSharedObjects)
	{
		super(objectsToBeSharedInto, design, diagram, messageReporter, continueToShareAfterFailure, saveDesignChanges,
				params, newlyCreatedSharedObjects);
	}

	@NotNull @Override protected DeltaShareIntoExecutor getAutoShareIntoExecutor(
			@NotNull ISharedObject sharedObject, boolean isNewlyCreatedObj)
	{
		return new DeltaShareIntoExecutor(m_project, m_design, m_diagram, sharedObject, m_messageReporter,
				m_params, isNewlyCreatedObj);
	}
}
