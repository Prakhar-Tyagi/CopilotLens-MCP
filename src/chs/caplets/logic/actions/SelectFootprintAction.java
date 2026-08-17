package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.library.PSDLibraryPartSelection;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCustomerPartNumber;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibrarySupplierPartNumber;
import chs.cof.parts.LibraryBooleanType;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.cof.parts.partselector.ILibrarySelectionFilter;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.cofUtils.parameterized.DeviceConnectorBackshellSyncOnRegeneration;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.WrappingRuntimeException;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.util.Collection;
import java.util.List;

/**
 * @author chandras on 07-07-2017.
 */
public class SelectFootprintAction extends AbstractChangeFootprintAction
{

	public SelectFootprintAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public String getActionUIClass()
	{
		return SelectFootprintActionUI.class.getName();
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
		ILibraryDevice libraryDevice = determineCandidateLibraryDevice(device);
		if (libraryDevice == null || libraryDevice.getDeviceFootprints().size() < 1) {
			return false;
		}

		try {
			PartSelectionContext partSelectionContext = new PartSelectionContext();
			ILibraryDeviceFootprint footprint = device.getFootprint();
			PSDLibraryPartSelection partSelection = new PSDLibraryPartSelection();
			partSelection.setSelectedLibraryObject(libraryDevice);
			partSelection.setSelectedFootprint(footprint);
			partSelectionContext.setSelectionFilter(new ILibrarySelectionFilter()
			{
				@Override public boolean include(ILibraryBaseObject obj)
				{
					return obj instanceof ILibraryDeviceFootprint;
				}

				@Nullable @Override
				public ILibraryBaseObject getDefaultSelection(Collection<ILibraryBaseObject> objs)
				{
					if (objs.contains(footprint)) {
						return footprint;
					}
					for (ILibraryDeviceFootprint deviceFootprint : CollectionUtils
							.filterByClass(objs, ILibraryDeviceFootprint.class)) {
						LibraryBooleanType isDefault = deviceFootprint.getDefaultFootprintType();
						if (LibraryBooleanType.TRUE.equals(isDefault)) {
							return deviceFootprint;
						}
					}
					return null;
				}

				@Override
				public void setSelectedCustomerpartNumber(ILibraryCustomerPartNumber custpartNumber)
				{

				}

				@Override
				public void setSelectedSupplierpartNumber(ILibrarySupplierPartNumber supppartNumber)
				{

				}
			});
			ILibraryDeviceFootprint selectedFootprint =
					selectLibraryDeviceFootprint(partSelectionContext, partSelection);
			if (selectedFootprint != null && selectedFootprint != footprint) {
				ISharedDevice sharedDevice = CommonUtils.cast(device.getSharedPinList(), ISharedDevice.class);
				if (sharedDevice != null) {
					if (!CTFLockUpdateHelper.lock(sharedDevice)) {
						return false;
					}
				}
				if (!LogicObjectLockFinder.tryEdit(device)) {
					return false;
				}
				try (DeviceConnectorBackshellSyncOnRegeneration ignored = new DeviceConnectorBackshellSyncOnRegeneration(
						device)) {
					device.setFootprintDescription(selectedFootprint.getFootprintName());
					device.setFootprintId(selectedFootprint.getUID());
					rebuildDeviceConnectors(device);
					if (hasGHCCreatedHarnessConnectors(device)) {
						performGHC(device);
					}
				}
				finally {
					if (sharedDevice != null) {
						saveSharedDevice(sharedDevice);
					}
				}
				return true;
			}
		}
		catch (RuntimeException e) {
			throw new WrappingRuntimeException(e);
		}
		return false;
	}

	@Nullable protected ILibraryDeviceFootprint selectLibraryDeviceFootprint(PartSelectionContext partSelectionContext,
			PSDLibraryPartSelection partSelection)
	{
		final String title = ResourceMgr.getString(SelectFootprintAction.class,
				"SelectFootprintAction.title");
		// init/show the PSD
		ILibraryPartSelector partSelector = UtilsHelper.getCHSSystem().getPartsLibrary().
				getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
		partSelector.showSelectDetailsDlg(partSelection, partSelectionContext, ConfigurationTypeEnum.LOGICAL, title);
		return partSelection.getSelectedFootprint();
	}

	@Nullable protected ILibraryDevice determineCandidateLibraryDevice(SelectSet selections)
	{
		IDevice device = determineCandidateDevice(selections);
		return determineCandidateLibraryDevice(device);
	}

	@Nullable protected IDevice determineCandidateDevice(SelectSet selections)
	{
		List<IPinList> selectedObjects = selections.getSelectedObjects(IPinList.class);
		if (selectedObjects.size() != 1) {
			return null;
		}
		IPinList schemDevice = selectedObjects.get(0);
		ISharedObject sharedObject = schemDevice != null ? schemDevice.getSharedObject() : null;
		if (sharedObject != null && sharedObject.isFrozen()) {
			//LOGIC-6668 User can change the FP of frozen shared device
			return null;
		}
		return schemDevice != null ? CommonUtils.cast(schemDevice.getConnectivity(), IDevice.class) : null;
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null && getController().getCapletModel().isEditable()) {
			ILibraryDevice libraryDevice = determineCandidateLibraryDevice(selections);
			if (libraryDevice != null && libraryDevice.getDeviceFootprints().size() > 1) {
				container.add(new ActionEntry(actionUI));
			}
		}
	}

	protected String getAuditTrailDescription()
	{
		return ResourceMgr.getString(AuditableEventType.class, "AuditableEventType.FOOTPRINT");
	}
}
