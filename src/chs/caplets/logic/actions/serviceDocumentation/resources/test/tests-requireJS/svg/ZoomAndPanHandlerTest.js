/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

//todo runs in isolation but fails with other tests
require(["ZoomAndPanModule", "SVGTransformModel"],
        function (zoomAndPanModule, SVGTransformModel)
        {
            describe("ZoomAndPanHandlerTest", function ()
            {
                "use strict"
                var svgTransformModel;

                beforeEach(function ()
                {
                    var root, viewport, viewBoxHeight = 0, viewBoxWidth = 0, clientWidth, clientHeight;
                    $('body').html('');
                    $('body').append('<svg id="svgZoomTest" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" xml:space="preserve" height="100%" width="100%" data-translation="marker-based" style="transform: translate3d(0px, 0px, 0px); cursor: default;" data-containerId="systemSVGLoadArea" onmousedown="handleMouseDown(evt)" onmousemove="handleMouseMove(evt)" onmouseup="handleMouseUp(evt)" onclick="handleMouseClick(evt)"> <defs> <style type="text/css"> <![CDATA[ .A,.L,.N,.Q,.M,.K{stroke-width:36px;} .A1,.B1,.C1,.O,.M,.H,.J,.I,.K{stroke-opacity:0.0;} .A1,.X,.Z,.S,.T,.R{font-family:Lucida Sans;} .A1,.X,.Z,.S,.T,.R,.B1,.C1,.V,.U,.W,.Y,.O,.M,.H,.J,.I,.K,.D1{fill-opacity:1.0;} .A1,.Z{font-size:336px;} .B,.A,.A1,.C1,.J{fill:#FFF;} .B,.A,.C{stroke-linecap:butt;fill-opacity:0.01;stroke-opacity:0.01;} .B,.A,.C,.P,.L,.N,.Q,.D,.E,.F,.G,.A1,.X,.Z,.S,.T,.R,.B1,.C1,.V,.U,.W,.Y,.O,.M,.H,.J,.I,.K,.D1{fill-rule:evenodd;font-style:normal;font-weight:normal;stroke-dasharray:none;stroke-linejoin:miter;stroke-miterlimit:10;} .B,.A,.C,.P,.L,.N,.Q,.D,.E,.F,.G,.O,.M,.H,.J,.I,.K,.D1{font-family:sans-serif;} .B,.A,.P,.L,.N,.X,.Z,.B1,.V,.U,.W,.Y,.O,.M{stroke:#000;} .B,.P,.D,.E,.A1,.C1,.O,.H,.J,.I{stroke-width:16px;} .B1,.C1,.U{font-size:192px;} .B1,.C1,.V,.U,.W,.Y{font-family:Lucida Sans Unicode;} .C{stroke:#FFF;stroke-width:274px;} .C,.P,.L,.N,.Q,.D,.E,.F,.G{fill:none;} .D,.E,.F,.G,.S,.T,.R,.H,.J,.I,.K,.D1{stroke:#808080;} .D1{stroke-width:16.0px;} .F,.G{stroke-width:63px;} .H,.D1{fill:#900;} .I{fill:url(#p1);} .K{fill:#FBE4E4;} .M{fill:#CCF;} .O{fill:#8EB0F4;} .P,.L,.N,.Q,.D,.E,.F,.G{fill-opacity:0.0;} .P,.L,.N,.Q,.D,.E,.F,.G,.A1,.X,.Z,.S,.T,.R,.B1,.C1,.V,.U,.W,.Y,.O,.M,.H,.J,.I,.K,.D1{stroke-linecap:square;} .P,.L,.N,.Q,.D,.E,.F,.G,.X,.Z,.S,.T,.R,.V,.U,.W,.Y,.D1{stroke-opacity:1.0;} .Q,.A1,.C1{stroke:#69F;} .R{font-size:538px;} .S{font-size:215px;} .S,.T,.R{fill:#808080;} .T{font-size:358px;} .V{font-size:153px;} .W{font-size:231px;} .X{font-size:322px;} .X,.Z,.B1,.V,.U,.W,.Y{fill:#000;} .X,.Z,.S,.T,.R,.B1,.V,.U,.W,.Y{stroke-width:0.5px;} .Y{font-size:276px;} .a_{stroke:red;} .b_{fill:red;} .c_{stroke:#FA0;} .d_{fill:#FA0;} ]]> </style> <pattern id="p1" patternUnits="userSpaceOnUse" x="0" y="0" width="308" height="308"> <g style="fill:#900;stroke:#900;stroke-width:42"> <rect x="0" y="0" height="308" width="308" style="stroke:#000;fill:#000"/> <path d="M19 0V311M57 0V311M96 0V311M134 0V311M173 0V311M211 0V311M250 0V311M288 0V311"/> </g> </pattern> </defs> <g id="viewport" transform="matrix(0.0596303753554821,0,0,0.0596303753554821,-669.6867997398876,-375.9843444824219)"> <g id="connJ1"><desc>chs.cof.logical.schem.CAFPinList UIDba4a80-14bb746b601-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b602-85ae53377f7ca99fd13e2e740f24ed3c</desc> <path class="A" d="M24020,13731v242h1456v-3883h-1456 z"/> <path class="K" d="M24263,13974h-242v-242v-3398v-242h242h970h242v242v3398v242h-242 z" style="cursor: pointer;"/> <path class="L" d="M24020,13731v242h1456v-3883h-1456 z" style="cursor: pointer;"/> <text class="U" x="24681" y="14185">J1</text> </g> <g><desc>chs.cof.logical.schem.CAFPinList UIDba4a80-14bb746b603-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b604-85ae53377f7ca99fd13e2e740f24ed3c</desc> <path class="A" d="M26933,10333L26933,13731C26933,13865 26824,13974 26690,13974L25476,13974 25476,10091L26690,10091C26824,10091 26933,10199 26933,10333"/> <path class="M" d="M25719,13974h-242v-3883L26690,10091 C26824,10091 26933,10199 26933,10333 L26933,13731 C26933,13865 26824,13974 26690,13974 L25719,13974" style="cursor: pointer;"/> <path class="N" d="M26690,13974h-1213v-3883h1213m242,3640v-3398M26690,13974 C26824,13974 26933,13865 26933,13731 M26933,10333 C26933,10199 26824,10091 26690,10091 " style="cursor: pointer;"/> <text class="U" x="26113" y="14185">P1</text> </g> <g id="dev1"><desc>chs.cof.logical.schem.CAFPinList UIDba4a80-14bb746b5fd-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b5fe-85ae53377f7ca99fd13e2e740f24ed3c</desc> <path class="B" d="M13826,13731v242h3883v-3398h-3883 z"/> <path class="O" d="M14069,13974h-242v-242v-2912v-242h242h3398h242v242v2912v242h-242 z" style="cursor: pointer;"/> <path class="P" d="M13826,13731v242h3883v-3398h-3883 z" style="cursor: pointer;"/> <text class="W" x="15483" y="14334">DEV1</text> <text class="X" x="13934" y="13457">This is Device 1</text> </g> <g id="gnd1"><desc>chs.cof.logical.schem.CAFPinList UIDba4a80-14bb746b5ff-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b600-85ae53377f7ca99fd13e2e740f24ed3c</desc> <rect class="A" x="31156" y="16401" width="1116" height="728"/> <path class="N" d="M31787,16401v436m-485,0h970m-145,291l145,-291m-1116,291l145,-291m339,291l145,-291"/> <text class="Z" x="31308" y="17516">GND1</text> </g> <g><desc>chs.cof.logical.schem.CAFConductor UIDba4a80-14bb746b5f5-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b5f6-85ae53377f7ca99fd13e2e740f24ed3c</desc> <path class="C" d="M17805,12032h6120" style="cursor: pointer;"/> <path class="Q" d="M17710,12032h6310" style="cursor: pointer;"/> <rect class="A1" x="20558" y="11862" width="613" height="242" style="cursor: pointer;"/> <text class="B1" x="20589" y="12103" style="cursor: pointer;">WIRE1</text> </g> <g id="wire2"><desc>chs.cof.logical.schem.CAFConductor UIDba4a80-14bb746b5f9-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b5fa-85ae53377f7ca99fd13e2e740f24ed3c</desc> <path class="C" d="M27028,12032h4664m95,95v4178" style="cursor: pointer;"/> <path class="Q" d="M26933,12032h4854v4368" style="cursor: pointer;"/> <rect class="C1" x="29053" y="11862" width="613" height="242" style="cursor: pointer;"/> <text class="B1" x="29084" y="12103">WIRE2</text> </g> <g><desc>chs.cof.logical.schem.CAFPin UIDba4a80-14bb746b605-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b606-85ae53377f7ca99fd13e2e740f24ed3c</desc> <text class="V" x="24076" y="12098">PIN1</text> </g> <g><desc>chs.cof.logical.schem.CAFPin UIDba4a80-14bb746b609-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b60a-85ae53377f7ca99fd13e2e740f24ed3c</desc> <text class="V" x="26559" y="12098">PIN1</text> </g> <g><desc>chs.cof.logical.schem.CAFPin UIDba4a80-14bb746b60b-85ae53377f7ca99fd13e2e740f24ed3c UIDba4a80-14bb746b60c-85ae53377f7ca99fd13e2e740f24ed3c</desc> <text class="Y" x="16988" y="12156">PIN1</text> </g> </g> </svg>');
                    //zoomAndPanModule = new SVGEventHandler();
                    root = document.getElementById("svgZoomTest");
                    clientHeight = root.clientHeight || $(root).innerHeight();
                    clientWidth = root.clientWidth || $(root).innerWidth();
                    if (root && root.viewBox) {
                        viewport = $('#viewport', root)[0];
                        if (root.viewBox.baseVal) {
                            viewBoxHeight = root.viewBox.baseVal.height;
                            viewBoxWidth = root.viewBox.baseVal.width;
                        }
                    }
                    svgTransformModel = new SVGTransformModel({
                        svgContainerId: 'svgZoomTest',
                        root: root,
                        viewport: viewport,
                        viewBoxWidth: viewBoxWidth,
                        viewBoxHeight: viewBoxHeight,
                        clientWidth: clientWidth,
                        clientHeight: clientHeight
                    });
                });

                function testTransform(expectedZoom, zoomEpsilon, expectedXTranslation, xTranslationEpsilon,
                        expectedYTranslation, yTranslationEpsilon, expectedZoomFactor, zoomFactorEpsilon, log)
                {
                    var viewPort = document.getElementById('viewport'), actualTransform = viewPort.getCTM(), zoomx, zoomy, tx, ty;
                    zoomx = Math.sqrt(actualTransform.a * actualTransform.a + actualTransform.c * actualTransform.c);
                    zoomy = Math.sqrt(actualTransform.b * actualTransform.b + actualTransform.d * actualTransform.d);
                    tx = actualTransform.e;
                    ty = actualTransform.f;
                    if (log) {
                        console.log('zoom=' + zoomx);
                        console.log('tx=' + tx);
                        console.log('ty=' + ty);
                        console.log('zoomfactor=' + zoomfactor);
                    }
                    //testing zoom values
                    expect(zoomx).toBeGreaterThan(expectedZoom - zoomEpsilon);
                    expect(zoomx).toBeLessThan(expectedZoom + zoomEpsilon);
                    expect(zoomy).toBeGreaterThan(expectedZoom - zoomEpsilon);
                    expect(zoomy).toBeLessThan(expectedZoom + zoomEpsilon);
                    //testing translation values
                    expect(tx).toBeGreaterThan(expectedXTranslation - xTranslationEpsilon);
                    expect(tx).toBeLessThan(expectedXTranslation + xTranslationEpsilon);
                    expect(ty).toBeGreaterThan(expectedYTranslation - yTranslationEpsilon);
                    expect(ty).toBeLessThan(expectedYTranslation + yTranslationEpsilon);
                    //testing zoomfactor
                    //expect(zoomfactor).toBeGreaterThan(expectedZoomFactor - zoomFactorEpsilon);
                    //expect(zoomfactor).toBeLessThan(expectedZoomFactor + zoomFactorEpsilon);

                }

                it("zoom and pan handler should fit the viewport element in the center'", function ()
                {
                    var viewPort = document.getElementById('viewport');
                    var element1 = document.getElementById('dev1');
                    var heightOrg = element1.getBoundingClientRect().height;
                    var widthOrg = element1.getBoundingClientRect().width;
                    zoomAndPanModule.bringToFront(svgTransformModel, [viewPort]);

                    var heightAfterTransformation = element1.getBoundingClientRect().height;
                    var widthAfterTransformation = element1.getBoundingClientRect().width;
                    expect(widthOrg).toBeGreaterThan(widthAfterTransformation);
                    expect(heightOrg).toBeGreaterThan(heightAfterTransformation);
                    //testTransform(.0175, .0010, 658.20, 1, -0.38, 1, .30, .05);
                });

                function fitSVGAndTestIfAnElementSizeHasReduced(element)
                {
                    var viewPort = document.getElementById('viewport'), actualTransform, zoomx, zoomy;
                    var element1 = document.getElementById(element);
                    var heightOrg = element1.getBoundingClientRect().height;
                    var widthOrg = element1.getBoundingClientRect().width;
                    zoomAndPanModule.fit(svgTransformModel);
                    var heightAfterTransformation = element1.getBoundingClientRect().height;
                    var widthAfterTransformation = element1.getBoundingClientRect().width;
                    expect(widthOrg).toBeGreaterThan(widthAfterTransformation);
                    expect(heightOrg).toBeGreaterThan(heightAfterTransformation);
                }

                it("zoom and pan handler should fit the bounding box of entire svg'", function ()
                {
                    fitSVGAndTestIfAnElementSizeHasReduced('dev1');
                });

                it("zoom and pan handler should fit the bounding box of J1'", function ()
                {
                    fitSVGAndTestIfAnElementSizeHasReduced('connJ1');
                });

                it("zoom and pan handler should fit the bounding box of gnd1'", function ()
                {
                    fitSVGAndTestIfAnElementSizeHasReduced('gnd1');
                });

                it("zoom and pan handler should fit the bounding box of wire2'", function ()
                {
                    fitSVGAndTestIfAnElementSizeHasReduced('wire2');
                });

                it("zoom and pan handler should fit the bounding box of gnd1 + wire2'", function ()
                {
                    var viewPort = document.getElementById('viewport'), element1 = document.getElementById('wire2'), element2 = document.getElementById('gnd1'), actualTransform, zoomx, zoomy;
                    zoomAndPanModule.bringToFront(svgTransformModel, [element1, element2]);
                    testTransform(.0235, .001, 61, 5, -271.43, 5, .39, .05);
                });

                it("zoom and pan handler should fit the bounding box of gnd1 + dev1'", function ()
                {
                    var viewPort = document.getElementById('viewport'), element1 = document.getElementById('dev1'), element2 = document.getElementById('gnd1'), actualTransform, zoomx, zoomy;
                    zoomAndPanModule.bringToFront(svgTransformModel, [element1, element2]);
                    testTransform(.01925, .001, 315, 5, -195.60, 5, .32, .05);
                });

                it("zoom and pan handler should fit the bounding box of gnd1 + dev1 + wire2'", function ()
                {
                    var viewPort = document.getElementById('viewport'), element1 = document.getElementById('dev1'), element2 = document.getElementById('gnd1'), element3 = document.getElementById('wire2'), actualTransform, zoomx, zoomy;
                    zoomAndPanModule.bringToFront(svgTransformModel, [element1, element2, element3]);
                    testTransform(.0192, .0010, 315.05, 5, -195.60, 5, .32, .05);
                });

                afterEach(function ()
                {
                    $("#svgZoomTest").remove();
                });

            });
        });