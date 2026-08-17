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
import java.util.Optional;

/**
 *
 */
public class ActionChange extends BaseDataChange
{

	private Action changedAction;

	public ActionChange(IBatchShareRow sourceItem, Action newValue)
	{
		super(sourceItem.getGroup(), sourceItem);
		changedAction = newValue;
	}

	@NotNull public List<IBatchShareRow> refreshRows()
	{
		List<IBatchShareRow> impactedRows = new ArrayList<>();
		if (!group.isValid() || !currentRow.isSelected()) {
			addCurrentChange(impactedRows);
			return impactedRows;
		}

		computeNewAnchorIfNeeded(impactedRows);
		addCurrentChange(impactedRows);
		ensureAtLeastOneAnchorPresent();
		return impactedRows;
	}

	private void computeNewAnchorIfNeeded(List<IBatchShareRow> impactedRows)
	{
		if (currentRow.getAction() != changedAction) {
			if (changedAction == Action.SHARE) {
				resetExistingAnchor(impactedRows);
			}
			else {
				findNewAnchor(impactedRows);
			}
		}
	}

	private void addCurrentChange(List<IBatchShareRow> impactedRows)
	{
		currentRow.setAction(changedAction);
		impactedRows.add(currentRow);
	}

	private void ensureAtLeastOneAnchorPresent()
	{
		Optional<IBatchShareRow> anySelectedRow =
				group.getBatchShareElements().stream().filter(r -> r.isSelected() && r != currentRow).findAny();

		if (!anySelectedRow.isPresent()) {
			currentRow.setAction(Action.SHARE);
		}
	}

	private void resetExistingAnchor(List<IBatchShareRow> impactedRows)
	{
		for (IBatchShareRow element : group.getBatchShareElements()) {
			if (element == currentRow) {
				continue;
			}
			if (element.getAction() == Action.SHARE && element.isSelected()) {
				element.setAction(Action.SHARE_INTO);
				impactedRows.add(element);
			}
		}
	}
}
