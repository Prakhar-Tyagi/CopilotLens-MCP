/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2010-2023 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.IHarnessEditingAction;
import chs.caf.caplet.helpers.PropertiesAction;
import chs.caf.caplet.helpers.SmartEditPropertiesAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IPrivilegedDevice;
import chs.cof.logical.schem.ISchemSector;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import chs.cof.project.naming.IIndexedNamedObject;
import chs.common.IShortDescriptionObject;
import chs.common.IUIDObject;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.editui.ICommonUIClient;
import chs.ctf.editui.INameClient;
import chs.ctf.editui.IShortDescriptionClient;
import chs.ctf.editui.NameClient;
import chs.ctf.editui.NameUIProperty;
import chs.ctf.editui.ShortDescriptionClient;
import chs.utilities.CommonUtils;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utility.helpers.DuplicateSharedObjectValidator;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.preferences.StylebleObjectUtility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

@SuppressWarnings({"ClassNameSameAsAncestorName"})
public class SmartEditAction extends chs.caf.caplet.helpers.SmartEditAction implements IHarnessEditingAction
{

	public SmartEditAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected void initStylableObjectUtility()
	{
		super.initStylableObjectUtility();
		m_stylebleObjectUtility = new StylebleObjectUtility();
	}

	public boolean isEnabled()
	{
		// todo ActionHierarchy this action does not call super.isEnabled - is this correct
		// This will make enabling and disabling from the framework difficult
		if (CAFUtils.getInstance().getFIB().isTaskActive(IFIB.TASK_SAVE) ||
				CAFUtils.getInstance().getUserSession() == null) {
			return false;
		}
		return getController().getCapletModel().isEditable() && isModeEnabled();
	}

	@Nullable protected IAction getDelegateAction(IUIDObject object)
	{
		ICapletController controller = getController();
		SelectSet preSelections = controller.getSelectMgr().getPreSelections();
		preSelections.remove(m_object.getUID());
		preSelections.add(new Selection(object));
		return controller.getAction(SmartEditPropertiesAction.class);
	}

	@Override protected boolean isObjectEditable()
	{
		IAttributeProvider attributeProvider = m_att != null ? m_att.getOwner() : null;
		if (getController().getCaplet().isLayoutCaplet() &&
				ILogicObject.class.isInstance(attributeProvider) &&
				!ILogicOtherComponent.class.isInstance(attributeProvider)) {
			return false;
		}
		return super.isObjectEditable();
	}

	protected boolean isSmartEditable(@NotNull IAttribute attribute)
	{
		String attributeName = attribute.getName();
		if (attribute.getOwner() instanceof IConductor) {
			return !(attributeName.equals(IAttributeTypes.WIRE_CSA) ||
					attributeName.equals(IAttributeTypes.WIRE_SPEC) ||
					attributeName.equals(IAttributeTypes.WIRE_MATERIAL));
		}
		return true;
	}

