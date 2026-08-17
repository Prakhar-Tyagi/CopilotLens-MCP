var __SMARTCLIENT_TEST_ENV__ = true;
var allTestFiles = [];
var TEST_REGEXP = /(Test|test)\.js$/i;

// Get a list of all the test files to include
Object.keys(window.__karma__.files).forEach(function (file) {
    if (TEST_REGEXP.test(file)) {
        // Normalize paths to RequireJS module names.
        // If you require sub-dependencies of test files to be loaded as-is (requiring file extension)
        // then do not normalize the paths
        console.log("**** " + file);
        var normalizedTestModule = file.replace(/^\/base\/|\.js$/g, '')
        allTestFiles.push(file)
    }
})

var progIds = ['MSXML2.XMLHTTP.6.0', 'Msxml2.XMLHTTP', 'Microsoft.XMLHTTP', 'Msxml2.XMLHTTP.4.0'];
require.config({
    // Karma serves files under /base, which is the basePath from your config file
    baseUrl: '/base/s',

    paths : {
        text: "libs/text",
        jquery: "../test/tests-requireJS/libs/jquery", // jquery file to be picked from test folder
        underscore: "libs/underscore",
        backbone: "libs/backbone",
        router: "router",
        currentPackage: "models/currentPackage",
        // DesignObjects : "collections/wireCollection",
        DesignObjectsView: "views/designObjectsView",
        PanelDataCollection: "collections/panelDataCollection",
        SectionCollection: "collections/sectionCollection",
        textSearch: "filters/textSearch",
        systems: "collections/systems",
        fileDisplayHandler: "filehandlers/fileHandlers",
        FaultCodes: "collections/faultCodes",
        Harnesses: "collections/harnesses",
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
        XRefsCollectionItem: "collections/p/xrefsCollectionItem",
        AttributesCollection: "collections/p/attributesCollection",
        DiagramsCollection: "collections/p/diagrams/diagramsCollection",
        // DevicesCollection : "collections/p/relatedData/devicesCollection",
        // SignalsCollection : "collections/p/relatedData/signalsCollection",
        ReportsCollection: "collections/p/reports/reportsCollection",
        OptionsCollection: "collections/p/c/optionsCollection",
        ConfigurationsCollection: "collections/p/c/configurationsCollection",
        LocalConfigurations: "collections/p/c/localConfigurations",
        PopoverItem: "collections/p/popoverItemCollection",
        PopoverItemView: "views/p/popoverItemView",
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
        SmartViewComponent: "models/SmartViewComponent",
        SmartViewComponents: "collections/SmartViewComponents",
        ProjectsViewModel: "viewModels/ProjectsViewModel",
        PackagesViewModel: "viewModels/PackagesViewModel",
        UserSession: "models/UserSession",
        ComponentLoader: "ComponentLoader",
        TranslationUtils: "utilities/TranslationUtils",
        harnessLayouts: "collections/harnessLayouts",
        harnessLayoutBarHandler: "views/contentpanel/harnessLayoutBarHandler",
        Diagnostics: "collections/diagnostics",
        DiagnosticsView: "views/diagnosticsView",
        DiagnosticPanel: "views/contentpanel/diagnosticPanel",
        XRefsViewItem: "views/p/XRefsViewItem",
        LanguagesPanel: "views/p/settings/LanguagesPanel",
        ProjectsPanel: "views/p/projects/ProjectsPanel",
        SystemScopePanel: "views/p/settings/SystemScopePanel",
        SVGTransforms: "utilities/SVGTransforms",
        allColors: "models/p/colors/allcolors",
        recentColors: "models/p/colors/recentcolors",
        treeViewHelper: "views/treeViewHelper",
        PlainSearch: "filters/PlainSearch",
        baseTreeView : "custom/treeViews/s/baseTreeView",
        systemsAsTreeView : "custom/treeViews/s/systemsAsTreeView"
    },

    shim: {
        underscore: {
            exports: "_"
        },
        backbone: {
            deps: [
                "underscore",
                "jquery"
            ],
            exports: "Backbone"
        }
    },

    // dynamically load all test files
    deps: allTestFiles,

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
    },

    // we have to kickoff jasmine, as it is asynchronous
    callback: function() {
        require(["filehandlers/effectivitySetter"], function(effSetter) {
            setTimeout(function() {
                window.__karma__.start();
            }.bind(this), 1000);
        });
    }
});

