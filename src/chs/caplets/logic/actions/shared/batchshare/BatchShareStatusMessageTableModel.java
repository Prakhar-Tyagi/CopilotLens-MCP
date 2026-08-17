/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caplets.logic.actions.shared.utils.StatusMessageTableColumnType;
import chs.caplets.logic.actions.shared.utils.StatusMessageTableModel;
import chs.common.GenericCSVWriter;
import chs.ctf.ui.common.CSVFileSelector;
import chs.ctf.ui.utility.statusmessage.IStatus;
import chs.utilities.ResourceMgr;
import chs.utilities.stream.StreamUtils;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.TableDataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Table model for batch share status messages with CSV export functionality.
 * Manages the display and export of batch share operation results including severity, message, design, and object information.
 */
public class BatchShareStatusMessageTableModel extends StatusMessageTableModel<IBatchShareStatusMessage>
{
	/** List of batch share status messages to be displayed in the table. */
	private final List<IBatchShareStatusMessage> m_items = new ArrayList<>();

	BatchShareStatusMessageTableModel()
	{
		super("BatchShareStatusMessageTable", new TableDataStorage<>(), null);
	}

	/**
	 * Adds batch share status messages to the model.
	 *
	 * @param items the items to add
	 */
	public void addItems(@NotNull Collection<IBatchShareStatusMessage> items)
	{
		m_items.addAll(items);
	}

	/**
	 * Removes batch share status messages from the model.
	 *
	 * @param items the items to remove
	 */
	public void removeItems(@NotNull Collection<IBatchShareStatusMessage> items)
	{
		m_items.removeAll(items);
	}

	/**
	 * Gets the list of batch share status messages.
	 *
	 * @return the list of items
	 */
	@NotNull public List<IBatchShareStatusMessage> getItems()
	{
		return m_items;
	}

	/**
	 * Generates the CSV header row from column titles.
	 *
	 * @return array of column titles for the CSV header
	 */
	protected String[] getCSVHeader()
	{
		List<ColumnInformation<IBatchShareStatusMessage>> columns = getColumns();

		return columns.stream()
				.map(ColumnInformation::getTitle)
				.toArray(String[]::new);
	}

	protected int getRowCount()
	{
		return m_items.size();
	}

	/**
	 * Generates a CSV row for the specified row index.
	 * Returns an empty array with correct column count if the row index is invalid.
	 *
	 * @param rowIndex the row index
	 * @return array of formatted string values for the row
	 */
	@NotNull protected String[] getCSVRow(int rowIndex)
	{
		/* Validate the row index */
		if (rowIndex < 0 || rowIndex >= m_items.size())
		{
			assert false : "Invalid row index: " + rowIndex + ", size: " + m_items.size();
			List<ColumnInformation<IBatchShareStatusMessage>> columns = getColumns();
			/* Return empty array with correct column count */
			return new String[columns.size()];
		}

		IBatchShareStatusMessage item = m_items.get(rowIndex);
		List<ColumnInformation<IBatchShareStatusMessage>> columns = getColumns();

		return columns.stream()
				.map(column -> getFormattedColumnValue(item, column))
				.toArray(String[]::new);
	}

	/**
	 * Gets the formatted string value for a given column and item.
	 *
	 * @param item the status message item
	 * @param column the column information
	 * @return the formatted string value for CSV export
	 */
	@NotNull protected String getFormattedColumnValue(@NotNull IBatchShareStatusMessage item,
			@NotNull ColumnInformation<IBatchShareStatusMessage> column)
	{
		Object value = column.getReadFunction() != null ? column.getReadFunction().apply(item) : "";
		if (value == null) {
			return "";
		}
		if (value instanceof IStatus) {
			return ((IStatus) value).getText();
		}
		return value.toString();
	}

	/**
	 * Prompts the user to select a CSV file for saving.
	 *
	 * @param parent the parent component for the file chooser dialog
	 * @return the selected file, or null if cancelled
	 */
	@Nullable protected File getCSVFile(@NotNull Component parent)
	{
		final CSVFileSelector csvFileSelector = new CSVFileSelector();
		return csvFileSelector.getCSVFile(parent, getClass());
	}

