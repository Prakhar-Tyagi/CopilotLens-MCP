/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2012-2024 Siemens
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.BatchLoadAndSequentialParseLogicDesigns;
import chs.caplets.logic.actions.ConnectionFlow;
import chs.caplets.logic.actions.shared.batchshare.BatchShareActionExecutor;
import chs.caplets.logic.actions.shared.batchshare.DeltaBatchShareCmd;
import chs.caplets.logic.actions.shared.batchshare.DeltaShareCmd;
import chs.caplets.logic.actions.shared.batchshare.FindAndShareCmd;
import chs.caplets.logic.actions.shared.batchshare.FunctionalBatchShareActionExecutor;
import chs.caplets.logic.actions.shared.batchshare.IBatchShareActionExecutor;
import chs.caplets.logic.actions.shared.batchshare.IEntityShareCriteria;
import chs.caplets.logic.actions.shared.batchshare.IFindAndShareCmd;
import chs.caplets.logic.actions.shared.batchshare.ShareableEntityTypeEnum;
import chs.caplets.logic.backshell.IBackshellBuilder;
import chs.caplets.logic.commands.AssociateLibraryPartCommand;
import chs.caplets.logic.commands.BatchUpdateFromDictionaryCmd;
import chs.caplets.logic.commands.BatchUpdateICDCmd;
import chs.caplets.logic.commands.BuildListDesignScope;
import chs.caplets.logic.commands.ConvertFilteredNetsToWiresCmd;
import chs.caplets.logic.commands.ConvertNetsToWiresCmd;
import chs.caplets.logic.commands.LogicObjectRegenerateHandler;
import chs.caplets.logic.commands.ManageConnectorsCmd;
import chs.caplets.logic.commands.ProjectWideDesignScope;
import chs.caplets.logic.commands.PropagateHarnessCmd;
import chs.caplets.logic.commands.RemoveLibraryPartHandler;
import chs.caplets.logic.commands.UnshareRedundantSharedObjectsCmd;
import chs.caplets.logic.harness.propagate.AutoPropagateHarnessController;
import chs.caplets.logic.helpers.backshell.BackshellBuilder;
import chs.caplets.logic.helpers.backshell.BackshellTransferService;
import chs.caplets.logic.helpers.backshell.IBackshellPinOverlapResolver;
import chs.caplets.logic.merge.IMergerFactory;
import chs.caplets.logic.merge.IBackshellTransferService;
import chs.caplets.logic.merge.MergerFactory;
import chs.caplets.shared.UnfreezeOutputTabHandler;
import chs.cof.draw.IColor;
import chs.cof.logical.IAssociateLibraryPartCommand;
import chs.cof.logical.IBatchUpdateICDCmd;
import chs.cof.logical.IConvertFilteredNetsToWiresCmd;
import chs.cof.logical.IConvertNetsToWiresCmd;
import chs.cof.logical.IDesignScopeResolver;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ILogicObjectRegenerateHandler;
import chs.cof.logical.IManageConnectorsCmd;
import chs.cof.logical.IPropagateHarnessCmd;
import chs.cof.logical.IPropagationInfo;
import chs.cof.logical.IRemoveLibraryPartHandler;
import chs.cof.logical.ISchemObjectDelete;
import chs.cof.logical.IUnfreezeOutputTabHandler;
import chs.cof.logical.IUnshareRedundantSharedObjectsCmd;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorIterator;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectsFinder;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.DefaultLibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.LibraryPinMapProvider;
import chs.ctf.caf.utils.PinMapProviderFactory;
import chs.system.FactoryMgr;
import chs.system.ILogicUtilsFactory;
import chs.utilities.AppInfo;
import chs.utilities.CommonUtils;
import chs.utilities.IBatchLoadAndSequentialParseLogicDesigns;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 1 Nov, 2012
 * <p/>
 * This is to construct & prvide logic specific objects to outside world (like datamodel, framework).
 */
public class LogicUtilsFactory implements ILogicUtilsFactory
{

	@Override @NotNull public IConvertNetsToWiresCmd createConvertNetsToWiresCmd(@NotNull IProject proj)
	{
		return new ConvertNetsToWiresCmd(proj);
	}

	@Override @NotNull public IConvertFilteredNetsToWiresCmd createConvertFilteredNetsToWiresCmd(@NotNull IProject proj,
			@NotNull Set<IUID> netsToConvert)
	{
		return new ConvertFilteredNetsToWiresCmd(proj, netsToConvert);
	}

