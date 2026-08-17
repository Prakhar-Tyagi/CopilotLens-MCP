/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caplets.logic.actions.shared.batchshare.ui.BatchShareRow;
import chs.common.IDesignAbstraction;
import chs.common.IDesignDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class providing common functionality for row data objects in batch share/unshare table operations.
 * <p>
 * This class implements the {@link IShareRow} interface and serves as the foundation for concrete
 * row implementations such as {@link BatchShareRow}. It manages the state and data access for objects
 * displayed in share/unshare table views, encapsulating selection state and design context.
 */
public abstract class AbstractShareRow implements IShareRow
{

	@NotNull private final String m_designName;
	private boolean m_selected;
	@NotNull private final String m_designAbstraction;
	@Nullable protected final IDesignDescriptor m_designDescriptor;

	protected AbstractShareRow(@Nullable IDesignDescriptor designDescriptor)
	{
		m_designDescriptor = designDescriptor;
		m_selected = false;

		if (m_designDescriptor != null) {
			m_designName = m_designDescriptor.getFullName();
			IDesignAbstraction abstraction = m_designDescriptor.getDesignAbstraction();
			m_designAbstraction = abstraction != null ? abstraction.getName() : "";
		}
		else {
			m_designName = "";
			m_designAbstraction = "";
		}
	}

	@Override public boolean isSelected()
	{
		return m_selected;
	}

	@Override public void setSelected(boolean selected)
	{
		m_selected = selected;
	}

	@Override @NotNull public String getDesignName()
	{
		return m_designName;
	}

	@Override @NotNull public String getDesignAbstraction()
	{
		return m_designAbstraction;
	}
}

