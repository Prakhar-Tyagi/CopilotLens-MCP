/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.IMulticoreShareContextProvider;
import chs.caplets.logic.actions.shared.MulticoreSharedController;
import chs.caplets.logic.actions.shared.MulticoreSharedController.NameValidationResult;
import chs.cof.COFTypeEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.common.IUID;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class AutoShareMulticoreContextProvider implements IMulticoreShareContextProvider
{

	private static final String NEW_SHAREDOBJECT_REVISION = "1";
	protected IMulticore m_multicore;
	protected ILogicDesign m_design;
	@NotNull private IMessageReporterWithContext m_messageReporter;

	public AutoShareMulticoreContextProvider(@NotNull IMulticore multicore, @NotNull ILogicDesign design,
			@NotNull IMessageReporterWithContext reporter)
	{
		m_multicore = multicore;
		m_design = design;
		m_messageReporter = reporter;
	}

	@Nullable @Override public IUID getSharedMulticoreUID()
	{
		return null;
	}

	@Nullable @Override public String getSharedMulticoreName()
	{
		return m_multicore.getName();
	}

	@Nullable @Override public String getSharedMulticoreRevision()
	{
		return NEW_SHAREDOBJECT_REVISION;
	}

	@Override public boolean isSharedMulticoreNameGenerated()
	{
		return m_multicore.isGeneratedName();
	}

	@NotNull @Override public Map<ILogicObject, IUID> getMulticoreToSharedHierarchyMap()
	{
		return Collections.emptyMap();
	}

	public boolean validate()
	{
		return checkAndReportNameValidationError();
	}

	protected void reportError(@NotNull String message)
	{
		m_messageReporter.report(PromptSeverity.ERROR, message, IMessageContext.createContext(m_multicore));
	}

	private boolean checkAndReportNameValidationError()
	{
		NameValidationResult validationResult =
				MulticoreSharedController.validateName(m_multicore.getName(), m_multicore, m_design);
		if (validationResult == NameValidationResult.Valid) {
			return true;
		}
		final String type = COFTypeEnum.getDisplayableTypeName(m_multicore);
		final String message;
		switch (validationResult) {
			case Invalid:
				message = ResourceMgr.getString(AutoShareMulticoreContextProvider.class,
						"AutoShareMulticoreContextProvider.InvalidName.text",
						StringUtils.toLowerCase(type));
				break;
			case DuplicateName:
				message = ResourceMgr.getString(AutoShareMulticoreContextProvider.class,
						"AutoShareMulticoreContextProvider.nameused.text",
						StringUtils.toLowerCase(type));
				break;
			case DuplicateNameAndOptionExpression:
				message = ResourceMgr.getString(AutoShareMulticoreContextProvider.class,
						"AutoShareMulticoreContextProvider.nameandoptionexpressionalreadyused.text",
						StringUtils.toLowerCase(type));
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + validationResult);
		}
		reportError(message);
		return false;
	}
}
