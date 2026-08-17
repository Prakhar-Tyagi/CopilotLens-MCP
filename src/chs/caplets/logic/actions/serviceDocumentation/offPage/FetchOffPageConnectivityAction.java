package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.WaitCursor;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.CAFCommandListener;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IChainSegment;
import chs.cof.logical.schem.IChainSegmentContainer;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IMultipleConnectivityRef;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.*;
import chs.ctf.ui.form.styles.ConfirmationDialogWithDoNotShowOption;
import chs.images.CHSImageLoader;
import chs.publisher.offPage.ISelectionForFetch;
import chs.publisher.offPage.ISignalContentToBeCopiedProvider;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationSpecification(includeIn = {Application.SvcDoc})
public class FetchOffPageConnectivityAction extends ControllerActionRT implements ICtxMenuProvider
{

	public FetchOffPageConnectivityAction(ICapletController controller)
	{
		super(controller);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		return actionPerformed();
	}

	private boolean actionPerformed()
	{
		if (!showWarning()) {
			return false;
		}
		IProject project = getCurrentProject();
		ICapletController activeCapletController = getController();
		SelectSet selections = activeCapletController.getSelectMgr().getPreSelections();
		IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();
		IDesignContainer activeDesignContainer = CAFUtils.getInstance().getActiveDesignContainer();
		assert project != null;
		assert activeDiagram instanceof ISchemDiagram;
		assert activeDesignContainer instanceof ILogicDesign;
		FactoryMgr.getAPIFactory().beginOperation();
		try (WaitCursor ignore = new WaitCursor()) {
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();

			try (ISignalContentToBeCopiedProvider contentProvider =
						 FactoryMgr.getCommonFactory().getDiagramContentToBeCopiedProvider(project,
								 ISignalContentToBeCopiedProvider.TRACER_TYPE.FULL_SIGNAL,
								 ISignalContentToBeCopiedProvider.TRACE_STRATEGY.TRACE_THROUGH_SHARED_OBJECTS,
								 getMultitermStrategy(),
								 getAssociatedObjectsStrategy(), null)) {
				if (view != null) {
					view.lock();
				}
				return doFetch(contentProvider, project, selections, (ISchemDiagram) activeDiagram,
						(ILogicDesign) activeDesignContainer);
			}
			catch (Exception ignored) {
			}
			finally {
				if (view != null) {
					view.unlock();
					view.setViewCurrentCursor(view.getViewDefaultCursor());
				}
				FactoryMgr.getAPIFactory().endOperation();
			}
		}
		return false;
	}

	@NotNull protected ISignalContentToBeCopiedProvider.ASSOCIATED_OBJECTS_STRATEGY getAssociatedObjectsStrategy()
	{
		return ISignalContentToBeCopiedProvider.ASSOCIATED_OBJECTS_STRATEGY.BRING;
	}

	@NotNull protected ISignalContentToBeCopiedProvider.MULTITERM_TRACE_STRATEGY getMultitermStrategy()
	{
		return ISignalContentToBeCopiedProvider.MULTITERM_TRACE_STRATEGY.BRING_ADJACENT_MULTITERMS;
	}

