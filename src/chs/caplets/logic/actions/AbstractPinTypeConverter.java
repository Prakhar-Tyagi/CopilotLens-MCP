package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.IUserSessionRemotePackage.SharedPinUsageInfo;
import chs.capitalmanager.appserver.UserSessionException;
import chs.cof.logical.ConvertPinTypeLogEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IPinTypeConverter;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IPrivilegedDevicePin;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.common.IUID;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.KeySeparatedStringBuilder;
import chs.utility.PinTypeConversionChecker;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA. User: nagamani Date: 13/3/14
 */
public abstract class AbstractPinTypeConverter implements IPinTypeConverter
{

	protected List<IDevicePin> m_selectedPins = new ArrayList<>();
	private boolean m_isSharedContext = false;
	protected Set<ISharedDevice> m_affectedSharedPinLists = new HashSet<ISharedDevice>();

	public void addPinForConversion(IDevicePin devicePin)
	{
		m_selectedPins.add(devicePin);
		if (devicePin.isShared()) {
			IPinList device = devicePin.getOwner();
			assert device != null;
			ISharedPinList sharedPinList = device.getSharedPinList();
			if (sharedPinList != null) {
				m_affectedSharedPinLists.add((ISharedDevice) sharedPinList);
			}
			m_isSharedContext = true;
		}
	}

	public ConvertPinTypeLogEnum doConversion()
	{
		ILogicDesign logicDesign = m_selectedPins.iterator().next().getLogicDesign();
		//MU Lock
		if (logicDesign != null && logicDesign.isUnderConcurrentEdit()) {
			//lock the pin lists
			Set<IUID> lockFailed = LogicObjectLockFinder.tryEdit(logicDesign, m_selectedPins);
			if (!lockFailed.isEmpty()) {
				List<String> errorArgs = new LinkedList<>();
				String hyperLink = linksToLockFailedObjs(logicDesign, lockFailed);
				errorArgs.add(hyperLink);
				IPinList owner = m_selectedPins.get(0).getOwner();
				errorArgs.add(owner != null ? owner.getName() : "");
				String[] lockFailedObjs = new String[1];
				lockFailedObjs[0] = owner.getUID().getString();
				addUserLockingTheObj(lockFailedObjs, errorArgs);
				return new ConvertPinTypeLogEnum(
						lockFailed.size() == 1 ? ConvertPinTypeLogEnum._CANNOT_LOCK_OBJECT_IN_MU :
								ConvertPinTypeLogEnum._CANNOT_LOCK_OBJECTS_IN_MU, errorArgs);
			}
			//check if conversion valid after design refreshed
			ConvertPinTypeLogEnum convertPinTypeLogEnum = isEnabled();
			if (convertPinTypeLogEnum != ConvertPinTypeLogEnum.NO_ERROR) {
				return convertPinTypeLogEnum;
			}
		}

		//lock
		ConvertPinTypeLogEnum status = lockSharedDevices(m_affectedSharedPinLists);
		if (status != ConvertPinTypeLogEnum.NO_ERROR) {
			return status;
		}

		//check validity
		status = checkSharedPinIsValidForConversion();
		if (status != ConvertPinTypeLogEnum.NO_ERROR) {
			for (ISharedPinList spl : m_affectedSharedPinLists) {
				SharedPinListHelper.unlock(spl);
			}
			return status;
		}

		//convert
		convertPins();

		//flush and unlock
		flushAndUnlockSharedDevices(m_affectedSharedPinLists);

		return ConvertPinTypeLogEnum.NO_ERROR;
	}

	private void addUserLockingTheObj(String[] lockFailedObjs, List<String> errorArgs)
	{
		try {
			IUserSession userSession = UtilsHelper.getCHSSystem().getUserSession();
			if (userSession != null) {
				ILockInfo[] lockInfos = userSession.getLockInfos(lockFailedObjs);
				if (lockInfos.length > 0) {
					errorArgs.add(lockInfos[0].getUserName());
				}
			}
		}
		catch (UserSessionException e) {
			e.printStackTrace();
		}
	}

	@NotNull private String linksToLockFailedObjs(ILogicDesign logicDesign, Set<IUID> lockFailed)
	{
		Set<IDevicePin> lockFailedPins = m_selectedPins.stream()
				.filter(pin -> lockFailed.contains(pin.getUID()))
				.collect(Collectors.toSet());
		KeySeparatedStringBuilder hyperLink = new KeySeparatedStringBuilder(", ");
		int objCount = 0;
		for (IDevicePin devicePin : lockFailedPins) {
			if (objCount > 2) {
				hyperLink.append("...");
				break;
			}
			hyperLink.append(HTMLHelper.linkCheckParams(logicDesign, devicePin, devicePin.getName()));
			++objCount;
		}
		return hyperLink.toString();
	}

