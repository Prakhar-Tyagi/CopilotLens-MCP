package chs.caplets.shared.actions;

import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caplets.shared.GroupingByAttributesTree;
import chs.caplets.shared.IGroupAttributeAddEntry;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISystemLogicDesign;
import chs.cof.project.naming.INameMgr;
import chs.cof.project.naming.INamePropertyComp;
import chs.cof.project.naming.INamePropertyCompEntry;
import chs.cof.project.naming.INamePropertyCompIterator;
import chs.cof.project.naming.NameAttributeCompEntry;
import chs.cof.project.naming.NamePropertyComp;
import chs.cof.project.naming.NamePropertyCompEntry;
import chs.cof.project.naming.NameTextCompEntry;
import chs.cof.project.objectinfo.IObjectTypeInfo;
import chs.cof.project.objectinfo.properties.IPropertyTemplateIterator;
import chs.cof.project.objectinfo.properties.PropertyTemplateIterator;
import chs.common.preferencesets.IPreferenceSet;
import chs.ctf.ui.form.AbstractEntryDialog;
import chs.ctf.ui.form.FormMode;
import chs.ctf.ui.form.naming.CompositeNamingDialog;
import chs.ctf.ui.form.naming.ICompNamingAttrPropProvider;
import chs.images.CHSImageLoader;
import chs.system.FactoryMgr;
import chs.system.ISystemObjectTypeInfoMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utility.attr.AttributeHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign,
		Application.SEElectricalDesign, Application.ArtisanFunction, Application.ArtisanArchitect, Application.SvcDoc})
public class GroupAttributeAddHierarchyAction extends AppAction
{

	@Override public void updateUI()
	{
		setEnabled(isEnabled());
	}

	private Function<CompositeNamingDialog, INamePropertyComp> compositeNamingVerifierInTest;

	public static class AttributeParams
	{

		private GroupingByAttributesTree tree;

		public AttributeParams(GroupingByAttributesTree tree)
		{
			this.tree = tree;
		}

		public GroupingByAttributesTree getTree()
		{
			return tree;
		}
	}

	private AttributeParams attributeParams;

