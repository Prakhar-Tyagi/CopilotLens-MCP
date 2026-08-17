/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.IOutputWindow;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.CAFCommandListener;
import chs.caplets.logic.actions.icdbrowser.ICDObjectActionHandler;
import chs.caplets.logic.commands.AssociateLibraryPartCommand;
import chs.caplets.logic.icd.ICDBrowserPanel;
import chs.caplets.logic.icd.ICDPlacementHelper;
import chs.caplets.logic.icd.ICDSelection;
import chs.caplets.logic.icd.UpdateICDPersistenceHandler;
import chs.cof.draw.IColor;
import chs.cof.icd.IDeviceICD;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CommandContext;
import chs.common.ICommandEvent;
import chs.common.IDesignContainer;
import chs.services.ui.HighlightICDHyperlinkListener;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.ICDUtils;
import chs.utility.IDeviceICDPinSignalAssociation;
import chs.utility.icd.placement.ICDPlacementServiceLocator;
import chs.utility.icd.placement.IICDPlacementPreferences;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper class to Update device from ICD
 */
public class UpdateICDActionHelper
{

	private IOutputWindow m_outputWindow;
	private boolean m_showPinMapper = false;


	public UpdateICDActionHelper(IOutputWindow outputWindow)
	{
		m_outputWindow = outputWindow;
	}

	public UpdateICDActionHelper(IOutputWindow outputWindow, boolean showPinMapper)
	{
		this(outputWindow);
		m_showPinMapper = showPinMapper;
	}

	public void performUpdateICDOnDevice(@NotNull IDeviceICD icd,
			@NotNull IDevice logicDevice, IPinList pinlist, ISchemDiagram diagram,
			@NotNull UpdateICDPersistenceHandler persistenceHandler, Set<IDevice> nonUpdatableICDs,
			boolean logMessage)
	{
		/*
		 * ICD-4381
		 * A modification to the ICD domains can happen from outside of the current context.
		 * It is important to re-check the domain accessibility prior to performing the update
		 */
		boolean icdUpdatePossible = hasAnyAccessibleDomain(icd, logicDevice);
		ILibraryDevice icdLibraryDevice = icd.getLibraryDevice();

		if (icdUpdatePossible && m_showPinMapper) {
			icdUpdatePossible = isVariantOpexValid(icd, logicDevice);

			if (icdUpdatePossible && icdLibraryDevice != null) {
				// If pin mapper is not requested this update happens in isUpdateICDPossible pre-check
				icdLibraryDevice =
						FactoryMgr.getSystemFactory().getCHSSystem().getPartsLibrary().update(icdLibraryDevice);
			}
		}

		if((!icdUpdatePossible) ||
				(!updateICD(diagram, icd, pinlist, logicDevice, icdLibraryDevice, persistenceHandler))) {
			nonUpdatableICDs.add(logicDevice);
		}
		else if (logMessage) {
			Set<ISharedObject> frozenConductors = persistenceHandler.getFrozenConductors();
			if (frozenConductors != null && !frozenConductors.isEmpty()) {
				for (ISharedObject sharedObject : frozenConductors) {
					String message = ResourceMgr.getString(UpdateICDAction.class,
							"UpdateICDAction.output.isFrozen", HTMLHelper.link(sharedObject));
					m_outputWindow.sendMessage(message, getOutputTabName(), true);
				}
			}
			IDesign design = logicDevice.getLogicDesign();
			if (design != null) {
				String msg = ResourceMgr.getString(UpdateICDAction.class,
						"UpdateICDAction.Success", HTMLHelper.link(design, logicDevice),
						icd.getFullName(),
						HTMLHelper.link(icd.getRole(), design.getUID().toString(), logicDevice.getUID().toString(),
								HighlightICDHyperlinkListener.HIGHLIGHTICDTEXT));
				m_outputWindow.sendMessage(msg, getOutputTabName(), true);
			}
		}
	}

