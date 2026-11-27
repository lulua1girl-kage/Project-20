import json, os, pathlib, socket, subprocess, sys, tempfile, time, unittest, urllib.request, urllib.error

ROOT=pathlib.Path(__file__).resolve().parents[1]
class LocalApiSmoke(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  s=socket.socket();s.bind(('127.0.0.1',0));cls.port=s.getsockname()[1];s.close()
  cls.tmp=tempfile.TemporaryDirectory();env=os.environ.copy();env['PEXCEPTIONAL_PORT']=str(cls.port);env['PEXCEPTIONAL_DB']=str(pathlib.Path(cls.tmp.name)/'test.db')
  cls.proc=subprocess.Popen([sys.executable,str(ROOT/'backend'/'server.py')],env=env,cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  cls.base=f'http://127.0.0.1:{cls.port}'
  for _ in range(40):
   try:
    with urllib.request.urlopen(cls.base+'/api/health',timeout=.2):break
   except Exception:time.sleep(.1)
  else:raise RuntimeError('local API did not start')
 @classmethod
 def tearDownClass(cls):
  cls.proc.terminate();cls.proc.wait(timeout=5);cls.tmp.cleanup()
 def req(self,path,method='GET',data=None):
  body=json.dumps(data).encode() if data is not None else None
  req=urllib.request.Request(self.base+path,data=body,method=method,headers={'Content-Type':'application/json'})
  try:
   with urllib.request.urlopen(req,timeout=2) as r:return r.status,r.read()
  except urllib.error.HTTPError as e:return e.code,e.read()
 def test_crud_and_invalid_input(self):
  status,b=self.req('/api/tasks','POST',{'title':'test','priority':'Normal'});self.assertEqual(status,201);tid=json.loads(b)['id']
  self.assertEqual(self.req('/api/tasks/'+tid,'PATCH',{'done':True})[0],200)
  self.assertEqual(self.req('/api/tasks/'+tid,'DELETE')[0],204)
  self.assertEqual(self.req('/api/tasks','POST',{'title':''})[0],400)
if __name__=='__main__':unittest.main()
