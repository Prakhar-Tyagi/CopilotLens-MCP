/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import org.jetbrains.annotations.NotNull;

import java.util.SortedSet;

/**
 * Interface to access connectivity details
 */
public interface IConnectivityInfo
{

	@NotNull SortedSet<String> getConnectedPinUIDs();
}
