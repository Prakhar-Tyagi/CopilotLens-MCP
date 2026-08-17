/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

/*global require, describe, it, expect, beforeEach, afterEach, mentor*/
(function () {
    "use strict";
    var context, stubs, Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend(),
            View = function (collection) {
                return Backbone.View.extend();
            };
    var navigationPanelView = new View();

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        currentPackage: new Model(),
        "DiagramsPopoverModel": new Model(),
        "RelatedDataPopoverModel": new Model(),
        "ReportsPopoverModel": new Model(),
        "preferences": new Model(),
        "views/navigationPanelView": navigationPanelView,
        "SignalTracerModel": {
            checkRendererAvailablility: function () {
                return false;
            },
            rendererLicenceAvaialable: function () {
                return true;
            }
        },
        "views/appNameAndLogo/appNameAndLogoView": {
            updateApplicationNameAndLogo: function () {
            }
        }
    };

    stubs.currentPackage.set({id: "packageId"});
    context = createContext(stubs);

    context(["views/contentpanel/toolbar/systemToolBar"],

            function (SystemButtons) {
                "use strict";
                var toolbar, orig_stopEventFlow, orig_showDiagrams, orig_showReports,
                    orig_showReferences, orig_getObjectById, orig_systemId, orig_diagramId;

                describe("systemToolBarTest", function () {
                    beforeEach(function () {
                        mentor.publisher.clientType = "CapitalServiceExplorer";
                        orig_stopEventFlow=mentor.publisher.stopEventFlow;
                        mentor.publisher.stopEventFlow = function (evt) {};
                        orig_systemId=mentor.publisher.selectedSystem.get("systemId");
                        mentor.publisher.selectedSystem.set("systemId", "testId");
                        orig_showDiagrams=mentor.publisher.systemData.showDiagrams;
                        mentor.publisher.systemData.showDiagrams = function(x, y, id, model) {};
                        orig_showReports=mentor.publisher.systemData.showReports;
                        mentor.publisher.systemData.showReports = function(x, y, id, model) {};
                        orig_showReferences=mentor.publisher.systemData.showReferences;
                        mentor.publisher.systemData.showReferences = function(x, y, id, model) {};
                        orig_getObjectById=mentor.publisher.project.getObjectById;
                        mentor.publisher.project.getObjectById = function (id) { return {getDiagrams: function() {return {length: 10}}}};
                        orig_diagramId=mentor.publisher.selectedSystem.get("diagramId");
                        mentor.publisher.selectedSystem.set("diagramId", "testId");
                    });

                    afterEach(function () {
                        mentor.publisher.clientType = undefined;
                        mentor.publisher.clientType = "CapitalServiceExplorer";
                        mentor.publisher.stopEventFlow = orig_stopEventFlow;
                        mentor.publisher.selectedSystem.set("systemId", orig_systemId);
                        mentor.publisher.systemData.showDiagrams = orig_showDiagrams;
                        mentor.publisher.systemData.showReports = orig_showReports;
                        mentor.publisher.systemData.showReferences = orig_showReferences;
                        mentor.publisher.project.getObjectById = orig_getObjectById;
                        mentor.publisher.selectedSystem.set("diagramId", orig_diagramId);
                    });

                    it("should be able to translate the title of systemtoolbar", function () {
                        var pre = Utils.translate;
                        Utils.translate = function (t) {
                            return "Translated" + t;
                        };
                        var buttons = new SystemButtons();
                        var parent$ = $('<div></div>');
                        buttons.$el = parent$;
                        SystemButtons.templateHTML = '<div class="component-label"></div>';
                        buttons.render({
                            title: "Title"
                        });
                        buttons.translateToolbarContent();
                        expect(buttons.$('.component-label').html()).toBe("TranslatedTitle");
                    });

                    it("should be able to toggle navigation panel", function () {
                        var buttons = new SystemButtons();
                        navigationPanelView.toggleVisibility= function () {};
                        spyOn(navigationPanelView, "toggleVisibility");
                        var evt={};

                        buttons.toggleNavigationPanel(evt);
                        expect(navigationPanelView.toggleVisibility).toHaveBeenCalled();
                    });

                    it("should be able to show diagrams", function () {
                        var buttons = new SystemButtons();
                        var evt={
                            clientX:0,
                            clientY:0,
                            stopPropagation: function () {},
                        };
                        spyOn(mentor.publisher.systemData, "showDiagrams");
                        spyOn(evt, "stopPropagation");

                        buttons.showDiagrams(evt);

                        expect(mentor.publisher.systemData.showDiagrams).toHaveBeenCalled();
                        expect(evt.stopPropagation).toHaveBeenCalled();
                    });

                    it("should be able to show reports", function () {
                        var buttons = new SystemButtons();
                        var evt={
                            clientX:0,
                            clientY:0,
                            stopPropagation: function () {},
                        };
                        spyOn(mentor.publisher.systemData, "showReports");
                        spyOn(evt, "stopPropagation");

                        buttons.showReports(evt);

                        expect(mentor.publisher.systemData.showReports).toHaveBeenCalled();
                        expect(evt.stopPropagation).toHaveBeenCalled();
                    });

                    it("should be able to show objects", function () {
                        var buttons = new SystemButtons();
                        var evt={
                            clientX:0,
                            clientY:0,
                            stopPropagation: function () {},
                        };
                        spyOn(mentor.publisher.systemData, "showReferences");
                        spyOn(evt, "stopPropagation");

                        buttons.showObjects(evt);

                        expect(mentor.publisher.systemData.showReferences).toHaveBeenCalled();
                        expect(evt.stopPropagation).toHaveBeenCalled();
                    });

                    it("should be able to show face views", function () {
                        var buttons = new SystemButtons();
                        buttons.faceViewSymbolHandler = {
                            showFaceViews: function (evt) {}
                        };
                        var evt={};
                        spyOn(buttons.faceViewSymbolHandler, "showFaceViews");
                        buttons.showFaceViews(evt);
                        expect(buttons.faceViewSymbolHandler.showFaceViews).toHaveBeenCalled();
                    });

                    it("should be able to get current project", function () {
                        var buttons = new SystemButtons();
                        expect(buttons.getCurrentProject()).toBe(mentor.publisher.project);
                    });

                    it("should be able get isDocumentTypeActive", function () {
                        var buttons = new SystemButtons();
                        expect(buttons.isDocumentTypeActive({})).toBeFalsy();

                        var buttons = new SystemButtons();
                        var parent$ = $('<div><div class="diagrams-button"><div><p>TEMP DATA</p></div></div></div>');
                        buttons.$el = parent$;
                        expect(buttons.isDocumentTypeActive({})).toBeTruthy();
                    });
                });

            }, function (err) {
                describe("systemToolBarTest - module load Error", function () {
                    it("Module load failed", function () {
                        console.log(err.message + "::\n" + err.stack);
                        expect(false).toBeTruthy();
                    });
                });
            });
})();




