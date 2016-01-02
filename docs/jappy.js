var jappyApp = angular.module('jappyApp', ['ui.router', 'ngMaterial', 'ngMessages']);

jappyApp.factory('menu', function() {
    var sections = [{
        name: 'Getting Started',
        url: 'home',
        type: 'link'
    }];

    sections.push({
        name: 'Documentation',
        type: 'heading',
        children: [
            {
                name : 'Logging',
                url : 'log',
                type : 'link'
            },
            {
                name: 'CSS',
                type: 'toggle',
                pages: [{
                    name: 'Typography',
                    url: 'CSS/typography',
                    type: 'link'
                },
                    {
                        name : 'Button',
                        url: 'CSS/button',
                        type: 'link'
                    },
                    {
                        name : 'Checkbox',
                        url: 'CSS/checkbox',
                        type: 'link'
                    }]
            },
            {
                name: 'Theming',
                type: 'toggle',
                pages: [
                    {
                        name: 'Introduction and Terms',
                        url: 'Theming/01_introduction',
                        type: 'link'
                    },
                    {
                        name: 'Declarative Syntax',
                        url: 'Theming/02_declarative_syntax',
                        type: 'link'
                    },
                    {
                        name: 'Configuring a Theme',
                        url: 'Theming/03_configuring_a_theme',
                        type: 'link'
                    },
                    {
                        name: 'Multiple Themes',
                        url: 'Theming/04_multiple_themes',
                        type: 'link'
                    },
                    {
                        name: 'Under the Hood',
                        url: 'Theming/05_under_the_hood',
                        type: 'link'
                    }
                ]
            }
        ]
    });

    return self = {
        sections: sections,

        selectSection: function(section) {
            self.openedSection = section;
        },
        toggleSelectSection: function(section) {
            self.openedSection = (self.openedSection === section ? null : section);
        },
        isSectionSelected: function(section) {
            return self.openedSection === section;
        },

        selectPage: function(section, page) {
            self.currentSection = section;
            self.currentPage = page;
        },
        isPageSelected: function(page) {
            return self.currentPage === page;
        }
    };
});

jappyApp.controller("RootController", function ($state, $stateParams, $cacheFactory, $scope, $rootScope, menu, $mdSidenav, $timeout) {
    $scope.menu = menu;

    this.autoFocusContent = false;
    this.isSelected = function(s) {
        return menu.isPageSelected(s);
    };
    this.isOpen = function(s) {
        return menu.isSectionSelected(s);
    };
    this.toggleOpen = function(s) {
        menu.toggleSelectSection(s);
    };
    $scope.isSectionSelected = function(section) {
        var selected = false;
        var openedSection = menu.openedSection;
        if(openedSection === section){
            selected = true;
        }
        else if(section.children) {
            section.children.forEach(function(childSection) {
                if(childSection === openedSection){
                    selected = true;
                }
            });
        }
        return selected;
    };

    $scope.closeMenu = function() {
        $timeout(function() { $mdSidenav('left').close(); });
    };

    $scope.openMenu = function() {
        $timeout(function() { $mdSidenav('left').open(); });
    };
});

jappyApp
    .filter('nospace', function () {
        return function (value) {
            return (!value) ? '' : value.replace(/ /g, '');
        };
    });
jappyApp.filter('humanizeDoc', function() {
    return function(doc) {
        if (!doc) return;
        if (doc.type === 'directive') {
            return doc.name.replace(/([A-Z])/g, function($1) {
                return '-'+$1.toLowerCase();
            });
        }
        return doc.label || doc.name;
    };
})

jappyApp.directive('menuLink', function() {
        return {
            scope: {
                section: '='
            },
            templateUrl: 'partials/menu-link.tmpl.html',
            link: function($scope, $element) {
                var controller = $element.parent().controller();

                $scope.isSelected = function() {
                    return controller.isSelected($scope.section);
                };

                $scope.focusSection = function() {
                    // set flag to be used later when
                    // $locationChangeSuccess calls openPage()
                    controller.autoFocusContent = true;
                };
            }
        };
    });

jappyApp.directive('menuToggle', [ '$timeout', function($timeout) {
    return {
        scope: {
            section: '='
        },
        templateUrl: 'partials/menu-toggle.tmpl.html',
        link: function($scope, $element) {
            var controller = $element.parent().controller();

            $scope.isOpen = function() {
                return controller.isOpen($scope.section);
            };
            $scope.toggle = function() {
                controller.toggleOpen($scope.section);
            };
            $scope.$watch(
                function () {
                    return controller.isOpen($scope.section);
                },
                function (open) {
                    var $ul = $element.find('ul');
                    var targetHeight = open ? getTargetHeight() : 0;
                    $timeout(function () {
                        $ul.css({ height: targetHeight + 'px' });
                    }, 0, false);

                    function getTargetHeight () {
                        var targetHeight;
                        $ul.addClass('no-transition');
                        $ul.css('height', '');
                        targetHeight = $ul.prop('clientHeight');
                        $ul.css('height', 0);
                        $ul.removeClass('no-transition');
                        return targetHeight;
                    }
                }
            );


            var parentNode = $element[0].parentNode.parentNode.parentNode;
            if(parentNode.classList.contains('parent-list-item')) {
                var heading = parentNode.querySelector('h2');
                $element[0].firstChild.setAttribute('aria-describedby', heading.id);
            }
        }
    };
}]);



jappyApp.run(function ($rootScope, $state, $stateParams, $http) {
    $rootScope.$state = $state;
    $rootScope.$stateParams = $stateParams;
});

jappyApp.config(function ($stateProvider, $urlRouterProvider, $locationProvider) {
    //$locationProvider.html5Mode(true);
    $urlRouterProvider.otherwise("/getting-started");

    $stateProvider.state('home', {
        controller: "DefaultController",
        url: "/getting-started",
        templateUrl: "getting_started.html"
    });
    $stateProvider.state('log', {
        controller : "DefaultController",
        url : "/logging",
        templateUrl : "logging.html"
    })
});

jappyApp.controller("DefaultController", function($scope) {

});

