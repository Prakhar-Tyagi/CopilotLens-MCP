/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2017-2024 Siemens
 */

package chs.caplets.logic.helpers.tabulareditor;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IUndoableContainer;
import chs.caf.caplet.helpers.tabulareditor.IFilterableObjectType;
import chs.caf.caplet.helpers.tabulareditor.LogicTabularEditorObjectNameDerivation;
import chs.caf.caplet.helpers.tabulareditor.TabularEditor;
import chs.caf.caplet.helpers.tabulareditor.TabularEditorDialog;
import chs.caf.caplet.helpers.tabulareditor.TabularSelection;
import chs.caplets.logic.LogicObjectComparator;
import chs.caplets.logic.helpers.LogicObjectHierarchyProvider;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObject;
import chs.cog.ICOGLockable;
import chs.common.IAttributePropertyProvider;
import chs.common.ILockable;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.utilities.CommonUtils;
import chs.utilities.stream.StreamUtils;
import chs.utility.ITypeOrdinalProvider;
import chs.utility.IUIDObjectHierarchyProvider;
import chs.utility.helpers.BatchLockRefreshHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.persist.promise.IPromiseBatchLock;
import chs.utility.persist.promise.PromiseFactory;
import com.mentor.capital.javafx.table.ColumnInformation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author pbhawsar on 25-05-2017
 */
public class LogicTabularEditor extends TabularEditor
{

	@NotNull protected final List<ISharedObject> lockedSharedObjects;
	@NotNull private final IFilterableObjectType.ObjectClass m_objectClass;

	public LogicTabularEditor(ICapletController controller,
			@NotNull IFilterableObjectType.ObjectClass objectClass)
	{
		super(controller, new LogicTabularEditorObjectNameDerivation());
		lockedSharedObjects = new ArrayList<>();
		m_objectClass = objectClass;
	}

	@Override @NotNull protected TabularEditorDialog createDialog(@NotNull TabularSelection selectedObjects)
	{
		return new LogicTabularEditorDialog(selectedObjects, this);
	}

	@Override @NotNull protected IUIDObjectHierarchyProvider createUIDObjectHierarchyProvider()
	{
		final LogicTabularEditorChildrenProvider childrenProvider = new LogicTabularEditorChildrenProvider();
		final LogicTabularEditorParentProvider parentProvider = new LogicTabularEditorParentProvider();
		return new LogicObjectHierarchyProvider(childrenProvider, parentProvider);
	}

	@Override @NotNull public IFilterableObjectType.ObjectClass getTableObjectClass()
	{
		return m_objectClass;
	}

	@Override protected ITypeOrdinalProvider createTypeOrdinalProvider()
	{
		return new LogicObjectComparator();
	}

	@Override protected boolean initAndShowGUI(@NotNull TabularSelection selectedObjects)
	{
		processObjectsToLock(selectedObjects.getExtendedSelectionObjects());
		return super.initAndShowGUI(selectedObjects);
	}

	@Override public boolean terminate()
	{
		final boolean result = super.terminate();
		unlockSharedObjects();

		// disable undo if any shared object has been edited
		IUndoableContainer currentUndoableContainer = getController().getUndoableContainer();
		if (result && isSharedObjectEdited && currentUndoableContainer != null) {
			currentUndoableContainer.endEdit();
			getController().clearUndoQueue();
		}

		return result;
	}

	@Override public void cellValueChanged(IAttributePropertyProvider sourceItem,
			ColumnInformation<IAttributePropertyProvider> sourceColumnInfo, Object oldValue, Object newValue)
	{
		super.cellValueChanged(sourceItem, sourceColumnInfo, oldValue, newValue);
		if (!isSharedObjectEdited) {
			if (sourceItem instanceof ILogicObject && ((ILogicObject) sourceItem).isShared()) {
				isSharedObjectEdited = true;
			}
		}
	}

	private void processObjectsToLock(@NotNull List<IAttributePropertyProvider> propertyProviders)
	{
		List<ISharedObject> sharedObjectsToLock = propertyProviders.stream()
				.map(ReferenceHelper::reduceToLogicObject)
				.filter(StreamUtils::notNull)
				.map(ILogicObject::getSharedObject)
				.filter(StreamUtils::notNull)
				.distinct()
				.collect(Collectors.toList());

		performPreLockOperation(sharedObjectsToLock);
	}

	/**
	 * Perform pre lock operation on the shared objects and store the lock status corresponding to each object in promise map.
	 * So, while actually trying to lock the objects, it will take the lock status from memory itself and will not be going
	 * to DB.
	 *
	 * @param sharedObjectsToLock - shared objects on which the lock operation to be performed.
	 */
	private void performPreLockOperation(@NotNull List<ISharedObject> sharedObjectsToLock)
	{
		BatchLockRefreshHelper.filterAndLockCOGObjects(sharedObjectsToLock);
		Set<ILockable> filteredNonCOGObjects = BatchLockRefreshHelper.getFilteredNonCOGObjects(sharedObjectsToLock);
		batchLockNonCOGObjects(filteredNonCOGObjects, sharedObjectsToLock);
	}

	private void batchLockNonCOGObjects(@NotNull Set<ILockable> nonCOGObjects,
			@NotNull List<ISharedObject> sharedObjectsToLock)
	{
		IPromiseBatchLock promise = PromiseFactory.createPromiseBatchLock();
		promise
				.requestLockAndRefreshOf(nonCOGObjects)
				.issue()
				.thenApply(() -> {
					lockSharedObjects(sharedObjectsToLock);
				});
	}

	private void lockSharedObjects(@NotNull List<ISharedObject> sharedObjectsToLock)
	{
		for (ISharedObject sharedObjectToLock : sharedObjectsToLock) {
			if (isCOGObjectLocked(sharedObjectToLock)) {
				lockedSharedObjects.add(sharedObjectToLock);
				continue;
			}
			boolean lockSuccess = LockUpdateHelper.lockRefreshAndCheckForDomain(sharedObjectToLock, false);
			if (lockSuccess) {
				lockedSharedObjects.add(sharedObjectToLock);
			}
		}
		isSharedObjectEdited = false;
	}

	private boolean isCOGObjectLocked(@NotNull ISharedObject sharedObjectToLock)
	{
		if (sharedObjectToLock instanceof ICOGLockable) {
			ICOGLockable sharedCOGObjectToLock = CommonUtils.cast(sharedObjectToLock, ICOGLockable.class);
			if (sharedCOGObjectToLock != null) {
				return sharedCOGObjectToLock.isLocked();
			}
		}
		return false;
	}

	private void unlockSharedObjects()
	{
		for (ISharedObject sharedObject : lockedSharedObjects) {
			LockUpdateHelper.flushAndUnlockSharedObject(sharedObject);
		}
	}
}