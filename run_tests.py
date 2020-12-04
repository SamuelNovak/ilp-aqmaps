import sys, os
import shlex
import traceback
from subprocess import Popen, PIPE
from math import sqrt

print(sys.argv)
os.chdir("ilp-results")

args = "{day} {month} {year} 55.9444 -3.1878 5678 8888"

year = 2020
path = os.path.join("..", sys.argv[1])

with open("log.txt", "wb") as log:
    for i in range(1, 13):
        day = month = i
        print(f"Running for {day} {month} {year}")
        log.write(f"Log for {day} {month} {year}:\n\n".encode())
        try:
            proc = Popen(shlex.split(f"java -jar {path} " \
                                     + args.format(year=2020,
                                                   month=month,
                                                   day=day)),
                         stdout=PIPE)
            stdout, stderr = proc.communicate(None)
            log.write(stdout)
            if stderr:
                log.write(b"Error:\n")
                log.write(stderr)
            log.write(f"\nEnd of log for {day} {month} {year}\n\n".encode())
        except KeyboardInterrupt:
            print(data)
            sys.exit()
        except:
            print(f"Error on {day} {month} {year}:")
            traceback.print_exc()
