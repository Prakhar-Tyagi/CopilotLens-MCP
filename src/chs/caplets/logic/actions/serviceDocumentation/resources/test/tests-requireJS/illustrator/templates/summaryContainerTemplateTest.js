/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("summaryContainerTemplateTest", function ()
{
    var golden = '';
    beforeEach(function ()
    {
        previousTraslated = Utils.translate;
        Utils.translate = function (value)
        {
            return value + "_traslated";
        }
    });
    afterEach(function ()
    {
        Utils.translate = previousTraslated;
    });
    it("should be able to load summary-container.html template", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/summary-container.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html, {
                        cols: ["name"]
                    });
                    expect(compiledTemplate.indexOf('<div class="change-reports">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<table class="grid-ui summary-grid">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<th data-col="name">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<span>name_traslated</span>') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<tbody>') >= 0).toBeTruthy();

                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();

    });
});