/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, Backbone, beforeEach, afterEach*/
(function () {
    "use strict";
    var Toolbar = Backbone.View.extend({
        render : function (content) {
            return '';
        }
    });

    var stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        "views/contentpanel/toolbar/contentToolBar": Toolbar,
    };
    var context = createContext(stubs);

    context(["views/contentpanel/faultCodePanel"], function (faultCodePanel) {
        "use strict";
        describe("faultCodePanelTest", function () {
            var viewRendered, org_render;
            beforeEach(function () {
                viewRendered = false;
                var Model = Backbone.Model.extend({});
                faultCodePanel.currentPackage = new Model();
                faultCodePanel.selectedSystem = new Model();
                org_render = faultCodePanel.render;
                faultCodePanel.render = function () {
                    viewRendered = true;
                };
                mentor.publisher.faultCodeTableGenerator.getObjects = function (object) {
                    return [];
                }
                faultCodePanel.initialize();

            });

            it("should be able to load faultCodePanel module", function () {
                expect(faultCodePanel).toBeDefined();
            });

            it("it should re render when report id is changed", function () {
                faultCodePanel.selectedSystem.set("faultCode", "faultData");
                expect(viewRendered).toBeTruthy();
            });

            it("it should re render when language is changed", function () {
                faultCodePanel.currentPackage.set("language", "fr");
                expect(viewRendered).toBeTruthy();
            });

            it("it should re render when option expression  is changed", function () {
                faultCodePanel.selectedSystem.set("optionExpression", "op1");
                expect(viewRendered).toBeTruthy();
            });

            it("it should close", function () {
                faultCodePanel.close();
                expect(faultCodePanel.selectedSystem.get("faultCode")).toBe("");
                expect(faultCodePanel.$el.html()).toBe('');
            });

            it("it should get Title", function () {
                var Model = Backbone.Model.extend({});
                var temp = new Model();
                temp.set("mainText", "mainTitle");
                temp.set("subText", "subTitle");
                faultCodePanel.selectedSystem.set("faultCode", temp);
                expect(faultCodePanel.getTitle()).toBe('mainTitle, subTitle');
            });

            xit("it should clear container", function () {
                faultCodePanel.clearContainer();
                expect(faultCodePanel.$el.html()).toBe('');
            });

            it("it should render", function () {
                spyOn(mentor.publisher.faultCodeTableGenerator, "getObjects");
                var Model = Backbone.Model.extend({});
                var temp = new Model();
                temp.set("mainText", "mainTitle");
                temp.set("subText", "subTitle");
                faultCodePanel.selectedSystem.set("faultCode", temp);
                faultCodePanel.selectedSystem.set("systemId", "systemId");
                faultCodePanel.templateHTML = "<%=title%>";
                faultCodePanel.render = org_render;
                var result = {};

                result=faultCodePanel.render();

                var expectedResult={
                    "cid": "158",
                    "options": {},
                    "$el": {},
                    "currentPackage": {},
                    "selectedSystem": {
                        "faultCode": {
                            "mainText": "mainTitle",
                            "subText": "subTitle"
                        },
                        "systemId": "systemId"
                    },
                    "templateHTML": "<%=title%>"
                };
                // expect(JSON.stringify(result)).toBe(JSON.stringify(expectedResult));
                // the cid keeps changing everytime, sonot sure on how to test the above line.
                expect(mentor.publisher.faultCodeTableGenerator.getObjects).toHaveBeenCalled();
            });




            afterEach(function () {
                faultCodePanel.render = org_render;
            });

            });

    }, function (err) {
        describe("faultCodePanelTest - module load Error", function () {
            it("Module load failed", function () {
                console.log(err);
                expect(false).toBeTruthy();
            });
        });
    });
})();