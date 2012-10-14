# Author: Corin Lawson

$(document).ready (socket) ->
  socket = io.connect()

  socket.on 'Pipe:connect', (data) ->
    $('#receiver').append "<li class='connect'>#{data}</li>"

  socket.on 'ArrayCloseableBlockingQueue:open', (data) ->
    $('#receiver').append "<li class='open'>#{data}</li>"

  socket.on 'ArrayCloseableBlockingQueue:close', (data) ->
    $('#receiver').append "<li class='close'>#{data}</li>"

  socket.on 'IN:put', (data) ->
    $('#receiver').append "<li class='put'>#{data}</li>"

  socket.on 'IN:offer', (data) ->
    $('#receiver').append "<li class='OUT'>#{data}</li>"

  socket.on 'OUT:drain', (data) ->
    $('#receiver').append "<li class='OUT'>#{data}</li>"

  socket.on 'OUT:take', (data) ->
    $('#receiver').append "<li class='OUT'>#{data}</li>"
