let day = ref 0
let month = ref 0
let year = ref 0
let start_lat = ref 0.
let start_lon = ref 0.

let () =
  let usage_msg = "aqmaps -day <day> ..." in
  let anon_fun x = () in
  let speclist = [("-day", Arg.Set_int day, "Day");
                  ("-month", Arg.Set_int month, "Month");
                  ("-year", Arg.Set_int year, "Year");
                  ("-start_lat", Arg.Set_float start_lat, "Starting latitude");
                  ("-start_lon", Arg.Set_float start_lon, "Starting longitude")] in
  Arg.parse speclist anon_fun usage_msg;
  Printf.printf "Args: Date: %i/%i/%i, Starting position: (lat, lng) = (%f, %f)\n" !day !month !year !start_lat !start_lon;

  let sensors = Data_client.sensor_list year month day in
  let no_fly_zones = Data_client.no_fly_zones () in

  let waypoints = Planner.find_path sensors no_fly_zones start_lat start_lon in
  let result = Controller.execute_plan no_fly_zones sensors waypoints in

  let rec iter_moves i (moves : Data.move list) =
    match moves with
    | m :: rest -> begin
        Printf.printf "[%i] (%f, %f) --(%i)-> (%f, %f)" i (fst m.prev) (snd m.prev)  m.dir (fst m.next) (snd m.next);
        let f (mm : Data.move) =
          match mm.sensor with
          | Val s -> Printf.printf " Sensor: (%f, %f), battery: %f, reading: %f" (fst s.loc) (snd s.loc) s.battery s.reading
          | Empty -> print_newline ()
        in f m;
           iter_moves (i + 1) rest
        end
    | [] -> ()
  in
  iter_moves 1 result;
  

