/**
 *  SVGPan library 1.2
 * ====================
 *
 * Given an unique existing element with id "viewport", including the
 * the library into any SVG adds the following capabilities:
 *
 *  - Mouse panning
 *  - Mouse zooming (using the wheel)
 *  - Object dargging
 *
 * Known issues:
 *
 *  - Zooming (while panning) on Safari has still some issues
 *
 * Releases:
 *
 * 1.2, Sat Mar 20 08:42:50 GMT 2010, Zeng Xiaohui
 *    Fixed a bug with browser mouse handler interaction
 *
 * 1.1, Wed Feb  3 17:39:33 GMT 2010, Zeng Xiaohui
 *    Updated the zoom code to support the mouse wheel on Safari/Chrome
 *
 * 1.0, Andrea Leofreddi
 *    First release
 *
 * This code is licensed under the following BSD license:
 *
 * Copyright 2009-2010 Andrea Leofreddi <a.leofreddi@itcharm.com>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Andrea Leofreddi ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Andrea Leofreddi OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Andrea Leofreddi.
 */

(function(window) {
    var IE_10		= !!window.navigator.msPointerEnabled,
            // Check below can mark as IE11+ also other browsers which implements pointer events in future
            // that is not issue, because touch capability is tested in IF statement bellow.
            // Note since Edge 16/Windows 10 1709 the property 'window.navigator.pointerEnabled' is undefined.
            IE_11_PLUS	= !!window.navigator.pointerEnabled || !!window.PointerEvent;

    // Only pointer enabled browsers without touch capability.
    if (IE_10 || (IE_11_PLUS && !('ontouchstart' in window))) {
        var document = window.document,
                POINTER_DOWN		= IE_11_PLUS ? "pointerdown"	: "MSPointerDown",
                POINTER_UP 			= IE_11_PLUS ? "pointerup"		: "MSPointerUp",
                POINTER_MOVE		= IE_11_PLUS ? "pointermove"	: "MSPointerMove",
                POINTER_CANCEL		= IE_11_PLUS ? "pointercancel"	: "MSPointerCancel",
                POINTER_TYPE_TOUCH 	= IE_11_PLUS ? "touch"	: MSPointerEvent.MSPOINTER_TYPE_TOUCH,
                POINTER_TYPE_MOUSE 	= IE_11_PLUS ? "mouse"	: MSPointerEvent.MSPOINTER_TYPE_MOUSE,
                POINTER_TYPE_PEN 	= IE_11_PLUS ? "pen"	: MSPointerEvent.MSPOINTER_TYPE_PEN, //IE11+ has also unknown type which Touchr doesn't support
                GESTURE_START		= "MSGestureStart",
                GESTURE_CHANGE		= "MSGestureChange",
                GESTURE_END			= "MSGestureEnd",
                TOUCH_ACTION		= IE_11_PLUS ? "touchAction" : "msTouchAction",
                _180_OVER_PI		= 180/Math.PI,
                // Which pointer types will be used for generating touch events: 1 - touch, 2 - mouse, 4 - pen or their combination
                ALLOWED_POINTER_TYPE = window.Touchr_ALLOWED_POINTER_TYPE || 1,
                createEvent = function (eventName, target, params) {
                    var k,
                            event = document.createEvent("Event");

                    event.initEvent(eventName, true, true);
                    for (k in params) {
                        event[k] = params[k];
                    }
                    target.dispatchEvent(event);
                },
                /**
                 * ECMAScript 5 accessors to the rescue
                 * @see http://perfectionkills.com/how-ecmascript-5-still-does-not-allow-to-subclass-an-array/
                 */
                makeSubArray = (function() {
                    var MAX_SIGNED_INT_VALUE = Math.pow(2, 32) - 1,
                            hasOwnProperty = Object.prototype.hasOwnProperty;

                    function ToUint32(value) {
                        return value >>> 0;
                    }

                    function getMaxIndexProperty(object) {
                        var maxIndex = -1,
                                isValidProperty,
                                prop;

                        for (prop in object) {

                            isValidProperty = (
                                    String(ToUint32(prop)) === prop &&
                                    ToUint32(prop) !== MAX_SIGNED_INT_VALUE &&
                                    hasOwnProperty.call(object, prop));

                            if (isValidProperty && prop > maxIndex) {
                                maxIndex = prop;
                            }
                        }
                        return maxIndex;
                    }

                    return function(methods) {
                        var length = 0;
                        methods = methods || { };

                        methods.length = {
                            get: function() {
                                var maxIndexProperty = +getMaxIndexProperty(this);
                                return Math.max(length, maxIndexProperty + 1);
                            },
                            set: function(value) {
                                var constrainedValue = ToUint32(value);
                                if (constrainedValue !== +value) {
                                    throw new RangeError();
                                }
                                for (var i = constrainedValue, len = this.length; i < len; i++) {
                                    delete this[i];
                                }
                                length = constrainedValue;
                            }
                        };
                        methods.toString = {
                            value: Array.prototype.join
                        };
                        return Object.create(Array.prototype, methods);
                    };
                })(),
                // methods passed to TouchList closure method to extend Array
                touchListMethods = {
                    /**
                     * Returns touch by id. This method fulfill the TouchList interface.
                     * @param {Number} id
                     * @returns {Touch}
                     */
                    identifiedTouch: {
                        value: function (id) {
                            var length = this.length;
                            while (length--) {
                                if (this[length].identifier === id) return this[length];
                            }
                            return undefined;
                        }
                    },
                    /**
                     * Returns touch by index. This method fulfill the TouchList interface.
                     * @param {Number} index
                     * @returns {Touch}
                     */
                    item: {
                        value: function (index) {
                            return this[index];
                        }
                    },
                    /**
                     * Returns touch index
                     * @param {Touch} touch
                     * @returns {Number}
                     */
                    _touchIndex: {
                        value: function (touch) {
                            var length = this.length;
                            while (length--) {
                                if (this[length].pointerId == touch.pointerId) return length;
                            }
                            return -1;
                        }
                    },

                    /**
                     * Add all events and convert them to touches
                     * @param {Event[]} events
                     */
                    _addAll: {
                        value: function(events) {
                            var i = 0,
                                    length = events.length;

                            for (; i < length; i++) {
                                this._add(events[i]);
                            }
                        }
                    },

                    /**
                     * Add and MSPointer event and convert it to Touch like object
                     * @param {Event} event
                     */
                    _add: {
                        value: function(event) {
                            var index = this._touchIndex(event);

                            index = index < 0 ? this.length : index;

                            //normalizing Pointer to Touch
                            event.type = POINTER_MOVE;
                            event.identifier = event.pointerId;
                            //in DOC is mentioned that it is 0..255 but actually it returns 0..1 value
                            //returns 0.5 for mouse down buttons in IE11, should it be issue?
                            event.force = event.pressure;
                            //default values for Touch which we cannot obtain from Pointer
                            event.radiusX = event.radiusY = 1;
                            event.rotationAngle = 0;

                            this[index] = event;
                        }
                    },

                    /**
                     * Removes an event from this touch list.
                     * @param {Event} event
                     */
                    _remove: {
                        value: function(event) {
                            var index = this._touchIndex(event);

                            if (index >= 0) {
                                this.splice(index,1);
                            }
                        }
                    }
                },

                /**
                 * This class store touches in an list which can be also accessible as array which is
                 * little bit bad because TouchList have to extend Array. Because we are aiming on
                 * IE10+ we can use ECMAScript5 solution.
                 * @extends Array
                 * @see http://www.w3.org/TR/2011/WD-touch-events-20110913/#touchlist-interface
                 * @see https://developer.mozilla.org/en-US/docs/DOM/TouchList
                 */
                TouchList = (function(methods) {
                    return function() {
                        var arr = makeSubArray(methods);
                        if (arguments.length === 1) {
                            arr.length = arguments[0];
                        }
                        else {
                            arr.push.apply(arr, arguments);
                        }
                        return arr;
                    };
                })(touchListMethods),

                /**
                 * list of all touches running during life cycle
                 * @type TouchList
                 */
                generalTouchesHolder,

                /**
                 * Storage of link between pointer {id} and original target
                 * @type Object
                 */
                pointerToTarget = {},

                /**
                 * General gesture object which fires MSGesture events whenever any associated MSPointer event changed.
                 */
                gesture = window.MSGesture ? new MSGesture() : null,

                gestureScale = 1,
                gestureRotation = 0,

                /**
                 * Storage of targets and anonymous MSPointerStart handlers for later
                 * unregistering
                 * @type Array
                 */
                attachedPointerStartMethods = [],

                /**
                 * Checks if node is some of parent children or sub-children
                 * @param {HTMLElement|Document} parent
                 * @param {HTMLElement} node
                 * @returns {Boolean}
                 */
                checkSameTarget = function (parent, node) {
                    if (node) {
                        if (parent === node) {
                            return true;
                        } else {
                            return checkSameTarget(parent, node.parentNode);
                        }
                    } else {
                        return false;
                    }
                },

                /**
                 * Returns bitmask type of pointer to compare with allowed pointer types
                 * @param {Number|String} pointerType
                 * @returns {Number}
                 */
                pointerTypeToBitmask = function (pointerType) {
                    if (pointerType == POINTER_TYPE_TOUCH) {
                        return 1;
                    } else if (pointerType == POINTER_TYPE_MOUSE) {
                        return 2;
                    } else {
                        return 4;
                    }
                },

                /**
                 * Main function which is rewriting the MSPointer event to touch event
                 * and preparing all the necessary lists of touches.
                 * @param {Event} evt
                 */
                pointerListener = function (evt) {
                    var type,
                            i,
                            target = evt.target,
                            originalTarget,
                            changedTouches,
                            targetTouches;

                    // Skip pointers which are not allowed by users:
                    if (!(pointerTypeToBitmask(evt.pointerType) & ALLOWED_POINTER_TYPE)) {
                        return;
                    }

                    if (evt.type === POINTER_DOWN) {
                        generalTouchesHolder._add(evt);
                        pointerToTarget[evt.pointerId] = evt.target;

                        type = "touchstart";

                        // Fires MSGesture event when we have at least two pointers in our holder
                        // (adding pointers to gesture object immediately fires Gesture event)
                        if (generalTouchesHolder.length > 1) {
                            gesture.target = evt.target;
                            for (i = 0; i < generalTouchesHolder.length; i++) {
                                // Adds to gesture only touches
                                // It is not necessary to create separate gesture for mouse or pen pointers
                                // because they cannot be present more than by 1 pointer.
                                if (generalTouchesHolder[i].pointerType === POINTER_TYPE_TOUCH) {
                                    gesture.addPointer(generalTouchesHolder[i].pointerId);
                                }
                            }
                        }
                    }

                    if (evt.type === POINTER_MOVE && generalTouchesHolder.identifiedTouch(evt.pointerId)) {
                        generalTouchesHolder._add(evt);

                        type = "touchmove";
                    }

                    //Preparation of touch lists have to be done before pointerup/MSPointerUp where we delete some information

                    //Which touch fired this event, because we know that MSPointer event is fired for every
                    //changed pointer than we create a list only with actual pointer
                    changedTouches = document.createTouchList(evt);
                    //Target touches is list of touches which started on (touchstart) on target element, they
                    //are in this array even if these touches have coordinates outside target elements
                    targetTouches = document.createTouchList();
                    for (i = 0; i < generalTouchesHolder.length; i++) {
                        //targetTouches._add(generalTouchesHolder[i]);
                        //check if the pointerTarget is in the target
                        if (checkSameTarget(target, pointerToTarget[generalTouchesHolder[i].identifier])) {
                            targetTouches._add(generalTouchesHolder[i]);
                        }
                    }
                    originalTarget = pointerToTarget[evt.pointerId];

                    if (evt.type === POINTER_UP || evt.type === POINTER_CANCEL) {
                        generalTouchesHolder._remove(evt);
                        pointerToTarget[evt.pointerId] = null;

                        delete pointerToTarget[evt.pointerId];
                        type = "touchend";

                        // Fires MSGestureEnd event when there is only one ore zero touches:
                        if (generalTouchesHolder.length <= 1) {
                            gesture.stop();
                        }
                    }

                    //console.log("+", evt.type, evt.pointerType, generalTouchesHolder.length, evt.target.nodeName+"#"+evt.target.id);
                    if (type && originalTarget) {
                        createEvent(type, originalTarget, {touches: generalTouchesHolder, changedTouches: changedTouches, targetTouches: targetTouches});
                    }
                },

                /**
                 * Main function which is rewriting the MSGesture event to gesture event.
                 * @param {Event} evt
                 */
                gestureListener = function (evt) {
                    //TODO: check first, other than IE (FF?), browser which implements pointer events how to make gestures from pointers. Maybe it would be mix of pointer/gesture events.
                    var type, scale, rotation;
                    if (evt.type === GESTURE_START) {type = "gesturestart"}
                    else if (evt.type === GESTURE_CHANGE) {type = "gesturechange"}
                    else if (evt.type === GESTURE_END) {type = "gestureend"}

                    // -------- SCALE ---------
                    //MSGesture:
                    //Scale values represent the difference in scale from the last MSGestureEvent that was fired.
                    //Apple:
                    //The distance between two fingers since the start of an event, as a multiplier of the initial distance. The initial value is 1.0.

                    // ------- ROTATION -------
                    //MSGesture:
                    //Clockwise rotation of the cursor around its own major axis expressed as a value in radians from the last MSGestureEvent of the interaction.
                    //Apple:
                    //The delta rotation since the start of an event, in degrees, where clockwise is positive and counter-clockwise is negative. The initial value is 0.0
                    if (evt.type === GESTURE_START) {
                        scale = gestureScale = 1;
                        rotation = gestureRotation = 0;
                    } else {
                        scale = gestureScale = gestureScale + (evt.scale - 1); //* evt.scale;
                        rotation = gestureRotation = gestureRotation + evt.rotation * _180_OVER_PI;
                    }

                    createEvent(type, evt.target, {scale: scale, rotation: rotation, screenX: evt.screenX, screenY: evt.screenY});
                },

                /**
                 * This method augments event listener methods on given class to call
                 * our own method which attach/detach the MSPointer events handlers
                 * when user tries to attach touch events.
                 * @param {Function} elementClass Element class like HTMLElement or Document
                 */
                augmentEventListener = function(elementClass) {
                    var customAddEventListener = attachTouchEvents,
                            customRemoveEventListener = removeTouchEvents,
                            oldAddEventListener = elementClass.prototype.addEventListener,
                            oldRemoveEventListener = elementClass.prototype.removeEventListener;

                    elementClass.prototype.addEventListener = function(type, listener, useCapture) {
                        //"this" is HTML element
                        if ((type.indexOf("gesture") === 0 || type.indexOf("touch") === 0)) {
                            customAddEventListener.call(this, type, listener, useCapture);
                        }
                        oldAddEventListener.call(this, type, listener, useCapture);
                    };

                    elementClass.prototype.removeEventListener = function(type, listener, useCapture) {
                        if ((type.indexOf("gesture") === 0 || type.indexOf("touch") === 0)) {
                            customRemoveEventListener.call(this, type, listener, useCapture);
                        }
                        oldRemoveEventListener.call(this, type, listener, useCapture);
                    };
                },
                /**
                 * This method attach event handler for MSPointer / MSGesture events when user
                 * tries to attach touch / gesture events.
                 * @param {String} type
                 * @param {Function} listener
                 * @param {Boolean} useCapture
                 */
                attachTouchEvents = function (type, listener, useCapture) {
                    //element owner document or document itself
                    var doc = this.nodeType == 9 ?  this : this.ownerDocument;

                    // Because we are listening only on document, it is not necessary to
                    // attach events on one document more times
                    if (attachedPointerStartMethods.indexOf(doc) < 0) {
                        //TODO: reference on node, listen on DOM removal to clean the ref?
                        attachedPointerStartMethods.push(doc);
                        doc.addEventListener(POINTER_DOWN, pointerListener, useCapture);
                        doc.addEventListener(POINTER_MOVE, pointerListener, useCapture);
                        doc.addEventListener(POINTER_UP, pointerListener, useCapture);
                        doc.addEventListener(POINTER_CANCEL, pointerListener, useCapture);
                        doc.addEventListener(GESTURE_START, gestureListener, useCapture);
                        doc.addEventListener(GESTURE_CHANGE, gestureListener, useCapture);
                        doc.addEventListener(GESTURE_END, gestureListener, useCapture);
                    }

                    // e.g. Document has no style
                    if (this.style && (typeof this.style[TOUCH_ACTION] == "undefined" || !this.style[TOUCH_ACTION])) {
                        this.style[TOUCH_ACTION] = "none";
                    }
                },
                /**
                 * This method detach event handler for MSPointer / MSGesture events when user
                 * tries to detach touch / gesture events.
                 * @param {String} type
                 * @param {Function} listener
                 * @param {Boolean} useCapture
                 */
                removeTouchEvents = function (type, listener, useCapture) {
                    //todo: are we able to understand when all listeners are unregistered and shall be removed?
                };


        /*
         * Adding DocumentTouch interface
         * @see http://www.w3.org/TR/2011/WD-touch-events-20110505/#idl-def-DocumentTouch
         */

        /**
         * Create touches list from array or touches or given touch
         * @param {Touch[]|Touch} touches
         * @returns {TouchList}
         */
        document.createTouchList = function(touches) {
            var touchList = new TouchList();
            if (touches) {
                if (touches.length) {
                    touchList._addAll(touches);
                } else {
                    touchList._add(touches);
                }
            }
            return touchList;
        };

        /*******  Fakes which persuade other code to use touch events ********/

        /**
         * AbstractView is class for document.defaultView === window
         * @param {AbstractView} view
         * @param {EventTarget} target
         * @param {Number} identifier
         * @param {Number} pageX
         * @param {Number} pageY
         * @param {Number} screenX
         * @param {Number} screenY
         * @return {Touch}
         */
        document.createTouch = function(view, target, identifier, pageX, pageY, screenX, screenY) {
            return {
                identifier: identifier,
                screenX: screenX,
                screenY: screenY,
                //clientX: clientX,
                //clientY: clientY,
                pageX: pageX,
                pageY: pageY,
                target: target
            };
        };
        //Fake Modernizer touch test
        //http://modernizr.github.com/Modernizr/touch.html
        if (!window.ontouchstart) window.ontouchstart = 1;

        /*******  End of fakes ***********************************/

        generalTouchesHolder = document.createTouchList();

        // Overriding HTMLElement and HTMLDocument to hand over touch handler to MSPointer event handler
        augmentEventListener(SVGElement);
        augmentEventListener(Document);
    }
}(window));

