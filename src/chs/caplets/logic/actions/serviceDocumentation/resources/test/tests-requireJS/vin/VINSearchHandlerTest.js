/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("vinSearchHandlerTest", function () {
    var vinNumberToSearch, appletStartedImpl, vinHandler, createAppletContainer, timeoutid;
    createAppletContainer = function () {
        if (!document.getElementById("vinApplet")) {
            $('body').append('<div id="vinApplet">applet</div>');
            //timeoutid = setTimeout(createAppletContainer, 10);
        }
        else {
            //clearTimeout(timeoutid)
        }
    };

    beforeEach(function () {
        createAppletContainer();
        appletStartedImpl = window.appletStarted;
        window.Utils = Utils || {};
        window.Utils.readCookie = function () {
        };
        vinHandler = new ServletVINClient({vinNumber: "testVinNumber"});

        vinHandler.setCredentialsAndVin = function (sUrl, pUrl, sPort, password, username, vinnumber) {
            vinNumberToSearch = vinnumber;
        }
        vinHandler.showLoadingVinMessage = function () {

        };
        vinHandler.uselocalVin = function () {
            return false;
        };

        vinHandler.getMessage = function () {

        };

        vinHandler.showServerFailureMessage = function () {

        };

        window.vinClient = vinHandler;

    });

    it("should be able to create VInSearchHandler", function () {
        expect(window.vinSearchHandlerInstance).toBeDefined();
    });

    it("should initialize ServletVINClient without invoking appletStarted.", function () {
        testNonAppletVINClientInitialization(new window.ServletVINClient());
    });

    function testNonAppletVINClientInitialization(client)
    {
        var invoked = false;
        appletStarted = function (started) {
            invoked = true;
        };

        client.initialize();

        expect(invoked).toBeFalsy();
    }

    it("should be able to fetchVIN", function () {
        var servletVINClient = new window.ServletVINClient();
        spyOn($, 'ajax').andCallThrough();
        servletVINClient.fetchVIN();
        expect($.ajax).toHaveBeenCalled();
    });

    it("should be able to show login popup in VINSearchHandler", function () {
        var VINSearchHandlerInstance = new window.VINSearchHandler({vinNumber:'123', onError: function () {}});
        spyOn(mentor.publisher.eventDispatcher, 'dispatchEvent');
        VINSearchHandlerInstance.showLoginPopup();
        expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
    });

    it("should be able to load user name panel in VINSearchHandler", function () {
        var VINSearchHandlerInstance = new window.VINSearchHandler({vinNumber:'123', onError: function () {}});
        vinSearchHandlerInstance.userNamePanel = {
            title: 'sampleTitle',
            setTitle: function () {},
            addListeners: function () {},
            appendEditable: function () {},
            getPanel: function () {},
            setActive: function () {},
        };
        vinSearchHandlerInstance.popupContent = {
            append: function () {},
        }
        mentor.publisher.PanelListener= function () {};
        spyOn(mentor.publisher, 'PanelListener');
        VINSearchHandlerInstance.loadUserNamePanel();
        expect(mentor.publisher.PanelListener).toHaveBeenCalled();
    });

    it("should be able to check username and password", function () {
        var VINSearchHandlerInstance = new window.VINSearchHandler({vinNumber:'123', onError: function () {}});
        vinSearchHandlerInstance.userNamePanel = {
            title: 'sampleTitle',
            setTitle: function () {},
        };
        var origGetElementByID = document.getElementById;
        document.getElementById = function () {
            return {
                value: 'testValue'
            };
        };

        spyOn(VINSearchHandlerInstance, 'loginAndFetchVinOptions');
        VINSearchHandlerInstance.checkUserNamePassword();
        expect(VINSearchHandlerInstance.loginAndFetchVinOptions).toHaveBeenCalled();

        document.getElementById = origGetElementByID;
    });

    it("should be able to show loading Vin message", function () {
        var VINSearchHandlerInstance = new window.VINSearchHandler({vinNumber:'123', onError: function () {}});
        spyOn(alertMsg, "showMessageWithLoadingImage").andCallThrough();
        VINSearchHandlerInstance.showLoadingVinMessage();
        expect(alertMsg.showMessageWithLoadingImage).toHaveBeenCalled();
    });

    it("should be able to fetch Vehicle Options", function () {
        var VINSearchHandlerInstance = new window.VINSearchHandler({vinNumber:'123', onError: function () {}});
        spyOn(VINSearchHandlerInstance, 'setCredentialsAndVin').andCallThrough();
        VINSearchHandlerInstance.fetchVehicalOptions();
        expect(VINSearchHandlerInstance.setCredentialsAndVin).toHaveBeenCalled();
    });

    it("should be able to use local Vin", function () {
        var VINSearchHandlerInstance = new window.VINSearchHandler({vinNumber:'123', onError: function () {}});
        mentor.publisher.urlParams.uselocalvin = true;
        expect(VINSearchHandlerInstance.uselocalVin()).toBeTruthy();
    });

    it("should be able to set credentials and Vin", function () {
        var VINSearchHandlerInstance = new window.VINSearchHandler({vinNumber:'123', onError: function () {}});

        spyOn(vinClient, 'fetchVIN').andCallThrough();
        VINSearchHandlerInstance.setCredentialsAndVin();
        expect(vinClient.fetchVIN).toHaveBeenCalled();
    });

    it("should be able to login and fetch vin options", function () {
        var VINSearchHandlerInstance = new window.VINSearchHandler({vinNumber:'123', onError: function () {}});
        spyOn(VINSearchHandlerInstance, 'fetchVehicalOptions');
        VINSearchHandlerInstance.loginAndFetchVinOptions();
        expect(VINSearchHandlerInstance.fetchVehicalOptions).toHaveBeenCalled();
    });

    it("should be able to create VInSearchHandler", function () {
        expect(window.vinSearchHandlerInstance).toBeDefined();
    });

    afterEach(function () {
        window.appletStarted = appletStartedImpl;
        $("#vinApplet").remove();
        //vinHandler = '';
    });

});