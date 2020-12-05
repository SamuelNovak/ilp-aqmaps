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

def ranges_numbers(s, admit_month_variable):
    try:
        chunks = s.split(",")
        numbers = []
        for c in chunks:
            if "-" not in c:
                if c == "M" and admit_month_variable:
                    numbers.append(c)
                else:
                    numbers.append(int(c))
            else:
                start, end = c.split("-")
                numbers += list(range(int(start), int(end)+1))
        return numbers
    except:
        raise ArgumentTypeError("Ranges must be of format (R|N)(,(R|N))* where R = (n\\-n) and N = ({integer}|\"M\")")

parser = ArgumentParser()
parser.add_argument("target", type=str)
parser.add_argument("-d", "--days", type=lambda s: ranges_numbers(s, True), default=["M"])
parser.add_argument("-m", "--months", type=lambda s: ranges_numbers(s, False), default=list(range(1,13)))
parser.add_argument("-y", "--year", type=int, default=2020)
parser.add_argument("-p", "--port", type=int, default=8888)
parser.add_argument("-c", "--coords", type=coords, default=(55.944425, -3.188396))
args = parser.parse_args()

print(args)

os.chdir("ilp-results")

run_args = "{day} {month} " + f"{args.year} {args.coords[0]} {args.coords[1]} 5678 {args.port}"
path = os.path.join("..", sys.argv[1])

dates = sorted(list(set([(m if d == "M" else d, m) for d in args.days for m in args.months])), key=lambda x: (x[1],x[0]))

with open("log.txt", "wb") as log:
    for d, m in dates:
        day = str(d).zfill(2)
        month = str(m).zfill(2)
        print(f"Running for {day} {month} {args.year}")
        log.write(f"Log for {day} {month} {args.year}:\n\u2502\n".encode())
        try:
            proc = Popen(shlex.split(f"java -jar {path} " \
                                     + run_args.format(month=month,
                                                       day=day)),
                         stdout=PIPE)
            stdout, stderr = proc.communicate(None)
            log.write(b"\n".join(["\u2502\t".encode() + line for line in stdout.split(b"\n")]))
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