var root = document.documentElement;
var state = 'none', stateTarget, stateOrigin, stateTf, pointer = {}, canMove = true;
var debounceWithRAF = (function (func, wait, immediate) {
    var orig = window.parent._.debounce, timeout, result, rAF = window.requestAnimationFrame,
            cancelRAF = window.cancelAnimationFrame, vendors = ['ms',
                'moz', 'webkit', 'o'], init;
    init = function () {
        for (var x = 0; x < vendors.length && !rAF; ++x) {
            rAF = window[vendors[x] + 'RequestAnimationFrame'];
            cancelRAF = window[vendors[x] + 'CancelAnimationFrame']
                    || window[vendors[x] + 'CancelRequestAnimationFrame'];
        }
    };
    init();
    if (rAF) {
        return function (func, wait) {
            var timeout, result;
            return function () {
                var context = this, args = arguments;
                var later = function () {
                    timeout = null;
                    result = func.apply(context, args);
                };
                cancelRAF(timeout);
                timeout = rAF(later);
                return result;
            }
        }
    }
    else {
        return orig;
    }
})();
/**
 * Handle mouse move event.
 */
var mouseWheelHandler = (function () {

    var handle, gCTM, debounceFunction, slideValue = 0, currentZoomValue = 0;

    // A private function which logs any arguments
    handle = function (evt) {
        if (evt.preventDefault) {
            evt.preventDefault();
        }

        evt.returnValue = false;
        canMove = false;
        var svgDoc = evt.target.ownerDocument;

        var z = 1;

        var g = svgDoc.getElementById("viewport");

        var p = getEventPoint(evt);

        if (!gCTM && !window.parent.$(g).data("ctm")) {
            gCTM = g.getCTM();
        }
        else if (!gCTM) {
            gCTM = window.parent.$(g).data("ctm");
        }

        if (currentZoomValue == 0 && !window.parent.heavySVGs) {
            currentZoomValue = window.parent.getCurrentZoomValue(evt.currentTarget.getAttribute("data-containerId"));
        }

        p = p.matrixTransform(gCTM.inverse());
        var containerId = evt.currentTarget.getAttribute("data-containerId");
        var zoomIn = true;
        var positiveDelta = 1;
        var negetiveDelta = (-1) * (positiveDelta);
        if (evt.wheelDelta) {
            zoomIn = (evt.wheelDelta > 0);
            if (!window.parent.checkIfZoomAllowed(currentZoomValue, zoomIn) && !window.parent.heavySVGs) {
                return;
            }
            if (zoomIn) {
                z = window.parent.calculateZoomFactor(positiveDelta);
            }
            else {
                z = window.parent.calculateZoomFactor(negetiveDelta);
            }
        }
        else {
            zoomIn = (evt.detail < 0);
            if (!window.parent.checkIfZoomAllowed(currentZoomValue, zoomIn) && !window.parent.heavySVGs) {
                return;
            }
            if (zoomIn) {
                z = window.parent.calculateZoomFactor(positiveDelta);
            }
            else {
                z = window.parent.calculateZoomFactor(negetiveDelta);
            }
        }

        // Compute new scale matrix in current mouse position
        var k = root.createSVGMatrix().translate(p.x, p.y).scale(z).translate(-p.x, -p.y);

        gCTM = gCTM.multiply(k);

        if (typeof (stateTf) == "undefined") {
            stateTf = gCTM.inverse();
        }

        stateTf = stateTf.multiply(k.inverse());
        if (z < 1) {
            slideValue = slideValue + positiveDelta * window.parent.mentor.publisher.constants.NegetiveSliderStep;
        }
        else {
            slideValue = slideValue + positiveDelta * window.parent.mentor.publisher.constants.PositiveSliderStep;
        }
        currentZoomValue = parseInt(slideValue) + parseInt(currentZoomValue * 1);

        function animate()
        {
            try {
                setCTM(g, gCTM);
                window.parent.moveZoomSlider(slideValue, slideValue, containerId);
            }
            catch (e) {
            }
            gCTM = null;
            currentZoomValue = 0;
            slideValue = 0;
            canMove = true;
        };
        if (!debounceFunction) {
            debounceFunction = window.parent._.debounce(animate, 50);
        }
        debounceFunction();
    };

    return {
        handleMouseWheel: function (evt) {
            handle(evt);
        }
    };

})();
setupHandlers(root);

