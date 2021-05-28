type point = { lng : float;
               lat : float }

let point_of_tuple x = { lng = fst x; lat = snd x }
let point_to_tuple x = (x.lng, x.lat)
let distance x y = Float.hypot (x.lng -. y.lng) (x.lat -. y.lat)

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

(* type polygon = point list *)

let print_list lst =
  let open Printf in
  let open List in
  printf "[ ";
  iter (fun x -> printf "%d " x) lst;
  printf "]\n"
