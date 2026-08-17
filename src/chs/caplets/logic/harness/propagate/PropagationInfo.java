/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.cof.logical.IPropagationInfo;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Class to hold the information from the Fx table
 */
public class PropagationInfo implements IPropagationInfo
{

	private Set<ILogicObject> m_logicObjects;
	private Map<ISharedObject, ILogicObject> m_sharedObjects;
	private Set<ILogicObject> m_logicObjectsToSkip;
	private Map<ISharedObject, ILogicObject> m_sharedObjectsToSkip;
	private String harness;
	private IUID m_designUid;


	public PropagationInfo(@NotNull IUID designUid, @NotNull Set<ILogicObject> logicObjects, @NotNull Map<ISharedObject, ILogicObject> sharedObjects,
			@NotNull Set<ILogicObject> logicObjectsToSkip, @NotNull Map<ISharedObject, ILogicObject> sharedObjectsToSkip,
			@NotNull String harness)
	{
		m_designUid = designUid;
		m_logicObjects = logicObjects;
		m_sharedObjects = sharedObjects;
		m_logicObjectsToSkip = logicObjectsToSkip;
		m_sharedObjectsToSkip = sharedObjectsToSkip;
		this.harness = harness;
	}

	@NotNull @Override public IUID getDesignUid()
	{
		return m_designUid;
	}

	@Override @NotNull public Set<ILogicObject> getLogicObjects()
	{
		return Collections.unmodifiableSet(m_logicObjects);
	}

	@Override @NotNull public Map<ISharedObject, ILogicObject> getSharedObjects()
	{
		return Collections.unmodifiableMap(m_sharedObjects);
	}

	@Override @NotNull public Set<ILogicObject> getLogicObjectsToSkip()
	{
		return Collections.unmodifiableSet(m_logicObjectsToSkip);
	}

	@Override @NotNull public Map<ISharedObject, ILogicObject> getSharedObjectsToSkip()
	{
		return Collections.unmodifiableMap(m_sharedObjectsToSkip);
	}

	@Override @NotNull public String getHarness()
	{
		return StringUtils.nonNull(harness);
	}
}
