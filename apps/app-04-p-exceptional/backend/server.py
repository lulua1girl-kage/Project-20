"""Loopback-only development API for P-Exceptional. Not a production auth service."""
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlparse
import json, os, sqlite3, uuid
from datetime import datetime, timezone

ROOT=Path(__file__).resolve().parents[1]
WEB=ROOT/'web'
DB=Path(os.environ.get('PEXCEPTIONAL_DB',str(ROOT/'backend'/'data'/'tasks.sqlite3')))
MAX_BODY=32768

def dbopen():
 DB.parent.mkdir(parents=True,exist_ok=True)
 db=sqlite3.connect(DB);db.row_factory=sqlite3.Row
 db.execute("""CREATE TABLE IF NOT EXISTS tasks(
 id TEXT PRIMARY KEY,title TEXT NOT NULL CHECK(length(title) BETWEEN 1 AND 180),
 priority TEXT NOT NULL CHECK(priority IN ('Normal','High','Urgent')),
 done INTEGER NOT NULL DEFAULT 0,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)""")
 return db

class Handler(SimpleHTTPRequestHandler):
 def __init__(self,*a,**kw):super().__init__(*a,directory=str(WEB),**kw)
 def end_headers(self):
  self.send_header('X-Content-Type-Options','nosniff')
  self.send_header('Referrer-Policy','no-referrer')
  self.send_header('Permissions-Policy','camera=(), microphone=(), geolocation=()')
  self.send_header('Content-Security-Policy',"default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'")
  super().end_headers()
 def reply(self,status,obj):
  body=json.dumps(obj,separators=(',',':')).encode()
  self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
 def body(self):
  try:n=int(self.headers.get('Content-Length','0'))
  except ValueError:return None
  if n<0 or n>MAX_BODY:return None
  try:v=json.loads(self.rfile.read(n) or b'{}')
  except (ValueError,UnicodeDecodeError):return None
  return v if isinstance(v,dict) else None
 def do_GET(self):
  p=urlparse(self.path).path
  if p=='/api/health':return self.reply(200,{'status':'ok','mode':'local-only','apiVersion':1})
  if p=='/api/tasks':
   with dbopen() as db:rows=db.execute('SELECT * FROM tasks ORDER BY created_at DESC').fetchall()
   return self.reply(200,{'tasks':[dict(x) for x in rows]})
  return super().do_GET()
 def do_POST(self):
  if urlparse(self.path).path!='/api/tasks':return self.reply(404,{'error':'not_found'})
  x=self.body()
  if x is None:return self.reply(400,{'error':'invalid_json_or_body_too_large'})
  title=x.get('title');priority=x.get('priority','Normal')
  if not isinstance(title,str) or not title.strip() or len(title.strip())>180:return self.reply(400,{'error':'invalid_title'})
  if priority not in ('Normal','High','Urgent'):return self.reply(400,{'error':'invalid_priority'})
  now=datetime.now(timezone.utc).isoformat();task={'id':str(uuid.uuid4()),'title':title.strip(),'priority':priority,'done':0,'created_at':now,'updated_at':now}
  with dbopen() as db:db.execute('INSERT INTO tasks VALUES(?,?,?,?,?,?)',tuple(task.values()))
  return self.reply(201,task)
 def do_PATCH(self):
  p=urlparse(self.path).path.strip('/').split('/')
  if len(p)!=3 or p[:2]!=['api','tasks']:return self.reply(404,{'error':'not_found'})
  x=self.body()
  if x is None or not isinstance(x.get('done'),bool):return self.reply(400,{'error':'done_boolean_required'})
  with dbopen() as db:
   cur=db.execute('UPDATE tasks SET done=?,updated_at=? WHERE id=?',(int(x['done']),datetime.now(timezone.utc).isoformat(),p[2]))
   if not cur.rowcount:return self.reply(404,{'error':'task_not_found'})
   row=db.execute('SELECT * FROM tasks WHERE id=?',(p[2],)).fetchone()
  return self.reply(200,dict(row))
 def do_DELETE(self):
  p=urlparse(self.path).path.strip('/').split('/')
  if len(p)!=3 or p[:2]!=['api','tasks']:return self.reply(404,{'error':'not_found'})
  with dbopen() as db:
   cur=db.execute('DELETE FROM tasks WHERE id=?',(p[2],))
   if not cur.rowcount:return self.reply(404,{'error':'task_not_found'})
  self.send_response(204);self.end_headers()
 def log_message(self,fmt,*args):super().log_message(fmt,*args)

def main():
 port=int(os.environ.get('PEXCEPTIONAL_PORT','8765'))
 server=ThreadingHTTPServer(('127.0.0.1',port),Handler)
 print(f'P-Exceptional: http://127.0.0.1:{port} (loopback only)')
 try:server.serve_forever()
 except KeyboardInterrupt:pass
 finally:server.server_close()
if __name__=='__main__':main()
