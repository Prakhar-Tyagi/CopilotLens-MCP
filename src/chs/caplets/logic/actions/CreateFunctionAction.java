package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.ICommonFactory;
import chs.common.IUID;
import chs.images.CHSImages;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;

import java.awt.Cursor;
import java.awt.Point;

/**
 * Created with IntelliJ IDEA. User: kajay Date: 21/3/14 Time: 12:37 PM To change this template use File | Settings |
 * File Templates.
 */
public class CreateFunctionAction extends CreateParameterizedObjectAction
{

	protected static Cursor cursor = null;

	private static final String m_objType = "function";

	public CreateFunctionAction(ICapletController controller)
	{
		super(controller);
		if (cursor == null) {
			cursor = CAFUtils.getInstance().loadCursor(controller.getCaplet(), CHSImages.FUNCTION_ADD_CURSOR,
					new Point(7, 7));
		}
	}

	protected String getObjectType()
	{
		return m_objType;
	}

	protected double calculateBorderSize()
	{
		return calculateBorderSize(getSchemDiagram());
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateFunctionActionUI.class.getName();
	}

	protected IGfxObject createParamObject(Point p1, Point p2)
	{
		// Create our connectivity device
		IFunction device = createConnectivityFunction();

		// Create our schem device
		return createSchemDevice(device, p1, p2, shouldAddPins(), getRotationIndicator());
	}

	private IFunction createConnectivityFunction()
	{
		final ICableFactory cblFactory = FactoryMgr.getCablePropertiedFactory();
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		IUID uid = commonFactory.createUID();
		IFunction function = cblFactory.createFunction(uid);
		ISchemDiagram diagram = getSchemDiagram();
		ILogicDesign design = diagram.getDesign();
		assert design != null;
		IConnectivity conn = design.getConnectivity();
		assert conn != null; // to keep IJ happy
		conn.addFunction(function);
		return function;
	}

	public static IPinList createSchemDevice(IFunction function, Point pt1, Point pt2, boolean addPins,
			DynamicRotationIndicator indicator)
	{
		IPinList schemPinList = createSchemPinList(function, pt1, pt2, addPins, indicator, m_objType);
		return schemPinList;
	}

	private static ISchemDiagram getSchemDiagram()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		return (ISchemDiagram) gview.getSheet();
	}

	/**
	 * Should this action automatically add pins to the device?
	 * <p/>
	 * The default behaviour of Add Device with/without Pins is toggled by pressing Ctrl as the device is placed.
	 *
	 * @return boolean
	 */
	protected boolean shouldAddPins()
	{
		return true;
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return cursor;
	}

	@Override protected String getFeedbackResourceString()
	{
		return "CreateParameterizedObjectAction.FunctionFeedback.text";
	}

	@Override protected String getRotatedFeedbackResourceString()
	{
		return "CreateParameterizedObjectAction.FunctionRotatedFeedback.text";
	}
}

