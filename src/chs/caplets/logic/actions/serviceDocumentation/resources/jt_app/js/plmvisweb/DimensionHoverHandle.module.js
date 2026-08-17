//© 2019 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC

import { THREE } from "@com.siemens.plm.web/PLMVisWeb";

/**
 * @class DimensionHoverHandle SGO Object
 * @classdesc Represents the DimensionHoverHandle SGO Object for the PLMVisWeb Viewer.<br><br>
 */

var DimensionHoverHandle = function ( type, color, size ) { //NOSONAR

	// Private variables
	var _object = null,
		_scene = null,
		_camera = null,
		_viewer = null,

		// Default variables to set
		_width = size ? size.width : 100,
		_height = size ? size.height : 100,
		_positionX = 0,
		_positionY = 0,

		// User-defined variables
		_controlPointInfo,
		_manager,
		_flip = false;

	var _DimensionHoverHandle = function () {

		_createObject( type );
		_createScene();

		this.name = "DimensionHoverHandle";
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

	_DimensionHoverHandle.prototype = {
		constructor: _DimensionHoverHandle
	};


	Object.defineProperties( _DimensionHoverHandle.prototype, {

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
					this.setPosition( _positionX, _positionY );
					_viewer.draw();
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
		},

		flip: {
			get: function () {
				return _flip;
			},
			set: function ( value ) {
				_flip = value;
			}
		},

		manager: {
			get: function () {
				return _manager;
			},

			set: function ( manager ) {
				_manager = manager;
			}
		},

		controlPointInfo: {
			get: function () {
				return _controlPointInfo;
			},

			set: function ( controlPointInfo ) {
				_controlPointInfo = controlPointInfo;
			}
		}
	} );

	function _createObject ( type ) {
		_object = new THREE.Object3D();
		var material = color ? new THREE.MeshPhongMaterial( { color: color } ) : new THREE.MeshPhongMaterial( { color: 0xff0000 } );

		var obj = null;
		if ( type === 'Cone' ) {
			//  _width / 4
			var cylinderGeometry = new THREE.CylinderBufferGeometry( 0.8, 0.8, 8, 32 );
			var cylinder = new THREE.Mesh( cylinderGeometry, material );
			var coneGeometry = new THREE.CylinderBufferGeometry( 2, 0, 8, 32 );
			var cone = new THREE.Mesh( coneGeometry, material );
			cylinder.position.y = 12;
			cone.position.y = 4;

			var group = obj = new THREE.Group();
			group.add( cylinder );
			group.add( cone );
		} else if ( type === 'Sphere' ) {
			//  _width / 4
			var sphereGeometry = new THREE.SphereGeometry( _width / 5, 20, 20 );
			obj = new THREE.Mesh( sphereGeometry, material );
		}

		_object.add( obj );
	}

	function _createScene () {
		_scene = new THREE.Scene();
		_camera = new THREE.OrthographicCamera( _width * -0.25, _width * 0.25, _height * 0.25, _height * -0.25, -100, 100 );

		// add subtle ambient lighting
		var ambientLight = new THREE.AmbientLight( 0x292929 );
		_scene.add( ambientLight );

		var dlight1 = new THREE.DirectionalLight( 0xffffff, 0.7 );
		dlight1.position.set( -100, 100, 100 );
		_scene.add( dlight1 );

		var dlight2 = new THREE.DirectionalLight( 0xffffff, 0.7 );
		dlight2.position.set( 10, -100, -80 );
		_scene.add( dlight2 );

		var dlight3 = new THREE.DirectionalLight( 0xffffff, 0.7 );
		dlight3.position.set( 100, 0, 0 );
		_scene.add( dlight3 );

		_scene.add( _camera );
		_scene.add( _object );
	}

	function _unprojectScreenDirection ( p1, p2 ) {
		// var viewport = _viewer._renderer.getCurrentViewport();
		var v1 = new THREE.Vector3( 2 * ( ( p1[ 0 ] - ( _positionX - _width / 2 ) ) / _width ) - 1,
			1 - 2 * ( ( p1[ 1 ] - ( _positionY - _height / 2 ) ) / _height ),
			0 );

		var v2 = new THREE.Vector3( 2 * ( ( p2[ 0 ] - ( _positionX - _width / 2 ) ) / _width ) - 1,
			1 - 2 * ( ( p2[ 1 ] - ( _positionY - _height / 2 ) ) / _height ),
			0 );

		v1.unproject( _camera );
		v2.unproject( _camera );

		var direction = v2.sub( v1 ).normalize();
		return direction;
	};


	_DimensionHoverHandle.prototype.updatePosition = function () {
		if ( !_controlPointInfo ) { return; }

		var pointTypes = [ { pointType: _controlPointInfo.pointType, side: _controlPointInfo.side } ];
		var controlPoints = _manager.getControlPoints( _controlPointInfo.dimensionId, pointTypes );

		if ( controlPoints ) {
			var point = controlPoints[ 0 ].value;
			if ( point ) {
				var position2d = this.viewer.projectModelPointToViewCoordinate( point[ 0 ], point[ 1 ], point[ 2 ] );
				this.setPosition( position2d[ 0 ], position2d[ 1 ] );
			}
		}
	};


	/**
	 * Defines how to render the DimensionHoverHandle
	 *
	 * @function render
	 * @memberof SceneGraphObject.prototype
	 *
	*/
	_DimensionHoverHandle.prototype.render = function ( renderer, camInfo ) {
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

		this.updatePosition();

		renderer.render( _scene, _camera );
	};

	_DimensionHoverHandle.prototype.setDirection = function ( point1, point2 ) {
		// convert to sgo's world coordinate
		var direction = _unprojectScreenDirection( point1, point2 );

		var quaternion = new THREE.Quaternion().setFromUnitVectors( new THREE.Vector3( 0, 1, 0 ), direction );
		if ( this.flip ) {
			var tmp = new THREE.Quaternion().setFromAxisAngle( new THREE.Vector3( 0, 0, 1 ), Math.PI );
			quaternion.multiply( tmp );
		}

		var child = _object.children[ 0 ];
		child.quaternion.copy( quaternion );
		child.updateMatrix();
		child.updateMatrixWorld();

		if ( _viewer ) {
			_viewer.draw();
		}
	};

	/**
	 * Repositions the DimensionHoverHandle.
	 *
	 * @function setPosition
	 * @memberof DimensionHoverHandle.prototype
	 *
	 * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.
	 * @param {Number} y - vertical position in pixels from the bottom left of the viewport.
	 */
	_DimensionHoverHandle.prototype.setPosition = function ( x, y ) {
		_positionX = x;
		_positionY = y;
		this.position.x = x;
		this.position.y = y;
	};

	/**
	 * Queries the position of the DimensionHoverHandle.
	 *
	 * @function getPosition
	 * @memberof DimensionHoverHandle.prototype
	 *
	 * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.
	 */
	_DimensionHoverHandle.prototype.getPosition = function () {
		return this.position;
	};

	/**
	 * Sets the size of the DimensionHoverHandle.
	 *
	 * @function setSize
	 * @memberof DimensionHoverHandle.prototype
	 *
	 * @param {Number} w - a number used to set the width of the DimensionHoverHandle render area.
	   * @param {Number} h - a number used to set the height of the DimensionHoverHandle render area.
	 */
	_DimensionHoverHandle.prototype.setSize = function ( w, h ) {
		_width = w;
		_height = h;
		this.size.width = w;
		this.size.height = h;
	};

	/**
	 * Queries the size of the DimensionHoverHandle.
	 *
	 * @function getSize
	 * @memberof DimensionHoverHandle.prototype
	 *
	 * @returns {Object} an object that contains the height and width (each as a number) of the DimensionHoverHandle render area.
	 */
	_DimensionHoverHandle.prototype.getSize = function () {
		return this.size;
	};

	/**
	 * Sets whether the DimensionHoverHandle is rendered on top of or behind the scene geometry.
	 *
	 * @function setOnTop
	 * @memberof DimensionHoverHandle.prototype
	 *
	 * @params {Boolean} onTop - true if on top, false if behind.
	 */
	_DimensionHoverHandle.prototype.setOnTop = function ( onTop ) {
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
	 * Queries whether the DimensionHoverHandle is rendered on top of or behind the scene geometry.
	 *
	 * @function getOnTop
	 * @memberof DimensionHoverHandle.prototype
	 *
	 * @returns {Boolean} true if on top, false if behind.
	 */
	_DimensionHoverHandle.prototype.getOnTop = function () {
		return this.onTop;
	};

	return new _DimensionHoverHandle();
};

DimensionHoverHandle.prototype = {
	constructor: DimensionHoverHandle,
	_name: "DimensionHoverHandle"
};

export default DimensionHoverHandle;
