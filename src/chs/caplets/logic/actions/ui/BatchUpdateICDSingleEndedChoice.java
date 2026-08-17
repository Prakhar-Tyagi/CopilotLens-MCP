/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class to compute user's choice in generation of single ended conductors in Batch Update ICD action
 */
public class BatchUpdateICDSingleEndedChoice extends ICDSingleEndedChoice
{

	public BatchUpdateICDSingleEndedChoice(@Nullable IProject project)
	{
		super(project);
	}

	@NotNull protected String getPrefKey()
	{
		return "BatchUpdateICD.userChoice";
	}

	@NotNull @Override protected String getMessageResourceKey()
	{
		return "BatchUpdateICDAction.userChoice";
	}

	@Override protected boolean shouldGetChoiceFromUser()
	{
		return true;
	}
}
