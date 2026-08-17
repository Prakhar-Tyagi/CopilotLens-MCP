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

import java.util.Collection;

/**
 * Group object for Propagate Harness rows
 */
public interface IHarnessPropagateStatusMessageGroup
{

	void addElement(@NotNull IHarnessPropagateStatusMessage element);

	@NotNull Collection<IHarnessPropagateStatusMessage> getElements();
}
