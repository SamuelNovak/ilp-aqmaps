open Array
open Data

let calculate_dist_matrix points =
  let n = length points in
  let matrix = make_matrix n n 0. in
  for i = 0 to n - 1 do
    for j = 0 to n - 1 do
      matrix.(i).(j) <- distance points.(i) points.(j)
    done
  done;
  matrix

let nearest_insert dist =
  [| 0 |]

let optimise seq = seq

let find vl arr =
  let rec find' i vl arr =
    if arr.(i) = vl then i
    else find' (i + 1) vl arr
  in
  find' 0 vl arr

let rotate rot seq = seq

let find_path sensors no_fly_zones start_lat start_lon =
  let sens_pts = List.map (fun s -> s.loc) sensors in
  let init_pt = { lng = start_lon; lat = start_lat } in

  let points = of_list (init_pt :: sens_pts) in
  let dist = calculate_dist_matrix points in

  let seq = nearest_insert dist |> optimise in
  let rot = find 0 seq in
  let seq = append (rotate rot seq) [| 0 |] in

  let waypoints = Array.map (fun x -> points.(x)) seq in
  waypoints
