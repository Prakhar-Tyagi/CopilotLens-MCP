/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.connection;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * Represents a pin and set of schem pins that will be used for making connection
 */
public interface IConnectablePin extends Comparable<IConnectablePin>
{

	@NotNull Set<IAbstractSchemPin> getSchemRepresentations();

	@NotNull IAbstractPin getPin();

	@NotNull Collection<? extends IUIDObject> getLockable();
}