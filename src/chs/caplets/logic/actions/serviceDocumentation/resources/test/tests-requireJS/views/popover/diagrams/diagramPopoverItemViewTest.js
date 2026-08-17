/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest, Backbone, $, _, createContext*/
(function () {
    "use strict";
    var mockModel = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        currentPackage : mockModel,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        DiagramsCollection : new (Backbone.Collection.extend())(),
        PopoverItemView : Backbone.Model.extend(),
        fileDisplayHandler : {
            display : function (content) {
                this.content = content;
            }
        }
    };
    context = createContext(stubs);

    context(['views/p/diagrams/diagramPopoverItemView'], function (diagramPopoverItemView) {
        describe("diagramPopoverItemViewTest", function () {
            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });
            it("should be able to load diagramPopoverItemView Module", function () {
                expect(diagramPopoverItemView).toBeDefined();
            });

            /* it("should be filter project and should not show current project", function () {
             expect(packagesPopoverItemView.filter([
             {id : "otherProject"},
             {id : "projectId"}
             ]).length).toBe(1);
             });*/

            it("should extend PopoverItemView", function () {
                expect(diagramPopoverItemView instanceof stubs.PopoverItemView).toBe(true);
            });

            it("should create correct URL for popout", function () {
                var URL;
                mockModel.set("systemId", "testSystemId");
                mockModel.set("diagramId", "testDiagramId");
                URL = diagramPopoverItemView.createURL(mockModel);
                expect(URL).toBe("popout.html#/system/testSystemId/testDiagramId/projectId");
            });

            it("should be able to open content when it is clicked", function () {
                var URL;
                mockModel.set("systemId", "testSystemId");
                mockModel.set("diagramId", "testDiagramId");
                URL = diagramPopoverItemView.createURL(mockModel);
                expect(URL).toBe("popout.html#/system/testSystemId/testDiagramId/projectId");
            });

           /* it("should be able to update URL in browser", function () {
                var URL, spy = sinon.spy(Backbone.history, "navigate");
                mockModel.set("systemId", "testSystemId");
                mockModel.set("diagramId", "testDiagramId");
                diagramPopoverItemView.updateURL(mockModel);
                expect(Backbone.history.navigate.getCall(0).args[0]).toBe("system/testSystemId/testDiagramId/projectId");
                expect(JSON.stringify(Backbone.history.navigate.getCall(0).args[1])).toBe(JSON.stringify({trigger : false}));
                Backbone.history.navigate.restore();
            });*/

            it("should be able display system in contentpanel", function () {
                var URL, spy = sinon.spy(diagramPopoverItemView, "updateURL");
                mockModel.set("systemId", "testSystemId");
                mockModel.set("diagramId", "testDiagramId");
                diagramPopoverItemView.displayContent(mockModel);
                expect(JSON.stringify(stubs.fileDisplayHandler.content)).toBe('{"id":"testSystemId","diagramId":"testDiagramId","reset":false,"type":"systemSVG"}');
                expect(JSON.stringify(diagramPopoverItemView.updateURL.getCall(0).args[0])).toBe(JSON.stringify(mockModel));
                diagramPopoverItemView.updateURL.restore();
            });

        });
    });
})();
