package chs.caplets.logic.actions.shared.autoshare;

import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.shared.IShareIntoActionHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedGeneralHighway;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import chs.utilities.ResourceMgr;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoShareIntoHighwayActionHelper extends AutoShareHighwayActionHelper implements IShareIntoActionHelper
{

	@Nullable private IUID mShareIntoHighway;
	private boolean misNewlyCreatedSharedObj;

	public AutoShareIntoHighwayActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext reporter, boolean isBulkPromotion, boolean isNewlyCreatedSharedObj)
	{
		super(design, diagram, reporter, isBulkPromotion);
		misNewlyCreatedSharedObj = isNewlyCreatedSharedObj;
	}

	@NotNull @Override protected IActionEnum doSetup()
	{
		if (mShareIntoHighway == null) {
			reportError(ResourceMgr.getString(AutoShareIntoHighwayActionHelper.class,
					"AutoShareIntoHighwayActionHelper.InvalidShareIntoConductor.msg"), getMessageContext());
			return IActionEnum.eCanceled;
		}
		shareInto = mShareIntoHighway;
		return IActionEnum.eCompleted;
	}

	@Override public boolean acceptSharedObject(@NotNull ISharedObject sharedObject)
	{
		if (shouldAcceptSharedObject(sharedObject)) {
			mShareIntoHighway = sharedObject.getUID();
			return true;
		}
		return false;
	}

	@Override protected boolean isNewlyCreatedSharedObject() {
		return misNewlyCreatedSharedObj;
	}

	protected boolean shouldAcceptSharedObject(@NotNull ISharedObject sharedObject)
	{
		return sharedObject instanceof ISharedGeneralHighway;
	}

	@NotNull @Override protected IMessageContext getMessageContext()
	{
		return IMessageContext.UndeterminedContext;
	}

	@Override public void cleanup()
	{
		super.cleanup();
		mShareIntoHighway = null;
	}
}
