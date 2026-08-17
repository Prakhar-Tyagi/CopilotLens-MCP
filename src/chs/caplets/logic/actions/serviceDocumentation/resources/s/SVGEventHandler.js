var SVGEventHandler = function ()
{
    this.create = function ()
    {
        this.svgContainerId = '';
        this.nodesStack = [];
        this.originalNodes = [];
        this.originalNodesOnMouseHover = [];
        this.mouseout = true;
        // For storing the last UID in case of heighlighting
        this.lastUID = '';
        // For checking SVG pan done or not
        this.doPan = false;
        this.mouseMoveAndEnter = false;

        this.hotSpotText = '';

        this.root = '';
        this.scalex = '';
        this.scaley = '';
        this.state = 'none';
        this.stateTarget;
        this.stateOrigin;
        this.stateTf;
        this.initialTransformMatrix;
        this.viewport = '';
        this.windowHeight = '';
        this.windowWidth = '';
        this.newWindowHeight = '';
        this.newWindowWidth = '';
        this.viewBoxHeight = '';
        this.viewBoxWidth = '';
        this.viewBox = '';
        this.zoomfactor = '';
        this.signalFlag = false;
        this.signalDataArray = [];
        this.currentClickUID = '';
        this.svgTransformModel = '',
        this.zoomTracer = new Array();
    };
    this.create();
    this.init = function (svgEl, viewPort)
    {
        var that = this;
        if(window.heavySVGs) {
            this.svgElementVisibiltyToggler =
                    new SVGElementVisibilityToggler({root: svgEl, xmlLoader: mentor.publisher.xmlLoader}, _, $);
        }
        if (svgEl && svgEl.viewBox) {
            $(svgEl).off();
            this.root = svgEl;
            this.viewport = $('#viewport', svgEl)[0];
            if (svgEl.viewBox.baseVal) {
                this.viewBoxHeight = svgEl.viewBox.baseVal.height;
                this.viewBoxWidth = svgEl.viewBox.baseVal.width;
            }
        }
    };

    this.isValidEvent = function (event)
    {
        return this.isValidElement(event);
        //return true;
    };

    this.mouseDownHandler = function (event)
    {
        this.currentPos = [event.pageX, event.pageY];
        this.doPan = 'mousedown';
    };

    this.mouseClickHandler = function (event)
    {
        var svgHandler = this;
        if (this.doPan !== true) {
            var target = event.target;
            svgHandler.mouseMoveAndEnter = true;
            if (target.timer) {
                clearTimeout(target.timer);
                target.timer = null;
            }
            /*
             Reset render connectivity handler
             */
            //RenderConnectivityHandler.reset();
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
            if (!svgHandler.mouseout) {
                svgHandler.mouseout = true;
                //                svgHandler.debumpHighlightStack();
            }
            svgHandler.pressed(event);
        }
        this.doPan = false;
    };

    this.getConnectivityId = function(descValue)
    {
        var parts = descValue.split(' ');
        var connectivityUID = parts[2];
        var fetchConnectivityIdsForRenderedSVG = this.getRenderedUIDs(descValue);
        if (fetchConnectivityIdsForRenderedSVG != null) {
            connectivityUID = fetchConnectivityIdsForRenderedSVG[1];
        }
        return connectivityUID;
    };

    this.mouseMoveHanlder = function (event)
    {
        var svgHandler = this;

        if (this.doPan === 'mousedown' && (event.pageX !== this.currentPos[0] && event.pageY !== this.currentPos[1])) {
            this.currentPos = [event.pageX, event.pageY];
            this.doPan = true;
        }
        var descValue = svgHandler.isValidElement(event);
        //////console.log(descValue);
        var target = event.target;
        if (descValue) {
            //Yaminee START --->If this element is valid then attach enter and leave handler on this
            //We need to add mouse leave and move only on Valid elements..Earlier in initializeMouseEvents it was
            // atatched to $(*,this.root) which is costly operation and appends mouse enter and leave at all junks.The
            // below mechanism is optimum for getting the same results. If i do Unbind and rebind then it was leaking
            // memory. So used this way an attribute m_enterLeavelisternersAdded attached once adding is done. And then
            // checking of it is not added add again
            if (!Utils.notNull(target.m_enterLeavelisternersAdded) || !target.m_enterLeavelisternersAdded) {//Add only once
                $(target).on("mouseleave", {svgHandle: svgHandler}, svgHandler.mouseLeaveHandler);
                $(target).on("mouseenter", {svgHandle: svgHandler}, svgHandler.mouseEnterHandler);
                target.m_enterLeavelisternersAdded = true;
            }
            //Yaminee END
            try {
                //                svgHandler.handleMouseEevents(descValue);
                if (currentState && currentState() !== 'pan' && this.style) {
                    this.style.cursor = 'pointer';
                }
                if (target.timer) {
                    clearTimeout(target.timer);
                    target.timer = null;
                }
                target.timer = setTimeout(function ()
                {
                    svgHandler.mouseout = false;
                    try {
                        svgHandler.mouseHoverHighLight(descValue, event);
                    }
                    catch (e) {
                        //ignore;
                    }
                }, 1000);

                if(target.showTooltipTimer){
                    clearTimeout(target.showTooltipTimer);
                    target.showTooltipTimer = null;
                }

                target.showTooltipTimer = setTimeout(function(){
                    if (typeof(descValue.split) == "undefined") {
                        return;
                    }
                    var connectivityUID = svgHandler.getConnectivityId(descValue);
                    var objectData = mentor.publisher.project.loadObjectData(undefined, connectivityUID);

                    svgHandler.showToolTipOnMouseover(objectData, event);

                    // hide the tooltip on mouseleave, also unbind the event when tooltip is removed
                    $(target).on("mouseleave", function mouseleaveTooltipHandler(){
                        $(this).off('mouseleave', mouseleaveTooltipHandler);
                        setTimeout(function(){
                            svgHandler.removeToolTipOnMouseleave();
                        },400);
                    });
                }, 300);

            }
            catch (e) {

            }

        }
        else {
            try {
                if (typeof currentState !== "undefined" && currentState() !== 'pan' && this.style) {
                    this.style.cursor = 'default';
                }
                if (!svgHandler.mouseMoveAndEnter) {
                    clearTimeout(target.timer);
                    target.timer = null;
                    if (!svgHandler.mouseout) {
                        svgHandler.mouseout = true;
                        if (!svgHandler.signalFlag) {
                            svgHandler.resetAttributesAndStack();
                        }
                    }
                }
            }
            catch (e) {

            }
        }
    };

    this.mouseLeaveHandler = function (event)
    {
        var svgHandler = event.data.svgHandle;
        var target = event.target;
        svgHandler.mouseMoveAndEnter = false;
        if (!svgHandler.isValidEvent(event)) {
            this.style.cursor = 'default';
            clearTimeout(target.timer);
            // event.cancel();
            return;
        }
        else {
            this.style.cursor = 'pointer';
        }

        if (target.timer) {
            clearTimeout(target.timer);
            target.timer = null;
            if (!svgHandler.mouseout) {
                svgHandler.mouseout = true;
                if (!svgHandler.signalFlag) {
                    svgHandler.resetAttributesAndStack();
                }
            }
        }
    };

    this.mouseEnterHandler = function (event)
    {
        var svgHandler = event.data.svgHandle;
        svgHandler.mouseMoveAndEnter = true;
        var target = event.target;
        if (!svgHandler.isValidEvent(event)) {
            this.style.cursor = 'default';
            clearTimeout(target.timer);
            // event.cancel();
            return;
        }
        else {
            this.style.cursor = 'pointer';
        }

        if (target.timer) {
            clearTimeout(target.timer);
            target.timer = null;
        }
        target.timer = setTimeout(function ()
        {
            svgHandler.mouseout = false;

            svgHandler.pressed(event);
        }, 1000);
    };

    this.hasDescriptionTag = function (descriptValue)
    {
        descriptValue = descriptValue || "";
        return descriptValue.toLowerCase() === 'desc';
    };

    this.isValidElement = function (event)
    {
        var descValue = '';
        if (Utils.notNull(event.target.tagName) && event.target.tagName !== 'svg') {

            if (event.target.tagName === 'g' && this.hasDescriptionTag($("desc", event.target).prop("tagName"))) {
                this.systemIdInDescAsAttribute = $(event.target.firstChild).attr('data-systemid');
                descValue = $("desc", event.target).first().text();
            }
            else if ($(event.target.parentNode).attr('id') !== 'viewport' &&
                    this.hasDescriptionTag($("desc", event.target.parentNode).prop("tagName"))) {
                this.systemIdInDescAsAttribute = $(event.target.parentNode.firstChild).attr('data-systemid');
                descValue = $("desc", event.target.parentNode).first().text()
            }
            else if (event.target.tagName === 'tspan' && event.target.parentNode &&
                event.target.parentNode.parentNode &&
                    this.hasDescriptionTag($("desc", event.target.parentNode.parentNode).prop("tagName"))) {
                this.systemIdInDescAsAttribute = $(event.target.parentNode.parentNode.firstChild).attr('data-systemid');
                descValue = $("desc", event.target.parentNode.parentNode).first().text()
            }
        }
        if (Utils.notNull(descValue) && descValue.match(/.*UID.*/)) {
            return descValue;
        }
        return false;
    };

	this.resetObjectHighlighting = function()
	{
		this.resetAttributesAndStack();
		this.signalDataArray = [];
		window.crossHighlightHandler.flushZoomedViews(this);
        mentor.publisher.selectedSystem.set("objectId", "",
                {silent: true});
		return;
	};

	this.pressed = function (evt)
    {
        var that = this;
        this.resetAttributesAndStack();
        setTimeout(function ()
        {
            alertMsg.removeAlertMsg();

            //If no target was hit, exit
            //If they've clicked on the div...
            if (evt.target.nodeName.toLowerCase() == 'div') {
                return;
            }

            evt.stopPropagation();

            var descValue = '';
            descValue = that.isValidElement(evt);
            if (descValue && Utils.notNull(descValue) && descValue.match(/.*UID.*/)) {
                that.handleHighlightEvents(evt, evt.type, descValue);
                return;
            }
            if ($(evt.target).parent().attr('id') === 'viewport' && $(evt.target)[0].tagName === 'rect' &&
                    window.heavySVGs) {
                that.svgElementVisibiltyToggler.showDesignLevelElements();
            }
            else if (!descValue) {
                if (window.heavySVGs) {
                    that.svgElementVisibiltyToggler.hideDesignLevelElements();
                }
				return that.resetObjectHighlighting();
			}

        }, 100)

    };

    this.handleHighlightEvents = function (evt, eventName, descValue)
    {
        var p = mentor.publisher;
        var x, y;
        x = evt.pageX;
        y = evt.pageY;

        if (window.opener && window.opener.mentor) {
            p = window.opener.mentor.publisher;
        }
        var offset = (window.heavySVGs && p.offset[this.svgContainerId]) || $("#" + this.svgContainerId).offset();
        if (typeof(descValue.split) == "undefined") {
            return;
        }
        if (eventName == 'mouseenter') {
            this.mouseHoverHighLight(descValue, evt);
        }
        else if (eventName == 'click') {
            window.isSVGClick = true;

            p.eventDispatcher.dispatchEvent(p.events.ALT_CLICK_TRIGGERED,
                {altKey: evt.altKey});

            if (descValue.indexOf("chs.cof.harness.diagram.HarnessDiagram") >= 0) {
                $(evt.target).parent().show();
            }
            var parts = descValue.split(' ');
            var cssClassName = descValue;
            if (!descValue || descValue.length < 2) {
                return;
            }
            var objectType = parts[0];
            var schematicUID = parts[1];
            var connectivityUID = parts[2];

            // user has clicked on a multicore. for multicores three uids are dumped in SVG, and 3rd it multicore uid
            if (objectType == 'chs.cof.logical.schem.CAFShieldBody' ||
                objectType == 'chs.cof.logical.schem.ShieldBody') {
                if (typeof(parts[3]) != "undefined") {
                    connectivityUID = parts[3];
                }
            }
            this.currentClickUID = connectivityUID;


            this.openPopup(x + offset.left, y + offset.top, descValue, evt.altKey);

            this.highlightOnMouseClick(descValue);
        }
        else if (eventName == 'dblclick') {
            window.isSVGClick = true;
            this.openPopup(x + offset.left, y + offset.top, descValue, evt.altKey);
        }
    };

    this.resetAttributes = function ()
    {
        try {

            for (var t = 0; t < this.originalNodes.length; t++) {
                var tuple = this.originalNodes[t];
                if (tuple[2] == "") {
                    //                tuple[2] = "black";
                    tuple[0].removeAttributeNS(null, tuple[1]);
                }
                else {
                    tuple[0].setAttributeNS(null, tuple[1], tuple[2]);
                }
            }
            this.originalNodes = [];
            this.lastUID = '';
        }
        catch (e) {
        }
    };

    this.debumpHighlightStack = function ()
    {
        //////console.log("debumpHighlightStack SVG");
        //Do nothing if there is nothing on the node stack
        if (this.nodesStack.length === 0) {
            this.resetAttributes();
            return;
        }
        //First clear any highlights
        this.resetAttributes();

        //Then restore the previous set
        this.originalNodes = this.nodesStack.pop();
    };

    this.mouseHoverHighLight = function (description, evt)
    {
        if (typeof(description.split) == "undefined") {
            return;

        }
        var parts = description.split(' ');
        var objectType = parts[0];
        var schematicUID = parts[1];
        var connectivityUID = parts[2];
        var renderedUIDs = this.getRenderedUIDs(description);
        var isRenderedSVG = false;
        if (renderedUIDs != null) {
            //do not signal trace in the rendered svg, not supported as of now.
            connectivityUID = renderedUIDs[1];
            var systemOfRenderedSVG = renderedUIDs[0];
            isRenderedSVG = true;
        }
        this.highlightObject(connectivityUID, mentor.publisher.colors[mentor.publisher.constants.orangeColorMsg],
            isRenderedSVG, evt);
        return;
    };

    this.highlightWholeSignalInRenderedSVG = function (signalName, color)
    {
        //////console.log("highlightWholeSignalInRenderedSVG SVG");
        var svgEventHandler = this;
        //        svgEventHandler.signalFlag = true;
        mentor.publisher.dataLoader.getSignalDataForHighlightInRenderedSVG(signalName, function (signalData)
        {
            signalData = signalData || {};
            if (signalData.textValue === 'success') {
                svgEventHandler.signalDataArray = signalData['dataArray']['objArray'];
                processSignals(svgEventHandler, svgEventHandler.signalDataArray, color);
            }
        });
    };

    this.highlightUid = function (connectivityUID, color, notToResetFlag)
    {
        var that = this;
        if (typeof(connectivityUID) == "undefined" || connectivityUID == null || connectivityUID.trim() == "") {
            return;
        }
        if (!notToResetFlag) {
            this.resetAttributesAndStackForEmptyClick();
            window.crossHighlightHandler.flushZoomedViews(this);
        }
        setTimeout(function ()
        {
            if (window.heavySVGs) {
                this.showHiddenGraphicsForElement(connectivityUID);
            }
            var svgElementPositionArray = that.doHighlighting(connectivityUID, color);
            if (svgElementPositionArray.length > 0) {
                that.setHighLightMap(svgElementPositionArray, notToResetFlag);
            }
            this.bumpHighlightStack();
        }.bind(this), 100);
    };

    this.convertData = function (data)
    {
        var xml = null;
        if (typeof data == "string") {
            xml = new ActiveXObject("Microsoft.XMLDOM");
            xml.async = false;
            xml.loadXML(data);
        }
        else {
            xml = data;
        }
        return xml;
    };

    this.showToolTipOnMouseover = function (objectData, evt) {
        objectData = objectData || {};
        if (objectData.getAttr) {
            var attr = objectData.getAttr("Description");
            if (attr) {
                mentor.publisher.toolTip.showToolTipForName(attr,
                        $("#" + this.svgContainerId).offset().left + evt.pageX,
                        $("#" + this.svgContainerId).offset().top + evt.pageY,
                        $('body').parent());

                mentor.publisher.toolTip.addCustomCSS({
                    'font-size': '13px',
                    'font-weight': 'normal'
                });
            }
        }
    };

    this.removeToolTipOnMouseleave = function(){
        mentor.publisher.toolTip.removeToolTip($('body').parent());
    };

    this.highlightObject = function (connectivityUID, color, isRenderedSVG, evt)
    {
        var svgEventHandler = this;
        require(["models/selectedSystem"], function (selectedSystem)
        {
            var systemId = selectedSystem.get("systemId");
            var objectData = mentor.publisher.project.loadObjectData(systemId, connectivityUID);
            var signalName;
            if (isRenderedSVG) {
                signalName = objectData.getGlobalSignal && objectData.getGlobalSignal();
            }
            else {
                signalName = objectData.getSignal ? objectData.getSignal() : "";
            }
            svgEventHandler.resetAttributesAndStack();
            setTimeout(function ()
            {
                svgEventHandler.signalFlag = true;
                if (signalName != "") {
                    if (!isRenderedSVG) {
                        svgEventHandler.highlightWholeSignal(signalName, color, systemId);
                    }
                    else {
                        svgEventHandler.highlightWholeSignalInRenderedSVG(signalName, color);
                    }
                }
                else {
                    svgEventHandler.resetAttributesAndStackForEmptyClick();
                    svgEventHandler.bumpHighlightStack();
                    svgEventHandler.signalDataArray.push(connectivityUID);
                    svgEventHandler.doHighlighting(connectivityUID, color);
                }
                var objectPathIn3D = (objectData.get3DViews && objectData.get3DViews()) || {};
                if (objectPathIn3D.objectId) {
                    window.crossHighlightHandler.zoomObjectIn3DXML(objectPathIn3D.objectId);

                }
            }, 100);

        });

    };

    this.highlightWholeSignal = function (signalName, color, systemId)
    {
        var svgEventHandler = this, index;
        this.resetAttributesAndStackForEmptyClick();
        this.bumpHighlightStack();
        var signalObjects = mentor.publisher.dataLoader.getSignalObjects(signalName, systemId) || [];
        processSignals(svgEventHandler, signalObjects, color);
    };

    this.resetAttributesAndStack = function ()
    {
        //////console.log("resetAttributesAndStack SVG");
        //Clear all stacked highlights
        while (this.nodesStack.length > 0) {
            this.debumpHighlightStack();
        }

        //Clear base highlights
        this.resetAttributes();
    };
    //todo delete
    this.resetAttributesAndStackForEmptyClick = function ()
    {

        this.resetAttributesAndStack();
    };

    this.openPopup = function (x, y, description, altClicked)
    {

        var flush, parts = description.split(' ');
        var objectType = parts[0];
        var schematicUID = parts[1];
        var connectivityUID = parts[2];


        // user has clicked on a multicore. for multicores three uids are dumped in SVG, and 3rd it multicore uid
        if (objectType == 'chs.cof.logical.schem.CAFShieldBody' ||
            objectType == 'chs.cof.logical.schem.ShieldBody') {
            if (typeof(parts[3]) != "undefined") {
                connectivityUID = parts[3];
            }
        }

        require(["routers/multipleDocumentRouter"], function(multipleDocumentRouter){
            multipleDocumentRouter.save(true, connectivityUID);
        });
        // For checking it is border for the SVG
        if (objectType == 'chs.cof.drawplus.CAFBorderHolder') {
            return;
        }

        var renderedUIDs = this.getRenderedUIDs(description);
        if (renderedUIDs != null) {
            connectivityUID = renderedUIDs[1];
            var sourcesystemuid = renderedUIDs[0];

            flush = false;
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLICKED_IN_SIGNAL_TRACE_VIEW, flush);
            if (altClicked) {
                this.renderOnAltClick(connectivityUID, sourcesystemuid, flush);
            }
            else {
                var popUpData = displayAttributes(schematicUID, connectivityUID, x, y, sourcesystemuid);
                popUpData.showPopUpPanel();
            }

            return;
        }
        else if (altClicked) {
            flush = true;
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLICKED_IN_SIGNAL_TRACE_VIEW, flush);
            var windowObj = window;
            if (window.opener && window.opener.mentor) {
                windowObj = window.opener;
            }
            windowObj.mentor.publisher.detailLayoutManager.resetContentPanel();
        }
        else {
            flush = true;
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLICKED_IN_SIGNAL_TRACE_VIEW, flush);
        }

        //for face view symbol SVG, system UID is written as second arg instead of schematicUID
        if ("FaceViewAbstractPin" == objectType) {
            sourcesystemuid = this.systemIdInDescAsAttribute;
            var popUpData = displayAttributes(schematicUID, connectivityUID, x, y, sourcesystemuid);
            popUpData.showPopUpPanel();

        }
        else {
            if (altClicked) {
                this.renderOnAltClick(connectivityUID, "", flush);
            }
            else {
                var popUpData = displayAttributes(schematicUID, connectivityUID, x, y);
                //show popup panel with the data loaded
                popUpData.showPopUpPanel();
            }

        }

        return;
    };

    this.showHiddenGraphicsForElement = function (connectivityUID) {
        var p = mentor.publisher;
        var actualIds = p.objectDataLoader.loadRefernceIdsIfAny("", connectivityUID, p.project.getId()) || [];
        if (!actualIds.forEach) {
            actualIds = [actualIds];
        }
        if (actualIds.indexOf(connectivityUID) == -1) {
            actualIds.push(connectivityUID);
        }
        if(window.heavySVGs) {
            this.svgElementVisibiltyToggler.toggleObjectLevelElementVisibility(actualIds);
        }
    };
    this.renderOnAltClick = function (connectivityUID, systemid, flush) {
        require(["models/selectedSystem","SignalTracerModel"], function (selectedSystem,signalTraceModel) {
            selectedSystem.set("objectId", connectivityUID, {silent: true});
            var p = mentor.publisher;
            if (window.opener && window.opener.mentor) {
                p = window.opener.mentor.publisher;
            }
            if (Utils.notNull(document.getElementById("RenderConnectivity") || window.renderInitialized)) {
                mentor.publisher.eventDispatcher.dispatchEvent(p.events.UPDATE_SIGNAL_TRACER,
                    {
                        systemId: systemid ||
                        selectedSystem.get("systemId"), id: connectivityUID, altKey: true, flush: flush
                    });
            }
            else {
                signalTraceModel.checkRendererAvailablility();
                initializeSignalRenderer(isHTTPProtocol());
                if(signalTraceModel.rendererLicenceAvaialable()) {
                    p.eventDispatcher.dispatchEvent(p.events.UPDATE_SIGNAL_TRACER,
                            {systemId: systemid || selectedSystem.get("systemId"), id: connectivityUID, flush: flush});
                }
            }
        });
    };

    this.getRenderedUIDs = function (description)
    {

        var parts = description.split(' ');
        var length = parts.length;
        var sourceDesignUID = '';
        var sourceObjectUID = '';
        for (k = length - 1; k >= 0; k--) {
            if (parts[k].indexOf('sourceDesignUID') != -1) {
                sourceDesignUID = parts[k].split(':')[1];
            }
            else if (parts[k].indexOf('sourceObjectUID') != -1) {
                sourceObjectUID = parts[k].split(':')[1];
            }
        }
        if (sourceDesignUID != '' && sourceObjectUID != '') {
            return [sourceDesignUID, sourceObjectUID];
        }
        else {
            return null;
        }
    };

    this.highlightOnMouseClick = function (description)
    {
        var signalPathFlag = false;
        var parts = description.split(' ');
        var objectType = parts[0];
        var schematicUID = parts[1];
        var connectivityUID = parts[2];
        var systemOfRenderedSVG = mentor.publisher.selectedSystem.get("systemId");

        // user has clicked on a multicore. for multicores three uids are dumped in SVG, and 3rd it multicore uid
        if (objectType == 'chs.cof.logical.schem.CAFShieldBody' ||
            objectType == 'chs.cof.logical.schem.ShieldBody') {
            if (typeof(parts[3]) != "undefined") {
                connectivityUID = parts[3];
            }
        }
        // For checking it is border for the SVG
        if (objectType == 'chs.cof.drawplus.CAFBorderHolder') {
            return;
        }

        var renderedUIDs = this.getRenderedUIDs(description);
        //todo handle render connectivity
        // var currentSystem = packageModel.get('currentFolder');
        if (renderedUIDs != null) {
            connectivityUID = renderedUIDs[1];
            systemOfRenderedSVG = renderedUIDs[0];
            //packageModel.set('currentFolder', systemOfRenderedSVG);
        }

        if (this.signalDataArray.length > 0) {
            for (var index in this.signalDataArray) {
                if (this.signalDataArray[index] === connectivityUID) {
                    signalPathFlag = true;
                    break;
                }
            }
        }

        this.resetAttributesAndStackForEmptyClick();

        //Store the current highlighting
        this.highlightUid(connectivityUID, mentor.publisher.colors[mentor.publisher.constants.redColorMsg]);

        if (signalPathFlag) {
            for (var index in this.signalDataArray) {
                if (this.signalDataArray[index] !== connectivityUID) {
                    this.doHighlighting(this.signalDataArray[index],
                        mentor.publisher.colors[mentor.publisher.constants.orangeColorMsg]);
                }
            }
        }
        else {
            this.signalDataArray = [];
        }
        this.bumpHighlightStack();

        window.crossHighlightHandler.initCrossHighlight(connectivityUID, this.svgContainerId, schematicUID,
            systemOfRenderedSVG);
        // packageModel.set('currentFolder', currentSystem);
    };

    this.bumpHighlightStack = function ()
    {
        this.nodesStack.push(this.originalNodes);
        this.originalNodes = [];
    };

    this.doHighlighting = function (uidToHighLight, color)
    {
        var svgElementPositionArray = [];

        //Highlighting the element only if uid has some value
        if (uidToHighLight != null && uidToHighLight != '') {
            this.leanSVG(uidToHighLight, svgElementPositionArray, color);
        }
        return svgElementPositionArray;
    };

    this.leanSVG = function (uidToHighLight, svgElementPositionArray, color)
    {
        var svgElementPosition = '';
        var hasObjectFoundInSVG = false;
        //Get all desc nodes
        var nodeList = this.root.getElementsByTagName('desc');
        var targetDesc = null;
        var targetNode = null;

        for (var i = 0; i < nodeList.length; i++) {
            targetDesc = nodeList.item(i);
            targetNode = targetDesc.firstChild.nodeValue;

            //Look for the UID in the description
            var attr = $(targetDesc)[0].id;
            var isTableElement = attr && attr.indexOf('tableGraphics') != -1;
            var matchedDesc = targetNode.indexOf(uidToHighLight) != -1;
            if (matchedDesc) {
                var containerGElement = targetDesc.parentNode;
                var shapes = $("line,circle,ellipse,polyline,polygon,path,rect", containerGElement);
                for (var ll = 0; ll < shapes.length; ll++) {
                    var firstshape = shapes[ll];
                    if (firstshape !== null && svgElementPosition === '') {
                        try {
                            this.setNewStyle(firstshape, color, false, uidToHighLight);

                        }
                        catch (error) {
                            //Ignore the exception
                        }
                    }
                }
                //the below check is to make sure that only those graphics elements which
                //have some graphics elements as children are added to the array
                //this array is used for zooming.
                if (Utils.notNull(shapes) && shapes.length > 0 && !isTableElement) {
                    svgElementPositionArray.push(containerGElement);
                }

                hasObjectFoundInSVG = true;

                var subGElement = containerGElement.getElementsByTagName('text');
                for (var j = 0; j < subGElement.length; j++) {
                    var subG = subGElement.item(j);
                    if (!isTableElement) {
                        svgElementPositionArray.push(subG.parentNode);
                    }
                    this.setNewStyle(subG, color, true, uidToHighLight);
                }
            }
        }
        if (hasObjectFoundInSVG) {
            this.lastUID = uidToHighLight;
        }
    };

    this.setNewStyle = function (ele, color, fill, UID)
    {
        //console.log(color);
        //console.log(fill);
        var elementStyle, strokeWidth, strokeWidthValue;
        if (typeof(fill) == 'undefined' || fill == 'undefined' || fill == null) {
            fill = false;
        }
        var originalStyle = $(ele).attr('style');

        var orgWidth = $(ele).attr('data-width');

        if (!orgWidth) {
            elementStyle = window.getComputedStyle(ele);
            strokeWidth = elementStyle.getPropertyValue('stroke-width');
            //this check is added for I.E, which does not have "px" in the stroke-width attr in computed style
            if (strokeWidth.indexOf("px") >= 0) {
                strokeWidthValue = strokeWidth.substring(0, strokeWidth.length - 2);
            }
            else {
                strokeWidthValue = strokeWidth;
            }
            // strokeWidthValue = strokeWidth.substring(0, strokeWidth.length - 2);
            $(ele).attr('data-width', strokeWidthValue);
            // $(ele).attr('data-style', originalStyle);
        }
        else {
            // $(ele).attr('style', "");
            // $(ele).attr('style', $(ele).attr('data-style'));
            strokeWidthValue = $(ele).attr('data-width');
        }

        //this.lastUID = UID;

        if (fill) {
            this.resetableSetAttribute(ele, 'style', originalStyle + ';fill:' + color + ';stroke:' + color);
        }
        else {
            if (UID != this.lastUID) {

                //  var elementStyle = window.getComputedStyle(ele);
                // var strokeWidth = elementStyle.getPropertyValue('stroke-width');
                // var strokeWidthValue = strokeWidth.substring(0, strokeWidth.length-2);
                if (strokeWidthValue) {
                    strokeWidthValue =
                        (strokeWidthValue * mentor.publisher.colors[mentor.publisher.constants.strokeWidth]);
                    this.resetableSetAttribute(ele, 'style',
                        originalStyle + ';stroke:' + color + ";" + ";stroke-width:" + strokeWidthValue + "px;");
                }

                //this.lastUID = UID;
                return;
            }
            this.resetableSetAttribute(ele, 'style', originalStyle + ';stroke:' + color + ";");
        }
    };

    this.setHighLightMap = function (svgElementPositionArray, notToResetFlag)
    {
        var that = this;
        require(["SelectedObjectsStore"], function (store)
        {
            //var container, svgContainerObject, viewHeight, viewWidth;
            //container = document.getElementById(that.svgContainerId);
            //svgContainerObject = container.getElementsByTagName('object')[0];
            //viewHeight = svgContainerObject.clientHeight || $(svgContainerObject).innerHeight();
            //viewWidth = svgContainerObject.clientWidth || $(svgContainerObject).innerWidth();
            store.addObjectsForContainer(that.svgTransformModel, svgElementPositionArray, notToResetFlag);
        });

        // display zoomSlider in touch devices
        var p = mentor.publisher;
        p.eventDispatcher.dispatchEvent(p.events.SHOW_SLIDER, {
            containerId: this.svgTransformModel.get('svgContainerId')
        });
    };

    this.resetableSetAttribute = function (theNode, attribute, newValue)
    {
        var tuple = [theNode, attribute, theNode.getAttributeNS(null, attribute)];

        this.originalNodes.push(tuple);

        theNode.setAttributeNS(null, attribute, newValue);
    };

    this.highlightUids = function (objectUids, color)
    {
        var svgElementPositionArray = [];
        this.resetObjectHighlighting();
        //Store the current highlighting
        this.bumpHighlightStack();
        for (var childIndex in objectUids) {
            var uidToHighlight = objectUids[childIndex];
            this.highlightUid(uidToHighlight, color, true);

        }
    };

    var processSignals = function (svgEventHandler, signalData, color)
    {
        if (signalData.textValue !== 'failure') {
            svgEventHandler.signalDataArray = signalData;
            for (var index in svgEventHandler.signalDataArray) {
                svgEventHandler.doHighlighting(svgEventHandler.signalDataArray[index], color);
            }
        }
    };

    this.zoomFit = function ()
    {
        var container, svgContainerObject, containerId = this.svgTransformModel.get('svgContainerId'), clientHeight, clientWidth;
        container = document.getElementById(containerId);
        //todo even when the pane is removed, we still have this handler, and on re-size, this method gets called.
        svgContainerObject = container ? container.getElementsByTagName('object')[0] : null;
        if (!svgContainerObject) {
            return;
        }
        var panelSize = mentor.publisher.panelSize;
        if (window.heavySVGs && panelSize && panelSize[containerId]) {
            clientHeight = panelSize[containerId].height || svgContainerObject.clientHeight ||
                    $(svgContainerObject).innerHeight();
            clientWidth = panelSize[containerId].width || svgContainerObject.clientWidth ||
                    $(svgContainerObject).innerWidth();
        }
        else {
            clientHeight = svgContainerObject.clientHeight || $(svgContainerObject).innerHeight();
            clientWidth = svgContainerObject.clientWidth || $(svgContainerObject).innerWidth();
        }
        this.svgTransformModel.set({'clientWidth': clientWidth, 'clientHeight': clientHeight});
        this.svgTransformModel.fit();
    };

    //todo remove duplicates from the above method
    this.zoomLockedView = function ()
    {
        var container, svgContainerObject, containerId = this.svgTransformModel.get('svgContainerId'), clientHeight, clientWidth;
        container = document.getElementById(containerId);
        //todo even when the pane is removed, we still have this handler, and on re-size, this method gets called.
        svgContainerObject = container ? container.getElementsByTagName('object')[0] : null;
        if (!svgContainerObject) {
            return;
        }
        var containerDiv$ = $(svgContainerObject).parent();
        clientHeight = containerDiv$.height() || svgContainerObject.clientHeight || $(svgContainerObject).innerHeight();
        clientWidth = containerDiv$.width() || svgContainerObject.clientWidth || $(svgContainerObject).innerWidth();
        this.svgTransformModel.set({'clientWidth': clientWidth, 'clientHeight': clientHeight});
        this.svgTransformModel.fitLockedView();
    };

    this.longPressHandler = function (evt) {
        var descValue = this.isValidElement(evt);
        if (descValue) {
            try {
                this.mouseHoverHighLight(descValue, evt);
            }
            catch (e) {
                //ignore;
            }
        }
    }
};

