/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.shared;

/**
 * Represents the outcome of closing windows during a design close operation.
 * <p>
 * This status is returned by {@link BaseLifecycle#closeAllWindows} and propagated to
 * {@link BaseLifecycle.WipeOutDesignOnSaveCompleteTask} to determine whether
 * {@code synchronizeChangeOnActivation} should be triggered on the currently active window.
 * <ul>
 *   <li>{@link #CLOSED} - a user-visible window was found and closed; synchronization is needed.</li>
 *   <li>{@link #NOT_CLOSED} - no window was closed (the design was only unloaded from memory);
 *       synchronization is not needed. This occurs, for example, when a logic design is
 *       temporarily loaded during generation from a functional design.</li>
 * </ul>
 *
 * @see BaseLifecycle#closeAllWindows
 * @see BaseLifecycle.WipeOutDesignOnSaveCompleteTask
 */
public enum WindowCloseStatus
{
	/**
	 * A user-visible caplet window was closed during the design close operation.
	 */
	CLOSED,

	/**
	 * No window was closed; the design was only unloaded from memory.
	 */
	NOT_CLOSED
}
