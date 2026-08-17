package chs.caplets.logic.actions;

import chs.analysis.AnalysisServices;
import chs.analysis.CapitalAnalysisFactory;
import chs.analysis.ICapitalAnalysis;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAnalysableSymbolAssociatable;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.ui.SymbolAssociationDlg;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.MessageHelper;
import chs.utility.SymbolUtils;
import chs.utility.helpers.AnalysableSymbolAssociater;
import chs.utility.helpers.AssociatabilityReporter;
import chs.utility.helpers.IAnalysableSymbolAssociatableValidator;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 16/07/12 Time: 11:07 To change this template use File | Settings |
 * File Templates.
 */
public class AssociateSymbolAction extends ControllerActionRT
{

	protected IUID m_selectedSymbolUID = null;
	private AnalysableSymbolAssociater m_associater = null;

	public AssociateSymbolAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		Collection<IAnalysableSymbolAssociatable> analysableSymbolAssociatables =
				AnalysableSymbolAssociater.getAnalysableSymbolAssociatables(getOperands());
		AssociatabilityReporter associatabilityReporter =
				new GUIAssociatabilityReporter(CAFUtils.getInstance().getDialogFrame());
		m_associater = new AnalysableSymbolAssociater(
				analysableSymbolAssociatables, associatabilityReporter);
		m_associater.addValidator(new SharedObjectAssociatabilityValidator());

		if (!m_associater.validate()) {
			return IActionEnum.eCanceled;
		}

		if (!selectSymbol(getAnalysableSymbol(analysableSymbolAssociatables))) {
			return IActionEnum.eCanceled;
		}

		return IActionEnum.eCompleted;
	}

	private String getAnalysableSymbol(Collection<IAnalysableSymbolAssociatable> analysableSymbolAssociatables)
	{
		IUID analysableSymbolUID = null;
		for (IAnalysableSymbolAssociatable associatable : analysableSymbolAssociatables) {
			IUID uid = associatable.getAnalysableSymbolUID();
			if (analysableSymbolUID == null) {
				analysableSymbolUID = uid;
			}
			if (analysableSymbolUID != uid) {
				analysableSymbolUID = null;
				break;
			}
		}
		return SymbolUtils.getNameAndPathOfSymbol(analysableSymbolUID);
	}

	protected boolean selectSymbol(String analysableSymbolPath)
	{
		SymbolAssociationDlg dialog =
				new SymbolAssociationDlg(CAFUtils.getInstance().getDialogFrame(), analysableSymbolPath);
		dialog.setVisible(true);
		if (dialog.isCancelled()) {
			return false;
		}
		m_selectedSymbolUID = dialog.getSelectedSymbol();

		return true;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			assert m_associater != null;
			m_associater.associate(m_selectedSymbolUID);
			ICapitalAnalysis m_capitalAnalysis = CapitalAnalysisFactory.getAnalysisInterface();
			if (m_capitalAnalysis != null) {
				String associatedAnalysisModel =
						AnalysisServices.exportAssociatedSymbol(SymbolUtils.getSymbolDef(m_selectedSymbolUID));
				if (!StringUtils.isBlank(associatedAnalysisModel.trim())) {
					m_capitalAnalysis.generateModel(StringUtils.getBytes(associatedAnalysisModel));
					((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(
							(Model) getController().getCapletModel(), false);
				}
			}
		}

		return true;
	}

	@Override public boolean isEnabled()
	{
		ICapletModel model = getController().getCapletModel();
		if (model instanceof ILogicModel) {
			ILogicDesign logicDesign = ((ILogicModel) model).getDesign();
			if (logicDesign.isUnderConcurrentEdit()) {
				return false;
			}
		}
		if (!getController().getCapletModel().isEditable()) {
			return false;
		}
		Collection<IUIDObject> operands = getOperands();
		if (operands == null) {
			return false;
		}
		Collection<IAnalysableSymbolAssociatable> analysableSymbolAssociatables =
				AnalysableSymbolAssociater.getAnalysableSymbolAssociatables(operands);

		boolean enabled = !hasSharedObjects(analysableSymbolAssociatables);

		return enabled && !analysableSymbolAssociatables.isEmpty() &&
				!AnalysableSymbolAssociater.hasInvalidObjects(analysableSymbolAssociatables);
	}

	private boolean hasSharedObjects(Collection<IAnalysableSymbolAssociatable> analysableSymbolAssociatables)
	{
		for (IAnalysableSymbolAssociatable associatable : analysableSymbolAssociatables) {
			ISharedObject sharedObject = ReferenceHelper.reduceToSharedObject(associatable);
			if (sharedObject != null) {
				return true;
			}
		}
		return false;
	}

	@Nullable private Collection<IUIDObject> getOperands()
	{
		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		if (preSelections.isEmpty()) {
			return null;
		}
		List<IUIDObject> selectedObjects = preSelections.getSelectedObjects(IUIDObject.class);
		for (IUIDObject selectedObject : selectedObjects) {
			IAnalysableSymbolAssociatable associatable =
					ReferenceHelper.reduceToAnalysableSymbolAssociatable(selectedObject);
			if (associatable == null) {
				if (selectedObject instanceof IDiagramObject) {
					IDiagramObject parent = ((IDiagramObject) selectedObject).getParent();
					if (parent == null) {
						return null;
					}
					if (!selectedObjects.contains(parent)) {
						return null;
					}
				}
			}
		}
		return selectedObjects;
	}

	@Override public String getActionUIClass()
	{
		return AssociateSymbolActionUI.class.getName();
	}

	private static class SharedObjectAssociatabilityValidator implements IAnalysableSymbolAssociatableValidator
	{

		@Override public boolean validate(IAnalysableSymbolAssociatable symbolAssociatable,
				AssociatabilityReporter associatabilityReporter)
		{
			ISharedLockableUpdateableObject sharedLockableUpdateableObject =
					ReferenceHelper.reduceToSharedUpdateableObject(symbolAssociatable);
			return validateSharedObject(sharedLockableUpdateableObject, associatabilityReporter);
		}

		private boolean validateSharedObject(ISharedLockableUpdateableObject sharedObject,
				AssociatabilityReporter associatabilityReporter)
		{
			if (sharedObject == null) {
				return true;
			}

			boolean isLocked = sharedObject.isLocked();
			boolean isFrozen = sharedObject.isFrozen();

			if (!isLocked) {
				associatabilityReporter.report(ResourceMgr
						.getString(AssociateSymbolAction.class, "associatesymbol.validate.sharedobject.unlocked",
								sharedObject.getName()));
			}
			else if (isFrozen) {
				associatabilityReporter.report(ResourceMgr
						.getString(AssociateSymbolAction.class, "associatesymbol.validate.sharedobject.frozen",
								sharedObject.getName()));
			}

			return sharedObject.isLocked() && !sharedObject.isFrozen();
		}
	}

	private static class GUIAssociatabilityReporter implements AssociatabilityReporter
	{

		private StringBuilder stringBuilder = new StringBuilder();
		private Frame dialogFrame = null;

		private GUIAssociatabilityReporter(Frame dialogFrame)
		{
			this.dialogFrame = dialogFrame;
		}

		@Override public void report(String message)
		{
			stringBuilder.append(message).append(System.getProperty("line.separator"));
		}

		@Override public void showReport()
		{
			MessageHelper.showErrorMessage(dialogFrame, stringBuilder.toString());
		}
	}
}