	@Nullable public IAssociateLibraryPartCommand createAssociateLibraryPartCmd(@NotNull IPinList pinList, @NotNull
	ILibraryObject libraryObject)
	{
		IConnector connector = CommonUtils.cast(pinList.getConnectivity(), IConnector.class);
		if (connector == null) {
			return null;
		}
		ILibraryPartSelection partSelection = new DefaultLibraryPartSelection()
		{
			@Override public ILibraryObject getSelectedObject()
			{
				return libraryObject;
			}
		};
		partSelection.assignDefaultValues();
		return new AssociateLibraryPartCommand(pinList, libraryObject, partSelection)
		{
			@Override protected void doExecutePostAssignmentTasks()
			{
				super.doExecutePostAssignmentTasks();
				connector.setMultiMateOptionAlreadyResolved(true);
			}

			@Override protected boolean skipActualAssignment()
			{
				ILibraryBaseObject partNumber = pinList.getConnectivity().getLibraryObject();
				return libraryObject.equals(partNumber);
			}

			protected boolean preparePinMapping()
			{
				LibraryPinMapProvider pinMapProvider =
						PinMapProviderFactory.instance().createLibraryPinMapperProvider(libraryObject, connector);
				setPinMapping(pinMapProvider.generateMapping());
				return true;
			}
		};
	}

	@NotNull @Override
	public IUnshareRedundantSharedObjectsCmd createUnshareRedundantSharedObjectsCmd(@NotNull IProject project , List<IBuildList> buildLists)
	{
		IDesignScopeResolver scopeResolver;
		if(buildLists != null){
			scopeResolver = new BuildListDesignScope(buildLists);
		}else{
			scopeResolver = new ProjectWideDesignScope();
		}
		return new UnshareRedundantSharedObjectsCmd(project, scopeResolver);
	}
	@NotNull @Override public IBatchUpdateICDCmd createBatchUpdateICDCmd(@NotNull Set<ILogicDesign> designs)
	{
		return new BatchUpdateICDCmd(new CAFCommandHelper(), designs);
	}

	@NotNull @Override
	public IBatchUpdateICDCmd createBatchUpdateFromDictionaryCmd(@NotNull Set<IFunctionLogicDesign> designs)
	{
		return new BatchUpdateFromDictionaryCmd(new CAFCommandHelper(), designs);
	}

	@NotNull @Override public IManageConnectorsCmd createManageConnectorsCmd()
	{
		return new ManageConnectorsCmd();
	}

	@NotNull @Override public ISchemObjectDelete createSchemObjectDelete(@NotNull ISchemDiagram diagram)
	{
		return new SchemObjectDelete(diagram);
	}

