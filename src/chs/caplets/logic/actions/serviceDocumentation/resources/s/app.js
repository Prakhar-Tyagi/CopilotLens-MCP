/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, getInstalledJRE, mentor, clearLoginPopup, alertMsg, require, $, addDocumentEventListener*/
define([
    'router',
    'jquery',
    "backbone"
], function (Router, $, backbone)
{
    "use strict";
    var initialize, stopPinging, showServerShutDownMessage, showSessionExpired;

    initialize = function ()
    {
        //getInstalledJRE();
        //todo move the below function to new script files
        addDocumentEventListener();
        mentor.publisher.router = new Router();
        require(["routers/multipleDocumentRouter"], function (multipleDocumentRouter)
        {
            mentor.publisher.router.setDocumentRouter(multipleDocumentRouter);
        });
        backbone.history.start();

        var intervalId = setInterval(pingServer, 1000);
        stopPinging = function ()
        {
            clearInterval(intervalId);
        };
    };

    showServerShutDownMessage = function ()
    {
        $(document.body).html(
                '<div style="background-color: hsl(214, 39%, 30%); bottom: 0; color: #eee; left: 0; position: absolute; right: 0; top: 0;"> \
                    <div style="margin: 25% auto; width: 640px"> \
                        <div style="font-size: 24px; line-height: 36px; text-align: center;"> \
                            The server has been closed.<br> \
                            Please start it again to browse the package.\
                        </div> \
                    </div> \
                </div>'
        );
    };

    showSessionExpired = function ()
    {
        $(document.body).html(
                '<div style="background-color: hsl(214, 39%, 30%); bottom: 0; color: #eee; left: 0; position: absolute; right: 0; top: 0;"> \
                    <div style="margin: 25% auto; width: 640px"> \
                        <div style="font-size: 24px; line-height: 36px; text-align: center;"> \
                            Your session has expired.<br>Please refresh to load the package again. \
                        </div> \
                    </div> \
                </div>'
        );
    };

    function pingServer()
    {
        $.ajax({
            url: "/ping",
            type: "POST",
            async: true,
            success: function (event, status, jqXHR) {
                var redirectUrl = jqXHR.getResponseHeader("x-redirect-url");
                if (redirectUrl) {
                    showSessionExpired();
                }
            },
            error: function (jqXHR, textStatus, errorThrown)
            {
                if (jqXHR.status != 404 && !Utils.isNotHTTP()) {
                    showServerShutDownMessage();
                }
                stopPinging && stopPinging();
            }
        });
    }

    return {
        initialize: initialize
    };
});

function addDocumentEventListener()
{

    "use strict";
    if (mentor.publisher.documentLabelEventHandlersSet) {
        return;
    }

    function isModalDialogOpen() {
        return $('#modal-container').html() !== "";
    }

    var handleKeyPressEvents = function (e)
    {
        /**
         * escape key is pressed
         */
        var publisher = mentor.publisher;
        if (e.keyCode === 27) {
            //close popover window
            // !isModalDialogOpen() && publisher.eventDispatcher.dispatchEvent(publisher.events.CLOSE_POPOVER, {
            //     isCancelAction: true
            // });

            if (isModalDialogOpen()) {
                publisher.eventDispatcher.dispatchEvent(publisher.events.CLOSE_MODAL)
            } else {
                publisher.eventDispatcher.dispatchEvent(publisher.events.CLOSE_POPOVER, {isCancelAction: true});
                publisher.eventDispatcher.dispatchEvent(publisher.events.CLOSE_CONFIG_POPOVER);
            }
            //close VIN login popover
            clearLoginPopup();
            // remove alert window
            if (alertMsg) {
                alertMsg.removeAlertMsg();
            }

        }
        else if (e.keyCode === 122 || e.keyCode === 90) {
            //else if key is 'z' -->zoom highlighted views
            if (e.altKey) {
                publisher.eventDispatcher.dispatchEvent(publisher.events.RESIZE_SVG, {});
            }
            else {

                window.crossHighlightHandler.zoomViews();
            }
        }

        else if (e.keyCode === 109) {
            if (e.altKey) {
                require(["currentPackage"], function (currentPackage)
                {
                    currentPackage.trigger("collapseAll");
                });

            }
        }
        else if (e.keyCode === 107) {
            if (e.altKey) {
                require(["currentPackage"], function (currentPackage)
                {
                    currentPackage.trigger("expandAll");
                });
            }
        }

    }, windowResized, closePopover = function ()
    {
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
    };

    function initBodyDimensions()
    {
        //computation of these properties are expansive (espacially in IE11) so here we recalculate them on resize for later use
        mentor.publisher.bodyWidth = $('body').width();
        mentor.publisher.bodyHeight = $('body').height();
        mentor.publisher.offset = {};
        mentor.publisher.offset.splitter1 = $("#splitter1").offset()
        mentor.publisher.offset.splitter2 = $("#splitter2").offset()
        mentor.publisher.offset.splitter3 = $("#splitter3").offset()

    }

    windowResized = function (event) {
        initBodyDimensions();
        require(["views/navigationPanelView"], function (navigationPanelView)
        {
            navigationPanelView.onWindowResize(event);
            if (mentor.publisher.dataLoader.getWindowTitleConfigData().autoFitSVGOnWindowResize) {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.RESIZE_SVG, {});
            }
        });
    };

    //when user clicks any where in document then popover should close
    document.addEventListener('click', closePopover, false);

    mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.ATTACH_CLOSE_POPOVER_LISTENER,
        function (evt) {
            document.addEventListener('click', closePopover, false);
        });

    mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.DETACH_CLOSE_POPOVER_LISTENER,
        function (evt) {
            document.removeEventListener('click', closePopover, false);
        });

    //on escape key press , popover should close
    $(document).keydown(handleKeyPressEvents);
    initBodyDimensions();
    $(window).on("resize", windowResized);
    $(window).on("orientationchange", windowResized);
    $('body').on("click", function (evt)
    {
        if ($(evt.target).hasClass("closeBtn") && $(evt.target).hasClass("component-button")) {
            require(["routers/multipleDocumentRouter", "models/selectedSystem"],
                    function (multipleDocumentRouter, selectedSystem)
                    {
                        var contents = mentor.publisher.contentArea.getAllOpenContentDetails();
                        var firstContent;
                        for (var i in contents) {
                            if (contents.hasOwnProperty(i)) {
                                firstContent = contents[i];
                            }
                        }
                        if (firstContent) {
                            var id = firstContent.id || (firstContent.get && firstContent.get('id'));
                            multipleDocumentRouter.save(true, selectedSystem.get("objectId"), id);
                        }
                        else {
                            console.warn("No open content found to save on close.");
                        }
                    });
            evt.stopPropagation();
        }

    });
    mentor.publisher.documentLabelEventHandlersSet = true;
}

