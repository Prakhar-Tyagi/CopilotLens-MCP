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

import chs.utilities.ResourceMgr;

public interface IReportTableDataRow
{

	enum DisplayInformationType
	{
		Information(),
		Warning();

		public String getName()
		{
			if (Information.equals(this)) {
				return ResourceMgr.getString(this, "reporttabledata.type.information");
			}
			return ResourceMgr.getString(this, "reporttabledata.type.warning");
		}
	}

	String getDisplayInformationType();

	// attribute or property value
	String getKey();

	String getInitialValue();

	String getTransformedValue();
}


