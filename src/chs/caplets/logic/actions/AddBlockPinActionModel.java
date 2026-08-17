package chs.caplets.logic.actions;

import chs.ans.IObjectDescriptor;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IBlockDevicePin;
import chs.cof.logical.schem.IPinList;
import chs.common.IDesignContainer;
import chs.ctf.caf.utils.BlockPinProxy;
import chs.ctf.caf.utils.IBlockPinProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.helpers.BlockDeviceUnconnectedPinSelectionHelper;
import chs.utility.logic.DesignHelper;
import chs.utility.logic.PinUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: brangan Date: 1/9/14 Time: 5:37 PM To change this template use File | Settings |
 * File Templates.
 */
public class AddBlockPinActionModel extends AddPinActionModel implements IAddBlockPinActionModel
{

	private Collection<IBlockPinAssociationInfo> m_pinListInfos = null;
	private static final String KEY_SEPARATOR = "$";
	@NotNull private final IBlockDevice m_blockDevice;
	private Map<String, IBlockDevicePin> m_existingPinInfos = null;
	private IDesignContainer m_design = null;
	private Map<String, IPinProxy> m_proxyMap = null;
	private boolean m_isProcessUsedPins = false;

	public static class BlockPinAssociationInfo implements IBlockPinAssociationInfo
	{

		private String mpinName;
		private String mpinListName;
		private String mpinListType;

		public BlockPinAssociationInfo(String pinListType, String pinListName, String pinName)
		{
			mpinListName = pinListName;
			mpinName = pinName;
			mpinListType = pinListType;
		}

		public String getPinName()
		{
			return mpinName;
		}

		public String getPinListName()
		{
			return mpinListName;
		}

		public String getPinListType()
		{
			return mpinListType;
		}
	}

	public AddBlockPinActionModel(@NotNull final IPinList pinList)
	{
		super(pinList);
		m_blockDevice = (IBlockDevice) pinList.getConnectivity();
		init();
	}

	public AddBlockPinActionModel(@NotNull final IBlockDevice blockdevice)
	{
		super(null);
		m_blockDevice = blockdevice;
		init();
	}

	@Override @NotNull public IBlockDevice getBlockDevice()
	{
		return m_blockDevice;
	}

	private void init()
	{
		m_design = DesignHelper.getDesignNotNull(m_blockDevice, ILogicDesign.class);

		populateExistingPinListInfos();
		populateValues();

		assert (m_design.getProject() != null);
		// dts0101023590 - if pinduplication is allowed then we have one more level of filter on the UI to allow/or not.
		m_isProcessUsedPins = PinUtils.allowDuplicatePinsOnDesign(m_design.getProject());
	}

	private void populateValues()
	{
		if (m_pinListInfos == null) {
			m_pinListInfos = new ArrayList<IBlockPinAssociationInfo>(100);
		}
		IBlockDevice blkDevice = m_blockDevice;
		List<IObjectDescriptor> unconnectedPins = getUnconnectedPins(blkDevice);

		for (IObjectDescriptor desc : unconnectedPins) {
			BlockDeviceUnconnectedPinSelectionHelper.TargetPinDetailsFromObjectDesc pinDetails =
					BlockDeviceUnconnectedPinSelectionHelper.getPinDetails(desc);

			BlockPinAssociationInfo pinListInfo = new BlockPinAssociationInfo(
					pinDetails.getAssociatedObjectType(),
					pinDetails.getAssociatedObject(), pinDetails.getAssociatedPinName());
			m_pinListInfos.add(pinListInfo);
		}
		getAllPinsProxyMap();
	}

	protected List<IObjectDescriptor> getUnconnectedPins(IBlockDevice blkDevice)
	{
		return BlockDeviceUnconnectedPinSelectionHelper.getUnconnectedPins(blkDevice);
	}

