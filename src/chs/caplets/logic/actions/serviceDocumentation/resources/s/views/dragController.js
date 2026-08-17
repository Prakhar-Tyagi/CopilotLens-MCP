define("DragController", [
    'jquery',
    'backbone'
], function ($, Backbone) {
    "use strict";
    var DragController, onmousemove, that, timeoutHandle;

    function getPositionX(evt) {
        if (evt.touches) {
            return evt.touches[0].clientX;
        } else {
            return evt.clientX;
        }
    }

    function getPositionY(evt) {
        if (evt.touches) {
            return evt.touches[0].clientY;
        } else {
            return evt.clientY;
        }
    }

    DragController = Backbone.Model.extend({
        defaults : {
            divid : '',
            container : ''
        },
        startDragging : function (evt) {
            evt = evt || window.event;
            var mousePositionX = getPositionX(evt),
                mousePositionY = getPositionY(evt),
                divid = this.get('divid'),
                container = this.get('container'),
                divTop = $(divid).css('top'),
                divLeft = $(divid).css('left'),
                elementWidth = parseInt($(divid).width()),
                elementHeight = parseInt($(divid).height()),
                containerWidth = parseInt($(container).width()),
                containerHeight = parseInt($(container).height()), diffX, diffY, onmouseup;
            //console.log('drag start...');
            that = this;
            $(container).css('cursor', 'move');
            divTop = divTop.replace('px', '');
            divLeft = divLeft.replace('px', '');
            diffX = mousePositionX - divLeft;
            diffY = mousePositionY - divTop;
            onmousemove = function (event) {
                //console.log('moving...');
                event = event || window.event;
                var posX = getPositionX(event),
                    posY = getPositionY(event),
                    aX = posX - diffX,
                    aY = posY - diffY;
                $('.panel_content > object').each(function () {
                    var doc = this.contentDocument && this.contentDocument.documentElement;
                    if (doc == event.currentTarget) {
                        aX = aX + $(this).parent().offset().left;
                        aY = aY + $(this).parent().offset().top;
                    }
                });
                if (aX < 0) {
                    aX = 0;
                }
                if (aY < 0) {
                    aY = 0;
                }
                if (aX + elementWidth > containerWidth) {
                    aX = containerWidth - elementWidth;
                }
                if (aY + elementHeight > containerHeight) {
                    aY = containerHeight - elementHeight;
                }
                $(divid).css('left', aX + 'px');
                $(divid).css('top', aY + 'px');
            };
            onmouseup = function (evt) {
                evt.stopImmediatePropagation();
                that.endDragging();
            };
            if (timeoutHandle) {
                clearTimeout(timeoutHandle);
            }
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.DETACH_CLOSE_POPOVER_LISTENER, {});
            $('.panel_content > object').each(function () {
                var doc = this.contentDocument && this.contentDocument.documentElement;
                $(doc).on("mousemove", onmousemove);
                $(doc).on("touchmove", onmousemove);
                $(doc).on("mouseup", onmouseup);
                $(doc).on("touchend", onmouseup);
            });
            $(document).on("mousemove", onmousemove);
            $(document).on("touchmove", onmousemove);
            $(document).on("mouseup", onmouseup);
            $(document).on("touchend", onmouseup);
        },
        endDragging : function () {
            //console.log("end dragging...");
            var container = that.get('container');
            $(container).css('cursor', 'default');
            $('.panel_content > object').each(function () {
                var doc = this.contentDocument && this.contentDocument.documentElement;
                $(doc).off("mousemove");
                $(doc).off("touchmove");
                $(doc).off("mouseup");
                $(doc).off("touchend");
            });
            $(document).off("mousemove");
            $(document).off("touchmove");
            $(document).off("mouseup");
            $(document).off("touchend");
            timeoutHandle = setTimeout(function () {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.ATTACH_CLOSE_POPOVER_LISTENER,
                    {});
            }, 1000);

        },
    });
    return DragController;
});