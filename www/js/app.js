/* global WebChatRemoteParties, sjcl, SignalingService, UserIDService, CreateAccountController, PeopleListController, LoginController, LocalWebcamService, LocalWebcamController, lwVideo, RoomsViewController, attachMediaStream, WebChat */

'use strict';

var app = angular.module("Webcam-Test", ['ngCookies', 'ngRoute']);

function logError (e) {
    console.log(e);
}

app.config(['$routeProvider',
    function ($routeProvider) {
        $routeProvider.
                when('/login', {
                    templateUrl: 'templates/login.html',
                    controller: 'LoginController'
                }).
                when('/account/create', {
                    templateUrl: 'templates/createaccount.html',
                    controller: 'CreateAccountController'
                }).
                when('/room/:rid', {
                    templateUrl: 'templates/videoChat.html',
                    controller: 'WebChatController'
                }).
                when('/rooms', {
                    templateUrl: 'templates/rooms.html',
                    controller: 'RoomsViewController'
                }).
                otherwise({
                    redirectTo: '/login'
                });
    }]);

app.config(function ($httpProvider) {
    $httpProvider.interceptors.push('authInterceptor');
});

app.factory('authInterceptor', [
    '$rootScope',
    '$q',
    '$window',
    '$cookies',
    function ($rootScope, $q, $window, $cookies) {
        return {
            request: function (config) {
                config.headers = config.headers || {};
                if ($cookies.get('token')) {
                    config.headers.Authorization = 'Bearer ' + $cookies.get('token');
                }
                return config;
            },
            response: function (response) {
                if (response.status === 401) {
                    $rootScope.loginStatus = "Login failed.";
                    $rootScope.$apply();
                }
                return response || $q.when(response);
            }
        };
    }]);

// SocketIO Signaling service
app.service('SignalingService', [
    'UserID',
    SignalingService]);

// Local webcam service and directive
app.service('LocalWebcamService', [
    '$rootScope',
    LocalWebcamService]);

app.directive('lwVideo', [
    '$rootScope',
    'LocalWebcamService',
    lwVideo]);

// The actual chat managment service
//NOTE: This and webChat should probaly be one and the same
//NOTE: Should find a way to get rid of $rootScope
app.service('WebChatRemoteParties', [
    '$rootScope',
    '$sce',
    'SignalingService',
    'UserID',
    'LocalWebcamService',
    WebChatRemoteParties]);

//This should be extended to complete ThisUser service (get, getID, get SocketID, get FullName...
app.service('UserID', [
    '$cookies',
    UserIDService]);

//This and remoteParties should probably be one and the same...
app.service('WebChat', [
    'SignalingService',
    '$http',
    '$sce',
    '$rootScope',
    'WebChatRemoteParties',
    'LocalWebcamService',
    function (SignalingService, $http, $sce, $rootScope, WebChatRemoteParties, LocalWebcamService) {

        this.init = function (uid, rid) {
            this.remoteParties = WebChatRemoteParties;
            this.remoteParties.$init(rid);
            this.signaling = SignalingService;
            this.receivedUsers = false;

            this.peerConnection = null;

            $rootScope.user;
            $http.get('/api/user/' + encodeURIComponent(uid)).then(function successCallback (response) {
                $rootScope.user = response.data;

                $rootScope.user['peerConnection'] = new RTCPeerConnection(null);

                $rootScope.user.peerConnection.onicecandidate = function (event) {
                    var user = $rootScope.user;

                    var c = event['candidate'];
                    if (c !== null) {
                        console.log("event['candidate'] : ", c['candidate']);
                        var candidate = {
                            candidate: c['candidate'],
                            sdpMLineIndex: c['sdpMLineIndex'],
                            sdpMid: c['sdpMid']
                        };
                        //console.log("Get Own Properties Names : ", event.getOwnPropertyNames());
                        SignalingService.sendICECandidates(candidate, user.sid);
                    }
                };

                //TODO: Add support for mesh communication
                //NOTE: This should find its place in a View Controller
                $rootScope.user.peerConnection.onaddstream = function (event) {
                    var user = $rootScope.user;

                    user.videoFeed = event.stream;
                    user.videoSource = $sce.trustAsResourceUrl(window.URL.createObjectURL(
                            event.stream));

                    if (typeof attachMediaStream !== 'undefined') {
                        var videoElement = document.querySelector("#video_" + user.sid);
                        attachMediaStream(videoElement, user.videoFeed);
                        LocalWebcamService.gotLocalVideo();
                    }

                    WebChatRemoteParties.$numberOfOngoingCalls++;
                    console.log("New call ongoing : ",
                            WebChatRemoteParties.$numberOfOngoingCalls);
                };

                LocalWebcamService.start();

            }, function errorCallback (response) {
                console.log("error getting user infos : ", response.status, " | ", response.statusText);
            });


            $http.get('/api/room/' + rid + '/users').then(function successCallback (response) {
                var users = [];
                response.data.forEach(function (user) {
                    user = angular.fromJson(user);
                    if (user !== null) users.push(user);
                });
                WebChatRemoteParties.$add(users, false);
                WebChatRemoteParties.$receivedUsers = true;
            }, function errorCallback (response) {
                console.log("error getting users : ", response.status, " | ", response.statusText);
            });

            SignalingService.onanswer = function (message) {
                var user = $rootScope.user;

                var description = new RTCSessionDescription(message);

                user.peerConnection.setRemoteDescription(description, function () {
                    console.log($rootScope.user.sid, "answered");
                }, window.logError);
            };

            SignalingService.onicecandidate = function (message) {
                var user = $rootScope.user;

                if (user !== null && message !== null) {
                    var candidate = {
                        candidate: message['candidate'],
                        sdpMLineIndex: message['sdpMLineIndex'],
                        sdpMid: message['sdpMid']
                    };
                    console.log('Received ICE candidate : ', candidate);
                    var rtcIceCandidate = new RTCIceCandidate(candidate);
                    user.peerConnection.addIceCandidate(rtcIceCandidate);
                }
            };

            //Start the local webcam
            LocalWebcamService.on(LocalWebcamService.events.LOADEDMETADATA, this, function () {
                this.gotLocalVideo();
            });



            // We have local Webcam
            this.gotLocalVideo = function () {
                var user = $rootScope.user;

                if (user.peerConnection === null) {
                    user.peerConnection = new RTCPeerConnection(null);
                }

                user.peerConnection.addStream($rootScope.stream);

                user.peerConnection.createOffer(function (description) {
                    description.sdp = description.sdp.replace("o=-", "o=" + user.sid);
                    //description.sdp = description.sdp.replace("a=sendrecv", "a=sendonly");
                    user.peerConnection.setLocalDescription(description, function () {
                        var desc = {
                            type: description['type'],
                            sdp: description['sdp']
                        };
                        SignalingService.sendOffer(desc, user.sid);
                    }, window.logError);
                }, window.logError);



            };

            this.oninfo = function (users) {
                this.remoteParties.add(users);
            };
        };

    }]);

