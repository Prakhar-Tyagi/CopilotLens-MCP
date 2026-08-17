/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
var renderer;
var cancelled;
var dialog;
require(['views/component/IndeterminateProgressDialog'], function (IndeterminateProgressDialog) {
    dialog = new IndeterminateProgressDialog({
        title: mentor.publisher.languageTranslator.localize(
                'TroubleshootingPanel.GenerateDiagram.Progress.Title'),
        message: mentor.publisher.languageTranslator.localize(
                'TroubleshootingPanel.GenerateDiagram.Progress.Message'),
        cancel: mentor.publisher.languageTranslator.localize('Cancel'),
        guidance: mentor.publisher.languageTranslator.localize(
                'TroubleshootingPanel.GenerateDiagram.Progress.ErrorGuidance'),
        implication: mentor.publisher.languageTranslator.localize(
                'TroubleshootingPanel.GenerateDiagram.Progress.ErrorImplication'),
        onCancelFn: function () {
            cancelled = true;
        },
    });
});

//this.flushConnectivity(this.flush);
function isHTTPProtocol()
{
    return (window.location.href).indexOf("http") !== (-1);
}
;

function initializeSignalRenderer(webProtocol, shouldNotDestroySession)
{
    //if the renderer is not already initialized
    //initiialize it based on the protocol, if its http then renderer is webbased
    //else it is standalone
    if (!renderer) {
            renderer = new WebBasedSignalRenderer();
        }

}

function displayConnectivity(connectivityFile, popOut, flushRenderConnectivity, titleToShow, connectivityUID, designID,
        isFullInstance)
{
    initializeSignalRenderer(isHTTPProtocol());
    LoadMask.removeLoadMask();
    //todo for ground signal tracer, the flush should happen in the same state machine?
    if (!renderer.doCheck()) {
        return;
    }
    //todo for ground signal tracer, the licence should be checked here?
    if (!connectivityFile) {
        return;
    }
    /*
     if the request is to pop-out, do not flush the signal data in the current window.
     */
    if ((!Utils.notNull(flushRenderConnectivity) || flushRenderConnectivity) && !popOut) {
        flushRenderedData();
    }

    var fullInstance = false;
    if (mentor.publisher.constants.FullInstanceTitle === titleToShow) {
        fullInstance = true;
    }
    titleToShow = "Build";
    var p = mentor.publisher;
    var activeProject = p.project.getId();
    var connectivityRoot = activeProject + "/Signals/";
    //the action triggerend to show in a pop-out
    if (popOut) {
        mentor.publisher.popoutHandler.openPopout("popout.html#/renderSignal/" + connectivityFile + "/" +
                activeProject.replace("\\", "/"));
    }
    else {

        require(['views/component/IndeterminateProgressDialog'], function (IndeterminateProgressDialog) {
            dialog = new IndeterminateProgressDialog({
                title: mentor.publisher.languageTranslator.localize(
                        'TroubleshootingPanel.GenerateDiagram.Progress.Title'),
                message: mentor.publisher.languageTranslator.localize(
                        'TroubleshootingPanel.GenerateDiagram.Progress.Message'),
                cancel: mentor.publisher.languageTranslator.localize('Cancel'),
                guidance: mentor.publisher.languageTranslator.localize(
                        'TroubleshootingPanel.GenerateDiagram.Progress.ErrorGuidance'),
                implication: mentor.publisher.languageTranslator.localize(
                        'TroubleshootingPanel.GenerateDiagram.Progress.ErrorImplication'),
                onCancelFn: function () {
                    cancelled = true;
                },
            });
            dialog.show();
        });

        renderConnectivityInWindow(document, connectivityFile, connectivityRoot, connectivityUID,
                isFullInstance,
                designID);
    }

}

/**
 * @param d is the document where to render connectivity
 * @param connectivityFile is the connectivity file which the applet should use
 * @param connectivityRoot is the root of the package connectivity
 */
function renderConnectivityInWindow(d, connectivityFile, connectivityRoot, connectivityUID, isFullInstance, designID)
{
    loadConnectivityFile(connectivityFile, connectivityUID, isFullInstance, designID);
}

function loadConnectivityFile(connectivityFile, connectivityUID, isFullInstance, designID)
{
    var projectRoot = mentor.publisher.project.getId();
    var signalFilePath = projectRoot + "/Signals/" + connectivityFile;

    var projectpreferences = mentor.publisher.dataLoader.getProjectPreferences();
    require(['views/component/IndeterminateProgressDialog'], function (IndeterminateProgressDialog) {
        dialog = new IndeterminateProgressDialog({
            title: mentor.publisher.languageTranslator.localize(
                    'TroubleshootingPanel.GenerateDiagram.Progress.Title'),
            message: mentor.publisher.languageTranslator.localize(
                    'TroubleshootingPanel.GenerateDiagram.Progress.Message'),
            cancel: mentor.publisher.languageTranslator.localize('Cancel'),
            guidance: mentor.publisher.languageTranslator.localize(
                    'TroubleshootingPanel.GenerateDiagram.Progress.ErrorGuidance'),
            implication: mentor.publisher.languageTranslator.localize(
                    'TroubleshootingPanel.GenerateDiagram.Progress.ErrorImplication'),
            onCancelFn: function () {
                cancelled = true;
            },
        });
        dialog.show();
    });
    cancelled = false;
    signalFileLoaded(signalFilePath, connectivityUID, isFullInstance, designID, projectpreferences);
}

