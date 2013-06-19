/*global jQuery: true, angular: true */

'use strict';

$(document).foundation('tooltips');
var muroApp = angular.module('muroApp', []);

muroApp.directive('formattedTime', function() {
  return {
    scope: {
      formattedTime: "="
    },
    template: '<span title="{{formatted}}" data-tooltip>{{ago}}</span>',
    link: function (scope, el, attrs) {
      var timeStr = scope.formattedTime
      var t = makeTime(timeStr);
      scope.ago = t.ago;
      scope.formatted = t.formatted;
      scope.timestamp = t.timestamp;
      scope.time = timeStr;
    }
  };
});

muroApp.controller("GuessCtrl", function($scope, $http) {

  var url = "/guesses";
  //var url = '/assets/mock/guesses.json';


  $http({method: 'GET', url: url}).
      success(function(guesses) {
        $scope.guesses = $.map(guesses, function (guess) {
          guess.count = guess.later.length + 1;
          guess.diff = function() {
            var correctLand = parseInt($scope.correctLand);
            var correctRoad = parseInt($scope.correctRoad);
            var correctWater = parseInt($scope.correctWater);
            if (correctLand >= 0 && correctRoad >= 0 && correctWater >= 0) {
              return Math.abs(correctLand - guess.guess.land) +
                  Math.abs(correctRoad - guess.guess.road) +
                  Math.abs(correctWater - guess.guess.water);
            }
            return "";
          };
          guess.laterTooltip = $.map(guess.later, function(el) {
            return '<div>' + el.user + ' <small>(' + el.delay + ' myöhemmin)</small></div>';
          }).join('');
          return guess;
        });
        $scope.viewReady = true;
      }).
      error(function(data, status, headers, config) {
        alert ('OMG. VIRHE!');
      });


  $scope.sort = {
    column: 'time',
    reverse: true
  };

  $scope.selectedClass = function(column) {
    var cls = "table-sort"
    if (column == $scope.sort.column) {
      cls += $scope.sort.reverse ? '-up' : '-down';
    }
    return cls;
  };

  $scope.changeSorting = function(column) {
    var sort = $scope.sort;
    if (sort.column == column) {
      sort.reverse = !sort.reverse;
    } else {
      sort.column = column;
      sort.reverse = (column === 'user' || column === 'diff()') ? false : true;
    }
  };

});

function makeTime(time) {
  var timeMoment = moment(time, "YYYY-MM-DD'T'HH:mmZ");
  return {
    ago: timeMoment.fromNow(),
    formatted: timeMoment.format("ddd [klo] HH:mm, MMMM Do"),
    timestamp: timeMoment.format("X"),
    time: time
  };
}
