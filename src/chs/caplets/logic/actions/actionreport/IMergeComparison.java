/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.common.INamedObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface IMergeComparison<T, V>
{

	// initial state of source object
	void setInitialStateOfSourceObject(@NotNull V obj);

	// initial state of target object in merge actions
	void setInitialStateOfTargetObject(@NotNull V obj);

	/* if the source object is changed , final state of source object
	   if the source object is merged, final state of merged object
	*/
	void setTransformedState(@NotNull V obj);

	void addObjectMapping(@NotNull INamedObject key, @NotNull INamedObject value);

	// collection of computed changes
	@NotNull Collection<T> computeChanges();

	void addChange(@NotNull T change);

	@Nullable String getSourceObjectName();

	Collection<IMergeComparison<T,V>> getChildren();

	@Nullable IMergeComparison<T,V> getParent();

	@Nullable ICachedObject getSourceObject();

	@Nullable ICachedObject getMergedObject();

	@Nullable ICachedObject getTargetObject();
}
