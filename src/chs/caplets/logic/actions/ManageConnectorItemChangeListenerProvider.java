/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.common.ITableCellValueChangeListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ManageConnectorItemChangeListenerProvider
		implements ITableCellValueChangeListener<ManageConnectorConnectionsInfo>
{

	private final Map<ColumnInformation<ManageConnectorConnectionsInfo>, ManageConnectorValueChangeListener>
			listeners;

	private final Map<ColumnInformation<ManageConnectorConnectionsInfo>, Map<ManageConnectorConnectionsInfo, Collection<?>>>
			possibleValuesForColumn;

	private ManageConnectorValueChangeListener columnAndRowChangeListener;

	private ManageConnectorPinDuplicationFinder pinDuplicationFinder;

	private ManageConnectorPlugMapHighlighter plugMaphightlight;

	public interface ManageConnectorValueChangeListener
	{

		void handleValueChange(ManageConnectorConnectionsInfo item,
				ColumnInformation<ManageConnectorConnectionsInfo> col, Object oldValue, Object newValue);
	}

	public ManageConnectorItemChangeListenerProvider(
			ManageConnectorPinDuplicationFinder manageConnectorPinDuplicationFinder)
	{
		listeners = new HashMap<>();
		pinDuplicationFinder = manageConnectorPinDuplicationFinder;
		possibleValuesForColumn = new HashMap<>();
	}

	public void setValidChangeValues(ColumnInformation<ManageConnectorConnectionsInfo> col,
			Map<ManageConnectorConnectionsInfo, Collection<?>> possibleValues)
	{
		possibleValuesForColumn.put(col, possibleValues);
	}

	public void addPlugmapHighlighter(ManageConnectorPlugMapHighlighter plugmapHighlighter)
	{
		this.plugMaphightlight = plugmapHighlighter;
	}

	private void setItemChangeListener(ColumnInformation<ManageConnectorConnectionsInfo> column,
			ManageConnectorValueChangeListener listener)
	{
		listeners.put(column, listener);
	}

	@Nullable private ManageConnectorValueChangeListener getIetmChangeListener(
			ColumnInformation<ManageConnectorConnectionsInfo> columnInformation)
	{
		return listeners.get(columnInformation);
	}

	public ManageConnectorValueChangeListener prepareRowAndColumnUpdate(
			final ColumnInformation<ManageConnectorConnectionsInfo> col, Table<ManageConnectorConnectionsInfo> table)
	{
		if (columnAndRowChangeListener == null) {
			columnAndRowChangeListener = new ManageConnectorValueChangeListener()
			{

				@Override public void handleValueChange(ManageConnectorConnectionsInfo givenItem,
						ColumnInformation<ManageConnectorConnectionsInfo> col, Object oldValue, Object newValue)
				{

					Collection<ManageConnectorConnectionsInfo> itemsToReplace = new ArrayList<>();
					table.data().forEach(anItem -> {

						boolean isUpdated = anItem.updateConnection(givenItem.getOriginalValue(), newValue.toString(),
								givenItem.getDesign());
						if (isUpdated) {
							itemsToReplace.add(anItem);
						}
					});
					if (ManageConnectorReplaceAllCollector.replaceAllCollector != null) {
						ManageConnectorReplaceAllCollector.replaceAllCollector.addItems(itemsToReplace, table);
					}
				}
			};
		}
		setItemChangeListener(col, columnAndRowChangeListener);
		return columnAndRowChangeListener;
	}

	@Override public void cellValueChanged(ManageConnectorConnectionsInfo sourceItem,
			ColumnInformation<ManageConnectorConnectionsInfo> sourceColumnInfo, Object oldValue, Object newValue)
	{

		String oldValueString = oldValue != null ? oldValue.toString() : "";
		String newvalueString = newValue != null ? newValue.toString() : "";
		Map<ManageConnectorConnectionsInfo, Collection<?>> possibleValues =
				possibleValuesForColumn.get(sourceColumnInfo);
		if (StringUtils.equals(oldValueString, newvalueString)) {
			return;
		}
		if (possibleValues != null && possibleValues.containsKey(sourceItem)) {
			Collection<String> possibleStringValues = possibleValues.get(sourceItem).stream()
					.map(v -> v.toString()).collect(Collectors.toSet());
			if (!possibleStringValues.contains(newvalueString)) {
				return;
			}
		}

		ManageConnectorValueChangeListener listener = getIetmChangeListener(sourceColumnInfo);
		if (listener != null) {
			pinDuplicationFinder.updateActivePins(sourceItem, oldValueString, newvalueString);
			if (plugMaphightlight != null) {

				plugMaphightlight.updateActivePins(oldValueString, newvalueString);
			}

			listener.handleValueChange(sourceItem, sourceColumnInfo, oldValueString, newvalueString);
		}
	}
}