	protected void invokeEditDialog()
	{
		if ((m_objectAttProvider != null) && (m_objectAttProvider instanceof IUIDObject)) {
			SelectSet selections = getController().getSelectMgr().getPreSelections();
			IDiagramObject parent = ((IDiagramObject) m_object).getParent();
			IAction action = getDelegateAction(parent);
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, action.getActionName(), 0);
			//dts0100874914 Editing shared conductor in MU mode resulting to exception. 
			for (ISharedObject propObj : m_sharedObjects) {
				new LockUpdateHelper(propObj).unlock();
			}
			getController().getActionMgr().actionPerformed(action, ae);
			registerIfInnerActionCompleted(action);
			selections.remove(parent.getUID());
		}
	}

	protected final void registerIfInnerActionCompleted(IAction action)
	{
		PropertiesAction propertiesAction = CommonUtils.cast(action, PropertiesAction.class);
		if (propertiesAction != null && propertiesAction.getCompleted()) {
			m_innerActionCompleted = true;
		}
	}

	@Override protected ICommonUIClient createSmartClient()
	{
		super.createSmartClient();
		ICommonUIClient smartClient = m_uiClient;
		if (smartClient instanceof NameClient && m_objectAttProvider instanceof IUIDObject) {
			IIndexedNamedObject namedObj = ((INameClient) smartClient).getNamedObject();
			if (namedObj != null) {
				IShortDescriptionClient m_shortDescriptionClient =
						new ShortDescriptionClient((IShortDescriptionObject) namedObj);
				((NameClient) smartClient).setShortDescriptionClientWithoutUpdate(m_shortDescriptionClient);
			}
		}
		else if (m_object instanceof IAttributeText) {
			IAttributeText attText = (IAttributeText) m_object;
			String attributeName = attText.getName();
			if (attributeName.equals(IAttributeTypes.IEC_FUNCTION) ||
					attributeName.equals(IAttributeTypes.IEC_LOCATION)) {
				if (m_objectAttProvider instanceof ISchemSector) {
					m_uiClient = new IECAttributePropertyClient(m_att, (ISchemSector) m_objectAttProvider);
				}
				if(m_objectAttProvider instanceof ILogicDesign){
					m_uiClient = new IECAttributePropertyClient(m_att,(ILogicDesign) m_objectAttProvider);
				}
			}
		}
		return m_uiClient;
	}

	@Override public boolean onTerminate(boolean successful)
	{
		if (successful && m_uiClient != null) {
			IPropertyGroup nameProperty = m_uiClient.getUI();
			if (nameProperty instanceof NameUIProperty) {
				if (((NameUIProperty) nameProperty).getShortDescriptionProp() != null) {
					//((NameUIProperty) nameProperty).setShortDescription("");
					((NameUIProperty) nameProperty).updateShortDescriptionProp(); // Replaces from OTI
				}
			}
		}
		boolean status = super.onTerminate(successful);
		return status;
	}

	@Override @NotNull
	protected ICommonUIClient createNameClient(@Nullable IIndexedNamedObject namedObject, @Nullable IProject project)
	{
		return new SmartEditNameClient(namedObject, project)
		{
			@Override protected boolean isNameAlreadyExistsInNameSpace(String name, IIndexedNamedObject nameObject)
			{
				if (super.isNameAlreadyExistsInNameSpace(name, nameObject)) {
					return true;
				}
				if (nameObject instanceof IDevicePin) {
					IDevicePin pin = (IDevicePin) nameObject;
					IPrivilegedDevice device = (IPrivilegedDevice) pin.getOwner();
					if (device != null && device.hasInternalPinByName(name, false)) {
						return true;
					}
				}
				ILogicObject logicObject = CommonUtils.cast(nameObject, ILogicObject.class);
				if (logicObject != null) {
					ISharedObject sharedObject = logicObject.getSharedObject();
					if (sharedObject != null && sharedObject instanceof IRevisionedSharedObject) {
						DuplicateSharedObjectValidator duplicateSharedObjectValidator =
								new DuplicateSharedObjectValidator((IRevisionedSharedObject) sharedObject, project);
						return duplicateSharedObjectValidator
								.checkForDuplicateSharedObjectBasedOnProjectPreference(name,
										sharedObject.getOptionExpression());
					}
				}
				return false;
			}
		};
//		}
//		return super.createNameClient(owner, namedObject, project);
	}

	protected boolean doTryEdit(@NotNull IAttributeProvider attProvider)
	{
		IUIDObject iuidObject = CommonUtils.cast(attProvider, IUIDObject.class);
		if (iuidObject != null) {
			if (!LogicObjectLockFinder.tryEdit(iuidObject)) {
				return false;
			}
			if (m_att != null && isAttributeEditable(attProvider)) {
				return false;
			}
		}
		return super.doTryEdit(attProvider);
	}

	private boolean isAttributeEditable(@NotNull IAttributeProvider attProvider)
	{
		if (IAttributeTypes.OPTION_EXP.equals(m_att.getName())) {
			return false;
		}
		return !attProvider.isFacetEditable(m_att);
	}

	@Override
	protected boolean areDesignChangesPropagated()
	{
		return m_uiClient instanceof IECAttributePropertyClient &&
				((IECAttributePropertyClient) m_uiClient).getDesignChangesPropagated();
	}

	@Override public boolean shouldTraverseAllDuringAutoPropagate()
	{
		return false;
	}
}
