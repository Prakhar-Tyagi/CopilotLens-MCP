package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.api.servicedoc.statusmessage.IssueReporterProvider;
import chs.api.servicedoc.statusmessage.XIssue;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.servicedoc.PublisherStatusWindowAssistant;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.IConductorRouteAction;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.serviceDocumentation.offPage.messages.FetchActionStatusMessageTableModel;
import chs.caplets.logic.actions.serviceDocumentation.offPage.messages.MessageReporterWithContext;
import chs.caplets.logic.actions.serviceDocumentation.offPage.routeAndConnect.RouteAndConnectFetchedObjectsCmd;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IGfxView;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IPinFilter;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cof.logical.shared.ISharedHighwayConnectionMgr;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CHSCommand;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IDesignContainer;
import chs.common.IDesignDescriptor;
import chs.common.IDesignEditabilityProvider;
import chs.common.IGuard;
import chs.common.IObjectFilter;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.common.preferencesets.IPreferenceSet;
import chs.ctf.ui.utility.statusmessage.IPublisherStatusMessage;
import chs.publisher.offPage.IDesignContentToBeCopied;
import chs.publisher.offPage.ISelectionForFetch;
import chs.publisher.offPage.ISignalContentToBeCopied;
import chs.publisher.offPage.ISignalContentToBeCopiedProvider;
import chs.system.FactoryMgr;
import chs.utilities.BuildInfo;
import chs.utilities.CommonUtils;
import chs.utilities.DesignLoadException;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.DiagramHelper;
import chs.utility.IMessageContext;
import chs.utility.Placement;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.IndicatorRefresherUtils;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.MulticoreIndicatorRefresher;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.IProgressGroup;
import chs.utility.ui.progress.ProgressGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FetchOffPageObjectsCmd extends CHSCommand
{

	private final ISchemDiagram m_activeDiagram;
	private final MessageReporterWithContext m_messageReporter;
	private final ISignalContentToBeCopiedProvider m_contentProvider;
	private final ISelectionForFetch m_selection;
	private final IProject m_project;
	private boolean m_result;
	protected final ILogicDesign m_activeDesignContainer;
	private IProgressGroup m_progressGroup;
	private IProgress m_copyProgress;
	private IProgress m_mergeSegmentsProgress;
	private IProgress m_applyStyleProgress;
	private IProgress m_autoRouteProgress;
	protected Set<ILogicDesign> m_designsToUnlock = new HashSet<>();
	protected List<ILogicDesign> m_logicDesigns;
	private IProgress m_showFeedbackProgress;
	private PublisherStatusWindowAssistant m_statusWindowAssistant;
	private boolean m_includeAllPins;

	public FetchOffPageObjectsCmd(IProject project, List<IDesignDescriptor> scope, ISchemDiagram activeDiagram,
			ILogicDesign activeDesignContainer, ISelectionForFetch selection,
			ISignalContentToBeCopiedProvider contentProvider, boolean includeAllPins)
	{
		super(new CAFCommandHelper());
		m_project = project;
		m_activeDiagram = activeDiagram;
		m_activeDesignContainer = activeDesignContainer;
		activeDiagram.getDesign();
		IssueReporterProvider issueReporterProvider = new IssueReporterProvider();
		m_messageReporter = new MessageReporterWithContext("Fetch action", "", issueReporterProvider);
		m_selection = selection;
		m_logicDesigns = scope
				.stream()
				.map(designDescriptor -> designDescriptor.getDesignContainer())
				.filter(container -> container instanceof ILogicDesign)
				.map(container -> (ILogicDesign) container)
				.collect(Collectors.toList());
		m_contentProvider = contentProvider;
		m_includeAllPins = includeAllPins;
		String tabName = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
				getClassNameForActionSpecificMessage() + ".feedback.tab.name");
		m_statusWindowAssistant = new PublisherStatusWindowAssistant(tabName);
		m_statusWindowAssistant.removeStatusTab();
		createProgress(contentProvider);
	}

	@Override public boolean doExecuteAllowed()
	{
		if (m_logicDesigns.isEmpty()) {
			m_messageReporter.report(PromptSeverity.ERROR, ResourceMgr.getString(FetchOffPageConnectivityAction.class,
					"FetchOffPageConnectivityAction.message.activebuildlist.notset"));
			showFeedback();
			return false;
		}
		boolean scopeContainsCurrentDesign = m_logicDesigns
				.stream()
				.map(descriptor -> descriptor.getUID())
				.filter(uid -> m_activeDesignContainer.getUID().equals(uid))
				.findFirst()
				.isPresent();
		//if there is only one design, do not do the check
//		scopeContainsCurrentDesign = scopeContainsCurrentDesign || m_logicDesigns.size() == 1;
		if (!scopeContainsCurrentDesign) {
			m_messageReporter.report(PromptSeverity.ERROR, ResourceMgr.getString(FetchOffPageConnectivityAction.class,
					"FetchOffPageConnectivityAction.message.design.notpartof.activebuildlist"),
					IMessageContext.createContext(m_activeDesignContainer));
		}
		boolean noDiagramOpen = true;
		if (checkIfDiagramsInScopeAreOpen()) {
			noDiagramOpen = false;
		}

		boolean acquiredAllLocks = false;
		try {
			acquiredAllLocks = acquireLocks();
			boolean goodToGoAhead = scopeContainsCurrentDesign && noDiagramOpen && acquiredAllLocks;
			if (!goodToGoAhead) {
				showFeedback();
			}
			return goodToGoAhead;
		}
		finally {
			if (!acquiredAllLocks) {
				releaseAcquiredLocks();
			}
		}
	}

	protected boolean checkIfDiagramsInScopeAreOpen()
	{
		boolean anyDiagramInScopeOpen = isAnyDiagramInScopeOpen();
		if (anyDiagramInScopeOpen) {
			Set<IBaseDiagram> openDiagrams = getOpenDiagrams();
			Object[] objects = openDiagrams.toArray();
			IMessageContext context = IMessageContext.createContext(objects);
			m_messageReporter
					.report(PromptSeverity.ERROR, ResourceMgr.getString(FetchOffPageConnectivityAction.class,
							"FetchOffPageConnectivityAction.message.diagrams.open")
							, context);
		}
		return anyDiagramInScopeOpen;
	}

	protected boolean isAnyDiagramInScopeOpen()
	{
		Set<IBaseDiagram> openDiagrams = getOpenDiagrams();
		return !openDiagrams.isEmpty();
	}

	private Set<IBaseDiagram> getOpenDiagrams()
	{
		List<ICapletWindow> capletWindows = CAFUtils.getInstance().getCapletWindowsForProject(m_project);
		Set<IBaseDiagram> openDiagrams = capletWindows
				.stream()
				.map(ICapletWindow::getCurrentView)
				.map(view -> CommonUtils.cast(view, IGfxView.class))
				.filter(Objects::nonNull)
				.map(IGfxView::getDiagram)
				.filter(Objects::nonNull)
				.filter(d -> !m_activeDiagram.equals(d))
				.filter(IBaseDiagram::isEditable)
				.filter(diagram -> {
					IDesignContainer design = diagram.getDesignContainer();
					return !m_activeDesignContainer.equals(design) && m_logicDesigns.contains(design);
				})
				.collect(Collectors.toSet());
		return openDiagrams;
	}

	@Override protected boolean doExecute()
	{
		m_result = doFetch();
		return m_result;
	}

	public boolean getResult()
	{
		return m_result;
	}

	@Override protected void doEnd(boolean executeOk)
	{
		//getCommandHelper().exitTransactionBoundary(this, executeOk);
		releaseAcquiredLocks();
		showFeedback();
	}

	protected boolean acquireLocks()
	{
		return batchLockDesigns();
	}

	/**
	 * only lock those which are not already locked and not the current design and unlock only those which are locked by
	 * the action
	 *
	 * @return true if designs are locked successfully
	 */
	protected boolean batchLockDesigns()
	{
		Set<ILogicDesign> toBeLocked = m_logicDesigns
				.stream()
				//not already locked and not the current design
				.filter(obj -> !obj.isLocked() && !obj.equals(m_activeDesignContainer))
				.collect(Collectors.toSet());
		Set<IUID> failedToLock = doBatchLock(toBeLocked);
		//only those which are locked by the action
		m_designsToUnlock = toBeLocked
				.stream()
				.filter(obj -> obj.isLocked())
				.collect(Collectors.toSet());
		if (!failedToLock.isEmpty()) {
			List<ILogicDesign> notAbleToLock =
					IUIDObject.Statics.getListOfType(failedToLock, ILogicDesign.class);
			for (ILogicDesign design : notAbleToLock) {
				IMessageContext context = IMessageContext.createContext(List.of(design).toArray());
				String lockOwner = getLockOwner(design);
				String messageString = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
						"FetchOffPageConnectivityAction.warning.close.designs", design.getFullName(), lockOwner);
				m_messageReporter.report(PromptSeverity.ERROR, messageString, context);
			}
			showLockedDesignsError();
			return false;
		}
		return true;
	}

	private void showLockedDesignsError()
	{
		ResourceBasedMessageContent content = getResourceBasedMessageContent();
		Message.show(PromptSeverity.ERROR, content);
	}

	@NotNull protected ResourceBasedMessageContent getResourceBasedMessageContent()
	{
		//content is assigned according to the flow of "Fetch Related" or "Fetch Related Extended"
		ResourceBasedMessageContent content =
				m_includeAllPins ? new ResourceBasedMessageContent(FetchOffPageConnectivityAction.class,
						"FetchOffPageConnectivityAction.warning.design.locked") :
						new ResourceBasedMessageContent(FetchOffPageConnectivityAction.class,
								"FetchWithOnlyPinsInSignalAction.warning.design.locked");
		return content;
	}

	@NotNull
	private String getLockOwner(ILogicDesign design)
	{
		try {
			ILockInfo lockInfo = UtilsHelper.getPersistenceSession().getLockInfo(design);
			return lockInfo.getUserName();
		}
		catch (UserSessionException ex) {
			BuildInfo.getBuildInfo().ifDeveloperExtensionsAreEnabled(() -> {
				System.out.println("Lock information cannot be retrieved " + ex.getMessage());
			});
		}
		return "";
	}

	@NotNull protected Set<IUID> doBatchLock(Set<ILogicDesign> toBeLocked)
	{
		return UtilsHelper.getPersistenceSession().batchLock(toBeLocked);
	}

	private void releaseAcquiredLocks()
	{
		unlockDesigns(m_designsToUnlock);
		m_designsToUnlock.clear();
	}

	@NotNull @Override public IProgress getProgress()
	{
		return m_progressGroup;
	}

	private void createProgress(ISignalContentToBeCopiedProvider contentProvider)
	{
		String className = getClassNameForActionSpecificMessage();
		m_progressGroup =
				new ProgressGroup(ResourceMgr.getString(FetchOffPageConnectivityAction.class,
						className + ".defaultProgress.name"));
		m_progressGroup.setChangeButtonStatus(true);
		setReportingContextForContentProvider(contentProvider, m_progressGroup);
		m_copyProgress = m_progressGroup.createChild(0, 1, "");
		m_mergeSegmentsProgress = m_progressGroup.createChild(0, 1, "");
		m_applyStyleProgress = m_progressGroup.createChild(1, 1, "");
		m_autoRouteProgress = m_progressGroup.createChild(1, 1, "");
		m_showFeedbackProgress = m_progressGroup.createChild(1, 1, "");
		m_copyProgress.setChangeButtonStatus(true);
		m_mergeSegmentsProgress.setChangeButtonStatus(true);
		m_applyStyleProgress.setChangeButtonStatus(true);
		m_autoRouteProgress.setChangeButtonStatus(true);
		m_showFeedbackProgress.setChangeButtonStatus(true);
	}

	@NotNull
	private String getClassNameForActionSpecificMessage()
	{
		String className = FetchWithOnlyPinsInSignalAction.class.getSimpleName();
		if (m_includeAllPins) {
			className = FetchOffPageConnectivityAction.class.getSimpleName();
		}
		return className;
	}

	protected boolean doFetch()
	{
		RouteAndConnectFetchedObjectsCmd routeAndConnect =
				new RouteAndConnectFetchedObjectsCmd(m_project, m_logicDesigns, m_activeDiagram, m_selection);
		routeAndConnect.setReporter(m_messageReporter);
		routeAndConnect.addProgress(m_mergeSegmentsProgress);
		if (isProgressCancelled()) {
			return false;
		}
		ISignalContentToBeCopied contentToCopy = getSignalContentToBeCopied();
		if (contentToCopy == null) {
			return false;
		}
//		m_progressGroup.setCancelButtonEnablement(false);
		m_copyProgress.setCancelButtonEnablement(false);
		m_mergeSegmentsProgress.setCancelButtonEnablement(false);
		m_applyStyleProgress.setCancelButtonEnablement(false);
		m_autoRouteProgress.setCancelButtonEnablement(false);
		m_showFeedbackProgress.setCancelButtonEnablement(false);
		List<IDesignContentToBeCopied> designContentToBeCopied = contentToCopy.getDesignContentToBeCopied();
		boolean preCheckResult = new PreFetchChecks(designContentToBeCopied, m_messageReporter).runChecks();
		if (!preCheckResult) {
			return false;
		}
		IObjectFilter<chs.cof.logical.cable.IConductor> endConductorFilter = contentToCopy.endConductorFilter();
		IObjectFilter<ILogicObject> objectInTheSignalFilter = contentToCopy.objectInTheSignalFilter();
		Set<IConductor> diagramConductorsInSignal =
				getDiagramConductorsInSignal(objectInTheSignalFilter, endConductorFilter);
		if (designContentToBeCopied.isEmpty()) {
			Set<IAbstractSchemPin> diagramPinsInSignal = getDiagramPinsInSignal(objectInTheSignalFilter);
			boolean result = false;
			String message;
			if (!diagramConductorsInSignal.isEmpty() && !diagramPinsInSignal.isEmpty()) {
				routeAndConnectAndPostProcess(routeAndConnect, diagramConductorsInSignal, diagramPinsInSignal);
				// TODO: different message to show
				message = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
						"FetchOffPageConnectivityAction.message.no.information.fetched");
				m_messageReporter.report(PromptSeverity.INFORMATION, message);
				result = true;
			}
			else {
				message = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
						"FetchOffPageConnectivityAction.message.nocontent.retrieved");
				m_messageReporter.report(PromptSeverity.WARNING, message);
			}
			return result;
		}
		m_copyProgress.setRange(designContentToBeCopied.size());
		unlockDesignsWhichAreNotPartOfSignal(designContentToBeCopied);
		return doCopyAndApplyStyle(designContentToBeCopied, routeAndConnect,
				contentToCopy.getPinInTheSignalPredicate(m_includeAllPins), endConductorFilter,
				diagramConductorsInSignal);
	}

	@NotNull private Set<IAbstractSchemPin> getDiagramPinsInSignal(IObjectFilter<ILogicObject> objectInTheSignalFilter)
	{
		return m_activeDiagram
				.getPinLists()
				.stream()
				.map(pinList -> pinList.getPins())
				.flatMap(IUIDObjectCollection::stream)
				.filter(c -> objectInTheSignalFilter.accept(c.getConnectivity()))
				.collect(Collectors.toSet());
	}

	@NotNull private Set<IConductor> getDiagramConductorsInSignal(IObjectFilter<ILogicObject> objectInTheSignalFilter,
			IObjectFilter<chs.cof.logical.cable.IConductor> endConductorFilter)
	{
		return m_activeDiagram
				.getConductors()
				.stream()
				.filter(c -> objectInTheSignalFilter.accept(c.getConnectivity()))
				.filter(c -> !endConductorFilter.accept(c.getConnectivity()))
				.collect(Collectors.toSet());
	}

	private void showFeedback()
	{
		try {
			if (isProgressCancelled()) {
				String feedbackSummaryMessage =
						ResourceMgr.getString(FetchOffPageConnectivityAction.class,
								"FetchOffPageConnectivityAction.feedback.cancel");
				showSummary(feedbackSummaryMessage);
			}
			else {
				String progressText =
						ResourceMgr.getString(FetchOffPageConnectivityAction.class,
								"FetchOffPageConnectivityAction.showingFeedback.text");
				m_showFeedbackProgress.increment(progressText);
				showReportedMessages();
			}
		}
		catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		finally {
			if (m_progressGroup.isStarted()) {
				String progressText =
						ResourceMgr.getString(FetchOffPageConnectivityAction.class,
								"FetchOffPageConnectivityAction.completed.text");
				m_progressGroup.complete(progressText);
			}
		}
	}

	private void showReportedMessages()
			throws InvocationTargetException, InterruptedException
	{
		Set<XIssue> issues = new LinkedHashSet<>();
		Integer noOfErrors = m_messageReporter.getIssueReporter().getNoOfErrors();
		Integer noOfWarnings = m_messageReporter.getIssueReporter().getNoOfWarnings();
		Set<? extends IPublisherStatusMessage> messages = m_messageReporter.getIssueReporter().getIssues();
		List<XIssue> xIssues =
				messages
						.stream()
						.filter(XIssue.class::isInstance)
						.map(XIssue.class::cast)
						.collect(Collectors.toList());
		issues.addAll(xIssues);
		if (issues.isEmpty()) {
			showGenerationSummaryMessage(noOfErrors, noOfWarnings);
			return;
		}
		FetchActionStatusMessageTableModel tableModel =
				new FetchActionStatusMessageTableModel(m_statusWindowAssistant);
		showGenerationSummaryMessage(noOfErrors, noOfWarnings);
		showFeedbackTab(issues, m_statusWindowAssistant, tableModel);
	}

	private void showGenerationSummaryMessage(Integer noOfErrors, Integer noOfWarnings)
	{
		String className = getClassNameForActionSpecificMessage();
		String feedbackSummaryMessage =
				ResourceMgr.getString(FetchOffPageConnectivityAction.class,
						className + ".feedback.status")
						.replace("{0}", noOfErrors.toString())
						.replace("{1}", noOfWarnings.toString());

		showSummary(feedbackSummaryMessage);
	}

	protected void showSummary(String feedbackSummaryMessage)
	{
		FactoryMgr.getAPIFactory()
				.getApplicationContext()
				.getOutputWindow()
				.println(UtilsHelper.getStandardPaneName(), feedbackSummaryMessage);
	}

	protected void showFeedbackTab(Set<XIssue> reportedMessages,
			PublisherStatusWindowAssistant statusWindowAssistant, FetchActionStatusMessageTableModel tableModel)
			throws InvocationTargetException, InterruptedException
	{
		SwingUtilities.invokeAndWait(() -> showFeedbackMessages(reportedMessages, statusWindowAssistant, tableModel));
	}

	protected void showFeedbackMessages(Set<XIssue> reportedMessages,
			PublisherStatusWindowAssistant statusWindowAssistant, FetchActionStatusMessageTableModel tableModel)
	{
		statusWindowAssistant.showStatusTab(reportedMessages, tableModel);
	}

	@Nullable private ISignalContentToBeCopied getSignalContentToBeCopied()
	{
		ISignalContentToBeCopied copyableDiagramContents = null;
		// CARCH-1291 - changed the creation deletion helper guard
		try (IGuard ignored = CreationDeletionHelper.createDisableCreationDeletionHelperInThreadGuard()) {
			copyableDiagramContents =
					m_contentProvider.getDesignContentToBeCopied(m_selection, m_logicDesigns,
							(uid) -> uid.equals(m_activeDiagram.getUID()));
		}
		catch (DesignLoadException e) {
			m_messageReporter
					.report(PromptSeverity.ERROR, e.getMessage(), IMessageContext.createContext(e.getDesign()));
		}
		return copyableDiagramContents;
	}

	protected void setReportingContextForContentProvider(ISignalContentToBeCopiedProvider contentProvider,
			IProgressGroup progressGroup)
	{
		IProgress signalTracerProgress = progressGroup.createChild(0, 1, "");
		contentProvider.setProgress(signalTracerProgress);
		contentProvider.setReporter(m_messageReporter);
	}

	protected boolean routeAndConnectAndPostProcess(RouteAndConnectFetchedObjectsCmd routeAndConnect,
			Set<IConductor> diagramConductorsInSignal, Set<IAbstractSchemPin> diagramPinsInSignal)
	{
		boolean isSuccess = doRouteAndConnect(routeAndConnect, diagramConductorsInSignal, Collections.emptySet(),
				diagramPinsInSignal);
		if (isSuccess) {
			doAutoRoute();
			if (isProgressCancelled()) {
				return false;
			}
			doApplyStyle();
			return true;
		}
		return false;
	}

	protected boolean doCopyAndApplyStyle(List<IDesignContentToBeCopied> copyableDesignContents,
			RouteAndConnectFetchedObjectsCmd routeAndConnect,
			IPinFilter pinFilter, IObjectFilter<chs.cof.logical.cable.IConductor> endConductor,
			Set<IConductor> diagramConductorsInSignal)
	{
		Set<IDesign> modifiedDesigns = new HashSet<>();
		boolean isCopySuccessfull =
				doCopy(copyableDesignContents, routeAndConnect, pinFilter, endConductor, diagramConductorsInSignal,
						modifiedDesigns);
		if (isCopySuccessfull) {
			doAutoRoute();
			if (isProgressCancelled()) {
				return false;
			}
			doApplyStyle();
			if (!modifiedDesigns.isEmpty()) {
				String msg = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
						"FetchOffPageConnectivityAction.showingChangedDesigns.text", modifiedDesigns.size());
				LogHelper.appMsgSafe(msg);
				LogHelper.appMsgSafe(
						modifiedDesigns.stream()
								.map(IDesignEditabilityProvider::getFullName)
								.collect(Collectors.joining(", "))
				);
			}

			return true;
		}
		return false;
	}

	private void doAutoRoute()
	{
		doAutoRoute(m_autoRouteProgress, this::isProgressCancelled);
	}

	public static void doAutoRoute(IProgress autoRouteProgress, Supplier<Boolean> isCancelled)
	{
		IConductorRouteAction routeAction = ConductorRouteAction.getInstance();
		if (isCancelled.get()) {
			routeAction.clear();
			return;
		}
		String progressMsg = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
				"FetchOffPageConnectivityAction.autoRoute.text");
		autoRouteProgress.increment(progressMsg);
		try {
			routeAction.processAction(true, false);
		}
		catch (RuntimeException ignored) {
		}
		finally {
			routeAction.clear();
		}
	}

	private void doApplyStyle()
	{
		doApplyStyle(m_activeDiagram, m_applyStyleProgress);
	}

	public static void doApplyStyle(ISchemDiagram activeDiagram, IProgress applyStyleProgress)
	{
		IPreferenceSet preferenceSet = activeDiagram.getPreferenceSet();
		if (preferenceSet != null) {
			String progressMsg = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
					"FetchOffPageConnectivityAction.applystyle.text");
			applyStyleProgress.increment(progressMsg);
			try {
				FactoryMgr.getCommonFactory().doApplyStyle(activeDiagram, preferenceSet, false, false);
			}
			catch (RuntimeException ignored) {
			}
		}
	}

	private boolean doCopy(List<IDesignContentToBeCopied> copyableDesignContents,
			RouteAndConnectFetchedObjectsCmd routeAndConnect, IPinFilter pinFilter,
			IObjectFilter<chs.cof.logical.cable.IConductor> endConductor,
			Set<IConductor> diagramConductorsInSignal, Set<IDesign> modifiedDesigns)
	{
		MatedPinListsGenerator matedPinListsGenerator =
				new MatedPinListsGenerator(m_selection, m_messageReporter, m_activeDiagram, pinFilter);
		ISymbolToParameterizedConverter converter;
		ISymbolsToConvertProvider symbolsToConvertProvider;
		if (m_includeAllPins) {
			converter = ISymbolToParameterizedConverter.getDefaultConverter();
			symbolsToConvertProvider = ISymbolsToConvertProvider.getDefaultProvider();
		}
		else {
			converter = new SymbolToParameterizedDeviceConverter(m_activeDiagram);
			symbolsToConvertProvider = new SymbolsToConvertProvider(pinFilter);
		}
		FetchOffPageContentHelper fetchHelper =
				new FetchOffPageContentHelper(pinFilter, converter, symbolsToConvertProvider);
		for (IDesignContentToBeCopied copyableDesignContent : copyableDesignContents) {
			if (isProgressCancelled()) {
				return false;
			}
			//Remove the diagramObjects that needs not be copied
			List<IDiagramObject> noCopyNeeded =
					matedPinListsGenerator.getContentForWhichCopyIsNotNeeded(copyableDesignContent);
			if (!fetchHelper.fetch(copyableDesignContent, noCopyNeeded, m_copyProgress, m_messageReporter,
					modifiedDesigns)) {
				return false;
			}
//			IUIDObject.Statics.getListOfType(failedToLock, ILogicDesign.class);
//			copyableDesignContent.getDesignId()
		}
		matedPinListsGenerator.generateMatedPinLists();
		LogicUtils.fireChangeEvent(fetchHelper.getAffectedSharedObjects((sharedObj) -> true));
		return doRouteAndConnect(routeAndConnect, fetchHelper, endConductor, diagramConductorsInSignal);
	}

	private void unlockDesignsWhichAreNotPartOfSignal(List<IDesignContentToBeCopied> copyableDesignContents)
	{
		Set<ILogicDesign> designsInSignal = new HashSet<>();
		Set<ILogicDesign> designsLockedButNotInSignal = new HashSet<>(m_designsToUnlock);
		for (IDesignContentToBeCopied copyableDesignContent : copyableDesignContents) {
			ILogicDesign design =
					CommonUtils.cast(copyableDesignContent.getDesignId().getObject(), ILogicDesign.class);
			designsInSignal.add(design);
		}
		designsLockedButNotInSignal.removeAll(designsInSignal);
//		unloadDesigns(designsLockedButNotInSignal);
		unlockDesigns(designsLockedButNotInSignal);
		m_designsToUnlock.retainAll(designsInSignal);
	}

	private void unlockDesigns(Collection<ILogicDesign> designs)
	{
		UtilsHelper.getPersistenceSession().batchUnlock(designs);
	}

