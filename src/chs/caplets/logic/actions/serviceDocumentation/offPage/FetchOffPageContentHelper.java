package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.replication.IDataTransferReplicator;
import chs.caplets.logic.actions.serviceDocumentation.offPage.modular.FetchedModularConnectorsOwnerResolver;
import chs.caplets.logic.actions.serviceDocumentation.offPage.modular.ModularConnectorHierarchyBasedComparator;
import chs.caplets.logic.actions.shared.autoshare.FetchOffPagesContextMessageReporter;
import chs.caplets.logic.commands.BulkAutoShareCmd;
import chs.caplets.logic.commands.BulkAutoShareIntoCmd;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.ISupplementaryObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IPinFilter;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinOwner;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.symbol.ISymbolRef;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.publisher.offPage.IDesignContentToBeCopied;
import chs.publisher.offPage.IDiagramContentToBeCopied;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageCollectorAndReporter;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.ConnectorHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.ui.progress.IProgress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FetchOffPageContentHelper
{

	private OffPageDataTransfer m_dataTransfer;
	private final Set<ISharedObject> m_affectedSharedObjects = new HashSet<>();
	private ISymbolToParameterizedConverter converter;
	private ISymbolsToConvertProvider symbolsToConvertProvider;

	public FetchOffPageContentHelper()
	{
		this(IPinFilter.getDefaultFilter());
	}

	public FetchOffPageContentHelper(IPinFilter pinFilter)
	{
		this(pinFilter, ISymbolToParameterizedConverter.getDefaultConverter(),
				ISymbolsToConvertProvider.getDefaultProvider());
	}

	public FetchOffPageContentHelper(IPinFilter pinFilter, ISymbolToParameterizedConverter converter,
			ISymbolsToConvertProvider symbolsToConvertProvider)
	{
		m_dataTransfer = new OffPageDataTransfer(pinFilter);
		this.converter = converter;
		this.symbolsToConvertProvider = symbolsToConvertProvider;
	}

	public boolean fetch(@NotNull IDesignContentToBeCopied designContentToBeCopied,
			@NotNull List<IDiagramObject> noCopyNeeded,
			@Nullable IProgress progress,
			@NotNull IMessageCollectorAndReporter messageReporter, Set<IDesign> modifiedDesigns)
	{
		ISchemDiagram targetDiagram = CommonUtils.cast(CAFUtils.getInstance().getActiveDiagram(), ISchemDiagram.class);
		if (targetDiagram == null) {
			return false;
		}
		ILogicDesign targetDesign = targetDiagram.getDesign();
		assert targetDesign != null;
		ILogicDesign sourceDesign =
				CommonUtils.cast(designContentToBeCopied.getDesignId().getObject(), ILogicDesign.class);
		if (sourceDesign == null || !sourceDesign.isLoadedInMemory()) {
			return false;
		}
		if (progress != null) {
			String progressMsg = ResourceMgr.getString(FetchOffPageContentHelper.class,
					"FetchOffPageContentHelper.progress.fetchingContent.text", sourceDesign.getName());
			progress.increment(progressMsg);
		}
		SetMap<ISchemDiagram, IDiagramObject> diagramObjectsToBeFetched = new SetMap<>();
		for (IDiagramContentToBeCopied diagramContentToBeCopied : designContentToBeCopied
				.getDiagramContentToBeCopied()) {
			ISchemDiagram srcDiagram =
					CommonUtils.cast(diagramContentToBeCopied.getDiagramId().getObject(), ISchemDiagram.class);
			if (srcDiagram == null) {
				return false;
			}
			diagramObjectsToBeFetched.addAll(srcDiagram, diagramContentToBeCopied.getDiagramObjects());
		}
		if (!sourceDesign.equals(targetDesign)) {
			if (!doShare(diagramObjectsToBeFetched, sourceDesign, messageReporter)) {
				return false;
			}
		}
		final Collection<IUIDObject> processedFetchedContent = m_dataTransfer.getCopiedContent();
		Map<ILogicObject, ILogicObject> newVsOldConnectivity = new LinkedHashMap<>();
		if (!doTransfer(messageReporter, diagramObjectsToBeFetched, noCopyNeeded, newVsOldConnectivity)) {
			return false;
		}
		//get the symbols to convert before we do share into
		final List<IPinList> pinListsToConvert = getSymbolsToConvert();
		final List<IPinList> groundsCopied = m_dataTransfer.getCopiedContent()
				.stream()
				.filter(copiedDiagramObject -> copiedDiagramObject instanceof IPinList)
				.map(copiedDiagramObject -> (IPinList) copiedDiagramObject)
				.filter(pl -> pl.getConnectivity() instanceof IGroundDevice)
				.collect(Collectors.toList());
		if (!sourceDesign.equals(targetDesign)) {
			if (!doShareInto(newVsOldConnectivity, targetDiagram, messageReporter)) {
				return false;
			}
			else {
				if(!newVsOldConnectivity.isEmpty()){
					modifiedDesigns.add(sourceDesign);
				}
			}
		}
		//convert symbols to parameterized after share into is successful.
		doSymbolToParameterizedConversion(pinListsToConvert);
		groundsCopied
				.stream()
				.forEach(this::setSymbolRefOnNewGround);
		Collection<IUIDObject> fetchedContent = m_dataTransfer.getCopiedContent();
		fetchedContent.removeAll(processedFetchedContent);
		postFetch(fetchedContent);
		return true;
	}

	private void setSymbolRefOnNewGround(IPinList ground)
	{
		IDataTransferReplicator replicator = m_dataTransfer.getReplicator();
		assert replicator != null;
		final IPinList oldGround = replicator.getOldObject(ground, IPinList.class);
		if (oldGround != null) {
			final chs.cof.logical.cable.IPinList connectivity = oldGround.getConnectivity();
			final ISymbolRef symbolRef = connectivity.getSymbolRef();
			if (symbolRef != null) {
				ISymbolRef newSymbolRef =
						FactoryMgr.getSymbolFactory()
								.constructSymbolRefTimestamped(symbolRef.getSymbolUID(),
										symbolRef.getTimestamp());
				final chs.cof.logical.cable.IPinList newConn = ground.getConnectivity();
				newConn.setSymbolRef(newSymbolRef);
			}
		}
	}

	@NotNull private List<IPinList> getSymbolsToConvert()
	{
		IDataTransferReplicator replicator = m_dataTransfer.getReplicator();
		assert replicator != null;
		return symbolsToConvertProvider.getPinListsToConvert(m_dataTransfer.getCopiedContent(), replicator);
	}

	private void doSymbolToParameterizedConversion(List<IPinList> pinListsToConvert)
	{
		converter.convert(pinListsToConvert);
	}

	private boolean doShare(@NotNull SetMap<ISchemDiagram, IDiagramObject> candidateDiagramObjectsToBeShared,
			@NotNull ILogicDesign sourceDesign, @NotNull IMessageCollectorAndReporter messageReporter)
	{
		SetMap<ISchemDiagram, IDiagramObject> diagramObjectsToBeShared = new SetMap<>();
		for (ISchemDiagram diagram : candidateDiagramObjectsToBeShared.keySet()) {
			for (IDiagramObject diagramObject : candidateDiagramObjectsToBeShared.get(diagram)) {
				ILogicObject connectivityObj = ReferenceHelper.reduceToLogicObject(diagramObject);
				if (connectivityObj != null && !connectivityObj.isShared()) {
					diagramObjectsToBeShared.add(diagram, diagramObject);
				}
			}
		}
		if (diagramObjectsToBeShared.isEmpty()) {
			return true;
		}
		IMessageCollectorAndReporter contextMessageReporter =
				new FetchOffPagesContextMessageReporter(messageReporter);
		BulkAutoShareCmd bulkAutoShareCmd =
				getBulkAutoShareCmd(sourceDesign, diagramObjectsToBeShared, contextMessageReporter);
		boolean success = bulkAutoShareCmd.execute();
		contextMessageReporter.reportStoredMessages(severity -> success || PromptSeverity.ERROR.equals(severity));
		return success;
	}

	@NotNull BulkAutoShareCmd getBulkAutoShareCmd(@NotNull ILogicDesign sourceDesign,
			SetMap<ISchemDiagram, IDiagramObject> diagramObjectsToBeShared,
			IMessageCollectorAndReporter contextMessageReporter)
	{
		return new BulkAutoShareCmd(diagramObjectsToBeShared, sourceDesign, contextMessageReporter);
	}

	private boolean doTransfer(@NotNull IMessageReporterWithContext messageReporter,
			@NotNull SetMap<ISchemDiagram, IDiagramObject> diagramObjectsToBeFetched,
			List<IDiagramObject> noCopyNeededContent,
			@NotNull Map<ILogicObject, ILogicObject> newVsOldConnectivity)
	{
		for (ISchemDiagram srcDiagram : diagramObjectsToBeFetched.keySet()) {
			Set<IDiagramObject> diagramObjects = diagramObjectsToBeFetched.get(srcDiagram);
			diagramObjects.removeAll(noCopyNeededContent);
			if (!m_dataTransfer.transfer(diagramObjects, srcDiagram, messageReporter)) {
				LogHelper.debugMsgSafe(getDataTransferDebugMessage(srcDiagram));
				return false;
			}
			newVsOldConnectivity.putAll(m_dataTransfer.getNewVsOldConnectivity());
		}
		return true;
	}

	@NotNull private String getDataTransferDebugMessage(@NotNull ISchemDiagram srcDiagram)
	{
		return "Failed to copy paste content from diagram " + srcDiagram.getName() + " , UID: " +
				srcDiagram.getUID().getString();
	}

	private boolean doShareInto(@NotNull Map<ILogicObject, ILogicObject> newVsOldConnectivity,
			@NotNull ISchemDiagram targetDiagram, @NotNull IMessageCollectorAndReporter messageReporter)
	{
		Map<IUIDObject, ISharedObject> objectsToBeSharedInto = new HashMap<>();
		for (ILogicObject newConnectivity : newVsOldConnectivity.keySet()) {
			ILogicObject oldConnectivity = newVsOldConnectivity.get(newConnectivity);
			if (oldConnectivity.getSharedObject() != null) {
				objectsToBeSharedInto.put(newConnectivity, oldConnectivity.getSharedObject());
			}
		}
		if (!objectsToBeSharedInto.isEmpty()) {
			final Map<Object, Object> contextMap =
					Collections.unmodifiableMap(new HashMap<>(newVsOldConnectivity));
			IMessageCollectorAndReporter reporter = getReporter(messageReporter, contextMap);
			ILogicDesign targetDesign = Objects.requireNonNull(targetDiagram.getDesign());
			FetchedModularConnectorsOwnerResolver fetchedModularConnectorsOwnerResolver =
					new FetchedModularConnectorsOwnerResolver(targetDesign, reporter);
			if (!fetchedModularConnectorsOwnerResolver.resolve(objectsToBeSharedInto)) {
				return false;
			}
			BulkAutoShareIntoCmd bulkAutoShareIntoCmd =
					getBulkAutoShareIntoCmd(targetDiagram, objectsToBeSharedInto, reporter);
			if (!bulkAutoShareIntoCmd.execute()) {
				reporter.reportStoredMessages(PromptSeverity.ERROR::equals);
				return false;
			}
			reporter.reportStoredMessages(severity -> true);
			m_affectedSharedObjects.addAll(objectsToBeSharedInto.values());
		}
		return true;
	}

	@NotNull
	public static IMessageCollectorAndReporter getReporter(@NotNull IMessageCollectorAndReporter messageReporter,
			Map<Object, Object> contextMap)
	{
		return new FetchOffPagesContextMessageReporter(messageReporter)
		{
			@Override
			public void setCurrentContextObject(@Nullable IMessageContext currentContextObject)
			{
				IMessageContext newContext = currentContextObject == null ? null :
						IMessageContext.createContext(currentContextObject.getObjectsInContext()
								.stream()
								.map(obj -> contextMap.getOrDefault(obj, obj))
								.toArray());
				super.setCurrentContextObject(newContext);
			}
		};
	}

	@NotNull BulkAutoShareIntoCmd getBulkAutoShareIntoCmd(@NotNull ISchemDiagram targetDiagram,
			Map<IUIDObject, ISharedObject> objectsToBeSharedInto, IMessageCollectorAndReporter reporter)
	{
		ILogicDesign design = targetDiagram.getDesign();
		assert design != null;
		Comparator<IUIDObject> comparator = new ModularConnectorHierarchyBasedComparator(objectsToBeSharedInto);
		return new BulkAutoShareIntoCmd(objectsToBeSharedInto, design, targetDiagram, reporter, comparator);
	}

	private void postFetch(Collection<IUIDObject> fetchedContent)
	{
		for (IUIDObject obj : fetchedContent) {
			if (obj instanceof ISupplementaryObject) {
				setFetched((ISupplementaryObject) obj);
			}

			if (obj instanceof IPinList) {
				IPinList schemCandidate = (IPinList) obj;
				IConnector connector = CommonUtils.cast(schemCandidate.getConnectivity(), IConnector.class);
				if (connector != null) {
					int cableDepth = ConnectorHelper.getCableModularDepth(connector);
					int schemDepth = ConnectorHelper.getSchematicModularDepth(schemCandidate);
					if (cableDepth > schemDepth) {
						ConnectorHelper.ensureModularSchematics(schemCandidate, schemCandidate.getDiagram());
					}
					IPinList parent = ConnectorHelper.getParentSchemPinList(schemCandidate);
					while (parent != null) {
						setFetched(parent);
						parent = ConnectorHelper.getParentSchemPinList(parent);
					}
				}
			}
		}
	}

	private void setFetched(@NotNull ISupplementaryObject obj)
	{
		obj.markAsSupplementary();
		if (obj instanceof IPinOwner) {
			((IPinOwner) obj).getPins().forEach(ISupplementaryObject::markAsSupplementary);
			((IPinOwner) obj).getStackPins().forEach(ISupplementaryObject::markAsSupplementary);
		}
	}

	Set<IUIDObject> getFetchedObjects(@NotNull IObjectFilter<IUIDObject> filter)
	{
		return m_dataTransfer.getCopiedContent().stream().filter(filter::accept).collect(Collectors.toSet());
	}

	Set<ISharedObject> getAffectedSharedObjects(@NotNull IObjectFilter<ISharedObject> filter)
	{
		return m_affectedSharedObjects.stream().filter(filter::accept).collect(Collectors.toSet());
	}

	public static boolean isCenterStrippedConductor(IConductor conductor)
	{
		chs.cof.logical.cable.IConductor connectivity = conductor.getConnectivity();
		Set<ISplice> centreStripSplices = Collections.emptySet();
		if (connectivity instanceof IWireConductor) {
			IWireConductor iWireConductor = (IWireConductor) connectivity;
			centreStripSplices = iWireConductor.getCenterStripSplicesAsSet();
		}
		if (centreStripSplices.isEmpty()) {
			return false;
		}
		IUIDObjectCollection<IPin> pins = conductor.getPins();
		for (IPin pin : pins) {
			chs.cof.logical.cable.IPinList owner = pin.getConnectivity().getOwner();
			if (owner instanceof ISplice) {
				ISplice spliceOwner = (ISplice) owner;
				return centreStripSplices.contains(spliceOwner);
			}
		}
		return false;
	}
}