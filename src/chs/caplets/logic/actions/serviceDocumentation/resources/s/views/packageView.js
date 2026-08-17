/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, mentor*/
define([
            'jquery',
            'underscore',
            'backbone',
            "preferences",
            "UserSession",
            "views/appNameAndLogo/appNameAndLogoView",
        ],
        function ($, underscore, Backbone, preferences, UserSession, appNameAndLogoView) {
            "use strict";
            var p = mentor.publisher;

            var localize,
                    Package;

            function updateTitle(view, title, selector)
            {
                var btn = view.$(selector);
                btn.html(title);
                btn.prop("title", title);
            }

            localize = function (view)
            {
                var projectsButton,
                        projectsButtonTitle,saveAsExcel,
                        range;

                var selectedPackage = UserSession.getActiveSession().get(UserSession.kSelectedPackageProperty);
                if (selectedPackage) {
                    range = selectedPackage.get('effectivityRange');
                }
                if(range){
                    projectsButtonTitle = mentor.publisher.languageTranslator.localize("Effectivity");
                } else {
                    projectsButtonTitle = mentor.publisher.languageTranslator.localize("Packages");
                }
                saveAsExcel=mentor.publisher.languageTranslator.localize("saveAsExcel")
                updateTitle(view, projectsButtonTitle, "#project_button");
                updateTitle(view, saveAsExcel, ".save-button");
            };

            Package = Backbone.View.extend({

                initialize: function ()
                {
                    preferences.on("change:language", function ()
                    {
                        localize(this);
                    }, this);
                },

                events: {
                    "click #project_button": "showPackages",
                    "click #back_button": "showPreviousDocument",
                    "click #forward_button": "showNextDocument",
                    "click .save-button": "onSaveButtonClick",
                },
                onSaveButtonClick: function ()
                {

                    require(["illustrator/exportPackage", "currentPackage"], function (exportPackage, currentPackage)
                    {
                        exportPackage.export({
                            packageId: currentPackage.get("id"),
                            language: currentPackage.get("language")
                        });
                    });
                },
                showPreviousDocument: function ()
                {
                    if (window.history.length > 0) {
                        window.history.back();
                    }
                },
                showNextDocument: function ()
                {
                    if (window.history.length > 0) {
                        window.history.forward();
                    }
                },
                showPackages: function (event)
                {
                    history.pushState(null, document.title, window.location.pathname );
                    var p = mentor.publisher;
                    p.router.showHome();
                    p.urlParams = {};
                    p.eventDispatcher.dispatchEvent(p.events.CLOSE_POPOVER, event);
                    p.popoutHandler.closePopoutWindows();
                    event.stopPropagation();
                },

                render: function ()
                {
                    this.remove();
                    this.setElement(this.container);
                    var selectedPackage = UserSession.getActiveSession().get(UserSession.kSelectedPackageProperty);
                    if (selectedPackage) {
                        var range = selectedPackage.get('effectivityRange');
                        if(range && selectedPackage.get('start') === selectedPackage.get('end')){
                            range = range.split('-')[0];
                        }
                    }
                    this.$el.append(underscore.template(this.templateHTML)({
                        range: range
                    }));
                    appNameAndLogoView.updateApplicationNameAndLogo(this);
                    localize(this);

                    return this;
                }
            });

            return new Package();
        }
);