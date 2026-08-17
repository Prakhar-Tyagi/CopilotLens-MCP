/*
 * Copyright 2002-2014 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.properties;

import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.PropertiesClientHelper;
import chs.caf.caplet.properties.CustomTextControl;
import chs.caf.caplet.properties.EditImageURL;
import chs.caf.caplet.properties.FunctionImplControl;
import chs.caf.caplet.properties.FunctionTypeControl;
import chs.caf.caplet.properties.PortTypeControl;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caplets.symbol.Model;
import chs.cof.draw.IGrid;
import chs.cof.draw.IText;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IGridDatumRepresentation;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.FunctionTypeEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IBackshellSymbol;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.schem.IInternalLinkPolyline;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.project.naming.INameValidator;
import chs.cof.project.objectinfo.properties.IPropertyTemplate;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.IPSMStamp;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolScaleTypeEnum;
import chs.cof.symbol.SymbolTypeEnum;
import chs.common.IDatum;
import chs.common.IDesignContainer;
import chs.common.IDrillPointDatum;
import chs.common.IFixturePlacementDatum;
import chs.common.IObjectFilter;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IReadOnlyNamedObject;
import chs.common.IReadOnlyShortDescriptionObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.PropertyStabilityEnum;
import chs.common.UnitTypeEnum;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IAttributeTypes;
import chs.common.attr.IReadOnlyFacet;
import chs.ctf.caf.ui.EditProperty;
import chs.ctf.caf.utils.PropertiesClientUtilsBase;
import chs.ctf.editui.IAttributesClient;
import chs.ctf.editui.PropertiesAttributesClient;
import chs.system.FactoryMgr;
import chs.system.ISystemObjectTypeInfoMgr;
import chs.system.UIDMgr;
import chs.utilities.SupportedFeatureInfo;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class PropertiesClient extends PropertiesClientHelper
{

	private Model m_model = null;
	private SupportedFeatureInfo m_filteredFeatures = null; // this is created lazily, once.
	private FunctionTypeControl mFunctionTypeControl;
	private FunctionImplControl mFunctionImplControl;
	private PortTypeControl mPortTypeControl;

	public PropertiesClient(Model model)
	{
		m_model = model;
		mFunctionTypeControl = new FunctionTypeControl(this);
		mFunctionImplControl = new FunctionImplControl(this);
		mPortTypeControl = new PortTypeControl(this);
		m_clientComponents.add(mFunctionTypeControl);
		m_clientComponents.add(mFunctionImplControl);
		m_clientComponents.add(mPortTypeControl);
		m_clientComponents.add(new CustomTextControl(this));
		m_clientComponents.add(new EditImageURL(this));
	}

	@Nullable public IDesignContainer getDesign()
	{
		return null;
	}

	public boolean doSelectionsHaveProperties(SelectSet selections)
	{
		// Overwrite the super class method
		// always return true, since if there is no selection we are editing
		// the symbol

		//Check for common except objects present in the selections
		if (PropertiesClientHelper.hasPrintRegionSelection(selections)) {
			return false;
		}

		IPropertiedObject pobj = m_model.getSymbolDef().getPropertyHolder();
		if (pobj instanceof IBackshellSymbol) {
			if (selections.isEmpty()) {
				return false;
			}
			if (selections.contains(m_model.getSymbolDef().getUID())) {
				return false;
			}
			return true;
		}
		else {
			return true;
		}
	}

	@Override public EditProperty getName()
	{
		NameCalcModifier nameModifier = new NameCalcModifier(true, true, PropertyStabilityEnum.TypeEditable)
		{
			@Override public void process(@Nullable IRepresentedObject namedObjOwner, IReadOnlyNamedObject namedObj)
			{
				if (namedObj instanceof chs.cof.logical.cable.IPinList) {
					setEditable(false);
					return;
				}
				super.process(namedObjOwner, namedObj);
			}
		};
		return calculateName(nameModifier);
	}

	@Override protected boolean doStartEditingProperties(SelectSet selections)
	{
		boolean nothingUsefulSelected = false;
		if (selections.getSelectCount() == 1) {
			IUIDObject obj = selections.getSelectedUIDObjects().getNext();
			if (obj instanceof IStamp && !(obj instanceof IBlock)) {
				nothingUsefulSelected = true;
			}
		}

		// Border names are not really a good thing to change...
		//
		if (selections.getSelectCount() == 0 || nothingUsefulSelected) {
			// When nothing is selected - select the SymbolDef from the model
			initDefaultPropertiedSet();
		}
		else {
			m_propertiedSet = new PropertiedSet(selections, isNameChangeAllowed(selections), m_model.getSymbolDef());
		}
		return true;
	}

	protected void initDefaultPropertiedSet()
	{
		IDiagramObject sdef = (IDiagramObject) m_model.getSymbolDef().getGfx();
		doInitPropertiedSet(sdef);
	}

	protected final void doInitPropertiedSet(@NotNull IDiagramObject sdef)
	{
		SelectSet selectSet = new SelectSet();
		selectSet.add(new Selection(sdef));
		m_propertiedSet = getPropertiedSet(selectSet);
	}

	protected boolean isNameChangeAllowed(@NotNull SelectSet selections)
	{
		return true;
	}

	/**
	 * @see PropertiesClientHelper#getNameValidator()
	 */
	protected INameValidator getNameValidator()
	{
		if (m_propertiedSet.getSingleNamedObject() instanceof IAbstractPin) {
			return PinNameValidator;
		}
		else if (m_propertiedSet.getSingleNamedObject() instanceof IBlock) {
			return BlockNameValidator;
		}
		else {
			return m_model.getSymbolDef().getNameMgr();
		}
	}

	/**
	 * We never allow the NameChooser to appear in CSymbol.
	 *
	 * @return false
	 *
	 * @see IPropertiesClient#getShortDescObject()
	 */
	public IReadOnlyShortDescriptionObject getShortDescObject()
	{
		return null;
	}

	protected ICapletModel getModel()
	{
		return m_model;
	}

	protected boolean shouldIgnoreEditabilityFlag()
	{
		return true; // always overrides the editability as it is a symbol edit
	}

	@Override
	@Nullable public IAttributesClient getAttributesClient()
	{
		// no attributes for comment symbols.
		IStamp symbolDef = m_model.getSymbolDef();
		if (symbolDef instanceof ISymbolDef && SymbolUtils.isCommentSymbol((ISymbolDef) symbolDef)) {
			//Enable this only for datum representation
			if (!enableForDatum(m_propertiedSet.getNamedObjects(), m_propertiedSet.getNamedObjOwners())) {
				return null;
			}
		}

		if (m_attributesClient == null) {
			Set<IAttributeProvider> attributeProviders = m_propertiedSet.getFilteredObjects(IAttributeProvider.class);

			// make sure there's some attributes to show in table
			if (!attributeProviders.isEmpty() && !m_propertiedSet.getNamedObjects().isEmpty()) {
				m_attributesClient = new SymbolAttributesClient(attributeProviders,
						m_propertiedSet.getNamedObjOwners());
			}
		}
		return m_attributesClient;
	}

	@Override
	@NotNull public Collection<IPropertyTemplate> getPredefinedProperties()
	{
		if (!isSymbolDefApplicableForOTI()) {
			return Collections.emptyList();
		}
		ISystemObjectTypeInfoMgr otpMgr = FactoryMgr.getCHSSystem().getSystemData().getObjectTypeInfoMgr();
		return getCombinedPropTemplates(otpMgr, getPropertiedSet().getNamedObjects());
	}

	private boolean isSymbolDefApplicableForOTI()
	{
		IPSMStamp symbolDef = m_model.getSymbolDef();
		if (symbolDef instanceof ISymbolDef) {
			return !(((ISymbolDef) symbolDef).getSymbolType() == SymbolTypeEnum.COMMENT);
		}
		return true;
	}

	public boolean allowColorEdit()
	{
		Iterator<IUID> iter = m_propertiedSet.gfxIterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject obj = UIDMgr.getObject(uid);
			if (obj instanceof IInternalSchemPin) {
				if (!((IInternalSchemPin) obj).areGraphicsEditable()) {
					return false;
				}
			}
			else if (obj instanceof IInternalLinkPolyline) {
				ISchemInternalLink link = ((IInternalLinkPolyline) obj).getInternalLink();
				if (link != null && !link.areGraphicsEditable()) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean allowLineStyleEdit()
	{
		Iterator<IUID> iter = m_propertiedSet.gfxIterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject obj = UIDMgr.getObject(uid);
			if (obj instanceof IInternalSchemPin) {
				if (!((IInternalSchemPin) obj).areGraphicsEditable()) {
					return false;
				}
			}
			else if (obj instanceof IInternalLinkPolyline) {
				ISchemInternalLink link = ((IInternalLinkPolyline) obj).getInternalLink();
				if (link != null && !link.areGraphicsEditable()) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean enableForDatum(Set<? extends IReadOnlyNamedObject> namedObjects,
			Set<IRepresentedObject> representedObjects)
	{
		boolean enabled = namedObjects.size() == 1 && representedObjects.size() == 1;
		if (enabled) {
			IReadOnlyNamedObject namedObject = namedObjects.iterator().next();
			IRepresentedObject representedObject = representedObjects.iterator().next();
			enabled = representedObject instanceof IDatumRepresentation &&
					!(representedObject instanceof IGridDatumRepresentation);
			if (enabled) {
				enabled = namedObject instanceof IDatum || namedObject instanceof IDrillPointDatum ||
						namedObject instanceof IFixturePlacementDatum;
			}
		}
		return enabled;
	}

	@NotNull @Override public IPropertiedSet getPropertiedSet(@NotNull SelectSet selections)
	{
		// Need to store the propertied set in case any client wants to check if the
		// graphics in the propertied set are editable.
		m_propertiedSet = new PropertiedSet(selections, isNameChangeAllowed(selections), m_model.getSymbolDef());
		return m_propertiedSet;
	}

	private class SymbolAttributesClient extends PropertiesAttributesClient
	{

		SymbolAttributesClient(IAttributeProvider obj, Set<IRepresentedObject> namedObjOwner)
		{
			super(obj, namedObjOwner);
		}

		SymbolAttributesClient(Set<IAttributeProvider> attProvider, Set<IRepresentedObject> namedObjOwner)
		{
			super(attProvider, namedObjOwner);
		}

		public boolean showValues()
		{
			// don't show the values column when editing the symbol def
			return !isSymbolDefGfx(getGfxObjects()) || getAttributeProviders().stream()
					.allMatch(anAttributeProvider -> anAttributeProvider instanceof IFunction);
		}

		public IAttributeText constructAttributeText(IRepresentedObject diagObject, IAttributeProvider owner,
				IReadOnlyFacet attribute,
				IText textAttribs, boolean visible)
		{
			IAttributeText attText = super.constructAttributeText(diagObject, owner, attribute, textAttribs, visible);
			// create placeholder text only when editing the symbol def except Function
			boolean issymbolDefGfx = isSymbolDefGfx(diagObject);
			IPSMStamp ipsmStamp = m_model.getSymbolDef();
			boolean isFunction = issymbolDefGfx && ipsmStamp instanceof ISymbolDef &&
					((ISymbolDef) ipsmStamp).getSymbolType().equals(
							SymbolTypeEnum.FUNCTION);
			attText.setPlaceholder(!isFunction && issymbolDefGfx);
			return attText;
		}

		private boolean isSymbolDefGfx(Set<IRepresentedObject> diagObject)
		{
			for (IRepresentedObject robj : diagObject) {
				if (!isSymbolDefGfx(robj)) {
					return false;
				}
			}
			return true;
		}

		private boolean isSymbolDefGfx(IRepresentedObject diagObject)
		{
			return diagObject == m_model.getSymbolDef().getGfx();
		}

		@Nullable protected Set<String> getValidAttributeNames(IAttributeProvider attributeProvider)
		{
			if (attributeProvider == null) {
				return null;
			}
			if (!(attributeProvider instanceof IInternalLink)) {
				return null; //For other symbol objects, its normal processing
			}
			Set<String> attNames = new HashSet<String>(10);
			IAttribute linkTypeAttr = attributeProvider.getAttribute("linktype");
			String linktype = linkTypeAttr == null ? null : linkTypeAttr.getAsString();
			attNames.add(IAttributeTypes.LINK_TYPE);

			if ("fusing".equalsIgnoreCase(linktype)) {
				attNames.add(IAttributeTypes.IMAX);
			}
			attNames.add(IAttributeTypes.DC_RES);

			return attNames;
		}

		@NotNull public Collection<IReadOnlyFacet> getAttributes()
		{
			Collection<IReadOnlyFacet> attrs = super.getAttributes();
			return PropertiesClientHelper.getAttributes(attrs, new IObjectFilter<IReadOnlyFacet>()
			{
				@Override public boolean accept(IReadOnlyFacet obj)
				{
					return ignoreAttribute(obj);
				}
			});
		}

		@Override public void commitChanges()
		{
			super.commitChanges();

			// Re-generate datum representations
			Set<IDatumRepresentation> datumReps = new HashSet<IDatumRepresentation>();
			for (IRepresentedObject repObj : getGfxObjects()) {
				if (repObj instanceof IDatumRepresentation) {
					datumReps.add((IDatumRepresentation) repObj);
				}
			}

			if (!datumReps.isEmpty()) {
				new VariableShapeDatumUpdater(m_model).updateRepresentations(datumReps);
			}
		}
	}

	public SupportedFeatureInfo getCapabilities()
	{
		//
		// Symbol does not have the Usage/Short Description facility.
		//
		if (m_filteredFeatures == null) {
			SupportedFeatureInfo basic = super.getCapabilities();
			m_filteredFeatures = new SupportedFeatureInfo(basic);
			m_filteredFeatures.removeFeature(SupportedFeatureInfo.Feature.SHORT_DESCRIPTION_ON_OBJECTS);
		}
		return m_filteredFeatures;
	}

	// A front for the properties name validation checker, until pin namespaces are fixed in symbol
	// TODO - fix pin namespaces so that this is unnecessary
	static final INameValidator PinNameValidator = new INameValidator()
	{
		public boolean nameExists(String name, IReadOnlyNamedObject obj)
		{
			IAbstractPin pin = (IAbstractPin) obj;
			chs.cof.logical.cable.IPinList pinList = pin.getOwner();
			if (pinList != null) {
				for (IAbstractPinIterator pinItr = pinList.getPins(); pinItr.hasNext(); ) {
					IAbstractPin p = pinItr.getNext();
					if (p != pin && p.getName().equals(name)) {
						return true;
					}
				}
			}
			return false;
		}

		public boolean nameExists(String name, String className, int objType)
		{
			return false;
		}
	};

	//to fix dts0100790976  Duplicate block names are allowed in a composite on renaming the block
	static final INameValidator BlockNameValidator = new INameValidator()
	{
		public boolean nameExists(String name, IReadOnlyNamedObject obj)
		{
			IBlock blk = (IBlock) obj;
			IPinList pl = blk.getGfx() != null ? (IPinList) blk.getGfx().getContainer() : null;
			if (pl != null && pl.getOwner() instanceof ISymbolDef) {
				ISymbolDef cmp = (ISymbolDef) pl.getOwner();
				for (IBlockIterator blkItr = cmp.getBlocks(); blkItr.hasNext(); ) {
					IBlock block = blkItr.getNext();
					if (block != blk && block.getName().equals(name)) {
						return true;
					}
				}
			}
			return false;
		}

		public boolean nameExists(String name, String className, int objType)
		{
			return false;
		}
	};

	@Override public boolean ignoreAttribute(IReadOnlyFacet f)
	{
		if (super.ignoreAttribute(f)) {
			return true;
		}

		String attrName = f.getName();
		return attrName.equals(IAttributeTypes.USER_FM_CODE) || attrName.equals(IAttributeTypes.USER_PM_CODE) ||
				attrName.equals(IAttributeTypes.GENERATED_FM_CODE) ||
				attrName.equals(IAttributeTypes.GENERATED_PM_CODE) || IAttributeTypes.FUNCTION_TYPE.equals(attrName) ||
				IAttributeTypes.FUNCTION_IMPL_TYPE.equals(attrName) || IAttributeTypes.PORT_TYPE.equals(attrName);
	}

	@Override public UnitTypeEnum getDistanceUnit()
	{
		//FEAT154946 Story 11. get unit type from the "logical" symbol thickness  OR from the "physical" symbol
		IStamp stamp = m_model.getSymbolDef();
		SymbolScaleTypeEnum symbolScaleType = ((ISymbolDef) stamp).getSymbolScaleType();
		if (symbolScaleType == SymbolScaleTypeEnum.PhysicalScale) {
			//get the current physical unit set on the symbol
			IGrid grid = m_model.getDiagram().getGrid();
			return grid.getRealMapping().getType();
		}
		return getGfxAttribute().getThickness().getUnit();
	}

	@Override
	protected void addEditProp(Collection<EditProperty> properties, Object editObj, boolean editable, IProperty prop,
			boolean isDeletable)
	{
		PropertiesClientUtilsBase.addEditProp(prop, properties, Collections.singleton(editObj), editable, false, true,
				isDeletable);
	}

	@Override protected void doUpdateUi()
	{
		mFunctionTypeControl.updateUi();
	}

	@Override protected boolean shouldUpdateUi(@NotNull chs.utilities.ui.property.IProperty propertyUI)
	{
		return propertyUI.getObject() instanceof FunctionTypeEnum;
	}
}
