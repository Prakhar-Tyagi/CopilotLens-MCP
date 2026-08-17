package chs.caplets.logic.actions.ui;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.footprint.IUserDeviceFootprint;
import chs.cof.logical.footprint.IUserFootprintConnector;
import chs.cof.logical.footprint.IUserFootprintMapping;
import chs.cof.logical.footprint.user.UserFootprintHelper;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import chs.common.IProjectPreferenceMgr;
import chs.utility.helpers.IFootprintMergeHandler;
import chs.utility.helpers.UserFootprintUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author chandras on 09-03-2018.
 */
public class MergeIntoFacetConflictResolutionController extends FacetConflictResolutionModel<ILogicObject, ILogicObject>
		implements IMergeIntoFacetConflictResolutionController
{

	public MergeIntoFacetConflictResolutionController(@NotNull ILogicObject source)
	{
		super(source);
	}

	protected boolean allowConflictResolution(@NotNull ILogicObject target)
	{
		ISharedObject sharedObject = target.getSharedObject();
		//no conflict resolution for shared objects here.
		return sharedObject == null;
	}

	private void doComputeConflictNodes(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull ILogicObject source, @NotNull ILogicObject target)
	{
		if (conflictNodes.containsKey(target)) {
			//for peer objects i.e inlines we may endup in recursive calls.
			return;
		}

		FacetConflictNode holder = new FacetConflictNode(new DelegateFacetDataProvider(source),
				new DelegateFacetDataProvider(target));
		if (allowConflictResolution(target)) {
			holder.computeConflicts(true);
		}

		conflictNodes.put(target, holder);
		if (holder.hasConflicts()) {
			addConflictTreeObject(conflictNodes, target);
		}
		//look for pins.
		if (source instanceof IPinList && target instanceof IPinList) {
			processPins(conflictNodes, (IPinList) source, (IPinList) target);
			if (source instanceof IConnector && target instanceof IConnector) {
				processConnectors(conflictNodes, (IConnector) source, (IConnector) target);
			}
			if (source instanceof IDevice && target instanceof IDevice) {
				processDeviceConnectors(conflictNodes, (IDevice) source, (IDevice) target);
			}
		}
	}

	private void processConnectors(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull IConnector source, @NotNull IConnector target)
	{
		IBackshell srcBS = source.getBackshell();
		IBackshell destBS = target.getBackshell();
		if (srcBS != null && destBS != null) {
			doComputeConflictNodes(conflictNodes, srcBS, destBS);
		}

		if (source instanceof IGenericInlineConnector && target instanceof IGenericInlineConnector) {
			IConnector sourceMate = deriveInlineMate((IGenericInlineConnector) source);
			IConnector destMate = deriveInlineMate((IGenericInlineConnector) target);
			if (sourceMate != null && destMate != null) {
				doComputeConflictNodes(conflictNodes, sourceMate, destMate);
			}
		}
	}

	private void processPins(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes, @NotNull IPinList source,
			@NotNull IPinList target)
	{
		Map<String, IAbstractPin> targetPins = new HashMap<>();
		for (IAbstractPin pin : target.getPins()) {
			targetPins.put(pin.getName(), pin);
		}
		for (IAbstractPin pin : source.getPins()) {
			IAbstractPin targetMatch = targetPins.get(pin.getName());
			if (targetMatch != null) {
				doComputeConflictNodes(conflictNodes, pin, targetMatch);
			}
		}
	}

	private void processDeviceConnectors(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull IDevice sourceDevice, @NotNull IDevice targetDevice)
	{
		Map<String, IDeviceConnector> sourceDevConns = new HashMap<>();
		sourceDevice.getDeviceConnectors().forEach(dc -> sourceDevConns.put(dc.getName(), dc));

		Map<String, IDeviceConnector> targetDevConns = new HashMap<>();
		targetDevice.getDeviceConnectors().forEach(dc -> targetDevConns.put(dc.getName(), dc));

		IFootprintMergeHandler mergeHandler = new IFootprintMergeHandler()
		{
			@Override public void exactMatch(@NotNull IUserFootprintConnector srcConnector,
					@NotNull IUserFootprintConnector tgtConnector)
			{
				IDeviceConnector srcDevConn = sourceDevConns.get(srcConnector.getName());
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

		IUserDeviceFootprint sourceFP = UserFootprintHelper.generateDefaultDeviceFootprint(sourceDevice);
		IUserDeviceFootprint targetFP = UserFootprintHelper.generateDefaultDeviceFootprint(targetDevice);

		Function<String, String> pinMapper = Function.identity();
		String renamePrefix = sourceDevice.getName();
		boolean mergeDeviceConnectors = Optional.ofNullable(sourceDevice.getProject())
				.map(IProject::getPreferences)
				.map(IProjectPreferenceMgr::isMergeDeviceConnectorsEnabled)
				.orElse(false);
		UserFootprintUtils.processFootprintMerge(renamePrefix, sourceFP, targetFP, pinMapper, mergeHandler,
				mergeDeviceConnectors);
	}

	@Override protected void doTargetChanged(@NotNull ILogicObject target)
	{
		Map<ILogicObject, IFacetConflictNode> conflictNodes = new HashMap<>();
		doComputeConflictNodes(conflictNodes, m_source, target);
	}
}
