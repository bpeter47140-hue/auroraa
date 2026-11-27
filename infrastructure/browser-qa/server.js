import express from 'express';
import { chromium } from 'playwright';
const app=express(); app.use(express.json({limit:'64kb'}));
const token=process.env.BROWSER_QA_TOKEN||'';
app.get('/health',(_req,res)=>res.json({status:'ok'}));
app.post('/run',async(req,res)=>{
 if(!token||req.get('authorization')!==`Bearer ${token}`) return res.status(401).json({error:'Unauthorized'});
 const {url}=req.body||{};
 if(typeof url!=='string'||!url.startsWith('http://backend:8080/api/v1/projects/')) return res.status(400).json({error:'URL not in preview allowlist'});
 const report={url,startedAt:new Date().toISOString(),pages:[],issues:[],checks:[],screenshots:{},status:'FAILED'};
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH||'/usr/bin/chromium',args:['--no-sandbox','--disable-dev-shm-usage']});
  const context=await browser.newContext({viewport:{width:1365,height:768},ignoreHTTPSErrors:false});
  const page=await context.newPage(); page.setDefaultTimeout(5000);
  const origin=new URL(url).origin;
  await context.route('**/*',route=>{const u=new URL(route.request().url()); if(u.origin!==origin) return route.abort('blockedbyclient'); return route.continue();});
  page.on('pageerror',e=>report.issues.push({severity:'high',type:'javascript-error',message:e.message}));
  page.on('console',m=>{if(m.type()==='error')report.issues.push({severity:'medium',type:'console-error',message:m.text().slice(0,500)});});
  page.on('requestfailed',r=>report.issues.push({severity:'medium',type:'request-failed',url:r.url().slice(0,500),reason:r.failure()?.errorText||'unknown'}));
  page.on('response',r=>{if(r.status()>=400)report.issues.push({severity:'high',type:'http-error',status:r.status(),url:r.url().slice(0,500)});});
  const resp=await page.goto(url,{waitUntil:'domcontentloaded',timeout:25000});
  report.checks.push({name:'initial-page-load',passed:!!resp&&resp.status()<400,status:resp?.status()??null});
  report.pages.push({url:page.url(),title:await page.title(),status:resp?.status()??null});
  report.screenshots.desktop=await page.screenshot({fullPage:true,type:'jpeg',quality:55,timeout:10000}).then(b=>b.toString('base64'));
  const links=await page.locator('a[href]').evaluateAll(as=>as.map(a=>({text:(a.innerText||a.textContent||'').trim().slice(0,120),href:a.href})).slice(0,60));
  report.checks.push({name:'navigation-links-discovered',count:links.length});
  const sameOrigin=links.filter(l=>l.href.startsWith(new URL(url).origin));
  await Promise.all(sameOrigin.slice(0,12).map(async l=>{try{const r=await context.request.get(l.href,{timeout:3000});if(r.status()>=400)report.issues.push({severity:'high',type:'broken-internal-link',href:l.href,status:r.status()});}catch(e){report.issues.push({severity:'medium',type:'link-check-failed',href:l.href,message:e.message.slice(0,180)});}}));
  const btns=await page.locator('button').count(),forms=await page.locator('form').count(),inputs=await page.locator('input,textarea,select').count();
  report.checks.push({name:'interactive-elements-inventory',buttons:btns,forms,fields:inputs});
  const unlabeled=await page.locator('button').evaluateAll(bs=>bs.filter(b=>!(b.innerText||b.getAttribute('aria-label')||b.title).trim()).length);
  if(unlabeled)report.issues.push({severity:'medium',type:'unlabelled-buttons',count:unlabeled});
  const images=await page.locator('img').evaluateAll(xs=>xs.filter(i=>!i.alt?.trim()).length); if(images)report.issues.push({severity:'low',type:'images-missing-alt',count:images});
  await page.setViewportSize({width:390,height:844}); await page.waitForTimeout(500);
  const overflow=await page.evaluate(()=>({scrollWidth:document.documentElement.scrollWidth,innerWidth:innerWidth}));
  report.checks.push({name:'mobile-overflow',passed:overflow.scrollWidth<=overflow.innerWidth,...overflow});
  if(overflow.scrollWidth>overflow.innerWidth)report.issues.push({severity:'high',type:'mobile-horizontal-overflow',...overflow});
  report.screenshots.mobile=await page.screenshot({fullPage:true,type:'jpeg',quality:55,timeout:10000}).then(b=>b.toString('base64'));
  report.checks.push({name:'mobile-rendered',passed:true,viewport:'390x844'});
  report.status=report.issues.some(i=>i.severity==='critical'||i.severity==='high')?'NEEDS_FIXES':'REVIEW';
 }catch(e){report.issues.push({severity:'critical',type:'browser-run-failure',message:e.message.slice(0,500)});}
 finally{if(browser)await browser.close();}
 report.finishedAt=new Date().toISOString(); report.screenshots=Object.fromEntries(Object.entries(report.screenshots).map(([k,v])=>[k,{mimeType:'image/png',data:v}]));res.json(report);
});
app.listen(8091,'0.0.0.0',()=>console.log('Browser QA listening on 8091'));
