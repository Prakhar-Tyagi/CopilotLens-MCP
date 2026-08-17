package chs.caplets.logic.actions.ui;

import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IModuleCodeInformationProvider;
import chs.cof.logical.shared.ISharedConductor;
import chs.common.IAssembledObject;
import chs.common.INamedPropertiedObject;
import chs.common.IUID;
import chs.common.ValueTypeEnum;
import chs.common.attr.AttributeType;
import chs.common.attr.AttributeValidatorFactory;
import chs.common.attr.IAttributeTypes;
import chs.common.attr.IReadOnlyFacet;
import chs.utilities.CaseLessStringKey;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author chandras on 01-04-2018.
 */
public abstract class FacetDataProvider implements IFacetDataProvider
{

	protected INamedPropertiedObject m_object;

	protected FacetDataProvider(@NotNull INamedPropertiedObject object)
	{
		m_object = object;
	}

	@NotNull @Override public INamedPropertiedObject getObject()
	{
		return m_object;
	}

	@NotNull @Override public COFTypeEnum getType()
	{
		return COFTypeEnum.from_object(m_object);
	}

	@NotNull @Override public IUID getUID()
	{
		return m_object.getUID();
	}

	@NotNull protected Collection<IMergeFacet> doGetMergeFacets(Iterable<? extends IReadOnlyFacet> attributes)
	{
		Map<CaseLessStringKey, IReadOnlyFacet> availableFacets = new HashMap<>();
		for (IReadOnlyFacet attribute : attributes) {
			if (!isIgnoredAttribute(attribute)) {
				availableFacets.put(CaseLessStringKey.toKey(attribute.getName()), attribute);
			}
		}
		HashSet<IMergeFacet> facets = new HashSet<>();
		for (IReadOnlyFacet facet : availableFacets.values()) {
			boolean alreadyProcessed = false;
			if (facet.isAttribute() && (m_object instanceof IConductor || m_object instanceof ISharedConductor)) {
				String name = facet.getName();
				if (IAttributeTypes.WIRE_CSA.equalsIgnoreCase(name)) {
					double wireCSA = facet.getDouble();
					if (AttributeValidatorFactory.shouldWireCSABeShownAsBlank(wireCSA)) {
						facets.add(new MergeFacet(facet.getName(), facet.getDisplayName(), "", facet.getType(),
								MergeFacet.determineStature(facet)));
						alreadyProcessed = true;
					}
				}
			}
			if (!alreadyProcessed) {
				facets.add(new MergeFacet(facet));
			}
		}
		if (m_object instanceof IModuleCodeInformationProvider) {
			String name = IAttributeTypes.MODULE_CODE;
			String displayName = ResourceMgr.getString(AttributeType.class, "AttributeType." + name);
			String value = ((IModuleCodeInformationProvider) m_object).getModuleCode();
			ValueTypeEnum type = ValueTypeEnum.TypeString;
			facets.add(new MergeFacet(name, displayName, value, type, FacetStature.EDITABLE_ATTR));
		}
		return Collections.unmodifiableCollection(facets);
	}

	protected boolean isIgnoredAttribute(@NotNull IReadOnlyFacet facet)
	{
		if (m_object instanceof IAssembledObject && ((IAssembledObject) m_object).getAssembly() != null) {
			String name = facet.getName();
			return IAttributeTypes.INCLUDE_ON_BOM.equalsIgnoreCase(name) ||
					IAttributeTypes.CONNECTOR_ASSEMBLY.equalsIgnoreCase(name);
		}
		if (m_object instanceof IModuleCodeInformationProvider) {
			String name = facet.getName();
			return IAttributeTypes.MODULE_CODE.equalsIgnoreCase(name);
		}
		return false;
	}
}
