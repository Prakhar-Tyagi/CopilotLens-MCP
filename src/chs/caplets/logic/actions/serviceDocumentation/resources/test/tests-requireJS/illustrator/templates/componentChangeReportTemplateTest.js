/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("componentTableChangeTest", function ()
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
    it("should be able to load componentTableChange template", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/change-report.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html, {
                        sections: [{
                            attributes: ["name"],
                            id: "table",
                            title: "tableTitle"
                        }]
                    });

                    expect(compiledTemplate.indexOf('div id="changeReport"') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<div id="table"') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<div id="table"') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<div class="section-header">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<div class="change-table">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf(
                                    '<span class="expand-section"><img src="images/ico_arrow_down.gif"></span>') >=
                            0).toBeTruthy();
                    expect(compiledTemplate.indexOf(
                                    '<span class="collapse-section" style="display: none"><img src="images/ico_arrow_right.gif"></span>') >=
                            0).toBeTruthy();
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();

    });
});