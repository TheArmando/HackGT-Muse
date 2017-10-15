angular.module('login').component('login', {
    templateUrl: 'login/login.template.html',

    controller: ['$timeout', '$rootScope', '$window', '$location', function loginController($timeout, $rootScope, $window, $location) {
        var self = this;

        provider = new firebase.auth.GoogleAuthProvider();

        firebase.auth().onAuthStateChanged(function(user) {
            if (!user) {
                document.getElementById("authScreen").style.visibility = "visible";
                document.getElementById("loading").style.visibility = "hidden";
            } else {
                document.getElementById("authScreen").style.visibility = "hidden";
                document.getElementById("loading").style.visibility = "visible";
                $rootScope.$apply(function() {
                    $location.path("/choose-music");
                });
            }
        });

        self.signIn = function () {
            firebase.auth().signInWithRedirect(provider);
        };


        firebase.auth().getRedirectResult().then(function(result) {
            if (result.credential) {
                // This gives you a Google Access Token. You can use it to access the Google API.
                var token = result.credential.accessToken;
                // ...
            }

            if (result.user && $location.path() == "/auth") {
                $rootScope.$apply(function() {
                    $location.path("/choose-music");
                    // console.log($location.path());
                });
            }
            var user = result.user;
        }).catch(function(error) {
            // Handle Errors here.
            var errorCode = error.code;
            var errorMessage = error.message;
            // The email of the user's account used.
            var email = error.email;
            // The firebase.auth.AuthCredential type that was used.
            var credential = error.credential;
            // ...
        });

    }]
});