(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["DimensionHoverHandle"] = factory();
	else
		root["DimensionHoverHandle"] = factory();
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
/******/ 	return __webpack_require__(__webpack_require__.s = "./js/SGO/DimensionHoverHandle.module_temp.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./js/SGO/DimensionHoverHandle.module_temp.js":
/*!****************************************************!*\
  !*** ./js/SGO/DimensionHoverHandle.module_temp.js ***!
  \****************************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n//© 2019 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC\n\n\n\n/**\n * @class DimensionHoverHandle SGO Object\n * @classdesc Represents the DimensionHoverHandle SGO Object for the PLMVisWeb Viewer.<br><br>\n */\n\nvar DimensionHoverHandle = function ( type, color, size ) { //NOSONAR\n\n\t// Private variables\n\tvar _object = null,\n\t\t_scene = null,\n\t\t_camera = null,\n\t\t_viewer = null,\n\n\t\t// Default variables to set\n\t\t_width = size ? size.width : 100,\n\t\t_height = size ? size.height : 100,\n\t\t_positionX = 0,\n\t\t_positionY = 0,\n\n\t\t// User-defined variables\n\t\t_controlPointInfo,\n\t\t_manager,\n\t\t_flip = false;\n\n\tvar _DimensionHoverHandle = function () {\n\n\t\t_createObject( type );\n\t\t_createScene();\n\n\t\tthis.name = \"DimensionHoverHandle\";\n\t\tthis.original = _object;\n\t\tthis.scene = _scene;\n\t\tthis.camera = _camera;\n\t\tthis.size = { width: _width, height: _height };\n\t\tthis.position = { x: _positionX, y: _positionY };\n\t\tthis.boundingBox = new THREE.Box3().setFromObject( _object );\n\t\tthis.boundingSphere = new THREE.Sphere();\n\t\tthis.boundingBox.getBoundingSphere( this.boundingSphere );\n\t\tthis.visible = true;\n\t\tthis.onTop = true;\n\n\t\tthis.front = true;\n\t\tthis.original.userData.ignoreRenderMode = true;\n\t};\n\n\t_DimensionHoverHandle.prototype = {\n\t\tconstructor: _DimensionHoverHandle\n\t};\n\n\n\tObject.defineProperties( _DimensionHoverHandle.prototype, {\n\n\t\tcamera: {\n\t\t\tget: function () {\n\t\t\t\treturn _camera;\n\t\t\t},\n\n\t\t\tset: function ( camera ) {\n\t\t\t\t_camera = camera;\n\t\t\t}\n\t\t},\n\n\t\tscene: {\n\t\t\tget: function () {\n\t\t\t\treturn _scene;\n\t\t\t},\n\n\t\t\tset: function ( scene ) {\n\t\t\t\t_scene = scene;\n\t\t\t}\n\t\t},\n\n\t\tviewer: {\n\t\t\tget: function () {\n\t\t\t\treturn _viewer;\n\t\t\t},\n\n\t\t\tset: function ( viewer ) {\n\t\t\t\t_viewer = viewer;\n\n\t\t\t\tif ( _viewer ) {\n\t\t\t\t\tthis.setPosition( _positionX, _positionY );\n\t\t\t\t\t_viewer.draw();\n\t\t\t\t}\n\t\t\t}\n\t\t},\n\n\t\tvisible: {\n\t\t\tget: function () {\n\t\t\t\treturn _object.visible;\n\t\t\t},\n\n\t\t\tset: function ( value ) {\n\t\t\t\t_object.visible = value;\n\t\t\t\tif ( _viewer ) {\n\t\t\t\t\t_viewer.draw();\n\t\t\t\t}\n\t\t\t}\n\t\t},\n\n\t\tflip: {\n\t\t\tget: function () {\n\t\t\t\treturn _flip;\n\t\t\t},\n\t\t\tset: function ( value ) {\n\t\t\t\t_flip = value;\n\t\t\t}\n\t\t},\n\n\t\tmanager: {\n\t\t\tget: function () {\n\t\t\t\treturn _manager;\n\t\t\t},\n\n\t\t\tset: function ( manager ) {\n\t\t\t\t_manager = manager;\n\t\t\t}\n\t\t},\n\n\t\tcontrolPointInfo: {\n\t\t\tget: function () {\n\t\t\t\treturn _controlPointInfo;\n\t\t\t},\n\n\t\t\tset: function ( controlPointInfo ) {\n\t\t\t\t_controlPointInfo = controlPointInfo;\n\t\t\t}\n\t\t}\n\t} );\n\n\tfunction _createObject ( type ) {\n\t\t_object = new THREE.Object3D();\n\t\tvar material = color ? new THREE.MeshPhongMaterial( { color: color } ) : new THREE.MeshPhongMaterial( { color: 0xff0000 } );\n\n\t\tvar obj = null;\n\t\tif ( type === 'Cone' ) {\n\t\t\t//  _width / 4\n\t\t\tvar cylinderGeometry = new THREE.CylinderBufferGeometry( 0.8, 0.8, 8, 32 );\n\t\t\tvar cylinder = new THREE.Mesh( cylinderGeometry, material );\n\t\t\tvar coneGeometry = new THREE.CylinderBufferGeometry( 2, 0, 8, 32 );\n\t\t\tvar cone = new THREE.Mesh( coneGeometry, material );\n\t\t\tcylinder.position.y = 12;\n\t\t\tcone.position.y = 4;\n\n\t\t\tvar group = obj = new THREE.Group();\n\t\t\tgroup.add( cylinder );\n\t\t\tgroup.add( cone );\n\t\t} else if ( type === 'Sphere' ) {\n\t\t\t//  _width / 4\n\t\t\tvar sphereGeometry = new THREE.SphereGeometry( _width / 5, 20, 20 );\n\t\t\tobj = new THREE.Mesh( sphereGeometry, material );\n\t\t}\n\n\t\t_object.add( obj );\n\t}\n\n\tfunction _createScene () {\n\t\t_scene = new THREE.Scene();\n\t\t_camera = new THREE.OrthographicCamera( _width * -0.25, _width * 0.25, _height * 0.25, _height * -0.25, -100, 100 );\n\n\t\t// add subtle ambient lighting\n\t\tvar ambientLight = new THREE.AmbientLight( 0x292929 );\n\t\t_scene.add( ambientLight );\n\n\t\tvar dlight1 = new THREE.DirectionalLight( 0xffffff, 0.7 );\n\t\tdlight1.position.set( -100, 100, 100 );\n\t\t_scene.add( dlight1 );\n\n\t\tvar dlight2 = new THREE.DirectionalLight( 0xffffff, 0.7 );\n\t\tdlight2.position.set( 10, -100, -80 );\n\t\t_scene.add( dlight2 );\n\n\t\tvar dlight3 = new THREE.DirectionalLight( 0xffffff, 0.7 );\n\t\tdlight3.position.set( 100, 0, 0 );\n\t\t_scene.add( dlight3 );\n\n\t\t_scene.add( _camera );\n\t\t_scene.add( _object );\n\t}\n\n\tfunction _unprojectScreenDirection ( p1, p2 ) {\n\t\t// var viewport = _viewer._renderer.getCurrentViewport();\n\t\tvar v1 = new THREE.Vector3( 2 * ( ( p1[ 0 ] - ( _positionX - _width / 2 ) ) / _width ) - 1,\n\t\t\t1 - 2 * ( ( p1[ 1 ] - ( _positionY - _height / 2 ) ) / _height ),\n\t\t\t0 );\n\n\t\tvar v2 = new THREE.Vector3( 2 * ( ( p2[ 0 ] - ( _positionX - _width / 2 ) ) / _width ) - 1,\n\t\t\t1 - 2 * ( ( p2[ 1 ] - ( _positionY - _height / 2 ) ) / _height ),\n\t\t\t0 );\n\n\t\tv1.unproject( _camera );\n\t\tv2.unproject( _camera );\n\n\t\tvar direction = v2.sub( v1 ).normalize();\n\t\treturn direction;\n\t};\n\n\n\t_DimensionHoverHandle.prototype.updatePosition = function () {\n\t\tif ( !_controlPointInfo ) { return; }\n\n\t\tvar pointTypes = [ { pointType: _controlPointInfo.pointType, side: _controlPointInfo.side } ];\n\t\tvar controlPoints = _manager.getControlPoints( _controlPointInfo.dimensionId, pointTypes );\n\n\t\tif ( controlPoints ) {\n\t\t\tvar point = controlPoints[ 0 ].value;\n\t\t\tif ( point ) {\n\t\t\t\tvar position2d = this.viewer.projectModelPointToViewCoordinate( point[ 0 ], point[ 1 ], point[ 2 ] );\n\t\t\t\tthis.setPosition( position2d[ 0 ], position2d[ 1 ] );\n\t\t\t}\n\t\t}\n\t};\n\n\n\t/**\n\t * Defines how to render the DimensionHoverHandle\n\t *\n\t * @function render\n\t * @memberof SceneGraphObject.prototype\n\t *\n\t*/\n\t_DimensionHoverHandle.prototype.render = function ( renderer, camInfo ) {\n\t\trenderer.setViewport(\n\t\t\t_positionX - _width / 2,\n\t\t\t_positionY - _height / 2,\n\t\t\t_width,\n\t\t\t_height\n\t\t);\n\n\t\tvar camPos = new THREE.Vector3().fromArray( camInfo.perspective.pos );\n\n\t\tvar tgt = new THREE.Vector3().fromArray( camInfo.perspective.tgt );\n\t\tcamPos.sub( tgt );\n\t\tcamPos.normalize();\n\n\t\tif ( this.boundingSphere ) {\n\t\t\tcamPos.setLength( this.boundingSphere.radius );\n\t\t}\n\t\telse {\n\t\t\tcamPos.setLength( 50 );\n\t\t}\n\n\t\t_camera.position.copy( camPos );\n\t\t_camera.up.fromArray( camInfo.perspective.up );\n\t\t_camera.lookAt( _scene.position );\n\n\t\tthis.updatePosition();\n\n\t\trenderer.render( _scene, _camera );\n\t};\n\n\t_DimensionHoverHandle.prototype.setDirection = function ( point1, point2 ) {\n\t\t// convert to sgo's world coordinate\n\t\tvar direction = _unprojectScreenDirection( point1, point2 );\n\n\t\tvar quaternion = new THREE.Quaternion().setFromUnitVectors( new THREE.Vector3( 0, 1, 0 ), direction );\n\t\tif ( this.flip ) {\n\t\t\tvar tmp = new THREE.Quaternion().setFromAxisAngle( new THREE.Vector3( 0, 0, 1 ), Math.PI );\n\t\t\tquaternion.multiply( tmp );\n\t\t}\n\n\t\tvar child = _object.children[ 0 ];\n\t\tchild.quaternion.copy( quaternion );\n\t\tchild.updateMatrix();\n\t\tchild.updateMatrixWorld();\n\n\t\tif ( _viewer ) {\n\t\t\t_viewer.draw();\n\t\t}\n\t};\n\n\t/**\n\t * Repositions the DimensionHoverHandle.\n\t *\n\t * @function setPosition\n\t * @memberof DimensionHoverHandle.prototype\n\t *\n\t * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.\n\t * @param {Number} y - vertical position in pixels from the bottom left of the viewport.\n\t */\n\t_DimensionHoverHandle.prototype.setPosition = function ( x, y ) {\n\t\t_positionX = x;\n\t\t_positionY = y;\n\t\tthis.position.x = x;\n\t\tthis.position.y = y;\n\t};\n\n\t/**\n\t * Queries the position of the DimensionHoverHandle.\n\t *\n\t * @function getPosition\n\t * @memberof DimensionHoverHandle.prototype\n\t *\n\t * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.\n\t */\n\t_DimensionHoverHandle.prototype.getPosition = function () {\n\t\treturn this.position;\n\t};\n\n\t/**\n\t * Sets the size of the DimensionHoverHandle.\n\t *\n\t * @function setSize\n\t * @memberof DimensionHoverHandle.prototype\n\t *\n\t * @param {Number} w - a number used to set the width of the DimensionHoverHandle render area.\n\t   * @param {Number} h - a number used to set the height of the DimensionHoverHandle render area.\n\t */\n\t_DimensionHoverHandle.prototype.setSize = function ( w, h ) {\n\t\t_width = w;\n\t\t_height = h;\n\t\tthis.size.width = w;\n\t\tthis.size.height = h;\n\t};\n\n\t/**\n\t * Queries the size of the DimensionHoverHandle.\n\t *\n\t * @function getSize\n\t * @memberof DimensionHoverHandle.prototype\n\t *\n\t * @returns {Object} an object that contains the height and width (each as a number) of the DimensionHoverHandle render area.\n\t */\n\t_DimensionHoverHandle.prototype.getSize = function () {\n\t\treturn this.size;\n\t};\n\n\t/**\n\t * Sets whether the DimensionHoverHandle is rendered on top of or behind the scene geometry.\n\t *\n\t * @function setOnTop\n\t * @memberof DimensionHoverHandle.prototype\n\t *\n\t * @params {Boolean} onTop - true if on top, false if behind.\n\t */\n\t_DimensionHoverHandle.prototype.setOnTop = function ( onTop ) {\n\t\tif ( this.onTop !== onTop || this.onTop === undefined ) {\n\t\t\tthis.onTop = onTop;\n\t\t\tif ( onTop ) {\n\t\t\t\tthis.front = true;\n\t\t\t\tthis.back = false;\n\t\t\t}\n\t\t\telse {\n\t\t\t\tthis.back = true;\n\t\t\t\tthis.front = false;\n\t\t\t}\n\t\t\tif ( _viewer ) {\n\t\t\t\t_viewer.renderOrderSGO( this );\n\t\t\t}\n\t\t}\n\t};\n\n\t/**\n\t * Queries whether the DimensionHoverHandle is rendered on top of or behind the scene geometry.\n\t *\n\t * @function getOnTop\n\t * @memberof DimensionHoverHandle.prototype\n\t *\n\t * @returns {Boolean} true if on top, false if behind.\n\t */\n\t_DimensionHoverHandle.prototype.getOnTop = function () {\n\t\treturn this.onTop;\n\t};\n\n\treturn new _DimensionHoverHandle();\n};\n\nDimensionHoverHandle.prototype = {\n\tconstructor: DimensionHoverHandle,\n\t_name: \"DimensionHoverHandle\"\n};\n\n/* harmony default export */ __webpack_exports__[\"default\"] = (DimensionHoverHandle);\n\n\n//# sourceURL=webpack://DimensionHoverHandle/./js/SGO/DimensionHoverHandle.module_temp.js?");

/***/ })

/******/ })["default"];
});