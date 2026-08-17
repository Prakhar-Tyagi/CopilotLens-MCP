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

import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Interface for fetching object Infos in a given set of designs
 */
public interface IObjectInfoProvider
{

	@NotNull Collection<IObjectInfo> getObjectInfos(@NotNull Set<ILogicDesign> designs,
			@NotNull Set<ShareableEntityTypeEnum> candidateTypes, @NotNull Predicate<IObjectInfo> objectInfoFilter);
}
