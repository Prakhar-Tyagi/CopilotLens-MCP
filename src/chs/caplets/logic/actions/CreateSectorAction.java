package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISchemSector;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.common.ICommonFactory;
import chs.common.IParameterized;
import chs.common.IUID;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.logic.SizeHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;

public class CreateSectorAction extends CreateParameterizedObjectAction
{

	private static final String mObjType = "sector";

	public CreateSectorAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected double calculateBorderSize()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		return calculateBorderSize(diagram);
	}

	@Override protected String getObjectType()
	{
		return mObjType;
	}

	@Override public String getActionUIClass()
	{
		return CreateSectorActionUI.class.getName();
	}

	@Override protected IGfxObject createParamObject(Point p1, Point p2)
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gview = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getSheet();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		gp.setNewObject(true);

		// Get our factories
		final ISchemFactory schemFactory = FactoryMgr.getSchemFactory();
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();

		Generator generator = Generator.getGenerator();
		IParameterized params = commonFactory.createParameterized();
		//
		// Get the generator, add the defaults, and go!
		//
		GeneratorStyle gs = generator.getStyle();
		if (gs != null) {
			gs.addDefaults(params, mObjType);
		}

		SizeHelper sizeH = new SizeHelper(p1, p2, params, gp);
		sizeH.setMinModelWidth((int) (gp.getSpacing() * gp.getWidth()));
		int height = sizeH.getModelHeight();
		int width = sizeH.getModelWidth();
		Point lowerLeft = sizeH.getModelLocation();

		// Create visible schem representation & adds pins to it as well as the connectivity.
		IUID uid = commonFactory.createUID();
		ISchemSector schemSector = schemFactory.constructSchemSector(uid, diagram, lowerLeft.x, lowerLeft.y);
		diagram.addObject(schemSector);

		schemSector.setParameterized(params);

		//
		// This area is the extent of the box where the pins would go.
		//
		params.setExtent(commonFactory.constructExtent(0, 0, width, height));
		IECAttributeResolver.inheritIECAttributes(diagram, schemSector, schemSector);
		generator.generateSector(schemSector, gp, Generator.REGENERATE_PROPERTIES);
		return schemSector;
	}

	@Override protected boolean shouldShowFeedback()
	{
		return false;
	}

	@Nullable @Override public String getStatusbarText()
	{
		return ResourceMgr
				.getString(CreateParameterizedObjectAction.class, "CreateParameterizedObjectAction.StatusBar.text");
	}
}
