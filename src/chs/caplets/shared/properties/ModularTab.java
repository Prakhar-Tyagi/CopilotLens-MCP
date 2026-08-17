package chs.caplets.shared.properties;

import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caplets.logic.Model;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.parts.ILibraryObject;
import chs.common.IUIDObject;
import chs.ctf.editui.IModularConnectorClient;
import chs.ctf.editui.shared.ISharedModularConnectorClient;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utilities.ui.property.IPropertyValidityListener;
import chs.utilities.ui.property.ValidityChangeEvent;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.revisioning.ValidationObject;
import chs.utility.logic.LogicObjectUtils;
import chs.utility.ui.IValidityListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ModularTab implements IPropertiesClientComponent, ChangeListener
{

	private Model m_model;
	private ModularTabDialog modularTabDialog;
	private List<IPropertyValidityListener> sharedObjectValidationListeners;

	ModularTab(Model model)
	{
		m_model = model;
		sharedObjectValidationListeners = new ArrayList<>();
	}

	public ModularTabDialog getModularTabDialog()
	{
		return modularTabDialog;
	}

	@Override public JPanel getWidget(IPropertiedSet propset)
	{
		IConnector connector = getSelectedConnector();
		//if(connector != null && connector.getNumPosition()>0)
		if (connector != null) {
			modularTabDialog = new ModularTabDialog(m_model);
			JPanel panel = modularTabDialog.getPanel(connector);
			modularTabDialog.getClient().addChangeListener(this);
			return panel;
		}
		return null;
	}

	public void doPartUpdate(ILibraryObject libraryObject)
	{
		if (modularTabDialog != null) {
			modularTabDialog.getClient().libraryPartChanged(libraryObject);
		}
	}

	@Nullable private IConnector getSelectedConnector()
	{
		IConnector selectedConnector = null;
		SelectSet selections = m_model.getController().getSelectMgr().getPreSelections();
		for (Selection selection : selections.getSelected()) {
			IUIDObject selectedUIDObj = UIDMgr.getNonDeletedObject(selection.getUID());
			ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(selectedUIDObj);
			if (logicObject != null) {
				if (LogicObjectUtils.isValidPositionContainer(logicObject)) {
					if (selectedConnector != null && selectedConnector != logicObject) {
						selectedConnector = null;
						break;
					}
					selectedConnector = (IConnector) logicObject;
				}
				else {
					selectedConnector = null;
					break;
				}
			}
		}
		return selectedConnector;
	}

	@Override public boolean isPropPage()
	{
		return true;
	}

	@Override public String getTabName(IPropertiedSet propset)
	{
		return ResourceMgr.getString(ModularTab.class, "ModularTab.Tab.Name");
	}

	@Override public boolean acceptsSet(IPropertiedSet propset)
	{
		return true;
	}

	@Override public boolean modifiesSet(IPropertiedSet propset)
	{
		return true;
	}

	@Override public void edit(IPropertiedSet propset)
	{
		if (modularTabDialog != null) {
			modularTabDialog.editModel();
		}
	}

	@Override public Set<ISharedObject> getSharedObjects()
	{
		return Collections.emptySet();
	}

	@Override public Set<ISharedObject> getEditedSharedObjects()
	{
		return Collections.emptySet();
	}

	@Override public void stopEditing(IPropertiedSet propset)
	{
		modularTabDialog = null;
	}

	@Override public void destroy()
	{
		m_model = null;
	}

	@Override public boolean isValid()
	{
		return true;
	}

	@Override public void addValidityListener(IValidityListener listener)
	{
		if (modularTabDialog != null && listener instanceof IPropertyValidityListener) {
			modularTabDialog.getClient().addValidityNameListener((IPropertyValidityListener) listener);
			addValidity(listener);
			if (modularTabDialog.getClient() instanceof ISharedModularConnectorClient) {
				sharedObjectValidationListeners.add((IPropertyValidityListener) listener);
			}
		}
	}

	@Override public void removeValidityListener(IValidityListener listener)
	{

	}

	@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
	{
	}

	@Nullable public IModularConnectorClient getModularConnectorClient()
	{
		if (modularTabDialog != null) {
			return modularTabDialog.getClient();
		}
		return null;
	}

	@Override public void stateChanged(ChangeEvent e)
	{
		IPropertyGroup modularTabUI = modularTabDialog.getClient().getUI();
		boolean areUIChangesProper =
				modularTabUI.getValidator() == null || modularTabUI.getValidator().validate(modularTabUI);
		for (IPropertyValidityListener listener : sharedObjectValidationListeners) {
			ValidationObject validationObject = modularTabDialog.getClient().validateModel();
			boolean valid = !validationObject.hasErrors() && areUIChangesProper;
			listener.validityChanged(new ValidityChangeEvent(modularTabUI, !valid, valid));
		}
	}

	private void addValidity(IValidityListener listener)
	{
		IPropertyGroup modularTabUI = modularTabDialog.getClient().getUI();
		if (modularTabUI != null) {

			modularTabUI.addValidityListener((IPropertyValidityListener) listener);

			modularTabUI.addValidator(new IPropertyValidator()
			{
				@Override public boolean validate(IProperty property)
				{
					ValidationObject validationObject = modularTabDialog.getClient().validateModel();
					return !validationObject.hasErrors();
				}

				@Nullable @Override public String getValidityReason()
				{
					ValidationObject validationObject = modularTabDialog.getClient().validateModel();
					List<String> errors = validationObject.getErrors();
					if (!errors.isEmpty()) {
						return errors.get(0);
					}
					return null;
				}
			});
		}
	}
}