//	private void unloadDesigns(Collection<ILogicDesign> designs)
//	{
//		designs.forEach(ILogicDesign::unload);
//	}

	private boolean doRouteAndConnect(RouteAndConnectFetchedObjectsCmd routeAndConnect,
			FetchOffPageContentHelper fetchHelper,
			IObjectFilter<chs.cof.logical.cable.IConductor> endConductor,
			Set<IConductor> diagramConductorsInSignal)
	{
		ISharedHighwayConnectionMgr sharedHighwayConnectionMgr = m_project.getSharedHighwayConnectionMgr();
		sharedHighwayConnectionMgr.update(m_activeDesignContainer);
		Set<chs.cof.logical.cable.IConductor> selectedConductors =
				m_selection.getConductors()
						.stream()
						.map(sc -> sc.getConnectivity())

						.collect(Collectors.toSet());
		Set<IConductor> schemConducters = fetchHelper
				.getFetchedObjects((uidO) -> uidO instanceof IConductor)
				.stream()
				.map(uidO -> (IConductor) uidO)
//				.filter(conductor -> {
//					return !endConductor.accept(conductor.getConnectivity()) ||
//							selectedConductors.contains(conductor.getConnectivity());
//				})
				.collect(Collectors.toSet());

		//add conductors in the diagram which are part of the signal to the route and connectible conds
		Set<chs.cof.logical.cable.IConductor> condConn =
				schemConducters
						.stream()
						.map(IConductor::getConnectivity)
						.collect(Collectors.toSet());

		Set<IMulticore> multicores = condConn
				.stream()
				.map(chs.cof.logical.cable.IConductor::getMulticore)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		diagramConductorsInSignal
				.stream()
				.filter(cond -> !condConn.contains(cond.getConnectivity()))
				.forEach(schemConducters::add);

		Set<IHighwaySchematic> schemHighways = fetchHelper
				.getFetchedObjects((uidO) -> uidO instanceof IHighwaySchematic)
				.stream().map(uidO -> (IHighwaySchematic) uidO)
				.collect(Collectors.toSet());

		Set<IAbstractSchemPin> fetchedPins = fetchHelper
				.getFetchedObjects((uidO) -> uidO instanceof IPinList)
				.stream()
				.map(uidO -> (IPinList) uidO)
				.map(IPinList::getAllPins)
				.flatMap(IUIDObjectCollection::stream)
				.collect(Collectors.toSet());
		boolean result = doRouteAndConnect(routeAndConnect, schemConducters, schemHighways, fetchedPins);
		regenerateShieldBodies(multicores);
		return result;
	}

	/**
	 * This method regenerates the shield bodies for the multi cores
	 * It deletes the existing shield bodies (only those which are not connected with shield conductors)
	 * If at least one of the existing shield bodies remains not deleted, then we do not regenerate.
	 * We call refresh indicators at the end.
	 * @param multicores for which the shield bodies needs to be regenerated
	 */
	private void regenerateShieldBodies(Set<IMulticore> multicores)
	{
		final Set<IMulticore> mcs = new LinkedHashSet<>();
		final Set<IMulticore> rootMcs = multicores
				.stream()
				.map(IMulticore::getRootMulticore)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		rootMcs
				.forEach(rootMc -> {
					mcs.addAll(rootMc.getAllMulticoresInHierarchy());
				});
		//for each multicore, we call regenerateShieldBodies, first for roots, then for multicores in hierarchy
		mcs
				.forEach(this::regenerateShieldBodies);
		//call refresh indicators at the end.
		IndicatorRefresherUtils.refreshMulticoreIndicators(mcs, m_activeDiagram);
	}

	private void regenerateShieldBodies(IMulticore multicore)
	{
		MulticoreIndicatorRefresher multicoreIndicatorRefresher = new MulticoreIndicatorRefresher(m_activeDiagram);
		Set<IShieldBody> allShieldBodies = new LinkedHashSet<IShieldBody>();
		//get all the indicators for the multi cores
		multicoreIndicatorRefresher.populateShieldIndicators(Collections.singleton(multicore), allShieldBodies);
		//find the indicators to delete, only those which does not have any shield conductors connected to the hookups
		//are deleted.
		Set<IShieldBody> shieldBodiesToDelete = allShieldBodies
				.stream()
				.filter(this::isConnectedToNoShieldConductors)
				.collect(Collectors.toSet());
		//only one shield body which is connected to a shield conductor, we retain one other shield body
		if (!shieldBodiesToDelete.isEmpty() && (allShieldBodies.size() - 1) == shieldBodiesToDelete.size()) {
			IShieldBody first = shieldBodiesToDelete.iterator().next();
			shieldBodiesToDelete.remove(first);
		}
		//delete the shield bodies to delete
		IndicatorRefresherUtils.deleteShieldBodies(m_activeDiagram, new ArrayList<>(shieldBodiesToDelete), true);
		//if all the shield bodies of the multi cores are deleted, then generate new indicators
		//if there exists at least one indicator which is not deleted, then we do not generate new indicators
		if (allShieldBodies.size() == shieldBodiesToDelete.size()) {
			Generator generator = Generator.getGenerator();
			GeneratorParameters params = DiagramHelper.createGeneratorParameters(m_activeDiagram);
			//additional check for making sure that the multicore has a shield body and the shield body has no representations
			if (multicore.getShieldBody() != null &&
					m_activeDiagram.getRepresentationsCollection(multicore.getShieldBody().getUID()).isEmpty()) {
				Placement
						.placeIndicators(generator, m_activeDiagram, multicore, multicore.getShieldBody(), params, true,
								null, false, true, true);
			}
		}
	}

	private boolean isConnectedToNoShieldConductors(IShieldBody s)
	{
		Collection<IShieldBodyHookup> shieldBodyHookups = s.getShieldBodyHookups();
		long hookupsWithShieldConductors = shieldBodyHookups
				.stream()
				.filter(this::isHookedToShieldOrDaisyChain)
				.count();
		//check if all the hookups are not connected with any shield conductors.
		return hookupsWithShieldConductors == 0;
	}

	private boolean isHookedToShieldOrDaisyChain(IShieldBodyHookup sbh)
	{
		return !sbh.getShieldConductors().isEmpty() || !sbh.getShieldChains().isEmpty();
	}

	private boolean doRouteAndConnect(RouteAndConnectFetchedObjectsCmd routeAndConnect, Set<IConductor> schemConducters,
			Set<IHighwaySchematic> schemHighways, Set<IAbstractSchemPin> fetchedPins)
	{
		if (isProgressCancelled()) {
			return false;
		}
		routeAndConnect.routeAndConnect(schemConducters, schemHighways, fetchedPins);
		return true;
	}

	private boolean isProgressCancelled()
	{
//		return m_progressGroup.isCancelled();
		return false;
	}
}
