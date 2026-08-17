/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(["PopoverItemView", "LanguagesCollection", "currentPackage"],
    function (PopoverItemView, languages, currentPackage) {
        "use strict";
        var LanguagesPopoverItem = PopoverItemView.extend({
            getData : function () {
                return languages;
            },
            getTitle : function () {
                return "";
            },
            getClassName : function () {
                return "languages";
            },
            isExpanded : function () {
                return true;
            },
            shouldShowPopup : function () {
                return false;
            },
            events : {
                "click .languages>.listItem" : "popoverItemClicked"
            },
            getItemContent : function (itemId) {
                return languages.get(itemId);
            },

            languageChanged : function (choiceString) {
                return mentor.publisher.languageDataLoader.hasLanguageChanged(choiceString);
            },

            getLanguageBtnTooltip : function (choiceString) {
                return mentor.publisher.languageTranslator.localize('Language') +
                    this.getLanguageCode(choiceString);
            },

            getLanguageCode : function (choiceString) {
                return choiceString.split(" : ")[0];
            },

            changeBtnLanuageToolTip : function (choiceString) {
                $('.languageBtn').html(this.getLanguageCode(choiceString));
                mentor.publisher.toolTip.changeToolTipTextOnButton($('.languageBtn'),
                    this.getLanguageBtnTooltip(choiceString));
            },

            displayContent : function (content) {
                var choiceString, change;
                var loadMask = new LoadingMaskCreator();
                if (content) {
                    choiceString = content.get('id');
                    loadMask.addLoadMask('applicationArea');
                    setTimeout(function() {
                        change = this.languageChanged(choiceString);
                        if (change) {
                            mentor.publisher.languageTranslator.translate(this.getLanguageCode(choiceString));
                            this.changeBtnLanuageToolTip(choiceString);
                        }
                        loadMask.removeLoadMask();
                    }.bind(this), 0);
                }
            }
        }), languagePopoverItem = new LanguagesPopoverItem();
        mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.LANGUAGE_FILTER_APPLIED,
            function (evt) {
                var lang = evt.detail.lang || "";

                currentPackage.set("language", lang);
            });
        return languagePopoverItem;
    });
