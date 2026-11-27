package com.aurora.projects;

import com.aurora.audit.AuditService;
import com.aurora.agents.AgentExecutionService;
import com.aurora.agents.AgentRun;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

@Service
public class ProjectWorkspaceService {
 private final VentureProjectRepository projects;
 private final ProjectArtifactRepository artifacts;
 private final ProjectArtifactVersionRepository versions;
 private final AuditService audit;
 private final AgentExecutionService agentExecution;
 private final ObjectMapper json;
 @Value("${aurora.projects.storage-path:./data/project-workspaces}") private String storagePath;
 public record WorkspaceFile(String path,String content,long sizeBytes){}
 public ProjectWorkspaceService(VentureProjectRepository projects,ProjectArtifactRepository artifacts,ProjectArtifactVersionRepository versions,AuditService audit,AgentExecutionService agentExecution,ObjectMapper json){this.projects=projects;this.artifacts=artifacts;this.versions=versions;this.audit=audit;this.agentExecution=agentExecution;this.json=json;}
 private VentureProject project(Long id){return projects.findByIdAndWorkspace_Id(id,WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found."));}
 private Path root(Long workspace,Long project){return Paths.get(storagePath).toAbsolutePath().normalize().resolve("ws-"+workspace).resolve("project-"+project).normalize();}
 private Path safe(Path root,String rel){Path p=root.resolve(rel).normalize();if(!p.startsWith(root)||Files.isSymbolicLink(root))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsafe path.");Path cursor=root;for(Path part:root.relativize(p)){cursor=cursor.resolve(part);if(Files.isSymbolicLink(cursor))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Symbolic links are not permitted in project workspaces.");}return p;}
 private boolean ignoredWorkspacePath(String rel){return Arrays.stream(rel.replace(File.separatorChar,'/').split("/")).anyMatch(part->Set.of("node_modules","dist","build","coverage",".git",".vite").contains(part)||part.endsWith(".aurora-tmp"));}
 private String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;").replace("{","&#123;").replace("}","&#125;");}
 @Transactional public List<ProjectArtifact> scaffold(Long id){
  VentureProject p=project(id);Long ws=WorkspaceContextHolder.id();Path root=root(ws,id);
  if(Files.exists(root.resolve("package.json"))){
   List<ProjectArtifact> registered=list(id);
   if(!registered.isEmpty())return registered;
   // Recover metadata if the durable files survived a database restore/recreation.
   try(var walk=Files.walk(root)){for(Path f:walk.filter(Files::isRegularFile).filter(x->!Files.isSymbolicLink(x)).filter(x->!ignoredWorkspacePath(root.relativize(x).toString())).toList()){
    String rel=root.relativize(f).toString().replace(File.separatorChar,'/');
    String type=rel.endsWith(".md")?"DOCUMENT":"SOURCE";
    ProjectArtifact recovered=artifacts.save(new ProjectArtifact(WorkspaceContextHolder.require(),p,type,rel,rel,"Recovered workspace file",Files.size(f)));
    snapshot(recovered,Files.readString(f),"system-recovery");
   }}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not recover workspace metadata.");}
   return list(id);
  }
  String name=esc(p.getName()),goal=esc(p.getGoal());
  String slug=p.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");
  if(slug.isBlank())slug="aurora-project";
  Map<String,String> files=new LinkedHashMap<>();
  files.put("package.json","{\"name\":\""+slug+"\",\"version\":\"0.1.0\",\"private\":true,\"type\":\"module\",\"scripts\":{\"dev\":\"vite\",\"build\":\"tsc -b && vite build\",\"preview\":\"vite preview\"},\"dependencies\":{\"react\":\"^19.0.0\",\"react-dom\":\"^19.0.0\"},\"devDependencies\":{\"@vitejs/plugin-react\":\"^4.4.1\",\"vite\":\"^6.0.0\",\"typescript\":\"^5.7.2\",\"@types/react\":\"^19.0.0\",\"@types/react-dom\":\"^19.0.0\"}}\n");
  files.put("index.html","<!doctype html>\n<html lang=\"en\"><head><meta charset=\"UTF-8\"/><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"/><meta name=\"description\" content=\""+goal+"\"/><title>"+name+"</title></head><body><div id=\"root\"></div><script type=\"module\" src=\"/src/main.tsx\"></script></body></html>\n");
  files.put("vite.config.ts","import { defineConfig } from 'vite'\nimport react from '@vitejs/plugin-react'\nexport default defineConfig({ base: './', plugins: [react()] })\n");
  files.put("tsconfig.json","{\"compilerOptions\":{\"target\":\"ES2020\",\"useDefineForClassFields\":true,\"lib\":[\"ES2020\",\"DOM\",\"DOM.Iterable\"],\"module\":\"ESNext\",\"skipLibCheck\":true,\"moduleResolution\":\"Bundler\",\"allowImportingTsExtensions\":true,\"resolveJsonModule\":true,\"isolatedModules\":true,\"noEmit\":true,\"jsx\":\"react-jsx\",\"strict\":true},\"include\":[\"src\"]}\n");
  files.put("src/main.tsx","import React from 'react'\nimport { createRoot } from 'react-dom/client'\nimport App from './App'\nimport './style.css'\ncreateRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>)\n");
  files.put("src/App.tsx","export default function App(){return <main><header><b className=\"mark\">A</b>"+name+"</header><section className=\"hero\"><p className=\"eyebrow\">PROJECT STARTER</p><h1>"+name+"</h1><p className=\"lead\">"+goal+"</p><a className=\"cta\" href=\"mailto:hello@example.com\">Get in touch ↗</a></section><footer>Starter workspace generated by Aurora. Replace the copy, imagery and links before launch.</footer></main>}\n");
  files.put("src/style.css",":root{font-family:ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;color:#17231c;background:#f4f5f0}*{box-sizing:border-box}body{margin:0;min-width:320px}main{max-width:1120px;margin:auto;padding:40px 7vw}header{display:flex;align-items:center;gap:12px;font-weight:600}.mark{width:34px;height:34px;border-radius:10px;background:#1d3528;color:white;display:grid;place-items:center}.hero{min-height:68vh;display:flex;flex-direction:column;justify-content:center;max-width:780px}.eyebrow{font-size:11px;letter-spacing:.18em;color:#54715e;font-weight:700}.hero h1{font-family:ui-sans-serif,system-ui,sans-serif;font-size:clamp(42px,8vw,88px);line-height:1.03;letter-spacing:-.06em;margin:16px 0}.lead{font-size:clamp(17px,2.3vw,22px);line-height:1.65;color:#526257;max-width:650px}.cta{display:inline-block;width:max-content;margin-top:28px;padding:14px 20px;border-radius:8px;background:#1d3528;color:white;text-decoration:none;font-weight:600}footer{border-top:1px solid #d9ded6;padding-top:18px;color:#7d897e;font-size:12px}@media(max-width:600px){main{padding:28px 6vw}}\n");
  files.put("README.md","# "+p.getName()+"\n\nReact + TypeScript starter generated by Aurora.\n\nGoal: "+p.getGoal()+"\n\n## Run locally\nInstall Node.js 20+ then run `npm install` and `npm run dev`.\n\n## Build\nRun `npm run build`. Review the generated site before deploying.\n");
  try{Files.createDirectories(root);for(var entry:files.entrySet()){Path target=safe(root,entry.getKey());Files.createDirectories(target.getParent());Files.writeString(target,entry.getValue(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);ProjectArtifact created=artifacts.save(new ProjectArtifact(WorkspaceContextHolder.require(),p,entry.getKey().equals("README.md")?"DOCUMENT":"SOURCE",entry.getKey(),entry.getKey(),"Aurora React starter file",Files.size(target)));snapshot(created,entry.getValue(),"aurora-scaffold");}}
  catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Failed to create starter workspace.");}
  audit.record("PROJECT_WORKSPACE_CREATED","PROJECT",id.toString(),"Created starter workspace with "+files.size()+" files.");
  return list(id);
 }
 public List<ProjectArtifact> list(Long id){project(id);return artifacts.findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(id,WorkspaceContextHolder.id()).stream().filter(a->!ignoredWorkspacePath(a.getRelativePath())).toList();}
 public record GenerationResult(long agentRunId,int filesWritten,List<WorkspaceFile> files){}
 public GenerationResult generate(Long projectId,String brief){
  VentureProject p=project(projectId);if(brief==null||brief.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide a development brief.");
  if(brief.length()>5000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Development brief is too long.");
  Path root=root(WorkspaceContextHolder.id(),projectId);if(!Files.exists(root.resolve("package.json")))scaffold(projectId);
  String prompt="PROJECT NAME: "+p.getName()+"\nPROJECT GOAL: "+p.getGoal()+"\nUSER REQUEST: "+brief+"\n\nGenerate a polished responsive website frontend using the existing React+TypeScript scaffold. Return JSON file entries only. Include App.tsx and style.css; add component files if useful. Do not change packages, configuration, entrypoint or backend.";
  AgentRun run;try{run=agentExecution.execute("developer-agent",prompt);}catch(IllegalStateException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI provider is not configured for website generation.");}
  if(!"SUCCEEDED".equals(run.getStatus()))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Code generation failed. Check Agent Runs.");
  String output=run.getOutput();if(output==null||output.length()>220000)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Developer agent response was too large.");
  try{
   String fence=String.valueOf((char)96).repeat(3);String clean=output.trim();
   if(clean.startsWith(fence)){int first=clean.indexOf('\n');int last=clean.lastIndexOf(fence);if(first>=0&&last>first)clean=clean.substring(first+1,last).trim();}
   JsonNode filesNode=json.readTree(clean).get("files");if(filesNode==null||!filesNode.isArray()||filesNode.isEmpty()||filesNode.size()>8)throw new IllegalArgumentException("Expected 1–8 file entries.");
   int total=0,written=0;Set<String> seen=new HashSet<>();
   for(JsonNode node:filesNode){String rel=node.path("path").asText("");String source=node.path("content").asText(null);
    if(source==null||source.isBlank()||source.length()>50000)throw new IllegalArgumentException("Invalid/oversized file content.");
    total+=source.length();if(total>180000)throw new IllegalArgumentException("Generated files exceed total size limit.");
    if(!rel.startsWith("src/")||rel.startsWith("/")||rel.indexOf(92)>=0||Arrays.asList(rel.split("/")).contains("..")||!(rel.endsWith(".tsx")||rel.endsWith(".ts")||rel.endsWith(".css")||rel.endsWith(".svg")||rel.endsWith(".md")))throw new IllegalArgumentException("File path not permitted.");
    if(!(rel.equals("src/App.tsx")||rel.equals("src/style.css")||rel.startsWith("src/components/")||rel.startsWith("src/pages/")))throw new IllegalArgumentException("File outside permitted frontend folders.");
    if(!seen.add(rel))throw new IllegalArgumentException("Duplicate path.");
    Path file=safe(root,rel);Files.createDirectories(file.getParent());if(Files.isRegularFile(file)){ProjectArtifact prior=artifacts.findByProject_IdAndWorkspace_IdAndRelativePath(projectId,WorkspaceContextHolder.id(),rel).orElse(null);if(prior!=null)snapshot(prior,Files.readString(file),"before-ai-generation");}Path temp=file.resolveSibling(file.getFileName()+".aurora-tmp");Files.writeString(temp,source,StandardCharsets.UTF_8);try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
    ProjectArtifact artifact=artifacts.findByProject_IdAndWorkspace_IdAndRelativePath(projectId,WorkspaceContextHolder.id(),rel).orElse(null);
    if(artifact==null)artifacts.save(new ProjectArtifact(WorkspaceContextHolder.require(),p,"AI_SOURCE",rel,rel,"AI-generated source; not yet built or security-reviewed",Files.size(file)));
    else{artifact.updateSize(Files.size(file));artifacts.save(artifact);}ProjectArtifact saved=artifacts.findByProject_IdAndWorkspace_IdAndRelativePath(projectId,WorkspaceContextHolder.id(),rel).orElseThrow();snapshot(saved,source,"developer-agent");written++;
   }
   audit.record("PROJECT_CODE_GENERATED","PROJECT",projectId.toString(),"Developer agent generated "+written+" frontend source files. Not executed.");
   return new GenerationResult(run.getId(),written,readFiles(projectId));
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Developer agent returned invalid source data. No deployment was performed.");}
 }
 public ProjectArtifact saveAgentOutput(Long projectId,Long workflowId,int stageOrder,String stageTitle,String markdown){
  VentureProject p=project(projectId);Path root=root(WorkspaceContextHolder.id(),projectId);
  String slug=stageTitle.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");
  String relative="agent-outputs/run-"+workflowId+"/stage-"+stageOrder+"-"+slug+".md";
  try{Path file=safe(root,relative);Files.createDirectories(file.getParent());Files.writeString(file,markdown,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
   ProjectArtifact a=artifacts.save(new ProjectArtifact(WorkspaceContextHolder.require(),p,"AGENT_OUTPUT",stageTitle,relative,"Workflow agent output: "+stageTitle,Files.size(file)));
   audit.record("PROJECT_AGENT_OUTPUT_SAVED","PROJECT_ARTIFACT",a.getId().toString(),"Saved workflow output as a project artifact.");return a;
  }catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not save agent output as project artifact.");}
 }
 public List<WorkspaceFile> readFiles(Long id){project(id);Path root=root(WorkspaceContextHolder.id(),id);if(!Files.exists(root))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workspace has not been scaffolded.");List<WorkspaceFile> out=new ArrayList<>();for(ProjectArtifact a:list(id)){try{Path f=safe(root,a.getRelativePath());if(Files.isRegularFile(f)){long size=Files.size(f);String text=size>300_000?"[File is too large to preview]":Files.readString(f);out.add(new WorkspaceFile(a.getRelativePath(),text,size));}}catch(IOException ignored){}}return out;}
 public byte[] readPreview(Long workspaceId,Long projectId,String relative){
  Path projectRoot=root(workspaceId,projectId);Path previewRoot=projectRoot.resolve("preview").normalize();
  if(!previewRoot.startsWith(projectRoot)||Files.isSymbolicLink(previewRoot))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Preview not found.");
  Path file=safe(projectRoot,"preview/"+relative);
  if(!file.startsWith(previewRoot)||Files.isSymbolicLink(file)||!Files.isRegularFile(file))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Preview file not found.");
  try{long size=Files.size(file);if(size>10_000_000)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Preview file is too large.");return Files.readAllBytes(file);}
  catch(IOException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Preview file not found.");}
 }

 public record SaveFileRequest(String content){}
 public record ArtifactVersionSummary(Long id,Long artifactId,int versionNumber,String sha256,String createdBy,java.time.Instant createdAt){}
 public record ArtifactVersionDetail(Long id,Long artifactId,int versionNumber,String content,String sha256,String createdBy,java.time.Instant createdAt){}
 private String sha256(String content){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private void snapshot(ProjectArtifact artifact,String content,String actor){int next=versions.countByArtifact_IdAndWorkspace_Id(artifact.getId(),WorkspaceContextHolder.id())+1;versions.save(new ProjectArtifactVersion(WorkspaceContextHolder.require(),artifactProject(artifact),artifact,next,content,sha256(content),actor));}
 private VentureProject artifactProject(ProjectArtifact artifact){return projects.findByIdAndWorkspace_Id(artifact.getProjectId(),WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found."));}
 @Transactional(readOnly=true) public List<ArtifactVersionSummary> versionHistory(Long projectId,Long artifactId){project(projectId);ProjectArtifact a=artifacts.findById(artifactId).filter(x->x.getProjectId().equals(projectId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Artifact not found."));return versions.findAllByArtifact_IdAndWorkspace_IdOrderByVersionNumberDesc(a.getId(),WorkspaceContextHolder.id()).stream().map(v->new ArtifactVersionSummary(v.getId(),v.getArtifactId(),v.getVersionNumber(),v.getSha256(),v.getCreatedBy(),v.getCreatedAt())).toList();}
 @Transactional(readOnly=true) public ArtifactVersionDetail version(Long projectId,Long artifactId,Long versionId){project(projectId);ProjectArtifact a=artifacts.findById(artifactId).filter(x->x.getProjectId().equals(projectId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Artifact not found."));ProjectArtifactVersion v=versions.findByIdAndArtifact_IdAndWorkspace_Id(versionId,a.getId(),WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Version not found."));return new ArtifactVersionDetail(v.getId(),v.getArtifactId(),v.getVersionNumber(),v.getContent(),v.getSha256(),v.getCreatedBy(),v.getCreatedAt());}
 @Transactional public WorkspaceFile saveFile(Long projectId,String relative,String content){VentureProject p=project(projectId);WorkspaceContextHolder.requireAnyRole(com.aurora.identity.WorkspaceRole.OWNER,com.aurora.identity.WorkspaceRole.ADMIN,com.aurora.identity.WorkspaceRole.MEMBER);if(content==null||content.length()>50000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"File content must be at most 50,000 characters.");if(relative==null||!(relative.equals("src/App.tsx")||relative.equals("src/style.css")||relative.startsWith("src/components/")||relative.startsWith("src/pages/"))||relative.contains("..")||relative.startsWith("/")||relative.contains("\\"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only frontend source files can be edited.");Path base=root(WorkspaceContextHolder.id(),projectId);Path file=safe(base,relative);if(!Files.isRegularFile(file))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Source file not found.");ProjectArtifact a=artifacts.findByProject_IdAndWorkspace_IdAndRelativePath(projectId,WorkspaceContextHolder.id(),relative).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Artifact not found."));try{String old=Files.readString(file);if(old.equals(content))return new WorkspaceFile(relative,old,Files.size(file));snapshot(a,old,"before-manual-edit");Path tmp=file.resolveSibling(file.getFileName()+".aurora-tmp");Files.writeString(tmp,content,StandardCharsets.UTF_8);try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}a.updateSize(Files.size(file));artifacts.save(a);snapshot(a,content,"workspace-member");audit.record("PROJECT_SOURCE_EDITED","PROJECT_ARTIFACT",a.getId().toString(),"Saved source file "+relative+" with immutable version history.");return new WorkspaceFile(relative,content,Files.size(file));}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not save source file.");}}

 public ProjectArtifact saveQualityReport(Long projectId,long agentRunId,String agentId,String markdown){
  VentureProject p=project(projectId);Path root=root(WorkspaceContextHolder.id(),projectId);
  String relative="quality-reports/agent-run-"+agentRunId+"-"+agentId+".md";
  try{Path file=safe(root,relative);Files.createDirectories(file.getParent());Files.writeString(file,markdown,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
   ProjectArtifact a=artifacts.save(new ProjectArtifact(WorkspaceContextHolder.require(),p,"QUALITY_REPORT",agentId.equals("security-agent")?"Security review":"QA review",relative,"On-demand AI review report; findings require human verification",Files.size(file)));
   snapshot(a,markdown,agentId);audit.record("PROJECT_QUALITY_REPORT_CREATED","PROJECT_ARTIFACT",a.getId().toString(),"Saved "+agentId+" report. No source files were modified.");return a;
  }catch(FileAlreadyExistsException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"A report for this agent run already exists.");}
   catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not save quality report.");}
 }
 public byte[] exportZip(Long id){project(id);Path root=root(WorkspaceContextHolder.id(),id);if(!Files.exists(root))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workspace has not been scaffolded.");try(ByteArrayOutputStream b=new ByteArrayOutputStream();ZipOutputStream zip=new ZipOutputStream(b)){for(ProjectArtifact a:list(id)){Path f=safe(root,a.getRelativePath());if(Files.isRegularFile(f)){zip.putNextEntry(new ZipEntry(a.getRelativePath()));Files.copy(f,zip);zip.closeEntry();}}zip.finish();audit.record("PROJECT_WORKSPACE_EXPORTED","PROJECT",id.toString(),"Exported project zip.");return b.toByteArray();}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to export workspace.");}}
}
