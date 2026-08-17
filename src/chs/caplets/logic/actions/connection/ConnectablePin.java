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
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a pin and set of schem pins that will be used for making connection
 */
public class ConnectablePin implements IConnectablePin
{

	@NotNull private final IAbstractPin m_pin;
	@NotNull private final Set<IAbstractSchemPin> m_schemPins;

	public ConnectablePin(@NotNull IAbstractPin pin, @NotNull Set<IAbstractSchemPin> schemPins)
	{
		m_pin = pin;
		if (schemPins.isEmpty()) {
			throw new IllegalArgumentException("Atleast one schem representation is required");
		}
		m_schemPins = new HashSet<>(schemPins);
	}

	@NotNull @Override public Set<IAbstractSchemPin> getSchemRepresentations()
	{
		return Collections.unmodifiableSet(m_schemPins);
	}

	@NotNull @Override public IAbstractPin getPin()
	{
		return m_pin;
	}

	@NotNull @Override public Collection<? extends IUIDObject> getLockable()
	{
		Set<IUIDObject> objects = new HashSet<>();
		objects.add(getPin().getOwner());
		objects.addAll(getPin().getConductorsAsSet());
		getSchemRepresentations().stream().forEach(schemPin -> addHighway(objects, schemPin));
		return objects;
	}

	private void addHighway(@NotNull Set<IUIDObject> objects, @NotNull IAbstractSchemPin abstractSchemPin)
	{
		if (abstractSchemPin instanceof ISchemStackPin) {
			for (IHighwaySchematic highwaySchematic : ((ISchemStackPin) abstractSchemPin).getConnectedHighways()) {
				objects.add(highwaySchematic.getConnectivity());
			}
		}
	}

	@Override public int compareTo(@NotNull IConnectablePin o)
	{
		return m_pin.getUID().compareTo(o.getPin().getUID());
	}

	@Override public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ConnectablePin connectablePin = (ConnectablePin) obj;
		return m_pin.equals(connectablePin.getPin());
	}

	@Override public int hashCode()
	{
		return m_pin.hashCode();
	}
}