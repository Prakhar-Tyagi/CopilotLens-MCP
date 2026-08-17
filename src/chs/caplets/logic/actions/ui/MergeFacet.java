package chs.caplets.logic.actions.ui;

import chs.bridges.harness.MergeHelper;
import chs.common.Value;
import chs.common.ValueTypeEnum;
import chs.common.attr.IAttributeTypes;
import chs.common.attr.IReadOnlyFacet;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author chandras on 01-04-2018.
 */
public class MergeFacet implements IMergeFacet
{

	private final String m_name;
	private final String m_displayName;
	private final String m_value;
	private final String m_rawValue;
	private final ValueTypeEnum m_type;
	private final IFacetStature m_stature;

	public MergeFacet(@NotNull IReadOnlyFacet facet)
	{
		m_name = facet.getName();
		m_displayName = facet.getDisplayName();
		m_type = facet.getType();
		m_value = facet.getAsString();
		m_rawValue = facet instanceof Value ? ((Value) facet).getRawString() : facet.getAsString();
		m_stature = determineStature(facet);
	}

	public MergeFacet(@NotNull String name, @NotNull String displayName, @Nullable String value,
			@NotNull ValueTypeEnum type, @NotNull IFacetStature stature)
	{
		m_name = name;
		m_displayName = displayName;
		m_type = type;
		m_value = value;
		m_rawValue = value;
		m_stature = stature;
	}

	@NotNull public static IFacetStature determineStature(@NotNull IReadOnlyFacet facet)
	{
		if (facet.isAttribute()) {
			if (facet.getStability().isEditable()) {
				return FacetStature.EDITABLE_ATTR;
			}
			return FacetStature.READ_ONLY_ATTR;
		}
		if (facet.getStability().isEditable()) {
			return FacetStature.EDITABLE_PROP;
		}
		return FacetStature.READ_ONLY_PROP;
	}

	@NotNull @Override public String getName()
	{
		return m_name;
	}

	@NotNull @Override public String getDisplayName()
	{
		return m_displayName;
	}

	@Nullable @Override public String getValue()
	{
		return m_value;
	}

	@Nullable @Override public String getRawValue()
	{
		return m_rawValue;
	}

	@NotNull @Override public ValueTypeEnum getType()
	{
		return m_type;
	}

	@Override public boolean isEqual(@NotNull IMergeFacet otherFacet)
	{
		return getType().equals(otherFacet.getType()) && areFacetValuesEqual(otherFacet);
	}

	private boolean areFacetValuesEqual(@NotNull IMergeFacet otherFacet)
	{
		if (IAttributeTypes.OPTION_EXP.equals(m_name)) {
			return MergeHelper.compareOptionExpressions(getValue(), otherFacet.getValue(), true) == 0;
		}
		return StringUtils.areEqualOrBothNull(getValue(), otherFacet.getValue());
	}

	@Override public boolean isEditable()
	{
		return m_stature.isEditable();
	}

	@Override public boolean isAttribute()
	{
		return m_stature.isAttribute();
	}
}
