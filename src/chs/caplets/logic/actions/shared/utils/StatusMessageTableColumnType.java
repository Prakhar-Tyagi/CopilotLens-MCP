/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.utils;

import com.mentor.capital.javafx.table.ColumnTypeInfo;
import com.mentor.capital.javafx.table.cell.AbstractTableColumnType;
import com.mentor.capital.javafx.table.helpers.IControlCreator;
import com.mentor.capital.javafx.table.types.ColumnDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Column type for status message table
 */
public class StatusMessageTableColumnType extends AbstractTableColumnType
{

	private static StatusMessageTableColumnType m_instance = null;

	private StatusMessageTableColumnType()
	{
		super(new ColumnTypeInfo(""));
	}

	@NotNull public static synchronized StatusMessageTableColumnType getInstance()
	{
		if (m_instance == null) {
			m_instance = new StatusMessageTableColumnType();
		}
		return m_instance;
	}

	@Override
	@NotNull public IControlCreator getControlCreator()
	{
		return new StatusMessageTableControlCreator();
	}

	@Override
	@NotNull public ColumnDataType<?> getDataType()
	{
		return ColumnDataType.UNKNOWN;
	}
}