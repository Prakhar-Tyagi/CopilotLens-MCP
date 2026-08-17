package chs.caplets.logic.properties;

import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.caf.caplet.helpers.GfxTextActionListener;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.cof.draw.IText;
import chs.cof.logical.schem.ISchemSector;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.attr.IAttributeTypes;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.system.UIDMgr;
import chs.utilities.CHSConstants;
import chs.utilities.HybridSet;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.EllipsisButtonEnum;
import chs.utilities.ui.IMultipleEditingFlags;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.IComponentProperty;
import chs.utilities.ui.property.IObjectProperty;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IPropertyValidityListener;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.OrientationValue;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyGroup;
import chs.utilities.ui.property.PropertyPanel;
import chs.utilities.ui.property.comp.PropertyButton;
import chs.utilities.ui.property.validator.FixedLengthPropertyValidator;
import chs.utility.GfxObjectUtils;
import chs.utility.helpers.TextHelper;
import chs.utility.ui.IValidityListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LogicSectorControl implements IPropertiesClientComponent
{

	private IPropertyGroup mGroupBox;
	private IStringProperty mLocationProperty;
	private IStringProperty mFunctionProperty;
	@NotNull private final Set<ISchemSector> mSelectedObjects;
	@NotNull private final IPropertiesClient mClient;

	private IBooleanProperty mLocationVisibleProperty;
	private IBooleanProperty mFunctionVisibleProperty;
	private IObjectProperty mLocationGfxProperty;
	private IObjectProperty mFunctionGfxProperty;

	public LogicSectorControl(@NotNull IPropertiesClient client)
	{
		mClient = client;
		mSelectedObjects = new HybridSet<>();
	}

	@Nullable @Override public JPanel getWidget(IPropertiedSet propset)
	{
		if (!acceptsSet(propset)) {
			return null;
		}

		mSelectedObjects.clear();
		for (ISchemSector schemSector : propset.getFilteredObjects(ISchemSector.class)) {
			mSelectedObjects.add(schemSector);
		}
		if (mSelectedObjects.isEmpty()) {
			return null;
		}

		buildIECAttributeComponents();
		Set<String> attribValues = getLocationAttribValues();
		String displayValue = getCommonAttribValue(attribValues);
		mLocationProperty.setValue(displayValue);


		attribValues = getFunctionAttribValues();
		displayValue = getCommonAttribValue(attribValues);
		mFunctionProperty.setValue(displayValue);


		// enable the control if the model is editable.
		mClient.isModelEditable();

		@SuppressWarnings("ConstantConditions")
		PropertyPanel propPanel = new PropertyPanel(null, mGroupBox);
		//propPanel.setEnabled(enabled);
		return propPanel;
	}

	public void buildIECAttributeComponents()
	{

		mGroupBox = new PropertyGroup("sectorgroup", GroupTypeValue.LABELLED_COLUMN);
		mGroupBox.setBorder(BorderValue.NONE);
		mGroupBox.setInsets(new Insets(5, 0, 0, 2));

		IPropertyGroup locationGroup = mGroupBox.createPropertyGroup("location", GroupTypeValue.ROW);
		locationGroup.setBorder(BorderValue.NONE);
		locationGroup.setAttribute(IPropertyAttributes.LABELLED_GROUP, Boolean.TRUE);
		locationGroup.setLabel(ResourceMgr.getString(LogicSectorControl.class, "LogicSectorControl.location.label"));
		//locationGroup.setInsets(new Insets(0, 40, 0, 2));

		IText locationTextRepresentation = mClient.getTextRepresentation(IAttributeTypes.IEC_LOCATION);
		if (locationTextRepresentation == null) {
			Set<ISchemSector> schemSectors = mClient.getPropertiedSet().getSchemSectors();
			if (schemSectors.iterator().hasNext()) {
				ISchemSector schemSector = schemSectors.iterator().next();
				locationTextRepresentation = TextHelper.createTempShortDescriptionText(schemSector);
			}
		}
		Boolean locationtextVisibilityForMultipleNamedObjOwners =
				mClient.getTextVisibilityForMultipleNamedObjOwners(IAttributeTypes.IEC_LOCATION);

		mLocationProperty =
				createProperty(IAttributeTypes.IEC_LOCATION, "LogicSectorControl.location.label", locationGroup);
		mLocationVisibleProperty = buildVisibilityComponent(locationGroup, "LocationVisibility", true,
				isVisibilityEditAllowed(locationTextRepresentation),
				locationtextVisibilityForMultipleNamedObjOwners);
		mLocationGfxProperty = buildGfxProperty(locationGroup, "locationGfxProp", "", null, Boolean.TRUE);

		mLocationVisibleProperty.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getNewValue() instanceof Boolean && mLocationGfxProperty != null) {
					mLocationGfxProperty.setEnabled(evt.getNewValue() != Boolean.FALSE);
				}
			}
		});
		mLocationProperty.addValidator(new FixedLengthPropertyValidator(CHSConstants.MAX_STRING_PROPERTY_LENGTH));

		if (locationTextRepresentation != null) {
			mLocationGfxProperty.setValue(locationTextRepresentation);
		}
		String fontDialogTitle =
				ResourceMgr.getString(LogicSectorControl.class, "LogicSectorControl.location.fontDialog.label");
		mLocationGfxProperty.setActionListener(createListener(mLocationGfxProperty, fontDialogTitle));

		IPropertyGroup functionGroup = mGroupBox.createPropertyGroup("function", GroupTypeValue.ROW);
		functionGroup.setBorder(BorderValue.NONE);
		functionGroup.setAttribute(IPropertyAttributes.LABELLED_GROUP, Boolean.TRUE);
		functionGroup.setLabel(ResourceMgr.getString(LogicSectorControl.class, "LogicSectorControl.function.label"));
		//functionGroup.setInsets(new Insets(0, 40, 0, 0));

		IText functionTextRepresentation = mClient.getTextRepresentation(IAttributeTypes.IEC_FUNCTION);
		if (functionTextRepresentation == null) {
			Set<ISchemSector> schemSectors = mClient.getPropertiedSet().getSchemSectors();
			if (schemSectors.iterator().hasNext()) {
				ISchemSector schemSector = schemSectors.iterator().next();
				functionTextRepresentation = TextHelper.createTempShortDescriptionText(schemSector);
			}
		}
		Boolean functiontextVisibilityForMultipleNamedObjOwners =
				mClient.getTextVisibilityForMultipleNamedObjOwners(IAttributeTypes.IEC_FUNCTION);

		mFunctionProperty =
				createProperty(IAttributeTypes.IEC_FUNCTION, "LogicSectorControl.function.label", functionGroup);
		mFunctionVisibleProperty = buildVisibilityComponent(functionGroup, "FunctionVisibility", true,
				isVisibilityEditAllowed(functionTextRepresentation),
				functiontextVisibilityForMultipleNamedObjOwners);
		mFunctionGfxProperty = buildGfxProperty(functionGroup, "FunctionGfxProp", "", null, Boolean.TRUE);
		mFunctionVisibleProperty.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getNewValue() instanceof Boolean && mFunctionGfxProperty != null) {
					mFunctionGfxProperty.setEnabled(evt.getNewValue() != Boolean.FALSE);
				}
			}
		});
		mFunctionProperty.addValidator(new FixedLengthPropertyValidator(CHSConstants.MAX_STRING_PROPERTY_LENGTH));
		fontDialogTitle =
				ResourceMgr.getString(LogicSectorControl.class, "LogicSectorControl.function.fontDialog.label");
		mFunctionGfxProperty.setActionListener(createListener(mFunctionGfxProperty, fontDialogTitle));
		if (functionTextRepresentation != null) {
			mFunctionGfxProperty.setValue(functionTextRepresentation);
		}
	}

	@NotNull protected GfxTextActionListener createListener(IObjectProperty gfxProperty,
			String fontDialogTitle)
	{
		return new GfxTextActionListener(gfxProperty, fontDialogTitle);
	}

	public IBooleanProperty buildVisibilityComponent(IPropertyGroup group, String name, boolean defaultValue,
			boolean isOverrideEnabled, Boolean isVisible)
	{
		IBooleanProperty booleanProperty = group.createBooleanProperty(name, "", defaultValue);
		booleanProperty.setEnabled(isOverrideEnabled);
		booleanProperty.setEditable(isOverrideEnabled);
		booleanProperty.setAttribute(IPropertyAttributes.THREE_STATE, Boolean.TRUE);
		booleanProperty.setThreeStateValue(isVisible);
		if (isVisible == null) {
			booleanProperty.setAttribute(IPropertyAttributes.THREE_STATE_CYCLE_MODEL, true);
		}
		return booleanProperty;
	}

	public IObjectProperty buildGfxProperty(IPropertyGroup group, String name, String label,
			@Nullable Object defaultValue, Boolean visible)
	{
		IObjectProperty m_gfxTextProperty = PropertyFactory.createObjectProperty(name, label, defaultValue);
		ImageIcon editAttributesIcon = CHSImageLoader.loadImageIcon(CHSImages.EDIT_ATTRS_SMALL);
		m_gfxTextProperty.setIcon(editAttributesIcon);
		m_gfxTextProperty.setIcon(editAttributesIcon);
		// Check the editAttrButton state based on the visibleCB
		m_gfxTextProperty.setEnabled(visible != Boolean.FALSE);
		m_gfxTextProperty.setBorder(BorderValue.NONE);

		IComponentProperty m_textComponentProperty = group.createComponentProperty("OptEditCompProp");

		m_textComponentProperty.setFill(OrientationValue.NONE);
		JComponent button = new PropertyButton(m_gfxTextProperty, EllipsisButtonEnum.NON_ELLIPSIS_BUTTON);
		button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		m_textComponentProperty.setValue(button);
		return m_gfxTextProperty;
	}

	private IStringProperty createProperty(String attribute, String resourceKey, IPropertyGroup group)
	{
		String label = ResourceMgr.getString(LogicSectorControl.class, resourceKey);
		IStringProperty stringProperty = PropertyFactory.createStringProperty(attribute, label, "");
		group.addProperty(stringProperty);
		return stringProperty;
	}

	@NotNull private Set<String> getLocationAttribValues()
	{
		Set<String> attribValues = new HashSet<>();
		for (ISchemSector selectedObject : mSelectedObjects) {
			String attrValue = selectedObject.getIECLocation();
			if (attrValue != null && !StringUtils.isBlank(attrValue)) {
				attribValues.add(attrValue);
			}
		}
		return attribValues;
	}

	@NotNull private Set<String> getFunctionAttribValues()
	{
		Set<String> attribValues = new HashSet<>();
		for (ISchemSector selectedObject : mSelectedObjects) {
			String attrValue = selectedObject.getIECFunction();
			if (attrValue != null && !StringUtils.isBlank(attrValue)) {
				attribValues.add(attrValue);
			}
		}
		return attribValues;
	}

	@Nullable private String getCommonAttribValue(Set<String> attribValues)
	{
		String displayValue = null;
		if (attribValues.size() == 1) {
			displayValue = attribValues.iterator().next();
		}
		else if (attribValues.size() > 1) {
			displayValue = IMultipleEditingFlags.MULTIPLE_VALUES;
		}
		return displayValue;
	}

	@Override public void edit(IPropertiedSet propset)
	{
		Set<ISchemSector> sectors = propset.getFilteredObjects(ISchemSector.class);
		if (sectors.isEmpty() || !acceptsSet(propset)) {
			return;
		}

		String locationValue = mLocationProperty.getValue();
		List<IText> locationTexts =
				mClient.getTextRepresentationWithCreate(IAttributeTypes.IEC_LOCATION);

		if (!IMultipleEditingFlags.MULTIPLE_VALUES.equals(locationValue)) {
			for (ISchemSector schemSector : sectors) {
				schemSector.setIECLocation(locationValue);
			}
		}
		for (IText locationText : locationTexts) {
			if (mLocationVisibleProperty != null && mLocationVisibleProperty.getThreeStateValue() != null
					&& mLocationVisibleProperty.getValue() != locationText.isMarkedVisible()) {
				locationText.setMarkedVisible(mLocationVisibleProperty.getValue());
			}

			if (mLocationGfxProperty != null && mLocationGfxProperty.isDirty()) {
				IText text = (IText) mLocationGfxProperty.getValue();
				locationText.setFont(text.getFont());
				locationText.setHeight(text.getHeight());
				locationText.setRotation(text.getRotation());
				locationText.setHorizontalJustification(text.getHorizontalJustification());
				locationText.setVerticalJustification(text.getVerticalJustification());
				locationText.setAttribute(text.getAttribute());
			}
		}
		String functionValue = mFunctionProperty.getValue();
		if (!IMultipleEditingFlags.MULTIPLE_VALUES.equals(functionValue)) {
			for (ISchemSector schemSector : sectors) {
				schemSector.setIECFunction(functionValue);
			}
		}
		List<IText> functionTexts =
				mClient.getTextRepresentationWithCreate(IAttributeTypes.IEC_FUNCTION);
		for (IText functionText : functionTexts) {
			if (mFunctionVisibleProperty != null && mFunctionVisibleProperty.getThreeStateValue() != null
					&& mFunctionVisibleProperty.getValue() != functionText.isMarkedVisible()) {
				functionText.setMarkedVisible(mFunctionVisibleProperty.getValue());
			}

			if (mFunctionGfxProperty != null && mFunctionGfxProperty.isDirty()) {
				IText text = (IText) mFunctionGfxProperty.getValue();
				functionText.setFont(text.getFont());
				functionText.setHeight(text.getHeight());
				functionText.setRotation(text.getRotation());
				functionText.setHorizontalJustification(text.getHorizontalJustification());
				functionText.setVerticalJustification(text.getVerticalJustification());
				functionText.setAttribute(text.getAttribute());
			}
		}
	}

	@Override public boolean isPropPage()
	{
		return false;
	}

	@Nullable @Override public String getTabName(IPropertiedSet propset)
	{
		return null;
	}

	@Override public boolean acceptsSet(IPropertiedSet propset)
	{

		Iterator<IUID> iter = propset.iterator();
		if(!iter.hasNext()){
			//Empty property set
			return false;
		}
		boolean allExpectedType = true;
		while (iter.hasNext()) {
			IUID uid = iter.next();
			IUIDObject obj = UIDMgr.getObject(uid);
			if (!(obj instanceof ISchemSector)) {
				allExpectedType = false;
				break;
			}
		}
		return allExpectedType;
	}

	@Override public boolean modifiesSet(IPropertiedSet propset)
	{
		return acceptsSet(propset);
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

	}

	@Override public void destroy()
	{

	}

	@Override public boolean isValid()
	{
		return true;
	}

	@Override public void addValidityListener(IValidityListener listener)
	{
		if (listener instanceof IPropertyValidityListener) {
			mLocationProperty.addValidityListener((IPropertyValidityListener) listener);
			mFunctionProperty.addValidityListener((IPropertyValidityListener) listener);
		}
	}

	@Override public void removeValidityListener(IValidityListener listener)
	{
		if (listener instanceof IPropertyValidityListener) {
			mLocationProperty.removeValidityListener((IPropertyValidityListener) listener);
			mFunctionProperty.removeValidityListener((IPropertyValidityListener) listener);
		}
	}

	@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
	{
		if (mLocationProperty != null) {
			mLocationProperty.addPropertyChangeListener(propertyChangeListener);
		}
		if (mFunctionProperty != null) {
			mFunctionProperty.addPropertyChangeListener(propertyChangeListener);
		}
	}

	//	private static class LocationEllipsesListener implements ActionListener
