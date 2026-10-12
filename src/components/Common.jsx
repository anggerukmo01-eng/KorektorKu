import React from 'react';
export function Field({label,children,...props}){return <label className="field"><span>{label}</span>{children||<input {...props}/>}</label>;}
export function Modal({title,children,onClose}){const ref=React.useRef();React.useEffect(()=>{ref.current.showModal();},[]);return <dialog ref={ref} onCancel={onClose}><header><h2>{title}</h2><button aria-label="Tutup" onClick={onClose}>✕</button></header>{children}</dialog>;}
export function Empty({title,children}){return <div className="empty"><div className="empty-symbol">✓</div><h3>{title}</h3><p>{children}</p></div>;}
export function Stat({label,value,detail}){return <div className="stat"><span>{label}</span><strong>{value}</strong>{detail&&<small>{detail}</small>}</div>;}
export function Badge({children,tone=''}){return <span className={'badge '+tone}>{children}</span>;}
export function AutoInput({value,onSave,...props}){const [v,setV]=React.useState(value??'');const focused=React.useRef(false);React.useEffect(()=>{if(!focused.current)setV(value??'');},[value]);return <input {...props} value={v} onFocus={()=>focused.current=true} onBlur={()=>focused.current=false} onChange={e=>{setV(e.target.value);onSave(e.target.value);}}/>;}
export function AnswerButtons({options,value,onChange}){return <div className="answer-buttons" onKeyDown={e=>{const k=e.key.toUpperCase();if(options.includes(k)){e.preventDefault();onChange(k);}}}>{options.map(o=><button type="button" className={value===o?'chosen':''} aria-pressed={value===o} key={o} onClick={()=>onChange(value===o?'':o)}>{o}</button>)}<button type="button" aria-label="Kosongkan jawaban" title="Kosongkan jawaban" onClick={()=>onChange('')}>○</button></div>;}
export const fmt=n=>new Intl.NumberFormat('id-ID',{maximumFractionDigits:2}).format(n??0);
export const typeName=t=>({pg:'Pilihan ganda',fill:'Isian',essay:'Uraian'}[t]);
