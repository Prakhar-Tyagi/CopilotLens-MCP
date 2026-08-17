/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

require(["routers/harnessLayoutDiagramRouter"],
        function (harnessLayoutDiagramRouter) {
            var componentType,
                    disaplyedContent,
                    harnessLayouts = {
                        Design1: {}
                    }, layout = {
                        id: "Door-ltft",
                        getDocumentsInGroupTitled: function (type) {
                            var data = {
                                "diagrams": [{mainText: "diagram1", id:"diagram1-id1"}, {mainText: "diagram2", id:"diagram-id2"}],
                                "reports": [{mainText: "report1", id:"report-id1"}, {mainText: "report2", id:"report-id2"}]
                            }
                            var Model = Backbone.Collection.extend();
                            var objects = new Model();
                            objects.set(data[type])
                            return objects;
                        }
                    };
            describe("harnessLayoutDiagramRouterTest", function () {
                beforeEach(function () {
                    contentDisplayed = undefined;
                    harnessLayoutDiagramRouter.getComponentType = function () {
                        return componentType;
                    };
                    harnessLayoutDiagramRouter.fileDisplayHandler.display = function (contentToDisplay) {
                        disaplyedContent = contentToDisplay;
                    }
                    harnessLayoutDiagramRouter.findElementInCollection =
                            function (harnessDesignName, allDesigns, type) {
                                return layout;
                            }
                    componentType = '';
                });
                it("should be able to load harnessLayoutDiagramRouter Module", function () {
                    expect(harnessLayoutDiagramRouter).toBeDefined();
                });
                it("should open first diagram if no componet and componetType is provided", function () {
                    var options = {
                        parameters: {
                            harnesslayout: "DOOR-LEFT"
                        }
                    };
                    harnessLayoutDiagramRouter.displayHarnessLayouts(options, harnessLayouts);
                    expect(JSON.stringify(disaplyedContent)).toBe('{"group":"diagrams","layoutId":"Door-ltft","listItemId":"Door-ltft","id":"diagram1-id1","type":"harnessLayoutDiagram"}');
                });
                it("should open first report if no componet and componetType is report ", function () {
                    componentType = 'report'
                    var options = {
                        parameters: {
                            harnesslayout: "DOOR-LEFT",
                        }
                    };
                    harnessLayoutDiagramRouter.displayHarnessLayouts(options, harnessLayouts);
                    expect(JSON.stringify(disaplyedContent)).toBe('{"group":"reports","layoutId":"Door-ltft","listItemId":"Door-ltft","id":"report-id1","type":"harnessLayoutReport"}');
                });
                it("should open correct diagram based on parameters ", function () {
                    componentType = 'diagram'
                    var options = {
                        parameters: {
                            harnesslayout: "DOOR-LEFT",
                            component:"diagram2",
                        }
                    };
                    harnessLayoutDiagramRouter.displayHarnessLayouts(options, harnessLayouts);
                    expect(JSON.stringify(disaplyedContent)).toBe('{"group":"diagrams","layoutId":"Door-ltft","listItemId":"Door-ltft","id":"diagram-id2","type":"harnessLayoutDiagram"}');
                });
                it("should open correct report based on parameters ", function () {
                    componentType = 'report'
                    var options = {
                        parameters: {
                            harnesslayout: "DOOR-LEFT",
                            component: "report2",
                        }
                    };
                    harnessLayoutDiagramRouter.displayHarnessLayouts(options, harnessLayouts);
                    expect(JSON.stringify(disaplyedContent)).toBe(
                            '{"group":"reports","layoutId":"Door-ltft","listItemId":"Door-ltft","id":"report-id2","type":"harnessLayoutReport"}');
                });

            });
        }, function (err) {
            describe("harnessLayoutDiagramRouterTestFailed", function () {

                it("failed to load harnessLayoutDiagramRouter Module", function () {
                    expect(true).toBeFalsy();
                });
            });
        });