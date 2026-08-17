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

    var colors,
        context,
        preferences,
        recentColors,
        stubs,
        svgBackgroundColor;

    preferences = new Backbone.Model();
    recentColors = ["#000", "red","#ded"];
    colors = mentor.publisher.colors;

    stubs = {
        jquery : $,
        underscore : _,
        backbone : Backbone,
        recentColors: recentColors,
        preferences: preferences
    };

    context = createContext(stubs);

    context(
        ["views/p/colors/historypanel"],
        function (Panel) {
            describe("historypanel", function () {
                beforeEach(function () {
                    mentor.publisher.colors = {};
                });

                it("should render the recents colors", function () {
                    testRender(Panel, "colors", "#000,red,#ded");
                });

                afterEach(function () {
                    mentor.publisher.colors = colors;
                });
            });
        },
        function (err)
        {
            describe("historypanel", function ()
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