var getPluginType = function () {
    return "text/html";
};

define("backbone",
        function () {
            "use strict";
            return Backbone;
        });

var LoadMask = function () {
    return {
        addLoadMask : function (svgContainer) {
        },

        LoadSVGMask : function (svgContainer) {
        },

        removeSVGMask : function () {
            //$('#LoadSVGMask').remove();
        },

        removeLoadMask : function () {
            //$('#LoadMask').remove();
        },

        addErrorMaskClass : function () {
            //$('#LoadMask').removeClass();
            //$('#LoadMask').addClass('LoadErrorMask');
        }
    };

}();

// var mentor = mentor || {};
// var p = mentor.publisher = mentor.publisher || {};
// mentor.publisher.events = p.events || {};
// var mentor = {
//     publisher: {
//         events: {
//
//         },
//         documentCategory: {
//
//         },
//         contentType: {
//
//         }
//     }
// }

mentor.publisher.project = {
    loadObjectData : function (systemId, objectId) {
        this.systemId = systemId;
        this.objectId = objectId;
        return {
            getAttr: jQuery.noop
        };
    },
    getId : function () {
        return "id1";
    },
    getObjectById : function (id) {
        return {
            systemId : "dummySystemId",
            optionExpression : "testOptionExpression",
            getObjectById : function (diagramId) {
                return {
                    diagramId : "dummyDiagramId",
                    getName : function () {
                        return "diagramName";
                    },
                    path : "diagramPath"
                };
            },
            getFirstDiagram : function () {
                return {
                    diagramId : "firstSystemDiagram",
                    getName : function () {
                        return "firstSystemDiagram";
                    },
                    path : "diagramPath"
                };
            }

        };
    },

    getObjects : function (type) {
        return [
            {id : "id1", mainText : "wire1", subText : "subText123", getToolTips : function () {
                    return [
                        {
                            getName : function () {
                                return "partNo";
                            },
                            getValue : function () {
                                return "partNo123";
                            }

                        }
                    ];
                }},
            {id : "id2", mainText : "wire123", subText : "subText", getToolTips : function () {
                    return [
                        {
                            getName : function () {
                                return "partNo";
                            },
                            getValue : function () {
                                return "partNo123";
                            }

                        }
                    ];
                }},
            {id : "id3", mainText : "wire3", subText : "subText", getToolTips : function () {
                    return [
                        {
                            getName : function () {
                                return "partNo";
                            },
                            getValue : function () {
                                return "partNo1234";
                            }

                        }
                    ];
                }},
            {id : "id4", mainText : "wire4", subText : "wire3", getToolTips : function () {
                    return [
                        {
                            getName : function () {
                                return "partNo";
                            },
                            getValue : function () {
                                return "partNo123";
                            }

                        }
                    ];
                }},
            {id : "id5", mainText : "wire5", subText : "subText", getToolTips : function () {
                    return [
                        {
                            getName : function () {
                                return "partNo1234";
                            },
                            getValue : function () {
                                return "partNo12345";
                            }

                        }
                    ];
                }}
        ];
    },
    getSystems : function () {
        return [
            {
                id : "systemId",
                idAttribute : "systemId",
                mainText : "systemName",
                subText : "systemSubTitle",
                getDiagrams : function () {
                    return [
                        {
                            id : "diagramID"
                        }
                    ];
                },
                getFirstDiagram : function () {
                    return { id : "firstDiagramId", path : "testPath", diagramId : "diagramID", type : "svg"};
                },
                getFolders : function()
                {
                    return "alpha\\bravo\\charlie\\systemName";
                }
            }
        ];
    },
    getInformation : function () {
        return [
            {
                id : "customFIle",
                mainText : "customFIle",
                path : "customFIle.svg"
            }
        ];
    },
    getByType : function () {
        return [
            {
                id : "customFIle",
                mainText : "customFIle",
                path : "customFIle.svg"
            },
            {
                id: "Test Pdf",
                mainText: "Test Pdf",
                path: "Top View.pdf"
            },
            {
                id: "UIDd23b1d-15167448921-2e9e9f7c017ee2a1645a236d182fb28c",
                mainText: "DTC047",
                subText: "Manual Internal Lighting Failure",
                type: "diagnostic"
            }
        ];
    },
    getData: function() {

    }
};
mentor.publisher.dataLoader = {
    loadProject : function (id) {
        return mentor.publisher.project;
    },
    getWindowTitleConfigData : function () {
        return {
            showPathFor2dViews : false
        }
    },
    getCustomPopoverSectionOrder: function () {

    },
    getObjectPropertyToUseForTitle: function noop(){}
};
mentor.publisher.config = mentor.publisher.config || {};
mentor.publisher.colors = mentor.publisher.colors || {};
mentor.publisher.serverConfig = mentor.publisher.serverConfig || {};
mentor.publisher.packectInfo = {
    packageId: '12da'
};
var TwoDS;