/**
 * Register handlers
 */
function setupHandlers(root)
{
    if (window.touchEnabled) {
        root.addEventListener('click', handleMouseClick, false);
    }
    else {
        setAttributes(root, {
            "onmousedown": "handleMouseDown(evt)",
            "onmousemove": "handleMouseMove(evt)",
            "onmouseup": "handleMouseUp(evt)",
            "onclick": "handleMouseClick(evt)"
        });

        var eventListenerOptions = (function () {
            var supportsPassive = false;
            try {
                var opts = Object.defineProperty({}, 'passive', {
                    get: function() {
                        supportsPassive = true;
                    }
                });
                window.addEventListener("testPassive", null, opts);
                window.removeEventListener("testPassive", null, opts);
            } catch (e) {}
            return supportsPassive ? {passive: false, capture: false} : false;
        })();

        if (navigator.userAgent.toLowerCase().indexOf('webkit') >= 0 ||
                navigator.appName === "Microsoft Internet Explorer") {
            root.addEventListener('mousewheel', mouseWheelHandler.handleMouseWheel, false);
        } // Chrome/Safari
        else {
            root.addEventListener('DOMMouseScroll', mouseWheelHandler.handleMouseWheel, false);
        } // Others
        root.addEventListener('mousewheel', mouseWheelHandler.handleMouseWheel, false);
        root.addEventListener('touchstart', touchStartHandler, eventListenerOptions);
        root.addEventListener('touchmove', touchMoveHandler, eventListenerOptions);
        root.addEventListener('touchend', touchEndHandler, eventListenerOptions);
        root.addEventListener('pointerdown', onPointerDown, false);
        root.addEventListener('pointerup', onPointerUp, false);
    }
}

