/**
 * Created by kayyagar on 08-01-2016.
 */
TestCase("zoomWhenMouseScrollHandlerTest", {
    setUp: function ()
    {
        "use strict";

        var that = this,
                //svg = $('<svg  height="100%" width="100%" style="cursor: default;"><g id="viewport"></g></svg>')[0],
                svg = svgForZoomTest,
                seOnSVG = document.createElementNS('http://www.w3.org/2000/svg', 'script');
        //seOnSVG.setAttributeNS(null, 'type', 'text/javascript');
        //seOnSVG.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', 's/SVGPan.js');
        //document.body.appendChild(seOnSVG);
        this.mockEvent = {};
        this.mockEvent.clientX = 10;
        this.mockEvent.clientY = 10;
        this.mockEvent.preventDefault = false;
        //this.mockEvent.target = {};
        //this.mockEvent.target.ownerDocument = svg;
        this.mockEvent.target = svg;
        this.mockEvent.currentTarget = {};
        $(this.mockEvent.currentTarget).attr('data-containerId', 'testContainer');
        this.count = 0;
        root = svg;
        _.debounce = function (func, wait, immediate)
        {
            that.count++;
        };
    },

    tearDown: function ()
    {
        "use strict";
    },
    //"test SVG Event Handler should load correctly": function ()
    //{
    //    assertTrue("SVG SVG Event Handler should load correctly ", this.svgEventHandler !== undefined);
    //},
    //"test SVG event handler should get initialized properly": function ()
    //{
    //    "use strict";
    //
    //    this.svgEventHandler.init($("svg").first()[0])
    //    assertTrue("SVG event handler should get initialized properly ", window.crossHighlightHandler.isCalled);
    //},

    "test click on SVG should close popover": function ()
    {
        "use strict";
        var that = this;
        //$('svg>g').click(function (event)
        //{
        //    that.svgEventHandler.mouseClickHandler(event);
        //});
        mouseWheelHandler.handleMouseWheel(this.mockEvent);
        assertEquals("SVG event handler should get initialized properly ", this.count, 1);
    }

});

