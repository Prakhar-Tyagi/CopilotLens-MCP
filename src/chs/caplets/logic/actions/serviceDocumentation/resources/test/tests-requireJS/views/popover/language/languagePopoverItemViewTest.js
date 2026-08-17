/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, window, require, describe, expect, it, listTemplateForTest, Backbone, $, _, createContext*/
(function () {
    "use strict";
    var mockModel = new (Backbone.Model.extend())(), context, stubs, xrefContent

    stubs = {
        currentPackage : mockModel,
        jquery : $,
        underscore : _,
        backbone : Backbone,
        LanguagesCollection : new (Backbone.Collection.extend())(),
        PopoverItemView : Backbone.Model.extend()
    };
    context = createContext(stubs);

    context(['views/p/languages/languagesPopoverItemView'], function (languagesPopoverItemView) {
        describe("languagesPopoverItemViewTest", function () {
            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
            });
            it("should be able to load languagesPopoverItemView Module", function () {
                expect(languagesPopoverItemView).toBeDefined();
            });

            /*   it("should listen to language filter event", function () {
             expect(mentor.publisher.eventDispatcher.attachEventListener.getCall(0).agrs[0]).toBe(mentor.publisher.events.LANGUAGE_FILTER_APPLIED);

             });*/

            it("should extend PopoverItemView", function () {
                expect(languagesPopoverItemView instanceof stubs.PopoverItemView).toBe(true);
            });

            it("should be able to load a language", function () {
                var toolTipChanged, spy = sinon.spy(mentor.publisher.languageTranslator, "translate");
                mockModel.set("id", "EN : English");

                languagesPopoverItemView.changeBtnLanuageToolTip = function (newLang) {
                    toolTipChanged = newLang;
                };
                languagesPopoverItemView.languageChanged = function (newLang) {
                    return true;
                };
                languagesPopoverItemView.displayContent(mockModel);
                expect(toolTipChanged).toBe("EN : English");
                expect(mentor.publisher.languageTranslator.translate.getCall(0).args[0]).toBe("EN");
                mentor.publisher.languageTranslator.translate.restore();
            });

            it("should show language key in the tooltip w.r.t locale.", function (){
                var langKey = 'LanguageJp:';
                var orgLocalize = mentor.publisher.languageTranslator.localize;
                mentor.publisher.languageTranslator.localize = function (language){
                    return langKey;
                }
                let languageBtnTooltip = languagesPopoverItemView.getLanguageBtnTooltip('JP');
                mentor.publisher.languageTranslator.localize = orgLocalize;
                expect(languageBtnTooltip).toBe("LanguageJp:JP");
            });

        });
    });
})();
