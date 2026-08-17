/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("printIntroductionTest", function ()
{
    var htmlDoc;
    beforeEach(function ()
    {
        var htmlContent = $('<div id="htmlTest"><h1>Welcome lazy</h1>\n' +
                '\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<h1>Welcome lazy</h1>\n' +
                '<ul id="footer-icons" class="noprint">\n' +
                '\t<li id="footer-copyrightico"><a href="https://wikimediafoundation.org/"><img src="/static/images/footer/wikimedia-button.png" srcset="/static/images/footer/wikimedia-button-1.5x.png 1.5x, /static/images/footer/wikimedia-button-2x.png 2x" width="88" height="31" alt="Wikimedia Foundation" loading="lazy" /></a></li>\n' +
                '\t<li id="footer-poweredbyico"><a href="https://www.mediawiki.org/"><img src="/static/images/footer/poweredby_mediawiki_88x31.png" alt="Powered by MediaWiki" srcset="/static/images/footer/poweredby_mediawiki_132x47.png 1.5x, /static/images/footer/poweredby_mediawiki_176x62.png 2x" width="88" height="31" loading="lazy"></a></li>\n' +
                '</ul>\n' +
                '\n' +
                '<h1>Welcome eager</h1>\n' +
                '<img src="/static/images/footer/poweredby_mediawiki_88x31.png" alt="Powered by MediaWiki" srcset="/static/images/footer/poweredby_mediawiki_132x47.png 1.5x, /static/images/footer/poweredby_mediawiki_176x62.png 2x" width="88" height="31" loading="eager">\n' +
                '\n' +
                '<h1>Welcome not lazy</h1>\n' +
                '<img alt="Bistorta officinalis" src="//upload.wikimedia.org/wikipedia/commons/thumb/a/ad/Bistorta_officinalis_01.JPG/300px-Bistorta_officinalis_01.JPG" decoding="async" width="300" height="454" class="mw-file-element" srcset="//upload.wikimedia.org/wikipedia/commons/thumb/a/ad/Bistorta_officinalis_01.JPG/450px-Bistorta_officinalis_01.JPG 1.5x, //upload.wikimedia.org/wikipedia/commons/thumb/a/ad/Bistorta_officinalis_01.JPG/600px-Bistorta_officinalis_01.JPG 2x" data-file-width="3505" data-file-height="5302" />\n' +
                '</div>');

        $('body').html(htmlContent);
        htmlDoc = $('#htmlTest').first();
    });
    afterEach(function ()
    {
        $('#htmlTest').remove();
    });

    it("printing should not throw exception when an document access error occured", function ()
    {
        try {
            var undefinedDocument;
            mentor.publisher.printer.getDocumentElement(undefinedDocument);
        }
        catch (e) {
            expect(true).toBeFalsy();
        }
    });

    it("should close print window after print is done", async () =>
    {
        var filter = mentor.publisher.filter, printed = false, documentCloseAfterPrint = false;
        mentor.publisher.filter = {
            vinOptions: "01"
        }
        var mockObjects = {
            vinOptions: "op1,op2",
            objectMap: {},
            windowToPrint: {},
            mainWinow: {
                $: {}
            }
        };

        runs(function() {
            var printWindow = {
                document: {
                    documentElement: htmlDoc[0]
                },
                location: {
                    href: "test.html"
                },
                print: function ()
                {
                    printed = true;
                }, close: function ()
                {
                    documentCloseAfterPrint = true;
                },
                $: $
            };
            mentor.publisher.printer.printAndCloseDocument(printWindow, {}, {
                document: {}
            }, mockObjects);
            setTimeout(() => {}, 101);
        });

        waitsFor(function() {
            return printed;
        }, 102);

        runs(function() {
            expect(printed).toBeTruthy();
            expect(documentCloseAfterPrint).toBeTruthy();
            mentor.publisher.filter = filter;
        });
    });
});
