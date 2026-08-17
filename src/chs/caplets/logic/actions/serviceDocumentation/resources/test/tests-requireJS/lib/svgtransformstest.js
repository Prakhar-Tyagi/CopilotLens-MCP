/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/* globals createContext, describe, it, beforeEach, afterEach, expect, Backbone, $, mentor */
(function () {
    "use strict";

    var backgroundTemplate,
        context,
        publisher,
        preferences,
        stubs;

    backgroundTemplate = '<div id="root"><div></div><div class="capital-background-fill"></div></div>';
    preferences = new Backbone.Model();
    stubs = {
        preferences: preferences
    };
    context = createContext(stubs);

    context(
            ["utilities/SVGTransforms"],
        function (SVGTransforms) {
            describe("SVGTransforms", function () {
                beforeEach(function () {
                    $("body").html("");

                    publisher = mentor.publisher;
                    mentor.publisher = {};
                    mentor.publisher.colors = {};

                    preferences.clear();
                });

                it("should customize background according to preference", function () {
                    $("body").html(backgroundTemplate);
                    preferences.set("background-color", "white");

                    var dom = $("#root");
                    SVGTransforms.customizeBackground(dom);

                    ensureBackgroundColorSet("rgb(255, 255, 255)");
                });

                it("should customize background according to author set if available and preference isn't set", function () {
                    $("body").html(backgroundTemplate);
                    mentor.publisher.colors["svg-background-color"] = "red";
                    var dom = $("#root");
                    SVGTransforms.customizeBackground(dom);

                    ensureBackgroundColorSet("rgb(255, 0, 0)");
                });

                it("should customize background default if preference isn't set and background template isn't set.", function () {
                    $("body").html(backgroundTemplate);

                    var dom = $("#root");
                    SVGTransforms.customizeBackground(dom);

                    expect($("body").html()).toBe(backgroundTemplate);
                });

                afterEach(function () {
                    mentor.publisher = publisher;
                });
            });
        },
        function (err) {
            describe("SVGTransforms", function ()
            {
                it("failed to load", function ()
                {
                    expect(err).toBeUndefined();
                });
            });
        }
    );


    function ensureBackgroundColorSet(color) {
        expect($("#root").css("background-color")).toBe(color);
        expect($(".capital-background-fill").css("fill")).toBe(color);
    }
})();