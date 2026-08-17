/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("printTemplateTest", function ()
{
    var translatorObj, printOptions = {
        mainText: "print"
    };
    beforeEach(function ()
    {
        translatorObj = window.mentor.publisher.languageTranslator;
        window.mentor.publisher.languageTranslator = {
            localize: function (str)
            {
                return "i18n_" + str;
            }
        }
    });

    afterEach(function ()
    {
        window.mentor.publisher.languageTranslator = translatorObj;
    });
    it("should be able to load projectsViewTemplate", function ()
    {
        var PrintOptionsColl = Backbone.Collection.extend({}), printOps;
        printOps = new PrintOptionsColl();
        printOps.add(printOptions);
        $.ajax("/base/s/templates/p/printTemplate.html", {async: false}).done(function (html)
        {
            expect(html).toBeDefined();
            var compiledTemplate = _.template(html)({
                className: "printCSS",
                items: [{
                    get: function (str)
                    {
                        return str;
                    }
                }],
                showTitle: false,
                showPopup: true,
                expand: true
            });

            expect(compiledTemplate.indexOf("i18n_NumberOfPages") > 0).toBeTruthy();
        }).fail(function ()
        {
            expect(false).toBeTruthy();
        });

    });
});