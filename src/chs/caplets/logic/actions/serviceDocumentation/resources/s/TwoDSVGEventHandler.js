var TwoDSVGEventHandler = function () {
    this.handleEvent = function (evt) {
        //If no target was hit, exit
        //If they've clicked on the div...
        if (this.doPan === 'pan') {
            this.doPan = false;
            return;
        }
        var currentNode = evt.target;
        if (evt.target.nodeName.toLowerCase() == 'div') {
            window.crossHighlightHandler.flushZoomedViews(this);
            return;
        }
        evt.stopPropagation();
        var eventName = evt.type;
        var clientX = evt.clientX;
        var clientY = evt.clientY;
        var altKey = evt.altKey;
        this.doHandleEvent(eventName, clientX, clientY, altKey, currentNode);
    };

    this.doHandleEvent = function (eventName, clientX, clientY, altKey, currentNode) {
        var objDataArray = new Array();
        var svgElementPositionArray = new Array();
        var changedElements = new Set();
        var selectedObjects = new Array();
        var allObjects = new Array();
        if (eventName == 'mouseenter' || eventName == 'mousemove') {
            //            this.bumpHighlightStack();
            this.bumpHighlightStack();
            this.resetAttributesAndStack();
            selectedObjects = this.getObjectsInSelection(currentNode,
                    mentor.publisher.colors[mentor.publisher.constants.orangeColorMsg], svgElementPositionArray,
                    changedElements, true);
            allObjects = flattenMapValues(selectedObjects);
            if (selectedObjects.size > 0) {
                window.crossHighlightHandler.triggerHighLightingfrom2DSVG(window.self, allObjects,
                        this.svgContainerId);
            }
        }
        else if (eventName == 'click') {
            window.isSVGClick = true;
            this.bumpHighlightStack();
            this.resetAttributesAndStack();
            selectedObjects = this.getObjectsInSelection(currentNode,
                    mentor.publisher.colors[mentor.publisher.constants.redColorMsg], svgElementPositionArray,
                    changedElements, true);
            allObjects = flattenMapValues(selectedObjects);
            if (svgElementPositionArray.length > 0) {
                this.setHighLightMap(svgElementPositionArray);
            }
            if (selectedObjects.size > 0) {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.ALT_CLICK_TRIGGERED,
                        {altKey: altKey});
                var uidToHighlight = allObjects[0].connId;
                var x = clientX + $('#' + this.svgContainerId).offset().left;
                var y = clientY + $('#' + this.svgContainerId).offset().top;
                var keys = [];
                selectedObjects.forEach(function (value, key) {
                    keys.push(key);
                });
                var firstText = _.reduce(keys, function (a, b) {
                    return a + ',' + b
                });
                display2DViewsAttributes(firstText, x, y, uidToHighlight, selectedObjects);
                //RenderConnectivityHandler.setAltClick(false);
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.ALT_CLICK_TRIGGERED,
                        {altKey: false});
                window.crossHighlightHandler.triggerHighLightingfrom2DSVG(window.self, allObjects,
                        this.svgContainerId);
            }
        }
        if (selectedObjects.size == 0) {
            this.resetAttributesAndStack();
            window.crossHighlightHandler.flushZoomedViews(this);
        }
    };

    this.forElements = function (parent, selector, filter, apply) {
        parent.find(selector)
                .filter(filter)
                .each(apply);
    };
    //highlights only the closest g
    this.getHighlightAncestorGroup = function (color, svgElementPositionArray, changedElements) {
        var handler = this;
        return function () {
            var closestGroupElement = $(this).closest('g').not('#viewport');
            handler.changeStyle(closestGroupElement, color, svgElementPositionArray, changedElements);
        };
    };
    //highlights only the text element even if it part of a group
    this.getHighlightText = function (color, svgElementPositionArray, changedElements) {
        var handler = this;
        return function () {
            var textElement = $(this);
            handler.changeStyle(textElement, color, svgElementPositionArray, changedElements);
        };
    };

    this.doUIDBasedHighlighting = function (uid, color, svgElementPositionArray, changedElements) {
        var text = mentor.publisher.locationViews.getLocationViewByObjectId(uid), handler = this;
        var descFilter = function () {
            var descVal = $(this).text();
            return descVal.indexOf(uid) != -1;
        };
        var parent = $(this.root);
        this.forElements(parent, 'g desc', descFilter,
                handler.getHighlightAncestorGroup(color, svgElementPositionArray, changedElements));
        if (text) {
            this.doTextBasedHighlighting(text, color, svgElementPositionArray, changedElements);
        }
    };

    this.doTextBasedHighlighting = function (text, color, svgElementPositionArray, changedElements) {
        var handler = this;
        var textFilter = function () {
            var textValue = $(this).text();
            var tspanValue = '';
            $(this)
                    .find('tspan')
                    .each(function () {
                        tspanValue = tspanValue + $(this).text();
                    });
            return textValue == text || tspanValue == text;
        };
        var parent = $(this.root);
        this.forElements(parent, 'text', textFilter,
                this.getHighlightText(color, svgElementPositionArray, changedElements));
        this.forElements(parent, 'g hotspot', textFilter,
                handler.getHighlightAncestorGroup(color, svgElementPositionArray, changedElements));
    };

    this.getObjectsInSelection = function (element, color, svgElementPositionArray, changedElements, doHighlight) {
        var texts = new Array(), objectData = new Map(), handler = this;
        var descFilter = function () {
            var descVal = $(this).text();
            var parts = descVal.split(' ');
            if (parts.length < 3) {
                return false;
            }
            var text = mentor.publisher.locationViews.getLocationViewByObjectId(parts[2]);
            return mentor.publisher.locationViews.doesObjectHasLinksWithin2dView((text));
        };
        var textFilter = function () {
            var text = $(this).text();
            return mentor.publisher.locationViews.doesObjectHasLinksWithin2dView((text).trim());
        };
        var handleDescElements = function () {
            var descVal = $(this).text();
            var parts = descVal.split(' ');
            if (parts.length < 3) {
                return;
            }
            var text = mentor.publisher.locationViews.getLocationViewByObjectId(parts[2]);
            texts.push(text);
            if (doHighlight) {
                handler.getHighlightAncestorGroup(color, svgElementPositionArray, changedElements).apply(this);
            }
        };
        var handleHotspotElements = function () {
            var text = $(this).text();
            texts.push(text);
            if (doHighlight) {
                handler.getHighlightAncestorGroup(color, svgElementPositionArray, changedElements).apply(this);
            }
        };
        var hasChildOfType = function (tag) {
            return function () {
                return $(this).children(tag).length > 0;
            };
        };
        //for the element , find ancestor group which has a valid description and highlight
        this.forElements(
                $(element).closest('g').filter(hasChildOfType('desc')),
                'desc',
                descFilter,
                handleDescElements);
        //for the element , find ancestor group which has a valid hotspot and highlight
        this.forElements(
                $(element).closest('g').filter(hasChildOfType('hotspot')),
                'hotspot',
                textFilter,
                handleHotspotElements);
        //for the element if is text, find ancestor group which has a valid description and highlight
        $(element).filter('text').filter(textFilter).each(function () {
            var text = $(this).text();
            $(this)
                    .find('tspan')
                    .each(function () {
                        text = text + $(this).text();
                    });
            texts.push(text);
            if (doHighlight) {
                handler.getHighlightText(color, svgElementPositionArray, changedElements).apply(this);
            }
        });
        $(element).filter('tspan').parent('text').filter(textFilter).each(function () {
            var text = '';
            $(this)
                    .find('tspan')
                    .each(function () {
                        text = text + $(this).text();
                    });
            texts.push(text);
            if (doHighlight) {
                handler.getHighlightText(color, svgElementPositionArray, changedElements).apply(this);
            }
        });
        texts.forEach(function (text) {
            var systemPaths = handler.getSystemPaths(text.trim());
            // TODO: DOCS-8856 - This is temporary fix.
            // Removing systems with no diagrams is not a ideal fix.
            // This needs to be relooked when we fix 2d location poppover for harness.
            if (systemPaths && systemPaths.length > 0) {
                objectData.set(text, systemPaths);
            }
        });
        return objectData;
    };

    //this method does not change the style of same element twice
    //it only changes the style of children of an element, along sometimes of the element itself
    //except for the case of tspan, when the element is tspan it changes the style of parent text
    this.changeStyle = function (element, color, svgElementPositionArray, changedElements) {
        var handler = this, anyChildGElement = false, anyChildTextElement = false;
        //highlight child g elements
        $(element).children('g').each(function () {
            handler.changeStyleOfElement(this, color, svgElementPositionArray, true, changedElements);
            anyChildGElement = true;
        });
        //highlight child text and it's tspan elements
        $(element).children('text').each(function () {
            handler.changeStyleOfElement(this, color, svgElementPositionArray, true, changedElements);
            anyChildTextElement = true;
            $(this).children('tspan').each(function () {
                handler.changeStyleOfElement(this, color, svgElementPositionArray, false, changedElements);
            });
        });
        //this condition is for handling event on text/tspan/g without any children element
        if (anyChildGElement || (!anyChildGElement && !anyChildTextElement)) {
            //for text/g without any children element, we want to add to zoom objects
            $(element).not('tspan').each(function () {
                handler.changeStyleOfElement(this, color, svgElementPositionArray, true, changedElements);
            });
            //for tspan element, we do not want to add to zoom objects, instead we add parent text element to zoom objects
            $(element).filter('tspan').each(function () {
                handler.changeStyleOfElement(this, color, svgElementPositionArray, false, changedElements);
            });
            //this means that the element is tspan, parent is text, we highlight and add text to zoom objects
            $(element).filter('tspan').parent().filter('text').each(function () {
                handler.changeStyleOfElement(this, color, svgElementPositionArray, true, changedElements);
            });
        }
        //highlight any other child elements (not g, not comment and not hotspot)
        $(element).children().not('g').not('text').not('comment').not('hotspot').each(function () {
            handler.changeStyleOfElement(this, color, svgElementPositionArray, true, changedElements);
        });
    };

    this.changeStyleOfElement =
            function (element, color, svgElementPositionArray, shouldAddToZoomObjects, changedElements) {
                if (changedElements.has(element)) {
                    return;
                }
                if (shouldAddToZoomObjects) {
                    svgElementPositionArray.push(element);
                }
                this.resetableSetAttribute(element, 'stroke', color);
                this.resetableSetAttribute(element, 'fill', color);
                var strokeWidth = element.getAttributeNS(null, 'stroke-width');
                if (strokeWidth) {
                    this.resetableSetAttribute(element, 'stroke-width', strokeWidth *
                            mentor.publisher.colors[mentor.publisher.constants.strokeWidth]);
                }
                var style = $(element).attr('style');
                var newStyle = 'stroke:' + color + ";fill:" + color;
                if (Utils.notNull(style)) {
                    newStyle = style + ';stroke:' + color + ";fill:" + color;
                }
                this.resetableSetAttribute(element, 'style', newStyle);
                changedElements.add(element);
            };

    this.getSystemPaths = function (textValue) {
        //todo handle popout window
        var locationViewsInfo = mentor.publisher.locationViews.locationViewByName(textValue);
        if (typeof locationViewsInfo.systems !== "undefined") {
            return locationViewsInfo.systems;
        }
        return [];
    };

    this.getSVGDocument = function (svgContainer) {
        return $('object', svgContainer[0])[0].contentDocument.documentElement;
    };
};
TwoDSVGEventHandler.prototype = new SVGEventHandler();
TwoDSVGEventHandler.prototype.init = function (svgEl, twoDViewPartNumber) {
    if (!svgEl || !svgEl.viewBox) {
        return;
    }
    $(svgEl).off();
    this.root = svgEl;

    this.viewport = $('#viewport', svgEl)[0];
    this.viewBoxHeight = svgEl.viewBox.baseVal ? svgEl.viewBox.baseVal.height : 0;
    this.viewBoxWidth = svgEl.viewBox.baseVal ? svgEl.viewBox.baseVal.width : 0;

};

