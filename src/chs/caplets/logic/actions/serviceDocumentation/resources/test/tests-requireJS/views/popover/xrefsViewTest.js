/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest*/
(function () {
    "use strict";
    var mockPack = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        currentPackage : mockPack,
        jquery : $,
        underscore : _,
        textSearch : mockPack,
        backbone : Backbone,
        XRefsCollection : new (Backbone.Collection.extend())(),
        fileDisplayHandler : {
            display : function (content) {
                xrefContent = content;
            }
        }
    };
    context = createContext(stubs);

    context(['views/p/xrefsView'], function (xrefsView) {
        describe("xrefsViewTest", function () {
            var origGetSystemObject = xrefsView.getSystemObject;

            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });
            it("should be able to load xrefsView Module", function () {
                expect(xrefsView).toBeDefined();
            });

            it("should be able to extract diagram id from diagram path", function () {
                var content = new (Backbone.Model.extend())(), diagramId;
                content.set("path", "UID1\\UId2\\DiagramUID.svg");
                diagramId = xrefsView.getDiagramId(content);
                expect(diagramId).toBe("DiagramUID");
                content.clear();
                content.set("diagramId", "testDiagId");
                expect(xrefsView.getDiagramId(content)).toBe("testDiagId");
                content.clear();
                content.set("systemId", "testSysId");
                content.set("objectId", "testObjId");
                var origFetchCrossReference=xrefsView.fetchCrossReference;
                xrefsView.fetchCrossReference=function () {return {diagramId: "testDiagId"}};
                expect(xrefsView.getDiagramId(content)).toBe("testDiagId");
                xrefsView.fetchCrossReference=origFetchCrossReference;
            });

            it("it should be able to open a system in popout", function () {
                var systemURL, systemData = new (Backbone.Model.extend())(), stopEvent;
                xrefsView.getDataId = function () {
                    return "systemId";
                };

                xrefsView.openPopout = function (url) {
                    systemURL = url;
                };
                systemData.set("id", "systemId");
                systemData.set("systemId", "systemId");
                systemData.set("diagramId", "diagramId");
                systemData.set("objectId", "objectId");
                stubs.XRefsCollection.get = function() {
                    return systemData;
                }
                xrefsView.popOut({
                    stopPropagation : function () {
                        stopEvent = true;
                    }
                });
                expect(stopEvent).toBeTruthy();
                expect(systemURL).toBe("popout.html#/system/systemId/diagramId/projectId/objectId");
            });

            it("should be able to display system", function () {
                var systemURL, contentDisplayed, systemData = new (Backbone.Model.extend())(), stopEvent, resetContentPanel;
                var origGetDataId = xrefsView.getDataId,
                    origGetWindowObj = xrefsView.getWindowObj,
                    origResetContentPanel = xrefsView.resetContentPanel,
                    origOpenPopout = xrefsView.openPopout,
                    origGetWindowObj = xrefsView.getWindowObj,
                    origDisplayContent = xrefsView.displayContent
                ;
                xrefsView.getDataId = function () {
                    return "systemId";
                };
                xrefsView.getWindowObj = function () {
                    return {
                        mentor : {
                            publisher : {
                                detailLayoutManager : {
                                    resetContentPanel : function () {

                                    }
                                },
                                fileDisplayHandler : stubs.fileDisplayHandler
                            }
                        }
                    };
                };

                xrefsView.resetContentPanel = function () {
                    resetContentPanel = true;
                };

                xrefsView.openPopout = function (url) {
                    systemURL = url;
                };
                systemData.set("id", "systemId");
                systemData.set("systemId", "systemId");
                systemData.set("diagramId", "diagramId");
                systemData.set("objectId", "objectId");
                systemData.set("path", "path");
                stubs.XRefsCollection.get = function() {
                    return systemData;
                }
                xrefsView.getWindowObj = function (content) {
                    return {
                        getIdToHighlight : function (content) {
                            return content.id;
                        }
                    };
                };
                xrefsView.displayContent = function (contentToDiaply) {
                    xrefContent = contentToDiaply;
                    contentDisplayed = true;
                };
                xrefsView.displaySelectedItem("systemId");
                expect(JSON.stringify(xrefContent)).toBe(JSON.stringify({  systemId : 'systemId', diagramId : 'diagramId', objectId : 'objectId', reset : false, type:"systemSVG", path : 'path', optionExpression : '' }));
                expect(contentDisplayed).toBeTruthy();

                xrefsView.getDataId=origGetDataId;
                xrefsView.getWindowObj=origGetWindowObj;
                xrefsView.resetContentPanel=origResetContentPanel
                xrefsView.openPopout=origOpenPopout;
                xrefsView.getWindowObj=origGetWindowObj;
                xrefsView.displayContent=origDisplayContent;
            });

            xrefsView.getSystemObject = function (systemId)
            {
                var systemObject = Backbone.Model.extend(), systemobj = new systemObject();
                systemobj.mainText = "designName"
                systemobj.id = systemId;
                return systemobj;
            }

            it("should show only diagram name for diagram as systems flow", function ()
            {
                var panelsReset = false;
                //selectedSystem.set("systemId", "systemId", {silent: true});

                window.diagramAsSystemsObjectFactoryImpl = true;
                var systemIdInMap, systemIdToXrefMap = [], systems = [], systemObject,
                    diagramWithinSystem, system = Backbone.Model.extend(), systemIns = new system();
                systemIns.set({systemId: "1", mainText: "systemName:diagrmName"});

                systems.push(systemIns);

                systemIdToXrefMap['1'] = systems;
                xrefsView.processGroupedXrefsToAddCorrectSystemName(systemIdInMap, systemIdToXrefMap, systems,
                    systemObject,
                    diagramWithinSystem);

                expect(systemIns.get("mainText")).toBe("diagrmName");
                window.diagramAsSystemsObjectFactoryImpl = '';

            });

            it("should show translated diagram and design name for diagram as systems flow", function ()
            {
                xrefsView.getSystemObject = function (systemId)
                {
                    var systemObject = Backbone.Model.extend(), systemobj = new systemObject();
                    systemobj.mainText = "designName"
                    systemobj.id = systemId;
                    return systemobj;
                }

                window.diagramAsSystemsObjectFactoryImpl = false;
                var systemIdInMap, systemIdToXrefMap = [], systems = [], systemObject = Backbone.Model.extend(),
                        systemOb = new systemObject(),diagramWithinSystem, system = Backbone.Model.extend(),
                        systemIns = new system()

                systemIns.set({systemId: "1", mainText: "systemName:diagrmName"});
                systemOb.mainText = "designName";

                systems.push(systemIns);

                systemIdToXrefMap['1'] = systems;

                var previousobj = xrefsView.getSystemObject
                var previousTraslated = Utils.translatePlainText;

                Utils.translatePlainText = function (txt)
                {
                    return txt + "_FR";
                }
                xrefsView.processGroupedXrefsToAddCorrectSystemName(systemIdInMap, systemIdToXrefMap, systems,
                        systemOb,
                        diagramWithinSystem);

                expect((systemIns.get("mainText"))).toBe("designName:diagrmName");
                window.diagramAsSystemsObjectFactoryImpl = '';
                Utils.translatePlainText = previousTraslated;
                xrefsView.getSystemObject = previousobj;
            });

            it("should be able to show toolTip", function () {
                var eventNameArg;
                xrefsView.generateEvent = function (event, eventName) {
                    eventNameArg = eventName;
                };
                xrefsView.showToolTip({currentTarget : ""});
                expect(eventNameArg).toBe(mentor.publisher.events.SHOW_TOOL_TIP);
            });

            it("should return the Title", function () {
                expect(xrefsView.getTitle()).toBe("XRefTitle");
            });

            it("should return the ClassName", function () {
                expect(xrefsView.getClassName()).toBe("Links");
            });

            it("should return the first active system", function () {
                expect(xrefsView.firstActiveSystem(["item1"])).toBe("item1");
            });

            it("should return the boolean indicating if it should process data before display", function () {
                expect(xrefsView.shouldProcessDataBeforeDisplay()).toBeTruthy();
            });

            it("should process data before render", function () {
                var origProcessXref=xrefsView.processXrefToShowCorrectSystemName;

                spyOn(xrefsView, "processXrefToShowCorrectSystemName");
                xrefsView.processDataBeforeRender();
                expect(xrefsView.processXrefToShowCorrectSystemName).toHaveBeenCalled();

                xrefsView.processXrefToShowCorrectSystemName=origProcessXref;
            });

            it("should get system object", function () {
                var origGetObjectById=mentor.publisher.project.getObjectById;
                xrefsView.getSystemObject = origGetSystemObject;
                spyOn(mentor.publisher.project, "getObjectById");
                xrefsView.getSystemObject("testSystemId");
                expect(mentor.publisher.project.getObjectById).toHaveBeenCalledWith("testSystemId");

                mentor.publisher.project.getObjectById=origGetObjectById;
            });

            it("should get diagram name", function () {
                expect(xrefsView.getDiagramName("test:testName"), "test");
                expect(xrefsView.getDiagramName("testDiagramName"), "");
            });

            it("should process Xref to show correct system name", function () {
                var origGroupXrefsBySystemId=xrefsView.groupXrefsBySystemId,
                    origProcessGroupedXrefsToAddCorrectSystemName=xrefsView.processGroupedXrefsToAddCorrectSystemName
                ;

                spyOn(xrefsView, "groupXrefsBySystemId");
                spyOn(xrefsView, "processGroupedXrefsToAddCorrectSystemName");
                xrefsView.processXrefToShowCorrectSystemName(['crossRef1', 'crossRef2']);
                expect(xrefsView.groupXrefsBySystemId).toHaveBeenCalled();
                expect(xrefsView.processGroupedXrefsToAddCorrectSystemName).toHaveBeenCalled();

                xrefsView.groupXrefsBySystemId=origGroupXrefsBySystemId;
                xrefsView.processGroupedXrefsToAddCorrectSystemName=origProcessGroupedXrefsToAddCorrectSystemName;
            });

            it("should display content", function () {
                var content={
                        diagramId:"",
                        type:"testType"
                    },
                    origFetchCrossReference=xrefsView.fetchCrossReference,
                    origShowXref=mentor.publisher.crossReferenceHandler.showXref
                ;

                spyOn(mentor.publisher.crossReferenceHandler, "showXref");
                xrefsView.fetchCrossReference=function () {return {mainText: "sampleText", xrefText: "testXrefText"}};
                xrefsView.displayContent(content);
                expect(mentor.publisher.crossReferenceHandler.showXref).toHaveBeenCalled();

                xrefsView.fetchCrossReference=function () {};
                xrefsView.displayContent(content);

                xrefsView.fetchCrossReference=origFetchCrossReference;
                mentor.publisher.crossReferenceHandler.showXref=origShowXref;
            });

            it("should fetch cross reference", function () {
                var origLoadObj=mentor.publisher.project.loadObjectData;

                mentor.publisher.project.loadObjectData=function () {return {getCrossReferences: function () {return {listItems: ['crossRef1', 'crossRef2']}}}};
                expect(xrefsView.fetchCrossReference("testSysId", "testObjId")).toBe('crossRef1');

                mentor.publisher.project.loadObjectData=function () {return {}};
                expect(xrefsView.fetchCrossReference("testSysId", "testObjId")).toBeUndefined();

                mentor.publisher.project.loadObjectData=origLoadObj;
            });

        });
    });
})();