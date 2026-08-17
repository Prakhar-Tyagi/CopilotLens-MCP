package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.cof.COFTypeEnum;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorIterator;
import chs.cof.project.IProject;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeTypes;
import chs.ctf.ui.form.RenameDialog;
import chs.services.gfx.GfxView;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.LanguageDictionaryHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;

/**
 * @author: Balaraju Kadukuntla
 * @Date: Feb 26, 2010 12:00:37 PM
 */
public class UnshareSegmentContainerActionHelper
{

	protected IDesign m_design;
	protected String m_newName = null;
	@Nullable protected String shortDescription = null;
	protected boolean m_bIncludeAllInstances = false;

	@Nullable private INameTemplate m_nameTemplate = null;
	@Nullable private INameTemplate m_signalMesssageTemplate = null;

	public UnshareSegmentContainerActionHelper(IDesign design)
	{
		m_design = design;
	}

	/**
	 * Throws a dialog and prompts to rename the highway/conductor being converted to a local object
	 *
	 * @param object The cable highway/conductor that may be renamed
	 * @return boolean True if succeeds
	 */
	protected boolean promptRenameLocalObject(ILogicObject object)
	{
		return promptRenameLocalObject(object, false, false, null);
	}

	/**
	 * Throws a dialog and prompts to rename the highway/conductor being converted to a local object
	 *
	 * @param object  The cable highway/conductor that may be renamed
	 * @param bEnable Unshare all the instances ?
	 * @return boolean True if succeeds
	 */
	protected boolean promptRenameLocalObject(ILogicObject object, boolean bEnable, boolean bValue, String toolTip)
	{

		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view == null) {
			// If the view is null - most likely we're in a unit test - let not both with the rename options
			return true;
		}

		String objectString = COFTypeEnum.from_object(object).toString();
		// Construct the rename dialog - overriding a couple of functions
		RenameDialog dialog = getRenameDialog(object, bEnable, bValue, toolTip, objectString,
				ResourceMgr.getString(UnshareSegmentContainerActionHelper.class,
						"UnshareSegmentContainerActionHelper.Rename.Title", objectString),
				ResourceMgr.getString(UnshareSegmentContainerActionHelper.class,
						"UnshareSegmentContainerActionHelper.Rename.label"), ResourceMgr
						.getString(UnshareSegmentContainerActionHelper.class,
								"UnshareSegmentContainerActionHelper.IncludeAllInstances.label"));

		// Display the dialog and get the new name.
		dialog.getOkButton().setEnabled(true);
		dialog.setVisible(true);
		m_newName = dialog.getNewName();
		if (m_newName == null) {
			return false;
		}
		m_bIncludeAllInstances = dialog.isIncludeAllInstances();
		IAttribute attribute = object.getAttribute(IAttributeTypes.SHORT_DESCRIPTION);
		shortDescription = attribute == null ? null : LanguageDictionaryHelper.getUntranslatedValueAsString(attribute);
		m_nameTemplate = dialog.getM_nameTemplate();
		m_signalMesssageTemplate = dialog.getM_signalMessageTemplate();
		// Check to see if there is a name clash with a shared conductor
		boolean sharedNameClash = false;
		String warningString = "UnshareSegmentContainerActionHelper.NameExistsError.Message.text";
		if (object instanceof IConductor) {
			ISharedConductorIterator condIter = m_design.getProject().getSharedConductorMgr().getSharedConductors();
			while (condIter.hasNext()) {
				ISharedConductor sharedCond = condIter.getNext();
				if (sharedCond.getName().equals(m_newName)) {
					sharedNameClash = true;
					warningString = "UnshareSegmentContainerActionHelper.NameExistsError.SharedMessage.text";
				}
			}
		}

		// Check to see if there is a name clash with another local conductor
		if (sharedNameClash || object.getNameMgr().nameExists(m_newName, object)) {
			// Even if there is, let them do it if they really REALLY want to.
			if (!MessageHelper.showDoNotShowThisMessageAgainYesNoDialogue(
					CAFUtils.getInstance().getWindowMgr().getDialogFrame(), "",
					ResourceMgr.getString(UnshareSegmentContainerActionHelper.class,
							"UnshareSegmentContainerActionHelper.NameExistsError.Header.text"),
					ResourceMgr.getString(UnshareSegmentContainerActionHelper.class,
							warningString, objectString, m_newName) +
							ResourceMgr.getString(UnshareSegmentContainerActionHelper.class,
									"UnshareSegmentContainerActionHelper.NameExistsError.Question.text"))) {
				return false;
			}
		}
		return true;
	}

	@NotNull
	protected ConductorRenameDialog getRenameDialog(ILogicObject object, boolean bEnable, boolean bValue,
			String toolTip, String objectString, String dialogTitle, String renameLabel,
			String includeAllInstancesLabel)
	{
		return new ConductorRenameDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				object, dialogTitle, renameLabel, false, m_design.getProject(),
				object instanceof IConductor, bEnable, bValue, toolTip, includeAllInstancesLabel);
	}

	protected static class ConductorRenameDialog extends RenameDialog
	{

		private ILogicObject object;

		ConductorRenameDialog(Frame parent, ILogicObject namedObj, String title,
				String label, boolean useWarning, IProject proj, boolean bShowIncludeAllInstances, boolean bEnabled,
				boolean bValue, String toolTip, String checkBoxLabel)
		{
			super(parent, namedObj, title, label, useWarning, proj, bShowIncludeAllInstances, bEnabled, bValue, toolTip,
					checkBoxLabel);
			object = namedObj;
		}

		// Return null if valid; errmsg otherwise.  Check name is not empty and not a duplicate
		public String checkValidName(String newName, String oldname)
		{
			if (StringUtils.getTrimmed(newName) == null) {
				return "";
			}
			if (newName.length() > CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH) {
				return ResourceMgr.getString(UnshareSegmentContainerActionHelper.class,
						"UnshareSegmentContainerActionHelper.NameTooLong",
						String.valueOf(CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH));
			}
			// Other than that, always return true - any name is valid
			return null;
		}

		// Need to override this too as we don't mind if the name is the same as the original.
		public String getNewName()
		{
			if (isCancelled()) {
				return null;
			}
			String nn = getNameProperty().getValue().trim();
			if (nn != null && nn.length() > 0) {
				return nn;
			}
			else {
				return null;
			}
		}

		@NotNull public String getHelpID()
		{
			return "chs.caplets.logic.actions.shared.UnshareSegmentContainerActionHelper_" +
					COFTypeEnum.from_object(object);
		}
	}

	@Nullable public INameTemplate getM_nameTemplate()
	{
		return m_nameTemplate;
	}

	@Nullable public INameTemplate getM_signalMesssageTemplate()
	{
		return m_signalMesssageTemplate;
	}
}
