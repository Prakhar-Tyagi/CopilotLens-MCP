/*
 * Copyright 2004-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.debug;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IText;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceConnectorIterator;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.shared.ISharedPinIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUIDObject;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeProvider;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.ui.HTMLHelper;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DumpSelectedAction extends ControllerActionRT implements ICtxMenuProvider
{

	private IOutputWindow m_outWindow;

	public DumpSelectedAction(ICapletController controller)
	{
		super(controller);
		m_outWindow = CAFUtils.getInstance().getOutputWindow();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	public boolean onTerminate(boolean successful)
	{
		if (successful) {
			dumpObjects();
		}

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}

		CAFUtils.getInstance().getStatusBar().clear();
		return true;
	}

	protected void printMsg(String s)
	{
		m_outWindow.sendMessage(s, IOutputWindow.DEBUG_TAB, IOutputWindow.APPEND);
	}

	private void dumpObjects()
	{
		SelectSet selections = getController().getSelectMgr().getCurrentSelections();
		SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects();
		while (iter.hasNext()) {
			IUIDObject obj = iter.getNext();
			dumpObject(obj);
		}
	}

	protected void dumpObject(IUIDObject obj)
	{
		if (obj instanceof IRepresentedObject) {
			dumpObject(((IRepresentedObject) obj).getRawConnectivity());
		}
		else if (obj instanceof IConnector) {
			IConnector conn = (IConnector) obj;
			printMsg("--------------");
			printMsg("[Connector]:" + dumpNamedObject(conn));
			printMsg(dumpIDs(conn));
		}
		else if (obj instanceof ISplice) {
			ISplice splice = (ISplice) obj;
			printMsg("--------------");
			printMsg("[Splice]:" + dumpNamedObject(splice));
			printMsg(dumpIDs(obj));
		}
		else if (obj instanceof IDevice) {
			IDevice device = (IDevice) obj;
			printMsg("--------------");
			printMsg("[Device]:" + dumpNamedObject(device));
			printMsg(dumpIDs(obj));

			for (IDeviceConnectorIterator ditr = device.getDeviceConnectors(); ditr.hasNext(); ) {
				IDeviceConnector dc = ditr.getNext();
				dumpObject(dc);
			}
		}
		else if (obj instanceof IWireConductor) {
			IWireConductor wire = (IWireConductor) obj;
			printMsg("--------------");
			printMsg("[Wire]:" + dumpNamedObject(wire));
			printMsg(dumpIDs(obj));
		}
		else if (obj instanceof INetConductor) {
			printMsg("--------------");
			printMsg("[Net]:" + dumpNamedObject((IReadOnlyNamedObject) obj));
			printMsg(dumpIDs(obj));
		}
		else if (obj instanceof IShieldConductor) {
			printMsg("--------------");
			printMsg("[Shield]:" + dumpNamedObject((IReadOnlyNamedObject) obj));
			printMsg(dumpIDs(obj));
		}
		else if (obj instanceof ISharedPinList) {
			ISharedPinList device = (ISharedPinList) obj;
			printMsg("--------------");
			printMsg("[Shared Device]:" + dumpNamedObject(device));
			printMsg(dumpIDs(obj));
			printMsg("&nbsp; [Shared Pins]: " + device.getPins().getSize());
			for (ISharedPinIterator pinit = device.getPins(); pinit.hasNext(); ) {
				printMsg("&nbsp;&nbsp;  [Shared Pin]:" + dumpNamedObject(pinit.getNext()));
			}
		}
		else {
			printMsg("------WARNING------");
			printMsg("  Can't dump object: " + obj);
			if (obj instanceof IReadOnlyNamedObject) {
				printMsg(dumpNamedObject((IReadOnlyNamedObject) obj));
			}
			printMsg(dumpIDs(obj));
		}

		if (obj instanceof IPinList) {
			printMsg("&nbsp; [Pins]: " + ((IPinList) obj).getNumPins());
			for (IAbstractPinIterator pinit = ((IPinList) obj).getPins(); pinit.hasNext(); ) {
				printMsg("&nbsp;&nbsp;  [Pin]:" + dumpNamedObject(pinit.getNext()));
			}
		}
		else if (obj instanceof IConductor) {
			printMsg("&nbsp; [Pins]: " + ((IConductor) obj).getNumPins());
			for (IAbstractPinIterator pinit = ((IConductor) obj).getPins(); pinit.hasNext(); ) {
				printMsg("&nbsp;&nbsp;  [Pin]:" + dumpNamedObject(pinit.getNext()));
			}
		}

		if (obj instanceof IGfxObject) {
			dumpGfx("&nbsp;&nbsp;", (IGfxObject) obj);
		}

		if (obj instanceof IAttributeProvider) {
			dumpAttribs("&nbsp;&nbsp;", (IAttributeProvider) obj);
		}
	}

	private void dumpAttribs(String indent, IAttributeProvider provider)
	{
		List<IAttribute> list = new ArrayList<IAttribute>(provider.getAttributes());
		Comparator<IAttribute> nameComparator = new Comparator<IAttribute>()
		{
			@Override public int compare(IAttribute o1, IAttribute o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		};
		Collections.sort(list, nameComparator);
		for (IAttribute attribute : list) {
			printMsg(indent + "[Attr] " + attribute.getName() + ": " + attribute.getAsString());
		}
	}

	private void dumpGfx(String indent, IGfxObject obj)
	{
		if (obj instanceof IText) {
			printMsg(indent + "[Text] " + obj + ": \"" + ((IText) obj).getString() + "\" : " +
					obj.getLocation() + " : " + obj.getExtent());
		}
		else {
			printMsg(indent + "[Graphics] " + obj + ": " + obj.getLocation() + " : " + obj.getExtent());
		}
		if (obj instanceof ICompoundObject) {
			ICompoundObject comp = (ICompoundObject) obj;
			if (comp.getNumObjects() > 0) {
				printMsg(indent + "&nbsp;[Child Objects] " + comp.getNumObjects());
				indent += HTMLHelper.spaces(2);
				for (IGfxObjectIterator it = comp.getObjects(); it.hasNext(); ) {
					dumpGfx(indent, it.getNext());
				}
			}
		}
	}

	private String dumpIDs(IUIDObject obj)
	{
		return "  BaseID: " + obj.getBaseId() + ", ParentID: " + obj.getParentId();
	}

	private String dumpNamedObject(IReadOnlyNamedObject obj)
	{
		return obj + " [" + obj.getName() + "] " + ((IUIDObject) obj).getUID();
	}

	public String getStatusbarText()
	{
		return "Dump Selected";
	}

	public String getActionUIClass()
	{
		return DumpSelectedActionUI.class.getName();
	}

	//
	// Context Menu methods
	//
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// Ask the client if the selections are something they edit the
		// properties of.  If so, put ourselves in the context menu.
		if (selections.getSelectCount() > 0) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
		// nothing to do here.
	}
}