	public GroupAttributeAddHierarchyAction(
			AttributeParams param)
	{
		super(CAFUtils.getInstance().getFIB());
		putValue(NAME, ResourceMgr
				.getString(GroupAttributeAddHierarchyAction.class, "GroupAttributeAddHierarchyAction.action.name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(GroupAttributeAddHierarchyAction.class,
				"GroupAttributeAddHierarchyAction.action.shortdesc"));
		putValue(LONG_DESCRIPTION, ResourceMgr
				.getString(GroupAttributeAddHierarchyAction.class, "GroupAttributeAddHierarchyAction.action.longdesc"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_addhierarchy.png"));

		attributeParams = param;
	}

	@Override public void actionPerformed(ActionEvent e)
	{
		Collection<IGroupAttributeAddEntry> childAttributes = getTreeAttributesConfigured();
		INamePropertyComp namePropComp = new NamePropertyComp();
		final INamePropertyComp givenNamePropComp = namePropComp;
		childAttributes.forEach(attributeAlreadyAdded -> {
			if (attributeAlreadyAdded.isAttribute()) {
				givenNamePropComp.addEntry(new NameAttributeCompEntry(attributeAlreadyAdded.getName()));
			}
			else {
				givenNamePropComp.addEntry(new NamePropertyCompEntry(attributeAlreadyAdded.getName()));
			}
			if (attributeAlreadyAdded.seperator() != null) {
				for (String aSep : attributeAlreadyAdded.seperator()) {
					givenNamePropComp.addEntry(new NameTextCompEntry(aSep));
				}
			}
		});
		String separator = ResourceMgr.getString(CompositeNamingDialog.class, "CompositeNamingDialog.separator");
		String title = ResourceMgr.getString(CompositeNamingDialog.class, "CompositeNamingDialog.AddHierarchy.title");

		CompositeNamingDialog.AttributeCreationParams attributeCreationParams =
				new CompositeNamingDialog.AttributeCreationParams((anEntry) -> {
					return anEntry.getDisplayText();
				});
		String textForEditanleAttributes =
				ResourceMgr.getString(CompositeNamingDialog.class, "CompositeNamingDialog.editable.guidance");
		attributeCreationParams
				.addAnotherListOfAttributes(GroupingByAttributesTree.applicableAttributes, textForEditanleAttributes);
		IPropertyValidator propertyNameValidator = new IPropertyValidator()
		{
			private String reason = StringUtils.BLANK;

			@Override public boolean validate(IProperty property)
			{

				String value = CommonUtils.cast(property.getObject(), String.class);
				if (value != null) {
					return StringUtils.isEmpty(value) || isPropertyNameValid(value);
				}
				return false;
			}

			public boolean isPropertyNameValid(String value)
			{
				if (StringUtils.isBlank(value)) {
					reason = ResourceMgr.getString(CompositeNamingDialog.class,
							"CompositeNamingDialog.PropertyText.invalidnameerror");
					return false;
				}
				if (StringUtils.getNormalStoredLength(value) > CHSConstants.MAX_NAME_LENGTH) {
					reason = ResourceMgr.getString(CompositeNamingDialog.class,
							"CompositeNamingDialog.PropertyText.nametoolongerror");
					return false;
				}
				return true;
			}

			@Override public String getValidityReason()
			{
				return reason;
			}
		};

		final Frame dialogFrame = CAFUtils.getInstance().getDialogFrame();
		final ICompNamingAttrPropProvider compNamingAttrPropProvider = new GroupAttrCompNamingAttrPropProvider();

		final CompositeNamingDialog.TextCreationParams createText = new CompositeNamingDialog.TextCreationParams(
				separator, new SeparatorPropertyValidator(), ResourceMgr.getString(CompositeNamingDialog.class,
				"CompositeNamingDialog.Separator.guidance"));

		final CompositeNamingDialog.TextCreationParams createPropertyText =
				new CompositeNamingDialog.TextCreationParams(ResourceMgr.getString(CompositeNamingDialog.class,
						"CompositeNamingDialog.propertyName"), propertyNameValidator, null);

		final CompositeNamingDialog.QueryStatus createQuery = new CompositeNamingDialog.QueryStatus(false, false);

		final CompositeNamingDialog.PanelCreationParams panelCreationParams =
				new CompositeNamingDialog.PanelCreationParams(title, false, attributeCreationParams, createText,
						createPropertyText, false, createQuery, false, false);

		CompositeNamingDialog groupAttributesDialog = CompositeNamingDialog.createCompositeDialog(
				dialogFrame, FormMode.MODE_READ_WRITE, namePropComp, compNamingAttrPropProvider, panelCreationParams);

		groupAttributesDialog.setVisible(!Environment.isHeadless());
		if (compositeNamingVerifierInTest != null) {
			namePropComp = compositeNamingVerifierInTest.apply(groupAttributesDialog);
		}
		if (groupAttributesDialog.getExitStatus() == AbstractEntryDialog.OK) {
			namePropComp = groupAttributesDialog.getNamePropertyComp();
		}
		if (namePropComp != null) {

			List<IGroupAttributeAddEntry> attributesSelected = new ArrayList<>();
			INamePropertyCompIterator namePropertyCompIterator = namePropComp.getNamePropertyCompEntries();

			GroupingByAttributesTree.AttributeConfigured previousAttributeEntry = null;
			while (namePropertyCompIterator.hasNext()) {
				INamePropertyCompEntry namePropertyCompEntry = namePropertyCompIterator.getNext();
				if (namePropertyCompEntry.getType() == INamePropertyComp.TEXT_TYPE) {
					if (previousAttributeEntry != null) {
						previousAttributeEntry.addSeperator(namePropertyCompEntry.getValue(null));
					}
				}
				else if (namePropertyCompEntry.getType() == INamePropertyComp.ATTR_TYPE ||
						namePropertyCompEntry.getType() == INamePropertyComp.PROP_NAME_TYPE) {
					String anAttribute = namePropertyCompEntry.getText();
					boolean isAttribute = namePropertyCompEntry.getType() == INamePropertyComp.ATTR_TYPE;
					GroupingByAttributesTree.AttributeConfigured attributeConfigured =
							new GroupingByAttributesTree.AttributeConfigured(anAttribute, isAttribute);
					previousAttributeEntry = attributeConfigured;
					attributesSelected.add(attributeConfigured);
				}
			}

			attributeParams.getTree().treeConfigurationChanged(0, attributesSelected);
		}
	}

	private static class SeparatorPropertyValidator implements IPropertyValidator
	{

		@Override public boolean validate(IProperty property)
		{

			Object value = property.getObject();
			if (value instanceof String) {
				return ((CharSequence) value).length() <= 5;
			}
			return false;
		}

		@Override public String getValidityReason()
		{
			return ResourceMgr.getString(CompositeNamingDialog.class,
					"CompositeNamingDialog.Separator.lengthlimiterror");
		}
	}

	private class GroupAttrCompNamingAttrPropProvider implements ICompNamingAttrPropProvider
	{

		@Override
		public List<Class<?>> getApplicableClassesForAttributes(Class<?> inputContextClass)
		{
			ISystemObjectTypeInfoMgr typeInfoMgr = FactoryMgr.getCHSSystem().getSystemData().getObjectTypeInfoMgr();
			List<String> applicableTypes = new ArrayList<String>();
			ICapletController controller = getActiveCapletController();
			if (controller != null) {
				ICapletModel model = controller.getCapletModel();
				ILogicDesign logicDesign = model instanceof ILogicModel ? ((ILogicModel) model).getDesign() : null;
				if (logicDesign instanceof ISystemLogicDesign) {
					applicableTypes.addAll(getApplicableTypesForSystemDesign());
				}
				else if (logicDesign instanceof ILayoutLogicDesign) {
					applicableTypes.addAll(getApplicableTypesForLayoutDesign());
				}
				else if (logicDesign instanceof IFunctionLogicDesign) {
					applicableTypes.addAll(getApplicableTypesForFunctionDesign());
				}
			}
			List<Class<?>> applicableClasses = new ArrayList<>();
			for (String aPinlistType : applicableTypes) {
				IObjectTypeInfo anObjectType = typeInfoMgr.getByName(aPinlistType);
				applicableClasses.add(anObjectType.getAssociation());
			}
			return applicableClasses;
		}

		@NotNull protected List<String> getApplicableTypesForFunctionDesign()
		{
			return Arrays.asList(INameMgr.FUNCTION, INameMgr.FUNCTIONCONDUCTOR, INameMgr.MESSAGE);
		}

		@NotNull protected List<String> getApplicableTypesForLayoutDesign()
		{
			return Arrays.asList(INameMgr.DEVICE, INameMgr.GROUND, INameMgr.BACKSHELL, INameMgr.SPLICE,
					INameMgr.RECEPTACLE, INameMgr.PLUG, INameMgr.WIRECONDUCTOR, INameMgr.SHIELDCONDUCTOR,
					INameMgr.MULTICORE);
		}

		@NotNull protected List<String> getApplicableTypesForSystemDesign()
		{
			return Arrays.asList(INameMgr.DEVICE, INameMgr.GROUND, INameMgr.BACKSHELL, INameMgr.SPLICE,
					INameMgr.BLOCKDEVICE, INameMgr.RECEPTACLE, INameMgr.PLUG, INameMgr.WIRECONDUCTOR,
					INameMgr.NETCONDUCTOR, INameMgr.SHIELDCONDUCTOR, INameMgr.MULTICORE, INameMgr.ASSEMBLY);
		}

		@Override public IPropertyTemplateIterator getAllowedProperties()
		{
			return new PropertyTemplateIterator(Collections.emptyList());
		}

		@Override public List<Class<?>> getClassesForDiagramRepresentations(@Nullable IPreferenceSet preferenceSet)
		{
			return Collections.emptyList();
		}

		@Override public boolean isAttributeAllowed(String attributeName)
		{
			return !AttributeHelper.getLogicIgnoreAtrributes(false).contains(attributeName);
		}

		@Override public Collection<String> getAdditionalAttributes()
		{
			return Arrays.asList(GroupingByAttributesTree.ObjectTypeAttribute);
		}

		@Override public boolean areOTIPropertiesAllowed()
		{
			return false;
		}
	}

	protected Collection<IGroupAttributeAddEntry> getTreeAttributesConfigured()
	{
		return attributeParams.getTree().getChildAttributesConfigured();
	}

	void setCompositeNamingVerifierInTest(Function<CompositeNamingDialog, INamePropertyComp> verifierInTest)
	{
		compositeNamingVerifierInTest = verifierInTest;
	}
}
