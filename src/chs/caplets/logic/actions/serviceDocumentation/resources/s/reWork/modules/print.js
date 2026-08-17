/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, Utils, $,window, doFilter, applyLanguageFilterOnSVG*/
mentor.publisher.printer = (function ()
{
    "use strict";
    var printWindow,
            openPrintWindow,
            printButtonClickHandler,
            printChoiceClickHandler,
            containers = [],
            addTitle,
            postProcessing,
            noOfPagesToPrint,
            createFourPageSVGPrintForIE,
            getWindowTitle;

    var getContainerType,
            getDocumentElement,
            openLocation,
            print,
            processDocumentElement;
    var kPrintWindowName = 'PublisherViewer'; // Constant, IE doesn't allow for spaces in the window name.

    createFourPageSVGPrintForIE = function (objectData)
    {
        var svgPath = $("object", objectData).attr('data'), container = $("<div></div>");
        require(["text!templates/p/printInFourPages.html", "underscore"], function (template, underscore)
        {

            var html = underscore.template(template)({
                path: svgPath
            });
            appendToPrintWindow(container.append($(html)));

        });

    };

    function appendToPrintWindow(html)
    {
        printWindow.appendContent(html, postProcessing);
    };

    openPrintWindow = function (printHTMLFile)
    {

        printWindow = window.open(printHTMLFile, kPrintWindowName, 'width=1024,height=768,resizable=yes');
    };

    openLocation = function (location)
    {
        location = location || "";
        if (location.toLowerCase().endsWith(".pdf")) {
            location = location.replace("pdfjs/web/viewer.html?file=../", "");
        }
        printWindow.location.href = location;
    };
    function getSizeFromSVGViewBox(viewBox)
    {
        var coordinates = [];
        var splitBySpace = viewBox.split(" ");
        for (var partOfViewBoxIndex in splitBySpace) {
            var partOfViewBox = splitBySpace[partOfViewBoxIndex];
            if (partOfViewBox) {
                var splitByComma = partOfViewBox.split(",");
                for (var viewBoxPartIndex in splitByComma) {
                    var part = splitByComma[viewBoxPartIndex];
                    if (part) {
                        part = part.trim();
                        coordinates.push(part);
                    }

                }
            }
        }
        if (coordinates.length == 4) {
            return [coordinates[2], coordinates[3]];
        }
    }

    function setSizeForMultipleDocumentPrint(clonedPanel, originalObject)
    {
        var boundingRect,
                offsetHeight,
                clonedObject;

        clonedObject = $('object', clonedPanel);
        if (clonedObject) {
            clonedObject.attr('style', 'width:1024px;height:768px');
        }
    };

    function appendContentToPrint(panelDetail, customziedPrintSettins)
    {
        var container, clone, obj, printDiv = $(
                '<div class="page-break" style="width: 100%;page-break-after: always"></div>'),
                panel;
        if (containers.length === 0) {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
            //before doing printing , filter SVGs
            //reports need not be filtered as they are already filtered
            //printing takes place inside this method
            postProcessing();
            return;
        }
        container = containers.pop();
        panel = $('.panel_content', $('#' + container).parent());
        clone = panel.clone();
        clone.css('max-height','none');
        clone.height('100%');
        obj = $('object', panel);
        customziedPrintSettins(clone, printDiv, printWindow, obj);
        if (panelDetail) {
            addTitle(printWindow.document, panelDetail);
        }
    };

    function getContentType(content)
    {
        return (content.get && content.get('type')) || content.type;
    }

    function getPathAttribute(panelDetail)
    {
        return (panelDetail.get && panelDetail.get('path')) || panelDetail.path;
    }

    function isSVGDocument(path)
    {
        return (path && path.indexOf(".svg") > -1);
    }

    /**
     * all the SVGs, whether custom or System SVGs, can be printed by opening the SVG directly in a separate window.
     * Only chrome needs to embed SVG in HTML document
     * On IE, If it is Systems SVG and needs to be printed on 2 or 4 pages then it returns false.
     *
     *
     * @param panelDetail
     * @returns {boolean}
     */
    function canDocumentBePrintedStandAloneWithoutAnyDecoration(panelDetail, singlePageForChrome)
    {
        //singlePageForChrome = singlePageForChrome || singlePageSVGPrintForChrome();
        var openStandAloneDocForPrinting = true;
        var path = getPathAttribute(panelDetail);
        var type = getContentType(panelDetail);
        if (isSVGDocument(path) ||
                type && type === mentor.publisher.contentType.SYSTEM_SVG) {
            /**
             * Chrome does not print correctly and needs different settings
             */
            if (singlePageForChrome) {
                openStandAloneDocForPrinting = false;
            }
            else if (isIEOrEdge() && noOfPagesToPrint && noOfPagesToPrint !== "1") {
                openStandAloneDocForPrinting = false;
            }
        }
        return openStandAloneDocForPrinting;
    };

    /**
     * Method to print single document or multiple documents.
     *
     * 1) determines whether print is for single document or for multiple docs
     *
     * 2) single docs are printed as stand alone documents.
     * e.g. if System SVG is printed then it will be opened in a seperate window as a SVG document (not as a HTML),
     * SVG window is then post processed to filter, translate and set background color
     * And finally print command will be fired for the window.
     *
     *
     * 3) If print is for multiple documents or for system SVG on more than one page then it is handled by
     * printMultipleDocuments method
     *
     */
    print = function (config)
    {
        var config = config || {};
        var getAllOpenDocs = config.getAllOpenContentDetails || mentor.publisher.contentArea.getAllOpenContentDetails;
        var printOneDoc = config.printSingleDocument || printSingleDocument;
        var pmulDocs = config.printMultipleDocuments || printMultipleDocuments;
        var openPanels = config.containers || containers;

        var singleDocAndCanBePrintedAsStandAloneWithoutDecoration = true, panelDetail;
        var openContentDeatils = getAllOpenDocs();
        var selectedPanelsToPrint = openPanels;
        if (selectedPanelsToPrint.length > 1) {
            singleDocAndCanBePrintedAsStandAloneWithoutDecoration = false;
        }
        else {
            panelDetail = openContentDeatils[containers[0]];
            if (panelDetail) {
                singleDocAndCanBePrintedAsStandAloneWithoutDecoration =
                        canDocumentBePrintedStandAloneWithoutAnyDecoration(panelDetail, singlePageSVGPrintForChrome());
            }
        }

        if (singleDocAndCanBePrintedAsStandAloneWithoutDecoration) {
            printOneDoc(panelDetail);
        }
        else {
            pmulDocs(panelDetail);
        }
    };

    function addPrintSupportForBackgroundGraphics(printWindow)
    {
        let styleEle = printWindow.document.createElement('style');
        let css = printWindow.document.createTextNode('@media print { body { ' +
                '-webkit-print-color-adjust: exact;	-moz-print-color-adjust: exact;	' +
                '-ms-print-color-adjust: exact; print-color-adjust: exact; }	}');
        styleEle.appendChild(css);
        printWindow.document.head.appendChild(styleEle);
    }

    function printAndCloseDocument(printWindow, container, panelDetail, config /*for passing default for UT*/)
    {
        processDocumentElement(printWindow.document.documentElement, container, config);
        addTitle(printWindow.document, panelDetail);
        if(printWindow.location.href.indexOf(".htm") > 0 && printWindow.document && printWindow.document.head) {
            addPrintSupportForBackgroundGraphics(printWindow);
        }
        setTimeout(function ()
        {
            printWindow.print();
            if (printWindow.location.href.indexOf(".pdf") < 0) {
                printWindow.close();
            }
        }, 100);
    };

    /**
     * System SVGs, harness diagram SVGs, location views SVGs and custom SVG files are printed here
     *
     * @param objects
     */
    function printAsStandAloneWithoutDecoration(objects)
    {
        var src = $(objects[0]).attr('data');
        if(objects[0].contentDocument && objects[0].contentDocument.location && objects[0].contentDocument.location.href) {
            src = objects[0].contentDocument.location.href;
        }
        if (!src) {
            $.each($('param', $(objects[0])), function (index, value)
            {
                if ($(value).attr('name') === 'src') {
                    src = $(value).attr('value');
                    return false;
                }
            });
        }
        openLocation(src);
    }

    function canItBePrintedAsStandAloneDocument(objects, contentType)
    {
        var p = mentor.publisher;
        return objects && objects[0] && !(contentType === p.contentType.CONNECTOR_FACE_VIEW);
    }

    function printSingleDocument(panelDetail)
    {
        var container = containers.pop();
        var panel = $('.panel_content', $('#' + container).parent());
        var objects = $('object', panel);

        if (canItBePrintedAsStandAloneDocument(objects, getContentType(panelDetail))) {
            printAsStandAloneWithoutDecoration(objects);
        }
        else {
            printWindow.appendContent(panel.clone()); // Reports & faceviews
        }

        setTimeout(function ()
        {
            printAndCloseDocument(printWindow, container, panelDetail);
        }, 500);
    };
    getWindowTitle = function (printDetail, translator)
    {
        translator = translator || Utils;
        return translator.translate(printDetail.get && printDetail.get("title") ? printDetail.get("title") :
                printDetail.title);
    };

    addTitle = function (doc, printDetail)
    {
        var title;
        if (printDetail) {
            title =
                    getWindowTitle(printDetail) + ", " + document.title;

            if (title) {
                /**
                 faceview title is a html string, extract the connector info
                 */
                if (title.indexOf("faceViewPathInfo") > -1) {
                    var htmlText = $("<div></div>").html(title);
                    title = $(".faceViewPathInfo", htmlText).html();
                }
                $(doc).attr("title", title);
            }
        }
    };

    function twoPageSVGPrintForIE(clone, printDiv, printWindow)
    {
        printDiv.append(clone);
        printWindow.appendContent(printDiv, false, "table1");
        printWindow.appendContent(printDiv.clone(), postProcessing, "table2");
    }

    function multiDocsPrintSettings(clone, printDiv, printWindow, obj)
    {

        if (obj && obj[0]) {
            setSizeForMultipleDocumentPrint(clone, obj[0]);
        }
        printDiv.append(clone);
        printWindow.appendContent(printDiv, function () {
            appendContentToPrint("", customziedPrintSettings)
        });
    }

    function singleSVGPrintSettingsForChrome(clonedObject, printDiv, printWindow)
    {
        var svgViewBox = $(clonedObject).attr("data-svg-viewBox");
        if (svgViewBox) {
            var widthAndHeight = getSizeFromSVGViewBox(svgViewBox);
            if (widthAndHeight && widthAndHeight.length == 2) {
                var width = parseFloat(widthAndHeight[0].trim());
                var height = parseFloat(widthAndHeight[1].trim());
                $(clonedObject).attr("width", width);
                $(clonedObject).attr("height", height);
            }
        }

        printDiv.append(clonedObject);
        printWindow.appendContent(printDiv, function () {
            appendContentToPrint("", customziedPrintSettings)
        });
    }

    function isIEOrEdge()
    {
        return navigator.userAgent.match(/msie/i) || navigator.userAgent.match(/Trident/i) ||
                navigator.userAgent.match(/Edge/i);
    }

    function is_moz()
    {
        var is_moz = navigator.userAgent.match(/.*Firefox.*/);
        return is_moz;
    }

    function is_safari()
    {
        var is_safari = navigator.userAgent.match(/Safari/i);
        var isChromiumOrChrome = navigator.userAgent.match(/Chromium|Chrome/i);
        return is_safari && !isChromiumOrChrome;
    }

    var multiDocsPrint = false;
    var customziedPrintSettings;

    function singlePageSVGPrintForChrome()
    {
        return !isIEOrEdge() && !is_moz() && !is_safari() && multiDocsPrint === false;
    }

    function multiPageSVGPrintForIE()
    {
        return isIEOrEdge() && noOfPagesToPrint;
    }

    /**
     * Determines different page settings for print
     *
     * @param panelDetail
     */
    function printMultipleDocuments(panelDetail)
    {
        if (multiPageSVGPrintForIE()) {
            if (noOfPagesToPrint === "4") {
                customziedPrintSettings = createFourPageSVGPrintForIE;
            }
            else if (noOfPagesToPrint === "2") {
                customziedPrintSettings = twoPageSVGPrintForIE;
            }
        }
        // Single SVG Print for chrome
        else if (singlePageSVGPrintForChrome()) {
            customziedPrintSettings = singleSVGPrintSettingsForChrome
        }
        else {
            // multiple document print
            customziedPrintSettings = multiDocsPrintSettings;
        }

        appendContentToPrint(panelDetail, customziedPrintSettings);
    };

    function filterSVGUsingOptions(isCustomContent, doc, vinOptions, windowToPrint, objectToOptionMap, mainWinow)
    {
        if (!isCustomContent) {
            if ($('svg>g', doc)[0]) {
                $('svg>g', doc)[0].id = "viewport";
            }

            if (vinOptions) {
                windowToPrint.mentor = {publisher: {dataLoader: {}}};
                windowToPrint.mentor.publisher.dataLoader.objectMap = objectToOptionMap;
                if (!windowToPrint.doFilter) {

                    windowToPrint.doFilter = mainWinow.doFilter;
                }
                windowToPrint.doFilter(doc, vinOptions);
            }
        }
    }

    function translateSVG(windowToPrint, mainWinow, doc, isCustomContent)
    {
        if (!windowToPrint.TranslationUtils) {
            windowToPrint.TranslationUtils = mainWinow.TranslationUtils
        }

        var translator = (windowToPrint.TranslationUtils && windowToPrint.TranslationUtils(windowToPrint.$)) ||
                TranslationUtils($);
        if (translator) {
            var rootTag = doc.nodeName.toLocaleLowerCase();
            if (rootTag === "svg") {
                translator.translateSVGContent(doc, isCustomContent);
            }
            else if (rootTag === "html") {
                translator.translateHTMLContent(doc);
            }
        }
    }

    function useBackgroundColorForSVG(moduleLoader, doc)
    {
        moduleLoader(["SVGTransforms"], function (SVGTransforms)
        {
            if (doc.nodeName.toLocaleLowerCase() === "svg") {
                SVGTransforms.customizeBackground(doc);
            }
        });

    }

    processDocumentElement = function (doc, container,
            config /*for passing default for UT*/)
    {
        config = config || {};
        var windowToPrint = config.windowToPrint /* UT will pass mock value*/ || printWindow;
        var moduleLoader = config.require || require;
        var mainWinow = config.mainWinow || window;
        var vinOptions = config.vinOptions || mentor.publisher.filter.vinOptions;
        var objectToOptionMap = config.objectMap || mentor.publisher.dataLoader.objectMap;
        if (!doc) {
            return;
        }
        if(doc.baseURI && doc.baseURI.indexOf(".htm") > 0) {
            doc.querySelectorAll('[loading="lazy"]').forEach((img) => {
                img.scrollIntoView();
            });
        }

        $(doc).removeAttr("height");
        $(doc).removeAttr("width");

        var containerType = config.contentType /* UT will pass mock value*/ || getContainerType(container);
        var isCustomContent = !((containerType === 'systemSVG') || (containerType === 'connectorFaceView'));
        if (!windowToPrint.$) {
            windowToPrint.$ = mainWinow.$;
        }
        filterSVGUsingOptions(isCustomContent, doc, vinOptions, windowToPrint, objectToOptionMap, mainWinow);
        translateSVG(windowToPrint, mainWinow, doc, isCustomContent);
        useBackgroundColorForSVG(moduleLoader, doc);
    };

    getDocumentElement = function (obj)
    {
        try {
            var docElement = obj.contentDocument && obj.contentDocument.documentElement;
        }
        catch (e) {

        }
        return docElement;
    };

    getContainerType = function (container)
    {
        var containerDetails = mentor.publisher.contentArea.getAllOpenContentDetails()[container];
        return containerDetails && (containerDetails.type || (containerDetails.get && containerDetails.get('type')));
    };

    postProcessing = function ()
    {
        var objects = $('object', printWindow.document) || [],
                k;

        for (k = 0; k < objects.length; k = k + 1) {
            var container,
                    doc,
                    object;

            object = objects[k];
            container = $(object).closest(".panel_content").attr("id");

            doc = getDocumentElement(object);
            processDocumentElement(doc, container);
        }
        printWindow.printAndClose();
    };

    printButtonClickHandler = function (event)
    {
        var printChoiceItems = [], containerToPrint = $(event.target).parent().parent();
        printChoiceItems.push({
            mainText: mentor.publisher.languageTranslator.localize("Print"),
            id: mentor.publisher.constants.print,
            container: containerToPrint
        });
        printChoiceItems.push({
            mainText: mentor.publisher.languageTranslator.localize("PrintSelection"),
            id: mentor.publisher.constants.printSelection
        });
        var currentTarget = event.currentTarget;
        var clientY = currentTarget ? (currentTarget.offsetTop + currentTarget.offsetHeight) : event.clientY;
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_PRINT_POPUP, {
            x: event.clientX || x,
            y: clientY || y,
            models: printChoiceItems
        });
        event.stopPropagation();
    };

    printChoiceClickHandler = function (event)
    {
        var k = 0, contentPanelItems = [], openViews = mentor.publisher.contentArea.getAllOpenContentDetails(), mainText, id, url, type,viewType;
        if (openViews) {
            for (k in openViews) {
                if (openViews.hasOwnProperty(k) && openViews[k]) {
                    viewType = openViews[k].type;
                    if (viewType === mentor.publisher.contentType.THREE_D_XML ||
                            viewType === mentor.publisher.contentType.JT_3D ||
                            viewType === mentor.publisher.contentType.RA_3D) {
                        continue;
                    }
                    mainText = openViews[k].title || (openViews[k].get ? openViews[k].get("title") : '');
                    id = openViews[k].path || (openViews[k].get ? openViews[k].get("path") : '');
                    url = openViews[k].path || (openViews[k].get ? openViews[k].get("path") : '');
                    type = k;
                    contentPanelItems.push({mainText: mainText, id: id, url: url, type: type, align: 'left'});
                }
            }
        }
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_PANELS_TO_PRINT_POPUP,
                {
                    x: event.clientX || x, y: event.clientY ||
                y, models: contentPanelItems
                });
    };

    return {
        //for UT
        processDocumentElement: processDocumentElement,
        //for UT to mock containers
        setContainersToPrint: function (containersToPrint)
        {
            containers = containersToPrint;
        },
        print: print,
        printAndCloseDocument: printAndCloseDocument,

        //for UT
        useBackgroundColorForSVG: useBackgroundColorForSVG,

        //for UT
        appendContentToPrint: appendContentToPrint,

        getDocumentElement: getDocumentElement,
        //only for UTs
        setNumberOfPagesToPrint: function (printPages)
        {
            noOfPagesToPrint = printPages;
        },

        isSingleOrMultiPagePrint: canDocumentBePrintedStandAloneWithoutAnyDecoration,

        printSingleDocument: printSingleDocument,

        getPrintWindow: function ()
        {
            return printWindow;
        },
        initiatePrinting: function (selectedViews, noOfPages)
        {
            var printHTML = "Print.html";
            noOfPagesToPrint = noOfPages
            containers = selectedViews;
            multiDocsPrint = containers.length > 1;
            if (containers.length === 1) {
                var panel = $('.panel_content', $('#' + containers[0]).parent());
                var obj = $('object', panel);

                //if only one panel is choosen and the panel has SVG, then print to PDF
                if (mentor.publisher.contentArea.getAllOpenContentDetails()[containers[0]]) {
                    var panelDetail = mentor.publisher.contentArea.getAllOpenContentDetails()[containers[0]];
                    var currentPanelPath = (panelDetail.get && panelDetail.get('path')) || panelDetail.path;
                    var contentType = (panelDetail.get && panelDetail.get('path')) || panelDetail.type;
                    if ((currentPanelPath && currentPanelPath.indexOf(".svg") > -1) ||
                            contentType === mentor.publisher.contentType.SYSTEM_SVG) {
                        if (mentor.publisher.config['print-method'] === 'pdf') {
                            mentor.publisher.PdfPrinter.print(containers[0], obj);
                            return;
                        }
                        else if (mentor.publisher.config['print-method'] === 'html' && Utils.is_msie()) {
                            printHTML = "multiplePagesPrint.html"
                        }
                        else {
                            printHTML = "Print.html"
                        }
                    }
                }
            }
            openPrintWindow(printHTML);
        },
        close: function ()
        {
        },
        printButtonClickHandler: function (event)
        {
            return printButtonClickHandler(event);
        },
        printSelectionClickHandler: function (event)
        {
            return printChoiceClickHandler(event);
        },
        filterSVGAndFirePrint: function ()
        {
            postProcessing();
        },

        getPrintWindowTitle: function (printWindowObj, translator)
        {
            return getWindowTitle(printWindowObj, translator);
        }
    };

}());
