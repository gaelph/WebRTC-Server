/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
'use strict';

function RoomsViewController ($scope, $http, $location) {
    $scope.rooms = [];

    $http.get('/api/rooms').then(function (response) {
        if (typeof response.data !== 'undefined') {
            response.data.forEach(function (object) {
                var room = angular.fromJson(object);
                var shouldAdd = true;
                for (var existingRoom in $scope.rooms) {
                    if (existingRoom.rid === room.rid) shouldAdd = false;
                }

                if (shouldAdd) $scope.rooms.push(angular.fromJson(object));
            });
            console.log("type of $scope.rooms = " + typeof $scope.rooms);
        }
    }, function (response) {
        console.log("Error while retreiving rooms");
    });

    //Should start a new socket to a room
    $scope.join = function (room) {
        room = angular.fromJson(room);
        $location.path('/room/' + room.rid);
    };

}