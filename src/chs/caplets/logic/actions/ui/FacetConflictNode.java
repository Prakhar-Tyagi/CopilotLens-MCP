package chs.caplets.logic.actions.ui;

import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IModuleCodeInformationProvider;
import chs.cof.logical.cable.IMulticore;
import chs.cof.project.IProject;
import chs.common.INamedPropertiedObject;
import chs.common.IProperty;
import chs.common.ValueTypeEnum;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeTypes;
import chs.system.FactoryMgr;
import chs.utilities.CaseLessStringKey;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.harness.MulticoreUtils;
import chs.utility.stream.IPipelineConsumer;
import chs.utility.stream.IPipelineStreamExecutable;
import chs.utility.stream.PipelineStream;
import chs.utility.stream.PipelineStreamInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author chandras on 08-03-2018.
 */
public class FacetConflictNode implements IFacetConflictNode
{

	private static final Set<String> m_ignoredAttributes = new HashSet<>(2);

	static {
		m_ignoredAttributes.add(IAttributeTypes.NAME);
		m_ignoredAttributes.add(IAttributeTypes.SHORT_DESCRIPTION);
	}

	@NotNull private final IFacetDataProvider m_source;
	@NotNull private final IFacetDataProvider m_target;
	@NotNull private final HashMap<IConflictInfoKey, IFacetConflictInfo> m_conflicts = new HashMap<>();

	public FacetConflictNode(@NotNull IFacetDataProvider source, @NotNull IFacetDataProvider target)
	{
		m_source = source;
		m_target = target;
	}

	public void computeConflicts(boolean overrideAllowed)
	{
		@NotNull IFacetDataProvider source = m_source;
		@NotNull IFacetDataProvider target = m_target;
		Map<CaseLessStringKey, IMergeFacet> sourceFacets = new HashMap<>();

		IPipelineStreamExecutable<IMergeFacet, Class<Void>> facetCollector = PipelineStream.<IMergeFacet>stream()
				.terminate((a) -> sourceFacets.put(CaseLessStringKey.toKey(a.getName()), a));

		IPipelineConsumer<IMergeFacet> conflict = (targetFacet) -> {
			String name = targetFacet.getName();
			CaseLessStringKey key = CaseLessStringKey.toKey(name);
			IMergeFacet srcFacet = sourceFacets.get(key);
			if (areConflicting(targetFacet, srcFacet)) {
				FacetConflictInfo facetConflictInfo = new FacetConflictInfo(srcFacet, targetFacet);
				IConflictInfoKey conflictInfoKey = facetConflictInfo.isAttribute() ? new ConflictInfoAttrKey(name) :
						new ConflictInfoPropKey(name);
				m_conflicts.put(conflictInfoKey, facetConflictInfo);
				if (overrideAllowed && StringUtils.isBlank(targetFacet.getValue()) &&
						!StringUtils.isBlank(srcFacet.getValue())) {
					facetConflictInfo.setUserChoice(ValueOption.Source);
				}
			}
		};
		IPipelineStreamExecutable<IMergeFacet, Class<Void>> conflictCollector =
				PipelineStream.<IMergeFacet>stream().terminate(conflict);

		facetCollector.execute(PipelineStreamInput.of(source.getAttributes()));
		conflictCollector.execute(PipelineStreamInput.of(target.getAttributes()));

		sourceFacets.clear();
		facetCollector.execute(PipelineStreamInput.of(source.getProperties()));
		conflictCollector.execute(PipelineStreamInput.of(target.getProperties()));
	}

