/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    [
        "jquery",
        "underscore",
        "backbone",
        "preferences",
        "UserSession",
        "Package"
    ],
    function ($, _, Backbone, preferences, UserSession, Package) {
        "use strict";

        var PackageInfoView;

        PackageInfoView = Backbone.View.extend({
            selectedPackage: null,

            events: {
                "click .open-button": function (evt) {
                    evt.preventDefault();
                    mentor.publisher.router.loadProject({
                        projectId: this.selectedPackage.get('id'),
                        range: this.selectedPackage.get('effectivityRange'),
                        projId: this.selectedPackage.get('projectId')
                    });
                }
            },

            initialize: function () {
                var activeSession = UserSession.getActiveSession();
                this.selectedPackage = activeSession.get(UserSession.kSelectedPackageProperty);
                activeSession.on("change:" + UserSession.kSelectedPackageProperty, function () {
                    this.onSelectedPackageChange();
                }, this);
                preferences.on("change:language", this.render, this);
            },

            onSelectedPackageChange: function () {
                var activeSession = UserSession.getActiveSession();
                this.selectedPackage = activeSession.get(UserSession.kSelectedPackageProperty);
                var langDataLoader = mentor.publisher.languageDataLoader;
                var currentPackageId = langDataLoader.getPackageId();
                var selectedPackageId = this.selectedPackage ? this.selectedPackage.get('id').replace("data/", "").replace("data\\", "") : null;

                if (currentPackageId !== selectedPackageId) {
                    langDataLoader.reset();
                    langDataLoader.resetDefaultLanguageChoice();
                    langDataLoader.setPackageId(selectedPackageId || '');
                }
                this.render();
            },

            render: function () {
                this.setElement(this.container);
                this.$el.html(_.template(this.templateHTML)({
                    selectedPackage: this.selectedPackage
                }));

                return this;
            }
        });

        return new PackageInfoView();
    }
)