TwoDSVGEventHandler.prototype.highlightUid = function (uidToHighLight, color, notToResetFlag) {
    if (typeof (uidToHighLight) == "undefined" || uidToHighLight == null || uidToHighLight.trim() == "") {
        return;
    }
    if (!notToResetFlag) {
        this.resetAttributesAndStack();
        window.crossHighlightHandler.flushZoomedViews(this);
    }
    var svgElementPositionArray = [];
    var changedElements = new Set();
    //Highlighting the element only if uid has some value
    this.doUIDBasedHighlighting(uidToHighLight, color, svgElementPositionArray, changedElements);
    this.setHighLightMap(svgElementPositionArray, notToResetFlag);
    this.bumpHighlightStack();
};

TwoDSVGEventHandler.prototype.mouseHoverHighLight = function (description, evt) {
    this.handleEvent(evt);
};

TwoDSVGEventHandler.prototype.pressed = function (evt) {
    this.handleEvent(evt);
};
TwoDSVGEventHandler.prototype.isValidEvent = function (evt) {
    var currentNode = evt.target;
    if (evt.target.nodeName.toLowerCase() == 'div') {
        return false;
    }
    var svgElementArray = [];
    var selectedElements = new Set();
    var selectedObjects = this.getObjectsInSelection(currentNode, svgElementArray, selectedElements, false);
    return selectedObjects.size > 0;
};

TwoDSVGEventHandler.prototype.isValidElement = function (evt) {
    return this.isValidEvent(evt);
};


