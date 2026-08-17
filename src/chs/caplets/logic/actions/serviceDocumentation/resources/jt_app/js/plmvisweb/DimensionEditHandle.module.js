//© 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC

import { CameraMode, InputManager, THREE } from "@com.siemens.plm.web/PLMVisWeb";

/**
 * @class DimensionEditHandle SGO Object
 * @classdesc Represents the DimensionEditHandle SGO Object for the PLMVisWeb Viewer.<br><br>
 */

var DimensionEditHandle = function ( color, enableDrag, visible, size, checkTextFn ) { //NOSONAR

	// Private variables
	var _object = null,
		_scene = null,
		_camera = null,
		_viewer = null,

		// Default variables to set
		_width = size ? size.width : 8,
		_height = size ? size.height : 8,
		_positionX = 0,
		_positionY = 0,

		// Event variables
		_mouseDownHandler = null,
		_mouseUpHandler = null,
		_mouseMoveHandler = null,
		_resizeHandler = null,

		// User-defined variables
		_dirty = false,
		_mouseDown = false,

		_checkTextFn = checkTextFn,
		_controlPointInfo,
		_manager,
		_mouseVector = new THREE.Vector3(),
		_intersectVector = new THREE.Vector3();

	var _DimensionEditHandle = function () {

		_createObject( color );
		_createScene();
		_createEventVariables.call( this );

		this.name = "DimensionEditHandle";
		this.original = _object;
		this.scene = _scene;
		this.camera = _camera;
		this.size = { width: _width, height: _height };
		this.position = { x: _positionX, y: _positionY };
		this.boundingBox = new THREE.Box3().setFromObject( _object );
		this.boundingSphere = new THREE.Sphere();
		this.boundingBox.getBoundingSphere( this.boundingSphere );
		this.visible = visible;
		this.onTop = true;

		this.front = true;
		this.original.userData.ignoreRenderMode = true;
	};

	_DimensionEditHandle.prototype = {
		constructor: _DimensionEditHandle
	};

	Object.defineProperties( _DimensionEditHandle.prototype, {

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

					if ( enableDrag ) {
						var inputManager = _viewer.getInputManager();
						if ( inputManager ) {
							inputManager.addEventListener( InputManager.EventTypes.Down, _mouseDownHandler, 0 );
							inputManager.addEventListener( InputManager.EventTypes.Up, _mouseUpHandler, 0 );
							inputManager.addEventListener( InputManager.EventTypes.Move, _mouseMoveHandler, 0 );
						}
					}
				}
			}
		},

		visible: {
			get: function () {
				return _object.visible;
			},

			set: function ( value ) {
				_object.visible = value;
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

	function _getViewCamera ( viewer ) {
		var camInfo = viewer.getCameraInfo();
		if ( viewer.getCameraMode() === CameraMode.PERSPECTIVE ) {
			var pCamera = new THREE.PerspectiveCamera( camInfo.perspective.fov, camInfo.perspective.aspect, camInfo.perspective.near, camInfo.perspective.far );
			pCamera.position.fromArray( camInfo.perspective.pos );
			pCamera.up.fromArray( camInfo.perspective.up );
			pCamera.lookAt( new THREE.Vector3().fromArray( camInfo.perspective.tgt ) );
			pCamera.updateMatrixWorld();
			return pCamera;
		}
		else {
			var oCamera = new THREE.OrthographicCamera( camInfo.orthographic.left, camInfo.orthographic.right, camInfo.orthographic.top,
				camInfo.orthographic.bottom, camInfo.orthographic.near, camInfo.orthographic.left );
			oCamera.position.fromArray( camInfo.orthographic.pos );
			oCamera.up.fromArray( camInfo.orthographic.up );
			oCamera.lookAt( new THREE.Vector3().fromArray( camInfo.orthographic.tgt ) );
			oCamera.updateMatrixWorld();
			return oCamera;
		}
	};


	function _createObject ( color ) {
		_object = new THREE.Object3D();
		var material = new THREE.MeshPhongMaterial( { color: color ? color : 0xff0000 } );

		//  _width / 4
		var geometry = new THREE.SphereGeometry( _width / 5, 20, 20 );
		var obj = new THREE.Mesh( geometry, material );

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

	function _createEventVariables () {
		_resizeHandler = function resize () {
			// this.setPosition( _positionX, _positionY );
			// _viewer.draw();
		}.bind( this );

		_mouseDownHandler = this.mouseDown.bind( this );
		_mouseUpHandler = this.mouseUp.bind( this );
		_mouseMoveHandler = this.mouseMove.bind( this );
	}


	// Only used for outline SGO inset viewport
	_DimensionEditHandle.prototype.renderOutline = function ( renderer ) {
		renderer.setScissorTest( true );
		renderer.setScissor(
			_positionX - _width / 2,
			_positionY - _height / 2,
			_width,
			_height );
		renderer.setClearColor( new THREE.Color( 0x00FF00, 1.0 ) ); // border color
		renderer.clearColor(); // clear color buffer
	};

	/**
	 * Defines how to render the DimensionEditHandle
	 *
	 * @function render
	 * @memberof SceneGraphObject.prototype
	 *
	 */
	_DimensionEditHandle.prototype.render = function ( renderer, camInfo ) {
		// this.renderOutline( renderer );

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

	/**
	 * Removes attached events
	 *
	 * @function removeEvents
	 * @memberof DimensionEditHandle.prototype
	 *
	 */
	_DimensionEditHandle.prototype.removeEvents = function () {
		if ( _viewer ) {
			var inputManager = _viewer.getInputManager();
			inputManager.removeEventListener( InputManager.EventTypes.Down, _mouseDownHandler );
			inputManager.removeEventListener( InputManager.EventTypes.Up, _mouseUpHandler );
			inputManager.removeEventListener( InputManager.EventTypes.Move, _mouseMoveHandler );
			window.removeEventListener( "resize", _resizeHandler );
		}
	};

	/**
	 * Repositions the DimensionEditHandle.
	 *
	 * @function setPosition
	 * @memberof DimensionEditHandle.prototype
	 *
	 * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.
	 * @param {Number} y - vertical position in pixels from the bottom left of the viewport.
	 */
	_DimensionEditHandle.prototype.setPosition = function ( x, y ) {
		_positionX = x;
		_positionY = y;
		this.position.x = x;
		this.position.y = y;
	};

	/**
	 * Queries the position of the DimensionEditHandle.
	 *
	 * @function getPosition
	 * @memberof DimensionEditHandle.prototype
	 *
	 * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.
	 */
	_DimensionEditHandle.prototype.getPosition = function () {
		return this.position;
	};

	/**
	 * Sets the size of the DimensionEditHandle.
	 *
	 * @function setSize
	 * @memberof DimensionEditHandle.prototype
	 *
	 * @param {Number} w - a number used to set the width of the DimensionEditHandle render area.
	   * @param {Number} h - a number used to set the height of the DimensionEditHandle render area.
	 */
	_DimensionEditHandle.prototype.setSize = function ( w, h ) {
		_width = w;
		_height = h;
		this.size.width = w;
		this.size.height = h;
	};

	/**
	 * Queries the size of the DimensionEditHandle.
	 *
	 * @function getSize
	 * @memberof DimensionEditHandle.prototype
	 *
	 * @returns {Object} an object that contains the height and width (each as a number) of the DimensionEditHandle render area.
	 */
	_DimensionEditHandle.prototype.getSize = function () {
		return this.size;
	};

	/**
	 * Sets whether the DimensionEditHandle is rendered on top of or behind the scene geometry.
	 *
	 * @function setOnTop
	 * @memberof DimensionEditHandle.prototype
	 *
	 * @params {Boolean} onTop - true if on top, false if behind.
	 */
	_DimensionEditHandle.prototype.setOnTop = function ( onTop ) {
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
	 * Queries whether the DimensionEditHandle is rendered on top of or behind the scene geometry.
	 *
	 * @function getOnTop
	 * @memberof DimensionEditHandle.prototype
	 *
	 * @returns {Boolean} true if on top, false if behind.
	 */
	_DimensionEditHandle.prototype.getOnTop = function () {
		return this.onTop;
	};

	_DimensionEditHandle.prototype._isMouseOver = function ( event ) {
		//
	};


	_DimensionEditHandle.prototype.getControlPointPosition = function () {
		if ( !_controlPointInfo ) { return; }

		var pointTypes = [ { pointType: _controlPointInfo.pointType, side: _controlPointInfo.side } ];
		var controlPoints = _manager.getControlPoints( _controlPointInfo.dimensionId, pointTypes );

		if ( controlPoints ) {
			return controlPoints[ 0 ].value;
		}
	};

	_DimensionEditHandle.prototype.updatePosition = function () {
		if ( !_controlPointInfo ) { return; }

		// check on ArrowLineStart
		// if text go outside extension line, should hide
		if (
			_controlPointInfo.pointType === 'ArrowLineStart' &&
			_checkTextFn ) {
			this.visible = _checkTextFn();
		}

		// optimized for render
		if ( !this.visible ) { return; }


		var point = this.getControlPointPosition();
		if ( point ) {
			var position2d = this.viewer.projectModelPointToViewCoordinate( point[ 0 ], point[ 1 ], point[ 2 ] );
			this.setPosition( position2d[ 0 ], position2d[ 1 ] );
		}
	};


	_DimensionEditHandle.prototype.mouseMove = function ( event ) {
		if ( !_mouseDown || !this.viewer || !_manager ) { return; }
		event.override = true;
		this.viewer.startRenderLoop();

		var camera = _getViewCamera( this.viewer );
		var viewerPlace = this.viewer.domElement;

		// Need to check event for touch or mouse.
		var ptX, ptY;
		if ( event.touches && event.touches.length > 0 ) {
			ptX = event.touches[ 0 ].pageX;
			ptY = event.touches[ 0 ].pageY;
		}
		else {
			ptX = event.offsetX;
			ptY = event.offsetY;
		}

		var pX = ( ptX / viewerPlace.width ) * 2 - 1;
		var pY = - ( ptY / viewerPlace.height ) * 2 + 1;

		_mouseVector = new THREE.Vector3( pX, pY, 1 );
		_mouseVector.unproject( camera );
		var raycaster = new THREE.Raycaster( camera.position, _mouseVector.clone().sub( camera.position ).normalize() );
		var currentIntersectVector = new THREE.Vector3();
		raycaster.ray.closestPointToPoint( _intersectVector, currentIntersectVector );
		var diff = new THREE.Vector3().subVectors( currentIntersectVector, _intersectVector );

		// update current mouseVector
		_intersectVector = currentIntersectVector;

		// invoke manager's updatePosition
		var params = {
			dragVector: diff.toArray(),
			type: _controlPointInfo.pointType,
			side: _controlPointInfo.side,
			isControlPoint: true
		};
		_manager.updatePosition( _controlPointInfo.dimensionId, params );
	};

	_DimensionEditHandle.prototype.mouseUp = function ( event ) {
		if ( _mouseDown ) {
			event.override = true;
			this.viewer.stopRenderLoop();
			_mouseDown = false;

			if ( _controlPointInfo.pointType === 'ArrowHeadStart' ) {
				var visible = _manager.isComponentVisible( _controlPointInfo.dimensionId, 'ArrowLine', _controlPointInfo.side );
				_manager.setComponentVisibility( _controlPointInfo.dimensionId, !visible, 'ArrowLine', _controlPointInfo.side );
			}
		}
	};

	_DimensionEditHandle.prototype.mouseDown = function ( event ) {
		// Need to check event for touch or mouse.
		var ptX, ptY;
		if ( event.touches && event.touches.length > 0 ) {
			ptX = event.touches[ 0 ].pageX;
			ptY = event.touches[ 0 ].pageY;
		}
		else {
			ptX = event.offsetX;
			ptY = event.offsetY;
		}

		if ( _manager && this.intersect( ptX, ptY ) ) {
			event.override = true;
			_mouseDown = true;

			_mouseVector.x = ptX;
			_mouseVector.y = ptY;

			// get corresponding model point
			_intersectVector = new THREE.Vector3().fromArray( this.getControlPointPosition() );
		}
	};

	/**
	 * Get the intersect object.
	 *
	 * @function intersect
	 * @memberof _DimensionEditHandle.prototype
	 *
	 * @param {Number} the click mouse poistion x
	 * @param {Number} the click mouse poistion y
	 */
	_DimensionEditHandle.prototype.intersect = function ( clientX, clientY ) {
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


	return new _DimensionEditHandle();
};

DimensionEditHandle.prototype = {
	constructor: DimensionEditHandle,
	_name: "DimensionEditHandle"
};

export default DimensionEditHandle;
