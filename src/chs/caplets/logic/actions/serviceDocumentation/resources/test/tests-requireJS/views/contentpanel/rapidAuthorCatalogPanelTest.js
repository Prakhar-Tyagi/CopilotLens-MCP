/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
(function () {
    var context, stubs, Model = Backbone.Model.extend(), View = Backbone.View.extend();

    // var fakeFileDisplayHandler = {
    //     display: function (content) {
    //         this.fileHandles[content.type](content);
    //     },
    //     addFileHandler: function (type, fileHandler) {
    //         this.fileHandles = this.fileHandles || {};
    //         this.fileHandles[type] = fileHandler;
    //     }
    // };

    var fakeCortona3DSoloAppIpcInteractivity = {
        getRowByItem: function (itemId) {
            return "name";
        },
        getObjectsNamesByRow: function (row) {
            return ["object1", "object2"];
        },
        getRowByObjectName: function (name) {
            return 1;
        },
        getIndexByRow: function (row) {
        }
    };

    var fakeCortona3DSoloAppIpcDpl = {
        selectRow(index, state)
        {
        }
    };

    var fakeCortona3DSoloApp = {
        setSelectedObjects: function (objects, state) {
        },
        fitObjectsInView: function (handles, state) {
        },
        getObjectWithName: function (name) {
        },
        ipc: {
            dpl: fakeCortona3DSoloAppIpcDpl,
            interactivity: fakeCortona3DSoloAppIpcInteractivity,
            setCurrentSheet: function (sheetId) {
            },
            selectItem: function (index) {
            }
        }
    };

    var fakeCortona3DSolo = {
        dispatch:function (name,index){
        },
        app: fakeCortona3DSoloApp
    };

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        currentPackage: new Model(),
        "DiagramsPopoverModel": new Model(),
        "RelatedDataPopoverModel": new Model(),
        "ReportsPopoverModel": new Model(),
        SignalTracerModel: new Model(),
        ListGroupView: new View(),
        "TranslationUtils": {
            translateHTMLContent: function () {
            }
        },
        "internalLinkHandler": {
            addMouseEventListener: function () {
            }
        },
        "views/navigationPanelView": new View(),
        // fileDisplayHandler: fakeFileDisplayHandler,
    };

    stubs.currentPackage.set({id: "packageId"});
    context = createContext(stubs);

    stubs.SignalTracerModel.checkRendererAvailablility = function () {
    };

    stubs.SignalTracerModel.rendererLicenceAvaialable = function () {
    };

    context(["views/contentpanel/rapidAuthorCatalogPanel", "views/contentpanel/toolbar/systemToolBar",
                "views/contentpanel/toolbar/generalButtons", "ra3DModel"],
            function (rapidAuthorCatalogPanel, systemToolBar, generalButtons, ra3DModel) {

                "use strict";

                describe("rapidAuthorCatalogPanelTest",
                    function () {
                        var toolbarSet, viewRendered, org_render, removedEventHandlers, content, appSpy,
                                appIpcDplSpy, appIpcInteractivitySpy, org_removeEventHandlers;
                        org_removeEventHandlers=rapidAuthorCatalogPanel.removeEventHandlers;
                        var initializeRAPanel = function () {
                            removedEventHandlers = false;
                            rapidAuthorCatalogPanel.removeEventHandlers = function () {
                                removedEventHandlers = true;
                            }

                            var Content = Backbone.Model.extend();
                            content = new Content({
                                path: "",
                                type: "RA",
                                mainText: "Test",
                                objectId: "123",
                                id: "abc",
                                selectedItems: [],
                                modelName: "",
                                modelPath: "",
                                modelUrl: ""
                            });
                            rapidAuthorCatalogPanel.model = content;
                        };

                        var spies = []
                        var clock = null;

                        beforeEach(function () {
                            viewRendered = false;
                            generalButtons.templateHTML = "";
                            systemToolBar.templateHTML = "";
                            var Model = Backbone.Model.extend({});
                            $('body').html('');
                            $('body').append($('<div id="RA3DViewLoadArea"></div>'));
                            rapidAuthorCatalogPanel.currentPackage = stubs.currentPackage;
                            rapidAuthorCatalogPanel.selectedSystem = new Model();
                            rapidAuthorCatalogPanel.templateHTML = "<%=path%> <%=title%> <%=type%> <%=objectId%>";
                            rapidAuthorCatalogPanel.container = "#RA3DViewLoadArea";
                            window.LoadMask = {
                                removeLoadMask: function () {
                                },
                                addLoadMask: function () {
                                },
                                LoadSVGMask: function () {

                                }
                            };
                            rapidAuthorCatalogPanel.getPagePathname = function () {
                                return "/";
                            }

                            org_render = rapidAuthorCatalogPanel.render;

                            spies = [];
                            clock = sinon.useFakeTimers();
                        });
                        afterEach(function () {
                            rapidAuthorCatalogPanel.render = org_render;
                            $("#RA3DViewLoadArea").remove();
                            //restore all spies
                            for (var i = 0; i < spies.length; i++) {
                                spies[i].restore();
                            }
                            clock.restore();
                            sinon.restore();
                        });

                        //basic setup and render tests
                        it("should be able to load rapidAuthorCatalogPanel module", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.render = function () {
                                viewRendered = true;
                            };
                            rapidAuthorCatalogPanel.initialize();
                            expect(rapidAuthorCatalogPanel).toBeDefined();
                        });

                        //url generation tests
                        it("model url is constructed from relative internal path correctly", function () {
                            initializeRAPanel();
                            expect(content).toBeDefined();
                            content.set("path", "data\\ad3d\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            rapidAuthorCatalogPanel.getPagePathname = function () {
                                return "";
                            }

                            rapidAuthorCatalogPanel.initialize();

                            rapidAuthorCatalogPanel.openObjectThreeD(content);
                            expect(rapidAuthorCatalogPanel.getPath()).toBe(
                                    "data\\ad3d\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelName()).toBe("Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelPath()).toBe(
                                    "data/ad3d/Resources/RapidAuthor/ipc/Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelUrl()).toBe(
                                    "/data/ad3d/Resources/RapidAuthor/ipc/Test_Model/Test_Model.interactivity.xml?packageId=12da");
                        });

                        it("model url is constructed from absolute internal path correctly", function () {
                            initializeRAPanel();
                            expect(content).toBeDefined();
                            content.set("path", "\\data\\ad3d\\Resources\\RapidAuthor\\ipc\\Test_Model");

                            rapidAuthorCatalogPanel.initialize();

                            rapidAuthorCatalogPanel.openObjectThreeD(content);

                            expect(rapidAuthorCatalogPanel.getPath()).toBe(
                                    "\\data\\ad3d\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelName()).toBe("Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelPath()).toBe(
                                    "data/ad3d/Resources/RapidAuthor/ipc/Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelUrl()).toBe(
                                    "/data/ad3d/Resources/RapidAuthor/ipc/Test_Model/Test_Model.interactivity.xml?packageId=12da");
                        });

                        it("model url is constructed from popout absolute internal path correctly", function () {
                            initializeRAPanel();
                            expect(content).toBeDefined();

                            rapidAuthorCatalogPanel.getPagePathname = function () {
                                return "/popout.html"
                            }
                            content.set("path", "\\data\\ad3d\\Resources\\RapidAuthor\\ipc\\Test_Model");

                            rapidAuthorCatalogPanel.initialize();

                            rapidAuthorCatalogPanel.openObjectThreeD(content);
                            expect(rapidAuthorCatalogPanel.getPath()).toBe(
                                    "\\data\\ad3d\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelName()).toBe("Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelPath()).toBe(
                                    "data/ad3d/Resources/RapidAuthor/ipc/Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelUrl()).toBe(
                                    "/data/ad3d/Resources/RapidAuthor/ipc/Test_Model/Test_Model.interactivity.xml?packageId=12da");
                        });

                        it("model url is constructed from diagram view absolute internal path correctly", function () {
                            initializeRAPanel();
                            expect(content).toBeDefined();

                            rapidAuthorCatalogPanel.getPagePathname = function () {
                                return "/index1.html"
                            }
                            content.set("path", "\\data\\ad3d\\Resources\\RapidAuthor\\ipc\\Test_Model");

                            rapidAuthorCatalogPanel.initialize();

                            rapidAuthorCatalogPanel.openObjectThreeD(content);
                            expect(rapidAuthorCatalogPanel.getPath()).toBe(
                                    "\\data\\ad3d\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelPath()).toBe(
                                    "data/ad3d/Resources/RapidAuthor/ipc/Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelUrl()).toBe(
                                    "/data/ad3d/Resources/RapidAuthor/ipc/Test_Model/Test_Model.interactivity.xml?packageId=12da");
                        });

                        it("model url is constructed from relative external path correctly", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.getPagePathname = function () {
                                return "SmartClient/"
                            }
                            content.set("path", "\\SmartClient\\RapidAuthor\\ipc\\Test_Model");
                            rapidAuthorCatalogPanel.initialize();
                            expect(content).toBeDefined();
                            rapidAuthorCatalogPanel.openObjectThreeD(content);
                            expect(rapidAuthorCatalogPanel.getPath()).toBe("\\SmartClient\\RapidAuthor\\ipc\\Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelName()).toBe("Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelPath()).toBe("RapidAuthor/ipc/Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelUrl()).toBe(
                                    "/SmartClient/RapidAuthor/ipc/Test_Model/Test_Model.interactivity.xml?packageId=12da");
                        });

                        it("model url is constructed from absolute external path correctly", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.getPagePathname = function () {
                                return "/SmartClient/"
                            }
                            content.set("path", "data\\3d5d\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            rapidAuthorCatalogPanel.initialize();
                            expect(content).toBeDefined();
                            rapidAuthorCatalogPanel.openObjectThreeD(content);
                            expect(rapidAuthorCatalogPanel.getPath()).toBe(
                                    "data\\3d5d\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelName()).toBe("Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelPath()).toBe(
                                    "data/3d5d/Resources/RapidAuthor/ipc/Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelUrl()).toBe(
                                    "/SmartClient/data/3d5d/Resources/RapidAuthor/ipc/Test_Model/Test_Model.interactivity.xml?packageId=12da");
                        });

                        it("model url is constructed from popout absolute external path correctly", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.getPagePathname = function () {
                                return "/SmartClient/popout.html"
                            }
                            content.set("path", "\\SmartClient\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            rapidAuthorCatalogPanel.initialize();
                            expect(content).toBeDefined();
                            rapidAuthorCatalogPanel.openObjectThreeD(content);
                            expect(rapidAuthorCatalogPanel.getPath()).toBe(
                                    "\\SmartClient\\Resources\\RapidAuthor\\ipc\\Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelName()).toBe("Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelPath()).toBe("Resources/RapidAuthor/ipc/Test_Model");
                            expect(rapidAuthorCatalogPanel.getModelUrl()).toBe(
                                    "/SmartClient/Resources/RapidAuthor/ipc/Test_Model/Test_Model.interactivity.xml?packageId=12da");
                        });

                        it("should be able to render view", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();
                            rapidAuthorCatalogPanel.openObjectThreeD(content);
                            expect($('body').html()).toBe(
                                    '<div id="RA3DViewLoadArea"><div class="toolbar background"></div>' +
                                    rapidAuthorCatalogPanel.getModelUrl() +
                                    ' Test RA 123</div>');
                        });

                        it("should close RA panel if user opens different project", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();
                            expect(rapidAuthorCatalogPanel.model).toBeDefined();

                            rapidAuthorCatalogPanel.currentPackage.set("projectId", "blah");
                            expect(removedEventHandlers).toBe(true);
                            expect(rapidAuthorCatalogPanel.model).toBeUndefined();

                        });

                        it("should remove listeners if user loads a different publication", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();
                            expect(rapidAuthorCatalogPanel.model).toBeDefined();

                            var Content = Backbone.Model.extend();
                            content = new Content({
                                path: "",
                                type: "RA",
                                mainText: "Test",
                                objectId: "123",
                                id: "abc",
                                selectedItems: [],
                                modelName: "",
                                modelPath: "",
                                modelUrl: ""
                            });
                            //reload
                            rapidAuthorCatalogPanel.openObjectThreeD(content);
                            expect(removedEventHandlers).toBe(true);
                            expect(rapidAuthorCatalogPanel.model).toBe(content);

                        });


                        it("should select items when load requested from another view", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();

                            fakeCortona3DSoloAppIpcInteractivity.getRowByObjectName = function (name) {
                                return "row1";
                            };
                            fakeCortona3DSoloAppIpcInteractivity.getIndexByRow = function (row) {
                                return "1";
                            }

                            var oldFunc = ra3DModel.getItemIdMapsForModel();
                            ra3DModel.getItemIdMapsForModel = function(path) {
                                return {
                                    getItemMappings: function() {
                                        return {
                                           findWhere: function() {
                                               return null;
                                           }
                                        };
                                    }
                                };
                            }

                            mentor.publisher.solo = fakeCortona3DSolo;
                            var dplSelectItemSpy = sinon.spy(fakeCortona3DSoloApp.ipc, "selectItem");
                            spies.push(dplSelectItemSpy);

                            rapidAuthorCatalogPanel.onLoad({
                                objectId: "object_name",
                                get: function (name) {
                                    return this.objectId;
                                }
                            });

                            clock.tick(1000);

                            expect(dplSelectItemSpy.calledOnce).toBe(true);
                            expect(dplSelectItemSpy.calledWith("1")).toBe(true);

                            expect(rapidAuthorCatalogPanel.getSelectedItems()).toContain("object_name");

                            ra3DModel.getItemIdMapsForModel = oldFunc;
                        });

                        it("should not request highlight in other windows when item with no links selected",
                                function () {
                                    initializeRAPanel();
                                    rapidAuthorCatalogPanel.initialize();

                                    var modelItem = {
                                        metadata: {
                                            ITEM: "123"
                                        },
                                        part: {
                                            metadata: {
                                                DFP: "some name"
                                            }
                                        }
                                    };

                                    var oldFunc = ra3DModel.getSystemDataForRapidAuthorItem;
                                    ra3DModel.getSystemDataForRapidAuthorItem = function (itemId, modelPath) {
                                        return [];
                                    }
                                    var eventCount = mentor.publisher.eventDispatcher.dispatchEvent.callCount;
                                    rapidAuthorCatalogPanel.doHoverItem(modelItem);

                                    // no events triggered
                                    expect(mentor.publisher.eventDispatcher.dispatchEvent.callCount).toEqual(
                                            eventCount);

                                    ra3DModel.getSystemDataForRapidAuthorItem = oldFunc;

                                });

                        it("should display attributes in popover when selected item has links", function () {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();

                            var spy = sinon.spy(rapidAuthorCatalogPanel, "displayRapidAuthorAttribs");
                            spies.push(spy);

                            var modelItem = {
                                metadata: {
                                    ITEM: "123"
                                },
                                part: {
                                    metadata: {
                                        DFP: "some name"
                                    }
                                }
                            };

                            var oldFunc = ra3DModel.getSystemDataForRapidAuthorItem;
                            ra3DModel.getSystemDataForRapidAuthorItem = function (itemId, modelPath) {
                                return [{
                                    objectId: "UID-123",
                                    systemId: "system1",
                                    connUID: "ConnId"
                                }, {
                                    objectId: "UID-abc",
                                    systemId: "system2",
                                    connUID: "ConnId"
                                }];
                            }

                            $('body').append($('<div id="splitter2"><div id="RA3DViewLoadArea"><object></object></div></div>'));

                            rapidAuthorCatalogPanel.selectMatchedRow(0, 0, modelItem);

                            expect(spy.calledOnce).toBe(true);
                            expect(spy.args[0]).toEqual([0, 0, { }, "some name",'row']);

                            ra3DModel.getSystemDataForRapidAuthorItem = oldFunc;
                        });

                        it("should zoom if Capital Object mapped to RA Model", function() {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();

                            mentor.publisher.solo = fakeCortona3DSolo;

                            var appFitInViewSpy = sinon.spy(fakeCortona3DSoloApp, "fitObjectsInView");
                            spies.push(appFitInViewSpy);

                            var oldFunc = ra3DModel.getItemNamesForSystemId;
                            ra3DModel.getItemNamesForSystemId = function (uid, path){
                                return ["abc"];
                            }
                            var oldFunc2 = ra3DModel.getItemIdMapsForModel;
                            ra3DModel.getItemIdMapsForModel = function(path) {
                                return {
                                    getItemMappings: function() {
                                        return {
                                            findWhere: function() {
                                                return null;
                                            }
                                        };
                                    }
                                };
                            }

                            var oldAppFunc = fakeCortona3DSoloApp.getObjectWithName;
                            fakeCortona3DSoloApp.getObjectWithName = function(name) {
                                return "1";
                            }

                            rapidAuthorCatalogPanel.zoomObjects("UID-123");

                            clock.tick(1000);

                            expect(appFitInViewSpy.called).toBe(true);

                            ra3DModel.getItemNamesForSystemId = oldFunc;
                            ra3DModel.getItemIdMapsForModel = oldFunc2;
                            fakeCortona3DSoloApp.getObjectWithName = oldAppFunc;
                        });

                        it("should open sheet for given item if set", function() {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();
                            ra3DModel.sheetViews = [{id: "view_1", items:[0,3]}, {id:"view_2", items:[2,4]}];

                            //C-1234 is row 2
                            fakeCortona3DSoloAppIpcInteractivity.getRowByObjectName = function() {
                                return 2;
                            }

                            expect(rapidAuthorCatalogPanel.getSheetForItemName("C-1234")).toEqual("view_2");

                        });

                        it("should open default sheet for given item if not set", function() {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();
                            ra3DModel.sheetViews = [{id: "view_1", items:[0,3]}, {id:"view_2", items:[2,4]}];

                            //C-NO-SHEET is row 9
                            fakeCortona3DSoloAppIpcInteractivity.getRowByObjectName = function() {
                                return 9;
                            }

                            expect(rapidAuthorCatalogPanel.getSheetForItemName("C-NO-SHEET")).toEqual("view_1");
                        });

                        it("should filter items for other models", function() {
                            initializeRAPanel();
                            rapidAuthorCatalogPanel.initialize();

                            var oldFunc = fakeCortona3DSoloApp.getObjectWithName;
                            fakeCortona3DSoloApp.getObjectWithName = function (name) {
                                switch (name) {
                                    case "good":
                                        return 1;
                                    case "bad":
                                        return 0;
                                }
                            };

                            var oldFunc2 = rapidAuthorCatalogPanel.getSheetForItemName;
                            rapidAuthorCatalogPanel.getSheetForItemName = function (name) {
                                return "view_1";
                            }

                            var sheetId;

                            expect(rapidAuthorCatalogPanel.getValidHandles(["good", "bad"], sheetId)).toEqual({sheetId:"view_1", handles:[1]});

                            fakeCortona3DSoloApp.getObjectWithName = oldFunc;
                            rapidAuthorCatalogPanel.getSheetForItemName = oldFunc2;
                        });

                        it("should filter add selected items", function() {
                            rapidAuthorCatalogPanel.addSelectedItem("item1");
                            expect(rapidAuthorCatalogPanel.model.get("selectedItems")).toEqual(["item1"]);
                            rapidAuthorCatalogPanel.model.set("selectedItems", ["item0"]);
                            rapidAuthorCatalogPanel.addSelectedItem("item1");
                            expect(rapidAuthorCatalogPanel.model.get("selectedItems")).toEqual(["item0", "item1"]);
                        });

                        it("should get page pathname", function() {
                            expect(rapidAuthorCatalogPanel.getPagePathname()).toBe('/');
                        });

                        xit("should remove event handlers", function() {
                            rapidAuthorCatalogPanel.undelegateEvents = function () {};
                            p.solo.removeAllListeners = function () {};
                            spyOn(rapidAuthorCatalogPanel, "undelegateEvents");

                            rapidAuthorCatalogPanel.removeEventHandlers=org_removeEventHandlers;
                            rapidAuthorCatalogPanel.removeEventHandlers();
                            expect(rapidAuthorCatalogPanel.undelegateEvents).toHaveBeenCalled();
                        });

                        it("should load sheet Views", function() {
                            var sheets = ["sheet1", "sheet2"]
                            rapidAuthorCatalogPanel.loadSheetViews(sheets);
                            expect(ra3DModel.sheetViews).toBe(sheets);
                        });

                        xit("should select matched Item", function() {
                            var dx=0, dy=0, itemName="testItem";
                            ra3DModel.getSystemDataForRapidAuthorItemInstance = function (itemName, path) {return ['item1']};
                            spyOn(ra3DModel, "getSystemDataForRapidAuthorItemInstance");
                            rapidAuthorCatalogPanel.displayRapidAuthorAttribs=function(Dx, Dy, objDataArray, ItemName){};
                            window.crossHighlightHandler.crossHighlightInRapidAuthorViews=function(arr, doc, bool){};

                            rapidAuthorCatalogPanel.selectMatchedItem(dx, dy, itemName);

                            expect(ra3DModel.getSystemDataForRapidAuthorItemInstance).toHaveBeenCalled();
                        });

                        describe("IPC Table", function() {
                            it("should enable 3D View Selection actions when row selected", function() {
                                initializeRAPanel();
                                rapidAuthorCatalogPanel.initialize();

                                var enabled = false;
                                var oldFunc = rapidAuthorCatalogPanel.enable3DViewSelectionToolbar;
                                rapidAuthorCatalogPanel.enable3DViewSelectionToolbar = function() {
                                    enabled = true;
                                }
                                var oldFunc2 = ra3DModel.getItemIdMapsForModel;
                                ra3DModel.getItemIdMapsForModel = function(path) {
                                    return {
                                        getItemMappings: function() {
                                            return {
                                                findWhere: function() {
                                                    return null;
                                                }
                                            };
                                        }
                                    };
                                }

                                rapidAuthorCatalogPanel.selectInIPC("C-1234");

                                expect(enabled).toBeTruthy();

                                rapidAuthorCatalogPanel.enable3DViewSelectionToolbar = oldFunc;
                                ra3DModel.getItemIdMapsForModel = oldFunc2;
                            });

                            it("should not send X-highlight event if other row already selected", function() {
                                initializeRAPanel();
                                rapidAuthorCatalogPanel.initialize();
                                rapidAuthorCatalogPanel.setSelectedItems(["Item-1"]);

                                var eventCount = mentor.publisher.eventDispatcher.dispatchEvent.callCount;

                                rapidAuthorCatalogPanel.doHoverItem("item-2");

                                clock.tick(1000);

                                expect(mentor.publisher.eventDispatcher.dispatchEvent.callCount).toBe(eventCount);
                            });

                            //highlight from user interaction with panel
                            it("should select row with valid RA Model Item", function () {
                                initializeRAPanel();
                                rapidAuthorCatalogPanel.initialize();

                                mentor.publisher.solo = fakeCortona3DSolo;
                                appSpy = sinon.spy(fakeCortona3DSoloApp, "setSelectedObjects");
                                spies.push(appSpy);

                                var oldFunc = ra3DModel.getSystemDataForRapidAuthorItem;
                                ra3DModel.getSystemDataForRapidAuthorItem = function (itemId, modelPath) {
                                    return [{
                                        objectId: "UID-123",
                                        systemId: "system1"
                                    }, {
                                        objectId: "UID-abc",
                                        systemId: "system2"
                                    }];
                                }

                                var modelSpy = sinon.spy(ra3DModel, "getSystemDataForRapidAuthorItem");
                                spies.push(modelSpy);

                                var modelItem = {
                                    metadata: {
                                        ITEM: "1234"
                                    },
                                    part: {
                                        metadata: [
                                            {
                                                DFP: "Title"
                                            }
                                        ]
                                    },
                                    objectNames: ["object1", "object2"]
                                };

                                var oldFunc2 = rapidAuthorCatalogPanel.displayRapidAuthorAttribs;
                                var displayAttributes = false;
                                rapidAuthorCatalogPanel.displayRapidAuthorAttribs = function (dx, dy, objDataArray, partName) {
                                    displayAttributes = true;
                                }

                                var eventCount = mentor.publisher.eventDispatcher.dispatchEvent.callCount;

                                rapidAuthorCatalogPanel.selectMatchedRow(0, 0, modelItem);

                                expect(displayAttributes).toBe(true);
                                expect(rapidAuthorCatalogPanel.getSelectedItems()).toEqual(["object1", "object2"]);
                                expect(appSpy.withArgs([], true).calledOnce);
                                expect(appSpy.withArgs(["object1", "object2"], true).calledOnce);

                                expect(modelSpy.calledWith("1234", "RapidAuthor/ipc/Test_Model"));

                                clock.tick(1000);

                                expect(mentor.publisher.eventDispatcher.dispatchEvent.callCount - eventCount).toBe(2);

                                ra3DModel.getSystemDataForRapidAuthorItem = oldFunc;
                                rapidAuthorCatalogPanel.displayRapidAuthorAttribs = oldFunc2

                            });

                            it("should not select rows if modelItem invalid", function () {
                                initializeRAPanel();
                                rapidAuthorCatalogPanel.initialize();

                                mentor.publisher.solo = fakeCortona3DSolo;
                                appSpy = sinon.spy(fakeCortona3DSoloApp, "setSelectedObjects");
                                spies.push(appSpy);

                                var modelSpy = sinon.spy(ra3DModel, "getSystemDataForRapidAuthorItem");
                                spies.push(modelSpy);

                                rapidAuthorCatalogPanel.selectMatchedItem(sinon.undefined);
                                expect(rapidAuthorCatalogPanel.selectedItems).toBeUndefined();
                                expect(appSpy.notCalled).toBe(true);
                                expect(modelSpy.notCalled).toBe(true);

                                expect(mentor.publisher.eventDispatcher.dispatchEvent.args).toContain(
                                    [mentor.publisher.events.CLOSE_POPOVER]);

                            });
                        });

                        describe("3D View", function () {
                            it('should zoom other 3D views when Fit To Selection actioned', function () {
                                initializeRAPanel();
                                rapidAuthorCatalogPanel.initialize();

                                var oldFunc = ra3DModel.getSystemDataForRapidAuthorItem;
                                ra3DModel.getSystemDataForRapidAuthorItem = function (itemId, modelPath) {
                                    return [{
                                        objectId: "UID-123",
                                        systemId: "system1"
                                    }, {
                                        objectId: "UID-abc",
                                        systemId: "system2"
                                    }];
                                }

                                var modelSpy = sinon.spy(ra3DModel, "getSystemDataForRapidAuthorItem");
                                spies.push(modelSpy);

                                var zoomSpy = sinon.spy(window.crossHighlightHandler, "zoomItemInRapidAuthorViews");

                                rapidAuthorCatalogPanel.zoomRelatedDiagrams("C-1234");

                                clock.tick(2000);

                                expect(modelSpy.called).toBe(true);
                                expect(zoomSpy.called).toBe(true);

                                ra3DModel.getSystemDataForRapidAuthorItem = oldFunc;

                            });

                            it('should zoom to fit the rapid author view', function () {

                                initializeRAPanel();
                                rapidAuthorCatalogPanel.initialize();
                                mentor.publisher.solo = fakeCortona3DSolo;

                                var objectSelectedSpy = sinon.spy(fakeCortona3DSoloApp, "setSelectedObjects");
                                var fitInViewSpy = sinon.spy(fakeCortona3DSoloApp, "fitObjectsInView");
                                spies.push(objectSelectedSpy);
                                spies.push(fitInViewSpy);

                                rapidAuthorCatalogPanel.zoomTo3dModel(['abc']);

                                clock.tick(2000);
                                expect(objectSelectedSpy.called).toBe(true);
                                expect(fitInViewSpy.called).toBe(true);

                            })
                        });
                    }
                );

                describe("ra3DModel",
                    function () {
                        beforeEach(function () {
                        });

                        afterEach(function () {
                        });

                        //basic setup and render tests
                        it("should be able to load system paths", function () {
                            ra3DModel.loadSystemPaths("testModelPath", "testUid", ["testSystemPath1", "testSystemPath2"]);
                        });

                        it("should be able to do ajax", function () {
                            spyOn($, "ajax");
                            ra3DModel.doAjax();
                            expect($.ajax).toHaveBeenCalled();
                        });

                        it("should be able to do get system data for rapid author item instance", function () {
                            var origGetItemIdMapsForModel = ra3DModel.getItemIdMapsForModel,
                                origfilterData = ra3DModel.filterData;

                            ra3DModel.getItemIdMapsForModel = function (modelPath) {
                                return {
                                    getItemMappings: function () {
                                        return {
                                            findWhere: function (obj) {
                                                return {
                                                    itemId: "testItemName1",
                                                    itemContent: "testContent1",
                                                    getMappings: function () {
                                                        return ["item1", "item2"];
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            };
                            ra3DModel.filterData = function () {
                                return ["item1", "item2"];
                            };

                            expect(
                                ra3DModel.getSystemDataForRapidAuthorItemInstance("testItemName1", "testModelPath")
                            ).toEqual([ 'item1', 'item2' ]);

                            ra3DModel.getItemIdMapsForModel = origGetItemIdMapsForModel;
                            ra3DModel.filterData = origfilterData;
                        });

                        it("should be able to do get system data for rapid author item instance", function () {
                            var origGetItemIdMapsForModel = ra3DModel.getItemIdMapsForModel,
                                origfilterData = ra3DModel.filterData;

                            ra3DModel.getItemIdMapsForModel = function (modelPath) {
                                return {
                                    getItemMappings: function () {
                                        return {
                                            findWhere: function (obj) {
                                                return {
                                                    itemId: "testItemName1",
                                                    itemContent: "testContent1",
                                                    getMappings: function () {
                                                        return ["item1", "item2"];
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            };
                            ra3DModel.filterData = function () {
                                return ["item1", "item2"];
                            };

                            expect(
                                ra3DModel.getSystemDataForRapidAuthorItem({name: "testItem", objectNames: "testObjectName"}, "testModelPath")
                            ).toEqual([ 'item1', 'item2', 'item1', 'item2', 'item1', 'item2', 'item1', 'item2', 'item1', 'item2', 'item1', 'item2', 'item1', 'item2', 'item1', 'item2', 'item1', 'item2', 'item1', 'item2' ]);

                            ra3DModel.getItemIdMapsForModel = origGetItemIdMapsForModel;
                            ra3DModel.filterData = origfilterData;
                        });

                        it("should be able to do get system item names for system ID", function () {
                            var origGetItemIdMapsForModel = ra3DModel.getItemIdMapsForModel,
                                origfilterData = ra3DModel.filterData;

                            ra3DModel.getItemIdMapsForModel = function (modelPath) {
                                return {
                                    getItemMappings: function () {
                                        return {
                                            findWhere: function (obj) {
                                                return {
                                                    itemId: "testItemName1",
                                                    itemContent: "testContent1",
                                                    getMappings: function () {
                                                        return ["item1", "item2"];
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            };
                            ra3DModel.filterData = function () {
                                return ["item1", "item2"];
                            };

                            expect(
                                ra3DModel.getItemNamesForSystemId("testConnID", "testModelPath")
                            ).toEqual([]);

                            ra3DModel.getItemIdMapsForModel = origGetItemIdMapsForModel;
                            ra3DModel.filterData = origfilterData;
                        });

                        it("should be able to get system item names for system ID", function () {
                            var origGetItemIdMapsForModel = ra3DModel.getItemIdMapsForModel;

                            ra3DModel.getItemIdMapsForModel = function (modelPath) {
                                return {
                                    getItemMappings: function () {
                                        return {
                                            models: [
                                                {
                                                    itemId: "testItemName1",
                                                    itemContent: "testContent1",
                                                    getMappings: function () {
                                                        return {
                                                            systems: [
                                                                {
                                                                    "connUID": "testConnID1",
                                                                },
                                                                {
                                                                    "connUID": "testConnID2",
                                                                }
                                                            ]
                                                        }
                                                    }
                                                }
                                            ]
                                        };
                                    }
                                };
                            };

                            expect(
                                ra3DModel.getItemNamesForSystemId("testConnID", "testModelPath")
                            ).toEqual([]);

                            ra3DModel.getItemIdMapsForModel = origGetItemIdMapsForModel;
                        });
                    }
                );
            }
            , function (err) {
                describe("rapidAuthor Panel Test Failed", function () {
                    it("should load the test and dependencies", function () {
                        console.log(err.message + "::\n" + err.stack);
                        console.dir(err);
                        expect(err).toBeUndefined();
                    });
                });
            }
    );
})();
