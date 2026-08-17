package chs.caplets.logic.actions;

import chs.caplets.logic.actions.ui.EditDeviceConnectorColumns;
import chs.cof.parts.ILibraryObject;
import chs.cofUtils.parts.PartNumberHelper;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditDeviceConnectorTableRow implements Comparable<EditDeviceConnectorTableRow>
{

	private static final AlphaNumComparator<String> alphaNumComparator = AlphaNumComparator.getUniqueObjectAsUniqueComparator();

	@NotNull private final IPrivilegedFootprintDevicePinKey m_devicePin;

	@NotNull private final EditDeviceConnectorDataModel m_dataModel;

	public EditDeviceConnectorTableRow(@NotNull IPrivilegedFootprintDevicePinKey devicePin,
			@NotNull EditDeviceConnectorDataModel dataModel)
	{
		m_devicePin = devicePin;
		m_dataModel = dataModel;
	}

	@NotNull public String getDevicePinName()
	{
		return m_devicePin.getName();
	}

	public void setDevicePinName(@Nullable String devicePinName)
	{
		m_devicePin.setName(devicePinName);
	}

	@NotNull public IFootprintDevicePinKey getDevicePin()
	{
		return m_devicePin;
	}

	@Override public int compareTo(@NotNull EditDeviceConnectorTableRow o)
	{
		return alphaNumComparator.compare(m_devicePin.getName(), o.m_devicePin.getName());
	}

	public void setDeviceConnectorPartNumber(@Nullable String deviceConnectorPartNumber)
	{
		//do not use stringutils as this will change the null values.
		m_dataModel.setDeviceConnectorPartNumber(m_devicePin,
				deviceConnectorPartNumber != null ? deviceConnectorPartNumber.trim() : null);
	}

	public void setDeviceConnectorPinName(@Nullable String dcPinName)
	{
		//do not use stringutils as this will change the null values.
		m_dataModel.setDeviceConnectorPinName(m_devicePin, dcPinName != null ? dcPinName.trim() : null);
	}

	public void setDeviceConnectorName(@Nullable String dcName)
	{
		//do not use stringutils as this will change the null values.
		m_dataModel.setDeviceConnectorName(m_devicePin, dcName != null ? dcName.trim() : null);
	}

	@Nullable public String getDeviceConnectorPinName()
	{
		return m_dataModel.getDeviceConnectorPinName(m_devicePin);
	}

	@Nullable public String getDeviceConnectorPartNumber()
	{
		return m_dataModel.getDeviceConnectorPartNumber(m_devicePin);
	}

	@Nullable public String getDeviceConnectorName()
	{
		return m_dataModel.getDeviceConnectorName(m_devicePin);
	}

	@NotNull private String toDisplay(@Nullable String value)
	{
		return StringUtils.isBlank(value) ? "<Blank>" : value;
	}

	@Nullable private String getDeviceConnectorCavityNameChange()
	{
		String originalCavityName = toDisplay(m_dataModel.getInitialDeviceConnectorPinName(m_devicePin));
		String connectorName = toDisplay(m_dataModel.getInitialDeviceConnectorName(m_devicePin));

		String updatedCavityName = toDisplay(m_dataModel.getDeviceConnectorPinName(m_devicePin));
		String updatedConnectorName = toDisplay(m_dataModel.getDeviceConnectorName(m_devicePin));

		if (!StringUtils.equals(originalCavityName, updatedCavityName)) {
			return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
					"EditDeviceConnectorAction.cellchange.cavitychanged", connectorName + ":" + originalCavityName,
					updatedConnectorName + ":" + updatedCavityName);
		}
		return null;
	}

	@Nullable private String getDeviceConnectorNameChange()
	{
		String connectorName = toDisplay(m_dataModel.getInitialDeviceConnectorName(m_devicePin));
		String updatedConnectorName = toDisplay(m_dataModel.getDeviceConnectorName(m_devicePin));

		if (!StringUtils.equals(connectorName, updatedConnectorName)) {
			return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
					"EditDeviceConnectorAction.cellchange.connectornamechanged", connectorName,
					updatedConnectorName);
		}
		return null;
	}

	@Nullable private String getDeviceConnectorPartnumberChange()
	{
		String partNumber = toDisplay(m_dataModel.getInitialDeviceConnectorPartNumber(m_devicePin));
		String updatedPartnumber = toDisplay(m_dataModel.getDeviceConnectorPartNumber(m_devicePin));

		if (!StringUtils.equals(partNumber, updatedPartnumber)) {
			return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
					"EditDeviceConnectorAction.cellchange.partnumberchanged", partNumber, updatedPartnumber);
		}
		return null;
	}

	@Nullable public String getChange(String columnName)
	{

		if (EditDeviceConnectorColumns.DEVICECONNECTORPIN.equalsName(columnName)) {
			return getDeviceConnectorCavityNameChange();
		}
		if (EditDeviceConnectorColumns.DEVICECONNECTORNAME.equalsName(columnName)) {
			return getDeviceConnectorNameChange();
		}
		if (EditDeviceConnectorColumns.DEVICECONNECTORPARTNUMBER.equalsName(columnName)) {
			return getDeviceConnectorPartnumberChange();
		}

		return null;
	}

	public boolean isUpdated(String columnName)
	{

		if (EditDeviceConnectorColumns.DEVICECONNECTORPIN.equalsName(columnName)) {
			return m_dataModel.isCavityUpdated(m_devicePin);
		}
		if (EditDeviceConnectorColumns.DEVICECONNECTORNAME.equalsName(columnName)) {
			return m_dataModel.isConnectorNameUpdated(m_devicePin);
		}
		if (EditDeviceConnectorColumns.DEVICECONNECTORPARTNUMBER.equalsName(columnName)) {
			return m_dataModel.isConnectorPartNumberUpdated(m_devicePin);
		}

		return false;
	}

	@Nullable public String getDCNameNotAssignedAndPNAssigned()
	{
		if (!StringUtils.isEmpty(getDeviceConnectorPartNumber())) {
			if (StringUtils.isEmpty(getDeviceConnectorName())) {
				return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
						"EditDeviceConnectorAction.cellerror.missingconnector");
			}
		}
		return null;
	}

	@Nullable public String getDCNameNotAssignedAndCavityNameAssigned()
	{
		if (!StringUtils.isEmpty(getDeviceConnectorPinName())) {
			if (StringUtils.isEmpty(getDeviceConnectorName())) {
				return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
						"EditDeviceConnectorAction.cellerror.missingconnector");
			}
		}
		return null;
	}

	@Nullable public String getDCNameAssignedAndCavityNameNotAssigned()
	{
		if (!StringUtils.isEmpty(getDeviceConnectorName())) {
			if (StringUtils.isEmpty(getDeviceConnectorPinName())) {
				return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
						"EditDeviceConnectorAction.cellerror.cavitynamemissing");
			}
		}
		return null;
	}

	@Nullable public String getInvalidPNError()
	{

		String partNumber = getDeviceConnectorPartNumber();
		if (StringUtils.isBlank(partNumber)) {
			return null;
		}
		ILibraryObject libraryObject = PartNumberHelper.getLibraryPartFromCombinedPartNumber(partNumber);
		if (libraryObject == null) {
			return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
					"EditDeviceConnectorAction.cellerror.invalidpartnumber");
		}
		return null;
	}

	@Nullable public String getInvalidNameLength()
	{
		String connectorName = getDeviceConnectorName();
		if (connectorName != null && connectorName.length() > EditDeviceNameLengthValidator.NAME_LENGTH) {
			return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
					"EditDeviceConnectorAction.cellerror.invalidnamelength");
		}
		return null;
	}

	@Nullable public String getInvalidCavityLength()
	{
		String cavityName = getDeviceConnectorPinName();
		if (cavityName != null && cavityName.length() > EditDeviceNameLengthValidator.NAME_LENGTH) {
			return ResourceMgr.getString(EditDeviceConnectorTableRow.class,
					"EditDeviceConnectorAction.cellerror.invalidnamelength");
		}
		return null;
	}
}
