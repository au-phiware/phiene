root = exports ? this

root.index = (socket) ->
  console.log 'Client Connected'

  socket.on 'disconnect', ->
    console.log 'Client Disconnected.'