	public ConvertPinTypeLogEnum checkSharedPinIsValidForConversion()
	{
		ConvertPinTypeLogEnum status = ConvertPinTypeLogEnum.NO_ERROR;
		if (m_isSharedContext) {
			PinTypeConversionChecker checker = new PinTypeConversionChecker();
			List<ISharedPin> sharedPins = getSharedPins(m_selectedPins);
			ILogicDesign logicDesign = getSelectedLogicDesign();
			assert logicDesign != null;
			Map<String, List<SharedPinUsageInfo>> usedDesigns =
					SharedPinHelper.getUsedDesigns(sharedPins);
			Map<String, List<SharedPinUsageInfo>> otherDesignUSages
					= filterSharedPinUsageInfo(usedDesigns, logicDesign);

			boolean bValid = checker.executeProjectLevelCheckForPinConversion(sharedPins, otherDesignUSages, true);
			if (!bValid) {
				for (ISharedPin sharedPin : sharedPins) {
					List<ConvertPinTypeLogEnum> log = checker.getPinConversionStatus(sharedPin);
					if (!log.isEmpty()) {
						return log.get(0);
					}
				}
			}
		}
		return status;
	}

	protected Map<String, List<SharedPinUsageInfo>> filterSharedPinUsageInfo(
			Map<String, List<SharedPinUsageInfo>> usedDesigns, ILogicDesign logicDesign)
	{
		Map<String, List<SharedPinUsageInfo>> otherDesignUsages = new HashMap<>();
		for (String shared_ID : usedDesigns.keySet()) {
			for (SharedPinUsageInfo usageInfo : usedDesigns.get(shared_ID)) {
				if (!usageInfo.designUID.equals(logicDesign.getUID().toString())) {
					if (!otherDesignUsages.containsKey(shared_ID)) {
						otherDesignUsages.put(shared_ID, new LinkedList<>());
					}
					otherDesignUsages.get(shared_ID).add(usageInfo);
				}
			}
		}
		return otherDesignUsages;
	}

	@Nullable protected ILogicDesign getSelectedLogicDesign()
	{
		if (m_selectedPins.isEmpty()) {
			return null;
		}
		IPinList pinlist = m_selectedPins.iterator().next().getOwner();
		assert pinlist != null;
		return pinlist.getLogicDesign();
	}

	private void convertPins()
	{
		for (IDevicePin pin : m_selectedPins) {
			if (pin instanceof IPrivilegedDevicePin) {
				((IPrivilegedDevicePin) pin).setStud(isStudAfterConversion());
			}
		}
	}

	@Override public List<IDevicePin> getSelectedPins()
	{
		return m_selectedPins;
	}

	public ConvertPinTypeLogEnum isEnabled()
	{
		return checkLibraryPartAssigned(m_selectedPins);
	}

	protected abstract boolean isStudAfterConversion();

	public boolean isSharedContext()
	{
		return m_isSharedContext;
	}

	protected void flushAndUnlockSharedDevices(Set<ISharedDevice> affectedSharedPinLists)
	{
		for (ISharedPinList spl : affectedSharedPinLists) {
			if (spl != null) {
				//Save the changes made to the shared object and release the lock
				if (SharedPinListHelper.flush(spl)) {
					IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
					String projectUid = spl.getProject().getUID().getString();
					auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, null, projectUid,
							spl.getFullName(), spl.getUID().getString());
				}

				//Refresh the designs where the shared device is used and they are open
				SharedPinListHelper.unlock(spl);
			}
		}
	}

	protected ConvertPinTypeLogEnum lockSharedDevices(Set<ISharedDevice> affectedSharedPinLists)
	{
		if (!affectedSharedPinLists.isEmpty()) {
			Set<ISharedDevice> editableSharedObjects =
					LogicUtils.getEditableSharedObjects(affectedSharedPinLists.iterator());
			if (!editableSharedObjects.containsAll(affectedSharedPinLists)) {
				return ConvertPinTypeLogEnum.SHAREDOBJECT_DOMAIN_NOTEDITABLE;
			}
		}
		for (ISharedPinList spl : affectedSharedPinLists) {
			if (spl != null) {
				//Check if the shared object can be modified
				if (!SharedPinListHelper.lock(spl)) {
					return ConvertPinTypeLogEnum.CANNOT_LOCK_SHAREDOBJECT;
				}
			}
		}
		return ConvertPinTypeLogEnum.NO_ERROR;
	}

	protected ConvertPinTypeLogEnum checkLibraryPartAssigned(List<IDevicePin> selectedPins)
	{
		for (IDevicePin studPin : selectedPins) {
			IPinList device = studPin.getOwner();
			assert device != null;
			if (device.isPartAssigned()) {
				return ConvertPinTypeLogEnum.CANNOT_CONVERT_LIBRARY_PIN;
			}
		}
		return ConvertPinTypeLogEnum.NO_ERROR;
	}

	protected List<ISharedPin> getSharedPins(List<IDevicePin> selectedPins)
	{
		List<ISharedPin> sharedPins = new ArrayList<ISharedPin>();
		for (IDevicePin devicePin : selectedPins) {
			ISharedPin sharedPin = devicePin.getSharedPin();
			if (sharedPin != null) {
				sharedPins.add(sharedPin);
			}
		}
		return sharedPins;
	}
}