	public boolean isUpdateICDPossible(@NotNull IDeviceICD icd, @NotNull IDevice logicDevice)
	{
		// ICD-4381
		if (!hasAnyAccessibleDomain(icd, logicDevice)) {
			return false;
		}

		IDesign design = logicDevice.getLogicDesign();
		if (!isVariantOpexValid(icd, logicDevice)) {
				return false;
			}

		ILibraryDevice icdLibraryDevice = icd.getLibraryDevice();

		boolean icdUpdatePossible;
		if (icdLibraryDevice != null) {
			icdLibraryDevice = FactoryMgr.getSystemFactory().getCHSSystem().getPartsLibrary().update(icdLibraryDevice);
			assert icdLibraryDevice != null;
			icdUpdatePossible = areLogicDeviceAndICDLibraryDevicePinNamesMatching(logicDevice, icdLibraryDevice);
			if (!icdUpdatePossible) {
				if (design != null) {
					String message = ResourceMgr.getString(UpdateICDAction.class,
							"UpdateICDAction.pinNamesMismatch.message", HTMLHelper.link(design, logicDevice));
					m_outputWindow.sendMessage(message, getOutputTabName(), true);
				}
			}
		}
		else {
			icdUpdatePossible = areLogicDeviceAndICDPinNamesMatching(logicDevice, icd);
			if (!icdUpdatePossible) {
				Set<String> invalidPinsNames = getInvalidPinNames(icd, logicDevice);
				String invalidPins = String.join(",", invalidPinsNames);
				if (design != null) {
					String message = ResourceMgr.getString(UpdateICDAction.class,
							"UpdateICDAction.ICDWithoutPart.pinNamesMismatch.message", invalidPins,
							HTMLHelper.link(design, logicDevice),
							icd.getFullName(),
							HTMLHelper.link(icd.getRole(), design.getUID().toString(), logicDevice.getUID().toString(),
									HighlightICDHyperlinkListener.HIGHLIGHTICDTEXT));
					m_outputWindow.sendMessage(message, getOutputTabName(), true);
				}
			}
		}
		return icdUpdatePossible;
	}

	private boolean isVariantOpexValid(@NotNull IDeviceICD icd, @NotNull IDevice logicDevice)
	{
		IDesignContainer design = logicDevice.getDesign();
		if (design != null) {
			IProject project = design.getProject();
			IICDPlacementPreferences preferences =
					ICDPlacementServiceLocator.getInstace().locateService(IICDPlacementPreferences.class);
			if (project != null && preferences.getOptionExpressionSource(logicDevice) ==
					IICDPlacementPreferences.OptionExpressionSource.ICD && !ICDUtils.isVariantOpexValid(icd)) {
				m_outputWindow.sendMessage(HTMLHelper.color(IColor.RED, ResourceMgr
						.getString(ICDObjectActionHandler.class, "ICDObjectActionHandler.invalidVariantOpexErrorMsg",
								icd.getRole())), getOutputTabName(), true);
				return false;
			}
		}
		return true;
	}

	private boolean areLogicDeviceAndICDLibraryDevicePinNamesMatching(IDevice device,
			@NotNull ILibraryDevice icdLibraryDevice)
	{
		Function<String, String> toKey = getPinComparisonCaseFunction();
		Set<String> logicdevicePinNames = getPinNamesOfTheDeviceOrItsSharedParent(device, toKey);
		Set<String> icddevicePinNames = new HashSet<>();
		for (ILibraryCavity cavity : icdLibraryDevice.getAllCavities()) {
			icddevicePinNames.add(toKey.apply(cavity.getName()));
		}
		return icddevicePinNames.containsAll(logicdevicePinNames);
	}

	@NotNull public static String getOutputTabName()
	{
		return ResourceMgr.getString(UpdateICDAction.class, "UpdateICDAction.output.tab");
	}

	private boolean areLogicDeviceAndICDPinNamesMatching(IDevice device, IDeviceICD icd)
	{
		//should this check be case-insensitive? No.
		Function<String, String> toKey = getPinComparisonCaseFunction();
		Set<String> icdPinNames = getICDPinNames(icd, toKey);
		Set<String> devicePinNames = getPinNamesOfTheDeviceOrItsSharedParent(device, toKey);
		return icdPinNames.containsAll(devicePinNames);
	}

	@NotNull private Set<String> getInvalidPinNames(@NotNull IDeviceICD icd, @NotNull IDevice logicDevice)
	{
		Function<String, String> toKey = getPinComparisonCaseFunction();
		Set<String> icdPinNames = getICDPinNames(icd, toKey);
		Set<String> devicePinNames = getPinNamesOfTheDeviceOrItsSharedParent(logicDevice, toKey);
		devicePinNames.removeAll(icdPinNames);
		return devicePinNames;
	}

