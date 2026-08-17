/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.shared.batchshare.ui.AbstractBatchShareDialog;
import chs.caplets.logic.actions.shared.batchshare.ui.BatchShareParams;
import chs.caplets.logic.actions.shared.batchshare.ui.FunctionalBatchShareDialog;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedFunctionMessage;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import chs.common.INamedPropertiedObject;
import chs.common.attr.IAttributeTypes;
import chs.common.attr.custom.ICustomAttributesProvider;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.attr.custom.CustomAttributesControl;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Functional batch share action executor
 */
public class FunctionalBatchShareActionExecutor extends AbstractBatchShareActionExecutor
{

	public FunctionalBatchShareActionExecutor(@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria)
	{
		super(project, designs, entitiesShareCriteria);
	}

	@Override protected boolean acquireLocks()
	{
		boolean lockSuccess = lockObjects(m_designs);
		lockSuccess = lockObjects(Set.of(m_project.getSharedConductorMgr())) && lockSuccess;
		return lockSuccess;
	}

	/**
	 * Constructs functional batch share dialog
	 *
	 * @param batchShareParams batch share data provider
	 * @return functional batch share dialog
	 */
	@NotNull @Override
	protected AbstractBatchShareDialog getBatchShareDialog(@NotNull BatchShareParams batchShareParams)
	{
		return new FunctionalBatchShareDialog(CAFUtils.getInstance().getDialogFrame(),
				ResourceMgr.getString(BatchShareActionExecutor.class, "BatchShareActionExecutor.Dialog.title"),
				batchShareParams);
	}

	/**
	 * @return true if to share unplaced objects
	 */
	@Override protected boolean doShareUnplacedObjects()
	{
		return false;
	}

	/**
	 * Constructs shareable functional objects finder to find, group and map the functional objects of given designs to functional shared objects in scope based on provided share criteria
	 *
	 * @param project              project containing the build list
	 * @param designs              designs of the build list
	 * @param sharedObjectsInScope shared objects of the project
	 * @param objectInfoProvider   object information provider
	 * @return shareable functional objects finder
	 */
	@NotNull @Override
	protected AbstractShareableObjectsFinder getShareableObjectsFinder(
			@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull Collection<ISharedObject> sharedObjectsInScope, @NotNull IObjectInfoProvider objectInfoProvider)
	{
		return new ShareableFunctionalObjectsFinder(project, designs, sharedObjectsInScope, objectInfoProvider);
	}

	/**
	 * Constructs functional object information provider
	 *
	 * @return object information provider for function design objects
	 */
	@NotNull @Override protected IObjectInfoProvider getObjectInfoProvider()
	{
		return new FunctionalObjectInfoProvider();
	}

	/**
	 * Provides collection of shared objects present in the project
	 *
	 * @param entitiesShareCriteria criteria used to match objects
	 * @return collection of shared objects
	 */
	@NotNull @Override protected Collection<ISharedObject> getSharedObjectsInScope(
			@NotNull Set<ShareableEntityTypeEnum> typesToBeConsidered)
	{
		ISharedConductorMgr sharedConductorMgr = m_project.getSharedConductorMgr();
		sharedConductorMgr.refresh();
		Collection<ISharedObject> sharedObjectsInScope = new HashSet<>();
		for (ShareableEntityTypeEnum type : typesToBeConsidered) {
			if (type.equals(ShareableEntityTypeEnum.FUNCTION_MESSAGE)) {
				sharedObjectsInScope
						.addAll(sharedConductorMgr.getFunctionalSharedMessages().stream()
								.filter(ISharedConductor::isMessage)
								.collect(
										Collectors.toSet()));
			}
			else if (type.equals(ShareableEntityTypeEnum.FUNCTION_SIGNAL)) {
				sharedObjectsInScope
						.addAll(sharedConductorMgr.getFunctionalSharedConductors().stream()
								.filter(ISharedConductor::isSignal)
								.collect(
										Collectors.toSet()));
			}
			else {
				assert false : "Shared Objects scope not defined for type : " + type;
			}
		}
		return sharedObjectsInScope;
	}

	/**
	 * @param srcObject          source object to be shared into
	 * @param targetSharedObject target shared object
	 * @param reporter           message reporter
	 * @return true if source object to be shared into has conflicting attributes/properties compared to target shared object
	 */
	@Override protected boolean checkAndReportConflictsForNamedObject(@NotNull INamedPropertiedObject srcObject,
			@NotNull ISharedObject targetSharedObject, @NotNull IMessageReporterWithContext reporter)
	{
		boolean hasConflict = super.checkAndReportConflictsForNamedObject(srcObject, targetSharedObject, reporter);
		// check for conflict in active message signals between message to be shared into and target shared message
		if (!hasConflict) {
			hasConflict = checkAndReportConflictsInActiveMessageSignals(srcObject, targetSharedObject, reporter);
		}
		return hasConflict;
	}

