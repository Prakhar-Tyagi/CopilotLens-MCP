package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.cafmain.actions.ans.compare.propagate.actions.logic.DeviceSideConnectorRebuilder;
import chs.caf.cafmain.actions.ans.compare.propagate.actions.logic.IDeviceConnectorRebuilder;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.HarnessConnectorGenerationEnum;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedObjectMgr;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryDevice;
import chs.cof.project.IProject;
import chs.cofUtils.parameterized.GeneratorHCFeedback;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.ctf.caf.utils.CTFLockUpdateHelper;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.IAuditTrailLogger;
import chs.utility.DiagramHelper;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.PinListHelper;
import chs.utility.helpers.revisioning.RevisionHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.Set;

/**
 * @author chandras on 07-07-2017.
 */
public abstract class AbstractChangeFootprintAction extends ControllerActionRT implements ICtxMenuProvider
{

	protected AbstractChangeFootprintAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Nullable protected abstract IDevice determineCandidateDevice(SelectSet selections);

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		SelectSet selections = getController().getSelectMgr().getCurrentSelections();
		IDevice device = determineCandidateDevice(selections);
		if (device == null) {
			return IActionEnum.eCanceled;
		}
		ISharedDevice sharedDevice = CommonUtils.cast(device.getSharedPinList(), ISharedDevice.class);
		if (sharedDevice != null) {
			sharedDevice.refresh();
			if (!sharedDevice.isAccesible()) {
				CTFLockUpdateHelper.displayDomainRestrictionDialog(sharedDevice);
				return IActionEnum.eCanceled;
			}
		}
		return IActionEnum.eCompleted;
	}

	protected IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	protected void rebuildDeviceConnectors(@NotNull IDevice device)
	{
		ILogicModel model = (ILogicModel) getController().getCapletModel();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(model.getDiagram());
		IDeviceConnectorRebuilder rebuilder = new DeviceSideConnectorRebuilder();
		rebuilder.addDeviceToRebuild(device);
		rebuilder.rebuild(gp);
	}

	protected boolean hasGHCCreatedHarnessConnectors(@NotNull IDevice device)
	{
		ILogicDesign logicDesign = device.getLogicDesign();
		if (logicDesign != null && PinListHelper.allowAutoCreation(logicDesign)) {
			//LOGIC-8211 Auto-GHC isn't triggered for DSC with matings defined
			return true;
		}
		for (IConnector connector : device.getConnectors()) {
			if (connector instanceof IHarnessPlugConnector && HarnessConnectorGenerationEnum.TypeUserDefined !=
					((IHarnessPlugConnector) connector).getGenerationType()) {
				return true;
			}
		}
		return false;
	}

	protected void performGHC(@NotNull IDevice device)
	{
		GeneratorHCFeedback feedback = new GeneratorHCFeedback()
		{
			public void outputMessage(String s, boolean writtenSomething)
			{
				getOutputWindow().sendMessage(s, getOutputTabName(), writtenSomething);
			}
		};
		ILogicModel model = (ILogicModel) getController().getCapletModel();
		ISchemDiagram diagram = model.getDiagram();
		GenerateHarnessConnActionHelper ghc = getGenerateHarnessConnActionHelper(feedback, diagram);
		try {
			ISharedPinList sharedPinList = device.getSharedPinList();
			if (sharedPinList != null) {
				ghc.generateHarnessConnectorsForSharedDevice(sharedPinList, device);
			}
			else {
				ghc.generateHarnessConnectorsForDevice(device);
			}
		}
		finally {
			IProject project = model.getDesign().getProject();
			if (project != null) {
				project.getSharedPinListMgr().fireChangeEvent();
			}
		}
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	@NotNull protected GenerateHarnessConnActionHelper getGenerateHarnessConnActionHelper(GeneratorHCFeedback feedback,
			ISchemDiagram diagram)
	{
		return new GenerateHarnessConnActionHelper(diagram, feedback,
				HarnessConnectorGenerationEnum.TypeManuallyGenerated);
	}

	@Nullable protected ILibraryDevice determineCandidateLibraryDevice(@Nullable IDevice device)
	{
		return (device != null) ? CommonUtils.cast(device.getLibraryObject(), ILibraryDevice.class) : null;
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	protected void saveSharedDevice(@NotNull ISharedDevice sharedDevice)
	{
		sharedDevice.saveAndUnlock();
		ISharedObjectMgr sharedObjectMgr = sharedDevice.getSharedObjectMgr();
		if (sharedObjectMgr != null) {
			sharedObjectMgr.fireChangeEvent(Set.of(sharedDevice.getUID()));
		}
		getController().getUndoableContainer().endEdit();
		getController().clearUndoQueue();
		auditTrailLogging(sharedDevice);
	}

	protected void auditTrailLogging(@NotNull ISharedDevice sharedObject)
	{
		IAuditTrailLogger auditLogger = getAuditTrailLogger();
		auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_MODIFIED, getAuditTrailDescription(),
					sharedObject.getProject().getUID().getString(),
					RevisionHelper.getFullName(sharedObject),
					sharedObject.getUID().getString());
	}

	@NotNull
	protected IAuditTrailLogger getAuditTrailLogger()
	{
		return FactoryMgr.getSystemFactory().getCHSSystem().getAuditLogger();
	}

	protected String getAuditTrailDescription()
	{
		return "";
	}
}