app.controller('WebChatController', [
    '$rootScope',
    '$scope',
    '$http',
    '$location',
    '$routeParams',
    'WebChat',
    'UserID',
    function ($rootScope, $scope, $http, $location, $routeParams, WebChat, UserID) {

        $scope.userFullName;
        $scope.webChat;

        if (UserID.get() !== '') {

            $scope.webChat = WebChat;
            WebChat.init(UserID.get(), $routeParams.rid);
            $scope.webChat.remoteParties.$updateView = function () {
                if (WebChat.remoteParties.$receivedUsers === true)
                    $scope.$apply();
            };

            getUserName(UserID.get());
        }
        else {
            $location.path("/login");
            //$rootScope.$apply;
        }



        function getUserName (uid) {

            $http.get('/api/user/' + uid)
                    .then(function (response) {
                        if (!response.data.firstname) {
                            console.log("Error retrieving User Info :(");
                            return;
                        }

                        $rootScope.userFullName = response.data.firstname + " " + response.data.lastname;


                    }, function (response) {
                        $location.path("/login");
                    });
        }

    }]);


/*
 * Things to think about :
 * Is it a normal thing to have all the code to manage a connection stuffed into one service ?
 * Shouldn't that be handled by a controller associated with views (video containers)
 *
 * Re-think the naming conventions to avoid confusion when the text chat thing will be implemented.
 *
 * - Merge WebChat Service and WebChatRemoteParties
 * - Put the one-peer-connection management code in a specific Object Type / controller / directive
 */

// Left-Side People List View controller
app.controller('PeopleListController', [
    'WebChatRemoteParties',
    '$scope',
    '$rootScope',
    '$cookies',
    '$location',
    'SignalingService',
    PeopleListController]);


//TODO: Controller for one Video Chat
app.controller('ChatViewController', [
    'WebChatRemoteParties',
    '$scope',
    function (WebChatRemoteParties, $scope) {
        $scope.remoteParties = WebChatRemoteParties;
        /* Stuff to do with Chat View
         * manage one connection
         * resize the divs
         * create the views ?
         *
         * how to associate one controller with one peer ?
         * how to load/unload dynamically ?
         */


    }]);

// Controller for the Log in page
app.controller('LoginController', [
    '$rootScope',
    '$scope',
    '$cookies',
    'UserID',
    '$http',
    '$location',
    LoginController]);

// Controller for the Account creation page
app.controller('CreateAccountController', [
    '$scope',
    '$cookies',
    '$http',
    '$location',
    '$window',
    'UserID',
    CreateAccountController]);

app.controller('RoomsViewController', [
    '$scope',
    '$http',
    '$location',
    RoomsViewController]);

// COntroller for the header
// NOTE: Currently useless as the header isn't in use
// TODO: Think about its use as an android-'Material Design'-like Toolbar
app.controller("headerController", [
    '$rootScope',
    '$scope',
    '$location',
    'UserID',
    '$cookies',
    function ($rootScope, $scope, $location, UserID, $cookies) {
        $rootScope.userFullName;

        $scope.loggout = function () {
            $rootScope.userFullName = '';
            $cookies.remove('token');

            $location.path("/login");
            //$rootScope.$apply();
        };

    }]);