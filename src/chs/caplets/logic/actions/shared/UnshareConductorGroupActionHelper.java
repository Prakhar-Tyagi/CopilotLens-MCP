/*
 * Copyright 2006-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.COFTypeEnum;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.INamedObject;
import chs.common.IUID;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeTypes;
import chs.ctf.ui.form.RenameDialog;
import chs.services.gfx.GfxView;
import chs.system.UIDMgr;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.LanguageDictionaryHelper;
import chs.utility.logic.MulticoreUnsharer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * UnshareConductorGroupActionHelper.  Responsible for unsharing a multicore.
 * <p>
 * This helper will unshare a connectivity multicore.  That is, all instances of a shared multicore in a design will
 * instead become an instance of a Design Wide Shared object.
 * <p>
 * This differs from Conductor unsharing which can work on either a SINGLE INSTANCE or ALL INSTANCES.  This is due to
 * the difficulties in determining what constitues a 'instance' of a multicore on a diagram.
 * <p>
 * This is a change to the previous functionality where unshare would unshare all instances on a DIAGRAM.  Due to design
 * wide shared objects - this rule was inadvertantly changed and essentially broke.
 */
public class UnshareConductorGroupActionHelper implements IShareActionHelper
{

	private IMulticore m_multicore;
	private IDesign m_design;
	private String m_newName = null;
	@Nullable private String shortDesc = null;
	private IUID m_sharedMulticoreUid;

	public UnshareConductorGroupActionHelper(IDesign design)
	{
		m_design = design;
	}

	@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		if (!(operands.target instanceof IMulticore)) {
			return IActionEnum.eCanceled;
		}

