package chs.caplets.logic.actions.ui;

import chs.common.INamedPropertiedObject;
import chs.utility.attr.AttributeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author chandras on 01-04-2018.
 */
public class DelegateFacetDataProvider extends FacetDataProvider
{

	public DelegateFacetDataProvider(@NotNull INamedPropertiedObject object)
	{
		super(object);
	}

	@NotNull @Override public String getName()
	{
		return m_object.getName();
	}

	@NotNull @Override public Collection<IMergeFacet> getAttributes()
	{
		return doGetMergeFacets(AttributeUtils.getUserVisibleAttributes(m_object));
	}

	@NotNull @Override public Collection<IMergeFacet> getProperties()
	{
		return doGetMergeFacets(m_object.getProperties());
	}
}
