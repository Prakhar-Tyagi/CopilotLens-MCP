/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import org.jetbrains.annotations.NotNull;

public interface IMergeReportTableDataRow extends IReportTableDataRow
{

	String getInitialTargetValue();

	String getObjectName();

	String getCSVData();

	@NotNull String getDetails();
}
