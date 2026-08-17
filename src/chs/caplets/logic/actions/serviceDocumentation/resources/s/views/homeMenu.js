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
        'jquery',
        'underscore',
        'backbone',
        "preferences",
        "UserSession",
        "models/Effectivity",
        "viewModels/homeMenuModel"
    ],
    function ($, _, Backbone, preferences, UserSession, effectivity, homeMenuModel) {
        "use strict";

        var localize, HomeMenu, homeMenuObj;

        localize = function (menu) {
            var translator;

            translator = mentor.publisher.languageTranslator;

            menu.$(".projects-button").prop("title", translator.localize("ProjectsMenuItemTitle"));
            menu.$(".settings-button").prop("title", translator.localize("SettingsMenuItemTitle"));
        };

        HomeMenu = Backbone.View.extend({

            events: {
                "click .settings-button": "onSettingsButtonClick",
                "click .projects-button": "onProjectsButtonClick",
                "keyup #effectivity-search": "searchKeyupEventListener",
                "click #reset-effectivity-search": "onResetEffectivitySearchClick"
            },

            initialize: function () {
                preferences.on("change:language", function () {
                    homeMenuObj.render();
                    var isHomePage = $('home-screen').css('display') === 'block';
                    var searchText = this.$('#effectivity-search').val();
                    var eventDispatcher = mentor.publisher.eventDispatcher;
                    if (eventDispatcher && isHomePage) {
                        eventDispatcher.dispatchEvent('publisher:filterPackages', {searchText: searchText});
                    }
                }, this);
                this.model = homeMenuModel;
                this.model.set('isEffectivityProj', effectivity.isEffectivityProj);

                var activeSession = UserSession.getActiveSession();
                activeSession.on("change:" + UserSession.kSelectedProjectProperty, this.resetFilterText.bind(this));
                this.debouncedOnSearchKeyup = _.debounce(this.onSearchKeyup, 200);
            },

            onSettingsButtonClick: function (event) {
                this.showPopover("views/p/settings/SettingsPopoverView", event);

                event.stopPropagation();
            },

            onProjectsButtonClick: function (event) {
                this.showPopover("views/p/projects/ProjectsPopoverView", event);

                event.stopPropagation();
            },

            searchKeyupEventListener: function(event){
                this.debouncedOnSearchKeyup(event)
            },

            onSearchKeyup: function (event) {
                var searchText = $(event.currentTarget).val();
                this.model.set('searchText', searchText);
                var eventDispatcher = mentor.publisher.eventDispatcher;
                if (eventDispatcher) {
                    eventDispatcher.dispatchEvent('publisher:filterPackages', {searchText: searchText});
                }
            },

            resetFilterText: function(){
                this.$('#effectivity-search').val('');
            },

            onResetEffectivitySearchClick: function(){
                this.$('#effectivity-search').val('');
                this.model.set('searchText', '');
                mentor.publisher.eventDispatcher.dispatchEvent("publisher:filterPackages", {searchText: ''});
                setTimeout(function(){
                    this.$('#effectivity-search').trigger("focus");
                }.bind(this), 100);
            },

            showPopover: function (type, event) {
                var options,
                    popover;

                options = {
                    preferredX: event.clientX,
                    preferredY: event.clientY
                }

                require([type], function (PopoverView) {
                    popover = new PopoverView();
                    popover.render(options);
                });
            },

            render: function () {
                this.setElement(this.container);
                this.$el.html(_.template(this.templateHTML)(
                    this.model.toJSON()
                ));

                this.hideProjectsButtonIfOnlyOneProjectExists();
                this.updateApplicationNameAndLogo();
                localize(this);
                setTimeout(function () {
                    this.$('#effectivity-search').trigger("focus");
                }.bind(this), 100)

                return this;
            },
            updateApplicationNameAndLogo: function () {
                var logoName = mentor.publisher.clientType.replace(/ /g, "");
                var fullPathToImage = "images/" + logoName + ".png";
                var applicationName = mentor.publisher.constants.clientTypeToNameMap[mentor.publisher.clientType];
                this.$(".ApplicationName").text(applicationName);
                this.$(".ApplicationLogo").attr("src", fullPathToImage);
            },

            hideProjectsButtonIfOnlyOneProjectExists: function () {
                var projects = UserSession.getActiveSession().get(UserSession.kProjectsProperty);
                if (!projects || projects.length == 1) {
                    this.$(".projects-button").hide();
                }
            }

        });
        homeMenuObj = new HomeMenu();
        return homeMenuObj;
    }
);