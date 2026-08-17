package chs.caplets.logic.commands;

import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.shared.OperandShareabilityStatus;
import chs.caplets.logic.actions.shared.autoshare.AutoShareParams;
import chs.caplets.logic.actions.shared.autoshare.FetchOffPageAutoShareIntoExecutor;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.ILockable;
import chs.common.ILockableDelegate;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.StringUtils;
import chs.utilities.WrappingRuntimeException;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.LockSharedPinListHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.properties.PropTextScrubber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cmd for share into multiple objects at once
 */
public class BulkAutoShareIntoCmd extends AbstractBulkAutoShareCmd
{

	@Nullable protected ISchemDiagram m_diagram;
	@NotNull protected Map<IUIDObject, ISharedObject> m_objectsToBeSharedInto;
	private boolean m_continueToShareAfterFailure;
	private boolean m_saveDesignChanges;
	@NotNull protected final AutoShareParams m_params;
	@NotNull private final Comparator<IUIDObject> m_comparator;
	@NotNull private Set<ISharedObject> m_newlyCreatedSharedObjs = new HashSet<>();

	public BulkAutoShareIntoCmd(@NotNull Map<IUIDObject, ISharedObject> objectsToBeSharedInto,
			@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext messageReporter, @NotNull Comparator<IUIDObject> comparator)
	{
		this(objectsToBeSharedInto, design, diagram, messageReporter, false, false,
				new AutoShareParams(false, true, false), comparator);
	}

	public BulkAutoShareIntoCmd(@NotNull Map<IUIDObject, ISharedObject> objectsToBeSharedInto,
			@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext messageReporter)
	{
		this(objectsToBeSharedInto, design, diagram, messageReporter, false, false,
				new AutoShareParams(false, true, false));
	}

	public BulkAutoShareIntoCmd(@NotNull Map<IUIDObject, ISharedObject> objectsToBeSharedInto,
			@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext messageReporter,
			boolean continueToShareAfterFailure, boolean saveDesignChanges, @NotNull AutoShareParams params)
	{
		this(objectsToBeSharedInto, design, diagram, messageReporter, continueToShareAfterFailure, saveDesignChanges,
				params, new UIDObjectTypeComparator());
	}

	public BulkAutoShareIntoCmd(@NotNull Map<IUIDObject, ISharedObject> objectsToBeSharedInto,
			@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext messageReporter,
			boolean continueToShareAfterFailure, boolean saveDesignChanges, @NotNull AutoShareParams params, Set<ISharedObject> newlyCreatedSharedObjs)
	{
		this(objectsToBeSharedInto, design, diagram, messageReporter, continueToShareAfterFailure, saveDesignChanges,
				params, new UIDObjectTypeComparator());
		m_newlyCreatedSharedObjs.addAll(newlyCreatedSharedObjs);
	}

	public BulkAutoShareIntoCmd(@NotNull Map<IUIDObject, ISharedObject> objectsToBeSharedInto,
			@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext messageReporter,
			boolean continueToShareAfterFailure, boolean saveDesignChanges, @NotNull AutoShareParams params,
			@NotNull Comparator<IUIDObject> comparator)
	{
		super(design, messageReporter);
		m_objectsToBeSharedInto = objectsToBeSharedInto;
		m_diagram = diagram;
		m_continueToShareAfterFailure = continueToShareAfterFailure;
		m_saveDesignChanges = saveDesignChanges;
		m_params = params;
		m_comparator = comparator;
	}

	@Override public boolean doExecuteAllowed()
	{
		boolean okToExecute = super.doExecuteAllowed();
		if (!m_continueToShareAfterFailure) {
			for (ISharedObject sharedObject : m_objectsToBeSharedInto.values()) {
				okToExecute = okToExecute && sharedObject.isLocked();
				if (!okToExecute) {
					releaseAcquiredLocks();
					break;
				}
			}
		}
		return okToExecute;
	}

	@Override protected boolean doExecute()
	{
		Collection<IUIDObject> sortedCandidates =
				CollectionUtils.createSortedList(m_objectsToBeSharedInto.keySet(), m_comparator);
		for (IUIDObject candidateObject : sortedCandidates) {
			if (candidateObject.isDeletedObject()) {
				continue;
			}
			ISharedObject sharedObject = m_objectsToBeSharedInto.get(candidateObject);
			boolean isNewlyCreated = m_newlyCreatedSharedObjs.contains(sharedObject);
			FetchOffPageAutoShareIntoExecutor autoShareIntoExecutor = getAutoShareIntoExecutor(sharedObject,isNewlyCreated);
			if (sharedObject instanceof ISharedMulticore) {
				IMulticore multicore = reduceToMulticoreObject(candidateObject);
				if (multicore == null || multicore.getParent() != null) {
					continue;
				}
				ISharedMulticore sharedMulticore = (ISharedMulticore) sharedObject;
				Map<ILogicObject, ISharedObject> mcHierarchyMapping = new HashMap<>();
				populateMulticoreHierarchyMapping(multicore, sharedMulticore, mcHierarchyMapping);
				autoShareIntoExecutor.setMulticoreHierarchyMap(mcHierarchyMapping);
			}
			autoShareIntoExecutor.setAuditObjUIDConsumer((logId) -> m_storedAuditLogIds.add(logId));
			m_params.getPreShareTask().accept(candidateObject, sharedObject);
			if (!doShare(autoShareIntoExecutor, candidateObject)) {
				LogHelper.debugMsgSafe(getDebugMessage(candidateObject));
				if (!m_continueToShareAfterFailure) {
					return false;
				}
			}
		}
		new PropTextScrubber().synchronizeChangedObjects(m_design.getDiagrams().stream().collect(Collectors.toSet()),
				Collections.emptySet());
		try {
			saveAll();
		}
		catch (UserSessionException ex) {
			throw new WrappingRuntimeException(ex);
		}
		return true;
	}

