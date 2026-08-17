/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/* global Backbone, createContext, $, _, Backbone */
(function ()
{
    "use strict";

    var context, dictionary, english, eventDispatcher, french, languageTranslator, exportPackage,
            popoutHandler, router, stubs, SignalTracerModel, preferences, appNameAndLogoView;

    english = {
        projects: "ProjectsEN",
        Packages: "PackagesEN",
        RenderConnectivityButtonTitle: "RenderEN"
    };

    french = {
        projects: "ProjectsFR",
        Packages: "PackagesFR",
        RenderConnectivityButtonTitle: "RenderFR"
    };

    preferences = new Backbone.Model();

    SignalTracerModel = {
        addEventHandlers: function ()
        {
        }
    };

    eventDispatcher = mentor.publisher.eventDispatcher;
    languageTranslator = mentor.publisher.languageTranslator;
    popoutHandler = mentor.publisher.popoutHandler;
    router = mentor.publisher.router;
    appNameAndLogoView = {
        updateApplicationNameAndLogo: function () {},
    };
    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        SignalTracerModel: SignalTracerModel,
        preferences: preferences,
        "views/appNameAndLogo/appNameAndLogoView": appNameAndLogoView,
    };

    context = createContext(stubs);

    context(["views/packageView"], function (packageView)
    {
        var isSignalTracerAvailable = packageView.isSignalTracerAvailable;

        describe("PackageViewTest", function ()
        {
            beforeEach(function ()
            {
                packageView.container = $("<div></div>");
                packageView.isSignalTracerAvailable = function ()
                {
                    return true;
                };
                packageView.templateHTML = "<div id='project_button'></div><div class='renderConnectivityBtn'></div>";

                mentor.publisher.languageTranslator = {
                    localize: function (word)
                    {
                        return dictionary[word] || word;
                    }
                };

                dictionary = english;
            });

            it("should be able to define packages module", function ()
            {
                expect(packageView).toBeDefined();
            });

            it("renders correctly", function () {
                packageView.render();
                expect(packageView.$el.html()).toBe(
                        '<div id="project_button" title="PackagesEN">PackagesEN</div><div class="renderConnectivityBtn"></div>');
            });

            it("renders correctly on language change", function ()
            {
                packageView.render();

                dictionary = french;
                preferences.trigger("change:language");

                expect(packageView.$el.html()).toBe(
                        '<div id="project_button" title="PackagesFR">PackagesFR</div><div class="renderConnectivityBtn"></div>');
            });

            it("it should be events for backward/forward navigation", function ()
            {
                packageView.render();
                expect(JSON.stringify(packageView.events)).toBe(
                        '{"click #project_button":"showPackages",' +
                        '"click #back_button":"showPreviousDocument",' +
                        '"click #forward_button":"showNextDocument",' +
                        '"click .save-button":"onSaveButtonClick"}');
            });

            // TODO: what do we achieve here?? this fails when run with coverage.
            xit("it should have callback for backward", function ()
            {
                packageView.render();
                expect(packageView.showPreviousDocument.toString().replace(/(\r\n|\n|\r)/gm, "")).toBe(
                        'function ()            {                if (window.history.length > 0) {                    window.history.back();                }            }');
            });

            // TODO: what do we achieve here?? this fails when run with coverage.
            xit("it should have callback for forward", function ()
            {
                packageView.render();
                expect(packageView.showNextDocument.toString().replace(/(\r\n|\n|\r|\t)/gm, "")).toBe(
                        'function ()            {                if (window.history.length > 0) {                    window.history.forward();                }            }');
            });

            it("it should be able to show previous and next documents and show packages", function ()
            {
                spyOn(window.history, "back").andCallThrough();
                packageView.showPreviousDocument();
                expect(window.history.back).toHaveBeenCalled();

                spyOn(window.history, "forward").andCallThrough();
                packageView.showNextDocument();
                expect(window.history.forward).toHaveBeenCalled();

                mentor.publisher.router={
                    showHome:function () {}
                };
                var evt={
                    stopPropagation: function () {}
                };
                spyOn(evt, "stopPropagation");
                packageView.showPackages(evt);
                expect(evt.stopPropagation).toHaveBeenCalled();
            });

            it("it should be able to render", function ()
            {
                spyOn(appNameAndLogoView, "updateApplicationNameAndLogo");
                packageView.render();
                expect(appNameAndLogoView.updateApplicationNameAndLogo).toHaveBeenCalled();
            });

            afterEach(function ()
            {
                packageView.container = undefined;
                packageView.isSignalTracerAvailable = isSignalTracerAvailable;
                packageView.templateHTML = undefined;

                mentor.publisher.eventDispatcher = eventDispatcher;
                mentor.publisher.languageTranslator = languageTranslator;
                mentor.publisher.popoutHandler = popoutHandler;
                mentor.publisher.router = router;
            });
        });
    });
})();