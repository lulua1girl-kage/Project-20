/* Safe, dependency-free backup validation. Imported strings are data, never markup. */
(function(root){
 function parseBackup(payload, existingIds, nowISO){
  if(!payload||payload.app!=='P-Exceptional'||!Array.isArray(payload.tasks)||payload.tasks.length>10000)throw new Error('Backup format not recognized.');
  const seen=new Set(existingIds||[]),clean=[],now=nowISO||new Date().toISOString();
  for(const raw of payload.tasks){
   if(!raw||typeof raw.title!=='string'||!raw.title.trim()||raw.title.trim().length>180)continue;
   let id=typeof raw.id==='string'&&raw.id.length>0&&raw.id.length<=100?raw.id:'';
   if(!id)id=(root.crypto&&root.crypto.randomUUID)?root.crypto.randomUUID():'restore-'+Date.now()+'-'+clean.length;
   if(seen.has(id))continue;seen.add(id);
   const priority=['normal','high','urgent'].includes(raw.priority)?raw.priority:'normal';
   const dueAt=typeof raw.dueAt==='string'&&!Number.isNaN(Date.parse(raw.dueAt))?raw.dueAt:null;
   clean.push({id,title:raw.title.trim(),priority,done:raw.done===true,dueAt,
    createdAt:typeof raw.createdAt==='string'?raw.createdAt:now,
    completedAt:typeof raw.completedAt==='string'?raw.completedAt:null,
    day:/^\\d{4}-\\d{2}-\\d{2}$/.test(raw.day||'')?raw.day:now.slice(0,10)});
  }
  return clean;
 }
 const api={parseBackup};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 root.PExceptionalBackup=api;
})(typeof window!=='undefined'?window:globalThis);
