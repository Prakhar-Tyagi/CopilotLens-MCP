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

import chs.caplets.logic.actions.shared.utils.StatusMessageTableColumnType;
import chs.caplets.logic.actions.shared.utils.StatusMessageTableModel;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.ColumnTypeInfo;
import com.mentor.capital.javafx.table.TableDataStorage;
import com.mentor.capital.javafx.table.cell.BooleanColumnType;
import com.mentor.capital.javafx.table.helpers.IControlCreator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Table Model for HarnessUpdateStatusMessage
 */
public class HarnessUpdateStatusMessageTableModel extends StatusMessageTableModel<IHarnessPropagateStatusMessage>
{

	private Set<IHarnessPropagateStatusMessage> m_rows = new HashSet<>();

	public HarnessUpdateStatusMessageTableModel()
	{
		super("HarnessUpdateStatusMessageTable", new TableDataStorage<>(), null);
	}

	@NotNull @Override public List<ColumnInformation<IHarnessPropagateStatusMessage>> getColumns()
	{
		List<ColumnInformation<IHarnessPropagateStatusMessage>> result = new ArrayList<>(8);
		result.add(new ColumnInformation<>(HarnessPropagateColumn.Propagate.getDisplayName(),
				HarnessPropagateColumn.Propagate.getName(),
				IHarnessPropagateStatusMessage::shouldPropgate, (row, obj) -> row.setupPropagationStatus((boolean) obj),
				new BooleanColumnType(new ColumnTypeInfo(true))
				{
					@NotNull @Override public IControlCreator getControlCreator()
					{
						return new PropagateHarnessTableBooleanCellControlCreator();
					}
				}));
		result.add(new ColumnInformation<>(HarnessPropagateColumn.Severity.getDisplayName(),
				HarnessPropagateColumn.Severity.getName(),
				IHarnessPropagateStatusMessage::getStatus, null, StatusMessageTableColumnType.getInstance()));
		result.add(new ColumnInformation<>(HarnessPropagateColumn.Object.getDisplayName(),
				HarnessPropagateColumn.Object.getName(), this::getObjectHyperlinkInfo,
				null, StatusMessageTableColumnType.getInstance()));
		result.add(new ColumnInformation<>(HarnessPropagateColumn.ObjectType.getDisplayName(),
				HarnessPropagateColumn.ObjectType.getName(), IHarnessPropagateStatusMessage::getObjectType));
		result.add(new ColumnInformation<>(HarnessPropagateColumn.Message.getDisplayName(),
				HarnessPropagateColumn.Message.getName(), IHarnessPropagateStatusMessage::getMessage));
		result.add(new ColumnInformation<>(HarnessPropagateColumn.Before.getDisplayName(),
				HarnessPropagateColumn.Before.getName(), IHarnessPropagateStatusMessage::getPreviousHarness));
		result.add(new ColumnInformation<>(HarnessPropagateColumn.After.getDisplayName(),
				HarnessPropagateColumn.After.getName(), IHarnessPropagateStatusMessage::getCurrentHarness));
		result.add(new ColumnInformation<>(HarnessPropagateColumn.Design.getDisplayName(),
				HarnessPropagateColumn.Design.getName(), IHarnessPropagateStatusMessage::getDesignName));
		return Collections.unmodifiableList(result);
	}

	public void addRows(@NotNull Collection<? extends IHarnessPropagateStatusMessage> rows)
	{
		m_rows.addAll(rows);
	}

	public void clearAllRows()
	{
		m_rows.clear();
	}

	@NotNull public Collection<IHarnessPropagateStatusMessage> getRows()
	{
		return Collections.unmodifiableSet(m_rows);
	}
}
