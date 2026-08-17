/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class HarnessPropagateStatusMessageGroup implements IHarnessPropagateStatusMessageGroup
{

	private List<IHarnessPropagateStatusMessage> m_elements = new ArrayList<>(1);

	public HarnessPropagateStatusMessageGroup()
	{
	}

	public void addElement(@NotNull IHarnessPropagateStatusMessage element)
	{
		if (!m_elements.contains(element)) {
			m_elements.add(element);
		}
	}

	@NotNull @Override public Collection<IHarnessPropagateStatusMessage> getElements()
	{
		return Collections.unmodifiableList(m_elements);
	}
}
