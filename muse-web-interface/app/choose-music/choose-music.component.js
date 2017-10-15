angular.module('chooseMusic').component('chooseMusic', {
    templateUrl: 'choose-music/choose-music.template.html',

    controller: ['$routeParams', '$route', '$firebaseObject', '$firebaseArray', function chooseMusicController($routeParams, $route, $firebaseObject, $firebaseArray) {
        var self = this;
        var user = firebase.auth().currentUser;
        self.chooseMusicRef = firebase.database().ref().child("choose-music");
        // self.chooseMusicRef.set("hello");

        self.happyMusicArray = $firebaseArray(self.chooseMusicRef.child("happy"));
        self.sadMusicArray = $firebaseArray(self.chooseMusicRef.child("sad"));
        self.angryMusicArray = $firebaseArray(self.chooseMusicRef.child("angry"));
        self.relaxedMusicArray = $firebaseArray(self.chooseMusicRef.child("relaxed"));
        self.neutralMusicArray = $firebaseArray(self.chooseMusicRef.child("neutral"));

        self.addSong = function(songtype, songLink) {
          if (songLink && songLink.length > 0) {
            self.chooseMusicRef.child(songtype).child(songLink.split('/')[songLink.split('/').length - 1]).set({
              link: songLink
            });
          }
        };

        self.deleteSong = function(songtype, songLink) {
            self.chooseMusicRef.child(songtype).child(songLink.split('/')[songLink.split('/').length - 1]).set(null);
        };

    }]
});