	private boolean doFetch(ISignalContentToBeCopiedProvider contentProvider, IProject project, SelectSet selections,
			ISchemDiagram activeDiagram,
			@NotNull ILogicDesign activeDesignContainer)
	{
		ISelectionForFetch selection = getSelectionForFetch(selections);
		List<IDesignDescriptor> scope = getScope(project);
		FetchOffPageObjectsCmd cmd =
				createCommand(contentProvider, project, activeDiagram, activeDesignContainer,
						selection, scope);
		runCommand(cmd);
		return cmd.getResult();
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	@NotNull
	protected FetchOffPageObjectsCmd createCommand(ISignalContentToBeCopiedProvider contentProvider, IProject project,
			ISchemDiagram activeDiagram,
			@NotNull ILogicDesign activeDesignContainer, ISelectionForFetch selection,
			List<IDesignDescriptor> scope)
	{
		return new FetchOffPageObjectsCmd(project, scope, activeDiagram, activeDesignContainer,
				selection, contentProvider, true);
	}

	protected void runCommand(FetchOffPageObjectsCmd cmd)
	{
		CAFCommandListener.executeCommandWithProgressDlg(cmd, getClass(), cmd.getProgress());
	}

	private boolean showWarning()
	{
//		final MessagingResourceReader resourceReader =
//				new MessagingResourceReader(FetchOffPageConnectivityAction.class,
//						"FetchOffPageConnectivityAction.warning.choices");
//		Choice cancelChoice = new Choice(resourceReader, "cancel");
//		Choice okChoice = new Choice(resourceReader, "fetch");
//		ResourceBasedMessageContent content =
//				new ResourceBasedMessageContent(this, "FetchOffPageConnectivityAction.warning");
//		Choice userChoice = showQuestion(cancelChoice, okChoice, content);

		String title = ResourceMgr.getString(this,getClass().getSimpleName()+ ".warning.context");
		String message = ResourceMgr.getString(this,
				"FetchOffPageConnectivityAction.warning.message");
		String implications = ResourceMgr.getString(this,
				"FetchOffPageConnectivityAction.warning.implications");
		String question = ResourceMgr.getString(this,
				"FetchOffPageConnectivityAction.warning.guidance");
		ConfirmationDialogWithDoNotShowOption confirmDlg =
				new ConfirmationDialogWithDoNotShowOption(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
						title, message, implications, question, getClass().getName());
		return getConfirmation(confirmDlg);
//		return userChoice == okChoice;
	}

	protected boolean getConfirmation(ConfirmationDialogWithDoNotShowOption confirmDlg)
	{
		String ok = ResourceMgr.getString(this,
				"FetchOffPageConnectivityAction.warning.choices.fetch");
		String cancel = ResourceMgr.getString(this,
				"FetchOffPageConnectivityAction.warning.choices.cancel");
		String[] buttonNames = new String[]{ok, cancel};
		return showConfirmationDialog(confirmDlg, buttonNames);
	}

	protected boolean showConfirmationDialog(ConfirmationDialogWithDoNotShowOption confirmDlg, String[] buttonNames)
	{
		return confirmDlg.displayWarningMessage(buttonNames);
	}

	protected boolean handleTransactionsInAction()
	{
		return false;
	}

	@NotNull
	private ISelectionForFetch getSelectionForFetch(@NotNull SelectSet selections)
	{
		List<IAbstractSchemPin> pins = new ArrayList<>();
		List<IPinList> pinLists = new ArrayList<>();
		List<IConductor> conductors = new ArrayList<>();
		List<IHighwaySchematic> highways = new ArrayList<>();
		List<IChainSegmentContainer> chains = new ArrayList<>();
		List<IMulticore> multicores = new ArrayList<>();
		SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects();
		while (iter.hasNext()) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof IDiagramObject) {
				IUIDObject connectivityRef = getMultipleConnectivityRef((IDiagramObject) obj);
				if (connectivityRef == null) {
					connectivityRef = getConnectivityRef((IDiagramObject) obj);
				}
				if (connectivityRef instanceof IAbstractSchemPin) {
					pins.add((IAbstractSchemPin) connectivityRef);
				}
				if (connectivityRef instanceof IPinList) {
					pinLists.add((IPinList) connectivityRef);
				}
				if (connectivityRef instanceof IConductor) {
					conductors.add((IConductor) connectivityRef);
				}
				if (connectivityRef instanceof IHighwaySchematic) {
					highways.add((IHighwaySchematic) connectivityRef);
				}
				if (obj instanceof IChainSegment) {
					chains.add(((IChainSegment) obj).getDaisyChain());
				}
				if (obj instanceof IChainSegmentContainer) {
					chains.add((IChainSegmentContainer) obj);
				}
				if (obj instanceof IShieldBody) {
					chs.cof.logical.cable.IShieldBody connectivity = IShieldBody.class.cast(obj).getConnectivity();
					multicores.add(connectivity.getMulticore());
				}
			}
			if (obj instanceof IMulticore) {
				multicores.add(IMulticore.class.cast(obj));
			}
		}
		return FactoryMgr.getCommonFactory()
				.createSelectionForFetch(pins, pinLists, conductors, highways, chains, multicores);
	}

	private List<IDesignDescriptor> getScope(IProject project)
	{
		IBuildList activeBuildList = project.getBuildListMgr().getActiveBuildList();
		IDesignMgr designMgr = project.getDesignMgr();
		int logicalDesignCount = designMgr.getLogicalDesignCount();
		if (logicalDesignCount == 1) {
			IDesignDescriptor oneDesign = designMgr.getLogicalDesignDescriptors().iterator().next();
			List<IDesignDescriptor> designDescriptors = new ArrayList<>();
			designDescriptors.add(oneDesign);
			return designDescriptors;
		}
		if (activeBuildList == null) {
			return Collections.emptyList();
		}
		ICHSIterator<IDesignDescriptor> designDescriptors = activeBuildList.getDesignDescriptors();
		List<IDesignDescriptor> scope = designDescriptors.stream().collect(Collectors.toList());
		return scope;
	}

	@Override public boolean isEnabled()
	{
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		boolean valid = false;
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (IGenericSchemPin.class.isInstance(obj)) {
				if (IGenericSchemPin.class.cast(obj).isReference()) {
					continue;
				}
			}
			IUIDObject connectivityRef = null;
			if (obj instanceof IDiagramObject) {
				connectivityRef = getMultipleConnectivityRef((IDiagramObject) obj);
				if (connectivityRef == null) {
					connectivityRef = getConnectivityRef((IDiagramObject) obj);
				}
			}
			valid = valid ||
					(connectivityRef instanceof IGenericSchemPin ||
							connectivityRef instanceof ISchemStackPin ||
							connectivityRef instanceof IConductor ||
							connectivityRef instanceof IPinList ||
							connectivityRef instanceof IHighwaySchematic ||
							obj instanceof IChainSegmentContainer ||
							obj instanceof IChainSegment ||
							obj instanceof IMulticore ||
							obj instanceof IShieldBody);
		}
		boolean isEnabled = valid && getController().getCapletModel().isEditable() && super.isEnabled();
		String className = getClass().getSimpleName();
		String toolTip = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
				className + ".shortDesc.action.text");
		Action actionUI = getActionUI();
		if (actionUI != null) {
			actionUI.putValue(Action.LONG_DESCRIPTION,
					ResourceMgr.getString(FetchOffPageConnectivityAction.class,
							className + ".longDesc.action.text"));
		}
		if (!isEnabled) {
			toolTip = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
					className + ".disabled.action.text");
			if (actionUI != null) {
				actionUI.putValue(Action.LONG_DESCRIPTION,
						ResourceMgr.getString(FetchOffPageConnectivityAction.class,
								className + ".disabled.action.text"));
			}
		}
		setDisabledReason(toolTip);
		return isEnabled;
	}

	@Nullable public static IConnectivityRef getConnectivityRef(IDiagramObject diagramObject)
	{
		return getMatchingRefObject(diagramObject, IConnectivityRef.class);
	}

	@Nullable public static IMultipleConnectivityRef getMultipleConnectivityRef(IDiagramObject diagramObject)
	{
		return getMatchingRefObject(diagramObject, IMultipleConnectivityRef.class);
	}

	@Nullable
	private static <T> T getMatchingRefObject(IDiagramObject diagramObject, Class<T> refClass)
	{
		IDiagramObject object = diagramObject;
		while (true) {
			if (refClass.isInstance(diagramObject)) {
				return refClass.cast(diagramObject);
			}
			IDiagramObject parent = object.getParent();
			if (parent == null) {
				return null;
			}
			object = parent;
		}
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null) {
			container.add(new ActionEntry(actionUI));
		}
	}

	public String getActionUIClass()
	{
		return UI.class.getName();
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@ApplicationSpecification(
			includeIn = {Application.SvcDoc})
	public static class UI extends ActionUI
	{

		public UI(ICaplet caplet)
		{
			super(caplet);
			setEnabled(true);
		}

		public void setupUI()
		{
			Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/fetch-related-extended-small.png");
			String name = ResourceMgr
					.getString(FetchOffPageConnectivityAction.class, "FetchOffPageConnectivityAction.name.action.text");
			Integer mnemonic = (int) ResourceMgr
					.getMnemonic(FetchOffPageConnectivityAction.class, "FetchOffPageConnectivityAction.mnemonic.text");
			putValue(NAME, name);
			KeyStroke accel =
					KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
//					KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK);
			putValue(ACCELERATOR_KEY, accel);
			putValue(MNEMONIC_KEY, mnemonic);
			putValue(SHORT_DESCRIPTION, ResourceMgr.getString(FetchOffPageConnectivityAction.class,
					"FetchOffPageConnectivityAction.shortDesc.action.text"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(FetchOffPageConnectivityAction.class,
							"FetchOffPageConnectivityAction.longDesc.action.text"));
			putValue(SMALL_ICON, icon);
		}

		@Override public Icon getInactiveIcon()
		{
			return CHSImageLoader.loadImageIcon("chs/images/app/fetch-related-extended-disabled-small.png");
		}

		@Override public boolean isEnabled()
		{
			IAction action = getAction();
			if (action != null) {
				boolean actionEnabled = action.isEnabled();
//				String toolTip = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
//						"FetchOffPageConnectivityAction.shortDesc.action.text");
//				if (!actionEnabled) {
//					toolTip = ResourceMgr.getString(FetchOffPageConnectivityAction.class,
//							"FetchOffPageConnectivityAction.disabled.action.text");
//				}
//				putValue(SHORT_DESCRIPTION, toolTip);
//				getAction().setDisabledReason(toolTip);
				return actionEnabled;
			}
			return false;
		}

		public String getActionClass()
		{
			return FetchOffPageConnectivityAction.class.getName();
		}
	}
}