	protected final boolean areConflicting(@NotNull IMergeFacet targetFacet, @Nullable IMergeFacet sourceFacet)
	{
		//both the facets should be editable.
		if (isBothFacetsNotEditable(targetFacet, sourceFacet)) {
			return false;
		}

		if (sourceFacet.isAttribute() != targetFacet.isAttribute()) {
			return false;
		}

		//check if there are conflicting type.
		ValueTypeEnum tgtType = targetFacet.getType();
		ValueTypeEnum srcType = sourceFacet.getType();
		if (!tgtType.equals(srcType)) {
			//for attributes the types must be same.
			if (sourceFacet.isAttribute() || targetFacet.isAttribute()) {
				return false;
			}
			//for properties allow conflict for non-boolean types. do we have boolean type of property?
			//anyway do some more robust check.
			if (ValueTypeEnum.TypeBoolean.equals(tgtType) || ValueTypeEnum.TypeBoolean.equals(srcType)) {
				return false;
			}
			//ideally we can return true from here. because if both values are blank,
			//we eventually return false. which is theoretically incorrect but this
			//will never arise if the types are different. leave as it is. this was
			//observed during unit test. testFacetConflictNodeGeneral.testAreConflicting
			//return true;
		}

		//check if there are conflicting value.
		if (targetFacet.isEqual(sourceFacet)) {
			return false;
		}

		if (sourceFacet.isAttribute() && m_ignoredAttributes.contains(sourceFacet.getName())) {
			return false;
		}

		return !StringUtils.isBlank(targetFacet.getValue()) || !StringUtils.isBlank(sourceFacet.getValue());
	}

	/**
	 * This method will check if both the facets are editable or not.
	 * In case of "In-House" attribute, we need to check if "NonInHouseTwistedNumOSSpec" preference is selected or not.
	 * If its selected, treat In-House as editable.
	 */
	private boolean isBothFacetsNotEditable(@NotNull IMergeFacet targetFacet, @Nullable IMergeFacet sourceFacet)
	{
		if (sourceFacet == null) {
			return true;
		}
		if (!sourceFacet.isEditable() || !targetFacet.isEditable()) {
			IProject project = getProject();
			if (MulticoreUtils.isNumericOSSpecAllowed(project)) {
				return !sourceFacet.getName().equals(IAttributeTypes.IN_HOUSE) &&
						!targetFacet.getName().equals(IAttributeTypes.IN_HOUSE);
			}
			return true;
		}
		return false;
	}

	@Nullable private IProject getProject()
	{
		INamedPropertiedObject namedPropertiedObject = m_target.getObject();
		ILogicObject cast = CommonUtils.cast(namedPropertiedObject, ILogicObject.class);
		if (cast != null) {
			return cast.getProject();
		}
		return null;
	}

	public boolean hasConflicts()
	{
		return !m_conflicts.isEmpty();
	}

	@NotNull public Collection<IFacetConflictInfo> getConflicts()
	{
		return Collections.unmodifiableCollection(m_conflicts.values());
	}

	@Nullable @Override public IFacetConflictInfo getAttributeInfo(@NotNull String attr)
	{
		return m_conflicts.get(new ConflictInfoAttrKey(attr));
	}

	@NotNull @Override public Set<IFacetConflictInfo> getRelatedFacets(@NotNull IFacetConflictInfo info)
	{
		Set<IFacetConflictInfo> relatedAttrs = new HashSet<>(4);
		for (String attr : getRelatedFacets(COFTypeEnum.from_object(getNodeObject()), info.getName())) {
			IFacetConflictInfo attributeInfo = getAttributeInfo(attr);
			if (attributeInfo != null) {
				relatedAttrs.add(attributeInfo);
			}
		}
		return Collections.unmodifiableSet(relatedAttrs);
	}

	@NotNull public INamedPropertiedObject getNodeObject()
	{
		return m_target.getObject();
	}

	@NotNull public String getSourceName()
	{
		return m_source.getName();
	}

	@NotNull public String getTargetName()
	{
		return m_target.getName();
	}

	@Override public void apply()
	{
		INamedPropertiedObject targetObject = m_target.getObject();
		for (IFacetConflictInfo info : m_conflicts.values()) {
			String name = info.getName();
			String result = info.getResult();
			ValueTypeEnum resultType = info.getResultType();
			if (info.isAttribute()) {
				if (targetObject instanceof IModuleCodeInformationProvider &&
						IAttributeTypes.MODULE_CODE.equalsIgnoreCase(name)) {
					((IModuleCodeInformationProvider) targetObject).setModuleCode(result);
				}
				else {
					IAttribute attribute = targetObject.getAttribute(name);
					if (attribute != null) {
						if (attribute.getName().equalsIgnoreCase(IAttributeTypes.IN_HOUSE)) {
							updateInHouseAttribute(targetObject, result);
						}
						else {
							attribute.setFromUnformattedString(resultType, result);
						}
					}
				}
			}
			else {
				IProperty newProp =
						FactoryMgr.getCommonFactory().constructProperty(name, resultType, info.getRawResult(), targetObject);
				IProperty oldProp = targetObject.findPropertyByName(name);
				if (oldProp != null) {
					targetObject.removeProperty(oldProp);
				}
				targetObject.addProperty(newProp);
			}
		}
	}

