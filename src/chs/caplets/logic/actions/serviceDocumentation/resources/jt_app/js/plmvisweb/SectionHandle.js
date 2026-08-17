(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["SectionHandle"] = factory();
	else
		root["SectionHandle"] = factory();
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
/******/ 	return __webpack_require__(__webpack_require__.s = "./js/SGO/SectionHandle.module_temp.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./js/SGO/ArrowHandle.module_temp.js":
/*!*******************************************!*\
  !*** ./js/SGO/ArrowHandle.module_temp.js ***!
  \*******************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n//© 2019 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC\n\n\n\n/**\n * @class Arrow Handle\n * @classdesc Represents the arrow with handle.<br><br>\n */\n// Private variables\nvar _object = null,\n\t_scene = null,\n\t_camera = null,\n\t_viewer = null,\n\n\t// Default variables to set\n\t_width = 300,\n\t_height = 300,\n\t_positionX = 10,\n\t_positionY = 10,\n\n\t// Event variables\n\t_mouseDownHandler = null,\n\t_mouseUpHandler = null,\n\t_mouseMoveHandler = null,\n\n\t// User-defined variables\n\t_textNodes = [];\n\nfunction ArrowHandle ( arrowColor, handleColor ) { //NOSONAR\n\n\t_createObject( arrowColor, handleColor );\n\t_createScene();\n\t_createEventVariables( this );\n\n\tthis.name = \"ArrowHandle\";\n\tthis.original = _object;\n\tthis.scene = _scene;\n\tthis.camera = _camera;\n\tthis.size = { width: _width, height: _height };\n\tthis.position = { x: _positionX, y: _positionY };\n\tthis.boundingBox = new THREE.Box3().setFromObject( _object );\n\tthis.boundingSphere = new THREE.Sphere();\n\tthis.boundingBox.getBoundingSphere( this.boundingSphere );\n\tthis.visible = false;\n\tthis.onTop = true;\n\n\tthis.front = true;\n\tthis.original.userData.ignoreRenderMode = true;\n}\n\nArrowHandle.prototype.constructor = ArrowHandle;\n\nObject.defineProperties( ArrowHandle.prototype, {\n\n\tcamera: {\n\t\tget: function () {\n\t\t\treturn _camera;\n\t\t},\n\n\t\tset: function ( camera ) {\n\t\t\t_camera = camera;\n\t\t}\n\t},\n\n\tscene: {\n\t\tget: function () {\n\t\t\treturn _scene;\n\t\t},\n\n\t\tset: function ( scene ) {\n\t\t\t_scene = scene;\n\t\t}\n\t},\n\n\tviewer: {\n\t\tget: function () {\n\t\t\treturn _viewer;\n\t\t},\n\t\tset: function ( viewer ) {\n\t\t\t_viewer = viewer;\n\t\t\tif ( _viewer ) {\n\t\t\t\tvar inputManager = _viewer.getInputManager();\n\t\t\t\tif ( inputManager ) {\n\t\t\t\t\tinputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Down, _mouseDownHandler, 0 );\n\t\t\t\t\tinputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Up, _mouseUpHandler, 0 );\n\t\t\t\t\tinputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Move, _mouseMoveHandler, 0 );\n\t\t\t\t}\n\t\t\t}\n\t\t}\n\t},\n\n\tvisible: {\n\t\tget: function () {\n\t\t\treturn _object.visible;\n\t\t},\n\n\t\tset: function ( value ) {\n\t\t\tif ( _object.visible !== value ) {\n\t\t\t\t_object.visible = value;\n\t\t\t\tif ( _viewer ) {\n\t\t\t\t\t_viewer.draw();\n\t\t\t\t}\n\t\t\t}\n\t\t}\n\t}\n} );\n\nfunction _createObject ( arrowColor, handleColor ) {\n\t_object = new THREE.Object3D();\n\n\tvar arrowMaterial = arrowColor ? new THREE.MeshPhongMaterial( { color: arrowColor } ) : new THREE.MeshPhongMaterial( { color: 0xf3dd5a } );\n\tvar handleMaterial = handleColor ? new THREE.MeshPhongMaterial( { color: handleColor } ) : new THREE.MeshPhongMaterial( { color: 0xdcdcdc } );\n\n\tvar cylinderGeometry = new THREE.CylinderBufferGeometry( 0.5, 1, 30, 32 );\n\tvar cylinder = new THREE.Mesh( cylinderGeometry, handleMaterial );\n\tvar coneGeometry = new THREE.CylinderBufferGeometry( 2.4, 0, 10, 32 );\n\tvar cone = new THREE.Mesh( coneGeometry, arrowMaterial );\n\tcylinder.position.y = -15;\n\tcone.position.y = -35;\n\t_object.add( cylinder );\n\t_object.add( cone );\n}\n\nfunction _createScene () {\n\n\t_scene = new THREE.Scene();\n\t_width = 200;\n\t_height = 200;\n\t_camera = new THREE.OrthographicCamera( _width * -0.25, _width * 0.25, _height * 0.25, _height * -0.25, -100, 100 );\n\n\tvar dlight1 = new THREE.DirectionalLight( 0xffffff );\n\tdlight1.position.set( 0, 0, 0 );\n\tdlight1.target.position.set( 1, -1, -1 );\n\n\tvar dlight2 = new THREE.DirectionalLight( 0xffffff );\n\tdlight2.position.set( 0, 0, 0 );\n\tdlight2.target.position.set( -1, -1, -1 );\n\n\tvar ambientLight = new THREE.AmbientLight( 0xffffff );\n\tvar intensity = 0.3;\n\tambientLight.color.setRGB( intensity, intensity, intensity );\n\n\t_scene.add( _camera );\n\t_scene.add( _object );\n\t_scene.add( dlight1 );\n\t_scene.add( dlight1.target );\n\t_scene.add( dlight2 );\n\t_scene.add( dlight2.target );\n\t_scene.add( ambientLight );\n}\n\nfunction _createEventVariables ( handle ) {\n\t_mouseDownHandler = handle.mouseDown.bind( handle );\n\t_mouseUpHandler = handle.mouseUp.bind( handle );\n\t_mouseMoveHandler = handle.mouseMove.bind( handle );\n}\n\n/**\n * Defines how to render the ArrowHandle\n *\n * @function render\n * @memberof ArrowHandle.prototype\n *\n */\nArrowHandle.prototype.render = function ( renderer, camInfo ) {\n\trenderer.setViewport(\n\t\t_positionX - _width / 2,\n\t\t_positionY - _height / 2,\n\t\t_width,\n\t\t_height\n\t);\n\n\tvar camPos = new THREE.Vector3().fromArray( camInfo.perspective.pos );\n\n\tvar tgt = new THREE.Vector3().fromArray( camInfo.perspective.tgt );\n\tcamPos.sub( tgt );\n\tcamPos.normalize();\n\n\tif ( this.boundingSphere ) {\n\t\tcamPos.setLength( this.boundingSphere.radius );\n\t}\n\telse {\n\t\tcamPos.setLength( 50 );\n\t}\n\n\n\t_camera.position.copy( camPos );\n\t_camera.up.fromArray( camInfo.perspective.up );\n\t_camera.lookAt( _scene.position );\n\n\trenderer.render( _scene, _camera );\n};\n\n/**\n * Removes attached events\n *\n * @function removeEvents\n * @memberof ArrowHandle.prototype\n *\n */\nArrowHandle.prototype.removeEvents = function () {\n\tif ( _viewer ) {\n\t\tvar inputManager = _viewer.getInputManager();\n\t\tinputManager.removeEventListener( PLMVisWeb.InputManager.EventTypes.Down, _mouseDownHandler );\n\t\tinputManager.removeEventListener( PLMVisWeb.InputManager.EventTypes.Up, _mouseUpHandler );\n\t\tinputManager.removeEventListener( PLMVisWeb.InputManager.EventTypes.Move, _mouseMoveHandler );\n\t}\n};\n\nArrowHandle.prototype.mouseMove = function ( event ) {\n};\n\nArrowHandle.prototype.mouseUp = function ( event ) {\n};\n\nArrowHandle.prototype.mouseDown = function ( event ) {\n};\n\n\n/**\n * Repositions the WCS trihedron.\n *\n * @function setPosition\n * @memberof ArrowHandle.prototype\n *\n * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.\n * @param {Number} y - vertical position in pixels from the bottom left of the viewport.\n */\nArrowHandle.prototype.setPosition = function ( x, y ) {\n\t_positionX = x;\n\t_positionY = y;\n\tthis.position.x = x;\n\tthis.position.y = y;\n};\n\n/**\n * Queries the position of the WCS\n *\n * @function getPosition\n * @memberof ArrowHandle.prototype\n *\n * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.\n */\nArrowHandle.prototype.getPosition = function () {\n\treturn this.position;\n};\n\n/**\n * Sets the size of the arrow.\n *\n * @function setSize\n * @memberof ArrowHandle.prototype\n *\n * @param {Number} w - a number used to set the width of the WCS render area.\n * @param {Number} h - a number used to set the height of the WCS render area.\n */\nArrowHandle.prototype.setSize = function ( w, h ) {\n\t_width = w;\n\t_height = h;\n\tthis.size.width = w;\n\tthis.size.height = h;\n};\n\n/**\n * Queries the size of the WCS.\n *\n * @function getSize\n * @memberof ArrowHandle.prototype\n *\n * @returns {Object} an object that contains the height and width (each as a number) of the WCS render area.\n */\nArrowHandle.prototype.getSize = function () {\n\treturn this.size;\n};\n\n/**\n * Sets whether the WCS is rendered on top of or behind the scene geometry.\n *\n * @function setOnTop\n * @memberof ArrowHandle.prototype\n *\n * @params {Boolean} onTop - true if on top, false if behind.\n */\nArrowHandle.prototype.setOnTop = function ( onTop ) {\n\tif ( this.onTop !== onTop || this.onTop === undefined ) {\n\t\tthis.onTop = onTop;\n\t\tif ( onTop ) {\n\t\t\tthis.front = true;\n\t\t\tthis.back = false;\n\t\t}\n\t\telse {\n\t\t\tthis.back = true;\n\t\t\tthis.front = false;\n\t\t}\n\t\tif ( _viewer ) {\n\t\t\t_viewer.renderOrderSGO( this );\n\t\t}\n\t}\n};\n\n/**\n * Queries whether the arrow handle is rendered on top of or behind the scene geometry.\n *\n * @function getOnTop\n * @memberof ArrowHandle.prototype\n *\n * @returns {Boolean} true if on top, false if behind.\n */\nArrowHandle.prototype.getOnTop = function () {\n\treturn this.onTop;\n};\n\n/**\n * Set the arrow handle direction.\n *\n * @function setDirection\n * @memberof ArrowHandle.prototype\n *\n * @param {Vector3} the intial vector (0, 1, 0 ) to toVector\n */\nArrowHandle.prototype.setDirection = function ( x, y, z ) {\n\tvar toVector = new THREE.Vector3( x, y, z );\n\t_object.quaternion.setFromUnitVectors( new THREE.Vector3( 0, 1, 0 ), toVector );\n\t_object.updateMatrix();\n\t_object.updateMatrixWorld();\n\n\tif ( _viewer ) {\n\t\t_viewer.draw();\n\t}\n};\n\n/**\n * Get the intersect object.\n *\n * @function intersect\n * @memberof ArrowHandle.prototype\n *\n * @param {Number} the click mouse poistion x\n * @param {Number} the click mouse poistion y\n */\nArrowHandle.prototype.intersect = function ( clientX, clientY ) {\n\tvar coordinateVector = new THREE.Vector3();\n\tcoordinateVector.set(\n\t\t2 * ( ( clientX - ( _positionX - _width / 2 ) ) / _width ) - 1,\n\t\t1 - 2 * ( ( clientY - ( _positionY - _height / 2 ) ) / _height ),\n\t\t1 );\n\n\tvar raycaster = new THREE.Raycaster();\n\traycaster.setFromCamera( coordinateVector, _camera );\n\tvar intersects = raycaster.intersectObject( _object, true );\n\tvar intersect;\n\tif ( intersects.length > 0 ) {\n\t\tintersect = intersects[ 0 ];\n\t}\n\treturn intersect;\n};\n\n/* harmony default export */ __webpack_exports__[\"default\"] = (ArrowHandle);\n\n\n//# sourceURL=webpack://SectionHandle/./js/SGO/ArrowHandle.module_temp.js?");

/***/ }),

