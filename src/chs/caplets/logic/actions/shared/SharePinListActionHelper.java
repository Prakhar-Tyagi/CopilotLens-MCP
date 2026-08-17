/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2014-2025 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.actionreport.ActionChangeReportMgr;
import chs.caplets.logic.actions.actionreport.IActionChange;
import chs.caplets.logic.actions.actionreport.IMergeActionChange;
import chs.caplets.logic.actions.actionreport.IMergeActionChangeReporter;
import chs.caplets.logic.actions.actionreport.IMergeComparison;
import chs.caplets.logic.actions.actionreport.MergeActionChange;
import chs.caplets.logic.actions.ui.ShareIntoFacetConflictResolutionController;
import chs.caplets.logic.actions.ui.ShareIntoFacetConflictResolutionDialog;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedObjectModificationObserver;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IAttributePropertyProvider;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.utils.IPinProxy;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.IPinListShareContext;
import chs.utility.helpers.IPinListShareHelper;
import chs.utility.helpers.IShareActionChange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SharePinListActionHelper extends AbstractSharePinListActionHelper
{

	private Model m_model;
	private IFIB m_fib;
	private EditSharedPinlistDialog m_dialog;
	/**
	 * A handle to our dynamic graphics service for convenience.
	 */
	private IDynamicGfxService m_dynamics;

	public SharePinListActionHelper(IFIB fib, Model model, boolean fromSymbol)
	{
		super(model.getDesign().getProject(), model.getDesign(), model.getDiagram(), fromSymbol);
		m_model = model;
		m_fib = fib;
		m_dynamics = m_model.getDynamicGfxService();
	}

	protected IActionEnum postSetup(@Nullable String dialogTitle)
	{
		Frame owner = m_fib.getWindowMgr().getDialogFrame();
		return showDialog(owner, dialogTitle);
	}

	protected IActionEnum showDialog(Frame owner, @Nullable String dialogTitle)
	{
		// Put up the EditSharedPinlistDialog in share mode

		m_dialog =
				getEditPinListDialog(owner, dialogTitle);
		m_dialog.setHelpID(SharePinListActionHelper.class.getName());
		m_dialog.setVisible(true);
		if (!m_dialog.isCancelled()) {
			return IActionEnum.eCompleted;
		}
		return IActionEnum.eCanceled;
	}

	@NotNull private EditSharedPinlistDialog getEditPinListDialog(Frame owner,
			@Nullable String dialogTitle)
	{
		if (cablePinList instanceof IFunction) {
			return new EditSharedFunctionDialog(owner, dialogTitle, null, cablePinList, m_pinList, m_model,
					m_fromSymbol);
		}
		return new EditSharedPinlistDialog(owner, dialogTitle, null, cablePinList, m_pinList, m_model, m_fromSymbol);
	}

	@Nullable protected IProjectPreferenceMgr getProjectPreferences()
	{
		return CAFUtils.getInstance().getCurrentProjectPreferences();
	}

	@Nullable protected IPinListShareContextProvider getPinlistShareContextProvider()
	{
		return m_dialog;
	}

	protected void reportSymbolNotAvailable()
	{
		String resourceKey;
		if (isInstanceOfFunction()) {
			resourceKey = "ShareAction.NoSymDefDialogFunction";
		}
		else {
			resourceKey = "ShareAction.NoSymDefDialog";
		}
		Message.show(PromptSeverity.INFORMATION, ShareAction.class, resourceKey);
	}

	public boolean isInstanceOfFunction()
	{
		return cablePinList instanceof IFunction;
	}

	protected void reportSymbolOutOfDate()
	{
		MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(ShareAction.class, "ShareAction.NoSymDefDialog.Heading.text"),
				ResourceMgr.getString(ShareAction.class, "ShareAction.NotUptoDate.Message.text"));
	}

	protected void reportDuplicatePins()
	{
		String resourceKey = "ShareAction.DuplicatePins";
		Message.show(PromptSeverity.ERROR, ShareAction.class, resourceKey);
	}

	protected void reportCompositeSymbolOutOfDate()
	{
		MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(ShareAction.class, "ShareAction.NoSymDefDialog.Heading.text"),
				ResourceMgr.getString(ShareAction.class, "ShareAction.OutOfDateChildren.Message.text"));
	}

	protected void reportError(@NotNull IPinListShareHelper.ErrorCode errorCode)
	{
		MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(ShareAction.class, "ShareAction.CannotShare.Heading.text"),
				errorCode.getErrorMessage());
	}

	@Override public void cleanup()
	{
		super.cleanup();
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
	}

	@NotNull protected Runnable getConflictResolver(@NotNull ISharedObjectModificationObserver observer,
			@NotNull IPinListShareContext pinListShareContext)
	{
		ShareIntoFacetConflictResolutionController controller =
				new ShareIntoFacetConflictResolutionController(cablePinList);
		ShareIntoFacetConflictResolutionDialog resolutionDialog =
				new ShareIntoFacetConflictResolutionDialog(observer, controller, controller);
		return () -> {
			ISharedPinList sharedPinList = pinListShareContext.getSharedPinList();
			if (sharedPinList != null) {
				IConnectivity connectivity = m_design.getConnectivity();
				if (connectivity != null) {
					IPinList newConnectivity = connectivity.findSharedPinList(sharedPinList);
					if (newConnectivity != null) {
						Map<IUID, IUID> srcToTgtPinMap = new HashMap<>();
						for (Map.Entry<IAbstractPin, IPinProxy> entry : pinListShareContext.getInstanceToSharedMap()
								.entrySet()) {
							IAbstractPin tgtPin = entry.getValue().getCablePin();
							if (tgtPin != null) {
								srcToTgtPinMap.put(entry.getKey().getUID(), tgtPin.getUID());
							}
						}
						controller.setupPinMap(srcToTgtPinMap);
						resolutionDialog.process(newConnectivity);
					}
				}
			}
		};
	}

	@NotNull @Override
	protected IShareActionChangeReporter getShareActionChangeReporter(@NotNull IPinListShareContext pinListShareContext)
	{
		return new ShareActionChangeReporter(pinListShareContext);
	}

	public static class ShareActionChangeReporter implements IShareActionChangeReporter
	{

		private boolean isShareInto;
		private IMergeActionChangeReporter mergeActionReporter;
		private IMergeComparison<IMergeActionChange, IAttributePropertyProvider> comparison = null;
		private IMergeComparison<IMergeActionChange, IAttributePropertyProvider> matecomparator = null;
		@NotNull private IPinListShareContext pinListShareContext;

		public ShareActionChangeReporter(@NotNull IPinListShareContext pinListShareContext)
		{
			this.pinListShareContext = pinListShareContext;
			isShareInto = pinListShareContext.getSharedPinList() != null;
			mergeActionReporter = ActionChangeReportMgr.getInstance().createMergeActionChangeReporter();

			if (isShareInto) {
				comparison = mergeActionReporter.createComparison();
				if (pinListShareContext.getCablePinListToShare() != null) {
					IPinList cablePinListToShare = pinListShareContext.getCablePinListToShare();
					comparison.setInitialStateOfSourceObject(cablePinListToShare);
					if (cablePinListToShare instanceof IGenericInlineConnector) {
						matecomparator = mergeActionReporter.createComparison();
						matecomparator.setInitialStateOfSourceObject(
								((IGenericInlineConnector) cablePinListToShare).getMatedInlines().iterator().next());
					}
					Map<IAbstractPin, IPinProxy> instanceToSharedMap = pinListShareContext.getInstanceToSharedMap();
					for (IAbstractPin p : instanceToSharedMap.keySet()) {
						comparison.addObjectMapping(p, instanceToSharedMap.get(p));
						if (matecomparator != null) {
							matecomparator.addObjectMapping(p, instanceToSharedMap.get(p));
						}
					}
				}
				ISharedPinList sharedPinList = pinListShareContext.getSharedPinList();
				comparison.setInitialStateOfTargetObject(sharedPinList);
				if (sharedPinList instanceof ISharedConnector &&
						((ISharedConnector) sharedPinList).isInlineHalf()) {
					Set<ISharedConnector> mates = ((ISharedConnector) sharedPinList).getMates();
					List<ISharedConnector> inlineList =
							mates.stream().filter(conn -> conn.isInlineHalf()).collect(Collectors.toList());
					if (matecomparator != null && !inlineList.isEmpty()) {
						matecomparator.setInitialStateOfTargetObject(inlineList.get(0));
					}
				}
			}
		}

		@Override public void reportChanges()
		{
			if (isShareInto && comparison != null) {
				ILogicDesign design = pinListShareContext.getDesign();
				IPinList pinList = design == null ? null :
						design.getSharedUsageMgr().getPinList(pinListShareContext.getSharedPinList());
				if (pinList != null) {
					comparison.setTransformedState(pinList);
					if (pinList instanceof IGenericInlineConnector && matecomparator != null) {
						matecomparator.setTransformedState(
								((IGenericInlineConnector) pinList).getMatedInlines().iterator().next());
					}
					mergeActionReporter.reportChanges();
				}
			}
		}

		@Override public void notify(@NotNull IShareActionChange change)
		{
			if (comparison != null) {
				IMergeActionChange changeToBeReported = null;
				switch (change.getReason()) {
					case DEVICE_CONNECTOR_RENAMED_DUE_TO_PART_MISMATCH:
						changeToBeReported = getRenameFeedback(change,
								ResourceMgr.getString(SharePinListActionHelper.class,
										"SharePinListActionHelper.DeviceConnector.renamedDueToPartMismatch.text"));
						break;
					case DEVICE_CONNECTOR_RENAMED_DUE_TO_DEVICE_PIN_MAPPED_TO_MULTIPLE_CONNECTORS:
						changeToBeReported = getRenameFeedback(change,
								ResourceMgr.getString(SharePinListActionHelper.class,
										"SharePinListActionHelper.DeviceConnector.renamedDueToDevicePinMappedToMultipleConnectors.text"));
						break;
					case DEVICE_CONNECTOR_RENAMED_DUE_TO_CAVITY_MISMATCH:
						changeToBeReported = getRenameFeedback(change,
								ResourceMgr.getString(SharePinListActionHelper.class,
										"SharePinListActionHelper.DeviceConnector.renamedDueToCavityMismatch.text"));
				}
				reportMergeChange(changeToBeReported);
			}
		}

		protected void reportMergeChange(IMergeActionChange changeToBeReported)
		{
			if (comparison != null) {
				comparison.addChange(changeToBeReported);
			}
		}

		@NotNull private MergeActionChange getRenameFeedback(@NotNull IShareActionChange change, String detailMessage)
		{
			StringBuilder sourceName = new StringBuilder();
			String parentName = comparison.getSourceObjectName();
			if (parentName != null) {
				sourceName.append(parentName).append(StringUtils.COLON);
			}
			sourceName.append(change.getOldValue());
			return new MergeActionChange(IAttributeTypes.NAME, change.getOldValue(), change.getOldValue(),
					change.getNewValue(), IActionChange.ComparisonField.Attribute, sourceName.toString(),
					COFTypeEnum.DeviceConnector.toString(), detailMessage);
		}
	}
}