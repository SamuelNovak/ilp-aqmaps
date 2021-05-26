type args = {
    day : int;
    month : int;
    year : int;
    start_lat : float;
    start_lon : float;
  }

val load_args : unit -> args
val serialize_moves : Data.move list -> unit
