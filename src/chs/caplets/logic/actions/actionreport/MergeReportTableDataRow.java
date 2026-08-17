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

import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

/**
 * csv data row
 */
public class MergeReportTableDataRow implements IMergeReportTableDataRow
{

	public static final String COMMA_SEPERATOR = ",";
	@NotNull private String mKey;
	@Nullable private String mSourceValue;
	@Nullable private String mTargetValue;
	@Nullable private String mMergedValue;
	private IReportTableDataRow.DisplayInformationType mType;
	private String mObjectName;
	@Nullable private String m_details;

	public MergeReportTableDataRow(@NotNull String key, @Nullable String sourceValue,
			@Nullable String targetValue, @Nullable String mergedValue, @NotNull
			IReportTableDataRow.DisplayInformationType type,
			String objectName, @Nullable String details)
	{
		mKey = key;
		mSourceValue = sourceValue;
		mTargetValue = targetValue;
		mMergedValue = mergedValue;
		mType = type;
		mObjectName = objectName;
		m_details = details;
	}

	@Nullable @Override public String getInitialTargetValue()
	{
		return mTargetValue;
	}

	@NotNull @Override public String getObjectName()
	{
		return mObjectName;
	}


	@NotNull @Override public String getCSVData()
	{
		StringJoiner joiner = new StringJoiner(COMMA_SEPERATOR);
		joiner.add(mObjectName).add(mKey).add(mSourceValue).add(mTargetValue).add(mMergedValue)
				.add(getDetails());
		return joiner.toString();
	}

	@Override @NotNull public String getDisplayInformationType()
	{
		return mType.getName();
	}

	@Override @NotNull public String getKey()
	{
		return mKey;
	}

	@Nullable @Override public String getInitialValue()
	{
		return mSourceValue;
	}

	@Nullable @Override public String getTransformedValue()
	{
		return mMergedValue;
	}

	@Override
	@NotNull public String getDetails()
	{
		return StringUtils.nonNull(m_details);
	}
}