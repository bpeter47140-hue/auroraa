package com.aurora.projects;

import com.aurora.audit.AuditService;
import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class CreativeStudioService {
    private static final long MAX_BYTES=5L*1024*1024;
    private static final Map<String,String> TYPES=Map.of("image/png","png","image/jpeg","jpg","image/webp","webp","image/gif","gif","video/mp4","mp4","video/webm","webm");
    private final VentureProjectRepository projects;
    private final CreativeAssetRepository assets;
    private final AuditService audit;
    @Value("${aurora.projects.storage-path:./data/project-workspaces}") private String storagePath;
    public CreativeStudioService(VentureProjectRepository projects,CreativeAssetRepository assets,AuditService audit){this.projects=projects;this.assets=assets;this.audit=audit;}
    private VentureProject project(Long id){return projects.findByIdAndWorkspace_Id(id,WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found."));}
    private Path root(Long ws,Long project){return Paths.get(storagePath).toAbsolutePath().normalize().resolve("creative-assets").resolve("ws-"+ws).resolve("project-"+project).normalize();}
    private Path safe(Path root,String key){Path path=root.resolve(key).normalize();if(!path.startsWith(root))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid asset path.");return path;}
    @Transactional(readOnly=true) public List<CreativeAsset> list(Long projectId){project(projectId);return assets.findAllByProject_IdAndWorkspace_IdOrderByCreatedAtDesc(projectId,WorkspaceContextHolder.id());}
    @Transactional public CreativeAsset upload(Long projectId,MultipartFile file,String notes,String actor){
        VentureProject p=project(projectId);WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);
        if(file==null||file.isEmpty()||file.getSize()>MAX_BYTES)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a non-empty file no larger than 5 MB.");
        String contentType=Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        String extension=TYPES.get(contentType);
        if(extension==null)throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Only PNG, JPEG, WebP, GIF, MP4 and WebM files are accepted.");
        String original=Optional.ofNullable(file.getOriginalFilename()).orElse("creative-asset");
        String base=Paths.get(original.replace('\\','/')).getFileName().toString().replaceAll("[^A-Za-z0-9._ -]","_");
        if(base.isBlank()||base.length()>160)base="creative-asset."+extension;
        byte[] bytes;try{bytes=file.getBytes();}catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Could not read uploaded file.");}
        if(!validSignature(contentType,bytes))throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"The file content does not match its declared media type.");
        Long ws=WorkspaceContextHolder.id();String key=UUID.randomUUID()+"."+extension;Path root=root(ws,projectId);Path target=safe(root,key);
        try{Files.createDirectories(root);Files.write(target,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not store creative asset.");}
        try{
            CreativeAsset saved=assets.save(new CreativeAsset(WorkspaceContextHolder.require(),p,base,contentType.startsWith("image/")?"IMAGE":"VIDEO",contentType,key,bytes.length,notes==null?null:notes.trim(),actor));
            audit.record("CREATIVE_ASSET_UPLOADED","CREATIVE_ASSET",saved.getId().toString(),"Uploaded "+saved.getMediaType()+" asset to project "+projectId+".");
            return saved;
        }catch(RuntimeException e){try{Files.deleteIfExists(target);}catch(IOException ignored){}throw e;}
    }
    @Transactional(readOnly=true) public CreativeStudioController.AssetFile file(Long projectId,Long assetId){
        project(projectId);CreativeAsset asset=assets.findByIdAndProject_IdAndWorkspace_Id(assetId,projectId,WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Asset not found."));
        Path target=safe(root(WorkspaceContextHolder.id(),projectId),asset.getStorageKey());
        try{return new CreativeStudioController.AssetFile(Files.readAllBytes(target),asset.getContentType(),asset.getName());}catch(IOException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Asset file is unavailable.");}
    }
    @Transactional public void delete(Long projectId,Long assetId){
        project(projectId);WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);
        CreativeAsset asset=assets.findByIdAndProject_IdAndWorkspace_Id(assetId,projectId,WorkspaceContextHolder.id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Asset not found."));
        Path target=safe(root(WorkspaceContextHolder.id(),projectId),asset.getStorageKey());
        try{Files.deleteIfExists(target);}catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not delete asset file.");}
        assets.delete(asset);audit.record("CREATIVE_ASSET_DELETED","CREATIVE_ASSET",assetId.toString(),"Deleted creative asset from project "+projectId+".");
    }
    private boolean validSignature(String type,byte[] b){
        if(b.length<4)return false;
        return switch(type){
            case "image/png" -> b.length>=8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71;
            case "image/jpeg" -> (b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;
            case "image/gif" -> b.length>=6&&new String(b,0,6,java.nio.charset.StandardCharsets.US_ASCII).matches("GIF8[79]a");
            case "image/webp" -> b.length>=12&&new String(b,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(b,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
            case "video/mp4" -> b.length>=12&&new String(b,4,4,java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp");
            case "video/webm" -> (b[0]&255)==0x1A&&(b[1]&255)==0x45&&(b[2]&255)==0xDF&&(b[3]&255)==0xA3;
            default -> false;
        };
    }
}