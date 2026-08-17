package chs.caplets.logic.icd;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.DeleteHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectMgr;
import chs.cof.project.IProject;
import chs.cofUtils.cmd.CommandHelper;
import chs.cog.PersistenceLockFailureCheckedException;
import chs.cog.PersistenceStateException;
import chs.common.IDesignContainer;
import chs.common.IGuard;
import chs.common.ILockable;
import chs.common.INamedPropertiedObject;
import chs.common.IUID;
import chs.common.IUIDProvider;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.dataservices.CapitalProjectDataServices;
import chs.dataservices.CapitalDataServices;
import chs.dataservices.LightWeightUsage;
import chs.dataservices.SharedObjectUsageInfo;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SharedDetailsLockHelper
{

	private Collection<IUID> lockedSharedObjects;
	private ISharedObjectMgr sharedObjectMgrToUnlock = null;
	private Collection<ILogicDesign> designsToSave;
	private Collection<ILogicDesign> designsToUnlock;
	private SetMap<IUID, ILogicDesign> sharedCondUsages;

	public SharedDetailsLockHelper()
	{
		lockedSharedObjects = new HashSet<>();
		designsToSave = new HashSet<>();
		designsToUnlock = new HashSet<>();
		sharedCondUsages = new SetMap<>();
	}

	@NotNull public Collection<IUID> getLockedSharedObjects()
	{
		return lockedSharedObjects;
	}

	public boolean lock(@Nullable ISharedConductor sharedConductor, boolean bLockRevisions)
	{
		if (sharedConductor == null) {
			return true;
		}

		String condName = sharedConductor.getName();
		if (!lockedSharedObjects.contains(sharedConductor.getUID()) && !sharedConductor.isLocked()) {
			if (!sharedConductor.lock()) {
				String msg = ResourceMgr
						.getString(MulticoreBuilder.class, "MulticoreBuilder.sharedConductorLockFailed.text", condName,
								condName, condName);
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
				return false;
			}
			sharedConductor.refresh();
			lockedSharedObjects.add(sharedConductor.getUID());
			ISharedMulticore multicore = sharedConductor.getMulticore();
			if (multicore != null) {
				lockedSharedObjects.add(multicore.getRootMulticore().getUID());
			}
		}
		// Lock shared revisions
		if (bLockRevisions) {
			Set<IRevisionedSharedObject> objectsFailedToLock = new HashSet<IRevisionedSharedObject>();
			Set<IRevisionedSharedObject> lockedObjs = new HashSet<IRevisionedSharedObject>();
			SharedObjectRevisionHelper
					.lockRevisionsDependentRevisions(sharedConductor, lockedObjs, objectsFailedToLock);
			lockedObjs.stream()
					.map(IUIDProvider::getUID)
					.collect(Collectors.toCollection(() -> lockedSharedObjects));
			if (!objectsFailedToLock.isEmpty()) {
				String msg = ResourceMgr
						.getString(MulticoreBuilder.class, "MulticoreBuilder.sharedConductorRevisionsLockFailed.text",
								condName, condName, condName);
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
				return false;
			}
		}

		return addImpactedDesign(sharedConductor, condName);
	}

	public boolean lock(ISharedMulticore sharedMulticore, boolean bLockManager, String condName)
	{
		return lock(sharedMulticore, bLockManager, condName, null);
	}

	public boolean lock(ISharedMulticore sharedMulticore, boolean bLockManager, String condName,
			@Nullable ISharedConductor sharedConductor)
	{
		IUID sharedMulticoreUID = sharedMulticore.getUID();
		IUID sharedCondUID = null;
		if (sharedConductor != null) {
			sharedCondUID = sharedConductor.getUID();
		}

		if (bLockManager) {
			ISharedObjectMgr sharedObjectMgr = sharedMulticore.getSharedObjectMgr();
			if (!lockManager(sharedObjectMgr, condName)) {
				return false;
			}
		}

		ISharedConductor conductorToLock = UIDMgr.getObjectOfType(sharedCondUID, ISharedConductor.class);
		if (!lock(conductorToLock, false)) {
			return false;
		}

		ISharedMulticore multicoreToLock = UIDMgr.getObjectOfType(sharedMulticoreUID, ISharedMulticore.class);
		if (multicoreToLock == null) {
			return false;
		}

		ISharedMulticore rootMulticore = multicoreToLock.getRootMulticore();
		if (lockedSharedObjects.contains(rootMulticore.getUID())) {
			assert rootMulticore.isLocked();
			return true;
		}

		if (multicoreToLock.isLocked()) {
			return true;
		}

		if (multicoreToLock.lock()) {
			multicoreToLock.refresh();
			lockedSharedObjects.add(multicoreToLock.getRootMulticore().getUID());
			return true;
		}

		String msg = ResourceMgr
				.getString(MulticoreBuilder.class, "MulticoreBuilder.multicoreLockFailed.text", condName,
						multicoreToLock.getName(), multicoreToLock.getName());
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
		return false;
	}

	public boolean lockManager(ISharedObjectMgr sharedObjectMgr, String condName)
	{
		if (!sharedObjectMgr.isLocked()) {
			if (sharedObjectMgr.lock()) {
				sharedObjectMgr.refresh();
				sharedObjectMgrToUnlock = sharedObjectMgr;
			}
			else {
				String msg = ResourceMgr
						.getString(MulticoreBuilder.class, "MulticoreBuilder.sharedMulticoreMgrLockFailed.text",
								condName);
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
				return false;
			}
		}
		return true;
	}

	public void unlock()
	{
		updateImpactedDesigns();
		saveDesigns();
		UtilsHelper.getPersistenceSession().batchUnlock(designsToUnlock);
		if (sharedObjectMgrToUnlock != null) {
			sharedObjectMgrToUnlock.unlock();
		}

		for (IUID sharedObjectUID : lockedSharedObjects) {
			ILockable lockable = UIDMgr.getObjectOfType(sharedObjectUID, ILockable.class);
			if (lockable != null) {
				ISharedLockableUpdateableObject sharedObject =
						CommonUtils.cast(lockable, ISharedLockableUpdateableObject.class);
				if (sharedObject != null) {
					LockUpdateHelper.flushAndUnlockSharedObject(sharedObject);
				}
				else {
					lockable.unlock();
				}
			}
		}
	}

	private void saveDesigns()
	{
		CommandHelper commandHelper = new CAFCommandHelper();
		try {
			for (ILogicDesign impactedDesign : designsToSave) {
				if (impactedDesign.isLoadedInMemory() && impactedDesign.isLocked() && impactedDesign.isEditable()) {
					commandHelper.saveDesign(impactedDesign);
				}
			}
		}
		catch (UserSessionException sessionException) {
			Environment.getExceptionDisplay().displayException(sessionException, "Update ICD action failed");
		}
	}

	private void updateImpactedDesigns()
	{
		// CARCH-1291 - changed the creation deletion helper guard
		try (IGuard ignored = CreationDeletionHelper.createDisableCreationDeletionHelperInThreadGuard()) {
			for (ILogicDesign impactedDesign : designsToSave) {
				if (impactedDesign.isLoadedInMemory() && impactedDesign.isLocked() && impactedDesign.isEditable()) {
					IConnectivity connectivity = impactedDesign.getConnectivity();
					assert connectivity != null;
					SharedConductorHelper.fixMissingDescendantsOfAllSharedMCs(connectivity);
					SharedConductorHelper.fixMissingParentsOfAllSharedMCs(impactedDesign);
					SharedConductorHelper.fixMissingParentsOfAllSharedConductors(connectivity);
				}
			}
		}
	}

	private boolean addImpactedDesign(@Nullable ISharedObject sharedObj, String condName)
	{
		if (sharedObj == null) {
			return true;
		}

		CapitalProjectDataServices dataServices = CapitalProjectDataServices.getDataServices();
		List<LightWeightUsage> lightWeightUsages =
				dataServices.getDesignsWhereUsedOrUnPlacedBatch(Collections.singleton(sharedObj), true);
		SharedObjectUsageInfo sharedObjectUsageInfo = new SharedObjectUsageInfo();
		sharedObjectUsageInfo.prepareSharedConductorUsageData(lightWeightUsages);

		Set<CapitalDataServices.SimpleDesignName> usedDesigns = new HashSet<>();
		usedDesigns.addAll(sharedObjectUsageInfo.getUsedDesignNames());

		IProject project = sharedObj.getProject();
		return addLoadedAndEditableImpactedDesigns(sharedObj.getUID(), usedDesigns, project, condName);
	}

	private boolean addLoadedAndEditableImpactedDesigns(
			IUID sharedObjUID, Collection<CapitalProjectDataServices.SimpleDesignName> collDesignUsages,
			IProject project, String condName)
	{

		// Load the designs using the UIDs
		Collection<IDesignContainer> designs = LogicUtils.loadDesigns(project.getDesignMgr(), collDesignUsages);
		Collection<ILogicDesign> impactedDesigns = new HashSet<>();

		//Update the lock and update design collections
		designs.stream().forEach(des -> {
			if (des instanceof ILogicDesign) {
				ILogicDesign logicDes = (ILogicDesign) des;
				impactedDesigns.add(logicDes);
			}
		});

		Set<ILogicDesign> lockedDesigns = new HashSet<ILogicDesign>();
		boolean isSuccess = true;
		for (ILogicDesign impactedDesign : impactedDesigns) {
			if (!lockDesigns(Collections.singleton(impactedDesign), lockedDesigns, condName)) {
				isSuccess = false;
			}
		}

		if (isSuccess) {
			designsToUnlock.addAll(lockedDesigns);
			designsToSave.addAll(impactedDesigns);
			sharedCondUsages.addAll(sharedObjUID, impactedDesigns);
			return true;
		}
		return false;
	}

	private boolean lockDesigns(Collection<ILogicDesign> designsImpacted, Set<ILogicDesign> lockedDesigns,
			String condName)
	{
		Set<ILogicDesign> designsToBeLocked = new HashSet<ILogicDesign>();
		for (ILogicDesign impactedDesign : designsImpacted) {
			if (!impactedDesign.isLocked()) {
				designsToBeLocked.add(impactedDesign);
			}
		}
		boolean bAllDesignModifiable = false;
		try {
			UtilsHelper.getPersistenceSession().batchAtomicLock(designsToBeLocked);
			lockedDesigns.addAll(designsToBeLocked);
			bAllDesignModifiable = true;

			boolean allDesignEditable = designsToBeLocked.stream().allMatch(ILogicDesign::isEditable);
			if (!allDesignEditable) {
				String msg = ResourceMgr
						.getString(SharedDetailsLockHelper.class, "SharedDetailsLockHelper.designNotEditable.text",
								condName);
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
				bAllDesignModifiable = false;
			}
		}
		catch (PersistenceLockFailureCheckedException e) {
			String msg = ResourceMgr
					.getString(MulticoreBuilder.class, "MulticoreBuilder.designLockFailed.text", condName,
							getShortListOfObjectNames(designsToBeLocked.iterator()));
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
		}
		catch (PersistenceStateException e) {
			String msg = ResourceMgr
					.getString(MulticoreBuilder.class, "MulticoreBuilder.designLockFailed.text", condName,
							getShortListOfObjectNames(designsToBeLocked.iterator()));
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(msg);
		}

		return bAllDesignModifiable;
	}

	private String getShortListOfObjectNames(Iterator<? extends INamedPropertiedObject> objIter)
	{
		StringBuilder nameList = new StringBuilder();
		if (objIter.hasNext()) {
			nameList.append(objIter.next().getName());
			if (objIter.hasNext()) {
				nameList.append(", ");
				nameList.append(objIter.next().getName());
				nameList.append(",...");
			}
		}
		return nameList.toString();
	}

	public void addLockedObjects(Set<IRevisionedSharedObject> lockedObjs)
	{
		lockedObjs.stream()
				.map(IUIDProvider::getUID)
				.collect(Collectors.toCollection(() -> lockedSharedObjects));
	}

	public void removeMulticoresFromDesigns(IUID sharedConductorUID,
			Collection<IUID> orphanedSharedInnercores)
	{
		// CARCH-1291 - changed the creation deletion helper guard
		try (IGuard ignored = CreationDeletionHelper.createDisableCreationDeletionHelperInThreadGuard()) {
			for (ILogicDesign logicDesign : sharedCondUsages.get(sharedConductorUID)) {
				IConnectivity connectivity = logicDesign.getConnectivity();
				if (connectivity != null) {
					IConductor cableCond = connectivity
							.findSharedConductor(UIDMgr.getObjectOfType(sharedConductorUID, ISharedConductor.class));
					IMulticore emptyMulticore = getEmptyParentMulticore(cableCond, orphanedSharedInnercores);
					if (emptyMulticore != null) {
						DeleteHelper.getInstance()
								.deleteEmptyMulticores(logicDesign, Collections.singleton(emptyMulticore),
										"UpdateICDRouting");
					}
				}
			}
		}
	}

	@Nullable
	private IMulticore getEmptyParentMulticore(@Nullable IConductor cableCond,
			Collection<IUID> orphanedSharedInnercores)
	{
		if (cableCond == null) {
			return null;
		}
		IMulticore emptyMulticore = null;
		IMulticore multicore = cableCond.getMulticore();
		while (multicore != null) {
			Set<IUID> sharedConductors =
					multicore.getAllConductorsInHierarchy().stream()
							.map(IConductor::getSharedObjectUID)
							.collect(Collectors.toSet());
			if (sharedConductors.isEmpty() || orphanedSharedInnercores.containsAll(sharedConductors)) {
				emptyMulticore = multicore;
			}
			multicore = multicore.getParent();
		}
		return emptyMulticore;
	}
}
