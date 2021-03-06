/*global jQuery: true, angular: true */

'use strict';

$(function() {
  $(document).foundation();
});
var muroApp = angular.module('muroApp', []);
var undefined;

muroApp.directive('formattedTime', function() {
  return {
    scope: {
      formattedTime: "="
    },
    //template: '<span title="{{formatted}}" data-tooltip>{{ago}}</span>',
    template: '<span>{{formatted}}</span>',
    link: function (scope, el, attrs) {
      var timeStr = scope.formattedTime;
      var t = makeTime(timeStr);
      scope.ago = t.ago;
      scope.formatted = t.formatted;
      scope.timestamp = t.timestamp;
      scope.time = timeStr;
    }
  };
});

muroApp.filter('filterLateGuesses', function() {
  return function(items, hide) {
    if (hide) {
      var filtered = [];
      angular.forEach(items, function(guess) {
        if (guess.valid) {
          filtered.push(guess);
        }
      });
      return filtered;
    }
    return items;
  };
});

muroApp.controller("GuessCtrl", function($scope, $http, $timeout) {

  $scope.years = [2016, 2015, 2014, 2013];
  $scope.results = {};
  $scope.sort = {
    column: 'time',
    reverse: true
  };

  $scope.selectYear = function(year) {
    $scope.selectedYear = year;
    //var url = "/guesses";
    //var url = '/assets/mock/guesses.json';
    var url = year === 2016 ? '/guesses' : '/assets/data/'+year+'.json';
    $scope.results = {};
    if (year === 2013) {
      $scope.results = {
        statusText: 'LOPPUSALDO MA 24.6 2013 klo 12.51: M3 + T7 + V6 = 16',
        url: 'http://murobbs.plaza.fi/yleista-keskustelua/1014346-juhannusveikkaus-2013-a.html#post1711131824',
        correctLand: 3,
        correctRoad: 7,
        correctWater: 6
      };
      sortByResult();
    } if (year === 2014) {
      $scope.results = {
        statusText: 'LOPPUSALDO MA 23.6 2014 klo 12: M2 + T2 + V1 = 5',
        url: 'http://murobbs.muropaketti.com/threads/juhannusveikkaus-2014.1117759/#post-1713198286',
        correctLand: 2,
        correctRoad: 2,
        correctWater: 1
      };
    } else if (year === 2015) {
      $scope.results = {
        statusText: 'LOPPUSALDO MA 22.6.2015 klo 15.03: M5 + T2 + V3 = 10',
        url: 'http://murobbs.muropaketti.com/threads/juhannusveikkaus-2015.1221647/#post-1715399487',
        correctLand: 5,
        correctRoad: 2,
        correctWater: 3
      };
    } else {
      initGuess();
    }
    $scope.viewReady = false;
    $http({method: 'GET', url: url}).
        success(function(guesses) {
          $scope.guesses = $.map(guesses, function (guess) {
            guess.count = guess.later.length + 1;
            guess.guessTotal = guess.guess.land + guess.guess.road + guess.guess.water;
            guess.diff = function() {
              var correct = getCorrectGuess();
              if (correct) {
                return Math.abs(correct[0] - guess.guess.land) +
                    Math.abs(correct[1] - guess.guess.road) +
                    Math.abs(correct[2] - guess.guess.water);
              }
              return '';
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
  };

  $scope.selectYear($scope.years[0]);

  function sortByResult() {
    $scope.sort.column = 'diff()';
    $scope.sort.reverse = false;
  }


  $scope.changeUrl = function () {
    if (history && $.isFunction(history.replaceState)) {
      var correct = getCorrectGuess();
      if (correct) {
        try {
          history.replaceState({}, '', '/?' + correct[0] + '-' + correct[1] + '-' + correct[2]);
        } catch (e) {
          // just to be safe...
        }
      }
    }
  };

  function getCorrectGuess() {
    var correctLand = parseInt($scope.results.correctLand);
    var correctRoad = parseInt($scope.results.correctRoad);
    var correctWater = parseInt($scope.results.correctWater);
    if (correctLand >= 0 && correctRoad >= 0 && correctWater >= 0) {
      return [correctLand, correctRoad, correctWater];
    }
    return undefined;
  }

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

  function initGuess() {
    var matches = (/^\?(\d+)-(\d+)-(\d+)$/gi).exec(location.search);
    if (matches && matches.length === 4) {
      $scope.results.correctLand = matches[1];
      $scope.results.correctRoad = matches[2];
      $scope.results.correctWater = matches[3];
      sortByResult();
    }
  }

});

function makeTime(time) {
  var timeMoment = moment(time, "YYYY-MM-DD'T'HH:mmZ");
  return {
    ago: timeMoment.fromNow(),
    //formatted: timeMoment.format("ddd [klo] HH:mm, MMMM Do"),
    formatted: timeMoment.format("dd L HH:mm"),
    timestamp: timeMoment.format("X"),
    time: time
  };
}
