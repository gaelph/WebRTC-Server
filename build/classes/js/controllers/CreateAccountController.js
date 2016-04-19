/* global sjcl*/

'use strict';

function CreateAccountController($scope, $cookies, $http, $location, $window, UserID) {
    $scope.fields = {
        firstName: {
            name: 'firstName',
            type: 'text',
            placeHolder: 'First Name',
            value: '',
            valid: false
        },
        lastName: {
            name: 'lastName',
            type: 'text',
            placeHolder: 'Last Name',
            value: '',
            valid: false
        },
        email: {
            name: 'email',
            type: 'email',
            placeHolder: 'email@example.com',
            value: '',
            valid: false
        },
        password: {
            name: 'password',
            type: 'password',
            placeHolder: 'Password',
            value: '',
            valid: false
        },
        passwordConfirm: {
            name: 'passwordConfirm',
            type: 'password',
            placeHolder: 'Confirm your password',
            value: '',
            valid: false
        }
    };

    $scope.cancel = function () {
        $window.history.back();
    };

    $scope.verifyPassword = function () {
        var pattern = /[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?/i;


        for (var i in $scope.fields) {
            var input = $scope.fields[i];
            if (input.value === '') {
                input.valid = false;
            } else {
                input.valid = true;

                if (input.name === 'passwordConfirm') {
                    if ($scope.fields.passwordConfirm.value === $scope.fields.password.value) {
                        input.valid = true;
                    } else {
                        input.valid = false;
                    }
                }
            }
        }

        var input = $scope.fields.email;
        if (!pattern.test(input.value)) {
            input.valid = false;
        } else {
            input.valid = true;
        }
    };

    $scope.createAccount = function () {
        var salt = sjcl.hash.sha1.hash($scope.fields.email);
        var passphrase = sjcl.misc.pbkdf2($scope.fields.password, salt, 1000, 128);

        $http.post('/user', {
            firstName: $scope.fields.firstName.value,
            lastName: $scope.fields.lastName.value,
            email: $scope.fields.email.value,
            password: sjcl.codec.base64.fromBits(passphrase)
        }).then(function (response) {
            if (typeof response.data.UID !== 'undefined') {
                UserID.set(response.data.UID);
                $scope.userloggedin = true;
                $location.path('/login');
            }
        }, function () {

        });
    };
}