//© 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC

import { InputManager, CameraMode, THREE } from "@com.siemens.plm.web/PLMVisWeb";

/**
 * @class Arrow
 * @classdesc Place an arrow at a mouse-click's position in the direction of the object's face normal.<br><br>
 */

// Private variables
var _object = null,
	_scene = null,
	_camera = null,
	_viewer = undefined,

	// Default variables to set
	_width = 100,
	_height = 100,
	_positionX = 10,
	_positionY = 10,

	// Event variables
	_mouseDownHandler = null,
	_mouseUpHandler = null,
	_mouseMoveHandler = null,
	_resizeHandler = null,

	// User-defined variables
	_pCamera = null,
	_oCamera = null,
	_mouseDown = false,
	_mousePos = [];

function Arrow ( color ) {
	_createObject( color );
	_createScene();
	_createEventVariables.call( this );

	// Public variables
	this.name = "Arrow";
	this.original = _object;
	//this.updateBBox = true; // NOTE: Applicable only for inSceneSGOs.Setting this property to true for the inSceneSGO's updates the global boundingbox to include  sgo's boundingbox
	//Deprecated:  updateBBox will be removed after the 1.7.0 release.  To update the global boundingbox to include sgo's boundingbox, use sgoBBox
	this.scene = _scene;
	this.camera = _camera;
	this.size = { width: _width, height: _height };
	this.position = { x: _positionX, y: _positionY };
	this.boundingBox = new THREE.Box3().setFromObject( _object );
	this.boundingSphere = new THREE.Sphere();
	this.boundingBox.getBoundingSphere( this.boundingSphere );
	this.sgoBBox = this.boundingBox; // NOTE: Applicable for all SGOs that want the global boundingbox to include sgo's boundingbox
	this.visible = true;

	this.original.userData.ignoreRenderMode = true;		// Determines if render modes should effect the SGO
};

Arrow.prototype.constructor = Arrow;

Object.defineProperties( Arrow.prototype, {

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
		get: function () {
			return _viewer;
		},

		set: function ( viewer ) {
			_viewer = viewer;

			if ( _viewer ) {
				this.setPosition( 0, 0 );
				var dim = _viewer.control.getSize();
				this.setSize( dim.width, dim.height );

				var inputManager = _viewer.getInputManager();
				if ( inputManager ) {
					inputManager.addEventListener( InputManager.EventTypes.Down, _mouseDownHandler );
					inputManager.addEventListener( InputManager.EventTypes.Up, _mouseUpHandler );
				}
				window.addEventListener( "resize", _resizeHandler );
			}
		}
	},

	visible: {
		get: function () {
			return _object.visible;
		},

		set: function ( value ) {
			if ( _object.visible !== value ) {
				_object.visible = value;
				if ( _viewer ) {
					_viewer.draw();
				}
			}
		}
	}
} );


function _createObject ( color ) {
	color = ( color === undefined ) ? 0xffff00 : color;
	_object = new THREE.ArrowHelper( new THREE.Vector3(), new THREE.Vector3(), 1, color, 0.2, 0.2 );
}

function _createScene () {
	_scene = new THREE.Scene();
	_pCamera = new THREE.PerspectiveCamera();
	_oCamera = new THREE.OrthographicCamera();
}

function _createEventVariables () {
	_resizeHandler = function resize () {
		var dim = _viewer.control.getSize();
		this.setSize( dim.width, dim.height );
		_viewer.draw();
	}.bind( this );

	_mouseDownHandler = this.mouseDown.bind( this );
	_mouseUpHandler = this.mouseUp.bind( this );
	_mouseMoveHandler = this.mouseMove.bind( this );
}

/**
 * Defines how to render the Arrow
 *
 * @function render
 * @memberof Arrow.prototype
 *
 */
