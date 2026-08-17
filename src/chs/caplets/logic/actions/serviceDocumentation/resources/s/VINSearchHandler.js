/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

var VINClient = function ()
{
    this.initialize = function ()
    {
        return true;
    },

            this.fetchVIN = function (serverUrl, proxyUrl, proxyPort, username, password, vinNumber,secureVin, rootCertsPath)
            {
            }
};

var ServletVINClient = function ()
{
};
ServletVINClient.prototype = new VINClient();
ServletVINClient.prototype.initialize = function ()
{
    return true;
}
ServletVINClient.prototype.fetchVIN = function (serverUrl, proxyUrl, proxyPort, username, password, vinNumber, secureVin, rootCertsPath)
{
    var headers = {};


    if (username && password) {
        const encodedCredentials = btoa(username + ":" + password);
        headers['Authorization'] = 'Basic ' + encodedCredentials;
    }
    $.ajax({
        //async: false,
        data: {
            "serverUrl": serverUrl,
            "proxyUrl": proxyUrl,
            "port": proxyPort,
            "vin": vinNumber,
            "secureVin": secureVin,
            "rootCertsPath" : rootCertsPath
        },
        headers:headers,
        type: "POST",
        url: "vin",
        error: function (xhr, status, errorThrown)
        {
            vinServerError(xhr.responseText);
        },
        success: function (data, status, xhr)
        {
            vinServerResponse(data, username, password, vinNumber);
        }
    });
};

var vinClient;

initializeVINClient();

function initializeVINClient()
{
    vinClient = new ServletVINClient();
}

/**
 * This is the class that handles the Login Panel for VIN Search.
 * Based on the Login and Password submission, handling will be performed.
 * @param itemDetails
 */
var vinSearchHandlerInstance = '';

