/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

define(
    ["jquery", "underscore", "backbone"],
    function ($, _, Backbone) {
        "use strict";

        var addLanguageChangeListener,
            addSystemScopeListener,
            addBackgroundColorChangeListener,
            addLegacySupport,
            initializeLanguagePreference,
            initializeSystemScope,
            initializeBackgroundColor,
            Preferences;

        addLanguageChangeListener = function (preferences) {
            preferences.on("change:language", function (model, language) {
                Utils.createCookie("language", language, Utils.getCookiesDuration());
            });
        };

        addSystemScopeListener = function (preferences) {
            preferences.on("change:systemScope", function (model, systemScope) {
                Utils.createCookie("systemScope", systemScope, Utils.getCookiesDuration());
            });
        };

        addBackgroundColorChangeListener = function (preferences) {
            preferences.on("change:background-color", function (model, color) {
                Utils.createCookie("background-color", color || "", Utils.getCookiesDuration());
            });

            preferences.on("change:background-color", function () {
                require(["SVGTransforms"], function (SVGTransforms) {
                    var popouts = mentor.publisher.popoutHandler.popouts;
                    var windows = [window];
                    if (popouts) {
                        windows = windows.concat(popouts);
                    }

                    windows.forEach(function (aWindow) {
                        aWindow.$('object').each(function () {
                            var contentDocument = this.contentDocument;
                            var svg = contentDocument && contentDocument.documentElement;
                            if (svg && svg.tagName.toLowerCase() === "svg") {
                                if(svgEventHandlers.locationViewSVGLoadArea && svgEventHandlers.locationViewSVGLoadArea.resetAttributesAndStack) {
                                    svgEventHandlers.locationViewSVGLoadArea.resetAttributesAndStack();
                                }
                                SVGTransforms.customizeBackground(svg);
                            }

                            this.style.display = '';
                            this.offsetHeight;
                            this.style.display = 'block';
                        });
                    });
                });
            });
        };

        addLegacySupport = function (preferences) {
            mentor.publisher.eventDispatcher.attachEventListener(
                mentor.publisher.events.LANGUAGE_FILTER_APPLIED,
                function (event) {
                    preferences.set("language", event.detail.lang);
                }
            );
        };

        initializeLanguagePreference = function (preferences) {
            var language;

            language = Utils.readCookie("language") || "EN";
            preferences.set("language", language);
        };

        initializeSystemScope = function (preferences) {
            var systemScope;

            systemScope = Utils.readCookie("systemScope") || "designs";
            preferences.set("systemScope", systemScope);
        };

        initializeBackgroundColor = function (preferences) {
            var color = Utils.readCookie("background-color");
            if (color) {
                preferences.set("background-color", color);
            }
        };

        Preferences = Backbone.Model.extend({

            initialize: function () {
                initializeLanguagePreference(this);
                initializeSystemScope(this);
                initializeBackgroundColor(this);
                addLanguageChangeListener(this);
                addSystemScopeListener(this);
                addBackgroundColorChangeListener(this);
                addLegacySupport(this);
            }

        });

        return new Preferences();
    }
);