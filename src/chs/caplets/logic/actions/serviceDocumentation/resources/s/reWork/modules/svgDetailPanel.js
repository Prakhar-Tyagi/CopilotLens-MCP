/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, $, document, SVGEventHandler, addNewSvgEventHandler, Utils, setTimeout, LoadMask, crossHighlightHandler, applyLanguageFilterOnSVG, doFilter, applyLanguageFilterOnReport, getPluginType, ReportEventHandler, TwoDSVGEventHandler*/

mentor.publisher.svgLoader = function (scriptsPath, svgType) {
    "use strict";
    var eventHandler,
            svgContainer,
            svgLoadedFirstTime;

    function filterSVGUsingSelectedOptions(options, svgEventHandler, svgDocument)
    {
        if (options) {
            if (!(svgEventHandler instanceof TwoDSVGEventHandler)) {
                doFilter(svgDocument, options);
                return true;
            }
        }
        return false;
    };

    function setViewpotIdAndSize(svg)
    {
        var svgElement = $('svg>g', svg)[0];
        if (svgElement) {
            svgElement.id = "viewport";
        }
        var transform = $('#viewport', svg).attr('transform');
        var svgEventHandler = eventHandler;
        svgEventHandler.svgCTM = transform;
        svg.style.cssText = 'transform: translate3d(0,0,0); user-select: none; -webkit-user-select: none;';
        svgEventHandler.svgContainerId = svgContainer;
        svgEventHandler.init(svg);
        svg.setAttribute('data-containerId', svgContainer);
        addNewSvgEventHandler(svgContainer, svgEventHandler);
        $(svg).attr('height', '100%');
        $(svg).attr('width', '100%');

    }

    function getContextPath(pathname)
    {
        var paths = pathname.split("/");
        var length = paths.length;
        var context = "";
        paths.filter(function (subPath) {
            return subPath.length > 0 && subPath.indexOf(".") < 0;
        }).forEach(function (subPath) {
            context += "/" + subPath;
        });
        return context;
    }

    function loadZoomPanLibUsingPathName(pathname, scriptEl, svg, isIE)
    {
        var context = getContextPath(pathname);
        var svgPanPath = context + "/s/SVGPan.js";
        scriptEl.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', svgPanPath);
        if (!isIE) {
            svg.removeAttributeNS(null, 'viewBox');
        }
        else {
            svg.setAttributeNS(null, 'viewBox', null);
        }
    }

    function loadZoomPanLibUsingRelativePath(scriptEl, svg)
    {
        if (scriptsPath && !Utils.is_msie()) {
            scriptEl.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', scriptsPath);
            svg.removeAttributeNS(null, 'viewBox');
        }
        else if (!Utils.is_msie()) {
            scriptEl.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', '../../../../s/SVGPan.js');
            svg.removeAttributeNS(null, 'viewBox');
        }
        else {
            scriptEl.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', 's/SVGPan.js');
            svg.setAttributeNS(null, 'viewBox', null);
        }
    }

    function addZoomAndPanScripts(svg)
    {
        var scriptEl = document.createElementNS('http://www.w3.org/2000/svg', 'script')
        scriptEl.setAttributeNS(null, 'type', 'text/javascript');
        $("object", $("#" + svgContainer)).attr('data-svg-viewBox', $(svg).attr('viewBox'));
        var pathname = window.location.pathname;
        if (pathname) {
            loadZoomPanLibUsingPathName(pathname, scriptEl, svg, Utils.is_msie());
        }
        else {
            loadZoomPanLibUsingRelativePath(scriptEl, svg);
        }
        svg.appendChild(scriptEl);
        return scriptEl;
    }

    function setSVGBackground(svg)
    {
        require(["SVGTransforms"], function (SVGTransforms) {
            SVGTransforms.customizeBackground(svg);
        });
    }

    function translateSVGContent(svg, svgEventHandler)
    {
        var isCustomContent = (svgEventHandler instanceof TwoDSVGEventHandler);
        require(["TranslationUtils"], function (TranslationUtils) {
            TranslationUtils.translateSVGContent(svg, isCustomContent);
        });
    }

    function resizeSVGTofitInTheContainer(svgEventHandler)
    {
        window.crossHighlightHandler.flushZoomedViews(svgEventHandler);
        svgEventHandler.zoomLockedView();
    }

    function setSVGTransformationModel(svgEventHandler, SVGTransformModel)
    {
        var container,
                svgContainerObject,
                clientHeight,
                clientWidth,
                onTransformChange,
                path;
        var p = mentor.publisher;
        container = document.getElementById(svgEventHandler.svgContainerId);
        svgContainerObject = container.getElementsByTagName('object')[0];
        svgContainerObject.setAttribute('width', '100%');
        svgContainerObject.setAttribute('height', '100%');

        var panelSize = p.panelSize;

        clientHeight = (panelSize && panelSize[svgEventHandler.svgContainerId] &&
                panelSize[svgEventHandler.svgContainerId].height) || svgContainerObject.clientHeight ||
                $(svgContainerObject).innerHeight();
        clientWidth = (panelSize && panelSize[svgEventHandler.svgContainerId] &&
                panelSize[svgEventHandler.svgContainerId].width) || svgContainerObject.clientWidth ||
                $(svgContainerObject).innerWidth();
        path = $(svgContainerObject).attr('data');
        svgEventHandler.svgTransformModel = new SVGTransformModel({
            id: svgType,
            svgContainerId: svgEventHandler.svgContainerId,
            root: svgEventHandler.root,
            type: svgType,
            viewport: svgEventHandler.viewport,
            viewBoxWidth: svgEventHandler.viewBoxWidth,
            viewBoxHeight: svgEventHandler.viewBoxHeight,
            clientWidth: clientWidth,
            clientHeight: clientHeight,
            path: path
        });
    }

    var setBackgroundColorAndTranslateSVGContent = function (svg) {
        var svgEventHandler = eventHandler;

        setSVGBackground(svg);
        require(["SVGTransformModel"], function (SVGTransformModel) {
            setSVGTransformationModel(svgEventHandler, SVGTransformModel);
            resizeSVGTofitInTheContainer(svgEventHandler);
            translateSVGContent(svg, svgEventHandler);
        });
    }

    function highlightSelectedObjectInSVG(svgEventHandler)
    {
        var selectedObjectId = mentor.publisher.selectedSystem.get("objectId");
        if (selectedObjectId) {
            require(["SelectedObjectsStore"], function (store) {
                store.bringToFrontOnAddition(svgEventHandler.svgTransformModel);
                crossHighlightHandler.initCrossHighlight(selectedObjectId);
            });
        }
    }

    function applyConfigFilter(currentOptionExpression, svgEventHandler, svg)
    {
        if (window.opener && window.opener.mentor) {
            currentOptionExpression =
                    window.opener.mentor.publisher.configurationsManager.getVehicleConfigObject().getCurrentSelectedOptionsAsString() ||
                    window.opener.mentor.publisher.filter.vinOptions;
        }
        filterSVGUsingSelectedOptions(currentOptionExpression, svgEventHandler, svg);
    }

    function removeLoadingMessage()
    {
        setTimeout(function () {
            LoadMask.removeSVGMask();
        }, 100);
    }

    function highlightSelectedObjectAndfilterSVGElementsUsingOptions(svg)
    {
        require(["currentPackage"], function (currentPackage) {
            highlightSelectedObjectInSVG(eventHandler);
            applyConfigFilter(currentPackage.get("config"), eventHandler, svg);
            filterSVGUsingSelectedOptions(currentPackage.get("vin"), eventHandler, svg);
            removeLoadingMessage();
        });
    }

    var postProcessLoadedSVG = function (svg) {
        svgLoadedFirstTime = false;
        setViewpotIdAndSize(svg);
        addZoomAndPanScripts(svg);
        $(svg).attr("onload", function () {
            setBackgroundColorAndTranslateSVGContent(svg)
            highlightSelectedObjectAndfilterSVGElementsUsingOptions(svg);
        });
    }

    return {

        loadSVGContentHTML: function (contentHTML, svgContainerId, svgEventHandler) {
            svgEventHandler = svgEventHandler || new SVGEventHandler();
            eventHandler = svgEventHandler;
            svgContainer = svgContainerId;
            var intervalId = setInterval(function () {
                var svgElement = this.getSVGDocumentNode(svgContainerId);
                if (svgElement) {
                    this.onSVGLoad(svgElement);
                    clearInterval(intervalId);
                }
            }.bind(this), 1000);

        },
        getSVGDocumentNode: function (svgContentId) {
            var svgContainerDiv = $("#" + svgContentId), svgEl;
            svgLoadedFirstTime = true;
            if ($('object', svgContainerDiv).length === 0) {
                return;
            }
            try {
                svgEl = $('object', svgContainerDiv)[0].contentDocument &&
                        $('object', svgContainerDiv)[0].contentDocument.documentElement;
                var that = this;
                $('object', svgContainerDiv)[0].addEventListener('load', function () {
                    var svgEl = $('object', svgContainerDiv)[0].contentDocument &&
                            $('object', svgContainerDiv)[0].contentDocument.documentElement;
                    if (!svgLoadedFirstTime) {
                        that.onSVGLoad(svgEl);
                    }

                }, false);
            }
            catch (e) {
                this.removeSVGContainer(svgContainer);
            }
            return svgEl;
        },
        removeSVGContainer: function (svgConteinerId) {
            var parentContainer = $("#" + svgConteinerId).parent();
            $("#" + svgConteinerId).remove();
            $("div", parentContainer).width("100%").height("100%");
        },
        onSVGLoad: function (svgDocumentElement) {
            if (!svgDocumentElement) {
                this.removeSVGContainer(svgContainer);
            }
            else {
                postProcessLoadedSVG(svgDocumentElement);
            }
        },
        loadZoomPanLibUsingPathName: loadZoomPanLibUsingPathName,
        filterSVGByOptions: filterSVGUsingSelectedOptions

    };
};
mentor.publisher.svgDetailPanel = function () {
    "use strict";
    var svgContainerId = 'systemSVGLoadArea', svgEventHandler, svgLoader = mentor.publisher.svgLoader(), svgURL;
    return {

        loadSVG: function (contentHTML, containerID, loader, handler) {
            var svgPanel;
            svgContainerId = containerID;
            svgEventHandler = handler || new SVGEventHandler();
            svgLoader = loader || mentor.publisher.svgLoader();
            svgLoader.loadSVGContentHTML(contentHTML, svgContainerId, svgEventHandler);
        }

    };
};

var numberOfSVGsToLoad = 2;
function svgLoaded()
{
    numberOfSVGsToLoad -= 1;
    if (numberOfSVGsToLoad === 0) {
        alertMsg.removeAlertMsg();
    }
}