/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.project.IProject;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.ctf.ui.form.NameBrowserActionListener;
import chs.utilities.ui.property.IStringProperty;
import chs.utility.helpers.NameTemplateHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;

/**
 * Unshare action helper for signals and messages
 */
public class UnshareFunctionBaseConductorActionHelper extends UnshareConductorActionHelper
{

	public UnshareFunctionBaseConductorActionHelper(IDesign theDesign)
	{
		super(theDesign);
	}

	@NotNull @Override
	protected ConductorRenameDialog getRenameDialog(ILogicObject object, boolean bEnable, boolean bValue,
			String toolTip, String objectString, String dialogTitle, String renameLabel,
			String includeAllInstancesLabel)
	{
		return new FunctionBaseConductorRenameDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), object,
				dialogTitle, renameLabel, false, m_design.getProject(), object instanceof IConductor, bEnable, bValue,
				toolTip, includeAllInstancesLabel);
	}

	/**
	 * Rename dialog will consider OTI template selection only if the selection was made in OTI dialog
	 * If name of signal/message was changed in text box, it will be considered as just name change of signal/message
	 * that was unshared.
	 */
	protected static class FunctionBaseConductorRenameDialog extends ConductorRenameDialog
	{

		FunctionBaseConductorRenameDialog(Frame parent, ILogicObject namedObj, String title, String label,
				boolean useWarning, IProject proj, boolean bShowIncludeAllInstances, boolean bEnabled, boolean bValue,
				String toolTip,
				String checkBoxLabel)
		{
			super(parent, namedObj, title, label, useWarning, proj, bShowIncludeAllInstances, bEnabled, bValue, toolTip,
					checkBoxLabel);
		}

		@Override
		protected void populateNameTemplateInfo(@NotNull NameBrowserActionListener ellipseListener,
				@NotNull IStringProperty property, @NotNull IStringProperty sd)
		{
			INameTemplate selectedNameTemplate = ellipseListener.getNameTempalte();
			if (selectedNameTemplate != null) {
				INameTemplate nameTemplate = NameTemplateHelper.getNameTemplateFromOTINameAndRevision(m_project,
						selectedNameTemplate.getName(), selectedNameTemplate.getRevision(), m_namedObj.getClass());
				m_nameTemplate = nameTemplate;
				m_signalMessageTemplate = ellipseListener.getSignalSourceNameTemplate();
				if (nameTemplate != null) {
					sd.setValue(NameTemplateHelper.getUntranslatedShortAndLongDescription(nameTemplate).getFirst());
				}
			}
		}
	}
}
