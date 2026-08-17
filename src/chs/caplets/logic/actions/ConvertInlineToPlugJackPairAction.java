package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IUndoable;
import chs.caf.caplet.IUndoableContainer;
import chs.caf.caplet.IUndoableObject;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.IUserSessionRemotePackage.ObjectUsageInfo;
import chs.capitalmanager.appserver.LockException;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.shared.ForeignDesignChangesHandler;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedInternalPosition;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.parts.ILibraryConnector;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IProject;
import chs.cofUtils.scrubber.LogicInvalidStateScrubber;
import chs.cofUtils.scrubber.SharedObjectInvalidStateScrubber;
import chs.common.IIncLoadable;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.IBoundaryTransactionMarshaller;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConvertInlineToPlugJackPairAction extends ControllerActionRT implements ICtxMenuProvider
{

	public ConvertInlineToPlugJackPairAction(
			@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		if (successful) {

			Pair<IInlinePlugConnector, IInlineJackConnector> selectedInline = getSelectedInline();

			if (selectedInline != null) {

				ConvertInlineToPlugJackConverter converter =
						new ConvertInlineToPlugJackConverter(selectedInline.getFirst(), selectedInline.getSecond());
				String error = converter.convertToPlugJackPair();
				if (error != null) {
					IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
					outputWindow.sendApplicationMessage(HTMLHelper
							.color("red", error));
				}
				return error == null;
			}
		}
		return false;
	}

	@Nullable private Pair<IInlinePlugConnector, IInlineJackConnector> getSelectedInline()
	{
		SelectSet inputSet = getController().getSelectMgr().getPreSelections();
		Set<IGenericInlineConnector> connectors =
				inputSet.getSelectedUIDS()
						.stream()
						.map(ReferenceHelper::reduceToConnectivityObject)
						.filter(connectivityObject -> connectivityObject instanceof IGenericInlineConnector)
						.map(aPlugConn -> (IGenericInlineConnector) aPlugConn)
						.limit(3)
						.collect(Collectors.toSet());

		if (connectors.size() == 2) {

			Iterator<IGenericInlineConnector> inlineConnectorIterator = connectors.iterator();
			IInlinePlugConnector inlinePlugConnector = null;
			IGenericInlineConnector genericInlineConnector = inlineConnectorIterator.next();
			if (genericInlineConnector instanceof IInlinePlugConnector) {
				inlinePlugConnector = (IInlinePlugConnector) genericInlineConnector;
			}
			IInlineJackConnector inlineJackConnector = null;
			if (genericInlineConnector instanceof IInlineJackConnector) {
				inlineJackConnector = (IInlineJackConnector) genericInlineConnector;
			}
			genericInlineConnector = inlineConnectorIterator.next();
			if (genericInlineConnector instanceof IInlinePlugConnector) {
				inlinePlugConnector = (IInlinePlugConnector) genericInlineConnector;
			}
			if (genericInlineConnector instanceof IInlineJackConnector) {
				inlineJackConnector = (IInlineJackConnector) genericInlineConnector;
			}

			if (inlinePlugConnector == null || inlineJackConnector == null) {
				return null;
			}
			if (!inlinePlugConnector.getMates().contains(inlineJackConnector)) {
				return null;
			}
			//inlinePlugConnector.addMate();
			//inlinePlugConnector.getMate()
			return new Pair<>(inlinePlugConnector, inlineJackConnector);
		}
		return null;
	}

	protected static class ConvertInlineToPlugJackConverter
	{

		private IInlinePlugConnector inlinePlugConnector;
		private IInlineJackConnector inlineJackConnector;

		ConvertInlineToPlugJackConverter(IInlinePlugConnector inlinePlug, IInlineJackConnector inlineJack)
		{
			inlinePlugConnector = inlinePlug;
			inlineJackConnector = inlineJack;
		}

		@Nullable String convertToPlugJackPair()
		{
			if (inlinePlugConnector.getSharedObject() != null && inlineJackConnector.getSharedObject() != null) {
				ConvertSharedInlineIntoPlugJack convertSharedInlineIntoPlugJack =
						createConvertSharedInlineIntoPlugJack(inlinePlugConnector, inlineJackConnector);

				String error = convertSharedInlineIntoPlugJack.convert();

				return error;
			}
			if (inlinePlugConnector.getSharedObject() == null &&
					inlineJackConnector.getSharedObject() == null) {
				DesignUsages designUsages =
						new DesignUsages(inlinePlugConnector.getLogicDesign(), inlinePlugConnector.getUID());

				designUsages.convertInlineConnectorIntoPlugJackPair(inlinePlugConnector, inlineJackConnector, true);

				return null;
			}
			return null;
		}

		@NotNull protected ConvertSharedInlineIntoPlugJack createConvertSharedInlineIntoPlugJack(
				@NotNull IInlinePlugConnector inlinePlug, @NotNull IInlineJackConnector inlineJack)
		{
			return new ConvertSharedInlineIntoPlugJack(
					(ISharedConnector) Objects.requireNonNull(inlinePlug.getSharedObject()),
					(ISharedConnector) Objects.requireNonNull(inlineJack.getSharedObject()));
		}
	}

	private static class DesignUsages implements AutoCloseable
	{

		private IUID cableUID;
		private ILogicDesign design;
		private boolean isDesignPreLocked;
		private boolean isDesignPreLoaded;
		private Collection<IUID> diagramUIDs = new LinkedHashSet<>(2);

		DesignUsages(ILogicDesign designUID, IUID cableUID)
		{
			this.cableUID = cableUID;
			design = designUID;
			isDesignPreLocked = design.isLocked();
			isDesignPreLoaded = design.isLoadedInMemory();
		}

		void addDiagramUsage(IUID diagramUID)
		{
			diagramUIDs.add(diagramUID);
		}

		boolean lockDesign()
		{
			if (!design.isLocked()) {
				return design.lock();
			}
			return true;
		}

		void refreshDiagrams()
		{
			for (IUID aDiagram : diagramUIDs) {
				ISchemDiagram diagram = UIDMgr.getObjectOfType(aDiagram, ISchemDiagram.class);

				if (diagram.isLoadedInMemory()) {
					for (IDiagramObject iDiagramObject : diagram.getRepresentations(cableUID)) {
						if (iDiagramObject instanceof IPinList) {
							IPinList schmePinlist = (IPinList) iDiagramObject;
							boolean schemPinlistHasPins = schmePinlist.getPins().stream()
									.anyMatch(pin -> !(pin.getConnectivity() instanceof IBackshellTermination));
							if (!schemPinlistHasPins) {
								((IPinList) iDiagramObject).getAttachedPinListObjects().forEach(anAttached -> {
									anAttached.removeAttachedObject(schmePinlist);
									schmePinlist.removeAttachedObject(anAttached);
								});
							}
						}
					}

					diagram.refreshRepresentations();
				}
			}
		}

		private Pair<IConnector, IConnector> convertInlineConnectorIntoPlugJackPair(IInlinePlugConnector inlinePlugHalf,
				IInlineJackConnector inlineJackHalf, boolean refreshDWO)

		{
			if (refreshDWO) {
				diagramUIDs = design.getDesignWideUsageMgr().getUsageDiagrams(inlinePlugHalf).stream()
						.map(aDiagram -> aDiagram.getUID()).collect(
								Collectors.toList());
			}
			IInlinePlugConnector fromInlinePlug = null;
			if (inlinePlugHalf instanceof IUndoable) {
				IUndoableObject undoableObject = ((IUndoable) inlinePlugHalf).snapshot();
				if (undoableObject instanceof IInlinePlugConnector) {
					fromInlinePlug = (IInlinePlugConnector) undoableObject;
				}
			}
			IInlineJackConnector fromInlineJack = null;
			if (inlinePlugHalf instanceof IUndoable) {
				IUndoableObject undoableObject = ((IUndoable) inlineJackHalf).snapshot();
				if (undoableObject instanceof IInlineJackConnector) {
					fromInlineJack = (IInlineJackConnector) undoableObject;
				}
			}
			BiConsumer<IConnector, IConnector> updateAttributes = (inline, connector) -> {

				if (inline.getSharedPinList() == null) {
					AttributeUtils.copyAttributes(inline, connector, null);
					inline.getProperties()
							.forEach(
									aProp -> connector
											.addProperty(FactoryMgr.getCommonFactory().constructProperty(aProp, connector)));
					connector.assignLibraryPart(CommonUtils.cast(inline.getLibraryObject(), ILibraryObject.class));
					addPositionsOnCableConnector(connector);
				}
			};

			IConnector plugConnector =
					LogicInvalidStateScrubber.convertInlineNonInline(inlinePlugHalf, fromInlinePlug, updateAttributes);

			IConnector jackConnector =
					LogicInvalidStateScrubber.convertInlineNonInline(inlineJackHalf, fromInlineJack, updateAttributes);

			refreshDiagrams();

			List<IUID> objectsToRemove = Arrays.asList(inlinePlugHalf.getUID(), inlineJackHalf.getUID());
			IUndoableContainer ucontainer = CAFUtils.getInstance().getCurrentUndoableContainer();
			if (ucontainer != null && ucontainer.getCurrentEdit() != null) {
				objectsToRemove
						.forEach(item -> ucontainer.getCurrentEdit().removeObjectFromUndo(item));
			}
			return new Pair<>(plugConnector, jackConnector);
		}

		boolean convertSharedInlineInstanceToPlugJack()
		{
			if (isDesignPreLoaded) {
				try (ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges ignored = new ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges(
						getControllerForDesign(design))) {
					IInlinePlugConnector inlinePlugHalf = UIDMgr.getObjectOfType(cableUID, IInlinePlugConnector.class);
					IInlineJackConnector inlineJackHalf =
							(IInlineJackConnector) inlinePlugHalf.getMates().iterator().next();

					convertInlineConnectorIntoPlugJackPair(inlinePlugHalf, inlineJackHalf, false);

					Consumer<ILogicDesign> saveHandler = ForeignDesignChangesHandler.createdSaveHandler();

					saveHandler.accept(design);
				}
			}
			return true;
		}

		@Override public void close()
		{

			if (!isDesignPreLoaded) {
				((IIncLoadable) design).setSkeleton(true);
			}
			if (!isDesignPreLocked) {
				design.unlock();
			}
		}

		private void addPositionsOnCableConnector(IConnector cableConnector)
		{
			ILibraryConnector libraryObject =
					CommonUtils.cast(cableConnector.getLibraryObject(), ILibraryConnector.class);
			ISharedConnector sharedConnector =
					CommonUtils.cast(cableConnector.getSharedPinList(), ISharedConnector.class);
			if (libraryObject != null && sharedConnector == null) {

				LibraryHelper.getPositionNames(libraryObject).forEach(aName -> {

					IInternalPosition internalPosition =
							FactoryMgr.getCableFactory().createInternalPosition(FactoryMgr.createUID());
					internalPosition.setName(aName);
					cableConnector.addPosition(internalPosition);
				});
			}
		}

		public ILogicDesign getDesign()
		{
			return design;
		}

		public boolean isEditable()
		{
			return !CAFUtils.getInstance().isDesignOpenReadOnly(design);
		}
	}

	@Nullable protected static ICapletController getControllerForDesign(ILogicDesign thisdesign)
	{
		return CAFUtils.getInstance().getControllerForDesign(thisdesign);
	}

	protected static class ConvertSharedInlineIntoPlugJack
	{

		@NotNull private ISharedConnector sharedPlug;
		@NotNull private ISharedConnector sharedJack;

		private Map<ILogicDesign, DesignUsages> designUsagesOfSharedInline = new LinkedHashMap<>();
		private IProject project;

		ConvertSharedInlineIntoPlugJack(@NotNull ISharedConnector sharedPlug, @NotNull ISharedConnector sharedJack)
		{
			this.sharedPlug = sharedPlug;
			this.sharedJack = sharedJack;
			project = sharedPlug.getProject();
		}

		@Nullable String convert()
		{

			IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
			boolean success = false;
			try {
				if (sharedPlug.isFrozen() || sharedJack.isFrozen()) {
					return ResourceMgr.getString(ConvertInlineToPlugJackPairAction.class,
							"ConvertInlineToPlugJackPairAction.failed.cannotedit", sharedPlug.getName(),
							sharedJack.getName());
				}
				if (!sharedJack.lock() || !sharedPlug.lock()) {
					return ResourceMgr.getString(ConvertInlineToPlugJackPairAction.class,
							"ConvertInlineToPlugJackPairAction.failed.cannotedit", sharedPlug.getName(),
							sharedJack.getName());
				}

				if (!sharedJack.isEditable() || !sharedPlug.isEditable()) {
					return ResourceMgr.getString(ConvertInlineToPlugJackPairAction.class,
							"ConvertInlineToPlugJackPairAction.failed.nodomainaccess", sharedPlug.getName(),
							sharedJack.getName());
				}

//				IProjectSharedUsageView suView = project.getSharedUsageView();
//				ISharedUsageInfo sharedUsageInfo = suView.getSharedUsageInfo(sharedPlug);
//				Collection<ISharedUsage> viewusages = sharedUsageInfo.getUsages();

				IUserSession userSession = UtilsHelper.getCHSSystem().getUserSession();

				//check transient usages
				ObjectUsageInfo[] usages =
						userSession.retrieveSharedPinListUsageInfo(new String[]{sharedPlug.getUID().getString()});

				for (ObjectUsageInfo sharedUsage : usages) {
					IUID connectivityUID = FactoryMgr.getCommonFactory().getUID(sharedUsage.connectivityObjectUID);
					IUID designUid = FactoryMgr.getCommonFactory().getUID(sharedUsage.designUID);
					ILogicDesign logicDesign = project.getLoadedDesignMgr().getAbstractLogicDesign(designUid);
					/**
					 * Skips shared usages where the connector UID does not resolve to a valid
					 * {@link IInlinePlugConnector}. This prevents issues during conversion
					 * when stale or deleted inline instances still exist in shared usage metadata.
					 */
					IInlinePlugConnector plug = UIDMgr.getObjectOfType(connectivityUID, IInlinePlugConnector.class);
                    if (plug == null) {
						continue;
					}
					evaluateDesignUsages(sharedUsage, logicDesign, connectivityUID);
				}
				for (DesignUsages designUsages : designUsagesOfSharedInline.values()) {
					if (!designUsages.isEditable()) {

						return ResourceMgr.getString(ConvertInlineToPlugJackPairAction.class,
								"ConvertInlineToPlugJackPairAction.failed.designnoteditable",
								designUsages.getDesign().getName(), sharedPlug.getName(), sharedJack.getName());
					}
				}
				for (DesignUsages designUsages : designUsagesOfSharedInline.values()) {
					if (!designUsages.lockDesign()) {
						return ResourceMgr.getString(ConvertInlineToPlugJackPairAction.class,
								"ConvertInlineToPlugJackPairAction.failed.designnoteditable",
								designUsages.getDesign().getName(), sharedPlug.getName(), sharedJack.getName());
					}
				}

				btm.enterTransactionBoundary(this, IBoundaryTransactionMarshaller.Nesting.NESTED);

				ISharedPinListMgr sharedPinListMgr = sharedPlug.getProject().getSharedPinListMgr();
				SharedObjectRevisionHelper.lockChildrenForChangingRevision(sharedPinListMgr, sharedPlug);
				SharedObjectRevisionHelper.lockChildrenForChangingRevision(sharedPinListMgr, sharedJack);

				SharedObjectInvalidStateScrubber.convertInlineToNormalConnector(sharedPlug, (change) -> {
				});

				addPositionsOnShared(sharedPlug);
				addPositionsOnShared(sharedJack);
				SharedObjectRevisionHelper
						.moveChildrenToParent(sharedPinListMgr, sharedPlug, true);
				SharedObjectRevisionHelper
						.moveChildrenToParent(sharedPinListMgr, sharedJack, true);
				sharedPlug.setParentId(null);
				sharedJack.setParentId(null);

				for (DesignUsages designUsages : designUsagesOfSharedInline.values()) {

					designUsages.convertSharedInlineInstanceToPlugJack();
				}
				success = true;

				return null;
			}
			catch (UserSessionException e) {
				return ResourceMgr.getString(ConvertInlineToPlugJackPairAction.class,
						"ConvertInlineToPlugJackPairAction.failed.cannotfindsharedinstances", sharedPlug.getName(),
						sharedJack.getName());
			}
			catch (LockException e) {
				return ResourceMgr.getString(ConvertInlineToPlugJackPairAction.class,
						"ConvertInlineToPlugJackPairAction.failed.cannotedit", sharedPlug.getName(),
						sharedJack.getName());
			}
			finally {
				try {
					if (success) {
						sharedPlug.flush();
						sharedJack.flush();
					}
				}
				finally {
					btm.exitTransactionBoundary(this, success);
					designUsagesOfSharedInline.values().forEach(anUsage -> anUsage.close());
					sharedPlug.unlock();
					sharedJack.unlock();
				}
			}
		}

		protected void evaluateDesignUsages(@NotNull ObjectUsageInfo sharedUsage, @NotNull ILogicDesign logicDesign,
				@NotNull IUID connectivityUID)
		{
			DesignUsages designUsages = designUsagesOfSharedInline
					.computeIfAbsent(logicDesign, design -> new DesignUsages(logicDesign, connectivityUID));

			if (!StringUtils.isBlank(sharedUsage.diagramUID)) {
				designUsages
						.addDiagramUsage(FactoryMgr.getCommonFactory().constructUID(sharedUsage.diagramUID));
			}
		}

		private void addPositionsOnShared(ISharedConnector sharedConnector)
		{
			ILibraryConnector libraryObject =
					CommonUtils.cast(sharedConnector.getLibraryObject(), ILibraryConnector.class);
			if (libraryObject != null) {

				LibraryHelper.getPositionNames(libraryObject).forEach(aName -> {

					ISharedInternalPosition position =
							FactoryMgr.getSharedFactory().createSharedInternalPosition(FactoryMgr.createUID());
					position.setName(aName);
					sharedConnector.addPosition(position);
				});
			}
		}
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{

		if (getSelectedInline() != null) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@Override public String getActionUIClass()
	{
		return ConvertInlineToPlugJackPairActionUI.class.getName();
	}
}
