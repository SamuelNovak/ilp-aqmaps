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

let rec complement n seq =
  if n < 0 then []
  else
    match List.find_opt (fun x -> x = n) seq with
    | Some _ -> complement (n - 1) seq
    | None -> n :: complement (n - 1) seq

let rec insert position item lst =
  match lst with
  | [] -> [ item ]
  | h :: t -> if position = 0 then item :: h :: t
              else h :: insert (position - 1) item t

let rec ni_fill (dist : float array array) (seq : int list) (unused : int list) =
  match unused with
  | [] -> seq
  | _ :: _ ->
     (* Printf.printf "Unused: ";
      * print_list unused; *)
     let open List in
     let min_pair = ref ((0, hd unused) , dist.(hd seq).(hd unused)) in
     iter (fun u ->
         iteri (fun i v ->
             let dst = dist.(v).(u) in
             if dst < snd !min_pair
             then min_pair := ((i, u), dst))
           seq)
       unused;
     let pair' = fst !min_pair in
     let i = fst pair' in
     let u = snd pair' in
     let seqlen = List.length seq in
     let i_plus = (i + 1 + seqlen) mod seqlen in
     let i_minus = (i - 1 + seqlen) mod seqlen in
     (* Printf.printf "n = %d; i = %d -> %d; u = %d; i+ = %d; i- = %d\n" n i (nth seq i) u i_plus i_minus;
      * print_list seq; *)
     let d_plus = dist.(nth seq i_plus).(u) in
     let d_minus = dist.(nth seq i_minus).(u) in
     let new_seq =
       if d_plus < d_minus then insert i u seq
       else insert i_plus u seq
     in
     let new_unused = filter (fun x -> x <> u) unused in
     ni_fill dist new_seq new_unused

let nearest_insert (dist : float array array) =
  let n = length dist in
  let m' = dist.(0).(1) in
  let min_pair = ref ((0, 1), m') in
  for i = 0 to n - 1 do
    for j = i + 1 to n - 1 do
      if dist.(i).(j) < snd !min_pair
      then min_pair := ((i,j), dist.(i).(j))
    done
  done;

  let pair' = fst !min_pair in
  let seq = [ fst pair'; snd pair' ] in
  let final = ni_fill dist seq (complement (n - 1) seq) in
  print_list final;
  Array.of_list final

let optimise (seq : int array) = seq

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
