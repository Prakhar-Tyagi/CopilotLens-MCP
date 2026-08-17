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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filters out Pins based on Criteria based on Criteria defined in project prefrences.
 */
public class CriteriaProvider
{

	private Map<IDesignAbstraction, List<IConnectivityMatchCriteria>> criteriaListMap = new HashMap<>();

	@NotNull
	protected List<IConnectivityMatchCriteria> getCriteriaForAbstraction(@Nullable IDesignAbstraction designAbstraction)
	{
		if (criteriaListMap.containsKey(designAbstraction)) {
			return criteriaListMap.get(designAbstraction);
		}

		IProject project = CAFUtils.getInstance().getCurrentProject();
		assert project != null;

		List<IConnectivityMatchCriteria> criteriaList = new ArrayList<>();

		List<IConnectivityMatchCriteria> allCriteriaList = project.getPreferences().getConnectivityMatchCriterias();
		if (designAbstraction != null) {
			for (IConnectivityMatchCriteria criteria : allCriteriaList) {
				if (criteria.isValid() &&
						criteria.getAbstractionsHolder().getDesignAbstractions().contains(designAbstraction)) {
					criteriaList.add(criteria);
				}
			}
		}

		if (criteriaList.isEmpty()) {
			for (IConnectivityMatchCriteria criteria : allCriteriaList) {
				if (criteria.isValid() && criteria.isDefaultCriteria()) {
					criteriaList.add(criteria);
				}
			}
		}
		criteriaListMap.put(designAbstraction, criteriaList);
		return criteriaList;
	}
}