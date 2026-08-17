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

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.IConnectivityMatchCriteria;
import chs.cof.project.IProject;
import chs.common.IDesignAbstraction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class DerivativeCriteriaProvider extends CriteriaProvider
{

	@NotNull @Override
	protected List<IConnectivityMatchCriteria> getCriteriaForAbstraction(@Nullable IDesignAbstraction designAbstraction)
	{
		IProject project = CAFUtils.getInstance().getCurrentProject();
		if (project == null) {
			return new ArrayList<>();
		}

		return project.getPreferences().getConnectivityMatchCriterias()
				.stream()
				.filter(criteria -> criteria.isValid())
				.collect(Collectors.toList());
	}
}
