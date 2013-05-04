# Author: Corin Lawson

@exports ?= window

exports.links =
links = {}

class ManagedObject
  @getInstances = ->
    $.map this, (o) => if o instanceof this then o else null

  @get = (uid) ->
    (if uid instanceof Object and 'uid' of uid
      this[uid.uid]
    else
      this[uid]
    ) or new this(uid)

  @remove = (uid) ->
    if uid instanceof Object and 'uid' of uid
      delete this[uid.uid]
    else
      delete this[uid]

  constructor: (@uid) ->
    this.constructor[@uid] = this

  getId: (that) -> that?.uid or @uid

exports.Individual =
class Individual extends ManagedObject
  constructor: (o) ->
    if o instanceof Object and 'uid' of o
      super o.uid
      {@type, @location, @tick} = o
    else super o

  sendTo: (dest) ->
    unless links["#{@locationi?.uid or 'main'}+#{dest.uid}"]
      links["#{@location?.uid or 'main'}+#{dest.uid}"] = source:(@location or Process.get 'main'), target:dest

    switch dest.constructor
      when Pipe
        @location = dest
      when Process
        @location = dest

class Feedable extends ManagedObject
  @subclasses = []

  constructor: (o) ->
    Feedable.subclasses.push @constructor unless @constructor in Feedable.subclasses

    if o instanceof Object and 'uid' of o
      {feeders} = o
      super o.uid
    else super o

    @feedees = []

  feeder: (f) =>
    unless this in f.feedees
      f.feedees.push this
    f

  feeders: () =>
    @feeder f for f in arguments
    feeders = []

    for sub in Feedable.subclasses
      feeders = feeders.concat sub.getInstances().filter (f)=>
        this in f.feedees
    feeders

  @getAllInstances = ->
    instances = []
    for sub in Feedable.subclasses
      instances = instances.concat sub.getInstances()
    instances

exports.Process =
class Process extends Feedable
  constructor: (o) ->
    if o instanceof Object and 'uid' of o
      super o.uid
    else super o

exports.Pipe =
class Pipe extends Feedable
  constructor: (o) ->
    if o instanceof Object and 'uid' of o
      {@capacity, @closed, @owner} = o
      super o.uid
    else super o
    @capacity or= 16
    @closed or= false

  open: (@capacity) ->

  close: (@closed = true) ->

$ ->
  exports.pipeline =
  pipeline = d3.select('svg#pipeline')

  exports.objects =
  objects = d3.nest().key (d) ->
    d.location?.uid
  map = objects.map Individual.getInstances().reverse()

  exports.layout =
  layout = d3.layout.pack()
    .size([(pipeline.attr 'width'), (pipeline.attr 'height')])
    .padding(1.5)
    .sort(null)
    #.sort (a, b) ->
    #  b.tick - a.tick
    .value (d) -> d.children?.length or 1
  layout = layout
    .children (d) ->
      d.values
  layout = layout
    .children (d) ->
      switch d.constructor
        when Process
          pipes = Pipe.getInstances().filter (p)-> p.owner == d
          for p in pipes
            p.children = p.feedees
          (
            (
              d.feedees if d.feedees.every (p)-> p instanceof Process
            ) or []
          ).concat(pipes)
          .concat (map[d.uid] or [])
        when Pipe
          (d.children or []).concat (map[d.uid] or [])

  exports.circles =
  circles = pipeline
    .append('g')
    .selectAll('circle')

  key = (d) -> d.uid or d.key
  attrs =
    id: key
    r: (d) -> d.r
    cx: (d) -> d.x
    cy: (d) -> d.y
    class: (d) ->
      if d instanceof Individual
        d.type + ' ' +
        (d.location?.constructor.name or '')
  exports.redraw =
  redraw = () ->
    map = objects.map Individual.getInstances().reverse()
    exports.nodes =
    nodes = layout.nodes Process.main
    {
      key:'universe'
      values:objects.entries Individual.getInstances().reverse()
    }

    circles = circles.data nodes, key
    circles.transition(200).attr attrs
    circles.enter()
      .append('circle')
      .attr(attrs)
      .append('title')
      .text key
    circles.exit()
      .remove()

  exports.processes =
  processes = d3.select('svg#processes')

  class Protocol
    socket = io.connect()

    @connect = ->
      console.log 'Connected.'
    socket.on 'connect', @connect

    @join = (pkt) ->
      console.log 'Join', pkt.in, 'to', pkt.out, 'for', pkt.proc, 'by', pkt.thread
    socket.on 'join', @join

    @open = (pkt) ->
      p = Pipe.get uid:pkt.pipe, capacity:pkt.data, owner: Process.get pkt.thread
      p.open pkt.data
    socket.on 'open', @open

    @close = (pkt) ->
      p = Pipe.get pkt.pipe
      p.close()
    socket.on 'close', @close

    @put = (pkt) ->
      $('button').click() if $('#stepthru').attr 'checked'
      i = Individual.get pkt.data
      i.tick = pkt.tick
      p = Pipe.get pkt.pipe
      grandfeeders = (p.feeder Process.get pkt.thread).feeders
      grandfeeders p.owner unless grandfeeders().length
      i.sendTo p
      redraw()
    socket.on 'put', @put
    socket.on 'offer', @put

    @take = (pkt) ->
      $('button').click() if $('#stepthru').attr 'checked'
      p = Process.get pkt.thread
      p.feeder Pipe.get pkt.pipe
      $.each pkt.data, (i, data) ->
        i = Individual.get data
        i.tick = pkt.tick
        i.sendTo p
      redraw()
    socket.on 'drain', @take
    socket.on 'take', @take

    @die = (pkt) ->
      $('button').click() if $('#stepthru').attr 'checked'
      Individual.remove pkt.data
    socket.on 'die', @die

    @pause = ->
      socket.emit 'pause'

    @resume = ->
      socket.emit 'resume'

  $('button').click ->
    button = $ this
    if button.text() == 'Pause'
      Protocol.pause()
      button.text 'Resume'
    else
      Protocol.resume()
      button.text 'Pause'
