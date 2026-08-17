/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.cof.logical.shared.ISharedObjectModificationObserver;

/**
 * Observe and track modifications to shared objects. It stores the modification status
 * of the shared object and allows us to check and update its state.
 */
public class SharedObjectModificationObserver implements ISharedObjectModificationObserver
{
    private boolean m_isSharedObjectModified = false;

    @Override public boolean isModified()
    {
        return m_isSharedObjectModified;
    }

    @Override public void setModified()
    {
        m_isSharedObjectModified = true;
    }
}