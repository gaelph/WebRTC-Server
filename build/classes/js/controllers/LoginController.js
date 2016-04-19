/* global sjcl */

'use strict';

function LoginController($rootScope, $scope, $cookies, UserID, $http, $location) {
    $scope.email;
    $scope.password;
    $rootScope.loginStatus = "";
    if (UserID.get() !== '') {
        $location.path('/rooms');
    }

    $scope.loggin = function () {
        var salt = sjcl.hash.sha1.hash($scope.email);
        var passphrase = sjcl.misc.pbkdf2($scope.password, salt, 1000, 128);

        $http.post('/api/login', {email: $scope.email, password: sjcl.codec.base64.fromBits(passphrase)}).then(function (response) {
            if (typeof response.data.user.UID !== 'undefined') {
                UserID.set(response.data.user.UID);

                $rootScope.loginStatus = '';
                $location.path('/rooms');
                //$rootScope.$apply();
            } else {
                $scope.userloggedin = false;
                $scope.email;
                $scope.password;
                $rootScope.loginStatus = response.data.error;
            }
        }, function (response) {
            $rootScope.loginStatus = "Login failed.";
        });
    };

    $scope.createUser = function () {
        $location.path('/user/create');
    };

    //This is to fix Firefox on mobile... maybe
    $scope.loginButton = document.querySelector("#login-button");
    $scope.createButton = document.querySelector("#create-button");

    addClickListener($scope, $scope.loginButton, $scope.loggin);
    addClickListener($scope, $scope.createButton, $scope.createUser);
}