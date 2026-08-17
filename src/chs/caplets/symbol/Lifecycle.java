/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2023 Siemens
 */
package chs.caplets.symbol;

import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.IFIB;
import chs.caf.ISymbolChangeListener;
import chs.caf.IWindowMgr;
import chs.caf.SymbolChangeEvent;
import chs.caf.caplet.CapletViewIterator;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.ILifecycleType;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caf.caplet.helpers.FileTypeHolder;
import chs.caf.caplet.helpers.GridScaleSettings;
import chs.caf.caplet.helpers.LifecycleTypeHolder;
import chs.caf.helpers.ui.common.ProjectAndSymbolTreeNodeIconProvider;
import chs.caf.helpers.ui.std.UIManager;
import chs.capitalmanager.appserver.ILockInfo;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.shared.BaseLifecycle;
import chs.caplets.shared.BorderCreationParameterHolder;
import chs.caplets.shared.StampCreationParameterHolder;
import chs.caplets.shared.UserActionFailureReason;
import chs.cof.draw.IColor;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.ISheet;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IPropertiedText;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.project.folder.FolderMgrEditException;
import chs.cof.project.folder.IFolder;
import chs.cof.project.folder.INormalFolder;
import chs.cof.project.folder.ISymbolNode;
import chs.cof.project.folder.ISymlibFolderMgr;
import chs.cof.security.IDomain;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.ILibraryAccessConfiguration;
import chs.cof.symbol.IPSMStamp;
import chs.cof.symbol.IPSMSymbolDef;
import chs.cof.symbol.ISheetAdapter;
import chs.cof.symbol.ISkeletonizePreventor;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.IStampIterator;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolLibrary;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.SymbolLibraryTypeEnum;
import chs.cof.symbol.SymbolScaleTypeEnum;
import chs.cof.symbol.SymbolSubTypeEnum;
import chs.cof.symbol.SymbolTypeEnum;
import chs.cof.symbol.drivers.DriverFactory;
import chs.cofUtils.SymbolGraphicUtils;
import chs.cofUtils.importer.symbolimporthelper.SymbolImportHelper;
import chs.cog.ICOGLockable;
import chs.common.IExtent;
import chs.common.INamedUIDObject;
import chs.common.IPendingModification;
import chs.common.IReadOnlyNamedObject;
import chs.common.ISystemPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDProvider;
import chs.common.IUnit;
import chs.common.attr.IEnumAttribute;
import chs.common.tags.impl.BooleanWithReason;
import chs.common.validation.ValidationHelper;
import chs.ctf.caf.ui.DomainPanelManager;
import chs.ctf.caf.ui.MoveSymbolDialog;
import chs.ctf.caf.ui.PropertyOptionPane;
import chs.ctf.caf.utils.DuplicateMoveSymbolAddRemoveHelper;
import chs.ctf.caf.utils.DuplicateMoveSymbolHelper;
import chs.ctf.caf.utils.DuplicateSymbolAddRemoveHelper;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.caf.utils.MoveSymbolAddRemoveHelper;
import chs.ctf.caf.utils.SymbolLibraryLockRefreshHelper;
import chs.ctf.caf.utils.SymbolLockAndRefreshHelper;
import chs.ctf.caf.utils.SymbolTargetLibrariesFetch;
import chs.ctf.ui.form.RenameDialog;
import chs.ctf.ui.form.SymbolLibraryRenameDlg;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.AppInfo;
import chs.utilities.CHSConstants;
import chs.utilities.CHS_unwind_error;
import chs.utilities.CapabilityHelper;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.IBoundaryTransactionMarshaller;
import chs.utilities.LifecycleUtils;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.SupportedFeatureInfo;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.ICloseAllContext;
import chs.utilities.ui.messaging.IMessagingChoices;
import chs.utilities.ui.messaging.MessagingResourceReader;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utilities.ui.messaging.impl.DefaultQuestionPromptSeverityProvider;
import chs.utility.DomainPermissionEnum;
import chs.utility.SymbolUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.gfx.IDrawingComponentOwner;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.ISymbolModel;
import chs.utility.persist.DataStorageHelper;
import chs.utility.persist.LockableHelper;
import chs.utility.persist.PersistPayload;
import chs.utility.persist.SymbolLibraryStorageHelper;
import chs.utility.symbol.LibraryLockRefreshStatus;
import chs.utility.task.ITask;
import chs.utility.ui.CommonMessages;
import chs.utility.ui.HTMLHelper;
import chs.utility.ui.SymbolErrorDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Lifecycle extends CapletLifecycleHelper implements ISymbolChangeListener, ISkeletonizePreventor
{

	@NotNull private ICaplet m_caplet;
	@NotNull protected Map<IUID, ModelMapData> m_models;
	private boolean m_programmaticCloseWindow = false;

	private static final String OPEN = "Open";
	@Nullable private Icon m_icon;

	public Lifecycle(@NotNull ICaplet caplet)
	{
		m_caplet = caplet;
		m_models = new HashMap<IUID, ModelMapData>();
		init();

		// Register ourselves for symbol change notification
		getFIB().getSymbolLibraryMgr().addSymbolChangeListener(this);
	}

	protected void init()
	{
		//
		// Add the file types.
		//
		FileTypeHolder xml = new FileTypeHolder(ResourceMgr.getString(Lifecycle.class, "Lifecycle.SymbolFile.Title"),
				"xml", "application/x-CapitalSymbol-xml");
		addFileTypeForOpen(xml);
		addFileTypeForSave(xml);

		// Add the Project Types
		//
		// New Types
		addTypeForNew(getLifecycleType(ISymbolLibrary.class, "Lifecycle.NewSymbol."));
		addTypeForNew(getLifecycleType(INormalFolder.class, "Lifecycle.NewSymbol.", ISymbolLibrary.class));

		// Open Types
		addTypeForOpen(getLifecycleType(ISymbolDef.class, "Lifecycle.Open."));

		// Delete Types
		addTypeForDelete(getLifecycleType(ISymbolLibrary.class, "Lifecycle.Delete."));
		addTypeForDelete(getLifecycleType(ISymbolDef.class, "Lifecycle.Delete."));

		// Convert to physical scale.
		addTypeForEdit(getLifecycleType(ISymbolLibrary.class, "Lifecycle.EditSymbolLibrary."));
		addTypeForRename(getLifecycleType(ISymbolDef.class, "Lifecycle.Rename."));

		// Move Types
		addTypeForMove(getLifecycleType(ISymbolDef.class, "Lifecycle.Move."));

		// Duplicate Types
		addTypeForDuplicate(getLifecycleType(ISymbolDef.class, "Lifecycle.Duplicate."));
	}

	private static ILifecycleType getLifecycleType(Class<?> actionClass, String prefix, @Nullable Class<?> ancestor)
	{
		return new LifecycleTypeHolder(actionClass,
				ResourceMgr.getString(Lifecycle.class, prefix + "Label"),
				ResourceMgr.getMnemonic(Lifecycle.class, prefix + "mnemonic"),
				ancestor,
				null);
	}

	private static ILifecycleType getLifecycleType(Class<?> actionClass, String prefix)
	{
		return getLifecycleType(actionClass, prefix, null);
	}

	@NotNull protected final IFIB getFIB()
	{
		return m_caplet.getFIB();
	}

	@Nullable
	protected Model createNewModel(IAbstractLibrary symbolLib, IStamp stampForWhichNewModelToBeCreated)
	{
		//
		// When we create the symboldef, make sure the library is locked.
		//

		IStamp stamp = stampForWhichNewModelToBeCreated;
		if (stamp == null) {
			return null;
		}
		// See if we already have a model for this symbol def, and
		// if so just return it. We only want one model/controller
		// per symbol def.
		if (m_models.containsKey(stamp.getUID())) {
			ModelMapData mmd = m_models.get(stamp.getUID());
			return mmd.geModel();
		}
		//
		// Ensure loaded.
		//

		if (!stamp.isLoadedInMemory()) {
			stamp = stamp.getContainerLibrary().loadFully(stamp);
		}

		// Create a new controller (which will create a model)
		Controller sc = createController(m_caplet, symbolLib, stamp);
		Model sm = (Model) sc.getCapletModel();

		// Put the model in the map so we can find it next time
		ModelMapData mmd = new ModelMapData(symbolLib, stamp, sm);
		m_models.put(stamp.getUID(), mmd);

		return sm;
	}

	protected Controller createController(ICaplet caplet, IAbstractLibrary library, IStamp symbol)
	{
		return new Controller(caplet, library, symbol, getLibraryEditableStatus(library).isValid());
	}

	@Override @Nullable public Icon getDiagramTabIcon()
	{
		return m_icon;
	}

	@Override public void setDiagramTabIcon(@Nullable Icon icon)
	{
		m_icon = icon;
	}

	public boolean createNew(List<?> context)
	{
		// Create a new Symbol
		// The context should have an ISymbolLibrary.

		// First make sure the context has one
		int numContext = context.size();
		if (numContext < 1) {
			throw new IllegalArgumentException("Wrong context for createNew()");
		}

		// First element in context should be the ISymbolLibrary
		//
		IAbstractLibrary symbolLib;
		if (context.get(0) instanceof IAbstractLibrary) {
			symbolLib = (IAbstractLibrary) context.get(0);
		}
		else {
			throw new IllegalArgumentException("Wrong context for createNew()");
		}
		//
		// Make sure we have/can lock the library.
		//
		boolean preLocked = symbolLib.isLocked();

		String newNameSelected = null;
		LibraryLockRefreshStatus lockRefreshStatus = null;
		UserActionFailureReason userActionFailureReason = null;
		try {
			List<IAbstractLibrary> libChanged = new ArrayList<IAbstractLibrary>(1);
			lockRefreshStatus = refreshAndCheckExistence(symbolLib, libChanged);
			if (lockRefreshStatus.isSuccessful()) {
				updateSymbolBrowser(libChanged);

				StampCreationParameterHolder creationParamHolder = getCreationParameterHolder(symbolLib);
				if (creationParamHolder == null) {
					return false;
				}
				creationParamHolder.collectParamsForCreation(getDialogFrame());
				newNameSelected = creationParamHolder.getName();
				boolean isCancelled = creationParamHolder.canceledAction();
				if (isCancelled) {
					return false;
				}
				lockRefreshStatus = lockAndRefresh(symbolLib);
				if (!lockRefreshStatus.isSuccessful()) {
					return false;
				}
				userActionFailureReason = creationParamHolder.validateCreationParameters();
				if (userActionFailureReason != null) {
					return false;
				}

				newNameSelected = creationParamHolder.getName();
				IPSMStamp symbolDef = creationParamHolder.createStampBasedOnParameters();

				if (symbolDef == null) {
					return false;
				}

				ISymlibFolderMgr folderMgr = symbolLib.getFolderMgr();
				if (numContext > 1) {
					Object obj = context.get(numContext - 1);
					if (obj instanceof IFolder) {
						try {
							// if obj exists, create symbol under it, else create under the library
							if (UIDMgr.getObject(((IUIDProvider) obj).getUID()) != null) {
								folderMgr.createSymbolNode((IFolder) obj, symbolDef);
							}
							else {
								ISymbolNode node = folderMgr.createSymbolNode(symbolDef);
								folderMgr.addChild(node);
							}
						}
						catch (FolderMgrEditException e) {
							throw new IllegalStateException("Wrong context for createNew() - Can't find folder " +
									((IReadOnlyNamedObject) obj).getName() + ": " + e.getLocalizedMessage(), e);
						}
					}
				}
				else {
					ISymbolNode node = folderMgr.createSymbolNode(symbolDef);
					folderMgr.addChild(node);
				}

				Model model = openSymbolDef(symbolLib, symbolDef, false);

				if (model == null) {

					return false;
				}

				// Tell the project manager that we have edited the project, This may create folders
				getFIB().getSymbolLibraryMgr().symbolLibraryEdited(symbolLib, context);

				// Disable the Save Event so the Symbol Saved audit event will not be posted
				// while creating a new symbol
				ISystemPreferenceMgr prefMgr =
						(ISystemPreferenceMgr) FactoryMgr.getSystemFactory().getCHSSystem().getSystemData()
								.getPreferences();
				boolean isSymbolSavedEventEnabled =
						prefMgr.isAuditableEventEnabled(AuditableEventType.SYMBOL_SAVED);
				prefMgr.setAuditableEventEnabled(AuditableEventType.SYMBOL_SAVED, false);

				// Save the symbolLibrary, must be after symbolLibraryEdited, as save does a validation which
				// checks for orphaned symbols. i.e symbol not in any folders. The call above can create the
				// folders
				IPropertiedText noSymbolContent = null;
				if (symbolDef instanceof ISymbolDef) {

					noSymbolContent = createNoSymbolContentProptext(((IGriddable) model.getSheet()).getGrid());
					((ISymbolDef) symbolDef).getPinList().addObject(noSymbolContent);
				}
				saveInternal(symbolLib, Collections.singleton(symbolDef));

				if (!symbolDef.lock()) {
					return false;
				}

				if (symbolDef instanceof IPSMSymbolDef) {
					((ISymbolDef) symbolDef).getPinList().removeObject(
							noSymbolContent);
				}
				if (symbolDef instanceof IPendingModification) {
					((IPendingModification) symbolDef).setModified(false);
				}

				//save(symbolLib, SaveContext.SAVE, false);

				// Restore the value of the Symbol saved audit event
				prefMgr.setAuditableEventEnabled(AuditableEventType.SYMBOL_SAVED, isSymbolSavedEventEnabled);

				return true;
			}
		}
		finally {
			unlockLibrary(symbolLib, preLocked);
			if (lockRefreshStatus != null && !lockRefreshStatus.isSuccessful()) {
				reportLibraryLockRefreshFailure(symbolLib, lockRefreshStatus,
						SymbolErrorDialog.UserAction.CreateSymbol);
			}
			reportUserOperationFailure(null, symbolLib, newNameSelected, userActionFailureReason,
					SymbolErrorDialog.UserAction.CreateSymbol);
		}
		return false;
	}

	protected IPropertiedText createNoSymbolContentProptext(IGrid grid)
	{
		IExtent bounds = FactoryMgr.getCommonFactory()
				.constructExtent(0, 0, 10 * grid.getGridSpacing(), 2 * grid.getGridSpacing());
		String message = ResourceMgr.getString(Lifecycle.class,
				"Lifecycle.nosymbolcontent");
		IPropertiedText noSymbolContent = FactoryMgr.getDrawPlusFactory().constructPropertiedText(
				FactoryMgr.getCommonFactory().createUID(), 0, 0, bounds,
				TextHelper.getDefaultHeight(grid), 0, message);

		return noSymbolContent;
	}

	@NotNull private LibraryLockRefreshStatus refreshAndCheckExistence(@NotNull IAbstractLibrary library,
			@NotNull List<IAbstractLibrary> libChanged)
	{
		LibraryLockRefreshStatus lockRefreshStatus = SymbolLibraryLockRefreshHelper.refreshAndCheckExistence(library);
		if (lockRefreshStatus == LibraryLockRefreshStatus.RefreshSuccessful) {
			libChanged.add(library);
		}
		return lockRefreshStatus;
	}

	@NotNull protected LibraryLockRefreshStatus lockAndRefresh(@NotNull IAbstractLibrary library)
	{
		return lockAndRefresh(library, new ArrayList<>());
	}

	@NotNull private LibraryLockRefreshStatus lockAndRefresh(@NotNull IAbstractLibrary library,
			@NotNull List<IAbstractLibrary> libChanged)
	{
		LibraryLockRefreshStatus lockRefreshStatus = SymbolLibraryLockRefreshHelper.safeLockAndRefresh(library);
		if (lockRefreshStatus == LibraryLockRefreshStatus.RefreshedAndLockSuccessful) {
			libChanged.add(library);
		}
		return lockRefreshStatus;
	}

	private boolean lockAllSymbolsAndRefresh(@NotNull IAbstractLibrary symbolLib, List<IAbstractLibrary> libChanged,
			SymbolErrorDialog.UserAction userAction)
	{
		List<INamedUIDObject> lockFailedObjects = new ArrayList<INamedUIDObject>(1);
		LibraryLockRefreshStatus status =
				SymbolLibraryLockRefreshHelper.lockIncludingSymbols(symbolLib, lockFailedObjects);
		if (!status.isSuccessful()) {
			reportLockRefreshFailure(lockFailedObjects, symbolLib, status, userAction);
			return false;
		}
		if (status == LibraryLockRefreshStatus.RefreshedAndLockSuccessful) {
			libChanged.add(symbolLib);
		}
		return true;
	}

	protected void updateSymbolBrowser(List<IAbstractLibrary> libChanged)
	{
		for (IAbstractLibrary changedLib : libChanged) {
			List<IAbstractLibrary> newContext = new ArrayList<IAbstractLibrary>(1);
			newContext.add(changedLib);
			getFIB().getSymbolLibraryMgr().symbolLibraryEdited(changedLib, newContext);
		}
	}

	private boolean checkLibraryExists(IAbstractLibrary symbolLib)
	{
		// Check if library deleted underneath us.
		return LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), symbolLib);
	}

	/**
	 * Opens a SymbolDef.  Create a model and a view for the symbolDef.
	 *
	 * @param symbolLib   the library
	 * @param symbolDef   the symbol to open
	 * @param alreadyopen the symbol is already open in another window -> set this window current
	 * @return the model
	 */
	@Nullable
	private Model openSymbolDef(IAbstractLibrary symbolLib, IPSMStamp symbolDef, boolean alreadyopen)
	{
		Model model;

		if (alreadyopen) {
			ModelMapData mmd = m_models.get(symbolDef.getUID());
			model = mmd.geModel();
		}
		else {
			// Create a new Model, then create a view on it.
			setDiagramTabIcon(ProjectAndSymbolTreeNodeIconProvider.getSymbolIcon(symbolDef));
			
			model = createNewModel(symbolLib, symbolDef);
		}

		if (model != null) {
			symbolDef.addSkeletonizePrevention(this);

			ICapletWindow cw = CAFUtils.getInstance().getCapletWindowForDiagram(model.getDiagram());
			if (cw == null) {

				// Create the window first and set the layout
				IWindowMgr wm = getFIB().getWindowMgr();
				cw = wm.createCapletWindow(m_caplet, model.getController());
				cw.setTitle(symbolLib.getName() + ':' + symbolDef.getName());
				cw.getContainer().setLayout(new GridLayout(1, 1, 4, 4));

				// Create the view and tell it about the sheet
				View dv = createView(model, cw);
				dv.setDiagram(model.getDiagram());
				dv.setName(symbolDef.getName());
				// Set the new window as active
				cw.display();

				// FEAT3184 - Object Model Integrity we should validate the symbol that was loaded.
				ValidationHelper.validateAfterLoad(model);
			}
			else {
				// window allready exists, activate it. This will bring it to the front.
				cw.activate();
			}
		}
		return model;
	}

	/**
	 * Open a window on the existing schematic
	 */
	public boolean openExisting(List<?> context)
	{
		// Open a symbolDef
		// The context should have an ISymbolLibrary and an ISymbolDef

		// First make sure the context has two items
		// in it.
		int numContext = context.size();
		if (numContext < 2) {
			String problematicUID = StringUtils.BLANK;
			if (numContext == 1) {
				problematicUID = ((IUIDProvider) context.get(0)).getUID().getString();
			}
			throw new IllegalArgumentException("Wrong context for openExisting() " + problematicUID);
		}

		// First element in context should be the symbol library
		//
		IAbstractLibrary symbolLib;
		if (context.get(0) instanceof IAbstractLibrary) {
			symbolLib = (IAbstractLibrary) context.get(0);
		}
		else {
			throw new IllegalArgumentException("Wrong context for openExisting()");
		}

		// The last element in the context should be a symbol def.
		IPSMStamp symbolDef;
		if (context.get(numContext - 1) instanceof IPSMStamp) {
			symbolDef = (IPSMStamp) context.get(numContext - 1);
		}
		else {
			throw new IllegalArgumentException("Wrong context for openExisting()");
		}

		if (!canBeUsedForAppSuite(symbolDef, OPEN)) {
			return false;
		}

		// If symbol is already loaded, it might not exist yet in db but the window can still be activated
		boolean alreadyOpen = m_models.get(symbolDef.getUID()) != null;

		//
		// When we open the symboldef, make sure the library is locked.
		//

		LibraryLockRefreshStatus refreshStatus = null;
		UserActionFailureReason failureReason = null;
		String symname = symbolDef.getName();
		BooleanWithReason libraryEditable = null;
		try {

			if (!alreadyOpen) {
				List<IAbstractLibrary> libChanged = new ArrayList<IAbstractLibrary>(1);
				refreshStatus = refreshAndCheckExistence(symbolLib, libChanged);
				if (!refreshStatus.isSuccessful()) {
					return false;
				}
				updateSymbolBrowser(libChanged);
				libraryEditable = getLibraryEditableStatus(symbolLib);
				//
				// Explicitly load this... [load it as propertied gfx though]
				//

				symbolDef = (IPSMStamp) UIDMgr.getObject(symbolDef.getUID());
				if (symbolDef != null) {
					// get the container for this symbol...

					failureReason = findObjectExistsInLibrary(symbolDef, symbolLib);
					if (failureReason != null) {
						IStamp stampMatchingName = symbolLib.findExistingSymbol(symbolDef.getName());
						if (stampMatchingName != null && stampMatchingName instanceof IPSMStamp) {
							failureReason = null;
							symbolDef = (IPSMStamp) stampMatchingName;
						}
						else {
							return false;
						}
					}

					if (libraryEditable.isValid() && !symbolDef.lock()) {
						failureReason = UserActionFailureReason.STAMPLOCKERROR;
						return false;
					}

					if (symbolDef.isSkeleton()) {
						symbolLib.loadFully(symbolDef);
					}
					else {
						symbolDef.refresh();
					}
				}

				//
				// Symbol could not be loaded - possibly deleted...
				//
				// DR 407816: When loadFully() fails we get back the original skeleton symbol. Must now trigger
				// open failure.
				if (symbolDef == null || !symbolDef.isLoadedInMemory()) {
					// If the Library wasn't locked before - unlock it

					failureReason = UserActionFailureReason.STAMPCANNOTBELOADED;

					return false;
				}
			}

			Model model = openSymbolDef(symbolLib, symbolDef, alreadyOpen);

			return model != null;
		}

		finally {
			if (refreshStatus != null && !refreshStatus.isSuccessful()) {
				reportLibraryLockRefreshFailure(symbolLib, refreshStatus, SymbolErrorDialog.UserAction.OpenSymbol);
			}
			if (failureReason != null) {
				if (symbolDef != null && symbolDef.isLocked()) {
					symbolDef.unlock();
				}
				reportUserOperationFailure(symbolDef, symbolLib, symname, failureReason,
						SymbolErrorDialog.UserAction.OpenSymbol);
			}
			else {
				if (libraryEditable != null && !libraryEditable.isValid()) {
					sendMessageToOutputWindow(libraryEditable.getReason());
				}
			}
		}
	}

	private boolean canBeUsedForAppSuite(IPSMStamp symbolDef, String context)
	{
		boolean isFunctionSymbol =
				symbolDef instanceof ISymbolDef
						&& SymbolUtils.isFunctionSymbol((ISymbolDef) symbolDef);

		if (isFunctionSymbol && !CapabilityHelper.supports(SupportedFeatureInfo.Feature.FUNCTION)) {
			CommonMessages.displayErrorCantOpenFunctionSymbol(context);
			return false;
		}
		return true;
	}

	protected void sendMessageToOutputWindow(String message)
	{
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(message);
	}

	@Nullable private UserActionFailureReason findObjectExistsInLibrary(IStamp symbolDef, IAbstractLibrary library)
	{
		if (symbolDef.getContainerLibrary() != library) {
			UserActionFailureReason failureReason = UserActionFailureReason.STAMPDELETEDINANOTHERSERSESSION;

			String objectType = getStampType(symbolDef);
			IUserSession userSession = FactoryMgr.getCHSSystem().getUserSession();
			try {
				if (objectType != null && userSession != null &&
						userSession.objectExists(objectType, symbolDef.getUID().getString())) {
					failureReason = UserActionFailureReason.STAMPMOVEDTOANOTHERSESSION;
				}
			}
			catch (UserSessionException e) {
				//ignore the exception.
			}

			return failureReason;
		}
		return null;
	}

	protected View createView(Model model, ICapletWindow window)
	{

		return new View(model, window);
	}

	public void createNewWindow(ICAFWindow window)
	{

		IWindowMgr wm = getFIB().getWindowMgr();
		if (window instanceof ICapletWindow) {
			ICapletWindow cw = (ICapletWindow) window;
			ICapletController sc = cw.getController();
			Model sm = (Model) sc.getCapletModel();
			if (sm!= null) {
				setDiagramTabIcon(ProjectAndSymbolTreeNodeIconProvider.getSymbolIcon(sm.getSymbolDef()));
			}
			ICapletWindow newwin = wm.createCapletWindow(m_caplet, sc);
			assert sm != null;
			newwin.setTitle(sm.getLibrary().getName() + ':' + sm.getSymbolDef().getName());
			newwin.getContainer().setLayout(new GridLayout(1, 1, 4, 4));

			// Create the view in the window
			View sv = createView(sm, newwin);
			sv.setDiagram(sm.getDiagram());
			sv.setName(sm.getSymbolDef().getName());

			// Set the new window as active
			newwin.display();
		}
	}

	public void saveChanges(ICapletController controller)
	{
		IAbstractLibrary root = getFIB().getSymbolLibraryMgr().getCurrentLibrary();
		for (ModelMapData mmd : m_models.values()) {
			if (mmd.geModel() == controller.getCapletModel()) {
				root = mmd.getLibrary();
			}
		}
		save(root, SaveContext.SAVE, false);
	}

	@Nullable @Override
	public ITask save(IUIDObject root, IUIDObject sub, boolean runDRCs)
	{
		if (sub == null) {
			return null;
		}
		// FEAT3184 - Object Model Integrity
		// we should validate the symbols before save.
		ValidationHelper.validateBeforeSave(sub);

		IPSMStamp stamp = (IPSMStamp) sub;
		Set<IPSMStamp> stampSet = new HashSet<>(1);
		stampSet.add(stamp);

		// Check stamp warnings before saving
		StringBuilder duplicatePinBuffer = new StringBuilder();
		StringBuilder duplicatePortBuffer = new StringBuilder();
		StringBuilder danglingLinkBuffer = new StringBuilder();
		StringBuilder outOfDateBlockBuffer = new StringBuilder();
		validateStamp(stamp, duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer, outOfDateBlockBuffer);
		boolean isStampModified = prepareStampForSave((IAbstractLibrary) root, duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer,
				outOfDateBlockBuffer,
				stamp);
        if(isStampModified) {
			if (saveInternal((IAbstractLibrary) root, stampSet)) {
				if (m_models.containsKey(stamp.getUID())) {
					ModelMapData stampModel = m_models.get(stamp.getUID());
					resetModel(stampModel.geModel());
				}
			}
		} else {
			sendMessageToOutputWindow(ResourceMgr.getString(BaseLifecycle.class, "BaseLifecycle.diagram.NoChangesToSave.message"));
		}

		reportWarningsForStampSave(duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer, outOfDateBlockBuffer);
		CAFUtils.getInstance().getSymbolLibraryMgr().refreshUI(this);
		return null;
	}

	protected boolean saveInternal(IAbstractLibrary lib, Collection<IPSMStamp> symbols)
	{
		return AbstractLibraryPersistenceHelper.saveSymbols(lib, symbols);
	}

	@Nullable @Override
	public ITask save(IUIDObject root, @NotNull SaveContext context, boolean runDRCs)
	{
		//
		// Save current [redirector...]
		//
		saveLibrary(root);
		return null; // Symbol library saves are fast, they don't need to use tasks.
	}

	private String checkForDuplicatePinNames(IStamp sdef)
	{
		boolean duplicateFound = false;
		StringBuilder buf = new StringBuilder();
		if (sdef instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) sdef;
			Set<String> pinNames = new HashSet<String>();
			for (IBlockIterator it = symDef.getBlocks(); it.hasNext() && !duplicateFound; ) {
				IBlock block = it.getNext();
				if (block.getConnectivity() != null) {
					for (IAbstractPinIterator pinIter = block.getConnectivity().getPins(); pinIter.hasNext(); ) {
						IAbstractPin curPin = pinIter.getNext();
						if (!pinNames.add(curPin.getName())) {
							duplicateFound = true;
							break;
						}
					}
				}
			}
			if (symDef.getConnectivity() != null) {
				for (IAbstractPinIterator pinIter = symDef.getConnectivity().getPins();
						pinIter.hasNext() && !duplicateFound; ) {
					IAbstractPin curPin = pinIter.getNext();
					if (!pinNames.add(curPin.getName())) {
						duplicateFound = true;
					}
				}
			}
		}
		if (duplicateFound) {
			buf.append(sdef.getName());
			buf.append(',');

			return buf.toString();
		}
		return buf.toString();
	}

	private String checkForDanglingLinks(IStamp sdef)
	{
		boolean danglingLinkFound = false;
		StringBuilder buff = new StringBuilder();

		if (sdef instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) sdef;

			if (symDef.getConnectivity() != null && symDef.getConnectivity() instanceof IDevice) {
				for (IInternalLink link : ((IDevice) symDef.getConnectivity()).getInternalLinkCollection()) {
					if (link.getStartPin() == null || link.getEndPin() == null) {
						danglingLinkFound = true;
						break;
					}
				}
			}

			for (IBlockIterator it = symDef.getBlocks(); it.hasNext() && !danglingLinkFound; ) {
				IBlock block = it.getNext();
				if (block.getConnectivity() != null && block.getConnectivity() instanceof IDevice) {
					for (IInternalLink link : ((IDevice) block.getConnectivity()).getInternalLinkCollection()) {
						if (link.getStartPin() == null || link.getEndPin() == null) {
							danglingLinkFound = true;
							break;
						}
					}
				}
			}
		}

		if (danglingLinkFound) {
			buff.append(sdef.getName());
			buff.append(',');
			return buff.toString();
		}
		return buff.toString();
	}

	protected String checkForOutOfDateBlocks(IStamp compositeSymbol)
	{
		StringBuilder symbolName = new StringBuilder();
		if (compositeSymbol instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) compositeSymbol;
			boolean outOfDateBlockFound = false;
			for (IBlockIterator it = symDef.getBlocks(); it.hasNext() && !outOfDateBlockFound; ) {
				IBlock block = it.getNext();
				ISymbolRef instanceSymbolRef = block.getSymbolRef();
				if (instanceSymbolRef != null) {
					ISymbolDef symbolDef = SymbolUtils.getSymbolDef(instanceSymbolRef.getSymbolUID());
					if (symbolDef != null) {
						if (instanceSymbolRef.getTimestamp() < symbolDef.getServerTimeModified()) {
							outOfDateBlockFound = true;
							symbolName.append(symDef.getName());
							symbolName.append(',');
						}
					}
				}
			}
		}
		return symbolName.toString();
	}

	private void saveLibrary(IUIDObject obj)
	{
		// FEAT3184 - Object Model Integrity
		// we should validate the symbols before save.
		ValidationHelper.validateBeforeSave(obj);

		// Save all modified in this library
		IAbstractLibrary currLibrary = (IAbstractLibrary) obj;
		//
		// Get a storage request for the imported project
		//
		// Make sure we have everything loaded before we save out...
		//
		try {
			Set<IStamp> emptySymbols = new HashSet<IStamp>();
			for (ModelMapData mmd : m_models.values()) {
				if (mmd.getLibrary() == currLibrary) {
					if (!mmd.geModel().canBePersisted()) {
						emptySymbols.add(mmd.getSymbol());
					}
				}
			}

			StringBuilder duplicatePinBuffer = new StringBuilder();
			StringBuilder duplicatePortBuffer = new StringBuilder();
			StringBuilder danglingLinkBuffer = new StringBuilder();
			StringBuilder outOfDateBlockBuffer = new StringBuilder();

			Set<IPSMStamp> saveSet =
					calculateSymbolsToBeSaved(currLibrary, duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer,
							outOfDateBlockBuffer);
			Iterator<IPSMStamp> iter = saveSet.iterator();
			while (iter.hasNext()) {
				IPSMStamp stamp = iter.next();
				if (emptySymbols.contains(stamp)) {
					iter.remove();
				}
			}

			boolean success = saveInternal(currLibrary, saveSet);

			if (success) {
				resetModels(currLibrary);
			}

			reportWarningsForStampSave(duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer,
					outOfDateBlockBuffer);
		}
		catch (Exception xcpt) {
			Environment.getExceptionDisplay().displayException(xcpt, false);
			throw new CHS_unwind_error(xcpt);
		}
	}

	protected void reportWarningsForStampSave(StringBuilder duplicatePinBuffer, StringBuilder duplicatePortBuffer,
			StringBuilder danglingLinkBuffer,
			StringBuilder outOfDateBlockBuffer)
	{
		int index = 1;
		String object =
				ResourceMgr.getString(Lifecycle.class, "Lifecycle.Pin");
		StringBuilder warningName = new StringBuilder();
		StringBuilder warningMessages = new StringBuilder();
		if (duplicatePinBuffer.length() > 0) {
			if (duplicatePortBuffer.length() > 0 || danglingLinkBuffer.length() > 0 ||
					outOfDateBlockBuffer.length() > 0) {
				warningMessages.append(index).append(")  ")
						.append(getDuplicatePinWarningMessage(duplicatePinBuffer, object));
			}
			else {
				warningMessages.append(getDuplicatePinWarningMessage(duplicatePinBuffer, object));
			}
			index++;
		}
		if (duplicatePortBuffer.length() > 0) {
			object = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Port");
			if (duplicatePinBuffer.length() > 0 || danglingLinkBuffer.length() > 0 ||
					outOfDateBlockBuffer.length() > 0) {
				warningMessages.append(index).append(")  ")
						.append(getDuplicatePinWarningMessage(duplicatePortBuffer, object));
			}
			else {
				warningMessages.append(getDuplicatePinWarningMessage(duplicatePortBuffer, object));
			}
			index++;
		}

		if (danglingLinkBuffer.length() > 0) {
			if (duplicatePinBuffer.length() > 0 || duplicatePortBuffer.length() > 0 ||
					outOfDateBlockBuffer.length() > 0) {
				warningMessages.append(index).append(") ").append(getDanglingLinkWarningMessage(danglingLinkBuffer));
			}
			else {
				warningMessages.append(getDanglingLinkWarningMessage(danglingLinkBuffer));
			}
			index++;
		}

		if (outOfDateBlockBuffer.length() > 0) {
			if (duplicatePinBuffer.length() > 0 || duplicatePortBuffer.length() > 0 ||
					danglingLinkBuffer.length() > 0) {
				warningMessages.append(index)
						.append(") ")
						.append(getOutOfDateBlockWarningMessage(outOfDateBlockBuffer));
			}
			else {
				warningMessages.append(getOutOfDateBlockWarningMessage(outOfDateBlockBuffer));
			}
			index++;
		}

		if (index > 1) {
			if (index == 2) {
				if (duplicatePinBuffer.length() > 0 || duplicatePortBuffer.length() > 0) {
					warningName.append(ResourceMgr
							.getString(Lifecycle.class, "Lifecycle.SaveDuplicatePinNameWarning.name", object));
					warningMessages
							.append(ResourceMgr
									.getString(Lifecycle.class, "Lifecycle.duplicatePinNameSummary.text"));
				}
				else if (danglingLinkBuffer.length() > 0) {
					warningName
							.append(ResourceMgr.getString(Lifecycle.class, "Lifecycle.SaveDanglingLinkWarning.name"));
					warningMessages
							.append(ResourceMgr
									.getString(Lifecycle.class, "Lifecycle.danglingLinkSummary.text"));
				}
				else if (outOfDateBlockBuffer.length() > 0) {
					warningName
							.append(ResourceMgr.getString(Lifecycle.class, "Lifecycle.SaveOutofdateBlockWarning.name"));
					warningMessages
							.append(ResourceMgr.getString(Lifecycle.class, "Lifecycle.outofdateBlockSummary.text"));
				}
			}
			else {
				warningName.append(ResourceMgr.getString(Lifecycle.class, "Lifecycle.SaveWarning.name"));
				warningMessages.append(ResourceMgr.getString(Lifecycle.class, "Lifecycle.summary.text"));
			}
			MessageHelper.showWarningMessage(getDialogFrame(),
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.SaveWarning.title"),
					warningName.toString(),
					warningMessages.toString());
		}
	}

	private Set<IPSMStamp> calculateSymbolsToBeSaved(IAbstractLibrary currLibrary,
			StringBuilder duplicatePinBuffer, StringBuilder duplicatePortBuffer, StringBuilder danglingLinkBuffer,
			StringBuilder outOfDateBlockBuffer)
	{
		Set<IPSMStamp> saveSet = new HashSet<IPSMStamp>();
		for (IStampIterator sdi = currLibrary.getSymbols(); sdi.hasNext(); ) {
			IStamp sdef = sdi.getNext();
			if (sdef instanceof IPSMStamp &&
					prepareStampForSave(currLibrary, duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer,
							outOfDateBlockBuffer,
							sdef)) {
				saveSet.add((IPSMStamp) sdef);
			}
		}
		return saveSet;
	}

	private void validateStamp(IStamp stamp, StringBuilder duplicatePinBuffer, StringBuilder duplicatePortBuffer,
			StringBuilder danglingLinkBuffer,
			StringBuilder outOfDateBlockBuffer)
	{
		//Check for Duplicates
		if (stamp instanceof ISymbolDef && SymbolUtils.isFunctionSymbol((ISymbolDef) stamp)) {
			duplicatePortBuffer.append(checkForDuplicatePinNames(stamp));
		}
		else {
			duplicatePinBuffer.append(checkForDuplicatePinNames(stamp));
		}
		danglingLinkBuffer.append(checkForDanglingLinks(stamp));
		//Check for out of date blocks
		outOfDateBlockBuffer.append(checkForOutOfDateBlocks(stamp));
	}

	private boolean prepareStampForSave(IAbstractLibrary currLibrary, StringBuilder duplicatePinBuffer,
			StringBuilder duplicatePortBuffer,
			StringBuilder danglingLinkBuffer, StringBuilder outOfDateBlockBuffer, IStamp sdef)
	{
		if (sdef.isEdited()) {
			validateStamp(sdef, duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer, outOfDateBlockBuffer);
			//
			// Update the time...
			//

			// Audit Symbol saved
			String symbolLibraryTypeName = currLibrary.getType().getAsLocaleSpecificName();
			IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
			auditLogger
					.postEvent(AuditableEventType.SYMBOL_SAVED, symbolLibraryTypeName, currLibrary.getUID().getString(),
							sdef.getName(), sdef.getUID().getString());
			return true;
		}
		return false;
	}

	private void resetModels(IAbstractLibrary currLibrary)
	{
		for (IStampIterator sdi = currLibrary.getSymbols(); sdi.hasNext(); ) {
			IStamp sdef = sdi.getNext();

			for (ModelMapData mmd : m_models.values()) {
				if (mmd.getSymbolUID().isEquiv(sdef.getUID())) {
					//
					Model currmod = mmd.geModel();
					resetModel(currmod);
					break;
				}
			}
		}
	}

	private void resetModel(ICapletModel currmod)
	{
		if (currmod.isModified()) {
			currmod.setModified(false);
		}
		//
		// Clear the undo queue after save.
		//
		currmod.getController().getUndoableContainer().clear();
	}

	private String getDuplicatePinWarningMessage(StringBuilder danglingLinkBuffer, String object)
	{
		Collection<String> danglingLinkSyms = StringUtils.splitStringintoList(danglingLinkBuffer.toString());

		String danglingLinkMessage =
				ResourceMgr.getString(Lifecycle.class, "Lifecycle.duplicatePinName.text", danglingLinkSyms.toString(),
						object);
		LogHelper.appMsg(danglingLinkMessage);

		return danglingLinkMessage;
	}

	private String getDanglingLinkWarningMessage(StringBuilder danglingLinkBuffer)
	{
		Collection<String> danglingLinkSyms = StringUtils.splitStringintoList(danglingLinkBuffer.toString());

		String danglingLinkMessage =
				ResourceMgr.getString(Lifecycle.class, "Lifecycle.danglingLink.text", danglingLinkSyms.toString());
		LogHelper.appMsg(danglingLinkMessage);

		return danglingLinkMessage;
	}

	private String getOutOfDateBlockWarningMessage(StringBuilder outOfDateBlockBuffer)
	{
		Collection<String> outOfDateBlockSyms = StringUtils.splitStringintoList(outOfDateBlockBuffer.toString());

		String outOfDateBlockMessage =
				ResourceMgr.getString(Lifecycle.class, "Lifecycle.outofdateBlock.text", outOfDateBlockSyms.toString());
		LogHelper.appMsg(outOfDateBlockMessage);

		return outOfDateBlockMessage;
	}

	public void discard(IUIDObject root)
	{
		IAbstractLibrary slib = (IAbstractLibrary) root;
		for (ModelMapData mmd : new ArrayList<ModelMapData>(m_models.values())) {
			if (mmd.getLibrary() == slib) {
				//
				//
				mmd.setSymbolEdited(false);
				mmd.geModel().setModified(false);
			}
		}
	}

	public boolean closingController(ICapletController controller)
	{
		if (m_programmaticCloseWindow) {
			return true;
		}

		Model model = (Model) controller.getCapletModel();
		IPSMStamp stamp = model.getSymbolDef();
		IAbstractLibrary library = model.getLibrary();

		ICapletWindow capWindow = findFirstSymbolWindow(stamp);
		List<ICapletWindow> windowsForLibrary = CAFUtils.getInstance().getCapletWindowsForLibrary(library);
		boolean isLastWindow = (windowsForLibrary.size() == 1);
		ICloseAllContext closeAllActionContext = CAFUtils.getInstance().getCloseContext();
		if (model.isModified() && hasEditPermission(library)) {
			SaveStampOptions saveOption;
			if (closeAllActionContext != null && !closeAllActionContext.promptMessageForUnSavedDesign()) {
				saveOption = SaveStampOptions.DISCARDANDCLOSE;
			}
			else {
				saveOption = doPrompt(stamp);
			}

			if (saveOption == SaveStampOptions.DISACRD) {
				return false;
			}
			if (saveOption == SaveStampOptions.DISCARDANDCLOSE) {
				model.getController().getSelectMgr().getCurrentSelections().clear();
				CreationDeletionHelper.getTheCreationHelper().clear();
				rollbackEditsAndClearUndo(model.getController());
			}
			else if (saveOption == SaveStampOptions.SAVEANDCLOSE) {
				if (closeAllActionContext != null && !closeAllActionContext.promptMessageForUnSavedDesign()) {
					return false;
				}
				StringBuilder duplicatePinBuffer = new StringBuilder();
				StringBuilder duplicatePortBuffer = new StringBuilder();
				StringBuilder danglingLinkBuffer = new StringBuilder();
				StringBuilder outOfDateBlockBuffer = new StringBuilder();

				if (model.isModified()) {
					model.setTransitional(false);
				}
				model.getController().getSelectMgr().getCurrentSelections().clear();
				CreationDeletionHelper.getTheCreationHelper().clear();
				preSaveSymbol(model, stamp);
				if (model.canBePersisted() &&
						prepareStampForSave(library, duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer,
								outOfDateBlockBuffer,
								stamp)) {
					if (saveInternal(model.getLibrary(), Collections.singleton(stamp))) {
						reportWarningsForStampSave(duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer,
								outOfDateBlockBuffer);
					}
				}
			}
			CAFUtils.getInstance().getSymbolLibraryMgr()
					.symbolLibraryEdited(library, Collections.singletonList(library));
		}

		resetModel(model);
		closeWindowOfModel(capWindow);

		AbstractLibraryPersistenceHelper.unlockSymbols(Collections.singleton(stamp.getUID()));

		if (capWindow != null &&
				!CAFUtils.getInstance().getWindowMgr().hasOtherWindowsForModel(capWindow, controller)) {
			unloadSymbols(Collections.singleton(stamp.getUID()));
		}

		//(SP1202)dts0100798070 Capital Symbol does not remove the �lock file� for a given symbol library until the application is terminated
		if (isLastWindow && LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), library)) {
			LockUpdateHelper.unlock(library);
		}

		return true;
	}

	private void preSaveSymbol(Model model, IPSMStamp stamp)
	{
		if (stamp instanceof ISymbolDef &&
				(!model.canBePersisted() || SymbolGraphicUtils.isSymbolEmpty(stamp))) {

			IPropertiedText noSymbolContent =
					createNoSymbolContentProptext(((IGriddable) model.getSheet()).getGrid());
			((ISymbolDef) stamp).getPinList().addObject(noSymbolContent);
		}
	}

	/*public boolean closingController(ICapletController controller)
	{
		// Called when the controller is closing down (e.g. if the window 'x' button is pressed.)
		// In that flow WE should ONLY get in here if we are closing the last symbol in the library
		// However we can also arrive here when closeWindow() is called programatically
		// In this context the user does not want to be prompted hence we set the modified flag to false
		// and then restore this again after the call to closeWindow.
		if (m_programmaticCloseWindow) {
			return true;
		}

		// we _MUST_ check modified status of all symbols  matching the library
		Model model = (Model) controller.getCapletModel();
		IAbstractLibrary library = model.getLibrary();

		// Retrieve models which cannot be peristed - e.g. empty symbols
		Set<Model> nonPersistableModels = new HashSet<Model>();
		for (ModelMapData mmd : m_models.values()) {
			if (mmd.m_library == library) {
				if (!mmd.m_model.canBePersisted()) {
					nonPersistableModels.add(mmd.m_model);
				}
			}
		}

		boolean doSaveLibrary = false;

		Collection<IUID> unloadSymbols = new LinkedList<IUID>();
		if (!isModified(library)) {
			collectSymbolsToUnload(library, unloadSymbols, nonPersistableModels);
		}
		else {
			switch (doPrompt(library)) {
				case JOptionPane.CANCEL_OPTION:
				case JOptionPane.CLOSED_OPTION:
					return false;

				case JOptionPane.NO_OPTION:
					//
					// Undo all the changes (back to year dot) for the controller.
					//
					for (ModelMapData mmd : m_models.values()) {
						if (mmd.m_library == library) {
							mmd.m_model.getController().getSelectMgr().getCurrentSelections().clear();
							rollbackEditsAndClearUndo(mmd.m_model.getController());
							if (!mmd.m_model.canBePersisted()) {
								// Record empty models as result of rollback
								nonPersistableModels.add(mmd.m_model);
							}
						}
					}
					collectSymbolsToUnload(library, unloadSymbols, nonPersistableModels);
					break;

				default:
					//
					// The save below will reset the modified status of the model
					//
					collectSymbolsToUnload(library, unloadSymbols, nonPersistableModels);
					for (ModelMapData mmd : m_models.values()) {
						if (mmd.m_library == library) {
							if (mmd.m_model.isModified()) {
								mmd.m_model.setTransitional(false);
							}
							mmd.m_model.getController().getSelectMgr().getCurrentSelections().clear();
						}
					}
					doSaveLibrary = true;
					break;
			}
		}

		if (doSaveLibrary || !nonPersistableModels.isEmpty()) {
			// Remove the empty symbols
			for (Model mm : nonPersistableModels) {
				deleteSymbol(library, mm.getSymbolDef(), true);
			}
			save(library, SaveContext.SAVE, false);
		}

		for (IUID symbolsToUnload : unloadSymbols) {
			IUIDObject stamp = UIDMgr.getObject(symbolsToUnload);
			if (stamp instanceof IStamp) {
				closeWindowOfModel((IStamp) stamp);
			}
		}
		AbstractLibraryPersistenceHelper.unlockSymbols(unloadSymbols);
		unloadSymbols(unloadSymbols);

		CAFUtils.getInstance().getSymbolLibraryMgr().symbolLibraryEdited(library, Collections.singletonList(library));

		//(SP1202)dts0100798070 Capital Symbol does not remove the �lock file� for a given symbol library until the application is terminated
		if (LockableHelper.objectExists(CAFUtils.getInstance().getUserSession(), library)) {
			LockUpdateHelper.unlock(library);
		}
		return true;
	}*/

	private void closeWindowOfModel(@Nullable ICapletWindow window)
	{
		try {
			m_programmaticCloseWindow = true;

			if (window != null) {
				window.closeWindow();
			}
		}
		finally {
			m_programmaticCloseWindow = false;
		}
	}

	protected void destroyModel(Model model)
	{
		model.getController().destroy();
		model.destroy();
	}

	protected Map<IUID, ModelMapData> getModelMaps()
	{
		return m_models;
	}

	private void unloadSymbols(Collection<IUID> unloadSymbols)
	{
		for (IUID uid : unloadSymbols) {
			ModelMapData data = getModelMaps().get(uid);
			if (data == null) {
				continue;
			}
			IPSMStamp iStamp = data.geModel().getSymbolDef();

			if (iStamp != null) {
				iStamp.skeletonizeUnlessPrevented(this);
			}

			destroyModel(data.geModel());
			m_models.remove(uid);
		}
	}

	protected enum SaveStampOptions
	{
		DISACRD,
		SAVEANDCLOSE,
		DISCARDANDCLOSE
	}

	protected SaveStampOptions doPrompt(IStamp stamp)
	{

		String type = ResourceMgr.getString(Lifecycle.class, "Lifecycle.closingsymbol.symboltype");
		if (stamp instanceof IBorder) {
			type = ResourceMgr.getString(Lifecycle.class, "Lifecycle.closingsymbol.bordertype");
		}

		final MessagingResourceReader resourceReader =
				new MessagingResourceReader(IMessagingChoices.class, "messaging.choices");
		Choice saveChoice = new Choice(resourceReader, "SaveClose");
		Choice discardChoice = new Choice(resourceReader, "DiscardClose");
		Choice cancelChoice = new Choice(resourceReader, "Cancel");
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(Lifecycle.class,
						"Lifecycle.closingsymbol");
		content.setGuidance(Lifecycle.class, "Lifecycle.closingsymbol.guidance", type);
		content.setContext(Lifecycle.class, "Lifecycle.closingsymbol.close", type);
		content.setMessage(Lifecycle.class, "Lifecycle.closingsymbol.message", type);
		content.setImplicationsSuffixParameters("Single", type, "'" + stamp.getName() + "'");

		ICloseAllContext closeAllActionContext = CAFUtils.getInstance().getCloseContext();
		final Choice result;
		boolean isCloseAllDiagramsAction = closeAllActionContext != null;
		if (isCloseAllDiagramsAction) {
			String checkBoxMessage;
			if (stamp instanceof IBorder) {
				checkBoxMessage = ResourceMgr.getString(Lifecycle.class, "Lifecycle.closingBorder.CheckBox.message");
			}
			else {
				checkBoxMessage = ResourceMgr.getString(Lifecycle.class, "Lifecycle.closingsymbol.CheckBox.message");
			}
			result = Question.show(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					new DefaultQuestionPromptSeverityProvider(), content, true, closeAllActionContext,
					checkBoxMessage
					, saveChoice, discardChoice, cancelChoice);
		}
		else {
			result = Question.show(content, saveChoice, discardChoice, cancelChoice);
		}
		if (result == saveChoice) {
			if (isCloseAllDiagramsAction) {
				closeAllActionContext.setUserSelectedChoice(SaveStampOptions.SAVEANDCLOSE.name());
			}
			return SaveStampOptions.SAVEANDCLOSE;
		}
		if (result == discardChoice) {
			if (isCloseAllDiagramsAction) {
				closeAllActionContext.setUserSelectedChoice(SaveStampOptions.DISCARDANDCLOSE.name());
			}
			return SaveStampOptions.DISCARDANDCLOSE;
		}
		if (isCloseAllDiagramsAction) {
			closeAllActionContext.setUserSelectedChoice(SaveStampOptions.DISACRD.name());
		}
		return SaveStampOptions.DISACRD;
	}

	public boolean isModified(Object obj)
	{
		IAbstractLibrary slib = (IAbstractLibrary) obj;
		//
		// Go through each symbol, and see if there have been any changes
		//
		for (ModelMapData mmd : m_models.values()) {
			if (mmd.getLibrary() == slib) {
				if (mmd.geModel().isModified()) {
					return true;
				}
			}
		}
		return false;
	}

	public List<String> getModifiedSymbolNamesForSave(IAbstractLibrary library)
	{
		Map<IUID, Lifecycle.ModelMapData> symbols = getModifiedSymbols(library);
		return symbols.values().stream()
				.map(mdl -> mdl.getSymbol().getName())
				.collect(Collectors.toList());
	}

	public Map<IUID, Lifecycle.ModelMapData> getModifiedSymbols(IAbstractLibrary library)
	{

		Map<IUID, Lifecycle.ModelMapData> symbols = new HashMap<>();
		m_models.values().stream()
				.filter(mdl -> mdl.getLibrary().equals(library))
				.filter(mdl -> mdl.geModel().isModified())
				.forEach(mdl -> symbols.put(mdl.getSymbol().getUID(), mdl));
		return symbols;
	}

	public void symbolLibraryChanged(SymbolChangeEvent e)
	{
		// If a symbol library was closed, see if one of our models was
		// in the symbol library, and if so forget about it.
		if (e.getChangeType() == SymbolChangeEvent.SYMBOL_LIBRARY_CLOSED) {
			for (ModelMapData mmd : new ArrayList<ModelMapData>(m_models.values())) {
				if (mmd.getLibrary() == e.getSymbolLibrary()) {
					// Forget about this model since its project has closed
					destroyModelMap(mmd);
					//
					// We're done...
					//
					LockUpdateHelper.unlock(mmd.getLibrary());
				}
			}
		}
	}

	private void destroyModelMap(ModelMapData mmd)
	{
		if (mmd.geModel().isModified()) {
			mmd.geModel().setModified(false);
		}
		mmd.geModel().getController().destroy();
		mmd.geModel().destroy();
		m_models.remove(mmd.getSymbolUID());
	}

	private void destroyModelMap(IStamp stamp)
	{
		if (m_models.containsKey(stamp.getUID())) {
			destroyModelMap(m_models.get(stamp.getUID()));
		}
	}

	/**
	 * Process delete This is for handling the deletionof SymbolLibrary or BorderLibrary, and SymbolDef or Border
	 *
	 * @param context the context list
	 * @return boolean
	 */
	public boolean delete(List<?> context)
	{
		List<IAbstractLibrary> abstractLibList = LifecycleUtils.getContextObjects(context, IAbstractLibrary.class);
		List<IPSMStamp> stampList = LifecycleUtils.getContextObjects(context, IPSMStamp.class);

		if (abstractLibList.isEmpty()) {   // Bad context - has to have at least a Library
			throw new IllegalArgumentException("Wrong context for delete()");
		}
		final IAbstractLibrary abstractLib = abstractLibList.get(0);

		IPSMStamp stamp = null;
		if (!stampList.isEmpty()) {
			stamp = stampList.get(0);
		}

		final SymbolErrorDialog.UserAction userAction =
				stamp == null ? SymbolErrorDialog.UserAction.DeleteLibrary : SymbolErrorDialog.UserAction.DeleteSymbol;

		if (!hasEditPermission(abstractLib)) {
			displayMessageForNoPermissionToDelete(abstractLib, userAction);
			return false;
		}

		if (stamp == null) {
			return deleteLibrary(abstractLib);
		}
		return deleteSymbol(abstractLib, stamp, false);
	}

	protected boolean hasEditPermission(IAbstractLibrary abstractLib)
	{
		return SymbolLockAndRefreshHelper.hasEditPermission(abstractLib);
	}

	protected BooleanWithReason getLibraryEditableStatus(IAbstractLibrary library)
	{
		String message;
		if (!hasEditPermission(library)) {
			message = ResourceMgr.getString(Lifecycle.class, "Lifecycle.outputwindow.message.noPermissionToEdit",
					AppInfo.getFullApplicationName());
			return new BooleanWithReason(false, message);
		}
		if (!hasDomainPermission(library)) {
			message = ResourceMgr.getString(Lifecycle.class, "Lifecycle.outputwindow.message.noDomainWriteAccess");
			return new BooleanWithReason(false, message);
		}
		return new BooleanWithReason(true);
	}

	protected boolean hasDomainPermission(IAbstractLibrary library)
	{
		return DomainPermissionEnum.WRITABLE.accept(library);
	}

	private void displayMessageForNoPermissionToDelete(@NotNull IAbstractLibrary library,
			@NotNull SymbolErrorDialog.UserAction userAction)
	{
		SymbolErrorDialog.showErrorDialog(library, library.getType(), userAction,
				LibraryLockRefreshStatus.InsufficientPermission);
	}

	private boolean deleteLibrary(@NotNull IAbstractLibrary abstractLib)
	{
		boolean preLocked = abstractLib.isLocked();
		Collection<ICOGLockable> preLockedSymbols = getLockedSymbols(abstractLib);
		String toDelete = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Type.Library");

		if (!doPromptForLibraryDelete(abstractLib)) {
			return false;
		}
		List<IAbstractLibrary> libChanged = new ArrayList<IAbstractLibrary>(1);
		boolean success = false;
		try {
			if (lockAllSymbolsAndRefresh(abstractLib, libChanged, SymbolErrorDialog.UserAction.DeleteLibrary)) {
				// No problem locking - continue with delete process

				// Now send the request to the server [to delete the library]
				String request = DataStorageHelper.getDeleteRequest("symbollib", abstractLib.getUID().getString());

				// Tell the server to stuff it
				updateServerData(request);

				// Explicitly remove lock...
				if (!SymbolLibraryLockRefreshHelper.unlockIncludingSymbols(abstractLib)) {
					return false;    // Failed to unlock
				}
				// Regardless of whether it was prelocked - we have unlocked it now, so don't try later.
				preLocked = true;

				// Discard the changes
				discard(abstractLib);

				// Audit Symbol Library deleted event
				String desc = null;
				if (SymbolLibraryTypeEnum.SYMBOL.equals(abstractLib.getType())) {
					desc = ResourceMgr.getString(Lifecycle.class, "Lifecycle.AuditTrail.SymbolLibrary");
				}
				else if (SymbolLibraryTypeEnum.BORDER.equals(abstractLib.getType())) {
					desc = ResourceMgr.getString(Lifecycle.class, "Lifecycle.AuditTrail.BorderLibrary");
				}
				IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
				auditLogger.postEvent(AuditableEventType.SYMBOL_LIBRARY_DELETED, desc, abstractLib.getUID().getString(),
						abstractLib.getName(), abstractLib.getUID().getString());

				// Remove the library from the Symbol Library Mgr.
				CAFUtils.getInstance().getSymbolLibraryMgr().removeLibrary(abstractLib);
				// Close the Symbol Library
				CAFUtils.getInstance().getSymbolLibraryMgr().closeLibrary(abstractLib);
				// remove the symbol library from memory
				abstractLib.unload();
				success = true;
			}
		}
		catch (RuntimeException ex) {
			Environment.getExceptionDisplay().displayException(ex, "Delete " + toDelete + " request failed");
		}
		finally {
			unlockSymbols(abstractLib, preLockedSymbols);
			unlockLibrary(abstractLib, preLocked);
			updateSymbolBrowser(libChanged);
		}
		return success;
	}

	private void unlockLibrary(@NotNull IAbstractLibrary abstractLib, boolean preLocked)
	{
		if (!preLocked && abstractLib.isLocked()) {
			LockUpdateHelper.unlock(abstractLib);
		}
	}

	private boolean deleteSymbol(IAbstractLibrary abstractLib, IPSMStamp selectedStampForDelete, boolean onClose)
	{
		boolean preLocked = abstractLib.isLocked();
		IPSMStamp theStampToDelete = selectedStampForDelete;
		boolean preLockedSymbol = theStampToDelete.isLocked();
		String nameOfStampToBeDeleted = theStampToDelete.getName();

		// When closing we only prompt when closing the last symbol open on the library. However if
		// the symbol we are about to delete is the _last_ one _open_ with a window on this library and we
		// have other modified symbols then we need show a different prompt.
		if (!onClose && !doPromptForSymbolDelete(abstractLib, theStampToDelete)) {
			return false;
		}

		LibraryLockRefreshStatus libLockRefreshStatus = null;
		UserActionFailureReason failureReason = null;
		List<IAbstractLibrary> libChanged = new ArrayList<IAbstractLibrary>(1);
		try {
			if (!onClose) {
				libLockRefreshStatus = lockAndRefresh(abstractLib, libChanged);
				if (!libLockRefreshStatus.isSuccessful()) {
					return false;
				}
			}

			// No problem locking - continue with delete process
			if (onClose) {
				libChanged.add(abstractLib);
			}

			failureReason = findObjectExistsInLibrary(theStampToDelete, abstractLib);
			if (failureReason != null) {
				IStamp stampMatchingName = abstractLib.findExistingSymbol(nameOfStampToBeDeleted);
				if (stampMatchingName != null && stampMatchingName instanceof IPSMStamp) {
					failureReason = null;
					theStampToDelete = (IPSMStamp) stampMatchingName;
				}
				else {
					return false;
				}
			}

			preLockedSymbol = theStampToDelete.isLocked();
			if (theStampToDelete.lock()) {

				String symbolNameBeingDeleted = theStampToDelete.getName();
				String uidOfSymbolBeingDeleted = theStampToDelete.getUID().getString();

				doPersistentDeleteOfSymbol(abstractLib, theStampToDelete);
				updateAuditTrialWithSymbolDeleted(abstractLib.getType(), abstractLib.getUID().getString(),
						symbolNameBeingDeleted,
						uidOfSymbolBeingDeleted);

				return true;
			}
			else {

				failureReason = UserActionFailureReason.STAMPLOCKERROR;

				return false;
			}
		}
		finally {

			if (!preLockedSymbol && theStampToDelete.isLocked()) {
				theStampToDelete.unlock();
			}
			if (!onClose && !preLocked && abstractLib.isLocked()) {
				LockUpdateHelper.unlock(abstractLib);
			}

			if (libLockRefreshStatus != null && !libLockRefreshStatus.isSuccessful()) {
				reportLibraryLockRefreshFailure(abstractLib, libLockRefreshStatus,
						SymbolErrorDialog.UserAction.DeleteSymbol);
			}
			reportUserOperationFailure(theStampToDelete, abstractLib, nameOfStampToBeDeleted, failureReason,
					SymbolErrorDialog.UserAction.DeleteSymbol);

			updateSymbolBrowser(libChanged);
		}
	}

	private void updateAuditTrialWithSymbolDeleted(SymbolLibraryTypeEnum libraryType, String libUID,
			String theStampToDelete,
			String uidOfStampBeingDeleted)
	{
		// Audit Symbol deleted event
		String symbolLibraryTypeName = libraryType.getAsLocaleSpecificName();

		IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
		auditLogger.postEvent(AuditableEventType.SYMBOL_DELETED, symbolLibraryTypeName, libUID,
				theStampToDelete, uidOfStampBeingDeleted);
	}

	private void doPersistentDeleteOfSymbol(IAbstractLibrary abstractLib, IPSMStamp theStampToDelete)
	{
		// Close the Stamp windows, if any are open.. This will avoid promting for stamp
		((IPendingModification) theStampToDelete).setModified(false);
		closeWindowForSymbolDef(theStampToDelete, true);

		// Remove the Stamp from the model
		destroyModelMap(theStampToDelete);

		// Remove the stamp from the AbstractLib.
		abstractLib.removeSymbol(theStampToDelete);

		//Delete the symbol
		theStampToDelete.persistentDelete();

		theStampToDelete.skeletonizeUnlessPrevented(this);

		// finally remove the symbol from the uid manager...
		UIDMgr.removeObject(theStampToDelete.getUID());

		abstractLib.getFolderMgr().flush();
	}

	protected boolean doPromptForSymbolDelete(IAbstractLibrary abstractLib, IStamp theStampToDelete)
	{
		boolean showDeletePrompt = true;
		ICapletWindow theWindowOnSymbol = findFirstSymbolWindow(theStampToDelete);
		if (theWindowOnSymbol != null) {
			if (isLastWindowOnContainer(theWindowOnSymbol)) {
				// special case of last window on symbol, see if we have any modified symbols
				boolean hasModifiedSymbols = false;
				for (ModelMapData mmd : m_models.values()) {
					if (mmd.getLibrary() == abstractLib) {
						if (mmd.getSymbolUID().isEquiv(theStampToDelete.getUID())) {
							continue; // ignore symbol we are deleting
						}

						if (mmd.geModel().isModified()) {
							hasModifiedSymbols = true;
							break;
						}
					}
				}

				if (hasModifiedSymbols && hasEditPermission(abstractLib)) {
					String msg;
					if (theStampToDelete instanceof ISymbolDef) {
						msg = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Symbol.LastWindow.Msg");
					}
					else {
						msg = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Border.LastWindow.Msg");
					}
					String hdg = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Warning.Title1");
					String dialogTitle = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Title.Symbol");
					if (!showDeletionConfirmationDialog(dialogTitle, hdg, msg)) {
						return false;
					}

					// save the library then proceed with delete, without further prompting. The save below will
					// setModified(false) for all symbols belonging to the library.
					save(abstractLib, SaveContext.SAVE, false);
					showDeletePrompt = false;
				}
			}
		}

		if (showDeletePrompt) {
			// Want to delete, but we may have changed some symbols. Let the user decide.
			return doPrompt(theStampToDelete, theStampToDelete.getName());
		}
		return true;
	}

	protected boolean doPromptForLibraryDelete(IAbstractLibrary abstractLib)
	{
		String dialogTitle = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Title.Library");
		// Want to delete, but we may have changed some symbols. Let the user decide.
		String hdg = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Warning.Title1");
		String toDelete = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Type.Library");
		String msg = ResourceMgr
				.getString(Lifecycle.class, "Lifecycle.Delete.Warning.Msg1", toDelete, abstractLib.getName());

		return showDeletionConfirmationDialog(dialogTitle, hdg, msg);
	}

	protected boolean showDeletionConfirmationDialog(@NotNull String dialogTitle, @NotNull String hdg,
			@NotNull String msg)
	{
		return MessageHelper.showOkCancelDialog(getDialogFrame(),
				dialogTitle, hdg, msg, JOptionPane.WARNING_MESSAGE);
	}

	private boolean doPrompt(IStamp theStampToDelete, String libName)
	{
		String toDelete;
		String dialogTitle;
		if (theStampToDelete instanceof ISymbolDef) {
			toDelete = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Type.Symbol");
			dialogTitle = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Title.Symbol");
		}
		else {
			toDelete = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Type.Border");
			dialogTitle = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Title.Border");
		}

		String hdg = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Warning.Title1");
		String msg = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Delete.Warning.Msg1", toDelete, libName);
		return showDeletionConfirmationDialog(dialogTitle, hdg, msg);
	}

	protected static Frame getDialogFrame()
	{
		return CAFUtils.getInstance().getWindowMgr().getDialogFrame();
	}

	@Nullable protected static UserActionFailureReason checkValidityOfSymbolName(String newName, IAbstractLibrary lib,
			@Nullable IReadOnlyNamedObject objectToRename)
	{
		if (!checkNameLength(newName)) {
			return UserActionFailureReason.IGNOREFAILURE;
		}

		IStamp dup = null;
		if (lib != null) {
			dup = lib.findExistingSymbol(newName);
		}
		if (dup != null && !(objectToRename != null && dup.toString().equalsIgnoreCase(objectToRename.toString()))) {

			return UserActionFailureReason.DUPLICATESYMBOLNAME;
		}
		return null;
	}

	private static boolean checkNameLength(String newName)
	{
		if (newName.length() > CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH) {
			MessageHelper.showErrorMessage(getDialogFrame(),
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.NameTooLong.Title"),
					ResourceMgr
							.getString(Lifecycle.class, "Lifecycle.NameTooLong.Msg",
									String.valueOf(CHSConstants.DIAGRAM_OBJECT_NAME_LENGTH)));
			return false;
		}
		return true;
	}

	/**
	 * Process rename This is for renaming SymbolLibrary or BorderLibrary and SymbolDef or Border
	 *
	 * @param context the context
	 * @return true iff successful
	 */
	public boolean rename(@NotNull List<?> context, @Nullable String newName)
	{
		// Get the AbstractLibrary & Stamp from the context
		List<IAbstractLibrary> abstractLibList = LifecycleUtils.getContextObjects(context, IAbstractLibrary.class);
		if (abstractLibList.isEmpty()) {   // Bad context - has to have at least a Library
			throw new IllegalArgumentException("Wrong context for rename()");
		}
		IAbstractLibrary abstractLib = abstractLibList.get(0);

		IPSMStamp stamp = null;
		List<IPSMStamp> stampList = LifecycleUtils.getContextObjects(context, IPSMStamp.class);
		if (!stampList.isEmpty()) {   // Get the Stamp
			stamp = stampList.get(0);
		}

		if (stamp != null) {
			return renameSymbol(context, stamp, newName, abstractLib);
		}
		else {
			return renameLibrary(context, newName, abstractLib);
		}
	}

	private boolean renameLibrary(List<?> context, @Nullable String giveNewName, IAbstractLibrary abstractLib)
	{
		boolean success = false;
		try {
			// prompt the user
			String newName = giveNewName;
			if (newName == null) {
				newName = promptForName(abstractLib, abstractLib);
				if (newName == null) {
					return false;
				}
			}

			if (!checkNameLength(newName)) {
				return false;   // check name length for library and symbol name
			}

			if (getFIB().getSymbolLibraryMgr().doesDuplicateSymbolLibNameExists(newName)) {
				return false;
			}
			// continue with rename process
			String request = SymbolLibraryStorageHelper.getSymbolLibraryRename(abstractLib.getUID(), newName);

			// Tell the server to stuff it
			boolean updateSuccess = false;
			if (request != null) {
				updateSuccess = updateServerData(request);
			}
			if (!updateSuccess) {
				// Failed to update the server
				return false;
			}

			// Only do the rename if the DB update worked.
			// This is for renaming borderLibrary
			abstractLib.setName(newName);
			CAFUtils.getInstance().getSymbolLibraryMgr().resetLibraryNameInCache(abstractLib.getUID(), newName);
			CAFUtils.getInstance().getSymbolLibraryMgr().setCurrentLibrary(null);
			CAFUtils.getInstance().getSymbolLibraryMgr().setCurrentLibrary(abstractLib);

			setWindowTitle(context, true);

			success = true;
		}

		catch (Exception ex) {
			Environment.getExceptionDisplay().displayException(ex, "Rename library " + " request failed");
		}
		return success;
	}

	/**
	 * Process rename This is for renaming SymbolDef or Border
	 *
	 * @param context the context
	 * @return true iff successful
	 */
	public boolean renameSymbol(List<?> context, IPSMStamp givenStampForRename, @Nullable String givenNewName,
			IAbstractLibrary abstractLib)
	{
		boolean success = false;
		boolean preLocked = abstractLib.isLocked();
		IPSMStamp stamp = givenStampForRename;
		String currentSymbolName = stamp.getName();
		boolean preLockedSymbol = stamp.isLocked();
		List<IAbstractLibrary> libChanged = new ArrayList<IAbstractLibrary>(1);
		String type = getStampType(stamp);
		UserActionFailureReason userActionFailureReason = null;
		String newNameToBeUsed = givenNewName;
		LibraryLockRefreshStatus libLockRefreshStatus = null;
		try {
			// Get the current name in case this symbol has been renamed in another session.
			libLockRefreshStatus = refreshAndCheckExistence(abstractLib, libChanged);
			if (libLockRefreshStatus.isSuccessful()) {
				// prompt the user
				if (newNameToBeUsed == null) {
					newNameToBeUsed = promptForName(stamp, abstractLib);
					if (newNameToBeUsed == null) {
						return false;
					}
				}
				libLockRefreshStatus = lockAndRefresh(abstractLib, libChanged);
				if (libLockRefreshStatus.isSuccessful()) {
					userActionFailureReason = findObjectExistsInLibrary(stamp, abstractLib);
					if (userActionFailureReason != null) {
						IStamp stampMatchingName = abstractLib.findExistingSymbol(currentSymbolName);
						if (stampMatchingName != null && stampMatchingName instanceof IPSMStamp) {
							userActionFailureReason = null;
							stamp = (IPSMStamp) stampMatchingName;
						}
						else {
							return false;
						}
					}
					preLockedSymbol = stamp.isLocked();
					if (stamp.lock()) {
						abstractLib.loadFully(stamp);

						if (!checkNameLength(newNameToBeUsed)) {
							return false;   // check name length for symbol name
						}
						userActionFailureReason = checkValidityOfSymbolName(newNameToBeUsed, abstractLib, stamp);
						if (userActionFailureReason != null) {
							return false;
						}

						// This is for renaming stamp
						stamp.setName(newNameToBeUsed);

						String symbolLibraryTypeName = abstractLib.getType().getAsLocaleSpecificName();

						// Symbol Renamed From Audit Trail
						IAuditTrailLogger auditLogger =
								FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
						auditLogger.postEvent(AuditableEventType.SYMBOL_RENAMED,
								symbolLibraryTypeName + ' ' +
										ResourceMgr.getString(Lifecycle.class, "Lifecycle.AuditTrail.RenamedFrom"),
								abstractLib.getUID().getString(), stamp.getName(), stamp.getUID().getString());

						// Symbol Renamed To Audit Trail
						auditLogger.postEvent(AuditableEventType.SYMBOL_RENAMED,
								symbolLibraryTypeName + ' ' +
										ResourceMgr.getString(Lifecycle.class, "Lifecycle.AuditTrail.RenamedTo"),
								abstractLib.getUID().getString(), newNameToBeUsed, stamp.getUID().getString());

						if (!libChanged.isEmpty()) {
							// If the library has been refreshed, then the input stamp, is *NOT* valid. Find the
							// same stamp in library and updated its names. If this is not done then the rename will not
							// show in the browser, when we update it at the bottom.
							for (IStampIterator sdi = abstractLib.getSymbols(); sdi.hasNext(); ) {
								IStamp sdef = sdi.getNext();
								if (sdef.getUID().isEquiv(stamp.getUID())) {
									sdef.setName(newNameToBeUsed);
									break;
								}
							}
						}
						setWindowTitle(context, true);

						saveInternal(abstractLib, Collections.singleton(stamp));

						abstractLib.getFolderMgr().flush();

						success = true;
					}
					else {
						userActionFailureReason = UserActionFailureReason.STAMPLOCKERROR;
						return false;
					}
				}
			}
		}
		catch (Exception ex) {
			Environment.getExceptionDisplay().displayException(ex, "Rename " + type + " request failed");
		}
		finally {
			if (!preLockedSymbol && (stamp.isLocked())) {
				stamp.unlock();
			}

			unlockLibrary(abstractLib, preLocked);
			if (libLockRefreshStatus != null && !libLockRefreshStatus.isSuccessful()) {
				reportLibraryLockRefreshFailure(abstractLib, libLockRefreshStatus,
						SymbolErrorDialog.UserAction.RenameSymbol);
			}
			reportUserOperationFailure(stamp, abstractLib, newNameToBeUsed, userActionFailureReason,
					SymbolErrorDialog.UserAction.RenameSymbol);

			updateSymbolBrowser(libChanged);
		}
		return success;
	}

	protected void reportLibraryLockRefreshFailure(@NotNull IAbstractLibrary library,
			@NotNull LibraryLockRefreshStatus lockRefreshStatus, @NotNull SymbolErrorDialog.UserAction userAction)
	{
		reportLockRefreshFailure(Arrays.asList(library), library, lockRefreshStatus, userAction);
	}

	private void reportLockRefreshFailure(@NotNull List<INamedUIDObject> lockFailedObjects,
			@NotNull IAbstractLibrary library,
			@NotNull LibraryLockRefreshStatus lockRefreshStatus, @NotNull SymbolErrorDialog.UserAction userAction)
	{
		SymbolErrorDialog.showErrorDialog(lockFailedObjects, library.getType(), userAction, lockRefreshStatus);
		if (!lockRefreshStatus.isLibraryAccessible()) {
			getFIB().getSymbolLibraryMgr().closeLibrary(library);
			library.setLibraryMgr(null);
			LockUpdateHelper.unlock(library);
		}
	}

	private void reportLibraryDoesNotExist(IAbstractLibrary symbolLib, SymbolErrorDialog.UserAction action)
	{
		reportLibraryLockRefreshFailure(symbolLib, LibraryLockRefreshStatus.SourceLibraryDoesNotExist, action);
	}

	protected void reportStampLockError(@NotNull IStamp stamp, @NotNull SymbolLibraryTypeEnum type,
			@NotNull SymbolErrorDialog.UserAction action)
	{
		LibraryLockRefreshStatus lockRefreshStatus =
				type == SymbolLibraryTypeEnum.SYMBOL ? LibraryLockRefreshStatus.SymbolLockFailed :
						LibraryLockRefreshStatus.BorderLockFailed;
		SymbolErrorDialog.showErrorDialog(stamp, type, action, lockRefreshStatus);
	}

	protected void reportUserOperationFailure(@Nullable IStamp stamp, @NotNull IAbstractLibrary abstractLib,
			@Nullable String name, @Nullable UserActionFailureReason userActionFailureReason,
			@NotNull SymbolErrorDialog.UserAction action)
	{
		if (userActionFailureReason == null || userActionFailureReason == UserActionFailureReason.IGNOREFAILURE) {
			return;
		}
		if (userActionFailureReason == UserActionFailureReason.STAMPLOCKERROR) {
			assert stamp != null : "Stamp must be passed for STAMPLOCKERROR";
			reportStampLockError(stamp, abstractLib.getType(), action);
		}
		else if (userActionFailureReason == UserActionFailureReason.STAMPDELETEDINANOTHERSERSESSION) {

			SymbolErrorDialog.reportStampDeleteError(action, name, abstractLib.getType());
		}
		else if (userActionFailureReason == UserActionFailureReason.STAMPCANNOTBELOADED) {
			MessageHelper.showErrorMessage(getDialogFrame(),
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.Load.Error.Title"),
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.Load.Error.Msg", name));
		}
		else if (userActionFailureReason == UserActionFailureReason.STAMPMOVEDTOANOTHERSESSION) {
			SymbolErrorDialog.reportStampMovedError(name, abstractLib.getType(), action);
		}
		else if (userActionFailureReason == UserActionFailureReason.DUPLICATESYMBOLNAME) {
			MessageHelper.showErrorMessage(getDialogFrame(),
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.DuplicateSymbol.Title"),
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.DuplicateSymbol.Msg", name));
		}
		else if (userActionFailureReason == UserActionFailureReason.DUPLICATEBORDERNAME) {
			MessageHelper.showErrorMessage(getFIB().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.DuplicateBorder.Title"),
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.DuplicateBorder.Msg", name));
		}
	}

	@Nullable
	private String promptForName(IReadOnlyNamedObject objectToRename, IAbstractLibrary abstractLib2)
	{
		final IAbstractLibrary abstractLib = abstractLib2;
		String title = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Rename.Title");
		String label = ResourceMgr.getString(Lifecycle.class, "Lifecycle.Rename.Label.Text");
		RenameDialog dialog = createSymbolRenameDialog(objectToRename, abstractLib, title, label);

		dialog.setVisible(true);
		return dialog.getNewName(); // null means user canceled
	}

	@NotNull protected SymbolLibraryRenameDlg createSymbolRenameDialog(@NotNull IReadOnlyNamedObject objectToRename,
			@NotNull IAbstractLibrary abstractLib, @NotNull String title, @NotNull String label)
	{
		return new SymbolLibraryRenameDlg(getDialogFrame(),
				objectToRename, title, label, Lifecycle.class, abstractLib,
				CAFUtils.getInstance().getSymbolLibraryMgr());
	}

	private String getStampType(IStamp stamp)
	{
		String type = "symbol";
		if (stamp instanceof IBorder) {
			type = "border";
		}
		return type;
	}

	@Override public void setWindowTitle(@NotNull List<?> context, boolean notifyModelChange)
	{
		// Reset titles of all windows displaying this diagram.

		int numContext = context.size();
		if (numContext < 1) {
			return;
		}
		IStamp stamp = null;
		if (numContext > 1) {
			List<IStamp> stampList = LifecycleUtils.getContextObjects(context, IStamp.class);
			stamp = stampList.get(0);
		}

		// update the browser if the symbol name has changed
		if (stamp != null) {
			// send a model change event so that the browser tree will
			// be notified that the diagram name has been changed
			if (m_models.containsKey(stamp.getUID())) {
				ModelMapData mmd = m_models.get(stamp.getUID());
				Model model = mmd.geModel();
				Collection<IUID> emptyList = Collections.emptyList();
				boolean bSaveReq = model.isModified();
				model.notifyModelChange(new ModelChangeEvent(model, emptyList));

				// Rename doesn't need to be saved
				// If at all there is change in the symbol and the symbol is renamed now, we do want to popup 'Save dialog'..
				// Hence preserve the Modified status
				if (!bSaveReq) {
					model.setModified(false);
				}
			}
		}

		// simply rename all of the windows without prejudice to ensure that they're
		// always current with the names of their containers and such
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (window instanceof ICapletWindow && window.isDisplayed()) {
				CapletViewIterator cvIt = ((ICapletWindow) window).getViews();
				while (cvIt.hasNext()) {
					GfxView view = (GfxView) cvIt.getNext();
					if (view.getCapletModel() instanceof Model) {
						Model model = (Model) view.getCapletModel();
						IStamp def = model.getSymbolDef();
						IAbstractLibrary lib = def.getContainerLibrary();
						StringBuilder sb = new StringBuilder();
						sb.append(lib.getName());
						sb.append(':');
						sb.append(def.getName());
						window.setTitle(sb.toString());
					}
				}
			}
		}
	}

	@Override public void setWindowTitle(@NotNull List<?> context, @Nullable IBaseDiagram diagram)
	{
	}

	private void closeWindowForSymbolDef(IStamp stamp, boolean justCloseWindow)
	{
		// construct separate window list because we need to remove windows from window mgr as we iterate over them
		List<ICAFWindow> windows = CAFUtils.getInstance().getWindowMgr().getWindows();
		boolean isWindowClosed = false;
		for (ICAFWindow window : windows) {
			if (window instanceof ICapletWindow && window.getCaplet() == m_caplet) {
				CapletViewIterator cvIt = ((ICapletWindow) window).getViews();
				while (cvIt.hasNext()) {
					GfxView view = (GfxView) cvIt.getNext();
					if (((ISymbolModel) view.getCapletModel()).getSymbolDef() == stamp) {
						// programmatically close all windows on this stamp
						// note that the window may not be displayed (e.g. when overwriting a symbol in another library)

						try {
							m_programmaticCloseWindow = justCloseWindow;

							resetModel(view.getCapletModel());
							window.closeWindow();
							isWindowClosed = true;
						}
						finally {
							m_programmaticCloseWindow = false;
						}
					}
				}
			}
		}
		if (!windows.isEmpty() && !isWindowClosed) {
			ICapletController activeCapletController = CAFUtils.getInstance().getActiveCapletController();
			if (activeCapletController != null) {
				ICapletModel model = activeCapletController.getCapletModel();
				if (model != null) {
					model.notifyModelActivated();
				}
			}
		}
	}

	private boolean isOpen(IStamp stamp)
	{
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (window instanceof ICapletWindow && window.getCaplet() == m_caplet) {
				CapletViewIterator cvIt = ((ICapletWindow) window).getViews();
				while (cvIt.hasNext()) {
					GfxView view = (GfxView) cvIt.getNext();
					if (((ISymbolModel) view.getCapletModel()).getSymbolDef() == stamp) {
						// windows open in non-active libraries are still considered "open" and must be closed by the client code
						return true;
					}
				}
			}
		}
		return false;
	}

	@Nullable
	protected ICapletWindow findFirstSymbolWindow(IStamp stamp)
	{
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (window instanceof ICapletWindow && window.getCaplet() == m_caplet) {
				CapletViewIterator cvIt = ((ICapletWindow) window).getViews();
				while (cvIt.hasNext()) {
					GfxView view = (GfxView) cvIt.getNext();
					if (((ISymbolModel) view.getCapletModel()).getSymbolDef() == stamp) {

						return ((ICapletWindow) window);
					}
				}
			}
		}
		return null;
	}

	public IUIDObject move(IAbstractLibrary sourceLibrary, List<?> context)
	{
		// we'll be picking the destination library interactively, later...
		IAbstractLibrary destLib = null;

		// we delegate the real work to this MoveStampCommand
		MoveStampCommand cmd = createMoveCommand(sourceLibrary, context);
		boolean success = true;
		try {
			success = cmd.execute();
			if (success) {
				destLib = cmd.getTargetLibrary();
			}
		}
		finally {
			duplicateMoveCompleted(cmd);
			if (!success) {
				cmd.indicateAboutError();
			}
		}

		return destLib;
	}

	protected MoveStampCommand createMoveCommand(IAbstractLibrary sourceLibrary, List<?> context)
	{
		return new MoveStampCommand(sourceLibrary, context);
	}

	private void duplicateMoveCompleted(DuplicateMoveStampCommand cmd)
	{
		IAbstractLibrary destLib = cmd.getTargetLibrary();
		cmd.unlockSourceStamps();

		IAbstractLibrary sourceLib = cmd.getSourceLibrary();
		boolean sourceLibPreLocked = cmd.getSourceLibPreLocked();
		if (sourceLib != null && !sourceLibPreLocked) {
			sourceLib.unlock();
		}
		boolean destLibPreLocked = cmd.getTargetLibPreLocked();
		if (destLib != null && !destLibPreLocked) {
			destLib.unlock();
		}
		List<IAbstractLibrary> libsChanged = cmd.getLibsChanged();
		updateSymbolBrowser(libsChanged);
	}

	@Nullable
	public IUIDObject duplicate(IAbstractLibrary sourceLibrary, List<?> context)
	{
		// this implementation is a lot like move, but different enough for it's own code here

		// we'll be picking the destination library interactively, later...
		IAbstractLibrary destLib = null;

		// we delegate the real work to this MoveStampCommand
		DuplicateStampCommand cmd = createDuplicateCommand(sourceLibrary, context);
		boolean success = true;
		try {
			if (!getFIB().getApplication().canDoNew()) {
				success = false;
				SymbolErrorDialog.showErrorDialog(sourceLibrary, sourceLibrary.getType(),
						SymbolErrorDialog.UserAction.Duplicate,
						LibraryLockRefreshStatus.InsufficientPermission);
			}
			else {
				success = cmd.execute();
				if (success) {
					destLib = cmd.getTargetLibrary();
				}
			}
		}
		finally {
			duplicateMoveCompleted(cmd);
			if (!success) {
				cmd.indicateAboutError();
			}
		}

		return destLib;
	}

	protected DuplicateStampCommand createDuplicateCommand(IAbstractLibrary sourceLibrary, List<?> context)
	{
		return new DuplicateStampCommand(sourceLibrary, context);
	}

	/* (non-Javadoc)
	 * @see chs.caf.caplet.ICapletLifecycle#edit(java.util.List)
	 */

	public boolean edit(List<?> context)
	{
		// Get the AbstractLibrary from the context
		List<IAbstractLibrary> abstractLibList = LifecycleUtils.getContextObjects(context, IAbstractLibrary.class);
		if (abstractLibList.size() != 1) {   // Bad context - has to have exactly one Library
			throw new IllegalArgumentException("Wrong context for edit()");
		}
		IAbstractLibrary library = abstractLibList.get(0);
		return editLibrary(context, library);
	}

	private boolean editLibrary(@NotNull List<?> context, @NotNull IAbstractLibrary library)
	{
		final String title = ResourceMgr.getString(EditSymbolLibraryDialog.class, "EditSymbolLibraryDialog.title");
		final Frame frame = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		EditSymbolLibraryDialog symbolDialog = createEditSymbolLibraryDialog(library, title, frame);
		symbolDialog.setVisible(true);

		boolean success = false;
		if (symbolDialog.isSave()) {
			boolean preLocked = library.isLocked();
			Collection<ICOGLockable> preLockedSymbols = getLockedSymbols(library);
			List<IAbstractLibrary> libChanged = new ArrayList<IAbstractLibrary>(1);
			try {
				if (lockAllSymbolsAndRefresh(library, libChanged, SymbolErrorDialog.UserAction.EDIT)) {
					// Do rename action
					boolean nameChanged =
							symbolDialog.isNameChanged() && renameLibrary(context, symbolDialog.getNewName(), library);
					if (nameChanged) {
						save(library, ICapletLifecycle.SaveContext.SAVE, false);
					}

					// Set domain
					if (symbolDialog.isDomainChanged()) {
						DomainPanelManager.getInstance().assignDomain(library, symbolDialog.getDomain());
					}

					GridScaleSettings scaleSettings = symbolDialog.getGridSettings();
					if (symbolDialog.shouldShowGridSetting() &&
							scaleSettings != null) { // Not applicable for border lib.
						// Set the scale type - physical / pin grid
						library.setSymbolScaleType(scaleSettings.getSymbolScaleType());
						library.setResizable(scaleSettings.isSymbolResizabale());

						// Set the units type & value
						IGrid outputGrid = library.getGrid();
						if (outputGrid == null) {
							outputGrid = FactoryMgr.getDrawFactory().createGrid();
						}
						outputGrid.setRealMapping(scaleSettings.getGridMapping());
						library.setGrid(outputGrid);
						saveLibraryAttributes(library);

						if (scaleSettings.isUpdateAllExistingSymbols() ||
								scaleSettings.isUpdateResizeAllExistingSymbols()) {
							Frame parent = frame;
							Cursor oldCursor = parent.getCursor();
							try {
								parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
								// Update all the symbols in the library.
								IStampIterator symbol = library.getSymbols();
								while (symbol.hasNext()) {
									IStamp stamp = symbol.next();
									updateScaleAndResizeabilityForSymbol(library, stamp, scaleSettings);
									IPSMStamp cogSavableStamp = CommonUtils.cast(stamp, IPSMStamp.class);
									if (cogSavableStamp != null) {
										saveInternal(library, Collections.singleton(cogSavableStamp));
									}
									stamp.unloadChildren();
								}
								CAFUtils.getInstance().getSymbolLibraryMgr()
										.symbolLibraryEdited(library, Collections.singletonList(library));
							}
							finally {
								parent.setCursor(oldCursor);
							}
						}
					}
					else if (symbolDialog.isDomainChanged()) {
						// save border library only if domain is changed
						saveLibraryAttributes(library);
					}

					if (nameChanged || symbolDialog.isDomainChanged()) {
						// We flush the folder mgr, so that another session can see this name and domain changes.
						library.getFolderMgr().flush();
					}
					success = true;
				}
			}
			finally {
				unlockSymbols(library, preLockedSymbols);
				unlockLibrary(library, preLocked);
				updateSymbolBrowser(libChanged);
			}
		}
		return success;
	}

	@NotNull
	protected EditSymbolLibraryDialog createEditSymbolLibraryDialog(@NotNull IAbstractLibrary library,
			@NotNull String title, @NotNull Frame frame)
	{
		return new EditSymbolLibraryDialog(frame, title, library, Lifecycle.class);
	}

	@NotNull private Collection<ICOGLockable> getLockedSymbols(@NotNull IAbstractLibrary library)
	{
		Collection<ICOGLockable> preLockedSymbols = new ArrayList<ICOGLockable>();
		for (IStamp symbol : library.getSymbols()) {
			if (symbol instanceof ICOGLockable && ((ICOGLockable) symbol).isLocked()) {
				preLockedSymbols.add((ICOGLockable) symbol);
			}
		}
		return preLockedSymbols;
	}

	private void unlockSymbols(@NotNull IAbstractLibrary library, @NotNull Collection<ICOGLockable> preLockedSymbols)
	{
		for (IStamp symbol : library.getSymbols()) {
			if (symbol instanceof ICOGLockable && !preLockedSymbols.contains(symbol)) {
				LockUpdateHelper.unlock((ICOGLockable) symbol);
			}
		}
	}

	private void saveLibraryAttributes(@NotNull IAbstractLibrary library)
	{
		// Save the scale and domain settings on the library. (- not the symbols)
		try {
			PersistPayload saveSymbolLibraryRequestPayload = SymbolLibraryStorageHelper
					.saveSymbolLibraryRequest(library, Collections.emptySet(), false);
			if (saveSymbolLibraryRequestPayload != null) {
				IUserSession userSession = FactoryMgr.getCHSSystem().getUserSession();
				if (userSession != null) {
					userSession.updateDataCompressed(saveSymbolLibraryRequestPayload.getResults());
				}
			}
		}
		catch (UserSessionException ex) {
			//
		}
	}

	private void updateScaleAndResizeabilityForSymbol(@NotNull IAbstractLibrary library, @NotNull IStamp stamp,
			@NotNull GridScaleSettings gridSettings)
	{
		if (stamp instanceof ISymbolDef) {
			ISymbolDef srcSymDef = (ISymbolDef) stamp;
			boolean canUpdateSymbolScale =
					srcSymDef.getSymbolType().isSymbolScaleSupported() && gridSettings.isUpdateAllExistingSymbols();
			boolean canUpdateSymbolResizability =
					srcSymDef.getSymbolType().isResizeable() && gridSettings.isUpdateResizeAllExistingSymbols();
			if (!canUpdateSymbolScale && !canUpdateSymbolResizability) {
				return;
			}
			ISymbolDef loadedSymDef = (ISymbolDef) library.loadFully(srcSymDef);
			if (canUpdateSymbolScale) {
				loadedSymDef.setSymbolScaleType(gridSettings.getSymbolScaleType());
				IUnit gridMap = gridSettings.getGridMapping();
				loadedSymDef.getGrid().setRealMapping(gridMap);
			}
			if (canUpdateSymbolResizability) {
				loadedSymDef.setResizable(gridSettings.isSymbolResizabale());
			}
			loadedSymDef.setEdited(true);
		}
	}

	public IUIDObject copy(List<?> context)
	{
		// Unimplemented method.
		return null;
	}

	public IUIDObject createRevision(List<?> context)
	{
		// Unimplemented method.
		return null;
	}

	public boolean allowPromptOnClosingLastWindowOnDesign()
	{
		return true;
	}

	public boolean isLastWindowOnContainer(ICapletWindow theClosingWindow)
	{
		ICapletController capletController = theClosingWindow.getController();
		IUIDObject theClosingWindowsRootModel = capletController.getCapletModel().getModelRoot();
		if (theClosingWindowsRootModel instanceof IStamp) {
			IStamp theStamp = (IStamp) theClosingWindowsRootModel;
			List<ICapletWindow> windowsOnDesign = getCapletWindowsForSymbol(theStamp);
			return windowsOnDesign.size() == 1;
		}
		return false;
	}

	private static List<ICapletWindow> getCapletWindowsForSymbol(IStamp theClosingWindowsSymbol)
	{
		List<ICapletWindow> capWinArray = new ArrayList<ICapletWindow>();
		// We loop on the caplet windows and check if they belong to the specified symbol.
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (window instanceof ICapletWindow) {
				ICapletWindow capWin = (ICapletWindow) window;
				ICapletView view = capWin.getCurrentView();
				if (view instanceof GfxView) {

					ISheet sht = ((IDrawingComponentOwner) view).getSheet();

					if (sht instanceof ISheetAdapter &&
							((ISheetAdapter) sht).getSymbol() == theClosingWindowsSymbol) {
						capWinArray.add(capWin);
					}
				}
			}
		}
		return capWinArray;
	}

	@Nullable protected StampCreationParameterHolder getCreationParameterHolder(IAbstractLibrary library)
	{
		if (SymbolLibraryTypeEnum.SYMBOL.equals(library.getType())) {
			if (Environment.isXSCAppSuite()) {
				return new XSCSymbolCreationParameterHolder(library);
			}
			return new SymbolCreationParameterHolder(library);
		}
		if (SymbolLibraryTypeEnum.BORDER.equals(library.getType())) {
			return new BorderCreationParameterHolder(library);
		}
		return null;
	}

	protected static class ModelMapData
	{

		private IAbstractLibrary m_library;

		// ** Important do NOT use equality operator for symbols mmd.m_symbol == sdef since they will
		// ** not always be equal. i.e symbols are first skeletonlally loaded, however when they are
		// ** later fully loaded they will have a different address, hence must use UID.
		// ** hence made m_symbol private to prevent accidental use of equality operator **
		private IUID m_symbol;

		private Model m_model;

		ModelMapData(IAbstractLibrary library, IStamp symbol, Model model)
		{
			m_library = library;
			m_symbol = symbol.getUID();
			m_model = model;
		}

		public IUID getSymbolUID()
		{
			return m_symbol;
		}

		public void setSymbolEdited(boolean flag)
		{
			getSymbol().setEdited(flag);
		}

		public IPSMStamp getSymbol()
		{
			return (IPSMStamp) UIDMgr.getObject(m_symbol);
		}

		public Model geModel()
		{
			return m_model;
		}

		public IAbstractLibrary getLibrary()
		{
			return m_library;
		}
	}

	/**
	 * Common base class for DuplicateStampCommand and MoveStampCommand
	 */
	private abstract class DuplicateMoveStampCommand
	{

		protected List<IPSMStamp> srcStamps;
		protected IAbstractLibrary sourceLibrary;
		protected IAbstractLibrary targetLibrary;
		protected IFolder targetFolder;
		protected boolean targetLibPreLocked = false;
		protected List<IAbstractLibrary> libsChanged = new ListSet<IAbstractLibrary>();
		protected boolean preparedOK = false;
		protected String newName;
		protected boolean overwrite = false;
		protected boolean m_isNewLibrary = false;
		protected String m_newLibraryName;
		protected IDomain newLibraryDomain;
		protected boolean sourceLibPreLocked = false;
		protected Map<IUID, String> folderNameOfStamp = new HashMap<IUID, String>();
		protected Set<IStamp> preLockedSourceStamps = new HashSet<IStamp>();
		protected boolean errorInTab;
//		protected IStamp targetStamp;

		protected DuplicateMoveStampCommand(IAbstractLibrary library, List<?> context)
		{
			// decode the context, we are expecting just a list of symbols/borders in the same source library
			sourceLibrary = library;
			srcStamps = new ArrayList<IPSMStamp>();
			for (Object obj : context) {
				IPSMStamp stamp = (IPSMStamp) obj;

				srcStamps.add(stamp);
			}
		}

		public IAbstractLibrary getSourceLibrary()
		{
			return sourceLibrary;
		}

		public boolean getSourceLibPreLocked()
		{
			return sourceLibPreLocked;
		}

		@Nullable
		public IAbstractLibrary getTargetLibrary()
		{
			return targetLibrary;
		}

		public boolean getTargetLibPreLocked()
		{
			return targetLibPreLocked;
		}

		public List<IAbstractLibrary> getLibsChanged()
		{
			assert libsChanged.size() <= 2; // we should only ever deal with a source and target library
			return libsChanged;
		}

		public List<IPSMStamp> getSourceStamps()
		{
			return srcStamps;
		}

		public void unlockSourceStamps()
		{
			for (IPSMStamp src : getSourceStamps()) {
				if (!preLockedSourceStamps.contains(src)) {
					if (src.isLocked()) {
						src.unlock();
					}
				}
			}
		}

		abstract SymbolErrorDialog.UserAction getUsetAction();

		@NotNull protected MoveSymbolDialog createMoveDialog(@NotNull Frame dialogFrame,
				@NotNull DuplicateMoveSymbolTargetLibraries targetLibraries,
				@NotNull String title, @NotNull IAbstractLibrary sourceLib)
		{
			return new MoveSymbolDialog(dialogFrame, targetLibraries, title, sourceLib, false, null);
		}

		public boolean execute()
		{

			final LibraryLockRefreshStatus refreshStatus = refreshAndCheckExistence(sourceLibrary, libsChanged);
			if (!refreshStatus.isSuccessful()) {
				reportLibraryLockRefreshFailure(sourceLibrary, refreshStatus, getUsetAction());
				sourceLibPreLocked = true;
				return false;
			}
			sourceLibPreLocked = sourceLibrary.isLocked();

			updateSymbolBrowser(libsChanged);

			return prepareAndExecute();
		}

		protected boolean prepareAndExecute()
		{
			assert targetLibrary == null : "The execute method should only be called once";
			return doPrepare() && doExecute();
		}

		/**
		 * Preparation is basically the same for Duplicate & Move
		 *
		 * @return true if the command is prepared succesfully, false otherwise
		 */
		protected boolean doPrepare()
		{
			if (sourceLibrary == null) {
				assert false;
				return false;
			}
			if (srcStamps.isEmpty()) {
				assert false;
				return false;
			}

			// prepare each border/symbol for Move
			// this will prompt for save/close of opened symbols

			if (!closeSymbolsWithOpenWindows()) {
				String type = ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.symboltype");
				if (SymbolLibraryTypeEnum.BORDER.equals(sourceLibrary.getType())) {
					type = ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.bordertype");
				}
				errorInTab = true;
				String message = getErrorHeading() + ": " + ResourceMgr
						.getString(Lifecycle.class, "Lifecycle.DuplicateMoveAction.AllSymbolsNotClosed", type);
				sendMessageToOutputWindow(HTMLHelper.color(IColor.RED, message));
				for (IPSMStamp src : srcStamps) {
					boolean isStampLocked = src.isLocked();
					if (isStampLocked) {
						preLockedSourceStamps.add(src);
					}
				}
				return false;
			}
			if (!pickTargetLibrary()) {
				unlockSourceStamps();
				return false;
			}
			if (!prepareSourceLibrary()) {
				return false;
			}

			List<IPSMStamp> stamps = new ArrayList<IPSMStamp>();
			for (IPSMStamp src : srcStamps) {
				// preparing the symbol might fully load it.
				boolean isStampLocked = src.isLocked();
				DuplicateMoveSymbolHelper.PrepareStampResult resultOfPrepare =
						prepareStamp(sourceLibrary, src, false, OPEN, false);

				if (resultOfPrepare.getError() != null) {
					errorInTab = true;
					String user = "";
					try {
						IUserSession userSession = FactoryMgr.getCHSSystem().getUserSession();
						if (userSession != null) {
							ILockInfo userInfo =
									userSession.getLockInfo(src.getUID().toString());
							if (userInfo != null && !"NOT_LOCKED".equals(userInfo.getLockStatus().toString())) {
								user = userInfo.getUserName();
							}
						}
					}
					catch (UserSessionException e) {
						e.printStackTrace();
					}
					outputApplicationErrorMsg(src, resultOfPrepare, user);
					if (isStampLocked) {
						preLockedSourceStamps.add(src);
					}
					continue;
				}

				IPSMStamp stamp =
						resultOfPrepare.getStamp(); // error if missing stamp
				if (stamp == null) {
					continue;
				}
				String sourceFolder = getSourceFolderName(stamp);

				folderNameOfStamp.put(stamp.getUID(), sourceFolder);

				if (!preparedOK) {
					// bombing out here on the first symbol not prepared (e.g. the user chooses not to close the window)
					return false;
				}

				stamps.add(stamp);
			}

			srcStamps = stamps; // the fully loaded stamps

			return !srcStamps.isEmpty();
		}

		private void outputApplicationErrorMsg(IPSMStamp src,
				DuplicateMoveSymbolHelper.PrepareStampResult resultOfPrepare,
				String user)
		{
			SymbolLockAndRefreshHelper.ErrorValue errorValue = resultOfPrepare.getError();
			if ((errorValue == SymbolLockAndRefreshHelper.ErrorValue.SYMBOLINSOURCEISLOCKED) ||
					(errorValue == SymbolLockAndRefreshHelper.ErrorValue.SOURCELIBRARYCANNOTBELOCKED)) {

				String message = getErrorHeading() + ": " + resultOfPrepare.getError()
						.getDisplayValue(sourceLibrary.getName(), src.getName(), sourceLibrary.getType()) + " '" +
						user + "'";
				sendMessageToOutputWindow(HTMLHelper.color(IColor.RED, message));
			}
			else {
				String message = getErrorHeading() + ": " + resultOfPrepare.getError()
						.getDisplayValue(sourceLibrary.getName(), src.getName(), sourceLibrary.getType());
				sendMessageToOutputWindow(message);
			}
		}

		/**
		 * Execution is slightly different for Duplicate & Move
		 *
		 * @return true if the command is executed succesfully, false otherwise
		 */
		protected abstract boolean doExecute();

		protected abstract String getErrorHeading();

		protected boolean pickTargetLibrary()
		{
			MoveSymbolDialog dialog = createDialog();

			dialog.setVisible(true);
			if (dialog.isCancelled()) {
				return false;
			}
			m_isNewLibrary = dialog.isNew();
			m_newLibraryName = null;
			if (!m_isNewLibrary) {
				targetLibrary = dialog.getDestinationLibrary();
				targetLibPreLocked = dialog.wasDestinationLibraryPreLocked();
				targetFolder = dialog.getDestinationFolder();
				return !(dialog.isCancelled() || targetLibrary == null);
			}
			else {
				m_newLibraryName = dialog.getNewLibraryName();
				newLibraryDomain = dialog.getNewLibraryDomain();
				return !StringUtils.isEmpty(m_newLibraryName);
			}
		}

		protected abstract MoveSymbolDialog createDialog();

		DuplicateMoveSymbolHelper.PrepareStampResult getPrepareStampResult(IAbstractLibrary lib, IPSMStamp src,
				boolean allowMissing, String actionStr)
		{
			return prepareStamp(lib, src, allowMissing, actionStr, true);
		}

		protected boolean closeSymbolsWithOpenWindows()
		{
			List<String> symbolnames = new ArrayList<String>(srcStamps.size());
			List<IPSMStamp> symbolsinOpenState = new ArrayList<IPSMStamp>(srcStamps.size());
			getSymbolsInOpenState(symbolnames, symbolsinOpenState);
			if (symbolsinOpenState.isEmpty()) {
				return true;
			}

			// symbols in open state and are not locked implies that they have been opened as read-only
			boolean readOnlySymbols = symbolsinOpenState.stream().allMatch(sym -> !sym.isLocked());
			final MessagingResourceReader resourceReader =
					new MessagingResourceReader(IMessagingChoices.class, "messaging.choices");
			Choice saveChoice = new Choice(resourceReader, "SaveClose");
			Choice discardChoice = new Choice(resourceReader, "DiscardClose");
			Choice cancelChoice = new Choice(resourceReader, "Cancel");

			// if all symbols that are being attempted to close are read-only, user does not have to be asked for choice
			// and can be proceeded with discard option, as there's nothing to save in read-only symbols
			final Choice result = readOnlySymbols ? discardChoice :
					getUserChoiceOfSaveOrDiscard(symbolnames, symbolsinOpenState, saveChoice, discardChoice,
							cancelChoice);

			if (result == saveChoice) {
				List<IPSMStamp> symbolsToBeSaved = symbolsinOpenState.stream().filter(sym -> sym.isLocked())
						.collect(Collectors.toList());
				for (IPSMStamp stamp : symbolsToBeSaved) {
					ModelMapData data = getModelMaps().get(stamp.getUID());
					if (data != null) {
						Model model = data.geModel();
						preSaveSymbol(model, stamp);
					}
				}
				saveInternal(sourceLibrary, symbolsToBeSaved);
			}
			if (result == discardChoice || result == saveChoice) {
				for (IPSMStamp aStamp : symbolsinOpenState) {
					((IPendingModification) aStamp).setModified(false);
					closeWindowForSymbolDef(aStamp, false);
				}
			}
			else {
				return result != cancelChoice;
			}
			return true;
		}

		protected Choice getUserChoiceOfSaveOrDiscard(List<String> symbolnames, List<IPSMStamp> symbolsinOpenState,
				Choice saveChoice, Choice discardChoice, Choice cancelChoice)
		{
			String type = symbolsinOpenState.iterator().next() instanceof IBorder ?
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.bordertype") :
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.symboltype");
			String operation = getDisplayValueOfCommand();

			ResourceBasedMessageContent content = new ResourceBasedMessageContent(Lifecycle.class,
					"Lifecycle.moveorduplicate.closeaffeted");
			content.setGuidance(Lifecycle.class, "Lifecycle.moveorduplicate.closeaffeted.guidance", type, operation);
			content.setContext(Lifecycle.class, "Lifecycle.moveorduplicate.closeaffeted.close", type, operation);
			content.setMessage(Lifecycle.class, "Lifecycle.moveorduplicate.closeaffeted.message", type, operation);

			if (symbolnames.size() > 1) {
				Collections.sort(symbolnames, new AlphaNumComparator<String>());
			}
			if (symbolnames.size() == 1) {
				content.setImplicationsSuffixParameters("Single", symbolnames.toArray()[0], type, operation);
			}
			else if (symbolnames.size() == 2) {
				content.setImplicationsSuffixParameters("Two", symbolnames.toArray()[0], symbolnames.toArray()[1], type,
						operation);
			}
			else if (symbolnames.size() == 3) {
				content.setImplicationsSuffixParameters("Three", symbolnames.toArray()[0], symbolnames.toArray()[1],
						symbolnames.toArray()[2], type, operation);
			}
			else {
				content.setImplicationsSuffixParameters("Multiple", symbolnames.size(), type, operation);
			}
			return Question.show(content, saveChoice, discardChoice, cancelChoice);
		}

		protected void getSymbolsInOpenState(List<String> symbolnames, List<IPSMStamp> symbolsinOpenState)
		{
			for (IPSMStamp aStamp : srcStamps) {
				if (isOpen(aStamp)) {
					symbolnames.add(aStamp.getName());
					symbolsinOpenState.add(aStamp);
				}
			}
		}

		@NotNull
		protected DuplicateMoveSymbolHelper.PrepareStampResult prepareStamp(IAbstractLibrary lib, IPSMStamp src,
				boolean allowMissing, String actionStr, boolean inTargetLibrary)
		{
			preparedOK = true;
			// symbol lib. has already been locked/refreshed, this also checks/errors for source symbol no longer existing.
			// NOTE: src symbol may have a null library at this point, if it was removed by the refresh
			// TODO jacobt FEAT10165 : No need to lock source library if duplicating (unless it is also the target)

			if (src.getContainerLibrary() != lib) {
				preparedOK = false;
				return new DuplicateMoveSymbolHelper.PrepareStampResult(null,
						SymbolLockAndRefreshHelper.ErrorValue.SYMBOLNOTINPARENT);
			}

			Frame frame = getDialogFrame();
			String type = lib.getType().getAsLocaleSpecificName();

			// prompt user to close/save open diagram
			if (isOpen(src)) {
				int result = confirmCloseStamp(frame, src);
				if (result == JOptionPane.CANCEL_OPTION) {
					preparedOK = false;
					if (inTargetLibrary) {
						return new DuplicateMoveSymbolHelper.PrepareStampResult(null,
								SymbolLockAndRefreshHelper.ErrorValue.SYMBOLINTARGETCANNOTBEREPLACED);
					}
					else {
						return new DuplicateMoveSymbolHelper.PrepareStampResult(null,
								SymbolLockAndRefreshHelper.ErrorValue.SYMBOLINSOURCEISLOCKED);
					}
				}
			}

			// if a window was open, close the window without any prompting
			closeWindowForSymbolDef(src, false);

			// save the symbol we are about to move **and** any other modified symbols that are closed
			// TODO jacobt FEAT10165 : move this outside of loop?  Multiple saves...
			if (src.isEdited()) {
				saveInternal(lib, Collections.singleton(src));
			}

			// remove symbol from the modelmap - this only applies to symbols whose window has just been closed
			destroyModelMap(src);
			if (!src.lock()) {
				preparedOK = false;
				if (inTargetLibrary) {
					return new DuplicateMoveSymbolHelper.PrepareStampResult(null,
							SymbolLockAndRefreshHelper.ErrorValue.SYMBOLINTARGETCANNOTBEREPLACED);
				}
				else {
					return new DuplicateMoveSymbolHelper.PrepareStampResult(null,
							SymbolLockAndRefreshHelper.ErrorValue.SYMBOLINSOURCEISLOCKED);
				}
			}

			// symbol needs to be fully loaded to move it
			// TODO jacobt FEAT10165 : This could fully load many objects for multiple Move - is that a problem?

			if (src.isSkeleton()) {
				lib.loadFully(src);
			}
			if (src.isSkeleton()) {
				if (!allowMissing) {
					preparedOK = false;

					JLabel actionLabel = new JLabel();
					Font newLabelFont =
							actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
					actionLabel.setFont(newLabelFont);
					actionLabel.setText(ResourceMgr.getString(Lifecycle.class, "Lifecycle.stamp.deleted.error.action"));

					String title = ResourceMgr
							.getString(Lifecycle.class, "Lifecycle.stamp.deleted.error.title", actionStr, type);
					Class<Lifecycle> cls = Lifecycle.class;
					String heading = ResourceMgr.getString(cls, "Lifecycle.stamp.deleted.error.heading", type);
					String msg = ResourceMgr.getString(cls, "Lifecycle.stamp.deleted.error.message", type, actionStr);
					MessageHelper.showErrorMessage(frame, title, heading, msg, actionLabel);
				}
				if (inTargetLibrary) {
					return new DuplicateMoveSymbolHelper.PrepareStampResult(null,
							SymbolLockAndRefreshHelper.ErrorValue.SYMBOLINTARGETCANNOTBEREPLACED);
				}
				else {
					return new DuplicateMoveSymbolHelper.PrepareStampResult(null,
							SymbolLockAndRefreshHelper.ErrorValue.SYMBOLINSOURCEISLOCKED);
				}
			}
			return new DuplicateMoveSymbolHelper.PrepareStampResult(src, null);
		}

		protected int confirmCloseStamp(Frame frame, IStamp stamp)
		{
			String type = stamp.getContainerLibrary().getType().getAsLocaleSpecificName();
			String name = stamp.getName();
			String msg = ResourceMgr.getString(Lifecycle.class, "Lifecycle.stamp.open.prompt.message", type, name);
			String title = ResourceMgr.getString(Lifecycle.class, "Lifecycle.stamp.open.prompt.title");
			return JOptionPane.showConfirmDialog(frame, msg, title, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
		}

		/**
		 * Update the folder mgr for the source/target library - if they were changed
		 */
		protected void updateFolders()
		{
			if (libsChanged.contains(sourceLibrary)) {
				sourceLibrary.getFolderMgr().flush();
			}
			if (libsChanged.contains(targetLibrary)) {
				targetLibrary.getFolderMgr().flush();
			}
		}

		protected void logInformation(List<String> duplicatedOrMovedSymbolNames, String targetLibraryName,
				boolean isDuplicate)
		{
			if (duplicatedOrMovedSymbolNames.isEmpty()) {
				return;
			}

			String singleSymbol =
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.information.singlesymbol");
			String multipleSymbols =
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.information.multiplesymbols");
			String singleBorder =
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.information.singleborder");
			String multipleBorders =
					ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.information.multipleborders");

			String symbolType;
			String symbolNames;
			if (duplicatedOrMovedSymbolNames.size() == 1) {
				symbolType =
						SymbolLibraryTypeEnum.SYMBOL.equals(sourceLibrary.getType()) ? singleSymbol : singleBorder;
				symbolNames = duplicatedOrMovedSymbolNames.get(0);
			}
			else {
				symbolType = SymbolLibraryTypeEnum.SYMBOL.equals(sourceLibrary.getType()) ? multipleSymbols :
						multipleBorders;
				if (duplicatedOrMovedSymbolNames.size() < 4) {
					symbolNames = duplicatedOrMovedSymbolNames.stream().collect(Collectors.joining(", "));
				}
				else {
					symbolNames = duplicatedOrMovedSymbolNames.stream()
							.limit(3)
							.collect(Collectors.joining(", ")) + "...";
				}
			}
			String actionMsg = ResourceMgr.getString(Lifecycle.class,
					isDuplicate ? "Lifecycle.moveorduplicate.information.duplicateaction" :
							"Lifecycle.moveorduplicate.information.moveaction");
			String outputMsg = ResourceMgr
					.getString(Lifecycle.class, "Lifecycle.moveorduplicate.information.message", symbolType,
							symbolNames, actionMsg, targetLibraryName);
			sendMessageToOutputWindow(outputMsg);
		}

		protected boolean handleAddToNewLibrary(String newLibraryName, IDomain newLibDomain, boolean isDuplicate)
				throws IOException

		{
			IAbstractLibrary targetNewLibrary;
			if (SymbolLibraryTypeEnum.BORDER.equals(sourceLibrary.getType())) {
				targetNewLibrary =
						FactoryMgr.getSymbolFactory().createBorderLibrary(FactoryMgr.getCommonFactory().createUID());
			}
			else {
				targetNewLibrary =
						FactoryMgr.getSymbolFactory().createSymbolLibrary(FactoryMgr.getCommonFactory().createUID());
			}
			targetNewLibrary.setName(newLibraryName);
			DomainPanelManager.getInstance().assignDomain(targetNewLibrary, newLibDomain);
			DuplicateMoveSymbolAddRemoveHelper addRemoveHelper;
			if (isDuplicate) {
				addRemoveHelper =
						new DuplicateSymbolAddRemoveHelper(targetNewLibrary, targetNewLibrary.getFolderMgr());
			}
			else {
				addRemoveHelper =
						new MoveSymbolAddRemoveHelper(targetNewLibrary, targetNewLibrary.getFolderMgr());
			}

			DuplicateMoveSymbolHelper resolveNameClashesDuplicateMove =
					new DuplicateMoveSymbolHelper(addRemoveHelper, targetLibrary, true);
			boolean creationCanceled = false;
			for (IStamp stamp : srcStamps) {

				resolveNameClashesDuplicateMove.addSymbolToTarget(stamp, "New Library");
				if (resolveNameClashesDuplicateMove.getResponse() == MessageHelper.RESULT_CANCEL) {
					creationCanceled = true;
				}
			}
			unlockSourceStamps();
			if (!isDuplicate) {
				libsChanged.add(sourceLibrary);
			}
			if (!creationCanceled) {
				SymbolImportHelper.save(targetNewLibrary, new Object());

				UIDMgr.getUIDMgr().removeObject(targetNewLibrary.getUID());
				IAbstractLibrary newDBLibraryDriver =
						DriverFactory.getFactory().buildDriver(targetNewLibrary.getUID(), "dbase",
								targetNewLibrary.getName(), targetNewLibrary.getUID().getString());
				UIDMgr.getUIDMgr().addObject(newDBLibraryDriver);
				getFIB().getSymbolLibraryMgr().addLibrary(newDBLibraryDriver);
				targetNewLibrary.unloadChildren();
				logInformation(srcStamps.stream().map(IStamp::getName).collect(Collectors.toList()),
						targetNewLibrary.getName(), isDuplicate);
			}
			else {
				targetNewLibrary.unload();
				getFIB().getSymbolLibraryMgr().removeLibrary(targetNewLibrary);
				targetNewLibrary.setLibraryMgr(null);
			}

			return true;
		}

		protected void reportErrorMessage(String error)
		{
			errorInTab = true;
			sendMessageToOutputWindow(HTMLHelper.color(IColor.RED, error));
		}

		protected String getDisplayValueOfCommand()
		{
			return "";
		}

		protected boolean prepareSourceLibrary()
		{
			final LibraryLockRefreshStatus refreshStatus = refreshAndCheckExistence(sourceLibrary, libsChanged);
			return showLockRefreshError(refreshStatus);
		}

		protected boolean showLockRefreshError(@NotNull LibraryLockRefreshStatus refreshStatus)
		{
			if (!refreshStatus.isSuccessful()) {
				reportLibraryLockRefreshFailure(sourceLibrary, refreshStatus, getUsetAction());
				return false;
			}
			return true;
		}
	}

	/**
	 * Command to Move a list of stamps (symbols or borders) to a single target library.
	 * <p>
	 * The source objects must be from the same source library and the target library is selected interactively by this
	 * command.
	 * <p>
	 * This command is currently only used by the Symbol lifecycle and is an inner class because it depends on methods
	 * there.  It is not clear if the class could be extracted out of here or if it could be made independent of the UI
	 * (which is usually the case for Commands)
	 */
	protected class MoveStampCommand extends DuplicateMoveStampCommand
	{

		MoveStampCommand(IAbstractLibrary library, List<?> context)
		{
			super(library, context);
		}

		protected MoveSymbolDialog createDialog()
		{

			DuplicateMoveSymbolTargetLibraries targetLibraries =
					new DuplicateMoveSymbolTargetLibraries(sourceLibrary, false);

			String type = sourceLibrary.getType().getAsLocaleSpecificName();
			String title = ResourceMgr.getString(Lifecycle.class, "MoveSymbolDialog.move.title", type);
			return createMoveDialog(getDialogFrame(), targetLibraries, title, sourceLibrary);
		}

		protected boolean doExecute()
		{
			boolean success;
			if (m_isNewLibrary) {
				success = false;
				try {
					success = handleAddToNewLibrary(m_newLibraryName, newLibraryDomain, false);
				}
				catch (IOException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}
			else {
				if (!checkLibraryExists(targetLibrary)) {
					reportLibraryDoesNotExist(targetLibrary, getUsetAction());
					// target lib. deleted in another session whilst dialog was active!
					return false;
				}
				if (!checkLibraryExists(sourceLibrary)) {
					reportLibraryDoesNotExist(sourceLibrary, getUsetAction());
					// source lib. deleted in another session whilst dialog was active - shouldn't happen
					assert false : "Source library should have been locked";
					return false;
				}

				// move each border/symbol
				// name clash checking is only done at this stage, doing it earlier would be more work
				DuplicateMoveSymbolAddRemoveHelper addRemoveHelper =
						new MoveSymbolAddRemoveHelper(targetLibrary, targetFolder);

				DuplicateMoveSymbol resolveNameClashes =
						new DuplicateMoveSymbol(addRemoveHelper, targetLibrary, this, false);

				List<String> namesOfSymbolsBeingMoved =
						srcStamps.stream().map(IStamp::getName).collect(Collectors.toList());
				for (IStamp stamp : srcStamps) {
					moveStamp(stamp, resolveNameClashes);
				}
				if (addRemoveHelper.isTargetLibraryModified()) {
					libsChanged.add(targetLibrary);
					logInformation(namesOfSymbolsBeingMoved, targetLibrary.getName(), false);
				}
				success = true;
			}

			// update the source/target folder mgrs - libraries have already been saved per stamp moved
			updateFolders();
			return success;
		}

		private void moveStamp(IStamp stamp, DuplicateMoveSymbolHelper duplicateMoveSymbolHelper)
		{
			assert sourceLibrary != targetLibrary;
			String sourceFolder = folderNameOfStamp.get(stamp.getUID());

			// check for name clash in target library, this may issue a UI prompt if a name clash occurs
			String type = stamp.getContainerLibrary().getType().getAsLocaleSpecificName();
			String title = ResourceMgr.getString(Lifecycle.class, "MoveSymbolDialog.move.title", type);
			IStamp stampAfterMoved;

			try {
				stampAfterMoved = duplicateMoveSymbolHelper.addSymbolToTarget(stamp, title);
			}
			catch (IOException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				return;
			}
			if (stampAfterMoved == null) {

				reportErrorMessage(getErrorHeading() + ": " + duplicateMoveSymbolHelper.getError());

				return;
			}
			//save the symbol. The symbol has changes for parent library id and name changes in case of nameclash.

			// Add the symbol back to the database as a member of the new library
			saveStampInTargetLibrary(stamp);

			//Audit Symbol moved
			String symbolLibraryTypeName = sourceLibrary.getType().getAsLocaleSpecificName();
			IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
			// dts0100631944
			String objectName;
			if (sourceFolder != null && !sourceFolder.isEmpty()) {
				objectName = sourceFolder + IFolder.FOLDER_SEPERATOR + stamp.getName();
			}
			else {
				objectName = stamp.getName();
			}
			auditLogger.postEvent(AuditableEventType.SYMBOL_MOVED,
					symbolLibraryTypeName + ' ' +
							ResourceMgr.getString(Lifecycle.class, "Lifecycle.AuditTrail.MovedFrom"),
					sourceLibrary.getUID().getString(),
					objectName, stamp.getUID().getString());

			auditLogger.postEvent(AuditableEventType.SYMBOL_MOVED,
					symbolLibraryTypeName + ' ' +
							ResourceMgr.getString(Lifecycle.class, "Lifecycle.AuditTrail.MovedTo"),
					targetLibrary.getUID().getString(),
					stamp.getName(), stamp.getUID().getString());

			// put the stamp into the target folder, if one was specified
			libsChanged.add(sourceLibrary);
		}

		protected void saveStampInTargetLibrary(IStamp stamp)
		{
			// Add the symbol back to the database as a member of the new library

			PersistPayload payload =
					SymbolLibraryStorageHelper.saveSymbolLibraryRequest(targetLibrary, Collections.singleton(stamp));
			boolean saved = updateServerData(payload);

			assert saved : "Failed to create stamp in target library";
		}

		protected String getErrorHeading()
		{
			return ResourceMgr.getString(Lifecycle.class, "Lifecycle.MoveFailed.heading");
		}

		@Override protected String getDisplayValueOfCommand()
		{
			return ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.moveaction");
		}

		@Override protected boolean prepareSourceLibrary()
		{
			sourceLibPreLocked = sourceLibrary.isLocked();
			final LibraryLockRefreshStatus lockRefreshStatus = lockAndRefresh(sourceLibrary, libsChanged);
			return showLockRefreshError(lockRefreshStatus);
		}

		@Override SymbolErrorDialog.UserAction getUsetAction()
		{
			return SymbolErrorDialog.UserAction.Move;
		}

		void indicateAboutError()
		{
			if (errorInTab) {
				SymbolErrorDialog
						.reportMoveDuplicateFailedError(sourceLibrary.getType(), SymbolErrorDialog.UserAction.Move);
			}
		}
	}

	@Nullable public static String getSourceFolderName(@Nullable IStamp stamp)
	{
		if (stamp != null) {
			IAbstractLibrary containerLibrary = stamp.getContainerLibrary();
			if (containerLibrary != null) {
				ISymlibFolderMgr folderMgr = containerLibrary.getFolderMgr();
				if (folderMgr != null) {
					ISymbolNode symbolNode = folderMgr.findSymbol(stamp.getUID());
					if (symbolNode != null) {
						IFolder parentFolder = symbolNode.getParent();
						if (parentFolder != null) {
							return parentFolder.getFolderPath();
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Command to Duplicate (copy) a list of stamps (symbols or borders) to a single target library.
	 * <p>
	 * The source objects must be from the same source library and the target library is selected interactively by this
	 * command.
	 * <p>
	 * This command is currently only used by the Symbol lifecycle and is an inner class because it depends on methods
	 * there.  It is not clear if the class could be extracted out of here or if it could be made independent of the UI
	 * (which is usually the case for Commands)
	 */
	protected class DuplicateStampCommand extends DuplicateMoveStampCommand
	{

		DuplicateStampCommand(IAbstractLibrary library, List<?> context)
		{
			super(library, context);
		}

		protected boolean doExecute()
		{
			boolean success;
			if (m_isNewLibrary) {
				success = false;
				try {
					success = handleAddToNewLibrary(m_newLibraryName, newLibraryDomain, true);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				if (!checkLibraryExists(targetLibrary)) {
					reportLibraryDoesNotExist(targetLibrary, getUsetAction());
					// target lib. deleted in another session whilst dialog was active!
					return false;
				}
				if (!checkLibraryExists(sourceLibrary)) {
					reportLibraryDoesNotExist(sourceLibrary, getUsetAction());
					// source lib. deleted in another session whilst dialog was active - shouldn't happen
					assert false : "Source library should have been locked";
					return false;
				}

				// move each border/symbol
				// name clash checking is only done at this stage, doing it earlier would be more work

				Set<IPSMStamp> newStamps = new HashSet<IPSMStamp>();

				DuplicateMoveSymbolAddRemoveHelper addRemoveHelper =
						new DuplicateSymbolAddRemoveHelper(targetLibrary, targetFolder);

				DuplicateMoveSymbol duplicateSymbol =
						new DuplicateMoveSymbol(addRemoveHelper, targetLibrary, this, false);
				List<String> successfullyDuplicatedSymbolNames = new ArrayList<>();
				for (IPSMStamp stamp : srcStamps) {
					// TODO DSI-Team - consider using IPersistenceSession.batchLock()/batchAtomicLock() instead for performance
					if (!stamp.lock()) {
						continue;
					}
					IPSMStamp newStamp = duplicateStamp(stamp, duplicateSymbol);
					if (newStamp != null) {
						newStamps.add(newStamp); // this changes each time
						successfullyDuplicatedSymbolNames.add(stamp.getName());
					}
					if (duplicateSymbol.getError() != null) {

						reportErrorMessage(getErrorHeading() + " : " + duplicateSymbol.getError());
					}
				}
				if (addRemoveHelper.isTargetLibraryModified()) {
					libsChanged.add(targetLibrary);
				}

				// save the target lib
				success = true; // return true if nothing happened
				if (!newStamps.isEmpty()) {
					updateFolders();
					success = saveTargetLibrary(newStamps);
					logInformation(successfullyDuplicatedSymbolNames, targetLibrary.getName(), true);
					// TODO jacobt : dts0100492868 - unloading the new stamps magically cured this defect but caused 495732
					// also caused unreported NPE on attempting to duplicate a newly duplicated symbol
					// so this fix is backed out and 492868 needs fixing a better way
				}
			}
			return success;
		}

		protected boolean saveTargetLibrary(Set<IPSMStamp> stamps)
		{
			IBoundaryTransactionMarshaller btm =
					CAFUtils.getInstance().getCHSSystem().getBoundaryTransactionMarshaller();
			btm.enterTransactionBoundary(this, IBoundaryTransactionMarshaller.Nesting.MAIN);
			boolean success = false;
			try {
				saveInternal(targetLibrary, stamps);
				success = true;
			}
			finally {
				btm.exitTransactionBoundary(this, success);
			}
			return success;
		}

		/**
		 * Duplicate a stamp.
		 *
		 * @param stamp                     The stamp to duplicate
		 * @param duplicateMoveSymbolHelper Handles name-clash checks
		 * @return The newly created duplicate
		 */
		@Nullable private IPSMStamp duplicateStamp(IStamp stamp, DuplicateMoveSymbolHelper
				duplicateMoveSymbolHelper)
		{

			// check for name clash in target library, this may issue a UI prompt if a name clash occurs
			String type = stamp.getContainerLibrary().getType().getAsLocaleSpecificName();
			String title = ResourceMgr.getString(Lifecycle.class, "StampNameClashHandler.dialog.title", type);
			IStamp newStamp = null;
			try {
				newStamp = duplicateMoveSymbolHelper.addSymbolToTarget(stamp, title);
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			if (newStamp == null) {

				//Symbol name clash observed and option to not overwrite chosen.
				//In ymbol overwrite option stamp in destination which was meant to be overwritten could be in locked state.
				return null;
			}

			//Audit Symbol duplicated
			String symbolLibraryTypeName = sourceLibrary.getType().getAsLocaleSpecificName();

			// dts0100631944
			String objectName;
			ISymbolNode symbolNode = stamp.getContainerLibrary().getFolderMgr().findSymbol(stamp.getUID());
			String folderPath =
					(symbolNode != null) ? symbolNode.getParent().getFolderPath() : null;
			if (folderPath != null && !folderPath.isEmpty()) {
				objectName = folderPath + IFolder.FOLDER_SEPERATOR + stamp.getName();
			}
			else {
				objectName = stamp.getName();
			}
			IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
			auditLogger.postEvent(AuditableEventType.SYMBOL_DUPLICATED,
					symbolLibraryTypeName, sourceLibrary.getUID().getString(),
					objectName,
					stamp.getUID().getString());

			return (IPSMStamp) newStamp;
		}

		protected MoveSymbolDialog createDialog()
		{
			DuplicateMoveSymbolTargetLibraries targetLibraries =
					new DuplicateMoveSymbolTargetLibraries(sourceLibrary, true);

			String type = sourceLibrary.getType().getAsLocaleSpecificName();
			String title = ResourceMgr.getString(Lifecycle.class, "MoveSymbolDialog.duplicate.title", type);
			MoveSymbolDialog dlg = createMoveDialog(getDialogFrame(), targetLibraries, title, sourceLibrary);
			dlg.setHelpID(dlg.getHelpID() + ".Duplicate"); // based on class name of MoveSymbolDialog
			return dlg;
		}

		protected String getErrorHeading()
		{
			return ResourceMgr.getString(Lifecycle.class, "Lifecycle.DuplicateFailed.heading");
		}

		@Override protected String getDisplayValueOfCommand()
		{
			return ResourceMgr.getString(Lifecycle.class, "Lifecycle.moveorduplicate.duplicateaction");
		}

		@Override SymbolErrorDialog.UserAction getUsetAction()
		{
			return SymbolErrorDialog.UserAction.Duplicate;
		}

		void indicateAboutError()
		{
			if (errorInTab) {
				SymbolErrorDialog.reportMoveDuplicateFailedError(sourceLibrary.getType(),
						SymbolErrorDialog.UserAction.Duplicate);
			}
		}
	}

	public boolean capletWindowClosed(ICapletWindow capWin)
	{
		if (!m_programmaticCloseWindow) {
			// Is the symbol empty? In which case remove (BUT ONLY WHEN THE MODEL IS EDITABLE)
			ICapletModel model = capWin.getController().getCapletModel();
			boolean isLastWindow = capWin.getCaplet().getLifecycle().isLastWindowOnContainer(capWin);
			if (!model.canBePersisted() && model.isEditable() && isLastWindow) {
				// remove
				IUIDObject root = model.getModelRoot();
				if (root instanceof IPSMSymbolDef) {
					System.out.println("Closing symbol: " + root);
					ISymbolDef symbol = (ISymbolDef) root;
					deleteSymbol(symbol.getContainerLibrary(), (IPSMStamp) symbol, true);
				}
			}
		}
		return true;
	}

	private class DuplicateMoveSymbol extends DuplicateMoveSymbolHelper
	{

		private DuplicateMoveStampCommand m_dulicateMoveStampCommand;

		DuplicateMoveSymbol(DuplicateMoveSymbolAddRemoveHelper addRemoveHelper,
				IAbstractLibrary targetLibrary,
				DuplicateMoveStampCommand command, boolean isNewLibrary)
		{
			super(addRemoveHelper, targetLibrary, isNewLibrary);
			m_dulicateMoveStampCommand = command;
		}

		@Override protected PrepareStampResult prepareTargetStamp(IAbstractLibrary lib, IPSMStamp target)
		{
			return m_dulicateMoveStampCommand
					.getPrepareStampResult(lib, target, true, OPEN);
		}

		public SymbolLockAndRefreshHelper getSymbolLockAndRefreshHelper(IAbstractLibrary lib, IStamp target)
		{
			return new SymbolLockAndRefresh(lib, target);
		}
	}

	protected static class DuplicateMoveSymbolTargetLibraries extends SymbolTargetLibrariesFetch
	{

		DuplicateMoveSymbolTargetLibraries(IAbstractLibrary sourceLibrary, boolean listSelf)
		{
			super(sourceLibrary, listSelf, ILibraryAccessConfiguration.DomainEditable);
		}

		public Set<IAbstractLibrary> getAlreadyOpenedSymbolLibraries()
		{
			Set<IAbstractLibrary> openLibraries = new HashSet<IAbstractLibrary>();
			Iterator<IAbstractLibrary> iter = CAFUtils.getInstance().getSymbolLibraryMgr().getOpenLibraries();
			while (iter.hasNext()) {
				openLibraries.add(iter.next());
			}
			return openLibraries;
		}
	}

	private static class SymbolLockAndRefresh extends SymbolLockAndRefreshHelper
	{

		private SymbolLockAndRefresh(IAbstractLibrary lib, IStamp stamp)
		{
			super(lib, stamp);
		}

		protected void handleError(ErrorValue error, Throwable t)
		{
			switch (error) {
				case SYMBOLLIBRARYREFRESHFAILURE: {
					Environment.getExceptionDisplay().displayException(t, false);
					throw new CHS_unwind_error(t);
				}
				case SYMBOLINANOTHERLIBRARYFINDFAILURE: {
					Environment.getExceptionDisplay()
							.displayException(t, "Object Exists " + getStampType() + " request failed");
					break;
				}
			}
		}
	}

	private static class TypeEnumComboBox<T extends IEnumAttribute> extends JComboBox<T>
	{

		private TypeEnumComboBox(final Map<T, String> typeMap, final T[] emptyArray)
		{
			List<T> symbolTypes = new ArrayList<T>(typeMap.keySet());
			Collections.sort(symbolTypes, new Comparator<T>()
			{
				@Override public int compare(T o1, T o2)
				{
					return o1.toDisplayString().compareTo(o2.toDisplayString());
				}
			});
			setModel(new DefaultComboBoxModel<>(symbolTypes.toArray(emptyArray)));

			setRenderer(new DefaultListCellRenderer()
			{
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
						boolean cellHasFocus)
				{
					return super
							.getListCellRendererComponent(list, typeMap.get(value), index, isSelected, cellHasFocus);
				}
			});
		}
	}

	protected static class SymbolCreationParameterHolder implements StampCreationParameterHolder
	{

		protected String symbolName;
		protected SymbolTypeEnum selType;
		protected SymbolSubTypeEnum selSubType;
		protected IAbstractLibrary symbolLib;
		protected SymbolScaleTypeEnum symbolScaleType;
		protected boolean isSymbolResizable;
		protected IUnit gridMapping;
		protected boolean isCanceled = false;

		SymbolCreationParameterHolder(IAbstractLibrary symbolLib)
		{
			this.symbolLib = symbolLib;
		}

		public boolean canceledAction()
		{
			return isCanceled;
		}

		protected Map<SymbolTypeEnum, String> getSymbolTypeEnumStringMap()
		{
			final Map<SymbolTypeEnum, String> typeMap = new HashMap<SymbolTypeEnum, String>();

			if (CapabilityHelper.supports(SupportedFeatureInfo.Feature.HARNESS_BACKSHELL)) {
				typeMap.put(SymbolTypeEnum.BACKSHELL,
						ResourceMgr.getString(Lifecycle.class, "Lifecycle.symbolType.backshell"));
			}
			typeMap.put(SymbolTypeEnum.COMMENT, ResourceMgr.getString(Lifecycle.class, "Lifecycle.symbolType.comment"));
			typeMap.put(SymbolTypeEnum.DEVICE, ResourceMgr.getString(Lifecycle.class, "Lifecycle.symbolType.device"));
			typeMap.put(SymbolTypeEnum.GROUND, ResourceMgr.getString(Lifecycle.class, "Lifecycle.symbolType.ground"));
			if (CapabilityHelper.supports(SupportedFeatureInfo.Feature.LOGIC_SPLICES)) {
				typeMap.put(SymbolTypeEnum.SPLICE,
						ResourceMgr.getString(Lifecycle.class, "Lifecycle.symbolType.splice"));
			}
			if (CapabilityHelper.supports(SupportedFeatureInfo.Feature.FUNCTION)) {
				typeMap.put(SymbolTypeEnum.FUNCTION,
						ResourceMgr.getString(Lifecycle.class, "Lifecycle.symbolType.function"));
			}
			return typeMap;
		}

		protected Map<SymbolSubTypeEnum, String> getSymbolSubTypeEnumStringMap()
		{
			final Map<SymbolSubTypeEnum, String> typeMap = new EnumMap<>(SymbolSubTypeEnum.class);
			for (SymbolSubTypeEnum value : SymbolSubTypeEnum.values()) {
				typeMap.put(value, value.toDisplayString());
			}
			return typeMap;
		}

		@SuppressWarnings("ZeroLengthArrayAllocation")
		public void collectParamsForCreation(Frame frame)
		{
			final Map<SymbolTypeEnum, String> typeMap = getSymbolTypeEnumStringMap();
			JComboBox<SymbolTypeEnum> symbolType = new TypeEnumComboBox<>(typeMap, new SymbolTypeEnum[0]);
			symbolType.setName("Symboltype");

			final Map<SymbolSubTypeEnum, String> subTypeMap = getSymbolSubTypeEnumStringMap();
			JComboBox<SymbolSubTypeEnum> symbolSubType = new TypeEnumComboBox<>(subTypeMap, new SymbolSubTypeEnum[0]);
			symbolSubType.setSelectedItem(SymbolSubTypeEnum.GENERIC); // Legacy
			symbolSubType.setName("CommentSubType");
			final JLabel symbolSubTypeLabel = new JLabel(ResourceMgr.getStringForLabel(Lifecycle.class,
					"Lifecycle.OptionPane_3.text"));
			// initially it is false as default is DEVICE
			symbolSubType.setVisible(false);
			symbolSubTypeLabel.setVisible(false);

			JLabel label = new JLabel(ResourceMgr.getStringForLabel(Lifecycle.class, "Lifecycle.symbolType.text"));
			PropertyOptionPane pop = new PropertyOptionPane();
			pop.setTextSize(12);
			final SymbolTypeEnum defaultSymbolType = SymbolTypeEnum.DEVICE;
			final NewSymbolDialog symbolDialog =
					createNewSymbolDialog(frame, ResourceMgr.getString(Lifecycle.class, "Lifecycle.OptionPane_2.text"),
							defaultSymbolType);
			symbolType.addItemListener(new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					SymbolTypeEnum symbolType = CommonUtils.cast(e.getItem(), SymbolTypeEnum.class);
					if (symbolType != null) {
						symbolDialog.setSymbolType(symbolType);
						if (areSymbolSubTypesSupported()) {
							if (symbolType == SymbolTypeEnum.COMMENT) {
								symbolSubType.setVisible(true);
								symbolSubTypeLabel.setVisible(true);
							}
							else {
								symbolSubType.setVisible(false);
								symbolSubTypeLabel.setVisible(false);
							}
						}
					}
				}
			});
			symbolType.setSelectedItem(defaultSymbolType);
			final String firstRowLabel = ResourceMgr.getStringForLabel(Lifecycle.class, "Lifecycle.OptionPane_1.text");
			symbolName = pop.getEnteredValue(symbolDialog, firstRowLabel, label, symbolType, symbolSubTypeLabel,
					symbolSubType, symbolDialog.getGridSettings().getPanel());
			isCanceled = symbolDialog.isCancelled();
			if (symbolDialog.isCancelled() || StringUtils.isBlank(symbolName)) {
				return;
			}
			selType = (SymbolTypeEnum) symbolType.getSelectedItem();
			selSubType = (SymbolSubTypeEnum) symbolSubType.getSelectedItem();
			symbolScaleType = symbolDialog.getGridSettings().getSymbolScaleType();
			isSymbolResizable = symbolDialog.getGridSettings().isSymbolResizabale();
			gridMapping = symbolDialog.getGridSettings().getGridMapping();
		}

		protected boolean areSymbolSubTypesSupported()
		{
			return Environment.isLayoutDesignSupported();
		}

		@NotNull protected NewSymbolDialog createNewSymbolDialog(Frame frame, String title,
				@NotNull SymbolTypeEnum defaultSymbolType)
		{
			return new NewSymbolDialog(frame, title, symbolLib, defaultSymbolType);
		}

		@Nullable public IPSMStamp createStampBasedOnParameters()
		{
			ISchemFactory schemFactory = FactoryMgr.getSchemFactory();

			IPSMSymbolDef symbolStamp = SymbolUtils.createSymbol(symbolLib, selType, symbolName, schemFactory);

			if (symbolStamp != null) {

				IPSMSymbolDef symbolDef = symbolStamp;

				if (symbolDef.getSymbolType().isResizeable()) {
					symbolDef.setResizable(isSymbolResizable);
				}
				if (symbolDef.getSymbolType().isSymbolScaleSupported()) {
					symbolDef.setSymbolScaleType(symbolScaleType);
					symbolDef.getGrid().setRealMapping(gridMapping);
				}
				symbolDef.setSymbolSubType(selSubType);
				IAuditTrailLogger auditLogger = FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
				auditLogger.postEvent(AuditableEventType.SYMBOL_CREATED,
						SymbolLibraryTypeEnum.SYMBOL.getAsLocaleSpecificName(),
						symbolLib.getUID().getString(), symbolStamp.getName(), symbolStamp.getUID().getString());
			}

			return symbolStamp;
		}

		@Nullable @Override public UserActionFailureReason validateCreationParameters()
		{
			if (symbolName == null || StringUtils.isBlank(symbolName.trim())) {
				return UserActionFailureReason.IGNOREFAILURE;
			}

			// PW - 04/14/03 - Defect#3059
			// Symbol name should be unique within a library
			// Check to make sure the name is unique
			return checkValidityOfSymbolName(symbolName, symbolLib, null);
		}

		@Override public String getName()
		{
			return symbolName;
		}
	}

	@Nullable public IUIDObject getSaveableObjectFromDiagram(@Nullable IBaseDiagram diagram)
	{
		ISheetAdapter adaptor = (ISheetAdapter) diagram;
		return adaptor == null ? null : adaptor.getSymbol();
	}

	@Nullable public IUIDObject getSaveableObjectParent(@Nullable IUIDObject obj)
	{
		return obj instanceof IStamp ? ((IStamp) obj).getContainerLibrary() : null;
	}

	@Override public void takeAllOpenedCapletWindowsAndSave(List<ICAFWindow> windows)
	{

		IAbstractLibrary library = null;
		Collection<IPSMStamp> symbols = new ArrayList<>();
		Collection<Model> models = new ArrayList<>();
		for (ICAFWindow obj : windows) {
			ICapletWindow capletWindow = (ICapletWindow) obj;
			Model model = (Model) capletWindow.getController().getCapletModel();
			models.add(model);
			IPSMStamp stamp = model.getSymbolDef();
			if (library == null) {
				library = model.getLibrary();
			}
			StringBuilder duplicatePinBuffer = new StringBuilder();
			StringBuilder duplicatePortBuffer = new StringBuilder();
			StringBuilder danglingLinkBuffer = new StringBuilder();
			StringBuilder outOfDateBlockBuffer = new StringBuilder();

			if (model.isModified()) {
				model.setTransitional(false);
			}
			model.getController().getSelectMgr().getCurrentSelections().clear();
			CreationDeletionHelper.getTheCreationHelper().clear();
			preSaveSymbol(model, stamp);
//			if (model.canBePersisted() &&
//					prepareStampForSave(library, duplicatePinBuffer, duplicatePortBuffer, danglingLinkBuffer,
//							outOfDateBlockBuffer,
//							stamp)) {
//				symbols.add(stamp);
//			}
			symbols.add(stamp);
		}
		saveInternal(library, symbols);
		for (Model m : models) {
			resetModel(m);
		}
	}
}




