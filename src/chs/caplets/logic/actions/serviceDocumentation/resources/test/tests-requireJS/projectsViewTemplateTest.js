/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("projectsViewTemplateTest", function ()
{

    it("should be able to load projectsViewTemplate", function ()
    {
        $.ajax("/base/s/templates/projectsViewTemplate.html", {async: false}).done(function (html)
        {
            expect(html).toBeDefined();
            var compiledTemplate = _.template(html)({
                previousTitle: "prevTitle",
                nextTitle: "nextTitle",
                project: {
                    getThumbnailPath: function ()
                    {
                        return "thumbnail_url";
                    },
                    get: function ()
                    {
                        return "";
                    }
                },
                projectTitle: "projectTitle",
                projectDescription: "des"
            });
            expect(compiledTemplate.indexOf("<object data=\"thumbnail_url\" style=\"width:100%; height:100%;\">") >0).toBeTruthy();
        }).fail(function ()
        {
            expect(false).toBeTruthy();
        });

    });
});
