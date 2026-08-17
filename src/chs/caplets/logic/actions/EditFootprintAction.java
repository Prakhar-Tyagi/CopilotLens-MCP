package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.ui.EditDeviceConnectorDialog;
import chs.cof.icd.IDeviceICD;
import chs.cof.library.FootprintSource;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.footprint.IUserDeviceFootprint;
import chs.cof.logical.footprint.user.IPrivilegedUserFootprintable;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedPinList;
import chs.cofUtils.parameterized.DeviceConnectorBackshellSyncOnRegeneration;
import chs.common.IUID;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utility.ICDUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.LogicObjectLockFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

/**
 * @author chandras on 07-07-2017.
 */
public class EditFootprintAction extends AbstractChangeFootprintAction
{

	public EditFootprintAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public String getActionUIClass()
	{
		return EditFootprintActionUI.class.getName();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		if (!successful) {
			return false;
		}
		SelectSet selections = getController().getSelectMgr().getCurrentSelections();
		IDevice device = determineCandidateDevice(selections);
		if (device == null) {
			return false;
		}

		EditDeviceConnectorParams params = createEditDeviceConnectorParams(device);
		EditDeviceConnectorDialog dialog = getEditDeviceConnectorsDialog(params);
		boolean status = dialog.showDialog(true);
		if (status) {
			try (DeviceConnectorBackshellSyncOnRegeneration ignored =
						 new DeviceConnectorBackshellSyncOnRegeneration(device)) {
				status = applyEdits(params, device);
			}
		}
		return status;
	}

	@NotNull
	protected EditDeviceConnectorParams createEditDeviceConnectorParams(@NotNull IDevice device)
	{
		return new EditDeviceConnectorParams(device);
	}

	@NotNull protected EditDeviceConnectorDialog getEditDeviceConnectorsDialog(EditDeviceConnectorParams params)
	{
		return new EditDeviceConnectorDialog(CAFUtils.getInstance().getDialogFrame(), true, params);
	}

	public boolean applyEdits(@NotNull EditDeviceConnectorParams params, @NotNull IDevice device)
	{
		IUserDeviceFootprint footprint = params.generateFootprint();

		IPrivilegedUserFootprintable userFootprintable = CommonUtils.cast(device, IPrivilegedUserFootprintable.class);
		if (userFootprintable == null) {
			return false;
		}

		ISharedDevice sharedDevice = CommonUtils.cast(device.getSharedPinList(), ISharedDevice.class);
		if (sharedDevice != null) {
			if (!CTFLockUpdateHelper.lock(sharedDevice)) {
				return false;
			}
		}
		if (!LogicObjectLockFinder.tryEdit(device)) {
			return false;
		}
		try {
			userFootprintable.setDeviceFootprint(footprint);
			device.setDeviceSideFootprintSource(FootprintSource.UserDefined);
			rebuildDeviceConnectors(device);
			if (hasGHCCreatedHarnessConnectors(device)) {
				performGHC(device);
			}
		}
		finally {
			userFootprintable.setDeviceFootprint(null);
			if (sharedDevice != null) {
				saveSharedDevice(sharedDevice);
			}
		}
		return true;
	}

	@Nullable protected IDevice determineCandidateDevice(SelectSet selections)
	{
		IDevice selectedDevice = null;
		for (Object obj : selections.getUIDObjects()) {
			IPinList schemPL = CommonUtils.cast(obj, IPinList.class);
			IDevice device = CommonUtils.cast(schemPL != null ? schemPL.getConnectivity() : obj, IDevice.class);
			if (device == null) {
				return null;
			}
			if (selectedDevice == null) {
				selectedDevice = device;
			}
			if (selectedDevice != device) {
				return null;
			}
		}

		if (selectedDevice == null) {
			return null;
		}

		ISharedPinList sharedObject = selectedDevice.getSharedPinList();
		if (sharedObject != null && sharedObject.isFrozen()) {
			//LOGIC-6668 User can change the FP of frozen shared device
			return null;
		}

		int numPins = sharedObject != null ? sharedObject.getNumPins() : selectedDevice.getNumPins();
		if (numPins <= 0) {
			return null;
		}

		return selectedDevice;
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null && getController().getCapletModel().isEditable()) {
			IDevice device = determineCandidateDevice(selections);
			if (device == null) {
				return;
			}
			IUID libraryDevice = device.getLibraryRef();
			IDeviceICD mappedICD = ICDUtils.getMappedICD(device);
			//do not allow this action on ICD devices also.
			if (libraryDevice == null && mappedICD == null) {
				container.add(new ActionEntry(actionUI));
			}
		}
	}

	protected String getAuditTrailDescription()
	{
		return ResourceMgr.getString(AuditableEventType.class, "AuditableEventType.DEVICE_CONNECTOR");
	}
}
