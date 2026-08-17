/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("SignalTracerModel", ["backbone", "fileDisplayHandler"],
        function (Backbone, fileDisplayHandler) {
            "use strict";
            var flushConnectivity = true, SignalTracerModel = Backbone.Model.extend({
                //todo this class is not properly encapsulated, all the methods,variables are public
                //todo one should be careful when calling the methods of this class
                connectivityFilePath: '',
                pop: '',
                flush: '',
                title: '',
                altClickRender: false,
                altClickPop: false,
                licence: false,
                connectivityUID: '',
                designID: '',
                groundSTLicence: false,
                handlersAdded: false,
                update: function (signalTraceFiles, connectivityUID, designID) {
                    this.connectivityUID = connectivityUID;
                    this.designID = designID;
                    if (signalTraceFiles) {
                        if (signalTraceFiles.signalTraceFile) {
                            this.updateRenderer(signalTraceFiles.signalTraceFile, mentor.publisher.languageTranslator.localize("SignalPath"));
                        }
                        if (signalTraceFiles.fullInstanceFile) {
                            this.updateRenderer(signalTraceFiles.fullInstanceFile, mentor.publisher.languageTranslator.localize("FullInstance"));
                        }
                    }
                },
                //update the data needed for renderer
                updateRenderer: function (connectivityFile, titleToShow)
                {
                    this.connectivityFilePath = connectivityFile;
                    this.title = titleToShow;
                    this.renderForAltClick();
                },
                flushConnectivity: function (f)
                {
                    flushConnectivity = f;
                },
                render: function (pop)
                {
                    var popupFlag = Utils.notNull(pop) ? pop : this.pop;
                    if (!this.rendererLicenceAvaialable()) {
                        return;
                    }
                    //this.flushConnectivity(this.flush);
                    displayConnectivity(this.connectivityFilePath, popupFlag, flushConnectivity, this.title,
                            this.connectivityUID,
                            this.designID, this.isFullInstanceClicked());
                    this.flushConnectivity(true);

                },
                renderForAltClick: function ()
                {
                    if (this.altClickRender) {
                        this.render(false);
                    }
                },
                setAltClick: function (f)
                {
                    this.altClickRender = f;
                },
                reset: function () {
                    this.connectivityFilePath = '';
                    this.flush = true;
                    this.title = '';
                    this.altClickRender = false;
                    this.altClickPop = false;
                },
                rendererLicenceAvaialable: function () {
                    if (window.opener && window.opener.mentor) {
                        //todo check the licence from parent window
                        /*return window.opener.RenderConnectivityHandler.rendererLicenceAvaialable();*/
                        return true;
                    }
                    return this.licence;

                },
                groundSTLicenceAvaialable: function ()
                {
                    if (Utils.getUrlParameter('uri') !== '' || Utils.getUrlParameter('popout') == 'yes') {
                        //todo check the licence from parent window
                        /*return window.opener.RenderConnectivityHandler.groundSTLicenceAvaialable();*/
                        return true;
                    }
                    return this.groundSTLicence;

                },
                checkRendererAvailablility: function ()
                {
                    //the following file is supposed to be dumped, in case
                    //render connectivity licence is available.
                    var p = mentor.publisher;
                    p.eventDispatcher.attachEventListener(p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, function (evt)
                    {
                        this.connectivityUID = evt.detail.objectId;
                    });
                    var connectivityRoot = (Utils.prepareFilePath(mentor.publisher.project.getId() +
                            '/GlobalSignals/globalsignal0.xml'));
                    this.checkURL(connectivityRoot, true);
                    if (!this.licence) {
                        connectivityRoot =
                                (Utils.prepareFilePath(mentor.publisher.project.getId() +
                                        '/GlobalSignals/globalsignal1.xml'));
                        this.checkURL(connectivityRoot, true);
                    }
                },
                checkGroundSTAvailablility: function ()
                {
                    //the following file is supposed to be dumped, in case
                    //ground signal tracer licence is available.
                    var connectivityRoot = (Utils.prepareFilePath(mentor.publisher.project.getId() +
                            '/Signals/gstIdentifier.xml'));
                    this.checkURL(connectivityRoot, false);
                },
                checkURL: function (connectivityRoot, isNormalForST)
                {
                    var t = this;
                    $.ajax({ url: connectivityRoot,
                        success: function (data, textStatus, XMLHttpRequest)
                        {
                            if (isNormalForST) {
                                t.licence = true;
                            }
                            else {
                                t.groundSTLicence = true;
                            }
                        }, error: function (XMLHttpRequest, textStatus, errorThrown)
                        {
                            if (isNormalForST) {
                                t.licence = false;
                            }
                            else {
                                t.groundSTLicence = false;
                            }
                        }, dataType: (Utils.is_msie()) ? "text" : "xml", async: false});
                },
                isRenderAction: function ()
                {
                    return this.altClickRender;
                },
                getTitle: function ()
                {
                    return this.title;
                },
                isFullInstanceClicked: function ()
                {
                    var isFullInstance = false;
                    if (mentor.publisher.constants.FullInstanceTitle == this.getTitle()) {
                        isFullInstance = true;
                    }
                    return isFullInstance;
                },
                getConnectivityUID: function ()
                {
                    return this.connectivityUID;
                },
                updateData: function (systemId, objectId)
                {
                    var objectData;
                    if (systemId && objectId) {
                        objectData =
                                mentor.publisher.objectDataLoader.load(systemId, objectId,
                                        mentor.publisher.project.getId());
                        if (objectData && objectData.getSignalTraceFiles) {
                            this.update(objectData.getSignalTraceFiles(), objectId, systemId);
                        }
                    }

                },
                showRenderedDiagram: function (evt)
                {
                    var windowObj = window, content = {
                        mainText: evt.detail.mainText,
                        type: mentor.publisher.contentType.RENDERED_SVG,
                        path: evt.detail.path
                    };
                    if (window.opener && window.opener.mentor) {
                        windowObj = window.opener;
                        //window.opener.mentor.publisher.fileDisplayHandler.display(content);
                    }
                    //if the newly opened window is a popped-out signal trace window
                    //then it will either have an applet or it will have renderSignal as part of its query params.
                    //if the popped out window is not a signal trace window (can be xref), we trace the signal in  the
                    //main window.
                    if (/*Utils.notNull(document.getElementById("RenderConnectivity")*/(window.location.href.indexOf("index") >=
                            0) ||
                            (window.location.href.indexOf('/renderSignal') != -1)) {
                        mentor.publisher.fileDisplayHandler.display(content);
                    }
                    else {
                        windowObj.mentor.publisher.fileDisplayHandler.display(content);
                    }

                }
            }), signalTracerModel = new SignalTracerModel(), reset = function (signalTracerModel)
            {
                signalTracerModel.reset();
            }, setAltClick = function (evt, signalTracerModel)
            {
                var data = evt.detail;
                signalTracerModel.setAltClick(data.altKey);
            }, doNotFlushConnectivity = function (signalTracerModel, flush)
            {
                signalTracerModel.flushConnectivity(flush);
            }, updateState = function (evt, signalTracerModel)
            {
                var data = evt.detail, systemId, id;
                systemId = data.systemId;
                id = data.id;
                if (data.altKey) {
                    signalTracerModel.altClickRender = data.altKey;
                    signalTracerModel.flushConnectivity(data.flush);
                }

                signalTracerModel.updateData(systemId, id);
            }, highlightTracedSignal = function (signalTracerModel)
            {
                var data = {};
                data.objectId = signalTracerModel.connectivityUID;
                data.systemId = signalTracerModel.designID;
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS,
                        data);
            }, showRenderedDiagram = function (evt, signalTracerModel)
            {
                setTimeout(function(){
                    signalTracerModel.showRenderedDiagram(evt);
                }, 10)

            };
            signalTracerModel.addEventHandlers = function ()
            {
                if (this.handlersAdded) {
                    return;
                }

                var that = this;
                mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.OPEN_RENDEREDSIGNAL,
                        function (evt)
                        {
                            showRenderedDiagram(evt, that);
                        });
                mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLOSE_POPOVER,
                        function (evt)
                        {
                            var doNotResetSignalTracer = false;
                            doNotResetSignalTracer = evt && evt.detail && evt.detail.doNotResetSignalTracer;
                            if (!doNotResetSignalTracer) {
                                reset(that);
                            }
                        });

                mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.ALT_CLICK_TRIGGERED,
                        function (evt)
                        {
                            setAltClick(evt, that);
                        });

                mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLICKED_IN_SIGNAL_TRACE_VIEW,
                        function (evt)
                        {
                            doNotFlushConnectivity(that, evt.detail);
                        });

                mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.HIGHLIGHT_TRACED_SIGNAL,
                        function (evt)
                        {
                            highlightTracedSignal(that);
                        });

                mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.UPDATE_SIGNAL_TRACER,
                        function (evt)
                        {
                            updateState(evt, that);
                        });
                that.handlersAdded = true;
            };
            return _.extend(signalTracerModel, Backbone.Events);
        });

