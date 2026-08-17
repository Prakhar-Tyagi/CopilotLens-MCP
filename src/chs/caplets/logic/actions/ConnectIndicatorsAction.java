package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.Model;
import chs.caplets.logic.commands.DaisyChainShieldDeletionCmd;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IShieldBody;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.IndicatorOrientation;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IViewInvalidationEnum;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConnectIndicatorsAction extends ControllerActionRT implements ICtxMenuProvider
{
	// List for storing the shield body which are selected by the user
	private List<IShieldBody> mShieldBodyList;
	private static final int HORIZONTAL_ORIENTATION = 0;
	private static final int VERTICAL_ORIENTATION = 1;
	private int mShieldOrientation;

	public ConnectIndicatorsAction(ICapletController controller)
	{
		super(controller);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	public boolean onTerminate(boolean successful)
	{
		if (successful) {
			// Do the action
			DaisyChainShieldDeletionCmd cmd = new DaisyChainShieldDeletionCmd(mShieldBodyList,
					(Model) getController().getCapletModel(),mShieldOrientation);
			cmd.connectIndicators();
		}

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
		CAFUtils.getInstance().getStatusBar().clear();
		mShieldBodyList.clear();
		return true;
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(ConnectIndicatorsActionUI.class, "ConnectIndicatorsActionUI.logic.shortDesc.decl");
	}

	public String getActionUIClass()
	{
		return ConnectIndicatorsActionUI.class.getName();
	}

	//
	// Context Menu methods
	//
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// Check if the objects selected by the client are valid for
		// this action. If so, put ourselves in the context menu.
		if (isEnabled()) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
		// nothing to do here.
	}

	protected boolean checkIndicatorOrientation()
	{
		SelectSet selections = getController().getSelectMgr().getCurrentSelections();
		SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects();

		// Set for ensuring that action is limited to selection of one indicator per multi-core
		Set<IMulticore> selectedMCs = new HashSet<IMulticore>();
		mShieldBodyList = new ArrayList<IShieldBody>();

		boolean isFinalOrientationDecided = false;
		int finalOrientation = -1;
		int shieldWithHookupsCounter = 0;
		boolean validSelection = true;
		String diagramName = "";
		while (iter.hasNext()) {
			IUIDObject obj = iter.getNext();

			if (obj instanceof IShieldBody) {
				if(! "".equals(diagramName))
				{
					// The shield body might be selected from different diagrams.
					// In that case we should not enable the action.
					if (! diagramName.equalsIgnoreCase(DiagramHelper.getDiagram((IDiagramObject) obj).getName()))
					{
						validSelection = false;
						break;
					}
				}   else
				{
					diagramName = DiagramHelper.getDiagram((IDiagramObject) obj).getName();
				}

				mShieldBodyList.add((IShieldBody) obj);
				if (((IShieldBody) obj).getShieldBodyHookups().size() == 2) {
					shieldWithHookupsCounter++;
				}
				IMulticore mc = ((IShieldBody) obj).getConnectivity().getMulticore();

				if (!selectedMCs.add(mc)) {
					validSelection = false;
					break;
				}
				int orientation = Generator.getShieldBodyOrientation(((IShieldBody) obj));
				int currentOrientation = -1;
				if (IndicatorOrientation.isVertical(orientation)) {
					currentOrientation = VERTICAL_ORIENTATION;
				}
				else if (IndicatorOrientation.isHorizontal(orientation)) {
					currentOrientation = HORIZONTAL_ORIENTATION;
				}
				if (!isFinalOrientationDecided) {
					isFinalOrientationDecided = true;
					finalOrientation = currentOrientation;
				}

				if (finalOrientation != currentOrientation) {
					validSelection = false;
					break;
				}
				mShieldOrientation = finalOrientation;
			}
		}
		// There should be atleast 2 shield body selected which have hookups available.
		return (validSelection && shieldWithHookupsCounter > 1)  ;
	}

	@Override public boolean isEnabled()
	{
		if (!getController().getCapletModel().isEditable()) {
			return false;
		}

		return checkIndicatorOrientation() && super.isEnabled();
	}

	protected int getShieldOrientation()
	{
		return mShieldOrientation;
	}


}
