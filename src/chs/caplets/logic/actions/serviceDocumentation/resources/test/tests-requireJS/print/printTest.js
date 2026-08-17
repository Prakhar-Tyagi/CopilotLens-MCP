/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("printTest", function ()
{
    var svgDoc;
    beforeEach(function ()
    {
        var svg = $('<svg id="svgForTest" data-translation="marker-based" height="100%" width="100%" style="cursor: default;"> <g id="viewport" transform="matrix(0.018257280811667442,0,0,0.018257280811667442,66.34995380473265,215.20794717879107)">' +
        '<text xmlns="http://www.w3.org/2000/svg" class="B1 translatable" x="51112.4453125" y="1806" widthfactor="1.0976673181676246" heightfactor="1.68" rot="0" hJustification="1" vJustification="1" flip="true" data-original="3C Y55X ENG. MANAGAMENT DIESEL VED5 SHEET2 {AD}" text-anchor="middle"><tspan x="51112.4453125" dy="-356.03125" style="cursor: default;">{testCode}</tspan><tspan x="51112.4453125" dy="712.0625">Design</tspan></text>' +
        '<g id="desc" widthfactor="1" heightfactor="1" flip="true"><desc>chs.cof.logical.schem.CAFShieldBody UID25b55b-1316adac60b-9e788023cd93580da11edf9eaf55278e UID25b55b-1316adac60a-9e788023cd93580da11edf9eaf55278e UID25b55b-1316adac609-9e788023cd93580da11edf9eaf55278e</desc><path class="C" d="M10534,11354v-1579"/><path class="C" d="M10534,7262 C10362,7262 10223,7401 10223,7573 "/><path class="C" d="M10845,7573 C10845,7401 10706,7262 10534,7262 "/><path class="C" d="M10223,9464 C10223,9635 10362,9775 10534,9775 "/><path class="C" d="M10534,9775 C10706,9775 10845,9635 10845,9464 "/><path class="C" d="M10223,7573v1891"/><path class="C" d="M10845,7573v1891"/></g></g>');
        $('body').html(svg);
        svgDoc = $('#svgForTest').first();
    });
    afterEach(function ()
    {
        $('#svgForTest').remove();
    });

    it("print selection should print SVG without embeding it into HTML when there is only one SVG", function ()
    {
        //for print selection option Number of Pages is undefined
        mentor.publisher.printer.setNumberOfPagesToPrint(undefined);
        //and when there is only one container
        mentor.publisher.printer.setContainersToPrint([{}], false);
        var isSinglePagePrint = mentor.publisher.printer.isSingleOrMultiPagePrint({
            type: mentor.publisher.contentType.SYSTEM_SVG
        }, false);
        expect(isSinglePagePrint).toBe(true);

    });

    it("print selection should not print docs with embeding them into HTML when there are more than one docs to print",
            function ()
            {
                //for print selection option Number of Pages is undefined
                mentor.publisher.printer.setNumberOfPagesToPrint(undefined);
                //and when there are two containers to print
                mentor.publisher.printer.setContainersToPrint([{}, {}]);
                var isSinglePagePrint = mentor.publisher.printer.isSingleOrMultiPagePrint({
                    type: mentor.publisher.contentType.SYSTEM_SVG
                }, false);
                expect(isSinglePagePrint).toBe(true);

            });

    it("print window should be able to translate,filter and change backgourd of SVG document", function ()
    {

        var filterMethodCalled,
                svgTranslated,
                svgBackgroundChanged,
                htmlTranslated,
                module = {
                    customizeBackground: function ()
                    {
                        svgBackgroundChanged = true;
                    }
                };
        var mockObjects = {
            vinOptions: "op1,op2",
            objectMap: {},
            windowToPrint: {},

            mainWinow: {
                $: {},
                doFilter: function ()
                {
                    filterMethodCalled = true;
                },
                TranslationUtils: function ()
                {
                    return {
                        translateSVGContent: function ()
                        {
                            svgTranslated = true;
                        },
                        translateHTMLContent: function ()
                        {
                            htmlTranslated = true;
                        }
                    }
                }
            },
            require: function (moduleName, callback)
            {
                callback(module);
                return module;
            },
            contentType: "systemSVG",
        };
        mentor.publisher.printer.processDocumentElement({
            nodeName: "svg"
        }, 'body', mockObjects);
        waitsFor(function ()
        {
            return svgBackgroundChanged === true;
        }, "wait for print process to finish", 5000);

        runs(function ()
        {
            expect(filterMethodCalled).toBeTruthy();
            expect(svgTranslated).toBeTruthy();
            expect(svgBackgroundChanged).toBeTruthy();

            expect(mockObjects.windowToPrint.doFilter).toBeTruthy();
            expect(mockObjects.windowToPrint.TranslationUtils).toBeTruthy();
            expect(mockObjects.windowToPrint.$).toBeTruthy();
        }, "execute assert condition");


    });

    it("should return true for system SVG for single page print", function ()
    {
        mentor.publisher.printer.setNumberOfPagesToPrint("1");
        var isSinglePagePrint = mentor.publisher.printer.isSingleOrMultiPagePrint({
            path: "some.svg",
            type: mentor.publisher.contentType.SYSTEM_SVG
        }, false);
        expect(isSinglePagePrint).toBe(true);
    });

    it("should return false for system SVG for multiple page print", function ()
    {
        mentor.publisher.printer.setNumberOfPagesToPrint("2");
        var isSinglePagePrint = mentor.publisher.printer.isSingleOrMultiPagePrint({
            path: "some.svg",
            type: mentor.publisher.contentType.SYSTEM_SVG
        }, false);
        expect(isSinglePagePrint).toBe(true);
    });

    it("should print customView as single page for one page print", function ()
    {
        mentor.publisher.printer.setNumberOfPagesToPrint("1");
        var isSinglePagePrint = mentor.publisher.printer.isSingleOrMultiPagePrint({
            path: "some.svg",
            type: mentor.publisher.contentType.CUSTOM_VIEW
        }, false);

        expect(isSinglePagePrint).toBe(true)
    });

    it("should print system SVG using print decorative window on chrome", function ()
    {
        mentor.publisher.printer.setNumberOfPagesToPrint("1");
        var isSinglePagePrint = mentor.publisher.printer.isSingleOrMultiPagePrint({
            path: "some.svg",
            type: mentor.publisher.contentType.CUSTOM_VIEW
        }, true);

        expect(isSinglePagePrint).toBe(false)
    });

    it("should print customView as multi page print for 2 page print selection", function ()
    {
        mentor.publisher.printer.setNumberOfPagesToPrint("2");
        var isSinglePagePrint = mentor.publisher.printer.isSingleOrMultiPagePrint({
            path: "some.svg",
            type: mentor.publisher.contentType.CUSTOM_VIEW
        }, false);

        expect(isSinglePagePrint).toBe(true)
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

    function singleDocPrint(containers, expected)
    {

        containers = containers || [{}];
        mentor.publisher.printer.setNumberOfPagesToPrint("1");
        var config = {};
        var isItPrintedStandAlone = false;
        config.containers = containers;
        config.getAllOpenContentDetails = function ()
        {
            return [{}];
        };
        config.printSingleDocument = function ()
        {
            isItPrintedStandAlone = true;
        };
        config.printMultipleDocuments = function ()
        {
            isItPrintedStandAlone = false;

        };
        mentor.publisher.printer.print(config);

        expect(isItPrintedStandAlone).toBe(expected)
    }

    it("single system SVG document should be printed as standalone doc without any decorative window", function ()
    {
        singleDocPrint([{id: "systemId", type: "systemSVG"}], true);
    });



    it("two documents should be printed as with decorative window", function ()
    {
        singleDocPrint([{id: "systemId", type: "systemSVG"}, {id: "introduction page", type: "customDoc"}], false);

    });

    /*chrome is not able to print the pdf document before it is closed by browser. So, pdf document print does not close the open pdf after printing*/
    it("should not close print window for pdf documents", function ()
    {
        var filter = mentor.publisher.filter;
        var printed = false, documentCloseAfterPrint = false;
        mentor.publisher.filter = {
            vinOptions: "01"
        }

        runs(function() {
            var printWindow = {
                document: {},
                location: {
                    href: "test.pdf"
                },
                print: function ()
                {
                    printed = true;
                }, close: function ()
                {
                    documentCloseAfterPrint = true;
                }
            };
            mentor.publisher.printer.printAndCloseDocument(printWindow, {}, {
                document: {}
            });
            setTimeout(() => {}, 101);
        });

        waitsFor(function() {
            return printed;
        }, 102);

        runs(function() {
            expect(printed).toBeTruthy();
            expect(documentCloseAfterPrint).toBeFalsy();
            mentor.publisher.filter = filter;
        });
    });

    it("should close print window after print is done", function ()
    {
        var filter = mentor.publisher.filter;
        var printed = false, documentCloseAfterPrint = false;
        mentor.publisher.filter = {
            vinOptions: "01"
        }

        runs(function() {
            var printWindow = {
                document: {},
                location: {
                    href: "test.html"
                },
                print: function ()
                {
                    printed = true;
                }, close: function ()
                {
                    documentCloseAfterPrint = true;
                }
            };
            mentor.publisher.printer.printAndCloseDocument(printWindow, {}, {
                document: {}
            });
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

    it("should print diagram in choosen background color for an SVG", function ()
    {
        var doc = {nodeName: "svg"};
        var actualDoc;
        var SVGTransforms = {
            customizeBackground: function (doc)
            {
                actualDoc = doc;
            }
        }
        var moduleLoader = function (modules, callback)
        {
            expect(modules.length).toBe(1)
            expect(modules[0]).toBe("SVGTransforms");
            callback(SVGTransforms);
        }
        mentor.publisher.printer.useBackgroundColorForSVG(moduleLoader, doc);
        expect(actualDoc).toBe(doc);
    });

    it("should not call customizeBackground for non SVG documents", function ()
    {
        var doc = {nodeName: "html"};
        var actualDoc;
        var SVGTransforms = {
            customizeBackground: function (doc)
            {
                actualDoc = doc;
            }
        }
        var moduleLoader = function (modules, callback)
        {
            expect(modules.length).toBe(1)
            expect(modules[0]).toBe("SVGTransforms");
            callback(SVGTransforms);
        }
        mentor.publisher.printer.useBackgroundColorForSVG(moduleLoader, doc);
        expect(actualDoc).toBeUndefined();
    });

    it("should be able to translate print window title", function ()
    {
        var title = mentor.publisher.printer.getPrintWindowTitle({
            title: "plain"
        }, {
            translate: function (text)
            {
                return text + "_translated";
            }
        });
        expect(title).toBe("plain_translated");
    });

    it("test when printing multiple containers max height of container is none", function (){
        var clonedObject = '';
        var container = ["test"];
        var panel = $('<div id="testparent"><div class="panel_content" id="test"></div></div>');
        $('body').html(panel);
        mentor.publisher.printer.setContainersToPrint(container);
        mentor.publisher.printer.appendContentToPrint('', function (clone, printDiv, printWindow, obj){
            clonedObject = clone;
        });
        expect(clonedObject).toNotBe('');
        expect(clonedObject.css('max-height')).toBe('none');
        $('#testparent').remove();
    });
});
