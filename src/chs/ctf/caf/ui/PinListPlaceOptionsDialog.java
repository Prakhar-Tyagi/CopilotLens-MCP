/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2015-2026 Siemens
 */

package chs.ctf.caf.ui;

import chs.caplets.logic.BasicAddPinListDialog;
import chs.caplets.logic.actions.AddMultiSymbolledPinListDialog;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.OrientationValue;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyGroup;
import chs.utilities.ui.property.PropertyPanel;
import chs.utility.ui.PinSelectionAbstractPanel;
import chs.utility.ui.PinSelectionCommonPanel;
import chs.utility.ui.PinSelectionUserOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * @author aaluri on 24-11-2015.
 */
public class PinListPlaceOptionsDialog extends CAFOkCancelDialog implements IOptionsDetailProvider
{

	public static final double HORIZONTAL_WEIGHT = 0.01;
	public static final int CHECKBOX_WIDTH = 128;
	private Preferences prefs;

	// This flag is used to determine whether the dialog is being used in the context of adding a composite symbol,
	// in which case some options are not applicable and should not be loaded, saved, or displayed.
    private boolean m_isAddSymbolWorkingMode;

	protected static final String PLACE_BUTTON_LABEL =
			ResourceMgr.getString(PinListPlaceOptionsDialog.class, "PinListPlaceOptionsDialog.place.label");

	protected static final String CHECKBOX_GROUP = "checkBoxGroup";
	public static final String INDIVIDUAL_OPTION = "IndividuallyOption";
	public static final String AUTOGENERATE_OPTION = "AutoGenerateOption";
	public static final String AS_STACK_OPTION = "PlaceAsStackOption";
	public static final String AS_GROUP_OPTION = "PlaceAsGroupOption";
	public static final String REFERENCE_OPTION = "ReferenceOption";
	public static final String WITH_CONDUCTOR_OPTION = "WithConductorOption";
	public static final String LOAD_SHARED_PIN_INFO_OPTION = "LoadSharedPinConnectionInfoOption";

	// these options are used in composite symbol placement
	protected static final String INDIVIDUAL_SYMBOL_OPTION = "IndividuallySymbolOption";
	protected static final String AS_GROUP_SYMBOL_OPTION = "PlaceAsGroupSymbolOption";

	// tooltips for pin placement options
	protected static final String INDIVIDUAL_TOOLTIP =
			ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.individual.title");
	protected static final String AUTOGENERATE_TOOLTIP =
			ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.autoGenerateCheckBox.title");
	protected static final String REFERENCE_TOOLTIP = ResourceMgr
			.getString(AddMultiSymbolledPinListDialog.class, "AddMultiSymbolledPinListDialog.referenceCheckBox.title");
	protected static final String PLACEASSTACK_TOOLTIP =
			ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.placeAsStack.title");
	protected static final String PLACEASGROUP_TOOLTIP =
			ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.placeAsGroup.title");
	protected static final String WITH_CONDUCTOR_TOOLTIP = ResourceMgr.getString(PinListPlaceOptionsDialog.class,
			"PinListPlaceOptionsDialog.withConductorCheckbox.title");
	protected static final String LOAD_SHARED_PIN_INFO_TOOLTIP = ResourceMgr.getString(PinListPlaceOptionsDialog.class,
			"PinListPlaceOptionsDialog.loadSharedPinConnectionInfo.title");

