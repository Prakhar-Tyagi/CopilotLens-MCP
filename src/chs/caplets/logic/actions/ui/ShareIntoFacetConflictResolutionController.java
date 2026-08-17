package chs.caplets.logic.actions.ui;

import chs.cof.COFTypeEnum;
import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.footprint.IUserDeviceFootprint;
import chs.cof.logical.footprint.IUserFootprintConnector;
import chs.cof.logical.footprint.IUserFootprintMapping;
import chs.cof.logical.footprint.user.UserFootprintHelper;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.common.INamedPropertiedObject;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import chs.utility.helpers.IFootprintMergeHandler;
import chs.utility.helpers.UserFootprintUtils;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * @author chandras on 09-03-2018.
 */
public class ShareIntoFacetConflictResolutionController
		extends FacetConflictResolutionModel<ILogicObject, ILogicObject>
		implements IShareIntoFacetConflictResolutionController
{

	@NotNull protected final SetMap<IFacetDataProvider, IFacetDataProvider> m_sourceTree =
			SetMap.createShallowSetMap(true);
	@NotNull protected final Set<IFacetDataProvider> m_rootSourceNodes = new LinkedHashSet<>();
	@NotNull private final HashMap<IUID, IUID> m_srcToTgtPinMap = new HashMap<>();
	@NotNull private final HashMap<IUID, IUID> m_srcToTgtMulticoreMap = new HashMap<>();
	@Nullable private IUserDeviceFootprint m_sourceDeviceFootprint;

	public ShareIntoFacetConflictResolutionController(@NotNull ILogicObject source)
	{
		super(source);
		IDevice sourceDevice = CommonUtils.cast(source, IDevice.class);
		if (sourceDevice != null) {
			m_sourceDeviceFootprint = UserFootprintHelper.generateDefaultDeviceFootprint(sourceDevice);
		}
		Map<ILogicObject, IFacetDataProvider> sourceNodes = new HashMap<>();
		doComputeSourceTreeNodes(sourceNodes, source);
	}

	public void setupPinMap(@NotNull Map<IUID, IUID> srcToTgtPinMap)
	{
		m_srcToTgtPinMap.clear();
		m_srcToTgtPinMap.putAll(srcToTgtPinMap);
	}

	public void setupMulticoreMap(@NotNull Map<IUID, IUID> srcToTgtMulticoreMap)
	{
		m_srcToTgtMulticoreMap.clear();
		m_srcToTgtMulticoreMap.putAll(srcToTgtMulticoreMap);
	}

	private void addSourceTreeObject(@NotNull Map<ILogicObject, IFacetDataProvider> conflictNodes,
			@NotNull ILogicObject target)
	{
		IFacetDataProvider targetConflictNode = conflictNodes.get(target);
		if (target instanceof IGenericPin) {
			addSourceTreeObject(conflictNodes, targetConflictNode, ((IGenericPin) target).getOwner());
		}
		else if (target instanceof IBackshell) {
			addSourceTreeObject(conflictNodes, targetConflictNode, ((IBackshell) target).getOwner());
		}
		else if (target instanceof IDeviceConnector) {
			addSourceTreeObject(conflictNodes, targetConflictNode, ((IDeviceOwned) target).getOwner());
		}
		else if (target instanceof IConductor && ((IConductor) target).getMulticore() != null) {
			addSourceTreeObject(conflictNodes, targetConflictNode, ((IConductor) target).getMulticore());
		}
		else if (target instanceof IMulticore && ((IAbstractMulticore) target).getParent() != null) {
			addSourceTreeObject(conflictNodes, targetConflictNode, ((IMulticore) target).getParent());
		}
		else {
			m_sourceTree.add(targetConflictNode);
			m_rootSourceNodes.add(targetConflictNode);
		}
	}

	private void addSourceTreeObject(@NotNull Map<ILogicObject, IFacetDataProvider> conflictNodes,
			IFacetDataProvider targetConflictNode, @Nullable ILogicObject owner)
	{
		if (owner != null) {
			IFacetDataProvider ownerFacetConflictNode = conflictNodes.get(owner);
			if (ownerFacetConflictNode != null) {
				addSourceTreeObject(conflictNodes, owner);
				m_sourceTree.add(ownerFacetConflictNode, targetConflictNode);
			}
		}
	}

	private void doComputeSourceTreeNodes(@NotNull Map<ILogicObject, IFacetDataProvider> sourceNodes,
			@NotNull ILogicObject source)
	{
		if (sourceNodes.containsKey(source)) {
			//for peer objects i.e inlines we may endup in recursive calls.
			return;
		}
		StoredFacetDataProvider holder = new StoredFacetDataProvider(source);
		sourceNodes.put(source, holder);
		addSourceTreeObject(sourceNodes, source);

		//look for pins.
		if (source instanceof IPinList) {
			for (IAbstractPin pin : ((IPinList) source).getPins()) {
				doComputeSourceTreeNodes(sourceNodes, pin);
			}

			if (source instanceof IConnector) {
				IBackshell srcBS = ((IConnector) source).getBackshell();
				if (srcBS != null) {
					doComputeSourceTreeNodes(sourceNodes, srcBS);
				}

				if (source instanceof IGenericInlineConnector) {
					IConnector sourceMate = deriveInlineMate((IGenericInlineConnector) source);
					if (sourceMate != null) {
						doComputeSourceTreeNodes(sourceNodes, sourceMate);
					}
				}
			}

			if (source instanceof IDevice) {
				for (IDeviceConnector srcDevConn : ((IDevice) source).getDeviceConnectors()) {
					doComputeSourceTreeNodes(sourceNodes, srcDevConn);
				}
			}
		}
		if (source instanceof IMulticore) {
			for (IConductor conductor : ((IMulticore) source).getConductorsIncludingShields()) {
				doComputeSourceTreeNodes(sourceNodes, conductor);
			}
			for (IMulticore multicore : ((IMulticore) source).getMulticores()) {
				doComputeSourceTreeNodes(sourceNodes, multicore);
			}
		}
	}

	protected boolean allowConflictResolution(@NotNull ILogicObject target)
	{
		ISharedObject sharedObject = target.getSharedObject();
		if (sharedObject == null) {
			//no conflict resolution for unshared objects here.
			return false;
		}

		if (!sharedObject.isFrozen()) {
			return true;
		}

		if (sharedObject instanceof ISharedPin) {
			//no conflict resolution for frozen shared objects except for pins if permission available.
			return UtilsHelper.getCHSSystem().getFunctionalPermissionMgr().hasPermission(
					FunctionalPermissionEnum.FrozenSharedObjectPinAttributesAndProperties);
		}
		return false;
	}

	private void doComputeConflictNodes(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull IFacetDataProvider sourceDataProvider, @NotNull ILogicObject target)
	{
		if (conflictNodes.containsKey(target)) {
			//for peer objects i.e inlines we may endup in recursive calls.
			return;
		}

		FacetConflictNode holder = new FacetConflictNode(sourceDataProvider, new DelegateFacetDataProvider(target));
		if (allowConflictResolution(target)) {
			holder.computeConflicts(false);
		}

		conflictNodes.put(target, holder);
		if (holder.hasConflicts()) {
			addConflictTreeObject(conflictNodes, target);
		}

		//look for pins.
		INamedPropertiedObject source = sourceDataProvider.getObject();
		if (source instanceof IPinList && target instanceof IPinList) {
			if (target instanceof IBackshell) {
				processBackshellTerminations(conflictNodes, sourceDataProvider, (IBackshell) target);
			}
			else {
				processPins(conflictNodes, sourceDataProvider, target);
			}

			if (source instanceof IConnector && target instanceof IConnector) {
				processConnectors(conflictNodes, sourceDataProvider, (IConnector) target, (IConnector) source);
			}

			if (source instanceof IDevice && target instanceof IDevice) {
				processDeviceConnectors(conflictNodes, sourceDataProvider, (IDevice) source, (IDevice) target);
			}
		}
		if (source instanceof IMulticore && target instanceof IMulticore) {
			processMulticores(conflictNodes, sourceDataProvider, (IMulticore) target);
		}
	}

	private void processMulticores(Map<ILogicObject, IFacetConflictNode> conflictNodes,
			IFacetDataProvider sourceDataProvider, @NotNull IMulticore target)
	{
		Map<IUID, ILogicObject> targetChildren = new HashMap<>();
		for (IConductor conductor : target.getConductorsIncludingShields()) {
			targetChildren.put(conductor.getUID(), conductor);
		}
		for (IMulticore multicore : target.getMulticores()) {
			targetChildren.put(multicore.getUID(), multicore);
		}
		for (IFacetDataProvider childNode : m_sourceTree.pullReadOnlySafeSet(sourceDataProvider)) {
			IUID tgtId = m_srcToTgtMulticoreMap.get(childNode.getUID());
			if (tgtId != null) {
				ILogicObject targetMatch = targetChildren.get(tgtId);
				if (targetMatch != null) {
					doComputeConflictNodes(conflictNodes, childNode, targetMatch);
				}
			}
		}
	}

	private void processDeviceConnectors(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull IFacetDataProvider sourceDataProvider, @NotNull IDevice sourceDevice,
			@NotNull IDevice targetDevice)
	{
		Map<String, IDeviceConnector> targetDevConns = new HashMap<>();
		targetDevice.getDeviceConnectors().forEach(dc -> targetDevConns.put(dc.getName(), dc));

		Map<String, IFacetDataProvider> sourceDevConns = new HashMap<>();
		for (IFacetDataProvider childNode : m_sourceTree.pullReadOnlySafeSet(sourceDataProvider)) {
			if (COFTypeEnum.DeviceConnector.equals(childNode.getType())) {
				sourceDevConns.put(childNode.getName(), childNode);
			}
		}

		IFootprintMergeHandler mergeHandler = new IFootprintMergeHandler()
		{
			@Override public void exactMatch(@NotNull IUserFootprintConnector srcConnector,
					@NotNull IUserFootprintConnector tgtConnector)
			{
				IFacetDataProvider srcDevConn = sourceDevConns.get(srcConnector.getName());
				IDeviceConnector targetMatch = targetDevConns.get(tgtConnector.getName());
				if (targetMatch != null && srcDevConn != null) {
					doComputeConflictNodes(conflictNodes, srcDevConn, targetMatch);
				}
			}

			@Override
			public void moveToTarget(@NotNull IUserFootprintMapping srcRow, @NotNull String connRename,
					@Nullable MergeFeedback feedback)
			{

			}

			@Override public void mergeWithTarget(@NotNull IUserFootprintMapping srcMapping,
					@NotNull IUserFootprintConnector targetConnector)
			{
				exactMatch(srcMapping.getConnector(), targetConnector);
			}
		};

		IUserDeviceFootprint sourceFP = m_sourceDeviceFootprint;
		IUserDeviceFootprint targetFP = UserFootprintHelper.generateDefaultDeviceFootprint(targetDevice);

		Map<String, String> srcToTgtDevPinMap = generateSourceDevToTargetDevPinMap(sourceDataProvider, targetDevice);

		Function<String, String> pinMapper = (p) -> srcToTgtDevPinMap.getOrDefault(p, p);
		String renamePrefix = sourceDataProvider.getName();
		boolean mergeDeviceConnectors = Optional.ofNullable(sourceDevice.getProject())
				.map(IProject::getPreferences)
				.map(IProjectPreferenceMgr::isMergeDeviceConnectorsEnabled)
				.orElse(false);
		UserFootprintUtils.processFootprintMerge(renamePrefix, sourceFP, targetFP, pinMapper, mergeHandler,
				mergeDeviceConnectors);
	}

	@NotNull private Map<String, String> generateSourceDevToTargetDevPinMap(
			@NotNull IFacetDataProvider sourceDataProvider, @NotNull IDevice targetDevice)
	{
		Map<IUID, IAbstractPin> targetPins = new HashMap<>();
		targetDevice.getPins().forEach(pin -> targetPins.put(pin.getUID(), pin));

		Map<String, String> srcToTgtDevPinMap = new HashMap<>();
		for (IFacetDataProvider childNode : m_sourceTree.pullReadOnlySafeSet(sourceDataProvider)) {
			IUID tgtId = m_srcToTgtPinMap.get(childNode.getUID());
			if (tgtId != null) {
				IAbstractPin targetMatch = targetPins.get(tgtId);
				if (targetMatch != null) {
					srcToTgtDevPinMap.put(childNode.getName(), targetMatch.getName());
				}
			}
		}
		return srcToTgtDevPinMap;
	}

	private void processConnectors(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull IFacetDataProvider sourceDataProvider, @NotNull IConnector target, @NotNull IConnector source)
	{
		for (IFacetDataProvider childNode : m_sourceTree.pullReadOnlySafeSet(sourceDataProvider)) {
			if (COFTypeEnum.Backshell.equals(childNode.getType())) {
				IBackshell destBS = target.getBackshell();
				if (destBS != null) {
					doComputeConflictNodes(conflictNodes, childNode, destBS);
				}
				break;
			}
		}

		if (source instanceof IGenericInlineConnector && target instanceof IGenericInlineConnector) {
			IConnector sourceMate = deriveInlineMate((IGenericInlineConnector) source);
			IConnector destMate = deriveInlineMate((IGenericInlineConnector) target);
			if (sourceMate != null && destMate != null) {
				IFacetDataProvider candidateSource = findRootFacetDataProvider(sourceMate);
				if (candidateSource != null) {
					doComputeConflictNodes(conflictNodes, candidateSource, destMate);
				}
			}
		}
	}

	private void processPins(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull IFacetDataProvider sourceDataProvider, @NotNull ILogicObject target)
	{
		Map<IUID, IAbstractPin> targetPins = new HashMap<>();
		for (IAbstractPin pin : ((IPinList) target).getPins()) {
			targetPins.put(pin.getUID(), pin);
		}
		for (IFacetDataProvider childNode : m_sourceTree.pullReadOnlySafeSet(sourceDataProvider)) {
			IUID tgtId = determineTargetPinUsingMateInformation(target, childNode);
			if (tgtId != null) {
				IAbstractPin targetMatch = targetPins.get(tgtId);
				if (targetMatch != null) {
					doComputeConflictNodes(conflictNodes, childNode, targetMatch);
				}
			}
		}
	}

	private void processBackshellTerminations(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull IFacetDataProvider sourceDataProvider, @NotNull IBackshell target)
	{
		Map<String, IAbstractPin> targetPins = new HashMap<>();
		for (IAbstractPin pin : target.getPins()) {
			targetPins.put(pin.getName(), pin);
		}
		for (IFacetDataProvider childNode : m_sourceTree.pullReadOnlySafeSet(sourceDataProvider)) {
			if (COFTypeEnum.BackshellTermination.equals(childNode.getType())) {
				IAbstractPin targetMatch = targetPins.get(childNode.getName());
				if (targetMatch != null) {
					doComputeConflictNodes(conflictNodes, childNode, targetMatch);
				}
			}
		}
	}

	@Nullable private IUID determineTargetPinUsingMateInformation(@NotNull ILogicObject target,
			@NotNull IFacetDataProvider childNode)
	{
		IUID tgtId = m_srcToTgtPinMap.get(childNode.getUID());
		if (tgtId == null && target instanceof IGenericInlineConnector) {
			IConnectorPin srcConnectorPin = CommonUtils.cast(childNode.getObject(), IConnectorPin.class);
			if (srcConnectorPin != null) {
				IConnectorPin srcConnMatedPin = srcConnectorPin.getMatedPin();
				if (srcConnMatedPin != null) {
					IUID tgtMatePinId = m_srcToTgtPinMap.get(srcConnMatedPin.getUID());
					IConnectorPin tgtMatePin = UIDMgr.getObjectOfType(tgtMatePinId, IConnectorPin.class);
					if (tgtMatePin != null) {
						IConnectorPin tgtPin = tgtMatePin.getMatedPin();
						if (tgtPin != null) {
							return tgtPin.getUID();
						}
					}
				}
			}
		}
		return tgtId;
	}

	@Override protected void doTargetChanged(@NotNull ILogicObject target)
	{
		IFacetDataProvider candidateSource = findRootFacetDataProvider(m_source);
		if (candidateSource != null) {
			Map<ILogicObject, IFacetConflictNode> conflictNodes = new HashMap<>();
			doComputeConflictNodes(conflictNodes, candidateSource, target);
		}
	}

	@Nullable private IFacetDataProvider findRootFacetDataProvider(@NotNull INamedPropertiedObject source)
	{
		IFacetDataProvider candidateSource = null;
		for (IFacetDataProvider rootSourceNode : m_rootSourceNodes) {
			if (rootSourceNode.getObject().equals(source)) {
				candidateSource = rootSourceNode;
				break;
			}
		}
		return candidateSource;
	}
}