/*mentor.publisher.eventDispatcher = {
 dispatchEvent : function () {

 }
 };*/

var languageDictionary = '<Table> \
    <Lang>      \
    <D>RU</D>   \
    <D>CATALLAN</D>  \
    <D>DE</D>        \
    <D>EN</D>    \
</Lang>           \
    <Language>      \
        <D>RUSSIA</D>    \
        <D>CATALLAN</D>    \
        <D>GERMANY</D>      \
        <D>UK</D>     \
    </Language>         \
    <Country>         \
        <D>RUSSIA</D>      \
        <D>CATALLAN</D>     \
        <D>GERMANY</D>      \
        <D>English</D>     \
    </Country>          \
    <Nation>           \
        <D>RUSSIA</D>    \
        <D>CATL</D>     \
        <D>GERMAN</D>    \
        <D>English</D>  \
    </Nation>     \
    <E>          \
        <D>CIRD</D>  \
        <D>some value</D> \
        <D>value1</D> \
        <D>german</D> \
        <D>english</D> \
    </E> \
       <E>          \
        <D>shortDes</D>  \
        <D>shortDesRUSSIA</D> \
        <D>shortDesCATL</D> \
        <D>shortDesgerman</D> \
        <D>shortDesenglish</D> \
    </E> \
</Table>';

var listTemplateForTest = '<%if(items.length){ %><%if(true){ %> <div class="listPanel <%=title%>"><%}%><div class="titlebar"><span class="headingCount"><span class="headingCountNumber"><%=items.length%></span></span><span class="headerText"><%=title%></span></div><% if(false){ %>span class="mainText">Previous..</span></div><%}%><% _.each(items, function(item) {%><div class="listItem" data-id="<%=item.id%>" <% if(!expand){ %> style="display: none;" <%}%>><span class="mainText"><%=item.get("mainText")%></span><br><span class="subText"><%=item.get("subText")%></span></div><% });%><% if( false){ %><div class="listItem next_prevous_btn next" <% if(!expand){ %> style="display: none;"<%}%>><span class="mainText">Next..</span></div><%}%><% if(true){ %></div><%}%><%}%>';

var cnt = 0;

var renderer = new Backbone.Model.extend();

