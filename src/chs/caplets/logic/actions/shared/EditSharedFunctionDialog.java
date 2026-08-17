package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.shared.helper.EditSharedFunctionHandler;
import chs.caplets.logic.actions.shared.helper.EditSharedPinlistHandler;
import chs.caplets.logic.actions.shared.helper.ISelectSharedAdapter;
import chs.caplets.logic.actions.shared.helper.SelectSharedHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;

public class EditSharedFunctionDialog extends EditSharedPinlistDialog
{

	public EditSharedFunctionDialog(Frame owner, @Nullable String inTitle, @Nullable final ISharedPinList spl,
			@Nullable chs.cof.logical.cable.IPinList cpl, @Nullable IPinList pl, Model lModel,
			boolean fromSymbol)
	{
		super(owner, inTitle, spl, cpl, pl, lModel, fromSymbol);
	}

	@NotNull @Override protected EditSharedPinlistHandler createHandler(@NotNull EditSharedPinListModel esplModel,
			ILogicDesign logicDesign, @Nullable chs.cof.logical.cable.IPinList cpl)
	{
		return new EditSharedFunctionHandler(esplModel, logicDesign, cpl, this);
	}

	public EditSharedFunctionDialog(Frame owner, @Nullable String inTitle, ISharedPinList spl, Model m)
	{
		this(owner, inTitle, spl, null, null, m, false);
	}

	@Override public void initMapperComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design)
	{
		mapperPanel = new MapPanelForFunction(esplModel, design);
	}

	public void initReuseComponent(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design)
	{
		reusePanel = new ReusePanelForFunction(esplModel, design);
	}

	@NotNull @Override protected SelectSharedPanel createSelectSharedPanel(@NotNull EditSharedPinListModel esplModel,
			@NotNull ILogicDesign design, boolean fromSymbol)
	{
		return new SelectSharedFunctionPanel(esplModel, fromSymbol, design);
	}

	@NotNull @Override protected String getMapPinsTooltip()
	{
		return "EditSharedPinlistDialog.mapports.tooltip";
	}

	@NotNull @Override protected String getMapPinsText()
	{
		return "EditSharedPinlistDialog.mapports.text";
	}

	@NotNull @Override protected String getReusePinsText()
	{
		return "EditSharedPinlistDialog.reuseports.text";
	}

	@NotNull @Override protected String getReusePinsTooltip()
	{
		return "EditSharedPinlistDialog.reuseports.tooltip";
	}

	@NotNull @Override protected String getAddPinsText()
	{
		return "EditSharedPinlistDialog.addports.text";
	}
}
