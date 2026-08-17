/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

jQuery.extend({
    Variables : function () {

        this.systemMsg = 'system';
        this.introduction_pageMsg = 'introduction-page';
        this.reportMsg = 'report';
        this.harnessMsg = 'harness';
        this.faultcodeMsg = 'faultcode';
        this.locationViewsMsg = 'locationViews';

        this.systemLevelSVG = 'SYSTEMSVG';
        this.systemReports = 'REPORTS';
        this.faceViews = 'FACEVIEWS';
        this.locationViews = 'LOCATIONVIEWS';
        this.system3DSVG = 'THREEDSVG';
        this.faultCodes = 'SYSTEMFAULTCODES';
        this.systemLevelReports = 'SYSTEMLEVELREPORTS';

        //This is for custom data, can be SVGs,HTML , PDF or any other type
        //Will be shown in the Location Views Panel.
        this.customData = 'CUSTOMDATA';
        this.ShowFullSignal = 'SHOWFULLSIGNAL';
        this.ShowFullInstance = 'SHOWFULLINSTANCE';
        this.ShowFullSignalOrInstance = "SHOWFULLSIGNALorINSTANCE";
        this.ShowFullSignalOrInstanceAfterLoad = "SHOWFULLSIGNALorINSTANCEAfterLoad";

        //this.systemSVGContentType = "SYSTEMSVG";
        //this.twoDThreeDContentType = "LOCATIONVIEWS";
        //this.reportFaceViewContentType = "REPORTS";

        this.HotSpotElementTagName = "hotspot";
        this.TextElementTagName = "text";
        this.CommentElementNode = "comment";
        this.GElementTagName = "g";

        //this.systemSVG = "SYSTEMSVG";

        this.redColorMsg = 'highlight-onclick-color';
        this.orangeColorMsg = 'highlight-onhover-color';
        this.strokeWidth = 'highlight-stroke-width';

        this.VINText = "VINTEXT";
        this.InteractiveBuilder = "INTERACTIVEBUILDER";

        /**
         *  These Constants will be used for the arranging the popup.
         */

        this.xrefPanel = "Links";
        this.attributesPanel = "Attributes";
        this.faceViewsPanel = "FaceViews";
        this.locationviewPanel = "LocationView";
        this.customObjectPanel = "CustomData"
        this.twodlocationviewpanel = "TwodViews";
        this.signalTracePanel = "ShowFullSignal";
        this.fullInstancePanel = "ShowFullInstance";
        this.groundAndPowerSignal = "ShowGroundAndPowerSignal";

        this.splitterOne = "splitter_one";
        this.splitterTwo = "splitter_two";
        this.splitterThree = "splitter_three";

        this.customToolTipArrayLength = 'customtooltiparraylength';
        this.customToolTip = 'customtooltip';
        this.colonSeparator = ' : ';
        this.MaxZoomPercentage = 1000;
        this.MinZoomPercentage = 10;
        this.PositiveSliderStep = 10;
        this.NegetiveSliderStep = -10;
        this.PositiveZoomPerUnit = 1.164993050750713;
        this.NegetiveZoomPerUnit = 0.858374218932557;
        this.ScaleDownFactorForZoomAll = .9;
        this.ScaleDownFactorForZoomObject = .7;

        this.TypeConfigurationFilter = 'ConfigurationFilter';
        this.TypeVINFilter = 'VINFilter';
        this.TypeNOFilter = '';

        this.TypeConfigurtaion = 'configuration';
        this.TypeOption = 'option';

        this.LoadingProject = 'loadingProject';

        this.InteractiveButtonFromVIN = 'InteractiveButtonFromVIN';
        this.InteractiveButtonFromXref = 'InteractiveButtonFromXref';

        /*
         This is the name of the tag that is used to mention the connectivityXML file to be used on clicking for rendering signal.
         */
        this.signalTraceObjectUIDTag = 'connectivityxml';
        /*
         This is the name of the tag that is used to mention the connectivityXML file to be used on clicking for fullInstance.
         */
        this.fullInstanceObjectUIDTag = 'fullinstancexml';

        /*
         This is the name of the tag that is used to mention the connectivityXML file to be used on clicking for groundAndPowerSignal.
         */
        this.groundAndPowerSignalObjectUIDTag = 'groundAndPowerSignal';

        this.FullInstanceTitle = "Full Instance";
        this.SignalPathTitle = "Signal Path";
        this.GroundAndPowerSignalTitle = "Ground and Power Path";
        this.maxCharsLengthToShowToolTip = 25;
        this.popOverHeightWithoutFilter = 204;
        this.popOverHeightWithFilter = 234;
        this.print = 'Print';
        this.printSelection = 'Print Selection';
        this.clientTypeToNameMap = {
            "CapitalServiceExplorer": "Capital Service Explorer",
            "CapitalChangeExplorer": "Capital Change Explorer",
            "CapitalDesignExplorer": "Capital Design Explorer",
            "CapitalSmartFlows": "Capital Smart Flows"
        }

    }
});

mentor.publisher.constants = new $.Variables();
