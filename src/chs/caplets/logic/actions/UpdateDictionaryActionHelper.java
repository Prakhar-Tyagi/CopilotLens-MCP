/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022-2024 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.IOutputWindow;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.UpdateFuncSignalToDictSignal;
import chs.cof.logical.cable.IFunctionBaseConductor;
import chs.cof.logical.cable.IFunctionConductor;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.project.naming.INameMgr;
import chs.cof.project.objectinfo.IObjectTypeInfo;
import chs.cof.project.objectinfo.IObjectTypeInfoRelations;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.common.attr.IAttributeTypes;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.FactoryMgr;
import chs.system.ISystemObjectTypeInfoMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.attr.custom.CustomAttributesControl;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class to handle update from dictionary action for signals and messages
 */
public class UpdateDictionaryActionHelper
{

	private IOutputWindow m_outputWindow;

	public UpdateDictionaryActionHelper(IOutputWindow outputWindow)
	{
		m_outputWindow = outputWindow;
	}

	@NotNull public String getOutputTabName()
	{
		return ResourceMgr.getString(UpdateDictionaryAction.class, "UpdateDictionaryAction.output.tab");
	}

	public void refreshDictionary()
	{
		// refresh the SYSTEM OTIs (which inturn refresh dictionary OTIs, network dictionary is planned to be taken out of system OTI when that is implemented this part of code needs to be changed to refresh netwokr dictionary)
		ISystemObjectTypeInfoMgr objectTypeInfoMgr = FactoryMgr.getCHSSystem().getSystemData().getObjectTypeInfoMgr();
		if (objectTypeInfoMgr != null) {
			objectTypeInfoMgr.refresh(); //refresh if required
		}
	}

	@Nullable public Pair<INameTemplate, INameTemplate> getNameTemplate(@NotNull IFunctionBaseConductor obj)
	{
		ISystemObjectTypeInfoMgr infoMgr = FactoryMgr.getCHSSystem().getSystemData().getObjectTypeInfoMgr();
		assert infoMgr!=null;
		if (obj instanceof IFunctionConductor) {
			return dictionarySignalMatch(obj, infoMgr);
		}
		INameTemplate messageTemplate = dictionaryMessageMatch(obj, infoMgr);
		if (messageTemplate != null) {
			return new Pair<>(messageTemplate, null);
		}
		return new Pair<>(null, null);
	}

	public void applyUpdateAction(@NotNull IFunctionBaseConductor obj,
			@NotNull Pair<INameTemplate, INameTemplate> selectedTemplatePair)
	{
		if (selectedTemplatePair.getFirst() == null) {
			disassociateSignalOrMessage(obj);
			return;
		}
		associateSignalOrMessage(obj, selectedTemplatePair);
	}

	private void associateSignalOrMessage(@NotNull IFunctionBaseConductor obj,
			@NotNull Pair<INameTemplate, INameTemplate> selectedTemplatePair)
	{
		INameTemplate template = selectedTemplatePair.getFirst();
		getUpdateFuncSignalToDictSignal().updateFunctionBaseConductor(obj, template, selectedTemplatePair.getSecond());
		displayUpdateMessageOnOutputTab(obj, template);
	}

	@NotNull protected UpdateFuncSignalToDictSignal getUpdateFuncSignalToDictSignal()
	{
		return new UpdateFuncSignalToDictSignal();
	}

	private void disassociateSignalOrMessage(@NotNull IFunctionBaseConductor obj)
	{
		boolean isDisAssociationSuccess = getUpdateFuncSignalToDictSignal().disAssociateFunctionBaseConductor(obj);
		if (isDisAssociationSuccess) {
			String message = obj instanceof IFunctionMessage ? "UpdateDictionaryAction.disassociate.messageSuccess" :
					"UpdateDictionaryAction.disassociate.signalSuccess";
			printOutputMessage(obj, message);
		} else if (dictionaryNameIsNotEmpty(obj)){
			String message = obj instanceof IFunctionMessage ? "UpdateDictionaryAction.notAssociated.messageSuccess" :
					"UpdateDictionaryAction.notAssociated.signalSuccess";
			printOutputMessage(obj, message);
		}
	}

	private boolean dictionaryNameIsNotEmpty(@NotNull IFunctionBaseConductor baseConductor)
	{
		final CustomAttributesControl control = new CustomAttributesControl(baseConductor);
		String conductorName = baseConductor instanceof IFunctionConductor ?
				control.getCustomAttributeValueAsNonNullString(IAttributeTypes.DICTIONARY_SIGNAL_NAME):
				control.getCustomAttributeValueAsNonNullString(IAttributeTypes.DICTIONARY_MESSAGE_NAME);
		return !StringUtils.isBlank(conductorName);
	}
	private void printOutputMessage(@NotNull IFunctionBaseConductor obj, @NotNull String message)
	{
		ILogicDesign logicDesign = obj.getLogicDesign();
		if (logicDesign != null) {
			String outputMessage =
					ResourceMgr.getString(UpdateDictionaryAction.class, message, HTMLHelper.link(logicDesign, obj));
			m_outputWindow.sendMessage(outputMessage, getOutputTabName(), true);
		}
	}

	private void displayUpdateMessageOnOutputTab(@NotNull IFunctionBaseConductor obj,
			@NotNull INameTemplate selectedTemplate)
	{
		String message = obj instanceof IFunctionMessage ? "UpdateDictionaryAction.output.messageSuccess" :
				"UpdateDictionaryAction.output.signalSuccess";
		ILogicDesign logicDesign = obj.getLogicDesign();
		if (logicDesign != null) {
			String outputMessage =
					ResourceMgr.getString(UpdateDictionaryAction.class, message, HTMLHelper.link(logicDesign, obj),
							getNameAndRevisionString(selectedTemplate));
			m_outputWindow.sendMessage(outputMessage, getOutputTabName(), true);
		}
	}