function createContext(stubs) {
    cnt++;
    var map = {};

    var i18n = stubs.i18n;
    stubs.i18n = {
        load : sinon.spy(function (name, req, onLoad) {
            onLoad(i18n);
        })
    };

    _.each(stubs, function (value, key) {
        var stubName = 'stub' + key + cnt;

        map[key] = stubName;

        define(stubName, function () {
            return value;
        });
    });

    return require.config({
        context : "context_" + cnt,
        paths : {
            text: "libs/text",
            jquery: "../test/tests-requireJS/libs/jquery",
            underscore: "libs/underscore",
            backbone: "libs/backbone",
            SectionCollection: "collections/sectionCollection",
            systems: "collections/systems",
            Harnesses: "collections/harnesses",
            PanelDataCollection: "collections/panelDataCollection",
            LocationViews: "collections/locationViews",
            // DesignObjects : "collections/wireCollection",
            fileDisplayHandler: "filehandlers/fileHandlers",
            AttributesCollection: "collections/p/attributesCollection",
            ConnectorFaceviewsCollection : "collections/p/connectorFaceviewCollection",
            ThreeDViewCollection : "collections/p/threeDViewCollection",
            TwoDLocationCollection : "collections/p/twoDLocationCollection",
            CustomDataCollection : "collections/p/customDataCollection",
            backbone : "libs/backbone",
            FaultCodes : "collections/faultCodes",
            XRefsCollection : "collections/p/xrefsCollection",
            DiagramsPopoverModel: "models/p/diagrams/diagramsPopoverModel",
            ReportsPopoverModel: "models/p/reports/reportsPopoverModel",
            ReportsCollection: "collections/p/reports/reportsCollection",
            ConfigurationsCollection : "collections/p/c/configurationsCollection",
            RelatedDataPopoverModel: "models/p/relatedData/relatedDataPopoverModel",
            OptionsCollection : "collections/p/c/optionsCollection",
            DiagramsCollection : "collections/p/diagrams/diagramsCollection",
            LanguagesCollection : "collections/p/languages/languagesCollection",
            PackagesCollection : "collections/p/packages/packagesCollection",
            PrintContentCollection : "collections/p/print/printContentCollection",
            PrintOptionsCollection : "collections/p/print/printOptionsCollection",
            PopoverItem : "collections/p/popoverItemCollection",
            PopoverModel : "models/p/popoverModel",
            PopoverFilterModel : "models/p/popoverFilterModel",
            LocationViewObjectPopoverModel : "models/p/locationViewObjectPopoverModel",
            DesignObjectPopoverModel : "models/p/designObjectPopoverModel",
            PopoverView : "views/p/popoverView",
            PopoverItemView : "views/p/popoverItemView",
            DragController: "views/dragController",
            harnessLayoutBarHandler: "views/contentpanel/harnessLayoutBarHandler",
            textSearch : "filters/textSearch",
            DesignObjectsView : "views/designObjectsView",
            ZoomToolBarModel : "models/zoomToolBarModel",
            componentRouter : "routers/componentRouter",
            PopoverView : "views/p/popoverView",
            XRefsViewItem : "views/p/XRefsViewItem",
            BasicPopoverView: "views/p/basicPopoverView",
            PopoverFilterModel: "models/p/popoverFilterModel",
            ListGroupView: "views/listGroupView",
            harnessLayouts: "collections/harnessLayouts",
            "models/HarnessLayout": "models/HarnessLayout",
            PackagesInSession: "models/PackagesInSession",
            Projects: "collections/Projects",
            Project: "models/Project",
            Package: "models/Package",
            ra3DModel: "models/ra3DModel",
            base3DModel: "models/base3DModel",
            Packages: "collections/Packages",
            SignalTracerModel : "models/signalTracerModel",
            ConfigurationsModel : "models/c/configurationsModel",
            Effectivity : "models/Effectivity",
            UserSession : "models/UserSession",
            recentColors : "models/p/colors/recentcolors",
            newDesignView : "illustrtor/views/newDesignView",
            ConfigurationsModel : "models/c/configurationsModel",
            XRefActiveConfigModel: "models/c/xrefActiveConfigModel",
            LocalConfigurations: "collections/p/c/localConfigurations",
            XRefsCollectionItem: "collections/p/xrefsCollectionItem",
            XRefsCollection: "collections/p/xrefsCollection",
            "routers/projectDataRouter": "routers/projectDataRouter",
            BaseConfigurationsModel: "models/c/baseConfigurationsModel",
            currentPackage: "models/currentPackage"
        },
        map : {
            "*" : map
        },
        baseUrl : "/base/s"
    });
}

requirejs.onError = function (err) {
    //console.log(err.requireType);
    if (err.requireType === 'timeout') {
        alert('module timeout:: ' + err.requireModules);
    }
    console.log('requirejs:: ' + err);
    //throw err;
};