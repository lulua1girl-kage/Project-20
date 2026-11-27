const test=require('node:test'),assert=require('node:assert/strict');
const {parseBackup}=require('../web/backup.js');
const now='2026-11-27T12:00:00.000Z';
test('valid backup is normalized and fields are allowlisted',()=>{const [t]=parseBackup({app:'P-Exceptional',tasks:[{id:'x',title:' Task ',priority:'high',extra:'drop'}]},[],now);assert.equal(t.title,'Task');assert.equal(t.extra,undefined);assert.equal(t.day,'2026-11-27')});
test('duplicate ids and invalid titles are skipped',()=>assert.deepEqual(parseBackup({app:'P-Exceptional',tasks:[{id:'x',title:'ok'},{id:'x',title:'dup'},{title:'  '}]},['x'],now),[]));
test('invalid format and over-cap list rejected',()=>{assert.throws(()=>parseBackup({app:'bad',tasks:[]},[],now));assert.throws(()=>parseBackup({app:'P-Exceptional',tasks:Array(10001).fill({title:'x'})},[],now))});
test('bad priority and due date normalized',()=>{const [t]=parseBackup({app:'P-Exceptional',tasks:[{title:'x',priority:'admin',dueAt:'bad'}]},[],now);assert.equal(t.priority,'normal');assert.equal(t.dueAt,null)});
