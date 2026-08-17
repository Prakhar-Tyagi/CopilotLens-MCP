package chs.caplets.logic.actions.ui;

import chs.common.ValueTypeEnum;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 08-03-2018.
 */
public class FacetConflictInfo implements IFacetConflictInfo
{

	private final IMergeFacet m_sourceVal;
	private final IMergeFacet m_targetVal;
	private ValueOption m_choice = ValueOption.Target;

	public FacetConflictInfo(@NotNull IMergeFacet sourceVal, @NotNull IMergeFacet targetVal)
	{
		m_sourceVal = sourceVal;
		m_targetVal = targetVal;
	}

	@Override public boolean isAttribute()
	{
		return m_sourceVal.isAttribute();
	}

	@NotNull @Override public String getDisplayName()
	{
		return m_sourceVal.getDisplayName();
	}

	@NotNull @Override public String getName()
	{
		return m_sourceVal.getName();
	}

	@NotNull @Override public String getSourceValue()
	{
		return StringUtils.nonNull(m_sourceVal.getValue());
	}

	@NotNull @Override public String getTargetValue()
	{
		return StringUtils.nonNull(m_targetVal.getValue());
	}

	@NotNull @Override public String getSourceRawValue()
	{
		return StringUtils.nonNull(m_sourceVal.getRawValue());
	}

	@NotNull @Override public String getTargetRawValue()
	{
		return StringUtils.nonNull(m_targetVal.getRawValue());
	}

	@NotNull @Override public ValueTypeEnum getSourceType()
	{
		return m_sourceVal.getType();
	}

	@NotNull @Override public ValueTypeEnum getTargetType()
	{
		return m_targetVal.getType();
	}

	@NotNull @Override public ValueTypeEnum getResultType()
	{
		return ValueOption.Source.equals(m_choice) ? m_sourceVal.getType() : m_targetVal.getType();
	}

	@Override public void setUserChoice(@NotNull ValueOption choice)
	{
		m_choice = choice;
	}

	@NotNull @Override public ValueOption getUserChoice()
	{
		return m_choice;
	}

	@Override public void setResult(@NotNull String result)
	{
		m_choice = getTargetValue().equals(result) ? ValueOption.Target : ValueOption.Source;
	}

	@NotNull @Override public String getResult()
	{
		return ValueOption.Source.equals(m_choice) ? getSourceValue() : getTargetValue();
	}

	@NotNull @Override public String getRawResult()
	{
		return ValueOption.Source.equals(m_choice) ? getSourceRawValue() : getTargetRawValue();
	}
}
