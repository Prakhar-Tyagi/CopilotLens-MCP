//© 2019 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC

import { InputManager, THREE } from "@com.siemens.plm.web/PLMVisWeb";

/**
 * @class Arrow Handle
 * @classdesc Represents the arrow with handle.<br><br>
 */
// Private variables
var _object = null,
	_scene = null,
	_camera = null,
	_viewer = null,

	// Default variables to set
	_width = 300,
	_height = 300,
	_positionX = 10,
	_positionY = 10,

	// Event variables
	_mouseDownHandler = null,
	_mouseUpHandler = null,
	_mouseMoveHandler = null,

	// User-defined variables
	_textNodes = [];

function ArrowHandle ( arrowColor, handleColor ) { //NOSONAR

	_createObject( arrowColor, handleColor );
	_createScene();
	_createEventVariables( this );

	this.name = "ArrowHandle";
	this.original = _object;
	this.scene = _scene;
	this.camera = _camera;
	this.size = { width: _width, height: _height };
	this.position = { x: _positionX, y: _positionY };
	this.boundingBox = new THREE.Box3().setFromObject( _object );
	this.boundingSphere = new THREE.Sphere();
	this.boundingBox.getBoundingSphere( this.boundingSphere );
	this.visible = false;
	this.onTop = true;

	this.front = true;
	this.original.userData.ignoreRenderMode = true;
}

ArrowHandle.prototype.constructor = ArrowHandle;