	private void updateInHouseAttribute(INamedPropertiedObject targetObject, String result)
	{
		IMulticore multicore = CommonUtils.cast(targetObject, IMulticore.class);
		if (multicore != null) {
			multicore.setNonInHouseTwistedNumOSSpec(!Boolean.parseBoolean(result));
		}
	}

	@NotNull protected final Set<String> getRelatedFacets(@NotNull COFTypeEnum cofTypeEnum, @NotNull String name)
	{
		RelatedAttributesStore attributesStore = m_relatedAttributes.get(cofTypeEnum);
		if (attributesStore == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(attributesStore.getRelatedAttributes(name));
	}

	@SuppressWarnings("MapReplaceableByEnumMap")
	private static final Map<COFTypeEnum, RelatedAttributesStore> m_relatedAttributes = new HashMap<>(2);

	static {
		RelatedAttributesStore store = new RelatedAttributesStore();
		store.add(IAttributeTypes.WIRE_MATERIAL, IAttributeTypes.WIRE_SPEC, IAttributeTypes.WIRE_CSA);
		store.add(IAttributeTypes.WIRE_SPEC, IAttributeTypes.WIRE_MATERIAL, IAttributeTypes.WIRE_CSA);
		store.add(IAttributeTypes.WIRE_CSA, IAttributeTypes.WIRE_MATERIAL, IAttributeTypes.WIRE_SPEC);
		m_relatedAttributes.put(COFTypeEnum.Net, store);
		m_relatedAttributes.put(COFTypeEnum.Wire, store);
		m_relatedAttributes.put(COFTypeEnum.Shield, store);

		store.add(IAttributeTypes.OS_SPEC, IAttributeTypes.IN_HOUSE);
		store.add(IAttributeTypes.IN_HOUSE, IAttributeTypes.OS_SPEC);
		m_relatedAttributes.put(COFTypeEnum.Multicore, store);
	}

	private static class RelatedAttributesStore
	{

		private SetMap<String, String> m_store = SetMap.createShallowSetMap();

		public void add(@NotNull String attr, @NotNull String relatedAttr, @NotNull String... otherRelated)
		{
			m_store.add(attr, relatedAttr);
			for (String other : otherRelated) {
				m_store.add(attr, other);
			}
		}

		@NotNull public Set<String> getRelatedAttributes(@NotNull String attr)
		{
			return m_store.pullReadOnlySafeSet(attr);
		}
	}

	private interface IConflictInfoKey
	{

		@NotNull String getName();

		boolean isAttribute();
	}

	private abstract static class AbstractConflictInfoKey implements IConflictInfoKey
	{

		@NotNull private final String m_name;

		protected AbstractConflictInfoKey(@NotNull String name)
		{
			m_name = name;
		}

		@Override @NotNull public String getName()
		{
			return m_name;
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override public boolean equals(Object obj)
		{
			IConflictInfoKey other = (IConflictInfoKey) obj;
			return Arrays.equals(convertToArray(this), convertToArray(other));
		}

		@NotNull private Object[] convertToArray(IConflictInfoKey other)
		{
			return new Object[]{StringUtils.toLowerCase(other.getName()), other.isAttribute()};
		}

		@Override public int hashCode()
		{
			return Arrays.hashCode(convertToArray(this));
		}
	}

	private static class ConflictInfoAttrKey extends AbstractConflictInfoKey
	{

		private ConflictInfoAttrKey(@NotNull String name)
		{
			super(name);
		}

		@Override public boolean isAttribute()
		{
			return true;
		}
	}

	private static class ConflictInfoPropKey extends AbstractConflictInfoKey
	{

		private ConflictInfoPropKey(@NotNull String name)
		{
			super(name);
		}

		@Override public boolean isAttribute()
		{
			return false;
		}
	}
}