Arrow.prototype.render = function ( renderer, camInfo ) {

	renderer.setViewport(
		_positionX,
		_positionY,
		_width,
		_height
	);

	if ( _viewer.getCameraMode() === CameraMode.PERSPECTIVE ) {

		_pCamera.position.fromArray( camInfo.perspective.pos );
		_pCamera.lookAt( new THREE.Vector3().fromArray( camInfo.perspective.tgt ) );
		_pCamera.up.fromArray( camInfo.perspective.up );
		_pCamera.near = camInfo.perspective.near;
		_pCamera.far = camInfo.perspective.far;
		_pCamera.fov = camInfo.perspective.fov;
		_pCamera.aspect = camInfo.perspective.aspect;
		this.camera = _pCamera;
	}
	else {

		_oCamera.position.fromArray( camInfo.orthographic.pos );
		_oCamera.lookAt( new THREE.Vector3().fromArray( camInfo.orthographic.tgt ) );
		_oCamera.up.fromArray( camInfo.orthographic.up );
		_oCamera.near = camInfo.orthographic.near;
		_oCamera.far = camInfo.orthographic.far;
		_oCamera.left = camInfo.orthographic.left;
		_oCamera.right = camInfo.orthographic.right;
		_oCamera.bottom = camInfo.orthographic.bottom;
		_oCamera.top = camInfo.orthographic.top;
		this.camera = _oCamera;
	}

	this.camera.updateProjectionMatrix();

	renderer.render( _scene, this.camera );
};

/**
 * Removes attached events
 *
 * @function removeEvents
 * @memberof Arrow.prototype
 *
 */
Arrow.prototype.removeEvents = function () {
	if ( _viewer ) {
		var inputManager = _viewer.getInputManager();
		inputManager.removeEventListener( InputManager.EventTypes.Down, _mouseDownHandler );
		inputManager.removeEventListener( InputManager.EventTypes.Up, _mouseUpHandler );
		window.removeEventListener( "resize", _resizeHandler );
	}
};

/**
 * Repositions the SGO.
 *
 * @function setPosition
 * @memberof Arrow.prototype
 *
 * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.
 * @param {Number} y - vertical position in pixels from the bottom left of the viewport.
 */
Arrow.prototype.setPosition = function ( x, y ) {
	_positionX = x;
	_positionY = y;
	this.position.x = x;
	this.position.y = y;
};

/**
 * Queries the position of the SGO
 *
 * @function getPosition
 * @memberof Arrow.prototype
 *
 * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.
 */
Arrow.prototype.getPosition = function () {
	return this.position;
};

/**
 * Sets the size of the SGO.
 *
 * @function setSize
 * @memberof Arrow.prototype
 *
 * @param {Number} value - a single value used to set the height and width of the WCS render area.
 */
Arrow.prototype.setSize = function ( w, h ) {
	_width = w;
	_height = h;
	this.size.width = w;
	this.size.height = h;
};

/**
 * Queries the size of the SGO.
 *
 * @function getSize
 * @memberof Arrow.prototype
 *
 * @returns {Object} a single value used to set the height and width of the WCS render area.
 */
Arrow.prototype.getSize = function () {
	return this.size;
};

/**
 *
 *  Mouse events pass through the viewer to the SGO and can be defined here.
 *
 *  Add 'event.override = true' to ignore the mouse event in the viewer
 *
 */
Arrow.prototype.mouseUp = function ( event ) {
	if ( _mouseDown && ( _mousePos[ 0 ] === event.offsetX || _mousePos[ 1 ] === event.offsetY ) ) {

		var point = _viewer.getModelPointAtViewCoordinate( event.offsetX, event.offsetY );
		if ( point ) {
			point = new THREE.Vector3().fromArray( point );
			if ( _object ) {
				_scene.remove( _object );
			}
			var dir = new THREE.Vector3().fromArray( _viewer.getFaceNormalAtViewCoordinate( event.offsetX, event.offsetY ) ).normalize();
			var origin = new THREE.Vector3( point.x, point.y, point.z );
			var length = _viewer.getVisibleModelBoundingBoxLength() / 25;
			_object.setDirection( dir );
			_object.setLength( length, 0.2 * length, 0.2 * length );
			_object.position.copy( origin );
			_scene.add( _object );
		}
		_viewer.draw();
		_viewer.setPickingEnabled( true );
	}
};

Arrow.prototype.mouseDown = function ( event ) {
	_mouseDown = true;
	_mousePos = [ event.offsetX, event.offsetY ];
	_viewer.setPickingEnabled( false );
};

Arrow.prototype.mouseMove = function () { };

export default Arrow;
