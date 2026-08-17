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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for change management in batch share row data
 */
public abstract class BaseDataChange implements IBatchShareDataChange
{

	protected IBatchShareGroup group;
	protected IBatchShareRow currentRow;

	protected BaseDataChange(IBatchShareGroup group, IBatchShareRow currentRow)
	{
		this.group = group;
		this.currentRow = currentRow;
	}

	protected boolean findNewAnchor(List<IBatchShareRow> impactedRows)
	{
		List<IBatchShareRow> batchShareElements = new ArrayList<>(group.getBatchShareElements());
		Collections.sort(batchShareElements);
		boolean found = false;
		for (IBatchShareRow element : batchShareElements) {
			if (element == currentRow) {
				continue;
			}
			if (element.getAction() == Action.SHARE_INTO && element.isSelected()) {
				element.setAction(Action.SHARE);
				impactedRows.add(element);
				found = true;
				break;
			}
		}
		return found;
	}
}
