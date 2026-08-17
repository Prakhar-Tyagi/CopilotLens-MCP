/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022-2024 Siemens
 */

package chs.caplets.logic.commands;

import chs.caplets.logic.actions.UpdateDictionaryActionHelper;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IFunctionBaseConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.cofUtils.cmd.CommandHelper;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.properties.PropTextScrubber;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Batch action to update function signals,messages to network signals,messages using dictionary name and revision
 */
public class BatchUpdateFromDictionaryCmd extends AbstractBatchUpdateICDCmd<IFunctionLogicDesign>
{

	private UpdateDictionaryActionHelper m_helper;

	public BatchUpdateFromDictionaryCmd(CommandHelper commandHelper, Set<IFunctionLogicDesign> designs)
	{
		super(commandHelper, designs, false);
		m_helper = new UpdateDictionaryActionHelper(outputWindow);
	}

	public boolean prepare()
	{
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(BatchUpdateFromDictionaryCmd.class,
						"BatchUpdateFromDictionaryCmd.CannotUndo");
		final Choice saveAndContinue =
				new Choice(BatchUpdateFromDictionaryCmd.class, "BatchUpdateFromDictionaryCmd.CannotUndo.choice.saveAndContinue");
		final Choice cancel = new Choice(BatchUpdateFromDictionaryCmd.class, "BatchUpdateFromDictionaryCmd.CannotUndo.choice.cancel");
		Choice selectedChoice = Question.show(content, saveAndContinue, cancel);

		return selectedChoice != cancel;
	}

	@Override protected boolean doExecute()
	{
		desProgressGroup.start();
		m_helper.refreshDictionary();
		return super.doExecute();
	}

	public boolean processDesign(@NotNull IFunctionLogicDesign design)
	{
		String message =
				ResourceMgr.getString(BatchUpdateFromDictionaryCmd.class, "BatchUpdateFromDictionaryCmd.BeginUpdate",
						design.getFullName());
		outputWindow.sendMessage(message, m_helper.getOutputTabName(), true);

		Map<IFunctionBaseConductor, Pair<INameTemplate, INameTemplate>> eligibleObjectsNameTemplateMap =
				getEligibleObjects(design);

		Set<IUIDObject> modifiedConductors = new HashSet<>();
		for (IFunctionBaseConductor conductor : eligibleObjectsNameTemplateMap.keySet()) {
			Pair<INameTemplate, INameTemplate> selectedTemplate = eligibleObjectsNameTemplateMap.get(conductor);

			if (conductor.isShared()) {
				ISharedConductor sharedConductor =
						CommonUtils.cast(conductor.getSharedObject(), ISharedConductor.class);
				if (sharedConductor != null) {
					String type = m_helper.getFunctionConductorType(conductor);

					if (!canEditSharedConductor(conductor, sharedConductor, design, type)) {
						continue;
					}

					boolean lockSuccess = false;
					try {
						lockSuccess = m_helper.lockSharedConductor(design, conductor, selectedTemplate, sharedConductor, type,
								false);
					}
					finally {
						if (lockSuccess) {
							modifiedConductors.add(conductor);
							LockUpdateHelper.flushAndUnlockSharedObject(sharedConductor);
						}
					}
				}
			}
			else {
				modifiedConductors.add(conductor);
				m_helper.applyUpdateAction(conductor, selectedTemplate);
			}
		}

		// Clean up orphaned property text due to removal of properties
		cleanUpPropTextForConductors(modifiedConductors);
		return true;
	}

	private void cleanUpPropTextForConductors(@NotNull Set<IUIDObject> conductors)
	{
		PropTextScrubber propTextScrubber = new PropTextScrubber();
		propTextScrubber.synchronizeChangedObjects(conductors, Collections.emptyList());
	}

	private boolean canEditSharedConductor(@NotNull IFunctionBaseConductor conductor,
			@NotNull ISharedConductor sharedConductor, @NotNull IFunctionLogicDesign design, @NotNull String type)
	{
		if (sharedConductor.isFrozen()) {
			String msg = ResourceMgr.getString(BatchUpdateFromDictionaryCmd.class,
					"BatchUpdateFromDictionaryCmd.SharedConductorIsFrozen", type, HTMLHelper.link(design, conductor));
			outputWindow.sendMessage(msg, m_helper.getOutputTabName(), true);
			return false;
		}
		return true;
	}

	@NotNull private Map<IFunctionBaseConductor, Pair<INameTemplate, INameTemplate>> getEligibleObjects(
			IFunctionLogicDesign design)
	{
		Map<IFunctionBaseConductor, Pair<INameTemplate, INameTemplate>> objectTemplateMap = new HashMap<>();
		IConnectivity connectivity = design.getConnectivity();
		if (connectivity == null) {
			return objectTemplateMap;
		}

		for (IConductor cond : connectivity.getFunctionBaseConductors()) {
			Pair<INameTemplate, INameTemplate> dictionary = m_helper.getNameTemplate((IFunctionBaseConductor) cond);
			if (dictionary != null) {
				objectTemplateMap.put((IFunctionBaseConductor) cond, dictionary);
			}
		}
		return objectTemplateMap;
	}

	@NotNull @Override protected String getOutputTabName()
	{
		return m_helper.getOutputTabName();
	}

	@NotNull protected String getDesignLockedByOtherUserMsg(String fullName, String lockedByUser)
	{
		return ResourceMgr
				.getString(BatchUpdateFromDictionaryCmd.class,
						"BatchUpdateFromDictionaryCmd.DesignLockedByOtherUser", fullName, lockedByUser);
	}

	@NotNull protected String getCannotLockDesignMsg(String fullName)
	{
		return ResourceMgr.getString(BatchUpdateFromDictionaryCmd.class,
				"BatchUpdateFromDictionaryCmd.CannotLockDesign", fullName);
	}

	@NotNull @Override String getInaccessibleDesignsOutputMessage()
	{
		return ResourceMgr.getString(BatchUpdateICDCmd.class, "BatchUpdateFromDictionaryCmd.InaccessibleDesign");
	}

	@NotNull protected String getCannotEditDesignMsg(IFunctionLogicDesign design)
	{
		return ResourceMgr.getString(BatchUpdateFromDictionaryCmd.class,
				"BatchUpdateFromDictionaryCmd.CannotEditDesign", design.getFullName());
	}

	@NotNull protected String getInAccessibleSharedObjectsMsg(IFunctionLogicDesign design)
	{
		return ResourceMgr.getString(BatchUpdateFromDictionaryCmd.class,
				"BatchUpdateFromDictionaryCmd.DesignHasInaccessibleSharedObjects", design.getFullName());
	}

	protected boolean isTransactionBoundaryNeededForDesignProcessing()
	{
		return true;
	}
}
