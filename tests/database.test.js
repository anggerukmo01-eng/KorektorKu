import 'fake-indexeddb/auto';import test from 'node:test';import assert from 'node:assert/strict';
import {db,initialize,snapshot,saveAnswer,saveQuestion,duplicateTest,deleteTest,deleteStudent} from '../src/db/database.js';
import {makeBackup,restoreBackup,validateBackup} from '../src/services/backup.js';import {grade} from '../src/utils/scoring.js';
test('database: seed, persistensi, backup atomik, relasi, duplikasi, invalidasi',async()=>{
 await initialize();assert.equal(await db.students.count(),20);assert.equal(await db.subjects.count(),5);await initialize();assert.equal(await db.students.count(),20);
 const student=(await db.students.toArray())[0],subject=(await db.subjects.toArray())[0];const t={id:'test',title:'ATS',class:'1',subjectId:subject.id,date:'2026-10-05',createdAt:new Date().toISOString()};const q={id:'q',testId:t.id,number:1,type:'pg',answerKey:'A',options:['A','B','C'],maxScore:1};await db.tests.put(t);await db.questions.put(q);await saveAnswer(t.id,student.id,q.id,{answer:'A'});const g=grade([q],await db.answers.toArray());await db.results.put({id:`test:${student.id}`,testId:t.id,studentId:student.id,...g});
 db.close();await db.open();assert.equal((await db.answers.toArray())[0].answer,'A');assert.equal((await db.results.toArray())[0].finalGrade,100);
 const backup=await makeBackup();const tampered=structuredClone(backup);tampered.data.answers[0].questionId='missing';assert.throws(()=>validateBackup(tampered));const before=await snapshot();await assert.rejects(()=>restoreBackup(tampered));assert.deepEqual(await snapshot(),before);
 const id=await duplicateTest(t);assert.equal(await db.questions.where('testId').equals(id).count(),1);assert.equal(await db.answers.where('testId').equals(id).count(),0);
 await saveQuestion({...q,answerKey:'B'});assert.equal(await db.results.count(),0);assert.equal(await db.answers.count(),1);
 await restoreBackup(backup);assert.equal(await db.results.count(),1);assert.equal(await db.tests.count(),1);
 await deleteStudent(student.id);assert.equal(await db.answers.count(),0);assert.equal(await db.results.count(),0);await deleteTest(t.id);assert.equal(await db.questions.count(),0);await db.delete();
});