var VINSearchHandler = function (itemDetails)
{

    vinSearchHandlerInstance = this;
    this.vinNumber = itemDetails.vinNumber;
    var updatedVinNumber = itemDetails.vinNumber;
    var onError = itemDetails.onError;

    this.showLoginPopup = function ()
    {
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_VIN_LOGIN_POPUP, {});
        setTimeout(function ()
        {
            vinSearchHandlerInstance.registerLoginEvents();
        }, 100);
    };

    this.registerLoginEvents = function ()
    {
        var loginPopover = $("#loginPopover").show(), btncancel, $console;
        $console = $("#login_error_message");
        $("#loginPopover").off('keypress');
        $("#loginPopover").on("keypress", function (e)
        {
            if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
                vinSearchHandlerInstance.checkUserNamePassword(vinSearchHandlerInstance.serverUrl,
                        vinSearchHandlerInstance.proxyUrl, vinSearchHandlerInstance.proxyPort);
            }
        });

        btncancel = $('<div class=standard_button_left><input id="login_cancel" type="button" value="Cancel"></div>');
        $("#login_cancel").off();
        $("#login_cancel").on("click", function ()
        {
            clearLoginPopup();
        });
        $("#detail").off();
        $("#detail").on("mousedown", function ()
        {
            clearLoginPopup();
        });

        $('.iesdLoginPopupFooter').append($console);
        $("#username").focus();
        $("#login_statusMsg").html("");
        registerLoginAndCancelEvents(vinSearchHandlerInstance.serverUrl,
                vinSearchHandlerInstance.proxyUrl, vinSearchHandlerInstance.proxyPort);
    };

    /**
     *  This method loads the username data for the current system
     */

    this.loadUserNamePanel = function ()
    {
        vinSearchHandlerInstance.userNamePanel.setTitle(mentor.publisher.languageTranslator.localize('Username'));
        var panelListeners = mentor.publisher.PanelListener({
            titlePanelClicked: function (panelItem)
            {
                window.hidePopup = false;
                if (vinSearchHandlerInstance.userNamePanel.isActive()) {
                    vinSearchHandlerInstance.userNamePanel.setActive(false);

                }
                else {
                    vinSearchHandlerInstance.userNamePanel.setActive(true);
                }
            },
            panelItemClicked: function (item)
            {
                //Edit User Name
            }
        });
        vinSearchHandlerInstance.userNamePanel.addListeners(panelListeners);
        //  packageModel.setVinUserName("");
        var somearray = new Array();
        userNameObj = {};
        // userNameObj['name'] = packageModel.getVinUserName();
        somearray.push(userNameObj);

        vinSearchHandlerInstance.userNamePanel.appendEditable(somearray, false, "username", "text");
        vinSearchHandlerInstance.popupContent.append(vinSearchHandlerInstance.userNamePanel.getPanel());
        vinSearchHandlerInstance.userNamePanel.setActive(false);

    };

    this.checkUserNamePassword = function (serverUrl, proxyUrl, proxyPort)
    {
        var username = "";
        var password = "";
        this.loginAndFetchVinOptions(serverUrl, proxyUrl, proxyPort, username, password);
    };
    this.readVINOptionsFromLocalVINData = function (username, password)
    {
        clearLoginPopup();
        var VIN = vinSearchHandlerInstance.vinNumber;

        if (typeof(VIN) != "undefined" && VIN != null) {
            var options = localVInData[vinSearchHandlerInstance.vinNumber.trim().toLowerCase()];
            if (typeof(options) == "undefined" || options == null || options.trim() == "") {
                vinServerError("noVinOptionsFound");
            }
            else {
                vinServerResponse(options, username, password, VIN);
            }
        }
        else {
            vinServerError("noVinOptionsFound");
        }
    };

    this.showLoadingVinMessage = function ()
    {

        alertMsg.showMessageWithLoadingImage(
                getLoadingMessage(mentor.publisher.languageTranslator.localize('fetchingvindata')),
                "loading");

    };
    this.fetchVehicalOptions = function (serverUrl, proxyUrl, proxyPort, username, password, vinNumber)
    {
        this.showLoadingVinMessage();
        this.username = username;
        this.password = password;
        if (this.uselocalVin() === 'true') {
            vinSearchHandlerInstance.readVINOptionsFromLocalVINData(username, password);
            return false;
        }
        this.setCredentialsAndVin(serverUrl, proxyUrl, proxyPort, username, password, vinNumber);
    };

    this.uselocalVin = function ()
    {
        return mentor.publisher.urlParams.uselocalvin;
    };

    this.setCredentialsAndVin = function (serverUrl, proxyUrl, proxyPort, username, password, vinNumber)
    {
        var vinApplet = document.getElementById('vinApplet');
        var secureVin = this.secureVin;
        var rootCertsPath = this.rootCertsPath;
        vinClient.fetchVIN(serverUrl, proxyUrl, proxyPort, username, password, vinNumber,secureVin, rootCertsPath);
    };

    this.getMessage = function (message)
    {
        return mentor.publisher.languageTranslator.localize(message);
    };

    this.getGuidance = function (message)
    {
        return mentor.publisher.languageTranslator.localize(message+".guidance");
    };

    this.getTitle = function (message)
    {
        return mentor.publisher.languageTranslator.localize(message+".title");
    };

    this.getImplication = function (message)
    {
        return mentor.publisher.languageTranslator.localize(message+".implication");
    };

    this.showServerFailureMessage = function (error,implication,guidance,title,errorMessage)
    {
        var loginPopup = document.getElementById('login_statusMsg');
        if (error!="" || error!=null && !error.includes(" ")) {
            this.createProgressDialog(error,implication,guidance,title,errorMessage);
            vinSearchHandlerInstance.popup && vinSearchHandlerInstance.popup.hidePopup();
            window.hidePopup = false;
        }
    };

    this.createProgressDialog = function (error,implication,guidance,title,errorMessage)
    {
        var that=this;
        require(['views/component/IndeterminateProgressDialog'],function(IndeterminateProgressDialog) {
            const dialog = new IndeterminateProgressDialog({
                title: '',
                message: '',
                cancel: mentor.publisher.languageTranslator.localize('Cancel'),
                guidance: '',
                implication: '',
                onCancelFn: function () {
                    dialog.close();
                }
            });
            dialog.onError({
                title: title,
                message: error,
                cancel: mentor.publisher.languageTranslator.localize(
                    'TroubleshootingPanel.GenerateDiagram.Progress.Close'),
                guidance: guidance,
                implication: implication,
                onCancelFn: function () {
                    dialog.close();
                }
            });
        });
    }

    /**
     *  reports server communication errors to user
     *
     * @param errorMessage errorMessage sent from server
     */
    this.vinServerError = function (errorMessage)
    {
        /**
         * our defalut server implementation sends error message code
         * which is internationalized at javascript side
         */

        var error = this.getMessage(errorMessage), loadFromLocalVinFile;
        loadFromLocalVinFile = false;
        var implication = this.getImplication(errorMessage);
        var guidance= this.getGuidance(errorMessage);
        var title= this.getTitle(errorMessage);
        /**
         * if there is no server started then load from local VIN file
         */
        if (errorMessage === "failedToConnectToServer" || errorMessage === "inValidSOAPResponse" ||
                errorMessage === "internalServerError") {
            loadFromLocalVinFile = true;
        }

        /**
         * no entry found in resurces.properties, show the error message as it is
         */
        if (typeof(error) === "undefined" || error.trim() == "") {
            error = errorMessage;
        }

            if (loadFromLocalVinFile) {
            vinSearchHandlerInstance.readVINOptionsFromLocalVINData(this.username, this.password);
        }
        else {
            this.showServerFailureMessage(error,implication,guidance,title,errorMessage);
        }
    };

    this.loginAndFetchVinOptions = function (serverUrl, proxyUrl, proxyPort, username, password)
    {

        this.fetchVehicalOptions(serverUrl, proxyUrl, proxyPort, username, password,
                vinSearchHandlerInstance.vinNumber);
    };

    this.loadVINDataFromLocalFile = function ()
    {
        if (localVInData != null) {
            return true;
        }
        var localVINFile = "vinOptions.xml";
        var VINdataExists = false;
        $.ajax({
            url: localVINFile,
            success: function (data, textStatus, XMLHttpRequest)
            {
                $("vin", data).each(function ()
                {
                    VINdataExists = true;
                    if (localVInData == null) {
                        localVInData = {};
                    }
                    localVInData[$(this).attr('id').toLowerCase()] = $(this).text();
                });
            }, error: function (XMLHttpRequest, textStatus, errorThrown)
            {
                return false;
            }, dataType: (Utils.is_msie()) ? "text" : "xml", async: false
        });

        return VINdataExists;
    };

    function isUserLoginRequired()
    {
        var isLoginRequired = mentor.publisher.config["VIN-server-login-required"];
        return !isLoginRequired || isLoginRequired === "true";
    }

    this.init = function ()
    {
        /**
         * user has entered empty string
         */
        if (vinSearchHandlerInstance.vinNumber == null || "" == vinSearchHandlerInstance.vinNumber.trim()) {
            return false;
        }
        var username = '';
        var password = '';
        if (!isUserLoginRequired()) {
            username = 'ANONYMOUS';
            password = 'ANONYMOUS';
        }
        //packageModel.setVinNumber(vinSearchHandlerInstance.vinNumber);

        var configUrl = "vinServerConfig.xml";
        var serverUrl = '';
        var proxyUrl = '';
        var proxyPort = '';
        var rootCertsPath = '';
        var secureVin = '';
        var doesLocalVInExists = this.loadVINDataFromLocalFile();
        //  if (doesLocalVInExists == false) {
        $.ajax({
            url: configUrl,
            success: function (data, textStatus, XMLHttpRequest)
            {
                serverUrl = $("server-url", data).first().text();
                proxyUrl = $("proxy-host", data).first().text();
                proxyPort = $("proxy-port", data).first().text();
                rootCertsPath = $("server-config",data).attr("rootCerts");
                secureVin = $("secure-config",data).attr("secure");


            }, error: function (XMLHttpRequest, textStatus, errorThrown)
            {
                $("#login_statusMsg").html(mentor.publisher.languageTranslator.localize('servererror'));
            }, dataType: (Utils.is_msie()) ? "text" : "xml", async: false
        });

        if (serverUrl !== '') {
            this.serverUrl = serverUrl;
            this.proxyUrl = proxyUrl;
            this.secureVin = secureVin;
            this.rootCertsPath = rootCertsPath;
            this.proxyPort = proxyPort;
            if (mentor.publisher.urlParams.uselocalvin === 'true' || vinClient.initialize()) {
                    vinSearchHandlerInstance.loginAndFetchVinOptions(serverUrl, proxyUrl, proxyPort, username,
                            password);
            }
        }

    };
    this.init();

};

