(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["Arrow"] = factory();
	else
		root["Arrow"] = factory();
})(window, function() {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = "./js/SGO/Arrow.module_temp.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./js/SGO/Arrow.module_temp.js":
/*!*************************************!*\
  !*** ./js/SGO/Arrow.module_temp.js ***!
  \*************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n//© 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC\n\n\n\n/**\n * @class Arrow\n * @classdesc Place an arrow at a mouse-click's position in the direction of the object's face normal.<br><br>\n */\n\n// Private variables\nvar _object = null,\n\t_scene = null,\n\t_camera = null,\n\t_viewer = undefined,\n\n\t// Default variables to set\n\t_width = 100,\n\t_height = 100,\n\t_positionX = 10,\n\t_positionY = 10,\n\n\t// Event variables\n\t_mouseDownHandler = null,\n\t_mouseUpHandler = null,\n\t_mouseMoveHandler = null,\n\t_resizeHandler = null,\n\n\t// User-defined variables\n\t_pCamera = null,\n\t_oCamera = null,\n\t_mouseDown = false,\n\t_mousePos = [];\n\nfunction Arrow ( color ) {\n\t_createObject( color );\n\t_createScene();\n\t_createEventVariables.call( this );\n\n\t// Public variables\n\tthis.name = \"Arrow\";\n\tthis.original = _object;\n\t//this.updateBBox = true; // NOTE: Applicable only for inSceneSGOs.Setting this property to true for the inSceneSGO's updates the global boundingbox to include  sgo's boundingbox\n\t//Deprecated:  updateBBox will be removed after the 1.7.0 release.  To update the global boundingbox to include sgo's boundingbox, use sgoBBox\n\tthis.scene = _scene;\n\tthis.camera = _camera;\n\tthis.size = { width: _width, height: _height };\n\tthis.position = { x: _positionX, y: _positionY };\n\tthis.boundingBox = new THREE.Box3().setFromObject( _object );\n\tthis.boundingSphere = new THREE.Sphere();\n\tthis.boundingBox.getBoundingSphere( this.boundingSphere );\n\tthis.sgoBBox = this.boundingBox; // NOTE: Applicable for all SGOs that want the global boundingbox to include sgo's boundingbox\n\tthis.visible = true;\n\n\tthis.original.userData.ignoreRenderMode = true;\t\t// Determines if render modes should effect the SGO\n};\n\nArrow.prototype.constructor = Arrow;\n\nObject.defineProperties( Arrow.prototype, {\n\n\tcamera: {\n\t\tget: function () {\n\t\t\treturn _camera;\n\t\t},\n\n\t\tset: function ( camera ) {\n\t\t\t_camera = camera;\n\t\t}\n\t},\n\n\tscene: {\n\t\tget: function () {\n\t\t\treturn _scene;\n\t\t},\n\n\t\tset: function ( scene ) {\n\t\t\t_scene = scene;\n\t\t}\n\t},\n\n\tviewer: {\n\t\tget: function () {\n\t\t\treturn _viewer;\n\t\t},\n\n\t\tset: function ( viewer ) {\n\t\t\t_viewer = viewer;\n\n\t\t\tif ( _viewer ) {\n\t\t\t\tthis.setPosition( 0, 0 );\n\t\t\t\tvar dim = _viewer.control.getSize();\n\t\t\t\tthis.setSize( dim.width, dim.height );\n\n\t\t\t\tvar inputManager = _viewer.getInputManager();\n\t\t\t\tif ( inputManager ) {\n\t\t\t\t\tinputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Down, _mouseDownHandler );\n\t\t\t\t\tinputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Up, _mouseUpHandler );\n\t\t\t\t}\n\t\t\t\twindow.addEventListener( \"resize\", _resizeHandler );\n\t\t\t}\n\t\t}\n\t},\n\n\tvisible: {\n\t\tget: function () {\n\t\t\treturn _object.visible;\n\t\t},\n\n\t\tset: function ( value ) {\n\t\t\tif ( _object.visible !== value ) {\n\t\t\t\t_object.visible = value;\n\t\t\t\tif ( _viewer ) {\n\t\t\t\t\t_viewer.draw();\n\t\t\t\t}\n\t\t\t}\n\t\t}\n\t}\n} );\n\n\nfunction _createObject ( color ) {\n\tcolor = ( color === undefined ) ? 0xffff00 : color;\n\t_object = new THREE.ArrowHelper( new THREE.Vector3(), new THREE.Vector3(), 1, color, 0.2, 0.2 );\n}\n\nfunction _createScene () {\n\t_scene = new THREE.Scene();\n\t_pCamera = new THREE.PerspectiveCamera();\n\t_oCamera = new THREE.OrthographicCamera();\n}\n\nfunction _createEventVariables () {\n\t_resizeHandler = function resize () {\n\t\tvar dim = _viewer.control.getSize();\n\t\tthis.setSize( dim.width, dim.height );\n\t\t_viewer.draw();\n\t}.bind( this );\n\n\t_mouseDownHandler = this.mouseDown.bind( this );\n\t_mouseUpHandler = this.mouseUp.bind( this );\n\t_mouseMoveHandler = this.mouseMove.bind( this );\n}\n\n/**\n * Defines how to render the Arrow\n *\n * @function render\n * @memberof Arrow.prototype\n *\n */\nArrow.prototype.render = function ( renderer, camInfo ) {\n\n\trenderer.setViewport(\n\t\t_positionX,\n\t\t_positionY,\n\t\t_width,\n\t\t_height\n\t);\n\n\tif ( _viewer.getCameraMode() === PLMVisWeb.CameraMode.PERSPECTIVE ) {\n\n\t\t_pCamera.position.fromArray( camInfo.perspective.pos );\n\t\t_pCamera.lookAt( new THREE.Vector3().fromArray( camInfo.perspective.tgt ) );\n\t\t_pCamera.up.fromArray( camInfo.perspective.up );\n\t\t_pCamera.near = camInfo.perspective.near;\n\t\t_pCamera.far = camInfo.perspective.far;\n\t\t_pCamera.fov = camInfo.perspective.fov;\n\t\t_pCamera.aspect = camInfo.perspective.aspect;\n\t\tthis.camera = _pCamera;\n\t}\n\telse {\n\n\t\t_oCamera.position.fromArray( camInfo.orthographic.pos );\n\t\t_oCamera.lookAt( new THREE.Vector3().fromArray( camInfo.orthographic.tgt ) );\n\t\t_oCamera.up.fromArray( camInfo.orthographic.up );\n\t\t_oCamera.near = camInfo.orthographic.near;\n\t\t_oCamera.far = camInfo.orthographic.far;\n\t\t_oCamera.left = camInfo.orthographic.left;\n\t\t_oCamera.right = camInfo.orthographic.right;\n\t\t_oCamera.bottom = camInfo.orthographic.bottom;\n\t\t_oCamera.top = camInfo.orthographic.top;\n\t\tthis.camera = _oCamera;\n\t}\n\n\tthis.camera.updateProjectionMatrix();\n\n\trenderer.render( _scene, this.camera );\n};\n\n/**\n * Removes attached events\n *\n * @function removeEvents\n * @memberof Arrow.prototype\n *\n */\nArrow.prototype.removeEvents = function () {\n\tif ( _viewer ) {\n\t\tvar inputManager = _viewer.getInputManager();\n\t\tinputManager.removeEventListener( PLMVisWeb.InputManager.EventTypes.Down, _mouseDownHandler );\n\t\tinputManager.removeEventListener( PLMVisWeb.InputManager.EventTypes.Up, _mouseUpHandler );\n\t\twindow.removeEventListener( \"resize\", _resizeHandler );\n\t}\n};\n\n/**\n * Repositions the SGO.\n *\n * @function setPosition\n * @memberof Arrow.prototype\n *\n * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.\n * @param {Number} y - vertical position in pixels from the bottom left of the viewport.\n */\nArrow.prototype.setPosition = function ( x, y ) {\n\t_positionX = x;\n\t_positionY = y;\n\tthis.position.x = x;\n\tthis.position.y = y;\n};\n\n/**\n * Queries the position of the SGO\n *\n * @function getPosition\n * @memberof Arrow.prototype\n *\n * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.\n */\nArrow.prototype.getPosition = function () {\n\treturn this.position;\n};\n\n/**\n * Sets the size of the SGO.\n *\n * @function setSize\n * @memberof Arrow.prototype\n *\n * @param {Number} value - a single value used to set the height and width of the WCS render area.\n */\nArrow.prototype.setSize = function ( w, h ) {\n\t_width = w;\n\t_height = h;\n\tthis.size.width = w;\n\tthis.size.height = h;\n};\n\n/**\n * Queries the size of the SGO.\n *\n * @function getSize\n * @memberof Arrow.prototype\n *\n * @returns {Object} a single value used to set the height and width of the WCS render area.\n */\nArrow.prototype.getSize = function () {\n\treturn this.size;\n};\n\n/**\n *\n *  Mouse events pass through the viewer to the SGO and can be defined here.\n *\n *  Add 'event.override = true' to ignore the mouse event in the viewer\n *\n */\nArrow.prototype.mouseUp = function ( event ) {\n\tif ( _mouseDown && ( _mousePos[ 0 ] === event.offsetX || _mousePos[ 1 ] === event.offsetY ) ) {\n\n\t\tvar point = _viewer.getModelPointAtViewCoordinate( event.offsetX, event.offsetY );\n\t\tif ( point ) {\n\t\t\tpoint = new THREE.Vector3().fromArray( point );\n\t\t\tif ( _object ) {\n\t\t\t\t_scene.remove( _object );\n\t\t\t}\n\t\t\tvar dir = new THREE.Vector3().fromArray( _viewer.getFaceNormalAtViewCoordinate( event.offsetX, event.offsetY ) ).normalize();\n\t\t\tvar origin = new THREE.Vector3( point.x, point.y, point.z );\n\t\t\tvar length = _viewer.getVisibleModelBoundingBoxLength() / 25;\n\t\t\t_object.setDirection( dir );\n\t\t\t_object.setLength( length, 0.2 * length, 0.2 * length );\n\t\t\t_object.position.copy( origin );\n\t\t\t_scene.add( _object );\n\t\t}\n\t\t_viewer.draw();\n\t\t_viewer.setPickingEnabled( true );\n\t}\n};\n\nArrow.prototype.mouseDown = function ( event ) {\n\t_mouseDown = true;\n\t_mousePos = [ event.offsetX, event.offsetY ];\n\t_viewer.setPickingEnabled( false );\n};\n\nArrow.prototype.mouseMove = function () { };\n\n/* harmony default export */ __webpack_exports__[\"default\"] = (Arrow);\n\n\n//# sourceURL=webpack://Arrow/./js/SGO/Arrow.module_temp.js?");

/***/ })

/******/ })["default"];
});