/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.layout.sync.rules;

import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.project.IProject;
import chs.common.sync.AbstractBaseSyncRule;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.common.sync.ISyncRule;
import chs.utilities.LazyEvaluatedOptional;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractLayoutDesignSyncRule extends AbstractBaseSyncRule<ILayoutLogicDesign>
{

	private final LazyEvaluatedOptional<Set<Class<? extends ISyncRule<?>>>> dependents = new LazyEvaluatedOptional<>();

	protected AbstractLayoutDesignSyncRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected AbstractLayoutDesignSync getSync()
	{
		return (AbstractLayoutDesignSync) super.getSync();
	}

	@NotNull @Override protected Class<?> getProgressResourceClass()
	{
		return getClass();
	}

	@NotNull @Override protected IProject getProject()
	{
		final IProject project = getSync().getDesign().getProject();
		assert project != null;
		return project;
	}

	@NotNull
	public Set<Class<? extends ISyncRule<?>>> getRuleDependencies()
	{
		return dependents.getValueOrElse(this::getDependencies, Collections.emptySet());
	}

	@NotNull
	private Set<Class<? extends ISyncRule<?>>> getDependencies()
	{
		final Set<Class<? extends ISyncRule<?>>> deps = new HashSet<>(2);
		deps.add(getClass());
		deps.addAll(getOtherDependencies());
		return deps;
	}

	@NotNull protected Set<Class<? extends ISyncRule<?>>> getOtherDependencies()
	{
		return Collections.emptySet();
	}

	public boolean execute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		executeStarted();
		setMessageSource(getMessageSourceResourceName());
		return doExecute(design, reporter);
	}

	@NotNull protected abstract String getMessageSourceResourceName();

	protected abstract boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter);
}
