/*
 * Copyright 2002-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.symbol.Model;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxGroup;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.ISheet;
import chs.cof.draw.IText;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramText;
import chs.cof.drawplus.IGridDatumRepresentation;
import chs.cof.drawplus.IPrintRegion;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IInternalLinkPolyline;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.IAttributeDatum;
import chs.common.IBaseDatum;
import chs.common.IDatum;
import chs.common.IPropertiedObject;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utility.SymbolUtils;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.ZoneHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

public class DeleteAction extends chs.caplets.shared.actions.DeleteAction
{

	public DeleteAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return DeleteActionUI.class.getName();
	}

	protected boolean isDeletable(@NotNull SelectSet sset)
	{
		// enable this thing only if there are items selected
		if (sset.getSelectCount() == 0) {
			return false;
		}

		// do not allow delete in non-editable diagram
		ICapletModel icm = getController().getCapletModel();
		if (icm != null && !icm.isEditable()) {
			return false;
		}

		Model model = (Model) m_model;
		//
		// Get the collection of top-level pins.
		//
		IStamp sdef = model.getSymbolDef();
		IPropertiedObject pobj = sdef.getPropertyHolder();
		Set pins = new HashSet();
		if (pobj instanceof IPinList) {
			for (IAbstractPinIterator pitr = ((IPinList) pobj).getPins(); pitr.hasNext(); ) {
				pins.add(pitr.getNext());
			}
		}
		//
		// Add the block pins....
		//
		if (sdef instanceof ISymbolDef) {
			for (IBlockIterator bitr = ((ISymbolDef) sdef).getBlocks(); bitr.hasNext(); ) {
				IBlock blk = bitr.getNext();
				for (Iterator pitr = blk.getPinList().getObjects(IPin.class).iterator(); pitr.hasNext(); ) {
					pins.add(((IPin) pitr.next()).getConnectivity());
				}
			}
		}
		// see if this is the symbol itself which can't be deleted here
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof IStamp && !(obj instanceof IBlock)) {
				return false;
			}
			if (obj instanceof IPin && !pins.contains(((IPin) obj).getConnectivity())) {
				return false;
			}
			if ((obj instanceof IGfxObject) && (sset.getSelectCount() == 1)) {
				IGfxObject graphic = (IGfxObject) obj;

				if (graphic.isDerivedObject()) {
					return false;   // don't do these. they are derived...
				}
			}
			if (AttributeUtils.isNameText(obj)) {
				//
				// Do not allow deleting of Name Text on a device/pin which comes from a symbol.
				//
				IAttributeText nt = (IAttributeText) obj;
				IDiagramObject owner = nt.getParent();
				if (owner instanceof IPrintRegion && sset.getSelectCount() == 1) {
					return false;
				}
			}
			if (ZoneHelper.isObjectBorderZoneAreaObject(sdef, obj)) {
				// Don't allow ZoneArea text objects to be deleted as they are generated as part
				// of the border zone area
				return false;
			}
		}

		return true;
	}

	protected boolean editModel()
	{
		// loop through select objects and delete them
		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		ISheet sheet = m_model.getSheet();
		//
		// first, order the objects on the select list so that the correct thing
		// is done. This will be done by simply shoving the objects in to Vectors
		// by type.
		List pins = new Vector();
		Set<ISchemInternalLink> internalLinks = new HashSet<ISchemInternalLink>();
		List properties = new Vector();
		List propertiedGraphics = new Vector();
		List diagText = new Vector();
		List<IBlock> blocks = new ArrayList<IBlock>();
		HashSet<IDatumRepresentation> datumReps = new HashSet<IDatumRepresentation>();
		IStamp symbolDef = ((Model) m_model).getSymbolDef();

		for (SelectedUIDObjectIterator iter = preSelections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof IGenericSchemPin) {
				pins.add(obj);
			}
			else if (obj instanceof IInternalLinkPolyline) {
				internalLinks.add((ISchemInternalLink) ((IDiagramObject) obj).getParent());
			}
			else if (obj instanceof ISchemInternalLink) {
				internalLinks.add((ISchemInternalLink) obj);
			}
			else if (obj instanceof IPropText) {
				properties.add(obj);
			}
			else if (obj instanceof IGridDatumRepresentation) {
				IDatumRepresentation datumRep = (IDatumRepresentation) obj;
				datumReps.add(datumRep);
			}
			else if (obj instanceof IPropertiedGraphic) {
				propertiedGraphics.add(obj);
			}
			else if (obj instanceof IAttributeText) {
				if (AttributeUtils.isNameText(obj)) {
					IAttributeText nt = (IAttributeText) obj;
					IDiagramObject owner = nt.getParent();
					boolean isLink = owner instanceof IInternalLinkPolyline ? true : false;
					if (isLink && !preSelections.contains(owner.getUID())) {
						diagText.add(obj);
					}

					boolean allowDelete = owner instanceof IPrintRegion ? false : true;
					if (!isLink && allowDelete) {
						diagText.add(obj);
					}
				}
				else {
					IAttributeText txt = (IAttributeText) obj;
					IDiagramObject owner = txt.getParent();
					boolean isLink = owner instanceof IInternalLinkPolyline ? true : false;
					if (!isLink || (isLink && !preSelections.contains(owner.getUID()))) {
						diagText.add(obj);
					}
				}
			}
			else if (obj instanceof IDiagramText) {
				diagText.add(obj);
			}
			else if (obj instanceof IBlock) {
				blocks.add((IBlock) obj);
			}
			else if (obj instanceof IDatumRepresentation) {

				IDatumRepresentation datumRep = (IDatumRepresentation) obj;
				datumReps.add(datumRep);
				// Method returns a list of child datums.
				if (datumRep.getDatum() != null) {
					List<IBaseDatum> childDatums = SymbolUtils.getAllRelatedDatums(symbolDef, datumRep.getDatum());
					// Add the represenations for all the child datums
					datumReps.addAll(SymbolUtils.getDatumRepresentation(symbolDef, childDatums));
				}
			}
			else {
				throw new RuntimeException("unknown type " + obj.getClass().getName());
			}
		}

		// now process the items in the alternate storage properly.

		// first check properties, since they should be easy to manage
		// and it is no problem if they're deleted before their owner
		for (int i = 0; i < properties.size(); i++) {
			IPropText prop = (IPropText) properties.get(i);
			IDiagramObject sobj = prop.getParent();
			prop.delete();
			((ICompoundObject) sobj).removeObject(prop);
		}
		properties.clear();

		for (int i = 0; i < pins.size(); i++) {
			IGenericSchemPin pin = (IGenericSchemPin) pins.get(i);
			IGenericPin connPin = pin.getConnectivity();
			DeleteHelper.getInstance().removeDependentText(diagText, pin);
			pin.delete();

			// you must delete the pin before you remove it from the pin list
			connPin.delete();

			// remove the pin from the symbol def pin list...
			if (symbolDef instanceof ISymbolDef) {
				((ISymbolDef) symbolDef).removePin(connPin);
				Set<IDevice> connectivities = new HashSet<IDevice>();
				if (((ISymbolDef) symbolDef).getConnectivity() instanceof IDevice) {
					connectivities.add((IDevice) ((ISymbolDef) symbolDef).getConnectivity());
					for (IBlock block : ((ISymbolDef) symbolDef).getBlocks()) {
						if (block.getConnectivity() instanceof IDevice) {
							connectivities.add((IDevice) block.getConnectivity());
						}
					}
					for (IDevice connectivity : connectivities) {
						for (chs.cof.logical.cable.IInternalLink internalLink : connectivity
								.getInternalLinkCollection()) {
							internalLink.removePin(connPin);
						}
					}
				}
			}
		}
		pins.clear();
		for (ISchemInternalLink internalLink : internalLinks) {
			chs.cof.logical.cable.IInternalLink connInternalLink = internalLink.getConnectivity();
			DeleteHelper.getInstance().removeDependentText(diagText, internalLink);
			internalLink.delete();
			connInternalLink.delete();
			if (symbolDef instanceof ISymbolDef) {
				((IDevice) ((ISymbolDef) symbolDef).getConnectivity()).removeInternalLink(connInternalLink);
			}
		}
		internalLinks.clear();
		for (IDatumRepresentation datumRep : datumReps) {

			IBaseDatum datum = datumRep.getDatum();
			DeleteHelper.getInstance().removeDependentText(diagText, datumRep);
			symbolDef.getGfx().removeObject(datumRep);
			datumRep.delete();

			symbolDef.removeDatum(datum);

			if (datum instanceof IAttributeDatum) {
				IAttributeDatum attributeDatum = (IAttributeDatum) datum;
				IDatum redDatum = symbolDef.getAssociatedDatum(attributeDatum);
				if (redDatum != null) {
					symbolDef.removeAttributeDatumAssociation(redDatum, attributeDatum);
				}
			}
			datum.delete();
		}

		for (int i = 0; i < diagText.size(); i++) {
			IDiagramText prop = (chs.cof.drawplus.IDiagramText) diagText.get(i);
			IDiagramObject sobj = prop.getParent();

			boolean removeNameText = shouldRemoveNameText(sobj);
			IGfxObjectIterator iter = ((ICompoundObject) sobj).getObjects();
			while (iter.hasNext()) {
				IGfxObject gobj = iter.getNext();
				if (gobj == prop) {
					if (AttributeUtils.isNameText(gobj) && !removeNameText) {
						gobj.setMarkedVisible(false);
					}
					else if (gobj instanceof IText) {
						((ICompoundObject) sobj).removeObject(gobj);
					}
				}
			}
			if (AttributeUtils.isNameText(prop) == false || removeNameText) {
				//prop.setVisibility(false);
				preSelections.remove(prop.getUID());
				// delete te name text
				prop.delete();
			}
		}
		diagText.clear();

		// take care of propertied graphics
		Stack pg = new Stack();
		pg.addAll(propertiedGraphics);
		while (!pg.isEmpty()) {
			chs.cof.drawplus.IPropertiedGraphic graphic = (chs.cof.drawplus.IPropertiedGraphic) pg.pop();
			if (graphic.isDerivedObject()) {
				continue;   // don't do these. they are derived...
			}
			if (graphic instanceof IGfxGroup) {
				IGfxGroup group = (IGfxGroup) graphic;
				// Remove objects from group and target them for removal too - if we don't do this, the group
				// goes away, but not the members.
				Collection refs = group.getGfxObjects();
				if (refs != null) {
					pg.addAll(refs);
				}
				group.removeAllGfxObjects();
			}
			graphic.delete();
		}
		propertiedGraphics.clear();

		// take care of blocks.  Be careful with hierarchical blocks here
		// Note Block.doDelete() will now remove the block from it's correct owner.
		for (IBlock blk : blocks) {
			blk.delete();
		}
		blocks.clear();
		preSelections.clear();
		return true;
	}

	private boolean shouldRemoveNameText(IDiagramObject textOwner)
	{
		//dts0100841670:we will delete permanently the pin nametexts also.
		//this will cause a change in behavior of name text placement for pins.
		//return !(textOwnern instanceof IGenericSchemPin);
		return true;
	}
}
