/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.cof.COFTypeEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * connector snapshot
 */
public class ConnectorSnapShot extends CachedObject implements IConnectorSnapShot
{

	@Nullable private String backShellUID;
	@NotNull private final Set<String> pins;

	public ConnectorSnapShot(@Nullable ICachedObject parent, @NotNull String name,
			@NotNull String uid, @Nullable String designUID,
			@NotNull COFTypeEnum objectType)
	{
		super(parent, name, uid, designUID, objectType);
		pins = new HashSet<>();
	}

	@Nullable @Override public String getBackShellUID()
	{
		return backShellUID;
	}

	@Override public void addBackShellUID(@NotNull String uid)
	{
		backShellUID = uid;
	}

	@NotNull @Override public Collection<String> getPinUIDs()
	{
		return Collections.unmodifiableSet(pins);
	}

	@Override public void addConnectorPinUID(@NotNull String uid)
	{
		pins.add(uid);
	}
}
