export const normalizeAnswer=v=>String(v??'').normalize('NFKC').trim().replace(/\s+/g,' ').toLocaleLowerCase('id');
export function calculateQuestionScore(q,a={}){
 const text=normalizeAnswer(a.answer); const max=Number(q.maxScore);
 if(!Number.isFinite(max)||max<=0) throw Error('Bobot soal harus lebih besar dari 0.');
 if(q.type==='essay'&&q.mode==='manual'){
  const given=a.manualScore!==null&&a.manualScore!==undefined&&a.manualScore!=='';
  if(!given)return {score:0,status:'pending',isCorrect:false};
  const score=Number(a.manualScore);
  if(!Number.isFinite(score)||score<0||score>max)throw Error(`Skor uraian nomor ${q.number} harus 0–${max}.`);
  return {score,status:score===max?'correct':(!text&&score===0?'blank':'wrong'),isCorrect:score===max};
 }
 if(!text)return {score:0,status:'blank',isCorrect:false};
 let ok=false;
 if(q.type==='pg'){
  if(!q.options.includes(text.toUpperCase())) throw Error(`Jawaban nomor ${q.number} tidak sesuai opsi.`);
  ok=text===normalizeAnswer(q.answerKey);
 } else if(q.type==='fill')ok=(q.acceptedAnswers||[]).some(k=>normalizeAnswer(k)===text);
 else {
  const keys=(q.acceptedAnswers||[]).map(normalizeAnswer).filter(Boolean);
  const hits=keys.filter(k=>(' '+text.replace(/[.,!?;:]/g,' ')+' ').includes(' '+k+' ')).length;
  const score=keys.length?max*hits/keys.length:0;
  return {score,status:score===max?'correct':'wrong',isCorrect:score===max};
 }
 return {score:ok?max:0,status:ok?'correct':'wrong',isCorrect:ok};
}
export function validateKeys(qs){
 if(!qs.length)return ['Tes belum mempunyai soal.'];
 return qs.flatMap(q=>{
 const e=[];
 if(!Number.isFinite(Number(q.maxScore))||Number(q.maxScore)<=0)e.push(`Bobot nomor ${q.number} belum valid.`);
 if(q.type==='pg'&&!q.options.includes(q.answerKey))e.push(`Kunci PG nomor ${q.number} belum tersedia.`);
 if((q.type==='fill'||q.mode==='auto')&&!(q.acceptedAnswers||[]).some(normalizeAnswer))e.push(`Kunci nomor ${q.number} belum tersedia.`);
 return e;
 });
}
export const calculateFinalGrade=(score,max)=>{if(!Number.isFinite(score)||!Number.isFinite(max)||max<=0||score<0||score>max+1e-8)throw Error('Skor tidak valid.');return Math.round(score*10000/max)/100;};
export const calculateTotalScore=details=>details.reduce((s,d)=>s+d.score,0);
export const calculateCorrectAnswers=details=>details.filter(d=>d.status==='correct').length;
export const calculateWrongAnswers=details=>details.filter(d=>d.status==='wrong').length;
export const calculateBlankAnswers=details=>details.filter(d=>d.status==='blank').length;
export const calculateAverage=values=>values.length?values.reduce((s,v)=>s+v,0)/values.length:0;
export function grade(qs,answers){
 const errors=validateKeys(qs);if(errors.length)throw Error(errors.join('\n'));
 const details=qs.map(q=>({questionId:q.id,type:q.type,...calculateQuestionScore(q,answers.find(a=>a.questionId===q.id)||{})}));
 const pending=details.filter(d=>d.status==='pending').length;
 const score=calculateTotalScore(details),maxScore=qs.reduce((s,q)=>s+Number(q.maxScore),0);
 return {details,correct:calculateCorrectAnswers(details),wrong:calculateWrongAnswers(details),blank:calculateBlankAnswers(details),pending,score,maxScore,finalGrade:calculateFinalGrade(score,maxScore),pgScore:calculateTotalScore(details.filter(d=>d.type==='pg')),fillScore:calculateTotalScore(details.filter(d=>d.type==='fill')),essayScore:calculateTotalScore(details.filter(d=>d.type==='essay'))};
}
export function calculateItemAnalysis(qs,results){return qs.map(q=>{const ds=results.map(r=>r.details.find(d=>d.questionId===q.id)).filter(Boolean);return {...q,correct:calculateCorrectAnswers(ds),wrong:calculateWrongAnswers(ds),blank:calculateBlankAnswers(ds),percentage:ds.length?calculateCorrectAnswers(ds)*100/ds.length:0};});}
export const category=(grade,settings)=>[...settings.categories].sort((a,b)=>b.min-a.min).find(c=>grade>=c.min)?.label||'Perlu Bimbingan';
export function parseQuick(text,count,options){const trimmed=text.trim().toUpperCase();const tokens=/[\s,;]/.test(trimmed)?trimmed.split(/[\s,;]+/):trimmed.split('');if(tokens.length!==count)throw Error(`Jawaban yang dimasukkan ${tokens.length}, sedangkan jumlah soal ${count}.`);if(tokens.some(t=>t!=='-'&&!options.includes(t)))throw Error(`Gunakan ${options.join(', ')} atau - untuk kosong.`);return tokens.map(t=>t==='-'?'':t);}
