/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, require*/
define("PopoverView",
        ['jquery', 'underscore', 'backbone', "BasicPopoverView", "XRefBuilderModel", "DragController",
            "SignalTracerModel"],
        function ($, underscore, Backbone, BasicPopoverView, xrefBuilderModel, DragController, signalTraceModel) {
            "use strict";
            var PopoverView = BasicPopoverView.extend({
                draggable: new DragController({'divid': '#detailPopup', 'container': '#applicationArea'}),
                initialize: function () {
                    PopoverView.__super__.initialize();
                    signalTraceModel.addEventHandlers();
                },
                events: {
                    "click #popover-grouped-list .titlebar": "toggleSection",
                    "click .listItem": "closePopover",
                    "click #buildConfigurationButton": "xrefConfigurationBuilderButtonClicked",
                    "keyup #relateddata_filter": "textEntered",
                    "click #relateddata_filter": "removeTextPlaceHolder",
                    "focusout #relateddata_filter": "addTextPlaceHolder",
                    "mousedown #detailPopup .auto-popover-header .auto-title-label": "startDragging",
                    "mouseup #detailPopup .auto-popover-header .auto-title-label": "endDragging",
                    "touchstart #detailPopup .auto-popover-header .auto-title-label": "touchStartHandler",
                    "touchend #detailPopup .auto-popover-header .auto-title-label": "touchEndHandler",
                    "click .renderConnectivityBtn": "traceSignal"
                },

            touchStartHandler: function(e) {
                if (e.touches.length === 1) {
                   this.draggable.startDragging(e);
                }
            },

            touchEndHandler: function(e) {
                if (e.touches.length === 1) {
                    this.draggable.endDragging(e);
                }
            },

            startDragging : function(evt){
                this.draggable.startDragging(evt);
            },
            endDragging : function(){
                this.draggable.endDragging();
            },

            xrefConfigurationBuilderButtonClicked: function (evt)
            {
                xrefBuilderModel.fetch(evt);
                evt.stopPropagation();
            },

            closePopover: function (evt)
            {
                this.removeView();

            },

            reRender: function ()
            {
                $('#detailPopup', this.$el).remove();
                this.render(this.getModel());
            },

            loadDefaultTitleTemplate: function (designObject, callback)
            {
                    var defaultObjectTemplateFile = "text!templates/p/objectTitle.html";
                return this.loadObjectTitleTemplate(designObject, defaultObjectTemplateFile, callback);
            },

            loadObjectTitleTemplate: function (designObject, templateFile, callback)
            {
				var that = this;
                require([templateFile], function (templateFileloaded)
                {
                    callback(underscore.template(templateFileloaded)({
                        designObject: designObject,
                        objectPopoverTitle : that.getObjectType(designObject)
                    }));
                });
            },

            getObjectType : function(designObject, i18nModule) {
                i18nModule = i18nModule || mentor.publisher.languageTranslator;
				var type = (designObject.getType ? designObject.getType() : "").trim();
				var name = this.getObjectName(designObject);
				var titleToDisplay = name;
			    if(type) {
                    var configuredTitleForObject = mentor.publisher.dataLoader.getObjectPropertyToUseForTitle(type);
                    if(!configuredTitleForObject) {
                        titleToDisplay = i18nModule.localize(type) + " "+ i18nModule.localize(name);
                    } else {
                        var configuredTitle = Utils.matchAndTranformObjectTitlePattern(configuredTitleForObject,
                                function (propsExpression, start, end) {
                                    if (propsExpression) {
                                        var attributeName = propsExpression.replace(start, "").replace(end, "");
                                        return i18nModule.localize(designObject.getAttr(attributeName)) || "";
                                    }
                                    return propsExpression
                                });
                        titleToDisplay = configuredTitle || titleToDisplay;
                    }
                }
                return titleToDisplay;
            },
            getObjectName: function (designObject) {
              return designObject.getName ? designObject.getName() : "";
            },

            loadTitleTemplateForObject: function (objectTitleTemplateFile)
            {
                return mentor.publisher.xmlLoader.loadXMLByAjax(objectTitleTemplateFile, false, true, "text");
            },

            getObjectTitleAndRenderPopover: function (designObject, callback)
            {
                var objectType = designObject.getType ? (designObject.getType() + " ") :
                        "", objectTitleTemplateFile, objectTittleFile, templateFile = "templates/p/" +
                        objectType.trim() + ".html";
                objectTitleTemplateFile = "s/" + templateFile;
                objectTittleFile = this.loadTitleTemplateForObject(objectTitleTemplateFile);
                if (objectTittleFile.error) {
                    return this.loadDefaultTitleTemplate(designObject, callback);
                }
                else {
                    return this.loadObjectTitleTemplate(designObject, "text!" + templateFile, callback);
                }

            },
                traceSignal: function (evt) {
                    signalTraceModel.render(evt.altKey);
                },
                isSignalTracerAvailable: function () {
                    signalTraceModel.checkRendererAvailablility();
                    initializeSignalRenderer(isHTTPProtocol());
                    return signalTraceModel.rendererLicenceAvaialable() &&
                            signalTraceModel.getTitle();
                },
                getRenderConnectivityBtnToolTip: function () {
                    return mentor.publisher.languageTranslator.localize(signalTraceModel.getTitle());
                },

                render: function (popOverModel) {
                    var that = this, designObject, x, y, showFilter, showXrefBuilderButton, coordinates;
                    this.setElement(this.container);
                    if (!popOverModel) {
                        return;
                    }
                    designObject = popOverModel.get("popoverModel");
                    if (!designObject) {
                        return;
                }
                x = popOverModel.get("x");
                y = popOverModel.get("y");

                showFilter = popOverModel.get('showFilter') || false;
                showXrefBuilderButton = popOverModel.get('showXrefBuilderButton') || false;
                coordinates = this.getCoordinates(x, y);
                this.getObjectTitleAndRenderPopover(designObject, function (title)
                {
                    var signalTraceFiles = null;
                    if (designObject && designObject.getSignalTraceFiles) {
                        signalTraceFiles = designObject.getSignalTraceFiles();
                    }
                    var isSignalTraceFileAvailable = signalTraceFiles != null &&
                            (signalTraceFiles.fullInstanceFile || signalTraceFiles.signalTraceFile);
                    var toolTip = "";
                    if (isSignalTraceFileAvailable) {
                        toolTip = signalTraceFiles.signalTraceFile ?
                                mentor.publisher.languageTranslator.localize("SignalPath") :
                                mentor.publisher.languageTranslator.localize("FullInstance");
                    }
                    var template = underscore.template(that.templateHTML)({
                        title: /*(designObject.getType ? (designObject.getType() + " ") : "") +
                         (designObject.getName ? designObject.getName() : "")*/title,
                        show: true,
                        x: coordinates.x,
                        y: coordinates.y,
                        height: showFilter ? mentor.publisher.constants.popOverHeightWithFilter :
                                mentor.publisher.constants.popOverHeightWithoutFilter,
                        showFilter: showFilter,
                        showXrefBuilderButton: showXrefBuilderButton,
                        showRenderConnectivityBtn: that.isSignalTracerAvailable() || isSignalTraceFileAvailable,
                        renderConnectivityBtnToolTip: toolTip
                    });
                    that.$el.append(template);
                });

                return this;
            }
        });
        return PopoverView;
    });
