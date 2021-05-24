type point = float * float

type sensor_reading = {
    loc : point;
    battery : float;
    reading : float;
  }

type 'a maybe = Val of 'a | Empty

type move = {
    prev : point;
    next : point;
    dir : int;
    sensor : sensor_reading maybe
  }
