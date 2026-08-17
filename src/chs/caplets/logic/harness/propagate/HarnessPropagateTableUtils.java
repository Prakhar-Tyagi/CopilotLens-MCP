/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.PropagateHarnessAction;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.DesignUtils;
import chs.common.IUID;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.logic.LOGIC_SHARED_TYPE;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.autotagharness.IHarnessUpdateReport;
import chs.utility.logic.autotagharness.SharedInfoCache;
import com.mentor.capital.javafx.table.Table;
import com.mentor.capital.javafx.table.impl.CapitalTableColumn;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utils class for testing the feature
 */
public class HarnessPropagateTableUtils
{

	private HarnessPropagateTableUtils()
	{
	}

	public static void updateStatusMessages(@NotNull List<IHarnessPropagateStatusMessage> messages,
			@NotNull IHarnessUpdateReport updateReport)
	{
		String harness = updateReport.getHarness();
		ILogicDesign design = updateReport.getDesign();

		for (ILogicObject object : updateReport.getEditedUnsharedObjects()) {
			IHarnessPropagateStatusMessageGroup group = new HarnessPropagateStatusMessageGroup();
			HarnessPropagateStatusMessage updatedMessage =
					new HarnessPropagateStatusMessage(HarnessPropagateMessageType.UPDATED, object,
							updateReport.getPreviousHarness(object), harness, design,
							new HarnessPropagateStatusMessageGroup());
			updatedMessage.setupPropagationStatus(false);
			messages.add(updatedMessage);
		}

		for (ILogicObject failedLogicObject : updateReport.getFailedLogicObjects()) {
			String failedMessage = updateReport.getFailedMessage(failedLogicObject);
			String message = failedMessage != null ? failedMessage : ResourceMgr.getString(HarnessPropagateMessageType.class, "HarnessPropagateMessageType.FailedToUpdate.message");
			HarnessPropagateStatusMessage failedLogicMessage =
					new HarnessPropagateStatusMessage(HarnessPropagateMessageType.FAILURE, failedLogicObject,
							updateReport.getPreviousHarness(failedLogicObject), harness, design,
							new HarnessPropagateStatusMessageGroup());
			failedLogicMessage.setMessage(message);
			failedLogicMessage.setupPropagationStatus(false);
			messages.add(failedLogicMessage);
		}

		Collections.sort(messages, new HarnessStatusComparator());
	}

	public static void updateStatusMessagesForShared(@NotNull List<IHarnessPropagateStatusMessage> messages,
			@NotNull SetMap<ILogicDesign, ISharedObject> designVsSharedObj,
			@NotNull Set<ISharedObject> failedSharedObjects, @NotNull Set<ISharedObject> updatedSharedObjects,
			@NotNull SharedInfoCache sharedInfoCache, @NotNull String harness)
	{

		for (ISharedObject sharedObject : updatedSharedObjects) {
			Set<ILogicDesign> designs = new HashSet<>();
			for (Map.Entry<ILogicDesign, Set<ISharedObject>> entry : designVsSharedObj.entrySet()) {
				ILogicDesign design = entry.getKey();
				Set<ISharedObject> sharedObjects = entry.getValue();
				for (ISharedObject object : sharedObjects) {
					if (object == sharedObject) {
						designs.add(design);
					}
				}
			}
			List<String> designNameList = designs.stream().map(o -> o.getFullName()).collect(Collectors.toList());
			Collections.sort(designNameList);
			String designString = StringUtils.convertCollectionToString(designNameList, ",");
			HarnessPropagateStatusMessage sharedMessage =
					new HarnessPropagateStatusMessage(HarnessPropagateMessageType.UPDATED, sharedObject,
							LOGIC_SHARED_TYPE.getSharedObjectType(sharedObject), sharedInfoCache.getHarness(sharedObject),
							harness, designString, new HarnessPropagateStatusMessageGroup());
			sharedMessage.setupPropagationStatus(false);
			messages.add(0, sharedMessage);
		}

		failedSharedObjects.addAll(sharedInfoCache.getFailedSharedObjects());

		for (ISharedObject sharedObject : failedSharedObjects) {
			String failedMessage = sharedInfoCache.getSharedMessage(sharedObject);
			String message = failedMessage != null ? failedMessage : ResourceMgr.getString(HarnessPropagateMessageType.class, "HarnessPropagateMessageType.FailedToUpdate.message");
			HarnessPropagateStatusMessage failedSharedMessage =
					new HarnessPropagateStatusMessage(HarnessPropagateMessageType.FAILURE, sharedObject,
							LOGIC_SHARED_TYPE.getSharedObjectType(sharedObject), sharedInfoCache.getHarness(sharedObject),
							harness, "-", new HarnessPropagateStatusMessageGroup());
			failedSharedMessage.setMessage(message);
			failedSharedMessage.setupPropagationStatus(false);
			messages.add(0, failedSharedMessage);
		}
	}

