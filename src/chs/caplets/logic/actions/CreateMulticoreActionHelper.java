package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.MCProxy;
import chs.caf.caplet.helpers.MulticoreEditPanel;
import chs.caplets.logic.DeleteShieldBodiesHelper;
import chs.cof.COFTypeEnum;
import chs.cof.draw.IColor;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.parts.ILibraryMultiWireCore;
import chs.cofUtils.logical.concurrency.IConcurrentEditReporter;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateMulticoreActionHelper
{

	private CreateMulticoreActionHelper()
	{

	}

	@Nullable public static IUIDObject getParentObject(@Nullable IUIDObject child)
	{
		IUIDObject obj = null;
		if (child instanceof IMulticore) {
			obj = ((IAbstractMulticore) child).getParent();
		}

		if (child instanceof IConductor) {
			obj = ((IConductor) child).getMulticore();
		}

		return obj;
	}

	@Nullable public static Set<IUIDObject> getChildrenObjects(@Nullable IUIDObject parent)
	{
		if (parent instanceof IConductor) {
			return null;
		}

		if (parent instanceof IMulticore) {
			Set<IUIDObject> multicoreContents = new HashSet<IUIDObject>();
			IMulticore multicore = (IMulticore) parent;
			multicoreContents.addAll(multicore.getConductorsAsSet());
			IShieldConductor shield = multicore.getShield();
			if (shield != null) {
				multicoreContents.add(shield);
			}
			multicoreContents.addAll(multicore.getMulticoresAsList());
			return multicoreContents;
		}
		return null;
	}

	public static boolean canProceedWithEditMulticore(ILogicDesign design, Set<MCProxy> editedMulticores,
			Collection<IUID> alreadyLockedInThisSession, COFTypeEnum editType,
			@NotNull IConcurrentEditReporter reporter)
	{
		Set<IUIDObject> editedSet = new HashSet<IUIDObject>();

		for (MCProxy mcp : editedMulticores) {
			IUIDObject ref = getNearestLogicParent(mcp);
			editedSet.addAll(getLogicObjectsToBeLockedForAMulticore(getRootMulticore(ref)));
			// add the children of multicore proxy too, for locking
			for (MCProxy childProxy : CollectionUtils.getSafeCollection(mcp.childrenVec())) {
				editedSet.addAll(getLogicObjectsToBeLockedForAMulticore(childProxy.getRef()));
			}

			editedSet.addAll(getAdditionalObjectsToBeLockedForLibraryMulticore(mcp));
		}

		Map<IUIDObject, MulticoreHierarchyInfo> mapBeforeRefresh = new HashMap<IUIDObject, MulticoreHierarchyInfo>();
		for (IUIDObject editedObject : editedSet) {
			mapBeforeRefresh.put(editedObject, new MulticoreHierarchyInfo(editedObject));
		}

		Collection<IUID> lockFailedObjects = LogicObjectLockFinder.tryEdit(design, editedSet);

		if (lockFailedObjects.isEmpty()) {
			for (IUIDObject editedObject : editedSet) {
				//after refresh we might (generally) get different uid object.
				IUIDObject editObjAfterRefresh = UIDMgr.getObject(editedObject.getUID());
				MulticoreHierarchyInfo hierarchyInfoAfterRefresh = new MulticoreHierarchyInfo(editObjAfterRefresh);
				String resourceKey = null;
				MulticoreHierarchyInfo hierarchyInfoBeforeRefresh = mapBeforeRefresh.get(editedObject);
				if (!hierarchyInfoAfterRefresh.compare(hierarchyInfoBeforeRefresh)) {
					resourceKey = "CreateMulticoreActionHelper.structureModified.message";
				} else if (!hierarchyInfoAfterRefresh.compareSharedState(hierarchyInfoBeforeRefresh)) {
					resourceKey = "CreateMulticoreActionHelper.sharedUnshared.message";
				}
				if (resourceKey != null) {
					String name = ((IReadOnlyNamedObject) editedObject).getName();
					resourceKey = ResourceMgr.getString(CreateMulticoreActionHelper.class, resourceKey, name);
					reporter.report(HTMLHelper.color(IColor.RED, resourceKey));
					releaseLocks(UIDUtils.convertToUIDSet(editedSet), alreadyLockedInThisSession, design);
					return false;
				}
			}
			return true;
		}
		else {
			reportLockFailure(design, editType, reporter, lockFailedObjects);
			releaseLocks(UIDUtils.convertToUIDSet(editedSet), alreadyLockedInThisSession, design);
			return false;
		}
	}

	@NotNull private static IUIDObject getNearestLogicParent(@NotNull MCProxy mcProxy)
	{
		MCProxy currentProxy = mcProxy;
		IUIDObject ref = currentProxy.getRef();
		while (ref instanceof ILibraryMultiWireCore && currentProxy.getParentProxy() != null) {
			// keep going up the hierarchy until we find a logic object
			ref = currentProxy.getParentProxy().getRef();
			currentProxy = currentProxy.getParentProxy();
		}
		return ref;
	}

	public static void reportLockFailure(ILogicDesign design, COFTypeEnum editType,
			@NotNull IConcurrentEditReporter reporter, Collection<IUID> lockFailedObjects)
	{
		String key = "CreateMulticoreActionHelper.multicoreFailure.output.message";
		String actionMessage = ResourceMgr.getString(CreateMulticoreActionHelper.class, key, editType.toString());
		Set<IUID> topLevelUIDs = lockFailedObjects.stream()
				.filter(object -> isObjectsParentNotPartOfCollection(object, lockFailedObjects))
				.collect(Collectors.toSet());

		reportLockFailure(design, actionMessage, topLevelUIDs, reporter);
	}

	public static boolean isObjectsParentNotPartOfCollection(IUID objectUID, Collection<IUID> objectUIDs)
	{
		IUIDObject parentObject = getParentObject(objectUID.getObject());
		return !objectUIDs.contains(parentObject != null ? parentObject.getUID() : null);
	}

	@Nullable public static IMulticore getRootMulticore(IUIDObject object)
	{
		if (object instanceof IConductor) {
			return ((IConductor) object).getRootMulticore();
		}

		if (object instanceof IMulticore) {
			return ((IMulticore) object).getRootMulticore();
		}

		return null;
	}

	public static Set<IUIDObject> getLogicObjectsToBeLockedForAMulticore(@Nullable IUIDObject proxy)
	{
		if (proxy == null || proxy instanceof IShieldBody) {
			return Collections.emptySet();
		}
		Set<IUIDObject> multicoreContents = new HashSet<IUIDObject>();
		multicoreContents.add(proxy);
		if (proxy instanceof IMulticore) {
			multicoreContents.addAll(((IMulticore) proxy).getAllConductorsInHierarchy(true));
			multicoreContents.addAll(((IMulticore) proxy).getAllMulticoresInHierarchy());
		}
		return Collections.unmodifiableSet(multicoreContents);
	}

	public static Collection<ILogicObject> getAdditionalObjectsToBeLockedForLibraryMulticore(MCProxy editedMulticore)
	{
		// for library mcs which have all inner cores unassigned, the shield body will be deleted
		// so lock all the objects that will be impacted upfront

		if (!editedMulticore.isContainerRef()) {
			return Collections.emptyList();
		}

		ISchemDiagram diagram = ((ISchemDiagram) CAFUtils.getInstance().getActiveDiagram());
		if (diagram == null) {
			return Collections.emptyList();
		}

		if (editedMulticore.isLibraryPartRef() && !hasNonShieldConductor(editedMulticore)) {
			IShieldBody sb = ((IMulticore) editedMulticore.getRef()).getShieldBody();
			if (sb != null) {
				// other mcs in the same hierarchy might have the same issue, so collect shield bodies of those too
				MCProxy rootProxy = MulticoreEditPanel.getRootProxy(editedMulticore);
				Set<chs.cof.logical.schem.IShieldBody> schemShieldBodies =
						new HashSet<chs.cof.logical.schem.IShieldBody>();
				Set<IShieldBody> connShieldBodies = new HashSet<IShieldBody>();
				for (MCProxy mcInHierarchy : getAllMulticoresInHierarchy(rootProxy)) {
					if (mcInHierarchy.isLibraryPartRef() && !hasNonShieldConductor(mcInHierarchy)) {
						IShieldBody sbInHierarchy = ((IMulticore) mcInHierarchy.getRef()).getShieldBody();
						if (sbInHierarchy != null) {
							for (IDiagramObject so : diagram.getRepresentations(sbInHierarchy.getUID())) {
								if (so instanceof chs.cof.logical.schem.IShieldBody) {
									schemShieldBodies.add((chs.cof.logical.schem.IShieldBody) so);
								}
							}
							connShieldBodies.add(sbInHierarchy);
						}
					}
				}

				DeleteShieldBodiesHelper deleteShieldBodiesHelper =
						new DeleteShieldBodiesHelper(schemShieldBodies, connShieldBodies);

				return deleteShieldBodiesHelper.getLogicObjectsToLock();
			}
		}
		return Collections.emptyList();
	}

	private static List<MCProxy> getAllMulticoresInHierarchy(MCProxy rootProxy)
	{
		List<MCProxy> childMulticores = new ArrayList<MCProxy>();
		childMulticores.add(rootProxy);
		for (MCProxy child : rootProxy.childrenVec()) {
			if (child.isContainerRef()) {
				childMulticores.addAll(getAllMulticoresInHierarchy(child));
			}
		}
		return childMulticores;
	}

	public static boolean refreshManager(CreateMulticoreContext context)
	{
		if (!context.isSharedEditScope()) {
			return true;
		}
		ISharedConductorMgr conductorMgr = context.getSharedConductorMgr();

		if (conductorMgr.lock()) {
			conductorMgr.refresh();
			return true;
		}
		else {
			LogicActionMessageHelper.warnLocked(conductorMgr);
			return false;
		}
	}


	public static void unlockManager(@NotNull CreateMulticoreContext context)
	{
		if (!context.isSharedEditScope()) {
			return;
		}
		ISharedConductorMgr conductorMgr = context.getSharedConductorMgr();
		conductorMgr.unlock();
	}

	private static boolean hasNonShieldConductor(MCProxy editedMCProxy)
	{
		for (MCProxy child : editedMCProxy.childrenVec()) {
			if ((child.isConductorRef() && !(child.getRef() instanceof IShieldConductor)) ||
					hasNonShieldConductor(child)) {
				return true;
			}
		}

		return false;
	}

	public static void releaseLocks(Set<IUID> lockedInThisAction, Collection<IUID> alreadyLockedInThisSession,
			ILogicDesign design)
	{
		Set<IUID> UIDsToBeUnlocked = new HashSet<IUID>();
		for (IUID editedUID : lockedInThisAction) {
			if (!alreadyLockedInThisSession.contains(editedUID)) {
				UIDsToBeUnlocked.add(editedUID);
			}
		}
		design.unlockLogicObjects(UIDsToBeUnlocked);
	}

	public static void reportLockFailure(ILogicDesign design, String actionMessage, Collection<IUID> lockFailedUIDs,
			@NotNull IConcurrentEditReporter reporter)
	{
		boolean hasAnythingBeenRemotelyDeleted = lockFailedUIDs.stream()
				.filter(lockFailedUID -> lockFailedUID.getObject() == null)
				.count() > 0;

		if (hasAnythingBeenRemotelyDeleted) {
			String errorMessage = actionMessage + ResourceMgr.getString(CreateMulticoreActionHelper.class,
					"CreateMulticoreActionHelper.remotelyDeleted.message");
			reporter.report(HTMLHelper.color(IColor.RED, errorMessage));
		}
		LogicConcurrencyLogger.getInstance().reportLockFailure(design, actionMessage, lockFailedUIDs, reporter);
	}
}
