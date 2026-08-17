/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("tableTemplateTest", function ()
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
    it("should be able to load table template", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/table.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html, {
                        data: {
                            items: [
                                {
                                    name: {
                                        value: "name-value",
                                        highlighted: true,
                                        uid: "uid-new"
                                    }
                                }
                            ],
                            cols: ["name"]
                        },

                    });
                    expect(compiledTemplate.indexOf('<tbody>') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<tr>') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<td data-objectId="uid-new"') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('class="modified"  >name-value_traslated</td>') >= 0).toBeTruthy();

                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();

    });
});