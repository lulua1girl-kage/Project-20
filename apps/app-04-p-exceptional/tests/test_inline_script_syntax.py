import pathlib,shutil,subprocess,tempfile,unittest
from html.parser import HTMLParser
ROOT=pathlib.Path(__file__).resolve().parents[1]
class Scripts(HTMLParser):
 def __init__(self):super().__init__();self.current=False;self.parts=[];self.items=[]
 def handle_starttag(self,tag,attrs):
  if tag=='script' and not dict(attrs).get('src'):self.current=True;self.parts=[]
 def handle_data(self,data):
  if self.current:self.parts.append(data)
 def handle_endtag(self,tag):
  if tag=='script' and self.current:self.items.append(''.join(self.parts));self.current=False
class InlineSyntax(unittest.TestCase):
 @unittest.skipUnless(shutil.which('node'),'Node.js required for inline JS syntax check')
 def test_inline_scripts_parse(self):
  p=Scripts();p.feed((ROOT/'web'/'index.html').read_text(encoding='utf8'))
  for i,src in enumerate(p.items):
   with tempfile.NamedTemporaryFile('w',suffix='.js',encoding='utf8') as f:
    f.write(src);f.flush();r=subprocess.run(['node','--check',f.name],capture_output=True,text=True)
    self.assertEqual(r.returncode,0,r.stderr)
if __name__=='__main__':unittest.main()
