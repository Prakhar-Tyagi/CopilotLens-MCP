/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.shared.utils.HyperlinkInfo;
import chs.caplets.logic.actions.shared.utils.StatusMessageTableColumnType;
import chs.caplets.logic.actions.shared.utils.StatusMessageTableModel;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.TableDataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Table model for Unfreeze output table
 */
public class UnfreezeStatusMessageTableModel extends StatusMessageTableModel<IUnfreezeStatusMessage>
{

	public UnfreezeStatusMessageTableModel()
	{
		super("UnfreezeStatusMessageTable", new TableDataStorage<>(), null);
	}

	@NotNull @Override public List<ColumnInformation<IUnfreezeStatusMessage>> getColumns()
	{
		List<ColumnInformation<IUnfreezeStatusMessage>> result = new ArrayList<>(3);
		result.add(new ColumnInformation<>(UnfreezeFeedbackColumn.Severity.getDisplayName(),
				UnfreezeFeedbackColumn.Severity.getName(),
				IUnfreezeStatusMessage::getStatus, null, StatusMessageTableColumnType.getInstance()));
		result.add(new ColumnInformation<>(UnfreezeFeedbackColumn.Message.getDisplayName(),
				UnfreezeFeedbackColumn.Message.getName(), IUnfreezeStatusMessage::getMessage));
		result.add(new ColumnInformation<>(UnfreezeFeedbackColumn.Object.getDisplayName(),
				UnfreezeFeedbackColumn.Object.getName(), this::getObjectHyperlinkInfo,
				null, StatusMessageTableColumnType.getInstance()));

		return Collections.unmodifiableList(result);
	}

	@Nullable @Override protected HyperlinkInfo getObjectHyperlinkInfo(@NotNull IHyperLinkStatusMessage statusMessage)
	{
		if (StringUtils.isBlank(statusMessage.getObjectDetailLink())) {
			return new HyperlinkInfo(statusMessage.getObjectDetailText(), statusMessage.getObjectDetailLink(), true);
		}
		else {
			return super.getObjectHyperlinkInfo(statusMessage);
		}
	}
}
