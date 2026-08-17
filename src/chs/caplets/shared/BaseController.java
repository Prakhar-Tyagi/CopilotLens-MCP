/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022-2026 Siemens
 */
package chs.caplets.shared;

import chs.analysis.AnalysisServices;
import chs.analysis.CapitalAnalysisFactory;
import chs.analysis.IAnalysisColoringProcessor;
import chs.analysis.IAnalysisNetlistScope;
import chs.analysis.IAnalysisNetlistTopoDesignScope;
import chs.analysis.IAnalysisScopedDesignProvider;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionSeparator;
import chs.caf.CAFUtils;
import chs.caf.IActionNode;
import chs.caf.SymbolLibraryBrowser;
import chs.caf.cafmain.actions.ApplyStyleOnDiagramObjectAction;
import chs.caf.cafmain.actions.ApplyStyleToAllAction;
import chs.caf.cafmain.actions.BrowseSelectedObjectAction;
import chs.caf.cafmain.actions.DumpOriginAction;
import chs.caf.cafmain.actions.EditLeaderLineJustificationAction;
import chs.caf.cafmain.actions.EditStyleAction;
import chs.caf.cafmain.actions.ExportAsSymbolAction;
import chs.caf.cafmain.actions.FindReplaceSelectionAction;
import chs.caf.cafmain.actions.ForceApplyStyleAction;
import chs.caf.cafmain.actions.LogicGenerateNamesAction;
import chs.caf.cafmain.actions.MakeBaseIdsSameAction;
import chs.caf.cafmain.actions.MoveToGridAction;
import chs.caf.cafmain.actions.ReloadIndicatorAction;
import chs.caf.cafmain.actions.ReplaceInstanceSymbolAction;
import chs.caf.cafmain.actions.ResetOrderAction;
import chs.caf.cafmain.actions.StyleFlyOutAction;
import chs.caf.cafmain.actions.UpdateBorderAction;
import chs.caf.cafmain.actions.UpdateCompositeTextAction;
import chs.caf.cafmain.actions.UpdateInstanceAction;
import chs.caf.cafmain.actions.WriteToStyleAction;
import chs.caf.cafmain.actions.analysis.AttachSVModelAction;
import chs.caf.cafmain.actions.analysis.EditModelAction;
import chs.caf.cafmain.actions.analysis.ExportNetlistAction;
import chs.caf.cafmain.actions.analysis.SubsystemFMEAAction;
import chs.caf.cafmain.actions.analysis.SubsystemImportAction;
import chs.caf.cafmain.actions.analysis.SubsystemSCAAction;
import chs.caf.cafmain.actions.analysis.SubsystemSimulatorAction;
import chs.caf.cafmain.actions.analysis.SubsystemViewNetlistAction;
import chs.caf.cafmain.actions.analysis.SubsystemViewProjectNetlistAction;
import chs.caf.cafmain.actions.analysis.derating.LogicDeratingAction;
import chs.caf.cafmain.actions.bridges.BridgeInAction;
import chs.caf.cafmain.actions.bridges.BridgeOptionAction;
import chs.caf.cafmain.actions.bridges.BridgeOutAction;
import chs.caf.cafmain.actions.bridges.ConnectedModeBridgeInAction;
import chs.caf.cafmain.actions.bridges.DataExportAction;
import chs.caf.cafmain.actions.bridges.DataExportFilterAction;
import chs.caf.cafmain.actions.bridges.DataImportAction;
import chs.caf.cafmain.actions.bridges.TCBridgeInAction;
import chs.caf.cafmain.actions.bridges.TCBridgeOutAction;
import chs.caf.cafmain.actions.ela.ELADataManagerAction;
import chs.caf.cafmain.actions.ela.ELANodeAction;
import chs.caf.cafmain.actions.link.AddLinkAction;
import chs.caf.cafmain.actions.link.LinkHandler;
import chs.caf.cafmain.actions.partbrowser.PartActionHandlerBase;
import chs.caf.cafmain.actions.qa.AddPropertiesToObjectsAction;
import chs.caf.caplet.IActionable;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IGraphicsFilterChangeListener;
import chs.caf.caplet.IIndicatorRefresherControl;
import chs.caf.caplet.IModelActivationListener;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.IUIResourceProvider;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.action.IActionMgrListener;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.CapletControllerHelper;
import chs.caf.caplet.helpers.CapletUtils;
import chs.caf.caplet.helpers.CommonControllerActions;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.ConvertToRingTerminalAction;
import chs.caf.caplet.helpers.ExcludeFromApplyStyleAction;
import chs.caf.caplet.helpers.FixPositionAction;
import chs.caf.caplet.helpers.FlyoutActionContainer;
import chs.caf.caplet.helpers.ICHSUndoRedoListener;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.ModifyBorderAction;
import chs.caf.caplet.helpers.PropertiesAction;
import chs.caf.caplet.helpers.PurgeFunctionalModuleCodeAction;
import chs.caf.caplet.helpers.SelectAllLockedDecorationAction;
import chs.caf.caplet.helpers.SmartEditPropertiesAction;
import chs.caf.caplet.helpers.SplitTableAction;
import chs.caf.caplet.helpers.SymbolInvokeFromDesignToolsAction;
import chs.caf.caplet.helpers.UndoableContainerHelper;
import chs.caf.caplet.helpers.automation.DumpSelectedObjectDetailsAction;
import chs.caf.caplet.helpers.browser.BrowserTabbedPane;
import chs.caf.caplet.helpers.browser.IBrowserTreeContainer;
import chs.caf.caplet.helpers.browser.LockedLogicObjectNodeDimmer;
import chs.caf.caplet.helpers.browser.LockedTreeNodeDimmer;
import chs.caf.caplet.helpers.browser.LogicBrowserTree;
import chs.caf.caplet.helpers.browser.teamplay.ITeamPlayLinksBrowserController;
import chs.caf.caplet.helpers.browser.teamplay.TeamPlayBaseLinksBrowserController;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolAction;
import chs.caf.caplet.helpers.graphics.CreateArcAction;
import chs.caf.caplet.helpers.graphics.CreateCircleAction;
import chs.caf.caplet.helpers.graphics.CreateCurveAction;
import chs.caf.caplet.helpers.graphics.CreateImageAction;
import chs.caf.caplet.helpers.graphics.CreatePolygonAction;
import chs.caf.caplet.helpers.graphics.CreatePolylineAction;
import chs.caf.caplet.helpers.graphics.CreateRectangleAction;
import chs.caf.caplet.helpers.graphics.CreateTextAction;
import chs.caf.caplet.helpers.graphics.DeleteGfxPointAction;
import chs.caf.caplet.helpers.graphics.FlipAction;
import chs.caf.caplet.helpers.graphics.GroupGfxAction;
import chs.caf.caplet.helpers.graphics.InsertGfxPointAction;
import chs.caf.caplet.helpers.graphics.MeasureDistanceAction;
import chs.caf.caplet.helpers.graphics.PivotTextAction;
import chs.caf.caplet.helpers.graphics.PolylineModifier;
import chs.caf.caplet.helpers.graphics.RotateAction;
import chs.caf.caplet.helpers.graphics.SegmentModifier;
import chs.caf.caplet.helpers.graphics.SetGraphicDimensionAction;
import chs.caf.caplet.helpers.graphics.SymbolPlaceAsGraphicsAction;
import chs.caf.caplet.helpers.graphics.ToggleChamferSegmentModifier;
import chs.caf.caplet.helpers.graphics.UngroupGfxAction;
import chs.caf.caplet.helpers.quickedit.action.LogicQuickEditAction;
import chs.caf.caplet.helpers.segment.ToggleChamferAction;
import chs.caf.caplet.helpers.tabulareditor.IFilterableObjectType;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.plugin.CustomChangeEventMgr;
import chs.caf.plugin.CustomDesignTabPanelMgr;
import chs.caplets.analysis.ui.audit.AuditReportPanel;
import chs.caplets.logic.AutoRouteSettingsAction;
import chs.caplets.logic.BrowserClient;
import chs.caplets.logic.IndicatorRefreshModelChangeListener;
import chs.caplets.logic.InterconnectSourceBrowserClient;
import chs.caplets.logic.InterconnectSourceBrowserTree;
import chs.caplets.logic.LockobjectsDisplay.LockObjectDisplayTask;
import chs.caplets.logic.LogicFilterControl;
import chs.caplets.logic.LogicFilterControlMgr;
import chs.caplets.logic.LogicLinkBrowserClient;
import chs.caplets.logic.Model;
import chs.caplets.logic.SegmentCrossingControlModelChangeListener;
import chs.caplets.logic.actions.AddBackshellAction;
import chs.caplets.logic.actions.AddBackshellTerminationAction;
import chs.caplets.logic.actions.AddChainAction;
import chs.caplets.logic.actions.AddCommentSymbolToMCAction;
import chs.caplets.logic.actions.AddConductorNameAction;
import chs.caplets.logic.actions.AddDeviceFromLibraryPartAction;
import chs.caplets.logic.actions.AddDeviceFromLibraryWithPinsAction;
import chs.caplets.logic.actions.AddDeviceListTableAction;
import chs.caplets.logic.actions.AddDiagramListTableAction;
import chs.caplets.logic.actions.AddFunctionPortAction;
import chs.caplets.logic.actions.AddIndicatorsAction;
import chs.caplets.logic.actions.AddInstanceAction;
import chs.caplets.logic.actions.AddInterconnectOverbraidAction;
import chs.caplets.logic.actions.AddInterconnectShieldAction;
import chs.caplets.logic.actions.AddInterconnectWireAction;
import chs.caplets.logic.actions.AddLibraryInnercoreNetAction;
import chs.caplets.logic.actions.AddLibraryInnercoreShieldAction;
import chs.caplets.logic.actions.AddLibraryInnercoreWireAction;
import chs.caplets.logic.actions.AddLibraryMulticoreAction;
import chs.caplets.logic.actions.AddPinAction;
import chs.caplets.logic.actions.AddPinListAction;
import chs.caplets.logic.actions.AddPinWNAccelAction;
import chs.caplets.logic.actions.AddShieldConductorAction;
import chs.caplets.logic.actions.AddSpliceFromLibraryPartAction;
import chs.caplets.logic.actions.AddToStackPinAction;
import chs.caplets.logic.actions.AddWireListTableAction;
import chs.caplets.logic.actions.AlignAction;
import chs.caplets.logic.actions.ApplyStyleToSegmentsAction;
import chs.caplets.logic.actions.AssociateConnectorAction;
import chs.caplets.logic.actions.AssociateSymbolAction;
import chs.caplets.logic.actions.AutoRouteAction;
import chs.caplets.logic.actions.ConnectActionFlyout;
import chs.caplets.logic.actions.ConnectActionUI;
import chs.caplets.logic.actions.ConnectByInterconnectAction;
import chs.caplets.logic.actions.ConnectByNetAction;
import chs.caplets.logic.actions.ConnectByPinAction;
import chs.caplets.logic.actions.ConnectByWireAction;
import chs.caplets.logic.actions.ConnectIndicatorsAction;
import chs.caplets.logic.actions.ConvertPinTypeAction;
import chs.caplets.logic.actions.ConvertSymbolToParamAction;
import chs.caplets.logic.actions.CreateAssemblyAction;
import chs.caplets.logic.actions.CreateBlockDeviceAction;
import chs.caplets.logic.actions.CreateChamferedNetInstanceAction;
import chs.caplets.logic.actions.CreateChamferedWireInstanceAction;
import chs.caplets.logic.actions.CreateCoaxialSheathMulticoreAction;
import chs.caplets.logic.actions.CreateCoaxialShieldMulticoreAction;
import chs.caplets.logic.actions.CreateConductorAction;
import chs.caplets.logic.actions.CreateDeviceAction;
import chs.caplets.logic.actions.CreateFunctionBlockAction;
import chs.caplets.logic.actions.CreateFunctionConductorAction;
import chs.caplets.logic.actions.CreateFunctionMessageAction;
import chs.caplets.logic.actions.CreateGeneralHighwayAction;
import chs.caplets.logic.actions.CreateICDFromPlacedICDAction;
import chs.caplets.logic.actions.CreateInlineConnectorAction;
import chs.caplets.logic.actions.CreateInlineInterconnectConnectorAction;
import chs.caplets.logic.actions.CreateInterconnectAction;
import chs.caplets.logic.actions.CreateInterconnectConnectorAction;
import chs.caplets.logic.actions.CreateInterconnectDeviceAction;
import chs.caplets.logic.actions.CreateJackConnectorAction;
import chs.caplets.logic.actions.CreateMulticoreAction;
import chs.caplets.logic.actions.CreateMultipleNetsAction;
import chs.caplets.logic.actions.CreateMultipleWiresAction;
import chs.caplets.logic.actions.CreateNoPinDeviceAction;
import chs.caplets.logic.actions.CreateNoPinFunctionAction;
import chs.caplets.logic.actions.CreateNoPinInlineConnectorAction;
import chs.caplets.logic.actions.CreateNoPinJackConnectorAction;
import chs.caplets.logic.actions.CreateNoPinPlugConnectorAction;
import chs.caplets.logic.actions.CreateOtherComponentOnlyWithSymbolAction;
import chs.caplets.logic.actions.CreateOverbraidAction;
import chs.caplets.logic.actions.CreateOverbraidwithAccelAction;
import chs.caplets.logic.actions.CreatePlugConnectorAction;
import chs.caplets.logic.actions.CreateRingTerminalAction;
import chs.caplets.logic.actions.CreateSectorAction;
import chs.caplets.logic.actions.CreateShieldConductorAction;
import chs.caplets.logic.actions.CreateSingleLineAction;
import chs.caplets.logic.actions.CreateSpliceAction;
import chs.caplets.logic.actions.CreateStackPinAction;
import chs.caplets.logic.actions.CreateTwistedSheathMulticoreAction;
import chs.caplets.logic.actions.CreateWireAction;
import chs.caplets.logic.actions.DeleteAction;
import chs.caplets.logic.actions.DeleteConductorSegmentModifier;
import chs.caplets.logic.actions.DisassociateConnectorAction;
import chs.caplets.logic.actions.DistributeAction;
import chs.caplets.logic.actions.EditFootprintAction;
import chs.caplets.logic.actions.EditHarnessAction;
import chs.caplets.logic.actions.EditSharedOverbraidAction;
import chs.caplets.logic.actions.EditStackPinAction;
import chs.caplets.logic.actions.GenerateHarnessConnAction;
import chs.caplets.logic.actions.InsertConductorSegmentModifier;
import chs.caplets.logic.actions.ManageSignalsAction;
import chs.caplets.logic.actions.MergeIntoAction;
import chs.caplets.logic.actions.MoveFunctionPortAction;
import chs.caplets.logic.actions.MovePinAction;
import chs.caplets.logic.actions.MoveWireEndAction;
import chs.caplets.logic.actions.MultiPartConnectorPartChooserAction;
import chs.caplets.logic.actions.PlaceAssemblyTreeAction;
import chs.caplets.logic.actions.PropagateAllHarnessAction;
import chs.caplets.logic.actions.PropagateSelectedHarnessAction;
import chs.caplets.logic.actions.RemoveDeviceConnectorsAction;
import chs.caplets.logic.actions.RemoveToDoItemAction;
import chs.caplets.logic.actions.RerouteSegmentAction;
import chs.caplets.logic.actions.ResetAssemblyAction;
import chs.caplets.logic.actions.ResizeAction;
import chs.caplets.logic.actions.RouteIntoHighwayAction;
import chs.caplets.logic.actions.SaveAssemblyConnectivityToLibraryAction;
import chs.caplets.logic.actions.SelectFootprintAction;
import chs.caplets.logic.actions.SetPinNonReferenceAction;
import chs.caplets.logic.actions.SetPinReferenceAction;
import chs.caplets.logic.actions.SliceAction;
import chs.caplets.logic.actions.SmartEditAction;
import chs.caplets.logic.actions.StripAtSpliceAction;
import chs.caplets.logic.actions.SymbolCreateSharedAction;
import chs.caplets.logic.actions.SymbolCreateSharedSpliceAction;
import chs.caplets.logic.actions.TabularEditAction;
import chs.caplets.logic.actions.TerminateAtSpliceAction;
import chs.caplets.logic.actions.ToggleHomeAction;
import chs.caplets.logic.actions.ToggleHomeActionUI;
import chs.caplets.logic.actions.ToggleIndicatorConstraintsAction;
import chs.caplets.logic.actions.ToggleShowXRefAction;
import chs.caplets.logic.actions.ToggleShowXRefActionUI;
import chs.caplets.logic.actions.UnRouteHighwayAction;
import chs.caplets.logic.actions.UnplaceAction;
import chs.caplets.logic.actions.UpdateICDAction;
import chs.caplets.logic.actions.UpdatePartAction;
import chs.caplets.logic.actions.analysis.AnalysisInterfaceToggleAction;
import chs.caplets.logic.actions.analysis.AnalysisPopupMenuBuilderAction;
import chs.caplets.logic.actions.analysis.AnalysisPopupMenuBuilderActionUI;
import chs.caplets.logic.actions.analysis.DynSimBackgroundAction;
import chs.caplets.logic.actions.analysis.DynSimOffAction;
import chs.caplets.logic.actions.analysis.DynSimOnDemandAction;
import chs.caplets.logic.actions.analysis.LogicAttachModelAction;
import chs.caplets.logic.actions.analysis.LogicBuildModelAction;
import chs.caplets.logic.actions.analysis.LogicStressAction;
import chs.caplets.logic.actions.analysis.MacroRecordingAction;
import chs.caplets.logic.actions.analysis.QualitativeSimulationModeAction;
import chs.caplets.logic.actions.analysis.ResetAction;
import chs.caplets.logic.actions.analysis.SetAnalysisNetlistScopeAction;
import chs.caplets.logic.actions.analysis.SimulateAction;
import chs.caplets.logic.actions.analysis.SpiceSimulationModeAction;
import chs.caplets.logic.actions.analysis.ViewFailedComponentsAction;
import chs.caplets.logic.actions.bridges.BridgeOutFilterAction;
import chs.caplets.logic.actions.debug.BuildICDFromJsonAction;
import chs.caplets.logic.actions.debug.DumpICDDeviceAction;
import chs.caplets.logic.actions.debug.DumpICDSignalsAction;
import chs.caplets.logic.actions.debug.DumpSelectedAction;
import chs.caplets.logic.actions.debug.DumpSelectedSharedPinMatingAction;
import chs.caplets.logic.actions.debug.VariantICDTogglePinConstraintAction;
import chs.caplets.logic.actions.icdbrowser.AddDeviceFromICDAction;
import chs.caplets.logic.actions.icdbrowser.AddParametrizedDeviceFromICDAction;
import chs.caplets.logic.actions.icdbrowser.ICDObjectActionHandler;
import chs.caplets.logic.actions.inlineassist.InsertInlineConnectorAction;
import chs.caplets.logic.actions.inlineassist.InsertSharedInlineConnectorAction;
import chs.caplets.logic.actions.partbrowser.PartActionHandler;
import chs.caplets.logic.actions.prototype.LogicPrototypeWireExpressionEditAction;
import chs.caplets.logic.actions.rules.LogicSetAttributesAndPropertiesByRuleAction;
import chs.caplets.logic.actions.serviceDocumentation.PublisherReplaceInstanceSymbolAction;
import chs.caplets.logic.actions.serviceDocumentation.smartflows.ChangeFlowDirectionAction;
import chs.caplets.logic.actions.shared.AddConductorAction;
import chs.caplets.logic.actions.shared.AddGeneralHighwayAction;
import chs.caplets.logic.actions.shared.AddSharedDeviceAction;
import chs.caplets.logic.actions.shared.AddSharedFunctionAction;
import chs.caplets.logic.actions.shared.AddSharedGeneralHighwayAction;
import chs.caplets.logic.actions.shared.AddSharedICDAction;
import chs.caplets.logic.actions.shared.AddSharedInlineConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedInterconnectConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedInterconnectDeviceAction;
import chs.caplets.logic.actions.shared.AddSharedJackConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedMessageAction;
import chs.caplets.logic.actions.shared.AddSharedNetAction;
import chs.caplets.logic.actions.shared.AddSharedPlugConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedRingTerminalAction;
import chs.caplets.logic.actions.shared.AddSharedShieldAction;
import chs.caplets.logic.actions.shared.AddSharedSignalAction;
import chs.caplets.logic.actions.shared.AddSharedSingleLineAction;
import chs.caplets.logic.actions.shared.AddSharedSpliceAction;
import chs.caplets.logic.actions.shared.AddSharedWireAction;
import chs.caplets.logic.actions.shared.AddSingleLineAction;
import chs.caplets.logic.actions.shared.CreateSharedConductorGroupAction;
import chs.caplets.logic.actions.shared.CreateSharedObjectRevisionAction;
import chs.caplets.logic.actions.shared.EditSharedPinListAction;
import chs.caplets.logic.actions.shared.EnhancedSwapOutSharedObjectRevisionAction;
import chs.caplets.logic.actions.shared.FreezeSharedObjectsAction;
import chs.caplets.logic.actions.shared.FreezeUnfreezeSharedObjectAction;
import chs.caplets.logic.actions.shared.LogicSharedObjectDeleteAction;
import chs.caplets.logic.actions.shared.LogicSharedObjectDeleteUnusedAction;
import chs.caplets.logic.actions.shared.ReplaceSharedCompositeSymbolAction;
import chs.caplets.logic.actions.shared.ReplaceSharedCompositeSymbolFunctionAction;
import chs.caplets.logic.actions.shared.ShareAction;
import chs.caplets.logic.actions.shared.SharePortAction;
import chs.caplets.logic.actions.shared.SharedObjectRevisionUsagesAction;
import chs.caplets.logic.actions.shared.SwapOutSharedObjectRevisionAction;
import chs.caplets.logic.actions.shared.UnassignPortAction;
import chs.caplets.logic.actions.shared.UnshareAction;
import chs.caplets.logic.actions.tests.HackVisibilityAction;
import chs.caplets.logic.analysis.LogicAnalysisAttachmentTargetProvider;
import chs.caplets.logic.analysis.LogicAnalysisBackAnnoProcessor;
import chs.caplets.logic.analysis.LogicAnalysisColoringProcessor;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.caplets.logic.analysis.ui.AnalysisBrowserPanel;
import chs.caplets.logic.connectivity.LogicPointConnector;
import chs.caplets.logic.harness.propagate.AutoPropagateHarnessController;
import chs.caplets.logic.icd.ICDBrowserPanel;
import chs.caplets.logic.shared.SharedObjectBrowserPanel;
import chs.caplets.logic.shared.SharedObjectBrowserTree;
import chs.caplets.shared.actions.MoveConnectorAction;
import chs.caplets.shared.actions.SelectAction;
import chs.caplets.shared.properties.PropertiesClient;
import chs.caplets.shared.properties.QAPLogicPropertiesClient;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IGfxView;
import chs.cof.library.IICDComponentSearchController;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.IWriteableDSUM;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.cof.project.buildlist.ILogicAnalysisBuildList;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cofUtils.logical.concurrency.ILogicConcurrencyEventListener;
import chs.cofUtils.logical.concurrency.LogicConcurrencyController;
import chs.cofUtils.logical.concurrency.SharedObjectChangeNotification;
import chs.cog.ICOGManaged;
import chs.cog.IPersistenceSession;
import chs.common.IDesignContainer;
import chs.common.IPrivilegedDesignMgr;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.caf.ui.TextAttributesEditor;
import chs.ctf.ui.form.SymbolSelectionEventListener;
import chs.services.dynamicgfx.connectivity.IPointConnector;
import chs.services.gfx.GfxView;
import chs.subsystem.immersed.ImmersedModeServices;
import chs.subsystem.immersed.service.DesignOpenedEvent;
import chs.subsystem.immersed.service.ICapitalEventClient;
import chs.subsystem.immersedapp.IControllerSelectionSyncService;
import chs.subsystem.immersedapp.ImmersedAppServices;
import chs.system.FactoryMgr;
import chs.utilities.AppInfo;
import chs.utilities.BuildInfo;
import chs.utilities.CapabilityHelper;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utilities.SupportedFeatureInfo;
import chs.utility.AnalysisHelper;
import chs.utility.ICDUtils;
import chs.utility.SymbolUtils;
import chs.utility.helpers.ConductorHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.MouseEventHelper;
import chs.utility.helpers.MultiPartConnectorUtils;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.DefaultSharedObjectPersistenceListner;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

