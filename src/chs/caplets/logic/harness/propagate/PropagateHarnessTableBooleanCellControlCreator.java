/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.utilities.CommonUtils;
import com.mentor.capital.javafx.table.cell.BooleanCellControlCreator;
import com.mentor.capital.javafx.table.cell.IGenericTableCell;
import javafx.scene.Node;
import org.jetbrains.annotations.Nullable;

/**
 * @author rmahato 
 */
class PropagateHarnessTableBooleanCellControlCreator extends BooleanCellControlCreator
{

	@Nullable @Override public Node createRenderer(IGenericTableCell<?> cell)
	{
		IHarnessPropagateStatusMessage
				message = CommonUtils.cast(cell.getRowItem(), IHarnessPropagateStatusMessage.class);
		final Node node = super.createRenderer(cell);
		if (message != null && node != null) {
			node.setDisable(!message.isEditable());
		}
		return node;
	}

	@Override public void updateValue(Node control, IGenericTableCell<?> cell)
	{
		super.updateValue(control, cell);
		IHarnessPropagateStatusMessage
				message = CommonUtils.cast(cell.getRowItem(), IHarnessPropagateStatusMessage.class);
		if (message != null && control != null) {
			control.setDisable(!message.isEditable());
		}
	}
}
