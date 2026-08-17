package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.shared.autoshare.AbstractAutoShareExecutor;
import chs.caplets.logic.actions.shared.autoshare.AutoShareParams;
import chs.caplets.logic.actions.shared.autoshare.FetchOffPageAutoShareExecutor;
import chs.caplets.logic.harness.propagate.AutoPropagateHarnessController;
import chs.caplets.shared.ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUIDObject;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.WrappingRuntimeException;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BulkAutoShareCmd extends AbstractBulkAutoShareCmd
{

	@NotNull protected SetMap<ISchemDiagram, ? extends IUIDObject> m_objectsToBeShared;
	private boolean m_continueToShareAfterFailure;
	@NotNull private final AutoShareParams m_params;

	public BulkAutoShareCmd(@NotNull SetMap<ISchemDiagram, ? extends IUIDObject> diagramObjectsToBeShared,
			@NotNull ILogicDesign sourceDesign, @NotNull IMessageReporterWithContext messageReporter)
	{
		this(diagramObjectsToBeShared, sourceDesign, messageReporter, false, new AutoShareParams(false, true, true));
	}

	public BulkAutoShareCmd(@NotNull SetMap<ISchemDiagram, ? extends IUIDObject> diagramObjectsToBeShared,
			@NotNull ILogicDesign sourceDesign, @NotNull IMessageReporterWithContext messageReporter,
			boolean continueToShareAfterFailure, @NotNull AutoShareParams params)
	{
		super(sourceDesign, messageReporter);
		m_objectsToBeShared = new SetMap<>(diagramObjectsToBeShared);
		m_continueToShareAfterFailure = continueToShareAfterFailure;
		m_params = params;
	}

	@Override protected void doEnd(boolean executeOk)
	{
		AutoPropagateHarnessController.getInstance().clearHarnessPropagateWindow();
		super.doEnd(executeOk);
	}

	@Override protected boolean doExecute()
	{
		ICapletController sourceDesignController = CAFUtils.getInstance().getControllerForDesign(m_design);
		try (UndoIdlerForForeignDesignChanges ignored = new UndoIdlerForForeignDesignChanges(sourceDesignController)) {
			for (ISchemDiagram sourceDiagram : m_objectsToBeShared.keySet()) {
				Set<? extends IUIDObject> candidateObjects = m_objectsToBeShared.get(sourceDiagram);
				AbstractAutoShareExecutor autoShareExecutor = getAutoShareExecutor(sourceDiagram);
				autoShareExecutor.setAuditObjUIDConsumer((logId) -> m_storedAuditLogIds.add(logId));
				for (IUIDObject candidateObj : candidateObjects) {
					List<IUIDObject> objectsToBeShared = getObjectsToBeShared(candidateObj);
					for (IUIDObject objectToBeShared : objectsToBeShared) {
						if (!doShare(autoShareExecutor, objectToBeShared)) {
							LogHelper.debugMsgSafe(getDebugMessage(objectToBeShared));
							if (!m_continueToShareAfterFailure) {
								return false;
							}
						}
					}
				}
			}
			try {
				saveAll();
			}
			catch (UserSessionException ex) {
				throw new WrappingRuntimeException(ex);
			}
		}
		return true;
	}

	@NotNull private String getDebugMessage(@NotNull IUIDObject objectToBeShared)
	{
		final ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(objectToBeShared);
		final String name = logicObject != null ? logicObject.getName() : StringUtils.EMPTY_STRING;
		return "Failed to share " + name + " , UID: " + objectToBeShared.getUID().getString();
	}

	@NotNull protected AbstractAutoShareExecutor getAutoShareExecutor(@Nullable ISchemDiagram sourceDiagram)
	{
		return new FetchOffPageAutoShareExecutor(m_project, m_design, sourceDiagram, m_messageReporter, m_params);
	}

	// We have to share non schematic conductors present in highway along with it
	@NotNull private List<IUIDObject> getObjectsToBeShared(@NotNull IUIDObject candidateObject)
	{
		List<IUIDObject> objectsToBeShared = new ArrayList<>();
		objectsToBeShared.add(candidateObject);
		if (candidateObject instanceof IHighwaySchematic) {
			for (IConductor conductor : HighwayHelper.toStackPinConductors(
					((IHighwaySchematic) candidateObject).getConnectivity())) {
				objectsToBeShared.add(conductor);
			}
		}
		return objectsToBeShared;
	}

	@Override protected void revertMemoryState()
	{
		getCommandHelper().unloadDesign(m_design);
		super.revertMemoryState();
	}

	private void saveAll() throws UserSessionException
	{
		saveSharedPinlistMgr();
		getCommandHelper().saveDesign(m_design);
		getCommandHelper().setDesignModifiedFlag(m_design, false);
	}
}
