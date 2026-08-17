/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.merge;

import chs.caplets.logic.actions.actionreport.IMergeActionChange;
import org.jetbrains.annotations.NotNull;

/**
 * merge changes reporter
 */
public interface IMergeActionChangeReporter
{

	IMergeActionChangeReporter NULL_REPORTER = change -> {
	};

	void report(@NotNull IMergeActionChange change);
}