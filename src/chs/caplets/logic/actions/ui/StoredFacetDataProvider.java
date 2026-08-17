package chs.caplets.logic.actions.ui;

import chs.common.INamedPropertiedObject;
import chs.utilities.CollectionUtils;
import chs.utility.attr.AttributeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author chandras on 01-04-2018.
 */
public class StoredFacetDataProvider extends FacetDataProvider
{

	@NotNull private final HashSet<IMergeFacet> m_facets = new HashSet<>();
	@NotNull private final String m_name;

	public StoredFacetDataProvider(@NotNull INamedPropertiedObject object)
	{
		super(object);
		m_name = object.getName();
		m_facets.addAll(doGetMergeFacets(AttributeUtils.getUserVisibleAttributes(m_object)));
		m_facets.addAll(doGetMergeFacets(m_object.getProperties()));
	}

	@NotNull @Override public String getName()
	{
		return m_name;
	}

	@NotNull @Override public Collection<IMergeFacet> getAttributes()
	{
		return CollectionUtils.getFilteredCollection(m_facets, (f) -> f.isAttribute());
	}

	@NotNull @Override public Collection<IMergeFacet> getProperties()
	{
		return CollectionUtils.getFilteredCollection(m_facets, (f) -> !f.isAttribute());
	}
}