function vinServerResponse(vinOptions, username, password, vinNumber)
{
    alertMsg.removeAlertMsg();
    if (vinOptions != null && vinOptions.trim() != "") {
        clearLoginPopup();

        applyVINFilter(vinOptions, false, vinNumber);

    }
}

function vinServerError(errorMessage)
{
    alertMsg.removeAlertMsg();
    vinSearchHandlerInstance.vinServerError(errorMessage);
}

var getLoadingMessage = function (message)
{
    var messageBody = "<div class='messageContainer'>" +
            "<div class='messageLoadingImage'>" +
            "</div>" +
            "<div class='messageContent'>" + message +
            "</div>" +
            "</div>";
    return messageBody;
}

clearLoginPopup = function ()
{
    $("#loginPopover").hide();
    $("#username").val("");
    $("#password").val("");
};

registerLoginAndCancelEvents = function (serverUrl, proxyUrl, proxyPort)
{
    $(".iesdLoginPopupContent div").each(function ()
    {
        $(this).addClass("default_mouse_pointer");
    });
    $(".iesdLoginPopupContent span").each(function ()
    {
        $(this).addClass("default_mouse_pointer");
    });
    $(".iesdLoginPopupFooter div").each(function ()
    {
        $(this).addClass("default_mouse_pointer");
    });
    $("#login_submit").off();

    $("#login_submit").on("click", function (event)
    {
        vinSearchHandlerInstance.checkUserNamePassword(serverUrl, proxyUrl, proxyPort);
    });
    $("#login_cancel").off();
    $("#login_cancel").on("click", clearLoginPopup);
}

var localVInData = null;