	/**
	 * Checks and reports error if there are conflicting active message signals between source object to be share into and target shared object
	 *
	 * @param srcObject          source object to be shared into
	 * @param targetSharedObject target shared object
	 * @param reporter           message reporter
	 * @return true if source object to be shared into has conflicting active message signals compared to target shared object
	 */
	private boolean checkAndReportConflictsInActiveMessageSignals(@NotNull INamedPropertiedObject srcObject,
			@NotNull ISharedObject targetSharedObject,
			@NotNull IMessageReporterWithContext reporter)
	{
		boolean hasConflict = false;
		IFunctionMessage sourceMessage = CommonUtils.cast(srcObject, IFunctionMessage.class);
		ISharedFunctionMessage targetSharedMessage =
				CommonUtils.cast(targetSharedObject, ISharedFunctionMessage.class);
		if (sourceMessage != null && targetSharedMessage != null) {
			if (hasConflictActiveMessageSignals(sourceMessage, targetSharedMessage)) {
				hasConflict = true;
				reportActiveMessageSignalsConflict(sourceMessage, targetSharedMessage, reporter);
			}
		}
		return hasConflict;
	}

	/**
	 * @param sourceMessage       function message to be shared into
	 * @param targetSharedMessage target shared function message
	 * @return true if function message to be shared into has conflicting active message signals compared to target shared function message
	 */
	private boolean hasConflictActiveMessageSignals(@NotNull IFunctionMessage sourceMessage,
			@NotNull ISharedFunctionMessage targetSharedMessage)
	{
		if (sourceMessage.getActiveSignals().size() != targetSharedMessage.getActiveSignals().size()) {
			return true;
		}

		Set<MessageSignalIdentifier> sourceIdentifiers = sourceMessage.getActiveSignals().stream()
				.map(messageSignal -> new MessageSignalIdentifier(getDictionarySignalName(messageSignal),
						getDictionarySignalRevision(messageSignal)))
				.collect(Collectors.toSet());

		Set<MessageSignalIdentifier> targetIdentifiers = targetSharedMessage.getActiveSignals().stream()
				.map(sharedMessageSignal -> new MessageSignalIdentifier(getDictionarySignalName(sharedMessageSignal),
						getDictionarySignalRevision(sharedMessageSignal)))
				.collect(Collectors.toSet());

		return !sourceIdentifiers.equals(targetIdentifiers);
	}

	private void reportActiveMessageSignalsConflict(@NotNull IFunctionMessage sourceMessage,
			@NotNull ISharedFunctionMessage targetSharedMessage, @NotNull IMessageReporterWithContext reporter)
	{
		String sharedObjectDisplayName = getSharedObjectDisplayName(targetSharedMessage);
		reporter.report(PromptSeverity.ERROR, ResourceMgr.getString(AbstractFindAndShareCmd.class,
				"AbstractFindAndShareCmd.ActiveMessageSignalsConflictWhileSharingInto.msg",
				sourceMessage.getName(), sharedObjectDisplayName), IMessageContext.createContext(sourceMessage));
	}

	@NotNull private String getDictionarySignalName(@NotNull ICustomAttributesProvider object)
	{
		CustomAttributesControl customAttributesControl = new CustomAttributesControl(object);
		String dictionarySignalName = CommonUtils.require(
				customAttributesControl.getCustomAttributeValue(IAttributeTypes.DICTIONARY_SIGNAL_NAME),
				String.class);
		return dictionarySignalName;
	}

	@NotNull private String getDictionarySignalRevision(@NotNull ICustomAttributesProvider object)
	{
		CustomAttributesControl customAttributesControl = new CustomAttributesControl(object);
		String dictionarySignalRevision = CommonUtils.require(
				customAttributesControl.getCustomAttributeValue(IAttributeTypes.DICTIONARY_SIGNAL_REVISION),
				String.class);
		return dictionarySignalRevision;
	}

	/**
	 * Identifier for message signal
	 */
	protected static class MessageSignalIdentifier
	{

		@NotNull private final String dictionarySignalName;
		@NotNull private final String dictionarySignalRevision;

		protected MessageSignalIdentifier(@NotNull String dictionarySignalName,
				@NotNull String dictionarySignalRevision)
		{
			this.dictionarySignalName = dictionarySignalName;
			this.dictionarySignalRevision = dictionarySignalRevision;
		}

		@Override public boolean equals(Object obj)
		{
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			MessageSignalIdentifier messageSignalIdentifier = (MessageSignalIdentifier) obj;
			return Objects.equals(dictionarySignalName, messageSignalIdentifier.dictionarySignalName) &&
					Objects.equals(dictionarySignalRevision, messageSignalIdentifier.dictionarySignalRevision);
		}

		@Override public int hashCode()
		{
			return Objects.hash(dictionarySignalName, dictionarySignalRevision);
		}
	}
}
