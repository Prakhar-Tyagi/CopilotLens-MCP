package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.cmd.ProjectTraverserCmd;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.IUserSessionRemotePackage.SingleUsageSharedObjectInfo;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.shared.BaseShareActionOperands;
import chs.caplets.logic.actions.shared.ConnectorUnshareHelper;
import chs.caplets.logic.actions.shared.DeviceUnshareHelper;
import chs.caplets.logic.actions.shared.GenericPinListUnshareHelper;
import chs.caplets.logic.actions.shared.InlineConnectorUnshareHelper;
import chs.caplets.logic.actions.shared.ModularConnectorUnshareHelper;
import chs.caplets.logic.actions.shared.UnshareAction;
import chs.caplets.logic.actions.shared.UnshareConductorActionHelper;
import chs.caplets.logic.actions.shared.UnshareConductorGroupActionHelper;
import chs.caplets.logic.actions.shared.UnsharePinListActionHelper;
import chs.caplets.logic.actions.shared.batchshare.BatchShareFeedbackTableColumnEnum;
import chs.caplets.logic.actions.shared.batchshare.BatchShareReporter;
import chs.caplets.logic.actions.shared.batchshare.BatchShareStatusWindowAssistant;
import chs.caplets.logic.actions.shared.batchshare.IBatchShareStatusMessage;
import chs.caplets.logic.actions.shared.batchshare.ShareableEntityTypeEnum;
import chs.caplets.logic.actions.shared.batchshare.unshare.BatchUnshareDialog;
import chs.caplets.logic.actions.shared.batchshare.unshare.BatchUnshareParams;
import chs.caplets.logic.actions.shared.batchshare.unshare.BatchUnshareRow;
import chs.caplets.logic.actions.shared.batchshare.unshare.IBatchUnshareParams;
import chs.caplets.logic.actions.shared.batchshare.unshare.IBatchUnshareRow;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.logical.IDesignScopeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IUnshareRedundantSharedObjectsCmd;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IBaseShareableDiagramObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CommandHelper;
import chs.common.IDesignMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.validation.ValidationException;
import chs.common.validation.ValidationHelper;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.task.InterruptableTaskHelper;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.Progress;
import chs.utility.ui.progress.ProgressTaskClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UnshareRedundantSharedObjectsCmd extends ProjectTraverserCmd implements IUnshareRedundantSharedObjectsCmd
{

	public static final int MSGBUFFER_SIZE = 20;
	private SelectSet m_selectionSet;
	private static List<String> m_messagePile = new ArrayList<>();
	private IDesignScopeResolver m_scopeResolver;
	protected Set<IBatchUnshareRow> m_selectedRows;
	private static final String CONN_KEY = "connectors";
	private static final String SPLICE_KEY = "splices";
	private static final String DEV_KEY = "devices";
	private static final String WIRE_KEY = "wires";
	private static final String NET_KEY = "nets";
	private static final String MULTICORE_KEY = "multicores";

	// Message collection infrastructure (reusing Batch Share infrastructure)
	@NotNull protected Collection<IBatchShareStatusMessage> m_feedbackMessages;
	@NotNull protected IMessageReporterWithContext m_reporter;

	private static void logMessage(String message)
	{
		m_messagePile.add(message);
		if (m_messagePile.size() > MSGBUFFER_SIZE) {
			flush();
		}
	}

	private static void flush()
	{
		for (String s : m_messagePile) {
			//System.out.println(s);
		}
		m_messagePile.clear();
	}

	public UnshareRedundantSharedObjectsCmd(CommandHelper commandHelper, IProject proj)
	{
		super(commandHelper, proj, Arrays.asList(ILogicDesign.class), null);
		m_feedbackMessages = new ArrayList<>();
		m_reporter = new BatchShareReporter(m_feedbackMessages::add);
	}

	public UnshareRedundantSharedObjectsCmd(IProject proj, IDesignScopeResolver scopeResolver)
	{
		this(new CAFCommandHelper(), proj);
		m_scopeResolver = scopeResolver;
	}

	public boolean collectAndShowUnshareDialog(@Nullable Frame parentFrame)
	{
		IUserSession userSession = UtilsHelper.getCHSSystem().getUserSession();
		if (userSession == null) {
			return false;
		}

		// Collect unshare candidate rows
		// Execute scanning unsharing with progress dialog
		int range = 2;
		String projectName = getProject().getName();
		String title = ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
				"UnshareRedundantSharedObjectsCmd.Progress.scanning.title");

		String header = ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
				"UnshareRedundantSharedObjectsCmd.Progress.scanning.header", projectName);
		IProgress unshareProgress = new Progress(range, "Scanning shared objects");
		Frame owner = CAFUtils.getInstance().getDialogFrame();
		List<Collection<IBatchUnshareRow>> rowsHolder = new ArrayList<>();
		Runnable unshareTask = () -> rowsHolder.add(collectDialogData(userSession, unshareProgress));
		executeTaskWithProgressDialog(owner, unshareTask, unshareProgress, false, title, header);

		Collection<IBatchUnshareRow> rows = rowsHolder.isEmpty() ? null : rowsHolder.get(0);

		if (rows == null || rows.isEmpty()) {
			return false;
		}

		IBatchUnshareParams params = new BatchUnshareParams(rows);

		BatchUnshareDialog dialog = new BatchUnshareDialog(parentFrame,
				ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
						"UnshareRedundantSharedObjectsCmd.Dialog.title", getProject().getName()), params);

		if (Environment.isImmersedMode()) {
			// In immersed mode the dialog is modeless so diagram view windows are not blocked.
			// Use a window listener to process selections after the dialog is closed.
			dialog.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosed(java.awt.event.WindowEvent e) {
					if (!dialog.isCancelled()) {
						m_selectedRows = params.retrieveRowsFromUserSelection();
						if (!m_selectedRows.isEmpty()) {
							executePostDialogWork();
						}
					}
				}
			});
			dialog.showDialog(true);
			return false;
		}

		if (dialog.showDialog(true)) {
			// Store user selections
			m_selectedRows = params.retrieveRowsFromUserSelection();
			return !m_selectedRows.isEmpty();
		}

		return false;
	}

	@NotNull protected Collection<IBatchUnshareRow> collectDialogData(IUserSession userSession, IProgress progress)
	{
		Map<String, UnsharedObjectsInfoHandler> candidates = new HashMap<>();

		collectUnShareObjects(getProject(), userSession, candidates, progress);

		if (candidates.isEmpty()) {
			if (!progress.isCancelled()) {
				progress.increment();
				progress.increment(ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
						"UnshareRedundantSharedObjectsCmd.Progress.processedPercentage", 100));
			}
			return Collections.emptyList();
		}

		if (progress.isCancelled()) {
			return Collections.emptyList();
		}
		//processed 50% of the work
		final int HALF_PROGRESS_PERCENTAGE = 50;
		progress.increment(ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
				"UnshareRedundantSharedObjectsCmd.Progress.processedPercentage", HALF_PROGRESS_PERCENTAGE));

		// Load Shared Pinlists
		getProject().getSharedPinListMgr().getNumSharedPinLists();

		Collection<IUID> designsUIDInScope = m_scopeResolver.getDesignsUIDInScope(getProject());
		Set<String> accessibleDesignUIDs =
				designsUIDInScope.stream().map(IUID::toString).collect(
						Collectors.toSet());

		List<IBatchUnshareRow> rows = new ArrayList<>();
		for (Map.Entry<String, UnsharedObjectsInfoHandler> entry : candidates.entrySet()) {
			if (progress.isCancelled()) {
				return Collections.emptyList();
			}

			List<IBatchUnshareRow> rowsByType = createRowsForObjectType(entry, accessibleDesignUIDs);
			rows.addAll(rowsByType);
		}

		if (progress.isCancelled()) {
			return Collections.emptyList();
		}

		progress.increment(ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
				"UnshareRedundantSharedObjectsCmd.Progress.processedPercentage", 100));

		return rows;
	}

	@NotNull private List<IBatchUnshareRow> createRowsForObjectType(
			@NotNull Map.Entry<String, UnsharedObjectsInfoHandler> entry,
			@NotNull Set<String> accessibleDesignUIDs)
	{
		String objectTypeKey = entry.getKey();
		UnsharedObjectsInfoHandler handler = entry.getValue();

		return handler.getDesignIds().stream()
				.filter(accessibleDesignUIDs::contains)
				.flatMap(designUID -> handler.getObjectMappings(designUID).stream()
						.map(mapping -> createUnshareRow(mapping, objectTypeKey, designUID)))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@Nullable
	private IBatchUnshareRow createUnshareRow(@NotNull SingleUsageSharedObjectInfo.ObjectToSharedMapping mapping,
			@NotNull String objectTypeKey, @NotNull String designStrUID)
	{
		IUID sharedUID = FactoryMgr.getCommonFactory().constructUID(mapping.getSharedObjectUID());
		ISharedObject sharedObject = CommonUtils.cast(sharedUID.getObject(), ISharedObject.class);

		if (sharedObject == null) {
			return null;
		}

		ShareableEntityTypeEnum entityType = determineEntityType(sharedObject, objectTypeKey);

		if (shouldSkipInlinePlug(sharedObject, entityType)) {
			return null;
		}
		IUID designUID = FactoryMgr.getCommonFactory().constructUID(designStrUID);
		return new BatchUnshareRow(sharedObject, mapping.getObjectUID(), designUID, entityType);
	}

	private boolean shouldSkipInlinePlug(@NotNull ISharedObject sharedObject,
			@NotNull ShareableEntityTypeEnum entityType)
	{
		return sharedObject instanceof ISharedConnector sharedConnector &&
				entityType == ShareableEntityTypeEnum.INLINE && sharedConnector.getType().isPlug();
	}

	@Override protected boolean doProcessObjects(@Nullable IBaseDiagram diagram,
			@NotNull Set<IUID> objectUIDsToProcess, int numObjsPerProgressInc)
	{
		return false;
	}

	private BaseShareActionOperands setupOperands(ILogicObject logicObject)
	{
		BaseShareActionOperands operands = new BaseShareActionOperands();
		operands.setLogicObject(logicObject);
		return operands;
	}

	@NotNull
	protected UnsharePinListActionHelper getPinListUnshareHelper(
			@SuppressWarnings("unused") final ISchemDiagram diagram,
			final ILogicDesign design, final Iterable<IPinList> schemInstances, ILogicObject conn)
	{
		return new UnsharePinListActionHelper(design)
		{
			@Override
			@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, String dialogTitle,
					@Nullable ISchemDiagram diagram)
			{
				operands.setLogicObject(conn);
				super.setup(operands, dialogTitle, diagram);

				List<IPinList> pinLists = new ArrayList<>();

				for (IPinList dev : schemInstances) {
					pinLists.add(dev);
				}
				getUnshareHelper().setSchemPinListsForUT(pinLists);

				return IActionEnum.eCompleted;
			}

			public GenericPinListUnshareHelper createUnshareHelper(@Nullable
			chs.cof.logical.cable.IPinList cablePinList, ISchemDiagram diagram)
			{
				if (cablePinList instanceof IConnector) {
					if (super.isInlineConnector(cablePinList)) {
						return new InlineConnectorUnshareHelper(design, diagram);
					}
					else if (super.isModularConnector((IConnector) cablePinList)) {
						return new ModularConnectorUnshareHelper(design, diagram);
					}
					else {
						return new ConnectorUnshareHelper(design, diagram);
					}
				}
				else {
					return new DeviceUnshareHelper(design, diagram)
					{
						protected void showError(Frame dialogFrame, String heading, String string1)
						{

						}

						protected void showWarning(Frame dialogFrame, String heading, String string1)
						{

						}
					};
				}
			}
		};
	}

	@NotNull
	protected UnshareConductorGroupActionHelper getMultiCoreUnshareHelper(final ILogicDesign design,
			ILogicObject logicObject)
	{
		return new UnshareConductorGroupActionHelper(design)
		{
			@Override
			@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
					@Nullable ISchemDiagram diagram)
			{
				operands.target = logicObject;
				return super.setup(operands, dialogTitle, diagram);
			}
		};
	}

	@Override public boolean doExecute()
	{

		if (!collectAndShowUnshareDialog(CAFUtils.getInstance().getDialogFrame())) {
			// Skip if no groups were selected in the dialog
			return false;
		}

		return executePostDialogWork();
	}

	/**
	 * Executes the unshare work after the user has confirmed selections in the dialog.
	 * Called synchronously in non-immersed mode, or from a callback in immersed mode.
	 */
	private boolean executePostDialogWork()
	{
		try {
			IProject iProject = getProject();
			long startTime = System.currentTimeMillis();
			m_selectionSet = new SelectSet();
			IUserSession userSession = UtilsHelper.getCHSSystem().getUserSession();
			if (userSession == null) {
				return false;
			}

			// Process user-selected groups
			processSelectedGroups(iProject);

			logMessage("Total time taken = " + (System.currentTimeMillis() - startTime) + "ms");
			flush();
		}
		finally {
			// Display table with feedback messages
			if (!m_feedbackMessages.isEmpty()) {
				displayUnshareStatusTab(m_feedbackMessages);
			}
		}
		return true;
	}

	private void processSelectedGroups(IProject iProject)
	{
		Map<String, Set<IBatchUnshareRow>> rowsByType = new HashMap<>();

		for (IBatchUnshareRow row : m_selectedRows) {
			String objectType = mapShareableEntityTypeToKey(row.getObjectType());
			rowsByType.computeIfAbsent(objectType, k -> new HashSet<>()).add(row);
		}

		Map<String, UnsharedObjectsInfoHandler> candidates = new HashMap<>();

		for (Map.Entry<String, Set<IBatchUnshareRow>> entry : rowsByType.entrySet()) {
			String objectTypeKey = entry.getKey();
			Set<IBatchUnshareRow> typeRows = entry.getValue();

			logMessage("Unsharing " + objectTypeKey + "s..................");

			UnsharedObjectsInfoHandler handler = new UnsharedObjectsInfoHandler();
			handler.populateFromSelectedObjects(typeRows);
			candidates.put(objectTypeKey, handler);
		}

		// Execute unsharing with progress dialog
		String projectName = getProject().getName();
		String title = ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
				"UnshareRedundantSharedObjectsCmd.Progress.title");
		String header = ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
				"UnshareRedundantSharedObjectsCmd.Progress.header", projectName);
		IProgress unshareProgress = new Progress(m_selectedRows.size(), "Unshare Redundant Shared Objects");
		Frame owner = CAFUtils.getInstance().getDialogFrame();
		Runnable unshareTask = () -> doUnshareObjects(iProject, candidates, unshareProgress);
		executeTaskWithProgressDialog(owner, unshareTask, unshareProgress, true, title, header);
	}

	private void collectUnShareObjects(IProject iProject, IUserSession userSession,
			Map<String, UnsharedObjectsInfoHandler> candidates, IProgress progress)
	{
		try {
			SingleUsageSharedObjectInfo.SharedObjectType[] objectTypes =
					new SingleUsageSharedObjectInfo.SharedObjectType[]{
							SingleUsageSharedObjectInfo.SharedObjectType.DEVICE,
							SingleUsageSharedObjectInfo.SharedObjectType.CONNECTOR,
							SingleUsageSharedObjectInfo.SharedObjectType.SPLICE,
							SingleUsageSharedObjectInfo.SharedObjectType.WIRE,
							SingleUsageSharedObjectInfo.SharedObjectType.NET,
							SingleUsageSharedObjectInfo.SharedObjectType.MULTICORE};

			SingleUsageSharedObjectInfo[] singleUsageSharedObjects =
					m_scopeResolver.getSingleUsageProvider().getSingleUsages(objectTypes, userSession, iProject);

			if (progress.isCancelled()) {
				return;
			}

			for (SingleUsageSharedObjectInfo singleUsageSharedObject : singleUsageSharedObjects) {
				if (progress.isCancelled()) {
					candidates.clear();
					return;
				}
				candidates.put(singleUsageSharedObject.getObjectType(),
						new UnsharedObjectsInfoHandler(singleUsageSharedObject.getDesignToObjectMap()));
			}
		}
		catch (UserSessionException ignored) {

		}
	}

	public void CommitChanges(IDesign idesign)
	{
		idesign.flush();
	}

	private static class UnsharedObjectsInfoHandler
	{

		private Map<String, List<SingleUsageSharedObjectInfo.ObjectToSharedMapping>> designConnMap =
				new HashMap<String, List<SingleUsageSharedObjectInfo.ObjectToSharedMapping>>();
		private Map<String, Integer> designToObjectCountMap = new HashMap<String, Integer>();

		private UnsharedObjectsInfoHandler()
		{

		}

		private UnsharedObjectsInfoHandler(
				Map<String, List<SingleUsageSharedObjectInfo.ObjectToSharedMapping>> designConnMap)
		{
			this.designConnMap = designConnMap;
			// Pre-calculate counts for each design
			for (Map.Entry<String, List<SingleUsageSharedObjectInfo.ObjectToSharedMapping>> entry : designConnMap.entrySet()) {
				designToObjectCountMap.put(entry.getKey(), entry.getValue().size());
			}
		}

		@NotNull public Collection<String> getDesignIds()
		{
			return new HashSet<>(designConnMap.keySet());
		}

		public int getCount(String designId)
		{
			return designToObjectCountMap.getOrDefault(designId, 0);
		}

		public List<String> getCandidates(String designId)
		{
			return designConnMap.getOrDefault(designId, Collections.emptyList()).stream()
					.map(SingleUsageSharedObjectInfo.ObjectToSharedMapping::getObjectUID)
					.collect(Collectors.toList());
		}

		@NotNull public List<SingleUsageSharedObjectInfo.ObjectToSharedMapping> getObjectMappings(String designId)
		{
			return designConnMap.getOrDefault(designId, Collections.emptyList());
		}

		public void populateFromSelectedObjects(Set<IBatchUnshareRow> selectedRows)
		{
			for (IBatchUnshareRow row : selectedRows) {
				String designId = row.getDesignUID().toString();
				String objectUIDStr = row.getObjectUID().toString();
				String sharedObjectUIDStr = row.getSharedObjectUID().toString();

				SingleUsageSharedObjectInfo.ObjectToSharedMapping mapping =
						new SingleUsageSharedObjectInfo.ObjectToSharedMapping(objectUIDStr, sharedObjectUIDStr);
				designConnMap.computeIfAbsent(designId, id -> new ArrayList<>()).add(mapping);
				designToObjectCountMap.put(designId, designConnMap.get(designId).size());
			}
		}
	}

	private void doUnshareObjects(IProject project, Map<String, UnsharedObjectsInfoHandler> candidates,
			IProgress progress)
	{
		Set<ILogicDesign> logicDesigns = getApplicableLogicDesigns(project, candidates);
		getCommandListener().startProcessing(logicDesigns.size());

		int objectsProcessed = 0;
		int processedDesigns = 0;
		int designCount = 0;
		boolean wasCancelled = false;

		for (ILogicDesign logicDesign : logicDesigns) {
			++designCount;
			logMessage("Processing Design Started: " + logicDesign.getFullName() + " -> " + designCount);
			String designId = logicDesign.getUID().toString();
			loadDesignToMemory(logicDesign);
			int objectsInThisDesign = getObjectCountInDesign(designId, candidates);

			boolean isDesignAlreadyLocked = logicDesign.isLocked();
			if (isDesignAlreadyLocked) {
				m_reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
						"UnshareRedundantSharedObjectsCmd.lockedDesign.msg", logicDesign.getFullName()));
				objectsProcessed += objectsInThisDesign;
				getCommandListener().incrementProcessing();
				continue;
			}
			if (!logicDesign.lock()) {
				m_reporter.report(PromptSeverity.ERROR,
						ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
								"UnshareRedundantSharedObjectsCmd.NonEditableDesign.msg",
								logicDesign.getFullName()));
				objectsProcessed += objectsInThisDesign;
				getCommandListener().incrementProcessing();
				continue;
			}

			try {
				processedDesigns++;

			for (Map.Entry<String, UnsharedObjectsInfoHandler> entry : candidates.entrySet()) {
				if (progress.isCancelled()) {
					wasCancelled = true;
					break;
				}
				String objectType = entry.getKey();
				List<String> devices = entry.getValue().getCandidates(designId);
				if (NET_KEY.equalsIgnoreCase(objectType) || WIRE_KEY.equalsIgnoreCase(objectType)) {
					int numConductorsProcessed =
							unShareConductorsUsedInSingleDesign(logicDesign, devices, objectType, progress,
									objectsProcessed);
					objectsProcessed += numConductorsProcessed;
					logMessage("Shared " + objectType + " processed:" + numConductorsProcessed);
				}
				else if (MULTICORE_KEY.equalsIgnoreCase(objectType)) {
					int numMultiCoresProcessed =
							unShareMultiCoreUsedInSingleDesign(logicDesign, devices, objectType, progress,
									objectsProcessed);
					objectsProcessed += numMultiCoresProcessed;
					logMessage("Shared " + objectType + " processed:" + numMultiCoresProcessed);
				}
				else {
					int numDevProcessed =
							unSharePinListsUsedInSingleDesign(logicDesign, devices, objectType, progress,
									objectsProcessed);
					objectsProcessed += numDevProcessed;
					logMessage("Shared " + objectType + " processed:" + numDevProcessed);
				}
			}
		}
		catch (RuntimeException e) {
			String errorMsg = String.format("Unexpected error processing design '%s': %s",
					logicDesign.getFullName(),
					e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
			m_reporter.report(PromptSeverity.ERROR, errorMsg);
			logMessage("ERROR: " + errorMsg);
		}
		finally {
			if (!validateAndSaveDesign(logicDesign)) {
				String validationMsg = String.format("Validation or save failed for design '%s'. Changes were not persisted.",
						logicDesign.getFullName());
				m_reporter.report(PromptSeverity.ERROR, validationMsg);
			}
			logicDesign.unlock();
			unloadDesignFromMemory(logicDesign);
			m_selectionSet.clear();
		}
		logMessage("Processing Design Ended: " + logicDesign.getFullName());
		getCommandListener().incrementProcessing();

			if (progress.isCancelled()) {
				wasCancelled = true;
				break;
			}
		}
		logMessage("Total Number of processed Designs: " + processedDesigns);

		// Report cancelled objects if user cancelled and there are remaining objects
		int remainingObjects = m_selectedRows.size() - objectsProcessed;
		if (wasCancelled && remainingObjects > 0) {
			m_reporter.report(PromptSeverity.WARNING, ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
					"UnshareRedundantSharedObjectsCmd.CancelledUnshare.msg", remainingObjects));
		}
	}

	@NotNull
	private Set<ILogicDesign> getApplicableLogicDesigns(@NotNull IProject project,
			@NotNull Map<String, UnsharedObjectsInfoHandler> candidates)
	{
		IDesignMgr designMgr = project.getDesignMgr();
		Set<String> candidateDesignIds =
				candidates.values().stream().flatMap(info -> info.getDesignIds().stream()).collect(Collectors.toSet());

		return candidateDesignIds.stream()
				.map(uidStr -> FactoryMgr.getCommonFactory().constructUID(uidStr))
				.map(designMgr::getLogicalDesign)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	/**
	 * Calculate total number of objects in a design across all object types
	 */
	private int getObjectCountInDesign(@NotNull String designId,
			@NotNull Map<String, UnsharedObjectsInfoHandler> candidates)
	{
		return candidates.values().stream()
				.mapToInt(handler -> handler.getCount(designId))
				.sum();
	}

	private int unShareConductorsUsedInSingleDesign(ILogicDesign design, List<String> devices, String objectType,
			IProgress progress,
			int currentProgress)
	{
		int objectsProcessed = 0;
		for (String dUID : devices) {
			if (progress.isCancelled()) {
				break;
			}

			// unshare each of these connectivity objects

			ILogicObject logicObject = selectObjects(List.of(dUID));
			if (!isValidForUnshare(design, objectType, logicObject)) {
				objectsProcessed++;
				updateProgress(progress, currentProgress, objectsProcessed);
				continue;
			}
			IDesignWideUsageMgr designWideUsageMgr = design.getDesignWideUsageMgr();
			List<IDesignSharedUsage> usages = designWideUsageMgr.getUsages(logicObject);

			List<IConductor> schemInstances = new ArrayList<IConductor>();
			ISchemDiagram mDiagram = null;
			for (IDesignSharedUsage usage : usages) {
				ISchemDiagram diagram = design.getDiagram(usage.getDiagramUID());
				assert diagram != null;
				// Load the diagrams based on the usage only, avoiding unnecessary loading of all diagrams
				diagram.loadToMemory();
				mDiagram = diagram;
				IBaseShareableDiagramObject schemCond = usage.getDiagramObject();
				if(schemCond instanceof IConductor iConductor) {
					schemInstances.add(iConductor);
				}
			}
			UnshareConductorActionHelper scach = getUnshareConductorActionHelper(design);
			BaseShareActionOperands operands = UnshareAction.getUnshareOperands(m_selectionSet);
			if (mDiagram == null) {
				// unplaced objects
				mDiagram = design.getDiagrams().getNext();
			}
			assert operands != null;
			IActionEnum setup = scach.setup(operands, "", mDiagram);
			scach.setSchemConductorsForUT(schemInstances);
			objectsProcessed++;
			updateProgress(progress, currentProgress, objectsProcessed);
			if (IActionEnum.eCompleted.equals(setup)) {
				if (scach.doEdit()) {
					m_reporter.report(PromptSeverity.INFORMATION,
							ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
									"UnshareRedundantSharedObjectsCmd.Success.msg", design.getFullName()),
							IMessageContext.createContext(logicObject));
				}
				else {
					m_reporter.report(PromptSeverity.ERROR,
							ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
									"UnshareRedundantSharedObjectsCmd.EditFailure.msg",
									COFTypeEnum.getDisplayableTypeName(logicObject)),
							IMessageContext.createContext(logicObject));
				}
			}
			else {
				m_reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
								"UnshareRedundantSharedObjectsCmd.UnshareFailure.msg", design.getFullName()),
						IMessageContext.createContext(logicObject));
			}
		}
		return objectsProcessed;
	}

	private int unShareMultiCoreUsedInSingleDesign(ILogicDesign design, List<String> multicores, String objectType,
			IProgress progress,
			int currentProgress)
	{
		//ensure all the diagrams are skeletally loaded
		design.getDiagrams();

		int objectsProcessed = 0;
		for (String multicoreUID : multicores) {
			if (progress.isCancelled()) {
				break;
			}

			IMulticore multicoreObject = UIDMgr.getObjectOfType(
					FactoryMgr.getCommonFactory().constructUID(multicoreUID), IMulticore.class);

			if (!isValidForUnshare(design, objectType, multicoreObject)) {
				objectsProcessed++;
				updateProgress(progress, currentProgress, objectsProcessed);
				continue;
			}
			assert multicoreObject != null;

			Set<IUID> uids = new HashSet<IUID>();
			design.getDesignWideUsageMgr().getMulticoreDiagrams(multicoreObject, uids);

			for (IUID uid : uids)
			{
				ISchemDiagram diagram = UIDMgr.getObjectOfType(uid, ISchemDiagram.class);
				if(diagram != null) {
					// Load the diagrams based on the usage only, avoiding unnecessary loading of all diagrams
					diagram.loadToMemory();
				}
			}

			UnshareConductorGroupActionHelper unsharehelper = getMultiCoreUnshareHelper(design, multicoreObject);
			BaseShareActionOperands operands = setupOperands(multicoreObject);
			IActionEnum setup = unsharehelper.setup(operands, "", null);
			objectsProcessed++;
			updateProgress(progress, currentProgress, objectsProcessed);
			if (IActionEnum.eCompleted.equals(setup)) {
				if (unsharehelper.doEdit()) {
					m_reporter.report(PromptSeverity.INFORMATION,
							ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
									"UnshareRedundantSharedObjectsCmd.Success.msg", design.getFullName()),
							IMessageContext.createContext(multicoreObject));
				}
				else {
					m_reporter.report(PromptSeverity.ERROR,
							ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
									"UnshareRedundantSharedObjectsCmd.EditFailure.msg",
									COFTypeEnum.getDisplayableTypeName(multicoreObject)),
							IMessageContext.createContext(multicoreObject));
				}
			}
			else {
				m_reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
								"UnshareRedundantSharedObjectsCmd.UnshareFailure.msg", design.getFullName()),
						IMessageContext.createContext(multicoreObject));
			}
		}
		return objectsProcessed;
	}

	private boolean isValidForUnshare(@NotNull ILogicDesign design, @NotNull String objectType,
			@Nullable ILogicObject logicObject)
	{
		if (logicObject == null) {
			m_reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
					"UnshareRedundantSharedObjectsCmd.ObjectIsNotFound.msg", objectType, design.getFullName()));

			return false;
		}
		ISharedObject sharedObject = logicObject.getSharedObject();
		if (sharedObject == null) {
			m_reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
							"UnshareRedundantSharedObjectsCmd.ObjectIsNotShared.msg",
							COFTypeEnum.getDisplayableTypeName(logicObject), logicObject.getName()),
					IMessageContext.createContext(logicObject));
			return false;
		}
		return true;
	}

	protected UnshareConductorActionHelper getUnshareConductorActionHelper(IDesign design)
	{
		return new UnshareConductorActionHelper(design)
		{
			protected boolean shouldIncludeAllInstances()
			{
				return true;
			}

			protected void showErrorPrompt(String heading, String error)
			{
				// do nothing
			}
		};
	}

	@Nullable private ILogicObject selectObjects(List<String> uidStrs)
	{
		m_selectionSet.clear();
		ILogicObject logicObject = null;
		for (String uidStr : uidStrs) {
			IUIDObject obj1 = UIDMgr.getObject(FactoryMgr.getCommonFactory().constructUID(uidStr));
			logicObject = ReferenceHelper.reduceToLogicObject(obj1);
			// different logic objects possible - should test not enabled
			if (obj1 != null) {
				m_selectionSet.add(new Selection(obj1));
			}
		}
		return logicObject;
	}

	private int unSharePinListsUsedInSingleDesign(ILogicDesign design, List<String> devices, String objectType,
			IProgress progress,
			int currentProgress)
	{
		int objectsProcessed = 0;
		for (String dUID : devices) {
			if (progress.isCancelled()) {
				break;
			}

			// unshare each of these connectivity objects
			ILogicObject logicObject = (ILogicObject) UIDMgr.getObject(
					FactoryMgr.getCommonFactory().constructUID(dUID));

			if (!isValidForUnshare(design, objectType, logicObject)) {
				objectsProcessed++;
				updateProgress(progress, currentProgress, objectsProcessed);
				continue;
			}
			IDesignWideUsageMgr designWideUsageMgr = design.getDesignWideUsageMgr();
			assert logicObject != null;
			List<IDesignSharedUsage> usages = designWideUsageMgr.getUsages(logicObject);

			List<IPinList> schemInstances = new ArrayList<IPinList>();
			ISchemDiagram mDiagram = null;
			for (IDesignSharedUsage usage : usages) {
				ISchemDiagram diagram = design.getDiagram(usage.getDiagramUID());
				assert diagram != null;
				// Load the diagrams based on the usage only, avoiding unnecessary loading of all diagrams
				diagram.loadToMemory();
				mDiagram = diagram;
				schemInstances.add((IPinList) usage.getDiagramObject());
			}

			if (mDiagram == null) {
				// unplaced objects
				mDiagram = design.getDiagrams().getNext();
			}

			UnsharePinListActionHelper unsharehelper =
					getPinListUnshareHelper(mDiagram, design, schemInstances, logicObject);
			BaseShareActionOperands operands = setupOperands(logicObject);
			IActionEnum setup = unsharehelper.setup(operands, "", mDiagram);
			objectsProcessed++;
			updateProgress(progress, currentProgress, objectsProcessed);
			if (IActionEnum.eCompleted.equals(setup)) {
				if (unsharehelper.doEdit()) {
					m_reporter.report(PromptSeverity.INFORMATION,
							ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
									"UnshareRedundantSharedObjectsCmd.Success.msg", design.getFullName()),
							IMessageContext.createContext(logicObject));
				}
				else {
					m_reporter.report(PromptSeverity.ERROR,
							ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
									"UnshareRedundantSharedObjectsCmd.EditFailure.msg", design.getFullName()),
							IMessageContext.createContext(logicObject));
				}
			}
			else {
				m_reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
								"UnshareRedundantSharedObjectsCmd.UnshareFailure.msg", design.getFullName()),
						IMessageContext.createContext(logicObject));
			}
		}
		return objectsProcessed;
	}

	private void updateProgress(IProgress progress, int currentProgress, int objectsProcessed)
	{
		progress.increment(ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
				"UnshareRedundantSharedObjectsCmd.Progress.update", (currentProgress + objectsProcessed),
				m_selectedRows.size()));
	}

	protected void unloadDesignFromMemory(ILogicDesign design)
	{
		design.unloadChildren();
	}

	private void loadDesignToMemory(ILogicDesign design)
	{
		design.loadToMemory();
	}

	protected boolean validateAndSaveDesign(IDesign design)
	{
		try {
			ValidationHelper.validateBeforeSave(design);
			CommitChanges(design);
			return true;
		}
		catch (ValidationException e) {
			return false;
		}
	}

	/**
	 * Execute a task with a progress dialog that includes a cancel button
	 */
	protected void executeTaskWithProgressDialog(@NotNull Frame owner, @NotNull Runnable task,
			@NotNull IProgress progress, boolean childProgressSupport, String title, String header)
	{
		ProgressTaskClient client = new ProgressTaskClient(progress, task, IProgress::cancel);
		InterruptableTaskHelper taskHelper = InterruptableTaskHelper.instanceReset();
		taskHelper.setStoppable(true);
		taskHelper.executeTask(client, owner, "UnshareTask", title, header, header, null, 100, childProgressSupport,
				true, null, null);
	}

	/**
	 * Display the unshare status table window with all feedback messages
	 * Reuses Batch Share infrastructure for consistent UI
	 *
	 * @param messages collection of status messages to display
	 */
	protected void displayUnshareStatusTab(@NotNull Collection<IBatchShareStatusMessage> messages)
	{
		BatchShareStatusWindowAssistant statusWindow = new BatchShareStatusWindowAssistant(
				ResourceMgr.getString(UnshareRedundantSharedObjectsCmd.class,
						"UnshareRedundantSharedObjectsCmd.StatusTab.title"),
				BatchShareFeedbackTableColumnEnum.SEVERITY.toString());
		statusWindow.addStatusMessages(messages);
	}

	@NotNull private String mapShareableEntityTypeToKey(@NotNull ShareableEntityTypeEnum type)
	{
		return switch (type) {
			case DEVICE -> DEV_KEY;
			case PLUG, JACK, INLINE, RING_TERMINAL -> CONN_KEY;
			case SPLICE -> SPLICE_KEY;
			case WIRE -> WIRE_KEY;
			case NET -> NET_KEY;
			case MULTICORE, OVERBRAID -> MULTICORE_KEY;
			default -> DEV_KEY;
		};
	}

	/**
	 * Determines the specific ShareableEntityTypeEnum from the shared object instance.
	 *
	 * @param sharedObject the shared object to analyze
	 * @param typeKey      the general type key (e.g., DEV_KEY, CONN_KEY)
	 * @return the specific ShareableEntityTypeEnum
	 */
	@NotNull private ShareableEntityTypeEnum determineEntityType(@NotNull ISharedObject sharedObject,
			@NotNull String typeKey)
	{
		if (CONN_KEY.equals(typeKey) && sharedObject instanceof ISharedConnector sharedConnector) {
			return determineConnectorEntityType(sharedConnector.getType());
		}

		if (MULTICORE_KEY.equals(typeKey)) {
			return (sharedObject instanceof ISharedOverbraid) ? ShareableEntityTypeEnum.OVERBRAID :
					ShareableEntityTypeEnum.MULTICORE;
		}

		return switch (typeKey) {
			case DEV_KEY -> ShareableEntityTypeEnum.DEVICE;
			case SPLICE_KEY -> ShareableEntityTypeEnum.SPLICE;
			case WIRE_KEY -> ShareableEntityTypeEnum.WIRE;
			case NET_KEY -> ShareableEntityTypeEnum.NET;
			default -> ShareableEntityTypeEnum.DEVICE;
		};
	}

	/**
	 * Maps PinListTypeEnum to ShareableEntityTypeEnum for connectors.
	 *
	 * @param pinListType the connector type from PinListTypeEnum
	 * @return the corresponding ShareableEntityTypeEnum
	 */
	@NotNull private ShareableEntityTypeEnum determineConnectorEntityType(@NotNull PinListTypeEnum pinListType)
	{
		if (pinListType.isInline()) {
			return ShareableEntityTypeEnum.INLINE;
		}

		if (pinListType.isRingTerminal()) {
			return ShareableEntityTypeEnum.RING_TERMINAL;
		}

		if (pinListType == PinListTypeEnum.TypePlug) {
			return ShareableEntityTypeEnum.PLUG;
		}

		else {
			return ShareableEntityTypeEnum.JACK;
		}
	}
}