	/**
	 * Writes the CSV data to the specified file.
	 *
	 * @param csvFile the file to write to
	 * @throws IOException if an I/O error occurs
	 */
	protected void writeCSVFile(File csvFile) throws IOException
	{
		try (GenericCSVWriter writer = GenericCSVWriter.newLocaleSpecificCSVProcessor(csvFile)) {
			writer.writeRecord(getCSVHeader());
			for (int i = 0; i < getRowCount(); i++) {
				writer.writeRecord(getCSVRow(i));
			}
		}
	}
	/**
	 * Writes the CSV file and handles any IOExceptions by displaying an error message.
	 *
	 * @param f the file to write to
	 */
	protected void writeCreatedCSVFile(File f)
	{
		try {
			writeCSVFile(f);
		}
		catch (IOException ioe) {
			showExceptionMsg(ioe, "BaseReporterTableModel.writeError");
		}
	}

	/**
	 * Displays an error message dialog for exceptions that occur during CSV operations.
	 *
	 * @param ex the exception that occurred
	 * @param resourceKey the resource key for the error message
	 */
	protected void showExceptionMsg(Exception ex, String resourceKey)
	{
		ResourceBasedMessageContent messageContent =
				new ResourceBasedMessageContent(chs.ctf.ui.common.BaseReporterTableModel.class, resourceKey);
		messageContent.setImplicationsParameters(ex.getLocalizedMessage());
		Message.show(PromptSeverity.ERROR, messageContent);
	}

	/**
	 * Prompts the user to select a CSV file and exports the batch share status messages to it.
	 *
	 * @param parent the parent component for the file chooser dialog
	 */
	public void saveToCSV(@NotNull Component parent)
	{
		File f = getCSVFile(parent);

		if (f == null) {
			return;
		}

		writeCreatedCSVFile(f);
	}

	@Override
	@NotNull public List<ColumnInformation<IBatchShareStatusMessage>> getColumns()
	{
		return Arrays.stream(BatchShareFeedbackTableColumnEnum.values()).map(this::getColumnInformation).filter(
				StreamUtils::notNull).collect(Collectors.toList());
	}

	/**
	 * Creates column information for the specified column type.
	 *
	 * @param column the column enum value
	 * @return the column information, or null if the column type is not recognized
	 */
	@Nullable private ColumnInformation<IBatchShareStatusMessage> getColumnInformation(
			@NotNull BatchShareFeedbackTableColumnEnum column)
	{
		ColumnInformation<IBatchShareStatusMessage> columnInformation = null;
		if (column.equals(BatchShareFeedbackTableColumnEnum.SEVERITY)) {
			String title = ResourceMgr.getString(BatchShareStatusMessageTableModel.class,
					"BatchShareStatusMessageTableModel.Column.Severity.title");
			columnInformation = new ColumnInformation<>(title, column.toString(), IBatchShareStatusMessage::getStatus,
					null, StatusMessageTableColumnType.getInstance());
		}
		else if (column.equals(BatchShareFeedbackTableColumnEnum.MESSAGE)) {
			columnInformation = new ColumnInformation<>(ResourceMgr.getString(BatchShareStatusMessageTableModel.class,
					"BatchShareStatusMessageTableModel.Column.Message.title"), column.toString(),
					IBatchShareStatusMessage::getMessage);
		}
		else if (column.equals(BatchShareFeedbackTableColumnEnum.DESIGN)) {
			columnInformation = new ColumnInformation<>(ResourceMgr.getString(BatchShareStatusMessageTableModel.class,
					"BatchShareStatusMessageTableModel.Column.Design.title"), column.toString(),
					IBatchShareStatusMessage::getDesignName);
		}
		else if (column.equals(BatchShareFeedbackTableColumnEnum.OBJECT)) {
			String title = ResourceMgr.getString(BatchShareStatusMessageTableModel.class,
					"BatchShareStatusMessageTableModel.Column.Object.title");
			columnInformation = new ColumnInformation<>(title, column.toString(), this::getObjectHyperlinkInfo, null,
					StatusMessageTableColumnType.getInstance());
		}
		return columnInformation;
	}
}