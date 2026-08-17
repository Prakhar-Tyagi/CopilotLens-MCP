/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.properties;

import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.caplets.logic.Model;
import chs.cof.logical.shared.ISharedObject;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.PropertyGroup;
import chs.utilities.ui.property.PropertyPanel;
import chs.utility.ui.IValidityListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;

public class MiscPropertiesComponent implements IPropertiesClientComponent
{

	private final static String MISC_TAB_LABEL =
			ResourceMgr.getString(MiscPropertiesComponent.class, "MiscPropertiesComponent.Tab.Label");
	private Model m_model = null;
	//	private boolean m_active = false;
	//	private boolean m_multipleObjectsSelected;
	private PropertyGroup m_properties;

	// For keeping boolean property isOverriden
//	private IBooleanProperty m_isOverridenProperty;

//	private IStringProperty m_testProperty;
//	private IStringProperty m_testProperty2;

	public MiscPropertiesComponent(Model model)
	{
		m_model = model;
	}

	private Model getModel()
	{
		return m_model;
	}

	public String getTabName(IPropertiedSet propset)
	{
		return MISC_TAB_LABEL;
	}

	private PropertyGroup getProperties(IPropertiedSet ipropset)
	{
		if (m_properties == null) {
			m_properties = new PropertyGroup("MiscProperties");

			// Add isOverriden property if there is any name object
//			boolean addOverridenProp = false;
//			boolean isOverriden = false;
//			INamedObject namedObj = ipropset.getNamedObject();
//			if ( (namedObj != null) || (ipropset.hasMultipleNames()) )
//			{	// The propertiedSet has one name object
//				addOverridenProp = true;
			// Get the isOverriden from the propertySet
//				isOverriden = getIsOverriden(ipropset);
//			}

//			if (addOverridenProp)
//			{	// Add isOverriden property
//				m_isOverridenProperty = Property.createBooleanProperty("FixedName", "Fixed Name", isOverriden);
//				m_properties.addProperty(m_isOverridenProperty);
//			}

//			m_testProperty = Property.createStringProperty("test", "My Test", "Default Value");
//			m_properties.addProperty(m_testProperty);
//			m_testProperty2 = Property.createStringProperty("test2", "My Test", "Default Value");
//			m_properties.addProperty(m_testProperty2);
		}

		return m_properties;
	}

	/**
	 * Given a particular "PropertiedSet" object, checks to see if it can operate on the set.
	 *
	 * @param ipropset DOCUMENT ME!
	 *
	 * @return
	 */
	public boolean acceptsSet(IPropertiedSet ipropset)
	{
		return (getProperties(ipropset).getSize() != 0);
	}

	public boolean modifiesSet(IPropertiedSet propset)
	{
		return acceptsSet(propset);
	}

	/**
	 * Creates a panel for port mapping
	 *
	 * @param ipropset DOCUMENT ME!
	 *
	 * @return
	 */
	public JPanel getWidget(IPropertiedSet ipropset)
	{
		JPanel panel = new JPanel();
		panel.setName("MiscPanel");
		panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		panel.setLayout(new BorderLayout());

		PropertyPanel propPanel = new PropertyPanel(
				ResourceMgr.getString(MiscPropertiesComponent.class, "MiscPropertiesComponent.Title"),
				getProperties(ipropset));
		panel.add(propPanel, BorderLayout.CENTER);

//		if (m_isOverridenProperty != null)
		{
//			if (m_isOverridenProperty.getValue())
			{
				// If m_isOverridenProperty is set to true, i.e. the name is overriden
				// then disable the m_isOverridenProperty's component
//				JComponent component = propPanel.getPropertyComponent(m_isOverridenProperty);
//				if (component != null)
//					component.setEnabled(false);
			}
		}

		return panel;
	}

	/**
	 * Returns true - this component should have it's own tab
	 *
	 * @return
	 */
	public boolean isPropPage()
	{
		return true;
	}

	/**
	 * Make necessary data changes
	 *
	 * @param ipropset DOCUMENT ME!
	 */
	public void edit(IPropertiedSet ipropset)
	{
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent#stopEditing()
	 */
	public void stopEditing(IPropertiedSet propset)
	{
		m_properties = null;
//		m_isOverridenProperty = null;
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent#destroy()
	 */
	public void destroy()
	{
		m_model = null;
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent#isValid()
	 */
	public boolean isValid()
	{
		return false;
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent addValidityListener(chs.caf.caplet.IValidityListener)
	 */
	public void addValidityListener(IValidityListener listener)
	{
		// todo Auto-generated method stub
	}

	/**
	 * @see chs.caf.caplet.IPropertiesClientComponent removeValidityListener(chs.caf.caplet.IValidityListener)
	 */
	public void removeValidityListener(IValidityListener listener)
	{
		// todo Auto-generated method stub
	}

	@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
	{
	}

	/**
	 * Return true only if all name objects in the propertiedSet has isOverriden = true;
	 * false otherwise
	 * @param propSet
	 * @return
	 */
//	private boolean getIsOverriden(IPropertiedSet propSet)
//	{
//		boolean allIsOverriden = true;
//
//		Iterator iter = propSet.iterator();
//		while (iter.hasNext())
//		{
//			IUID uid = (IUID)iter.next();
//			IUIDObject obj = chs.system.UIDMgr.getObject(uid);
//			if (obj instanceof IRepresentedObject)
//			{
//				IUIDObject logicObj = ((IRepresentedObject)obj).getRawConnectivity();
//
//				if (logicObj instanceof IIndexedNamedObject)
//				{
//					if (!( ((IIndexedNamedObject)logicObj).isOverridden() ) )
//					{	// This object name is not overriden
//						allIsOverriden = false;
//						break;
//					}
//				}
//			}
//		}
//		return allIsOverriden;
//	}

	/**
	 * Set all the named objects in the propertied set to override their name
	 */
//	private void setIsOverriden(IPropertiedSet propSet)
//	{
//		Iterator iter = propSet.iterator();
//		while (iter.hasNext())
//		{
//			IUID uid = (IUID)iter.next();
//			IUIDObject obj = chs.system.UIDMgr.getObject(uid);
//			if (obj instanceof IRepresentedObject)
//			{
//				IUIDObject logicObj = ((IRepresentedObject)obj).getRawConnectivity();
//
//				if (logicObj instanceof IIndexedNamedObject)
//				{
//					IIndexedNamedObject indexNamedObj = (IIndexedNamedObject) logicObj;
//					if (!(indexNamedObj.isOverridden()))
//					{	// The name is not overriden yet - override it
//						String name = indexNamedObj.getName();
//						// Override the object name. i.e:
//						// First tell object that clear its name, so we can set the same name
//						// Then set the name to the old name
//						indexNamedObj.setName(""); 	// First set it to empty string
//						indexNamedObj.setName(name); 	// First set it to empty string
//					}
//				}
//			}
//		}
//	}
	public Set<ISharedObject> getSharedObjects()
	{
		return Collections.EMPTY_SET;
	}

	public Set<ISharedObject> getEditedSharedObjects()
	{
		return Collections.EMPTY_SET;
	}
}
