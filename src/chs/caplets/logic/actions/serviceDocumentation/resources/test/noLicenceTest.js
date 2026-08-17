describe("noLicenceTest", function () {
    it("should be able to load noLicence template", function () {
        var goldenExpectedCompiledTemplate = '<!DOCTYPE html>' +
                '<html lang="en">' +
                '<head>' +
                '    <link rel="icon" class="applicationIcon" type="image/png" sizes="32x32" href="images/CapitalDesignExplorerIcon.png">' +
                '    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">' +
                '    <meta http-equiv="X-UA-Compatible" content="IE=edge">' +
                '    <title>Capital Design Explorer</title>' +
                '    <link rel="stylesheet" type="text/css" href="styles/general.css">' +
                '    <link rel="stylesheet" type="text/css" href="styles/layout.css">' +
                '</head>' +
                '<body>' +
                '<div style="background-color: hsl(214, 39%, 30%); bottom: 0; color: #eee; left: 0; position: absolute; right: 0; top: 0;">' +
                '    <div style="margin: 25% auto; width: 100%">' +
                '        <div style="font-size: 24px; line-height: 36px; text-align: center;">' +
                '            Unable to open Capital Design Explorer package - No license found.<br>Please make sure you have a valid Capital' +
                '            Design Explorer package license.<br>' +
                '            <input type="button" onclick="refresh()" value="Try Again" width="150px"' +
                '                   style="border-radius: 5px;font-size: 14px;background: #dc6914;color: #eee;margin-top: 15px; padding: 3px 10px 2px 10px;box-shadow: 1px 2px 4px 0 rgb(0 0 0 / 30%);border: solid 1px #843f0c;"/>' +
                '        </div>' +
                '    </div>' +
                '</div>' +
                '</body>' +
                '<script type="text/javascript">' +
                '    function refresh()' +
                '    {' +
                '        window.location.href = "index.html?rand=" + Math.floor((Math.random() * 1000) + 1);' +
                '    }' +
                '</script>' +
                '</html>';

        $.ajax("/base/noLicence.html", { async: false }).done(function (html) {
            expect(html).toBeDefined();

            // Compiling the template
            var compiledTemplate = _.template(html)();

            // Removes line breaks and extra spaces for comparison
            var normalizedCompiledTemplate = compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "").replace(/\s+/g, " ");
            var normalizedGoldenTemplate = goldenExpectedCompiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "").replace(/\s+/g, " ");

            expect(normalizedCompiledTemplate).toBe(normalizedGoldenTemplate);
        }).fail(function () {
            expect(false).toBeTruthy();
        });
    });
});
