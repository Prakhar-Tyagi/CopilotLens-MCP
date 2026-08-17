/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.shared;

import chs.cof.logical.shared.ISharedConductorIterator;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedFunctionMessageIterator;
import chs.cof.logical.shared.ISharedGeneralHighwayIterator;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.cof.logical.shared.ISharedOverbraidIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListIterator;
import chs.cof.logical.shared.ISharedSingleLineIterator;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;

/**
 * Class used to cache all data in SharedPinListMgr and SharedConductorMgr to prevent ConcurrentModificationException
 * when the background thread and EDT access or update the shared tree at the same time.
 */
public class SharedTreeBackgroundCache
{

	private ISharedPinListIterator mSharedPinLists;
	private ISharedConductorIterator mSharedConductors;
	private ISharedMulticoreIterator mSharedMultiCores;

	private ISharedFunctionMessageIterator mSharedFunctionMessages;
	private ISharedOverbraidIterator mSharedOverBraids;
	private ISharedSingleLineIterator mSharedSingleLines;

	private ISharedGeneralHighwayIterator mSharedHighways;
	private IProject mProject;

	public SharedTreeBackgroundCache(IProject project)
	{
		mProject = project;
		initCache();
	}

	private void initCache()
	{
		mSharedPinLists = ((ISharedFullyLoadedPinListMgr) mProject.getSharedPinListMgr()).getSharedPinLists();
		ISharedConductorMgr sharedConductorMgr = mProject.getSharedConductorMgr();
		mSharedConductors = sharedConductorMgr.getSharedConductors();
		mSharedMultiCores = sharedConductorMgr.getSharedMulticores();
		mSharedFunctionMessages = sharedConductorMgr.getSharedFunctionMessages();

		mSharedHighways = sharedConductorMgr.getSharedGeneralHighways();
		mSharedOverBraids = sharedConductorMgr.getSharedOverbraids();
		mSharedSingleLines = sharedConductorMgr.getSharedSingleLines();
	}

	@NotNull
	public ISharedConductorIterator getSharedConductors()
	{
		return mSharedConductors;
	}

	@NotNull
	public ISharedMulticoreIterator getSharedMultiCores()
	{
		return mSharedMultiCores;
	}

	@NotNull
	public ISharedOverbraidIterator getSharedOverBraids()
	{
		return mSharedOverBraids;
	}

	@NotNull
	public ISharedFunctionMessageIterator getSharedFunctionMessages()
	{
		return mSharedFunctionMessages;
	}

	@NotNull
	public ISharedGeneralHighwayIterator getSharedHighways()
	{
		return mSharedHighways;
	}

	@NotNull
	public ISharedSingleLineIterator getSharedSingleLines()
	{
		return mSharedSingleLines;
	}

	@NotNull
	public Iterable<? extends ISharedPinList> getSharedPinLists()
	{
		return mSharedPinLists;
	}
}