	@NotNull private static String getNameAndRevisionString(@NotNull INameTemplate selectedTemplate)
	{

		StringBuilder nameRevString = new StringBuilder(selectedTemplate.getName());
		if (!StringUtils.isBlank(selectedTemplate.getRevision())) {
			nameRevString.append(":").append(selectedTemplate.getRevision());
		}
		return nameRevString.toString();
	}

	@Nullable private INameTemplate dictionaryMessageMatch(@NotNull IFunctionBaseConductor obj,
			@NotNull ISystemObjectTypeInfoMgr infoMgr)
	{
		IObjectTypeInfo messageOTI = infoMgr.getByObject(obj);
		final CustomAttributesControl control = new CustomAttributesControl(obj);
		String messageName = control.getCustomAttributeValueAsNonNullString(IAttributeTypes.DICTIONARY_MESSAGE_NAME);
		String messageRev = control.getCustomAttributeValueAsNonNullString(IAttributeTypes.DICTIONARY_MESSAGE_REVISION);
		return messageOTI.getNameTemplateByNameAndRevision(messageName, messageRev);
	}

	@NotNull private Pair<INameTemplate, INameTemplate> dictionarySignalMatch(@NotNull IFunctionBaseConductor obj,
			@NotNull ISystemObjectTypeInfoMgr infoMgr)
	{
		IObjectTypeInfo signalOTI = infoMgr.getByObject(obj);
		final CustomAttributesControl control = new CustomAttributesControl(obj);
		String signalName = control.getCustomAttributeValueAsNonNullString(IAttributeTypes.DICTIONARY_SIGNAL_NAME);
		String signalRev = control.getCustomAttributeValueAsNonNullString(IAttributeTypes.DICTIONARY_SIGNAL_REVISION);
		INameTemplate signalTemplate = signalOTI.getNameTemplateByNameAndRevision(signalName, signalRev);
		IObjectTypeInfoRelations messageSignalRelations =
				infoMgr.getRelations(INameMgr.MESSAGE, INameMgr.FUNCTIONCONDUCTOR);
		if (signalTemplate != null) {
			String messageName =
					control.getCustomAttributeValueAsNonNullString(IAttributeTypes.DICTIONARY_MESSAGE_NAME);
			String messageRev =
					control.getCustomAttributeValueAsNonNullString(IAttributeTypes.DICTIONARY_MESSAGE_REVISION);

			if (messageName.isEmpty() && messageRev.isEmpty()) {
				if (messageSignalRelations == null ||
						messageSignalRelations.findRelationsByTarget(signalTemplate).isEmpty()) {
					return new Pair<>(signalTemplate, null);
				}
				else {
					return new Pair<>(null, null);
				}
			}

			INameTemplate msgNameTemplate =
					infoMgr.getByName(INameMgr.MESSAGE).getNameTemplateByNameAndRevision(messageName, messageRev);
			if (msgNameTemplate != null &&
					doesRelationExistInDictionary(signalTemplate, msgNameTemplate, messageSignalRelations)) {
				return new Pair<>(signalTemplate, msgNameTemplate);
			}
		}
		return new Pair<>(null, null);
	}

	private boolean doesRelationExistInDictionary(@NotNull INameTemplate signalTemplate,
			@NotNull INameTemplate messageTemplate, @Nullable IObjectTypeInfoRelations messageSignalRelations)
	{
		if (messageSignalRelations != null) {
			return messageSignalRelations.findRelationsByTarget(signalTemplate).
					stream().map(relation -> relation.getSource())
					.anyMatch(iNameTemplate -> iNameTemplate.equals(messageTemplate));
		}
		return false;
	}

	public boolean lockSharedConductor(@NotNull IFunctionBaseConductor conductor,
			@NotNull Pair<INameTemplate, INameTemplate> selectedTemplate, @NotNull ISharedConductor sharedConductor,
			boolean showPopup)
	{
		return lockSharedConductor(conductor.getLogicDesign(), conductor, selectedTemplate, sharedConductor,
				getFunctionConductorType(conductor), showPopup);
	}

	@NotNull public String getFunctionConductorType(@NotNull IFunctionBaseConductor conductor)
	{
		return conductor instanceof IFunctionConductor ?
				ResourceMgr.getString(UpdateDictionaryActionHelper.class,
						"UpdateDictionaryAction.SignalObjectType") :
				ResourceMgr.getString(UpdateDictionaryActionHelper.class,
						"UpdateDictionaryAction.MessageObjectType");
	}

	public boolean lockSharedConductor(@Nullable ILogicDesign design, @NotNull IFunctionBaseConductor conductor,
			@NotNull Pair<INameTemplate, INameTemplate> selectedTemplate, @NotNull ISharedConductor sharedConductor,
			@NotNull String type, boolean showPopup)
	{
		boolean lockSuccess = LockUpdateHelper.obtainLockOnSharedObject(sharedConductor, showPopup);
		if (lockSuccess) {
			applyUpdateAction(conductor, selectedTemplate);
		}
		else if (design != null && !showPopup) {
			// Show lock failure message in output tab when popup is not shown
			String msg = ResourceMgr.getString(UpdateDictionaryActionHelper.class,
					"UpdateDictionaryAction.SharedConductorLocked",
					type, HTMLHelper.link(design, conductor), design.getFullName(), type);
			m_outputWindow.sendMessage(msg, getOutputTabName(), true);
		}
		return lockSuccess;
	}
}
