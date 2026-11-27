import pathlib,re,unittest
from html.parser import HTMLParser
ROOT=pathlib.Path(__file__).resolve().parents[1]
class Parser(HTMLParser):
 def __init__(self):super().__init__();self.ids=set();self.scripts=[]
 def handle_starttag(self,tag,attrs):
  a=dict(attrs)
  if 'id' in a:self.ids.add(a['id'])
  if tag=='script' and a.get('src'):self.scripts.append(a['src'])
class Contract(unittest.TestCase):
 def test_dom_references_resolve(self):
  p=Parser();html=(ROOT/'web'/'index.html').read_text();p.feed(html)
  refs=set(re.findall(r"\$\('([A-Za-z0-9_-]+)'\)",html))
  self.assertFalse(refs-p.ids,sorted(refs-p.ids))
 def test_pwa_files_exist(self):
  p=Parser();p.feed((ROOT/'web'/'index.html').read_text())
  for f in p.scripts:self.assertTrue((ROOT/'web'/f).exists(),f)
  self.assertTrue((ROOT/'web'/'manifest.webmanifest').exists())
if __name__=='__main__':unittest.main()
