/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions.immersed.strategy;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletController;
import chs.capitalmanager.appserver.UserSessionException;
import chs.cof.icd.IICD;
import chs.cof.icd.IICDDescriptor;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IProject;
import chs.common.IProjectPreferenceMgr;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import chs.utility.ICDUtils;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates all the resolved context needed for device creation.
 * <p>
 * This object is built once from a {@link CreateDeviceInfo} and provides
 * lazy-resolved access to the ICD, library objects, graphics, logic design,
 * FIB, and active caplet controller. Strategies query this context to
 * determine applicability and to obtain the objects they need.
 * </p>
 */
public class DeviceCreationContext
{

	@NotNull private final CreateDeviceInfo m_deviceInfo;
	@Nullable private final IICD m_icd;
	@Nullable private final ILibraryObject m_libraryObject;
	@NotNull private final Set<ILibraryGraphic> m_libraryGraphics;
	@Nullable private final ILibraryObject m_icdLibraryDevice;
	@NotNull private final IFIB m_fib;
	@NotNull private final ICapletController m_activeCapletController;
	@NotNull private ILogicDesign m_logicDesign;
	@NotNull private Set<ISharedPinList> m_sharedPinLists;
	@Nullable private IPinList m_placedDevicePinList;

	/**
	 * Constructs a fully-resolved device creation context from the given device info.
	 *
	 * @param deviceInfo the device creation request data, must not be null
	 */
	public DeviceCreationContext(@NotNull CreateDeviceInfo deviceInfo, @NotNull ILogicDesign logicDesign)
	{
		m_deviceInfo = deviceInfo;

		m_libraryObject = UtilsHelper.getCHSSystem().getPartsLibrary()
				.getLibraryObject(deviceInfo.getPartNumber());

		m_libraryGraphics = resolveLibraryGraphics(m_libraryObject);

		m_fib = CAFUtils.getInstance().getFIB();
		m_activeCapletController = CAFUtils.getInstance().getActiveCapletController();

		m_logicDesign = logicDesign;

		if (m_logicDesign.getProject() != null) {
			m_sharedPinLists = m_logicDesign.getProject().getSharedPinListMgr()
					.findSharedPinLists(m_deviceInfo.getDisplayName(), PinListTypeEnum.TypeDevice);
		}
		m_icd = findICDs(deviceInfo);

		m_icdLibraryDevice = resolveIcdLibraryDevice(m_icd);

		findPlacedDevicePinList(deviceInfo);
	}

	@Nullable private IICD findICDs(@NotNull CreateDeviceInfo deviceInfo)
	{
		Set<IICDDescriptor> icdDescriptorsMatchingByRole = null;
		try {
			icdDescriptorsMatchingByRole =
					ICDUtils.getICDDescriptorsMatchingByRole(deviceInfo.getDisplayName());
		}
		catch (UserSessionException e) {
			throw new RuntimeException(e);
		}
		if (icdDescriptorsMatchingByRole.isEmpty()) {
			return null;
		}
		return ICDUtils.getICD(icdDescriptorsMatchingByRole.iterator().next().getName(), "");
	}

	private void findPlacedDevicePinList(@NotNull CreateDeviceInfo deviceInfo)
	{
		if (m_logicDesign.getConnectivity() != null) {
			Set<IDevice> allDevices = m_logicDesign.getConnectivity().getAllDevices();
			for (IDevice device : allDevices) {
				if (device.getName().equals(deviceInfo.getDisplayName())) {
					m_placedDevicePinList = device;
					break;
				}
			}
		}
	}

	@Nullable public IPinList getPlacedDevicePinList()
	{
		return m_placedDevicePinList;
	}

	@NotNull
	private static Set<ILibraryGraphic> resolveLibraryGraphics(@Nullable ILibraryObject libraryObject)
	{
		if (libraryObject != null) {
			return libraryObject.getLibraryGraphics();
		}
		return new HashSet<>();
	}

	@Nullable
	private static ILibraryObject resolveIcdLibraryDevice(@Nullable IICD icd)
	{
		if (icd == null || icd.getLibraryDeviceUID() == null) {
			return null;
		}
		return UtilsHelper.getCHSSystem().getPartsLibrary().getLibraryObject(icd.getLibraryDeviceUID());
	}

	@NotNull
	public CreateDeviceInfo getDeviceInfo()
	{
		return m_deviceInfo;
	}

	@Nullable
	public IICD getIcd()
	{
		return m_icd;
	}

	@Nullable
	public ILibraryObject getLibraryObject()
	{
		return m_libraryObject;
	}

	@NotNull
	public Set<ILibraryGraphic> getLibraryGraphics()
	{
		return m_libraryGraphics;
	}

	/**
	 * @return the library device resolved from the ICD, or null if none
	 */
	@Nullable
	public ILibraryObject getIcdLibraryDevice()
	{
		return m_icdLibraryDevice;
	}

	@NotNull
	public Set<ISharedPinList> getSharedPinLists()
	{
		return m_sharedPinLists;
	}

	@NotNull
	public IFIB getFib()
	{
		return m_fib;
	}

	@NotNull
	public ICapletController getActiveCapletController()
	{
		return m_activeCapletController;
	}

	@NotNull
	public ILogicDesign getLogicDesign()
	{
		return m_logicDesign;
	}

	public boolean getDoNotAutoShareICD()
	{
		IProject project = getLogicDesign().getProject();
		IProjectPreferenceMgr preferences = project != null ? project.getPreferences() : null;
		boolean doNotAutoShareICD = preferences != null && preferences.getDoNotAutoShareICD();
		return !doNotAutoShareICD;
	}
}

