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
    var mockPack = new (Backbone.Model.extend())(), context, stubs, xrefContent;

    stubs = {
        currentPackage : mockPack,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        PackagesCollection : new (Backbone.Collection.extend())(),
        PopoverItemView : Backbone.Model.extend()
    };
    context = createContext(stubs);

    context(['views/p/packages/packagesPopoverItemView'], function (packagesPopoverItemView) {
        describe("packagesPopoverItemViewTest", function () {
            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });
            it("should be able to load packagesPopoverItemView Module", function () {
                expect(packagesPopoverItemView).toBeDefined();
            });

            it("should be filter project and should not show current project", function () {
                expect(packagesPopoverItemView.filter([
                    {id : "otherProject"},
                    {id : "projectId"}
                ]).length).toBe(1);
            });

            it("should extend PopoverItemView", function () {
                expect(packagesPopoverItemView instanceof stubs.PopoverItemView).toBe(true);
            });

            it("should be able to load a project", function () {
                var spy = sinon.spy(Backbone.history, "navigate"), selectedProjectIdFetched, anyOpenedPopoverRemoved;
                packagesPopoverItemView.getSelectedProjectId = function () {
                    selectedProjectIdFetched = true;
                };

                packagesPopoverItemView.removeAnyOpenedPopup = function () {
                    anyOpenedPopoverRemoved = true;
                };
                stubs.PackagesCollection.get = function () {
                    return { id : "projectToLoad"};
                };
                packagesPopoverItemView.loadProject({});
                expect(Backbone.history.navigate.getCall(0).args[0]).toBe("package/projectToLoad");
                expect(JSON.stringify(Backbone.history.navigate.getCall(0).args[1])).toBe(JSON.stringify({trigger : true}));
                Backbone.history.navigate.restore();
            });

        });
    });
})();
