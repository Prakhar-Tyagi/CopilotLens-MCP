/*
 * Copyright 2003-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.properties;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.helpers.GfxEditFormBuilder;
import chs.caf.caplet.helpers.IGfxEditFormProvider;
import chs.caf.caplet.helpers.LibraryControl;
import chs.caf.caplet.helpers.PropertiedSetHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.LogicGfxEditFormProvider;
import chs.caplets.symbol.Model;
import chs.cof.COFTypeEnum;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IText;
import chs.cof.draw.RecentreOnFlip;
import chs.cof.draw.TextBoxing;
import chs.cof.draw.TextWrap;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IBorderHolder;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IFunctionPin;
import chs.cof.logical.schem.IInternalLinkPolyline;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.parts.ILibraryObject;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.IUserDefinedZone;
import chs.common.IAttributeDatum;
import chs.common.IBaseDatum;
import chs.common.IDatum;
import chs.common.IDrillPointDatum;
import chs.common.IEngineeringDatum;
import chs.common.IFixturePlacementDatum;
import chs.common.IFormboardRegionDatum;
import chs.common.IGenericDatum;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.reln.IRelatedEntityType;
import chs.common.reln.Relation;
import chs.ctf.caf.ui.EditProperty;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utility.SymbolUtils;
import chs.utility.helpers.PropertyRemover;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.ISymbolModel;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * This is the PropertiedSet for Symbol caplet User: pwijaya Date: Apr 25, 2003 Time: 11:24:26 AM To change this
 * template use Options | File Templates.
 */
public class PropertiedSet extends PropertiedSetHelper
{

	static {

		GfxEditFormBuilder.getInstance().registerProvider(
				IGfxEditFormProvider.class,
				GfxEditFormBuilder.GfxEditFormBuilderContext.createInstance(PropertiedSet.class),
				new LogicGfxEditFormProvider());
	}

	private String m_setString = null;
	private boolean m_allowNameChange = true;
	private final boolean isComment;

	public PropertiedSet(SelectSet selections, boolean allowNameChange, @Nullable IStamp symbolDef)
	{
		m_allowNameChange = allowNameChange;
		SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects();

		while (iter.hasNext()) {
			// In Symbol - all selectable objects are shemObjects
			IUIDObject obj = iter.getNext();
			if (obj instanceof IDiagramObject
					// If object is IDiagramText, it should not be added to editset if its parent exists in selection(dt725697)
					&& !shouldExcludeFromSelection(selections, obj)) {
				IDiagramObject sobj = (IDiagramObject) obj;
				sortObject(sobj);
				determineSetString(sobj);
			}
		}
		isComment = isComment(symbolDef);
	}

	private boolean isComment(@Nullable IStamp symDef)
	{
		return symDef instanceof ISymbolDef && SymbolUtils.isCommentSymbol((ISymbolDef) symDef);
	}

	/**
	 * This is for Logic only
	 */
	@Override public void associateLibraryPart(@Nullable ILibraryObject libObj, @Nullable LibraryControl libraryControl)
	{
	}

	/**
	 * Return true if there is a shared object in the edit set; false otherwise
	 *
	 * @return true if has shared object
	 */
	public boolean hasSharedPinList()
	{
		return false; // Symbol does not have shared objects
	}

	public boolean hasSharedPin()
	{
		return false; // Symbol does not have shared objects
	}

	public boolean hasSharedConductor()
	{
		return false; // Symbol does not have shared objects
	}

	/**
	 * Returns the string representing the propertiedSet.
	 */
	public String getSetString()
	{
		return m_setString;
	}

	/**
	 * Returns an object that represents all the schematic objects. Returns null if nothing is selected.
	 */
	@Nullable public IUIDObject getCommonRepresentingObject()
	{
		IDiagramObject sobj = null;
		for (IUID uid : m_gfxEditSet) {
			Object obj = UIDMgr.getObject(uid);
			if (obj instanceof IDiagramObject) {
				if (sobj != null) {
					return null;
				}
				sobj = (IDiagramObject) obj;
			}
		}
		return sobj;
	}

	/**
	 * Logic only
	 *
	 * @return always null
	 */
	@Nullable public IRepresentedObject getConductorRep()
	{
		return null;
	}

	public void editProperty(EditProperty ep)
	{
		for (IUID uid : m_editSet) {
			IUIDObject editObj = UIDMgr.getObject(uid);
			IPropertiedObject propObj = getPropertiedObject(editObj);

			if (propObj == null) {
				continue;
			}

			IProperty prop = propObj.findPropertyByName(ep.getName());
			if (prop == null) {
				continue;
			}

			if (ep.isSingleValue()) {
				prop = ep.replaceProperty(prop, propObj);
			}
			//Copy/pasted the second condition from Logic's PropertiedSet..
			if (editObj instanceof IDiagramObject && ep.wasGraphicsEdited()) {
				// Apply any graphical attributes to the properties on the current sheet
				boolean found =
						updatePropertyGraphics((IDiagramObject) editObj, prop, ep.getGfxText(), ep.getVisible());
				if (!found) {
					// Means that the property doesn't have a graphical representation so we should creation one.
					addPropertyGraphics(prop, editObj, ep.getGfxText(), ep.getVisible());
				}
			}
		}
	}

