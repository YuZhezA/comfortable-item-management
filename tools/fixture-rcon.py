import os, pathlib, socket,struct,sys
root=pathlib.Path(os.environ['COMFORT_QA_SERVER'])
properties=dict(line.split('=',1) for line in (root/'server.properties').read_text().splitlines() if '=' in line)
class Rcon:
    def __enter__(self):
        self.socket=socket.create_connection(('127.0.0.1',int(properties['rcon.port'])),timeout=15)
        self.counter=0
        self.send(properties['rcon.password'],3)
        return self
    def __exit__(self,*args): self.socket.close()
    def read(self,n):
        data=b''
        while len(data)<n:
            more=self.socket.recv(n-len(data))
            if not more: raise EOFError('RCON disconnected')
            data+=more
        return data
    def send(self,command,kind=2):
        self.counter+=1
        data=struct.pack('<ii',self.counter,kind)+command.encode()+b'\0\0'
        self.socket.sendall(struct.pack('<i',len(data))+data)
        while True:
            n=struct.unpack('<i',self.read(4))[0];response=self.read(n)
            identity,response_type=struct.unpack_from('<ii',response)
            if identity==-1: raise RuntimeError('RCON authentication failed')
            if identity==self.counter and (kind!=3 or response_type==2): return response[8:-2].decode()
if __name__=='__main__':
    with Rcon() as rcon:
        for command in sys.argv[1:]: print(rcon.send(command))
