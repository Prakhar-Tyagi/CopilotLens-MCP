package chs.caplets.logic;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.IEditClient;
import chs.caf.caplet.action.IDeferredAction;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.ConvertInlineToPlugJackPairAction;
import chs.caplets.logic.actions.CreateModularSchematicsAction;
import chs.caplets.logic.actions.DisconnectAction;
import chs.caplets.logic.actions.JoinPinlistsAction;
import chs.caplets.logic.actions.ManageConnectorsAction;
import chs.caplets.shared.BaseController;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.ctf.editui.LogicEditSelectionHelper;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractLogicDerivativeController extends BaseController
{

	protected AbstractLogicDerivativeController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		super(caplet, design, diagram); // false means is not Logic

		createLogicControllerActions();

		getDeferredActionProcessor().addDeferredAction(RegenerateGraphicsAction.getInstance());
		getDeferredActionProcessor().addDeferredAction((IDeferredAction) ConductorRouteAction.getInstance());
	}

	protected void createLogicControllerActions()
	{
		super.createLogicControllerActions();
		addAction(new JoinPinlistsAction(this));
		addAction(new DisconnectAction(this));
		addAction(new ManageConnectorsAction(this));
		addAction(new ConvertInlineToPlugJackPairAction(this));
		addAction(new CreateModularSchematicsAction(this));
	}

	protected Class<? extends BaseController> getResourceClass()
	{
		// 	dts0100518262 - use LogicController properties
		return LogicController.class;
	}

	public String getDoubleClickAction()
	{
		LogicEditSelectionHelper hesHelper =
				new LogicEditSelectionHelper(getSelectMgr().getPreSelections());
		return hesHelper.getDoubleClickAction();
	}

	@Nullable public IEditClient getEditClient(SelectSet selections, @Nullable Object owner)
	{
		LogicEditSelectionHelper hesHelper = new LogicEditSelectionHelper(selections);
		return hesHelper.getEditClient(this);
	}
}
