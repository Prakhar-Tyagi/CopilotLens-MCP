package chs.caplets.shared.properties;

import chs.caf.CAFUtils;
import chs.caplets.logic.Model;
import chs.caplets.logic.properties.LogicPartNumberClient;
import chs.caplets.shared.ForeignDesignChangesHandler;
import chs.cof.library.SymbolContextEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.parts.ILibraryObject;
import chs.cofUtils.logical.concurrency.PropertiesConcurrencyHelper;
import chs.ctf.editui.InternalPositionUsageManager;
import chs.ctf.editui.ModularConnectorClientBase;
import chs.ctf.editui.ModularConnectorPartNumberClient;
import chs.ctf.editui.logic.LogicInternalPositionUsageManager;
import chs.ctf.editui.logic.LogicModularConnectorClient;
import chs.ctf.editui.shared.SharedModularConnectorClient;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.PropertyPanel;

import javax.swing.JPanel;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ModularTabDialog
{

	private Model m_model;
	private ModularConnectorClientBase client;

	public ModularTabDialog(Model model)
	{
		m_model = model;
	}

	public JPanel getPanel(IConnector connector)
	{
		InternalPositionUsageManager m_posStatusManager =
				new LogicInternalPositionUsageManager((ILibraryObject) connector.getLibraryObject());

		ModularConnectorPartNumberClient modPartNumberClient =
				new LogicPartNumberClient(m_model, null, SymbolContextEnum.CONTEXT_COMMENT);
		final ISharedConnector sharedConnector = (ISharedConnector) connector.getSharedObject();
		if (sharedConnector == null) {
			client = new LogicModularConnectorClient(CAFUtils.getInstance().getDialogFrame(), connector,
					CAFUtils.getInstance().getCurrentProject(), modPartNumberClient, true, m_posStatusManager);
		}
		else {
			Consumer<ILogicDesign> saveHandler = ForeignDesignChangesHandler.createdSaveHandler();

			Collection<ILogicDesign> logicDesigns = CAFUtils.getInstance().getOpenedDesigns(
					ILogicDesign.class).stream().filter(aDes -> aDes.isLocked()).collect(Collectors.toList());
			client = new SharedModularConnectorClient(CAFUtils.getInstance().getDialogFrame(), sharedConnector,
					CAFUtils.getInstance().getCurrentProject(), modPartNumberClient, false, m_posStatusManager,
					logicDesigns);
			((SharedModularConnectorClient) client).setSaveDesignHandler(saveHandler);
			((SharedModularConnectorClient) client).createBoundsForDesignChanges(
					(design) -> new ForeignDesignChangesHandler.UndoIdlerForForeignDesignChanges(
							CAFUtils.getInstance().getControllerForDesign(design)));
		}
		IPropertyGroup grp = client.getUI();
		boolean status = PropertiesConcurrencyHelper.checkModularConnectorEditability(connector);
		status = status && (sharedConnector == null || (sharedConnector.isLocked() && !sharedConnector.isFrozen()));
		grp.setEditable(status);
		return new PropertyPanel("Modular Tab", grp);
	}

	public ModularConnectorClientBase getClient()
	{
		return client;
	}

	public void editModel()
	{

	}
}