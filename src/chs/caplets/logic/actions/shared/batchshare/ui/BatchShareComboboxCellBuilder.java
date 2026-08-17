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

import com.mentor.capital.javafx.table.cell.ComboBoxCellBuilder;
import com.mentor.capital.javafx.table.cell.ITableCell;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;

/**
 *
 */
public class BatchShareComboboxCellBuilder extends ComboBoxCellBuilder
{

	@Override protected void addEventFilterForComboBox(ITableCell<?> cell, ComboBox<Object> comboBox)
	{
		comboBox.valueProperty().addListener(new ChangeListener<Object>()
		{
			@Override public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
			{
				if (cell.getValue() != newValue) {
					cell.editCell();
					cell.commitEdit(newValue);
				}
			}
		});
	}
}