var transformProp = (function (props)
{
    var style = document.documentElement.style;
    for (var i = 0; i < props.length; i++) {
        if (props[i] in style) {
            return {
                transform: 'transform',
                WebkitTransform: '-webkit-transform',
                OTransform: '-o-transform',
                MozTransform: '-moz-transform',
                msTransform: '-ms-transform'
            }[props[i]];
        }
    }
    return false;
})(['transform', 'WebkitTransform', 'OTransform', 'MozTransform', 'msTransform']);
/**
 * Sets the current transform matrix of an element.
 */
function setCTM(element, matrix)
{
    var s = "matrix(" + matrix.a + "," + matrix.b + "," + matrix.c + "," + matrix.d + "," + matrix.e + "," +
            matrix.f +
            ")";

    //element.setAttribute("transform", s);
    setCTMString(element, s);
    $(element).data("ctm", matrix);
    //element.style.cssText = transformProp + ":"+s;
    //element.style[transformProp] = s;
}

function setCTMString(element, s)
{
    element.setAttribute("transform", s);
}

/**
 * Sets attributes of an element.
 */
function setAttributes(element, attributes)
{
    for (i in attributes) {
        element.setAttributeNS(null, i, attributes[i]);
    }
}

function moveZoomSlider(zoomx, zoomy, svgContainerId)
{
    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.ZOOM_TRIGGERED,
            {zoomx: zoomx, containerId: svgContainerId});
}

