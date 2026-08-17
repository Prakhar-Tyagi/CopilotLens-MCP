/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedObject;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

public class ShareConductorActionHelper extends AbstractShareConductorActionHelper
{

	private static final String CREATE_NEW = ResourceMgr.getString(ShareConductorActionHelper.class,
			"ShareConductorActionHelper.ShareInto.CreateNew");
	private static final String USE_EXISTING = ResourceMgr.getString(ShareConductorActionHelper.class,
			"ShareConductorActionHelper.ShareInto.UseExisting");
	private static final String CANCEL = ResourceMgr.getString(ShareConductorActionHelper.class,
			"ShareConductorActionHelper.ShareInto.Cancel");

	public ShareConductorActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram)
	{
		super(design, diagram);
	}

	protected int handleDuplicateName(@NotNull String name, @NotNull String objectType)
	{
		String[] options = {CREATE_NEW, USE_EXISTING, CANCEL};
		String title = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareConductorActionHelper.NameExistsError.Header.text");
		String heading = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareConductorActionHelper.NameExistsError.Message.text", objectType, name);
		String msg = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareConductorActionHelper.NameExistsError.Question.text", objectType);
		return showDialogToHandleDuplicateName(title, heading, msg, options, CREATE_NEW);
	}

	public void setShareInto(@NotNull ISharedConductor shareInto)
	{
		this.shareInto = shareInto.getUID();
	}

	@Override protected boolean isBulkPromotion()
	{
		return false;
	}

	@Override protected boolean isChangeReportingRequired()
	{
		return true;
	}

	protected void reportSharedObjectMgrLocked()
	{
		LogicActionMessageHelper.warnLocked(m_sharedObjectMgr);
	}

	protected void reportSharedObjectDeleted(@NotNull ISharedObject shareIntoObj)
	{
		MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(AbstractBaseShareConductorActionHelper.class,
						"BaseShareActionHelper.SharedObjectDeleted.Heading"),
				ResourceMgr.getString(AbstractBaseShareConductorActionHelper.class,
						"BaseShareActionHelper.SharedObjectDeleted.Text", shareIntoObj.getName()));
	}
}
