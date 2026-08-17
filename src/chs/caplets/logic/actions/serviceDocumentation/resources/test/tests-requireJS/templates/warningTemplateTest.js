/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("Warning Template", function ()
{

    it("should render app name correctly", function ()
    {
        $.ajax("/base/s/templates/warningTemplate.html", {async: false})
            .done(function (html) {
                expect(html).toBeDefined();
                var compiledTemplate = _.template(html)({
                    message: "{app}"
                });

                expect(compiledTemplate.indexOf("Capital Design Explorer package") > 0).toBeTruthy();
            })
            .fail(function () {
                expect(false).toBeTruthy();
            });
    });
});