function getCurrentZoomValue(svgContainerId)
{
    var htmlText = $('.diagramControl .component-label', $('#' + svgContainerId).parent()).html() ||
            "", zoomcurrVal = htmlText.replace('%', '');
    return zoomcurrVal;
}

//to check if further zoom is allowed if zoomed in/out.
function checkIfZoomAllowed(zoomcurrVal, zoomIn)
{
    if (zoomcurrVal >= mentor.publisher.constants.MaxZoomPercentage && zoomIn) {
        return false;
    }
    else if (zoomcurrVal <= mentor.publisher.constants.MinZoomPercentage && !zoomIn) {
        return false;
    }
    return true;
}

//the following function calculates the slider -step to be incremented
//given a zoomfactor, it reverse-engineers whatever is done in the calculateZoomFactor method.
function calculateSliderStep(zoomFactor)
{
    var positiveZoomPerUnit = 1.164993050750713;
    var negetiveZoomPerUnit = 0.858374218932557;
    var zoomPerUnit;
    if (zoomFactor > 0) {
        zoomPerUnit = positiveZoomPerUnit;
    }
    else {
        zoomPerUnit = negetiveZoomPerUnit;
    }
    return (Math.log(zoomFactor)) / (Math.log(zoomPerUnit));
}
;

function calculateZoomFactor(diff)
{

    //the calculation in the above commecnted code for delta of mouse wheel
    //would result in the zoomfactor which is given below for zoomin and zoomout
    //respectively.The same value will be used as per-unit-zoom-facot also in case of
    //zoom using slider and +/- buttons adjacent to slider.
    var zoomPerUnit;
    if (diff > 0) {
        zoomPerUnit = mentor.publisher.constants.PositiveZoomPerUnit;
    }
    else {
        zoomPerUnit = mentor.publisher.constants.NegetiveZoomPerUnit;
    }
    //zoomGlobal = zoomGlobal * Math.pow(zoomPerUnit, Math.abs(diff));
    return Math.pow(zoomPerUnit, Math.abs(diff));
}