	// tooltips for pin placement
	protected static final HashMap<String, String> tooltip_Messages = new HashMap<String, String>()
	{
		{
			put(INDIVIDUAL_TOOLTIP,
					ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.individual.tooltip"));
			put(AUTOGENERATE_TOOLTIP,
					ResourceMgr.getString(BasicAddPinListDialog.class,
							"DefaultAddPinListDialog.autoGenerateCheckBox.tooltip"));
			put(PLACEASSTACK_TOOLTIP,
					ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.placeAsStack.tooltip"));
			put(PLACEASGROUP_TOOLTIP,
					ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.placeAsGroup.tooltip"));
			put(WITH_CONDUCTOR_TOOLTIP,
					ResourceMgr.getString(PinListPlaceOptionsDialog.class,
							"PinListPlaceOptionsDialog.withConductorCheckbox.tooltip"));
			put(REFERENCE_TOOLTIP,
					ResourceMgr.getString(AddMultiSymbolledPinListDialog.class,
							"AddMultiSymbolledPinListDialog.referenceCheckBox.tooltip"));
			put(LOAD_SHARED_PIN_INFO_TOOLTIP,
					ResourceMgr.getString(PinListPlaceOptionsDialog.class,
							"PinListPlaceOptionsDialog.loadSharedPinConnectionInfo.tooltip"));
		}
	};

	// tooltips for composite symbol placement options
	protected static final HashMap<String, String> tooltip_MessagesSymbol = new HashMap<String, String>()
	{
		{
			put(INDIVIDUAL_TOOLTIP,
					ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.individualSymbol.tooltip"));
			put(PLACEASGROUP_TOOLTIP,
					ResourceMgr.getString(BasicAddPinListDialog.class, "DefaultAddPinListDialog.placeAsGroupSymbol.tooltip"));
		}
	};

	protected PinSelectionCommonPanel m_pinSelectionPanel = null;
	protected String pinListType;
	protected IPropertyGroup rootGroup;
	protected IPropertyGroup radioGroup;

	protected IBooleanProperty individualOption;
	protected boolean m_autoGenerate;
	@Nullable protected IBooleanProperty m_autoGenerateOption;
	protected boolean m_placeAsStack;
	protected IBooleanProperty m_placeAsStackOption;
	protected boolean m_placeAsGroup;
	protected IBooleanProperty m_placeAsGroupOption;
	protected IBooleanProperty m_withConductorOption;
	protected boolean m_reference;
	protected IBooleanProperty m_referenceOption;
	protected IBooleanProperty m_loadSharedDetailsOption;
	protected JPanel m_optionsPanel;

	protected PinListPlaceOptionsDialog(@Nullable Frame frame, String title, boolean modal, boolean symbolWorkingMode)
	{
		this(frame, title, modal);
		// The symbolWorkingMode flag indicates whether the dialog is being used for composite symbol placement,
		// which affects the availability and behavior of certain options.
		m_isAddSymbolWorkingMode = symbolWorkingMode;
	}
	protected PinListPlaceOptionsDialog(@Nullable Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		pinListType = "";
		prefs = Preferences.userNodeForPackage(PinListPlaceOptionsDialog.class);
		m_isAddSymbolWorkingMode = false;
	}

	@NotNull protected String extendedSuffix()
	{
		return "";
	}

	protected void loadPrefs()
	{
		loadOptionPref(individualOption);
		loadOptionPref(m_placeAsGroupOption);

		loadOptionPref(m_referenceOption);

		if (!m_isAddSymbolWorkingMode) {
			// Only load these options for pin placement, as they are not applicable for composite symbol placement
			loadOptionPref(m_autoGenerateOption);
			loadOptionPref(m_placeAsStackOption);
			loadOptionPref(m_withConductorOption);
		}
	}

	protected void savePrefs()
	{
		saveOptionPrefIfPresent(individualOption);
		saveOptionPrefIfPresent(m_placeAsGroupOption);

		saveOptionPrefIfPresent(m_referenceOption);

		if (!m_isAddSymbolWorkingMode) {
			// Only save these options for pin placement, as they are not applicable for composite symbol placement
			saveOptionPrefIfPresent(m_autoGenerateOption);
			saveOptionPrefIfPresent(m_placeAsStackOption);
			saveOptionPrefIfPresent(m_withConductorOption);

			saveOptionsPrefNotPresent();
		}
	}

	protected void saveOptionPrefIfPresent(@Nullable IBooleanProperty property)
	{
		Optional.ofNullable(property)
				.ifPresent(option -> saveOptionPrefs(option, option.getValue()));
	}

	protected void dialogDisposed()
	{
		if (getPinSelectionPanel() != null) {
			getPinSelectionPanel().destroy();
		}
	}

	@Nullable protected PinSelectionAbstractPanel getPinSelectionPanel()
	{
		return null;
	}

	private void saveOptionsPrefNotPresent()
	{
		saveDefaultOptionPrefIfNotPresent(m_autoGenerateOption, AUTOGENERATE_OPTION);
		saveDefaultOptionPrefIfNotPresent(m_referenceOption, REFERENCE_OPTION);
		saveDefaultOptionPrefIfNotPresent(m_placeAsStackOption, AS_STACK_OPTION);
		saveOptionPrefIfNotPresent(m_withConductorOption, WITH_CONDUCTOR_OPTION, m_reference || m_placeAsStack);
	}

	private void saveDefaultOptionPrefIfNotPresent(@Nullable IBooleanProperty property, @NotNull String propertyName)
	{
		saveOptionPrefIfNotPresent(property, propertyName, true);
	}

	private void saveOptionPrefIfNotPresent(@Nullable IBooleanProperty property, @NotNull String propertyName,
			boolean isSaveNeeded)
	{
		if (property == null && isSaveNeeded) {
			prefs.putBoolean(propertyName + pinListType + extendedSuffix(), false);
		}
	}

	protected void loadOptionPref(@Nullable IBooleanProperty property)
	{
		if (property != null) {
			setPropertyPref(property.getName(), prefs.getBoolean(property.getPreferenceKey(),
					property.getDefaultValue()));
		}
	}

	protected void saveOptionPrefs(@Nullable IBooleanProperty property, boolean value)
	{
		if (property != null) {
			prefs.putBoolean(property.getPreferenceKey(), value);
		}
	}

	private void setPropertyPref(String option, boolean prefsBoolean)
	{
		IBooleanProperty property = rootGroup.getBooleanPropertyByName(option);
		if (property != null) {
			property.setValue(prefsBoolean);
		}
	}

	@NotNull public IBooleanProperty buildOption(String option, String tooltip, boolean isRadio)
	{
		IBooleanProperty property = radioGroup.createBooleanProperty(option, tooltip, false);
		if (!isRadio) {
			property.setChoiceType(ChoiceTypeValue.CHECK_BOX);
		}
		property.setLayoutParams(OrientationValue.HORIZONTAL, HORIZONTAL_WEIGHT, 0.0);
		// For tooltip, if in composite symbol placement mode and there is a specific tooltip for symbol placement, use it.
		// Otherwise, use the default tooltip.
		String tooltip_Value = m_isAddSymbolWorkingMode? tooltip_MessagesSymbol.get(tooltip) : tooltip_Messages.get(tooltip);
		property.setResources(-1,
				tooltip_Value != null ? tooltip_Value : tooltip,
				option + pinListType + extendedSuffix());
		return property;
	}

	public void buildTypeSpecificOptions(@Nullable PinListTypeEnum plType, @NotNull IPlacementOptionParams params)
	{
		String plTypeName = plType == null ? "" :
				plType.isConnector() || plType.isRingTerminal() ? "_TypeConnector" : plType.getNonI18NName();
		buildOptionsPanel(plTypeName, params);
	}

	protected void buildOptionsPanel(String type, @NotNull IPlacementOptionParams params)
	{
		pinListType = type;
		doBuildOptionsPanel(params);
		int numOfCheckBoxes = 0;
		for (IProperty property : radioGroup.getProperties()) {
			if (ChoiceTypeValue.CHECK_BOX.equals(property.getChoiceType())) {
				numOfCheckBoxes++;
			}
		}
		Dimension minimumSize = getMinimumSize();
		double minwidth = minimumSize.getWidth();
		minwidth += numOfCheckBoxes * CHECKBOX_WIDTH;
		Dimension newDimension = new Dimension();
		newDimension.setSize(minwidth, minimumSize.getHeight());
		setMinimumSize(newDimension);
	}

	protected void doBuildOptionsPanel(@NotNull IPlacementOptionParams params)
	{
		initOptionsPanel(params);
		PropertyPanel rootPanel = createPropertyPanel();
		m_optionsPanel.add(rootPanel, BorderLayout.CENTER);
		loadPrefs();
	}

	protected void initOptionsPanel(@NotNull IPlacementOptionParams params)
	{
		if (params.isIndividuallyOptionEnabled()) {
			createIndividualOption();
		}
		if (params.isAutoGenerateOptionEnabled()) {
			createAutoGenerateOption();
		}
		if (params.isAsStackOptionEnabled()) {
			createAsStackOption();
		}
		if (params.isAsGroupOptionEnabled()) {
			createAsGroupOption();
		}
		if (params.isWithConductorOptionEnabled()) {
			createWithConductorCheckBox();
		}
		if (params.isAsReferenceOptionEnabled()) {
			createAsReferenceCheckBox();
		}
		if (params.isLoadSharedPinInfoOptionEnabled()) {
			createLoadSharedPinConnectionInfoCheckBox();
		}
	}

	protected void createIndividualOption()
	{
		// For composite symbol placement, we reuse the same individual placement option but with different resources,
		// The preference key remains the same for both cases, as the preference is applicable to both pin and composite symbol placement.
		individualOption =
				buildOption(m_isAddSymbolWorkingMode ? INDIVIDUAL_SYMBOL_OPTION : INDIVIDUAL_OPTION, INDIVIDUAL_TOOLTIP,
						true);
	}

	protected void createAutoGenerateOption()
	{
		m_autoGenerateOption = buildOption(AUTOGENERATE_OPTION, AUTOGENERATE_TOOLTIP, true);
		m_autoGenerateOption.setName(getAutoGenerationOptionComponentName());
		m_autoGenerateOption.setMnemonic(ResourceMgr.getMnemonic(PinListPlaceOptionsDialog.class,
				"PinListPlaceOptionsDialog.autoGenerateCheckBox.mnemonic"));
		m_autoGenerateOption.setEnabled(true);
		m_autoGenerateOption.addPropertyChangeListener(evt -> m_autoGenerate = (boolean) evt.getNewValue());
	}

	protected void createAsStackOption()
	{

	}

	protected void createAsGroupOption()
	{
		// For composite symbol placement, we reuse the same place as group option but with different resources,
		// The preference key remains the same for both cases, as the preference is applicable to both pin and composite
		m_placeAsGroupOption =
				buildOption(m_isAddSymbolWorkingMode ? AS_GROUP_SYMBOL_OPTION : AS_GROUP_OPTION, PLACEASGROUP_TOOLTIP,
						true);
		m_placeAsGroupOption.addPropertyChangeListener(evt -> m_placeAsGroup = (boolean) evt.getNewValue());
	}

	protected void createWithConductorCheckBox()
	{
		m_withConductorOption = buildOption(WITH_CONDUCTOR_OPTION, WITH_CONDUCTOR_TOOLTIP, false);
		m_withConductorOption.setName("chkWithConductor");
		m_withConductorOption.setMnemonic(ResourceMgr.getMnemonic(PinListPlaceOptionsDialog.class,
				"PinListPlaceOptionsDialog.withConductorCheckbox.mnemonic"));
		m_withConductorOption.addPropertyChangeListener(new PinPlaceOptionStateHandler(this));
	}

	protected void createAsReferenceCheckBox()
	{

	}

	protected void createLoadSharedPinConnectionInfoCheckBox()
	{

	}

	protected void addLoadSharedPinConnectionInfo(@NotNull Consumer<Boolean> consumer)
	{
		m_loadSharedDetailsOption = buildOption(LOAD_SHARED_PIN_INFO_OPTION, LOAD_SHARED_PIN_INFO_TOOLTIP, false);
		m_loadSharedDetailsOption.setMnemonic(ResourceMgr.getMnemonic(PinListPlaceOptionsDialog.class,
				"PinListPlaceOptionsDialog.loadSharedPinConnectionInfo.mnemonic"));
		String name = ResourceMgr.getString(PinListPlaceOptionsDialog.class,
				"PinListPlaceOptionsDialog.loadSharedPinConnectionInfo.title");
		m_loadSharedDetailsOption.setLabel(name);
		m_loadSharedDetailsOption.setToolTipText(name);
		m_loadSharedDetailsOption
				.addPropertyChangeListener(evt -> consumer.accept(m_loadSharedDetailsOption.getValue()));
	}

	@NotNull protected PropertyPanel createPropertyPanel()
	{
		if (radioGroup.getProperties().isEmpty()) {
			rootGroup.removeProperty(radioGroup);
		}
		return new PropertyPanel("optionsPanel", rootGroup);
	}

	protected void initOptionsPropertyGroup()
	{
		rootGroup = new PropertyGroup(CHECKBOX_GROUP, GroupTypeValue.ROW);
		rootGroup.setBorder(BorderValue.SIMPLE);
		rootGroup.setFill(OrientationValue.HORIZONTAL);
		radioGroup =
				PropertyFactory.createLabelledGroup(rootGroup, PLACE_BUTTON_LABEL, "", GroupTypeValue.RADIO_ROW, 1.0);
		radioGroup.setFill(OrientationValue.HORIZONTAL);
	}

	@NotNull
	protected String getAutoGenerationOptionComponentName()
	{
		return AUTOGENERATE_OPTION;
	}

	protected boolean canAutogeneratePins()
	{
		return false;
	}

	protected IBooleanProperty getLoadSharedDetailsOption()
	{
		return m_loadSharedDetailsOption;
	}

	protected void saveResetAndDisableOption(IBooleanProperty booleanProperty, boolean value)
	{
		saveOptionPrefs(booleanProperty, value);
		disableOption(booleanProperty);
	}

	protected void disableOption(@Nullable IBooleanProperty booleanProperty)
	{
		if (booleanProperty != null) {
			booleanProperty.setEnabled(false);
		}
	}

	protected boolean shouldOptionBeEnabled(IBooleanProperty property)
	{
		return true;
	}

	protected void resetOption(@Nullable IBooleanProperty property, boolean value)
	{
		if (property != null) {
			if (shouldOptionBeEnabled(property)) {
				property.setEnabled(true);
			}
			else {
				saveResetAndDisableOption(property, value);
			}
		}
	}

	public void setVisible(boolean show)
	{
		if (show) {
			updateRadioGroupStatus();
		}
		super.setVisible(show);
	}

	protected void updateRadioGroupStatus()
	{
		List<IProperty> properties = radioGroup.getProperties();
		boolean noOptionsSelected = false;
		for (IProperty property : properties) {
			if (property instanceof IBooleanProperty &&
					!(property.getChoiceType() == ChoiceTypeValue.CHECK_BOX)) {
				if (((IBooleanProperty) property).getValue()) {
					noOptionsSelected = true;
					break;
				}
			}
		}
		if (!noOptionsSelected) {
			if (individualOption != null) {
				individualOption.setValue(true);
			}
			else if (m_autoGenerateOption != null) {
				m_autoGenerateOption.setValue(true);
			}
		}
	}

	protected boolean isLoadSharedDetailsOn()
	{
		if (m_loadSharedDetailsOption != null) {
			return m_loadSharedDetailsOption.getValue();
		}
		return false;
	}

	protected boolean isReferenceOn()
	{
		if (m_referenceOption != null) {
			return m_referenceOption.getValue();
		}
		return false;
	}

	protected boolean isWithConductorOn()
	{
		if (m_withConductorOption != null) {
			return m_withConductorOption.getValue();
		}
		return false;
	}

	@NotNull protected PinSelectionUserOptions createUserSelectionOption()
	{
		return new PinSelectionUserOptions(isReferenceOn(), isLoadSharedDetailsOn());
	}

	// This method is used to enable or disable symbol placement options (individual and place as group)
	// when the dialog is used for composite symbol placement,
	public void enableSymbolPlaceOptions(boolean enable)
	{
		if (individualOption != null) {
			individualOption.setEnabled(enable);
		}
		if (m_placeAsGroupOption != null) {
			m_placeAsGroupOption.setEnabled(enable);
		}
	}

	protected void placeAsStackButtonStatusUpdate(@Nullable List<?> pinObjects)
	{
		if (m_placeAsStackOption != null) {
			if (pinObjects != null && !pinObjects.isEmpty() && hasPinsFromDifferentPinLists(pinObjects)) {
				m_placeAsStackOption.setEnabled(false);
				m_placeAsStackOption.setValue(false);
				updateRadioGroupStatus();
			}
			else {
				boolean symbolSelection = false;
				if (m_pinSelectionPanel != null) {
					symbolSelection = m_pinSelectionPanel.isSymbolSelection();
				}
				m_placeAsStackOption.setEnabled(!isReferenceOn() && !isWithConductorOn() && !symbolSelection);
			}
		}
	}

	private boolean hasPinsFromDifferentPinLists(@NotNull List<?> pinObjects)
	{
		IUIDObject owner = null;
		for (Object pin : pinObjects) {
			if (pin instanceof IPinProxy) {
				if (owner == null) {
					owner = getOwner((IPinProxy) pin);
				}
				else if (owner != getOwner((IPinProxy) pin)) {
					return true;
				}
			}
		}
		return false;
	}

	@Nullable private IUIDObject getOwner(IPinProxy pinProxy)
	{
		if (pinProxy.getSharedPin() != null) {
			return pinProxy.getSharedPin().getOwner();
		}
		return pinProxy.getCablePinList();
	}

	@Override @Nullable public IBooleanProperty getPlaceAsStackOption()
	{
		return m_placeAsStackOption;
	}

	protected void setPlaceAsStackOption(IBooleanProperty m_placeAsStackOption)
	{
		this.m_placeAsStackOption = m_placeAsStackOption;
	}

	@Override @Nullable public IBooleanProperty getWithConductorOption()
	{
		return m_withConductorOption;
	}

	@Override @Nullable public IBooleanProperty getReferenceOption()
	{
		return m_referenceOption;
	}

	@Override @Nullable public IBooleanProperty getIndividualOption()
	{
		return individualOption;
	}

	@Override public boolean isSymbolSelected()
	{
		return false;
	}
}
