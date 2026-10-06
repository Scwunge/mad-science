"""Sends commands to a local test server over RCON. Usage: python scripts/rcon.py <port> <password> <command> [command...]"""
import socket
import struct
import sys


def packet(request_id, kind, body):
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def receive(sock):
    length = struct.unpack("<i", sock.recv(4))[0]
    data = b""
    while len(data) < length:
        data += sock.recv(length - len(data))
    request_id, _ = struct.unpack("<ii", data[:8])
    return request_id, data[8:-2].decode("utf-8", "replace")


def main():
    port, password, commands = int(sys.argv[1]), sys.argv[2], sys.argv[3:]
    with socket.create_connection(("127.0.0.1", port), timeout=10) as sock:
        sock.sendall(packet(1, 3, password))
        if receive(sock)[0] == -1:
            sys.exit("rcon login refused")
        for i, command in enumerate(commands, start=2):
            sock.sendall(packet(i, 2, command))
            print(f"> {command}\n{receive(sock)[1]}")


if __name__ == "__main__":
    main()