/***/ "./js/SGO/SectionHandle.module_temp.js":
/*!*********************************************!*\
  !*** ./js/SGO/SectionHandle.module_temp.js ***!
  \*********************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony import */ var _ArrowHandle_module_temp__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./ArrowHandle.module_temp */ \"./js/SGO/ArrowHandle.module_temp.js\");\n//© 2019 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC\n\n\n\n\nvar _manager = null,\n\t_mouseDown = false,\n\t_savedCaps = false,\n\t_savedEdges = false,\n\t_intersectPoint = null,\n\t_sectionPlaneChangeHandle = null;\n\n/**\n * @class SectionHandle\n * @classdesc Represents the arrow with handle.<br><br>\n */\nfunction SectionHandle ( arrowColor, handleColor ) { //NOSONAR\n\t_ArrowHandle_module_temp__WEBPACK_IMPORTED_MODULE_0__[\"default\"].call( this, arrowColor, handleColor );\n\tthis.type = \"SectionHandle\";\n\tthis.name = \"SectionHandle\";\n}\n\nSectionHandle.prototype = Object.create( _ArrowHandle_module_temp__WEBPACK_IMPORTED_MODULE_0__[\"default\"].prototype );\nSectionHandle.prototype.constructor = SectionHandle;\n\nfunction _getViewCamera ( viewer ) {\n\tvar camInfo = viewer.getCameraInfo();\n\tif ( viewer.getCameraMode() === PLMVisWeb.CameraMode.PERSPECTIVE ) {\n\t\tvar pCamera = new THREE.PerspectiveCamera( camInfo.perspective.fov, camInfo.perspective.aspect, camInfo.perspective.near, camInfo.perspective.far );\n\t\tpCamera.position.fromArray( camInfo.perspective.pos );\n\t\tpCamera.up.fromArray( camInfo.perspective.up );\n\t\tpCamera.lookAt( new THREE.Vector3().fromArray( camInfo.perspective.tgt ) );\n\t\tpCamera.updateMatrixWorld();\n\t\treturn pCamera;\n\t}\n\telse {\n\t\tvar oCamera = new THREE.OrthographicCamera( camInfo.orthographic.left, camInfo.orthographic.right, camInfo.orthographic.top,\n\t\t\tcamInfo.orthographic.bottom, camInfo.orthographic.near, camInfo.orthographic.left );\n\t\toCamera.position.fromArray( camInfo.orthographic.pos );\n\t\toCamera.up.fromArray( camInfo.orthographic.up );\n\t\toCamera.lookAt( new THREE.Vector3().fromArray( camInfo.orthographic.tgt ) );\n\t\toCamera.updateMatrixWorld();\n\t\treturn oCamera;\n\t}\n}\n\nfunction _setHandlePosition ( handle, planeId ) {\n\tif ( handle.viewer && planeId !== \"NONE\" ) {\n\t\tvar sectionPosition = _manager.getSectionPosition( planeId );\n\n\t\tif ( sectionPosition ) {\n\t\t\tvar planeVector = new THREE.Vector3( sectionPosition[ 0 ], sectionPosition[ 1 ], sectionPosition[ 2 ] );\n\t\t\tvar camera = _getViewCamera( handle.viewer );\n\t\t\tvar vector = planeVector.project( camera );\n\t\t\tvar dim = handle.viewer.control.getSize();\n\t\t\tvar halfWidth = dim.width / 2,\n\t\t\t\thalfHeight = dim.height / 2;\n\n\t\t\thandle.setPosition( Math.round( vector.x * halfWidth + halfWidth ), Math.round( -vector.y * halfHeight + halfHeight ) );\n\t\t}\n\t}\n};\n\nObject.defineProperties( SectionHandle.prototype, {\n\tmanager: {\n\t\tset: function ( manager ) {\n\t\t\t_manager = manager;\n\t\t\tif ( _manager ) {\n\t\t\t\t_sectionPlaneChangeHandle = function ( obj ) {\n\t\t\t\t\tvar sectionPlaneID = obj.selectedPlane;\n\t\t\t\t\tif ( sectionPlaneID !== \"NONE\" ) {\n\t\t\t\t\t\tvar planeId = _manager.getSelectedPlane();\n\t\t\t\t\t\t_setHandlePosition( this, planeId );\n\t\t\t\t\t\tvar sectionDirection = _manager.getSectionDirection( planeId );\n\t\t\t\t\t\tvar sectionSide = _manager.getSectionSide( planeId );\n\t\t\t\t\t\tif ( sectionSide === PLMVisWeb.SectionSide.NEGATIVE ) {\n\t\t\t\t\t\t\tsectionDirection[ 0 ] = -sectionDirection[ 0 ];\n\t\t\t\t\t\t\tsectionDirection[ 1 ] = -sectionDirection[ 1 ];\n\t\t\t\t\t\t\tsectionDirection[ 2 ] = -sectionDirection[ 2 ];\n\t\t\t\t\t\t}\n\t\t\t\t\t\tthis.setDirection( sectionDirection[ 0 ], sectionDirection[ 1 ], sectionDirection[ 2 ] );\n\t\t\t\t\t\tthis.visible = true;\n\t\t\t\t\t}\n\t\t\t\t\telse {\n\t\t\t\t\t\tthis.visible = false;\n\t\t\t\t\t}\n\t\t\t\t}.bind( this );\n\t\t\t\t_manager.registerPlaneSelectionEvent( _sectionPlaneChangeHandle );\n\t\t\t}\n\t\t}\n\t}\n} );\n\nSectionHandle.prototype.render = function ( renderer, camInfo ) {\n\tif ( _manager ) {\n\t\t_setHandlePosition( this, _manager.getSelectedPlane() );\n\t}\n\n\t_ArrowHandle_module_temp__WEBPACK_IMPORTED_MODULE_0__[\"default\"].prototype.render.apply( this, arguments );\n};\n\n/**\n * Removes attached events\n *\n * @function removeEvents\n * @memberof SectionHandle.prototype\n *\n */\nSectionHandle.prototype.removeEvents = function () {\n\tif ( _manager ) {\n\t\t_manager.unregisterPlaneSelectionEvent( _sectionPlaneChangeHandle );\n\t}\n\n\t_ArrowHandle_module_temp__WEBPACK_IMPORTED_MODULE_0__[\"default\"].prototype.removeEvents.apply( this, arguments );\n};\n\nSectionHandle.prototype.mouseMove = function ( event ) {\n\tif ( _mouseDown && this.viewer && _manager ) {\n\t\tthis.viewer.startRenderLoop();\n\n\t\tvar camera = _getViewCamera( this.viewer );\n\t\tvar viewerPlace = this.viewer.domElement;\n\n\t\t// Need to check event for touch or mouse.\n\t\tvar ptX, ptY;\n\t\tif ( event.touches && event.touches.length > 0 ) {\n\t\t\tptX = event.touches[ 0 ].pageX;\n\t\t\tptY = event.touches[ 0 ].pageY;\n\t\t}\n\t\telse {\n\t\t\tptX = event.offsetX;\n\t\t\tptY = event.offsetY;\n\t\t}\n\n\t\tvar pX = ( ptX / viewerPlace.width ) * 2 - 1;\n\t\tvar pY = - ( ptY / viewerPlace.height ) * 2 + 1;\n\n\t\tvar mouseVector = new THREE.Vector3( pX, pY, 1 );\n\t\tmouseVector.unproject( camera );\n\t\tvar raycaster = new THREE.Raycaster( camera.position, mouseVector.sub( camera.position ).normalize() );\n\t\tvar dragDist = new THREE.Vector3();\n\t\traycaster.ray.closestPointToPoint( _intersectPoint, dragDist );\n\t\tdragDist.subVectors( dragDist, _intersectPoint );\n\t\tvar target = this.viewer.getActiveCameraControl().target;\n\t\tvar cameraVector = new THREE.Vector3().subVectors( target, camera.position );\n\t\tvar selectedPlaneId = _manager.getSelectedPlane();\n\t\tvar sectionD = _manager.getSectionDirection( selectedPlaneId );\n\t\tvar sectionP = _manager.getSectionPosition( selectedPlaneId );\n\t\tvar sectionDirection = new THREE.Vector3( sectionD[ 0 ], sectionD[ 1 ], sectionD[ 2 ] );\n\t\tvar sectionPosition = new THREE.Vector3( sectionP[ 0 ], sectionP[ 1 ], sectionP[ 2 ] );\n\t\tvar rads = cameraVector.angleTo( sectionDirection );\n\t\tif ( rads < Math.PI * 0.10 || rads > Math.PI * 0.90 ) {\n\t\t\treturn; \t// Angle too severe, drag unpredictable\n\t\t}\n\n\t\t// Distance to section plane\n\t\tdragDist.projectOnVector( sectionDirection );\n\t\tvar newPos = new THREE.Vector3().copy( sectionPosition ).add( dragDist );\n\t\t_manager.setSectionPosition( selectedPlaneId, newPos.x, newPos.y, newPos.z, false );\n\n\t\t_intersectPoint.add( dragDist );\n\t}\n};\n\nSectionHandle.prototype.mouseUp = function ( event ) {\n\tif ( _mouseDown ) {\n\t\t// restore saved cap/edge states:\n\t\tthis.viewer.setSectionCaps( _savedCaps );\n\t\tthis.viewer.setSectionEdges( _savedEdges );\n\n\t\tthis.viewer.stopRenderLoop();\n\t\t_mouseDown = false;\n\t}\n};\n\nSectionHandle.prototype.mouseDown = function ( event ) {\n\n\t// Need to check event for touch or mouse.\n\tvar ptX, ptY;\n\tif ( event.touches && event.touches.length > 0 ) {\n\t\tptX = event.touches[ 0 ].pageX;\n\t\tptY = event.touches[ 0 ].pageY;\n\t}\n\telse {\n\t\tptX = event.offsetX;\n\t\tptY = event.offsetY;\n\t}\n\n\tif ( _manager && this.intersect( ptX, ptY ) ) {\n\t\tevent.override = true;\n\t\t_mouseDown = true;\n\n\t\t// save off cap/edge state and disable for drag:\n\t\t_savedCaps = this.viewer.getSectionCaps();\n\t\t_savedEdges = this.viewer.getSectionEdges();\n\t\tthis.viewer.setSectionCaps( false );\n\t\tthis.viewer.setSectionEdges( false );\n\n\t\tvar dim = this.viewer.control.getSize();\n\t\tvar pX = ( ptX / dim.width ) * 2 - 1;\n\t\tvar pY = - ( ptY / dim.height ) * 2 + 1;\n\t\t_intersectPoint = new THREE.Vector3( pX, pY, 1 ).unproject( _getViewCamera( this.viewer ) );\n\t}\n};\n\n/* harmony default export */ __webpack_exports__[\"default\"] = (SectionHandle);\n\n\n//# sourceURL=webpack://SectionHandle/./js/SGO/SectionHandle.module_temp.js?");

/***/ })

/******/ })["default"];
});