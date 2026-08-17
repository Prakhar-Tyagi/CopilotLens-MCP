/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.shared.properties;

import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IUID;
import com.mentor.capital.logging.ILogger;
import com.mentor.capital.logging.LoggingService;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * A specialized {@link PropertiedSet} for the QuickAccessPanel behavior.
 * <p>
 * This class avoids locking shared objects to facilitate efficient access and modifications without typical lock
 * overhead, focusing on direct edit scenarios only when user interacts with UI.
 */
@SuppressWarnings("SpellCheckingInspection")
public class QAPLogicPropertiedSet extends PropertiedSet
{

	private static final ILogger LOGGER = LoggingService.INSTANCE.getLogger(QAPLogicPropertiedSet.class);

	/**
	 * Constructs a {@code QAPLogicPropertiedSet} for handling properties
	 * in the QuickAccessPanel, bypassing standard lock operations on initialization.
	 *
	 * @param selections               the selected set of properties
	 * @param safemodel                the model context
	 * @param willEditSharedObjects    flag indicating shared object edit potential
	 *                                 (This is ignored in QAPLogicPropertiedSet), as will never edit shared objects
	 * @param checkArtificallyReadOnly flag for artificial read-only checks
	 */
	public QAPLogicPropertiedSet(SelectSet selections, ICapletModel safemodel, boolean willEditSharedObjects,
			boolean checkArtificallyReadOnly)
	{
		super(selections, safemodel, willEditSharedObjects, checkArtificallyReadOnly);
		performOperationOnSharedObjects();
	}

	/**
	 * Fetches an empty set of dependent multicore UIDs, indicating no locks.
	 * <p>
	 * This method ensures no dependent multicore objects are considered for locking.
	 * </p>
	 */
	@NotNull protected Set<IUID> fetchDependentMCtoLock(ISharedMulticore shared)
	{
		LOGGER.debug("Invoked fetchDependentMCtoLock: No dependent multicores fetched for " + shared);
		return Collections.emptySet();
	}

	/**
	 * Fetches an empty set of shared connector mates, skipping locks.
	 * <p>
	 * Designed to exclude any shared connector mates from the lock process.
	 * </p>
	 */
	@NotNull
	protected Set<ISharedConnector> fetchMatesForSCtoLock(ISharedConnector shared)
	{
		LOGGER.debug("Invoked fetchMatesForSCtoLock: No mates fetched for shared object " + shared);
		return Collections.emptySet();
	}

	/**
	 * Overrides locking of MC dependents and shared mates, executing no actions.
	 * <p>
	 * Maintains focus on non-lock environments for the QAP logic requirements.
	 * </p>
	 */
	protected void lockMCDependentsAndSharedMates(@NotNull Set<IUID> sharedMCDependents,
			@NotNull Set<ISharedPinList> sharedMates)
	{
		// Intentionally avoids locking
		LOGGER.debug("Invoked lockMCDependentsAndSharedMates: No locking performed for " +
				sharedMCDependents.size() + " dependents and " + sharedMates.size() + " shared mates.");
	}

	/**
	 * Skips locking the shared object during property set creation for QAP.
	 */
	@Override void lockShared(@NotNull ISharedObject shared)
	{
		// Intentionally avoids lock operations
		LOGGER.debug("Invoked lockShared: No locking performed for " + shared + " object.");
	}

	/**
	 * This method is overridden such that we do not perform any locking operation as it is essential to acquire lock
	 * only when user is editing the UI in QAP
	 */
	@Override protected void performLockOperation()
	{
		// Intentionally avoids lock operations
		LOGGER.debug("Invoked performLockOperation: Skipping operation as QAP Propertied set should not perform this " +
				"operation.");
	}

	/**
	 * Overridden to perform no action when adding shared objects for the QAP context.
	 *
	 * <p>This method provides a no-operation implementation for adding shared objects to a list.
	 * It is intentionally left empty because the QAP manages locking of shared
	 * objects independently and does not require this method's functionality.</p>
	 *
	 * @param shared the shared object, which is ignored in this implementation
	 */
	@Override
	protected void lockAndAddToSharedObjectList(ISharedObject shared)
	{
		// Intentionally left blank
		LOGGER.debug("Invoked lockAndAddToSharedObjectList: Skipping operation as QAP will acquire lock only on UI " +
				"edit");
	}
}