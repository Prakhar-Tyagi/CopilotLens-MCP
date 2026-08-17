//© 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC

( function () {
	//"use strict";

    /**
     * @class Scene Graph Object Name
     * @classdesc Describe object here.<br><br>
     */

	var SceneGraphObject = function () {  //NOSONAR

		// Private variables
		var _object = null,
			_scene = null,
			_camera = null,
			_viewer = null,

			// Set these variable's values here
			_width = 100,
			_height = 100,
			_positionX = 10,
			_positionY = 10,

			// Event function variables
			_mouseDownHandler = null,
			_mouseUpHandler = null,
			_mouseMoveHandler = null,
			_resizeHandler = null,

			// User-defined variables
			_userVar = null;

		var _SceneGraphObject = function () {

			// Defines _object
			_createObject();

			// Defines _scene & _camera
			_createScene();

			// Defines event function variables
			_createEventVariables.call( this );

			// Public variables
			this.name = "SceneGraphObject name";
			this.original = _object;
			//this.updateBBox = false; // Deprecated:  updateBBox will be removed after the 1.7.0 release.  To update the global boundingbox to include sgo's boundingbox, use sgoBBox
			this.scene = _scene;
			this.camera = _camera;
			this.size = { width: _width, height: _height };
			this.position = { x: _positionX, y: _positionY };
			this.boundingBox = new THREE.Box3().setFromObject( _object );
			this.boundingSphere = new THREE.Sphere();
			this.boundingBox.getBoundingSphere( this.boundingSphere );
			this.sgoBBox = this.boundingBox; // NOTE: Applicable for all SGOs that want the global boundingbox to include sgo's boundingbox
			this.visible = true;

			// Set these variable's values here
			//this.front   									// Set to true to render sgo always in front of model scene, ignore otherwise
			//this.back  									// Set to true to render sgo always behind model scene, ignore otherwise
			this.original.userData.ignoreRenderMode = true;	// Determines if render modes should effect the SGO
		};

		_SceneGraphObject.prototype = {
			constructor: _SceneGraphObject
		};

		Object.defineProperties( _SceneGraphObject.prototype, {

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

					/* 	Event listener's can be attached to be attached to the viewer here. Example:

					if ( viewer ) {
						var inputManager = _viewer.getInputManager();
						if ( inputManager ) {
							var priority = 0; // 0:highest, Infinity:lowest
							inputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Down, _mouseDownHandler, priority );
							inputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Up, _mouseUpHandler, priority );
							inputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Move, _mouseMoveHandler, priority );
						}
						viewer.addEventListener( "resize", _resizeHandler );
					}

					// In-Scene SGO's starting 1.7 should listen for viewer 'resize' event to ensure sgo size

					_viewer.addEventListener( "resize", function () {
						if ( _viewer ) {
							var dim = _viewer.control.getSize();
							this.setSize( dim.width, dim.height );
							_viewer.draw();
						}
					}.bind( this ) );

					*/
				}
			},

			visible: {
				get: function () {
					return _object.visible;
				},

				set: function ( value ) {
					_object.visible = value;

					/*  The draw is optional here.  It can be performed internally or externally of the SGO object for visibility changes.
					*	if ( _viewer ) {
					*		_viewer.draw();
					*	}
					*/
				}
			}
		} );

		// Define your custom object(s) here as a THREE.js Mesh/group/object3d
		// MUST DEFINE '_object'

		function _createObject () {

			var geometry = new THREE.BufferGeometry();
			var material = new THREE.MeshBasicMaterial();

			_object = new THREE.Mesh( geometry, material );
		}

		// Define your scene and camera here
		// DEFINE '_scene' and '_camera', see Arrow.js to copy model scene's active camera
		function _createScene () {

			_scene = new THREE.Scene();
			_camera = new THREE.OrthographicCamera( _width * -0.25, _width * 0.25, _height * 0.25, _height * -0.25, -100, 100 );

			_scene.add( _camera );
			_scene.add( _object );
		}

		// Define your SGO event functions
		// OPTIONAL - Define event variables
		function _createEventVariables () {
			_resizeHandler = function resize ( resizeEvent ) {
				//resizeEvent.width;
				//resieEvent.height;
				/* Resize Action */
			}.bind( this );

			_mouseDownHandler = this.mouseDown.bind( this );
			_mouseUpHandler = this.mouseUp.bind( this );
			_mouseMoveHandler = this.mouseMove.bind( this );
		}

		/**
		 *
		 * 	Define further private functions here!
		 *
		 */


		/**
		 * Defines how to render your SGO, automatically called by the ViewerManager when an SGO is added.
		 *
		 * @function render
		 * @memberof SceneGraphObject.prototype
		 *
		 */
		_SceneGraphObject.prototype.render = function ( renderer, camInfo ) {

			// Sets portion of screen to render to
			renderer.setViewport(
				_positionX,
				_positionY,
				_width,
				_height
			);

			/**
			 *  Describe camera behaivor here
			 *   - see WCSTrihedron.js or NavCube.js to sync rotation with the model scene
			 * 	 - see Arrow.js to copy model scene's active camera
			 */

			// Render
			renderer.render( _scene, _camera );
		};

		/**
		 * Removes attached events
		 *
		 * @function removeEvents
		 * @memberof SceneGraphObject.prototype
		 *
		 */
		_SceneGraphObject.prototype.removeEvents = function () {

			/* Remove attached events. Example:
			if( _viewer ) {
				var inputManager = _viewer.getInputManager();
				inputManager.removeEventListener( PLMVisWeb.InputManager.EventTypes.Down, _mouseDownHandler );
				inputManager.removeEventListener( PLMVisWeb.InputManager.EventTypes.Up, _mouseUpHandler );
				inputManager.removeEventListener( PLMVisWeb.InputManager.EventTypes.Move, _mouseMoveHandler );
				_viewer.removeEventListener( "resize", _resizeHandler );
			}
			*/

			console.warn( "Events were not removed, define events to be removed and delete this warning." );
		};

		/**
		 * Repositions the SGO.
		 *
		 * @function setPosition
		 * @memberof SceneGraphObject.prototype
		 *
		 * @param {Number} x - horizontal position in pixels from the bottom left of the viewport.
		 * @param {Number} y - vertical position in pixels from the bottom left of the viewport.
		 */
		_SceneGraphObject.prototype.setPosition = function ( x, y ) {
			_positionX = x;
			_positionY = y;
			this.position.x = x;
			this.position.y = y;
		};

		/**
		 * Queries the position of the SGO
		 *
		 * @function getPosition
		 * @memberof SceneGraphObject.prototype
		 *
		 * @returns {Number[]} represents the X (0) and Y (1) coordinates as measured from the bottom left of the viewport.
		 */
		_SceneGraphObject.prototype.getPosition = function () {
			return this.position;
		};

		/**
		 * Sets the size of the SGO.
		 *
		 * @function setSize
		 * @memberof SceneGraphObject.prototype
		 *
		 * @param {Number} w - a number used to set the width of the SGO render area.
	 	 * @param {Number} h - a number used to set the height of the SGO render area.
		 */
		_SceneGraphObject.prototype.setSize = function ( w, h ) {
			_width = w;
			_height = h;
			this.size.width = w;
			this.size.height = h;
		};

		/**
		 * Queries the size of the SGO.
		 *
		 * @function getSize
		 * @memberof SceneGraphObject.prototype
		 *
		 * @returns {Object} an object that contains the height and width (each as a number) of the SGO render area.
		 */
		_SceneGraphObject.prototype.getSize = function () {
			return this.size;
		};

		/**
		 *
		 *  Mouse events pass through the viewer to the SGO and can be defined here.
		 *
		 *  Add 'event.override = true' to ignore the mouse event in the viewer
		 *
		 */
		_SceneGraphObject.prototype.mouseUp = function ( event ) { };

		_SceneGraphObject.prototype.mouseDown = function ( event ) { };

		_SceneGraphObject.prototype.mouseMove = function ( event ) { };

		/**
		 *
		 * 	Define further prototype functionality here!
		 *
		 */

		return new _SceneGraphObject();
	};

	SceneGraphObject.prototype = {
		constructor: SceneGraphObject,
		_name: "SceneGraphObject"
	};

	self.SceneGraphObject = SceneGraphObject;
} )();
