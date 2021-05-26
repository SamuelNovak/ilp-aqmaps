let () =
  let argv = External.load_args () in
  Printf.printf "Args: Date: %i/%i/%i, Starting position: (lat, lng) = (%f, %f)\n" argv.day argv.month argv.year argv.start_lat argv.start_lon;

  let sensors = Data_client.sensor_list argv.year argv.month argv.day in
  let no_fly_zones = Data_client.no_fly_zones () in

  let waypoints = Planner.find_path sensors no_fly_zones argv.start_lat argv.start_lon in
  let result = Controller.execute_plan no_fly_zones sensors waypoints in

  External.serialize_moves result
