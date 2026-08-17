/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.properties;

/*
 * Copyright 2005 Mentor Graphics Corporation.
 *            All Rights Reserved.
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.IParameterizedSchemObject;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPort;
import chs.cof.logical.schem.ISchemOtherComponentParameterized;
import chs.cof.logical.schem.ISchemSector;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IParameter;
import chs.common.IParameterContainer;
import chs.common.IParameterContainerIterator;
import chs.common.IParameterized;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.ctf.ui.form.styles.EditParameterModel;
import chs.ctf.ui.form.styles.EditParameterPanel;
import chs.ctf.ui.form.styles.IEditParameterModel;
import chs.ctf.ui.form.styles.MutableParameterTreeNode;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.IDuplicate;
import chs.utility.ui.IValidityListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EditParameterControl implements IPropertiesClientComponent
{

	private Generator m_generator;
	private boolean m_active;
	private EditParameterPanel m_editParameterPanel;
	private IEditParameterModel m_editParameterModel;

	public EditParameterControl()
	{
		m_active = false;
		m_generator = Generator.getGenerator();
	}

	/**
	 * * @return a component that may be used to edit the parameters.
	 */
	public JPanel getWidget(IPropertiedSet propset)
	{
		IUIDObject currObject = propset.getCommonRepresentingObject();
		if (currObject == null) {
			// PW - 03/27/03 - Defect #2862
			// To be consistent, we will not display the tab on multiple objects
			return null;

			//JPanel jp = new JPanel();
			//jp.add(new JLabel("Multiple Objects"));
			//return jp;
		}
		//
		// first off, we do some checks on the object,
		// to see if it is indeed valid.
		//
		if (!(currObject instanceof IPinList || currObject instanceof IShieldBody || currObject instanceof IPort ||
				currObject instanceof IParameterizedSchemObject)) {
			return null; // no it isn't
		}

		if (currObject instanceof IParameterizedSchemObject &&
				!((IParameterizedSchemObject) currObject).supportsEditability()) {
			return null; // no it isn't
		}

		//
		// Get the parameter information off the object.
		//
		IParameterized params;

		if (currObject instanceof IPinList) {
			IPinList pinlist = (IPinList) currObject;
			params = pinlist.getParameterized();
		}
		else if (currObject instanceof IShieldBody) {
			params = ((IShieldBody) currObject).getParameterized();
		}
		else if (currObject instanceof IParameterizedSchemObject) {
			params = ((IParameterizedSchemObject) currObject).getParameterized();
		}
		else {
			params = ((IPort) currObject).getParameterized();
		}

		//
		// No parameters -> no widget.
		//
		if (params == null) {
			return null;
		}

		if (m_editParameterPanel == null) {
			m_editParameterPanel = new EditParameterPanel(m_generator);
		}

		m_editParameterModel =
				new EditParameterModel(new MutableParameterTreeNode("Parameters"), currObject, m_generator);
		m_editParameterPanel.updatePanel(m_editParameterModel, modifiesSet(propset));
		return m_editParameterPanel;
	}

	/**
	 * Returns true - this component should have it's own tab
	 */
	public boolean isPropPage()
	{
		return true;
	}

	/**
	 * When the 'OK' is given after editing the parameters, this does the work of updating things.
	 *
	 * @param currObject updates the parameters of this object
	 */
	public void commitChanges(IDiagramObject currObject)
	{
		if (m_editParameterPanel == null) {
			return;
		}

		ISharedPinList spl = null;
		try {
			IParameterized par = null;
			if (currObject instanceof IPinList) {
				//
				// Remove the parameters from my pins, them myself.
				//
				IPinList pl = (IPinList) currObject;
				spl = (ISharedPinList) pl.getSharedObject();

				if (spl != null && !CTFLockUpdateHelper.lock(spl)) {
					return;
				}

				//
				// Duplicate and set before modify.
				//
				IParameterized currPar = pl.getParameterized();
				if (currPar == null) {
					currPar = FactoryMgr.getCommonFactory().createParameterized();
				}
				else {
					currPar = (IParameterized) ((IDuplicate) currPar).duplicate();
				}
				pl.setParameterized(currPar);
				//
				for (IGfxObjectIterator pitr = pl.getObjects(); pitr.hasNext(); ) {
					IGfxObject o = pitr.getNext();
					if (!(o instanceof IPin)) {
						continue;
					}
					removeParameters(((IPin) o).getParameterized());
				}
				par = ((IPinList) currObject).getParameterized();
			}
			else if (currObject instanceof IPin) {
				IPin pin = (IPin) currObject;
				IDuplicate duplpar = ((IDuplicate) m_editParameterModel.getPinParameterized(pin)).duplicate();
				pin.setParameterized((IParameterized) duplpar);
				par = pin.getParameterized();
			}
			else if (currObject instanceof IShieldBody) {
				IShieldBody sb = (IShieldBody) currObject;
				IDuplicate duplpar = ((IDuplicate) sb.getParameterized()).duplicate();
				sb.setParameterized((IParameterized) duplpar);
				par = sb.getParameterized();
			}
			else if (currObject instanceof IPort) {
				IPort port = (IPort) currObject;
				IDuplicate portParams = ((IDuplicate) port.getParameterized()).duplicate();
				port.setParameterized((IParameterized) portParams);
				par = port.getParameterized();
			}
			else if (currObject instanceof IParameterizedSchemObject) {
				IParameterizedSchemObject schemSector = (IParameterizedSchemObject) currObject;
				IParameterized parameterized = schemSector.getParameterized();
				assert parameterized != null;
				IDuplicate portParams = ((IDuplicate) parameterized).duplicate();
				schemSector.setParameterized((IParameterized) portParams);
				par = schemSector.getParameterized();
			}
			//
			// No parameter holder -> no parameters.
			//
			if (par == null) {
				return;
			}

			//
			// First, clear out all the parameters from the object. We will
			// throw them back on.
			//
			removeParameters(par);
			//
			// Update the parameters.
			//
			if (m_editParameterPanel != null) {
				// during the recursive update we check if glyphs have valid names
				List<IParameter> invalidGlyphs = new ArrayList<IParameter>();
				boolean success = m_editParameterModel.applyChanges(invalidGlyphs);
				assert success : "Parameters not saved";
				if (!invalidGlyphs.isEmpty()) {
					reportInvalidGlyphs(invalidGlyphs);
				}
			}
			//
			// Generate!!!!!
			//
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			GfxView gview = (GfxView) view;
			IBaseDiagram diagram = (IBaseDiagram) gview.getSheet();
			IGrid grid = diagram.getGrid();
			int pinspacing = grid.getGridSpacing();
			GeneratorParameters gp = new GeneratorParameters(pinspacing);
			//

			if (currObject instanceof IPinList) {
				m_generator.generate((IPinList) currObject, gp,
						Generator.NOREGENERATE_PROPERTIES, false);
			}
			else if (currObject instanceof IShieldBody) {
				IShieldBody sb = (IShieldBody) currObject;
//			IConnectivity conn = ((Model) view.getCapletModel()).getDesign().getConnectivity();
				m_generator.generateShieldBody(sb, gp);
			}
			else if (currObject instanceof IPort) {
				m_generator.generatePort((IPort) currObject, gp);
			}
			else if (currObject instanceof ISchemSector) {
				m_generator.generateSector((ISchemSector) currObject, gp);
			}
			else if (currObject instanceof ISchemOtherComponentParameterized) {
				m_generator.generateOtherComponent((ISchemOtherComponentParameterized) currObject, gp);
			}
		}
		finally {
			if (spl != null) {
				spl.unlock();
			}
		}
	}

	private void reportInvalidGlyphs(Collection<IParameter> invalidGlyphs)
	{
		assert (!invalidGlyphs.isEmpty());
		String heading = ResourceMgr.getString(this, "EditParameterControl.Info.InvalidGlyphs.Hdr");
		String message = ResourceMgr.getString(this, "EditParameterControl.Info.InvalidGlyphs.Msg");

		int i = 0;
		String[] details = new String[invalidGlyphs.size()];
		for (IParameter glyph : invalidGlyphs) {
			details[i] = new StringBuilder().append(glyph.getName()).append(' ').append(glyph.getValue()).toString();
			i++;
		}

		MessageHelper
				.showInformationMessage(m_editParameterPanel, heading, message, new JScrollPane(new JList(details)));
	}

	/**
	 * Removes all the existing parameters from the object. Cleans things up ready for thrm * to be re-added.
	 *
	 * @param par parameters of this object to be removed
	 */
	private void removeParameters(IParameterized par)
	{
		if (par != null) {
			for (IParameterContainerIterator pitr = par.getParameterContainers(); pitr.hasNext(); ) {
				IParameterContainer p = pitr.getNext();
				p.removeAllParameters();
			}
			par.removeAllParameterContainers();
		}
	}

	public String getTabName(IPropertiedSet propset)
	{
		return ResourceMgr.getString(EditParameterControl.class, "EditParameterControl.Tab.Name");
	}

	public boolean acceptsSet(IPropertiedSet propset)
	{
		m_active = false;
		if (propset.editType(IPinList.class) || propset.editType(IShieldBody.class) || propset.editType(IPort.class) ||
				propset.editType(IParameterizedSchemObject.class)) {
			m_active = true;
			return true;
		}
		return false;
	}

	public boolean modifiesSet(IPropertiedSet propset)
	{
		return acceptsSet(propset) && propset.areGraphicsEditable();
	}

	protected boolean editsMade()
	{
		if (m_editParameterModel == null) {
			return false;
		}
		else {
			return m_editParameterModel.hasChanged();
		}
	}

	public void edit(IPropertiedSet propset)
	{
		if (!editsMade()) {
			return;
		}

		if (!m_active) {
			return;
		}

		Iterator<IUID> iter = propset.iterator();
		while (iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject editObj = UIDMgr.getObject(uid);
			if (editObj instanceof IDiagramObject) {
				//
				// Go and commit the changes.
				//
				commitChanges((IDiagramObject) editObj);
			}
		}
	}

	/**
	 * @see IPropertiesClientComponent#stopEditing(IPropertiedSet)
	 */
	public void stopEditing(IPropertiedSet propset)
	{
		if (m_editParameterPanel != null) {
			m_editParameterPanel.hibernate();
		}
	}

	/**
	 * @see IPropertiesClientComponent#destroy()
	 */
	public void destroy()
	{
		m_editParameterPanel = null;
		m_generator = null;
	}

	/**
	 * @see IPropertiesClientComponent#isValid()
	 */
	public boolean isValid()
	{
		// todo Auto-generated method stub
		return false;
	}

	/**
	 * @see IPropertiesClientComponent#addValidityListener(IValidityListener)
	 */
	public void addValidityListener(IValidityListener listener)
	{
		// todo Auto-generated method stub

	}

	/**
	 * @see IPropertiesClientComponent#removeValidityListener(IValidityListener)
	 */
	public void removeValidityListener(IValidityListener listener)
	{
		// todo Auto-generated method stub

	}

	@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
	{
	}

	public Set<ISharedObject> getSharedObjects()
	{
		return Collections.emptySet();
	}

	public Set<ISharedObject> getEditedSharedObjects()
	{
		return Collections.emptySet();
	}
}