var STATE = {NONE: -1, PAN: 0, ZOOM: 1};
var cState = STATE.NONE;
var pinchStartDist = 0;
var pinchMoveDist = 0;
var pinchZoomOnX = 0;
var pinchZoomOnY = 0;
var __lastTouchTimeoutId = null;
var __longPressTimerId = null;
var ongoingTouches = [];

var pinchZoomHandler = (function () {

    var handle, gCTM, debounceFunction, slideValue = 0, currentZoomValue = 0;

    // A private function which logs any arguments
    handle = function (evt) {
        // if (evt.preventDefault) {
        //     evt.preventDefault();
        // }

        //evt.returnValue = false;
        canMove = false;
        var svgDoc = evt.target.ownerDocument;

        var z = 1;

        var g = svgDoc.getElementById("viewport");

        var p = getEventPointTouch2();

        if (!gCTM && !window.parent.$(g).data("ctm")) {
            gCTM = g.getCTM();
        }
        else if (!gCTM) {
            gCTM = window.parent.$(g).data("ctm");
        }

        if (currentZoomValue == 0 && !window.parent.heavySVGs) {
            currentZoomValue = window.parent.getCurrentZoomValue(evt.currentTarget.getAttribute("data-containerId"));
        }

        p = p.matrixTransform(gCTM.inverse());
        var containerId = evt.currentTarget.getAttribute("data-containerId");
        var zoomIn = true;
        var positiveDelta = .25;
        var negetiveDelta = (-1) * (positiveDelta);
        var delta = pinchMoveDist - pinchStartDist;
        pinchStartDist = pinchMoveDist;
        if (delta) {
            zoomIn = (delta > 0);
            if (!window.parent.checkIfZoomAllowed(currentZoomValue, zoomIn) && !window.parent.heavySVGs) {
                return;
            }
            if (zoomIn) {
                z = window.parent.calculateZoomFactor(positiveDelta);
            }
            else {
                z = window.parent.calculateZoomFactor(negetiveDelta);
            }
        }

        // Compute new scale matrix in current mouse position
        var k = root.createSVGMatrix().translate(p.x, p.y).scale(z).translate(-p.x, -p.y);

        gCTM = gCTM.multiply(k);

        if (typeof (stateTf) == "undefined") {
            stateTf = gCTM.inverse();
        }

        stateTf = stateTf.multiply(k.inverse());
        if (z < 1) {
            slideValue = slideValue + positiveDelta * window.parent.mentor.publisher.constants.NegetiveSliderStep;
        }
        else {
            slideValue = slideValue + positiveDelta * window.parent.mentor.publisher.constants.PositiveSliderStep;
        }
        currentZoomValue = parseInt(slideValue) + parseInt(currentZoomValue * 1);
        try {
            setCTM(g, gCTM);
            window.parent.moveZoomSlider(slideValue, slideValue, containerId);
        }
        catch (e) {
        }
        gCTM = null;
        currentZoomValue = 0;
        slideValue = 0;
        canMove = true;
        // function animate()
        // {
        //
        // };
        // if (!debounceFunction) {
        //     debounceFunction = window.parent._.debounce(animate, 50);
        // }
        // debounceFunction();
    };

    return {
        handlePinchZoom: function (evt) {
            handle(evt);
        }
    };

})();

