package chs.ctf.editui;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IEditClient;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IBlockDevicePin;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IPin;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;
import chs.common.attr.IAttributeTypes;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utility.attr.AttributeUtils;
import chs.caplets.symbol.actions.SymbolPropertiesAction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LogicEditSelectionHelper
{

	private EditActionSelectionHelper m_baseHelper = null;

	public LogicEditSelectionHelper(SelectSet selections)
	{
		m_baseHelper = new EditActionSelectionHelper(filterSelections(selections),
				new ObjectFilter());
	}

	public boolean hasSelectionType(SelectionTypeEnum selectionType)
	{
		return m_baseHelper.hasSelectionsOfType(selectionType);
	}

	public Set<Object> getSelectedObjects(SelectionTypeEnum selectionType)
	{
		return m_baseHelper.getSelectedObjects(selectionType);
	}

	private static class ObjectFilter implements IObjectFilter<Object>
	{

		public boolean accept(Object obj)
		{
			Object theObject;

			if (obj instanceof IRepresentedObject) {
				IRepresentedObject repObj = (IRepresentedObject) obj;
				theObject = repObj.getRawConnectivity();
			}
			else {
				theObject = obj;
			}
			return theObject instanceof IPinList;
		}
	}

	private static IUIDObjectIterator filterSelections(SelectSet selectSet)
	{
		IUIDObjectIterator selectedObjIter = selectSet.getUIDObjects();
		Collection<IUIDObject> actualSelections = CollectionUtils.createSet(selectedObjIter);
		Collection<Object> desiredSelections = new HashSet<Object>(actualSelections);
		for (Object selectionObj : actualSelections) {
			if (selectionObj instanceof IDiagramObject) {
				// if the parent of the selection object is already present in the select set then
				// remove the selection object from desired selections
				if (actualSelections.contains(((IDiagramObject) selectionObj).getParent())) {
					desiredSelections.remove(selectionObj);
				}
			}
		}
		return FactoryMgr.getCommonFactory().createUIDObjectIterator(desiredSelections);
	}

	public String getDoubleClickAction()
	{
		//Name
		//Shared Device Name, Shared Pin Name [which are not editable]
		//
		if (hasSelectionType(SelectionTypeEnum.TypeTextSingle)) {
			Iterator<Object> iter = getSelectedObjects(SelectionTypeEnum.TypeTextSingle).iterator();
			Object selectedObj = iter.next();
			if (selectedObj instanceof IAttributeText) {
				IAttributeText selectedAttrText = (IAttributeText) selectedObj;
				if (!(isShortDescription(selectedAttrText) || isOptionExpression(selectedAttrText))) {
					if (isOwnerValidForSmartEditableAttribute(selectedAttrText) &&
							isAttributeSmartEditable(selectedAttrText)) {
						return "chs.caplets.logic.actions.SmartEditAction";
					}
				}
			}
		}
		return "chs.caf.caplet.helpers.PropertiesAction";
	}

	public String getSymbolDoubleClickAction()
	{
		//Name
		//Shared Device Name, Shared Pin Name [which are not editable]
		//
		if (hasSelectionType(SelectionTypeEnum.TypeTextSingle)) {
			Iterator<Object> iter = getSelectedObjects(SelectionTypeEnum.TypeTextSingle).iterator();
			Object selectedObj = iter.next();
			if (selectedObj instanceof IAttributeText) {
				IAttributeText selectedAttrText = (IAttributeText) selectedObj;
				if (!(isShortDescription(selectedAttrText) || isOptionExpression(selectedAttrText))) {
					if (isAttributeSmartEditable(selectedAttrText) && !isSymbolNameText(selectedAttrText)) {
						return "chs.caplets.symbol.actions.SmartEditAction";
					}
				}
			}
		}
		return SymbolPropertiesAction.class.getCanonicalName();

	}

	@Nullable public IEditClient getEditClient(ICapletController controller)
	{
		return null;
	}

	private boolean isAttributeSmartEditable(IAttributeText selectedAttrText)
	{
		boolean editable = true;
		if (AttributeUtils.isReadOnly(selectedAttrText.getOMAttribute())) {
			editable = false;
		}
//		editable = isAttachedtoDevicewithSymbol(selectedAttrText);
		return editable;
	}

	private boolean isOwnerValidForSmartEditableAttribute(IAttributeText attText)
	{
		IDiagramObject parent = attText.getParent();
		if (parent instanceof chs.cof.logical.schem.IPinList &&
				((chs.cof.logical.schem.IPinList) parent).getConnectivity() instanceof IDeviceConnector) {
			return false;
		}
		if (parent instanceof IPin) {
			IPin schemPin = (IPin) parent;
			IAbstractPin cablePin = schemPin.getConnectivity();
			if (cablePin.isShared() || cablePin instanceof IBlockDevicePin) {
				return false;
			}
			IDiagramObject deviceParent = schemPin.getParent();
			if (deviceParent instanceof chs.cof.logical.schem.IPinList) {
				chs.cof.logical.schem.IPinList device = (chs.cof.logical.schem.IPinList) deviceParent;
				if (device.getConnectivity().getLibraryRef() != null) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isSymbolNameText(IAttributeText attText)
	{
		IDiagramObject parent = attText.getParent();
		return parent instanceof chs.cof.logical.schem.IPinList;
	}

	private boolean isShortDescription(Object obj)
	{
		if (obj instanceof IAttributeText) {
			IAttributeText atxt = (IAttributeText) obj;
			if (IAttributeTypes.SHORT_DESCRIPTION.equalsIgnoreCase(atxt.getName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isOptionExpression(Object obj)
	{
		if (obj instanceof IAttributeText) {
			IAttributeText atxt = (IAttributeText) obj;
			if (IAttributeTypes.OPTION_EXP.equalsIgnoreCase(atxt.getName())) {
				return true;
			}
		}
		return false;
	}
}