Object.defineProperties( ArrowHandle.prototype, {

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
				var inputManager = _viewer.getInputManager();
				if ( inputManager ) {
					inputManager.addEventListener( InputManager.EventTypes.Down, _mouseDownHandler, 0 );
					inputManager.addEventListener( InputManager.EventTypes.Up, _mouseUpHandler, 0 );
					inputManager.addEventListener( InputManager.EventTypes.Move, _mouseMoveHandler, 0 );
				}
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

function _createObject ( arrowColor, handleColor ) {
	_object = new THREE.Object3D();

	var arrowMaterial = arrowColor ? new THREE.MeshPhongMaterial( { color: arrowColor } ) : new THREE.MeshPhongMaterial( { color: 0xf3dd5a } );
	var handleMaterial = handleColor ? new THREE.MeshPhongMaterial( { color: handleColor } ) : new THREE.MeshPhongMaterial( { color: 0xdcdcdc } );

	var cylinderGeometry = new THREE.CylinderBufferGeometry( 0.5, 1, 30, 32 );
	var cylinder = new THREE.Mesh( cylinderGeometry, handleMaterial );
	var coneGeometry = new THREE.CylinderBufferGeometry( 2.4, 0, 10, 32 );
	var cone = new THREE.Mesh( coneGeometry, arrowMaterial );
	cylinder.position.y = -15;
	cone.position.y = -35;
	_object.add( cylinder );
	_object.add( cone );
}

function _createScene () {

	_scene = new THREE.Scene();
	_width = 200;
	_height = 200;
	_camera = new THREE.OrthographicCamera( _width * -0.25, _width * 0.25, _height * 0.25, _height * -0.25, -100, 100 );

	var dlight1 = new THREE.DirectionalLight( 0xffffff );
	dlight1.position.set( 0, 0, 0 );
	dlight1.target.position.set( 1, -1, -1 );

	var dlight2 = new THREE.DirectionalLight( 0xffffff );
	dlight2.position.set( 0, 0, 0 );
	dlight2.target.position.set( -1, -1, -1 );

	var ambientLight = new THREE.AmbientLight( 0xffffff );
	var intensity = 0.3;
	ambientLight.color.setRGB( intensity, intensity, intensity );

	_scene.add( _camera );
	_scene.add( _object );
	_scene.add( dlight1 );
	_scene.add( dlight1.target );
	_scene.add( dlight2 );
	_scene.add( dlight2.target );
	_scene.add( ambientLight );
}

function _createEventVariables ( handle ) {
	_mouseDownHandler = handle.mouseDown.bind( handle );
	_mouseUpHandler = handle.mouseUp.bind( handle );
	_mouseMoveHandler = handle.mouseMove.bind( handle );
}

/**
 * Defines how to render the ArrowHandle
 *
 * @function render
 * @memberof ArrowHandle.prototype
 *
 */
ArrowHandle.prototype.render = function ( renderer, camInfo ) {
	renderer.setViewport(
		_positionX - _width / 2,
		_positionY - _height / 2,
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
 * @memberof ArrowHandle.prototype
 *
 */
ArrowHandle.prototype.removeEvents = function () {
	if ( _viewer ) {
		var inputManager = _viewer.getInputManager();
		inputManager.removeEventListener( InputManager.EventTypes.Down, _mouseDownHandler );
		inputManager.removeEventListener( InputManager.EventTypes.Up, _mouseUpHandler );
		inputManager.removeEventListener( InputManager.EventTypes.Move, _mouseMoveHandler );
	}
};

ArrowHandle.prototype.mouseMove = function ( event ) {
};

ArrowHandle.prototype.mouseUp = function ( event ) {
};

ArrowHandle.prototype.mouseDown = function ( event ) {
};


/**
 * Repositions the WCS trihedron.
 *
 * @function setPosition
 * @memberof ArrowHandle.prototype
 *
 * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.
 * @param {Number} y - vertical position in pixels from the bottom left of the viewport.
 */
ArrowHandle.prototype.setPosition = function ( x, y ) {
	_positionX = x;
	_positionY = y;
	this.position.x = x;
	this.position.y = y;
};

/**
 * Queries the position of the WCS
 *
 * @function getPosition
 * @memberof ArrowHandle.prototype
 *
 * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.
 */
ArrowHandle.prototype.getPosition = function () {
	return this.position;
};

/**
 * Sets the size of the arrow.
 *
 * @function setSize
 * @memberof ArrowHandle.prototype
 *
 * @param {Number} w - a number used to set the width of the WCS render area.
 * @param {Number} h - a number used to set the height of the WCS render area.
 */
ArrowHandle.prototype.setSize = function ( w, h ) {
	_width = w;
	_height = h;
	this.size.width = w;
	this.size.height = h;
};

/**
 * Queries the size of the WCS.
 *
 * @function getSize
 * @memberof ArrowHandle.prototype
 *
 * @returns {Object} an object that contains the height and width (each as a number) of the WCS render area.
 */
ArrowHandle.prototype.getSize = function () {
	return this.size;
};

/**
 * Sets whether the WCS is rendered on top of or behind the scene geometry.
 *
 * @function setOnTop
 * @memberof ArrowHandle.prototype
 *
 * @params {Boolean} onTop - true if on top, false if behind.
 */
ArrowHandle.prototype.setOnTop = function ( onTop ) {
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
 * Queries whether the arrow handle is rendered on top of or behind the scene geometry.
 *
 * @function getOnTop
 * @memberof ArrowHandle.prototype
 *
 * @returns {Boolean} true if on top, false if behind.
 */
ArrowHandle.prototype.getOnTop = function () {
	return this.onTop;
};

/**
 * Set the arrow handle direction.
 *
 * @function setDirection
 * @memberof ArrowHandle.prototype
 *
 * @param {Vector3} the intial vector (0, 1, 0 ) to toVector
 */
ArrowHandle.prototype.setDirection = function ( x, y, z ) {
	var toVector = new THREE.Vector3( x, y, z );
	_object.quaternion.setFromUnitVectors( new THREE.Vector3( 0, 1, 0 ), toVector );
	_object.updateMatrix();
	_object.updateMatrixWorld();

	if ( _viewer ) {
		_viewer.draw();
	}
};

/**
 * Get the intersect object.
 *
 * @function intersect
 * @memberof ArrowHandle.prototype
 *
 * @param {Number} the click mouse poistion x
 * @param {Number} the click mouse poistion y
 */
ArrowHandle.prototype.intersect = function ( clientX, clientY ) {
	var coordinateVector = new THREE.Vector3();
	coordinateVector.set(
		2 * ( ( clientX - ( _positionX - _width / 2 ) ) / _width ) - 1,
		1 - 2 * ( ( clientY - ( _positionY - _height / 2 ) ) / _height ),
		1 );

	var raycaster = new THREE.Raycaster();
	raycaster.setFromCamera( coordinateVector, _camera );
	var intersects = raycaster.intersectObject( _object, true );
	var intersect;
	if ( intersects.length > 0 ) {
		intersect = intersects[ 0 ];
	}
	return intersect;
};

export default ArrowHandle;
