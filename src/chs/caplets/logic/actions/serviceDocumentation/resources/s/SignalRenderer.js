var SignalRenderer = function ()
{
    this.doCheck = function ()
    {
    };

    this.signalFileLoaded = function (signalFilePath, connectivityUID, isFullInstance, designID)
    {
    };

    this.regenerateSVG = function ()
    {
    };

    this.deleteRenderedFile = function (svgUrl)
    {
    };

    /*
     this function is a synchronous function
     to prevent signal tracing before the session is flushed
     */
    this.flushRenderedData = function ()
    {
    };

    this.destroySession = function ()
    {
    };
};

/*
 This is the web based signal trace renderer
 which communicates with server to fetch the data
 */
var WebBasedSignalRenderer = function ()
{
    this.flush = false;

    this.renderSVG = function (renderurl, connectivityUID, designID)
    {
        var t = this;
        $.ajax({async: true, url: Utils.prepareFilePath(renderurl), success: function (data, textStatus, XMLHttpRequest)
        {
            if (cancelled) {
                return;
            }
            t.writeLog(XMLHttpRequest.responseText);
            displayRenderedSVG(XMLHttpRequest.responseText, connectivityUID, designID);
        }, error: function (data, textStatus, XMLHttpRequest) {
                t.writeLog(textStatus);
                t.writeLog(data.responseText);
                displayErrorInRenderedSVG(data.responseText);
            }, dataType: (Utils.is_msie()) ? "text" : "html"
        }).always(dialog.close.bind(dialog));
    };

    this.loadFromServer = async function (signalFilePath, connectivityUID, isFullInstance, designID, projectpreferences)
    {
        this.writeLog("connectivity file name" + signalFilePath);
        var styleSetFile = 'styleset.xml';
        var renderurl = "render?connectivityXml=" + signalFilePath;
        renderurl = renderurl + "&viewerStyleSet=" + styleSetFile;
        renderurl = renderurl + "&fullInstance=" + isFullInstance;
        renderurl = renderurl + "&clickedUID=" + connectivityUID;
        renderurl = renderurl + "&clickedSchemUID=" + "";
        renderurl = renderurl + "&options=" + getCurrentConfigurationData();
        renderurl = renderurl + "&hookupConnectOntoMulticore=" + projectpreferences.hookupConnectOntoMulticore;
        renderurl = renderurl + "&hookupConnectOntoOverbraid=" + projectpreferences.hookupConnectOntoOverbraid;
        const projectName = await getProjectName();
        renderurl = renderurl + "&projectName=" + projectName;
		renderurl = renderurl + "&rand=" + (Math.random());
        if (this.getFlush()) {
            this.setFlush(false);
            renderurl = renderurl + "&flush=true";
        }
        this.writeLog(renderurl);
        this.renderSVG(renderurl, connectivityUID, designID);
    };

    this.setFlush = function (f)
    {
        this.flush = f;
    };

    this.getFlush = function ()
    {
        return this.flush;
    };

    this.writeLog = function (message)
    {
        if (window.console) {
            window.console.log(message);
        }
    };
};
WebBasedSignalRenderer.prototype = new SignalRenderer();
WebBasedSignalRenderer.prototype.doCheck = function ()
{
    return true;
};
WebBasedSignalRenderer.prototype.flushRenderedData = function ()
{
    this.setFlush(true);
};
WebBasedSignalRenderer.prototype.deleteRenderedFile = function (svgUrl)
{
    var deleteFileServletUrl = "deleteRenderedFile?fileName=" + encodeURIComponent(svgUrl);
    $.ajax({async: false, url: deleteFileServletUrl});
};
WebBasedSignalRenderer.prototype.regenerateSVG = function ()
{
	var that = this, renderurl = this.getRegenerateURL();
    require(["models/selectedSystem"], function (selectedSystem)
    {
        that.renderSVG(renderurl, selectedSystem.get("objectId"), selectedSystem.get("systemId"));
    });

};
WebBasedSignalRenderer.prototype.getRegenerateURL = function(){
	return "render?regenerate=true&rand=" + (Math.random());
};
WebBasedSignalRenderer.prototype.signalFileLoaded =
        function (signalFilePath, connectivityUID, isFullInstance, designID, projectpreferences)
        {
            this.loadFromServer(signalFilePath, connectivityUID, isFullInstance, designID, projectpreferences);
        };
WebBasedSignalRenderer.prototype.destroySession = function ()
{
    var destroy = "destroy";
    $.ajax({async: false, url: destroy});
};