function onPinchStart(evt)
{
    var dx = evt.touches[0].clientX - evt.touches[1].clientX;
    var dy = evt.touches[0].clientY - evt.touches[1].clientY;
    pinchStartDist = Math.sqrt(dx * dx + dy * dy);
    pinchZoomOnX = (evt.touches[0].clientX + evt.touches[1].clientX) / 2;
    pinchZoomOnY = (evt.touches[0].clientY + evt.touches[1].clientY) / 2;
}

function onPinchMove(evt)
{
    var dx = evt.touches[0].clientX - evt.touches[1].clientX;
    var dy = evt.touches[0].clientY - evt.touches[1].clientY;
    pinchMoveDist = Math.sqrt(dx * dx + dy * dy);
    pinchZoomHandler.handlePinchZoom(evt);
}

function onPinchEnd(evt)
{
    pinchStartDist = 0;
    pinchMoveDist = 0;
    pinchZoomOnX = 0;
    pinchZoomOnY = 0;
}

function getOngoingTouchIndexByIdentifier(touchId)
{
    for (var i = 0; i < ongoingTouches.length; i++) {
        var id = ongoingTouches[i].identifier;
        if (id === touchId) {
            return i;
        }
    }
    return -1;
}

function copyTouch(touchItem)
{
    return {
        identifier: touchItem.identifier,
        pageX: touchItem.pageX,
        pageY: touchItem.pageY,
        startPageX: touchItem.pageX,
        startPageY: touchItem.pageY,
        startTime: (new Date()).getTime(),
        lastActiveTime: (new Date()).getTime()
    };
}

