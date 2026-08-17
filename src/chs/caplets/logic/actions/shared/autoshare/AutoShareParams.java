/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.logical.shared.ISharedObject;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Wrapper for auto share parameters
 */
public class AutoShareParams
{

	private boolean m_shareUnplacedObjects;
	private boolean m_makePinsReusable;
	private boolean m_mateCompatibilityCheck;
	@NotNull private BiConsumer<IUIDObject, ISharedObject> m_preShareTask = (uidObj, sharedObj) -> {
	};

	public AutoShareParams()
	{
		setShareUnplacedObjects(false);
		setMakePinsReusable(true);
		setMateCompatibilityCheck(false);
	}

	public AutoShareParams(boolean shareUnplacedObjects, boolean makePinsReusable, boolean mateCompatibilityCheck)
	{
		setShareUnplacedObjects(shareUnplacedObjects);
		setMakePinsReusable(makePinsReusable);
		setMateCompatibilityCheck(mateCompatibilityCheck);
	}

	public boolean doShareUnplacedObjects()
	{
		return m_shareUnplacedObjects;
	}

	public void setShareUnplacedObjects(boolean shareUnplacedObjects)
	{
		m_shareUnplacedObjects = shareUnplacedObjects;
	}

	public boolean doMakePinsReusable()
	{
		return m_makePinsReusable;
	}

	public void setMakePinsReusable(boolean makePinsReusable)
	{
		m_makePinsReusable = makePinsReusable;
	}

	public boolean getMateCompatibilityCheck()
	{
		return m_mateCompatibilityCheck;
	}

	public void setMateCompatibilityCheck(boolean mateCompatibilityCheck)
	{
		m_mateCompatibilityCheck = mateCompatibilityCheck;
	}

	@NotNull public BiConsumer<IUIDObject, ISharedObject> getPreShareTask()
	{
		return m_preShareTask;
	}

	public void setPreShareTask(@NotNull BiConsumer<IUIDObject, ISharedObject> preShareTask)
	{
		m_preShareTask = preShareTask;
	}

	public boolean usePinReservationPreferenceSetting()
	{
		return false;
	}
}
