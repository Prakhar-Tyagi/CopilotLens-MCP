package chs.extension.api;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.selection.ISelectListener;
import chs.caplets.logic.Caplet;
import chs.caplets.logic.LogicController;
import chs.caplets.logic.View;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.harness.physical.IClip;
import chs.cof.harness.physical.IGrommet;
import chs.cof.harness.physical.IOtherComponent;
import chs.cof.harness.shared.IHarnessBase;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IRingTerminal;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.topology.physical.IBundle;
import chs.cof.topology.physical.INode;
import chs.cof.topology.physical.IReferenceNode;
import chs.cof.topology.physical.IStructureNode;
import chs.common.IDesignContainer;
import org.jetbrains.annotations.Nullable;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: kpaul Date: Oct 3, 2010 Time: 11:07:44 AM To change this template use File | Settings
 * | File Templates.
 */
//ToDO - Needs to be rearranged later. Most of this not applicable as of now
public class MutableLogicCreationHelper extends MutableCAFCreationHelper
{

	public IStructureNode createStructureNode(Point2D point, IHarnessBase harn)
	{
		return null; 
	}

	public IReferenceNode createReferenceNode(IBundle bun, double offset, IHarnessBase harn)
	{
		return null;
	}

	@Nullable public IBundle createBundle(IStructureNode startNode, IStructureNode endNode, List<Point2D> points, IHarnessBase harn)
	{
		return null;
	}

	@Nullable public IConnector createConnector(IStructureNode node, IHarnessBase harn)
	{
		return null;
	}

	@Nullable public IRingTerminal createRingTerminal(IStructureNode node, IHarnessBase harn)
	{
		return null;
	}

	@Nullable public IClip createClip(INode node, IHarnessBase harn)
	{
		return null;
	}

	@Nullable public IGrommet createGrommet(INode node, IHarnessBase harn)
	{
		return null;
	}

	@Nullable public IOtherComponent createOtherComponent(INode node, IHarnessBase harn)
	{
		return null;
	}
	public void setActiveViewForTests(IDesignContainer design, IBaseDiagram diagram)
	{
		ICaplet caplet = Caplet.getCaplet();
		final LogicController controller = new LogicController(caplet, (ILogicDesign) design, (ISchemDiagram)diagram);
		View view = new MockLogicView(controller);
		view.setDiagram(diagram);
		CAFUtils.getInstance().setActiveCapletViewForTests(view);

		// get undo to hang together when there is not a window (!)
//		CAFUtils.getInstance().setTempUndoableContainer(controller.getUndoableContainer());

		// workaround various other problems due to there not being a window (e.g. null active datatransfer)
		CAFUtils.getInstance().setActiveCapletControllerForTests(controller);
	}
	/**
	 * A "mock" logic view for use in unit tests.
	 * <p/>
	 * This one works fine by extending the base class to put some UI listeners out of action
	 */
	public static class MockLogicView extends View
	{

		MockLogicView(ICapletController controller)
		{
			super(controller.getCapletModel(), null);
			controller.getCapletModel().removeModelChangeListener((IModelChangeListener) getDynamicGfxService());

			// hackaround for "java.lang.AssertionError: Select listener being removed not present ..."
			controller.getSelectMgr().addSelectListener((ISelectListener) getDynamicGfxService());
			controller.getSelectMgr().removeSelectListener((ISelectListener) getDynamicGfxService());
		}

		/**
		 * Overridden here to create the graphics context used by some drawing actions (e.g. use of GfxContext to identify
		 * overlapping graphics).
		 *
		 * @param diagram The diagram of the view
		 */
//		public void setDiagram(IBaseDiagram diagram)
//		{
//			super.setDiagram(diagram);
//			m_drawingCanvas = new MockDrawingComponent(this);
//			m_drawingCanvas.createContext();
//		}
	}
}