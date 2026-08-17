package chs.caplets.logic.icd;

import chs.caf.CAFUtils;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDNetCableElement;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.common.IFlushable;
import chs.common.INamedPropertiedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.dataservices.CapitalProjectDataServices;
import chs.dataservices.LightWeightUsage;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utilities.SortedList;
import chs.utilities.StringUtils;
import chs.utility.ICDUtils;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.MulticoreUtils;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MulticoreBuilder
{

	@NotNull private IICDAssociatedSignal mAssociatedSignal;
	@NotNull private IConductor mConductor;
	private List<ICDMulticoreAdapter> mConnectedMulticores;
	private List<ICDMulticoreAdapter> mDesignMulticores;
	private PersistenceHandler mPersistenceHandler;

	public MulticoreBuilder(@NotNull IICDAssociatedSignal associatedSignal, @NotNull IConductor conductor,
			List<ICDMulticoreAdapter> connectedMulticores, List<ICDMulticoreAdapter> designMulticores,
			PersistenceHandler persistenceHandler)
	{
		mAssociatedSignal = associatedSignal;
		mConductor = conductor;
		mPersistenceHandler = persistenceHandler;
		mConnectedMulticores = new ArrayList<>(connectedMulticores);
		mDesignMulticores = new ArrayList<>(designMulticores);
	}

	@Nullable public Collection<IMulticore> createMulticore()
	{
		if (mConductor.getMulticore() != null) {
			return Collections.emptySet();
		}
		IConnectivity connectivity = mConductor.getConnectivity();

		List<ICDMulticore> icdMulticores = new ArrayList<>();
		ICDMulticoreAdapter reusableParent = findReusableMulticore(connectivity, mConnectedMulticores, icdMulticores);
		if (reusableParent != null) {
			return fillInCableHierarchy(connectivity, icdMulticores, reusableParent);
		}

		Collection<IMulticore> createdConnectedMulticores =
				createSharedMulticoreFromDesignMulticores(connectivity, mConnectedMulticores);
		if (createdConnectedMulticores != null) {
			return createdConnectedMulticores;
		}

		icdMulticores.clear();
		reusableParent = findReusableMulticore(connectivity, mDesignMulticores, icdMulticores);
		if (reusableParent != null) {
			return fillInCableHierarchy(connectivity, icdMulticores, reusableParent);
		}

		Collection<IMulticore> createdDesignMulticores =
				createSharedMulticoreFromDesignMulticores(connectivity, mDesignMulticores);
		if (createdDesignMulticores != null) {
			return createdDesignMulticores;
		}

		IProject project = connectivity.getProject();
		if (project != null) {
			ISharedConductorMgr sharedConductorMgr = project.getSharedConductorMgr();
			if (mPersistenceHandler.getLockTracker()
					.lockManager(project.getSharedConductorMgr(), mConductor.getName())) {
				Collection<ISharedMulticore> sharedMulticores =
						mPersistenceHandler.getSharedMulticores(connectivity, sharedConductorMgr, mConductor.getType());
				Set<ISharedConductor> sharedConductors =
						mPersistenceHandler.getSharedConductors(connectivity, sharedConductorMgr, mConductor.getType());
				Collection<IMulticore> createdMulticores =
						updateSharedMulticore(connectivity, sharedConductors, sharedMulticores);
				if (createdMulticores == null) {
					return fillInCableHierarchy(connectivity, icdMulticores, null);
				}
				else {
					return createdMulticores;
				}
			}
		}
		return Collections.emptySet();
	}

	@Nullable private Collection<IMulticore> createSharedMulticoreFromDesignMulticores(IConnectivity connectivity,
			List<ICDMulticoreAdapter> multicoresToSearch)
	{
		Set<ISharedMulticore> sharedMulticores = new HashSet<>();
		Set<ISharedConductor> sharedConductors = new HashSet<>();
		for (ICDMulticoreAdapter multicoreAdapter : multicoresToSearch) {
			IMulticore multicore = multicoreAdapter.getMulticore();
			ISharedMulticore sharedMulticore = multicore.getSharedMulticore();
			if (sharedMulticore != null) {
				ISharedMulticore rootMulticore = MulticoreUtils.getRootMulticore(sharedMulticore, true);
				if (rootMulticore != null) {
					collectAllMulticoresInHierarchy(rootMulticore, sharedMulticores);
					CollectionUtils.add(rootMulticore.getAllSharedConductorsInHierarchy(false), sharedConductors);
				}
			}
		}
		Collection<ISharedMulticore> filteredsharedMulticores =
				mPersistenceHandler.getSharedMulticores(sharedMulticores);
		return updateSharedMulticore(connectivity, sharedConductors, filteredsharedMulticores);
	}

	@Nullable private Collection<IMulticore> updateSharedMulticore(IConnectivity connectivity,
			Set<ISharedConductor> sharedConductors, Collection<ISharedMulticore> sharedMulticores)
	{
		Collection<IMulticore> createdMulticores = findAndUseSharedSignal(connectivity, sharedConductors);
		if (createdMulticores == null) {
			createdMulticores = createSharedMulticore(connectivity, sharedMulticores);
		}
		return createdMulticores;
	}

	@Nullable
	private Collection<IMulticore> findAndUseSharedSignal(IConnectivity connectivity,
			Set<ISharedConductor> sharedConductors)
	{
		if(sharedConductors.isEmpty())
		{
			return null;
		}
		Set<ISharedConductor> matchingSharedConductors =
				mPersistenceHandler.getMatchingSharedConductors(sharedConductors, mConductor, mAssociatedSignal);

		ILogicDesign logicDesign = mConductor.getLogicDesign();
		assert logicDesign != null;

		Set<ISharedConductor> reusableSharedConductors =
				filterOutRevisionsUsedInDesign(logicDesign, connectivity, matchingSharedConductors);

		Collection<IMulticore> createdMulticores = null;
		if (reusableSharedConductors.size() > 1) {
			Set<String> matchingCondutorNames = reusableSharedConductors.stream()
					.map(INamedPropertiedObject::getName)
					.collect(Collectors.toSet());
			String conductorNames = StringUtils.concatenate(matchingCondutorNames, ", ");
			String msg = ResourceMgr
					.getString(MulticoreBuilder.class, "MulticoreBuilder.multipleMatchingSignals.text",
							mConductor.getName(), conductorNames);
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
			createdMulticores = Collections.emptySet();
		}
		else if (reusableSharedConductors.size() == 1) {
			ISharedConductor sharedCond = reusableSharedConductors.iterator().next();
			if (sharedCond.getMulticore() != null) {
				mConductor.assignLibraryPart(null);
			}
			SharedConductorHelper.assignToShared(mConductor, sharedCond, logicDesign, null);
			IMulticore rootMulticore = MulticoreUtils.getRootMulticore(mConductor.getMulticore(), true);
			if (rootMulticore != null) {
				createdMulticores = rootMulticore.getAllMulticoresInHierarchy();
			}
			else {
				createdMulticores = Collections.emptySet();
			}
		}
		return createdMulticores;
	}

	@NotNull
	private Set<ISharedConductor> filterOutRevisionsUsedInDesign(ILogicDesign logicDesign, IConnectivity connectivity,
			Set<ISharedConductor> matchingSharedConductors)
	{
		IProject project = logicDesign.getProject();
		assert project != null;

		Set<ISharedConductor> reusableSharedConductors = new HashSet<>();
		for (ISharedConductor matchingSharedConductor : matchingSharedConductors) {
			boolean hasRevisionInDesign;
			ISharedMulticore multicore = matchingSharedConductor.getMulticore();
			if (multicore != null) {
				hasRevisionInDesign =
						isAnyMulticoreOtherRevisionUsedInDesigns(multicore, Collections.singletonList(logicDesign),
								project);
			}
			else {
				hasRevisionInDesign = connectivity.getConductors().stream()
						.filter(ILogicObject::isShared)
						.map(IConductor::getSharedConductor)
						.filter(sharedConductor -> !sharedConductor.getUID().isEquiv(matchingSharedConductor.getUID()))
						.map(IUIDObject::getBaseId)
						.anyMatch(iuid -> iuid.isEquiv(matchingSharedConductor.getBaseId()));
			}
			if (!hasRevisionInDesign) {
				reusableSharedConductors.add(matchingSharedConductor);
			}
		}
		return reusableSharedConductors;
	}

	@Nullable
	private Collection<IMulticore> createSharedMulticore(IConnectivity connectivity,
			Collection<ISharedMulticore> sharedMulticores)
	{
		if(sharedMulticores.isEmpty())
		{
			return null;
		}
		Collection<ISharedMulticore> multicoresToSearch = new ArrayList<>(sharedMulticores);
		if (mConductor.isShared()) {
			multicoresToSearch = sharedMulticores.stream()
					.filter(multi -> !ICDUtils.doesMulticoreHaveSignal(multi, mAssociatedSignal, mConductor.getType()))
					.collect(Collectors.toList());
		}

		if (mPersistenceHandler.isUpdate()) {
			ListSet<ISharedMulticore> reusableSharedParents =
					mPersistenceHandler.getMatchingSharedMulticores(multicoresToSearch, mAssociatedSignal);
			Collection<IMulticore> createdMulticores =
					addConductorToSharedMulticore(connectivity, reusableSharedParents);
			if (createdMulticores != null) {
				return createdMulticores;
			}
		}
		else {
			Set<List<IICDNetCableElement>> signalGroupPaths = getSignalGroupPaths(connectivity);
			for (List<IICDNetCableElement> signalGroupPath : signalGroupPaths) {
				for (IICDNetCableElement element : signalGroupPath) {
					Pair<String, String> cableNameTypePair = new Pair<>(element.getOriginalName(), element.getType());
					Set<ISharedMulticore> reusableSharedParents =
							mPersistenceHandler.getMatchingSharedMulticores(multicoresToSearch, cableNameTypePair);
					Collection<IMulticore> createdMulticores =
							addConductorToSharedMulticore(connectivity, reusableSharedParents);
					if (createdMulticores != null) {
						return createdMulticores;
					}
				}
			}
		}
		return null;
	}

	@Nullable private Collection<IMulticore> addConductorToSharedMulticore(IConnectivity connectivity,
			Set<ISharedMulticore> reusableSharedParents)
	{
		ILogicDesign logicDesign = CommonUtils.cast(connectivity.getDesign(), ILogicDesign.class);
		assert logicDesign != null;

		List<IDesignContainer> designsToCheck = new ArrayList<>();
		designsToCheck.add(logicDesign);

		IProject project = logicDesign.getProject();
		assert project != null;

		ISharedConductor sharedConductor = mConductor.getSharedConductor();
		if (sharedConductor != null) {
			CapitalProjectDataServices dataServices = CapitalProjectDataServices.getDataServices();
			List<LightWeightUsage> designUsages =
					dataServices.
							getDesignsWhereUsedOrUnPlacedBatch(Collections.singleton(sharedConductor),true);
			Collection<String> usedDesignIds = collectDesignUIDs(designUsages);
			designsToCheck.addAll(LogicUtils.loadDesignsFromUIDs(project.getDesignMgr(), usedDesignIds));
		}
		Set<ISharedMulticore> reusableSharedParentRevisions = new HashSet<>();
		for (ISharedMulticore reusableSharedParent : reusableSharedParents) {
			boolean hasRevisionInDesign =
					isAnyMulticoreOtherRevisionUsedInDesigns(reusableSharedParent, designsToCheck, project);
			if (!hasRevisionInDesign) {
				reusableSharedParentRevisions.add(reusableSharedParent);
			}
		}

		Collection<IMulticore> createdMulticores = null;
		if (reusableSharedParentRevisions.size() > 1) {
			Set<String> matchingCondutorNames = reusableSharedParentRevisions.stream()
					.map(INamedPropertiedObject::getName)
					.collect(Collectors.toSet());
			String conductorNames = StringUtils.concatenate(matchingCondutorNames, ", ");
			String msg = ResourceMgr
					.getString(MulticoreBuilder.class, "MulticoreBuilder.multipleMatchingMulticores.text",
							mConductor.getName(), conductorNames);
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
			createdMulticores = Collections.emptySet();
		}
		else if (reusableSharedParentRevisions.size() == 1) {
			createdMulticores =
					addConductorToSharedMulticore(connectivity, reusableSharedParentRevisions.iterator().next());
		}
		return createdMulticores;
	}

	@NotNull private Collection<String> collectDesignUIDs(@NotNull List<LightWeightUsage> designUsages)
	{
		return designUsages.stream()
				.map(LightWeightUsage::getDesignUID)
				.collect(Collectors.toSet());
	}

	private boolean isAnyMulticoreOtherRevisionUsedInDesigns(ISharedMulticore reusableSharedParent,
			List<IDesignContainer> designsToCheck, IProject project)
	{
		ISharedMulticore rootSharedMulticore = reusableSharedParent.getRootMulticore();
		Set<IRevisionedSharedObject> otherRevisionsOfReusableSharedMC =
				SharedObjectRevisionHelper.getOtherRevisionsOfSharedObject(rootSharedMulticore);
		boolean hasOtherRevisionInDesign = false;
		for (IRevisionedSharedObject revisionedSharedObject : otherRevisionsOfReusableSharedMC) {
			Set<String> usedDesignIds =
					CapitalProjectDataServices.getDataServices().getDesignsWhereUsedOrUnPlaced(project,
							revisionedSharedObject);
			Collection<IDesignContainer> designsUsingSharedMCRev =
					LogicUtils.loadDesignsFromUIDs(project.getDesignMgr(), usedDesignIds);
			if (CollectionUtils.containsAtLeastOne(designsToCheck, designsUsingSharedMCRev)) {
				hasOtherRevisionInDesign = true;
				break;
			}
		}
		return hasOtherRevisionInDesign;
	}

	@NotNull private Collection<IMulticore> addConductorToSharedMulticore(IConnectivity connectivity,
			ISharedMulticore sharedMulticore)
	{
		ISharedMulticore targetSharedMulticore = sharedMulticore;
		if (targetSharedMulticore == null) {
			return Collections.emptySet();
		}

		ISharedConductor sharedConductor = mConductor.getSharedConductor();
		if (sharedConductor != null) {
			if (!mPersistenceHandler.getLockTracker().lock(sharedConductor, true)) {
				return Collections.emptySet();
			}
		}

		IUID sharedMulticoreUID = targetSharedMulticore.getUID();
		if (!mPersistenceHandler.getLockTracker().lock(targetSharedMulticore, false, mConductor.getName())) {
			return Collections.emptySet();
		}

		targetSharedMulticore = UIDMgr.getObjectOfType(sharedMulticoreUID, ISharedMulticore.class);
		assert targetSharedMulticore != null;

		List<ICDMulticore> sharedIcdMulticores = new ArrayList<>();
		List<ICDMulticoreAdapter> reusableSharedMCs =
				Collections.singletonList(new ICDMulticoreAdapter(targetSharedMulticore));
		ICDMulticoreAdapter reusableSharedParent =
				findReusableMulticore(connectivity, reusableSharedMCs, sharedIcdMulticores);

		if (reusableSharedParent != null) {
			ISharedMulticore sharedParent = reusableSharedParent.getSharedMulticore();
			ILogicDesign logicDesign = mConductor.getLogicDesign();
			assert logicDesign != null;
			if (sharedParent != null) {
				mConductor.assignLibraryPart(null);
				if (sharedIcdMulticores.isEmpty()) {
					shareConductor(logicDesign, sharedParent, mConductor);
					sharedParent.flush();
					SharedConductorHelper.fixupParentageForConductor(mConductor, logicDesign);
				}
				else {
					IMulticore leafMulticore = null;
					IMulticore childMulticore = null;
					for (ICDMulticore icdMulticore : sharedIcdMulticores) {
						childMulticore = icdMulticore.createMulticore(childMulticore);
						if (leafMulticore == null) {
							leafMulticore = childMulticore;
						}
					}
					assert childMulticore != null;

					SharedConductorGroupHelper.share(childMulticore, logicDesign, sharedParent, null, null);

					ISharedMulticore targetSharedMC = leafMulticore.getSharedMulticore();
					assert targetSharedMC != null;
					shareConductor(logicDesign, targetSharedMC, mConductor);
					leafMulticore.addConductor(mConductor);

					sharedParent.flush();
					SharedConductorHelper.fixupParentageForMulticore(childMulticore, logicDesign);
				}
				IMulticore rootMulticore = MulticoreUtils.getRootMulticore(mConductor.getMulticore(), true);
				if (rootMulticore != null) {
					return rootMulticore.getAllMulticoresInHierarchy();
				}
			}
		}
		return Collections.emptySet();
	}

	private void shareConductor(ILogicDesign logicDesign, ISharedMulticore targetSharedMC, IConductor conductor)
	{
		ISharedConductor sharedConductor = conductor.getSharedConductor();
		if (sharedConductor == null) {
			SharedConductorHelper.shareConductor(mConductor, targetSharedMC, logicDesign);
		}
		else {
			addToParent(targetSharedMC, sharedConductor);
		}
	}

	private void addToParent(ISharedMulticore targetSharedMC, ISharedConductor sharedConductor)
	{
		targetSharedMC.addConductor(sharedConductor);
		Set<IRevisionedSharedObject> modifiedObjs = null;
		Set<IRevisionedSharedObject> lockedObjs = new HashSet<IRevisionedSharedObject>();
		try {
			modifiedObjs = SharedObjectRevisionHelper.removeFromRevisionStructure(sharedConductor, lockedObjs);
			sharedConductor.setParentId(null);
			sharedConductor.setBaseId(sharedConductor.getUID());
		}
		finally {
			if (modifiedObjs != null) {
				modifiedObjs.forEach(IFlushable::flush);
			}
			mPersistenceHandler.getLockTracker().addLockedObjects(lockedObjs);
		}
	}

	private void collectAllMulticoresInHierarchy(ISharedMulticore sharedMulticore,
			Set<ISharedMulticore> allMulticores)
	{
		sharedMulticore.getMulticores().stream()
				.forEach(smc -> {
					allMulticores.add(smc);
					collectAllMulticoresInHierarchy(smc, allMulticores);
				});
	}

	@Nullable
	private Collection<IMulticore> fillInCableHierarchy(IConnectivity connectivity,
			List<ICDMulticore> icdMulticores,
			@Nullable ICDMulticoreAdapter reusableParent)
	{
		if (reusableParent != null) {
			ISharedMulticore sharedMulticore = reusableParent.getSharedMulticore();
			if (sharedMulticore != null) {
				Collection<IMulticore> createdMulticores = addIntoExistingSharedMC(connectivity, sharedMulticore);
				if(createdMulticores != null)
				{
					return createdMulticores;
				}
			}
		}

		ISharedConductor sharedConductor = mConductor.getSharedConductor();
		if (sharedConductor != null) {
			if (!mPersistenceHandler.getLockTracker().lock(sharedConductor, true)) {
				return Collections.emptySet();
			}
		}

		Collection<IMulticore> createdMulticores = new HashSet<>();
		IMulticore leafMulticore = null;
		IMulticore childMulticore = null;
		for (ICDMulticore icdMulticore : icdMulticores) {
			childMulticore = icdMulticore.createMulticore(childMulticore);
			createdMulticores.add(childMulticore);
			if (leafMulticore == null) {
				leafMulticore = childMulticore;
			}
		}

		if (childMulticore != null && reusableParent != null) {
			reusableParent.getMulticore().addMulticore(childMulticore);
			for (IConductor conductor : childMulticore.getAllConductorsInHierarchy()) {
				conductor.assignLibraryPart(null);
			}
			for (IMulticore multicore : childMulticore.getAllMulticoresInHierarchy()) {
				multicore.assignLibraryPart(null);
			}
		}
		if (leafMulticore == null && reusableParent != null) {
			leafMulticore = reusableParent.getMulticore();
		}
		if (mConductor.isShared() && childMulticore != null) {

			IMulticore rootMulticore = MulticoreUtils.getRootMulticore(childMulticore, true);
			if (rootMulticore != null && !rootMulticore.isShared()) {
				IDesign design = CommonUtils.cast(connectivity.getDesign(), IDesign.class);
				assert design != null;
				SharedConductorGroupHelper.share(rootMulticore, design);
			}
		}
		if (leafMulticore != null) {
			ISharedMulticore sharedLeafMC = leafMulticore.getSharedMulticore();
			if(sharedLeafMC == null && sharedConductor != null)
			{
				// Reusable MC is not shared MC but shared conductor is being added to root of Reusable MC
				IMulticore rootMulticore = MulticoreUtils.getRootMulticore(leafMulticore, true);
				if (rootMulticore != null && !rootMulticore.isShared()) {
					// First share the identified multicore and then add conductor to it
					IDesign design = CommonUtils.cast(connectivity.getDesign(), IDesign.class);
					assert design != null;
					SharedConductorGroupHelper.share(rootMulticore, design);
					final ISharedMulticore sharedMulticore = rootMulticore.getSharedMulticore();
					if (sharedMulticore != null) {
						return addIntoExistingSharedMC(connectivity, sharedMulticore);
					}
				}
			}
			leafMulticore.addConductor(mConductor);
			mConductor.assignLibraryPart(null);
			if (sharedLeafMC != null && sharedConductor != null) {
				addToParent(sharedLeafMC, sharedConductor);
				sharedLeafMC.flush();
			}
			return createdMulticores;
		}
		return null;
	}

	@Nullable
	private Collection<IMulticore> addIntoExistingSharedMC(IConnectivity connectivity, @NotNull ISharedMulticore sharedMulticore)
	{
		ISharedMulticore existingSharedMC = sharedMulticore;
		Collection<IMulticore> createdMulticores = null;
		IUID sharedMulticoreUID = existingSharedMC.getUID();
		IProject project = connectivity.getProject();
		if (project != null && mPersistenceHandler.getLockTracker()
				.lockManager(project.getSharedConductorMgr(), mConductor.getName())) {
			existingSharedMC = UIDMgr.getObjectOfType(sharedMulticoreUID, ISharedMulticore.class);
			if (existingSharedMC != null) {
				ISharedMulticore rootMulticore = existingSharedMC.getRootMulticore();
				Set<ISharedConductor> sharedConductors =
						rootMulticore.getAllSharedConductorsInHierarchy(false).stream()
								.collect(Collectors.toSet());
				Set<ISharedMulticore> allMulticores = new HashSet<>();
				allMulticores.add(rootMulticore);
				if (!mPersistenceHandler.isUpdate()) {
					collectAllMulticoresInHierarchy(rootMulticore, allMulticores);
				}
				createdMulticores = updateSharedMulticore(connectivity, sharedConductors, allMulticores);
			}
		}
		else {
			createdMulticores =  Collections.emptySet();
		}
		return createdMulticores;
	}

	@Nullable private ICDMulticoreAdapter findReusableMulticore(IConnectivity connectivity,
			List<ICDMulticoreAdapter> multicoresToSearch, List<ICDMulticore> icdMulticores)
	{
		// comparator to choose the multicore which contains most of the hierarchy and hence needs minimum multicores
		// to be created
		SortedList<Pair<ICDMulticoreAdapter, List<ICDMulticore>>> candidatesForReuse = new SortedList<>(
				new Comparator<Pair<ICDMulticoreAdapter, List<ICDMulticore>>>()
				{
					@Override public int compare(Pair<ICDMulticoreAdapter, List<ICDMulticore>> o1,
							Pair<ICDMulticoreAdapter, List<ICDMulticore>> o2)
					{
						return Integer.compare(o1.getValue().size(), o2.getValue().size());
					}
				});
		Set<List<IICDNetCableElement>> signalGroupPaths = getSignalGroupPaths(connectivity);
		for (List<IICDNetCableElement> path : signalGroupPaths) {
			List<ICDMulticore> icdMCsToBuild = new ArrayList<>();
			ICDMulticoreAdapter reusableMC =
					findReusableMCForAHierarchy(connectivity, multicoresToSearch, icdMCsToBuild, path);
			candidatesForReuse.add(new Pair<>(reusableMC, icdMCsToBuild));
		}
		if (!candidatesForReuse.isEmpty()) {
			icdMulticores.addAll(candidatesForReuse.get(0).getValue());
			return candidatesForReuse.get(0).getKey();
		}

		return null;
	}

	@Nullable private ICDMulticoreAdapter findReusableMCForAHierarchy(IConnectivity connectivity,
			List<ICDMulticoreAdapter> multicoresToSearch, @NotNull List<ICDMulticore> icdMulticores,
			@NotNull List<IICDNetCableElement> cableHierarchy)
	{
		ICDMulticoreAdapter reusableParent = null;
		for (IICDNetCableElement cableNameTypePair : cableHierarchy) {
			for (ICDMulticoreAdapter multicoreAdapter : multicoresToSearch) {
				String existingMCSourceName = multicoreAdapter.getSourceName();
				if (existingMCSourceName.equalsIgnoreCase(cableNameTypePair.getOriginalName())) {
					reusableParent = multicoreAdapter;
					break;
				}
			}
			if (reusableParent != null) {
				break;
			}
			else {
				icdMulticores.add(new ICDMulticore(cableNameTypePair.getOriginalName(), cableNameTypePair.getType(),
						connectivity));
			}
		}
		return reusableParent;
	}

	@NotNull private Set<List<IICDNetCableElement>> getSignalGroupPaths(@NotNull IConnectivity connectivity)
	{
		IDesignContainer design = connectivity.getDesign();
		ILogicDesign logicDesign = CommonUtils.cast(design, ILogicDesign.class);
		assert logicDesign != null;
		return logicDesign.getDesignICDContainer().getEquivalentSignalGroupPaths(mAssociatedSignal);
	}
}
