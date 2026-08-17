/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;


import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.capitalmanager.appserver.UserSessionException;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.IPrivilegedProjectSharedUsageView;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListUsage;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.project.IProject;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolLibraryMgr;
import chs.common.DesignUtils;
import chs.common.ICommonFactory;
import chs.common.IDesignContainer;
import chs.common.IDesignDescriptor;
import chs.common.IDesignMgr;
import chs.common.ILockable;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.common.*;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.ctf.dataservices.CapitalProjectDataServices;
import chs.ctf.replacesymbol.DeltaRecorder;
import chs.ctf.replacesymbol.ReplaceSharedCompositeDialog;
import chs.ctf.replacesymbol.ReplaceSharedCompositeHelper;
import chs.ctf.ui.form.sharedobjectrevisioning.ShowSharedObjectUsagesHelper;
import chs.dataservices.SharedObjectUsageInfo;
import chs.system.FactoryMgr;
import chs.system.ICAFUtilsBase;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.TransactionHelper;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.helpers.UtilsHelper;
import chs.utility.persist.LockableHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Created: 22/12/2005 Author: andyw
 */
public class ReplaceSharedCompositeSymbolAction extends ControllerActionRT
{

	private DeltaRecorder m_deltas = new DeltaRecorder();
	private ISpecialSelectMgr m_sharedSelectMgr;
	private List<ILockable> m_lockedObjects = Collections.emptyList();

	public ReplaceSharedCompositeSymbolAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller);
		m_sharedSelectMgr = sharedSelectMgr;
		if (getActionUI() != null) {
			m_sharedSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION))
					{
						public boolean shouldDisplay()
						{
							ISharedPinList spl = getOperand();

							// Side effect - change the action name just in time
							ReplaceSharedCompositeSymbolActionUI ui =
									(ReplaceSharedCompositeSymbolActionUI) getActionUI();
							if (ui == null) {
								return false;
							}
							setName((String) ui.getValue(Action.SHORT_DESCRIPTION));

							return spl != null;
						}
					});
		}
		setUndoableAction(false);
	}

	/**
	 * Quick but limited check [Used for the Context Menu]
	 *
	 * @return The shared pinlist if it looks OK at a quich glance
	 */
	@SuppressWarnings({"IfStatementWithIdenticalBranches"})
	@Nullable protected ISharedPinList getOperand()
	{
		ISharedPinList sharedPinlist = null;
		if (m_sharedSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_sharedSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedPinList) {
				sharedPinlist = (ISharedPinList) uidObj;
			}
		}
		if (sharedPinlist == null) {
			return null;
		}
		if (sharedPinlist.isFrozen()) {
			// Shared Object is frozen
			return null;
		}
		if (!isItDeviceOrFunction(sharedPinlist)) {
			// Shared Object is not a device
			return null;
		}
		if (sharedPinlist.getLibraryRef() != null) {
			// Shared Object has a library part
			return null; // don't have a problem with the choice.
		}
		if (sharedPinlist.getNumSymbols() == 0) {
			// No symbols -> Not valid for this action.
			return null;
		}
		if (sharedPinlist.getNumSymbols() != 1) {
			// Multiple symbols -> Not valid for this action.
			return null;
		}
		return sharedPinlist;
	}

	private boolean isItDeviceOrFunction(ISharedPinList sharedPinlist)
	{
		return sharedPinlist.getType() == PinListTypeEnum.TypeDevice || sharedPinlist.isFunctionType();
	}

	@Nullable
	protected String getOperandComplaint(IProject proj, ISharedPinList sharedPinlist)
	{
		if (sharedPinlist == null) {
			// Invalid
			return "";
		}
		if (sharedPinlist.isFrozen()) {
			// Shared Object is frozen
			return ResourceMgr
					.getString(ReplaceSharedCompositeSymbolAction.class, "ReplaceSharedCompositeSymbolAction.isFrozen");
		}
		if (sharedPinlist.getLibraryRef() != null) {
			// Shared Object has a library part
			return ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.hasLibraryPart");
		}
		if (sharedPinlist.getNumSymbols() == 0) {
			// No symbols -> Not valid for this action.
			return ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.hasNoSymbolInstances");
		}
		if (sharedPinlist.getNumSymbols() != 1) {
			// Multiple symbols -> Not valid for this action.
			return ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.hasMultipleSymbolInstances");
		}
		ISymbolDef sdef = getSingleSymbol(sharedPinlist);
		if (sharedPinlist.getNumInstances(sdef) != 1) {
			// Single Mapping only
			return ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.hasMultipleSymbolMappings");
		}
		if (sdef.getNumBlocks() == 0) {
			// Must be a composite.
			return ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.isNotAComposite");
		}
		IPrivilegedProjectSharedUsageView sharedUsageView =
				CommonUtils.cast(proj.getSharedUsageView(), IPrivilegedProjectSharedUsageView.class);
		for (ISharedPinListUsage splusage : Objects.requireNonNull(sharedUsageView)
				.getUsagesOfWithoutDomainFilter(ISharedPinListUsage.class,
						sharedPinlist)) {
			if (splusage.getSymbol() == null) {
				// Must be a block usage [note code will need changing if remove this restriction].
				return ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
						getNonSymbolUsageKey());
			}
		}

		return null;
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		//
		// clear the deltas (no changes yet!)
		//
		m_deltas.clear();
		final ICAFUtilsBase utils = FactoryMgr.getSystemFactory().getCAFUtils();
		Frame owner = CAFUtils.getInstance().getDialogFrame();
		//
		// Build up the tree of shared devices and their composites - if there are no composites
		// found, then there is no swapping that can be done.
		//
		IProject currProject = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
		//
		ISharedPinList spl = getOperand();
		String complaint = getOperandComplaint(currProject, spl);
		if (complaint != null) {
			//
			// Nothing to do [no Shared Objects to select from]
			//
			String title = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.unableToContinue.text");
			//
			// Got to display something... [Null would mean no problem...]
			//
			String msg = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					getNoValidSharedDeviceFoundKey(), complaint);
			MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), title, msg);
			return IActionEnum.eCanceled;
		}
		//
		// Fill in the detals we know, and fire up the dialog.
		//
		ISymbolDef stmp = getSingleSymbol(spl);
		String symName = "";
		IAbstractLibrary library = null;
		if (stmp != null) {
			symName = stmp.getName();
			library = stmp.getContainerLibrary();
		}
		if (library != null) {
			stmp = (ISymbolDef) library.loadFully(stmp);
		}
		//
		if (stmp == null) {
			//
			// Display dialog - symbol could not be found
			//
			String title = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.unableToContinue");
			String msg = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					getUnableToAccessSymbolKey(), symName);
			MessageHelper.showErrorMessage(utils.getDialogFrame(), title, msg);
			return IActionEnum.eCanceled;
		}

		//
		m_deltas.setSharedPinList(spl);
		m_deltas.setSourceSymbol(stmp);
		//
		// Before we do this, we need to get the usages for this shared object, and lock ALL designs that it is used on
		//
		//1) design collection should reflect unsaved designs + unplaced objects in a design + transient usages
		Set<ILogicDesign> allSchematicallyUsedDesigns = new TreeSet<>(new NamedObjectComparator<ILogicDesign>());
		Set<ILogicDesign> designsToModify = new TreeSet<>(new NamedObjectComparator<ILogicDesign>());
		for (ISharedPinListUsage splusage : currProject.getSharedUsageView()
				.getUsagesOf(ISharedPinListUsage.class, spl)) {
			ILogicDesign des = currProject.getDesignMgr().getAbstractLogicDesign(splusage.getDesignUID());
			allSchematicallyUsedDesigns.add(des);
		}
		IDesignMgr iDesignMgr = currProject.getDesignMgr();
		ICommonFactory iCommonFactory = FactoryMgr.getCommonFactory();
		try {
			//need to use data services because we may have unplaced usages of this shared object.
			Collection<CapitalProjectDataServices.SimpleDesignName> collDesignUsages =
					CapitalProjectDataServices.getDataServices().getDesignNamesForSharedPinList(currProject, spl);
			for (CapitalProjectDataServices.SimpleDesignName desUsage : collDesignUsages) {
				IDesignContainer design = iDesignMgr.getDesign(iCommonFactory.constructUID(desUsage.getId()));
				if (design instanceof ILogicDesign) {
					designsToModify.add((ILogicDesign) design);
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			String title = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.unableToContinue");
			String msg = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
					"ReplaceSharedCompositeSymbolAction.unableToGetDesignUsages.text");
			MessageHelper.showErrorMessage(utils.getDialogFrame(), title, msg);
			return IActionEnum.eCanceled;
		}
		//what about transient usages in other sessions.
		SharedObjectUsageInfo splUsageInfo = ShowSharedObjectUsagesHelper.getSharedObjUsageInfo(currProject, spl);
		Set<ILogicDesign> applicableLogicDesigns =
				iDesignMgr.getDesigns(UIDUtils.convertStringIdsToUIDSet(splUsageInfo.getAllUsedDesignIds())).stream()
						.map(designContainer -> CommonUtils.cast(designContainer, ILogicDesign.class))
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
		designsToModify.addAll(applicableLogicDesigns);

		// NOTE: all locked logic designs will be saved if we do end up making changes to the shared object model
		// because we cannot have the risk of deleting shared objects which are still referenced by persisted logic
		// designs as this will throw a database constraint violation exception
		Set<ILogicDesign> designsWithUnplacedUsages = new TreeSet<>(new NamedObjectComparator<ILogicDesign>());
		designsWithUnplacedUsages.addAll(designsToModify);
		designsWithUnplacedUsages.removeAll(allSchematicallyUsedDesigns);
		m_deltas.setAffectedDesignsWithUnplacedUsages(designsWithUnplacedUsages);
		m_deltas.setAffectedDesigns(designsToModify);
		//
		// Use the designs + the shared pin list as the set of objects to modify.
		//
		if (!spl.isEditable()) {
			CTFLockUpdateHelper.displayDomainRestrictionDialog(spl);
			return IActionEnum.eCanceled;
		}
		Set<ILockable> lockNeeds = new HashSet<ILockable>(designsToModify);
		lockNeeds.add(spl);
		//
		// Get the locks on the designs [or fail completely if you cant]
		//
		m_lockedObjects = LockableHelper.atomicLock(lockNeeds, new LockableHelper.ILockFeedback()
		{
			public void failedToLock(ILockable lbl)
			{
				//
				// Display dialog
				//
				CTFLockUpdateHelper
						.displayLockFailureDialog(lbl, FactoryMgr.getSystemFactory().getCHSSystem().getUserSession());
			}
		});
		if (m_lockedObjects == LockableHelper.ATOMIC_LOCK_FAILED) {
			return IActionEnum.eCanceled;
		}
		//
		final ReplaceSharedCompositeDialog dialog = new ReplaceSharedCompositeDialog(owner,
				CAFUtils.getInstance().getDialogTitleByAction(this, true), true, currProject, m_deltas);
		dialog.setCancelled(true);
		dialog.getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dialog.setVisible(false);
			}
		});
		dialog.getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dialog.setCancelled(false);
				dialog.setVisible(false);
			}
		});
		dialog.setVisible(true);
		//
		// Handle cancellation/issues etc. The dialog should only allow the OK button if all OK.
		//
		if (dialog.isCancelled()) {
			return IActionEnum.eCanceled;
		}
		else {
			return IActionEnum.eCompleted;
		}
	}

	@NotNull protected String getNoValidSharedDeviceFoundKey()
	{
		return "ReplaceSharedCompositeSymbolAction.noValidSharedDevicesFound.text";
	}

	@NotNull protected String getUnableToAccessSymbolKey()
	{
		return "ReplaceSharedCompositeSymbolAction.unableToAccessSymbol.text";
	}

	@NotNull protected String getNonSymbolUsageKey()
	{
		return "ReplaceSharedCompositeSymbolAction.nonSymbolUsage";
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean editOk = true;
		try {
			if (successful) {
				getController().getSelectMgr().getCurrentSelections().clear();
				//
				try {
					IProject currProject = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
					//
					// Make sure the symbol dates maqtch correctly..
					//
					ISymbolLibraryMgr symLibMgr = UtilsHelper.getCHSSystem().getSymbolLibraryMgr();
					//
					ISymbolDef srcSym = m_deltas.getSourceSymbol();
					ISymbolDef tgtSym = m_deltas.getTargetSymbol();
					IStamp srcSymFromLib = symLibMgr.getReferencedSymbol(srcSym.getUID());
					IStamp tgtSymFromLib = symLibMgr.getReferencedSymbol(tgtSym.getUID());
					//
					if (srcSymFromLib == null ||
							(srcSymFromLib.getServerTimeModified() != srcSym.getServerTimeModified())) {
						String title = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
								"ReplaceSharedCompositeSymbolAction.unableToContinue");
						String msg = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
								"ReplaceSharedCompositeSymbolAction.symbolVersionMismatch.text", srcSym.getName());
						ICAFUtilsBase utils = FactoryMgr.getSystemFactory().getCAFUtils();
						MessageHelper.showErrorMessage(utils.getDialogFrame(), title, msg);
						return false; // Source Symbol times do not match
					}
					if (tgtSymFromLib == null ||
							(tgtSymFromLib.getServerTimeModified() != tgtSym.getServerTimeModified())) {
						String title = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
								"ReplaceSharedCompositeSymbolAction.unableToContinue");
						String msg = ResourceMgr.getString(ReplaceSharedCompositeSymbolAction.class,
								"ReplaceSharedCompositeSymbolAction.symbolVersionMismatch.text", tgtSym.getName());
						ICAFUtilsBase utils = FactoryMgr.getSystemFactory().getCAFUtils();
						MessageHelper.showErrorMessage(utils.getDialogFrame(), title, msg);
						return false; // Target Symbol times do not match
					}

					CAFCommandHelper commandHelper = new CAFCommandHelper();
					editOk = ReplaceSharedCompositeHelper
							.applyChangesToProject(currProject, m_deltas, m_lockedObjects, commandHelper);
					TransactionHelper.endTransaction();

					getController().getUndoableContainer().clear();
				}
				catch (UserSessionException e) {
					//
					// If anything went wrong, we rollback - this will also inform the user.
					//
					TransactionHelper.rollbackTransaction();
					Environment.getExceptionDisplay().displayException(e, true);
				}
				catch (Exception e) {
					//
					// If anything went wrong, we rollback - this will also inform the user.
					//
					TransactionHelper.rollbackTransaction();
					Environment.getExceptionDisplay().displayException(e, true);
				}
			}
		}
		finally {
			//
			// Unlock what we took out earlier.
			//
			LockableHelper.atomicUnlock(m_lockedObjects);
		}
		return editOk;
	}

	@Override
	protected boolean checkCache()
	{
		return false;
	}

	/**
	 * Action will be enabled by default - will constrain where possible
	 *
	 * @return true if it is enabled
	 */
	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		return getController().getCapletModel().isEditable() && getOperand() != null && super.isEnabled();
	}

	/**
	 * Gets the ActionUIClass attribute of the ReplaceSharedCompositeSymbolAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return ReplaceSharedCompositeSymbolActionUI.class.getName();
	}

	/**
	 * Helper to get the single symbol def from a shared object - given the action, there will be only one - so it is
	 * not applicable as a general utility.
	 *
	 * @param spl The shared pin list to look at
	 *
	 * @return The symbol def (if none, will return null)
	 */
	protected static ISymbolDef getSingleSymbol(ISharedPinList spl)
	{
		return spl.getSymbols().getNext();
	}
}

