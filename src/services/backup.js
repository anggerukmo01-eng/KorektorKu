import {db,snapshot} from '../db/database.js';
import {grade} from '../utils/scoring.js';
export async function makeBackup(){return {app:'KOREKSIKU',version:1,createdAt:new Date().toISOString(),data:await snapshot()};}
export function validateBackup(input){
 if(input?.app!=='KOREKSIKU'||input.version!==1||!input.data)throw Error('Format backup KOREKSIKU tidak dikenali.');
 const d=input.data, tables=['students','subjects','tests','questions','answers','results','settings'];
 for(const t of tables){if(!Array.isArray(d[t])||d[t].length>100000)throw Error(`Tabel ${t} tidak valid.`);const ids=new Set();for(const r of d[t]){if(!r||typeof r.id!=='string'||!r.id||ids.has(r.id))throw Error(`ID tabel ${t} tidak valid/duplikat.`);ids.add(r.id);}}
 const students=new Map(d.students.map(s=>[s.id,s])),subjects=new Set(d.subjects.map(s=>s.id)),tests=new Map(d.tests.map(t=>[t.id,t])),questions=new Map(d.questions.map(q=>[q.id,q]));
 for(const s of d.students)if(typeof s.name!=='string'||!s.name.trim()||!Number.isInteger(s.number)||s.number<1||typeof s.class!=='string'||typeof s.active!=='boolean')throw Error('Data siswa tidak valid.');
 for(const s of d.subjects)if(typeof s.name!=='string'||!s.name.trim()||typeof s.active!=='boolean')throw Error('Mata pelajaran tidak valid.');
 for(const t of d.tests)if(!subjects.has(t.subjectId)||typeof t.title!=='string'||!t.title.trim()||typeof t.class!=='string'||typeof t.date!=='string'||typeof t.createdAt!=='string')throw Error('Relasi atau identitas tes tidak valid.');
 const numbers=new Set();
 for(const q of d.questions){const key=`${q.testId}:${q.number}`;if(!tests.has(q.testId)||!['pg','fill','essay'].includes(q.type)||!Number.isFinite(q.maxScore)||q.maxScore<=0||!Number.isInteger(q.number)||q.number<1||numbers.has(key))throw Error('Data soal tidak valid.');numbers.add(key);
 if(q.type==='pg'&&(!Array.isArray(q.options)||q.options.length<2||q.options.length>5||q.options.join('')!=='ABCDE'.slice(0,q.options.length)||typeof q.answerKey!=='string'||(q.answerKey&&!q.options.includes(q.answerKey))))throw Error('Opsi/kunci PG tidak valid.');
 if(q.type!=='pg'&&(!Array.isArray(q.acceptedAnswers)||q.acceptedAnswers.some(k=>typeof k!=='string')))throw Error('Kunci isian/uraian tidak valid.');
 if(q.type==='essay'&&!['auto','manual'].includes(q.mode))throw Error('Mode uraian tidak valid.');}
 for(const a of d.answers){const q=questions.get(a.questionId);if(!students.has(a.studentId)||!q||q.testId!==a.testId||a.id!==`${a.testId}:${a.studentId}:${a.questionId}`||typeof a.answer!=='string')throw Error('Relasi jawaban tidak valid.');if(q.type==='pg'&&a.answer&&!q.options.includes(a.answer))throw Error('Jawaban PG tidak valid.');if(a.manualScore!==null&&a.manualScore!==undefined&&(!Number.isFinite(a.manualScore)||a.manualScore<0||a.manualScore>q.maxScore))throw Error('Skor manual tidak valid.');}
 for(const r of d.results){if(!students.has(r.studentId)||!tests.has(r.testId)||r.id!==`${r.testId}:${r.studentId}`)throw Error('Relasi hasil tidak valid.');const g=grade(d.questions.filter(q=>q.testId===r.testId),d.answers.filter(a=>a.testId===r.testId&&a.studentId===r.studentId));if(g.pending)throw Error('Hasil final mempunyai skor manual belum lengkap.');Object.assign(r,g);}
 const s=d.settings[0];if(d.settings.length!==1||s.id!=='main'||!Array.isArray(s.categories)||!s.categories.length||s.categories.some(c=>typeof c.label!=='string'||!Number.isFinite(c.min)||c.min<0||c.min>100)||!Number.isFinite(s.passing)||s.passing<0||s.passing>100||![s.pgWeight,s.fillWeight,s.essayWeight].every(v=>Number.isFinite(v)&&v>0)||!Number.isFinite(s.medium)||!Number.isFinite(s.easy)||s.medium<0||s.easy>100||s.medium>s.easy)throw Error('Pengaturan backup tidak valid.');
 for(const k of ['school','teacher','class','schoolYear','semester'])if(typeof s[k]!=='string')throw Error('Identitas pengaturan tidak lengkap.');
 if(!['light','dark'].includes(s.theme)||![16,18,20].includes(s.fontSize)||!['normal','compact'].includes(s.density)||typeof s.ranking!=='boolean')throw Error('Pengaturan tampilan tidak valid.');
 return d;
}
export async function restoreBackup(input){const d=validateBackup(input);await db.transaction('rw',db.tables,async()=>{for(const t of db.tables){await t.clear();if(d[t.name].length)await t.bulkPut(d[t.name]);}});}
