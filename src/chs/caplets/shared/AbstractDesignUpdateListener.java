package chs.caplets.shared;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.logical.concurrency.AbstractLogicConcurrencyEventListener;
import chs.cofUtils.logical.concurrency.ILogicConcurrencyActionInfo;
import chs.cofUtils.logical.concurrency.ILogicConcurrencyEvent;
import chs.cofUtils.logical.concurrency.LogicConcurrencyEventType;
import chs.common.DesignUtils;
import chs.common.IUID;
import chs.system.UIDMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractDesignUpdateListener extends AbstractLogicConcurrencyEventListener
{

	@NotNull private static final List<LogicConcurrencyEventType> design_data_update_events =
			Arrays.asList(LogicConcurrencyEventType.SESSION_CONNECTIVITY_REFRESHED,
					LogicConcurrencyEventType.SESSION_DIAGRAM_REFRESHED);
	@NotNull private IUID mDesign;
	@NotNull protected ICapletController mController;
	private Long connectivityModifiedTime = null;
	private Map<IUID, Long> m_fullyLoadedDiagrams = null;
	private boolean mWasDesignDataUpdated = false;
	private boolean mDesignDataUpdateWithingAction = false;
	private boolean mDesignDataUpdateHandlingPending = false;

	AbstractDesignUpdateListener(@NotNull ILogicDesign design, @NotNull ICapletController capletController)
	{
		mDesign = design.getUID();
		mController = capletController;
	}

	@Override public void actionStarted(@NotNull ILogicConcurrencyActionInfo actionId)
	{
		mDesignDataUpdateWithingAction = true;
		mDesignDataUpdateHandlingPending = false;
	}

	@Override public void actionEnded(@NotNull ILogicConcurrencyActionInfo actionId)
	{
		if (mDesignDataUpdateHandlingPending) {
			handleDesignDataUpdateFromRemote();
			mDesignDataUpdateHandlingPending = false;
		}
		mDesignDataUpdateWithingAction = false;
	}

	@Override public void designUpdateFromRemoteStarted(@NotNull ILogicDesign logicDesign)
	{
		mWasDesignDataUpdated = false;
		ILogicDesign design = getDesign();
		if (design != null) {
			m_fullyLoadedDiagrams = new HashMap<>();
			for (ISchemDiagram dia : design.getDiagrams()) {
				if (dia.isFullyLoaded()) {
					m_fullyLoadedDiagrams.put(dia.getUID(), dia.getTimeModified());
				}
			}
			//use loaded connectivity only. we would be coming here while starting
			//the design parse in case of fresh load also. and this would invoke
			//recursive request and might cause intermittent issues.
			IConnectivity connectivity = design.getLoadedConnectivity();
			if (connectivity != null) {
				connectivityModifiedTime = connectivity.getTimeModified();
			}
		}
	}

	@Override public void designUpdateFromRemoteEnded(@NotNull ILogicDesign logicDesign)
	{
		if (mWasDesignDataUpdated) {
			if (mDesignDataUpdateWithingAction && shouldHandleUpdateFromRemoteOncePerAction()) {
				mDesignDataUpdateHandlingPending = true;
			}
			else {
				handleDesignDataUpdateFromRemote();
			}
			mWasDesignDataUpdated = false;
		}
		m_fullyLoadedDiagrams = null;
		connectivityModifiedTime = null;
	}

	protected abstract boolean shouldHandleUpdateFromRemoteOncePerAction();

	protected abstract void handleDesignDataUpdateFromRemote();

	@Override protected void doProcessEvent(@NotNull ILogicConcurrencyEvent event)
	{
		LogicConcurrencyEventType eventType = event.getEventType();
		if (design_data_update_events.contains(eventType)) {
			if (eventType.equals(LogicConcurrencyEventType.SESSION_DIAGRAM_REFRESHED)) {
				if (m_fullyLoadedDiagrams != null) {
					//for diagram refresh we nedd to consider only those which were already fully loaded.
					for (IUID obj : event.getObjects()) {
						ISchemDiagram diagram = UIDMgr.getObjectOfType(obj, ISchemDiagram.class);
						if (diagram != null) {
							Long prevTimeStamp = m_fullyLoadedDiagrams.get(obj);
							if (prevTimeStamp != null && (diagram.getTimeModified() > prevTimeStamp)) {
								mWasDesignDataUpdated = true;
							}
						}
					}
				}
			}
			else if (eventType.equals(LogicConcurrencyEventType.SESSION_CONNECTIVITY_REFRESHED)) {
				//unfortunately we need to do this check because if we load a diagram it brings the
				//connectivity also. so to detect a real refresh of connectivity we need to do this.
				if (connectivityModifiedTime != null) {
					ILogicDesign design = getDesign();
					if (design != null) {
						//use loaded connectivity only. we might come here while finishing
						//the design parse in case of refresh without loaded connectivity also.
						//and this would invoke recursive request and might cause intermittent issues.
						IConnectivity connectivity = design.getLoadedConnectivity();
						if (connectivity != null) {
							long currentConnectivityModifiedTime = connectivity.getTimeModified();
							if (currentConnectivityModifiedTime > connectivityModifiedTime) {
								mWasDesignDataUpdated = true;
							}
						}
					}
				}
			}
			else {
				mWasDesignDataUpdated = true;
			}
		}
	}

	@NotNull @Override public Collection<LogicConcurrencyEventType> getInterestedEvents()
	{
		return Collections.unmodifiableCollection(design_data_update_events);
	}

	protected final boolean isContextMatching(@Nullable ILogicDesign context)
	{
		return (context != null) && (mDesign == context.getUID());
	}

	@Nullable protected final ILogicDesign getDesign()
	{
		return DesignUtils.getLoadedDesign(mDesign, ILogicDesign.class);
	}
}