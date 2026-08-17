package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.AbstractShareConductorGroupActionHelper;
import chs.caplets.logic.actions.shared.IShareIntoActionHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class AutoShareIntoConductorGroupActionHelper extends AutoShareConductorGroupActionHelper
		implements IShareIntoActionHelper
{

	@NotNull protected final Map<ILogicObject, ISharedObject> m_multicoreHierarchyMap = new HashMap<>();
	private boolean m_isNewlyCreatedSharedObj;


	public AutoShareIntoConductorGroupActionHelper(@NotNull ILogicDesign design,
			@NotNull IMessageReporterWithContext messageReporter,
			@NotNull Map<ILogicObject, ISharedObject> multicoreHierarchyMap, boolean isBulkPromotion, boolean isNewlyCreatedSharedObj)
	{
		super(design, messageReporter, isBulkPromotion);
		m_multicoreHierarchyMap.putAll(multicoreHierarchyMap);
		m_isNewlyCreatedSharedObj = isNewlyCreatedSharedObj;

	}

	@Nullable @Override
	protected AutoShareMulticoreContextProvider createAutoShareMulticoreContextProvider(@NotNull IMulticore multicore)
	{
		if (m_sharedMulticore == null) {
			return null;
		}
		return new AutoShareIntoMulticoreContextProvider(multicore, m_design, m_sharedMulticore,
				m_multicoreHierarchyMap, mMessageReporter);
	}

	@Override
	protected boolean attemptLockOnSourceMulticoreForShare(@NotNull IMulticore multicore,
			@NotNull ILogicDesign logicDesign, @NotNull String failureMsg)
	{
		return ShareConcurrencyHelper.attemptLockOnSourceMulticoreForShareInto(multicore, logicDesign, failureMsg);
	}

	@Override
	protected void reportSharedObjectDeleted(@NotNull ISharedMulticore sharedMulticore)
	{
		final String message = ResourceMgr.getString(AbstractShareConductorGroupActionHelper.class,
				"BaseShareActionHelper.SharedObjectDeleted.Text", sharedMulticore.getName());
		sendMessage(PromptSeverity.ERROR, message, IMessageContext.EmptyContext);
	}

	@Override public boolean acceptSharedObject(@NotNull ISharedObject sharedObject)
	{
		if (sharedObject instanceof ISharedMulticore) {
			m_sharedMulticore = sharedObject.getUID();
			return true;
		}
		return false;
	}

	@Override protected boolean isNewlyCreatedSharedObject() {
		return m_isNewlyCreatedSharedObj;
	}

	@Override public void cleanup()
	{
		super.cleanup();
		m_multicoreHierarchyMap.clear();
	}
}