		if (!renameLocalMulticore((IMulticore) operands.target)) {
			return IActionEnum.eCanceled;
		}
		m_multicore = (IMulticore) operands.target;
		if (m_multicore != null) {
			m_sharedMulticoreUid = m_multicore.getSharedObjectUID();
		}
		return IActionEnum.eCompleted;
	}

	public boolean doEdit()
	{
		return unshareMulticore(m_multicore);
	}

	/**
	 * This function does all the work to unshare multicores and thier contents.   As multicores can contain other
	 * multicores, this function is recursive.
	 * <p>
	 * Essentially this function removes the reference to the shared object from the connectivity objects.  That is for
	 * all innercores (conductors) shields and owning MCs (including nested).  It also copies all attributes and
	 * properties and makes sure that property text matches up correctly.
	 *
	 * @param multicore The multicore that needs to be unshared
	 *
	 * @return boolean Was it successful
	 */
	private boolean unshareMulticore(IMulticore multicore)
	{
		if (m_design instanceof ILogicDesign) {
			COFTypeEnum editType = m_multicore instanceof IOverbraid ? COFTypeEnum.Overbraid : COFTypeEnum.Multicore;
			String failureMsg = ResourceMgr.getString(UnshareConductorGroupActionHelper.class,
					"UnshareConductorGroupActionHelper.UnshareFailureInMU.Message.text", editType.toString());
			if (!ShareConcurrencyHelper.attemptLockOnSourceMulticoreForUnshare(multicore, (ILogicDesign) m_design,
					failureMsg)) {
				return false;
			}
		}

		unshareMulticoreRetainingOriginalNames(multicore);

		// Set the new name only on the master multicore
		if (m_newName != null && multicore.getParent() == null) {
			multicore.setName("");
			multicore.setName(m_newName);
			if (shortDesc != null) {
				multicore.setShortDescription(shortDesc);
			}
		}

		// Happy happy happy, joy joy joy?
		return true;
	}

	private void unshareMulticoreRetainingOriginalNames(IMulticore multicore)
	{
		//collect the old names before unsharing multicore.
		Map<IUID, String> currentNames = new HashMap<>();
		for (IMulticore mc : multicore.getAllMulticoresInHierarchy()) {
			currentNames.put(mc.getUID(), mc.getName());
		}
		for (IConductor conductor : multicore.getAllConductorsInHierarchy(true)) {
			currentNames.put(conductor.getUID(), conductor.getName());
		}

		new MulticoreUnsharer(m_design, multicore).unshareMulticore();

		//set the names again after unsharing multicore
		for (Map.Entry<IUID, String> entry : currentNames.entrySet()) {
			INamedObject namedObject = UIDMgr.getObjectOfType(entry.getKey(), INamedObject.class);
			if (namedObject != null) {
				namedObject.setName(entry.getValue());
			}
		}
	}

	public void cleanup()
	{
		m_multicore = null;
		m_sharedMulticoreUid = null;
	}

	public boolean isNewSharedObject()
	{
		return false;
	}

	private boolean renameLocalMulticore(IMulticore multicore)
	{

		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view == null) {
			// If the view is null - most likely we're in a unit test - let not both with the rename options
			return true;
		}

		String title = ResourceMgr.getString(UnshareConductorGroupActionHelper.class,
				"UnshareConductorGroupActionHelper.Rename.Title");
		if (multicore instanceof IOverbraid) {
			title = ResourceMgr.getString(UnshareConductorGroupActionHelper.class,
					"UnshareConductorGroupActionHelper.Rename.Overbraid.Title");
		}

		RenameDialog dialog = new RenameDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				multicore, title,
				ResourceMgr.getString(UnshareConductorGroupActionHelper.class,
						"UnshareConductorGroupActionHelper.Rename.label"), false, m_design.getProject())
		{

			// Return null if valid; errmsg otherwise.  Check name is not empty and not a duplicate
			public String checkValidName(String newName, String oldName)
			{
				if (StringUtils.getTrimmed(newName) == null) {
					return "";
				}
				if (newName.length() > CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH) {
					return ResourceMgr.getString(UnshareConductorGroupActionHelper.class,
							"UnshareConductorGroupActionHelper.NameTooLong",
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
		};
		dialog.getOkButton().setEnabled(true);
		dialog.setVisible(true);
		m_newName = dialog.getNewName();
		if (m_newName == null) {
			return false;
		}
		IAttribute attribute = multicore.getAttribute(IAttributeTypes.SHORT_DESCRIPTION);
		shortDesc = attribute == null ? null : LanguageDictionaryHelper.getUntranslatedValueAsString(attribute);

		boolean sharedNameClash = false;
		String warningString = "UnshareConductorGroupActionHelper.NameExistsError.Message.text";
		ISharedMulticoreIterator condIter = m_design.getProject().getSharedConductorMgr().getSharedMulticores();
		while (condIter.hasNext()) {
			ISharedMulticore sharedCond = condIter.getNext();
			if (sharedCond.getName().equals(m_newName)) {
				sharedNameClash = true;
				warningString = "UnshareConductorGroupActionHelper.NameExistsError.SharedMessage.text";
			}
		}

		if (sharedNameClash || multicore.getNameMgr().nameExists(m_newName, multicore)) {
			if (!MessageHelper.showDoNotShowThisMessageAgainYesNoDialogue(
					CAFUtils.getInstance().getWindowMgr().getDialogFrame(), "",
					ResourceMgr.getString(UnshareConductorGroupActionHelper.class,
							"UnshareConductorGroupActionHelper.NameExistsError.Header.text"),
					ResourceMgr.getString(UnshareConductorGroupActionHelper.class,
							warningString, m_newName) +
							ResourceMgr.getString(UnshareConductorGroupActionHelper.class,
									"UnshareConductorGroupActionHelper.NameExistsError.Question.text"))) {
				return false;
			}
		}

		return true;
	}

	@Override @Nullable public IUID getSharedObjectUID()
	{
		return m_sharedMulticoreUid;
	}

	@Override public boolean isShareInto()
	{
		return false;
	}
}
