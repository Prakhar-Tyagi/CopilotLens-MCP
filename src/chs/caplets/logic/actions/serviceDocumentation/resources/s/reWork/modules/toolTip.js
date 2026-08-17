/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*
 This class is used to show tool tips
 */
/*global mentor, Utils, $, callFunction*/
mentor.publisher.toolTip = (function (p) {
    "use strict";
    var getToolTipHTML, appendToolTipHTML, show, showToolTipOnMouseHover, removeToolTipOnMouseLeave, display, remove,
            showName, getToolTipHTMLForName, registerTTOnButton, ttAttr = 'customtooltip-0',
            registerToolTipOnTextField, addCustomCSS;

    function convertNewlineCharToLineBreak(tooltipvalue)
    {
        if (tooltipvalue) {
            try {
                return decodeURIComponent(encodeURIComponent(tooltipvalue).replace(/%5Cn/g, '<br />'));
            }
            catch (e) {
            }
        }
        return tooltipvalue;
    }

    getToolTipHTML = function (toolTips) {
        var html = '', k;
        for (k = 0; k < toolTips.length; k = k + 1) {
            var tooltipname = toolTips[k].name || toolTips[k].getName();
            var tooltipvalue = toolTips[k].value || toolTips[k].getValue();
            var s = ':</span> &nbsp;&nbsp;&nbsp;';
            if (tooltipname) {
                html = html + '<span class="tooltipName">' +
                    (tooltipname.indexOf("{") == 0 ? Utils.translate(tooltipname) :
                        Utils.translatePlainText(tooltipname));
                if (tooltipvalue) {
                    html += s + '<span class="tooltipValue" style="white-space: pre-wrap">'
                        + convertNewlineCharToLineBreak(Utils.translate(tooltipvalue)) + '</span><br>';
                } else {
                    html += '</span>';
                }
            } else {
                if (!tooltipname) {
                    s = "</span>";
                }
                var translate = Utils.translate(tooltipname);
                var tooltipTranslated = convertNewlineCharToLineBreak(translate);
                html = html + '<span class="tooltipName">' + tooltipTranslated +
                    s +
                    convertNewlineCharToLineBreak(Utils.translate(tooltipvalue)) + '<br>';
            }
        }
        return $(html);
    };

    getToolTipHTMLForName = function (name) {
        var html = '';
        html = html + '<span class="tooltipName">' + _.escape(name) + '</span>';
        return $(html);
    };

    appendToolTipHTML = function (html, currentDoc) {
        //tooltip should also get translated when language changes
        $('.tooltip', $(currentDoc)).append(html);
    };

    display = function (x, y, currentDoc) {
        var paddingX = 25, paddingY = 20, toolTipDiv = $('.tooltip', $(currentDoc)), body = $('body', $(currentDoc));

        // x = x + paddingX;
        /**
         * check if tool tip is going off the screen
         */
        if (x + paddingX + toolTipDiv.width() > body.width()) {
            //   ////////console.log(x + paddingX + theTooltipPopover.width());
            //yes ..show tool tip to the left
            x = x - (toolTipDiv.width() + paddingX);
        }
        else {
            // show tool tip to the right
            x = x + paddingX;
        }

        y = y + paddingY;
        if (y < paddingY) {
            y = paddingY;
        }
        if (y + toolTipDiv.height() > body.height() - paddingY) {
            y = $('body').height() - (toolTipDiv.height() + paddingY);
        }
        //toolTipDiv.delay(2000).hide("fast");
        toolTipDiv.show();
        toolTipDiv.css('left', x);
        toolTipDiv.css('top', y);
        toolTipDiv.fadeIn(500);
        /* toolTipDiv[0].timer = null;
         toolTipDiv[0].timer = setTimeout(function()
         {
         remove(currentDoc);
         }, 2000);*/
    };

    showToolTipOnMouseHover = function (event) {
        var data = event.detail.detail, tt,
                x = event.detail.pageX, y = event.detail.pageY,
                container, nameSpan, valueSpan, ttObject, mainTextSpan, showToolTipAlways,
                popoutSpan, subTextSpan, currentDoc = event.currentTarget;
        if (!data) {
            return false;
        }
        /*if (!(data.get || data.toolTip)) {
         return false;
         }*/
        tt = data.get ? (callFunction(data.get("getToolTips")) ||
                (typeof data.get("getToolTips") === "function" ? data.get("getToolTips")() : "")) : "";

        if (!tt) {
            tt = data.getToolTips ? data.getToolTips() : data.toolTips;
        }

        if (!tt) {
            tt = data.attributes ? data.attributes.tooltips : "";
        }

        //1. the first case is for listitems which send their model data through the event
        //and their model has a method called getToolTips
        //2. the second condition is for events coming from components like buttons
        //their model has a entry called toolTip
        //3. the third condition is to check if the container (where the tooltip event is registered)
        //width is not sufficient for the data to be shown, this is for tooltips on attributes
        //todo what about configuration filter?
        container = $(event.detail.currentTarget);
        if (tt && tt.length > 0) {
            mainTextSpan = $('.mainText', container);
            subTextSpan = $('.subText', container);
            var toolTipMainText = $(mainTextSpan).text(), titletoolTip;
            if (toolTipMainText && toolTipMainText.length && toolTipMainText.length > 40 &&
                    diagramAsSystemsObjectFactoryImpl) {
                titletoolTip = {
                    getName: function () {
                        return "Title";
                    },
                    getValue: function () {
                        return toolTipMainText;
                    }
                };
                tt.unshift(titletoolTip);
            }
            showToolTipAlways = data.showToolTipAlways || (data.get && data.get('showToolTipAlways'));
            /*popoutSpan = $('popUp', container);
             if (mainTextSpan.width() < (mainTextSpan.width() + popoutSpan.width())) {
             show(tt, x, y);
             }*/
            //if (data.showAlways) {
            //todo more elegant way to do the below, may be the data should have some flag which
            //todo would tell the tooltip to always show the tooltip
            if (mentor.publisher.configurationsManager.isDynamicNavigationActive() || tt.length > 2 ||
                    showToolTipAlways) {
                show(tt, x, y, currentDoc);
            }
            //Note for these calcs to work correctly the element requires layout: block
            else if (mainTextSpan.length > 0 && mainTextSpan[0].offsetWidth < mainTextSpan[0].scrollWidth) {
                show(tt, x, y, currentDoc);
            }
            else if (subTextSpan && subTextSpan[0] && (subTextSpan[0].offsetWidth < subTextSpan[0].scrollWidth)) {
                show(tt, x, y, currentDoc);
            }
        }
        else if (data.toolTip) {
            tt = data.toolTip;
            if (tt) {
                showName(tt, x, y, currentDoc);
            }
        }
        else {
            nameSpan = $('.attributeName', container);
            valueSpan = $('.attributeValue', container);
            mainTextSpan = $('.mainText', container);
            subTextSpan = $('.subText', container);
            if (container && valueSpan && nameSpan) {
                if (container.width() < (valueSpan.width() + nameSpan.width())) {
                    ttObject = {};
                    tt = [];
                    ttObject.getName = function () {
                        return data.name;
                    };
                    ttObject.getValue = function () {
                        return data.value;
                    };
                    tt.push(ttObject);
                    show(tt, x, y, currentDoc);
                }
            }

        }
        if (titletoolTip && diagramAsSystemsObjectFactoryImpl) {
            tt.shift();
        }

    };

    //todo make this uniform, there are different methods presently but there should be a single piece of code which
    // can take of this
    registerToolTipOnTextField = function (container) {
        $(container).off('mouseover');
        $(container).off('mouseleave');
        $(container).off('mousemove');
        container.on("mouseover", function (event) {
            if (container[0].offsetWidth >= container[0].scrollWidth) {
                return;
            }
            event.detail = {};
            event.detail.toolTip = $(container).val();
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP, event);
        });
        container.on("mouseleave", function (event) {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP, event);
        });
        container.on("mousemove", function (event) {
            event.detail = {};
            event.detail.toolTip = $(container).text();
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP, event);
        });
    };

    removeToolTipOnMouseLeave = function (event) {
        var currentDoc = event.currentTarget;
        remove(currentDoc);
    };

    show = function (toolTips, x, y, currentDoc) {
        remove(currentDoc);
        appendToolTipHTML(getToolTipHTML(toolTips), currentDoc);
        display(x, y, currentDoc);
    };

    addCustomCSS = function (cssProps) {
        if(cssProps && !$.isEmptyObject(cssProps)){
            var tooltipNames = $('.tooltip > .tooltipName', $('body').parent());
            tooltipNames.css(cssProps);
        }
    };

    showName = function (name, x, y, currentDoc) {
        remove(currentDoc);
        appendToolTipHTML(getToolTipHTMLForName(mentor.publisher.languageTranslator.localize(name) || name),
                currentDoc);
        display(x, y, currentDoc);
    };

    remove = function (currentDoc) {
        $('.tooltip', $(currentDoc)).empty();
        $('.tooltip', $(currentDoc)).hide();
    };

    registerTTOnButton = function (container) {
        container.on("mouseover", function (event) {
            event.detail = {};
            event.detail.toolTip = $(this).attr(ttAttr);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP, event);
        });
        container.on("mouseleave", function (event) {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP, event);
        });
        container.on("mousemove", function (event) {
            event.detail = {};
            event.detail.toolTip = $(this).attr(ttAttr);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP, event);
        });
    };

    p.eventDispatcher.attachEventListener(p.events.SHOW_TOOL_TIP, showToolTipOnMouseHover);
    p.eventDispatcher.attachEventListener(p.events.REMOVE_TOOL_TIP, removeToolTipOnMouseLeave);
    p.eventDispatcher.attachEventListener(p.events.CLOSE_POPOVER, removeToolTipOnMouseLeave);

    return {
        showToolTip: function (toolTips, x, y, currentDoc) {
            show(toolTips, x, y, currentDoc);
        },
        showToolTipForName: function (name, x, y, currentDoc) {
            showName(name, x, y, currentDoc);
        },
        removeToolTip: function (currentDoc) {
            remove(currentDoc);
        },
        registerOnButton: function (container) {
            registerTTOnButton(container);
        },
        changeToolTipTextOnButton: function (container, text) {
            container.attr(ttAttr, text);
        },
        registerToolTipBasedOnLength: function (container) {
            registerToolTipOnTextField(container);
        },
        showToolTipFromEvent: function (event) {
            event.detail = {};
            event.detail.toolTip = $(event.currentTarget).attr(ttAttr);
            p.eventDispatcher.dispatchEvent(p.events.SHOW_TOOL_TIP, event);
        },
        addCustomCSS: addCustomCSS
    };

}(mentor.publisher));
