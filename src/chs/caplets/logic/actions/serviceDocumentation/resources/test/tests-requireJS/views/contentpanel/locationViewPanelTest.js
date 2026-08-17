/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global require, describe, it, expect, Backbone, beforeEach, afterEach*/
require(["views/contentpanel/locationViewPanel"], function (locationViewPanel)
{
    "use strict";
    describe("locationViewPanelTest", function ()
    {
        var viewRendered, org_render;
        beforeEach(function ()
        {
            viewRendered = false;
            var Model = Backbone.Model.extend({});
            locationViewPanel.currentPackage = new Model();
            locationViewPanel.selectedSystem = new Model();
            locationViewPanel.selectedSystem.set("locationView", {
                mainText: "testLocationView"
            });
            locationViewPanel.selectedSystem.set("objectId", "testObjectId");
            locationViewPanel.currentPackage.set("id", "testId", {silent: true});

            org_render = locationViewPanel.render;

        });

        it("should be able to highlight object when it is open in a popout window", function ()
        {
            var popoutShown = false;
            var popoutHandler = function (url)
            {
                popoutShown = true;
                expect(url).toBe("popout.html#/showLocation/testLocationView/testId/testObjectId");

            }
            locationViewPanel.showPopout({}, {
                popoutHandler: {openPopout: popoutHandler}
            });
            expect(popoutShown).toBe(true);

        });

        it("should be able to load locationViewPanel module", function ()
        {
            locationViewPanel.render = function ()
            {
                viewRendered = true;
            };
            locationViewPanel.initialize();
            expect(locationViewPanel).toBeDefined();
        });

        it("it should re render when report id is changed", function ()
        {
            locationViewPanel.render = function ()
            {
                viewRendered = true;
            };
            locationViewPanel.initialize();
            locationViewPanel.selectedSystem.set("locationView", "locationView");
            expect(viewRendered).toBeTruthy();
        });

        it("it should re render when language is changed", function ()
        {
            locationViewPanel.render = function ()
            {
                viewRendered = true;
            };
            locationViewPanel.initialize();
            locationViewPanel.currentPackage.set("language", "fr");
            expect(viewRendered).toBeTruthy();
        });

        it("it should re render when option expression  is changed", function ()
        {
            locationViewPanel.render = function ()
            {
                viewRendered = true;
            };
            locationViewPanel.initialize();
            locationViewPanel.selectedSystem.set("optionExpression", "op1");
            expect(viewRendered).toBeTruthy();
        });

        it("it should show loading images before content is shown", function ()
        {
            var loadRingShown = false, loadRingRemovedIntheEnd;
            var isWaiting = true;
            runs(function(){
                locationViewPanel.render = org_render;
                locationViewPanel.initialize();
                mentor.publisher.features = {
                    allowsPrinting: false
                }
                window.LoadMask = {
                    LoadSVGMask: function ()
                    {
                        loadRingShown = true;
                    },
                    addLoadMask: function ()
                    {

                    },
                    removeLoadMask: function ()
                    {
                        loadRingRemovedIntheEnd = true;
                    }
                }
                locationViewPanel.createToolBar = function ()
                {
                };
                locationViewPanel.compileTemplate = function ()
                {
                };
                locationViewPanel.updateView = function ()
                {
                };
                locationViewPanel.populateContent = function ()
                {
                };
                locationViewPanel.selectedSystem.set("locationView", {path: "somePath", type: "type"});

                locationViewPanel.render();

                setTimeout(function() {
                    isWaiting = false;
                }, 10);
            });

            waitsFor(function(){
                return !isWaiting;
            }, 11);

            runs(function(){
                expect(loadRingShown).toBeTruthy();
                expect(loadRingRemovedIntheEnd).toBeTruthy();
            });
        });

        afterEach(function ()
        {
            locationViewPanel.render = org_render;
        });

    });

});