//	{
//
//		@Override public void actionPerformed(ActionEvent e)
//		{
//			// Just checking
//		}
//	}
//	public class GfxTextActionListener implements ActionListener
//	{
//
//		private final String mDialogTitle;
//		private IObjectProperty mGfxTextProperty;
//
//		public GfxTextActionListener(IObjectProperty gfxTextProperty, String dialogTitle)
//		{
//			mGfxTextProperty = gfxTextProperty;
//			mDialogTitle = dialogTitle;
//		}
//
//		public void actionPerformed(ActionEvent e)
//		{
//			Component src = (Component) e.getSource();
//			Dialog owner = (Dialog) SwingUtilities.getWindowAncestor(src);
//			final CAFOkCancelDialog dialog = new CAFOkCancelDialog(owner, mDialogTitle, true);
//			IText textObject = (IText)mGfxTextProperty.getValue();
//
//			if (textObject == null && CAFUtils.getInstance().getActiveCapletView() instanceof GfxView) {
//				// Have to have some text object or can't edit option text attributes - these project preference mgr
//				// methods create temporary, non-IUndoable IText objects with the right default values.
//				IProjectPreferenceMgr prefMgr = CAFUtils.getInstance().getCurrentProjectPreferences();
//				ISheet sheet = ((GfxView) CAFUtils.getInstance().getActiveCapletView()).getSheet();
//				if (prefMgr != null) {
//					if (sheet instanceof ISystemLogicDiagram) {
//						textObject = prefMgr.getLogicPropertyTextPreferences(((IGriddable) sheet).getGrid());
//					}
//				}
//			}
//			//
//			// Last chance - We have nothing to hang the attributes off - create one.
//			//
//			if (textObject == null) {
//				textObject = FactoryMgr.getDrawFactory().createText();
//			}
//			final TextAttributesEditor textAttributesEditor = new TextAttributesEditor(dialog, textObject);
//			dialog.getOkButton().addActionListener(new ActionListener()
//			{
//				public void actionPerformed(ActionEvent e)
//				{
//					IText text = textAttributesEditor.getText(FactoryMgr.getDrawFactory());
//					if (text != null) {
//						mGfxTextProperty.setDirty(true);
//						mGfxTextProperty.setValue(text);
//					}
//					dialog.dispose();
//				}
//			});
//			textAttributesEditor.addValidityListener(new IValidityListener()
//			{
//				public void validityChanged(Object source, boolean valid)
//				{
//					dialog.getOkButton().setEnabled(valid);
//				}
//			});
//			dialog.getCancelButton().addActionListener(new ActionListener()
//			{
//				public void actionPerformed(ActionEvent e)
//				{
//					dialog.dispose();
//				}
//			});
//			DialogHelper.centerOnOwner(dialog);
//			JPanel outer = new JPanel(new BorderLayout());
//			outer.add(textAttributesEditor.getComponent(), BorderLayout.CENTER);
//			dialog.getContentPane().add(outer);
//			dialog.pack();
//			dialog.show();
//		}
//	}

	public boolean isVisibilityEditAllowed(@Nullable IText text)
	{
		if (text == null) {
			return true;
		}
		return GfxObjectUtils.isVisibilityOverridable(text);
	}
}
