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
        ReportsCollection : new (Backbone.Collection.extend())(),
        PopoverItemView : Backbone.Model.extend(),
        fileDisplayHandler : {
            display : function (content) {
                this.content = content;
            }
        }
    };
    context = createContext(stubs);

    context(['views/p/reports/reportsPopoverItemView'], function (reportsPopoverItemView) {
        describe("reportsPopoverItemViewTest", function () {
            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });
            it("should be able to load diagramPopoverItemView Module", function () {
                expect(reportsPopoverItemView).toBeDefined();
            });

            it("should extend PopoverItemView", function () {
                expect(reportsPopoverItemView instanceof stubs.PopoverItemView).toBe(true);
            });

            it("should create correct URL for popout", function () {
                var URL;
                mockModel.set("systemId", "testSystemId");
                mockModel.set("reportId", "testreportId");
                URL = reportsPopoverItemView.createURL(mockModel);
                expect(URL).toBe("popout.html#/report/testSystemId/projectId/projectId");
            });

            it("should be able display system in contentpanel", function () {
                mockModel.set("systemId", "testSystemId");
                mockModel.set("reportId", "testreportId");
                reportsPopoverItemView.displayContent(mockModel);
                expect(JSON.stringify(stubs.fileDisplayHandler.content)).toBe('{"reportId":"projectId","systemId":"testSystemId","reset":false,"type":"systemReport"}');
            });

        });
    });
})();