function touchStartHandler(evt)
{
    // console.log("!!!! touchStartHandler");
    var changedTouches = evt.changedTouches;
    for (var i = 0; i < changedTouches.length; i++) {
        ongoingTouches.push(copyTouch(changedTouches[i]));
    }

    switch (evt.touches.length) {
        case 1:
            var containerId = evt.currentTarget.getAttribute("data-containerId");
            __longPressTimerId = setTimeout(onLongPress.bind(null, evt, containerId), 1000);
            cState = STATE.PAN;
            onTouchStart(evt);
            break;
        case 2:
            if (evt.preventDefault) {
                evt.preventDefault();
                evt.stopPropagation();
            }
            cState = STATE.ZOOM;
            onPinchStart(evt);
            break;
        default:
            cState = STATE.NONE;
            return;
    }
}

function touchMoveHandler(evt)
{
    // console.log("!!!! touchMoveHandler");
    var changedTouches = evt.changedTouches;
    for (var i = 0; i < changedTouches.length; i++) {
        var idx = getOngoingTouchIndexByIdentifier(changedTouches[i].identifier);
        if (idx >= 0) {
            var thisOngoingTouch = ongoingTouches[idx];
            thisOngoingTouch.pageX = changedTouches[i].pageX;
            thisOngoingTouch.pageY = changedTouches[i].pageY;
            thisOngoingTouch.lastActiveTime = (new Date()).getTime();
        }
    }

    if (evt.preventDefault) {
        evt.preventDefault();
        evt.stopPropagation();
    }
    switch (evt.touches.length) {
        case 1:
            if (cState !== STATE.PAN) {
                cState = STATE.NONE;
                onPinchEnd(evt);
                break;
            }
            onTouchMove(evt);
            break;
        case 2:
            if (cState !== STATE.ZOOM) {
                cState = STATE.NONE;
                onPinchEnd(evt);
                break;
            }
            onPinchMove(evt);
            break;
        default:
            return;
    }
}

function touchEndHandler(evt)
{
    // console.log("!!!! touchEndHandler");
    // update ongoingTouches lastActiveTime
    for (var i = 0; i < ongoingTouches.length; i++) {
        var touch = ongoingTouches[i];
        touch.lastActiveTime = (new Date()).getTime();
    }

    if (__longPressTimerId) {
        clearTimeout(__longPressTimerId);
        __longPressTimerId = null;
    } else {
        // if it is long press, disable default behaviour. so that highlight do not go away.
        // console.log("---- touchEndHandler preventDefault called ");
        evt.preventDefault && evt.preventDefault()
    }

    var changedTouches = evt.changedTouches;
    switch (changedTouches.length) {
        case 1:
            if (cState !== STATE.PAN) {
                cState = STATE.NONE;
                onPinchEnd(evt);
                break;
            }
            var changedTouch = changedTouches[0];
            var idx = getOngoingTouchIndexByIdentifier(changedTouch.identifier);
            if (idx >= 0) {
                var distanceMoved = getDistanceMoved(ongoingTouches[idx]);
                var activeTime = getActiveTime(ongoingTouches[idx]);
                // console.log("---- distanceMoved, activeTime: ", distanceMoved, activeTime);
                if (distanceMoved <= 0 && activeTime < 200) {
                    singleTouchPointHandler(evt);
                } else if (distanceMoved >= 10) {
                    // this is pan
                }
            }

            onTouchEnd(evt);
            break;
        case 2:
            if (evt.preventDefault) {
                evt.preventDefault();
                evt.stopPropagation();
            }
            onPinchEnd(evt);
            break;
        default:
            break;
    }
    for (var i = 0; i < changedTouches.length; i++) {
        var idx = getOngoingTouchIndexByIdentifier(changedTouches[i].identifier);
        if (idx >= 0) {
            ongoingTouches.splice(idx, 1);
        }
    }
}

function getDistanceMoved(ongoingTouch)
{
    var x1 = ongoingTouch.startPageX || 0;
    var x2 = ongoingTouch.pageX;
    var y1 = ongoingTouch.startPageY || 0;
    var y2 = ongoingTouch.pageY;
    var distanceSquared = Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2);
    var distanceMoved = Math.sqrt(distanceSquared);
    return distanceMoved;
}

function getActiveTime(ongoingTouch)
{
    return ongoingTouch.lastActiveTime - ongoingTouch.startTime;
}

function singleTouchPointHandler(evt)
{
    // console.log("!!!! singleTouchPointHandler");
    if (!__lastTouchTimeoutId) {
        var currentTarget = evt.currentTarget.getAttribute("data-containerId");
        __lastTouchTimeoutId = setTimeout(function() {
            __lastTouchTimeoutId = null;
            // show image slider
            showImageSlider(currentTarget);
        }.bind(this), 400);
    } else {
        clearTimeout(__lastTouchTimeoutId);
        __lastTouchTimeoutId = null;
        resetZoomLevel(evt);
    }
}

function resetZoomLevel(evt)
{
    // zoom to fit
    var containerId = evt.currentTarget.getAttribute("data-containerId");
    var topWindow = window.parent;
    var p = topWindow.mentor.publisher;
    topWindow.svgEventHandlers[containerId].zoomFit();

    // set zoom slider value to 100
    var currentZoomValue = topWindow.getCurrentZoomValue(containerId);
    var delta = 100 - currentZoomValue;
    topWindow.moveZoomSlider(delta, delta, containerId);
}

function showImageSlider(containerId) {
    var topWindow = window.parent;
    var p = topWindow.mentor.publisher;
    p.eventDispatcher.dispatchEvent(p.events.SHOW_SLIDER, { containerId: containerId });
}

/**
 * Handle mouse move event.
 */
function onTouchMove(evt)
{
    var containerId = evt.currentTarget.getAttribute("data-containerId");
    //todo why following call is needed
    if (!canMove) {
        //console.log('cant move');
        return;
    }
    //console.log('move');
    window.parent.svgEventHandlers[containerId].mouseMoveHanlder(evt);
    // if (evt.preventDefault) {
    //     evt.preventDefault();
    //     evt.stopPropagation();
    // }
    /* evt.returnValue = false; */
    var svgDoc = evt.target.ownerDocument;
    var g = svgDoc.getElementById("viewport");
    if (state === 'pan') {
        // Pan mode

        //window.parent.svgEventHandlers[containerId].doPan = true;
        var p = getEventPointTouch1(evt).matrixTransform(stateTf);
        setCTM(g, stateTf.inverse().translate(p.x - stateOrigin.x, p.y - stateOrigin.y));
    }
    else if (state === 'move') {
        // Move mode
        var p = getEventPointTouch1(evt).matrixTransform(g.getCTM().inverse());
        setCTM(stateTarget, root.createSVGMatrix().translate(p.x - stateOrigin.x,
                p.y - stateOrigin.y).multiply(g.getCTM().inverse()).multiply(stateTarget.getCTM()));
        stateOrigin = p;
    }
}

