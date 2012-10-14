# Avoid `console` errors in browsers that lack a console.
noop = ->
methods = [
  'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error'
  'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log'
  'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd'
  'timeStamp', 'trace', 'warn'
]
length = methods.length
console = window.console or {}
console[methods[length]] = console[methods[length]] or noop while length--

# Place any jQuery/helper plugins in here.
