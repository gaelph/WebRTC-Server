'use strict';

function PeopleListController(WebChatRemoteParties, $scope, $rootScope, $cookies, $location, SignalingService) {
    $scope.remoteParties = WebChatRemoteParties;
    $rootScope.userFullName;
    $scope.listPosition = '0px';
    /*Stuff to do with the peer list
     *  add/remove
     *  update statuses
     *  show/hide
     *  
     */
    $scope.showHideList = function () {
        //var listDiv = document.querySelector("#list");
        if (parseInt($scope.listPosition) < 0) {
            $scope.listPosition = '0px';
        } else {
            $scope.listPosition = '-200px';
        }
    };



    $scope.logout = function () {
        var l = $scope.remoteParties.length
        for (var i = l-1; i >= 0; i++) {
            $scope.remoteParties[i].hangup();
        }
        
        $rootScope.userFullName = '';
        $cookies.remove('token');
        SignalingService.sendDisconnect();
        $location.path("/login");
        //$rootScope.$apply();
    };
}