/**
 * Handle click event.
 */
function onTouchStart(evt)
{
    var containerId = evt.currentTarget.getAttribute("data-containerId");
    // if (evt.preventDefault) {
    //     evt.preventDefault();
    //     evt.stopPropagation();
    // }
    if (!window.parent.heavySVGs) {
        evt.target.style.cursor =
                ' url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAAlQTFRFAAAA////////c3ilYwAAAAN0Uk5T//8A18oNQQAAAExJREFUeNp0jwEOwCAIAw///+ixFqZm2qChl1CUYUE3NkRglpdMnqw/wCN24uzgZRUpOdoJLYGYQLGxiFp7BYx+xw7m2vrcR47gEWAAkHEBFiebq0wAAAAASUVORK5CYII="), auto';

    }
    var svgDoc = evt.target.ownerDocument;
    var g = svgDoc.getElementById("viewport");

    state = 'pan';

    stateTf = g.getCTM().inverse();

    stateOrigin = getEventPointTouch1(evt).matrixTransform(stateTf);
    window.parent.svgEventHandlers[containerId].mouseDownHandler(evt);
}

// For IE 11
var pointersMax = 0;
var pointersCurrent = 0;
var pointersActiveTimeInfo = [];

function onPointerDown(evt) {
    if (evt.pointerType !== "touch") {
        return;
    }

    pointersCurrent += 1;
    if (pointersCurrent > pointersMax) {
        pointersMax = pointersCurrent;
        pointersActiveTimeInfo.push({
            id: evt.pointerId,
            startTime: (new Date()).getTime()
        });
    }
}

function onPointerUp(evt) {
    if (evt.pointerType !== "touch") {
        return;
    }

    var pointerInfoIdx = getPointerInfoIndex(evt.pointerId);
    if (pointerInfoIdx >= 0) {
        var pointerInfo = pointersActiveTimeInfo[pointerInfoIdx];
        var pointerActiveTime = (new Date()).getTime() - pointerInfo.startTime;
        if (pointersMax == 1 && pointerActiveTime < 300) {
            handleMouseClick(evt);
            // console.log("@@@@ handleMouseClick || pointerId || pointerActiveTime: ", evt.pointerId, pointerActiveTime);
        }
        pointersCurrent -= 1;
        if (pointersCurrent == 0) {
            pointersMax = 0;
        }

        // update pointersActiveTimeInfo
        pointersActiveTimeInfo.splice(pointerInfoIdx, 1);
    }
}


function getPointerInfoIndex(pointerId) {
    if (!pointerId) {
        return -1;
    }

    for (var i = 0; i < pointersActiveTimeInfo.length; i++) {
        var pointerTimeInfo = pointersActiveTimeInfo[i];
        if (pointerTimeInfo.id === pointerId) {
            return i;
        }
    }

    return -1;
}

/**
 * Handle mouse button release event.
 */
function onTouchEnd(evt)
{
    var svgDoc = evt.target.ownerDocument;
    // if (evt.preventDefault) {
    //     evt.preventDefault();
    //     evt.stopPropagation();
    // }
    if (!window.parent.heavySVGs) {
        evt.target.style.cursor = 'default';
    }
    if (state == 'pan' || state == 'move' /*|| state == 'pinch-zoom'*/) {
        // Quit pan mode
        state = '';
        //scaling = false;
    }
}

function onLongPress(evt, containerId)
{
    // console.log("!!!! onLongPress, container: ", containerId);
    var topWindow = window.parent;
    topWindow.svgEventHandlers[containerId].longPressHandler(evt);
    evt.preventDefault && evt.preventDefault();
    __longPressTimerId = null;
}

/**
 * Instance an SVGPoint object with given event coordinates.
 */
function getEventPointTouch1(evt)
{
    var p = root.createSVGPoint();

    p.x = evt.touches[0].clientX;
    p.y = evt.touches[0].clientY;

    return p;
}

function getEventPointTouch2()
{
    var p = root.createSVGPoint();

    p.x = pinchZoomOnX;
    p.y = pinchZoomOnY;

    return p;
}

function handleMouseClick(event)
{
    window.parent.svgEventHandlers[event.currentTarget.getAttribute("data-containerId")].mouseClickHandler(event);
}

/**
 * Instance an SVGPoint object with given event coordinates.
 */
function getEventPoint(evt)
{
    var p = root.createSVGPoint();

    p.x = evt.clientX;
    p.y = evt.clientY;

    return p;
}

/**
 * Sets the current transform matrix of an element.
 */
function setCTM(element, matrix)
{
    //var s = "matrix(" + matrix.a + "," + matrix.b + "," + matrix.c + "," + matrix.d + "," + matrix.e + "," +
    //        matrix.f +
    //        ")";
    //
    //element.setAttribute("transform", s);
    //element.style.cssText = transformProp + ":"+s;
    //element.style[transformProp] = s;
    window.parent.setCTM(element, matrix);
}

/**
 * Dumps a matrix to a string (useful for debug).
 */
