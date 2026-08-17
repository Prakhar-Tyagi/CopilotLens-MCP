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
            "preferences"
        ],
        function ($, _, Backbone, preferences)
        {
            "use strict";

            var LanguagesPanel;

            LanguagesPanel = Backbone.View.extend({

                events: {
                    "click .languages-panel .titlebar": "onTitlebarClick",
                    "click .languages-panel .listItem": "onItemClick"
                },

                onTitlebarClick: function (event)
                {
                    $(event.currentTarget).parent().find(".listItem").each(function ()
                    {
                        $(this).toggle();
                    });

                    event.stopPropagation();
                },

                onItemClick: function (event)
                {
                    var code = $(event.currentTarget).attr('data-id');
                    var list = mentor.publisher.languageDataLoader.getOrderedLangList();
                    var lang = _.findWhere(list, {code: code});

                    mentor.publisher.languageTranslator.loadResources(lang.code);

                    var loadMask = new LoadingMaskCreator();
                    loadMask.addLoadMask('home-screen');
                    
                    setTimeout(function() {
                        mentor.publisher.languageDataLoader.hasLanguageChanged(lang.name);

                        var currentPackage = require("currentPackage");
                        currentPackage.set("language", code, {fromSettingsPanel: true});

                        preferences.set("language", code);

                        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});

                        event.stopPropagation();
                        loadMask.removeLoadMask();
                    }, 0);
                },

                render: function ()
                {
                    var currentLanguageID,
                            options,
                            panel,
                            languages,
                            renderedPanel;

                    panel = this;

                    currentLanguageID = mentor.publisher.languageDataLoader.getCurrentLanguage();
                    languages = mentor.publisher.languageDataLoader.getOrderedLangList();

                    options = {};
                    options.items = languages.map(function (language)
                    {
                        var item = new Backbone.Model({
                            id: language.code,
                            mainText: language.name
                        });
                        item.isActive = (item.id === currentLanguageID) ? "preferred" : "";

                        return item;
                    });
                    options.className = "languages-panel";
                    options.expand = true;
                    options.showPopup = false;
                    options.showTitle = true;
                    options.title = mentor.publisher.languageTranslator.localize("LanguagesPanelTitle");
                    options.totalItems = languages;

                    renderedPanel = _.template(LanguagesPanel.templateHTML)(options);
                    panel.$el.append(renderedPanel);

                    panel.$el.find(".languages-panel .listItem .mainText").before("<span class=\"tick\"></span>");

                    return this;
                }

            });

            return LanguagesPanel;
        }
)