package chs.caplets.shared.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.shared.GroupingByAttributesTree;
import chs.cof.changepolicy.IChangePolicyMgr;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.project.IProject;
import chs.cof.project.objectinfo.IObjectTypeInfoMgr;
import chs.cof.project.objectinfo.properties.IPropertyTemplate;
import chs.cof.project.objectinfo.properties.IPropertyTemplateIterator;
import chs.common.IAttrOrPropProvider;
import chs.common.IDesignContainer;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IUID;
import chs.common.IUIDObjectIterator;
import chs.common.PropertyStabilityEnum;
import chs.common.RangeTypeEnum;
import chs.common.UIDObjectCollection;
import chs.common.attr.AttributeType;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IInternalAttributeSetter;
import chs.common.attr.SetAttributeResult;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.caf.utils.PropertiesClientUtilsBase;
import chs.system.FactoryMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.helpers.AttrOrPropHelper;
import chs.utility.helpers.IAttrOrPropHelperClient;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.PropertyTemplateHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.rules.AttrOrProp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class GroupAttributeUpdateAction extends ControllerActionRT
{

	public static class UpdateParams
	{

		private Collection<ILogicObject> logicObjects;
		private Collection<GroupAttributeUpdateData> logicObjectModifier;

		public UpdateParams(Collection<ILogicObject> logicObjects, Collection<GroupAttributeUpdateData> logicObjectModifier)
		{
			this.logicObjects = logicObjects;
			this.logicObjectModifier = logicObjectModifier;
		}

		public Collection<GroupAttributeUpdateData> getLogicObjectModifier()
		{
			return logicObjectModifier;
		}

		public Collection<ILogicObject> getLogicObjects()
		{
			return logicObjects;
		}
	}

	private Collection<UpdateParams> updateParams;

	protected class LogicObjectsToWorkUpon implements AutoCloseable
	{

		private Collection<ILogicObject> logicObjects;
		private Collection<ISharedObject> sharedObjects = new ArrayList<>();
		private Collection<ISharedObject> sharedObjectsLocked = new ArrayList<>();
		private ILogicDesign logicDesign;

		public LogicObjectsToWorkUpon(ILogicDesign logicDesign,
				Collection<ILogicObject> logicObjectsBeingUpdated)
		{

			this.logicDesign = logicDesign;
			logicObjects = logicObjectsBeingUpdated;
		}

		boolean areSharedObjectsLocked()
		{
			for (ILogicObject aLogicObject : logicObjects) {
				IPropertiedObject propObj = ReferenceHelper.reduceToPropertiedObject(aLogicObject);

				if (propObj instanceof ISharedObject) {
					sharedObjects.add(aLogicObject.getSharedObject());
					if (propObj instanceof ISharedMulticore) {
						Collection<IUID> sharedMCDependents =
								SharedConductorGroupHelper.findAllDependents((ISharedMulticore) propObj);
						for (IUIDObjectIterator it = new UIDObjectCollection(sharedMCDependents).getUIDObjects();
								it.hasNext(); ) {
							ISharedObject sharedDep = (ISharedObject) it.getNext();
							sharedObjects.add(sharedDep);
						}
					}
				}
			}
			for (ISharedObject aSharedObject : sharedObjects) {
				ISharedLockableUpdateableObject sharedObjectToLock = aSharedObject.getLockableUpdateableRoot();
				LockUpdateHelper luh = (sharedObjectToLock == null ? null : new LockUpdateHelper(sharedObjectToLock));

				if (luh == null || !luh.lockAndRefresh()) {
					return false;
				}

				sharedObjectsLocked.add(aSharedObject);
			}
			return true;
		}

		boolean edit(Map<ILogicObject, Collection<Function<ILogicObject, Boolean>>> logicObjectModifier)
		{

			for (ILogicObject logicObject : logicObjects) {
				for (Function<ILogicObject, Boolean> aSetter : logicObjectModifier.get(logicObject)) {
					if (!aSetter.apply(logicObject)) {
						return false;
					}
				}
			}
			return true;
		}

		@Override public void close()
		{
			if (sharedObjectsLocked != null) {
				for (ISharedObject aSharedObject : sharedObjectsLocked) {

					LockUpdateHelper.flushAndUnlockSharedObject(aSharedObject);
				}
			}
			if (!sharedObjects.isEmpty()) {
				IProject project = logicDesign.getProject();
				if (project != null && project.getSharedPinListMgr() != null) {
					project.getSharedPinListMgr().fireChangeEvent();
				}

				getController().getCapletModel().setModified(true);
				getController().getUndoableContainer().endEdit();
				getController().clearUndoQueue();
			}
		}
	}

	@Override protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	public GroupAttributeUpdateAction(@NotNull ICapletController controller, Collection<UpdateParams> updateParams)
	{
		super(controller);
		ICapletModel capletModel = getController().getCapletModel();
		if (capletModel instanceof ILogicModel) {
			this.updateParams = updateParams;
		}
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{

		ICapletModel capletModel = getController().getCapletModel();
		if (capletModel instanceof ILogicModel && successful) {
			ILogicModel logicModel = (ILogicModel) capletModel;
			return update(logicModel.getDesign());
		}
		return false;
	}

	@Override public String getActionUIClass()
	{
		return GroupAttributeUpdateActionUI.class.getName();
	}

	@NotNull @Override public Action getActionUI()
	{
		return new GroupAttributeUpdateActionUI(getController().getCaplet());
	}

	@Override protected boolean isModeEnabled()
	{
		return true;
	}

	public boolean update(ILogicDesign design)
	{

		Collection<ILogicObject> logicObjectsToUpdate = new ArrayList<>();
		for (UpdateParams anUpdateParam : updateParams) {
			logicObjectsToUpdate.addAll(anUpdateParam.getLogicObjects());
		}

		AttrOrPropHelper.IAttrOrPropLogger<IAttrOrPropProvider> attLogger =
				new AttrOrPropHelper.DefaultAttrOrPropLogger(null);

		IAttrOrPropHelperClient clientHelper = new IAttrOrPropHelperClient()
		{

			@Override public PropertyStabilityEnum getStability(@NotNull IAttribute attribute)
			{
				return attribute.getStability();
			}

			@Nullable @Override public IDesignContainer getDesignContext()
			{
				return design;
			}
		};

		try (LogicObjectsToWorkUpon logicObjectsToWorkUpon = new LogicObjectsToWorkUpon(design, logicObjectsToUpdate)) {

			if (logicObjectsToWorkUpon.areSharedObjectsLocked()) {
				Map<ILogicObject, Collection<Function<ILogicObject, Boolean>>> modifiers = new LinkedHashMap<>();
				for (UpdateParams anUpdate : updateParams) {
					for (ILogicObject aLogicObject : anUpdate.getLogicObjects()) {
						for (GroupAttributeUpdateData dataToUpdate : anUpdate.getLogicObjectModifier()) {

							Pair<String, String> nameValue = new Pair<String, String>(dataToUpdate.getName(), dataToUpdate.getValue());
							Function<ILogicObject, Boolean> attrSetter = null;
							Function<ILogicObject, Boolean> propSetter = null;
							if (dataToUpdate.isAttribute()) {
								AttrOrProp attrOrProp = AttrOrPropHelper
										.createAttrOrPropChange(aLogicObject, nameValue.getFirst(), nameValue.getSecond());
								if (!attrOrProp.isAttribute()) {
									if (StringUtils.isBlank(nameValue.getSecond())) {
										continue;
									}
									displayInapplicableAttributeMessage(nameValue, aLogicObject);
									return false;
								}
								IAttribute currentAttrValue =
										FactoryMgr.getAPIFactory().getInternalAttribute(aLogicObject, nameValue.getFirst());
								String currentValueString =
										(currentAttrValue != null ? currentAttrValue.getAsString() : "");
								if (StringUtils.equals(nameValue.getSecond(), currentValueString)) {
									continue;
								}
								if (!isApplicable(nameValue.getFirst())) {

									displayRestrictedAttributesMessage(nameValue);
									return false;
								}

								AttrOrPropHelper.CreateAttributeSetterResult result = AttrOrPropHelper
										.createInternalAttributeSetter(aLogicObject, nameValue.getFirst(),
												nameValue.getSecond(), clientHelper, attLogger, attrOrProp);

								Pair<IAttribute, IInternalAttributeSetter<? extends IAttributeProvider>> attributeSetter =
										result.getAttributeSetter();
								if (result.issueFound()) {
									return false;
								}
								if (attributeSetter == null) {
									continue; //attribute is already set to correct value
								}

								attrSetter = getAttributeSetter(attributeSetter, attLogger, nameValue, attrOrProp);
							}
							else {
								// check if property can be created or edited
								String propertyName = nameValue.getFirst();
								String propertyValue = nameValue.getSecond();
								if (isPropertyNotEditable(aLogicObject, propertyName, propertyValue)) {
									return false;
								}
								propSetter = getPropertySetter(propertyName, propertyValue);
							}
							Collection<Function<ILogicObject, Boolean>> setters =
									modifiers.computeIfAbsent(aLogicObject, objectToModify -> {
										return new ArrayList<Function<ILogicObject, Boolean>>();
									});

							if (attrSetter != null) {
								setters.add(attrSetter);
							}
							else {
								setters.add(propSetter);
							}
						}
					}
				}
				if (modifiers.isEmpty()) {
					return true; //Everything is upto date. Nothing to update.
				}

				return logicObjectsToWorkUpon.edit(modifiers);
			}
		}
		return false;
	}

	private void displayInapplicableAttributeMessage(Pair<String, String> nameValue, ILogicObject aLogicObject)
	{
		String errorMessage = ResourceMgr.getString(GroupAttributeUpdateAction.class,
				"GroupAttributeUpdateAction.update.notapplicable", ResourceMgr.getString(
						AttributeType.class, "AttributeType." + nameValue.getFirst()), aLogicObject.getName());
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(errorMessage);
	}

	private void displayRestrictedAttributesMessage(Pair<String, String> nameValue)
	{
		String errorMessage = ResourceMgr.getString(GroupAttributeUpdateAction.class,
				"GroupAttributeUpdateAction.update.restrictededitattrs", nameValue.getFirst());
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(errorMessage);
	}

	@NotNull private Function<ILogicObject, Boolean> getAttributeSetter(
			Pair<IAttribute, IInternalAttributeSetter<? extends IAttributeProvider>> attributeSetter,
			AttrOrPropHelper.IAttrOrPropLogger<IAttrOrPropProvider> attLogger, Pair<String, String> nameValue,
			AttrOrProp attrOrProp)
	{
		return (theLogicObjectToModify) -> {

			SetAttributeResult setAttributeResult =
					attributeSetter.getSecond().setAttributeValue(attLogger, theLogicObjectToModify,
							attributeSetter.getFirst(), attrOrProp.getType(), nameValue.getSecond());
			return setAttributeResult.isSuccess();
		};
	}

	@NotNull private Function<ILogicObject, Boolean> getPropertySetter(String propertyName, String propertyValue)
	{
		return (theLogicObjectToModify) -> {
			IProperty property = theLogicObjectToModify.findPropertyByName(propertyName);
			if (property != null) {
				// update if already present
				theLogicObjectToModify.removeProperty(property);
				theLogicObjectToModify.addProperty(FactoryMgr.getCommonFactory()
						.constructProperty(propertyName, property.getType(), propertyValue, theLogicObjectToModify));
			}
			else {
				// create property if not present
				theLogicObjectToModify.addProperty(FactoryMgr.getCommonFactory()
						.constructProperty(propertyName, propertyValue,theLogicObjectToModify));
			}
			return true;
		};
	}

	private boolean isPropertyNotEditable(@NotNull ILogicObject aLogicObject, @NotNull String propertyName,
			@NotNull String propertyValue)
	{
		ISharedObject sharedObject = aLogicObject.getSharedObject();
		String objectName = aLogicObject.getName();
		if (sharedObject != null) {
			boolean frozenStatus = PropertiesClientUtilsBase
					.isRestrictedByFrozen(Collections.singleton(sharedObject), sharedObject instanceof ISharedPin);
			if (frozenStatus) {
				String errorMessage = ResourceMgr.getString(GroupAttributeUpdateAction.class,
						"GroupAttributeUpdateAction.update.nonEditableProp", propertyName, objectName);
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(errorMessage);
				return true;
			}
		}
		AttrOrProp attrOrProp = AttrOrPropHelper.createAttrOrPropChange(aLogicObject, propertyName, propertyValue);
		IProperty property = aLogicObject.findPropertyByName(propertyName);
		if (property != null) {
			if (!property.isEditable()) {
				String errorMessage = ResourceMgr.getString(GroupAttributeUpdateAction.class,
						"GroupAttributeUpdateAction.update.nonEditableProp", propertyName, objectName);
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(errorMessage);
				return true;
			}
			if (!IChangePolicyMgr.Statics.allowsPropertyChange(aLogicObject, propertyName)) {
				String errorMessage = ResourceMgr.getString(GroupAttributeUpdateAction.class,
						"GroupAttributeUpdateAction.update.cannotEditProperty", propertyName, objectName);
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(errorMessage);
				return true;
			}
			// Check that the new value we are going to set on this object is valid
			if (!AttrOrPropHelper.validateAttribute(new AttrOrPropHelper.DefaultAttrOrPropLogger(null), aLogicObject, attrOrProp, property)) {
				return true;
			}
		}
		else {
			property = FactoryMgr.getCommonFactory().constructPropertyLocalized(propertyName, attrOrProp.getType(),
					propertyValue, aLogicObject);
			// Check that the new value we are going to set on this object is valid
			if (!AttrOrPropHelper.validateAttribute(new AttrOrPropHelper.DefaultAttrOrPropLogger(null), aLogicObject, attrOrProp, property)) {
				return true;
			}
			if (!PropertyHelper.canCreateNewProperties(Collections.singleton(aLogicObject.getClass())) ||
					!IChangePolicyMgr.Statics.allowsPropertyAddition(aLogicObject, propertyName)) {
				String errorMessage = ResourceMgr.getString(GroupAttributeUpdateAction.class,
						"GroupAttributeUpdateAction.update.cannotCreateProperty", propertyName, objectName);
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(errorMessage);
				return true;
			}
		}
		IProject project = aLogicObject.getProject();
		if (project != null) {
			IObjectTypeInfoMgr objTypeMgr = project.getObjectTypeInfoMgr();
			IPropertyTemplateIterator propTemplates =
					PropertyTemplateHelper.getTemplatesForObject(objTypeMgr, aLogicObject);
			if (propTemplates != null) {
				IPropertyTemplate propertyTemplate = PropertyTemplateHelper.getTemplateByName(propTemplates, propertyName);
				if (propertyTemplate != null) {
					boolean notOfTypeString = !"String".equalsIgnoreCase(propertyTemplate.getType());
					boolean notOfTypeUnconstrained =
							propertyTemplate.getConstraintStyle() != RangeTypeEnum.RangeStyleFreeForm;
					if (notOfTypeString || notOfTypeUnconstrained) {
						String errorMessage = ResourceMgr.getString(GroupAttributeUpdateAction.class,
								"GroupAttributeUpdateAction.update.nonEditablePropOti", propertyName, objectName);
						CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(errorMessage);
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean isApplicable(String attributeName)
	{
		return GroupingByAttributesTree.applicableAttributes.contains(attributeName);
	}
}
