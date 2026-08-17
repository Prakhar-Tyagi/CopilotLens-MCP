/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared;

/**
 * This interface is used to indicate that the action is related to the shared object browser.
 */
public interface ISharedObjectBrowserAction {

    static void setTreeConstructionComplete(boolean complete) {
        StateHolder.treeConstructionComplete = complete;
    }

    static boolean isTreeConstructionComplete() {
        return StateHolder.treeConstructionComplete;
    }

    // Static nested class to hold mutable state
    class StateHolder {
        // This flag indicates whether the tree construction is complete.
        private static boolean treeConstructionComplete = true;
    }
}
