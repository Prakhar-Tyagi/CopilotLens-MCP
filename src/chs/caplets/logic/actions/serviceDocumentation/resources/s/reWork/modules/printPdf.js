/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global mentor, Utils, $,window, doFilter, applyLanguageFilterOnSVG*/
function printDone(appendage)
{
    LoadMask.removeLoadMask();
    if (appendage && /^[a-zA-Z0-9_\-]+$/.test(appendage)) {
        window.open("PrintPdf.html?appendage=" + appendage, '', 'width=500px,height=600px,resizable=1');
    }
}

function destroyPrintApplet(appendage)
{
    var printConnector;
    if (isHTTPProtocol()) {
        printConnector = new ConnectServletPrinter();
        printConnector.flush(appendage);
    }

}

function serializeXmlNode(xmlNode)
{
    if (typeof window.XMLSerializer != "undefined") {
        return (new window.XMLSerializer()).serializeToString(xmlNode);
    }
    else if (typeof xmlNode.xml != "undefined") {
        return xmlNode.xml;
    }
    return "";
}

function getSVG(obj)
{
    return obj.contentDocument && obj.contentDocument.documentElement;
}

var ConnectPrinter = function () {
    this.connect = function (container, html, type) {
    };
    this.flush = function (appendage) {
    };
};

var ConnectServletPrinter = function () {
    this.connect = function (container, html, type) {
        var printURL = "print";
        var language = mentor.publisher.LanguageFilteredProject.getCurrentLanguage();
        language = language ? language.toLowerCase() : "en";
        $.ajax({
            async: true,
            cache: false,
            url: printURL,
            type: 'POST',
            processData: true,
            data: {
                'name': container,
                'type': type,
                'html': html,
                'language': language
            },
            success: function (data, textStatus, XMLHttpRequest) {
                printDone(XMLHttpRequest.responseText);
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                require(['views/component/ModalDialog'], function (ModalDialog) {
                    var translator = mentor.publisher.languageTranslator;
                    var modalDialog = new ModalDialog({
                        title: translator.localize("insufficient.form.size.error.title"),
                        message: translator.localize("insufficient.form.size.error.message"),
                        implication: translator.localize("insufficient.form.size.error.implication"),
                        guidance: translator.localize("insufficient.form.size.error.guidance"),
                        primaryButton: false,
                        secondaryButton: "OK",
                        dialogFlag: mentor.publisher.modalDialogFlag.ERROR,
                        onCancelFn: function () {
                        }.bind(this)
                    });
                    modalDialog.show();
                });
                LoadMask.removeLoadMask();
            },
            contentType: 'application/x-www-form-urlencoded;charset=utf-8',
            dataType: (Utils.is_msie()) ? "text" : "html"
        });
    };

    this.flush = function (appendage) {
        var printURL = "print?delete=" + appendage;
        $.ajax({
            async: false,
            url: printURL,
            success: function () {
            },
            error: function () {
            },
            dataType: (Utils.is_msie()) ? "text" : "html"
        });
    };
};
ConnectServletPrinter.prototype = new ConnectPrinter();
mentor.publisher.PdfPrinter = (function () {
    "use strict";
    var printPDF;
    printPDF = function (container, obj) {
        var originalWidth, originalHeight, originalViewBox, svgString, svgDom, originalSVGDom, currentPanelPath,
                panelDetail, printConnector;
        if (obj && obj[0]) {
            if (getSVG(obj[0])) {
                svgString = serializeXmlNode(getSVG(obj[0]));
                svgDom = $($.parseXML(svgString));
                originalSVGDom = mentor.publisher.xmlLoader.loadXMLByAjax(obj[0].data, false, false, "xml");
                if (originalSVGDom.data) {
                    $('svg', originalSVGDom.data).each(function () {
                        originalWidth = $(this).attr('width');
                        originalHeight = $(this).attr('height');
                        originalViewBox = $(this).attr('viewBox');
                    });
                }
                svgDom.find('svg').removeAttr('width');
                svgDom.find('svg').removeAttr('height');
                svgDom.find('svg').removeAttr('viewBox');
                svgDom.find('svg').attr('width', originalWidth);
                svgDom.find('svg').attr('height', originalHeight);
                svgDom.find('svg').attr('viewBox', originalViewBox);
                svgDom.find('svg').attr('version', "1.2");
                svgDom.find('g[id=viewport]').removeAttr('transform');
                var svgDomString = serializeXmlNode($('svg', svgDom)[0]).replace("transform=''", "");
                // modifiedSvgString =
                // svgDomString.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
                svgDomString = encodeURIComponent(svgDomString);
                loadMaskForRendererApplet();
                if (isHTTPProtocol()) {
                    printConnector = new ConnectServletPrinter();
                    printConnector.connect(container, (svgDomString), "svg");
                }
            }
        }
    };

    /*getSVG = function (obj)
    {
        return obj.contentDocument && obj.contentDocument.documentElement;
    };*/

    return {
        print: function (container, obj) {
            printPDF(container, obj);
        }
    };

}());
