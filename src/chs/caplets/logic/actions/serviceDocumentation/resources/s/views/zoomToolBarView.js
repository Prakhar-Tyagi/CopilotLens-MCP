/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define([
    'jquery',
    'underscore',
    'backbone',
    "ZoomToolBarModel"
], function ($, underscore, Backbone, model)
{
    "use strict";
    var ZoomToolBarView, hideSliderTimer;
    ZoomToolBarView = Backbone.View.extend(
            {
                events: {
                    "click .zoomout-button": "zoomOut",
                    "click .zoomin-button": "zoomIn",
                    "click .zoomall-button": "zoomFit",
                    "click .fix-view-button": "saveZoomLevel",
                    "click .zoom-selected-button": "zoomSelectedObject",
                    "click": "show",
                    "mouseup .sliderNotch": "notchUp",
                    "mousedown .sliderNotch": "notchDown",
                    "mouseenter": "show",
                    "mouseleave": "hide",
                    "mouseover .fix-view-button,.zoom-selected-button": "showToolTip",
                    "mousemove .fix-view-button,.zoom-selected-button": "showToolTip",
                    "mouseover .zoomall-button": "showDynamicToolTip",
                    "mousemove .zoomall-button": "showDynamicToolTip",
                    "mouseleave .fix-view-button,.zoomall-button,.zoom-selected-button": "removeToolTip"

                },
                showToolTip: function (event)
                {
                    mentor.publisher.toolTip.showToolTipFromEvent(event);
                },

                showDynamicToolTip: function (event)
                {
                    if (event.altKey) {
                        $(event.currentTarget).attr('customtooltip-0', "ZoomToDefault");
                    }
                    else {
                        $(event.currentTarget).attr('customtooltip-0', "ZoomToFit");
                    }
                    mentor.publisher.toolTip.showToolTipFromEvent(event);
                },

                removeToolTip: function (event)
                {
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP, event);
                },
                show: function (evt)
                {
                    this.updateLocation();
                    $('.diagramControl', this.el).show();
                    $('.diagramControl', this.el).removeClass('splitPanel_bottomToolBar_mouseLeave');
                    $('.diagramControl', this.el).addClass('splitPanel_bottomToolBar_mouseEnter');

                    if (is_touch_device()) {
                        clearTimeout(hideSliderTimer);
                        hideSliderTimer = setTimeout(function () {
                            this.hide();
                        }.bind(this), 5000);
                    }
                },
                hide: function (evt)
                {
                    this.updateLocation();
                    // TODO: Check with team before removing this.
                    // if (is_touch_device()) {
                    //     return;
                    // }
                    $('.diagramControl', this.el).hide();
                    $('.diagramControl', this.el).removeClass('splitPanel_bottomToolBar_mouseEnter');
                    $('.diagramControl', this.el).addClass('splitPanel_bottomToolBar_mouseLeave');
                },

                zoomSelectedObject: function(event) {
                    window.crossHighlightHandler.zoomViews();
                },

                getCurrentZoom: function (panel) {
                    var zoomLevelText;
                    var zoomModel = model.get(this.el.id);
                    if (zoomModel) {
                        zoomLevelText = zoomModel.get("currentZoomLevel");
                    } else {
                        zoomLevelText = $('.diagramControl .component-label', panel).html();
                    }
                    return zoomLevelText+"";
                }, zoomOut: function (evt)
                {
                    /*                    if (parseInt(zoolLevelText.html().replace('%', ''), 10) === mentor.publisher.constants.MaxZoomPercentage) {
                     return;
                     }
                     //for 1 units of change, calculateZoomFactor and resize SVg based on that.
                     resizeSvg(calculateZoomFactor(1), containerId);*/

                    var panel = this.el;
                    var zoomLevelText = this.getCurrentZoom(panel);

                    this.updateZoom(this.el.id, zoomLevelText, 1);
                    this.moveZoomSlider(mentor.publisher.constants.PositiveSliderStep,
                            mentor.publisher.constants.PositiveSliderStep);
                    //this.setSliderPosition();
                },
                zoomIn: function (evt)
                {
                    /*
                     if (parseInt(zoolLevelText.html().replace('%', ''), 10) === mentor.publisher.constants.MinZoomPercentage) {
                     return;
                     }
                     //for -1 units of change, calculateZoomFactor and resize SVg based on that.
                     resizeSvg(calculateZoomFactor(-1), containerId);
                     */
                    var panel = this.el;
                    var zoomLevelText = this.getCurrentZoom(panel);
                    this.updateZoom(this.el.id, zoomLevelText, -1);
                    this.moveZoomSlider(mentor.publisher.constants.NegetiveSliderStep,
                            mentor.publisher.constants.NegetiveSliderStep);
                    //this.setSliderPosition();
                },
                zoomFit: function (evt)
                {
                    if (evt.altKey) {
                        if (svgEventHandlers[this.el.id]) {
                            svgEventHandlers[this.el.id].zoomLockedView();
                        }
                        return;
                    }
                    if (svgEventHandlers[this.options.handler.svgContainerId]) {
                        svgEventHandlers[this.options.handler.svgContainerId].zoomFit();
                    }
                    else if (svgEventHandlers[this.el.id]) {
                        svgEventHandlers[this.el.id].zoomFit();
                    }
                    //this.options.handler.zoomFit();
                    model.get(this.el.id).set("currentZoomLevel", 100);
                },
                saveZoomLevel: function ()
                {
                    if (svgEventHandlers[this.el.id]) {
                        svgEventHandlers[this.el.id].svgTransformModel.saveZoomLevel();
                    }
                    //$('.fix-view-button', this.el).removeClass('unlocked');
                    //$('.fix-view-button', this.el).addClass('locked');
                },
                moveZoomSlider: function (zoomx, zoomy)
                {
                    /*var panel = this.el, zoomLevelText = $('.diagramControl .component-label', panel) ?
                     $('.diagramControl .component-label', panel).html() : "",
                     zoomCurrentVal = zoomLevelText.replace('%', '');
                     var newZoomLevel = parseInt(zoomx) + parseInt(zoomCurrentVal * 1);*/
                    //setZoomSliderLevel(this.el.id, newZoomLevel);
                    model.updateZoomLevel(this.el.id, zoomx, zoomy);
                },
                dragX: 0,
                dragY: 0,
                sliderx: 0,
                notchWidth: 0,
                zoomLevelDefinitionNotchWidth: 0,
                notchParentWidth: 0,
                notchDown: function (evt)
                {
                    //we'll be using the zoom value instead of the position of cursor, because the absolute postion of the cursor
                    //can change depending on the window's events.
                    var panel = this.el;
                    var zoomLevelText = this.getCurrentZoom(panel);
                    this.sliderx = zoomLevelText+"";
                    this.startSlide(evt);
                },
                startSlide: function (event)
                {
                    this.dragX = event.pageX;
                    this.dragY = event.pageY;
                    // movesemove listener
                    var viewInstance = this;
                    $('body').on("mousemove", function (evt)
                    {
                        viewInstance.sliderMouseDrag(evt);
                    });
                    // movseup listener
                    $('body').on("mouseup", function (evt)
                    {
                        viewInstance.stopSlide(evt);
                    });
                },
                sliderMouseDrag: function (event)
                {
                    var deltaX, notchPosition, zoomCurrVal, notch = $('.sliderNotch', this.el), newSliderContent;
                    deltaX = event.pageX - this.dragX;
                    notchPosition = $(notch).position();
                    zoomCurrVal = $('.diagramControl .component-label', this.el).html().replace('%', '');
                    //adjust the position, clipping to the container
                    newSliderContent = model.getSliderContent(this.el.id, deltaX, zoomCurrVal, notchPosition.left);
                    if (!newSliderContent) {
                        return;
                    }
                    $(notch).css('left', newSliderContent.x);
                    $('.diagramControl .component-label', this.el).html(newSliderContent.zoom);
                    this.dragX = event.pageX;
                    this.dragY = event.pageY;
                },
                notchUp: function (evt)
                {
                    //using the same logic which is used to calculate the zoom factor for mouse wheel
                    //10 units of change equals one unit of zoom. so for x units of zoom, calculate the zoom factor.
                    //All this logic is done in calculateZoomFactor .
                    var zoomLevelText = $('.diagramControl .component-label', this.el).html();
                    this.updateZoom(this.el.id, zoomLevelText,
                            (zoomLevelText.replace('%', '') - this.sliderx) / 10);
                },
                updateZoom: function (container, zoomLevelText, stepLength)
                {
                    var zoomFactor;
                    model.updateZoom(container, zoomLevelText, stepLength);
                    zoomFactor = model.get(this.el.id).get("zoomFactor");
                    if (svgEventHandlers[this.el.id]) {
                        svgEventHandlers[this.el.id].svgTransformModel.zoomToMiddle(zoomFactor);
                    }
                    //resizeSvg(zoomFactor);
                },
                stopSlide: function (event)
                {
                    // For unbinding the mousemove
                    //$('body').unbind('mousemove', this.sliderMouseDrag);
                    //todo dangerous??
                    $('body').off('mousemove');
                    // unbinding the mouseup event listener
                    //$('body').unbind('mouseup', this.stopSlide);
                    $('body').off('mouseup');
                },

                setSliderPosition: function ()
                {
                    if(this.zoomUpdateProcess) {
                        clearTimeout(this.zoomUpdateProcess);
                    }
                    this.zoomUpdateProcess = setTimeout(function () {
                        var notchOffset, notchParent, currentPercentage = model.get(this.el.id).get('currentZoomLevel');
                        var panel = this.$el.parent(), notch = $('.sliderNotch',panel),
                                zoolLevelText = $('.diagramControl .component-label', panel);
                        $(zoolLevelText).html('' + Math.round(currentPercentage) + '%');
                        notchParent = notch.parent();
                        if (this.notchWidth == 0) {
                            this.notchWidth = notch.width();
                        }
                        if (this.notchParentWidth == 0) {
                            this.notchParentWidth = notchParent.width();
                        }
                        this.setNotchPosition(notch, this.notchWidth, this.notchParentWidth, currentPercentage, 0);
                    }.bind(this), 1000);

                },
                setZoomLevelDefinitionNotchPosition: function ()
                {
                    var notchOffset, notchParent, zoomPercentage = model.get(this.el.id).get('currentZoomLevel'),
                            panel = this.$el.parent(), zoomDefinitionNotch = $('.zoomDefinitionNotch', panel),
                            sliderNotch = $('.sliderNotch', panel), offset;
                    notchParent = zoomDefinitionNotch.parent();
                    if (this.notchWidth == 0) {
                        this.notchWidth = sliderNotch.width();
                    }
                    if (this.zoomLevelDefinitionNotchWidth == 0) {
                        this.zoomLevelDefinitionNotchWidth = zoomDefinitionNotch.width();
                    }
                    if (this.notchParentWidth == 0) {
                        this.notchParentWidth = notchParent.width();
                    }
                    //offset = (this.notchWidth / 2) - (this.zoomLevelDefinitionNotchWidth / 2);
                    offset = 0;
                    this.setNotchPosition(zoomDefinitionNotch, this.zoomLevelDefinitionNotchWidth,
                            this.notchParentWidth,
                            zoomPercentage, offset);
                },
                setNotchPosition: function (notch, notchWidth, notchParentWidth, zoomPercentage, offset)
                {
                    var notchOffset = ((notchParentWidth - notchWidth) / mentor.publisher.constants.MaxZoomPercentage) *
                            zoomPercentage;
                    notchOffset = notchOffset + offset;
                    $(notch).css('left', notchOffset + 'px');
                },
                changeLockState: function ()
                {
                    var lockState = model.get(this.el.id).get('lockState'), panel = this.$el.parent(), notch;
                    notch = $('.zoomDefinedNotch', panel);
                    if (!lockState) {
                        //$('.fix-view-button', this.el).removeClass('locked');
                        //$('.fix-view-button', this.el).addClass('unlocked');
                        $(notch).hide();
                    }
                    else {
                        //$('.fix-view-button', this.el).removeClass('unlocked');
                        //$('.fix-view-button', this.el).addClass('locked');
                        $(notch).show();
                        this.setZoomLevelDefinitionNotchPosition();
                    }
                },
                initialize: function ()
                {
                    var withDelayFn = function () {
                        setTimeout(function () {
                            this.updateLocation();
                        }.bind(this), 400);
                    }.bind(this);

                    var hideSliderTimer;
                    var showSlider = function (event) {
                        var containerId = event.detail.containerId;
                        if (this.el.id === containerId) {
                            this.show();
                        }
                    }.bind(this)

                    $(window).on("resize", withDelayFn.bind(this));
                    $(window).on("orientationchange", withDelayFn.bind(this));
                    var evtDispatcher = mentor.publisher.eventDispatcher;
                    var publisherEvents = mentor.publisher.events;
                    evtDispatcher.attachEventListener(publisherEvents.REPOSITION_SVG_SLIDER, withDelayFn.bind(this));
                    evtDispatcher.attachEventListener(publisherEvents.SHOW_SLIDER, showSlider.bind(this));
                },
                reposition: function ()
                {
                    var x = model.get(this.el.id).get('parentDimensions').x,
                            y = model.get(this.el.id).get('parentDimensions').y,
                            panelWidth = model.get(this.el.id).get('parentDimensions').width,
                            panelheight = model.get(this.el.id).get('parentDimensions').height,
                            toolBarWidth, toolBarHeight, left, top;
                    //todo can all these calculations be moved to the model?
                    toolBarWidth = $('.diagramControl', this.el).width();
                    toolBarHeight = $('.diagramControl', this.el).height();
                    //the toolbar height comes as 0, add the height as 35
                    if (!toolBarHeight) {
                        toolBarHeight = 40;
                    }
                    left = x + panelWidth - toolBarWidth;
                    top = y + panelheight - toolBarHeight;
                    $('.diagramControl', this.el).css('left', left);
                    $('.diagramControl', this.el).css('top', top);
                },
                render: function ()
                {
                    //this.container = this.el;
                    //this.setElement(this.container);
                    var hideFixZoomButton = this.options.hideFixZoomButton;
                    if (!this.el) {
                        return;
                    }
                    $('.diagramControl', this.el).remove();
                    var template = underscore.template(ZoomToolBarView.templateHTML)({
                        show: true,
                        showFixZoomButton: !hideFixZoomButton
                    });
                    this.$el.append(template);
                    var notch = $('.sliderNotch', this.el);
                    if (this.el) {
                        model.createContainerModel(this.el.id, $(notch).parent().position(), $(notch).parent().width(),
                                $(notch).width());
                        model.get(this.el.id).on("change:currentZoomLevel", this.setSliderPosition, this);
                        model.get(this.el.id).on("change:lockedZoomLevel", this.changeLockState, this);
                        model.get(this.el.id).on("change:parentDimensions", this.reposition, this);
                        setTimeout(function() {
                            this.updateLocation();
                        }.bind(this), 500);
                    }
                    return this;
                },
                updateLocation: function ()
                {
                    var x, y, panelWidth, panelheight;
                    if (($(this.$el).position())) {
                        x = $(this.$el).position().left;
                        y = $(this.$el).position().top;
                    }
                    panelWidth = $(this.$el).width();
                    panelheight = $(this.$el).height();
                    model.updateLocation(this.el && this.el.id, x, y, panelWidth, panelheight);
                }
            }
    );
    return ZoomToolBarView;
});