	public void addProperty(EditProperty ep)
	{
		for (IUID uid : m_editSet) {
			IUIDObject editObj = UIDMgr.getObject(uid);
			IPropertiedObject propObj = getPropertiedObject(editObj);
			if (propObj != null) {
				// PW - 03/31/03 - defect #3817
				// When adding a property which is already in the PropertiedObject
				// treat it the same as edit (same for multiple objects)
				String propName = ep.getName();
				if (propObj.findPropertyByName(propName) != null) {
					// This propertyObject already has the property - edit the property
					editProperty(ep);
					continue;
				}

				IProperty prop = ep.constructProperty(propObj);
				propObj.addProperty(prop);

				if (editObj instanceof IDiagramObject) {
					//
					// If it is a graphical object (as opposed to a logical-only one) then add graphical
					// properties.
					boolean newVis = (ep.getVisible() == Boolean.TRUE);
					addPropertyGraphics(prop, editObj, ep.getGfxText(), newVis);
				}
			}
		}
	}

	@Override
	protected IPropText constructPropText(IProperty prop, IPropertiedObject propertiedObject, ICompoundObject owner,
			int x, int y, int height)
	{
		if (isComment) {
			return FactoryMgr.getDrawPlusFactory().constructPropText(FactoryMgr.createUID(),
					FactoryMgr.getDrawPlusFactory().constructPlaceholderProviderStrategy(),
					prop.getName(), height, 0, TextBoxing.OVERFLOW, TextWrap.NO_WRAP, RecentreOnFlip.NO,
					x, y, null);
		}
		return super.constructPropText(prop, propertiedObject, owner, x, y, height);
	}

	public void delProperty(EditProperty ep)
	{
		for (IUID uid : m_editSet) {
			IUIDObject editObj = UIDMgr.getObject(uid);
			IPropertiedObject propObj = getPropertiedObject(editObj);
			if (propObj != null) {
				IProperty prop = propObj.findPropertyByName(ep.getName());
				if (prop != null) {
					// delete the property graphics first
					PropertyRemover.delPropertyGraphics(prop, editObj);
					propObj.removeProperty(prop);
				}
			}
		}
	}

	/**
	 * Get the Proprtied Object corresponding to the given editObj. When editing in Symbol the graphics (althogh they
	 * are properied graphics) cannot have Properties added to them.
	 *
	 * @param editObj The input object
	 *
	 * @return IPropertiedObject the propertied object corresponding to the given editObj or null
	 */
	@Nullable private static IPropertiedObject getPropertiedObject(IUIDObject editObj)
	{
		IPropertiedObject propObj = null;
		// PropertiedGraphcis in CSymbol do not have properties persisted so do not include them here
		if (editObj instanceof IPropertiedObject && !(isCSymbol())) {
			propObj = (IPropertiedObject) editObj;
		}
		else if (editObj instanceof IRepresentedObject) {
			IUIDObject logicObj =
					((IRepresentedObject) editObj).getRawConnectivity();

			if (logicObj instanceof IPropertiedObject) {
				propObj = (IPropertiedObject) logicObj;
			}
		}
		return propObj;
	}

	/**
	 * Check if currently in Symbol or not
	 *
	 * @return true if editing in symbol
	 */
	private static boolean isCSymbol()
	{
		return FactoryMgr.getCAFUtils().isSymbolCaplet();
	}

	public boolean acceptsFootprint()
	{
		return false;
	}

	/*
	 * Given a schematic object, this determines which editset to put the object in (and perhaps what object to use).  We
	 * are always storing the schem object.  The corresponding cable object can easily be obtained from the schem object.
	 */
	private void sortObject(IDiagramObject sobj)
	{
		if (sobj instanceof IInternalLinkPolyline) {
			m_gfxEditSet.add(sobj.getUID());
		}
		else if (sobj instanceof ISchemInternalLink) {
			addObject(sobj);
		}
		else if (sobj != null) {
			if (!(sobj instanceof IText)) {
				m_gfxEditSet.add(sobj.getUID());
			}
			addObject(sobj);
		}
	}

