package com.aurora.projects;

import com.aurora.identity.WorkspaceRole;
import com.aurora.security.WorkspaceContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/campaigns")
public class CampaignExportController {
    private final MarketingCampaignRepository campaigns;
    private final CreativeStudioService creative;
    public CampaignExportController(MarketingCampaignRepository campaigns, CreativeStudioService creative) {
        this.campaigns=campaigns; this.creative=creative;
    }

    @GetMapping(value="/{campaignId}/export.zip", produces="application/zip")
    public ResponseEntity<byte[]> export(@PathVariable Long projectId,@PathVariable Long campaignId) {
        WorkspaceContextHolder.requireAnyRole(WorkspaceRole.OWNER,WorkspaceRole.ADMIN,WorkspaceRole.MEMBER);
        MarketingCampaign campaign=campaigns.findByIdAndProject_IdAndWorkspace_Id(campaignId,projectId,WorkspaceContextHolder.id())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Campaign not found."));
        List<CreativeAsset> assets=creative.list(projectId);
        if(assets.size()>10) assets=assets.subList(0,10);
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(ZipOutputStream zip=new ZipOutputStream(bytes,StandardCharsets.UTF_8)) {
                String readme="# "+campaign.getTitle()+"\n\n"
                    +"Channel: "+campaign.getChannel()+"\nFormat: "+campaign.getFormat()+"\nStatus: "+campaign.getStatus()+"\n"
                    +"Created: "+campaign.getCreatedAt()+"\nScheduled in AURORA: "+(campaign.getScheduledAt()==null?"No":campaign.getScheduledAt())+"\n\n"
                    +"This is an export pack, not a published post. Review the copy, verify all claims and attach the correct media before publishing.\n\n"
                    +"## Campaign copy\n\n"+campaign.getContent()+"\n\n"
                    +"## Call to action\n\n"+(campaign.getCallToAction()==null?"Not specified":campaign.getCallToAction())+"\n\n"
                    +"## Review\n\nStatus: "+campaign.getStatus()+"\nReviewer: "+(campaign.getReviewedBy()==null?"Not reviewed":campaign.getReviewedBy())+"\nFeedback: "+(campaign.getFeedback()==null?"None":campaign.getFeedback())+"\n";
                put(zip,"CAMPAIGN.md",readme.getBytes(StandardCharsets.UTF_8));
                StringBuilder manifest=new StringBuilder("AURORA CAMPAIGN EXPORT\nGenerated: ").append(Instant.now()).append("\nAssets included: ").append(assets.size()).append("\n\n");
                long total=0;int included=0;
                for(CreativeAsset asset:assets) {
                    if(total+asset.getSizeBytes()>20L*1024*1024) continue;
                    CreativeStudioController.AssetFile file=creative.file(projectId,asset.getId());
                    String filename=asset.getName().replaceAll("[^A-Za-z0-9._ -]","_").replaceAll("\\.\\.","_");
                    put(zip,"assets/"+asset.getId()+"-"+filename,file.content());
                    total+=file.content().length;included++;
                    manifest.append(asset.getName()).append(" | ").append(asset.getMediaType()).append(" | ").append(asset.getContentType()).append(" | ").append(asset.getNotes()==null?"":asset.getNotes()).append("\n");
                }
                manifest.append("\nOnly the first 10 project assets are considered; assets over the 20 MB total export limit are skipped.\n");
                put(zip,"ASSET-MANIFEST.txt",manifest.toString().getBytes(StandardCharsets.UTF_8));
            }
            String filename="aurora-campaign-"+campaignId+"-pack.zip";
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"")
                .header(HttpHeaders.CACHE_CONTROL,"private, no-store").body(bytes.toByteArray());
        } catch(IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Could not create campaign export.");
        }
    }
    private static void put(ZipOutputStream zip,String name,byte[] bytes)throws IOException {
        zip.putNextEntry(new ZipEntry(name));zip.write(bytes);zip.closeEntry();
    }
}
