/**
 * Created with IntelliJ IDEA.
 * User: mukumar
 * Date: 15/10/12
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
/*global assertEquals, mentor, $*/
describe("languageTranslatorTest", function ()
{
    beforeEach(function ()
    {
        var that = this;
      /*  mentor.publisher.project = {
            getId: function ()
            {
                return "dProjectId";
            },
            getObjectById: function ()
            {
                return {
                    getToolTips: function ()
                    {
                        return [];
                    }
                };
            }
        };*/
        that.orgLangLoader = mentor.publisher.languageDataLoader;
        mentor.publisher.languageDataLoader = {
            getOrderedLangList: function ()
            {
                return [
                    {"name": "en : english"},
                    {"name": "de : german"}
                ];
            },
            getViewerLanguage: function ()
            {
                return "de";
            },
            setCurrentLanguageChoice: function (choice)
            {
                that.langChoice = choice;
            },
            getLanguageDictionary: function ()
            {
                return {};
            }
        };
        this.orgTranslate = mentor.publisher.languageTranslator.translate;
        mentor.publisher.languageTranslator.translate = function (langChoice)
        {
            that.langToTranslate = langChoice;
        };

        that.orgAjax = $.ajax;
        $.ajax = function (param)
        {
            that.ajaxParam = param;
        };
        this.orgPopover = mentor.publisher.popover;
        mentor.publisher.popover = {
            initializePopover: function ()
            {

            },
            addSubPanel: function (subPanel)
            {
                that.subPanels = that.subPanels ? that.subPanels : [];
                that.subPanels.push(subPanel);
            },
            show: function ()
            {

            }
        }
    });
    afterEach(function ()
    {
        $.ajax = this.orgAjax;
        mentor.publisher.languageTranslator.translate = this.orgTranslate;
        mentor.publisher.popover = this.orgPopover;
        mentor.publisher.languageDataLoader = this.orgLangLoader;
    });
    it("test language Translation xml loaded/parsed correctly.", function ()
    {
        "use strict";
        mentor.publisher.languageTranslator.initialize();
        expect(1).toBe(this.langChoice);
        expect("de").toBe(this.langToTranslate);
        // assertEquals("", "resources/resources_de.properties", this.url);
    });
    it("test english should be the language when current viewer language is not in language xml." < function ()
    {
        "use strict";
        mentor.publisher.languageDataLoader.getViewerLanguage = function ()
        {
            return "fr"
        };
        mentor.publisher.languageTranslator.initialize();
        expect(0).toBe(this.langChoice);
        expect("en").toBe(this.langToTranslate);
        // assertEquals("", "resources/resources_de.properties", this.url);
    });
    it("test language translator object should be able to translate to given language.", function ()
    {
        "use strict";
        var passedValue, preFilterFun;
        mentor.publisher.languageTranslator.translate = this.orgTranslate;
        preFilterFun = applyLanguageFilter;
        applyLanguageFilter = function ()
        {
            passedValue = true;
        };
        mentor.publisher.languageTranslator.translate('de');
        expect(passedValue).toBeTruthy();

        $('body').append($("<div class='languageBtn'>en</div>"));
        passedValue = false;
        mentor.publisher.languageTranslator.translate();
        expect(passedValue).toBeTruthy();
        applyLanguageFilter = preFilterFun;
    });

    it("test language button tooltip key translated according to translated language", function(){

        var data = {
            'en' :"Language = language1:\n Translate = translate1",
            'de' :"Language = languageDe:\n Translate = translateDe"
        };
        mentor.publisher.languageTranslator.loadResources("en");
        this.ajaxParam.success(data.en, undefined, undefined);
        var toolTipKey = 'customtooltip-0';
        $('body').append($("<div class='languageBtn' customtooltip-0=''></div>"));
        mentor.publisher.languageTranslator.initialize('en');
        let attrVal = $('.languageBtn').attr('customtooltip-0');
        expect("language1:en").toBe(attrVal);

        mentor.publisher.languageTranslator.loadResources("de");
        this.ajaxParam.success(data.de, undefined, undefined);
        mentor.publisher.languageTranslator.initialize('de');
        attrVal = $('.languageBtn').attr('customtooltip-0');
        expect("languageDe:de").toBe(attrVal);
    });
});