public abstract class BaseController extends CommonControllerActions
		implements ISelectListener, IModelActivationListener, IIndicatorRefresherControl
{

	protected SymbolLibraryBrowser m_libBrowser = null;
	private Model m_model = null;
	private BrowserContainer m_browserContainer = null;
	private boolean m_bSelectingAssoc = false;

	private LogicBrowserTree m_treeView = null;
	private SharedObjectBrowserPanel m_sobPanel = null;
	protected InterconnectSourceBrowserTree m_toDoView = null;

	private PartActionHandler m_partActionHandler = null;
	private ICDObjectActionHandler m_icdObjectActionHandler = null;
	private LogicPointConnector m_pointConnector;
	private LogicFilterControl filterControl = null;

	private AnalysisBrowserPanel analysisBrowserPanel;
	private BrowserClient designTreeClient;

	@Nullable private CAFDesignSharedObjectCleanupAdaptor sharedObjectCleanupAdaptor;
	private ICDBrowserPanel m_icdPanel = null;

	private LogicDesignConcurrentUpdateHandler withinActionDesRefreshHandler;
	private List<IModelChangeListener> m_suspendedIndicatorRefreshers;
	@Nullable private IModelChangeListener m_indicatorRefreshControl;
	@Nullable private IModelChangeListener m_segmentCrossingControl;
	private IUIResourceProvider uiResourceProvider = null;

	@NotNull private DefaultSharedObjectPersistenceListner sharedObjPersistenceListner;
	@NotNull private Stack<DefaultSharedObjectPersistenceListner> persistenceListnerStack;

	protected BaseController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		super(caplet);

		// We'd like to override as necessary in derived classes, e.g LogicController but the order
		// that Logic only work is done in this class is important which makes overriding awkward
		// m_isLogic = isLogic;

		// Create our model
		m_model = createModel(design, diagram);

		// Create the browser for this controller
		m_browserContainer = new BrowserContainer(CAFUtils.getInstance().getDialogFrame(), m_model);
		m_browserContainer.setName("LogicBrowserTabbedPane");

		// Add the design browser tab
		addDesignTab();

		// Add the SymbolLibraryBrowser as a symbol change listener of the SymbolLibraryMgr
		// This is necessary so the SymbolLibraryMgr can let the SymbolLibraryBrowser know if it has been refreshed
		addSymbolTab();

		// Add the Parts Browser a
		addPartsTab(design);

		// Add the shared object tab (after the symbol tab) iff we're in logic (capture doesn't have shared objs)
		addSharedTab(design);

		// if the analysis extensions are installed add the analysis tab to the browser
		addAnalysisTab(design);

		addCommonApplicableActions(new PropertiesClient(getCapletModel(), false));

		addICDTab(design);

		addLinksTabs(createlogicLinkBrowserClient(), getLinkHandler(), m_model.getDesign());

		addTeamPlayLinksTab();

		addConfigurableAttributeTab(design);

		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			addAuditTab();
		}

		// Add ourselves as a select listener on our SelectMgr
		// so we can select associated objects.
		getSelectMgr().addSelectListener(this);

		sharedObjectCleanupAdaptor = createDesignSharedObjectCleanupAdaptor(design);

		if (getGraphicsFilterControl() != null) {
			// When an object becomes invisible, the SelectMgr deselects it.
			filterControl.addFilterChangeListener(getSelectMgr());

			// When an object becomes visible or invisible in the diagram, it must also be made
			// visible or invisible in the diagram browser.
			filterControl.addFilterChangeListener(m_treeView);

			// When an object becomes visible or invisible in the diagram and we are doing simulation,
			// it must also be made visible or invisible in the simulation view.
			AnalysisServices logicServices = LogicAnalysisServices.getAnalysisServices();
			if (logicServices != null) {
				filterControl.addFilterChangeListener((IGraphicsFilterChangeListener) logicServices);
			}

			// When a data model change occurs, filterControl re-evaluates the option expressions
			// of all objects in the diagram and changes their visibility when necessary.
			m_model.addModelChangeListener(filterControl);
		}
		// else CCapture has no filterControl.

		m_pointConnector = new LogicPointConnector(() -> m_model.getDiagram());
		m_model.getDynamicGfxService().setCurrentPointConnector(m_pointConnector);
		getDeferredActionProcessor().addDeferredAction(m_pointConnector);

		m_browserContainer.postInit();

		if (filterControl != null) {
			// Apply the current filter so the diagram initially appears with objects hidden or shown as appropriate.
			filterControl.filterDiagram(diagram);
		}
		// else CCapture has no filterControl.
		CustomDesignTabPanelMgr.getInstance().registerModel(m_model);
		CustomChangeEventMgr.getInstance().registerModel(m_model);

		//Currently we have one display task per logic controller. If this proves in-efficient due then
		//we need to make a single handler using getInstance on LockObjectDisplayTask and handle multiple designs.
		ILogicDesign logicDesign = CommonUtils.cast(design, ILogicDesign.class);
		if (logicDesign != null) {
			if (logicDesign.isUnderConcurrentEdit()) {
				LockObjectDisplayTask.getInstance().addDesign(getTreeNodeDimmer(), design);
			}
			IConnectivity connectivity = logicDesign.getConnectivity();
			if (connectivity != null) {
				withinActionDesRefreshHandler = new LogicDesignConcurrentUpdateHandler(logicDesign, this);
			}
		}
		CommonUtils.castOptional(AutoPropagateHarnessController.getInstance(), IActionMgrListener.class)
				.ifPresent(getActionMgr()::addActionMgrListener);

		SharedObjectBrowserTree sharedBrowserTree = m_sobPanel != null ? m_sobPanel.getSharedView() : null;
		if (sharedBrowserTree != null) {
			getActionMgr().addActionMgrListener(sharedBrowserTree);
		}
		m_browserContainer.setSelectedIndex(0);
		m_indicatorRefreshControl = new IndicatorRefreshModelChangeListener(design.getUID());
		m_model.addModelChangeListener(m_indicatorRefreshControl);
		SegmentCrossingControlModelChangeListener segmentCrossingControl =
				new SegmentCrossingControlModelChangeListener(design.getUID());
		m_segmentCrossingControl = segmentCrossingControl;
		m_model.addModelChangeListener(segmentCrossingControl);
		UndoableContainerHelper.addCHSUndoRedoListener(segmentCrossingControl);
		sharedObjPersistenceListner = createSharedObjectPersistenceListener(design);
		persistenceListnerStack = new Stack<>();
		getActionMgr().addActionMgrListener(new SharedObjectPreModifyListenerControl());
	}

	@NotNull protected DefaultSharedObjectPersistenceListner createSharedObjectPersistenceListener(ILogicDesign design)
	{
		if ((isSharedObjectCleanupSupported()) && design != null) {
			return new SharedObjectPersistenceListner();
		}
		else {
			return new DefaultSharedObjectPersistenceListner();
		}
	}

	@Override public void onControllerActionActivate(ActionEvent e)
	{
		super.onControllerActionActivate(e);
		IPersistenceSession persistenceSession = FactoryMgr.getCHSSystem().getPersistenceSession();
		if (persistenceSession != null) {
			persistenceSession.addGlobalListener(sharedObjPersistenceListner);
			persistenceListnerStack.push(sharedObjPersistenceListner); //Is count better than stack?
		}
		ISharedConductorMgr sharedConductorMgr = getSharedConductorMgr();
		if (sharedConductorMgr != null) {
			sharedConductorMgr.addSaveListener(sharedObjPersistenceListner);
		}
	}

	@Override public void onControllerActionTerminate(boolean success)
	{
		super.onControllerActionTerminate(success);
		IPersistenceSession persistenceSession = FactoryMgr.getCHSSystem().getPersistenceSession();
		if (persistenceSession != null) {
			assert !persistenceListnerStack.isEmpty();
			persistenceListnerStack.pop();
			if (persistenceListnerStack.isEmpty()) {
				persistenceSession.removeGlobalListener(sharedObjPersistenceListner);
			}
		}
		ISharedConductorMgr sharedConductorMgr = getSharedConductorMgr();
		if (sharedConductorMgr != null) {
			sharedConductorMgr.removeSaveListener(sharedObjPersistenceListner);
		}
		sharedObjPersistenceListner.clear(success);
	}

	@Nullable private ISharedConductorMgr getSharedConductorMgr()
	{
		ISharedConductorMgr sharedConductorMgr = null;
		IProject project = getProject();
		if (project != null) {
			sharedConductorMgr = project.getSharedConductorMgr();
		}
		return sharedConductorMgr;
	}

	@Nullable protected CAFDesignSharedObjectCleanupAdaptor createDesignSharedObjectCleanupAdaptor(ILogicDesign design)
	{
		if ((isSharedObjectCleanupSupported()) && design != null) {
			return new CAFDesignSharedObjectCleanupAdaptor(m_model,
					new CAFSharedObjectPropTextCleaner(m_model),
					new CAFSharedDeviceConnectorCleaner(m_model),
					new CAFSharedModularConnectCleaner(m_model),
					new CAFSharedMulticoreIndicatorUpdater(m_model),
					new CAFSharedConductorCrossingsUpdater(m_model),
					new SharedFunctionMessageCleaner(design));
		}
		return null;
	}

	@Override protected void addQuickEditAction()
	{
		addAction(new LogicQuickEditAction(this));
	}

	protected LogicLinkBrowserClient createlogicLinkBrowserClient()
	{
		return new LogicLinkBrowserClient(this, getLinkHandler());
	}

	private void addConfigurableAttributeTab(IDesign design)
	{

		GroupingByAttributesPanel groupingByAttributesPanel = new GroupingByAttributesPanel(design, this);

		JPanel treePanel = groupingByAttributesPanel.addToPanel(null);
		m_browserContainer
				.addTab(ResourceMgr.getString(getResourceClass(), "Controller.Browser.AttributeGroupingTab.Title.text"),
						null,
						treePanel,
						ResourceMgr
								.getString(getResourceClass(), "Controller.Browser.AttributeGroupingTab.ToolTip.text"));
	}

	protected LogicBrowserTree constructDesignBrowserTree(IBrowserClient client, String name)
	{
		return new LogicBrowserTree(client, name);
	}

	private void addDesignTab()
	{

		designTreeClient = getDesignConnectivityBrowserClient();
		m_treeView = constructDesignBrowserTree(designTreeClient, "LogicBrowser");
		JPanel treePanel = buildDesignPanel();
		Class<? extends BaseController> resourceClass = getResourceClass();
		String title = ResourceMgr.getString(resourceClass, "Controller.Browser.TreeTab.Title.text");
		String tooltip = ResourceMgr.getString(resourceClass, "Controller.Browser.TreeTab.ToolTip.text");

		m_browserContainer.addTab(title, null, treePanel, tooltip);
		m_browserContainer.setHomeTab(treePanel);
	}

	@NotNull protected JPanel buildDesignPanel()
	{
		return m_treeView.buildContentPanel(null);
	}

	protected BrowserClient getDesignConnectivityBrowserClient()
	{
		BrowserClient browserClient = new BrowserClient(this);
		browserClient.setTreeNodeDimmer(getTreeNodeDimmer());
		return browserClient;
	}

	protected LockedTreeNodeDimmer getTreeNodeDimmer()
	{
		return LockedLogicObjectNodeDimmer.getInstance();
	}

	@Nullable public AnalysisBrowserPanel getAnalysisBrowserPanel()
	{
		return analysisBrowserPanel;
	}