	private boolean updateICD(ISchemDiagram diagram, IDeviceICD icd, IPinList pinlist, IDevice connectivity,
			@Nullable ILibraryDevice icdLibraryDevice, UpdateICDPersistenceHandler persistenceHandler)
	{
		ILibraryDevice transiantLibraryPart = null;
		ILibraryObject origPart = CommonUtils.cast(connectivity.getLibraryObject(), ILibraryObject.class);
		boolean updatePartSuccess = false;
		try {
			if (icdLibraryDevice != null) {
				updatePartSuccess = updateLibraryPart(diagram, icd, pinlist, connectivity, icdLibraryDevice);
			}
			else {
				//we need to ignore the existing libraryref check during update.
				transiantLibraryPart = PinListAddPinHelper.assignTransiantLibraryPart(icd, connectivity,
						(p) -> true);
				if (transiantLibraryPart != null) {
					updatePartSuccess = updateLibraryPart(diagram, icd, pinlist, connectivity, transiantLibraryPart);
				}
			}
		}
		finally {
			if (transiantLibraryPart != null) {
				PinListAddPinHelper.removeTransiantLibraryPart(transiantLibraryPart, connectivity);

				if (!updatePartSuccess && origPart != null) {
					// Restore original part, if any, on the logic device in case of cancellation of the pin mapper or any other failure
					connectivity.assignLibraryPart(origPart);
			}
		}
		}
		if(updatePartSuccess) {
			ICDPlacementHelper
					.updateICDNameRoutingAndProperties(pinlist, diagram, icd.getRole(), icd,
							persistenceHandler);
			return true;
		}
		return false;
	}

	private boolean hasAnyAccessibleDomain(IDeviceICD icd, IDevice logicDevice)
	{
		if (!ICDUtils.hasAnyAccessibleDomain(icd.getICD(), ICDUtils.getUserDomains())) {
			IDesignContainer design = logicDevice.getDesign();
			if (design != null) {
				String message = ResourceMgr
						.getString(UpdateICDActionHelper.class, "UpdateICDActionHelper.ICDNoDomainAccess",
								HTMLHelper.link(design, logicDevice));
				m_outputWindow.sendMessage(message, getOutputTabName(), true);
			}
			return false;
		}
		return true;
	}

	@NotNull private Function<String, String> getPinComparisonCaseFunction()
	{
		//should this check be case-insensitive? No.
		return (s) -> s;
	}

	/**
	 * Gets pin names from a Logic device.
	 * If the device is non-shared, the pins available in the device are returned
	 * If the device is shared, all the pins in the shared parent are returned
	 * @param device A device which may be an instance of a shared device
	 * @param toKey A function to transform a pin name in the returned collection. Pass an identity s -> s if you want
	 * just the pin names
	 * @return Set of strings that are the result of applying toKey transform on the pin name
	 */
	@NotNull
	private Set<String> getPinNamesOfTheDeviceOrItsSharedParent(IDevice device, Function<String, String> toKey)
	{
		Set<String> logicdevicePinNames = new HashSet<>();

		if (device.isShared()) {
			ISharedDevice sharedDevice = CommonUtils.cast(device.getSharedObject(), ISharedDevice.class);
			if (sharedDevice == null) {
				throw new IllegalStateException("Failed to retreive SharedDevice");
			}
			for (ISharedPin pin : sharedDevice.getPins()) {
				logicdevicePinNames.add(toKey.apply(pin.getName()));
			}
		}
		else {
			for (IAbstractPin pin : device.getPinCollection()) {
				logicdevicePinNames.add(toKey.apply(pin.getName()));
			}
		}
		return logicdevicePinNames;
	}

	@NotNull private Set<String> getICDPinNames(IDeviceICD icd, @NotNull Function<String, String> toKey)
	{
		Collection<IDeviceICDPinSignalAssociation> pinSignalAssociations =
				icd.getICDUsageDefinition().getPinSignalAssociations();
		return pinSignalAssociations.stream()
				.map(pinSignalAssociation -> pinSignalAssociation.getPinName())
				.map(toKey)
				.collect(Collectors.toSet());
	}

