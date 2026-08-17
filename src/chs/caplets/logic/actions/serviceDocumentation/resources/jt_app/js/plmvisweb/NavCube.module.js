//© 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC

import { InputManager, THREE } from "@com.siemens.plm.web/PLMVisWeb";

/**
 * @class Navigation Cube
 * @classdesc Represents the Navigation Cube for the PLMVisWeb Viewer.<br><br>
 */

var NavCube = function () { //NOSONAR

	// Private variables
	var _object = null,
		_scene = null,
		_camera = null,
		_viewer = null,

		// Default variables to set
		_width = 200,
		_height = 200,
		_positionX = 1650,
		_positionY = 650,

		// Event variables
		_mouseDownHandler = null,
		_mouseUpHandler = null,
		_mouseMoveHandler = null,
		_resizeHandler = null,

		// User-defined variables
		_dirty = false,
		_mouseDown = false;

	var _NavCube = function () {


		_createObject();
		_createScene();
		_createEventVariables.call( this );

		this.name = "NavCube";
		this.original = _object;
		this.scene = _scene;
		this.camera = _camera;
		this.size = { width: _width, height: _height };
		this.position = { x: _positionX, y: _positionY };
		this.boundingBox = new THREE.Box3().setFromObject( _object );
		this.boundingSphere = new THREE.Sphere();
		this.boundingBox.getBoundingSphere( this.boundingSphere );
		this.visible = true;
		this.onTop = true;

		this.front = true;
		this.original.userData.ignoreRenderMode = true;
	};

	_NavCube.prototype = {
		constructor: _NavCube
	};

	Object.defineProperties( _NavCube.prototype, {

		camera: {
			get: function () {
				return _camera;
			},

			set: function ( camera ) {
				_camera = camera;
			}
		},

		scene: {
			get: function () {
				return _scene;
			},

			set: function ( scene ) {
				_scene = scene;
			}
		},

		viewer: {
			set: function ( viewer ) {
				_viewer = viewer;

				if ( _viewer ) {
					var dim = _viewer.control.getSize();
					this.setPosition( dim.width - ( this.size.width + 50 ), dim.height - ( this.size.height + 50 ) );
					_viewer.draw();
					var inputManager = _viewer.getInputManager();
					if ( inputManager ) {
						inputManager.addEventListener( InputManager.EventTypes.Down, _mouseDownHandler, 0 );
						inputManager.addEventListener( InputManager.EventTypes.Up, _mouseUpHandler, 0 );
						inputManager.addEventListener( InputManager.EventTypes.Move, _mouseMoveHandler, 0 );
					}
					_viewer.addEventListener( "resize", _resizeHandler );
				}
			}
		},

		visible: {
			get: function () {
				return _object.visible;
			},

			set: function ( value ) {
				_object.visible = value;
				if ( _viewer ) {
					_viewer.draw();
				}
			}
		}
	} );

	function _createObject () {

		var geo = new THREE.BufferGeometry();

		var vertices = new Float32Array( [

			// Faces
			3.0, 2.0, -2.0, 3.0, 2.0, 2.0, 3.0, -2.0, 2.0, 3.0, -2.0, 2.0, 3.0, -2.0, -2.0, 3.0, 2.0, -2.0,	// X+ Middle
			2.0, 3.0, -2.0, -2.0, 3.0, -2.0, -2.0, 3.0, 2.0, -2.0, 3.0, 2.0, 2.0, 3.0, 2.0, 2.0, 3.0, -2.0,	// Y+ Middle
			2.0, 2.0, 3.0, -2.0, 2.0, 3.0, -2.0, -2.0, 3.0, -2.0, -2.0, 3.0, 2.0, -2.0, 3.0, 2.0, 2.0, 3.0,	// Z+ Middle
			-3.0, 2.0, 2.0, -3.0, 2.0, -2.0, -3.0, -2.0, -2.0, -3.0, -2.0, -2.0, -3.0, -2.0, 2.0, -3.0, 2.0, 2.0,	// X- Middle
			2.0, -3.0, 2.0, -2.0, -3.0, 2.0, -2.0, -3.0, -2.0, -2.0, -3.0, -2.0, 2.0, -3.0, -2.0, 2.0, -3.0, 2.0,	// Y- Middle
			-2.0, 2.0, -3.0, 2.0, 2.0, -3.0, 2.0, -2.0, -3.0, 2.0, -2.0, -3.0, -2.0, -2.0, -3.0, -2.0, 2.0, -3.0,	// Z- Middle

			// Edges
			3.0, 3.0, -2.0, 3.0, 3.0, 2.0, 3.0, 2.0, 2.0, 3.0, 2.0, 2.0, 3.0, 2.0, -2.0, 3.0, 3.0, -2.0,	// X+ Top
			3.0, 3.0, -2.0, 2.0, 3.0, -2.0, 2.0, 3.0, 2.0, 2.0, 3.0, 2.0, 3.0, 3.0, 2.0, 3.0, 3.0, -2.0,	// Y+ Right

			3.0, 2.0, 2.0, 3.0, 2.0, 3.0, 3.0, -2.0, 3.0, 3.0, -2.0, 3.0, 3.0, -2.0, 2.0, 3.0, 2.0, 2.0,	// X+ Left
			3.0, 2.0, 3.0, 2.0, 2.0, 3.0, 2.0, -2.0, 3.0, 2.0, -2.0, 3.0, 3.0, -2.0, 3.0, 3.0, 2.0, 3.0,	// Z+ Right

			3.0, 2.0, -3.0, 3.0, 2.0, -2.0, 3.0, -2.0, -2.0, 3.0, -2.0, -2.0, 3.0, -2.0, -3.0, 3.0, 2.0, -3.0,	// X+ Right
			2.0, 2.0, -3.0, 3.0, 2.0, -3.0, 3.0, -2.0, -3.0, 3.0, -2.0, -3.0, 2.0, -2.0, -3.0, 2.0, 2.0, -3.0,	// Z- Left

			3.0, -2.0, -2.0, 3.0, -2.0, 2.0, 3.0, -3.0, 2.0, 3.0, -3.0, 2.0, 3.0, -3.0, -2.0, 3.0, -2.0, -2.0,	// X+ Bottom
			3.0, -3.0, 2.0, 2.0, -3.0, 2.0, 2.0, -3.0, -2.0, 2.0, -3.0, -2.0, 3.0, -3.0, -2.0, 3.0, -3.0, 2.0,	// Y- Right

			2.0, 3.0, -3.0, -2.0, 3.0, -3.0, -2.0, 3.0, -2.0, -2.0, 3.0, -2.0, 2.0, 3.0, -2.0, 2.0, 3.0, -3.0,	// Y+ Top
			-2.0, 3.0, -3.0, 2.0, 3.0, -3.0, 2.0, 2.0, -3.0, 2.0, 2.0, -3.0, -2.0, 2.0, -3.0, -2.0, 3.0, -3.0,	// Z- Top

			2.0, 3.0, 2.0, -2.0, 3.0, 2.0, -2.0, 3.0, 3.0, -2.0, 3.0, 3.0, 2.0, 3.0, 3.0, 2.0, 3.0, 2.0,	// Y+ Bottom
			2.0, 3.0, 3.0, -2.0, 3.0, 3.0, -2.0, 2.0, 3.0, -2.0, 2.0, 3.0, 2.0, 2.0, 3.0, 2.0, 3.0, 3.0,	// Z+ Top

			-3.0, 3.0, 2.0, -3.0, 3.0, -2.0, -3.0, 2.0, -2.0, -3.0, 2.0, -2.0, -3.0, 2.0, 2.0, -3.0, 3.0, 2.0,	// X- Top
			-2.0, 3.0, -2.0, -3.0, 3.0, -2.0, -3.0, 3.0, 2.0, -3.0, 3.0, 2.0, -2.0, 3.0, 2.0, -2.0, 3.0, -2.0,	// Y+ Left

			-3.0, 2.0, -2.0, -3.0, 2.0, -3.0, -3.0, -2.0, -3.0, -3.0, -2.0, -3.0, -3.0, -2.0, -2.0, -3.0, 2.0, -2.0,	// X- Left
			-3.0, 2.0, -3.0, -2.0, 2.0, -3.0, -2.0, -2.0, -3.0, -2.0, -2.0, -3.0, -3.0, -2.0, -3.0, -3.0, 2.0, -3.0,	// Z- Right

			-3.0, 2.0, 3.0, -3.0, 2.0, 2.0, -3.0, -2.0, 2.0, -3.0, -2.0, 2.0, -3.0, -2.0, 3.0, -3.0, 2.0, 3.0,	// X- Right
			-2.0, 2.0, 3.0, -3.0, 2.0, 3.0, -3.0, -2.0, 3.0, -3.0, -2.0, 3.0, -2.0, -2.0, 3.0, -2.0, 2.0, 3.0,	// Z+ Left

			-3.0, -2.0, 2.0, -3.0, -2.0, -2.0, -3.0, -3.0, -2.0, -3.0, -3.0, -2.0, -3.0, -3.0, 2.0, -3.0, -2.0, 2.0,	// X- Bottom
			-2.0, -3.0, 2.0, -3.0, -3.0, 2.0, -3.0, -3.0, -2.0, -3.0, -3.0, -2.0, -2.0, -3.0, -2.0, -2.0, -3.0, 2.0,	// Y- Left

			2.0, -3.0, 3.0, -2.0, -3.0, 3.0, -2.0, -3.0, 2.0, -2.0, -3.0, 2.0, 2.0, -3.0, 2.0, 2.0, -3.0, 3.0,	// Y- Top
			2.0, -2.0, 3.0, -2.0, -2.0, 3.0, -2.0, -3.0, 3.0, -2.0, -3.0, 3.0, 2.0, -3.0, 3.0, 2.0, -2.0, 3.0,	// Z+ Bottom

			2.0, -3.0, -2.0, -2.0, -3.0, -2.0, -2.0, -3.0, -3.0, -2.0, -3.0, -3.0, 2.0, -3.0, -3.0, 2.0, -3.0, -2.0,	// Y- Bottom
			-2.0, -2.0, -3.0, 2.0, -2.0, -3.0, 2.0, -3.0, -3.0, 2.0, -3.0, -3.0, -2.0, -3.0, -3.0, -2.0, -2.0, -3.0,	// Z- Bottom

			// Corners
			3.0, 3.0, 2.0, 3.0, 3.0, 3.0, 3.0, 2.0, 3.0, 3.0, 2.0, 3.0, 3.0, 2.0, 2.0, 3.0, 3.0, 2.0,	// X+ Top Left
			3.0, 3.0, 2.0, 2.0, 3.0, 2.0, 2.0, 3.0, 3.0, 2.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 2.0,	// Y+ Bottom Right
			3.0, 3.0, 3.0, 2.0, 3.0, 3.0, 2.0, 2.0, 3.0, 2.0, 2.0, 3.0, 3.0, 2.0, 3.0, 3.0, 3.0, 3.0,	// Z+ Top Right

			3.0, 3.0, -3.0, 3.0, 3.0, -2.0, 3.0, 2.0, -2.0, 3.0, 2.0, -2.0, 3.0, 2.0, -3.0, 3.0, 3.0, -3.0,	// X+ Top Right
			3.0, 3.0, -3.0, 2.0, 3.0, -3.0, 2.0, 3.0, -2.0, 2.0, 3.0, -2.0, 3.0, 3.0, -2.0, 3.0, 3.0, -3.0,	// Y+ Top Right
			2.0, 3.0, -3.0, 3.0, 3.0, -3.0, 3.0, 2.0, -3.0, 3.0, 2.0, -3.0, 2.0, 2.0, -3.0, 2.0, 3.0, -3.0,	// Z- Top Left

			3.0, -2.0, 2.0, 3.0, -2.0, 3.0, 3.0, -3.0, 3.0, 3.0, -3.0, 3.0, 3.0, -3.0, 2.0, 3.0, -2.0, 2.0,	// X+ Bottom Left
			3.0, -3.0, 3.0, 2.0, -3.0, 3.0, 2.0, -3.0, 2.0, 2.0, -3.0, 2.0, 3.0, -3.0, 2.0, 3.0, -3.0, 3.0,	// Y- Top Right
			3.0, -2.0, 3.0, 2.0, -2.0, 3.0, 2.0, -3.0, 3.0, 2.0, -3.0, 3.0, 3.0, -3.0, 3.0, 3.0, -2.0, 3.0,	// Z+ Bottom Right

			3.0, -2.0, -3.0, 3.0, -2.0, -2.0, 3.0, -3.0, -2.0, 3.0, -3.0, -2.0, 3.0, -3.0, -3.0, 3.0, -2.0, -3.0,	// X+ Bottom Right
			3.0, -3.0, -2.0, 2.0, -3.0, -2.0, 2.0, -3.0, -3.0, 2.0, -3.0, -3.0, 3.0, -3.0, -3.0, 3.0, -3.0, -2.0,	// Y- Bottom Right
			2.0, -2.0, -3.0, 3.0, -2.0, -3.0, 3.0, -3.0, -3.0, 3.0, -3.0, -3.0, 2.0, -3.0, -3.0, 2.0, -2.0, -3.0,	// Z- Bottom Left

			-3.0, 3.0, -2.0, -3.0, 3.0, -3.0, -3.0, 2.0, -3.0, -3.0, 2.0, -3.0, -3.0, 2.0, -2.0, -3.0, 3.0, -2.0,	// X- Top Left
			-2.0, 3.0, -3.0, -3.0, 3.0, -3.0, -3.0, 3.0, -2.0, -3.0, 3.0, -2.0, -2.0, 3.0, -2.0, -2.0, 3.0, -3.0,	// Y+ Top Left
			-3.0, 3.0, -3.0, -2.0, 3.0, -3.0, -2.0, 2.0, -3.0, -2.0, 2.0, -3.0, -3.0, 2.0, -3.0, -3.0, 3.0, -3.0,	// Z- Top Right

			-3.0, 3.0, 3.0, -3.0, 3.0, 2.0, -3.0, 2.0, 2.0, -3.0, 2.0, 2.0, -3.0, 2.0, 3.0, -3.0, 3.0, 3.0,	// X- Top Right
			-2.0, 3.0, 2.0, -3.0, 3.0, 2.0, -3.0, 3.0, 3.0, -3.0, 3.0, 3.0, -2.0, 3.0, 3.0, -2.0, 3.0, 2.0,	// Y+ Bottom Left
			-2.0, 3.0, 3.0, -3.0, 3.0, 3.0, -3.0, 2.0, 3.0, -3.0, 2.0, 3.0, -2.0, 2.0, 3.0, -2.0, 3.0, 3.0,	// Z+ Top Left

			-3.0, -2.0, -2.0, -3.0, -2.0, -3.0, -3.0, -3.0, -3.0, -3.0, -3.0, -3.0, -3.0, -3.0, -2.0, -3.0, -2.0, -2.0,	// X- Bottom Left
			-2.0, -3.0, -2.0, -3.0, -3.0, -2.0, -3.0, -3.0, -3.0, -3.0, -3.0, -3.0, -2.0, -3.0, -3.0, -2.0, -3.0, -2.0,	// Y- Bottom Left
			-3.0, -2.0, -3.0, -2.0, -2.0, -3.0, -2.0, -3.0, -3.0, -2.0, -3.0, -3.0, -3.0, -3.0, -3.0, -3.0, -2.0, -3.0,	// Z- Bottom Right

			-3.0, -2.0, 3.0, -3.0, -2.0, 2.0, -3.0, -3.0, 2.0, -3.0, -3.0, 2.0, -3.0, -3.0, 3.0, -3.0, -2.0, 3.0,	// X- Bottom Right
			-2.0, -3.0, 3.0, -3.0, -3.0, 3.0, -3.0, -3.0, 2.0, -3.0, -3.0, 2.0, -2.0, -3.0, 2.0, -2.0, -3.0, 3.0,	// Y- Top Left
			-2.0, -2.0, 3.0, -3.0, -2.0, 3.0, -3.0, -3.0, 3.0, -3.0, -3.0, 3.0, -2.0, -3.0, 3.0, -2.0, -2.0, 3.0	// Z+ Bottom Left

		] );

		var uvs = new Float32Array( [
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0,
			1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0
		] );

		geo.addAttribute( 'position', new THREE.BufferAttribute( vertices, 3 ) );
		geo.addAttribute( 'uv', new THREE.BufferAttribute( uvs, 2 ) );

		var faceGroups = [];
		var edgeGroups = [];
		var cornerGroups = [];

		for ( var i = 0; i < 26; i++ ) {
			var curGroup;
			var start;
			if ( i < 6 ) {
				start = i * 6;
				geo.addGroup( start, 6, 0 );
				curGroup = geo.groups[ i ];
				curGroup.isFace = true;
				faceGroups.push( curGroup );
				curGroup.face = determineFace( start, vertices );
			}
			else if ( i >= 6 && i < 18 ) {
				start = 36 + ( i - 6 ) * 12;
				geo.addGroup( start, 12, 0 );
				curGroup = geo.groups[ i ];
				curGroup.isEdge = true;
				edgeGroups.push( curGroup );
				curGroup.face = determineFace( start, vertices ) + determineFace( start + 6, vertices );
			}
			else if ( i >= 18 ) {
				start = 180 + ( i - 18 ) * 18;
				geo.addGroup( start, 18, 0 );
				curGroup = geo.groups[ i ];
				curGroup.isCorner = true;
				cornerGroups.push( curGroup );
				curGroup.face = determineFace( start, vertices ) + determineFace( start + 6, vertices ) + determineFace( start + 12, vertices );
			}

			curGroup.view = [];

			// Determine Camera X pos
			if ( curGroup.face.indexOf( "X+" ) !== -1 ) {
				curGroup.view.push( 1 );
			}
			else if ( curGroup.face.indexOf( "X-" ) !== -1 ) {
				curGroup.view.push( -1 );
			}
			else {
				curGroup.view.push( 0 );
			}

			// Determine Camera Y pos
			if ( curGroup.face.indexOf( "Y+" ) !== -1 ) {
				curGroup.view.push( 1 );
			}
			else if ( curGroup.face.indexOf( "Y-" ) !== -1 ) {
				curGroup.view.push( -1 );
			}
			else {
				curGroup.view.push( 0 );
			}

			// Determine Camera Z pos
			if ( curGroup.face.indexOf( "Z+" ) !== -1 ) {
				curGroup.view.push( 1 );
			}
			else if ( curGroup.face.indexOf( "Z-" ) !== -1 ) {
				curGroup.view.push( -1 );
			}
			else {
				curGroup.view.push( 0 );
			}
		}

		var materials = [
			new THREE.MeshBasicMaterial( { color: new THREE.Color( "#ffffff" ), polygonOffset: true, polygonOffsetFactor: 1 } ),
			new THREE.MeshBasicMaterial( { color: new THREE.Color( "#8cc3d2" ), polygonOffset: true, polygonOffsetFactor: 1 } ),
			new THREE.MeshBasicMaterial( { color: new THREE.Color( "#7f00ff" ), polygonOffset: true, polygonOffsetFactor: 1 } )
		];

		var cubeMesh = new THREE.Mesh( geo, materials );

		cubeMesh.faceGroups = faceGroups;
		cubeMesh.edgeGroups = edgeGroups;
		cubeMesh.cornerGroups = cornerGroups;

		cubeMesh.scale.set( 8.333, 8.333, 8.333 );

		var edges = new THREE.EdgesGeometry( cubeMesh.geometry );
		var outline = new THREE.LineSegments( edges, new THREE.LineBasicMaterial( { color: "#000000" } ) );
		cubeMesh.add( outline );

		loadDefaultCubeTextures( cubeMesh );

		_object = cubeMesh;
	}

	function _createScene () {

		_scene = new THREE.Scene();
		_camera = new THREE.OrthographicCamera( _width * -0.25, _width * 0.25, _height * 0.25, _height * -0.25, -100, 100 );

		_scene.add( _camera );
		_scene.add( _object );
	}

	function _createEventVariables () {

		_resizeHandler = function resize ( resizeEvent ) {
			this.setPosition( resizeEvent.width - ( this.size.width + 50 ), resizeEvent.height - ( this.size.height + 50 ) );
			_viewer.draw();
		}.bind( this );

		_mouseDownHandler = this.mouseDown.bind( this );
		_mouseUpHandler = this.mouseUp.bind( this );
		_mouseMoveHandler = this.mouseMove.bind( this );
	}

	function determineFace ( start, vertices ) {

		var i = start * 3;

		if ( vertices[ i ] === vertices[ i + 3 ] && vertices[ i ] === vertices[ i + 6 ] ) {
			if ( vertices[ i ] > 0 ) {
				return "X+";
			}
			else {
				return "X-";
			}
		}
		else if ( vertices[ i + 1 ] === vertices[ i + 4 ] && vertices[ i + 1 ] === vertices[ i + 7 ] ) {
			if ( vertices[ i + 1 ] > 0 ) {
				return "Y+";
			}
			else {
				return "Y-";
			}
		}
		else if ( vertices[ i + 2 ] === vertices[ i + 5 ] && vertices[ i + 2 ] === vertices[ i + 8 ] ) {
			if ( vertices[ i + 2 ] > 0 ) {
				return "Z+";
			}
			else {
				return "Z-";
			}
		}
	}

	function loadDefaultCubeTextures ( cube ) {

		var materials = cube.material;

		var loader = new THREE.TextureLoader();

		loader.load( 'img/cubeTextures/px.png',
			function ( texture ) {
				materials.push( new THREE.MeshBasicMaterial( { color: "#ffffff", map: texture } ) );
				loader.load( 'img/cubeTextures/py.png',
					function ( texture ) {
						materials.push( new THREE.MeshBasicMaterial( { color: "#ffffff", map: texture } ) );
						loader.load( 'img/cubeTextures/pz.png',
							function ( texture ) {
								materials.push( new THREE.MeshBasicMaterial( { color: "#ffffff", map: texture } ) );
								loader.load( 'img/cubeTextures/nx.png',
									function ( texture ) {
										materials.push( new THREE.MeshBasicMaterial( { color: "#ffffff", map: texture } ) );
										loader.load( 'img/cubeTextures/ny.png',
											function ( texture ) {
												materials.push( new THREE.MeshBasicMaterial( { color: "#ffffff", map: texture } ) );
												loader.load( 'img/cubeTextures/nz.png',
													function ( texture ) {
														materials.push( new THREE.MeshBasicMaterial( { color: "#ffffff", map: texture } ) );

														for ( var i = 0, l = cube.faceGroups.length; i < l; i++ ) {

															cube.faceGroups[ i ].materialIndex = i + 3;
															cube.faceGroups[ i ].matIdx = i + 3;

														}

														if ( _viewer !== null ) {
															_viewer.draw();
														}
													}
												);
											}
										);
									}
								);
							}
						);
					}
				);
			}
		);
	}

	/**
	 * Defines how to render the NavCube
	 *
	 * @function render
	 * @memberof SceneGraphObject.prototype
	 *
	 */
	_NavCube.prototype.render = function ( renderer, camInfo ) {

		renderer.setViewport(
			_positionX,
			renderer.getSize().height - _height - _positionY,
			_width,
			_height
		);

		var camPos = new THREE.Vector3().fromArray( camInfo.perspective.pos );

		var tgt = new THREE.Vector3().fromArray( camInfo.perspective.tgt );
		camPos.sub( tgt );
		camPos.normalize();

		if ( this.boundingSphere ) {
			camPos.setLength( this.boundingSphere.radius );
		}
		else {
			camPos.setLength( 50 );
		}

		_camera.position.copy( camPos );
		_camera.up.fromArray( camInfo.perspective.up );
		_camera.lookAt( _scene.position );

		renderer.render( _scene, _camera );
	};

	/**
	 * Removes attached events
	 *
	 * @function removeEvents
	 * @memberof NavCube.prototype
	 *
	 */
	_NavCube.prototype.removeEvents = function () {
		if ( _viewer ) {
			var inputManager = _viewer.getInputManager();
			inputManager.removeEventListener( InputManager.EventTypes.Down, _mouseDownHandler );
			inputManager.removeEventListener( InputManager.EventTypes.Up, _mouseUpHandler );
			inputManager.removeEventListener( InputManager.EventTypes.Move, _mouseMoveHandler );
			_viewer.removeEventListener( "resize", _resizeHandler );
		}
	};

	/**
	 * Repositions the NavCube.
	 *
	 * @function setPosition
	 * @memberof NavCube.prototype
	 *
	 * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.
	 * @param {Number} y - vertical position in pixels from the bottom left of the viewport.
	 */
	_NavCube.prototype.setPosition = function ( x, y ) {
		_positionX = x;
		_positionY = y;
		this.position.x = x;
		this.position.y = y;

		if ( _viewer !== null ) {
			_viewer.draw();
		}
	};

	/**
	 * Queries the position of the NavCube.
	 *
	 * @function getPosition
	 * @memberof NavCube.prototype
	 *
	 * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.
	 */
	_NavCube.prototype.getPosition = function () {
		return this.position;
	};

	/**
	 * Sets the size of the NavCube.
	 *
	 * @function setSize
	 * @memberof NavCube.prototype
	 *
	 * @param {Number} w - a number used to set the width of the NavCube render area.
	   * @param {Number} h - a number used to set the height of the NavCube render area.
	 */
	_NavCube.prototype.setSize = function ( w, h ) {
		_width = w;
		_height = h;
		this.size.width = w;
		this.size.height = h;
	};

	/**
	 * Queries the size of the NavCube.
	 *
	 * @function getSize
	 * @memberof NavCube.prototype
	 *
	 * @returns {Object} an object that contains the height and width (each as a number) of the NavCube render area.
	 */
	_NavCube.prototype.getSize = function () {
		return this.size;
	};

	/**
	 * Sets whether the NavCube is rendered on top of or behind the scene geometry.
	 *
	 * @function setOnTop
	 * @memberof NavCube.prototype
	 *
	 * @params {Boolean} onTop - true if on top, false if behind.
	 */
	_NavCube.prototype.setOnTop = function ( onTop ) {
		if ( this.onTop !== onTop || this.onTop === undefined ) {
			this.onTop = onTop;
			if ( onTop ) {
				this.front = true;
				this.back = false;
			}
			else {
				this.back = true;
				this.front = false;
			}
			if ( _viewer ) {
				_viewer.renderOrderSGO( this );
			}
		}
	};

	/**
	 * Queries whether the NavCube is rendered on top of or behind the scene geometry.
	 *
	 * @function getOnTop
	 * @memberof NavCube.prototype
	 *
	 * @returns {Boolean} true if on top, false if behind.
	 */
	_NavCube.prototype.getOnTop = function () {
		return this.onTop;
	};

	_NavCube.prototype._isMouseOver = function ( event ) {

		var mouseX = event.offsetX;
		var mouseY = event.offsetY;

		var x1 = _positionX;
		var x2 = _positionX + _width;

		var y1 = _viewer.domElement.height - ( _positionY + _height );
		var y2 = _viewer.domElement.height - _positionY;

		if ( ( x1 <= mouseX && mouseX <= x2 ) && ( y1 <= mouseY && mouseY <= y2 ) ) {
			return true;
		}
		return false;
	};

	function clean () {
		for ( var j = 0, groupLen = _object.geometry.groups.length; j < groupLen; j++ ) {
			var curGroup = _object.geometry.groups[ j ];
			if ( curGroup.matIdx ) {
				curGroup.materialIndex = curGroup.matIdx;
			}
			else {
				curGroup.materialIndex = 0;
			}
		}
		_dirty = false;
	}

	_NavCube.prototype.mouseMove = function ( event ) {
		if ( _dirty ) {
			clean();
		}

		if ( this._isMouseOver( event ) ) {
			event.override = true;
			_viewer.startRenderLoop();

			var mouseX = event.offsetX;
			var mouseY = event.offsetY;

			var mouseVector = new THREE.Vector2();
			var viewerPlace = _viewer.domElement;

			mouseVector.set(
				2 * ( ( mouseX - _positionX ) / _width ) - 1,
				2 * ( ( viewerPlace.offsetHeight - mouseY - _positionY ) / _height ) - 1,
				1 );

			var raycaster = new THREE.Raycaster();
			raycaster.linePrecision = 0.001;
			raycaster.setFromCamera( mouseVector, _camera );

			var intersects = raycaster.intersectObjects( _scene.children, true );

			for ( var i = 0, len = intersects.length; i < len; i++ ) {

				if ( intersects[ i ].face !== null ) {

					var groups = intersects[ i ].object.geometry.groups;
					//value of index was changed in three.Js update to equal the value of faceIndex * 3 (https://github.com/mrdoob/three.js/issues/13894)
					var index = ( 3 * intersects[ i ].faceIndex );

					for ( var j = 0, groupLen = groups.length; j < groupLen; j++ ) {

						if ( groups[ j ].start <= index && index < groups[ j ].start + groups[ j ].count ) {
							// Set moused-over highlighted
							groups[ j ].materialIndex = 1;
							_dirty = true;
						}
					}
					break;
				}
			}
			_viewer.stopRenderLoop();
		}
	};

	_NavCube.prototype.mouseUp = function ( event ) {
		if ( _mouseDown && this._isMouseOver( event ) ) {
			event.override = true;
			_viewer.startRenderLoop();
			clean();

			var mouseX = event.offsetX;
			var mouseY = event.offsetY;

			var mouseVector = new THREE.Vector2();
			var viewerPlace = _viewer.domElement;

			mouseVector.set(
				2 * ( ( mouseX - _positionX ) / _width ) - 1,
				2 * ( ( viewerPlace.offsetHeight - mouseY - _positionY ) / _height ) - 1,
				1 );

			var raycaster = new THREE.Raycaster();
			raycaster.linePrecision = 0.001;
			raycaster.setFromCamera( mouseVector, _camera );

			var intersects = raycaster.intersectObjects( _scene.children, true );

			for ( var i = 0, len = intersects.length; i < len; i++ ) {

				if ( intersects[ i ].face !== null ) {

					var groups = intersects[ i ].object.geometry.groups;
					//value of index was changed in three.Js update to equal the value of faceIndex * 3 (https://github.com/mrdoob/three.js/issues/13894)
					var index = ( 3 * intersects[ i ].faceIndex );

					for ( var j = 0, groupLen = groups.length; j < groupLen; j++ ) {

						if ( groups[ j ].start <= index && index < groups[ j ].start + groups[ j ].count && groups[ j ].view !== null ) {

							groups[ j ].materialIndex = 2;
							_viewer.setCameraPosition( groups[ j ].view );

						}
					}
					break;
				}
			}
			_viewer.stopRenderLoop();
		}
	};

	_NavCube.prototype.mouseDown = function ( event ) {
		if ( this._isMouseOver( event ) ) {
			event.override = true;
			_mouseDown = true;
		}
	};

	return new _NavCube();
};

NavCube.prototype = {
	constructor: NavCube,
	_name: "NavCube"
};

export default NavCube;