//	public void setAnalysisBrowserPanel(AnalysisBrowserPanel analysisBrowserPanel)
//	{
//		this.analysisBrowserPanel = analysisBrowserPanel;
//	}

	public void addAnalysisTab(IDesign design)
	{
		if (!Environment.isUnitTest() && CapitalAnalysisFactory.getAnalysisInterface() != null) {
			initializeAnalysisBrowserPanel(this);

			m_browserContainer
					.addTab(ResourceMgr.getString(getResourceClass(), "Controller.Browser.AnalysisTab.Title.text"),
							null, analysisBrowserPanel,
							ResourceMgr.getString(getResourceClass(), "Controller.Browser.AnalysisTab.ToolTip.text"));
		}
	}

	private AuditReportPanel auditReportPanel = null;

	@Override public void addAuditTab()
	{
		if (!Environment.isUnitTest() && CapitalAnalysisFactory.getAnalysisInterface() != null &&
				auditReportPanel == null) {
			auditReportPanel = new AuditReportPanel();
			int indexOfAnalysisTab = m_browserContainer
					.indexOfTab(ResourceMgr.getString(getResourceClass(), "Controller.Browser.AnalysisTab.Title.text"));
			m_browserContainer.insertTab(
					ResourceMgr.getString(getResourceClass(), "Controller.Browser.AuditReportTab.Title.text"), null,
					auditReportPanel,
					ResourceMgr.getString(getResourceClass(), "Controller.Browser.AuditReportTab.ToolTip.text"),
					indexOfAnalysisTab + 1);
			m_browserContainer.postInit();
		}
	}

	@Override @Nullable public AuditReportPanel getAuditReportPanel()
	{
		return auditReportPanel;
	}

	@Nullable public IAction getPostSelectControlAction(MouseEvent e)
	{
		ICapletView activeCapletView = CAFUtils.getInstance().getActiveCapletView();
		boolean bMode = e.getSource() == activeCapletView
				&& activeCapletView.isPartResolutionHighlightEnabled()
				&& getCapletModel().isEditable()
				&& MultiPartConnectorUtils.specialConnectorInSelection(getSelectMgr().getCurrentSelections()) != null;
		if (bMode) {
			return new MultiPartConnectorPartChooserAction(this, e);
		}
		return null;
	}

	private void initializeAnalysisBrowserPanel(ICapletController capletController)
	{
		analysisBrowserPanel = new AnalysisBrowserPanel(capletController);
	}

	public void refreshAnalysisScopeAction()
	{
		// The scope action is a container for the individual scope actions. This forms the drawer from
		// which possible scopes may be selected.
		ActionContainer scopeAction = ((BaseLogicResource) getCaplet().getResource()).getScopeAction();
		scopeAction.clear();

		// Add the design level scope actions that are available...

		// Handle the build list scopes that are available...
		//dts0100690429 : Null check placed to handle the scenario when we are opening a project after closing all the projects
		//					with opened diagrams in the same session
		IProject currentPrj = CAFUtils.getInstance().getCurrentProject();
		if (currentPrj != null) {

			//this has been done to avoid a situation when we have deleted and set the active build list to null
			//this will help in determining, if at all we don't have any scoped object we shouldn't have the scope set
			//while refreshing when build list is deleted and we have removed scope object but scope remains
			IAnalysisNetlistScope currentAnalysisNetlistScope = AnalysisServices.getCurrentAnalysisNetlistScope();
			if (currentAnalysisNetlistScope != null) {
				if (currentAnalysisNetlistScope.getScopedObject() == null) {
					AnalysisServices.unsetScopeAndScopeRegistry(currentPrj);
				}
			}

			Set<IBuildList> logicAnalysisBuildLists = AnalysisServices.getAnalyzableBuildLists(currentPrj, true);

			// add the active build list to the list of possible scopes...
			IBuildList activeBuildList =
					currentPrj.getBuildListMgr().getActiveBuildList();

			ActionEntry myAction;
			//here we need to verify that the active build list hasn't been deleted
			if (activeBuildList != null && activeBuildList instanceof ILogicAnalysisBuildList &&
					logicAnalysisBuildLists.contains(activeBuildList)) {
				AbstractAction action = getScopeAction(activeBuildList);
				myAction = new ActionEntry(action);
				scopeAction.add(myAction);
			}

			// add any remaining buildlists...
			for (IBuildList buildList : logicAnalysisBuildLists) {
				if (buildList != null) {
					if (buildList != activeBuildList) {
						AbstractAction action = getScopeAction(buildList);
						myAction = new ActionEntry(action);
						scopeAction.add(myAction);
					}
				}
			}

			boolean buildListInserted = false;

			//if till now scopeAction contains any member - then build list has been inserted
			if (!scopeAction.getMembersAsList().isEmpty()) {
				buildListInserted = true;
			}

			IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();
			if (activeDiagram != null) {

				Set<ILogicDesign> openDesigns = CAFUtils.getInstance().getOpenedDesigns(ILogicDesign.class);

				if (!openDesigns.isEmpty()) {
					// if we can add designs add a separator to the list...
					if (buildListInserted) {
						scopeAction.add(new ActionSeparator());
					}

					IDesignContainer activeDesignContainer = activeDiagram.getDesignContainer();
					AbstractAction action = getScopeAction(activeDesignContainer);
					myAction = new ActionEntry(action);
					scopeAction.add(myAction);
				}
			}
		}

		// we need to let the action know which is the first selected entry in the list...
		if (scopeAction.shouldDisplay()) {
			IActionNode node = scopeAction.getMembers().getNext();
			if (node instanceof ActionEntry &&
					((ActionEntry) node).getAction() instanceof SetAnalysisNetlistScopeAction) {
				SetAnalysisNetlistScopeAction
						.setSelectedComponent((SetAnalysisNetlistScopeAction) ((ActionEntry) node).getAction());
			}
		}

		// now add actions for the qualitatve, spice and macro recording actions.
	}

	private AbstractAction getScopeAction(IUIDObject activeBuildList)
	{
		AbstractAction action = AnalysisServices.getScopeAction(activeBuildList.getUID());
		if (action == null) {
			action = new SetAnalysisNetlistScopeAction(activeBuildList);
			AnalysisServices.addScopeAction(activeBuildList.getUID(), action);
		}
		return action;
	}

	public void createAnalysisToolbarActions()
	{
		refreshAnalysisScopeAction();
		if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
			if (getAction(QualitativeSimulationModeAction.class) == null) {
				addAction(new QualitativeSimulationModeAction(this));
			}
			if (getAction(SpiceSimulationModeAction.class) == null) {
				addAction(new SpiceSimulationModeAction(this));
			}
		}
		if (getAction(MacroRecordingAction.class) == null) {
			addAction(new MacroRecordingAction(this));
		}
	}

	@Nullable protected IAction determineActiveSymbolAction()
	{
		IAction action = getAction(AddInstanceAction.class);
		if (action != null && action.isEnabled()) {
			return action;
		}
		IStamp sub = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getActiveSymbol();
		if (sub instanceof ISymbolDef) {
			ISymbolDef subsd = (ISymbolDef) sub;
			if (SymbolUtils.isCommentSymbol(subsd)) {
				return getAction(AddCommentSymbolAction.class);
			}
		}
		return null;
	}

	private void addPartsTab(IDesign design)
	{
		if (isPartsBrowserSupported()) {
			m_partActionHandler = new PartActionHandler(m_model);
			// Create Part Selector Browser
			m_partsView = CAFUtils.getInstance().getCHSSystem().getPartsLibrary()
					.createLibraryBrowser(CAFUtils.getInstance().getDialogFrame(),
							new PartActionHandlerBase.IgnoreMenuActionFocusListener(), design,
							m_partActionHandler, new PartSelectionContext());
			((JComponent) m_partsView)
					.setName(ResourceMgr.getString(getResourceClass(), "Controller.Browser.PartsTab.Title.text"));

			m_browserContainer
					.addTab(ResourceMgr.getString(getResourceClass(), "Controller.Browser.PartsTab.Title.text"), null,
							((JComponent) m_partsView),
							ResourceMgr.getString(getResourceClass(), "Controller.Browser.PartsTab.ToolTip.text"));
		}
	}

	protected final void invokeRelevantAction(int mouseModifiers, String command)
	{
		// No reason it should be here, but the nullable says we must protect.
		final IActionMgr actionMgr = getActionMgr();
		IAction action = determineActiveSymbolAction();
		final IAction activeAction = actionMgr.getActiveAction();
		if (action != null && activeAction != action && action.isEnabled()) {
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command, mouseModifiers);
			actionMgr.actionPerformed(action, ae);
		}
	}

	protected void initLibraryBrowser()
	{
		m_libBrowser = new SymbolLibraryBrowser(true)
		{
			protected void restart(IAction previousAction)
			{
				Class<?> actionClass = previousAction.getClass();
				if (actionClass == AddCommentSymbolAction.class) {
					int modifiers = 0;
					doubleClickFired(modifiers);
				}
			}

			@Override protected void handleActionInvocationViaDragDropOnDiagram(@NotNull Point diagramDropPoint)
			{
				if (getCapletModel().isEditable()) {
					invokeRelevantAction(0, "mousedrag");
					ICapletView view = CAFUtils.getInstance().getActiveCapletView();
					if (view instanceof GfxView) {
						final GfxView gfxView = (GfxView) view;
						final Point currMouseLoc = gfxView.convertWorldPointToViewComponentPoint(diagramDropPoint);
						final IActionMgr actionMgr = getActionMgr();
						if (actionMgr != null) {
							final MouseEvent mouseEvent = MouseEventHelper
									.createMouseEvent(gfxView, 0, currMouseLoc.x, currMouseLoc.y, 0, false, 0);
							actionMgr.mouseMoved(mouseEvent);
						}
					}
				}
			}

			protected void doubleClickFired(int mouseModifiers)
			{
				if (getCapletModel().isEditable()) {
					invokeRelevantAction(mouseModifiers, "addinstance");
				}
			}
		};
	}

	private void addSymbolTab()
	{

		initLibraryBrowser();
		CAFUtils.getInstance().getSymbolLibraryMgr().addSymbolChangeListener(m_libBrowser);
		final IProject project = getProject();
		IProjectPreferenceMgr preferenceMgr = project != null ? project.getPreferences() : null;
		if (preferenceMgr != null) {
			preferenceMgr.addPreferenceChangeListener(m_libBrowser);
		}
		m_browserContainer
				.addTab(ResourceMgr.getString(getResourceClass(), "Controller.Browser.SymbolTab.Title.text"), null,
						m_libBrowser,
						ResourceMgr.getString(getResourceClass(), "Controller.Browser.SymbolTab.ToolTip.text"));
	}

	protected Class<? extends BaseController> getResourceClass()
	{
		return getClass();
	}

	private void addSharedTab(IDesign design)
	{
		// Not logic?  Don't do the shared tab
		if (areSharedObjectsSupported()) {
			m_sobPanel = new SharedObjectBrowserPanel(design, this);
			// We know w're logic at this point
			createLogicSharedObjectControllerActions();
		}

		// Create the actions AFTER the sharedView is constructed
		createSharedControllerActions();

		if (areSharedObjectsSupported()) {
			//Below line commented to fix dts0100733491 - Shared Tab Icons in the browser tree are not always displayed
			//In case an integrator plane is opened and then a logic diagram is opened, the logic caplet window is not
			//created at this moment, but SharedObjectBrowserPanel relies on active caplet to determine if the shared
			//tab icons should be displayed or not (In this case it the integrator's caplet that is active)
			//Hence, do this initialization after creating the logic caplet window
			//m_sobPanel.initialize();

			// Finally add the whole tab to the browser container with the other tabs
			m_browserContainer
					.addTab(ResourceMgr.getString(getResourceClass(), "Controller.Browser.SharedTab.Title.text"), null,
							m_sobPanel,
							ResourceMgr.getString(getResourceClass(), "Controller.Browser.SharedTab.ToolTip.text"));

			final InterconnectSourceBrowserClient icxClient = new InterconnectSourceBrowserClient(this);
			m_toDoView = new InterconnectSourceBrowserTree(icxClient, "InterconnectSourceBrowser");
		}
	}

	protected boolean areSharedObjectsSupported()
	{
		return CapabilityHelper.supports(SupportedFeatureInfo.Feature.LOGIC_SHARED_OBJECTS);
	}

	protected boolean isSharedObjectCleanupSupported()
	{
		return areSharedObjectsSupported();
	}

	private void addICDTab(IDesign design)
	{
		if (isICDBrowserSupported()) {
			ILogicDesign logicDesign = CommonUtils.cast(design, ILogicDesign.class);
			if (logicDesign != null && ICDUtils.areICDsSupported()) {
				ConfigurationTypeEnum configurationTypeEnum =
						ConfigurationTypeEnum.fromDesignType(logicDesign.getDesignType());
				if (configurationTypeEnum != null) {
					IICDComponentSearchController searchController =
							CAFUtils.getInstance().getCHSSystem().getPartsLibrary()
									.createLibraryComponentSearchController(configurationTypeEnum);
					m_icdPanel = new ICDBrowserPanel(logicDesign, this, m_model, searchController);
					m_icdObjectActionHandler = m_icdPanel.getActionHandler();
					m_browserContainer
							.addTab(ResourceMgr.getString(getResourceClass(), "Controller.Browser.ICDTab.Title.text"),
									null, m_icdPanel, ResourceMgr
											.getString(getResourceClass(), "Controller.Browser.ICDTab.ToolTip.text"));
				}
			}
		}
	}

	public void createSharedTabToolbar()
	{
		if (areSharedObjectsSupported()) {
			m_sobPanel.initialize();
		}
	}

	public void createDesignBrowserToolbar()
	{

	}

	public IPointConnector getPointConnector()
	{
		return m_pointConnector;
	}

	/**
	 * @see CapletControllerHelper # cleanUp()
	 */
	public void destroy()
	{
		if (withinActionDesRefreshHandler != null) {
			withinActionDesRefreshHandler.destroy();
			withinActionDesRefreshHandler = null;
		}
		LockObjectDisplayTask.getInstance().removeDesign(getCapletModel().getDesign());
		//clear paste buffer when the model is unloaded. Paste buffer will have reference to designs that are unloaded.
		getDataTransfer().clearPasteBuffer();

		ImmersedAppServices.getService(IControllerSelectionSyncService.class).unregister(this);

		CustomDesignTabPanelMgr.getInstance().deRegisterModel(m_model);
		CustomChangeEventMgr.getInstance().deRegisterModel(m_model);
		// Remove the indicator refresher listeners for each diagram
		// This will remove the indictor refresher from the "singleton" list and return it so we can remove it from
		// the listeners - if it is null, which it really shouldn't be, it wont be constructed.
		// If this is null don't bother removing it as a listener (even though it wouldn't do any harm trying ;)
		if (m_indicatorRefreshControl != null) {
			m_model.removeModelChangeListener(m_indicatorRefreshControl);
			m_indicatorRefreshControl = null;
		}

		if (m_segmentCrossingControl != null) {
			m_model.removeModelChangeListener(m_segmentCrossingControl);
			UndoableContainerHelper.removeCHSUndoRedoListener((ICHSUndoRedoListener) m_segmentCrossingControl);
			m_segmentCrossingControl = null;
		}

		if (filterControl != null) {
			// filterControl is shared by all diagrams in a project. Must update it to prevent
			// future notifications to our diagram.
			filterControl.removeFilterChangeListener(getSelectMgr());
			filterControl.removeFilterChangeListener(m_treeView);

			// For completeness, also disconnect filterControl from model. Probably redundant since
			// model will soon be destroyed.
			m_model.removeModelChangeListener(filterControl);
		}
		// else CCapture has no filterControl.

		//
		// Remove the listener [Have to do this before the destroy, as we need
		// the action]
		//
		IAction aii = getAction(AddInstanceAction.class);
		if (aii != null) {
			m_libBrowser.removeSymbolSelectionEventListener((SymbolSelectionEventListener) aii);
		}

		SharedObjectBrowserTree sharedView = m_sobPanel != null ? m_sobPanel.getSharedView() : null;
		IProject project = getProject();
		if (project != null) {
			if (areSharedObjectsSupported()) {
				ISharedConductorMgr sharedConductorMgr = project.getSharedConductorMgr();
				ISharedPinListMgr sharedPinListMgr = project.getSharedPinListMgr();
				sharedConductorMgr.removeChangeListener(sharedView);
				sharedPinListMgr.removeChangeListener(sharedView);
			}
			deregisterSharedSyncHandler(project);
		}
		destroySharedObjectCleanupAdaptor();

		CAFUtils.getInstance().getSymbolLibraryMgr().removeSymbolChangeListener(m_libBrowser);
		IProjectPreferenceMgr preferenceMgr = project != null ? project.getPreferences() : null;
		if (preferenceMgr != null) {
			preferenceMgr.removePreferenceChangeListener(m_libBrowser);
		}

		m_treeView.destroy();

		//Mark Analysis BrowserPanel aware of design close event and hence make it avoid setting scope from preference.
		if (analysisBrowserPanel != null) {
			analysisBrowserPanel.setDesignClosing(true);
		}

		m_browserContainer.removeAll();
		if (sharedView != null) {
			sharedView.destroy();
		}
		if (m_sobPanel != null) {
			m_sobPanel.destroy();
		}
		if (m_icdPanel != null) {
			m_icdPanel.destroy();
		}
		if (m_toDoView != null) {
			m_toDoView.destroy();
		}
		if (m_partsView != null) {
			m_partsView.destroy();
		}
		if (designTreeClient != null) {
			designTreeClient.destroy();
		}

		if (m_partActionHandler != null) {
			CAFUtils.getInstance().getCAFProjectMgr().removeProjectChangeListener(m_partActionHandler);
			m_partActionHandler = null;
		}

		if (m_icdObjectActionHandler != null) {
			//TODO is this really a project change listner???
			CAFUtils.getInstance().getCAFProjectMgr().removeProjectChangeListener(m_icdObjectActionHandler);
			m_icdObjectActionHandler = null;
		}
		// ensure the Analysis browser panel cleans itself up...
		if (analysisBrowserPanel != null) {
			if (project != null) {
				project.removeProjectChangeListener(analysisBrowserPanel);
			}
			analysisBrowserPanel.destroy();
		}

		m_browserContainer.destroy();
		m_browserContainer = null;
		//
		super.destroy();

		m_model = null;
		m_libBrowser = null;
		m_treeView = null;
		m_sobPanel = null;
		m_icdPanel = null;
		m_toDoView = null;
		m_partsView = null;
		designTreeClient = null;
		if (m_pointConnector != null) {
			m_pointConnector = null;
		}
		filterControl = null;
	}

	private void deregisterSharedSyncHandler(@NotNull IProject project)
	{
		if (isSharedObjectCleanupSupported()) {
			//deregister design shared object sync handler.
			IPrivilegedDesignMgr designMgr = CommonUtils.cast(project.getDesignMgr(), IPrivilegedDesignMgr.class);
			ILogicDesign design = m_model.getDesign();
			if (designMgr != null && design != null) {
				designMgr.removeCustomSharedObjectChangeSyncHandler(design);
			}
		}
	}

	protected void destroySharedObjectCleanupAdaptor()
	{
		if (isSharedObjectCleanupSupported()) {
			if (sharedObjectCleanupAdaptor != null) {
				sharedObjectCleanupAdaptor.destroy();
				sharedObjectCleanupAdaptor = null;
			}
		}
	}

	protected Model createModel(ILogicDesign design, ISchemDiagram diagram)
	{
		Model model = new Model(this, design);
		model.addDiagram(diagram);
		return model;
	}

	@NotNull public Model getCapletModel()
	{
		return m_model;
	}

	protected void createLogicSharedObjectControllerActions()
	{
		SharedObjectBrowserTree m_sharedView = m_sobPanel.getSharedView();
		addAction(new AddSharedDeviceAction(this, m_sharedView));
		addAction(new AddSharedPlugConnectorAction(this, m_sharedView));
		addAction(new AddSharedJackConnectorAction(this, m_sharedView));
		addAction(new AddSharedInlineConnectorAction(this, m_sharedView));
		addAction(new InsertSharedInlineConnectorAction(this, m_sharedView));
		addAction(new AddSharedInterconnectConnectorAction(this, m_sharedView));
		addAction(new AddSharedInterconnectDeviceAction(this, m_sharedView));
		addAction(new AddSharedRingTerminalAction(this, m_sharedView));

		addAction(new AddSharedNetAction(this, m_sharedView));
		addAction(new AddSharedWireAction(this, m_sharedView));
		addAction(new AddSharedShieldAction(this, m_sharedView));
		addAction(new AddSharedSignalAction(this, m_sharedView));
		addAction(new AddSharedMessageAction(this, m_sharedView));
		addAction(new AddSharedGeneralHighwayAction(this, m_sharedView));
		addAction(new AddSharedSingleLineAction(this, m_sharedView));
		addAction(new AddSharedFunctionAction(this, m_sharedView));
		addAction(new SharePortAction(this, m_sharedView));
		addAction(new AddSharedSpliceAction(this, m_sharedView));
		addAction(new CreateSharedConductorGroupAction(this));
		addAction(new CreateSharedObjectRevisionAction(this, m_sharedView));
		addAction(new EditSharedOverbraidAction(this, m_sharedView));

		addAction(new EditSharedPinListAction(this, m_sharedView));
		addAction(new FreezeSharedObjectsAction(this));
		addAction(new ReplaceSharedCompositeSymbolAction(this, m_sharedView));
		addAction(new ReplaceSharedCompositeSymbolFunctionAction(this, m_sharedView));
		addAction(new AddSharedICDAction(this));
	}

	protected void createLogicControllerActions()
	{
		addAction(new CreateMultipleNetsAction(this));
		addAction(new CreateMultipleWiresAction(this));
		addAction(new CreateChamferedNetInstanceAction(this));
		addAction(new CreateChamferedWireInstanceAction(this));
		addAction(new CreateWireAction(this));
		addAction(new CreateInterconnectAction(this));

		addAction(new CreatePlugConnectorAction(this));
		addAction(new CreateNoPinPlugConnectorAction(this));
		addAction(new CreateJackConnectorAction(this));
		addAction(new CreateNoPinJackConnectorAction(this));
		addAction(new CreateInlineConnectorAction(this));
		addAction(new CreateInlineInterconnectConnectorAction(this));
		addAction(new CreateNoPinInlineConnectorAction(this));
		addAction(new InsertInlineConnectorAction(this));
		addAction(new CreateInterconnectConnectorAction(this));
		addAction(new CreateRingTerminalAction(this));
		addAction(new CreateSpliceAction(this));
		addAction(new CreateAssemblyAction(this));
		addAction(new PlaceAssemblyTreeAction(this));
		addAction(new ResetAssemblyAction(this));
		addAction(new TerminateAtSpliceAction(this));
		addUpdateDictionaryAction();
		addAction(new ManageSignalsAction(this));
		addAction(new AddLibraryMulticoreAction(this));

		if (m_treeView != null) {
			addAction(new AddLibraryInnercoreWireAction(this, m_treeView));
			addAction(new AddLibraryInnercoreNetAction(this, m_treeView));
			addAction(new AddLibraryInnercoreShieldAction(this, m_treeView));
		}

		if (m_toDoView != null) {
			addAction(new AddInterconnectWireAction(this, m_toDoView));
			addAction(new AddInterconnectShieldAction(this, m_toDoView));
			addAction(new AddInterconnectOverbraidAction(this, m_toDoView));
			addAction(new RemoveToDoItemAction(this, m_toDoView));
		}

		addAction(new SymbolCreateSharedAction(this));
		addAction(new SymbolPlaceAsGraphicsAction(this));
		addAction(new SymbolCreateSharedSpliceAction(this));

		addAction(new SymbolInvokeFromDesignToolsAction(this));

		addAction(new SetPinReferenceAction(this)); // TODO - Include in CCapture?
		addAction(new SetPinNonReferenceAction(this)); // TODO - Include in CCapture?

		addAction(new LogicGenerateNamesAction(this));
		addAction(new AddConductorNameAction(this));

		addAction(new BridgeOutFilterAction(this));
		addAction(new CreateOverbraidAction(this));

		addActionToLibBrowser(SymbolCreateSharedAction.class);
		addActionToLibBrowser(SymbolPlaceAsGraphicsAction.class);
		addActionToLibBrowser(SymbolCreateSharedSpliceAction.class);
		addActionToLibBrowser(SymbolInvokeFromDesignToolsAction.class);
		addActionToLibBrowser(CreateOtherComponentOnlyWithSymbolAction.class);

		addAction(new EditHarnessAction(this));
		addAction(new ELADataManagerAction(this));

		// put Properties action last so it appears last on the context menu
		createZOrderActions();
		createNonElectricalGraphicsActions();
		addAction(new SmartEditAction(this));

		addAction(new LogicPrototypeWireExpressionEditAction(this));

		addAction(new PropertiesAction(this, createPropertiesClient(), new TextAttributesEditor()));
		addAction(new SmartEditPropertiesAction(this, createPropertiesClient(), new TextAttributesEditor()));

		//Add print region specific actions
		createPrintRegionActions();
		addAction(new EditLeaderLineJustificationAction(this));
		addAction(new LogicSetAttributesAndPropertiesByRuleAction(this));

		addAction(new AddConductorAction(this));
		addAction(new AddPinListAction(this));
		addAction(new CreateICDFromPlacedICDAction(this));
		addAction(new AddGeneralHighwayAction(this));
		addAction(new AddSingleLineAction(this));

		addAction(new PurgeFunctionalModuleCodeAction(this));

		addAction(new ChangeFlowDirectionAction(this));
	}

	protected void addUpdateDictionaryAction()
	{

	}

	protected void addActionToLibBrowser(Class<? extends IAction> actionClass)
	{
		IAction action = getAction(actionClass);
		if (action instanceof ActionRT) {
			final Action actionUI = ((ActionRT) action).getActionUI();
			if (actionUI != null) {
				m_libBrowser.contextMenuAddAction(actionUI);
			}
			assert actionUI != null;
		}
	}

	@Override @NotNull public IPropertiesClient createPropertiesClient()
	{
		return new PropertiesClient(getCapletModel());
	}

	@Override
	@NotNull
	public IPropertiesClient createPropertiesClientForQep(boolean willLockSharedObject)
	{
		// Returns properties client that does not lock the currently selected shared object while start editing.
		return new QAPLogicPropertiesClient(getCapletModel(), willLockSharedObject);
	}

	@NotNull protected IFilterableObjectType.ObjectClass getTabularEditorObjectClass()
	{
		return IFilterableObjectType.ObjectClass.Logic;
	}

	/**
	 * Create controller actions that are shared between CCapture and CLogic.
	 */
	protected void createSharedControllerActions()
	{
		// Create the controller actions
		SelectAction selectAction = getSelectAction();
		addAction(getSelectAction());
		DeleteAction deleteAction = getDeleteAction();
		addAction(deleteAction);
		addAction(getUnplaceAction());
		addAction(new FlipAction(this));
		addAction(new RotateAction(this));
		addAction(new PivotTextAction(this));

		// DR 456389: Apply Styles should be available in CCapture and CLogic.

		StyleFlyOutAction styleFlyOut = new StyleFlyOutAction(this);
		populateStyleFlyOutAction(styleFlyOut);
		addAction(styleFlyOut);

		IAction conductorCreateAction = new CreateConductorAction(this);
		addAction(conductorCreateAction);
		addAction(new CreateGeneralHighwayAction(this));
		addAction(new CreateSingleLineAction(this));
		addAction(new CreateDeviceAction(this));
		addAction(new CreateNoPinDeviceAction(this));
		addAction(new CreateBlockDeviceAction(this));
		addAction(new CreateFunctionBlockAction(this));
		addAction(new CreateNoPinFunctionAction(this));
		addAction(new CreateFunctionConductorAction(this));
		addAction(new CreateFunctionMessageAction(this));
		addAction(new CreateInterconnectDeviceAction(this));
		addAction(new CreateCircleAction(this));
		addAction(new CreatePolylineAction(this));
		addAction(new CreateRectangleAction(this));
		addAction(new CreatePolygonAction(this));
		addAction(new CreateImageAction(this));
		addAction(new AddCommentSymbolAction(this));
		addAction(new SymbolPlaceAsGraphicsAction(this));
		addAction(new CreateArcAction(this));
		addAction(new CreateCurveAction(this));
		addAction(new CreateTextAction(this));
		createDraftingActions();
		addAction(new AddPinAction(this));
		addAction(new AddFunctionPortAction(this));
		addAction(new AddPinWNAccelAction(this));
		addAction(new AddBackshellAction(this));
		addAction(new AddBackshellTerminationAction(this));
		addAction(new AssociateConnectorAction(this));
		addAction(new DisassociateConnectorAction(this));
		addAction(new GenerateHarnessConnAction(this));
		addAction(new SelectFootprintAction(this));
		addAction(new EditFootprintAction(this));
		addAction(new ConvertToRingTerminalAction(this));
		addAction(new ConvertPinTypeAction(this));
		addAction(new MergeIntoAction(this));
		addAction(new ConvertSymbolToParamAction(this));
		addAction(new SetGraphicDimensionAction(this));
		if (areSharedObjectsSupported()) {
			SharedObjectBrowserTree m_sharedView = (m_sobPanel == null) ? null : m_sobPanel.getSharedView();
			addAction(new UnassignPortAction(this));
			addAction(new FreezeUnfreezeSharedObjectAction(this, m_sharedView));
			addAction(new LogicSharedObjectDeleteAction(this, m_sharedView));
			addAction(new LogicSharedObjectDeleteUnusedAction(this));
			addAction(new ShareAction(this));
			addAction(new UnshareAction(this));
			addAction(new EnhancedSwapOutSharedObjectRevisionAction(this, m_sharedView));
			addAction(new SharedObjectRevisionUsagesAction(this, m_sharedView));
		}
		addAction(new ResizeAction(this));
		addAction(new CreateShieldConductorAction(this));
		addAction(new AddShieldConductorAction(this));
		addAction(new CreateMulticoreAction(this));
		addAction(new CreateTwistedSheathMulticoreAction(this));
		addAction(new CreateCoaxialSheathMulticoreAction(this));
		addAction(new CreateCoaxialShieldMulticoreAction(this));
		addAction(new CreateOverbraidwithAccelAction(this));
		addAction(new AddDiagramListTableAction(this));
		addAction(new AddDeviceListTableAction(this));
		addAction(new AddWireListTableAction(this));
		addAction(new CreateSectorAction(this));

		// have to add this here so it appears before Disconnect Selected in the context menu!
		FlyoutActionContainer connectFlyout = new ConnectActionFlyout(this, ConnectActionUI.class.getName());
		ConnectByWireAction byWire = new ConnectByWireAction(this);
		ConnectByNetAction byNet = new ConnectByNetAction(this);
		ConnectByPinAction byPin = new ConnectByPinAction(this);

		// ConnectByPinAction byPin = new ConnectByPinAction(this);
		ConnectByInterconnectAction byInt = new ConnectByInterconnectAction(this);
		connectFlyout.addAction(byWire);
		connectFlyout.addAction(byNet);
		connectFlyout.addAction(byInt);
		connectFlyout.addAction(byPin);
		// connectFlyout.addAction
		addAction(connectFlyout);

		addAction(new UpdateBorderAction(this));
		addAction(new UpdateCompositeTextAction(this));

		//addAction(new ReloadIndicatorAction(this)); - to fix dts0100717716

		if (Environment.isVeSysMigrationPathAllowed()) {
			addAction(new MoveToGridAction(this));
		}
		addAction(new RerouteSegmentAction(this));

		SegmentModifier insertModifier = new InsertConductorSegmentModifier(this);
		SegmentModifier deleteModifier = new DeleteConductorSegmentModifier(this);
		FlyoutActionContainer gripPointContainer = createGripPointActions(insertModifier, deleteModifier);

		SegmentModifier toggleChamfers = new ToggleChamferSegmentModifier(this);
		gripPointContainer.addAction(new ToggleChamferAction(this, toggleChamfers));
		IAction autoRouteAction = new AutoRouteAction(this);
		addAction(autoRouteAction);

		if (!BuildInfo.getBuildInfo().isOfficialRelease() &&
				(BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled() ||
						BuildInfo.getBuildInfo().areQAExtensionsEnabled())) {
			addAction(new AutoRouteSettingsAction(this));
		}

		addAction(new RouteIntoHighwayAction(this));
		addAction(new UnRouteHighwayAction(this));

		addAction(new MeasureDistanceAction(this));

		addAction(new MovePinAction(this));
		addAction(new MoveFunctionPortAction(this));
		addAction(new MoveConnectorAction(this));
		addAction(new CreateStackPinAction(this));
		addAction(new AddToStackPinAction(this));
		addAction(new EditStackPinAction(this));
		addAction(new MoveWireEndAction(this));

		addAction(new StripAtSpliceAction(this));
		addAction(new InsertGfxPointAction(this, new PolylineModifier(this)));
		addAction(new DeleteGfxPointAction(this, new PolylineModifier(this)));
		addAction(new GroupGfxAction(this));
		addAction(new UngroupGfxAction(this));

		addAction(new ToggleIndicatorConstraintsAction(this));

		addAction(new ModifyBorderAction(this));
		addAction(new ToggleHomeAction(this, ToggleHomeActionUI.MARK_HOME));
		addAction(new ToggleHomeAction(this, ToggleHomeActionUI.REMOVE_HOME));
		addAction(new ToggleShowXRefAction(this, ToggleShowXRefActionUI.SHOW_XREF));
		addAction(new ToggleShowXRefAction(this, ToggleShowXRefActionUI.HIDE_XREF));

		addAction(new AddIndicatorsAction(this));
		addAction(new AddCommentSymbolToMCAction(this));
		addAction(new AddChainAction(this));
		// Feat00015752 Improve Publisher
		addAction(new SliceAction(this));
		// addAction(new ModifyPokeHomeAction(this);
		addAction(new AssociateConnectorAction(this));
		addAction(new DisassociateConnectorAction(this));

		addAction(new DataImportAction(this));
		addAction(new DataExportAction(this));
		addAction(new DataExportFilterAction(this));

		addAction(new BridgeInAction(this));
		addAction(new ConnectedModeBridgeInAction(this));
		addAction(new BridgeOutAction(this));
		addAction(new TCBridgeOutAction(this));
		addAction(new TCBridgeInAction(this));
		addAction(new BridgeOptionAction(this));

		addAction(new SwapOutSharedObjectRevisionAction(this));

		//
		// Create the action, and make it a listener on the
		// library browser.
		//
		AddInstanceAction addInstanceAction = new AddInstanceAction(this);
		addAction(new UpdatePartAction(this));
		if (ICDUtils.areICDsSupported()) {
			addAction(new UpdateICDAction(this));
		}
		addAction(new RemoveDeviceConnectorsAction(this));
		addAction(new TabularEditAction(this, getTabularEditorObjectClass()));
		addAction(new SaveAssemblyConnectivityToLibraryAction(this));
		addAction(addInstanceAction);
		addAction(new AddDeviceFromLibraryPartAction(this));
		addAction(new AddDeviceFromLibraryWithPinsAction(this));
		addAction(new AddSpliceFromLibraryPartAction(this));

		addAction(new AddDeviceFromICDAction(this));
		addAction(new AddParametrizedDeviceFromICDAction(this));

		addAction(new UpdateInstanceAction(this, ConductorRouteAction.getInstance()));
		addAction(new ExportAsSymbolAction(this));
		if (!AppInfo.isSvcDoc()) {
			addAction(new ReplaceInstanceSymbolAction(this, ConductorRouteAction.getInstance()));
		}
		else {
			addAction(new PublisherReplaceInstanceSymbolAction(this, ConductorRouteAction.getInstance()));
		}
		addAction(new FindReplaceSelectionAction(this));
		m_libBrowser.addSymbolSelectionEventListener(addInstanceAction);
		// addAction(new AddLibraryAction(this, m_libBrowser);
		addAction(new PropagateSelectedHarnessAction(this));
		addAction(new PropagateAllHarnessAction(this));

		addAction(new DumpSelectedAction(this));
		addAction(new DumpSelectedObjectDetailsAction(this));
		addAction(new DumpOriginAction(this));
		addAction(new MakeBaseIdsSameAction(this));
		addAction(new AddLinkAction(this, getLinkHandler()));
		addAction(new DumpSelectedSharedPinMatingAction(this));
		addAction(new DumpICDDeviceAction(this));
		addAction(new BuildICDFromJsonAction(this));
		addAction(new DumpICDSignalsAction(this));
		addAction(new VariantICDTogglePinConstraintAction(this));
		addAction(new AddPropertiesToObjectsAction(this));

		addAction(new HackVisibilityAction(this));
		addAction(new BrowseSelectedObjectAction(this));
		// vkhatri
		addAction(new ConnectIndicatorsAction(this));

		// Add the actions to the controller

		// Create and add the Analysis actions if analysis is available
		if (CapitalAnalysisFactory.getAnalysisInterface() != null) {
			// Create the actions
			LogicAnalysisAttachmentTargetProvider targetProvider = new LogicAnalysisAttachmentTargetProvider(m_model);
			LogicAttachModelAction attachModelAction =
					new LogicAttachModelAction(this, targetProvider, false, false, false);
			AttachSVModelAction svAttachAction = new AttachSVModelAction(this, targetProvider, false, false);
			LogicBuildModelAction buildModelAction =
					new LogicBuildModelAction(this, targetProvider, false, false, false);
			EditModelAction editModelAction = new EditModelAction(this, targetProvider);

			// use in dynamic simulation......
			final LogicAnalysisColoringProcessor processor = LogicAnalysisColoringProcessor.getProcessor(m_model);

			IAnalysisScopedDesignProvider provider = new IAnalysisScopedDesignProvider()
			{
				private IAnalysisNetlistScope scope;

				private IAnalysisColoringProcessor baseProcessor = processor;
				private IAnalysisColoringProcessor coloringProcessor;

				@Nullable public IAnalysisNetlistScope getScope()
				{
					scope = AnalysisServices.getCurrentAnalysisNetlistScope();
					if (scope == null) {
						scope = createScope();
					}

					if (scope != null) {
						coloringProcessor = baseProcessor.getScopedColoringProcessor(scope);
					}
					return scope;
				}

				private IAnalysisNetlistScope createScope()
				{
					final IDesignContainer activeDesign = m_model.getDesign();
					if (activeDesign != null) {
						final IProject currentProject = activeDesign.getProject();

						boolean isScopeSetFromPreference = AnalysisServices
								.setPreferenceAnalysisScope(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
										activeDesign, currentProject, true);
						if (!isScopeSetFromPreference) {
							AnalysisServices.changeScope(activeDesign, currentProject.getUID().toString());
						}
						if (CAFUtils.getInstance().getActiveCapletController() != null) {
							addAuditTab();
							CAFUtils.getInstance()
									.tickleUI(CAFUtils.getInstance().getActiveCapletController().getCaplet().getFIB());
						}
					}

					return AnalysisServices.getCurrentAnalysisNetlistScope();
				}

				@Nullable public IDesignContainer getDesign()
				{
					IAnalysisNetlistScope analysisScope = getScope();
					if (analysisScope instanceof IAnalysisNetlistTopoDesignScope) {
						return null;
					}
					else {
						return Objects.requireNonNull(analysisScope).getDesigns().next();
					}
				}

				public boolean isCoherentWithCurrentScope()
				{
					return !(getScope() instanceof IAnalysisNetlistTopoDesignScope);
				}

				public void clearDynamicGfx()
				{
					getScope();
					if (coloringProcessor != null) {
						coloringProcessor.clearColoring();
					}
					else {
						baseProcessor.clearColoring();
					}
				}

				@Nullable public String getDesignNetlist()
				{
					getScope();
					if (scope != null) {
						return scope.netlist();
					}
					else {
						return null;
					}
				}

				@Nullable public IAnalysisColoringProcessor getScopedColoringProcessor()
				{
					getScope();
					return coloringProcessor != null ? coloringProcessor : baseProcessor;
				}

				public void setAnalysisActive(boolean active)
				{
					getScope();
					if (scope != null) {
						scope.setAnalysisActive(active);
					}
				}

				public void destroy()
				{
					// todo: destroy and tidy. rharring
				}
			};

			SubsystemSimulatorAction subsystemSimulatorAction = new SubsystemSimulatorAction(this, processor, provider);
			SubsystemFMEAAction subsystemFMEAAction = new SubsystemFMEAAction(this, processor, provider);
			SubsystemSCAAction subsystemSCAAction = new SubsystemSCAAction(this, processor, provider);
			SubsystemImportAction subsystemImportAction = new SubsystemImportAction(this, processor, provider);
			LogicStressAction subsystemStressAction =
					new LogicStressAction(this, new LogicAnalysisBackAnnoProcessor(), provider);

			DynSimOffAction dsOffAction = new DynSimOffAction(this);
			DynSimOnDemandAction dsDemandAction = new DynSimOnDemandAction(this);
			DynSimBackgroundAction dsBackgroundAction = new DynSimBackgroundAction(this);
			SimulateAction simAction = new SimulateAction(this);
			ResetAction resetAction = new ResetAction(this);
			QualitativeSimulationModeAction qualAction = new QualitativeSimulationModeAction(this);
			SpiceSimulationModeAction spiceAction = new SpiceSimulationModeAction(this);

			AnalysisPopupMenuBuilderAction menuAction = new AnalysisPopupMenuBuilderAction(this);
			AnalysisInterfaceToggleAction toggleAction = new AnalysisInterfaceToggleAction(this);
			ViewFailedComponentsAction fcAction = new ViewFailedComponentsAction(this);

			ELANodeAction elaNodeAction = new ELANodeAction(this, processor, provider);
			ExportNetlistAction enAction = new ExportNetlistAction(this, processor, provider);

			if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled()) {
				SubsystemViewNetlistAction viewNetlistAction =
						new SubsystemViewNetlistAction(this, processor, provider);
				SubsystemViewProjectNetlistAction viewProjectNetlistAction =
						new SubsystemViewProjectNetlistAction(this, processor, provider);
				addAction(viewProjectNetlistAction);
				addAction(viewNetlistAction);
			}

			getCaplet().addActionUI(new AnalysisPopupMenuBuilderActionUI(getCaplet()));

			AssociateSymbolAction associateSymbolAction = new AssociateSymbolAction(this);

			// Add the Actions
			addAnalysisAction(attachModelAction, true);
			addAnalysisAction(buildModelAction, true);
			addAnalysisAction(editModelAction, true);
			addAnalysisAction(svAttachAction, false);
			addAction(new LogicDeratingAction(this));
			addAction(associateSymbolAction);
			addAction(subsystemSimulatorAction);
			addAction(subsystemFMEAAction);
			addAction(subsystemSCAAction);
			addAction(subsystemStressAction);
			addAction(subsystemImportAction);
			addAction(simAction);
			addAction(resetAction);
			addAction(menuAction);
			addAction(dsOffAction);
			addAction(dsDemandAction);
			addAction(dsBackgroundAction);
			addAction(fcAction);
			addAnalysisAction(qualAction, true);
			addAnalysisAction(spiceAction, true);
			addAction(menuAction);
			addAction(enAction);
			addAction(elaNodeAction);

			// must add to ui b4 we addAction else the accel key is not added.
			getCaplet().addActionUI(toggleAction);
			addAction(toggleAction);
			// add a stroke to allow the interface properties to be toggled.
			addStroke("14789", toggleAction);
		}

		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.LEFT));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.RIGHT));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.TOP));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.BOTTOM));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.VERTICAL_CENTER));
		addAction(new AlignAction(this, chs.caf.caplet.helpers.graphics.AlignAction.HORIZONTAL_CENTER));
		addAction(new DistributeAction(this, chs.caf.caplet.helpers.graphics.DistributeAction.HORIZONTAL));
		addAction(new DistributeAction(this, chs.caf.caplet.helpers.graphics.DistributeAction.VERTICAL));

		if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled()) {
			addAction(new ReloadIndicatorAction(this));
		}

		// Add App Actions defined by Resource - extend selection action and show functional source action.
		processResourceAppActions();

		// Register Strokes
		addStroke("741236987", deleteAction);
		addStroke("7412687", deleteAction); // Aliased
		addStroke("258", conductorCreateAction);
		addStroke("96321", autoRouteAction);

		// Set the Select Action as the base action in the
		// action manager.
		getActionMgr().setBaseAction(selectAction);
	}

	@NotNull protected SelectAction getSelectAction()
	{
		return new SelectAction(this);
	}

	@NotNull protected DeleteAction getUnplaceAction()
	{
		return new UnplaceAction(this);
	}

	@NotNull protected DeleteAction getDeleteAction()
	{
		return new DeleteAction(this);
	}

	private void populateStyleFlyOutAction(StyleFlyOutAction styleFlyOut)
	{

		IAction action = new ApplyStyleOnDiagramObjectAction(this);
		styleFlyOut.addAction(action);

		action = new ApplyStyleToAllAction(this);
		styleFlyOut.addAction(action);

		action = new ApplyStyleToSegmentsAction(this);
		styleFlyOut.addAction(action);

		action = new EditStyleAction(this);
		styleFlyOut.addAction(action);

		action = new SelectAllLockedDecorationAction(this);
		styleFlyOut.addAction(action);

		action = new ExcludeFromApplyStyleAction(this);
		styleFlyOut.addAction(action);

		action = new FixPositionAction(this);
		styleFlyOut.addAction(action);

		action = new ForceApplyStyleAction(this);
		styleFlyOut.addAction(action);

		action = new WriteToStyleAction(this);
		styleFlyOut.addAction(action);

		action = new SplitTableAction(this);
		styleFlyOut.addAction(action);

		action = new ResetOrderAction(this);
		styleFlyOut.addAction(action);
	}

	public JComponent getBrowser()
	{
		return m_browserContainer;
	}

	public LogicFilterControl getGraphicsFilterControl()
	{
		if (filterControl == null) {
			IProject project = getCapletModel().getDesign().getProject();
			if (project != null) {
				IProjectPreferenceMgr preferenceMgr = project.getPreferences();
				if (preferenceMgr.getValidateableEnableOptionExpression()) {
					filterControl = LogicFilterControlMgr.getInstance().getFilterControl(project);
				}
			}
		}
		// else only create it once or not at all in the case of CCapture which doesn't support
		// option expressions and hence cannot do option filtering.

		return filterControl;
	}

	public void selectionChanged(SelectEvent e)
	{
		// Ignore notification resulting from us selecting
		// associated objects
		if (m_bSelectingAssoc) {
			return;
		}

		// Set the state so we ignore selections
		m_bSelectingAssoc = true;
		CapletUtils.selectAssociatedObjects(e);
		m_bSelectingAssoc = false;
	}

	@SuppressWarnings({"NoopMethodInAbstractClass"}) public void modelDeactivated(boolean isClosing)
	{
	}

	// simons DR 11476. Moved from CAFDiagram.

	public void modelActivated()
	{
		CAFUtils.getInstance().getScanningLock().obtainScanningLock();
		try {
			//
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			if (view instanceof IGfxView) {
				IBaseDiagram viewDiagram = ((IGfxView) view).getDiagram();
				if (viewDiagram instanceof ISchemDiagram) {
					ISchemDiagram schemDiagram = (ISchemDiagram) viewDiagram;
					m_model.setCurrentDiagram(schemDiagram);

					//17.1 dts0101250904 In Read only diagram allowing user to edit the design (Need to update flag of the Read-only mode)
					boolean editable = view.getCapletModel().isEditable();
					ICapletWindow capletWindow = view.getWindow();
					if (editable && capletWindow != null) {
						String readonlyText = ResourceMgr
								.getString(LogicCapletUtils.class, "LogicCapletUtils.ReadOnlyQualifier.text");
						if (capletWindow.getTitle().contains(readonlyText)) {
							String title = LogicCapletUtils.getDiagramTitle(schemDiagram, false);
							capletWindow.setTitle(title);
						}
					}

					TextHelper.setPinSpacing(schemDiagram.getGrid().getGridSpacing());

					// Add the listener that will be used to refresh indicators...
					// TODO jacobt FEAT13040 : listener leak?
					// this appeared to be leaking when it was previously added in the Model constructor when the Model was the diagram

					// dts0100586677 - add the ONLY listener - this should not leak & be the only one.
					// previously multiple indicatorRefresher where being added.
					if (m_indicatorRefreshControl != null &&
							!m_model.hasModelChangeListener(m_indicatorRefreshControl)) {
						m_model.addModelChangeListener(m_indicatorRefreshControl);
					}
				}
			}

			// Check to see if any of my conductors has been added to or
			// removed from a shared conductor group while I was out.

			ILogicDesign design = m_model.getDesign();
			ISchemDiagram diagram = m_model.getDiagram();
			// dts0100969009 - do not bother doing this work on deleted diagrams
			if (design == null || diagram == null || diagram.isDeleted()) {
				return;
			}
			Set<IMulticore> seen = new HashSet<IMulticore>(); // We don't want to do any Multicore twice.
			List<chs.cof.logical.cable.IConductor> conductorsWithShared =
					new ArrayList<chs.cof.logical.cable.IConductor>();
			Set<IMulticore> outermost = new HashSet<IMulticore>();
			for (Object o : diagram.getConductors()) {
				IConductor schemCond = (IConductor) o;
				chs.cof.logical.cable.IConductor cond = schemCond.getConnectivity();

				IMulticore omc = ConductorHelper.findOutermost(cond);
				if (omc != null) {
					outermost.add(omc);
				}
				// First, make sure this isn't a shield, since it won't have ancestory from within a multicore
				// but could have a shared multicore with which it is asociated. In otherwords, we don't care about
				// shields. dts0100346936
				if (cond instanceof IShieldConductor) {
					continue;
				}

				// Gathering up cable conductors with shared
				if (cond.getSharedConductor() != null && cond.getSharedConductor().getMulticore() != null) {
					IMulticore mc = cond.getMulticore();
					if (mc == null) {
						// Want all the ones without a multicore, SharedConductorHelper.fixupMulticoreAncestry(), may need
						// to create cable multicore(s)
						conductorsWithShared.add(cond);
					}
					else if (!seen.contains(mc)) {
						// Only need one from each multicore. Just need to create schem indicators for existing cable
						// multicores. SharedConductorHelper.fixupMulticoreAncestry() will work
						// from the inside out and catch all outer multicore(s).
						conductorsWithShared.add(cond);
						seen.add(cond.getMulticore());
					}
				}
			}

			design.beginLocalEdit();
			try {
				// This removes cable multicore content based on updated contents of shared multicores
				Set<IMulticore> alteredMulticores = SharedConductorHelper.fixupDescendantsForMulticore(outermost);

				//dts0100564226: we shouldn't place the indicators in the diagram loading because the user can delete them.
				alteredMulticores.addAll(SharedConductorHelper
						.fixupParentageForConductors(conductorsWithShared, diagram, false));

				// Check if any of my shared multicores have acquired a shield, and if so add hookups to all schem indicators.
				SharedConductorGroupHelper.updateIndicatorHookups(diagram, CommonUtils.getNoFilter());

				// Delete empty multicores, regenerate indicaters on the rest
				SharedConductorHelper.processAlteredMulticores(alteredMulticores, diagram);

				// DR 399063: activation can cause creation of multicores - and it happens outside of a controller action.
				// We therefore need to regenerate the usages. A more generic fix would be for
				// UIManager.fireModelActivationChange() to call CapletModelHelper.notifyModelChange() with the lists of
				// new and deleted models extracted from the CDH.
				IDesignSharedUsageMgr usageMgr = design.getSharedUsageMgr();
				// FEAT15840: Usages are regenerated even on read only switching between diagrams on a read only design.
				// So need to do it in scrubbing mode.
				((IWriteableDSUM) usageMgr).regenerateUsages(true);
			}
			finally {
				design.endLocalEdit();
			}

			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
			if (cdh.getPendingCount() != 0) {
				cdh.clear();
			}
			if (diagram.getUID().isEquiv(design.getInterconnectSourceInfo().getDiagramUID())) {
				activateBrowser("ToDo");
			}
			activateDesignBrowserInImmersedMode(design);
		}
		finally {
			CAFUtils.getInstance().getScanningLock().releaseScanningLock();
		}

		// we need to clear any graphics due to selections on other diagrams in this design
		m_model.getDynamicGfxService().resetSelections();
	}

	private void activateDesignBrowserInImmersedMode(ILogicDesign design)
	{
		if (Environment.isImmersedMode()) {
			ImmersedModeServices.getService(ICapitalEventClient.class)
					.sendRequestFor(new DesignOpenedEvent(Objects.requireNonNull(design.getProject()), design));
		}
	}

	public void activateBrowser(String whichBrowser)
	{
		super.activateBrowser(whichBrowser);
		if ("ToDo".equalsIgnoreCase(whichBrowser) && !m_browserContainer.containsBrowser(m_toDoView)) {
			IDesign design = m_model.getDesign();
			ISchemDiagram diagram = m_model.getDiagram();
			if (diagram.getUID().isEquiv(design.getInterconnectSourceInfo().getDiagramUID())) {
				m_browserContainer.insertTab(ResourceMgr.getString(InterconnectSourceBrowserTree.class,
						"InterconnectSourceBrowserTree.ToDo.title"), null, m_toDoView, ResourceMgr
						.getString(InterconnectSourceBrowserTree.class,
								"InterconnectSourceBrowserTree.ToDo.tooltip"), 1);
				//dts0100533167: we should re-initialize the browserContainer to add the newly added tab.
				m_browserContainer.postInit();
			}
		}
	}

	private LinkHandler getLinkHandler()
	{
		return new LinkHandler();
	}

	@Nullable protected IProject getProject()
	{
		IDesign design = m_model.getDesign();
		if (design == null) {
			return null;
		}
		return design.getProject();
	}

	@Nullable public IActionable getActionableBrowser(String whichBrowser)
	{
		if ("Diagram".equalsIgnoreCase(whichBrowser)) {
			return m_treeView;
		}
		else {
			return null;
		}
	}

	@Override protected void switchToReadOnly(ICapletModel model)
	{
		super.switchToReadOnly(model);
		if (model instanceof ILogicModel) {
			((ILogicModel) model).getDesign().getSharedUsageMgr().designUnlocked();
		}
	}

	@Override @NotNull public IUIResourceProvider getUIResourceProvider()
	{
		if (uiResourceProvider == null) {
			uiResourceProvider = new BaseUIResourceProvider();
		}
		return uiResourceProvider;
	}

	private class BrowserContainer extends BrowserTabbedPane implements IModelChangeListener, IBrowserTreeContainer
	{

		private List<Component> m_browsers;
		private Component homeTab = null;

		BrowserContainer(Frame parent, ICapletModel capletModel)
		{
			super(parent, capletModel);
			m_browsers = new ArrayList<Component>(5);
		}

		public boolean containsBrowser(Component browser)
		{
			return m_browsers.contains(browser);
		}

		public void insertTab(@NotNull String title, @Nullable Icon icon, @NotNull Component component,
				@Nullable String tip, int index)
		{
			int newIndex;
			if (component instanceof JTree) {
				Component browserPanel;
				if (containsBrowser(component)) {
					final int existingComponentIndex = m_browsers.indexOf(component);
					browserPanel = getComponentAt(existingComponentIndex);
				}

				else {
					// Wrap trees in a scroll pane.
					JPanel newBrowserPanel = new JPanel();
					newBrowserPanel.setName(title);
					newBrowserPanel.setLayout(new BorderLayout());
					newBrowserPanel.add(new JScrollPane(component), BorderLayout.CENTER);
					browserPanel = newBrowserPanel;
				}
				assert browserPanel != null;
				super.insertTab(title, icon, browserPanel, tip, index);
				newIndex = indexOfComponent(browserPanel);
			}
			else {
				// If the component isn't a tree, assume the component either alreay has or doesn't need a scroll pane.
				super.insertTab(title, icon, component, tip, index);
				newIndex = indexOfComponent(component);
			}

			// List what's been added.
			// dts0101178410 : Reinsert of same component must have called removeTabAt(index), hence use new index
			if (newIndex >= 0 && newIndex <= m_browsers.size()) {
				m_browsers.add(newIndex, component);
			}
			else {
				assert false : "Wrong new index : indexoutofbound";
			}
		}

		public void setHomeTab(Component homeCmpt)
		{
			homeTab = homeCmpt;
		}

		public void setHomeTreeExpansionEnabled(boolean enabled)
		{
			if (designTreeClient != null) {
				designTreeClient.setAutoExpandOnCreation(enabled);
			}

			boolean alreadyEnabled = m_model.hasModelChangeListener(this);
			if (enabled != alreadyEnabled) {
				if (enabled) {
					m_model.addModelChangeListener(this);
				}
				else {
					m_model.removeModelChangeListener(this);
				}
			}
		}

		@Override public void activateHomeTab()
		{
			if (homeTab != null) {
				setSelectedComponent(homeTab);
			}
		}

		public void modelPreChanged(ModelChangeEvent e)
		{
		}

		public void modelChanged(ModelChangeEvent e)
		{
			if (homeTab != null) {
				// Only change tab for added objects
				Collection<IUID> newObjs = e.getNewObjectsUIDs();
				if (newObjs != null && !newObjs.isEmpty()) {
					setSelectedComponent(homeTab);
				}
			}
		}

		public void destroy()
		{
			m_browsers.clear();
			m_model.removeModelChangeListener(this);
			super.destroy();
		}
	}

	private static class LogicDesignConcurrentUpdateHandler
	{

		private Set<ILogicConcurrencyEventListener> m_concurDesignUpdateListeners = new LinkedHashSet<>();

		private LogicDesignConcurrentUpdateHandler(@NotNull ILogicDesign logicDesign,
				@NotNull ICapletController baseController)
		{
			IConnectivity connectivity = logicDesign.getConnectivity();
			if (connectivity != null) {
				registerHandler(new ConnectivityRefreshHandler(logicDesign, baseController));
				registerHandler(new LogicDesignUpdateBrowserRefresh(logicDesign, baseController));
				registerHandler(new LogicDesignUpdateSelectionCleaner(logicDesign, baseController));
			}
		}

		private void registerHandler(@NotNull ILogicConcurrencyEventListener concurrencyEventListener)
		{
			m_concurDesignUpdateListeners.add(concurrencyEventListener);
			LogicConcurrencyController.getInstance().registerListener(concurrencyEventListener);
		}

		private void destroy()
		{
			for (ILogicConcurrencyEventListener concurrentDesignUpdateListener : m_concurDesignUpdateListeners) {
				LogicConcurrencyController.getInstance().unregisterListener(concurrentDesignUpdateListener);
			}
		}
	}

	@Override protected ITeamPlayLinksBrowserController createLinksTabController(ICapletController capletController)
	{
		return new TeamPlayBaseLinksBrowserController(capletController);
	}

	@Override public BrowserTabbedPane getBrowserTabbedPane()
	{
		return m_browserContainer;
	}

	@Override public void addIndicatorRefreshListener()
	{
		for (IModelChangeListener listener : m_suspendedIndicatorRefreshers) {
			getCapletModel().addModelChangeListener(listener);
		}
		m_suspendedIndicatorRefreshers.clear();
	}

	@Override public void removeIndicatorRefreshListener()
	{
		m_suspendedIndicatorRefreshers = new ArrayList<>();
		if (m_indicatorRefreshControl != null && m_model.hasModelChangeListener(m_indicatorRefreshControl)) {
			m_model.removeModelChangeListener(m_indicatorRefreshControl);
			m_suspendedIndicatorRefreshers.add(m_indicatorRefreshControl);
		}
	}

	protected class SharedObjectPreModifyListenerControl implements IActionMgrListener
	{

		public SharedObjectPreModifyListenerControl()
		{
		}

		@Override public void activateEnded(IAction action, @Nullable IActionEnum status)
		{
		}

		@Override public void terminateStarted(IAction action)
		{
			SharedObjectChangeNotification.getInstance().actionTerminateStarted();
		}

		@Override public void terminateEnded(IAction action, boolean status)
		{
			SharedObjectChangeNotification.getInstance().actionTerminateEnded(status);
		}
	}

	protected class SharedObjectPersistenceListner extends DefaultSharedObjectPersistenceListner
	{

		private Collection<IUID> persistedSharedObjects = new HashSet<>();

		public SharedObjectPersistenceListner()
		{

		}

		@Override public void saved(@NotNull ISharedObject sharedObject)
		{
			super.saved(sharedObject);
			persistedSharedObjects.add(sharedObject.getUID());
		}

		@Override public void clear(boolean success)
		{
			if (!persistedSharedObjects.isEmpty() && success) {
				getUndoableContainer().endEdit();
				getUndoableContainer().clear();
				clearUndoQueue();
			}
			persistedSharedObjects.clear();
		}

		@Override public void saved(@NotNull ICOGManaged managed)
		{
			super.saved(managed);
			if (managed instanceof ISharedObject) {
				persistedSharedObjects.add(managed.getUID());
			}
		}
	}
}
