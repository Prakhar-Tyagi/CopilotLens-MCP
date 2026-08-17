/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBackshellTerminationIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellOwner;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedBackshellTerminationIterator;
import chs.cof.logical.shared.ISharedConnector;
import chs.common.INamedPropertiedObject;
import chs.common.RefreshStatusEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public interface IBackshellUtils
{

	@NotNull IConnector getConnector();

	@Nullable default IBackshell getExistingBackshell()
	{
		return getConnector().getBackshell();
	}

	@Nullable default ISharedBackshell getSharedBackshell()
	{
		final IConnector connector = getConnector();

		if (connector.getSharedObject() != null) {
			// We may have a shared backshell, but no local instance of it yet
			ISharedBackshellOwner sc = (ISharedBackshellOwner) connector.getSharedObject();
			return sc.getBackshell();
		}
		return null;
	}

	default Set<INamedPropertiedObject> getExistingBackshellTerminations()
	{
		IBackshell bs = getExistingBackshell();
		ISharedBackshell sbs = getSharedBackshell();

		//
		// If we have a backshell, then get the values.. .
		//
		Set<INamedPropertiedObject> existingBackehellTerminations = new LinkedHashSet<>();
		if (bs != null || sbs != null) {

			// get any shared terminations that we may want to add
			if (hasNonDeletedSharedBackshell(sbs)) {
				// Get the shared backshell off the shared connector again, as the refresh wont actually refresh this
				// object, and it may now be out of date
				sbs = getSharedBackshell();
				assert sbs != null;
				for (ISharedBackshellTerminationIterator btitr = sbs.getBackshellTerminations(); btitr.hasNext(); ) {
					ISharedBackshellTermination bt = btitr.getNext();
					existingBackehellTerminations.add(bt);
				}
			}
			else {
				assert bs != null;
				for (IBackshellTerminationIterator btitr = bs.getBackshellTerminations(); btitr.hasNext(); ) {
					IBackshellTermination bt = btitr.getNext();
					existingBackehellTerminations.add(bt);
				}
			}
		}
		return existingBackehellTerminations;
	}

	default boolean hasNonDeletedSharedBackshell(@Nullable ISharedBackshell sbs)
	{
		return sbs != null && sbs.refresh() != RefreshStatusEnum.eObjectDoesNotExist;
	}
}
