/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.caf.caplet.action.IActionEnum;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.cable.SingleLineEndType;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.cof.logical.shared.ISharedSingleLine;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.DiagramHelper;
import chs.utility.PortHelper;
import chs.utility.helpers.HighwayShareHelper;
import chs.utility.helpers.SingleLineHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to support sharing a Single Line
 */
public class ShareSingleLineActionHelper extends AbstractBaseShareSingleLineActionHelper
{

	private static final String CREATE_NEW =
			ResourceMgr.getString(ShareConductorActionHelper.class, "ShareHighwayActionHelper.ShareInto.CreateNew");
	private static final String USE_EXISTING =
			ResourceMgr.getString(ShareConductorActionHelper.class, "ShareHighwayActionHelper.ShareInto.UseExisting");
	private static final String CANCEL =
			ResourceMgr.getString(ShareConductorActionHelper.class, "ShareHighwayActionHelper.ShareInto.Cancel");

	protected ShareSingleLineActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram)
	{
		super(design, diagram, HighwayShareHelper.getInstance());
	}

	@NotNull @Override public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		IUIDObject target = operands.target;
		if ((target instanceof IHighwaySchematic highwaySchematic &&
				SingleLineHelper.isSingleLineSchematic(highwaySchematic))) {
			ISingleLine singleLine = (ISingleLine) highwaySchematic.getConnectivity();
			if (!attemptLockOnSourceSingleLineForShare(singleLine, m_design, getShareFailureInMUMsg())) {
				return IActionEnum.eCanceled;
			}
		}
		return super.setup(operands, dialogTitle, diagram);
	}

	@NotNull private String getShareFailureInMUMsg()
	{
		return ResourceMgr.getString(AbstractShareConductorGroupActionHelper.class,
				"ShareHighwayActionHelper.ShareFailureInMU.Message.text");
	}

	private boolean attemptLockOnSourceSingleLineForShare(@NotNull ISingleLine singleLine, @NotNull ILogicDesign design,
			@NotNull String failureMessage)
	{
		return ShareConcurrencyHelper.attemptLockOnSourceSingleLineForShare(singleLine, design, failureMessage);
	}

	@Override public boolean doEdit()
	{
		final ISharedSingleLine shareIntoObject = UIDMgr.getObjectOfType(shareInto, ISharedSingleLine.class);

		if (m_logicObject instanceof ISingleLine singleLine && shareIntoObject != null) {
			if (!areMulticorePartsCompatible(singleLine, shareIntoObject)) {
				ResourceBasedMessageContent resourceBasedMessageContent =
						new ResourceBasedMessageContent(ShareSingleLineActionHelper.class,
								"ShareSingleLineActionHelper.InvalidShareIntoSingleLine");
				resourceBasedMessageContent.setMessageParameters(singleLine.getName(), shareIntoObject.getName());
				Message.show(PromptSeverity.ERROR, resourceBasedMessageContent);
				return false;
			}
		}
		return super.doEdit();
	}

	private boolean areMulticorePartsCompatible(@NotNull ISingleLine singleLine,
			@NotNull ISharedSingleLine shareIntoObject)
	{
		IMulticoreIterator sourceMulticores = singleLine.getSingleLineMulticores();
		ISharedMulticoreIterator targetMulticores = shareIntoObject.getSharedSingleLineMulticores();

		if (sourceMulticores.hasNext() && targetMulticores.hasNext()) {
			IMulticore sourceMulticore = sourceMulticores.getNext();
			ISharedMulticore targetMulticore = targetMulticores.getNext();
			if (sourceMulticore.isPartAssigned()) {
				if (!targetMulticore.isPartAssigned()) {
					return false;
				}
				//return true only if the assigned parts are same
				return sourceMulticore.getLibraryRef() == targetMulticore.getLibraryRef();
			}
		}
		return true;
	}

	@Override protected int handleDuplicateName(@NotNull String name, @NotNull String objectType)
	{
		String[] options = {CREATE_NEW, USE_EXISTING, CANCEL};
		String title = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareSingleLineActionHelper.NameExistsError.Header.text");
		String heading = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareSingleLineActionHelper.NameExistsError.Message.text", name);
		String msg = ResourceMgr.getString(ShareConductorActionHelper.class,
				"ShareSingleLineActionHelper.NameExistsError.Question.text");
		return showDialogToHandleDuplicateName(title, heading, msg, options, CREATE_NEW);
	}

	@Override protected void reassignConnectivityForSchematic(IDiagramObject diagObj, ILogicObject logicObject)
	{
		IHighwaySchematic highwaySchematic = (IHighwaySchematic) diagObj;
		ISingleLine singleLine = CommonUtils.require(logicObject, ISingleLine.class);
		highwaySchematic.setConnectivity(singleLine);

		ISchemDiagram diagram = DiagramHelper.getDiagram(diagObj);
		if (diagram == null) {
			return;
		}
		PortHelper.updatePortGfx(highwaySchematic, diagram.getGrid().getGridSpacing());

		List<IUID> ends = SingleLineHelper.fetchConnectedDevices(highwaySchematic)
				.stream()
				.map(IDevice::getUID)
				.collect(Collectors.toList());

		for (IUID endUid: ends) {
			if (!singleLine.isConnectedEnd(endUid) && !singleLine.isSharedConnectedEnd(endUid)) {
				singleLine.addEnd(endUid, SingleLineEndType.DEVICE);
			}
		}
	}

	@Override protected boolean isBulkPromotion()
	{
		return false;
	}

	@Override protected boolean isChangeReportingRequired()
	{
		return true;
	}
}
