/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

require(["systemsAsTreeView", "systems", "currentPackage"], function (systemUnderTest, designs, selectedPackage) {
    describe('systemsAsTreeViewTest', function () {
        "use strict";

        function getTestableModel(obj) {
            return new Backbone.Model(obj);
        }

        function getTestDataForDesignsWithNoDiagram() {
            return {
                get: function () {
                    return "123"
                }, attributes: {
                    getFirstDiagram: function () {
                        return {id: 123};
                    }
                }
            }
        }

        function getTestableCollection() {
            return [
                {
                    "id": "systemId",
                    "idAttribute": "systemId",
                    "mainText": "systemName",
                    "subText": "systemSubTitle"
                },
                {
                    "id": "system-id2",
                    "idAttribute": "uid2",
                    "mainText": "Design2",
                    "subText": "WYSIWYG"
                },
                {
                    "id": "system-id3",
                    "idAttribute": "uid3",
                    "mainText": "Design1",
                    "subText": "Subtitle for design1"
                }
            ];
        }

        it("should load the module", function () {
            expect(systemUnderTest).toBeTruthy();
            expect(systemUnderTest).toBeDefined();
        });
        it("should handle click event on listItem", function () {
            $('body').append($('<div class="systemUnderTest"><div class="listItem highlight" id="highlighted" data-id="systemId"></div> <div class="listItem" id="unselected" data-id="systemId"></div></div>'));
            systemUnderTest.setElement("body");
            spyOn(systemUnderTest, "clicked");
            $("#highlight").trigger("click");
            expect(systemUnderTest.clicked).wasNotCalled();
            $("#unselected").trigger("click");
            expect(systemUnderTest.clicked).toHaveBeenCalled();
        });
        xit("should handle popup event", function () {
            $('body').append($('<div data-id="systemId"> <div class="popUp"></div></div></div>'));
            spyOn(mentor.publisher.popoutHandler, "openPopout").andCallThrough();
            $(".popUp").trigger("click");
            expect(mentor.publisher.popoutHandler.openPopout).toHaveBeenCalledWith("popout.html#/system/systemId/firstDiagramId/" + selectedPackage.get("id").replace("\\", "/"));
            spyOn(designs, "get").andReturn(getTestDataForDesignsWithNoDiagram());
            $(".popUp").trigger("click");
            expect(mentor.publisher.popoutHandler.openPopout).toHaveBeenCalledWith("popout.html#/report/123/123/" + selectedPackage.get("id").replace("\\", "/"));
        });

        it("should return unique model key for system view", function () {
            expect(systemUnderTest.getModelIdString()).toBe("idAttribute");
        });

        it("should contain global models", function () {
            // this model is part of global data that is set in main-test.js
            var models = systemUnderTest.getData().getModels();
            expect(JSON.stringify(models)).toBe(
                    '[{"id":"systemId","idAttribute":"systemId","mainText":"systemName","subText":"systemSubTitle"}]');
        });
    });
}, function (err)
{
    describe("systemsAsTreeViewTest - module load Error", function ()
    {
        it("Module load failed", function ()
        {
            console.log(err.message + "::\n" + err.stack);
            expect(false).toBeTruthy();
        });
    });
});