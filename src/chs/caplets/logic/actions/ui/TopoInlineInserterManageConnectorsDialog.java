/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.ConductorConnectionChanger;
import chs.caplets.logic.actions.ManageConnectorConnectionsInfo;
import chs.caplets.logic.actions.ManageConnectorDesignScope;
import chs.caplets.logic.actions.ManageConnectorPinDuplicationFinder;
import chs.caplets.logic.actions.ManageConnectorPinSelections;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.utilities.StringUtils;
import chs.utility.topology.utils.IInlineAssistFailureCollector;
import chs.utility.topology.utils.subsystem.messaging.MessagingServices;
import chs.utility.ui.MessageBarHelper;
import chs.utility.ui.PinTableColumnProvider;
import chs.utility.ui.pintable.ColumnChooserObjectType;
import chs.utility.ui.pintable.ColumnCreationParams;
import chs.utility.ui.pintable.IPinTableColumnInfo;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.TableModel;
import com.mentor.capital.javafx.table.strategy.TableColumnPosition;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TableColumnBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog for managing connections for topology inline inserter.
 * Handle design column addition and warning bar creation, manage connector designs and failure messages.
 */
public class TopoInlineInserterManageConnectorsDialog extends ManageConnectorsDialog
{

	// Track whether we added a Design column, if so will need to remove it on closing.
	private boolean mAddedDesignColumn = false;
	private static final String DESIGN_COLUMN_NAME = IPinTableColumnInfo.PINATTRPREFIX + "Design";
	public static final String WARNING_BAR_LABEL_NAME = "PlaceInlineShieldTerminationFailureWarning";

	public TopoInlineInserterManageConnectorsDialog(@Nullable String title,
			@NotNull IGenericInlineConnector inlineConnector,
			@NotNull ConductorConnectionChanger conductorConnectionChanger,
			ManageConnectorDesignScope designsInScope)
	{
		super(CAFUtils.getInstance().getDialogFrame(), title, true, inlineConnector, conductorConnectionChanger,
				designsInScope);
	}

	@Override
	protected void initFX(JFXPanel fxPanel, JPanel topPanel, ManageConnectorPinSelections manageConnectorPinSelections,
			ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder,
			Collection<ManageConnectorConnectionsInfo> data)
	{
		super.initFX(fxPanel, topPanel, manageConnectorPinSelections, manageConnectorPinDuplicationFinder, data);
		final boolean match =
				table.columns()
						.map(ColumnInformation::getName)
						.anyMatch(DESIGN_COLUMN_NAME::equals);
		if (!match) {
			// Treat the design column as a mandatory column, i.e. don't allow it to be removed by the user
			mandatoryColumnNames.add(DESIGN_COLUMN_NAME);
			PinTableColumnProvider pinTableColumnProvider =
					new PinTableColumnProvider(getSortHelperProvider(),
							ColumnCreationParams.getDefaultColumnCreationParams(
									ColumnChooserObjectType.Connector, mSharedPinList != null));
			table.addColumns(TableColumnPosition.after(WIRE_NAME),
					Collections.singleton(pinTableColumnProvider.generateInformation(DESIGN_COLUMN_NAME)));
			mAddedDesignColumn = true;
		}
	}

	@Nullable @Override protected JPanel createWarningPanel()
	{
		String failureMessage =
				MessagingServices.getService(IInlineAssistFailureCollector.class)
						.getCombinedMessage(StringUtils.NEWLINE);
		if (StringUtils.isBlank(failureMessage)) {
			return null;
		}
		JPanel infoBarPanel = new JPanel(new BorderLayout());
		JLabel placeInlineShieldTerminationFailureWarningBar =
				MessageBarHelper.getWarningMessageBarWithHeader(failureMessage, WARNING_BAR_LABEL_NAME,
						SwingConstants.LEFT);

		infoBarPanel.add(placeInlineShieldTerminationFailureWarningBar, BorderLayout.WEST);
		FlatEmptyBorder border = new FlatEmptyBorder(2, 2, 2, 2);
		infoBarPanel.setBorder(border);
		return infoBarPanel;
	}

	@NotNull @Override
	protected Table<ManageConnectorConnectionsInfo> createTable(ColumnsToBeAdded columnsToBeAdded,
			Consumer<TableColumnBase<?, ?>> columnCreationListener)
	{
		return new Table<ManageConnectorConnectionsInfo>(MANAGE_CONNECTORS_TABLE_ID,
				new TableModel<>(tableDataStorage, getColumnsCreator(columnsToBeAdded)),
				columnCreationListener)
		{
			@Override protected void deactivate()
			{
				if (mAddedDesignColumn) {
					removeColumns(List.of(DESIGN_COLUMN_NAME));
				}
				super.deactivate();
			}
		};
	}
}
