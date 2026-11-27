package com.aurora.projects;

import com.aurora.security.WorkspaceContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ProjectBrowserQAService {
    private final ProjectPreviewService previews;
    private final ProjectWorkspaceService workspace;
    private final ObjectMapper json;
    @Value("${aurora.browser-qa.url:http://localhost:8091}") private String runnerUrl;
    @Value("${aurora.browser-qa.token:}") private String runnerToken;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public ProjectBrowserQAService(ProjectPreviewService previews,ProjectWorkspaceService workspace,ObjectMapper json){this.previews=previews;this.workspace=workspace;this.json=json;}

    public JsonNode run(Long projectId){
        if(runnerToken==null||runnerToken.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Browser QA runner is not configured.");
        var build=previews.build(projectId);
        if(!"SUCCEEDED".equalsIgnoreCase(build.status()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Build must succeed before browser QA. Build logs: "+build.logs());
        var access=previews.createAccess(projectId);
        String target="http://backend:8080"+access.url();
        try{
            String body=json.writeValueAsString(java.util.Map.of("url",target));
            var request=HttpRequest.newBuilder(URI.create(runnerUrl.replaceAll("/+$","")+"/run"))
                .timeout(Duration.ofSeconds(100)).header("Authorization","Bearer "+runnerToken).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Browser QA runner returned HTTP "+response.statusCode());
            JsonNode report=json.readTree(response.body());
            if(report.toString().length()>4000000)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Browser QA report exceeds safe size.");
            ObjectNode stored=((ObjectNode)report).deepCopy();stored.remove("screenshots");
            workspace.saveQualityReport(projectId,System.currentTimeMillis(),"browser-qa",stored.toPrettyString());
            return report;
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Browser QA failed to run.");}
    }
}
