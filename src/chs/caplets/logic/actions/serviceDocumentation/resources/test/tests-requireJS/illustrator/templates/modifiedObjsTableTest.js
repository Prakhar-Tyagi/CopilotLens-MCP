/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("modifiedObjectsTableTemplateTest", function ()
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
    it("should be able to load modifiedObjsTable template", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/modified-objs-table.html", {async: false}).done(
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
                                    },
                                    "old-name": {
                                        value: "old-name-value",
                                        highlighted: true,
                                        uid: "uid-nold"
                                    }
                                }
                            ],
                            cols: ["name"]
                        },

                    });
                    expect(compiledTemplate.indexOf('<td class="mergedCellTop  modified" data-objectId="uid-new">') >=
                            0).toBeTruthy();
                    expect(compiledTemplate.indexOf('name-value_traslated') >=
                            0).toBeTruthy();

                    expect(compiledTemplate.indexOf('<td class="mergedCellBottom modified"') >=
                            0).toBeTruthy();
                    expect(compiledTemplate.indexOf(' data-objectId="uid-nold">') >=
                            0).toBeTruthy();
                    expect(compiledTemplate.indexOf('old-name-value_traslated') >=
                            0).toBeTruthy();

                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();

    });
});