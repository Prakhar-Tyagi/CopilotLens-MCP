package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.IShareIntoActionHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoShareIntoPinListActionHelper extends AutoSharePinListActionHelper implements IShareIntoActionHelper
{

	@Nullable protected ISharedPinList mSharedPinlist;

	protected AutoShareIntoPinListActionHelper(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, @NotNull IMessageReporterWithContext reporter,
			@NotNull AutoShareParams params)
	{
		super(project, design, diagram, reporter, params);
	}

	@Nullable @Override protected AutoSharePinlistView createAutoSharePinlistView()
	{
		if (mSharedPinlist != null) {
			return new AutoShareIntoPinlistView(mSharedPinlist, cablePinList, m_pinList, mLogicDesign,
					mMessageReporter, isBulkPromotion(), m_params);
		}
		return null;
	}

	@Override public boolean acceptSharedObject(@NotNull ISharedObject sharedObject)
	{
		if (sharedObject instanceof ISharedPinList) {
			if (sharedObject instanceof ISharedConnector &&
					((ISharedPinList) sharedObject).getType() == PinListTypeEnum.TypeInlinePlug) {
				mSharedPinlist = ((ISharedConnector) sharedObject).getMates().iterator().next();
			}
			else {
				mSharedPinlist = (ISharedPinList) sharedObject;
			}
			return true;
		}
		return false;
	}

	@Override public void cleanup()
	{
		super.cleanup();
		mSharedPinlist = null;
	}
}