	private Map<String, IPinProxy> getOnlyUnUsedPinProxyMap()
	{
		Map<String, IPinProxy> allPins = getAllPinsProxyMap();
		Map<String, IPinProxy> unUsedPins = new LinkedHashMap<String, IPinProxy>(allPins.size());
		for (Map.Entry<String, IPinProxy> entry : allPins.entrySet()) {
			IBlockPinProxy value = (IBlockPinProxy) entry.getValue();
			if (value.getCablePin() == null) {
				unUsedPins.put(entry.getKey(), entry.getValue());
			}
		}

		return unUsedPins;
	}

	public Map<String, IPinProxy> getPinProxyMap()
	{
		Map<String, IPinProxy> returnVal;
		if (isProcessUsedPins()) {
			returnVal = getAllPinsProxyMap();
		}
		else {
			returnVal = getOnlyUnUsedPinProxyMap();
		}

		return returnVal;
	}

	private Map<String, IPinProxy> getAllPinsProxyMap()
	{
		if (m_proxyMap == null) {
			m_proxyMap = new LinkedHashMap<String, IPinProxy>();
			// library part - use the pin names from the library "cavities"
			for (IBlockPinAssociationInfo info : m_pinListInfos) {
				if (!isPlaceable(info)) {
					continue;
				}
				String pinKey = getPinKey(info);
				IBlockDevicePin blockDevicePin = m_existingPinInfos.get(pinKey);
				PinProxy pinProxy = new BlockPinProxy(info.getPinName(), false,
						info.getPinListType(), info.getPinListName(), info.getPinName(),
						blockDevicePin);
				m_proxyMap.put(getPinKey(info), pinProxy);
			}
		}

		return m_proxyMap;
	}

	protected String getPinKey(IBlockPinAssociationInfo info)
	{
		return getPinKey(info.getPinListType(), info.getPinListName(), info.getPinName());
	}

	protected String getPinKey(String pinListType, String pinListName, String pinName)
	{
		return pinListType.toUpperCase() + KEY_SEPARATOR + pinListName + KEY_SEPARATOR + pinName;
	}

	public boolean isPlaceable(IBlockPinAssociationInfo info)
	{
		String pinKey = getPinKey(info);
		IBlockDevicePin blockDevicePin = m_existingPinInfos.get(pinKey);
		//we can have null parameter in case library cavities which are not yet used/placed.

		return blockDevicePin == null || PinUtils.isPinPlaceableInDesign(blockDevicePin, m_design);
	}

	private void populateExistingPinListInfos()
	{
		m_existingPinInfos = new HashMap<String, IBlockDevicePin>();
		Collection<IAbstractPin> pinCollection = m_blockDevice.getPinCollection();
		List<IBlockDevicePin> blockPins = CollectionUtils.getObjectList(pinCollection, IBlockDevicePin.class);
		for (IBlockDevicePin pin : blockPins) {
			String targetPinlistType = pin.getAssociatedObjectType();
			String targetPinlistName = pin.getAssociatedObject();
			String targetPinName = pin.getAssociatedPinName();
			if (!StringUtils.isBlank(targetPinlistType) && !StringUtils.isBlank(targetPinlistName)
					&& !StringUtils.isBlank(targetPinName)) {
				String pinKey = getPinKey(targetPinlistType, targetPinlistName, targetPinName);
				m_existingPinInfos.put(pinKey, pin);
			}
		}
	}

	public boolean isValid(@NotNull List<IPinProxy> pinsToValidate)
	{
		boolean isValid = true;
		for (IPinProxy pin : pinsToValidate) {
			if (!pin.isValid()) {
				isValid = false;
			}
		}

		return isValid;
	}

	public String getInvalidityReason()
	{
		return ResourceMgr.getString(AddBlockPinActionModel.class
				, "AddBlockPinActionModel.PinNameHasMoreThanDBLimitChars.text");
	}

	public void setProcessUsedPins(boolean isProcessUsedPins)
	{
		m_isProcessUsedPins = isProcessUsedPins;
	}

	public boolean isProcessUsedPins()
	{
		return m_isProcessUsedPins;
	}

	public boolean isGetInputsForUsedPins()
	{
		return PinUtils.allowDuplicatePinsOnDesign(m_design.getProject());
	}
}