	/*
	 * Get a string representing the object(s) that are in this set.
	 */
	private void determineSetString(IUIDObject sobj)
	{
		if ("Objects".equals(m_setString)) {
			return;
		}
		String objStr = "Unknown";

		if (sobj instanceof IPinList) {
			// just claim we're editing the symbol.  pay no attention to the man behind the curtain.
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.symbol.name");
			setSetClass(IDevice.class);
		}
		else if (sobj instanceof IBorderHolder) {
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.border.name");
			setSetClass(IGfxObject.class);        // scared to change currently.
		}
		else if (sobj instanceof IUserDefinedZone) {
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.userzone.name");
			setSetClass(IUserDefinedZone.class);
		}
		else if (sobj instanceof IInternalSchemPin) {
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.internalPin.name");
			setSetClass(IInternalSchemPin.class);
		}
		else if (sobj instanceof IPin) {
			IAbstractPin cablePin = ((IPin) sobj).getConnectivity();
			if (cablePin instanceof IFunctionPin) {
				objStr = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.port.name");
			}
			else {
				objStr = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.pin.name");
			}
			setSetClass(IPin.class);
		}
		else if (sobj instanceof IPropText) {
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.propertyText.name");
			setSetClass(IPropText.class);
		}
		else if (sobj instanceof IDatumRepresentation) {
			ICapletModel model = CAFUtils.getInstance().getActiveCapletController().getCapletModel();
			assert model instanceof Model;
			IBaseDatum datum = ((IDatumRepresentation) sobj).getDatum();
			if (datum instanceof IDatum) {
				IRelatedEntityType datumContext = ((ISymbolModel) model).getSymbolDef()
						.getRelatedEntityType((IDatum) datum);
				String key = "PropertiedSet.edit.title.datum.";
				COFTypeEnum inputEntityType = datumContext.getInputEntityType();
				Relation relation = datumContext.getRelation();
				if (inputEntityType == COFTypeEnum.Unknown || relation == Relation.Unknown) {
					key += IRelatedEntityType.Unknown.toString();
				}
				else {
					key += inputEntityType.name() + '.' + relation.name();
				}
				objStr = ResourceMgr.getString(getClass(), key);
			}
			else if (datum instanceof IEngineeringDatum) {
				objStr = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.engineeringDatum.name");
			}
			else if (datum instanceof IAttributeDatum) {
				objStr = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.attributeDatum.name");
			}
			else if (datum instanceof IDrillPointDatum) {
				objStr = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.drillPointDatum.name");
			}
			else if (datum instanceof IFixturePlacementDatum) {
				objStr = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.fixturePlacementDatum.name");
			}
			else if (datum instanceof IFormboardRegionDatum) {
				objStr = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.formboardRegion.name");
			}
			else if (datum instanceof IGenericDatum) {
				objStr = ResourceMgr.getString(
						PropertiedSet.class, "PropertiedSet.genericDatum.name");
			}
			setSetClass(IDatumRepresentation.class);
		}
		else if (sobj instanceof IPropertiedGraphic) {
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.propertiedGraphic.name");
			setSetClass(IPropertiedGraphic.class);
		}
		else if (sobj instanceof ISchemInternalLink) {
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.link.name");
			setSetClass(ISchemInternalLink.class);
		}
		else if (sobj instanceof IGfxObject) {
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.graphicsObject.name");
			setSetClass(IGfxObject.class);
		}
		if ((m_setString != null) && (!m_setString.equals(objStr))) {
			objStr = ResourceMgr.getString(
					PropertiedSet.class, "PropertiedSet.objects.name");
		}

		m_setString = objStr;
	}

	/*
	 * Given a property and attribute, this updates all the corresponding graphics on the current diagram to reflect the
	 * new property and attribute data.
	 */
	private boolean updatePropertyGraphics(IDiagramObject editObj, IProperty prop, IText attObj, Boolean visible)
	{
		IBaseDiagram diagram = getParentDiagram();
		IUIDObject connectivityObj = getConnectivity(editObj);
		if (connectivityObj == null) {
			return false;
		}
		// if editing properties on the pinlist, also search for text on the blocks
		boolean traverseBlocks = editObj instanceof IPinList;
		boolean found = false;
		IDiagramObjectIterator iter = diagram.getRepresentations(connectivityObj.getUID());
		while (iter.hasNext()) {
			IDiagramObject propOwner = iter.getNext();
			// Check if the property owner are the same
			if (propOwner == editObj) {
				Collection<IPropText> texts = TextHelper.getPropTexts(propOwner, prop.getName(), traverseBlocks);
				for (IPropText ptext : texts) {
					found = true;
					TextHelper.applyTextChanges(attObj, ptext, visible);
					ptext.setString(prop.getAsString());
				}
			}
		}
		return found;
	}

	/**
	 * Expose the flag that is set on us to the world.
	 */
	public boolean allowNameField()
	{
		return m_allowNameChange;
	}
}
