package com.aurora.projects;

import com.aurora.security.WorkspaceContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class ProjectPreviewService {
    private final VentureProjectRepository projects;
    private final ProjectWorkspaceService workspace;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Value("${aurora.preview.runner-url:http://localhost:8090}") private String runnerUrl;
    @Value("${aurora.preview.runner-token:}") private String runnerToken;
    @Value("${aurora.preview.signing-secret:}") private String signingSecret;

    public record BuildResult(String status, String logs, Instant builtAt) {}
    public record PreviewAccess(String url, Instant expiresAt) {}
    public record PreviewFile(byte[] content, String contentType) {}

    public ProjectPreviewService(VentureProjectRepository projects, ProjectWorkspaceService workspace, ObjectMapper json) {
        this.projects = projects; this.workspace = workspace; this.json = json;
    }

    private VentureProject project(Long id) {
        return projects.findByIdAndWorkspace_Id(id, WorkspaceContextHolder.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found."));
    }

    public BuildResult build(Long projectId) {
        VentureProject project = project(projectId);
        if (runnerToken == null || runnerToken.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The isolated preview builder is not configured.");
        try {
            String body = json.writeValueAsString(java.util.Map.of("workspaceId", WorkspaceContextHolder.id(), "projectId", project.getId()));
            HttpRequest request = HttpRequest.newBuilder(URI.create(runnerUrl.replaceAll("/+$", "") + "/v1/build"))
                .timeout(Duration.ofMinutes(7)).header("Authorization", "Bearer " + runnerToken)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 422) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Preview runner unavailable (HTTP " + response.statusCode() + ").");
            JsonNode node = json.readTree(response.body());
            String status = node.path("status").asText("FAILED");
            String logs = node.path("logs").asText("");
            Instant builtAt = Instant.now();
            if (node.hasNonNull("builtAt")) try { builtAt = Instant.parse(node.get("builtAt").asText()); } catch (Exception ignored) {}
            return new BuildResult(status, logs, builtAt);
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not reach the isolated preview builder."); }
    }

    public PreviewAccess createAccess(Long projectId) {
        VentureProject project = project(projectId);
        if (signingSecret == null || signingSecret.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Preview access signing is not configured.");
        long expires = Instant.now().plus(Duration.ofMinutes(30)).getEpochSecond();
        String payload = WorkspaceContextHolder.id() + "." + project.getId() + "." + expires;
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
        return new PreviewAccess("/api/v1/projects/" + projectId + "/workspace/preview/" + token + "/index.html", Instant.ofEpochSecond(expires));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("Could not sign preview access.", e); }
    }

    private long validate(Long projectId, String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2) throw new IllegalArgumentException();
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] claims = payload.split("\\.");
            if (claims.length != 3 || Long.parseLong(claims[0]) < 1 || Long.parseLong(claims[1]) != projectId || Long.parseLong(claims[2]) < Instant.now().getEpochSecond()) throw new IllegalArgumentException();
            byte[] expected = sign(payload).getBytes(StandardCharsets.UTF_8);
            byte[] actual = parts[1].getBytes(StandardCharsets.UTF_8);
            if (!java.security.MessageDigest.isEqual(expected, actual)) throw new IllegalArgumentException();
            return Long.parseLong(claims[0]);
        } catch (Exception e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Preview link is invalid or expired."); }
    }

    public PreviewFile file(Long projectId, String token, String requestedPath) {
        long workspaceId = validate(projectId, token);
        if (projects.findByIdAndWorkspace_Id(projectId, workspaceId).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found.");
        String relative = requestedPath == null ? "index.html" : requestedPath.replaceFirst("^/+", "");
        if (relative.isBlank()) relative = "index.html";
        if (relative.contains("\\") || relative.contains("..") || relative.startsWith("/")) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Preview file not found.");
        try {
            byte[] content = workspace.readPreview(workspaceId, projectId, relative);
            String type = Files.probeContentType(java.nio.file.Path.of(relative));
            if (type == null) type = switch (relative.substring(relative.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)) {
                case "html" -> "text/html"; case "js" -> "text/javascript"; case "css" -> "text/css";
                case "svg" -> "image/svg+xml"; case "png" -> "image/png"; case "jpg", "jpeg" -> "image/jpeg";
                case "webp" -> "image/webp"; case "woff2" -> "font/woff2"; case "json" -> "application/json";
                default -> "application/octet-stream";
            };
            return new PreviewFile(content, type);
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Preview file not found."); }
    }
}
