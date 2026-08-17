/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, alert, requirejs*/
require.config({
    baseUrl : '/test/s',
    shim : {
        underscore : {
            exports : '_'
        },
        backbone : {
            deps : [
                'underscore',
                'jquery'
            ],
            exports : 'Backbone'
        }
    },
    paths : {
        text: "libs/text",
        jquery : 'libs/jquery',
        underscore : 'libs/underscore',
        backbone : 'libs/backbone',
        textSearch : '../s/filters/textSearch',
        currentPackage : '../s/models/currentPackage',
        Packages : '../s/collections/Packages',
        SectionCollection : "../s/collections/sectionCollection",
        systems : '../s/collections/systems',
        Harnesses : '../s/collections/harnesses',
        LocationViews : '../s/collections/locationViews',
        // DesignObjects : '../s/collections/wireCollection',
        fileDisplayHandler : '../s/filehandlers/fileHandlers',
        AttributesCollection : '../s/collections/popover/attributesCollection',
        ConnectorFaceviewsCollection : '../s/collections/popover/connectorFaceviewCollection',
        ThreeDViewCollection : '../s/collections/popover/threeDViewCollection',
        TwoDLocationCollection : '../s/collections/popover/twoDLocationCollection',
        CustomDataCollection : '../s/collections/popover/customDataCollection',
        backbone : 'libs/backbone',
        FaultCodes : '../s/collections/faultCodes',
        XRefsCollection : '../s/collections/popover/xrefsCollection',
        ConfigurationsCollection : '../s/collections/popover/configurations/configurationsCollection',
        OptionsCollection : '../s/collections/popover/configurations/optionsCollection',
        DiagramsCollection : '../s/collections/popover/diagrams/diagramsCollection',
        ReportsPopoverModel: '../s/models/popover/reports/reportsPopoverModel',
        LanguagesCollection : '../s/collections/popover/languages/languagesCollection',
        PackagesCollection : '../s/collections/popover/packages/packagesCollection',
        PrintContentCollection : '../s/collections/popover/print/printContentCollection',
        PrintOptionsCollection : '../s/collections/popover/print/printOptionsCollection',
        ReportsCollection: '../s/collections/popover/reports/reportsCollection',
        PopoverItem : '../s/collections/popover/popoverItemCollection',
        PopoverModel : '../s/models/popover/popoverModel',
        PopoverFilterModel : '../s/models/popover/popoverFilterModel',
        LocationViewObjectPopoverModel : '../s/models/popover/locationViewObjectPopoverModel',
        DesignObjectPopoverModel : '../s/models/popover/designObjectPopoverModel',
        PopoverItemView : '../s/views/popover/popoverItemView',
        textSearch : '../s/filters/textSearch',
        DesignObjectsView : '../s/views/designObjectsView',
        ZoomToolBarModel : '../s/models/zoomToolBarModel',
        componentRouter : '../s/routers/componentRouter',
        PopoverView : '../s/views/popover/popoverView',
        BasicPopoverView: '../s/views/popover/basicPopoverView',
        PopoverFilterModel: '../s/models/popover/popoverFilterModel',
        ListGroupView: '../s/views/listGroupView',
        ListView:  '../s/views/ListView',
        TranslationUtils: '../s/utilities/TranslationUtils',
        PackagesInSession: '../s/models/PackagesInSession',
        Projects: '../s/collections/Projects',
        Project: '../s/models/Project',
        Package: '../s/models/Package',
        "SummaryGridModel": '../s/illustrator/models/report',
        "harnessLayoutBarHandler": "../s/views/contentpanel/harnessLayoutBarHandler",
        baseTreeView : "../s/custom/treeViews/s/baseTreeView",
        systemsAsTreeView : "../s/custom/treeViews/s/systemsAsTreeView",
        harnessAsTreeView : "../s/custom/treeViews/s/harnessAsTreeView",
        exportPackage: "../s/illustrator/exportPackage",
        "LocalConfigurations": "../s/collections/p/c/localConfigurations",
        "xRefsBuilderView": "../s/views/p/c/xRefsBuilderView"
    }
    /*
    paths : {
        jquery : 'libs/jquery',
        underscore : 'libs/underscore',
        backbone : 'libs/backbone',
        textSearch : '../s/filters/textSearch',
        currentPackage : '../s/models/currentPackage',
        Packages : '../s/models/Package',
        FaultCodes : '../s/collections/faultCodes',
        sectionCollection: '../s/collections/sectionCollection',
        PackagesPopoverModel : '../s/models/popover/packages/packagesPopoverModel',
        PopoverFilterModel: '../s/models/popover/popoverFilterModel',
        popoverModel: '../s/models/popover/popoverModel',
        SignalTracerModel : '../s/models/signalTracerModel',
        PrintContentCollection:'../s/collections/popover/print/printContentCollection',
        ReportsPopoverModel: '../s/models/popover/reports/reportsPopoverModel',
        ReportsCollection: '../s/collections/popover/reports/reportsCollection',
        zoomToolBarModel: '../s/models/zoomToolBarModel',
        ListView:  '../s/views/ListView'
    }  */
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
    getData: function(){

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
mentor.publisher.packectInfo = {
    packageId: '12da'
}
var TwoDS

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
            SectionCollection : "../s/collections/sectionCollection",
            systems : "../s/collections/systems",
            Harnesses : "../s/collections/harnesses",
            LocationViews : "../s/collections/locationViews",
            // DesignObjects : "../s/collections/wireCollection",
            fileDisplayHandler : "../s/filehandlers/fileHandlers",
            AttributesCollection : "../s/collections/p/attributesCollection",
            ConnectorFaceviewsCollection : "../s/collections/p/connectorFaceviewCollection",
            ThreeDViewCollection : "../s/collections/p/threeDViewCollection",
            TwoDLocationCollection : "../s/collections/p/twoDLocationCollection",
            CustomDataCollection : "../s/collections/p/customDataCollection",
            backbone : 'libs/backbone',
            FaultCodes : "../s/collections/faultCodes",
            XRefsCollection : "../s/collections/p/xrefsCollection",
            ConfigurationsCollection : "../s/collections/p/configurations/configurationsCollection",
            OptionsCollection : "../s/collections/p/configurations/optionsCollection",
            DiagramsCollection : "../s/collections/p/diagrams/diagramsCollection",
            LanguagesCollection : "../s/collections/p/languages/languagesCollection",
            PackagesCollection : "../s/collections/p/packages/packagesCollection",
            PrintContentCollection : "../s/collections/p/print/printContentCollection",
            PrintOptionsCollection : "../s/collections/p/print/printOptionsCollection",
            PopoverItem : "../s/collections/p/popoverItemCollection",
            PopoverModel : "../s/models/p/popoverModel",
            PopoverFilterModel : "../s/models/p/popoverFilterModel",
            LocationViewObjectPopoverModel : "../s/models/p/locationViewObjectPopoverModel",
            DesignObjectPopoverModel : "../s/models/p/designObjectPopoverModel",
            PopoverItemView : "../s/views/p/popoverItemView",
            textSearch : "../s/filters/textSearch",
            DesignObjectsView : "../s/views/designObjectsView",
            ZoomToolBarModel : "../s/models/zoomToolBarModel",
            componentRouter : "../s/routers/componentRouter",
            PopoverView : "../s/views/p/popoverView",
            BasicPopoverView: "../s/views/p/basicPopoverView",
            PopoverFilterModel: "../s/models/p/popoverFilterModel",
            ListGroupView: "../s/views/listGroupView",
            harnessLayouts: "../s/collections/harnessLayouts",
            "models/HarnessLayout": "../s/models/HarnessLayout",
            PackagesInSession: '../s/models/PackagesInSession',
            Projects: '../s/collections/Projects',
            Project: '../s/models/Project',
            Package: '../s/models/Package',
            SignalTracerModel : '../s/models/signalTracerModel',
            ConfigurationsModel : '../s/models/c/configurationsModel',
            Effectivity : '../s/models/Effectivity',
            UserSession : '../s/models/UserSession',
            recentColors : '../s/models/p/colors/recentcolors',
            newDesignView : '../s/illustrtor/views/newDesignView',
            ConfigurationsModel : '../s/models/c/configurationsModel',
            LocalConfigurations: "../s/collections/p/c/localConfigurations",
            "routers/projectDataRouter": "../s/routers/projectDataRouter",
            LocalConfigurations: "../s/collections/p/c/localConfigurations",
            ra3DModel: "../s/models/ra3DModel",
            base3DModel: "../s/models/base3DModel"
        },
        map : {
            "*" : map
        },
        baseUrl : '/test/s'
    });
}

requirejs.onError = function (err) {
    //console.log(err.requireType);
    if (err.requireType === 'timeout') {
        alert('modules: ' + err.requireModules);
    }

    //throw err;
};
