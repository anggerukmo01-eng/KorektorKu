import Dexie from 'dexie';
import {names,subjectNames,defaultSettings} from '../data/seed.js';
export const db=new Dexie('koreksiku-v1');
db.version(1).stores({students:'id,number,name,class,active',subjects:'id,name,active',tests:'id,subjectId,class,date',questions:'id,testId,number',answers:'id,testId,studentId,questionId,[testId+studentId]',results:'id,testId,studentId,[testId+studentId]',settings:'id'});
export const uid=()=>crypto.randomUUID();
export async function initialize(){await db.transaction('rw',db.tables,async()=>{if(!await db.settings.get('main')){await db.students.bulkPut(names.map((name,i)=>({id:uid(),name,number:i+1,gender:'',class:'1',active:true,notes:''})));await db.subjects.bulkPut(subjectNames.map(name=>({id:uid(),name,active:true})));await db.settings.put({id:'main',...defaultSettings});}});}
export async function snapshot(){return db.transaction('r',db.tables,async()=>{const data={};for(const t of db.tables)data[t.name]=await t.toArray();return data;});}
export async function saveAnswer(testId,studentId,questionId,patch){
 await db.transaction('rw',[db.answers,db.results],async()=>{const id=`${testId}:${studentId}:${questionId}`;const prev=await db.answers.get(id);await db.answers.put({id,testId,studentId,questionId,answer:'',manualScore:null,...prev,...patch});await db.results.delete(`${testId}:${studentId}`);});
}
export async function saveQuestion(q){await db.transaction('rw',[db.questions,db.results],async()=>{await db.questions.put(q);await db.results.where('testId').equals(q.testId).delete();});}
export async function deleteStudent(id){await db.transaction('rw',[db.students,db.answers,db.results],async()=>{await db.students.delete(id);await db.answers.where('studentId').equals(id).delete();await db.results.where('studentId').equals(id).delete();});}
export async function deleteTest(id){await db.transaction('rw',[db.tests,db.questions,db.answers,db.results],async()=>{await db.tests.delete(id);for(const t of [db.questions,db.answers,db.results])await t.where('testId').equals(id).delete();});}
export async function duplicateTest(test){const id=uid();await db.transaction('rw',[db.tests,db.questions],async()=>{await db.tests.put({...test,id,title:test.title+' (Salinan)'});const qs=await db.questions.where('testId').equals(test.id).toArray();await db.questions.bulkPut(qs.map(q=>({...q,id:uid(),testId:id})));});return id;}
