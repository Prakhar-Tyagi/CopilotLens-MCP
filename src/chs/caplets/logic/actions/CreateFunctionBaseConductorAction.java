package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import org.jetbrains.annotations.NotNull;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CreateFunctionBaseConductorAction extends CreateBaseConductorAction
{

	protected CreateFunctionBaseConductorAction(ICapletController controller)
	{
		super(controller, true, false);
	}

	/**
	 * Access the command object which contains the details of the conductor to be created.
	 * <p/>
	 * This is protected as will only be valid at certain points in the action's lifecyle - it is created on activation
	 * and deleted on action termination
	 *
	 * @return the command object
	 */
	protected CreateSchemConductorCmd getCommand()
	{
		return m_cmd;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_cmd = new CreateSchemConductorCmd();
		return super.onActivate(e);
	}



	@Override protected void assignLibraryDetails()
	{

	}

	/**
	 * Description of the Method
	 *
	 * @param ref_point Description of the Parameter
	 *
	 * @return Description of the Return Value
	 */
	protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		List<Point> vec = new ArrayList<Point>(1);

		vec.add(ref_point);
		return getDynamicGfxService().getFactory().constructPolyline(vec, new Point(0, 0), true, false);
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Class<?> snappingSource()
	{
		return getConductorType();
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Return Value
	 */
	protected Collection<IDynamicGfxMediator> connectingObjects()
	{
		return m_connectingObjects;
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return CAFUtils.getInstance()
				.loadCursor(getController().getCaplet(), getCursorImage(), new Point(7, 7));
	}

	@NotNull
	protected abstract String getCursorImage();
}
