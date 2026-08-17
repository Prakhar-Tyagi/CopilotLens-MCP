/*
 * Copyright 2005-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared.autoshare;

import chs.caf.caplet.helpers.NamePropertyValidator;
import chs.caf.caplet.helpers.PropertiesClientHelper;
import chs.caf.caplet.helpers.ValidateNameResult;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.helper.ISelectSharedAdapter;
import chs.caplets.logic.actions.shared.helper.IShareMessageContextReporter;
import chs.caplets.logic.actions.shared.helper.SelectSharedHandler;
import chs.caplets.logic.shared.AbstractLockedSharedObjectFilter;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.cof.project.naming.INameMgr;
import chs.common.IReadOnlyNamedObject;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.PropertyFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 25, 2005 Time: 12:39:16 PM
 */
public class AutoSelectSharedView
{

	protected Model m_model;
	@NotNull protected SelectSharedHandler mHandler;

	public AutoSelectSharedView(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			boolean fromSymbol, @NotNull IShareMessageContextReporter reporter, boolean isShareInto,
			boolean isBulkShare)
	{
		final IProject project = design.getProject();
		final ISelectSharedAdapter adapter = new AutoSelectSharedAdapter(project, !isShareInto, reporter);
		mHandler = new SelectSharedHandler(model, design, fromSymbol, adapter, project, reporter, isBulkShare,
				(v, p) -> getPropertyValidationMessage(v, p));

		final IPinList pinlist = mHandler.getCablePinlist();
		if (mHandler.isModularConnectorWithAtLeastOneFilledPosition()) {
			AutoModularConnectorView modularConnectorView =
					new AutoModularConnectorView(model, design, (IConnector) pinlist, reporter, isBulkShare);
			modularConnectorView.setupModularHierarchy();
		}
	}

	public boolean isBackshellCompatible(@NotNull ISharedPinList targetSharedPinlist)
	{
		return mHandler.isBackshellCompatible(targetSharedPinlist);
	}

	public void init()
	{
		mHandler.init();
	}

	public boolean performShareInto(@NotNull ISharedPinList spl)
	{
		return mHandler.shareInto(spl);
	}

	@Nullable public IStringProperty getNameProperty()
	{
		return mHandler.getNameProperty();
	}

	@Nullable private static String getPropertyValidationMessage(
			@NotNull SelectSharedHandler.PropertyValidationErrorEnum propertyValidationEnum, @Nullable IPinList pinList)
	{
		final String objectType =
				pinList != null ? StringUtils.toLowerCase(COFTypeEnum.getDisplayableTypeName(pinList)) :
						StringUtils.EMPTY_STRING;
		switch (propertyValidationEnum) {
			case EmptyName:
				return ResourceMgr.getString(AutoSelectSharedView.class, "AutoSelectSharedView.InvalidName.text");
			case NameAndOptionAlreadyUsed:
				return ResourceMgr.getString(AutoSelectSharedView.class,
						"AutoSelectSharedView.nameandoptionexpressionalreadyused.text", objectType);
			case NameAlreadyUsed:
				return ResourceMgr
						.getString(AutoSelectSharedView.class, "AutoSelectSharedView.nameused.text", objectType);
			case InvalidRevision:
				return ResourceMgr
						.getString(AutoSelectSharedView.class, "AutoSelectSharedView.InvalidRevision.text", objectType);
			case LibraryPartOutOfDateByPins:
				return ResourceMgr
						.getString(AutoSelectSharedView.class, "AutoSelectSharedView.OutOfDatePartByPins.text",
								objectType);
		}
		return null;
	}

	private static class AutoSelectSharedAdapter implements ISelectSharedAdapter
	{

		@Nullable private IProject mProject;
		private final boolean mReportPropertyValidation;
		@NotNull private IShareMessageContextReporter mReporter;

		private AutoSelectSharedAdapter(@Nullable IProject project, boolean reportPropValidation,
				@NotNull IShareMessageContextReporter reporter)
		{
			mProject = project;
			mReportPropertyValidation = reportPropValidation;
			mReporter = reporter;
		}

		@NotNull @Override public IStringProperty createNameProperty(boolean isInlineJack)
		{
			return PropertyFactory.createStringProperty("SelectSharedPanel.SharedObjectNameTF", null, null);
		}

		@NotNull @Override
		public IBooleanProperty createGeneratedProperty(boolean defaultGeneratedValue, boolean isEnabled)
		{
			return PropertyFactory.createBooleanProperty("Generated", "Generated", defaultGeneratedValue);
		}

		@NotNull @Override public IStringProperty createMateNameProperty()
		{
			return PropertyFactory.createStringProperty("SelectSharedPanel.SharedObjectMateNameTF", null, null);
		}

		@NotNull @Override
		public IBooleanProperty createMateGeneratedProperty(boolean defaultMateGeneratedvalue, boolean isEnabled)
		{
			return PropertyFactory.createBooleanProperty("MateGenerated", "Generated", defaultMateGeneratedvalue);
		}

		@NotNull @Override public IStringProperty createRevisionProperty()
		{
			return PropertyFactory.createStringProperty("SelectSharedPanel.sharedrevision.text", null, null);
		}

		@NotNull @Override public AbstractLockedSharedObjectFilter getLockedSharedObjectFilter(boolean isBulkShare)
		{
			return new AutoLockedSharedObjectFilter(isBulkShare, mReporter);
		}

		@NotNull @Override
		public IPropertyValidator createNamePropertyValidator(@Nullable IReadOnlyNamedObject namedObject,
				@Nullable INameMgr nameMgr)
		{
			return new NamePropertyValidator(null, namedObject)
			{
				@Override protected ValidateNameResult validateNameProperty(@NotNull String propName,
						@Nullable IReadOnlyNamedObject namedObject)
				{
					return PropertiesClientHelper.validateNameProperty(propName, namedObject, nameMgr);
				}
			};
		}

		@Override public boolean shouldReportNameValidation()
		{
			return mReportPropertyValidation;
		}
	}
}
