type colour =
  | Green
  | MediumGreen
  | LightGreen
  | LimeGreen
  | Gold
  | Orange
  | RedOrange
  | Red
  | Black
  | Gray

type symbol =
  | Lighthouse
  | Danger
  | Cross
  | NoSymbol

let colourValue : colour -> string = function
  | Green -> "#00ff00"
  | MediumGreen -> "#40ff00"
  | LightGreen -> "#80ff00"
  | LimeGreen -> "#c0ff00"
  | Gold -> "#ffc000"
  | Orange -> "#ff8000"
  | RedOrange -> "#ff4000"
  | Red -> "#ff0000"
  | Black -> "#000000"
  | Gray -> "#aaaaaa"

let symbolValue : symbol -> string = function
  | Lighthouse -> "lighthouse"
  | Danger -> "danger"
  | Cross -> "cross"
  | NoSymbol -> ""

let pollutionColour p =
  if p < 32 then Green
  else if p < 64 then MediumGreen
  else if p < 96 then LightGreen
  else if p < 128 then LimeGreen
  else if p < 160 then Gold
  else if p < 192 then Orange
  else if p < 224 then RedOrange
  else Red (* assumes pollution < 256 *)

let pollutionSymbol p =
  if p < 128 then Lighthouse
  else Danger

let pollutionColourSymbol p =
  let col = pollutionColour p in
  let sym = pollutionSymbol p in
  col, sym

