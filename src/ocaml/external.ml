type args = { day : int;
              month : int;
              year : int;
              start_lat : float;
              start_lon : float;
            }

let _day = ref 0
let _month = ref 0
let _year = ref 0
let _start_lat = ref 0.
let _start_lon = ref 0.

let usage_msg = "aqmaps -day <day> ..."
let anon_fun x = ()
let speclist = [("-day", Arg.Set_int _day, "Day");
                    ("-month", Arg.Set_int _month, "Month");
                    ("-year", Arg.Set_int _year, "Year");
                    ("-start_lat", Arg.Set_float _start_lat, "Starting latitude");
                    ("-start_lon", Arg.Set_float _start_lon, "Starting longitude")]

let load_args () =
  Arg.parse speclist anon_fun usage_msg;
  { day = !_day; month = !_month; year = !_year;
    start_lat = !_start_lat; start_lon = !_start_lon }

let print_move i (m : Data.move) =
  Printf.printf "[%i] (%f, %f) --(%i)-> (%f, %f)" i m.prev.lng m.prev.lat  m.dir m.next.lng m.next.lat;
  match m.sensor with
  | Val s -> Printf.printf " Sensor: (%f, %f), battery: %f, reading: %f\n" s.loc.lng s.loc.lat s.battery s.reading
  | Empty -> Printf.printf " No sensor\n"

let serialize_moves (moves : Data.move list) =
  let rec iter_moves i mvs =
    match mvs with
    | m :: rest -> print_move i m;
                   iter_moves (i+1) rest
    | [] -> ()
  in
  iter_moves 1 moves