function dumpMatrix(matrix)
{
    var s = "[ " + matrix.a + ", " + matrix.c + ", " + matrix.e + "\n  " + matrix.b + ", " + matrix.d + ", " +
            matrix.f + "\n  0, 0, 1 ]";

    return s;
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

/**
 * Handle mouse move event.
 */
//function handleMouseWheel(evt)
//{
//    if (evt.preventDefault) {
//        evt.preventDefault();
//    }
//
//    evt.returnValue = false;
//
//    var svgDoc = evt.target.ownerDocument;
//    //window.parent.svgDoc = svgDoc;//Looks liek not used any where..Find the need of it and then enable it...Any unnecessary global variable adds to memory
//
//    var z = 1;
//
//    var g = svgDoc.getElementById("viewport");
//
//    var p = getEventPoint(evt);
//
//    p = p.matrixTransform(g.getCTM().inverse());
//
//    var zoomIn = true;
//    if (evt.wheelDelta) {
//        zoomIn = (evt.wheelDelta > 0);
//        if (!window.parent.checkIfZoomAllowed(evt.currentTarget.getAttribute("data-containerId"), zoomIn)) {
//            return;
//        }
//        if (zoomIn) {
//            z = window.parent.calculateZoomFactor(1);
//        }
//        else {
//            z = window.parent.calculateZoomFactor(-1);
//        }
//    }
//    else {
//        zoomIn = (evt.detail < 0);
//        if (!window.parent.checkIfZoomAllowed(evt.currentTarget.getAttribute("data-containerId"), zoomIn)) {
//            return;
//        }
//        if (zoomIn) {
//            z = window.parent.calculateZoomFactor(1);
//        }
//        else {
//            z = window.parent.calculateZoomFactor(-1);
//        }
//    }
//
//    // Compute new scale matrix in current mouse position
//    var k = root.createSVGMatrix().translate(p.x, p.y).scale(z).translate(-p.x, -p.y);
//
//    setCTM(g, g.getCTM().multiply(k));
//
//    if (typeof(stateTf) == "undefined") {
//        stateTf = g.getCTM().inverse();
//    }
//
//    stateTf = stateTf.multiply(k.inverse());
//
//    var slideValue = window.parent.mentor.publisher.constants.PositiveSliderStep;
//    if (z < 1) {
//        slideValue = window.parent.mentor.publisher.constants.NegetiveSliderStep;
//    }
//    window.parent.moveZoomSlider(slideValue, slideValue, evt.currentTarget.getAttribute("data-containerId"));
//
//    //    window.parent.testFun( z );
//}

/**
 * Handle mouse move event.
 */
function handleMouseMove(evt)
{

    var containerId = evt.currentTarget.getAttribute("data-containerId");

    //todo why following call is needed
    if (!canMove) {
        //console.log('cant move');
        return;
    }
    //console.log('move');
    window.parent.svgEventHandlers[containerId].mouseMoveHanlder(evt);
    if (evt.preventDefault) {
        evt.preventDefault();
    }

    evt.returnValue = false;

    var svgDoc = evt.target.ownerDocument;

    var g = svgDoc.getElementById("viewport");

    if (state === 'pan') {
        // Pan mode

        //window.parent.svgEventHandlers[containerId].doPan = true;
        var p = getEventPoint(evt).matrixTransform(stateTf);
        //console.log("mouse moving "+p.x +'-'+stateOrigin.x+'='+p.y +'-'+stateOrigin.y);
        setCTM(g, stateTf.inverse().translate(p.x - stateOrigin.x, p.y - stateOrigin.y));
    }
    else if (state === 'move') {
        // Move mode
        var p = getEventPoint(evt).matrixTransform(g.getCTM().inverse());

        setCTM(stateTarget, root.createSVGMatrix().translate(p.x - stateOrigin.x,
                p.y - stateOrigin.y).multiply(g.getCTM().inverse()).multiply(stateTarget.getCTM()));

        stateOrigin = p;

    }
}

/**
 * Handle click event.
 */
function handleMouseDown(evt)
{
    /* var pointerId = evt.pointerId;*/

    if (evt.preventDefault) {
        evt.preventDefault();
    }

    var containerId = evt.currentTarget.getAttribute("data-containerId");
    evt.returnValue = false;
    if (!window.parent.heavySVGs) {
        evt.target.style.cursor =
                ' url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAAlQTFRFAAAA////////c3ilYwAAAAN0Uk5T//8A18oNQQAAAExJREFUeNp0jwEOwCAIAw///+ixFqZm2qChl1CUYUE3NkRglpdMnqw/wCN24uzgZRUpOdoJLYGYQLGxiFp7BYx+xw7m2vrcR47gEWAAkHEBFiebq0wAAAAASUVORK5CYII="), auto';

    }
    var svgDoc = evt.target.ownerDocument;
    var g = svgDoc.getElementById("viewport");

    state = 'pan';

    stateTf = g.getCTM().inverse();

    stateOrigin = getEventPoint(evt).matrixTransform(stateTf);
    window.parent.svgEventHandlers[containerId].mouseDownHandler(evt);
}

/**
 * Handle mouse button release event.
 */
function handleMouseUp(evt)
{
    if (evt.preventDefault) {
        evt.preventDefault();
    }

    evt.returnValue = false;
    var svgDoc = evt.target.ownerDocument;
    if (!window.parent.heavySVGs) {
        evt.target.style.cursor = 'default';
    }

    if (state == 'pan' || state == 'move' /*|| state == 'pinch-zoom'*/) {
        // Quit pan mode
        state = '';
        //scaling = false;
    }
}

window.parent.resizeSvg = function (zoomFactor, containerId, isFromResetSVG) {

    ////console.log("resize SVG");
    var container = window.parent.document.getElementById(containerId);

    var svgDoc = container.getElementsByTagName('object')[0].contentDocument.documentElement.ownerDocument;

    var g = svgDoc.getElementById("viewport");

    var p = root.createSVGPoint();

    p.x = 0;
    p.y = 0;

    p = p.matrixTransform(g.getCTM().inverse());

    // Compute new scale matrix in current mouse position
    var k = root.createSVGMatrix().translate(p.x, p.y).scale(zoomFactor).translate(-p.x, -p.y);
    //
    setCTM(g, g.getCTM().multiply(k));
    //
    if (typeof (stateTf) == "undefined") {
        stateTf = g.getCTM().inverse();
    }

    stateTf = stateTf.multiply(k.inverse());

};

window.parent.setIntialSize = function () {

};
window.parent.currentState = function () {
    try {
        return state;
    }
    catch (e) {
        return '';
    }
};