//function signalFileLoaded(data, textStatus, XMLHttpRequest)
function signalFileLoaded(signalFilePath, connectivityUID, isFullInstance, designID, projectpreferences)
{
    renderer.signalFileLoaded(signalFilePath, connectivityUID, isFullInstance, designID, projectpreferences);
}
function displayRenderedSVG(svgFilePath, connectivityUID, designID, title)
{
    //the panel title is got from the package model.
    //var panelTitle = packageModel.getRenderPanelTitle();
    //alert("displayRenderedSVG...."+svgFilePath);
    var panelTitle = 'Build';
    if (Utils.getUrlParameter('uri') !== '' || Utils.getUrlParameter('popout') == 'yes') {
        //panelTitle = window.opener.packageModel.getRenderPanelTitle();
        panelTitle = 'Build';
    }
    panelTitle = title || panelTitle;
    loadRenderedSVG("cns/temp/" + svgFilePath, panelTitle, connectivityUID, designID);
}
function displayErrorInRenderedSVG(msg)
{
    if (msg == "AccessDenied") {
        displayErrorPopup(msg);
    }
    else {
        alert(msg);
    }
    LoadMask.removeLoadMask();
}

function displayErrorPopup(errorMsg){
    require(["views/component/ModalDialog"], function (ModalDialog) {
        var modalDialog = new ModalDialog({
            title: mentor.publisher.languageTranslator.localize(errorMsg+".title"),
            message: mentor.publisher.languageTranslator.localize(errorMsg+".message"),
            implication: mentor.publisher.languageTranslator.localize(errorMsg+".implication"),
            guidance: mentor.publisher.languageTranslator.localize(errorMsg+".guidance"),
            primaryButton: "OK",
            secondaryButton: "Cancel",
            dialogFlag: mentor.publisher.modalDialogFlag.ERROR,
            onConfirmFn: onConfirm.bind(this),
            onCancelFn: function () {}.bind(this)
        });
        modalDialog.show();
    });
    function onConfirm()
    {
        $(event.currentTarget).closest(".listItem").remove();
    }
}

function loadRenderedSVG(svgFilePath, signalName, connectivityUID, designID)
{

    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_RENDEREDSIGNAL,
            {mainText: signalName, path: svgFilePath});
    //for closing the object pop-over for alt+click
    setTimeout(function ()
    {
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER,
                {});
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.HIGHLIGHT_TRACED_SIGNAL, {});
    }, .1);
}

function deleteRenderedFile(svgUrl)
{
    renderer.deleteRenderedFile(svgUrl);
}

function flushRenderedData()
{
    renderer.flushRenderedData();
}
//| seperated options for the active configurations.
function getCurrentConfigurationData()
{
    //var options = packageModel.getSelectedOptions();
    //todo it turns out that this is not the correct way to get the options
    //todo need to find the correct way and fix it.
    var options = window.opener && window.opener.mentor ? window.opener.mentor.publisher.filter.vinOptions :
            mentor.publisher.filter.vinOptions;
    var optionsToSend = '';
    if (Utils.notNull(options)) {
        if (options instanceof Array) {
            for (k = 0; k < options.length; k++) {
                if (k !== options.length - 1) {
                    optionsToSend = options[k].value + '|';
                }
                else {
                    optionsToSend = options[k].value;
                }
            }
        }
        else {
            optionsToSend = options;
        }
    }
    return optionsToSend;
}
;
function getProjectName() {
    return new Promise((resolve) => {
        require(["UserSession"], function (userSession) {
            var selectedProject = userSession.getActiveSession().get(userSession.kSelectedProjectProperty);
            if (selectedProject) {
                resolve(selectedProject.get('mainText'));
            }
        });
    });
}
;
function loadMaskForRendererApplet()
{
    if (Utils.notNull($('#' + 'locationViewSVGLoadArea')) && ($('#' + 'locationViewSVGLoadArea').length == 1)) {

        LoadMask.addLoadMask('locationViewSVGLoadArea');
    }
    else if (Utils.notNull($('#' + 'systemSVGLoadArea')) && ($('#' + 'systemSVGLoadArea').length == 1)) {

        LoadMask.addLoadMask('systemSVGLoadArea');
    }
    else if (Utils.notNull($('#' + 'detail')) && ($('#' + 'detail').length == 1)) {

        LoadMask.addLoadMask('detail');
    }
}
;

mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.GROUND_PATH_TRACE, function (evt)
{
    var connectivityFile = evt.detail.id;
    //todo need to move this to the right place, and also get connectivityUID, designId, fullInstance values to pass
    displayConnectivity(connectivityFile, false, true, 'Build');
});