	public void registerAffectedRegionForIndicatorRefresh(@NotNull ISegment segment)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(segment);
		if (diagram != null) {
			IndicatorRefresher indicatorRefresher = IndicatorRefresher.getIndicatorRefresher(diagram);
			ILocation startLoc = segment.getStartPoint();
			ILocation endLoc = segment.getEndPoint();
			if (SegmentHelper.isVerticalForShieldBody(segment)) {
				indicatorRefresher.registerYRange(startLoc.getY(), endLoc.getY());
			}
			else {
				indicatorRefresher.registerXRange(startLoc.getX(), endLoc.getX());
			}
		}
	}

	public void registerAffectedStripForIndicatorRefresh(@NotNull ISegment segment)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(segment);
		if (diagram != null) {
			IndicatorRefresher indicatorRefresher = IndicatorRefresher.getIndicatorRefresher(diagram);
			ILocation location = segment.getAbsLocation();
			indicatorRefresher.registerSegmentExtent(FactoryMgr.getCommonFactory()
					.constructExtent(location.getX(), location.getY(), segment.getExtent().getWidth(),
							segment.getExtent().getHeight()));
		}
	}

	@Override
	@NotNull public IFindAndShareCmd createFindAndShareCmd(@NotNull IProject project,
			@NotNull Set<ILogicDesign> designs, @NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		return new FindAndShareCmd(project, designs, entitiesShareCriteria);
	}

	@NotNull @Override
	public IFindAndShareCmd createDeltaShareCmd(@NotNull IProject project, @NotNull ILogicDesign design,
			@NotNull ISharedObjectsFinder sharedObjectsFinder,
			Map<ILogicObject, ISharedObject> targetToSourceShared, Collection<ILogicObject> objectsToBeShared)
	{
		return new DeltaShareCmd(project, design, sharedObjectsFinder, targetToSourceShared, objectsToBeShared);
	}
	@NotNull @Override
	public IFindAndShareCmd createDeltaBatchShareCmd(@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull SetMap<ShareableEntityTypeEnum, String> deltaAddedObjectsMap)
	{
		return new DeltaBatchShareCmd(project, designs, deltaAddedObjectsMap);
	}
	@Override
	@NotNull public IBatchShareActionExecutor createBatchShareActionExecutor(@NotNull IProject project,
			@NotNull Set<ILogicDesign> designs, @NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		return new BatchShareActionExecutor(project, designs, entitiesShareCriteria);
	}

	@Override
	@NotNull public IBatchShareActionExecutor createFunctionalBatchShareActionExecutor(@NotNull IProject project,
			@NotNull Set<ILogicDesign> designs, @NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		return new FunctionalBatchShareActionExecutor(project, designs, entitiesShareCriteria);
	}

	@NotNull
	public IAssociateLibraryPartCommand removeLibraryPartCmd(@NotNull IUIDObject object,
			@NotNull ISchemDiagram diagram)
	{
		//noinspection ConstantConditions
		AssociateLibraryPartCommand logicCmd =
				new AssociateLibraryPartCommand(new CAFCommandHelper(), diagram, object, null);
		logicCmd.setSyncPropertiesWithSource(false);
		logicCmd.setSyncModularConnectors(true);
		logicCmd.setPreserveIncludeOnBOM(false);
		logicCmd.setLibGrpTypeValidationBeforePartAssignment(false);
		return logicCmd;
	}

	@NotNull @Override public IRemoveLibraryPartHandler getRemoveLibraryPartHandler()
	{
		return new RemoveLibraryPartHandler();
	}

	@NotNull @Override public ILogicObjectRegenerateHandler getRegenerateHandler()
	{
		return new LogicObjectRegenerateHandler();
	}

	@NotNull @Override public IPropagateHarnessCmd createPropagateHarnessCmd(@NotNull ILogicDesign design, @NotNull
	IPropagationInfo propagationInfo)
	{
		return new PropagateHarnessCmd(new CAFCommandHelper(), design, propagationInfo);
	}

	@Override public void clearHarnessPropagateWindow()
	{
		AutoPropagateHarnessController.getInstance().clearHarnessPropagateWindow();
	}

	public void validateInlinesForPinMatings(@NotNull IConnectorIterator connectors)
	{
		getStream(connectors).filter(connector -> connector instanceof IGenericInlineConnector).forEach(
				connector -> {

					chs.cof.logical.cable.IPinList mate = connector.getMates().iterator().next();
					if (mate != null) {
						Set<IAbstractPin> pinsWithMissingMate = new HashSet<>();
						for (IAbstractPin pin : connector.getPins()) {
							if (pin.getConnectedPin(mate) == null) {
								pinsWithMissingMate.add(pin);
							}
						}
						if (!pinsWithMissingMate.isEmpty()) {
							notifyInlineValidationFailure(pinsWithMissingMate);
						}
					}
				}
		);
	}

	@NotNull protected Stream<IConnector> getStream(@NotNull IConnectorIterator connectors)
	{
		return connectors.stream();
	}

	@Override public void notifyInlineValidationFailure(@NotNull Set<IAbstractPin> pinsWithMissingMate)
	{
		if (!CAFUtils.getInstance().hasWindowMgr()) {
			return;
		}
		String applicationTitle = AppInfo.getAppInfo().getApplicationTitle();
		IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();

		if (outputWindow == null || StringUtils.isBlank(applicationTitle) || pinsWithMissingMate.isEmpty()) {
			return;
		}

		for (IAbstractPin pin : pinsWithMissingMate) {
			StringBuilder sb = new StringBuilder();
			chs.cof.logical.cable.IPinList owner = pin.getOwner();
			if (owner != null) {
				sb.append(owner.getName()).append(":");
			}
			sb.append(pin.getName());

			String displayValue =
					ResourceMgr.getString(getClass(), "InlineValidationFailure.message.text", sb.toString(), Objects
							.requireNonNull(pin.getDesign()).getName());

			String quotedString = HTMLHelper.quoteChars(displayValue);

			if (quotedString != null) {
				outputWindow.sendMessage(HTMLHelper.color(IColor.RED, quotedString), applicationTitle, true);
			}
		}
		outputWindow.setActivePane(applicationTitle);
	}

	@NotNull @Override public IUnfreezeOutputTabHandler getUnfreezeOutputHandler(boolean isActive)
	{
		return new UnfreezeOutputTabHandler(isActive);
	}

	@Override
	public void refreshIndicators(@NotNull ISchemDiagram diagram, @NotNull Collection<IUID> multicoresToBeRefreshed,
			boolean cleanupIndicators)
	{
		IndicatorRefresher indicatorRefresher = IndicatorRefresher.getIndicatorRefresher(diagram);
		indicatorRefresher.refreshIndicators(multicoresToBeRefreshed, cleanupIndicators);
	}

	@NotNull public IBatchLoadAndSequentialParseLogicDesigns getBatchLoadLogicDesignsHelper(){
		return new BatchLoadAndSequentialParseLogicDesigns();
	}

	@NotNull @Override
	public IMergerFactory getMergerFactory()
	{
		return new MergerFactory();
	}

	@NotNull public IBackshellTransferService getBackshellTranserService(@NotNull ConnectionFlow connectionFlow)
	{
		return new BackshellTransferService(IBackshellPinOverlapResolver.getInstance(connectionFlow));
	}

	@NotNull @Override public IBackshellBuilder getBackshellBuilder()
	{
		return new BackshellBuilder();
	}
}
