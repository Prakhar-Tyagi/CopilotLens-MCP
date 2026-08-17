package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: Aug 2, 2009 Time: 10:52:47 AM
 */
public class ShareHighwayActionHelper extends AbstractShareHighwayActionHelper
{

	private static final String CREATE_NEW = ResourceMgr.getString(ShareConductorActionHelper.class,
			"ShareHighwayActionHelper.ShareInto.CreateNew");
	private static final String USE_EXISTING = ResourceMgr.getString(ShareConductorActionHelper.class,
			"ShareHighwayActionHelper.ShareInto.UseExisting");
	private static final String CANCEL = ResourceMgr.getString(ShareConductorActionHelper.class,
			"ShareHighwayActionHelper.ShareInto.Cancel");

	public ShareHighwayActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram)
	{
		super(design, diagram);
	}

	protected int handleDuplicateName(@NotNull String name, @NotNull String objectType)
	{
		String[] options = {CREATE_NEW, USE_EXISTING, CANCEL};
		String title = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareHighwayActionHelper.NameExistsError.Header.text");
		String heading = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareHighwayActionHelper.NameExistsError.Message.text", name);
		String msg = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareHighwayActionHelper.NameExistsError.Question.text");
		return showDialogToHandleDuplicateName(title, heading, msg, options, CREATE_NEW);
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
