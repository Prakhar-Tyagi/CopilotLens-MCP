package chs.caplets.logic.actions.shared.autoshare;

import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.shared.IShareIntoActionHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoShareIntoConductorActionHelper extends AutoShareConductorActionHelper implements IShareIntoActionHelper
{

	@Nullable private IUID mShareIntoConductor;
	private boolean m_isNewlyCreatedSharedObj;

	public AutoShareIntoConductorActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext reporter, boolean isBulkPromotion, boolean isNewlyCreatedSharedObj)
	{
		super(design, diagram, reporter, isBulkPromotion);
		m_isNewlyCreatedSharedObj = isNewlyCreatedSharedObj;
	}

	@NotNull @Override protected IActionEnum doSetup()
	{
		if (mShareIntoConductor == null) {
			reportError(ResourceMgr.getString(AutoShareIntoConductorActionHelper.class,
					"AutoShareIntoConductorActionHelper.InvalidShareIntoConductor.msg"), getMessageContext());
			return IActionEnum.eCanceled;
		}
		if (isAnotherRevisionAlreadyUsed(mShareIntoConductor)) {
			final String usedRevisionMsg = ResourceMgr.getString(AutoShareIntoConductorActionHelper.class,
					"AutoShareIntoConductorActionHelper.AnotherRevisionUsed.msg",
					StringUtils.toLowerCase(getLogicObjectDisplayType()));
			reportError(usedRevisionMsg, getMessageContext());
			return IActionEnum.eCanceled;
		}
		shareInto = mShareIntoConductor;
		return IActionEnum.eCompleted;
	}

	private boolean isAnotherRevisionAlreadyUsed(@NotNull IUID sharedConductorUID)
	{
		final ISharedConductor sharedConductor = UIDMgr.getObjectOfType(sharedConductorUID, ISharedConductor.class);
		if (sharedConductor != null) {
			return SharedObjectRevisionHelper.getOtherRevisionUsedInDesign(sharedConductor, m_design) != null;
		}
		return false;
	}

	@Override public boolean acceptSharedObject(@NotNull ISharedObject sharedObject)
	{
		if (sharedObject instanceof ISharedConductor) {
			mShareIntoConductor = sharedObject.getUID();
			return true;
		}
		return false;
	}

	@Override protected boolean isNewlyCreatedSharedObject() {
		return m_isNewlyCreatedSharedObj;
	}

	@NotNull @Override protected IMessageContext getMessageContext()
	{
		return IMessageContext.UndeterminedContext;
	}

	@Override public void cleanup()
	{
		super.cleanup();
		mShareIntoConductor = null;
	}
}
