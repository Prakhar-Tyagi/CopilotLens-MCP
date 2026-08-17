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
describe("tableContainerTemplateTest", function ()
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
    it("should not render table for empty items", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/table-container.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html, {
                        items: [],
                        cols: ["name"]
                    });
                    expect($(compiledTemplate).html().trim()).toBeFalsy();

                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();

    });

    it("should make table filterable ", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/table-container.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html, {
                        items: [{
                            name: {
                                value: "value",
                                uid: "uid",
                                highlight: true
                            }
                        }],
                        sorting: {
                            col: "name",
                            order: "asc"
                        },
                        searchable: true,
                        cols: ["name"],
                        filtering: {
                            name: "name"
                        }
                    });
                    expect(compiledTemplate.indexOf('<input type="search" data-col="name" type="text"') >=
                            0).toBeTruthy();
                    expect(compiledTemplate.indexOf('alt="Search"') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('placeholder="Search"') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('value="name">') >= 0).toBeTruthy();
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });

    });

    it("should  render table for non empty items and should not create filter for non filterable table", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/table-container.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html, {
                        items: [{
                            name: {
                                value: "value",
                                uid: "uid",
                                highlight: true
                            }
                        }],
                        sorting: {
                            col: "name",
                            order: "asc"
                        },
                        searchable: false,
                        cols: ["name"]
                    });
                    expect(compiledTemplate.indexOf('<div class="change-reports">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<table class="grid-ui">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<thead>') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<tr>') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<th data-col="name">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<span>name_traslated</span>') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<span class="sort-order">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('asc') >= 0).toBeTruthy();
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();

    });
});