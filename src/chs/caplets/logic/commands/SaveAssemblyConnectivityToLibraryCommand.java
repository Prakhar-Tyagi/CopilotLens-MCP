/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */
package chs.caplets.logic.commands;

import chs.bridges.CapitalBridges;
import chs.bridges.adaptors.AdaptorFactory;
import chs.bridges.adaptors.IAdaptorFormat;
import chs.bridges.configuration.BridgeConfigurationHelper;
import chs.bridges.configuration.ConfigurationFactory;
import chs.cof.parts.ILibraryAssembly;
import chs.cof.parts.ILibraryAssemblyConnectivityDetails;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryBatchLoader;
import chs.cof.parts.ILibraryComponentScopeCode;
import chs.cof.parts.ILibraryDomain;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryHousingDefinition;
import chs.cof.parts.ILibraryMultipleWireFitsCavity;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibraryPartAuditCollector;
import chs.cof.parts.ILibrarySingleWireFitsCavity;
import chs.cof.parts.ILibraryUserPropertyPart;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryBooleanType;
import chs.cof.parts.LibraryLoadChildVisitor;
import chs.cof.parts.LibraryPartAuditCollector;
import chs.cof.parts.LibraryPrivilegeMgr;
import chs.cof.security.FunctionalPermissionEnum;
import chs.cofUtils.autoGen.copy.CopyableObjectHelper;
import chs.cofUtils.autoGen.copy.ICopyableObject;
import chs.cofUtils.autoGen.factory.COFModelFactory;
import chs.cofUtils.autoGen.factory.TransientModelFactory;
import chs.cofUtils.autoGen.trans.TransientUIDMgr;
import chs.cofUtils.autoGen.visitor.IObjectVisitor;
import chs.cofUtils.autoGen.visitor.VisitableObjectHelper;
import chs.cofUtils.cmd.CHSCommand;
import chs.cofUtils.cmd.CommandEvent;
import chs.cofUtils.parts.LibraryBatchLoader;
import chs.common.ICommandEvent;
import chs.common.IDesignContainer;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.common.attachments.LibraryAssemblyConnectivityBlobAttachment;
import chs.common.attachments.LibraryAssemblyConnectivityBlobAttachmentPersister;
import chs.common.attr.IAttributeTypes;
import chs.system.CHSSystemMgr;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ZipUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by kjuthi on 06-05-2015
 */
@SuppressWarnings("OverlyCoupledClass")
 public class SaveAssemblyConnectivityToLibraryCommand extends CHSCommand
{

	@NotNull private final IDesignContainer m_design;
	@Nullable private ILibraryAssembly m_selectedAssemblyPart;

	public SaveAssemblyConnectivityToLibraryCommand(@NotNull IDesignContainer designContainer)
	{
		m_design = designContainer;
	}

