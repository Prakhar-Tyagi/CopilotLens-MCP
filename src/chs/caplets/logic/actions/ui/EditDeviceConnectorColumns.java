package chs.caplets.logic.actions.ui;

import chs.caplets.logic.actions.EditDeviceConnectorTableRow;
import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.ColumnInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author chandras on 20-06-2018.
 */
public enum EditDeviceConnectorColumns
{
	DEVICEPIN("devicepinname", ResourceMgr.getString(EditDeviceConnectorColumnsProvider.class,
			"EditDeviceConnectorAction.ColumnName.DevicePinName"),
			(editDeviceConnectorColumnsProvider -> editDeviceConnectorColumnsProvider.createDevicePinColumn()),
			null),
	DEVICECONNECTORPIN("deviceconnectorpin", ResourceMgr.getString(EditDeviceConnectorColumnsProvider.class,
			"EditDeviceConnectorAction.ColumnName.DeviceConnectorPin"),
			(editDeviceConnectorColumnsProvider -> editDeviceConnectorColumnsProvider
					.createDeviceConnectorCavityNameColumn()),
			((editDeviceConnectorTableRow, value) -> {
				editDeviceConnectorTableRow.setDeviceConnectorPinName(value);
			})),
	DEVICECONNECTORPARTNUMBER("deviceconnectorpartnumber", ResourceMgr
			.getString(EditDeviceConnectorColumnsProvider.class,
					"EditDeviceConnectorAction.ColumnName.DeviceConnectorPN"),
			(editDeviceConnectorColumnsProvider -> editDeviceConnectorColumnsProvider
					.createDeviceConnectorPartNumberColumn()),
			((editDeviceConnectorTableRow, value) -> {
				editDeviceConnectorTableRow.setDeviceConnectorPartNumber(value);
			})),
	DEVICECONNECTORNAME("deviceconnectorname", ResourceMgr.getString(EditDeviceConnectorColumnsProvider.class,
			"EditDeviceConnectorAction.ColumnName.DeviceConnectorName"),
			(editDeviceConnectorColumnsProvider -> editDeviceConnectorColumnsProvider
					.createDeviceConnectorNameColumn()),
			((editDeviceConnectorTableRow, value) -> {
				editDeviceConnectorTableRow.setDeviceConnectorName(value);
			}));

	@NotNull private String name;
	@NotNull private String displayName;
	@NotNull private Function<EditDeviceConnectorColumnsProvider, ColumnInformation<EditDeviceConnectorTableRow>>
			columnCreator;
	@Nullable private BiConsumer<EditDeviceConnectorTableRow, String> columnUpdater;

	EditDeviceConnectorColumns(@NotNull String columnName, @NotNull String columnDisplayName, @NotNull
			Function<EditDeviceConnectorColumnsProvider, ColumnInformation<EditDeviceConnectorTableRow>> column,
			@Nullable BiConsumer<EditDeviceConnectorTableRow, String> consumer)
	{
		name = columnName;
		displayName = columnDisplayName;
		columnCreator = column;
		columnUpdater = consumer;
	}

	public boolean equalsName(String givenName)
	{
		return name.equals(givenName);
	}

	public boolean equalsName(@Nullable ColumnInformation<?> column)
	{
		if (column != null) {
			return name.equals(column.getName());
		}
		return false;
	}

	ColumnInformation<EditDeviceConnectorTableRow> getColumn(EditDeviceConnectorColumnsProvider provider)
	{

		return columnCreator.apply(provider);
	}

	@Nullable public static EditDeviceConnectorColumns getColumnByName(String colName)
	{
		for (EditDeviceConnectorColumns aValue : EditDeviceConnectorColumns.values()) {
			if (aValue.equalsName(colName)) {
				return aValue;
			}
		}
		return null;
	}

	public void update(EditDeviceConnectorTableRow row, Object value)
	{
		if (columnUpdater != null) {
			columnUpdater.accept(row, value != null ? value.toString() : null);
		}
	}

	@NotNull public String getDisplayName()
	{
		return displayName;
	}

	@NotNull public String getName()
	{
		return name;
	}
}
