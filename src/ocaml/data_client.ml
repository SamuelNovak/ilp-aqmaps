open Yojson.Safe
open Yojson.Safe.Util
open Data

let map_filename year month day =
  Printf.sprintf "../../WebServer/maps/%04i/%02i/%02i/air-quality-data.json" year month day;;

let w3w_decode w3 : point =
  let replace = function
    | '.' -> '/'
    | c -> c
  in
  let w3path = "../../WebServer/words/" ^ (String.map replace w3) ^ "/details.json" in
  let json = from_file w3path in
  let coords = json |> member "coordinates" in
  let lng = coords |> member "lng" |> to_float in
  let lat = coords |> member "lat" |> to_float in
  { lng = lng; lat = lat }

let sensor_list year month day =
  let path = map_filename year month day in
  let json = from_file path in
  let lst = json |> to_list in
  List.map (fun x -> { loc = w3w_decode (x |> member "location" |> to_string);
                       battery = x |> member "battery" |> to_float;
                       reading = x |> member "reading" |> to_string |> Float.of_string }) lst

let lat_max = 55.946233
let lat_min = 55.942617
let lng_max = -3.184319
let lng_min = -3.192473
let outside_boundary = List.map point_of_tuple [ (lng_min, lat_min);
                                                 (lng_max, lat_min);
                                                 (lng_max, lat_max);
                                                 (lng_min, lat_max);
                                                 (lng_min, lat_min) ]

let read_point coord : point =
  let plst = coord |> to_list |> List.map to_float in
  let lng = List.nth plst 0 in
  let lat = List.nth plst 1 in
  { lng = lng; lat = lat }

let read_polygon ftr =
  let coords = ftr |> member "geometry" |> member "coordinates" |> to_list in
  let poly = List.nth coords 0 |> to_list in
  List.map read_point poly

let no_fly_zones () =
  let path = "../../WebServer/buildings/no-fly-zones.geojson" in
  let json = from_file path in
  let features = json |> member "features" |> to_list in
  outside_boundary :: List.map read_polygon features