	public static void showMessages(List<IHarnessPropagateStatusMessage> messages, @NotNull IUID designUid)
	{
		String propagate_harness = IAutoPropagateHarnessController.propagate_harness_tab;
		if (messages.isEmpty() && !CAFUtils.getInstance().getOutputWindow().paneExists(propagate_harness)) {
			return;
		}
		CAFUtils.getInstance().getOutputWindow().removePane(propagate_harness);
		HarnessUpdateStatusMessageTableModel tableModel = new HarnessUpdateStatusMessageTableModel();
		Collections.sort(messages, new HarnessStatusComparator());
		tableModel.addRows(messages);
		HarnessPropagateTableWindow statusWindow = new HarnessPropagateTableWindow(propagate_harness, designUid, tableModel);
		Platform.runLater(() -> {
			Table<IHarnessPropagateStatusMessage> table = statusWindow.getTable();
			if (table != null) {
				table.columns().forEach(columnInfo -> {
					if (HarnessPropagateColumn.Severity.getName().equals(columnInfo.getName()) ||
							HarnessPropagateColumn.Propagate.getName().equals(columnInfo.getName())) {
						CapitalTableColumn<IHarnessPropagateStatusMessage> tableColumn = columnInfo.getTableColumn();
						if (tableColumn != null) {
							final int fixedSize = 65;
							tableColumn.setMinWidth(fixedSize);
							tableColumn.setMaxWidth(fixedSize);
						}
					}
				});
			}
		});
		statusWindow.addData(messages);
	}

	public static void updateStatusMessagesForNonSelectedRows(@NotNull List<IHarnessPropagateStatusMessage> messages,
			@NotNull ILogicDesign design, @NotNull Set<ILogicObject> logicObjectsToSkip,
			@NotNull String harness, @NotNull Map<ISharedObject, ILogicObject> sharedObjects,
			@NotNull Map<ILogicObject, IHarnessPropagateStatusMessage> logicMessages)
	{
		for (ILogicObject logicObject : logicObjectsToSkip) {
			String oldHarness = StringUtils.nonNull(logicObject.getHarness());
			if (!StringUtils.equalsTrimmed(harness, oldHarness)) {
				HarnessPropagateMessageType messageType = LogicUtils.hasMultipleHarness(logicObject) ?
						HarnessPropagateMessageType.READY_TO_UPDATE_VARIANT_CONN : HarnessPropagateMessageType.READY_TO_UPDATE;
				HarnessPropagateStatusMessage logicStatusMessage = new HarnessPropagateStatusMessage(messageType,
						logicObject, oldHarness, harness, design, new HarnessPropagateStatusMessageGroup());
				if (messageType == HarnessPropagateMessageType.READY_TO_UPDATE_VARIANT_CONN) {
					logicStatusMessage.setupPropagationStatus(false);
				}
				if (logicObject.isShared()) {
					logicStatusMessage.setMessage(ResourceMgr.getString(HarnessPropagateMessageType.class,
							"HarnessPropagateMessageType.ReadyToUpdateShared.message"));
					sharedObjects.put(logicObject.getSharedObject(), logicObject);
				}
				logicMessages.put(logicObject, logicStatusMessage);
				messages.add(logicStatusMessage);
			}
		}
	}

	public static void updateSkippedSharedMessages(@NotNull List<IHarnessPropagateStatusMessage> messages,
			@NotNull Map<ISharedObject, ILogicObject> sharedObjects, @NotNull ILogicDesign design,
			@NotNull String harness, @NotNull Map<ILogicObject, IHarnessPropagateStatusMessage> logicMessages)
	{
		for (Map.Entry<ISharedObject, ILogicObject> entry : sharedObjects.entrySet()) {
			ILogicObject logicObject = entry.getValue();
			ISharedObject sharedObject = entry.getKey();
			IHarnessPropagateStatusMessage logicMessage = logicMessages.get(logicObject);
			IHarnessPropagateStatusMessageGroup messageGroup =
					logicMessage != null ? logicMessage.getGroup() : new HarnessPropagateStatusMessageGroup();
			HarnessPropagateStatusMessage sharedStatusMessage =
					new HarnessPropagateStatusMessage(HarnessPropagateMessageType.SHARED_READY_TO_UPDATE,
							logicObject, StringUtils.nonNull(sharedObject.getHarness()), harness, design, messageGroup);
			sharedStatusMessage.setupPropagationStatus(false);
			messages.add(sharedStatusMessage);
		}
	}

	public static void showPropagateErrorMessage(@Nullable IUID designUid)
	{
		ILogicDesign design = DesignUtils.getDesign(designUid, ILogicDesign.class);
		if (design == null) {
			return;
		}
		String outputMessage =
				ResourceMgr.getString(PropagateHarnessAction.class, "PropagateHarnessAction.Failure.text", design.getName());
		CAFUtils.getInstance().sendApplicationMessage(outputMessage);
	}
}
