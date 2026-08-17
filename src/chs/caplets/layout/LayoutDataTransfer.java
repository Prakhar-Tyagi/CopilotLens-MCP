/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.layout;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caplets.logic.LogicDataTransfer;
import chs.caplets.logic.View;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IJointContainer;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.IUIDObject;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.GridHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LayoutDataTransfer extends LogicDataTransfer
{

	@Override public boolean canPreserveObjectNames()
	{
		return false;
	}

	@Override public boolean isPasteAllowed()
	{
		if (!super.isPasteAllowed()) {
			return false;
		}
		View srcView = (View) getSourceView();
		IBaseDiagram srcDiagram = srcView != null ? srcView.getDiagram() : null;
		IBaseDiagram targetDiagram = CAFUtils.getInstance().getActiveDiagram();
		if (srcDiagram != null && targetDiagram != null) {
			return GridHelper.areGridContainersEquivalentInPhysicalScale(srcDiagram, targetDiagram);
		}
		return true;
	}

	@Override public boolean isObjectPastable(IDiagramObject obj)
	{
		return isPasteinSameDesign() || isNonElectricalComponent(obj);
	}

	@Override protected void filterNonPasteableContent(Collection<IUIDObject> objectBuffer)
	{
		super.filterNonPasteableContent(objectBuffer);
		Set<IUIDObject> nonPasteableContent = objectBuffer.stream()
				.filter(obj -> {
					if (obj instanceof IDiagramObject) {
						return !isObjectPastable((IDiagramObject) obj);
					}
					return true;
				}).collect(Collectors.toSet());
		removeObjects(nonPasteableContent);
	}

	private void removeObjects(@NotNull Set<IUIDObject> objectsToBeRemoved)
	{
		Set<IUIDObject> extendedObjectsToBeRemoved = new HashSet<>();
		for (IUIDObject object : objectsToBeRemoved) {
			extendedObjectsToBeRemoved.add(object);
			addObjectsToBeRemoved(object, extendedObjectsToBeRemoved);
		}
		Set<ILogicObject> connectivityToBeDeleted = new HashSet<>();
		extendedObjectsToBeRemoved.forEach(object -> {
			if (object instanceof IConnectivityRef) {
				connectivityToBeDeleted.add(((IConnectivityRef) object).getConnectivity());
			}
			removeAndDeleteObject(object);
		});
		for (ILogicObject logicObject : connectivityToBeDeleted) {
			if (logicObject instanceof IShieldBody) {
				IMulticore multicore = ((IShieldBody) logicObject).getMulticore();
				if (!m_multicores.contains(multicore)) {
					continue;
				}
				m_multicores.remove(multicore);
				if(multicore.hasShield()) {
					removeAndDeleteObject(multicore.getShield());
				}
				removeAndDeleteObject(logicObject);
				removeAndDeleteObject(multicore);
			}
			else {
				removeAndDeleteObject(logicObject);
			}
		}
	}

	private void removeAndDeleteObject(@NotNull IUIDObject object)
	{
		doRemoveObject(object);
		if (!object.isDeletedObject()) {
			object.delete();
		}
	}

	private void doRemoveObject(IUIDObject object)
	{
		m_objectBuffer.remove(object);
		if (attributeResolver != null && object instanceof IDiagramObject) {
			attributeResolver.removeCachedDiagramObject((IDiagramObject) object);
		}
		m_creationObjects.remove(object);
		CreationDeletionHelper.getTheCreationHelper().removeCreationObject(object);
		if (object instanceof ICompoundObject) {
			for (IGfxObject gfxObject : ((ICompoundObject) object).getObjects()) {
				if (gfxObject instanceof IUIDObject) {
					doRemoveObject((IUIDObject) gfxObject);
				}
			}
		}
	}

	private void addObjectsToBeRemoved(@NotNull IUIDObject object, @NotNull Set<IUIDObject> extendedObjectsToBeRemoved)
	{
		if (object instanceof IJointContainer) {
			IJoint pinJoint = ((IJointContainer) object).getJoint();
			if (pinJoint != null) {
				extendedObjectsToBeRemoved.add(pinJoint);
			}
		}
		if (object instanceof IConnected) {
			IJoint connectingJoint = ((IConnected) object).getStartJoint();
			if (connectingJoint != null) {
				extendedObjectsToBeRemoved.add(connectingJoint);
			}
			connectingJoint = ((IConnected) object).getEndJoint();
			if (connectingJoint != null) {
				extendedObjectsToBeRemoved.add(connectingJoint);
			}
		}
		if (object instanceof ICompoundObject) {
			for (IGfxObject gfxObject : ((ICompoundObject) object).getObjects()) {
				if (gfxObject instanceof IUIDObject) {
					addObjectsToBeRemoved((IUIDObject) gfxObject, extendedObjectsToBeRemoved);
				}
			}
		}
	}

	private boolean isNonElectricalComponent(IUIDObject object)
	{
		if (object instanceof IConnectivityRef) {
			ILogicObject connectivityObject = ((IConnectivityRef) object).getConnectivity();
			if(connectivityObject != null) {
				return connectivityObject instanceof ILogicOtherComponent;
			}
		}
		return true;
	}

	@Override public boolean isCutAllowed(ICapletController controller)
	{
		return false;
	}

	@Nullable @Override public String getDisabledTooltipForCut(ICapletController controller)
	{
		return null;
	}

	@Override public void reportOnOutputWindow()
	{
	}

	@Override protected void performPostPaste()
	{
		super.performPostPaste();
		if (isPasteinSameDesign()) {
			assignOldConnectivityForElectricalComponents();
		}
	}

	private void assignOldConnectivityForElectricalComponents()
	{
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		ISchemDiagram activeDiagram = ((ISchemDiagram) CAFUtils.getInstance().getActiveDiagram());
		if (activeDiagram != null) {
			ILogicDesign design = activeDiagram.getDesign();
			if (design != null) {
				IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
				assignOldConnectivity(activeDiagram, dwum, cdh);
			}
		}
	}
}
