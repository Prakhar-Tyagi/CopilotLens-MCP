/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SelectionChange extends BaseDataChange
{

	private boolean isSelected;

	public SelectionChange(IBatchShareRow sourceItem, Boolean newValue)
	{
		super(sourceItem.getGroup(), sourceItem);
		isSelected = newValue;
	}

	@Override @NotNull public List<IBatchShareRow> refreshRows()
	{
		List<IBatchShareRow> impactedRows = new ArrayList<>();
		// should not update invalid row
		if(!group.isValid()){
			return impactedRows;
		}
		currentRow.setSelected(isSelected);
		impactedRows.add(currentRow);
		manageActionState(impactedRows);
		return impactedRows;
	}

	private void manageActionState(List<IBatchShareRow> impactedRows)
	{
		if (!group.isValid() || group.getTargetSharedObjectName() != null) {
			return;
		}
		boolean anchorAlreadyExists = anchorAlreadyExists();
		if (currentRow.getAction() == Action.SHARE) {
			if (!isSelected) {
				boolean newAnchorFound = findNewAnchor(impactedRows);
				if(newAnchorFound){
					currentRow.setAction(Action.SHARE_INTO);
				}
			}

			if (isSelected && anchorAlreadyExists) {
				currentRow.setAction(Action.SHARE_INTO);
			}
		}
		if (currentRow.getAction() == Action.SHARE_INTO) {
			if (isSelected && !anchorAlreadyExists) {
				currentRow.setAction(Action.SHARE);
				resetOtherAnchorsIfPresent(impactedRows);
			}
		}
	}

	private void resetOtherAnchorsIfPresent(List<IBatchShareRow> impactedRows)
	{
		for (IBatchShareRow element : group.getBatchShareElements()) {
			if (element != currentRow && element.getAction() == Action.SHARE) {
				element.setAction(Action.SHARE_INTO);
				impactedRows.add(element);
			}
		}
	}

	private boolean anchorAlreadyExists()
	{
		boolean anchorExists = false;
		for (IBatchShareRow element : group.getBatchShareElements()) {
			if (element != currentRow && element.getAction() == Action.SHARE && element.isSelected()) {
				anchorExists = true;
				break;
			}
		}
		return anchorExists;
	}
}
