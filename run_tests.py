import sys, os
import shlex
import traceback
from subprocess import Popen, PIPE
from math import sqrt

from argparse import ArgumentParser, ArgumentTypeError

def coords(s):
    try:
        lat, lon = map(float, s.split(","))
        return lat, lon
    except:
        raise ArgumentTypeError("Coordinates must be lat,lon") from None

parser = ArgumentParser()
parser.add_argument("target", type=str)
parser.add_argument("-y", "--year", type=int, default=2020)
parser.add_argument("-p", "--port", type=int, default=8888)
parser.add_argument("-c", "--coords", type=coords, default=(55.944425, -3.188396))
args = parser.parse_args()

os.chdir("ilp-results")

run_args = "{day} {month} " + f"{args.year} {args.coords[0]} {args.coords[1]} 5678 {args.port}"
path = os.path.join("..", sys.argv[1])

with open("log.txt", "wb") as log:
    for i in range(1, 13):
        day = month = str(i).zfill(2)
        print(f"Running for {day} {month} {args.year}")
        log.write(f"Log for {day} {month} {args.year}:\n\n".encode())
        try:
            proc = Popen(shlex.split(f"java -jar {path} " \
                                     + run_args.format(month=month,
                                                       day=day)),
                         stdout=PIPE)
            stdout, stderr = proc.communicate(None)
            log.write(stdout)
            if stderr:
                log.write(b"Error:\n")
                log.write(stderr)
            log.write(f"\nEnd of log for {day} {month} {args.year}\n\n".encode())
        except KeyboardInterrupt:
            print(data)
            sys.exit()
        except:
            print(f"Error on {day} {month} {args.year}:")
            traceback.print_exc()