	private void saveAll() throws UserSessionException
	{
		saveSharedPinlistMgr();
		if (m_saveDesignChanges) {
			getCommandHelper().saveDesign(m_design);
			getCommandHelper().setDesignModifiedFlag(m_design, false);
		}
	}

	@NotNull protected FetchOffPageAutoShareIntoExecutor getAutoShareIntoExecutor(@NotNull ISharedObject sharedObject, boolean isNewlyCreatedObj)
	{
		return new FetchOffPageAutoShareIntoExecutor(m_project, m_design, m_diagram, sharedObject, m_messageReporter,
				m_params, isNewlyCreatedObj);
	}

	@NotNull private String getDebugMessage(@NotNull IUIDObject objectToBeSharedInto)
	{
		final ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(objectToBeSharedInto);
		final String name = logicObject != null ? logicObject.getName() : StringUtils.EMPTY_STRING;
		return "Failed to share into" + name + " , UID: " + objectToBeSharedInto.getUID().getString();
	}

	protected void acquireLocks()
	{
		super.acquireLocks();

		Set<ISharedPinList> sharedPinLists = new HashSet<>();
		Set<ILockable> processedLockables = new HashSet<>();
		Set<ILockable> nonSharedPinLists = new HashSet<>();

		for (ISharedObject sharedObject : m_objectsToBeSharedInto.values()) {
			ILockable lockable = sharedObject.getLockableUpdateableRoot();
			if (lockable instanceof ILockableDelegate) {
				lockable = ((ILockableDelegate) lockable).getLockableDelegate();
			}
			if (processedLockables.contains(lockable)) {
				continue;
			}
			processedLockables.add(lockable);
			if (lockable != null && !lockable.isLocked()) {
				if (lockable instanceof ISharedPinList) {
					sharedPinLists.add((ISharedPinList) lockable);
				}
				else {
					nonSharedPinLists.add(lockable);
				}
			}
		}
		if (!sharedPinLists.isEmpty()) {
			Set<IUID> batchLockFailures = LockSharedPinListHelper.lockMultipleSharedPinLists(sharedPinLists);
			sharedPinLists.forEach(sharedPinList -> {
				if (batchLockFailures.contains(sharedPinList.getUID())) {
					reportLockFailure(sharedPinList);
				}
				else {
					m_acquiredLocks.add(sharedPinList);
				}
			});
		}

		BatchLockRefreshHelper.batchLockWithPromise(nonSharedPinLists, () -> {
			nonSharedPinLists.forEach(lockable -> lockAndReportOnFailure(lockable));
		});
	}

	private void populateMulticoreHierarchyMapping(@NotNull IMulticore multicore,
			@NotNull ISharedMulticore sharedMulticore, @NotNull Map<ILogicObject, ISharedObject> mcHierarchyMapping)
	{
		mcHierarchyMapping.put(multicore, sharedMulticore);
		IShieldConductor shield = multicore.getShield();
		ISharedConductor sharedShield = sharedMulticore.getShield();
		if (shield != null && sharedShield != null) {
			mcHierarchyMapping.put(shield, sharedShield);
		}
		for (IConductor conductor : multicore.getConductors()) {
			ISharedConductor sharedConductor =
					CommonUtils.cast(m_objectsToBeSharedInto.get(conductor), ISharedConductor.class);
			if (sharedConductor != null) {
				mcHierarchyMapping.put(conductor, sharedConductor);
			}
		}
		for (IMulticore innerMC : multicore.getMulticores()) {
			ISharedMulticore sharedInnerMC = CommonUtils.cast(m_objectsToBeSharedInto.containsKey(innerMC) ?
							m_objectsToBeSharedInto.get(innerMC) : m_objectsToBeSharedInto.get(innerMC.getShieldBody()),
					ISharedMulticore.class);
			if (sharedInnerMC != null) {
				populateMulticoreHierarchyMapping(innerMC, sharedInnerMC, mcHierarchyMapping);
			}
		}
	}

	@Nullable private IMulticore reduceToMulticoreObject(@NotNull IUIDObject uidObject)
	{
		IMulticore multicore = null;
		if (uidObject instanceof IMulticore) {
			multicore = (IMulticore) uidObject;
		}
		else if (uidObject instanceof IShieldBody) {
			multicore = ((IShieldBody) uidObject).getMulticore();
		}
		return multicore;
	}

	public static class UIDObjectTypeComparator implements Comparator<IUIDObject>
	{

		@Override public int compare(IUIDObject o1, IUIDObject o2)
		{
			return Integer.compare(getTypeOrder(o1), getTypeOrder(o2));
		}

		protected int getTypeOrder(IUIDObject uidObject)
		{
			if (uidObject instanceof IDevice) {
				return 1;
			}
			else if (uidObject instanceof IMulticore ||
					uidObject instanceof IShieldBody) {
				return 2;
			}
			else {
				return 3;
			}
		}
	}
}
