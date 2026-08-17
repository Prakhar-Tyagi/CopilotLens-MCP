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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * merge action change
 */
public class MergeActionChange implements IMergeActionChange
{

	private final String key;
	private final String sourceValue;
	@Nullable private final String targetValue;
	private final String mergedValue;
	private final IActionChange.ComparisonField field;
	private final String sourceObjectName;
	private final String sourceObjectType;
	@Nullable private final String m_details;


	public MergeActionChange(String key, String sourceValue, @Nullable String targetValue, String mergedValue,
			IActionChange.ComparisonField field, String sourceObjectName, String sourceObjectType,
			@Nullable String details)
	{
		this.key = key;
		this.sourceValue = sourceValue;
		this.targetValue = targetValue;
		this.mergedValue = mergedValue;
		this.field = field;
		this.sourceObjectName=sourceObjectName;
		this.sourceObjectType = sourceObjectType;
		m_details = details;
	}

	@NotNull @Override public String getKey()
	{
		return key;
	}

	@Nullable @Override public String getInitialValue()
	{
		return sourceValue;
	}

	@Nullable @Override public String getTransformedValue()
	{
		return mergedValue;
	}

	@NotNull @Override public IActionChange.ComparisonField getKeyType()
	{
		return field;
	}

	@Nullable @Override public String getInitialTargetValue()
	{
		return targetValue;
	}

	@Nullable @Override public String getSourceObjectName()
	{
		return sourceObjectName;
	}

	@NotNull @Override public String sourceObjectType()
	{
		return sourceObjectType;
	}

	@Override
	@Nullable public String getDetails()
	{
		return m_details;
	}
}