	private boolean updateLibraryPart(ISchemDiagram diagram, IDeviceICD icd, IPinList pinlist, IDevice connectivity,
			@NotNull ILibraryDevice icdLibraryDevice)
	{
		ILogicDesign design = diagram.getDesign();
		if (design == null) {
			return false;
		}
		ICDSelection icdSelection = new ICDSelection(icd, design);
		icdSelection.setSelectedLibraryObject(icdLibraryDevice);
		ILibraryDeviceFootprint selectedFp = getFootprintFromICD(icdLibraryDevice, icd);
		icdSelection.setSelectedFootprint(selectedFp);
		ICDBrowserPanel.updateSupplierPartNumber(icdSelection, icd, icdLibraryDevice);
		ICDBrowserPanel.updateCustomerPartNumber(icdSelection, icd, icdLibraryDevice);
		boolean isUpdated = executeLibraryPartCommand(diagram, pinlist, icdLibraryDevice, icdSelection, icd, connectivity);
		if (isUpdated) {
			ensureSelectedICDFootprintOnDevice(connectivity, selectedFp);
		}
		return isUpdated;
	}

	@Nullable
	private ILibraryDeviceFootprint getFootprintFromICD(ILibraryDevice icdLibraryDevice, @NotNull IDeviceICD icd)
	{
		String icdFPName = icd.getFootprintName();
		if (!StringUtils.isBlank(icdFPName)) {
			for (ILibraryDeviceFootprint footPrint : icdLibraryDevice.getDeviceFootprints()) {
				if (icdFPName.equalsIgnoreCase(footPrint.getFootprintName())) {
					return footPrint;
				}
			}
		}
		return null;
	}

	private boolean executeLibraryPartCommand(ISchemDiagram diagram, IPinList pinlist, ILibraryDevice icdLibraryDevice,
			ICDSelection icdSelection, IDeviceICD icd, IDevice connectivity)
	{
		CommandContext.start(this, null);
		CommandContext.setDesignContainer(diagram.getDesignContainer());
		AssociateLibraryPartCommand cmd = new AssociateLibraryPartCommand(new CAFCommandHelper(), diagram,
				pinlist, icdLibraryDevice, icdSelection)
		{
			@Override protected boolean shouldAssignDefaultFootprintIfNotSpecified()
			{
				return false;
			}
		};
		try {
			turnOnAutoMap(icd, connectivity, cmd);
			cmd.setCommandListener(new CAFCommandListener<>(UpdateICDAction.class, false));
			if (cmd.prepare()) {
				return cmd.execute();
			}
		}
		finally {
			ICommandEvent andClearEvent = CommandContext.getAndClearEvent();
			if (andClearEvent != null) {
				String eventMessage = cmd.getCommandListener().getEventMessage(andClearEvent);
				if (eventMessage != null) {
					m_outputWindow.sendMessage(eventMessage, getOutputTabName(), true);
				}
			}
			CommandContext.end();
		}
		return false;
	}

	/*
	 * If the pin mapper is not requested, auto mapping will remain ON.
	 * If pin mapper is requested, automapping will remain ON only when pin reconcilation is not required
	 */
	private void turnOnAutoMap(IDeviceICD icd, IDevice logicDevice,
			AssociateLibraryPartCommand cmd)
	{
		boolean autoMapOn = true;

		if (m_showPinMapper) {
			ILibraryDevice icdLibraryDevice = icd.getLibraryDevice();

			autoMapOn = icdLibraryDevice == null ? areLogicDeviceAndICDPinNamesMatching(logicDevice, icd) :
					areLogicDeviceAndICDLibraryDevicePinNamesMatching(logicDevice, icdLibraryDevice);

			if (!autoMapOn) {
				UpdatePartAction.setupPinMapper(cmd, UpdatePartAction.PinMappingOperationContext.ICD);
			}
		}

		cmd.setAllowAutoMapAllByName(autoMapOn);
	}

	@SuppressWarnings("ConstantConditions")
	protected final void ensureSelectedICDFootprintOnDevice(@NotNull IDevice connectivity,
			@Nullable ILibraryDeviceFootprint selectedFp)
	{
		//In case of net diagram footprint may not set on the object. Try to update the foortprint on device from ICD
		//if ICD doesn't have any footprint assigned we shouldn't have any footprint on device also.
		if (connectivity.getFootprint() != selectedFp) {
			connectivity.setFootprintId(selectedFp != null ? selectedFp.getUID() : null);
			connectivity.setFootprintDescription(selectedFp != null ? selectedFp.getFootprintName() : null);
		}
	}
}
