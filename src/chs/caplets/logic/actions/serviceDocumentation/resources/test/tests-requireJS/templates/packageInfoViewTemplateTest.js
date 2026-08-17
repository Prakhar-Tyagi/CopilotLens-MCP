/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */
describe("packageInfoViewTemplateTest", function ()
{
    it("check the Package Description", function ()
    {
        var templateLoaded = false;
        var goldenExpectedCompiledTemplate = '<div id="package-info-view" class="auto-package-info-view"> ' +
                '<div class="name auto-title-label">translatedValue</div> ' +
                '<div class="description auto-description" title="translatedValue">translatedValue</div> '+
                '<div class="open-button auto-open"><span class="Open">Open</span> </div> </div>';
        $.ajax("/base/s/templates/packageInfoViewTemplate.html", {async: false}).done(function (html)
        {
            templateLoaded = true;
            expect(html).toBeDefined();
            var Model = Backbone.Model.extend({});
            var item1 = new Model();
            item1.set({id: "123"});
            item1.set({name:"translatedValue"});
            item1.set({description:"translatedValue"});
            item1.set({projectId:"326"});

            var compiledTemplate = _.template(html)({
                selectedPackage: item1,
            });

            document.body.innerHTML = compiledTemplate;

            var scriptContent = compiledTemplate.replace(/[\s\S]*<script[^>]*>([\s\S]*?)<\/script>[\s\S]*/i, '$1').trim();
            var scriptElement = document.createElement('script');
            scriptElement.textContent = scriptContent;
            document.body.appendChild(scriptElement);

            const scriptRegex = /<script[\s\S]*?<\/script>/gi;
            var cleanedOutput = document.body.innerHTML.replace(scriptRegex, '');

            cleanedOutput = cleanedOutput.trim().replace(/(\r\n|\n|\r)/gm, "").replace(/\s\s+|\t/gm, " ");
            expect(cleanedOutput).toBe(goldenExpectedCompiledTemplate);

        }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});