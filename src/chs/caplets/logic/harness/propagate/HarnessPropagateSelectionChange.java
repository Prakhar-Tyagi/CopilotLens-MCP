/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class HarnessPropagateSelectionChange implements IHarnessPropagateDataChange
{

	private final IHarnessPropagateStatusMessage m_currentRow;
	private boolean isSelected;

	public HarnessPropagateSelectionChange(IHarnessPropagateStatusMessage sourceItem, Boolean newValue)
	{
		m_currentRow = sourceItem;
		isSelected = newValue;
	}

	@Override @NotNull public List<IHarnessPropagateStatusMessage> getRefreshedRows()
	{
		m_currentRow.setupPropagationStatus(isSelected);

		Collection<IHarnessPropagateStatusMessage> elements = m_currentRow.getGroup().getElements();
		List<IHarnessPropagateStatusMessage> impactedRows = new ArrayList<>();

		Optional<IHarnessPropagateStatusMessage> optional =
				elements.stream().filter(o -> o != m_currentRow).findFirst();
		IHarnessPropagateStatusMessage message = optional.isPresent() ? optional.get() : null;

		if (message != null && message.getObjectId().equals(m_currentRow.getObjectId())) {
			if (m_currentRow.isSharedRow() && !message.isSharedRow() && isSelected) {
				message.setupPropagationStatus(true);
			}
			else if (!m_currentRow.isSharedRow() && message.isSharedRow() && !isSelected) {
				message.setupPropagationStatus(false);
			}
			impactedRows.add(message);
		}

		impactedRows.add(m_currentRow);
		return impactedRows;
	}
}
