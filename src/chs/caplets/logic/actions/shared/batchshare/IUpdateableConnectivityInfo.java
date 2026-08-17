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

/**
 * Interface to update connectivity details
 */
interface IUpdateableConnectivityInfo extends IConnectivityInfo
{

	void addPinConnection(@NotNull String pinUID);
}
