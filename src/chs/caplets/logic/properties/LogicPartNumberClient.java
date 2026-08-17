package chs.caplets.logic.properties;

import chs.cof.library.ILibrariedObject;
import chs.cof.library.SymbolContextEnum;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.project.IProject;
import chs.ctf.editui.ModularConnectorPartNumberClient;
import chs.ctf.editui.ModularConnectorPartNumberUIProperty;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LogicPartNumberClient extends ModularConnectorPartNumberClient
{

	private ILogicModel m_logicModel = null;
	private boolean m_respondDeleteBtnToPartNumberChanges;

	public LogicPartNumberClient(ILogicModel model, ILibrariedObject libObj, SymbolContextEnum symbolContext)
	{
		super(libObj, symbolContext);
		m_logicModel = model;
		m_respondDeleteBtnToPartNumberChanges = true;
	}

	@Nullable
	public String getCustomerName()
	{
		return null;
	}

	public IPropertyGroup getUI()
	{
		if (m_partNumberUI == null) {
			m_partNumberUI = new ModularConnectorPartNumberUIProperty(this, false, ConfigurationTypeEnum.LOGICAL);
		}
		return m_partNumberUI;
	}

	@Override
	public boolean allowDeleteBtnToRespondToPartNumberChanges()
	{
		return m_respondDeleteBtnToPartNumberChanges;
	}

	public List<String> getPreferencedSymbolLibraries()
	{
		return null;
	}

	public IProject getProject()
	{
		return m_logicModel.getDesign().getProject();
	}

	public boolean showLibraryPartButtons()
	{
		return false;
	}
}
