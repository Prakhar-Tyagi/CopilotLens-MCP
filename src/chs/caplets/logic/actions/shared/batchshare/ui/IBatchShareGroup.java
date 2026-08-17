/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.ShareabilityStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Group object for batch share rows
 */
public interface IBatchShareGroup
{

	@NotNull Collection<IBatchShareRow> getBatchShareElements();

	boolean isValid();

	@Nullable String getTargetSharedObjectName();

	@NotNull ShareabilityStatus getStatus();

	@NotNull String getMatchCriteriaValues();
}
