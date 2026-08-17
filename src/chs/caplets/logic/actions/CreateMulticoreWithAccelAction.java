package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IUndoableObject;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.MulticoreEditPanel;
import chs.caf.caplet.helpers.SharedMulticoreTracker;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.Model;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cofUtils.logical.concurrency.IDiagramRepresentationUpdateStrategy;
import chs.cofUtils.logical.concurrency.LogicConcurrencyFactory;
import chs.cofUtils.logical.concurrency.LogicConcurrentEditReporter;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IDesignContainer;
import chs.common.INamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utility.AssemblyUtils;
import chs.utility.DiagramHelper;
import chs.utility.Placement;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CreateMulticoreWithAccelAction extends ControllerActionRT
{

	private Model m_model;

	@NotNull private Set<IMulticore> siblingLevelMulticores = new HashSet<>();
	@NotNull private Set<IConductor> siblingLevelConductors = new HashSet<>();
	private CreateMulticoreContext context;
	@NotNull private final SharedMulticoreTracker tracker = new SharedMulticoreTracker();

	protected CreateMulticoreWithAccelAction(@NotNull ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
	}

	public abstract String getSheathType();

	public void setEditTypeForContext(CreateMulticoreContext createContext)
	{
		createContext.setEditType(COFTypeEnum.Multicore);
	}

//	@Override protected IActionEnum onActivate(ActionEvent e)
//	{
//		return checkifHasValidSelections();
//	}

	private boolean checkifHasValidSelections()
	{
		context =
				new CreateMulticoreContext(m_model.getDesign(), m_model.getDiagram());
		setEditTypeForContext(context);
		m_model.getDesign().refresh();

		Map<String, Set<ILogicObject>> selectedLogicObjects = getLogicObjectsInSelection();
		if (selectedLogicObjects.isEmpty()) {
			return false;
		}
		if (selectedLogicObjects.size() > 1) {
			logSharedAndNonObjsSelectedError(selectedLogicObjects.get("shared"), selectedLogicObjects.get("nonshared"));
			return false;
		}
		Collection<ILogicObject> selectedInnerCores = selectedLogicObjects.get("shared");
		if (selectedInnerCores != null) {
			if (ActionRT.isDesignUnderConcurrentEdit()) {
				logSharedObjectsSelectedInMU(selectedLogicObjects.get("shared"));
				return false;
			}
			context.setEditScope(MulticoreEditPanel.SHARED_SCOPE);
		}
		if (!CreateMulticoreActionHelper.refreshManager(context)) {
			return false;
		}
		if (selectedInnerCores == null) {
			selectedInnerCores = selectedLogicObjects.get("nonshared");
		}
		Map<String, List<ILogicObject>> segregatedByAssemblies = findAssemblies(selectedInnerCores.stream());
		Map<String, List<ILogicObject>> nonNullAssemblies = segregatedByAssemblies.entrySet().stream()
				.filter(entry -> !"null".equals(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		if (!nonNullAssemblies.isEmpty()) {

			logObjectsSelectedFromAssemblies(nonNullAssemblies);
			return false;
		}
		populateSiblingLevelConductorsAndMulticoreSelections(selectedInnerCores);

		boolean noSharedRelatedError = !context.isSharedEditScope() || reportAnySharedRelatedErrorsInSelection();

		return noSharedRelatedError;
	}

	private Map<String, Set<ILogicObject>> getLogicObjectsInSelection()
	{
		return getSelections().streamSelectedObjectsOfType(IUIDObject.class)
				.map(uidObj -> reduceToRequiredLogicObject(uidObj))
				.filter(aObj -> aObj != null).collect(Collectors.groupingBy(aObj -> {
					return aObj.getSharedObject() != null ? "shared" : "nonshared";
				}, Collectors.toSet()));
	}

	@Nullable private ILogicObject reduceToRequiredLogicObject(IUIDObject iuidObject)
	{
		ILogicObject aObj = ReferenceHelper.reduceToLogicObject(iuidObject);
		if (aObj instanceof IMulticore || aObj instanceof INetConductor ||
				aObj instanceof IWireConductor) {
			return aObj;
		}
		if (aObj instanceof IShieldBody) {
			return ((IShieldBody) aObj).getMulticore();
		}
		return null;
	}

	private void populateSiblingLevelConductorsAndMulticoreSelections(Collection<ILogicObject> selectedLogicObjects)
	{

		Set<IMulticore> selectedMulticores = selectedLogicObjects.stream().filter(log -> log instanceof IMulticore)
				.map(log -> (IMulticore) log)
				.map(mult -> mult.getRootMulticore())
				.collect(Collectors.toSet());
		siblingLevelConductors = selectedLogicObjects.stream()
				.filter(con -> con instanceof INetConductor || con instanceof IWireConductor)
				.map(con -> (IConductor) con)
				.filter(con -> addMulticoreIfnotalreadyAdded(selectedMulticores, con.getMulticore()))
				.filter(con -> con.getMulticore() == null)
				.collect(Collectors.toSet());
		//we are always adding the root level multicore into the selection when selecting an innercore multicore or innercore conductor.
		siblingLevelMulticores = new HashSet<>(selectedMulticores);

		//noinspection MapReplaceableByEnumMap
		Map<CreateMulticoreContext.MCAcceptenceResult, List<ILogicObject>> filteredOutObjects = new HashMap<>();
		siblingLevelConductors =
				siblingLevelConductors.stream().filter(aCond -> {
							CreateMulticoreContext.MCAcceptenceResult isAcceptable = context.accept(aCond, true);
							if (!CreateMulticoreContext.MCAcceptenceResult.ACCEPTABLE.equals(isAcceptable)) {
								List<ILogicObject> logicObjects =
										filteredOutObjects.computeIfAbsent(isAcceptable, it -> new ArrayList<>());

								logicObjects.add(aCond);
							}
							return CreateMulticoreContext.MCAcceptenceResult.ACCEPTABLE.equals(isAcceptable);
						})
						.collect(Collectors.toSet());
		siblingLevelMulticores =
				siblingLevelMulticores.stream().filter(aMc ->
						{
							CreateMulticoreContext.MCAcceptenceResult isAcceptable = context.accept(aMc);
							if (!CreateMulticoreContext.MCAcceptenceResult.ACCEPTABLE.equals(isAcceptable)) {
								List<ILogicObject> filteredLojObjs =
										filteredOutObjects.computeIfAbsent(isAcceptable, it -> new ArrayList<>());
								filteredLojObjs.add(aMc);
							}

							return CreateMulticoreContext.MCAcceptenceResult.ACCEPTABLE.equals(isAcceptable);
						}
				).collect(Collectors.toSet());
		if (!filteredOutObjects.isEmpty()) {
			logFilteredOutObjects(filteredOutObjects);
		}
	}

	private SelectSet getSelections()
	{
		ISelectMgr selectMgr = CAFUtils.getInstance().getActiveSelectMgr();
		assert selectMgr != null;
		return selectMgr.getCurrentSelections();
	}

	private Map<String, List<ILogicObject>> findAssemblies(Stream<ILogicObject> stream)
	{
		return stream.collect(Collectors
				.groupingBy(lobj -> {
							IAssembly assembly = AssemblyUtils.getAssembly(lobj);
							return assembly != null ? assembly.getUID().getString() : "null";
						},
						Collectors.toList()));
	}

	private Set<ILogicObject> findApplicableSharedObjectsForMCCreation()
	{
		return Stream.concat(siblingLevelConductors.stream(),
						siblingLevelMulticores.stream())
				.filter(lobj -> lobj.getSharedObject() != null)
				.collect(Collectors.toSet());
	}

	private boolean reportAnySharedRelatedErrorsInSelection()
	{

		Set<ILogicObject> sharedSelectedObjs = findApplicableSharedObjectsForMCCreation();

		Set<ISharedObject> lockedObjs = new HashSet<>();
		Set<ISharedObject> lockFailObjs = new HashSet<>();

		List<ISharedLockableUpdateableObject> objectsToLock = tracker.getLockableUpdateableObjects(sharedSelectedObjs);

		BatchLockRefreshHelper.batchLockWithPromise(objectsToLock,
				() -> lockSharedObjects(sharedSelectedObjs, lockedObjs, lockFailObjs));

		return lockedObjs.size() == sharedSelectedObjs.size();
	}

	private void lockSharedObjects(Set<ILogicObject> sharedSelectedObjs, Set<ISharedObject> lockedObjs,
			Set<ISharedObject> lockFailObjs)
	{
		for (ILogicObject obj : sharedSelectedObjs) {
			ISharedObject shared = obj.getSharedObject();
			assert shared != null;
			if (tracker.acquireLock(shared)) {
				lockedObjs.add(shared);
			}
			else {
				lockFailObjs.add(shared);
			}
		}
		logIfLockFailForSharedObjects(lockFailObjs);
	}

	private void logFilteredOutObjects(
			Map<CreateMulticoreContext.MCAcceptenceResult, List<ILogicObject>> filteredOutObjects)
	{
		StringJoiner message = new StringJoiner(",");
		for (CreateMulticoreContext.MCAcceptenceResult result : filteredOutObjects.keySet()) {
			List<ILogicObject> filteredOut = filteredOutObjects.get(result);
			//noinspection EnumSwitchStatementWhichMissesCases
			switch (result) {
				case FROZENSHARED: {

					message.add(ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
							"CreateMulticoreWithAccelAction.Output.frozensharedfailure", editTypeString(),
							createMessageDisplayForObjects(filteredOut)));

					break;
				}
				case OVERBRAIDCHILD: {
					message.add(ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
							"CreateMulticoreWithAccelAction.Output.overbraidchildfailure",
							createMessageDisplayForObjects(getWireFilteredObjects(filteredOut))));
					break;
				}
				default: {
					message.add(ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
							"CreateMulticoreWithAccelAction.Output.UnknownError", editTypeString()));
					break;
				}
			}
		}

		getOutputWindow().sendApplicationMessage(getActionDescription() + HTMLHelper.color("red", message.toString()));
	}

	private String getActionDescription()
	{
		return HTMLHelper.bold(String.valueOf(getActionUI().getValue(Action.NAME)) + ": ");
	}

	private void logObjectsSelectedFromAssemblies(Map<String, List<ILogicObject>> segregatedByAssemblies)
	{
		StringJoiner message = new StringJoiner(",");
		for (String key : segregatedByAssemblies.keySet()) {
			StringJoiner assemblyChildren = createMessageDisplayForObjects(segregatedByAssemblies.get(key));
			message.add(assemblyChildren.toString());
		}
		getOutputWindow().sendApplicationMessage(getActionDescription() + HTMLHelper.color("red"
				, ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
						"CreateMulticoreWithAccelAction.Output.differentassemblies", editTypeString(),
						message.toString()))
		);
	}

	private void logIfLockFailForSharedObjects(Set<ISharedObject> lockFailObjs)
	{
		if (!lockFailObjs.isEmpty()) {

			StringJoiner stringJoiner = createMessageDisplayForObjects(lockFailObjs);
			IOutputWindow outputWindow = getOutputWindow();
			outputWindow.sendApplicationMessage(getActionDescription() + HTMLHelper.color("red"
					, ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
							"CreateMulticoreWithAccelAction.Output.lockFail", stringJoiner.toString()))
			);
		}
	}

	protected void logSharedAndNonObjsSelectedError(Collection<ILogicObject> sharedSelected,
			Collection<ILogicObject> nonSharedSelected)
	{
		StringJoiner selectedSharedNames = createMessageDisplayForObjects(sharedSelected);
		StringJoiner selectedNonSharedNames = createMessageDisplayForObjects(nonSharedSelected);

		IOutputWindow outputWindow = getOutputWindow();
		outputWindow.sendApplicationMessage(getActionDescription() + HTMLHelper.color("red"
				, ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
						"CreateMulticoreWithAccelAction.Output.Message", editTypeString(),
						selectedSharedNames.toString(),
						selectedNonSharedNames.toString()))
		);
	}

	private void logSharedObjectsSelectedInMU(Collection<ILogicObject> sharedSelected)
	{
		StringJoiner selectedSharedNames = createMessageDisplayForObjects(sharedSelected);
		IOutputWindow outputWindow = getOutputWindow();
		outputWindow.sendApplicationMessage(getActionDescription() + HTMLHelper.color("red"
				, ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
						"CreateMulticoreWithAccelAction.Output.MUModeSharedFailure", editTypeString(),
						selectedSharedNames.toString()))
		);
	}

	private String outputWindowTitle()
	{
		Action actionUI = getActionUI();
		return actionUI == null ? ResourceMgr
				.getString(CreateMulticoreWithAccelAction.class, "CreateMulticoreWithAccelAction.Output.title",
						editTypeString()) : String.valueOf(actionUI.getValue(Action.NAME));
	}

	private String editTypeString()
	{
		return ResourceMgr.getString(CreateMulticoreWithAccelAction.class,
				context.isMulticoreEditType() ? "CreateMulticoreWithAccelAction.Output.Multicore" :
						"CreateMulticoreWithAccelAction.Output.Overbraid");
	}

	private StringJoiner createMessageDisplayForObjects(Collection<? extends INamedObject> logicObjects)
	{
		IDesignContainer designContainer = CAFUtils.getInstance().getActiveDesignContainer();
		StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
		logicObjects.stream().limit(4).forEach(logObj -> {
			stringJoiner.add(designContainer != null ? HTMLHelper.link(designContainer, logObj) :
					logObj.getName());
		});

		if (logicObjects.size() > 4) {
			stringJoiner.add("...");
		}
		return stringJoiner;
	}

	private Collection<IConductor> getWireFilteredObjects(Collection<? extends INamedObject> logicObjects)
	{
		return getSelections().getSelectedObjects(chs.cof.logical.schem.IConductor.class)
				.stream()
				.map(obj -> obj.getConnectivity())
				.filter(conductor -> conductor.getRootMulticore() instanceof IOverbraid)
				.filter(conductor -> logicObjects.contains(conductor.getRootMulticore()))
				.collect(Collectors.toList());
	}

	private boolean addMulticoreIfnotalreadyAdded(Set<IMulticore> selectedMulticores,
			@Nullable IMulticore multicore)
	{
		if (multicore != null) {
			selectedMulticores.add(multicore.getRootMulticore());
			return false;
		}
		return true;
	}

	@Override
	public boolean onTerminate(@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") boolean allGood)
	{
		boolean successful;
		try {
			if (!isModelEditable()) {
				return false;
			}
			boolean success = allGood && checkifHasValidSelections();
			if (!success) {
				return false;
			}
			successful = doChanges();
		}
		finally {
			cleanUpPostAction();
		}
		return successful;
	}

	private void cleanUpPostAction()
	{
		if (context != null) {
			CreateMulticoreActionHelper.unlockManager(context);
		}
		tracker.releaseAllLocks();
		siblingLevelMulticores.clear();
		siblingLevelConductors.clear();
		getCurrentSelections().clear();
	}

	protected boolean doChanges()
	{
		Set<ILogicDesign> impactedDesigns = new HashSet<>();
		IUserSession userSession = UtilsHelper.getCHSSystem().getUserSession();
		boolean editStatus = false;
		try {
			userSession.startClientTransaction();
			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
			if (siblingLevelMulticores.size() + siblingLevelConductors.size() > 0) {
				if (lockSelectedObjects()) {
					Set<ISharedObject> sharedObjs = new HashSet<>();
					if (context.isSharedEditScope()) {
						pupulateSelectedSharedObjects(sharedObjs);
					}
					Set<ILogicDesign> designsToUpdate = new HashSet<>();
					if (context.lockImpactedDesigns(impactedDesigns, sharedObjs, designsToUpdate)) {
						Optional<IAssembly>
								assemblySearch =
								Stream.concat(siblingLevelMulticores.stream(), siblingLevelConductors.stream())
										.filter(alog -> alog.getAssembly() != null)
										.map(alog -> alog.getAssembly()).findFirst();
						IAssembly assembly = null;
						if (assemblySearch.isPresent()) {
							assembly = assemblySearch.get();
						}
						if (assembly != null && context.isSharedEditScope()) {
							return false;
						}
						IMulticore newMC = context.isMulticoreEditType() ? FactoryMgr.getCablePropertiedFactory()
								.createMulticore(FactoryMgr.getCommonFactory().createUID())
								: FactoryMgr.getCablePropertiedFactory()
								.createOverbraid(FactoryMgr.getCommonFactory().createUID());

						LogicObjectLockFinder.tryEdit(m_model.getDesign(), newMC);
						IConnectivity connectivity = m_model.getDesign().getConnectivity();
						assert connectivity != null;
						connectivity.addMulticore(newMC);
						Collection<IUndoableObject> newObjs = new ArrayList<>();

						IShieldBody shield = createShieldBodyForMulticore(newMC);
						newObjs.add((IUndoableObject) shield);
						Set<IRevisionedSharedObject> sharedCreations = new HashSet<>();
						if (context.isSharedEditScope()) {
							createSharedMulticoreAndShield(sharedObjs, newMC, sharedCreations);
						}
						if (isShieldedIndicator()) {
							context.constructShieldConductorIfNeeded(newMC, newMC.getSharedMulticore(), newObjs,
									sharedCreations);
						}
						if (assembly != null) {
							removeSiblingsFromAssembly(assembly);
							assembly.addElement(newMC);
						}
						Set<IMulticore> modifiedMC = addSiblingsToNewMC(newMC);
						context.propagateHarnessAttribute(Collections.singleton(newMC),
								newMC.getSharedMulticore() != null ? Collections.singleton(newMC.getSharedMulticore()) :
										Collections.emptySet());
						newMC.setSheathType(getSheathGroupType());
						newObjs.add((IUndoableObject) newMC);
						newObjs.stream().forEach(obj -> cdh.addCreationObject(obj));
						processDesignsAndSharedObjects(designsToUpdate, modifiedMC, sharedObjs, sharedCreations);
						userSession.commitClientTransaction();
						editStatus = true;
					}
				}
			}
		}
		catch (CreateMulticoreAction.SharedObjectLockException ignored) {

		}
		catch (UserSessionException sessionException) {
			Environment.getExceptionDisplay().displayException(sessionException, "CreateMulticoreAction failed");
		}
		finally {
			context.releaseDesignLocks(impactedDesigns);
			if (!editStatus && userSession != null) {
				userSession.rollbackClientTransaction();
			}
		}
		return editStatus;
	}

	private void removeSiblingsFromAssembly(IAssembly assembly)
	{
		if (context.isMulticoreEditType()) {
			siblingLevelConductors.stream().forEach(mcContent -> assembly.removeElement(mcContent));
			siblingLevelMulticores.stream().forEach(mcContent -> assembly.removeElement(mcContent));
		}
	}

	@NotNull private Set<IMulticore> addSiblingsToNewMC(IMulticore newMC)
	{
		siblingLevelMulticores.stream()
				.forEach(m -> newMC.addMulticore(m));
		siblingLevelConductors.stream()
				.forEach(c -> newMC.addConductor(c));
		Set<IMulticore> modifiedMC = new LinkedHashSet<>();
		siblingLevelMulticores.stream()
				.forEach(m -> modifiedMC.add(m));
		modifiedMC.add(newMC);
		return modifiedMC;
	}

	private void pupulateSelectedSharedObjects(Set<ISharedObject> sharedObjs)
	{
		sharedObjs.addAll(siblingLevelConductors.stream().map(c -> c.getSharedConductor())
				.collect(Collectors.toSet()));
		sharedObjs.addAll(siblingLevelMulticores.stream().map(m -> m.getSharedMulticore())
				.collect(Collectors.toSet()));
	}

	@NotNull private IShieldBody createShieldBodyForMulticore(IMulticore newMC)
	{
		IShieldBody shield =
				FactoryMgr.getCablePropertiedFactory().createShieldBody(FactoryMgr.createUID(), newMC);
		shield.setMulticore(newMC);
		shield.setType(getSheathType());
		return shield;
	}

	private void processDesignsAndSharedObjects(@NotNull Set<ILogicDesign> otherDesignsToUpdate,
			@NotNull Set<IMulticore> modifiedMCs, @NotNull Set<ISharedObject> sharedObjs,
			@NotNull Set<IRevisionedSharedObject> sharedCreations) throws UserSessionException
	{
		Set<ISharedMulticore> sharedMCs = new HashSet<>();
		for (IMulticore mc : modifiedMCs) {
			ISharedMulticore sharedMulticore = mc.getSharedMulticore();
			if (sharedMulticore != null) {
				sharedMCs.add(sharedMulticore);
			}
		}
		Consumer<ILogicDesign> currDesignSchematicUpdate =
				designToUpdate -> regenerateIndicators(designToUpdate, modifiedMCs);
		Consumer<ILogicDesign> otherDesignSchematicUpdate =
				designToUpdate -> regenerateSharedIndicators(designToUpdate, sharedMCs);
		context.saveSharedObjectChanges(sharedObjs, SetMap.emptySetMap(), sharedCreations, Collections.emptySet());
		context.updateImpactedDesigns(m_model.getDesign(), otherDesignsToUpdate, currDesignSchematicUpdate,
				otherDesignSchematicUpdate);
	}

	private void regenerateSharedIndicators(@NotNull ILogicDesign logicDesign, @NotNull Set<ISharedMulticore> sharedMCs)
	{
		if (!sharedMCs.isEmpty()) {
			Set<IMulticore> multicores = new HashSet<>();
			IConnectivity connectivity = logicDesign.getConnectivity();
			if (connectivity != null) {
				for (IMulticore multicore : connectivity.getMulticores(true, false)) {
					ISharedMulticore sharedMulticore = multicore.getSharedMulticore();
					if (sharedMulticore != null && sharedMCs.contains(sharedMulticore)) {
						multicores.add(multicore);
					}
				}
			}
			regenerateIndicators(logicDesign, multicores);
		}
	}

	private void regenerateIndicators(@NotNull ILogicDesign logicDesign, @NotNull Set<IMulticore> multicores)
	{
		Generator generator = Generator.getGenerator();
		IDiagramRepresentationUpdateStrategy updateStrategy = LogicConcurrencyFactory.getInstance()
				.geDefaultDiagramRepresentationUpdateStrategy(logicDesign);
		for (IMulticore multicore : multicores) {
			updateStrategy.getDiagramProcessor(Collections.singleton(multicore)).processDiagrams(diagram -> {
				GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
				Placement.placeIndicators(generator, diagram, multicore, multicore.getShieldBody(),
						gp, true, null, true, false);
			});
		}
	}

	private void createSharedMulticoreAndShield(Set<ISharedObject> sharedObjs, IMulticore newMC,
			Set<IRevisionedSharedObject> sharedCreations)
	{
		ISharedMulticore smc = context.createSharedMulticore(sharedCreations, newMC);
		Set<IRevisionedSharedObject> lockedSharedObjs = new HashSet<>();
		try {
			sharedObjs.stream().forEach(sobj -> context.lockChildRevisions(lockedSharedObjs,
					(IRevisionedSharedObject) sobj, smc));
			if (context.isMulticoreEditType()) {
				context.getSharedConductorMgr().addSharedMulticore(smc);
			}
			else {
				context.getSharedConductorMgr().addSharedOverbraid((ISharedOverbraid) smc);
			}
			siblingLevelMulticores.stream().map(m -> m.getSharedMulticore())
					.forEach(sm -> {
						smc.addMulticore(sm);
						context.removeChildRevisionStrucAndUpdateParentValue(sm, smc);
					});
			siblingLevelConductors.stream().map(c -> c.getSharedConductor())
					.forEach(sc -> {
						smc.addConductor(sc);
						context.removeChildRevisionStrucAndUpdateParentValue(sc, smc);
					});
			smc.addIndicator(getSheathType());
		}
		finally {
			lockedSharedObjs.stream().forEach(lockedObj -> lockedObj.unlock());
		}
	}

	protected abstract boolean isShieldedIndicator();

	private boolean lockSelectedObjects()
	{
		Set<IUIDObject> tobeLocked = new LinkedHashSet<>();
		siblingLevelMulticores.stream()
				.forEach(m -> tobeLocked.addAll(CreateMulticoreActionHelper.getLogicObjectsToBeLockedForAMulticore(m)));
		tobeLocked.addAll(siblingLevelConductors);
		Collection<IUID> lockFailedObjects = LogicObjectLockFinder.tryEdit(m_model.getDesign(), tobeLocked);
		if (lockFailedObjects.isEmpty()) {
			return true;
		}

		CreateMulticoreActionHelper
				.reportLockFailure(m_model.getDesign(), COFTypeEnum.Multicore, new LogicConcurrentEditReporter(),
						lockFailedObjects);
		//CreateMulticoreActionHelper.releaseLocks(UIDUtils.convertToUIDSet(tobeLocked));
		return false;
	}

	protected abstract String getSheathGroupType();

	protected IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	@NotNull
	protected Set<IMulticore> getSiblingLevelMulticores()
	{
		return siblingLevelMulticores;
	}

	@NotNull
	protected Set<IConductor> getSiblingLevelConductors()
	{
		return siblingLevelConductors;
	}

	@NotNull
	protected SharedMulticoreTracker getSharedMCTracker()
	{
		return tracker;
	}
}