	@Override protected boolean doExecute()
	{
		getCommandListener().startProcessing(4);
		boolean success = false;

		try {
			if (m_selectedAssemblyPart == null) {
//				getCommandListener().handleEvent(ResourceCommandEvent.create("Message.partnotset"));
				return false;
			}
			preLoadSelectedPartChildren();

			if(!isEditableByDomain(m_selectedAssemblyPart))
            {
                getCommandListener().handleFailure(
                        new CommandEvent(m_selectedAssemblyPart, ICommandEvent.FAILURE.OBJECT_NOT_EDITABLE));
                return false;
            }
			if (!allowEditingOfCurrentComponent(m_selectedAssemblyPart)) {
				getCommandListener().handleFailure(
						new CommandEvent(m_selectedAssemblyPart, ICommandEvent.FAILURE.USER_HAS_NO_PERMISSION));
				return false;
			}
           	if (!lockAssemblyPart(m_selectedAssemblyPart)) {
				getCommandListener().handleFailure(new CommandEvent(m_selectedAssemblyPart,
						ICommandEvent.FAILURE.LOCK_LIBRARYPART_FAILED));
				return false;
			}
            if(!refreshAssemblyPart(m_selectedAssemblyPart))
            {
                getCommandListener().handleFailure(new CommandEvent(m_selectedAssemblyPart,
                        ICommandEvent.FAILURE.OBJECT_DOES_NOT_EXIST));
                return false;
            }
			getCommandListener().incrementProcessing();
			IObjectVisitor visitor = new LibraryLoadChildVisitor();
			VisitableObjectHelper.apply(visitor, m_selectedAssemblyPart);

			ILibraryAssembly assembly =
					TransientUIDMgr.getObjectOfType(ILibraryAssembly.class, m_selectedAssemblyPart);
			if (assembly != null) {
				CopyableObjectHelper.copyFromCOF(assembly, ICopyableObject.Depth.DEEP);
			}
			if (assembly != null) {
				ILibraryAssemblyConnectivityDetails details = processLibraryMetaData(assembly);
				success = processAssemblyConnectivity(assembly, details);
				ILibraryPartAuditCollector iLibraryPartAuditCollector
						= new LibraryPartAuditCollector();
				iLibraryPartAuditCollector.pushAuditTrailEvent(assembly, ILibraryPartAuditCollector.AUDIT_TYPE.PART_MODIFIED,
						ResourceMgr.getString(this, "SaveAssemblyConnectivityToLibraryCommand.LogPartModifiedEvent.SaveasAssembly"));
				iLibraryPartAuditCollector.flushCollectedAuditTrail();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			unlockAssemblyPart(m_selectedAssemblyPart);
/*
			if (m_selectedAssemblyPart != null) {
				if (success) {
					getCommandListener().handleEvent(
							ResourceCommandEvent.create("Message.success", m_selectedAssemblyPart.getPartNumber()));
				}
				else {
					getCommandListener().handleEvent(
							ResourceCommandEvent.create("Message.failed", m_selectedAssemblyPart.getPartNumber()));
				}
			}
*/
		}
		return success;
	}

	private void preLoadSelectedPartChildren()
	{
		if (m_selectedAssemblyPart == null) {
			return;
		}
		ILibraryBatchLoader libraryBatchLoader = LibraryBatchLoader.createInstance();
		List<Class<? extends ILibraryBaseObject>> childrenToBeLoaded = Arrays.asList(
				ILibraryHousingDefinition.class,
				ILibraryUserPropertyPart.class,
				ILibraryDomain.class,
				ILibraryGraphic.class,
				ILibrarySingleWireFitsCavity.class,
				ILibraryMultipleWireFitsCavity.class,
				ILibraryComponentScopeCode.class
		);
		libraryBatchLoader.loadLibrarySelectiveChildren(Collections.singletonList(m_selectedAssemblyPart.getUID()),
				IAttributeTypes.OWNER, childrenToBeLoaded);
	}

	protected boolean processAssemblyConnectivity(@NotNull ILibraryAssembly assembly,
			@NotNull ILibraryAssemblyConnectivityDetails details)
			throws IOException
	{
//		getCommandListener().handleEvent(ResourceCommandEvent.create("Message.exportingConnectivity"));
		ByteArrayOutputStream connectivityData = exportX2MLData();
		getCommandListener().incrementProcessing();
		if (connectivityData == null) {
//			getCommandListener().handleEvent(ResourceCommandEvent.create("Message.exportfailed"));
			return false;
		}
//		getCommandListener().handleEvent(ResourceCommandEvent.create("Message.updatingConnectivity"));
		saveConnectivityBlob(assembly, details, connectivityData);
		CopyableObjectHelper.copyToCOF(assembly, COFModelFactory.getInstance(), ICopyableObject.Depth.DEEP);
		getCommandListener().incrementProcessing();
		return true;
	}

	@NotNull protected ILibraryAssemblyConnectivityDetails processLibraryMetaData(ILibraryAssembly assembly)
	{
		assembly.clearAssemblyDetails();
		ILibraryAssemblyConnectivityDetails details = getLibraryAssemblyConnectivityDetails(assembly);
		details.setChangeIdentifier(FactoryMgr.createUID().getString());
//		getCommandListener()
//				.handleEvent(ResourceCommandEvent.create("Message.processingPart", assembly.getPartNumber()));
		assembly.setConnectivityIncluded(LibraryBooleanType.TRUE);
        assembly.setTimeModified(assembly.getTimeModified() + 1);
		saveAssemblyPart(assembly);
		getCommandListener().incrementProcessing();
		return details;
	}

	protected boolean allowEditingOfCurrentComponent(@NotNull ILibraryAssembly assembly)
	{
		ILibraryObject.PartStatusType status = assembly.getStatus();
		if (status == ILibraryObject.PartStatusType.CURRENT) {
			return CHSSystemMgr.getCHSSystem().getFunctionalPermissionMgr()
					.hasPermission(FunctionalPermissionEnum.ModifyCurrentLibraryComponents);
		}
		return true;
	}
    protected boolean isEditableByDomain(@NotNull ILibraryAssembly assembly)
    {
        return new LibraryPrivilegeMgr().isEditableByDomain(assembly);
    }

	protected void saveAssemblyPart(@NotNull ILibraryAssembly assembly)
	{
		Library.getInstance().getPersister().persistPart(assembly);
	}

	@NotNull
	protected ILibraryAssemblyConnectivityDetails getLibraryAssemblyConnectivityDetails(
			@NotNull ILibraryAssembly assembly)
	{
		ILibraryAssemblyConnectivityDetails details = assembly.getAssemblyConnectivityDetails();
		if (details == null) {
			details = TransientModelFactory.getInstance().createObject(ILibraryAssemblyConnectivityDetails.class);
			assembly.setAssemblyConnectivityDetails(details);
		}
		details.setOwner(assembly);
		return details;
	}

	protected void unlockAssemblyPart(@Nullable ILibraryAssembly assembly)
	{
		if (assembly != null) {
			assembly.unlock();
		}
	}
    protected boolean refreshAssemblyPart(@Nullable ILibraryAssembly assembly)
    {
        if (assembly != null) {
            RefreshStatusEnum refStstus = assembly.refresh();
			return refStstus != RefreshStatusEnum.eObjectDoesNotExist;
        }
        return true;
    }
	protected boolean lockAssemblyPart(@NotNull ILibraryAssembly selectedObject)
	{
		return selectedObject.lock();
	}

	protected void saveConnectivityBlob(@NotNull ILibraryAssembly selectedObject,
			@NotNull ILibraryAssemblyConnectivityDetails details,
			@NotNull ByteArrayOutputStream connectivityData) throws IOException
	{
		LibraryAssemblyConnectivityBlobAttachment attachment =
				new LibraryAssemblyConnectivityBlobAttachment(details.getUID());
		attachment.setContainerId(selectedObject.getUID());
		LibraryAssemblyConnectivityBlobAttachmentPersister persister =
				new LibraryAssemblyConnectivityBlobAttachmentPersister();
		persister.saveAttachment(ZipUtils.compressBytes(connectivityData.toByteArray()), attachment);
	}

	@Nullable protected ByteArrayOutputStream exportX2MLData()
	{
		ByteArrayOutputStream output = null;
		try {
			IAdaptorFormat format = AdaptorFactory.getFactory().getFormat("X2ML");
			output = performBridgeOut(format.getBridgeType(), m_design);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return output;
	}

	@SuppressWarnings({"ProhibitedExceptionDeclared", "SSBasedInspection"})
	protected ByteArrayOutputStream performBridgeOut(short bridgeType, IUIDObject target)
			throws Exception
	{
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		CapitalBridges bridges = new CapitalBridges(ConfigurationFactory.getInstance().getConfiguration(
				BridgeConfigurationHelper.getUserBridgesContext(null)));
		bridges.setBridgeType(bridgeType);
		bridges.bridgeDesignOut(target, outStream, false, false);
		return outStream;
	}

	public void setSelectedLibraryPart(ILibraryAssembly selectedAssemblyPart)
	{
		m_selectedAssemblyPart = selectedAssemblyPart;
	}
}
