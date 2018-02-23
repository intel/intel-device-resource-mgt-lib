#!/usr/bin/env python

import getopt
import sys
from coapthon.resources.resource import Resource
from coapthon.server.coap import CoAP
from exampleresources import Hera, Poseidon, Demeter, Hades, Athena, Chaos, Zeus, Zeus1, Zeus2, Zeus3, \
    MultipleEncodingResource
from plugtest_resources import ObservableResource

__author__ = 'giacomo'


class CoAPServer(CoAP):
    def __init__(self, host, port, multicast=False):
        CoAP.__init__(self, (host, port), multicast)
        self.add_resource('Hera/', Hera())
        self.add_resource('Poseidon/', Poseidon())
        self.add_resource('Demeter/', Demeter())
        self.add_resource('Hades/', Hades())
        self.add_resource('Athena/', Athena())
        self.add_resource('Zeus', Zeus())
        self.add_resource('Zeus/10', Zeus1())
        self.add_resource('Zeus/11', Zeus2())
        self.add_resource('Zeus/12', Zeus3())

        print "CoAP Server start on " + host + ":" + str(port)
        print self.root.dump()


def usage():  # pragma: no cover
    print "coapserver.py -i <ip address> -p <port>"


def main(argv):  # pragma: no cover
    ip = "127.0.0.1"
    port = 5683
    try:
        opts, args = getopt.getopt(argv, "hi:p:", ["ip=", "port="])
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            usage()
            sys.exit()
        elif opt in ("-i", "--ip"):
            ip = arg
        elif opt in ("-p", "--port"):
            port = int(arg)

    server = CoAPServer(ip, port)
    try:
        server.listen(10)
    except KeyboardInterrupt:
        print "Server Shutdown"
        server.close()
        print "Exiting..."


if __name__ == "__main__":  # pragma: no cover
    main(sys.argv[1:])
