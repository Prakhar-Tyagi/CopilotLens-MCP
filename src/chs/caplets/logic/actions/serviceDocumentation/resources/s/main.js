/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, alert, requirejs*/
var progIds = ['MSXML2.XMLHTTP.6.0', 'Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'];
require.config({
    baseUrl: "s",
    waitSeconds: 30,
    shim: {
        underscore: {
            exports: '_'
        },
        backbone: {
            deps: [
                'underscore',
                'jquery'
            ],
            exports: 'Backbone'
        },
        select2: {
            deps: [
                'jquery'
            ],
            exports: 'select2'
        }
    },
    paths: {
        text: "libs/text",
        jquery: 'libs/jquery',
        select2: 'libs/select2',
        underscore: 'libs/underscore',
        backbone: 'libs/backbone',
        router: "router",
        currentPackage: "models/currentPackage",
        // DesignObjects : "collections/wireCollection",
        DesignObjectsView: "views/designObjectsView",
        SectionCollection: "collections/sectionCollection",
        textSearch: "filters/textSearch",
        systems: "collections/systems",
        fileDisplayHandler: "filehandlers/fileHandlers",
        FaultCodes: "collections/faultCodes",
        Harnesses: "collections/harnesses",
        GlobalReports: "collections/globalreports",
        PanelDataCollection: "collections/panelDataCollection",
        LocationViews: "collections/locationViews",
        PopoverModel: "models/p/popoverModel",
        PopoverFilterModel: "models/p/popoverFilterModel",
        DesignObjectPopoverModel: "models/p/designObjectPopoverModel",
        LocationViewObjectPopoverModel: "models/p/locationViewObjectPopoverModel",
        PackagesPopoverModel: "models/p/packages/packagesPopoverModel",
        PrintOptionsPopoverModel: "models/p/print/printOptionsPopoverModel",
        PrintContentPopoverModel: "models/p/print/printContentPopoverModel",
        SelectedPrintContentModel: "models/p/print/selectedPrintContentModel",
        DiagramsPopoverModel: "models/p/diagrams/diagramsPopoverModel",
        ReportsPopoverModel: "models/p/reports/reportsPopoverModel",
        RelatedDataPopoverModel: "models/p/relatedData/relatedDataPopoverModel",
        LanguagesPopoverModel: "models/p/languages/languagesPopoverModel",
        ZoomToolBarModel: "models/zoomToolBarModel",
        ZoomAndPanModule: "models/svg/ZoomAndPanModule",
        SVGTransformModel: "models/svg/SVGTransformModel",
        DragController: "views/dragController",
        SelectedObjectsStore: "models/svg/SelectedObjectsStore",
        jt3DModel: "models/jt3DModel",
        ra3DModel: "models/ra3DModel",
        base3DModel: "models/base3DModel",
        PersistenceModelFactory: "models/store/PersistenceModelFactory",
        LocalStoragePersistenceModel: "models/store/LocalStoragePersistenceModel",
        CookieBasedePersistenceModel: "models/store/CookieBasedePersistenceModel",
        RemoteStoragePersistenceModel: "models/store/RemoteStoragePersistenceModel",
        // GridModel: "illustrator/models/reports/gridModel",
        GroupByModel: "illustrator/models/reports/groupByModel",
        SummaryGridModel: "illustrator/models/reports/SummaryGridModel",
        SignalTracerModel: "models/signalTracerModel",
        XRefActiveConfigModel: "models/c/xrefActiveConfigModel",
        BaseConfigurationsModel: "models/c/baseConfigurationsModel",
        ConfigurationsModel: "models/c/configurationsModel",
        XRefBuilderModel: "models/c/xRefBuilderModel",
        LanguagesCollection: "collections/p/languages/languagesCollection",
        PackagesCollection: "collections/p/packages/packagesCollection",
        PrintOptionsCollection: "collections/p/print/printOptionsCollection",
        PrintContentCollection: "collections/p/print/printContentCollection",
        XRefsCollection: "collections/p/xrefsCollection",
        AttributesCollection: "collections/p/attributesCollection",
        DiagramsCollection: "collections/p/diagrams/diagramsCollection",
        // DevicesCollection : "collections/p/relatedData/devicesCollection",
        // SignalsCollection : "collections/p/relatedData/signalsCollection",
        ReportsCollection: "collections/p/reports/reportsCollection",
        OptionsCollection: "collections/p/c/optionsCollection",
        ConfigurationsCollection: "collections/p/c/configurationsCollection",
        LocalConfigurations: "collections/p/c/localConfigurations",
        PopoverItem: "collections/p/popoverItemCollection",
        XRefsCollectionItem: "collections/p/XRefsCollectionItem",
        PopoverItemView: "views/p/popoverItemView",
        XRefsViewItem: "views/p/XRefsViewItem",
        ConnectorFaceviewsCollection: "collections/p/connectorFaceviewCollection",
        ThreeDViewCollection: "collections/p/threeDViewCollection",
        CustomDataCollection: "collections/p/customDataCollection",
        TwoDLocationCollection: "collections/p/twoDLocationCollection",
        ListGroupView: "views/listGroupView",
        BasicPopoverView: "views/p/basicPopoverView",
        PopoverView: "views/p/popoverView",
        ListView: "views/ListView",
        BaseConfigurationsBuilderView: "views/p/c/baseConfigurationsBuilderView",
        layoutManager: "reWork/modules/layoutManager",
        EULA: "views/EULA",
        componentRouter: "routers/componentRouter",
        Projects: "collections/Projects",
        PackagesInSession: "models/PackagesInSession",
        Project: "models/Project",
        Packages: "collections/Packages",
        Package: "models/Package",
        SmartViewComponent: 'models/SmartViewComponent',
        SmartViewComponents: 'collections/SmartViewComponents',
        ProjectsViewModel: 'viewModels/ProjectsViewModel',
        PackagesViewModel: 'viewModels/PackagesViewModel',
        UserSession: 'models/UserSession',
        ComponentLoader: 'ComponentLoader',
        TranslationUtils: 'utilities/TranslationUtils',
        harnessLayouts: "collections/harnessLayouts",
        harnessLayoutBarHandler: "views/contentpanel/harnessLayoutBarHandler",
        Diagnostics: "collections/diagnostics",
        DiagnosticsView: "views/diagnosticsView",
        DiagnosticPanel: "views/contentpanel/diagnosticPanel",
        LanguagesPanel: "views/p/settings/LanguagesPanel",
        ProjectsPanel: "views/p/projects/ProjectsPanel",
        SystemScopePanel: "views/p/settings/SystemScopePanel",
        SVGTransforms: "utilities/SVGTransforms",
        allColors: "models/p/colors/allcolors",
        recentColors: "models/p/colors/recentcolors",
        treeViewHelper: "views/treeViewHelper"
    },
    config: {
        text: {

            createXhr: function () {
                var xhr, i, progId;

                for (i = 0; i < 3; i += 1) {
                    progId = progIds[i];
                    try {
                        xhr = new ActiveXObject(progId);
                    }
                    catch (e) {
                    }

                    if (xhr) {
                        progIds = [progId];  // so faster next time
                        break;
                    }
                }
                if (!xhr && typeof XMLHttpRequest !== "undefined") {
                    return new XMLHttpRequest();
                }
                return xhr;
            }

        }
    }

});

require([
    'app'
], function (App) {
    "use strict";
    App.initialize();
});
