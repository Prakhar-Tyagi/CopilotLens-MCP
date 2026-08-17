/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/* globals $, _, Backbone, createContext */
(function () {
    "use strict";

    var allColors,
        colors,
        context,
        preferences,
        stubs,
        svgBackgroundColor;

    preferences = new Backbone.Model();
    allColors = [["#000", "red"], ["#ded"]];
    colors = mentor.publisher.colors;

    stubs = {
        jquery : $,
        underscore : _,
        backbone : Backbone,
        allColors: allColors,
        preferences: preferences
    };

    context = createContext(stubs);

    context(
        ["views/p/colors/palettepanel"],
        function (PalettePanel) {
            describe("palettepanel", function () {
                beforeEach(function () {
                    mentor.publisher.colors = {};
                });

                it("should render the grid colors", function () {
                    testRender(PalettePanel, "gridColors", "#000,red,#ded");
                });

                it("should render the default color if set", function () {
                    mentor.publisher.colors["svg-background-color"] = "lightgray";
                    testRender(PalettePanel, "defaultColor", "lightgray");
                });

                it("should render white as the default color if not set", function () {
                    testRender(PalettePanel, "defaultColor", "white");
                });

                it("should reset the background color preference when default color is clicked", function () {
                    $("body").html("");

                    preferences.set("background-color", "purple");

                    PalettePanel.templateHTML = "<div class='cp-default-item'></div>";
                    PalettePanel.container = "body";

                    var panel = new PalettePanel();
                    panel.render();

                    panel.$(".cp-default-item").trigger("click");

                    expect(preferences.get("background-color")).toBe("");
                });

                afterEach(function () {
                    mentor.publisher.colors = colors;
                });
            });
        },
        function (err)
        {
            describe("palettepanel", function ()
            {
                it("failed to load", function ()
                {
                    expect(err).toBeUndefined();
                });
            });
        }
    );

    function testRender(Panel, param, value) {
        $("body").html("");

        Panel.templateHTML = "<%= " + param +" %>";
        Panel.container = "body";

        var panel = new Panel();
        panel.render();

        expect($("body").html()).toBe(value);
    }
})();