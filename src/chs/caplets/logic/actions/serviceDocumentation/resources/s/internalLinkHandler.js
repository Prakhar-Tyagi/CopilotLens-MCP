/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global setInterval, clearInterval,setTimeout*/
define(["URLQueryParameterHandler"], function (URLQueryParameterHandler)
{
    return {

        isItaRelativeURL: function (linkValue)
        {
            return linkValue && linkValue.indexOf("index.html") === 0;
        },

        addCurrentPackageName: function (hrefValue) {
            try {
                var effSetter = getWindowObj().require("filehandlers/effectivitySetter");
                //var currentPackage = require("currentPackage");
                //hrefValue = hrefValue + "&project=" + currentPackage.get("title");
                return effSetter.addEffAndProjectIdInURLs(hrefValue);
            } catch (e) {

            }
        }, displayHref: function (hrefValue, event, offset)
        {
            var indexOfQueryStart = hrefValue.indexOf('?');
            var preResetValue;
            if (indexOfQueryStart >= 0 && this.isItaRelativeURL(hrefValue)) {
                preResetValue = mentor.publisher.urlParams.reset;
                hrefValue = this.addCurrentPackageName(hrefValue);
                mentor.publisher.setURLParams(hrefValue.substring(indexOfQueryStart));
                if (!mentor.publisher.urlParams.view && event) {

                    mentor.publisher.urlParams.internalLink = true;
                    /**
                     * x and y position is needed to show object popover
                     *
                     */
                    mentor.publisher.urlParams.x = event.clientX + offset.getX();
                    mentor.publisher.urlParams.y = event.clientY + offset.getY();
                }

                mentor.publisher.urlParams.reset = false;
                var showDefaultPage = URLQueryParameterHandler.handleQueryParameters();
                setTimeout(function ()
                {
                    mentor.publisher.urlParams.reset = preResetValue;
                    mentor.publisher.urlParams.internalLink = false;
                }, 2000);
                /**
                 * if handleQueryParameters method is not able to show the page then do the default behaviour
                 */
                if (event && !showDefaultPage) {
                    event.preventDefault();
                }
            }
        }, processAnchorEvent: function (event, anchorEle, offset)
        {

            var hrefValue = $(event.target).attr("href") ||
                    $(anchorEle).attr("xlink:href") || $(event.target).parent().attr("href");
            this.displayHref(hrefValue, event, offset);
        },

        addEventHandler: function (customDOM, offset)
        {
            var that = this;
            $('a', customDOM).on("click", function (event)
            {
                that.processAnchorEvent(event, this, offset);

            });
        },

        addMouseEventListener: function (containerId, config)
        {
            config = config || {};
            config.contentLoadAreaCSSSelector = config.contentLoadAreaCSSSelector || '#systemDiagnosticLoadArea';
            var that = this, customDOM, contentLoadArea, intervalID, offset, addMouseEventListener;

            addMouseEventListener = function (customDOM2, offset)
            {
                if (config.mouseEventListener) {
                    config.mouseEventListener(customDOM2);
                }
                else {

                    that.addEventHandler(customDOM2, offset);
                }
            };

            intervalID = setInterval(function ()
            {
                if ($('object', $(containerId)).length === 0) {
                    clearInterval(intervalID);
                }
                customDOM = $('object', $(containerId)).length && $('object', $(containerId))[0].contentDocument &&
                $('object', $(containerId))[0].contentDocument.documentElement;
                if (customDOM) {
                    clearInterval(intervalID);

                    offset = {
                        getX: function ()
                        {
                            return $(containerId).offset().left;
                        },
                        getY: function ()
                        {
                            return $(containerId).offset().top;
                        }
                    }

                    $('object', $(containerId))[0].addEventListener('load', function ()
                    {
                        var customDOM2 = $('object', $(containerId))[0].contentDocument.documentElement;
                        addMouseEventListener(customDOM2, offset);
                    });

                    addMouseEventListener(customDOM, offset);

                    return;
                }

                contentLoadArea = $(config.contentLoadAreaCSSSelector, $(containerId))[0];
                if (contentLoadArea) {
                    clearInterval(intervalID);

                    offset = {
                        getX: function ()
                        {
                            return 0;
                        },
                        getY: function ()
                        {
                            return 0;
                        }
                    };

                    that.addEventHandler(contentLoadArea, offset);
                }
            }, 100);

        }
    }

});
